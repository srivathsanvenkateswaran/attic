package app.attic.player.data.backup

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Why a file could not be read as a backup. */
class BackupReadException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        /** Not JSON, or JSON that isn't an Attic backup. */
        NotABackup,

        /** An Attic backup written by a newer version, with a schema this one doesn't know. */
        TooNew,

        /** An Attic backup that is cut short or has fields of the wrong type. */
        Damaged,

        /** Larger than any real backup, so it is refused before it fills the memory. */
        TooLarge,
    }
}

/**
 * Reads and writes the backup file: UTF-8 JSON, optionally gzipped (the copy Android's own backup
 * carries is). The top level names the format and a schema version:
 *
 * ```
 * { "format": "attic-backup", "schemaVersion": 1, "createdAt": 1795000000000, "appVersion": "1.0.0",
 *   "settings": {…}, "likes": […], "playlists": […], "plays": […], "listens": […], "lyrics": […],
 *   "languages": […], "queue": {…} }
 * ```
 *
 * A reader accepts any schema up to [SCHEMA_VERSION] and ignores fields it doesn't know, so a later
 * version can add fields without a new number. A change an older reader would misread (a field
 * renamed, or its meaning changed) raises the number; a reader refuses a number above its own.
 */
object BackupCodec {
    const val FORMAT = "attic-backup"
    const val SCHEMA_VERSION = 1

    /** Refused above this. Real backups are a few megabytes. */
    const val MAX_BYTES = 64 * 1024 * 1024

    fun encode(backup: Backup): String = JSONObject().apply {
        put("format", FORMAT)
        put("schemaVersion", SCHEMA_VERSION)
        put("createdAt", backup.createdAt)
        putOpt("appVersion", backup.appVersion)
        backup.settings?.let { s ->
            put(
                "settings",
                JSONObject().apply {
                    putOpt("theme", s.theme)
                    putOpt("uiStyle", s.uiStyle)
                    s.autoplay?.let { put("autoplay", it) }
                },
            )
        }
        put("likes", array(backup.likes) { JSONObject().put("song", it.song).put("likedAt", it.likedAt) })
        put(
            "playlists",
            array(backup.playlists) {
                JSONObject().put("name", it.name).put("createdAt", it.createdAt).put("songs", strings(it.songs))
            },
        )
        put(
            "plays",
            array(backup.plays) {
                JSONObject().put("song", it.song).put("lastPlayedAt", it.lastPlayedAt).put("playCount", it.playCount)
            },
        )
        put(
            "listens",
            array(backup.listens) {
                JSONObject()
                    .put("song", it.song)
                    .put("startedAt", it.startedAt)
                    .put("listenedMs", it.listenedMs)
                    .put("durationMs", it.durationMs)
                    .put("outcome", it.outcome)
                    .putOpt("context", it.context)
                    .put("radioCross", it.radioCross)
            },
        )
        put(
            "lyrics",
            array(backup.lyrics) {
                JSONObject()
                    .put("song", it.song)
                    .putOpt("synced", it.synced)
                    .putOpt("plain", it.plain)
                    .put("instrumental", it.instrumental)
                    .put("fetchedAt", it.fetchedAt)
            },
        )
        put(
            "languages",
            array(backup.languages) {
                JSONObject().put("song", it.song).put("language", it.language).put("source", it.source)
            },
        )
        backup.queue?.let { q ->
            put(
                "queue",
                JSONObject()
                    .put("songs", strings(q.songs))
                    .put("index", q.index)
                    .put("positionMs", q.positionMs)
                    .put("repeatMode", q.repeatMode)
                    .put("shuffle", q.shuffle)
                    .putOpt("baseOrder", q.baseOrder?.let(::strings))
                    .putOpt("context", q.context)
                    .putOpt("radioSeed", q.radioSeed),
            )
        }
    }.toString()

    fun gzip(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    /** Reads a backup from a file's bytes, plain or gzipped. */
    fun decode(bytes: ByteArray): Backup {
        if (bytes.size > MAX_BYTES) throw BackupReadException(BackupReadException.Reason.TooLarge)
        val text = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            try {
                gunzip(bytes)
            } catch (e: IOException) {
                throw BackupReadException(BackupReadException.Reason.Damaged, e)
            }
        } else {
            String(bytes, Charsets.UTF_8)
        }
        return decode(text)
    }

    fun decode(text: String): Backup {
        val root = try {
            JSONObject(text.trimStart('\uFEFF'))
        } catch (e: JSONException) {
            // Cut short and not JSON at all look alike to the parser; the format marker tells them apart.
            val reason = if (text.contains("\"$FORMAT\"")) BackupReadException.Reason.Damaged else BackupReadException.Reason.NotABackup
            throw BackupReadException(reason, e)
        }
        if (root.opt("format") != FORMAT) throw BackupReadException(BackupReadException.Reason.NotABackup)
        val version = root.opt("schemaVersion") as? Number ?: throw BackupReadException(BackupReadException.Reason.Damaged)
        if (version.toInt() < 1) throw BackupReadException(BackupReadException.Reason.Damaged)
        if (version.toInt() > SCHEMA_VERSION) throw BackupReadException(BackupReadException.Reason.TooNew)
        return try {
            read(root)
        } catch (e: JSONException) {
            throw BackupReadException(BackupReadException.Reason.Damaged, e)
        } catch (e: ClassCastException) {
            throw BackupReadException(BackupReadException.Reason.Damaged, e)
        }
    }

    private fun read(root: JSONObject): Backup = Backup(
        createdAt = root.long("createdAt"),
        appVersion = root.stringOrNull("appVersion"),
        settings = root.objectOrNull("settings")?.let {
            BackupSettings(it.stringOrNull("theme"), it.stringOrNull("uiStyle"), it.booleanOrNull("autoplay"))
        },
        likes = root.objects("likes") { BackupLike(it.string("song"), it.long("likedAt")) },
        playlists = root.objects("playlists") {
            BackupPlaylist(it.string("name"), it.long("createdAt"), it.stringList("songs"))
        },
        plays = root.objects("plays") { BackupPlay(it.string("song"), it.long("lastPlayedAt"), it.long("playCount").toInt()) },
        listens = root.objects("listens") {
            BackupListen(
                song = it.string("song"),
                startedAt = it.long("startedAt"),
                listenedMs = it.long("listenedMs"),
                durationMs = it.long("durationMs"),
                outcome = it.string("outcome"),
                context = it.stringOrNull("context"),
                radioCross = it.booleanOrNull("radioCross") ?: false,
            )
        },
        lyrics = root.objects("lyrics") {
            BackupLyrics(
                song = it.string("song"),
                synced = it.stringOrNull("synced"),
                plain = it.stringOrNull("plain"),
                instrumental = it.booleanOrNull("instrumental") ?: false,
                fetchedAt = it.long("fetchedAt"),
            )
        },
        languages = root.objects("languages") { BackupLanguage(it.string("song"), it.string("language"), it.string("source")) },
        queue = root.objectOrNull("queue")?.let {
            BackupQueue(
                songs = it.stringList("songs"),
                index = it.long("index").toInt(),
                positionMs = it.long("positionMs"),
                repeatMode = it.long("repeatMode").toInt(),
                shuffle = it.booleanOrNull("shuffle") ?: false,
                baseOrder = if (it.has("baseOrder") && !it.isNull("baseOrder")) it.stringList("baseOrder") else null,
                context = it.stringOrNull("context"),
                radioSeed = it.stringOrNull("radioSeed"),
            )
        },
    )

    private fun gunzip(bytes: ByteArray): String {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_BYTES) throw BackupReadException(BackupReadException.Reason.TooLarge)
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun <T> array(items: List<T>, write: (T) -> JSONObject): JSONArray =
        JSONArray().apply { items.forEach { put(write(it)) } }

    private fun strings(items: List<String>): JSONArray = JSONArray().apply { items.forEach { put(it) } }

    // Typed reads that behave the same on Android's org.json and the reference one the tests run on,
    // which differ in how far they coerce a value of the wrong type.

    private fun damaged(field: String): Nothing = throw JSONException("Bad or missing $field")

    private fun JSONObject.string(name: String): String = opt(name) as? String ?: damaged(name)

    private fun JSONObject.stringOrNull(name: String): String? = when (val value = opt(name)) {
        null, JSONObject.NULL -> null
        is String -> value
        else -> damaged(name)
    }

    private fun JSONObject.long(name: String): Long = (opt(name) as? Number)?.toLong() ?: damaged(name)

    private fun JSONObject.booleanOrNull(name: String): Boolean? = when (val value = opt(name)) {
        null, JSONObject.NULL -> null
        is Boolean -> value
        else -> damaged(name)
    }

    private fun JSONObject.objectOrNull(name: String): JSONObject? = when (val value = opt(name)) {
        null, JSONObject.NULL -> null
        is JSONObject -> value
        else -> damaged(name)
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val array = opt(name) as? JSONArray ?: damaged(name)
        return List(array.length()) { array.opt(it) as? String ?: damaged(name) }
    }

    /** A missing list is an empty one, so a later schema can drop a section an older one wrote. */
    private fun <T> JSONObject.objects(name: String, read: (JSONObject) -> T): List<T> {
        val array = when (val value = opt(name)) {
            null, JSONObject.NULL -> return emptyList()
            is JSONArray -> value
            else -> damaged(name)
        }
        return List(array.length()) { read(array.opt(it) as? JSONObject ?: damaged(name)) }
    }
}
