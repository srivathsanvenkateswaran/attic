package app.attic.player.ui.expressive

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import app.attic.player.R
import app.attic.player.ui.LocalIsDark
import app.attic.player.ui.model.WrappedStoryActions
import app.attic.player.ui.model.WrappedStoryState
import kotlinx.coroutines.flow.Flow

/**
 * Recap's story in the Expressive style (pixel/09-recap.html, frames D1 to D4): Now Playing's layout
 * with the card where the cover is and the next card as a sliver beside it, one wavy line for the
 * whole sequence with a stop at each card, and Save and Share under it. The screen takes the scheme
 * of the card it shows, in the app's light or dark; the card keeps its own. Holding the card pauses
 * the line and steps the card back. [messages] are the app's messages, shown here under the buttons
 * because the story covers the shell's snackbar.
 */
@Composable
fun ExpressiveRecapStory(state: WrappedStoryState, actions: WrappedStoryActions, messages: Flow<String>) {
    val cards = state.cards
    val page = state.page
    val card = cards[page]
    val layer = rememberGraphicsLayer()
    val current by rememberUpdatedState(actions)
    val capture: suspend () -> Bitmap = { layer.toImageBitmap().asAndroidBitmap() }
    var pressed by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    Dialog(onDismissRequest = actions.close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = actions.close)
        val dark = LocalIsDark.current
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        FromArt(rememberRecapCardSeed(card, state.data)) {
            val colors = MaterialTheme.colorScheme
            Box(Modifier.fillMaxSize().background(if (dark) colors.surfaceContainerLowest else colors.surfaceContainer)) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    TopBar(state, actions.close)
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
                        // The card as large as fits beside a 40dp sliver, 20dp in from each side.
                        val cardWidth = min(maxWidth - 40.dp - 8.dp - 40.dp, maxHeight * 9f / 16f)
                        val cardHeight = cardWidth * 16f / 9f
                        val radius by animateDpAsState(if (pressed) 40.dp else 28.dp * (cardWidth / 360.dp), ExpressiveMotion.defaultSpatial(), label = "storyCorner")
                        val scale by animateFloatAsState(if (pressed) 0.93f else 1f, ExpressiveMotion.defaultSpatial(), label = "storyScale")
                        Row(
                            Modifier.align(Alignment.CenterStart).padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AnimatedContent(
                                targetState = page,
                                transitionSpec = {
                                    val forward = targetState > initialState
                                    (slideInHorizontally(ExpressiveMotion.defaultSpatial()) { if (forward) it else -it } + fadeIn(ExpressiveMotion.defaultEffects()))
                                        .togetherWith(slideOutHorizontally(ExpressiveMotion.defaultSpatial()) { if (forward) -it else it } + fadeOut(ExpressiveMotion.fastEffects()))
                                },
                                label = "storyPage",
                            ) { shown ->
                                ExpressiveRecapCard(
                                    cards[shown],
                                    state.data,
                                    Modifier
                                        .width(cardWidth)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clip(RoundedCornerShape(radius))
                                        .pointerInput(cards.size) {
                                            detectTapGestures(
                                                onPress = {
                                                    pressed = true
                                                    current.setHeld(true)
                                                    tryAwaitRelease()
                                                    pressed = false
                                                    current.setHeld(false)
                                                },
                                                onTap = { offset -> current.tap(offset.x < size.width / 3) },
                                            )
                                        },
                                    layer = if (shown == page) layer else null,
                                    animate = true,
                                )
                            }
                            val next = cards.getOrNull(page + 1)
                            Box(Modifier.size(40.dp, if (pressed) cardHeight * 0.93f else cardHeight).clip(RoundedCornerShape(20.dp))) {
                                if (next != null) {
                                    ExpressiveRecapCard(
                                        next,
                                        state.data,
                                        Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredWidth(cardWidth),
                                    )
                                }
                            }
                        }
                    }
                    val count = cards.size
                    StoryProgress(
                        progress = { ((page + state.progress()) / count).coerceIn(0f, 1f) },
                        stops = (1 until count).map { it / count.toFloat() },
                        running = !pressed,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveButton(
                            stringResource(R.string.story_save),
                            { current.save(capture) },
                            variant = ButtonVariant.Tonal,
                            size = ButtonSize.Medium,
                            leading = Symbol.Download,
                            leadingFilled = false,
                        )
                        ExpressiveButton(
                            stringResource(R.string.story_share),
                            { current.share(capture) },
                            Modifier.weight(1f),
                            size = ButtonSize.Medium,
                            leading = Symbol.Share,
                            leadingFilled = false,
                        )
                    }
                    // Room under the buttons for a message, so it never covers them.
                    Spacer(Modifier.height(72.dp))
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp)) { data ->
                    ExpressiveSnackbar(data)
                }
            }
        }
    }
}

/** Close, "Recap" over the period, and "5 of 10" (`.np-top` with `.st-count`). */
@Composable
private fun TopBar(state: WrappedStoryState, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpressiveIconButton(Symbol.Close, stringResource(R.string.story_close), onClose)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.recap_title), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(
                recapPeriodLabel(state.data),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            stringResource(R.string.story_position, state.page + 1, state.cards.size),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp),
        )
    }
}
