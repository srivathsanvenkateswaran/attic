package app.attic.player.ui

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Library
import app.attic.player.data.ThemeMode

@Composable
fun SettingsScreen(library: Library) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val mode by app.settings.themeMode.collectAsStateWithLifecycle()
    val bulk by app.lyrics.bulk.collectAsStateWithLifecycle()
    val saved by app.lyrics.savedKeys().collectAsStateWithLifecycle(emptyList())
    val savedCount = saved.count { it in library.trackByKey }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomPadding.current),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton({ ui.back() })
            Spacer(Modifier.width(4.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        SectionTitle("Appearance")
        ThemeMode.entries.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(scale = 0.985f) { app.settings.setThemeMode(option) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                RadioButton(selected = option == mode, onClick = { app.settings.setThemeMode(option) })
            }
        }

        SectionTitle("Lyrics")
        Text(
            "$savedCount of ${library.tracks.size} songs have lyrics saved on this phone. Saved lyrics work offline.",
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
                onClick = { app.lyrics.downloadAll(library.tracks) },
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text("Download lyrics for all songs")
            }
        }

        SectionTitle("About")
        Text(
            "Attic plays the music in Music/Offline. It has no account and no streaming. ${songCount(library.tracks.size)} on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
