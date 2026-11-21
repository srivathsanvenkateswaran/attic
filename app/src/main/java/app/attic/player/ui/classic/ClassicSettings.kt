package app.attic.player.ui.classic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.*
import app.attic.player.ui.model.SettingsActions
import app.attic.player.ui.model.SettingsState

@Composable
fun ClassicSettings(state: SettingsState, actions: SettingsActions) {
    val mode = state.themeMode
    val autoplay = state.autoplay
    val bulk = state.bulkLyrics
    val analysis = state.analysis

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomPadding.current),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(actions.back)
            Spacer(Modifier.width(4.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        SectionTitle("Appearance")
        ThemeMode.entries.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(scale = 0.985f) { actions.setThemeMode(option) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                RadioButton(selected = option == mode, onClick = { actions.setThemeMode(option) })
            }
        }
        // The style picker, until each style draws its own Settings.
        Text(
            stringResource(R.string.settings_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        UiStyle.choices.forEach { option ->
            val label = stringResource(if (option == UiStyle.Comfort) R.string.settings_style_comfort else R.string.settings_style_expressive)
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(scale = 0.985f) { actions.setUiStyle(option) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                RadioButton(selected = option == state.uiStyle, onClick = { actions.setUiStyle(option) })
            }
        }

        SectionTitle("Lyrics")
        Text(
            "${state.savedLyricsCount} of ${state.songCount} songs have lyrics saved on this phone. Saved lyrics work offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are never sent.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (bulk.running) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (bulk.total == 0) 0f else bulk.done.toFloat() / bulk.total },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Text(
                "Checked ${bulk.done} of ${bulk.total}, found ${bulk.found}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        } else {
            if (bulk.failed > 0) {
                Text(
                    if (bulk.gaveUp) {
                        "Stopped because the connection failed. Check your internet, then download again to continue."
                    } else {
                        "${songCount(bulk.failed)} couldn't be checked. Download again to retry them."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }
            TextButton(
                onClick = actions.downloadAllLyrics,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text("Download lyrics for all songs")
            }
        }

        SectionTitle("Song radio")
        Row(
            Modifier
                .fillMaxWidth()
                .pressable(scale = 0.985f) { actions.setAutoplay(!autoplay) }
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Autoplay", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "When a queue reaches its last song, radio keeps going with similar songs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = autoplay, onCheckedChange = actions.setAutoplay)
        }
        Text(
            "Radio finds similar songs by listening to your files. This happens on this phone, and nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { analysis.fraction },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Text(
            analysis.status,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
        )
        if (analysis.canStart) {
            TextButton(
                onClick = actions.startAnalysis,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text("Analyse now without a charger")
            }
        }

        SectionTitle("About")
        Text(
            "Attic plays the music in Music/Offline. It has no account and no streaming. ${songCount(state.songCount)} on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
