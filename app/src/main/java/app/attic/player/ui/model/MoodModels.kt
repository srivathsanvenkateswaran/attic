package app.attic.player.ui.model

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.AtticApp
import app.attic.player.R
import app.attic.player.attic
import app.attic.player.data.Interests
import app.attic.player.data.LanguageLabels
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.home.Mood
import app.attic.player.home.MoodMixes
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.mosaicIds
import app.attic.player.ui.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@get:StringRes
val Mood.nameRes: Int
    get() = when (this) {
        Mood.Chill -> R.string.mood_chill
        Mood.Happy -> R.string.mood_happy
        Mood.Energetic -> R.string.mood_energetic
        Mood.Focus -> R.string.mood_focus
        Mood.Party -> R.string.mood_party
        Mood.Melancholy -> R.string.mood_melancholy
        Mood.Romantic -> R.string.mood_romantic
    }

/** The mood's one line: "Slow and easy". */
@get:StringRes
val Mood.descriptionRes: Int
    get() = when (this) {
        Mood.Chill -> R.string.mood_chill_body
        Mood.Happy -> R.string.mood_happy_body
        Mood.Energetic -> R.string.mood_energetic_body
        Mood.Focus -> R.string.mood_focus_body
        Mood.Party -> R.string.mood_party_body
        Mood.Melancholy -> R.string.mood_melancholy_body
        Mood.Romantic -> R.string.mood_romantic_body
    }

/**
 * A mood's mix as Home and Search show it: songs no longer on the phone left out. [contextName] is what
 * it plays under ("Chill mood mix").
 */
@Immutable
class MoodTile(
    val mood: Mood,
    val name: String,
    val description: String,
    val tracks: List<Track>,
    val mosaicIds: List<Long>,
    val contextName: String,
)

/** Every mood with a mix today, as tiles; a mix whose songs have mostly left the phone is dropped. */
fun moodTiles(mixes: List<MoodMixes.Mix>, library: Library, resources: Resources): List<MoodTile> =
    mixes.mapNotNull { mix ->
        val tracks = mix.keys.mapNotNull { library.trackByKey[it] }
        if (tracks.size < MoodMixes.MIN_SONGS) return@mapNotNull null
        val name = resources.getString(mix.mood.nameRes)
        MoodTile(mix.mood, name, resources.getString(mix.mood.descriptionRes), tracks, mosaicIds(tracks), resources.getString(R.string.mood_context, name))
    }

/** Plays a mood's mix in its own order, and tells radio, so autoplay after it carries on in that mood. */
fun AtticApp.playMood(tile: MoodTile, index: Int = 0, shuffled: Boolean = false) {
    radio.playingMoodMix(tile.contextName, tile.mood, tile.tracks.map { it.key })
    if (shuffled) player.shufflePlay(tile.tracks, tile.contextName) else player.play(tile.tracks, index, tile.contextName, shuffled = false)
}

/** "Chill, Focus, Romantic · 3 genres · Tamil, English", or "Nothing picked yet". */
fun interestsSummary(interests: Interests, resources: Resources): String {
    if (interests.isEmpty) return resources.getString(R.string.settings_interests_none)
    val parts = ArrayList<String>()
    val moods = Mood.entries.filter { it.id in interests.moods }.sortedBy { interests.moods.indexOf(it.id) }
    if (moods.isNotEmpty()) parts += moods.joinToString(", ") { resources.getString(it.nameRes) }
    if (interests.genres.isNotEmpty()) {
        parts += resources.getQuantityString(R.plurals.settings_interests_genres, interests.genres.size, interests.genres.size.toString())
    }
    if (interests.languages.isNotEmpty()) parts += interests.languages.joinToString(", ") { LanguageLabels.displayName(it) }
    return parts.joinToString(" · ")
}

// Mood page

@Immutable
class MoodState(
    val mood: Mood?,
    /** Null while the day's mixes are still being built ([loading]) or when the mood has no mix today. */
    val tile: MoodTile?,
    val loading: Boolean,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class MoodActions(
    val back: () -> Unit,
    val controls: PlayControlsActions,
    val play: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
    /** Radio from the middle of the mix, picking only songs that fit the mood. */
    val startRadio: () -> Unit,
    /** A swipe on a song row adds it to the queue. */
    val queue: (Track) -> Unit,
)

/** A mood's page. The mixes are built off the main thread by [app.attic.player.home.HomeFeed]. */
@Composable
fun rememberMoodModel(library: Library, id: String): UiModel<MoodState, MoodActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val resources = LocalResources.current
    val feed by app.homeFeed.feed.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val mood = Mood.fromId(id)
    val tile = remember(feed.moodMixes, library, mood) { moodTiles(feed.moodMixes.filter { it.mood == mood }, library, resources).firstOrNull() }
    val tracks = tile?.tracks.orEmpty()
    val contextName = tile?.contextName.orEmpty()
    val base = playControls(app, ui, rememberHaptics(), player, tracks, contextName)
    // Playing through the mood keeps autoplay in the mood afterwards.
    val controls = PlayControlsActions(
        addToPlaylist = base.actions.addToPlaylist,
        shuffle = { tile?.let { app.playMood(it, shuffled = true) } },
        playOrPause = {
            if (tile != null && player.contextName != contextName) app.radio.playingMoodMix(contextName, tile.mood, tracks.map { it.key })
            base.actions.playOrPause()
        },
        toggleSmartShuffle = base.actions.toggleSmartShuffle,
    )
    // Opened before Home has built anything (from Search right after launch): build now.
    LaunchedEffect(feed.moodsBuilt) { if (!feed.moodsBuilt) app.homeFeed.refresh() }
    val state = MoodState(mood, tile, loading = !feed.moodsBuilt, base.state, player.current?.key, player.isPlaying)
    val actions = MoodActions(
        back = { ui.back() },
        controls = controls,
        play = { index -> tile?.let { app.playMood(it, index) } },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        startRadio = { if (tile != null) app.scope.launch { app.radio.startFromMood(tile.mood, tile.name, tracks.map { it.key }) } },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}

// Moods and interests

/** One chip: what it stores, what it says, and whether it is picked. */
@Immutable
class InterestChoice(val id: String, val label: String, val selected: Boolean)

@Immutable
class InterestsState(
    val moods: List<InterestChoice>,
    /** The library's genres, biggest first, up to [MAX_GENRES]. Empty until there are genres. */
    val genres: List<InterestChoice>,
    val languages: List<InterestChoice>,
    /** True when the picks differ from what is saved, so Save has something to do. */
    val changed: Boolean,
)

class InterestsActions(
    val back: () -> Unit,
    val toggleMood: (String) -> Unit,
    val toggleGenre: (String) -> Unit,
    val toggleLanguage: (String) -> Unit,
    /** Saves the picks and goes back. Home's mixes are built again for them. */
    val save: () -> Unit,
)

const val MAX_GENRES = 24

@Composable
fun rememberInterestsModel(library: Library): UiModel<InterestsState, InterestsActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val resources = LocalResources.current
    val saved by app.settings.interests.collectAsStateWithLifecycle()
    val genres by app.genres.genres.collectAsStateWithLifecycle()
    var moods by rememberSaveable { mutableStateOf(saved.moods) }
    var genreKeys by rememberSaveable { mutableStateOf(saved.genres) }
    var languages by rememberSaveable { mutableStateOf(saved.languages) }
    // The languages the library holds, worked out once the page opens; until then, every choice.
    val present by produceState<Set<String>?>(null, library) { value = libraryLanguages(app, library) }

    val genreChoices = genres.orEmpty().take(MAX_GENRES).let { top ->
        // A picked genre stays offered even if it has slipped out of the top.
        top + genres.orEmpty().drop(MAX_GENRES).filter { it.key in genreKeys }
    }.map { InterestChoice(it.key, it.name, it.key in genreKeys) }
    val languageChoices = LanguageLabels.CHOICES
        .filter { (code, _) -> present == null || present!!.isEmpty() || code in present!! || code in languages }
        .map { (code, name) -> InterestChoice(code, if (code == LanguageLabels.NO_VOCALS) resources.getString(R.string.language_no_vocals) else name, code in languages) }
    val state = InterestsState(
        moods = Mood.entries.map { InterestChoice(it.id, resources.getString(it.nameRes), it.id in moods) },
        genres = genreChoices,
        languages = languageChoices,
        changed = moods != saved.moods || genreKeys != saved.genres || languages != saved.languages || saved.updatedAt == 0L,
    )
    val actions = InterestsActions(
        back = { ui.back() },
        toggleMood = { id -> moods = if (id in moods) moods - id else moods + id },
        toggleGenre = { key -> genreKeys = if (key in genreKeys) genreKeys - key else genreKeys + key },
        toggleLanguage = { code -> languages = if (code in languages) languages - code else languages + code },
        save = {
            app.settings.setInterests(Interests(moods, genreKeys, languages, System.currentTimeMillis()))
            app.settings.dismissMoodPrompt()
            app.scope.launch { app.homeFeed.refresh() }
            ui.back()
        },
    )
    return UiModel(state, actions)
}

/** The languages [LanguageLabels.resolve] finds among the library's songs. */
private suspend fun libraryLanguages(app: AtticApp, library: Library): Set<String> = withContext(Dispatchers.Default) {
    val dao = app.database.dao()
    val stored = dao.storedLanguages().associate { it.trackKey to it.language }
    val lyrics = dao.lyricsTexts().associateBy { it.trackKey }
    library.tracks.mapNotNullTo(HashSet()) { track ->
        val text = lyrics[track.key]
        LanguageLabels.resolve(track.folder, track.album, stored[track.key], text?.plain ?: text?.synced, text?.instrumental == true)
    }
}
