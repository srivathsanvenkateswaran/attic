package app.attic.player.ui.comfort

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.OnboardingStep
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.LookPreview
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.AccessState
import app.attic.player.ui.model.LibraryScan
import app.attic.player.ui.model.OnboardingActions
import app.attic.player.ui.model.OnboardingState
import java.text.NumberFormat

/**
 * The first run, laid out as the permission screen was (oneui/08-settings-states.html C): back and
 * the step pips at the top, the title centred in the upper part of the screen, settings cards for the
 * choices, and the one full-width button at the bottom edge. Steps slide in a little, as detail
 * screens do in the shell.
 */
@Composable
fun ComfortOnboarding(state: OnboardingState, actions: OnboardingActions) {
    val colors = Comfort.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.step.previous() != null) {
                ComfortIconButton(Symbol.ArrowBack, stringResource(R.string.back), actions.back)
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
                (slideInHorizontally(tween(300)) { if (forward) it / 8 else -it / 8 } + fadeIn(tween(220)))
                    .togetherWith(fadeOut(tween(120)))
            },
            label = "onboardingStep",
        ) { step ->
            // Welcome and the scan sit centred in the space; the rest start at the top. Either way the page scrolls.
            val centred = step == OnboardingStep.Welcome || (step == OnboardingStep.Library && state.scan == LibraryScan.Scanning)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(bottom = 24.dp),
                    verticalArrangement = if (centred) Arrangement.Center else Arrangement.Top,
                ) {
                    when (step) {
                        OnboardingStep.Welcome -> WelcomeStep()
                        OnboardingStep.Look -> LookStep(state, actions)
                        OnboardingStep.Access -> AccessStep(state, actions)
                        OnboardingStep.Library -> LibraryStep(state, actions)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)) { BottomButton(state, actions) }
    }
}

@Composable
private fun BottomButton(state: OnboardingState, actions: OnboardingActions) {
    val (label, onClick) = when {
        state.step == OnboardingStep.Welcome -> R.string.onboarding_get_started to actions.next
        state.step == OnboardingStep.Access && state.audio == AccessState.NotAsked -> R.string.permission_allow to actions.allowAudio
        state.step == OnboardingStep.Access && state.audio == AccessState.Refused -> R.string.permission_open_settings to actions.openAppPermissions
        state.step == OnboardingStep.Library -> R.string.onboarding_start to actions.finish
        else -> R.string.onboarding_continue to actions.next
    }
    ComfortButton(stringResource(label), onClick, size = ButtonSize.Large, block = true)
}

/** Where the first run is: one pip per step, the current one a wider amber capsule. */
@Composable
private fun StepPips(position: Int, count: Int, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    val description = stringResource(R.string.onboarding_step, position, count)
    Row(modifier.semantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (index in 1..count) {
            val width by animateDpAsState(if (index == position) 22.dp else 8.dp, tween(240), label = "pipWidth")
            val color by animateColorAsState(if (index <= position) colors.accent else colors.fill2, tween(240), label = "pipColor")
            Box(Modifier.size(width, 8.dp).clip(ComfortShapes.Pill).background(color))
        }
    }
}

/** The step's title, centred, in the permission screen's 30/36 bold. */
@Composable
private fun Title(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = onest(30f, 36f, 700, -0.022f),
        color = Comfort.colors.on,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp).semantics { heading() },
    )
}

@Composable
private fun Body(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = onest(16f, 23f),
        color = Comfort.colors.on2,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(start = 36.dp, end = 36.dp, top = 14.dp),
    )
}

@Composable
private fun ColumnScope.WelcomeStep() {
    MarkBox(Modifier.align(Alignment.CenterHorizontally), size = 112.dp)
    Title(stringResource(R.string.onboarding_welcome_title), Modifier.padding(top = 32.dp))
    Body(stringResource(R.string.onboarding_welcome_body))
    // Lifts the block above the middle, as the permission screen did.
    Spacer(Modifier.height(96.dp))
}

@Composable
private fun ColumnScope.LookStep(state: OnboardingState, actions: OnboardingActions) {
    Title(stringResource(R.string.onboarding_look_title), Modifier.padding(top = 16.dp))
    Body(stringResource(R.string.onboarding_look_body))
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UiStyle.choices.forEach { style ->
            LookCard(style, state.themeMode, selected = state.uiStyle == style, onSelect = { actions.setUiStyle(style) }, Modifier.weight(1f))
        }
    }
    Label(stringResource(R.string.onboarding_theme))
    Group {
        listOf(
            ThemeMode.System to R.string.settings_theme_system,
            ThemeMode.Dark to R.string.settings_theme_dark,
            ThemeMode.Light to R.string.settings_theme_light,
        ).forEach { (mode, label) ->
            item { SetRadioRow(stringResource(label), state.themeMode == mode, { actions.setThemeMode(mode) }) }
        }
    }
}

/** A look as a selectable card: the live picture, then the kit's radio and the name. Selected is ringed in amber. */
@Composable
private fun LookCard(style: UiStyle, mode: ThemeMode, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    val edge by animateColorAsState(if (selected) colors.accent else Color.Transparent, tween(160), label = "lookEdge")
    Column(
        modifier
            .clip(ComfortShapes.Group)
            .background(colors.card)
            .border(2.dp, edge, ComfortShapes.Group)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(8.dp),
    ) {
        LookPreview(style, mode, Modifier.fillMaxWidth().clip(ComfortShapes.Control), scale = 0.42f)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComfortRadio(selected, onClick = null)
            Text(
                stringResource(if (style == UiStyle.Comfort) R.string.settings_style_comfort else R.string.settings_style_expressive),
                style = Comfort.type.rowTitle,
                color = colors.on,
            )
        }
    }
}

/** The mini Home this style draws for the look card: the large title, Home's filters and the dock's tab bar. */
@Composable
fun ComfortLookPreview() {
    val colors = Comfort.colors
    Column(Modifier.fillMaxWidth().background(colors.ground)) {
        Text(
            stringResource(R.string.tab_home),
            style = Comfort.type.hero,
            color = colors.on,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
        )
        SubTabs(
            listOf(stringResource(R.string.home_filter_all), stringResource(R.string.home_filter_albums), stringResource(R.string.home_filter_artists)),
            0,
            { },
            Modifier.padding(horizontal = 16.dp),
        )
        Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) { Cover(null, Modifier.weight(1f)) }
        }
        Dock(insets = WindowInsets(0, 0, 0, 0)) {
            TabBar(
                items = listOf(
                    TabItem(stringResource(R.string.tab_home), Symbol.Home),
                    TabItem(stringResource(R.string.tab_search), Symbol.Search),
                    TabItem(stringResource(R.string.tab_library), Symbol.LibraryMusic),
                ),
                selectedIndex = 0,
                onSelect = { },
            )
        }
    }
}

@Composable
private fun ColumnScope.AccessStep(state: OnboardingState, actions: OnboardingActions) {
    Title(stringResource(R.string.onboarding_access_title), Modifier.padding(top = 16.dp))
    Spacer(Modifier.height(28.dp))
    Group {
        item {
            AccessRow(
                Symbol.MusicNote,
                stringResource(R.string.onboarding_access_audio),
                stringResource(R.string.onboarding_access_audio_body),
                state.audio,
                onAllow = null,
            )
        }
        item {
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
 * One permission as a settings row: an icon box, what it is and why, then where it stands. [onAllow]
 * puts an Allow button there while it has not been asked; audio has none, because the screen's own
 * button asks for it.
 */
@Composable
private fun AccessRow(symbol: Symbol, title: String, body: String, access: AccessState, onAllow: (() -> Unit)?) {
    val colors = Comfort.colors
    SetRow(
        title,
        summary = body,
        leading = { IconBox(symbol, size = 40.dp) },
        trailing = {
            when {
                access == AccessState.Granted -> Row(verticalAlignment = Alignment.CenterVertically) {
                    ComfortIcon(Symbol.Check, size = 20.dp, tint = colors.accentInk)
                    Text(
                        stringResource(R.string.onboarding_access_allowed),
                        style = onest(14f, 18f, 600),
                        color = colors.accentInk,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                access == AccessState.NotAsked && onAllow != null ->
                    ComfortButton(stringResource(R.string.onboarding_access_allow), onAllow, style = ButtonStyle.Tonal, size = ButtonSize.Small)
                access == AccessState.Refused ->
                    Text(
                        stringResource(R.string.onboarding_access_off),
                        style = onest(14f, 18f, 600),
                        color = if (onAllow == null) colors.danger else colors.on2,
                    )
                else -> Unit
            }
        },
    )
}

@Composable
private fun ColumnScope.LibraryStep(state: OnboardingState, actions: OnboardingActions) {
    val format = NumberFormat.getIntegerInstance()
    when (val scan = state.scan) {
        LibraryScan.Scanning -> {
            Spinner(Modifier.align(Alignment.CenterHorizontally).size(44.dp))
            Text(
                stringResource(R.string.onboarding_scanning),
                style = Comfort.type.barTitle,
                color = Comfort.colors.on,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).semantics { heading() },
            )
            Spacer(Modifier.height(96.dp))
        }
        is LibraryScan.Found -> if (scan.count == 0) {
            EmptyState(stringResource(R.string.home_empty_title), body = stringResource(R.string.home_empty_body))
            Note(stringResource(R.string.onboarding_empty_hint))
        } else {
            Title(pluralStringResource(R.plurals.onboarding_found, scan.count, format.format(scan.count)), Modifier.padding(top = 16.dp))
            Label(stringResource(R.string.onboarding_optional), Modifier.padding(top = 12.dp))
            Group {
                item {
                    SetSwitchRow(
                        stringResource(R.string.settings_lyrics_download_all),
                        state.downloadLyrics,
                        actions.setDownloadLyrics,
                        summary = stringResource(R.string.settings_lyrics_privacy),
                    )
                }
                item {
                    SetBlock {
                        SetBlockText(stringResource(R.string.onboarding_radio_note))
                        ComfortButton(
                            stringResource(R.string.onboarding_radio_settings),
                            actions.openRadioSettings,
                            Modifier.padding(top = 12.dp).offset(x = (-12).dp),
                            style = ButtonStyle.Text,
                        )
                    }
                }
            }
        }
    }
}
