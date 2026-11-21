package app.attic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicSurface
import app.attic.player.ui.classic.ClassicWrapped
import app.attic.player.ui.classic.ClassicWrappedCard
import app.attic.player.ui.classic.ClassicWrappedStory
import app.attic.player.ui.model.WrappedCard
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.model.rememberWrappedModel
import app.attic.player.ui.model.rememberWrappedStoryModel

@Composable
fun WrappedScreen(library: Library) {
    val (state, actions) = rememberWrappedModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ClassicSurface { ClassicWrapped(state, actions) }
        UiStyle.Comfort -> ClassicSurface { ClassicWrapped(state, actions) }
        UiStyle.Classic -> ClassicWrapped(state, actions)
    }

    // Outside the page's drawing, so the card and pause survive a style switch.
    val loaded = state.data
    val start = state.storyAt
    if (loaded != null && start != null && !loaded.recap.isEmpty) {
        WrappedStory(loaded, WrappedCard.available(loaded), start, onClose = actions.closeStory)
    }
}

@Composable
private fun WrappedStory(data: WrappedData, cards: List<WrappedCard>, start: Int, onClose: () -> Unit) {
    val (state, actions) = rememberWrappedStoryModel(data, cards, start, onClose)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ClassicSurface { ClassicWrappedStory(state, actions) }
        UiStyle.Comfort -> ClassicSurface { ClassicWrappedStory(state, actions) }
        UiStyle.Classic -> ClassicWrappedStory(state, actions)
    }
}

/**
 * One Wrapped card, laid out on a fixed 360 by 640 canvas and scaled to fit, so the story, the
 * thumbnails and the shared image all match. When [layer] is set, the card also draws into it, and
 * that is what Save and Share write out, so a shared image looks like the style that drew it.
 */
@Composable
fun WrappedCardView(card: WrappedCard, data: WrappedData, modifier: Modifier = Modifier, layer: GraphicsLayer? = null, animate: Boolean = false) {
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ClassicSurface { ClassicWrappedCard(card, data, modifier, layer, animate) }
        UiStyle.Comfort -> ClassicSurface { ClassicWrappedCard(card, data, modifier, layer, animate) }
        UiStyle.Classic -> ClassicWrappedCard(card, data, modifier, layer, animate)
    }
}
