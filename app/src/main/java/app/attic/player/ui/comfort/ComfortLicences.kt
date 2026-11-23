package app.attic.player.ui.comfort

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.LicenceState
import app.attic.player.ui.model.LicencesActions
import app.attic.player.ui.model.OpenSourceNotice

/** The licences list, laid out like Settings: the hero, the back arrow, then one card of rows with chevrons. */
@Composable
fun ComfortLicences(notices: List<OpenSourceNotice>, actions: LicencesActions) {
    val title = stringResource(R.string.settings_licences)
    RootPage(title = title, listState = rememberLazyListState(), onBack = actions.back) {
        item(key = "hero") { Hero(title) }
        item(key = "tools") {
            LeadTools { ComfortIconButton(Symbol.ArrowBack, stringResource(R.string.back), actions.back) }
        }
        groupItems(notices, key = { it.id }) { _, notice ->
            SetRow(
                notice.name,
                summary = notice.licence,
                onClick = { actions.open(notice) },
                trailing = { ComfortIcon(Symbol.ChevronRight, tint = Comfort.colors.on3) },
            )
        }
    }
}

/** One entry's notice and licence, as paragraphs under a short hero. */
@Composable
fun ComfortLicence(state: LicenceState, back: () -> Unit) {
    val colors = Comfort.colors
    RootPage(title = state.notice.name, listState = rememberLazyListState(), onBack = back) {
        item(key = "hero") { Hero(state.notice.name, short = true) }
        item(key = "tools") {
            LeadTools { ComfortIconButton(Symbol.ArrowBack, stringResource(R.string.back), back) }
        }
        val paragraphs = state.paragraphs
        if (paragraphs == null) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Spinner() }
            }
        } else {
            items(paragraphs) { paragraph ->
                Text(paragraph, style = onest(14f, 20f), color = colors.on2, modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp))
            }
        }
    }
}
