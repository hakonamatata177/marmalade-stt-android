package app.marmalade.stt.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import app.marmalade.stt.R
import app.marmalade.stt.data.SpeechPrefs
import app.marmalade.stt.install.InstallState
import app.marmalade.stt.install.SttEngineDescriptor
import app.marmalade.stt.ui.formatBytes
import app.marmalade.stt.ui.progressPercent
import app.marmalade.stt.ui.rememberUiLocale
import app.marmalade.stt.ui.theme.accent
import app.marmalade.ui.components.MarmaladeCard
import app.marmalade.ui.theme.depth
import java.util.Locale

// -----------------------------------------------------------------------------
// Data flow
// -----------------------------------------------------------------------------
//   EnginesScreen → EngineCard(engine, state, prefs) per EngineGroups.main
//     engineCardUi(engine, state, prefs) → selection + actions
//     prefs.speedTier(engine)            → SPEED meter, "May be slow" pill
//     engineFacts(...)                   → pills (Recommended, May be slow),
//                                          size-line tags (Streaming, Developer)
//     languageNote(engine, prefs, locale) → "Doesn't transcribe Japanese" /
//                                          "Can't detect the language…"
//     ratingLanguage(prefs, locale)      → the language QUALITY rates in
//                                          (Recognition language, else UI locale)
//     EngineSpecColumn                   → SPEED / QUALITY / LANGUAGES
//
//   Show more → onShowMore → EngineInfoDialog (B1)
//   Action row: Remove + Use (→ onUse → vm.select) / Remove + "✓ In use" /
//   the download bar + Cancel / Download · size / Resume, Retry, … (→ onAction)
// -----------------------------------------------------------------------------

/**
 * One engine as a full card, the design lab's A1 (Max, 2026-09-28), after
 * marmalade-tts-android's cards: name, pills, the full description, the
 * language note, the size line and "Show more" beside the SPEED / QUALITY /
 * LANGUAGES column; the buttons in a full-width row at the bottom, secondary
 * at the start and primary at the end. The card itself isn't a button: "Use"
 * picks the engine. The card in use gets a Toast outline (the preset's pill
 * colour, [app.marmalade.stt.ui.theme.AccentColors]) and a "✓ In use" pill
 * where "Use" would be; never orange, which the setup guide's Download
 * keeps. Its frame: [EngineCardFrame].
 *
 * [prefs] is null until the settings have been read: no Use button, pills or
 * speed then, rather than a guess.
 *
 * At 150 % font size and up ([stackSpecs]) the spec column goes under the
 * text at full width: in 118 dp its labels broke mid-word (#68).
 */
@Composable
fun EngineCard(
    engine: SttEngineDescriptor,
    state: InstallState,
    prefs: SpeechPrefs?,
    onUse: () -> Unit,
    onShowMore: () -> Unit,
    onAction: (EngineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = engineCardUi(engine, state, prefs)
    val tier = prefs?.speedTier(engine)
    val selected = ui.selection == EngineSelection.SELECTED
    val facts = engineFacts(engine, prefs?.recommendedEngineId, prefs?.developerEngines == true, inUse = selected, tier = tier)
    val systemLocale = Locale.getDefault().toLanguageTag()
    val note = languageNote(engine, prefs, systemLocale)
    val language = ratingLanguage(prefs, systemLocale)
    EngineCardFrame(
        selected = selected,
        // One TalkBack stop for the card's text; its links and buttons stay their own.
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (stackSpecs()) {
                EngineCardText(engine = engine, state = state, facts = facts, note = note, onShowMore = onShowMore)
                Spacer(Modifier.height(8.dp))
                EngineSpecColumn(engine = engine, speedTier = tier, language = language)
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    EngineCardText(
                        engine = engine,
                        state = state,
                        facts = facts,
                        note = note,
                        onShowMore = onShowMore,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    EngineSpecColumn(engine = engine, speedTier = tier, language = language)
                }
            }
            EngineCardActions(engine = engine, state = state, ui = ui, onUse = onUse, onAction = onAction)
        }
    }
}

/**
 * An engine card's frame. Light mode: the outlined card from before the
 * library (Max, #87 round 3: "I want the before of the stt engine cards"),
 * the page's colour inside a hairline, the cream container in use. Dark
 * mode: the library's card (#84), as TTS's Engines tab, with no wash in use
 * ("the in use engine doesn't get a darker background, just the outline").
 * In use, both modes: a 2 dp outline in the pill colour (Toast in
 * Marmalade; primaryContainer is toast only in light mode).
 */
@Composable
private fun EngineCardFrame(selected: Boolean, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val inUse = if (selected) BorderStroke(2.dp, MaterialTheme.accent.pill) else null
    if (MaterialTheme.depth.isDark) {
        MarmaladeCard(border = inUse, modifier = modifier, content = content)
    } else {
        OutlinedCard(
            shape = MaterialTheme.shapes.medium,
            colors = if (selected) {
                // contentColor explicitly: light mode's surfaceContainer is the same colour as
                // surfaceVariant, so contentColorFor() would pick the grey onSurfaceVariant.
                CardDefaults.outlinedCardColors(containerColor = colors.surfaceContainer, contentColor = colors.onSurface)
            } else {
                CardDefaults.outlinedCardColors()
            },
            border = inUse ?: CardDefaults.outlinedCardBorder(),
            modifier = modifier,
            content = content,
        )
    }
}

/** The card's text column: name, pills, description, language note, size line and "Show more". */
@Composable
private fun EngineCardText(
    engine: SttEngineDescriptor,
    state: InstallState,
    facts: List<EngineFact>,
    note: LanguageNote?,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = engine.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            // Merges up: the whole card is a heading stop, so TalkBack's
            // heading navigation jumps card to card (#68).
            modifier = Modifier.semantics { heading() },
        )
        val pills = facts.filter { it == EngineFact.Recommended || it == EngineFact.MayBeSlow }
        if (pills.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            EnginePills(pills)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = engine.descriptionText ?: stringResource(engine.description),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        if (note != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = languageNoteText(note),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = sizeLine(engine, state, facts),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
        // In the text column, not under the row: the spec column is
        // usually taller, and a link below it would float in the gap.
        ShowMoreLink(engineName = engine.displayName, onClick = onShowMore)
    }
}

/**
 * "Doesn't transcribe Japanese", and on the engine in use "… Whisper Large v3
 * Turbo does."; for an engine that can't detect the language, "Can't
 * detect the language. Choose one in Settings → Recognition language:
 * English, Spanish, German, French."
 */
@Composable
fun languageNoteText(note: LanguageNote): String {
    if (note.choices.isNotEmpty()) {
        val locale = rememberUiLocale()
        val names = note.choices.joinToString(stringResource(R.string.list_separator)) { languageNameInText(it, locale) }
        return stringResource(R.string.engine_card_language_choose, names)
    }
    val language = languageNameInText(note.language)
    return note.suggestion?.let { stringResource(R.string.engine_card_language_suggest, language, it.displayName) }
        ?: stringResource(R.string.engine_card_language_missing, language)
}

/** "682 MB on your phone" / "682 MB download", then " · Streaming · 1120 ms steps" and " · Developer". */
@Composable
private fun sizeLine(engine: SttEngineDescriptor, state: InstallState, facts: List<EngineFact>): String {
    val onDisk = state is InstallState.Installed || state is InstallState.Corrupt
    val size = stringResource(
        if (onDisk) R.string.engine_card_size_on_disk else R.string.engine_card_size_download,
        formatBytes(engine.sizeBytes),
    )
    val tags = facts.filter { it is EngineFact.Streaming || it == EngineFact.Developer }.map { factLabel(it) }
    return (listOf(size) + tags).joinToString(FACT_SEPARATOR)
}

/**
 * "Show more" in the link colour; TalkBack and Voice Access get "Show more,
 * <engine>", like the card's buttons: the visible words first, then whose
 * (the visible text is replaced, not read after it, see [ButtonLabel]). Its
 * tap target is 48 dp tall (and at least 48 wide) but it takes the room of
 * the 24 dp link: the extra hangs over the size line above and the gap below
 * ([overhang]), so nothing moves (#68).
 */
@Composable
private fun ShowMoreLink(engineName: String, onClick: () -> Unit) {
    val label = stringResource(R.string.engine_show_more)
    val spoken = stringResource(R.string.a11y_engine_action_named, label, engineName)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.accent.ink,
        modifier = Modifier
            .padding(top = 2.dp)
            .overhang(ShowMoreOverhang)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.Button, onClick = onClick)
            // Clears the Text's own text; the click above it stays.
            .clearAndSetSemantics { contentDescription = spoken }
            .widthIn(min = 48.dp)
            .padding(vertical = 4.dp + ShowMoreOverhang),
    )
}

/**
 * Lays the content out [extra] shorter at the top and the bottom than it
 * measures, drawn [extra] higher: padding inside this grows a tap target
 * without taking room from its neighbours.
 */
private fun Modifier.overhang(extra: Dp): Modifier = layout { measurable, constraints ->
    val px = extra.roundToPx()
    val placeable = measurable.measure(constraints.offset(vertical = 2 * px))
    layout(placeable.width, (placeable.height - 2 * px).coerceAtLeast(0)) { placeable.place(0, -px) }
}

private val ShowMoreOverhang = 12.dp

/**
 * The card's bottom row. Downloading: the progress bar, "Downloading · 280
 * of 682 MB" and the percent, and a text "Cancel". Paused, failed or damaged:
 * what happened ([EngineStatus]) above the buttons. Otherwise the buttons of
 * [ui], with "Use" or "✓ In use" at the end for an installed engine.
 */
@Composable
private fun EngineCardActions(
    engine: SttEngineDescriptor,
    state: InstallState,
    ui: EngineCardUi,
    onUse: () -> Unit,
    onAction: (EngineAction) -> Unit,
) {
    val resumeFocus = rememberResumeFocus(state)
    if (state is InstallState.Downloading) {
        Spacer(Modifier.height(10.dp))
        EngineProgressBar(done = state.bytesDone, total = state.totalBytes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = shortStatusText(state, engine.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // The bar already tells TalkBack the percent.
            Text(
                text = stringResource(R.string.engine_progress_percent, progressPercent(state.bytesDone, state.totalBytes)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            val cancel = stringResource(R.string.engine_action_cancel)
            EngineTextButton(
                label = cancel,
                onClick = {
                    resumeFocus.cancelled()
                    onAction(EngineAction.CANCEL)
                },
                spokenLabel = namedActionLabel(cancel, engine.displayName),
            )
        }
        return
    }
    if (state is InstallState.Paused || state is InstallState.Failed || state is InstallState.Corrupt) {
        Spacer(Modifier.height(12.dp))
        EngineStatus(state = state)
        Spacer(Modifier.height(8.dp))
    } else {
        Spacer(Modifier.height(12.dp))
    }
    EngineActionBar(
        actions = ui.actions,
        onAction = onAction,
        downloadSize = formatBytes(engine.sizeBytes),
        engineName = engine.displayName,
        resumeFocus = resumeFocus,
    ) {
        when (ui.selection) {
            EngineSelection.SELECTED -> InUsePill()
            EngineSelection.SELECTABLE -> {
                TonalEngineButton(
                    label = stringResource(R.string.engine_action_use),
                    onClick = onUse,
                    spokenLabel = stringResource(R.string.engine_action_use_named, engine.displayName),
                )
            }
            EngineSelection.NONE -> Unit
        }
    }
}
