package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.model.RootActions
import app.attic.player.ui.model.RootState
import androidx.compose.foundation.layout.Arrangement

/**
 * The permission screen (oneui/08-settings-states.html C and F): the mark in a tonal superellipse,
 * the title and one sentence centred above the middle, and the one button full width at the bottom
 * edge. Once the prompt has been declined the sentence and the button change to the app's
 * permissions page.
 */
@Composable
fun ComfortPermission(state: RootState.NeedsAccess, actions: RootActions) {
    val colors = Comfort.colors
    val refused = state.askedBefore
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MarkBox(size = 96.dp)
            Text(
                stringResource(R.string.permission_title),
                style = onest(30f, 36f, 700, -0.022f),
                color = colors.on,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 30.dp).semantics { heading() },
            )
            Text(
                stringResource(if (refused) R.string.permission_body_refused else R.string.permission_body),
                style = onest(16f, 23f),
                color = colors.on2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        ComfortButton(
            stringResource(if (refused) R.string.permission_open_settings else R.string.permission_allow),
            if (refused) actions.openAppPermissions else actions.allowAccess,
            size = ButtonSize.Large,
            block = true,
        )
    }
}

/** The first library scan (sheet 08 D1): a spinner in the accent on the bare ground, nothing else. */
@Composable
fun ComfortLoading() {
    Box(Modifier.fillMaxSize().background(Comfort.colors.ground), contentAlignment = Alignment.Center) {
        Spinner(Modifier.size(44.dp))
    }
}
