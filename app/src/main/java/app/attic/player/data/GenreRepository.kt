package app.attic.player.data

import android.content.Context
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.analysis.StyleCodec
import app.attic.player.analysis.StyleLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * The Genres list, rebuilt on a background thread whenever the library changes or analysis stores more
 * styles. Null until the first build. Built only while a screen is watching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GenreRepository(context: Context, dao: MusicDao, library: StateFlow<Library?>, scope: CoroutineScope) {
    private val app = context.applicationContext
    /** The model's 400 style names, read once. Mood mixes use them to file untagged songs under genres. */
    val labels by lazy { StyleLabels.parse(app.assets.open(StyleLabels.ASSET).bufferedReader().use { it.readText() }) }

    val genres: StateFlow<List<Genre>?> =
        combine(library.filterNotNull(), dao.styles(SongAnalyzer.MODEL_VERSION)) { lib, rows -> lib to rows }
            // During a backfill styles arrive a song at a time; only the latest state is worth building.
            .conflate()
            .mapLatest { (lib, rows) ->
                GenreIndex.build(lib, rows.associate { it.trackKey to StyleCodec.decode(it.top) }, labels)
            }
            .flowOn(Dispatchers.Default)
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), null)

    private companion object {
        /** Keeps the list through a rotation or a quick trip to another tab. */
        const val STOP_AFTER_MS = 5_000L
    }
}
