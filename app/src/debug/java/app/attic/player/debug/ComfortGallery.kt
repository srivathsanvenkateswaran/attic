package app.attic.player.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.ui.comfort.ActionBar
import app.attic.player.ui.comfort.ActionBarItem
import app.attic.player.ui.comfort.ArtShape
import app.attic.player.ui.comfort.ArtistCover
import app.attic.player.ui.comfort.Bar
import app.attic.player.ui.comfort.BrowseTile
import app.attic.player.ui.comfort.ButtonSize
import app.attic.player.ui.comfort.ButtonStyle
import app.attic.player.ui.comfort.ChipRow
import app.attic.player.ui.comfort.CollectionActions
import app.attic.player.ui.comfort.CollectionHeader
import app.attic.player.ui.comfort.CollectionPlayButton
import app.attic.player.ui.comfort.CollectionRow
import app.attic.player.ui.comfort.Comfort
import app.attic.player.ui.comfort.ComfortButton
import app.attic.player.ui.comfort.ComfortIcon
import app.attic.player.ui.comfort.ComfortIconButton
import app.attic.player.ui.comfort.ComfortMark
import app.attic.player.ui.comfort.ComfortProgress
import app.attic.player.ui.comfort.ComfortRadio
import app.attic.player.ui.comfort.ComfortShapes
import app.attic.player.ui.comfort.ComfortSurface
import app.attic.player.ui.comfort.ComfortSwitch
import app.attic.player.ui.comfort.Cover
import app.attic.player.ui.comfort.Deck
import app.attic.player.ui.comfort.DialogCard
import app.attic.player.ui.comfort.DialogField
import app.attic.player.ui.comfort.Dock
import app.attic.player.ui.comfort.EmptyState
import app.attic.player.ui.comfort.FavouritesArt
import app.attic.player.ui.comfort.FilterChip
import app.attic.player.ui.comfort.Group
import app.attic.player.ui.comfort.Hairlined
import app.attic.player.ui.comfort.HeartButton
import app.attic.player.ui.comfort.Hero
import app.attic.player.ui.comfort.IconBox
import app.attic.player.ui.comfort.IconBoxStyle
import app.attic.player.ui.comfort.IndexPop
import app.attic.player.ui.comfort.IndexScrubber
import app.attic.player.ui.comfort.Inset
import app.attic.player.ui.comfort.KeyValue
import app.attic.player.ui.comfort.Label
import app.attic.player.ui.comfort.LyricsCard
import app.attic.player.ui.comfort.LyricsLine
import app.attic.player.ui.comfort.LyricsMessage
import app.attic.player.ui.comfort.MarkBox
import app.attic.player.ui.comfort.MenuRow
import app.attic.player.ui.comfort.MiniPlayer
import app.attic.player.ui.comfort.MiniPlayerPlacement
import app.attic.player.ui.comfort.Mosaic
import app.attic.player.ui.comfort.Note
import app.attic.player.ui.comfort.NowPlayingCover
import app.attic.player.ui.comfort.NowPlayingHeader
import app.attic.player.ui.comfort.NowPlayingLyric
import app.attic.player.ui.comfort.NowPlayingTitle
import app.attic.player.ui.comfort.Pager
import app.attic.player.ui.comfort.PlayButton
import app.attic.player.ui.comfort.PlaySize
import app.attic.player.ui.comfort.RowSize
import app.attic.player.ui.comfort.SearchField
import app.attic.player.ui.comfort.SeekBar
import app.attic.player.ui.comfort.SetBlock
import app.attic.player.ui.comfort.SetBlockProgressPadding
import app.attic.player.ui.comfort.SetBlockSmall
import app.attic.player.ui.comfort.SetBlockText
import app.attic.player.ui.comfort.SetRadioRow
import app.attic.player.ui.comfort.SetSwitchRow
import app.attic.player.ui.comfort.SheetHeader
import app.attic.player.ui.comfort.SheetRow
import app.attic.player.ui.comfort.Shelf
import app.attic.player.ui.comfort.SongAddButton
import app.attic.player.ui.comfort.SongDragHandle
import app.attic.player.ui.comfort.SongLead
import app.attic.player.ui.comfort.SongMoreButton
import app.attic.player.ui.comfort.SongRadioMark
import app.attic.player.ui.comfort.SongRow
import app.attic.player.ui.comfort.Spinner
import app.attic.player.ui.comfort.StaticMenu
import app.attic.player.ui.comfort.StaticSheet
import app.attic.player.ui.comfort.Stats
import app.attic.player.ui.comfort.SubTabs
import app.attic.player.ui.comfort.SwipeToRemove
import app.attic.player.ui.comfort.TabBar
import app.attic.player.ui.comfort.TabItem
import app.attic.player.ui.comfort.Tile
import app.attic.player.ui.comfort.TileGrid
import app.attic.player.ui.comfort.Tinted
import app.attic.player.ui.comfort.Toast
import app.attic.player.ui.comfort.Toggles
import app.attic.player.ui.comfort.TonalIconButton
import app.attic.player.ui.comfort.Tools
import app.attic.player.ui.comfort.Transport
import app.attic.player.ui.comfort.onest
import app.attic.player.ui.comfort.tabular
import app.attic.player.ui.comfort.trayArt
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol

/** Comfort's sample covers and the covers.css tint each one stands for. */
private object ComfortSamples {
    val revenge = Art.Picture(Samples.revenge)
    val viva = Art.Picture(Samples.viva)
    val anbe = Art.Picture(Samples.anbe)
    val oso = Art.Picture(Samples.oso)
    val premam = Art.Picture(Samples.premam)
    val rockstar = Art.Picture(Samples.rockstar)
    val dhurandhar = Art.Picture(Samples.dhurandhar)
    val yjhd = Art.Picture(Samples.sweep(0xFFF7C948.toInt(), 0xFFE8436A.toInt()))
    val ghost = Art.Picture(Samples.glow(0xFFFF6A5A.toInt(), 0xFF7A1616.toInt(), 0xFF120404.toInt()))

    /** Five covers for the album-art colour strips, as 00-direction.html shows them, with their mid-tone tints. */
    val tinted = listOf(
        Triple("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", revenge to Color(0xFFA04E16)),
        Triple("Viva La Vida", "Coldplay", viva to Color(0xFF284A70)),
        Triple("Aaruyire", "A. R. Rahman", anbe to Color(0xFF7FA32B)),
        Triple("GHOST!", "phonk.me, KIIXSHI", ghost to Color(0xFF7A1616)),
        Triple("Gehra Hua", "Shashwat Sachdev", dhurandhar to Color(0xFF5D6470)),
    )

    val hindi = listOf(oso, yjhd, rockstar, revenge)
    val lateDrive = listOf(revenge, viva, anbe, oso)
}

private val NoInsets = WindowInsets(0, 0, 0, 0)

/**
 * The Comfort kit laid out the way oneui/00-direction.html lays out its strips. Opened by
 * KitGalleryActivity with `--es style comfort`. `section` scrolls to a strip (see ui/comfort/KIT.md).
 */
@Composable
internal fun ComfortGallery(section: Int, dark: Boolean, toggleTheme: () -> Unit) {
    val list = rememberLazyListState()
    val strips: List<Pair<String, @Composable ColumnScope.() -> Unit>> = listOf(
        "Palette" to { PaletteStrip() },
        "Type: Onest" to { TypeStrip() },
        "Shape" to { ShapeStrip() },
        "Icons and the mark" to { IconStrip() },
        "Album-art colour: Tinted" to { TintStrip(wash = false) },
        "Album-art colour: Tinted, wash" to { TintStrip(wash = true) },
        "Buttons" to { ButtonStrip() },
        "Hero, tools, sub-tabs, chips, search field" to { FilterStrip() },
        "Song row" to { SongStrip() },
        "Collections: rows and tiles" to { CollectionStrip() },
        "Dock: mini player and tab bar" to { DockStrip() },
        "Now Playing deck and seek bar" to { DeckStrip() },
        "Now Playing page, lyrics card, credits" to { NowPlayingStrip() },
        "Collection header and actions" to { HeaderStrip() },
        "Artist header, and the collapsed bar" to { ArtistStrip() },
        "Settings group, switch, radio, progress" to { SettingsStrip() },
        "Stat tiles and empty state" to { StatsStrip() },
        "Pop-over menu, dialogs, toast, action bar" to { SurfacesStrip() },
        "Bottom sheet" to { SheetStrip() },
        "Index scrubber" to { IndexStrip() },
    )
    LaunchedEffect(section) { if (section > 0) list.scrollToItem(section) }
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(Comfort.colors.ground),
        contentPadding = PaddingValues(bottom = 64.dp),
    ) {
        item { GalleryHeader(dark, toggleTheme) }
        items(strips) { (title, content) -> Strip(title, content) }
    }
}

@Composable
private fun GalleryHeader(dark: Boolean, toggleTheme: () -> Unit) {
    Row(
        Modifier.statusBarsPadding().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComfortMark(size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Comfort kit", style = Comfort.type.barTitle, color = Comfort.colors.on)
            Text(if (dark) "dark" else "light", style = Comfort.type.sub, color = Comfort.colors.on2)
        }
        ComfortButton(if (dark) "Light" else "Dark", toggleTheme, style = ButtonStyle.Tonal, size = ButtonSize.Small)
    }
}

/** A captioned strip, like a `.col` on the direction sheet. */
@Composable
private fun Strip(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
        Text(
            title,
            style = onest(12.5f, 16f, 700),
            color = Comfort.colors.accentInk,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
        )
        content()
    }
}

@Composable
private fun PaletteStrip() {
    val c = Comfort.colors
    val names = if (c.dark) {
        listOf(
            "Night · ground" to c.ground, "Loft · cards" to c.card, "Rafter · dock, sheets" to c.raised, "Slate · tracks, tonal" to c.fill,
            "Fill 2 · switch off, grab" to c.fill2, "Seam · hairlines" to c.line, "Chalk · text, icons" to c.on, "Dust · secondary text" to c.on2,
            "Ash · hints, trailing" to c.on3, "Lamplight · the accent" to c.accent, "Soot · ink on accent" to c.onAccent, "Accent ink" to c.accentInk,
            "Lamplight, tonal" to c.accentSoft, "On tonal" to c.onAccentSoft, "Ember · delete" to c.danger, "Toast" to c.toast,
        )
    } else {
        listOf(
            "Plaster · ground" to c.ground, "Paper · cards" to c.card, "Paper, raised" to c.raised, "Putty · tracks, tonal" to c.fill,
            "Fill 2 · switch off, grab" to c.fill2, "Seam · hairlines" to c.line, "Ink · text, icons" to c.on, "Dust · secondary text" to c.on2,
            "Ash · hints, trailing" to c.on3, "Lamplight · fills" to c.accent, "Soot · ink on accent" to c.onAccent, "Brass · accent as text" to c.accentInk,
            "Lamplight, tonal" to c.accentSoft, "On tonal" to c.onAccentSoft, "Ember · delete" to c.danger, "Toast" to c.toast,
        )
    }
    Group(pad = true) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                names.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        pair.forEach { (name, color) ->
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color).border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(12.dp)))
                                Column {
                                    Text(name, style = onest(12.5f, 16f, 600), color = c.on, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("#%06X".format(color.toArgb() and 0xFFFFFF), style = onest(11.5f, 15f).tabular(), color = c.on2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeStrip() {
    val t = Comfort.type
    val c = Comfort.colors
    @Composable
    fun Specimen(caption: String, sample: String, style: androidx.compose.ui.text.TextStyle) {
        Column {
            Text(caption, style = onest(11.5f, 15f), color = c.on3)
            Text(sample, style = style, color = c.on, maxLines = 2)
        }
    }
    Group(pad = true) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Specimen("Display · 40/46 · 700 · expanded title", "Library", t.hero)
                Specimen("Title L · 26/32 · 700 · song and collection titles", "Jaan Se Guzarte Hain", t.title)
                Specimen("Title M · 20/26 · 700 · collapsed bar, sheet and dialog titles", "Add to playlist", t.barTitle)
                Specimen("Row · 16/22 · 500 · row titles", "Kun Faya Kun", t.rowTitle)
                Specimen("Body · 15/21 · 400 · dialog and empty-state text", "The playlist goes away. The songs stay in your library.", t.body)
                Specimen("Sub · 13.5/18 · 400 · second line of a row", "A. R. Rahman, Javed Ali, Mohit Chauhan", t.sub)
                Specimen("Label · 14/18 · 600 · section labels, buttons at 15.5", "Recently played", t.label)
                Specimen("Numerals · tabular in times and counts", "1:47   5:31   18,460   214 plays", onest(20f, 26f, 700).tabular())
            }
        }
    }
}

@Composable
private fun Caption(text: String) = Text(text, style = onest(11.5f, 15f), color = Comfort.colors.on2, modifier = Modifier.padding(top = 7.dp))

@Composable
private fun ShapeStrip() {
    val c = Comfort.colors
    Group(pad = true) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                    Column { Cover(ComfortSamples.revenge, size = 48.dp); Caption("48") }
                    Column { Cover(ComfortSamples.premam, size = 80.dp); Caption("80 · sq") }
                    Column { ArtistCover(ComfortSamples.anbe, size = 80.dp); Caption("80 · sq-soft") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                    Column { Cover(ComfortSamples.viva, size = 132.dp); Caption("132 · sq-lg") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                    Column { IconBox(Symbol.Favorite, size = 48.dp, filled = true); Caption("icon box") }
                    Column { TonalIconButton(Symbol.Shuffle, "Shuffle", {}); Caption("toggle") }
                    Column { TonalIconButton(Symbol.Shuffle, "Shuffle", {}, checked = true); Caption("toggle, on") }
                    Column { PlayButton(false, {}, size = PlaySize.Small); Caption("play") }
                    Column { MarkBox(size = 48.dp, style = IconBoxStyle.Solid); Caption("the mark") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(ComfortShapes.Floating to "30 · floating", ComfortShapes.Group to "26 · group", ComfortShapes.Control to "18 · control").forEach { (shape, label) ->
                        Column { Box(Modifier.size(96.dp, 64.dp).clip(shape).background(c.fill)); Caption(label) }
                    }
                }
                Column { Box(Modifier.size(96.dp, 40.dp).clip(ComfortShapes.Pill).background(c.fill)); Caption("pill · button, chip") }
            }
        }
    }
}

@Composable
private fun IconStrip() {
    val c = Comfort.colors
    val names = listOf(
        "home", "search", "library_music", "settings", "favorite", "more_vert", "chevron_right", "arrow_back", "expand_more", "play_arrow",
        "pause", "skip_previous", "skip_next", "shuffle", "repeat", "repeat_one", "queue_music", "sensors", "lyrics", "playlist_add",
        "playlist_play", "add", "check", "close", "drag_handle", "swap_vert", "sort", "grid_view", "view_list", "edit",
        "delete", "language", "album", "person", "do_not_disturb_on", "download", "ios_share", "folder_open", "music_note", "graphic_eq",
    )
    Group(pad = true) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                names.chunked(5).forEach { row ->
                    Row {
                        row.forEach { name ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                ComfortIcon(Symbol.named(name)!!, tint = c.on)
                                Text(name, style = onest(9.5f, 12f), color = c.on3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    MarkBox(size = 72.dp, style = IconBoxStyle.Solid)
                    MarkBox(size = 72.dp, style = IconBoxStyle.Tonal)
                    MarkBox(size = 72.dp, style = IconBoxStyle.Neutral)
                    ComfortMark(size = 56.dp)
                }
            }
        }
    }
}

/** 00-direction.html's album-art colour cards: the same markup under five covers. */
@Composable
private fun TintStrip(wash: Boolean) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ComfortSamples.tinted.forEach { (title, artist, cover) ->
            val (art, seed) = cover
            Tinted(seed, wash = wash) {
                TintCard(title, artist, art)
            }
        }
    }
}

@Composable
private fun TintCard(title: String, artist: String, art: Art) {
    var favourite by remember { mutableStateOf(true) }
    var shuffle by remember { mutableStateOf(true) }
    var autoplay by remember { mutableStateOf(true) }
    ComfortSurface(Modifier.fillMaxWidth(), shape = ComfortShapes.Group) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cover(art, size = 44.dp)
                SeekBar({ 0.323f }, "1:47", "5:31", {}, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                PlayButton(true, {}, size = PlaySize.Small)
                TonalIconButton(Symbol.Shuffle, "Shuffle", { shuffle = !shuffle }, checked = shuffle)
                HeartButton(favourite, { favourite = !favourite }, tonal = true, large = false)
                ComfortSwitch(autoplay, { autoplay = it })
            }
            Box(Modifier.fillMaxWidth().clip(ComfortShapes.Group).background(Comfort.colors.card).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Column {
                    Text("The window lamp stays lit", style = onest(16f, 22f, 700), color = Comfort.colors.on)
                    Text("We cross on borrowed time", style = onest(16f, 22f, 700), color = Comfort.colors.on3)
                }
            }
            MiniPlayer(
                title, artist, art, { 0.323f }, playing = true, favourite = false,
                onToggleFavourite = {}, onPlayPause = {}, onClick = {}, placement = MiniPlayerPlacement.Alone,
            )
        }
    }
}

@Composable
private fun ButtonStrip() {
    var favourite by remember { mutableStateOf(true) }
    Group(pad = true) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ComfortButton("Play", {}, leading = Symbol.PlayArrow, leadingFilled = true)
                    ComfortButton("Open lyrics", {}, style = ButtonStyle.Tonal)
                }
                ComfortButton("Shuffle", {}, style = ButtonStyle.Plain, leading = Symbol.Shuffle)
                ComfortButton("Download lyrics for all songs", {}, style = ButtonStyle.Text)
                ComfortButton("Delete playlist", {}, style = ButtonStyle.Text, danger = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ComfortButton("Allow access", {}, size = ButtonSize.Large)
                    ComfortButton("Open lyrics", {}, style = ButtonStyle.Tonal, size = ButtonSize.Small)
                }
                ComfortButton("Create", {}, enabled = false)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ComfortIconButton(Symbol.MoreVert, "More options", {})
                    HeartButton(false, {})
                    HeartButton(favourite, { favourite = !favourite })
                    TonalIconButton(Symbol.PlaylistAdd, "Add to a playlist", {})
                    TonalIconButton(Symbol.Shuffle, "Shuffle", {}, checked = true)
                    PlayButton(false, {}, size = PlaySize.Medium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayButton(true, {}, size = PlaySize.Small)
                    PlayButton(true, {}, size = PlaySize.Large)
                }
            }
        }
    }
}

@Composable
private fun FilterStrip() {
    var tab by remember { mutableStateOf(0) }
    var albums by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("dhur") }
    Hero("Library", subtitle = "127 songs on this phone", insets = NoInsets)
    Tools(
        leading = { SubTabs(listOf("All", "Albums", "Artists", "Folders"), tab, { tab = it }) },
    ) {
        ComfortIconButton(Symbol.Settings, "Settings", {})
    }
    ChipRow {
        FilterChip("Playlists", false, {})
        FilterChip("Albums", albums, { albums = !albums })
        FilterChip("Artists", false, {})
        FilterChip("Songs", false, {})
    }
    Spacer(Modifier.height(8.dp))
    SearchField("", {}, "Search your music")
    Spacer(Modifier.height(12.dp))
    SearchField(query, { query = it }, "Search your music")
    Spacer(Modifier.height(12.dp))
    Hero("Home", subtitle = "127 songs on this phone", short = true, mark = true, insets = NoInsets)
}

@Composable
private fun SongStrip() {
    var added by remember { mutableStateOf(true) }
    Label("With a cover, the current song, and trailing variants", tight = true)
    Group {
        item { SongRow("Aari Aari", "Bombay Rockers, Khan Saab, Jasmine Sandlas, Reble", lead = SongLead.Cover(ComfortSamples.revenge), onClick = {}) { SongMoreButton("Aari Aari", {}) } }
        item { SongRow("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", lead = SongLead.Cover(ComfortSamples.revenge), current = true, playing = true, onClick = {}) { SongMoreButton("Jaan Se Guzarte Hain", {}) } }
        item { SongRow("Phir Se", "Arijit Singh", lead = SongLead.Cover(ComfortSamples.revenge)) { SongDragHandle("Phir Se") } }
        item { SongRow("Kun Faya Kun", "A. R. Rahman, Javed Ali, Mohit Chauhan", lead = SongLead.Cover(ComfortSamples.rockstar)) { SongAddButton("Kun Faya Kun", false, {}) } }
        item { SongRow("Malare", "Vijay Yesudas", lead = SongLead.Cover(ComfortSamples.premam)) { SongAddButton("Malare", added, { added = !added }) } }
    }
    Label("Numbered, for an album")
    Group {
        item { SongRow("Main Aur Tu", "Jasmine Sandlas, Reble, Shashwat Sachdev", lead = SongLead.Number(2), meta = "4:05", onClick = {}) { SongMoreButton("Main Aur Tu", {}) } }
        item { SongRow("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", lead = SongLead.Number(3), meta = "5:31", current = true, playing = false, onClick = {}) { SongMoreButton("Jaan Se Guzarte Hain", {}) } }
        item { SongRow("Udd Gaye", "Ritviz", onClick = {}) { SongMoreButton("Udd Gaye", {}) } }
    }
    Label("Rank and play count, for Recap")
    Group {
        item { SongRow("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", lead = SongLead.Cover(ComfortSamples.revenge), rank = 1, meta = "214 plays") }
        item { SongRow("Kun Faya Kun", "A. R. Rahman, Javed Ali, Mohit Chauhan", lead = SongLead.Cover(ComfortSamples.rockstar), rank = 2, meta = "187 plays") }
    }
    Label("Queue: lifted while dragged, swiped aside")
    Group {
        item { SongRow("Aakhri Ishq", "Jubin Nautiyal", lead = SongLead.Cover(ComfortSamples.revenge)) { SongDragHandle("Aakhri Ishq") } }
        item { SongRow("Vaari Jaavan", "Jyoti Nooran, Jasmine Sandlas, Reble", lead = SongLead.Cover(ComfortSamples.revenge), lifted = true) { SongDragHandle("Vaari Jaavan") } }
        item { SwipeToRemove({}) { SongRow("Phir Se", "Arijit Singh", lead = SongLead.Cover(ComfortSamples.revenge)) { SongDragHandle("Phir Se") } } }
    }
    Label("Instant mixes: a tap starts radio")
    Pager(3) { page ->
        listOf(
            Triple("Kun Faya Kun", "A. R. Rahman", ComfortSamples.rockstar),
            Triple("Viva La Vida", "Coldplay", ComfortSamples.viva),
            Triple("Malare", "Vijay Yesudas", ComfortSamples.premam),
        ).forEach { (title, artist, art) ->
            item { SongRow(title, artist, lead = SongLead.Cover(art), onClick = {}) { SongRadioMark() } }
        }
    }
}

@Composable
private fun CollectionStrip() {
    Group {
        item { CollectionRow("Favourites", "Playlist, 31 songs", { m -> FavouritesArt(m) }, {}) }
        item { CollectionRow("Late drive", "Playlist, 12 songs", { m -> Mosaic(ComfortSamples.lateDrive, m) }, {}) }
        item { CollectionRow("Hindi", "Playlist, 46 songs", { m -> Mosaic(ComfortSamples.hindi, m) }, {}) }
        item { CollectionRow("A. R. Rahman", "Artist", { m -> ArtistCover(ComfortSamples.anbe, m) }, {}) }
        item { CollectionRow("Rockstar", "Album, A. R. Rahman", { m -> Cover(ComfortSamples.rockstar, m) }, {}, chevron = true) }
        item { CollectionRow("A. R. Rahman", "3,120 min", { m -> ArtistCover(ComfortSamples.anbe, m) }, {}, size = RowSize.Small, value = "1") }
    }
    Label("Tiles: shelf item, grid tile, person, browse tile")
    Shelf {
        item { Tile("Yeh Jawaani Hai Deewani", {}, { m -> Cover(ComfortSamples.yjhd, m) }, subtitle = "Pritam", width = 96.dp) }
        item { Tile("Jaan Se Guzarte Hain", {}, { m -> Cover(ComfortSamples.revenge, m) }, subtitle = "Khan Saab, Shashwat Sachdev", width = 96.dp, now = true, playing = true) }
        item { Tile("Coldplay", {}, { m -> ArtistCover(ComfortSamples.viva, m) }, width = 96.dp, centered = true) }
        item { Tile("Premam", {}, { m -> Cover(ComfortSamples.premam, m) }, subtitle = "Rajesh Murugesan", width = 96.dp) }
    }
    Spacer(Modifier.height(16.dp))
    TileGrid(2, 2) { index ->
        if (index == 0) {
            BrowseTile("Hindi", {}, { m -> Mosaic(ComfortSamples.hindi, m) }, subtitle = "46 songs")
        } else {
            BrowseTile("Rockstar", {}, { m -> Cover(ComfortSamples.rockstar, m) }, subtitle = "A. R. Rahman", seed = Color(0xFFA62A2A))
        }
    }
    Label("A tray in a group: three across, and four")
    Group(pad = true) {
        item {
            TileGrid(3, 3, tray = true) { index ->
                val (title, art) = listOf("Favourites" to null, "Premam" to ComfortSamples.premam, "Hindi" to ComfortSamples.oso)[index]
                Tile(title, {}, { m -> if (art == null) FavouritesArt(m) else Cover(art, m) }, width = null, centered = true, twoLines = true, artSize = trayArt(3))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Group(pad = true) {
        item {
            TileGrid(4, 4, tray = true) { index ->
                val (title, arts) = listOf("English" to listOf(ComfortSamples.viva), "Hindi" to ComfortSamples.hindi, "Tamil" to listOf(ComfortSamples.anbe), "Phonk" to listOf(ComfortSamples.ghost))[index]
                Tile(title, {}, { m -> Mosaic(arts, m) }, width = null, centered = true, artSize = trayArt(4))
            }
        }
    }
}

private val Tabs = listOf(TabItem("Home", Symbol.Home), TabItem("Search", Symbol.Search), TabItem("Library", Symbol.LibraryMusic))

@Composable
private fun DockStrip() {
    var favourite by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(0) }
    Dock(
        mini = {
            MiniPlayer(
                "Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", ComfortSamples.revenge, { 0.323f },
                playing = playing, favourite = favourite, onToggleFavourite = { favourite = !favourite },
                onPlayPause = { playing = !playing }, onClick = {}, seed = Color(0xFFA04E16),
            )
        },
        insets = NoInsets,
    ) { TabBar(Tabs, tab, { tab = it }) }
    Dock(insets = NoInsets) { TabBar(Tabs, 1, {}) }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.padding(horizontal = 12.dp)) {
        MiniPlayer(
            "Viva La Vida", "Coldplay", ComfortSamples.viva, { 0.6f }, playing = false, favourite = true,
            onToggleFavourite = {}, onPlayPause = {}, onClick = {}, seed = Color(0xFF284A70), placement = MiniPlayerPlacement.Alone,
        )
    }
}

@Composable
private fun DeckStrip() {
    var shuffle by remember { mutableStateOf(true) }
    var favourite by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0.323f) }
    Tinted(Color(0xFFA04E16), wash = true) {
        ComfortSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(bottom = 16.dp)) {
                NowPlayingHeader("Dhurandhar: The Revenge", {}, {}, "More options for Jaan Se Guzarte Hain", insets = NoInsets)
                Deck {
                    SeekBar({ progress }, "1:47", "5:31", { progress = it })
                    Transport(playing, {}, { playing = !playing }, {})
                    Toggles {
                        TonalIconButton(Symbol.Shuffle, "Shuffle", { shuffle = !shuffle }, checked = shuffle, large = true)
                        TonalIconButton(Symbol.Repeat, "Repeat", {}, large = true)
                        HeartButton(favourite, { favourite = !favourite }, tonal = true)
                        TonalIconButton(Symbol.QueueMusic, "Open queue", {}, large = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingStrip() {
    Tinted(Color(0xFFA04E16), wash = true) {
        ComfortSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(bottom = 16.dp)) {
                NowPlayingCover(ComfortSamples.revenge, paused = false)
                NowPlayingTitle("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev")
                NowPlayingLyric("The window lamp stays lit")
                Spacer(Modifier.height(16.dp))
                LyricsCard {
                    LyricsLine("The window lamp stays lit", current = true)
                    LyricsLine("We cross on borrowed time")
                    LyricsLine("Under the roof the rain keeps")
                    ComfortButton("Open lyrics", {}, style = ButtonStyle.Tonal, size = ButtonSize.Small, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                LyricsCard {
                    LyricsMessage("Lyrics for this song are not on your phone yet.")
                    Spinner(Modifier.padding(top = 14.dp), small = true)
                }
                Label("About the artist")
                Group { item { CollectionRow("Khan Saab", "3 songs in your library", { m -> ArtistCover(ComfortSamples.revenge, m) }, {}, size = RowSize.Large, chevron = true) } }
                Label("Credits")
                Group {
                    item { KeyValue("Performed by", "Khan Saab, Shashwat Sachdev") }
                    item { KeyValue("Album", "Dhurandhar: The Revenge") }
                    item { KeyValue("Year", "2026") }
                }
                Spacer(Modifier.height(12.dp))
                MiniPlayer(
                    "Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", ComfortSamples.revenge, { 0.323f }, playing = true, favourite = true,
                    onToggleFavourite = {}, onPlayPause = {}, onClick = {}, seed = Color(0xFFA04E16), placement = MiniPlayerPlacement.Top,
                )
            }
        }
    }
}

@Composable
private fun HeaderStrip() {
    Bar("", onBack = {}, insets = NoInsets) { ComfortIconButton(Symbol.MoreVert, "Playlist options", {}) }
    CollectionHeader(
        "Dhurandhar: The Revenge",
        { m -> Cover(ComfortSamples.revenge, m) },
        byline = "Shashwat Sachdev",
        onBylineClick = {},
        meta = "Album • 2026",
    )
    CollectionActions {
        CollectionPlayButton(false, {}, Modifier.weight(1f))
        TonalIconButton(Symbol.Shuffle, "Shuffle Dhurandhar: The Revenge", {}, large = true)
        TonalIconButton(Symbol.PlaylistAdd, "Add Dhurandhar: The Revenge to a playlist", {}, large = true)
    }
}

@Composable
private fun ArtistStrip() {
    Bar("", onBack = {}, insets = NoInsets)
    CollectionHeader("A. R. Rahman", { m -> ArtistCover(ComfortSamples.anbe, m) }, meta = "14 songs, 1 hr 13 min", person = true)
    CollectionActions {
        CollectionPlayButton(true, {}, Modifier.weight(1f))
        TonalIconButton(Symbol.Shuffle, "Shuffle A. R. Rahman", {}, checked = true, large = true)
        TonalIconButton(Symbol.PlaylistAdd, "Add A. R. Rahman to a playlist", {}, large = true)
    }
    Spacer(Modifier.height(12.dp))
    Bar("A. R. Rahman", onBack = {}, lift = true, insets = NoInsets) { ComfortIconButton(Symbol.MoreVert, "More options", {}) }
    Bar("2 selected", onBack = {}, backSymbol = Symbol.Close, accentTitle = true, insets = NoInsets)
    SubTabs(listOf("This week", "This month", "This year", "All time"), 2, {}, lift = true)
}

@Composable
private fun SettingsStrip() {
    var theme by remember { mutableStateOf(1) }
    var autoplay by remember { mutableStateOf(true) }
    Label("Appearance", tight = true)
    Group {
        listOf("Match phone", "Dark", "Light").forEachIndexed { index, name ->
            item { SetRadioRow(name, theme == index, { theme = index }) }
        }
    }
    Label("Song radio")
    Group {
        item { SetSwitchRow("Autoplay", autoplay, { autoplay = it }, summary = "When a queue reaches its last song, radio keeps going with similar songs.") }
        item {
            SetBlock(afterRow = true) {
                SetBlockText("Radio finds similar songs by listening to your files. This happens on this phone, and nothing is uploaded.")
                ComfortProgress({ 0.66f }, SetBlockProgressPadding)
                SetBlockSmall("Analysing songs: 84 of 127")
                ComfortButton("Analyse now without a charger", {}, style = ButtonStyle.Text, modifier = Modifier.padding(top = 12.dp).offset(x = (-12).dp))
            }
        }
    }
    Note("Small print under a group.")
    Spacer(Modifier.height(18.dp))
    Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ComfortSwitch(false, {})
        ComfortSwitch(true, {})
        ComfortRadio(false, {})
        ComfortRadio(true, {})
        Spinner()
        Spinner(small = true)
        ComfortProgress({ 0.4f }, Modifier.width(64.dp))
    }
    Spacer(Modifier.height(12.dp))
    ComfortProgress({ 0.66f }, Modifier.padding(horizontal = 24.dp), thick = true)
}

@Composable
private fun StatsStrip() {
    Group {
        item {
            Stats(
                listOf("18,460" to "Minutes", "6,212" to "Plays", "412" to "Songs", "96" to "Artists", "251" to "Days with music", "34 days" to "Longest streak"),
            )
        }
    }
    EmptyState("No results for \"zzz\"", body = "Check the spelling, or try fewer words.", symbol = Symbol.Search)
    EmptyState(
        "Your library is empty",
        body = "Put music in the Music folder on this phone.",
        art = { MarkBox(size = 76.dp, style = IconBoxStyle.Neutral) },
        action = { ComfortButton("Allow access", {}) },
    )
}

@Composable
private fun SurfacesStrip() {
    var name by remember { mutableStateOf("Rahman, slow") }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StaticMenu(Modifier.fillMaxWidth()) {
            MenuRow(Symbol.Edit, "Rename", {})
            MenuRow(Symbol.Delete, "Delete playlist", {}, danger = true)
        }
        StaticMenu {
            MenuRow(null, "Title", {}, selected = true)
            MenuRow(null, "Artist", {})
            MenuRow(null, "Recently added", {})
        }
        DialogCard("Delete Late drive?", "The playlist goes away. The songs stay in your library.", "Delete playlist", {}, "Cancel", {}, danger = true)
        DialogCard("Name this playlist", null, "Create", {}, "Cancel", {}, confirmEnabled = name.isNotBlank()) {
            DialogField(name, { name = it }, "Playlist name")
        }
        Toast("Added to Late drive")
    }
    ActionBar(Modifier.padding(top = 16.dp), insets = NoInsets) {
        ActionBarItem(Symbol.Add, "Add songs", {})
        ActionBarItem(Symbol.Check, "Done", {}, go = true)
    }
}

@Composable
private fun SheetStrip() {
    StaticSheet {
        SheetHeader("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", { m -> Cover(ComfortSamples.revenge, m) })
        Hairlined {
            item { SheetRow(Symbol.Sensors, "Start radio", {}) }
            item { SheetRow(Symbol.Favorite, "Add to favourites", {}) }
            item { SheetRow(Symbol.PlaylistPlay, "Play next", {}) }
            item { SheetRow(Symbol.Language, "Language: Hindi", {}, value = "Change") }
            item { SheetRow(Symbol.Language, "Hindi", {}, selected = true) }
            item { SheetRow(Symbol.DoNotDisturbOn, "Remove from this playlist", {}, danger = true) }
        }
        Spacer(Modifier.height(8.dp))
        SearchField("", {}, "Search your music")
        Hairlined {
            item { SongRow("Kun Faya Kun", "A. R. Rahman, Javed Ali, Mohit Chauhan", lead = SongLead.Cover(ComfortSamples.rockstar)) { SongAddButton("Kun Faya Kun", true, {}) } }
            item { SongRow("Malare", "Vijay Yesudas", lead = SongLead.Cover(ComfortSamples.premam)) { SongAddButton("Malare", false, {}) } }
        }
    }
}

@Composable
private fun IndexStrip() {
    val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".map { it.toString() }
    Box(Modifier.fillMaxWidth().height(440.dp)) {
        Column(Modifier.padding(end = 28.dp)) {
            Group {
                listOf("Kabira", "Khwaja Mere Khwaja", "Kun Faya Kun", "Lost!", "Main Aur Tu", "Malare").forEach { title ->
                    item { SongRow(title, "A. R. Rahman", lead = SongLead.Cover(ComfortSamples.rockstar)) { SongMoreButton(title, {}) } }
                }
            }
        }
        IndexScrubber(letters, 10, {}, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 1.dp))
        IndexPop("K", Modifier.align(Alignment.TopEnd).padding(end = 34.dp).offset(y = 8.dp + 15.5.dp * 10 - 20.dp))
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.padding(horizontal = 16.dp)) { Cover(null, size = 56.dp, shape = ArtShape) }
}
