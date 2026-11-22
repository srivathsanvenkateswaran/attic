package app.attic.player.ui.comfort

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.attic.player.R
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.recapHeadline
import app.attic.player.ui.model.WrappedStoryActions
import app.attic.player.ui.model.WrappedStoryState
import kotlinx.coroutines.flow.Flow

/**
 * Recap's story in the Comfort style (oneui/09-recap.html, frames D and D2). Over everything, dock
 * included: a header with Close and "Recap" over the headline, the card as the viewing area, and one
 * deck under it holding a single thick progress bar with "4 of 10" beside it, then Save and Share.
 * The story is dark, as the cards are; on a cover-washed card the screen takes the wash too, and the
 * bar and Share follow. Tap the left third of the card to go back, anywhere else to go on, hold to
 * pause. [messages] are the app's messages, shown here as the toast above the deck because the story
 * covers the shell's.
 */
@Composable
fun ComfortRecapStory(state: WrappedStoryState, actions: WrappedStoryActions, messages: Flow<String>) {
    val cards = state.cards
    val page = state.page
    val card = cards[page]
    val layer = rememberGraphicsLayer()
    val current by rememberUpdatedState(actions)
    val capture: suspend () -> Bitmap = { layer.toImageBitmap().asAndroidBitmap() }
    val toasts = remember { SnackbarHostState() }
    LaunchedEffect(messages) {
        messages.collect { message ->
            toasts.currentSnackbarData?.dismiss()
            toasts.showSnackbar(message)
        }
    }

    Dialog(onDismissRequest = actions.close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        SystemBarsFor(dark = true)
        RecapCardColors(card, state.data) {
            val colors = Comfort.colors
            Box(Modifier.fillMaxSize().background(colors.ground)) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    StoryHead(state, actions.close)
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val cardWidth = min(maxWidth - 58.dp, maxHeight * 9f / 16f)
                        val radius = 26.dp * (cardWidth / 360.dp)
                        Crossfade(page, animationSpec = tween(220), label = "storyCard") { shown ->
                            ComfortRecapCard(
                                cards[shown],
                                state.data,
                                Modifier
                                    .width(cardWidth)
                                    .clip(RoundedCornerShape(radius))
                                    .pointerInput(cards.size) {
                                        detectTapGestures(
                                            onPress = {
                                                current.setHeld(true)
                                                tryAwaitRelease()
                                                current.setHeld(false)
                                            },
                                            onTap = { offset -> current.tap(offset.x < size.width / 3) },
                                        )
                                    },
                                layer = if (shown == page) layer else null,
                                animate = true,
                            )
                        }
                    }
                    StoryDeck(state, onSave = { current.save(capture) }, onShare = { current.share(capture) })
                }
                val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ToastHost(toasts, Modifier.align(Alignment.BottomCenter).padding(bottom = navigation + 148.dp))
            }
        }
    }
}

/** `.np-head`: Close, then "Recap" as the overline over the period's headline. */
@Composable
private fun StoryHead(state: WrappedStoryState, onClose: () -> Unit) {
    val colors = Comfort.colors
    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ComfortIconButton(Symbol.Close, stringResource(R.string.story_close), onClose)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(stringResource(R.string.recap_title), style = Comfort.type.caption, color = colors.on2, maxLines = 1)
            Text(recapHeadline(state.data), style = onest(15.5f, 20f, 600), color = colors.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * `.st-deck`: one card under the story card. A single thick bar, filling steadily through each card's
 * seven seconds across the whole story, with "4 of 10" beside it; then Save (plain) and Share (filled).
 */
@Composable
private fun StoryDeck(state: WrappedStoryState, onSave: () -> Unit, onShare: () -> Unit) {
    val colors = Comfort.colors
    val count = state.cards.size
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clip(ComfortShapes.Floating)
            .background(colors.card)
            .padding(14.dp),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ComfortProgress(progress = { ((state.page + state.progress()) / count).coerceIn(0f, 1f) }, Modifier.weight(1f))
            Text(
                stringResource(R.string.story_position, state.page + 1, count),
                style = onest(13f, 18f, 600).tabular(),
                color = colors.on2,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ComfortButton(stringResource(R.string.story_save), onSave, Modifier.weight(1f), style = ButtonStyle.Plain, leading = Symbol.Download)
            ComfortButton(stringResource(R.string.story_share), onShare, Modifier.weight(1f), leading = Symbol.IosShare)
        }
    }
}
