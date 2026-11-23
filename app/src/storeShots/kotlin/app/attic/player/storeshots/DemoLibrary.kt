package app.attic.player.storeshots

import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Library
import app.attic.player.data.LyricLine
import app.attic.player.data.Track

/**
 * An invented library for the store screenshots: made-up artists, albums and songs in English, Tamil,
 * Hindi and Spanish, the mix a personal collection tends to have. None of it names or imitates a real
 * artist or song. Every song's media id is its album's index times 100 plus its track number, so
 * [coverFor] can find the album's generated cover from the id alone.
 */
object DemoLibrary {
    private class AlbumSpec(
        val artist: String,
        val name: String,
        val year: Int,
        val folder: String,
        val cover: Int,
        val songs: List<Pair<String, String>>,
    )

    private val specs = listOf(
        AlbumSpec(
            "Mira Okafor", "Paper Lanterns", 2025, "English", 0,
            listOf(
                "Lights Over the Water" to "3:52", "Porch Light" to "3:21", "Moth Letters" to "4:05",
                "Second Bus Home" to "3:37", "Salt in the Radio" to "2:58", "Paper Lanterns" to "4:41",
                "Small Hours, Big Sky" to "3:12", "Coming Back Slow" to "5:03",
            ),
        ),
        AlbumSpec(
            "Mira Okafor", "Weather Report", 2022, "English", 11,
            listOf(
                "Pressure Front" to "3:44", "Umbrella Song" to "3:05", "Clear by Morning" to "4:12",
                "Fog on Garden Road" to "3:58", "Heatwave Postcard" to "3:26", "Low Tide Hymn" to "4:49",
            ),
        ),
        AlbumSpec(
            "Kavya Raman", "Iravu Rail", 2024, "Tamil", 8,
            listOf(
                "Iravu Rail" to "4:22", "Platform Ettu" to "3:48", "Nila Paadhai" to "4:05",
                "Theneer Kadai" to "3:16", "Kadaisi Vandi" to "4:37", "Ooru Thirumbum" to "5:02",
            ),
        ),
        AlbumSpec(
            "Kavya Raman", "Kolam", 2021, "Tamil", 5,
            listOf(
                "Vaasal Kolam" to "3:34", "Maarghazhi Kaalai" to "4:18", "Thinnai Pechu" to "3:51",
                "Neer Kudam" to "3:09", "Saalai Ooram" to "4:26",
            ),
        ),
        AlbumSpec(
            "Ruhaan Sethi", "Chhat Pe Dhoop", 2023, "Hindi", 9,
            listOf(
                "Chhat Pe Dhoop" to "3:47", "Patang" to "3:22", "Chai Ka Doosra Cup" to "4:01",
                "Purani Diary" to "4:33", "Sheher Ki Shaam" to "3:55", "Khidki Wala Ghar" to "3:40",
                "Aadhi Neend" to "4:14",
            ),
        ),
        AlbumSpec(
            "Ruhaan Sethi", "Monsoon Tapes", 2020, "Hindi", 6,
            listOf(
                "Bheegi Sadak" to "3:29", "Chhatri" to "3:03", "Geeli Mitti" to "4:20",
                "Bijli Gayi" to "2:47", "Tapri Pe Milna" to "3:58", "Baadal Ka Khat" to "4:36",
            ),
        ),
        AlbumSpec(
            "Juniper Vale", "Glasshouse Summer", 2023, "English", 3,
            listOf(
                "Glasshouse" to "3:39", "Neon Peaches" to "3:14", "Swimming Pool Static" to "4:02",
                "Sunburn Choir" to "3:31", "Late Bloom" to "4:27",
            ),
        ),
        AlbumSpec(
            "Velvet Circuit", "Afterglow Protocol", 2025, "Late Night", 10,
            listOf(
                "Afterglow Protocol" to "3:18", "Night Drive Firmware" to "2:54", "Chrome Lungs" to "3:07",
                "404 Heartbeat" to "2:41", "Midnight Checksum" to "3:26", "Drift Mode" to "2:59",
            ),
        ),
        AlbumSpec(
            "Small Hours Tape Club", "Tea at 2 AM", 2024, "Late Night", 1,
            listOf(
                "Kettle On" to "2:16", "Rain on the Skylight" to "2:48", "Cat on the Keyboard" to "2:05",
                "Library Closing Soon" to "2:37", "Unsent Drafts" to "3:02",
            ),
        ),
        AlbumSpec(
            "Paloma Ferrer", "Marea Lenta", 2022, "Español", 2,
            listOf(
                "Marea Lenta" to "3:53", "Azulejo" to "3:24", "Verano en la Azotea" to "4:08",
                "Calle de los Naranjos" to "3:46", "Faro Apagado" to "4:31",
            ),
        ),
        AlbumSpec(
            "Odile Marchetti", "Room Tone", 2021, "English", 4,
            listOf(
                "Room Tone" to "5:12", "Brushes" to "4:38", "Third Floor Walk-Up" to "6:04",
                "Slow Elevator" to "4:57", "Velvet Rope" to "5:26",
            ),
        ),
        AlbumSpec(
            "Tide & Tamarind", "Kadalkarai Sessions", 2025, "Tamil", 7,
            listOf(
                "Kadalkarai" to "4:09", "Tamarind Skies" to "3:44", "Auto Rickshaw Waltz" to "3:17",
                "Filter Coffee Funk" to "3:36", "Marina at Six" to "4:52",
            ),
        ),
    )

    private fun keyOf(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun parseDuration(text: String): Long {
        val (m, s) = text.split(":").map { it.toLong() }
        return (m * 60 + s) * 1000
    }

    /** About six weeks of adding music, newest last, so "Recently added" has an order. */
    private const val ADDED_FROM = 1_758_000_000L

    val library: Library by lazy {
        val albums = specs.mapIndexed { index, spec ->
            val albumKey = "${keyOf(spec.artist)}/${keyOf(spec.name)}"
            val tracks = spec.songs.mapIndexed { i, (title, length) ->
                Track(
                    key = "$albumKey/${i + 1}",
                    mediaId = index * 100L + i + 1,
                    title = title,
                    artist = spec.artist,
                    artistKeys = listOf(keyOf(spec.artist)),
                    album = spec.name,
                    albumKey = albumKey,
                    folder = spec.folder,
                    trackNumber = i + 1,
                    durationMs = parseDuration(length),
                    year = spec.year,
                    dateAddedSec = ADDED_FROM + index * 86_400L * 4 + i,
                )
            }
            Album(albumKey, spec.name, spec.artist, keyOf(spec.artist), tracks, spec.year)
        }
        val tracks = albums.flatMap { it.tracks }
        val artists = albums.groupBy { it.artist }.map { (name, owned) ->
            Artist(keyOf(name), name, owned.flatMap { it.tracks }, owned.sortedByDescending { it.year })
        }
        val folders = listOf("English", "Tamil", "Hindi", "Español", "Late Night").map { name ->
            FolderPlaylist(name, tracks.filter { it.folder == name })
        }
        Library(tracks.sortedBy { it.title.lowercase() }, albums, artists, folders)
    }

    /** The generated cover for a media id, or null for an id that is not in the library. */
    fun coverFor(mediaId: Long): Int? {
        val index = (mediaId / 100).toInt()
        return specs.getOrNull(index)?.cover
    }

    fun album(name: String): Album = library.albums.first { it.name == name }
    fun artist(name: String): Artist = library.artists.first { it.name == name }
    fun track(title: String): Track = library.tracks.first { it.title == title }

    /** The song every Now Playing and lyrics shot shows. */
    val nowPlaying: Track get() = track("Lights Over the Water")

    /** Where the Now Playing shots are paused in the song: on the line [lyrics] marks as sung. */
    const val POSITION_MS = 78_500L

    /** Words written for these screenshots. */
    val lyrics: List<LyricLine> = listOf(
        0 to "",
        12_400 to "I left the porch light on for you",
        17_800 to "the moths are writing letters on the glass",
        23_600 to "every bus that turns the corner",
        28_900 to "sounds a little like you coming back",
        35_000 to "",
        41_200 to "and the kettle knows your name",
        46_700 to "the stairs still creak the way you climbed them",
        52_300 to "I kept the window open in the rain",
        58_100 to "in case the night forgot to bring you",
        66_000 to "",
        72_400 to "paper lanterns over the water",
        78_000 to "carry what we couldn't say",
        83_600 to "let them drift until the morning",
        89_200 to "let them burn the long way",
        96_800 to "",
        103_500 to "I counted boats along the harbour",
        109_100 to "named the lights that didn't move",
        114_800 to "one for every year you've been gone",
        120_200 to "one for the way back home to you",
    ).map { (ms, text) -> LyricLine(ms.toLong(), text) }
}
