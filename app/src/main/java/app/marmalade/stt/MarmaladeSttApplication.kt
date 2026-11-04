package app.marmalade.stt

import android.app.Application
import android.util.Log
import app.marmalade.stt.data.SpeechSettings
import app.marmalade.stt.install.ExtraEngines
import app.marmalade.stt.install.ModelInstaller
import app.marmalade.stt.speech.SpeechEngineManager
import app.marmalade.stt.speech.SpeedCalibrator
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. `@HiltAndroidApp` generates the app-level Hilt
 * component that [MainActivity] and every `hiltViewModel()` draw from.
 *
 * At startup it loads the speech model in the background, if one is
 * installed, so the first dictation doesn't wait for it, starts keeping
 * engines that fall behind real time as speed-budget evidence (#30, #27),
 * and cleans up after models that left the app: their files (engines cut in
 * #45, the Smart Turn download from before #44) and stored picks of them, so
 * an upgrader on a removed engine ends up on the recommendation.
 */
@HiltAndroidApp
class MarmaladeSttApplication : Application() {

    @Inject lateinit var speechEngines: SpeechEngineManager

    @Inject lateinit var speedCalibrator: SpeedCalibrator

    @Inject lateinit var installer: ModelInstaller

    @Inject lateinit var speechSettings: SpeechSettings

    override fun onCreate() {
        // Before Hilt builds anything and before cleanUpRemovedEngines: an engine missing from
        // the catalog would have its files deleted.
        ExtraEngines.load(this)
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch { speechEngines.warmUp() }
        scope.launch {
            try {
                cleanUpRemovedEngines(installer, speechSettings, speedCalibrator)
            } catch (e: IOException) {
                Log.w(TAG, "Couldn't forget removed engines", e)
            }
        }
        speedCalibrator.watch(speechEngines.tooSlow)
    }

    private companion object {
        const val TAG = "MarmaladeStt"
    }
}
