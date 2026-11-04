package app.marmalade.stt.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.marmalade.stt.R
import app.marmalade.stt.data.SpeechPrefs
import app.marmalade.stt.install.EngineLanguages
import app.marmalade.stt.install.EngineQuality
import app.marmalade.stt.install.InstallState
import app.marmalade.stt.install.LanguageOptions
import app.marmalade.stt.install.SttEngineDescriptor
import app.marmalade.stt.ui.formatBytes
import app.marmalade.stt.ui.rememberUiLocale
import app.marmalade.stt.ui.theme.isDark
import app.marmalade.ui.theme.depth
import java.util.Locale

// -----------------------------------------------------------------------------
// Data flow
// -----------------------------------------------------------------------------
//   EnginesScreen → a card's "Show more" or a row's chevron
//     → EngineInfoDialog(engine) (centered modal; the lab's B1 spec sheet)
//     state    ◄── EnginesViewModel.states[id]
//     prefs    ◄── EnginesViewModel.prefs (language pick, speed budget, the
//                  phone's measured factor); null until read
//     checking ◄── id in EnginesViewModel.checking (deep verify running)
//     snackbar ◄── EnginesViewModel.message, hosted inside the dialog while it
//                  is open (the screen's own host would sit under it)
//
//   Title, pills (engineFacts), description, SpecTiles, the install status
//   while it has something to say; then labelled paragraphs: Language for this
//   engine (developers only: LanguageDialog → onSetLanguage → vm.setLanguage →
//   SpeechSettings); Speed (speedTierRes + speedFactLines: cost vs Parakeet
//   Ultra, and whether the phone is measured yet); Quality (qualitySource,
//   rated in ratingLanguage(prefs) like the tiles); Languages
//   (EngineLanguages.of, by name); Chunk size (streamingStepMs, Nemotron
//   3.5); Size (+ "Check files" → vm.checkFiles);
//   License (→ onOpenLicense → Routes.licenseText); Source.
//   Buttons: engineActions(state, onDetail = true) without "Check files"
//   → the same handlers as the card, plus Close.
// -----------------------------------------------------------------------------

/**
 * Everything about one engine that doesn't fit on its card, and its file
 * management ("Check files", "Remove"). A centered dialog over the list
 * (Max, 2026-09-27: a modal, not a bottom sheet); its content scrolls, and
 * the list's dialogs (download, remove) open on top of it.
 */
@Composable
fun EngineInfoDialog(
    engine: SttEngineDescriptor,
    state: InstallState,
    prefs: SpeechPrefs?,
    checking: Boolean,
    snackbarHostState: SnackbarHostState,
    onAction: (EngineAction) -> Unit,
    onSetLanguage: (String) -> Unit,
    onOpenLicense: (licenseKey: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var choosingLanguage by rememberSaveable { mutableStateOf(false) }
    // A long sheet ran under the navigation bar on Android 15 (emulator, #35):
    // the dialog window spans the screen, and the 3-button bar (a taskbar
    // window there) reports no inset to either window. So the sheet is capped
    // at 80% of the screen's height, centered, as well as kept clear of the
    // insets that are reported.
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.8f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        EngineDetails(
            engine = engine,
            state = state,
            prefs = prefs,
            checking = checking,
            snackbarHostState = snackbarHostState,
            onAction = onAction,
            onChooseLanguage = { choosingLanguage = true },
            onOpenLicense = onOpenLicense,
            onDismiss = onDismiss,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .heightIn(max = maxHeight)
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
        )
    }

    val languageOptions = engine.languageOptions
    if (languageOptions != null && prefs?.developerEngines == true && choosingLanguage) {
        LanguageDialog(
            title = stringResource(R.string.engine_language),
            options = rememberUiLocale().let { locale ->
                remember(languageOptions, locale) { sortedLanguageChoices(languageOptions.codes, locale) }
            },
            selected = prefs.languageFor(engine),
            label = { languageChoiceLabel(it) },
            onSelect = { code ->
                onSetLanguage(code)
                choosingLanguage = false
            },
            onDismiss = { choosingLanguage = false },
        )
    }
}

/**
 * The dialog's card, outside the Dialog window so it can be previewed: the
 * scrolling spec sheet, then its buttons. Cream with a hairline in light mode
 * (the tiles are the cream-orange), the stone card in dark (tiles a step above).
 */
@Composable
fun EngineDetails(
    engine: SttEngineDescriptor,
    state: InstallState,
    prefs: SpeechPrefs?,
    checking: Boolean,
    snackbarHostState: SnackbarHostState,
    onAction: (EngineAction) -> Unit,
    onChooseLanguage: () -> Unit,
    onOpenLicense: (licenseKey: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (colors.isDark) colors.surfaceContainerHigh else colors.surface,
        border = if (colors.isDark) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier,
    ) {
        // The snackbar floats over the bottom of the dialog, not at the
        // end of the scrolling content, where it could be out of view.
        Box {
            Column {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                ) {
                    EngineDetailsBody(
                        engine = engine,
                        state = state,
                        prefs = prefs,
                        checking = checking,
                        onAction = onAction,
                        onChooseLanguage = onChooseLanguage,
                        onOpenLicense = onOpenLicense,
                    )
                }
                EngineActionBar(
                    actions = engineActions(state, onDetail = true) - EngineAction.CHECK_FILES,
                    onAction = onAction,
                    downloadSize = formatBytes(engine.sizeBytes),
                    resumeFocus = rememberResumeFocus(state),
                    modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                ) {
                    EngineTextButton(label = stringResource(R.string.engine_dialog_close), onClick = onDismiss)
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Title, pills, description, the three tiles and the install status, then the labelled facts. */
@Composable
private fun EngineDetailsBody(
    engine: SttEngineDescriptor,
    state: InstallState,
    prefs: SpeechPrefs?,
    checking: Boolean,
    onAction: (EngineAction) -> Unit,
    onChooseLanguage: () -> Unit,
    onOpenLicense: (licenseKey: String) -> Unit,
) {
    val tier = prefs?.speedTier(engine)
    val language = ratingLanguage(prefs, Locale.getDefault().toLanguageTag())
    Text(
        text = engine.displayName,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp),
        modifier = Modifier.semantics { heading() },
    )
    // Being in use shows on the card; the dialog keeps to what the engine is.
    val pills = engineFacts(engine, prefs?.recommendedEngineId, prefs?.developerEngines == true, inUse = false, tier = tier)
    if (pills.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        EnginePills(pills, long = true)
    }
    Spacer(Modifier.height(12.dp))
    Text(text = engine.descriptionText ?: stringResource(engine.description), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
    // Dark: raised above the dialog (#84; surfaceContainer now sits below it). Light: the cream-orange as before.
    val depth = MaterialTheme.depth
    val tileColor = if (depth.isDark) depth.raised else MaterialTheme.colorScheme.surfaceContainer
    SpecTiles(engine = engine, speedTier = tier, language = language, tileColor = tileColor)
    // Installed and not downloaded say themselves through the buttons and the size.
    if (state !is InstallState.Installed && state != InstallState.NotInstalled) {
        Spacer(Modifier.height(12.dp))
        EngineStatus(state = state)
    }
    Spacer(Modifier.height(4.dp))

    // The per-engine override of Settings → Recognition language (#27): developers only.
    if (engine.languageOptions != null && prefs?.developerEngines == true) {
        DetailParagraph(
            label = stringResource(R.string.engine_language),
            value = languageChoiceLabel(prefs.languageFor(engine)),
            onClick = onChooseLanguage,
        )
    }
    // Needs the settings for the phone's budget and factor; no paragraph until read.
    if (prefs != null) {
        DetailParagraph(label = stringResource(R.string.engine_detail_speed), value = speedText(engine, prefs))
    }
    val source = qualitySource(EngineQuality.rating(engine, language))
    DetailParagraph(
        label = stringResource(R.string.engine_detail_quality),
        value = source.language?.let { stringResource(source.text, *(source.sets + languageNameInText(it)).toTypedArray()) }
            ?: stringResource(source.text),
    )
    val languages = EngineLanguages.of(engine)
    DetailParagraph(
        label = pluralStringResource(R.plurals.engine_languages_count, languages.size, languages.size),
        value = sortedLanguageNames(languages, rememberUiLocale()).joinToString(stringResource(R.string.list_separator)),
    )
    streamingStepMs(engine)?.let { ms ->
        DetailParagraph(label = stringResource(R.string.engine_detail_chunk), value = stringResource(R.string.engine_chunk_steps, ms))
    }
    // Files are stored as downloaded, so the download size is the size on disk.
    val onDisk = state is InstallState.Installed || state is InstallState.Corrupt
    val checkFiles = EngineAction.CHECK_FILES in engineActions(state, onDetail = true)
    // At large font sizes the button goes under the size: beside it, it left
    // the label and value a narrow column (#68).
    val checkFilesBelow = stackSpecs()
    DetailParagraph(
        label = stringResource(if (onDisk) R.string.engine_detail_size_on_disk else R.string.engine_detail_download_size),
        value = formatBytes(engine.sizeBytes),
    ) {
        if (checkFiles && !checkFilesBelow) {
            CheckFilesButton(checking = checking, onClick = { onAction(EngineAction.CHECK_FILES) })
        }
    }
    if (checkFiles && checkFilesBelow) {
        CheckFilesButton(checking = checking, onClick = { onAction(EngineAction.CHECK_FILES) })
    }
    DetailParagraph(
        label = stringResource(R.string.engine_detail_license),
        value = engine.licenseId,
        onClick = { onOpenLicense(engine.licenseKey) },
    )
    DetailParagraph(
        label = stringResource(R.string.engine_detail_source),
        value = modelSourceLabel(engine.files.first().url),
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * "Fastest on this phone. 0.3× the decoding time of Parakeet Ultra
 * (estimated)." and, until the phone is measured, "This phone hasn't been
 * measured yet" on its own line. Just the tier for an engine without a
 * reference measurement.
 */
@Composable
private fun speedText(engine: SttEngineDescriptor, prefs: SpeechPrefs): String {
    val phrase = stringResource(speedTierRes(prefs.speedTier(engine)))
    val facts = speedFactLines(engine.id, prefs.deviceSpeedFactor) ?: return phrase
    val cost = facts.first()
    val lines = listOf(stringResource(R.string.engine_detail_speed_text, phrase, cost.resolve())) +
        facts.drop(1).map { it.resolve() }
    return lines.joinToString("\n")
}

/**
 * A label over its value, the dialog's paragraph. With [onClick] the whole
 * paragraph is a button with a chevron (License, Language); [trailing] sits
 * at its end ("Check files" by the size).
 */
@Composable
private fun DetailParagraph(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val interaction = if (onClick != null) {
        Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .then(interaction)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

/** "Auto-detect" for [LanguageOptions.AUTO], else the language's name ("German"). */
@Composable
private fun languageChoiceLabel(code: String): String =
    if (code == LanguageOptions.AUTO) stringResource(R.string.engine_language_auto) else languageName(code)
