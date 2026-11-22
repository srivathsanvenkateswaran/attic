package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.model.RootActions
import app.attic.player.ui.model.RootState

/**
 * The permission screen (sheet 08 C and G), laid out like a setup screen: a picture made of Material
 * shapes (an arch holding the mark, a clover with a note for audio, a pill with a bell for
 * notifications), the start-aligned headline, one sentence, and the one button at the bottom edge.
 * Once the prompt has been declined the clover goes grey and its note is crossed out in error.
 */
@Composable
fun ExpressivePermission(state: RootState.NeedsAccess, actions: RootActions) {
    val colors = MaterialTheme.colorScheme
    val refused = state.askedBefore
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            GateArt(refused)
            Text(
                stringResource(R.string.permission_title),
                style = Expressive.type.headlineLargeEmphasized.copy(letterSpacing = (-0.3).sp),
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 32.dp),
            )
            Text(
                stringResource(if (refused) R.string.permission_body_refused else R.string.permission_body),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 14.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
        if (refused) {
            ExpressiveButton(
                stringResource(R.string.permission_open_settings),
                actions.openAppPermissions,
                Modifier.fillMaxWidth(),
                size = ButtonSize.Medium,
                trailing = Symbol.OpenInNew,
            )
        } else {
            ExpressiveButton(
                stringResource(R.string.permission_allow),
                actions.allowAccess,
                Modifier.fillMaxWidth(),
                size = ButtonSize.Medium,
            )
        }
    }
}

/** The 320dp picture: three shapes on a surface container card, placed as the sheet places them. */
@Composable
private fun GateArt(refused: Boolean) {
    val colors = MaterialTheme.colorScheme
    val clover by animateColorAsState(
        if (refused) colors.surfaceContainerHighest else colors.secondaryContainer,
        ExpressiveMotion.defaultEffects(),
        label = "gateClover",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        // The sheet draws the three shapes 284dp across, inset 40dp on a 358dp card; centred here.
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
                Modifier
                    .offset(x = 176.dp)
                    .size(108.dp)
                    .clip(CloverShape)
                    .background(clover),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(refused, animationSpec = ExpressiveMotion.defaultEffects(), label = "gateNote") { off ->
                    if (off) {
                        SymbolIcon(Symbol.MusicOff, size = 36.dp, tint = colors.error)
                    } else {
                        SymbolIcon(Symbol.MusicNote, size = 36.dp, tint = colors.onSecondaryContainer)
                    }
                }
            }
            Box(
                Modifier
                    .offset(x = 176.dp, y = 136.dp)
                    .size(108.dp, 80.dp)
                    .clip(ExpressiveShapes.Pill)
                    .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(Symbol.Notifications, size = 36.dp, tint = colors.onSurfaceVariant)
            }
        }
    }
}

/** The first library scan (sheet 08 D1): the loading indicator on the surface colour and nothing else. */
@Composable
fun ExpressiveLoading() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}
