package app.attic.player.ui.expressive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.model.LicenceState
import app.attic.player.ui.model.LicencesActions
import app.attic.player.ui.model.OpenSourceNotice

/** The licences list, laid out like Settings: the large title that collapses into the bar, then one group of rows. */
@Composable
fun ExpressiveLicences(notices: List<OpenSourceNotice>, actions: LicencesActions) {
    val listState = rememberLazyListState()
    val title = stringResource(R.string.settings_licences)
    LicencePage(title, listState, actions.back) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = LocalBottomPadding.current + 16.dp)) {
            item(key = "title") { LargeTitle(title) }
            groupItems(notices, key = { it.id }) { _, notice ->
                SetRow(
                    notice.name,
                    Modifier.clickable(role = Role.Button) { actions.open(notice) },
                    summary = notice.licence,
                )
            }
        }
    }
}

/** One entry's notice and licence, as paragraphs under the large title. */
@Composable
fun ExpressiveLicence(state: LicenceState, back: () -> Unit) {
    val listState = rememberLazyListState()
    LicencePage(state.notice.name, listState, back) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = LocalBottomPadding.current + 16.dp)) {
            item(key = "title") { LargeTitle(state.notice.name) }
            val paragraphs = state.paragraphs
            if (paragraphs == null) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                }
            } else {
                items(paragraphs) { paragraph ->
                    Text(
                        paragraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** The bar shows [title] once the large one has scrolled away, as in Settings. */
@Composable
private fun LicencePage(title: String, listState: LazyListState, onBack: () -> Unit, content: @Composable () -> Unit) {
    val titleGone = with(LocalDensity.current) { 44.dp.toPx() }
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleGone }
    }
    Column(Modifier.fillMaxSize()) {
        DetailAppBar(
            title = if (collapsed) title else "",
            onBack = onBack,
            surface = if (collapsed) AppBarSurface.Scrolled else AppBarSurface.Plain,
        )
        content()
    }
}

@Composable
private fun LargeTitle(text: String) {
    Text(
        text,
        style = Expressive.type.displaySmallEmphasized.copy(letterSpacing = (-0.4).sp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    )
}
