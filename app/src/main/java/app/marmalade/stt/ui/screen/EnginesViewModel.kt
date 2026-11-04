package app.marmalade.stt.ui.screen

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marmalade.stt.R
import app.marmalade.stt.data.SpeechPrefs
import app.marmalade.stt.data.SpeechSettings
import app.marmalade.stt.install.ExtraEngines
import app.marmalade.stt.install.InstallState
import app.marmalade.stt.install.ModelDownloads
import app.marmalade.stt.install.ModelInstaller
import app.marmalade.stt.install.SttEngineCatalog
import app.marmalade.stt.speech.SpeedCalibrator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State and actions for [EnginesScreen], its cards and rows and the engine details dialog.
 * Install state is the installer's own per-engine flow, so a download started
 * here, in the setup guide, or before the user left the tab shows the same
 * progress everywhere. Downloads go through [ModelDownloads] (app-wide scope)
 * so they outlive this ViewModel; "Check files" and "Remove" are short and
 * run here.
 *
 * The list is [engineGroups]: cards for the engines offered within this
 * phone's speed budget (#24), rows behind "Other engines" for the rest.
 * Install states cover the whole catalog, so a developer engine that is still
 * downloading when the setting goes off keeps its state for when it comes back. Opening the tab
 * measures the phone's speed once ([SpeedCalibrator.ensureMeasured]) if it
 * never was.
 *
 * Picking an engine and its language are [SpeechSettings] writes;
 * SpeechEngineManager follows the prefs and rebuilds the engine itself.
 */
@HiltViewModel
class EnginesViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val installer: ModelInstaller,
    private val downloads: ModelDownloads,
    private val settings: SpeechSettings,
    calibrator: SpeedCalibrator,
) : ViewModel() {

    init {
        calibrator.ensureMeasured()
        // New engines in the fork's manifest (ExtraEngines) appear in the list as soon as it arrives.
        viewModelScope.launch { ExtraEngines.refresh(context) }
    }

    /**
     * The speech settings: which engine runs, whether developer engines are
     * shown, the recommended engine, the language picks, the phone's speed.
     * Null until DataStore has been read, so no card flashes the wrong radio.
     */
    val prefs: StateFlow<SpeechPrefs?> = settings.prefs
        .catch { e ->
            // Unreadable settings file: show (and listen with) the defaults.
            Log.w(TAG, "Couldn't read the speech settings", e)
            emit(SpeechPrefs())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Live install state per engine id, for every catalog engine. Follows the
     * catalog itself too, which grows when [ExtraEngines] fetches a manifest.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val states: StateFlow<Map<String, InstallState>> =
        ExtraEngines.engines.flatMapLatest {
            val all = SttEngineCatalog.engines
            combine(all.map { engine -> installer.state(engine.id) }) { values ->
                all.map { it.id }.zip(values).toMap()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SttEngineCatalog.engines.associate { it.id to installer.state(it.id).value },
        )

    /**
     * The engines to list ([engineGroups]): full cards for the engines offered
     * on this phone, compact rows behind "Other engines" for the rest. Empty
     * until the settings have been read, so the order never jumps when the
     * recommended engine arrives.
     */
    val engines: StateFlow<EngineGroups> = combine(prefs, states) { p, s ->
        if (p == null) return@combine NO_ENGINES
        engineGroups(p, s.filterValues { it != InstallState.NotInstalled }.keys)
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NO_ENGINES)

    private val _checking = MutableStateFlow<Set<String>>(emptySet())

    /** The engines whose files "Check files" is re-hashing. */
    val checking: StateFlow<Set<String>> = _checking.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)

    /** A one-off snackbar message (a string resource), cleared by [messageShown]. */
    val message: StateFlow<Int?> = _message.asStateFlow()

    /**
     * Cheap disk re-probe of every catalog engine (sizes and install record,
     * no hashing), so files removed behind the app's back show up. The
     * installer publishes the result; a running download is left alone.
     */
    fun refresh() {
        viewModelScope.launch {
            for (engine in SttEngineCatalog.engines) installer.verify(engine.id)
        }
    }

    /** Start, resume or retry a download; also re-downloads over damaged files. */
    fun download(engineId: String) = downloads.start(engineId)

    /** Stop a running download. The bytes so far are kept (Paused). */
    fun cancel(engineId: String) = downloads.cancel(engineId)

    /** Delete a partial download. */
    fun removeDownload(engineId: String) {
        viewModelScope.launch {
            installer.discardDownload(engineId).onFailure { e ->
                Log.w(TAG, "Couldn't remove the download of $engineId", e)
                show(R.string.engine_remove_download_failed)
            }
        }
    }

    /**
     * Re-hash every installed file ("Check files"). The installer publishes
     * Installed or Corrupt to [states]; the snackbar says which.
     */
    fun checkFiles(engineId: String) {
        if (engineId in _checking.value) return
        _checking.update { it + engineId }
        viewModelScope.launch {
            try {
                when (installer.verify(engineId, deep = true)) {
                    InstallState.Installed -> show(R.string.engine_check_ok)
                    is InstallState.Corrupt -> show(R.string.engine_check_damaged)
                    // Something else changed the files meanwhile; the status line shows what.
                    else -> Unit
                }
            } catch (e: IOException) {
                Log.w(TAG, "Check files failed for $engineId", e)
                show(R.string.engine_check_failed)
            } finally {
                _checking.update { it - engineId }
            }
        }
    }

    /**
     * Delete the model. A running download is cancelled first; uninstall
     * waits for it to stop. Removing the selected engine selects the default
     * again ([selectionAfterRemoving]), so the app falls back to the user engine.
     */
    fun remove(engineId: String) {
        if (downloads.isRunning(engineId)) downloads.cancel(engineId)
        viewModelScope.launch {
            installer.uninstall(engineId)
                .onSuccess {
                    try {
                        selectionAfterRemoving(engineId, settings.current())?.let { settings.setSelectedEngine(it) }
                    } catch (e: IOException) {
                        // The selection still names this engine; with its files
                        // gone, dictation reports that no model is installed.
                        Log.w(TAG, "Couldn't reset the engine choice after removing $engineId", e)
                        show(R.string.engine_use_failed)
                    }
                }
                .onFailure { e ->
                    Log.w(TAG, "Couldn't remove $engineId", e)
                    show(R.string.engine_remove_failed)
                }
        }
    }

    /** A card's "Use" or a tap on an installed row: run [engineId] from the next listening session on. */
    fun select(engineId: String) = save(engineId, R.string.engine_use_failed) { settings.setSelectedEngine(engineId) }

    /** Show or hide "Other engines"; remembered. */
    fun setOtherEnginesExpanded(expanded: Boolean) {
        viewModelScope.launch {
            try {
                settings.setOtherEnginesExpanded(expanded)
            } catch (e: IOException) {
                Log.w(TAG, "Couldn't save the Other engines state", e)
            }
        }
    }

    /** [code] is one of the engine's language options, or "" for auto-detect. */
    fun setLanguage(engineId: String, code: String) =
        save(engineId, R.string.engine_language_failed) { settings.setEngineLanguage(engineId, code) }

    private fun save(engineId: String, @StringRes failure: Int, write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (e: IOException) {
                Log.w(TAG, "Couldn't save a speech setting for $engineId", e)
                show(failure)
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun show(@StringRes message: Int) {
        _message.value = message
    }

    private companion object {
        const val TAG = "EnginesViewModel"
        val NO_ENGINES = EngineGroups(emptyList(), emptyList())
    }
}
