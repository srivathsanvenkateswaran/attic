package app.attic.player.designrefs

import app.attic.player.data.UiStyle
import app.attic.player.data.Track
import app.attic.player.home.Mood
import app.attic.player.storeshots.DemoLibrary
import app.attic.player.storeshots.DemoStates
import app.attic.player.ui.model.BrowseTile
import app.attic.player.ui.model.Destination
import app.attic.player.ui.model.HomeFilter
import app.attic.player.ui.model.HomeState
import app.attic.player.ui.model.InterestChoice
import app.attic.player.ui.model.InterestsActions
import app.attic.player.ui.model.InterestsState
import app.attic.player.ui.model.MoodActions
import app.attic.player.ui.model.MoodState
import app.attic.player.ui.model.MoodTile
import app.attic.player.ui.model.PlayControlsActions
import app.attic.player.ui.model.PlayControlsState
import app.attic.player.ui.model.SearchState
import app.attic.player.ui.model.SettingsState
import app.attic.player.ui.mosaicIds

/** Mood mixes from the demo library, as the Moods section of both design files draws them. */
object MoodStates {
    private val lib get() = DemoLibrary.library
    private val noop: () -> Unit = {}

    private val names = mapOf(
        Mood.Chill to ("Chill" to "Slow and easy"),
        Mood.Happy to ("Happy" to "Bright and cheerful"),
        Mood.Energetic to ("Energetic" to "Fast and driving"),
        Mood.Focus to ("Focus" to "Mostly instrumental, steady"),
        Mood.Party to ("Party" to "Made for dancing"),
        Mood.Melancholy to ("Melancholy" to "Sad and reflective"),
        Mood.Romantic to ("Romantic" to "Tender and warm"),
    )

    /** Chill opens with the songs the design lists; the other moods take turns through the library. */
    private fun tracks(mood: Mood): List<Track> {
        if (mood == Mood.Chill) {
            val named = listOf("Rain on the Skylight", "Lights Over the Water", "Nila Paadhai", "Brushes", "Marea Lenta", "Low Tide Hymn")
                .mapNotNull { title -> lib.tracks.firstOrNull { it.title == title } }
            return (named + lib.tracks.filter { it !in named }).take(25)
        }
        val all = lib.tracks
        val size = if (mood == Mood.Focus) 14 else 25
        return List(size) { all[(mood.ordinal * 7 + it * 3) % all.size] }.distinct()
    }

    fun tile(mood: Mood): MoodTile {
        val (name, line) = names.getValue(mood)
        val tracks = tracks(mood)
        return MoodTile(mood, name, line, tracks, mosaicIds(tracks), "$name mood mix")
    }

    val tiles: List<MoodTile> get() = Mood.entries.map(::tile)

    fun home(ask: Boolean): HomeState {
        val h = DesignStates.home(HomeFilter.All)
        return HomeState(
            isEmpty = false, filter = HomeFilter.All, shortcuts = h.shortcuts, wrapped = h.wrapped, speedDial = h.speedDial, tasteMixes = h.tasteMixes,
            recentTracks = h.recentTracks, onRepeat = h.onRepeat, notLately = h.notLately, newlyAdded = h.newlyAdded, startListening = h.startListening,
            folders = h.folders, biggestAlbums = h.biggestAlbums, biggestArtists = h.biggestArtists, spotlight = h.spotlight,
            currentKey = h.currentKey, isPlaying = h.isPlaying, favouriteCount = h.favouriteCount, radioSeedKey = h.radioSeedKey,
            moods = tiles, askForMoods = ask,
        )
    }

    fun page(playing: Boolean): MoodState {
        val tile = tile(Mood.Chill)
        val now = if (playing) tile.tracks[1].key else DemoLibrary.nowPlaying.key
        return MoodState(Mood.Chill, tile, loading = false, PlayControlsState(tile.contextName, enabled = true, shuffleOn = false, isPlaying = playing), now, isPlaying = true)
    }

    val pageActions = MoodActions(noop, PlayControlsActions(noop, noop, noop), {}, {}, noop, {})

    val browse: SearchState get() {
        val base = DemoStates.searchBrowse
        val moods = tiles.map { BrowseTile(it.name, it.tracks.first().mediaId, Destination.Mood(it.mood.id), it.tracks.size, mosaicIds = it.mosaicIds) }
        return SearchState(base.query, base.results, base.tiles, base.currentKey, base.isPlaying, base.genres, moods)
    }

    /** Search for [query] with the moods whose names match among the results. */
    fun results(query: String): SearchState {
        val base = DemoStates.search(query)
        val r = base.results!!
        val moods = browse.moods.filter { it.title.lowercase().contains(query.lowercase()) }
        return SearchState(
            query,
            app.attic.player.ui.model.SearchResults(r.tracks, r.artists, r.albums, r.folders, r.genres, moods),
            base.tiles, base.currentKey, base.isPlaying,
        )
    }

    fun interests(picked: Boolean, genres: Boolean = true): InterestsState {
        fun choice(id: String, label: String, on: Boolean) = InterestChoice(id, label, picked && on)
        val pickedMoods = setOf(Mood.Chill, Mood.Focus, Mood.Romantic)
        return InterestsState(
            moods = Mood.entries.map { choice(it.id, names.getValue(it).first, it in pickedMoods) },
            genres = if (!genres) {
                emptyList()
            } else {
                listOf("Pop", "Film", "Electronic", "Folk, World, & Country", "Jazz", "Latin", "Hip hop", "Funk / Soul", "Classical")
                    .map { choice(it.lowercase(), it, it in setOf("Pop", "Film", "Jazz")) }
            },
            languages = listOf("ta" to "Tamil", "hi" to "Hindi", "en" to "English", "none" to "No vocals").map { (id, label) -> choice(id, label, id in setOf("ta", "en")) },
            changed = picked,
        )
    }

    val interestsActions = InterestsActions(noop, {}, {}, {}, noop)

    fun settings(style: UiStyle, dark: Boolean, picked: Boolean): SettingsState {
        val b = DesignStates.settings(style, dark)
        return SettingsState(
            themeMode = b.themeMode, uiStyle = b.uiStyle, autoplay = b.autoplay, volumeLevelling = b.volumeLevelling,
            transitions = b.transitions, crossfadeSeconds = b.crossfadeSeconds, bulkLyrics = b.bulkLyrics, savedLyricsCount = b.savedLyricsCount,
            songCount = b.songCount, analysis = b.analysis, backup = b.backup, equaliser = b.equaliser,
            interestsSummary = if (picked) "Chill, Focus, Romantic · 3 genres · Tamil, English" else "Nothing picked yet",
        )
    }
}
