package app.attic.player.data

/**
 * Turns MediaStore rows into the albums, artists and folder playlists the app shows. Folder meaning
 * is read below each song's library root, which [LibraryScope.roots] picks.
 */
object LibraryBuilder {
    private const val VARIOUS_ARTISTS = "Various Artists"
    private const val UNKNOWN_ARTIST = "Unknown artist"
    private val VARIOUS_KEY = TitleCleaner.normalize(VARIOUS_ARTISTS)

    private class Draft(
        val raw: RawTrack,
        val topFolder: String?,
        val kind: FolderKind?,
        val title: String,
        val artist: String,
        val credits: List<String>,
        val album: String?,
        val trackNumber: Int?,
        /** Who the album belongs to: the album artist tag, else the artist or Various Artists folder. */
        val owner: String?,
    ) {
        val isPlaylist get() = kind == FolderKind.Playlist
        val ownerKey get() = owner?.let(TitleCleaner::normalize)
        var albumKey: String? = null
    }

    /** [root] fixes one library root for every row; null picks one per song with [LibraryScope.roots]. */
    fun build(rows: List<RawTrack>, root: String? = null): Library {
        // A key is a path relative to its volume, so the same path on two volumes would collide.
        // Keep the primary volume's copy, which is the one a key pointed at before SD cards counted.
        val unique = rows.sortedByDescending { it.volume == RawTrack.PRIMARY_VOLUME }
            .distinctBy { it.relativePath + it.displayName }
        val roots = if (root == null) LibraryScope.roots(unique) else null
        val paths = unique.associateWith { LibraryScope.pathInRoot(it.relativePath, root ?: roots!!.getValue(it)) }
        // A folder's kind depends on all of its songs, so it is settled before any one song is read.
        val kinds = unique.filter { paths.getValue(it).isNotEmpty() }
            .groupBy { paths.getValue(it).first() }
            .mapValues { (name, rows) -> LibraryScope.folderKind(name, rows) }
        val drafts = unique.map { raw ->
            val pathParts = paths.getValue(raw)
            draft(raw, pathParts, pathParts.firstOrNull()?.let(kinds::getValue))
        }

        // Folder names are the most reliable spelling of an artist, so they seed the name table.
        val names = ArtistNames()
        drafts.filter { it.kind == FolderKind.Artist }
            .mapNotNull { it.topFolder }
            .distinct()
            .forEach(names::addFolder)
        drafts.forEach { d -> d.credits.forEach(names::addCredit) }
        drafts.forEach { d -> names.ownerCredits(d.raw.albumArtist).forEach(names::addAlbumArtist) }

        assignAlbumKeys(drafts)
        val junkNumbers = junkTrackNumberAlbums(drafts)

        val tracks = drafts.map { d ->
            val creditKeys = d.credits.map(TitleCleaner::normalize)
            val folderKey = d.topFolder.takeIf { d.kind == FolderKind.Artist }?.let(TitleCleaner::normalize)
            val albumArtistKeys = names.ownerCredits(d.raw.albumArtist).map(TitleCleaner::normalize)
            val artistKeys = (listOfNotNull(folderKey) + creditKeys + albumArtistKeys)
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
                genre = TitleCleaner.genre(d.raw.genre),
                composer = LibraryScope.tagOrNull(d.raw.composer)?.let(TitleCleaner::dedupeCredits),
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

        val albums = buildAlbums(tracks, drafts, names)
        val artists = buildArtists(tracks, albums, names)
        val playlistKeys = drafts.filter { it.isPlaylist }.map { it.raw.relativePath + it.raw.displayName }.toSet()
        val folders = tracks.filter { it.key in playlistKeys }
            .groupBy { it.folder!! }
            .map { (name, list) -> FolderPlaylist(name, list) }
            .sortedBy { it.name }

        return Library(tracks, albums, artists, folders)
    }

    private fun draft(raw: RawTrack, pathParts: List<String>, kind: FolderKind?): Draft {
        val topFolder = pathParts.firstOrNull()
        val isPlaylist = kind == FolderKind.Playlist

        val tagTitleRaw = raw.title?.trim()?.takeIf { it.isNotEmpty() }?.let(TitleCleaner::stripYoutubeId)
        val tagArtist = LibraryScope.tagOrNull(raw.artist)

        // MediaStore fills a missing album with the parent folder's name, so that is not a real tag.
        val parentFolder = pathParts.lastOrNull() ?: raw.relativePath.trim('/').substringAfterLast('/')
        val tagAlbum = LibraryScope.tagOrNull(raw.album)
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
        val folderArtist = topFolder.takeIf { kind == FolderKind.Artist }

        val title: String
        val artist: String
        val credits: List<String>
        if (album == null && isPlaylist) {
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

        val albumArtist = LibraryScope.tagOrNull(raw.albumArtist)
        val owner = albumArtist ?: topFolder.takeIf { kind == FolderKind.Artist || kind == FolderKind.Various }

        return Draft(raw, topFolder, kind, title, artist, credits, album, trackNumber, owner)
    }

    /**
     * Same album name under one owner is one album, where the owner is the album artist tag or else
     * the artist folder. The album artist comes first so a compilation filed under each performer's
     * folder stays one album. A song with no owner, such as a single in a playlist folder, joins the
     * one owned album of that name, so "Yeh Jawaani Hai Deewani" in Hindi/ merges with the full album.
     */
    private fun assignAlbumKeys(drafts: List<Draft>) {
        val filed = drafts.filter { it.album != null && it.owner != null }
        filed.forEach { d ->
            d.albumKey = TitleCleaner.normalize(d.album!!) + "|" + d.ownerKey
        }
        val keysByName = filed.groupBy { TitleCleaner.normalize(it.album!!) }
            .mapValues { (_, list) -> list.mapNotNull { it.albumKey }.distinct() }
        drafts.filter { it.album != null && it.owner == null }.forEach { d ->
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
                    .firstNotNullOfOrNull { it.owner }
                val ownerKey = owner?.let(TitleCleaner::normalize)
                val isVarious = ownerKey == VARIOUS_KEY
                // "Pritam, Arijit Singh" as album artist: the album shows both and opens Pritam's page.
                val ownerCredits = names.ownerCredits(owner)
                val leadKey = ownerCredits.firstOrNull()?.let(TitleCleaner::normalize)
                Album(
                    key = key,
                    name = list.first().album!!,
                    artist = when {
                        isVarious -> VARIOUS_ARTISTS
                        ownerCredits.size == 1 -> names.display(leadKey!!) ?: ownerCredits.single()
                        owner != null -> TitleCleaner.dedupeCredits(owner)
                        else -> list.first().artist
                    },
                    artistKey = leadKey.takeUnless { isVarious },
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
        private val albumArtists = HashMap<String, String>()

        fun addFolder(name: String) {
            folders[TitleCleaner.normalize(name)] = name
        }

        /**
         * The artists an album artist tag or owner folder names. A name an artist folder already
         * spells stays whole, so a "Simon & Garfunkel" folder isn't split in two. Various Artists
         * names nobody.
         */
        fun ownerCredits(owner: String?): List<String> {
            val name = LibraryScope.tagOrNull(owner) ?: return emptyList()
            val credits = if (TitleCleaner.normalize(name) in folders) listOf(name) else TitleCleaner.splitCredits(name)
            return credits.filter { TitleCleaner.normalize(it) != VARIOUS_KEY }
        }

        fun addCredit(name: String) {
            val key = TitleCleaner.normalize(name)
            if (key.isEmpty() || key == VARIOUS_KEY) return
            credits.getOrPut(key) { HashMap() }.merge(name, 1, Int::plus)
        }

        /** Only names an artist no folder or song credit spells, such as a soundtrack's composer. */
        fun addAlbumArtist(name: String) {
            albumArtists.putIfAbsent(TitleCleaner.normalize(name), name)
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
            return best ?: folder ?: albumArtists[key]
        }
    }
}
