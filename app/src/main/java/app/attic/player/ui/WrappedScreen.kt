package app.attic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import app.attic.player.attic
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortRecap
import app.attic.player.ui.comfort.ComfortRecapCard
import app.attic.player.ui.comfort.ComfortRecapStory
import app.attic.player.ui.expressive.ExpressiveRecap
import app.attic.player.ui.expressive.ExpressiveRecapCard
import app.attic.player.ui.expressive.ExpressiveRecapStory
import app.attic.player.ui.model.WrappedCard
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.model.rememberWrappedModel
import app.attic.player.ui.model.rememberWrappedStoryModel

@Composable
fun WrappedScreen(library: Library) {
    val (state, actions) = rememberWrappedModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveRecap(state, actions)
        UiStyle.Comfort -> ComfortRecap(state, actions)
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
        // The story covers the shell, so it shows the app's messages ("Saved to Pictures/Attic") itself.
        UiStyle.Expressive -> ExpressiveRecapStory(state, actions, LocalContext.current.attic.messages)
        UiStyle.Comfort -> ComfortRecapStory(state, actions, LocalContext.current.attic.messages)
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
        UiStyle.Expressive -> ExpressiveRecapCard(card, data, modifier, layer, animate)
        UiStyle.Comfort -> ComfortRecapCard(card, data, modifier, layer, animate)
    }
}
