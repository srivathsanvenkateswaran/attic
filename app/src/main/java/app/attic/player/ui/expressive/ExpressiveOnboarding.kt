package app.attic.player.ui.expressive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.data.OnboardingStep
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.LookPreview
import app.attic.player.ui.model.AccessState
import app.attic.player.ui.model.LibraryScan
import app.attic.player.ui.model.OnboardingActions
import app.attic.player.ui.model.OnboardingState
import java.text.NumberFormat

/**
 * The first run, laid out as the permission screen was (sheet 08 C): a bar with back and the step
 * pips, the start-aligned emphasised headline, the step's content on the surface, and the one filled
 * button at the bottom edge. Steps slide along the X axis, as detail screens do.
 */
@Composable
fun ExpressiveOnboarding(state: OnboardingState, actions: OnboardingActions) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.step.previous() != null) {
                ExpressiveIconButton(Symbol.ArrowBack, stringResource(R.string.back), actions.back)
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            StepPips(state.position, OnboardingStep.count, Modifier.padding(end = 16.dp))
        }
        AnimatedContent(
            state.step,
            Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(ExpressiveMotion.defaultSpatial()) { if (forward) it / 10 else -it / 10 } + fadeIn(ExpressiveMotion.defaultEffects()))
                    .togetherWith(fadeOut(ExpressiveMotion.fastEffects()))
            },
            label = "onboardingStep",
        ) { step ->
            // No side padding here: text and pictures pad themselves 16dp (24 for text), and groups bring their own margin.
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                when (step) {
                    OnboardingStep.Welcome -> WelcomeStep()
                    OnboardingStep.Look -> LookStep(state, actions)
                    OnboardingStep.Access -> AccessStep(state, actions)
                    OnboardingStep.Library -> LibraryStep(state, actions)
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp)) { BottomButton(state, actions) }
    }
}

@Composable
private fun BottomButton(state: OnboardingState, actions: OnboardingActions) {
    val modifier = Modifier.fillMaxWidth()
    when {
        state.step == OnboardingStep.Welcome ->
            ExpressiveButton(stringResource(R.string.onboarding_get_started), actions.next, modifier, size = ButtonSize.Medium)
        state.step == OnboardingStep.Access && state.audio == AccessState.NotAsked ->
            ExpressiveButton(stringResource(R.string.permission_allow), actions.allowAudio, modifier, size = ButtonSize.Medium)
        state.step == OnboardingStep.Access && state.audio == AccessState.Refused ->
            ExpressiveButton(
                stringResource(R.string.permission_open_settings),
                actions.openAppPermissions,
                modifier,
                size = ButtonSize.Medium,
                trailing = Symbol.OpenInNew,
            )
        state.step == OnboardingStep.Library ->
            ExpressiveButton(stringResource(R.string.onboarding_start), actions.finish, modifier, size = ButtonSize.Medium)
        else ->
            ExpressiveButton(stringResource(R.string.onboarding_continue), actions.next, modifier, size = ButtonSize.Medium)
    }
}

/** Where the first run is: one pip per step, the current one a wider primary pill. */
@Composable
private fun StepPips(position: Int, count: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.onboarding_step, position, count)
    Row(modifier.semantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (index in 1..count) {
            val current = index == position
            val width by animateDpAsState(if (current) 24.dp else 8.dp, ExpressiveMotion.fastSpatial(), label = "pipWidth")
            val color by animateColorAsState(
                if (index <= position) colors.primary else colors.surfaceContainerHighest,
                ExpressiveMotion.defaultEffects(),
                label = "pipColor",
            )
            Box(Modifier.size(width, 8.dp).clip(ExpressiveShapes.Pill).background(color))
        }
    }
}

@Composable
private fun Headline(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Expressive.type.headlineLargeEmphasized.copy(letterSpacing = (-0.3).sp),
        modifier = modifier.padding(horizontal = 24.dp).semantics { heading() },
    )
}

@Composable
private fun Body(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp),
    )
}

@Composable
private fun ColumnScope.WelcomeStep() {
    WelcomeArt()
    Headline(stringResource(R.string.onboarding_welcome_title), Modifier.padding(top = 32.dp))
    Body(stringResource(R.string.onboarding_welcome_body))
}

/**
 * The welcome picture, from the permission screen's family of shapes: the arch holding the mark, a
 * clover with a folder for your own files, and a pill with the aeroplane for playing offline.
 */
@Composable
private fun WelcomeArt() {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(284.dp, 216.dp)) {
            Box(
                Modifier
                    .offset(y = 16.dp)
                    .size(164.dp, 200.dp)
                    .clip(RoundedCornerShape(topStart = 82.dp, topEnd = 82.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                AtticMarkIcon(Modifier.padding(top = 14.dp), size = 72.dp, tint = colors.onPrimaryContainer)
            }
            Box(
                Modifier.offset(x = 176.dp).size(108.dp).clip(CloverShape).background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(Symbol.Folder, size = 36.dp, tint = colors.onSecondaryContainer)
            }
            Box(
                Modifier
                    .offset(x = 176.dp, y = 136.dp)
                    .size(108.dp, 80.dp)
                    .clip(ExpressiveShapes.Pill)
                    .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(Symbol.AirplanemodeActive, size = 36.dp, tint = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ColumnScope.LookStep(state: OnboardingState, actions: OnboardingActions) {
    Headline(stringResource(R.string.onboarding_look_title), Modifier.padding(top = 8.dp))
    Body(stringResource(R.string.onboarding_look_body))
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UiStyle.choices.forEach { style ->
            LookCard(style, state.themeMode, selected = state.uiStyle == style, onSelect = { actions.setUiStyle(style) }, Modifier.weight(1f))
        }
    }
    SettingsSection(stringResource(R.string.onboarding_theme))
    ThemeChoice(state.themeMode, actions.setThemeMode)
}

@Composable
private fun LookCard(style: UiStyle, mode: ThemeMode, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.surfaceContainer,
        ExpressiveMotion.defaultEffects(),
        label = "lookCard",
    )
    val edge by animateColorAsState(if (selected) colors.primary else Color.Transparent, ExpressiveMotion.defaultEffects(), label = "lookEdge")
    val shape = RoundedCornerShape(ExpressiveShapes.ExtraLarge)
    Column(
        modifier
            .clip(shape)
            .background(container)
            .border(2.dp, edge, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(8.dp),
    ) {
        LookPreview(style, mode, Modifier.fillMaxWidth().clip(RoundedCornerShape(ExpressiveShapes.LargeIncreased)), scale = 0.42f)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpressiveRadio(selected, onClick = null)
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(if (style == UiStyle.Comfort) R.string.settings_style_comfort else R.string.settings_style_expressive),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) colors.onSecondaryContainer else colors.onSurface,
            )
        }
    }
}

/** The three themes as Settings draws them: one connected group, the same labels in the same order. */
@Composable
private fun ThemeChoice(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val modes = listOf(ThemeMode.System, ThemeMode.Dark, ThemeMode.Light)
    ConnectedButtonGroup(
        listOf(
            stringResource(R.string.settings_theme_system),
            stringResource(R.string.settings_theme_dark),
            stringResource(R.string.settings_theme_light),
        ),
        modes.indexOf(mode),
        { onSelect(modes[it]) },
    )
}

/** The mini Home this style draws for the look card: the title, Home's filters and the navigation bar. */
@Composable
fun ExpressiveLookPreview() {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AtticMarkIcon(size = 32.dp, tint = colors.primary)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.tab_home), style = Expressive.type.headlineMediumEmphasized, color = colors.onSurface)
        }
        ConnectedButtonGroup(
            listOf(stringResource(R.string.home_filter_all), stringResource(R.string.home_filter_albums), stringResource(R.string.home_filter_artists)),
            0,
            { },
        )
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { Cover(null, Modifier.weight(1f)) }
        }
        ExpressiveNavigationBar(
            items = listOf(
                NavItem(stringResource(R.string.tab_home), Symbol.Home),
                NavItem(stringResource(R.string.tab_search), Symbol.Search),
                NavItem(stringResource(R.string.tab_library), Symbol.LibraryMusic),
            ),
            selectedIndex = 0,
            onSelect = { },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
    }
}

@Composable
private fun ColumnScope.AccessStep(state: OnboardingState, actions: OnboardingActions) {
    Headline(stringResource(R.string.onboarding_access_title), Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(24.dp))
    ListGroup(2) { index ->
        if (index == 0) {
            AccessRow(
                Symbol.MusicNote,
                stringResource(R.string.onboarding_access_audio),
                stringResource(R.string.onboarding_access_audio_body),
                state.audio,
                onAllow = null,
            )
        } else {
            AccessRow(
                Symbol.Notifications,
                stringResource(R.string.onboarding_access_notifications),
                stringResource(R.string.onboarding_access_notifications_body),
                state.notifications,
                onAllow = actions.allowNotifications,
            )
        }
    }
    if (state.audio == AccessState.Refused) Body(stringResource(R.string.permission_body_refused), Modifier.padding(top = 10.dp))
}

/**
 * One permission as a settings row: the icon in a tonal circle, what it is and why, then where it
 * stands. [onAllow] puts an Allow button there while it has not been asked; audio has none, because
 * the screen's own button asks for it.
 */
@Composable
private fun AccessRow(symbol: Symbol, title: String, body: String, access: AccessState, onAllow: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
            SymbolIcon(symbol, size = 20.dp, tint = colors.onSecondaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        when {
            access == AccessState.Granted -> Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon(Symbol.Check, size = 18.dp, tint = colors.primary)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.onboarding_access_allowed), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            }
            access == AccessState.NotAsked && onAllow != null ->
                ExpressiveButton(stringResource(R.string.onboarding_access_allow), onAllow, variant = ButtonVariant.Tonal)
            access == AccessState.Refused ->
                Text(
                    stringResource(R.string.onboarding_access_off),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (onAllow == null) colors.error else colors.onSurfaceVariant,
                )
            else -> Unit
        }
    }
}

@Composable
private fun ColumnScope.LibraryStep(state: OnboardingState, actions: OnboardingActions) {
    val format = NumberFormat.getIntegerInstance()
    when (val scan = state.scan) {
        LibraryScan.Scanning -> Column(
            Modifier.fillMaxWidth().padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingIndicator(contained = true, size = 72.dp)
            Text(
                stringResource(R.string.onboarding_scanning),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp).semantics { heading() },
            )
        }
        is LibraryScan.Found -> if (scan.count == 0) {
            EmptyState(
                stringResource(R.string.home_empty_title),
                stringResource(R.string.home_empty_body),
                clover = true,
            )
            Text(
                stringResource(R.string.onboarding_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        } else {
            Headline(pluralStringResource(R.plurals.onboarding_found, scan.count, format.format(scan.count)), Modifier.padding(top = 8.dp))
            SettingsSection(stringResource(R.string.onboarding_optional), Modifier.padding(top = 8.dp))
            ListGroup(2) { index ->
                if (index == 0) {
                    SetSwitchRow(
                        stringResource(R.string.settings_lyrics_download_all),
                        state.downloadLyrics,
                        actions.setDownloadLyrics,
                        summary = stringResource(R.string.settings_lyrics_privacy),
                        leading = Symbol.Lyrics,
                    )
                } else {
                    SetNote(stringResource(R.string.onboarding_radio_note)) {
                        ExpressiveButton(
                            stringResource(R.string.onboarding_radio_settings),
                            actions.openRadioSettings,
                            Modifier.padding(top = 8.dp).offset(x = (-12).dp),
                            variant = ButtonVariant.Text,
                            leading = Symbol.GraphicEq,
                        )
                    }
                }
            }
        }
    }
}
