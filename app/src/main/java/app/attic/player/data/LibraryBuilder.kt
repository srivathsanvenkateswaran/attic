package app.attic.player.data

/** Turns MediaStore rows into the albums, artists and folder playlists the app shows. */
object LibraryBuilder {
    const val ROOT = "Music/Offline/"
    private val THEMED_FOLDERS = setOf("English", "Hindi", "Tamil", "Phonk")
    private const val VARIOUS_ARTISTS = "Various Artists"
    private const val UNKNOWN_ARTIST = "Unknown artist"
    private val VARIOUS_KEY = TitleCleaner.normalize(VARIOUS_ARTISTS)
    private val UNKNOWN_TAGS = setOf("<unknown>", "unknown", "")

    private class Draft(
        val raw: RawTrack,
        val topFolder: String?,
        val isThemed: Boolean,
        val title: String,
        val artist: String,
        val credits: List<String>,
        val album: String?,
        val trackNumber: Int?,
    ) {
        var albumKey: String? = null
    }

    fun build(rows: List<RawTrack>): Library {
        val drafts = rows.map(::draft)

        // Folder names are the most reliable spelling of an artist, so they seed the name table.
        val names = ArtistNames()
        drafts.mapNotNull { it.topFolder.takeUnless { _ -> it.isThemed } }
            .distinct()
            .filter { TitleCleaner.normalize(it) != VARIOUS_KEY }
            .forEach(names::addFolder)
        drafts.forEach { d -> d.credits.forEach(names::addCredit) }

        assignAlbumKeys(drafts)
        val junkNumbers = junkTrackNumberAlbums(drafts)

        val tracks = drafts.map { d ->
            val creditKeys = d.credits.map(TitleCleaner::normalize)
            val folderKey = d.topFolder.takeUnless { d.isThemed }?.let(TitleCleaner::normalize)
            val artistKeys = (listOfNotNull(folderKey) + creditKeys)
                .distinct()
                .filter { it.isNotEmpty() && it != VARIOUS_KEY }
            Track(
                key = d.raw.relativePath + d.raw.displayName,
                mediaId = d.raw.mediaId,
                title = d.title,
                artist = d.artist,
                artistKeys = artistKeys,
                album = d.album,
                albumKey = d.albumKey,
                folder = d.topFolder,
                trackNumber = d.trackNumber.takeUnless { d.albumKey in junkNumbers },
                durationMs = d.raw.durationMs,
                year = d.raw.year,
                dateAddedSec = d.raw.dateAddedSec,
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

        val albums = buildAlbums(tracks, drafts, names)
        val artists = buildArtists(tracks, albums, names)
        val folders = tracks.filter { it.folder in THEMED_FOLDERS }
            .groupBy { it.folder!! }
            .map { (name, list) -> FolderPlaylist(name, list) }
            .sortedBy { it.name }

        return Library(tracks, albums, artists, folders)
    }

    private fun draft(raw: RawTrack): Draft {
        val pathInRoot = raw.relativePath.removePrefix(ROOT).trim('/')
        val pathParts = pathInRoot.split('/').filter { it.isNotEmpty() }
        val topFolder = pathParts.firstOrNull()
        val isThemed = topFolder in THEMED_FOLDERS
        val isVarious = topFolder != null && TitleCleaner.normalize(topFolder) == VARIOUS_KEY

        val tagTitleRaw = raw.title?.trim()?.takeIf { it.isNotEmpty() }?.let(TitleCleaner::stripYoutubeId)
        val tagArtist = raw.artist?.trim()?.takeUnless { it.lowercase() in UNKNOWN_TAGS }

        // MediaStore fills a missing album with the parent folder's name, so that is not a real tag.
        val parentFolder = pathParts.lastOrNull() ?: raw.relativePath.trim('/').substringAfterLast('/')
        val tagAlbum = raw.album?.trim()?.takeUnless { it.lowercase() in UNKNOWN_TAGS }
        val albumIsPlaceholder = tagAlbum == null || (pathParts.size <= 1 && tagAlbum == parentFolder)
        val album = when {
            !albumIsPlaceholder -> tagAlbum
            pathParts.size >= 2 -> pathParts[1]
            else -> null
        }

        val fileTitle = TitleCleaner.titleFromFilename(raw.displayName)
        // MediaStore also fills a missing title, with the raw filename stem. Treat that as no tag.
        val stem = raw.displayName.substringBeforeLast('.')
        val tagTitle = tagTitleRaw?.takeUnless { it == TitleCleaner.stripYoutubeId(stem) }
        val folderArtist = topFolder.takeUnless { isThemed || isVarious }

        val title: String
        val artist: String
        val credits: List<String>
        if (album == null && isThemed) {
            val rip = TitleCleaner.parseRip(tagTitle ?: fileTitle, tagArtist)
            title = rip.title
            artist = rip.artist ?: UNKNOWN_ARTIST
            credits = rip.credits
        } else {
            val tagCredits = tagArtist?.let { TitleCleaner.splitCredits(it) }.orEmpty()
            title = TitleCleaner.stripArtistPrefix(
                tagTitle ?: fileTitle,
                tagCredits + listOfNotNull(folderArtist),
            )
            artist = tagArtist?.let(TitleCleaner::dedupeCredits) ?: folderArtist ?: UNKNOWN_ARTIST
            credits = tagCredits
        }

        // Some rips tag every song as track 63, so a number past any real soundtrack's length is noise.
        val trackNumber = raw.cdTrackNumber?.substringBefore('/')?.trim()?.toIntOrNull()?.takeIf { it in 1..40 }
            ?: TitleCleaner.trackNumberFromFilename(raw.displayName)

        return Draft(raw, topFolder, isThemed, title, artist, credits, album, trackNumber)
    }

    /**
     * Same album name under one artist folder is one album. A single from a themed folder joins an
     * existing album of that name, so "Yeh Jawaani Hai Deewani" in Hindi/ merges with the full album.
     */
    private fun assignAlbumKeys(drafts: List<Draft>) {
        val filed = drafts.filter { it.album != null && !it.isThemed }
        filed.forEach { d ->
            d.albumKey = TitleCleaner.normalize(d.album!!) + "|" + TitleCleaner.normalize(d.topFolder.orEmpty())
        }
        val keysByName = filed.groupBy { TitleCleaner.normalize(it.album!!) }
            .mapValues { (_, list) -> list.mapNotNull { it.albumKey }.distinct() }
        drafts.filter { it.album != null && it.isThemed }.forEach { d ->
            val name = TitleCleaner.normalize(d.album!!)
            d.albumKey = keysByName[name]?.singleOrNull() ?: "$name|"
        }
    }

    /** A number shared across a whole album says nothing about order. */
    private fun junkTrackNumberAlbums(drafts: List<Draft>): Set<String?> =
        drafts.filter { it.albumKey != null }
            .groupBy { it.albumKey }
            .filter { (_, list) ->
                val numbers = list.mapNotNull { it.trackNumber }
                numbers.size > 1 && numbers.distinct().size == 1
            }.keys

    private fun buildAlbums(tracks: List<Track>, drafts: List<Draft>, names: ArtistNames): List<Album> {
        val draftByKey = drafts.associateBy { it.raw.relativePath + it.raw.displayName }
        return tracks.filter { it.albumKey != null }
            .groupBy { it.albumKey!! }
            .map { (key, list) ->
                val ordered = list.sortedWith(
                    compareBy<Track, Int?>(nullsLast()) { it.trackNumber }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
                )
                val owner = list.mapNotNull { t -> draftByKey[t.key] }
                    .firstOrNull { !it.isThemed }
                    ?.topFolder
                val ownerKey = owner?.let(TitleCleaner::normalize)
                val isVarious = ownerKey == VARIOUS_KEY
                Album(
                    key = key,
                    name = list.first().album!!,
                    artist = when {
                        isVarious -> VARIOUS_ARTISTS
                        ownerKey != null -> names.display(ownerKey) ?: owner
                        else -> list.first().artist
                    },
                    artistKey = ownerKey.takeUnless { isVarious },
                    tracks = ordered,
                    year = list.mapNotNull { it.year }.maxOrNull(),
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun buildArtists(tracks: List<Track>, albums: List<Album>, names: ArtistNames): List<Artist> {
        val tracksByArtist = HashMap<String, MutableList<Track>>()
        tracks.forEach { t -> t.artistKeys.forEach { tracksByArtist.getOrPut(it) { mutableListOf() }.add(t) } }

        return tracksByArtist.mapNotNull { (key, list) ->
            val name = names.display(key) ?: return@mapNotNull null
            val own = list.mapNotNull { it.albumKey }.toSet()
            val artistAlbums = albums.filter { it.key in own }
                .sortedWith(compareByDescending<Album> { it.artistKey == key }.thenByDescending { it.year ?: 0 })
            Artist(key, name, list, artistAlbums)
        }.sortedBy { it.name.lowercase() }
    }

    /** Picks one spelling per artist across "A.R. RAHMAN", "A.R. Rahman" and the folder name. */
    private class ArtistNames {
        private val folders = HashMap<String, String>()
        private val credits = HashMap<String, MutableMap<String, Int>>()

        fun addFolder(name: String) {
            folders[TitleCleaner.normalize(name)] = name
        }

        fun addCredit(name: String) {
            val key = TitleCleaner.normalize(name)
            if (key.isEmpty() || key == VARIOUS_KEY) return
            credits.getOrPut(key) { HashMap() }.merge(name, 1, Int::plus)
        }

        fun display(key: String): String? {
            val folder = folders[key]
            // "--" is a filename stand-in for a character a folder can't hold, as in "Axwell -- Ingrosso".
            if (folder != null && "--" !in folder) return folder
            val best = credits[key]?.entries
                ?.sortedWith(
                    compareBy<Map.Entry<String, Int>> { it.key == it.key.uppercase() }
                        .thenByDescending { it.value }
                        .thenBy { it.key },
                )
                ?.firstOrNull()?.key
            return best ?: folder
        }
    }
}
