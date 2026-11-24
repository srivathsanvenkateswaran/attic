package app.attic.player.playback.eq

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import app.attic.player.data.AppSettings
import app.attic.player.data.EQ_SCOPE_ALBUM
import app.attic.player.data.EQ_SCOPE_SONG
import app.attic.player.data.EqPresetEntity
import app.attic.player.data.EqSettingEntity
import app.attic.player.data.Library
import app.attic.player.data.MusicDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where a song's curve comes from, in the order they are looked for. */
enum class EqScope { Song, Album, Default }

/** Something a curve can be saved for. */
sealed interface EqTarget {
    data class Song(val key: String) : EqTarget
    data class Album(val key: String) : EqTarget
    data object Default : EqTarget
}

/** A curve and where it came from. */
data class ResolvedEq(val curve: EqCurve, val scope: EqScope)

/** A named curve of the user's own. */
data class UserPreset(val id: Long, val name: String, val curve: EqCurve)

/** Everything the equaliser holds, as one immutable value. */
data class EqualiserState(
    val on: Boolean,
    val default: EqCurve,
    val songs: Map<String, EqCurve>,
    val albums: Map<String, EqCurve>,
) {
    /**
     * The curve [trackKey] plays with: its own, else its album's, else the default. This is the
     * whole rule; the playback thread, the sheet and the Now Playing indicator all ask it.
     */
    fun resolve(trackKey: String?, albumKey: String?): ResolvedEq {
        trackKey?.let { songs[it] }?.let { return ResolvedEq(it, EqScope.Song) }
        albumKey?.let { albums[it] }?.let { return ResolvedEq(it, EqScope.Album) }
        return ResolvedEq(default, EqScope.Default)
    }

    fun curveOf(target: EqTarget): EqCurve? = when (target) {
        is EqTarget.Song -> songs[target.key]
        is EqTarget.Album -> albums[target.key]
        EqTarget.Default -> default
    }

    companion object {
        val Empty = EqualiserState(on = false, default = EqCurve.FLAT, songs = emptyMap(), albums = emptyMap())

        /** Builds the maps from saved rows, dropping any whose curve can't be read. */
        fun of(on: Boolean, default: EqCurve, rows: List<EqSettingEntity>): EqualiserState {
            val songs = HashMap<String, EqCurve>()
            val albums = HashMap<String, EqCurve>()
            for (row in rows) {
                val curve = EqCurve.decode(row.gains) ?: continue
                when (row.scope) {
                    EQ_SCOPE_SONG -> songs[row.key] = curve
                    EQ_SCOPE_ALBUM -> albums[row.key] = curve
                }
            }
            return EqualiserState(on, default, songs, albums)
        }
    }
}

/**
 * The equaliser's settings, held in memory as one [EqualiserState] that the playback thread reads
 * without waiting, and saved to the database and settings behind it.
 *
 * A slider being dragged changes the state many times a second. Each change is in memory at once, so
 * the next audio buffer hears it, and the save waits until the slider has been still for
 * [SAVE_DELAY_MS]. While a save is waiting the database is behind memory, so a change read back from
 * it (a restore, say) is taken only when nothing is waiting.
 */
@OptIn(UnstableApi::class)
class EqualiserStore(
    private val dao: MusicDao,
    private val settings: AppSettings,
    private val library: StateFlow<Library?>,
    private val scope: CoroutineScope,
) : EqSource {
    private val _state = MutableStateFlow(
        EqualiserState(settings.equaliserOn.value, EqCurve.decode(settings.equaliserDefault.value) ?: EqCurve.FLAT, emptyMap(), emptyMap()),
    )
    val state: StateFlow<EqualiserState> = _state.asStateFlow()

    /** The user's own presets, oldest first. */
    val presets: StateFlow<List<UserPreset>> = dao.eqPresets()
        .map { rows -> rows.mapNotNull { row -> EqCurve.decode(row.gains)?.let { UserPreset(row.id, row.name, it) } } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    @Volatile
    override var version: Long = 0
        private set

    private val pending = HashMap<EqTarget, Job>()

    /** A database change arrived while saves were waiting, so it was set aside; it is read again once they finish. */
    private var missedRows = false

    init {
        scope.launch {
            dao.eqSettings().collect { rows ->
                if (pending.isEmpty()) publish(EqualiserState.of(_state.value.on, _state.value.default, rows)) else missedRows = true
            }
        }
        // A restore writes the settings directly; follow it.
        scope.launch { settings.equaliserOn.collect { on -> if (on != _state.value.on) publish(_state.value.copy(on = on)) } }
        scope.launch {
            settings.equaliserDefault.collect { encoded ->
                if (EqTarget.Default !in pending) {
                    val curve = EqCurve.decode(encoded) ?: EqCurve.FLAT
                    if (curve != _state.value.default) publish(_state.value.copy(default = curve))
                }
            }
        }
    }

    private fun publish(next: EqualiserState) {
        _state.value = next
        version++
    }

    fun setOn(on: Boolean) {
        if (on == _state.value.on) return
        publish(_state.value.copy(on = on))
        settings.setEqualiserOn(on)
    }

    /**
     * Sets [target]'s curve. It is heard at once and saved shortly after. Setting a song or album
     * that had no curve gives it one, which is how "Save for this song" works. Any change also turns
     * the equaliser on, since a change no one can hear would only confuse.
     */
    fun set(target: EqTarget, curve: EqCurve) {
        val now = _state.value
        val next = when (target) {
            is EqTarget.Song -> now.copy(songs = now.songs + (target.key to curve))
            is EqTarget.Album -> now.copy(albums = now.albums + (target.key to curve))
            EqTarget.Default -> now.copy(default = curve)
        }.copy(on = true)
        if (next == now) return
        publish(next)
        if (!now.on) settings.setEqualiserOn(true)
        save(target) {
            when (target) {
                is EqTarget.Song -> dao.saveEqSetting(EqSettingEntity(EQ_SCOPE_SONG, target.key, curve.encode(), System.currentTimeMillis()))
                is EqTarget.Album -> dao.saveEqSetting(EqSettingEntity(EQ_SCOPE_ALBUM, target.key, curve.encode(), System.currentTimeMillis()))
                EqTarget.Default -> withContext(Dispatchers.Main) { settings.setEqualiserDefault(curve.encode()) }
            }
        }
    }

    /** Removes a song's or album's own curve, so it falls back to the next one in line. */
    fun clear(target: EqTarget) {
        val now = _state.value
        val next = when (target) {
            is EqTarget.Song -> now.copy(songs = now.songs - target.key)
            is EqTarget.Album -> now.copy(albums = now.albums - target.key)
            EqTarget.Default -> now.copy(default = EqCurve.FLAT)
        }
        if (next == now) return
        publish(next)
        save(target, delayMs = 0) {
            when (target) {
                is EqTarget.Song -> dao.deleteEqSetting(EQ_SCOPE_SONG, target.key)
                is EqTarget.Album -> dao.deleteEqSetting(EQ_SCOPE_ALBUM, target.key)
                EqTarget.Default -> withContext(Dispatchers.Main) { settings.setEqualiserDefault(EqCurve.FLAT.encode()) }
            }
        }
    }

    private fun save(target: EqTarget, delayMs: Long = SAVE_DELAY_MS, write: suspend () -> Unit) {
        pending.remove(target)?.cancel()
        lateinit var job: Job
        job = scope.launch {
            delay(delayMs)
            withContext(Dispatchers.IO) { write() }
            if (pending[target] === job) pending.remove(target)
            // A restore that landed during a save of the default writes nothing the default's save
            // would echo back, so read the rows again now rather than wait for the next change.
            if (pending.isEmpty() && missedRows) {
                missedRows = false
                val rows = withContext(Dispatchers.IO) { dao.allEqSettings() }
                if (pending.isEmpty()) publish(EqualiserState.of(_state.value.on, _state.value.default, rows))
            }
        }
        pending[target] = job
    }

    fun savePreset(name: String, curve: EqCurve) {
        scope.launch(Dispatchers.IO) { dao.insertEqPreset(EqPresetEntity(name = name.trim(), gains = curve.encode(), createdAt = System.currentTimeMillis())) }
    }

    fun renamePreset(id: Long, name: String) {
        scope.launch(Dispatchers.IO) { dao.renameEqPreset(id, name.trim()) }
    }

    fun deletePreset(id: Long) {
        scope.launch(Dispatchers.IO) { dao.deleteEqPreset(id) }
    }

    /** Called on the playback thread for each new item, and again whenever [version] moves. */
    override fun curveFor(timeline: Timeline, periodUid: Any?): EqCurve? {
        val state = _state.value
        if (!state.on || periodUid == null || timeline.isEmpty) return null
        val index = timeline.getIndexOfPeriod(periodUid)
        if (index == C.INDEX_UNSET) return null
        val window = timeline.getPeriod(index, Timeline.Period()).windowIndex
        val key = timeline.getWindow(window, Timeline.Window()).mediaItem.mediaId
        val album = library.value?.trackByKey?.get(key)?.albumKey
        return state.resolve(key, album).curve.takeUnless { it.isFlat }
    }

    private companion object {
        const val SAVE_DELAY_MS = 400L
    }
}
