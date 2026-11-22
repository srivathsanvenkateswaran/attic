package app.attic.player.debug

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.AtticTheme
import app.attic.player.ui.LocalIsDark
import app.attic.player.ui.LocalUiStyle
import app.attic.player.ui.expressive.AppBarSurface
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.ArtistCover
import app.attic.player.ui.expressive.AtticMarkIcon
import app.attic.player.ui.expressive.AtticWordmark
import app.attic.player.ui.expressive.BrowseTile
import app.attic.player.ui.expressive.ButtonGroup
import app.attic.player.ui.expressive.ButtonSize
import app.attic.player.ui.expressive.ButtonVariant
import app.attic.player.ui.expressive.ChipRow
import app.attic.player.ui.expressive.CircularProgress
import app.attic.player.ui.expressive.CollectionRow
import app.attic.player.ui.expressive.ConnectedButtonGroup
import app.attic.player.ui.expressive.CookieShape
import app.attic.player.ui.expressive.Cover
import app.attic.player.ui.expressive.DetailAppBar
import app.attic.player.ui.expressive.DialogTextField
import app.attic.player.ui.expressive.EmptyState
import app.attic.player.ui.expressive.Expressive
import app.attic.player.ui.expressive.ExpressiveButton
import app.attic.player.ui.expressive.ExpressiveDialog
import app.attic.player.ui.expressive.ExpressiveExtendedFab
import app.attic.player.ui.expressive.ExpressiveFab
import app.attic.player.ui.expressive.ExpressiveFilterChip
import app.attic.player.ui.expressive.ExpressiveIconButton
import app.attic.player.ui.expressive.ExpressiveMenu
import app.attic.player.ui.expressive.ExpressiveMenuItem
import app.attic.player.ui.expressive.ExpressiveNavigationBar
import app.attic.player.ui.expressive.ExpressiveShapes
import app.attic.player.ui.expressive.ExpressiveSheet
import app.attic.player.ui.expressive.ExpressiveToggleButton
import app.attic.player.ui.expressive.FavouritesArt
import app.attic.player.ui.expressive.FloatingToolbar
import app.attic.player.ui.expressive.FromArt
import app.attic.player.ui.expressive.HeartButton
import app.attic.player.ui.expressive.HeroCaption
import app.attic.player.ui.expressive.HeroCarousel
import app.attic.player.ui.expressive.FavouritesPill
import app.attic.player.ui.expressive.IconButtonVariant
import app.attic.player.ui.expressive.IconButtonWidth
import app.attic.player.ui.expressive.InfoCard
import app.attic.player.ui.expressive.InstantMixPill
import app.attic.player.ui.expressive.InstantMixes
import app.attic.player.ui.expressive.KeyValue
import app.attic.player.ui.expressive.LinearProgress
import app.attic.player.ui.expressive.ListGroup
import app.attic.player.ui.expressive.ListLabel
import app.attic.player.ui.expressive.LoadingIndicator
import app.attic.player.ui.expressive.LyricLine
import app.attic.player.ui.expressive.LyricState
import app.attic.player.ui.expressive.Mosaic
import app.attic.player.ui.expressive.NarrowingText
import app.attic.player.ui.expressive.NavItem
import app.attic.player.ui.expressive.NoArt
import app.attic.player.ui.expressive.NowPlayingWave
import app.attic.player.ui.expressive.PlayButton
import app.attic.player.ui.expressive.PlaySize
import app.attic.player.ui.expressive.RecapCard
import app.attic.player.ui.expressive.RootAppBar
import app.attic.player.ui.expressive.RowAddToggle
import app.attic.player.ui.expressive.RowCount
import app.attic.player.ui.expressive.RowDragHandle
import app.attic.player.ui.expressive.RowLead
import app.attic.player.ui.expressive.RowMoreButton
import app.attic.player.ui.expressive.SearchField
import app.attic.player.ui.expressive.SectionHeader
import app.attic.player.ui.expressive.SeekBar
import app.attic.player.ui.expressive.SeekTimes
import app.attic.player.ui.expressive.SetNote
import app.attic.player.ui.expressive.SetNoteStatus
import app.attic.player.ui.expressive.SetRadioRow
import app.attic.player.ui.expressive.SetSwitchRow
import app.attic.player.ui.expressive.SettingsSection
import app.attic.player.ui.expressive.Shelf
import app.attic.player.ui.expressive.SheetDivider
import app.attic.player.ui.expressive.SheetHeader
import app.attic.player.ui.expressive.SheetRow
import app.attic.player.ui.expressive.SongRow
import app.attic.player.ui.expressive.SplitButton
import app.attic.player.ui.expressive.StatTile
import app.attic.player.ui.expressive.StaticSnackbar
import app.attic.player.ui.expressive.SwipeToRemove
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.SymbolIcon
import app.attic.player.ui.expressive.Tile
import app.attic.player.ui.expressive.TileGrid
import app.attic.player.ui.expressive.Transport
import app.attic.player.ui.expressive.WavyLinearProgress
import app.attic.player.ui.expressive.narrowed
import app.attic.player.ui.expressive.rememberArtScheme
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The Expressive kit laid out the way pixel/00-direction.html lays out its strips, with sample covers
 * drawn in code. Debug builds only. Extras: `theme` (dark, light, system), `section` (the strip to
 * scroll to, from 0) and `style` (`comfort` for the Comfort kit, ComfortGallery.kt). See
 * ui/expressive/KIT.md and ui/comfort/KIT.md for the commands.
 */
class KitGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTheme = when (intent.getStringExtra("theme")) {
            "light" -> ThemeMode.Light
            "system" -> ThemeMode.System
            else -> ThemeMode.Dark
        }
        val section = intent.getIntExtra("section", 0)
        val uiStyle = if (intent.getStringExtra("style") == "comfort") UiStyle.Comfort else UiStyle.Expressive
        setContent {
            var mode by remember { mutableStateOf(initialTheme) }
            CompositionLocalProvider(LocalUiStyle provides uiStyle) {
                AtticTheme(mode, uiStyle) {
                    val dark = LocalIsDark.current
                    LaunchedEffect(dark) {
                        val clear = Color.Transparent.toArgb()
                        val style = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
                        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    }
                    val toggle = { mode = if (dark) ThemeMode.Light else ThemeMode.Dark }
                    if (uiStyle == UiStyle.Comfort) ComfortGallery(section, dark, toggle) else Gallery(section, dark, toggle)
                }
            }
        }
    }
}

/** Sample covers: soft glows and gradients in the fixture library's colours. */
internal object Samples {
    fun glow(center: Int, edge: Int, deep: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(deep)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(128f, 150f, 150f, intArrayOf(center, edge, deep), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 256f, 256f, paint)
        return bitmap.asImageBitmap()
    }

    fun sweep(a: Int, b: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 256f, 256f, a, b, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 256f, 256f, paint)
        return bitmap.asImageBitmap()
    }

    val revenge = glow(0xFFFFC66B.toInt(), 0xFFE0701A.toInt(), 0xFF3A1406.toInt())
    val viva = glow(0xFFDDE8FF.toInt(), 0xFF3B5E9C.toInt(), 0xFF0B1630.toInt())
    val anbe = glow(0xFFFFD24A.toInt(), 0xFF8FBF2E.toInt(), 0xFF3F6B12.toInt())
    val oso = glow(0xFFFFE23A.toInt(), 0xFFC2187E.toInt(), 0xFF5A0B44.toInt())
    val premam = sweep(0xFF3E9C84.toInt(), 0xFFD9503A.toInt())
    val rockstar = glow(0xFFE8443A.toInt(), 0xFF9C1616.toInt(), 0xFF2A0606.toInt())
    val dhurandhar = sweep(0xFF5A6470.toInt(), 0xFF1C2128.toInt())

    /** Cover tints in the range coverTint produces: the cover's hue, dark and moderately saturated. */
    val seeds = listOf(
        "Jaan Se Guzarte Hain" to (Color(0xFF7A3A12) to revenge),
        "Viva La Vida" to (Color(0xFF233A66) to viva),
        "Aaruyire" to (Color(0xFF4A6A14) to anbe),
        "Ajab Si" to (Color(0xFF6E1458) to oso),
        "Malare" to (Color(0xFF7A2E22) to premam),
        "Ishq Jalakar" to (Color(0xFF2E3A4A) to dhurandhar),
    )
}

private fun Art(bitmap: ImageBitmap) = Art.Picture(bitmap)

/** The gallery's bars are not at the top of the screen, so they skip the status-bar inset. */
private val NoInsets = WindowInsets(0, 0, 0, 0)

@Composable
private fun Gallery(section: Int, dark: Boolean, toggleTheme: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val list = rememberLazyListState()
    LaunchedEffect(section) { if (section > 0) list.scrollToItem(section) }
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(colors.surface),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp),
    ) {
        item { Header(dark, toggleTheme) }
        item { Strip("Colour: the wallpaper's scheme") { SchemeSwatches() } }
        item { Strip("Colour from the cover: FromArt") { FromArtStrip() } }
        item { Strip("Type scale: Google Sans Flex") { TypeStrip() } }
        item { Strip("Shape") { ShapeStrip() } }
        item { Strip("Icons: Material Symbols Rounded") { IconStrip() } }
        item { Strip("Buttons") { ButtonStrip() } }
        item { Strip("Connected group, filter chips, search bar") { FilterStrip() } }
        item { Strip("Song row") { SongRowStrip() } }
        item { Strip("Collection row and browse tile") { CollectionStrip() } }
        item { Strip("Tiles, shelf, carousel, mixes") { TileStrip() } }
        item { Strip("Navigation bar and mini player") { NavStrip() } }
        item { Strip("Seek bar and transport") { SeekStrip() } }
        item { Strip("App bars") { AppBarStrip() } }
        item { Strip("Cards, queue rows and lyric lines on an art scheme") { ArtCardsStrip() } }
        item { Strip("Collection and artist header") { HeaderStrip() } }
        item { Strip("Settings rows, switch, radio, progress") { SettingsStrip() } }
        item { Strip("Sheet, dialog, menu, snackbar, empty state") { SurfacesStrip() } }
    }
}

@Composable
private fun Header(dark: Boolean, toggleTheme: () -> Unit) {
    Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AtticWordmark("Attic", Expressive.type.displaySmallEmphasized.copy(letterSpacing = (-0.4).sp), Modifier.weight(1f))
            ExpressiveButton(if (dark) "Light" else "Dark", toggleTheme, variant = ButtonVariant.Tonal)
        }
        Text("Expressive kit · ${if (dark) "dark" else "light"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A captioned strip, like a `.strip` on the direction sheet. */
@Composable
private fun Strip(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        content()
    }
}

@Composable
private fun SchemeSwatches() {
    val c = MaterialTheme.colorScheme
    val roles = listOf(
        "primary" to c.primary, "onPrimary" to c.onPrimary, "primaryContainer" to c.primaryContainer, "onPrimaryContainer" to c.onPrimaryContainer,
        "secondary" to c.secondary, "onSecondary" to c.onSecondary, "secondaryContainer" to c.secondaryContainer, "onSecondaryContainer" to c.onSecondaryContainer,
        "tertiary" to c.tertiary, "onTertiary" to c.onTertiary, "tertiaryContainer" to c.tertiaryContainer, "onTertiaryContainer" to c.onTertiaryContainer,
        "error" to c.error, "onError" to c.onError, "errorContainer" to c.errorContainer, "onErrorContainer" to c.onErrorContainer,
        "surface" to c.surface, "surfaceContainerLow" to c.surfaceContainerLow, "surfaceContainer" to c.surfaceContainer, "surfaceContainerHigh" to c.surfaceContainerHigh,
        "surfaceContainerHighest" to c.surfaceContainerHighest, "onSurface" to c.onSurface, "onSurfaceVariant" to c.onSurfaceVariant, "outline" to c.outline,
        "outlineVariant" to c.outlineVariant, "inverseSurface" to c.inverseSurface, "inversePrimary" to c.inversePrimary, "surfaceBright" to c.surfaceBright,
    )
    SwatchGrid(roles)
}

@Composable
private fun SwatchGrid(roles: List<Pair<String, Color>>, columns: Int = 4) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        roles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (name, color) ->
                    val ink = if (color.luminance() > 0.4f) Color.Black else Color.White
                    Column(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(8.dp)).background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(6.dp),
                    ) {
                        Text(name, fontSize = 9.sp, lineHeight = 11.sp, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(1f))
                        Text("#%06X".format(color.toArgb() and 0xFFFFFF), fontSize = 9.sp, color = ink.copy(alpha = 0.8f))
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A mini player drawn from kit parts: the pill the mini player strip shows, in the cover's scheme. */
@Composable
private fun MiniPlayerSample(title: String, artist: String, art: ImageBitmap, playing: Boolean, favourite: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(ExpressiveShapes.Pill).background(colors.surfaceContainerHigh).padding(start = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(Art(art), size = 48.dp, shape = RoundedCornerShape(ExpressiveShapes.Large))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        var fav by remember { mutableStateOf(favourite) }
        HeartButton(fav, { fav = !fav })
        var isPlaying by remember { mutableStateOf(playing) }
        PlayButton(isPlaying, { isPlaying = !isPlaying }, progress = { 0.323f })
    }
}

@Composable
private fun FromArtStrip() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Samples.seeds.forEachIndexed { index, (title, pair) ->
            val (seed, art) = pair
            FromArt(seed) { MiniPlayerSample(title, listOf("Khan Saab, Shashwat Sachdev", "Coldplay", "A. R. Rahman", "KK", "Vijay Yesudas", "Shashwat Sachdev")[index], art, playing = index % 2 == 0) }
        }
        Text("Roles rebuilt from each cover", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(4.dp))
    Samples.seeds.take(4).forEach { (_, pair) ->
        val scheme = rememberArtScheme(pair.first)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(Art(pair.second), size = 36.dp)
            listOf(
                scheme.surface, scheme.surfaceContainer, scheme.surfaceContainerHighest, scheme.primary,
                scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer, scheme.onSurfaceVariant, scheme.outlineVariant,
            ).forEach { Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(it)) }
        }
    }
}

@Composable
private fun TypeStrip() {
    val t = Expressive.type
    val rows = listOf(
        Triple("Display L 57/64", "18,460", t.displayLarge to t.displayLargeEmphasized),
        Triple("Display M 45/52", "A. R. Rahman", t.displayMedium to t.displayMediumEmphasized),
        Triple("Display S 36/44", "Library", t.displaySmall to t.displaySmallEmphasized),
        Triple("Headline L 32/40", "Top artists", t.headlineLarge to t.headlineLargeEmphasized),
        Triple("Headline M 28/36", "6,212 plays", t.headlineMedium to t.headlineMediumEmphasized),
        Triple("Headline S 24/32", "Name this playlist", t.headlineSmall to t.headlineSmallEmphasized),
        Triple("Title L 22/28", "Start listening", t.titleLarge to t.titleLargeEmphasized),
        Triple("Title M 16/24", "Kun Faya Kun", t.titleMedium to t.titleMediumEmphasized),
        Triple("Title S 14/20", "Dhurandhar: The Revenge", t.titleSmall to t.titleSmallEmphasized),
        Triple("Body L 16/24", "Add to playlist", t.bodyLarge to t.bodyLargeEmphasized),
        Triple("Body M 14/20", "Picked at random from your songs", t.bodyMedium to t.bodyMediumEmphasized),
        Triple("Body S 12/16", "Shashwat Sachdev, 2026", t.bodySmall to t.bodySmallEmphasized),
        Triple("Label L 14/20", "Open lyrics", t.labelLarge to t.labelLargeEmphasized),
        Triple("Label M 12/16", "Playing from", t.labelMedium to t.labelMediumEmphasized),
        Triple("Label S 11/16", "songs", t.labelSmall to t.labelSmallEmphasized),
    )
    Column(Modifier.padding(horizontal = 16.dp)) {
        rows.forEach { (name, sample, styles) ->
            Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sample, style = styles.first, maxLines = 1)
            Text(sample, style = styles.second, maxLines = 1)
            Spacer(Modifier.height(6.dp))
        }
        Text("Width: a title narrows before it is cut. 100, 75, 50", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(100, 75, 50).forEach { Text("In Lamhon Ke Daaman Mein", style = t.headlineLargeEmphasized.narrowed(it), maxLines = 1) }
        Text("NarrowingText in 260dp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NarrowingText("Jaan Se Guzarte Hain", Modifier.width(260.dp), style = t.headlineLargeEmphasized)
        NarrowingText("In Lamhon Ke Daaman Mein", Modifier.width(260.dp), style = t.headlineLargeEmphasized)
        NarrowingText("Yeh Jawaani Hai Deewani: Badtameez Dil", Modifier.width(260.dp), style = t.headlineLargeEmphasized)
    }
}

@Composable
private fun ShapeStrip() {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(4, 8, 12, 16, 20, 28, 32, 48, 999).forEach { r ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(if (r == 999) ExpressiveShapes.Pill else RoundedCornerShape(r.dp)).background(colors.primaryContainer))
                Text(if (r == 999) "full" else "$r", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Labelled("Album") { Cover(Art(Samples.revenge), size = 88.dp) }
        Labelled("Folder") { Mosaic(listOf(Art(Samples.oso), Art(Samples.revenge), Art(Samples.premam), Art(Samples.rockstar)), size = 88.dp) }
        Labelled("Favourites") { FavouritesArt(size = 88.dp) }
        Labelled("Artist") { ArtistCover(Art(Samples.anbe), size = 88.dp) }
        Labelled("No art") { NoArt(size = 88.dp) }
        Labelled("Instant mix") { InstantMixPill("Kun Faya Kun", Art(Samples.rockstar), {}, {}) }
        Labelled("Play control") {
            Row { PlayButton(false, {}, size = PlaySize.Large); Spacer(Modifier.width(8.dp)); PlayButton(true, {}, size = PlaySize.Large) }
        }
        Labelled("Toggle") {
            Row {
                ExpressiveToggleButton(Symbol.Shuffle, false, {}, null, medium = true, tonalWhenOff = true)
                Spacer(Modifier.width(8.dp))
                ExpressiveToggleButton(Symbol.Shuffle, true, {}, null, medium = true, tonalWhenOff = true)
            }
        }
        Labelled("Loading") { Row { LoadingIndicator(); Spacer(Modifier.width(8.dp)); LoadingIndicator(contained = true) } }
        Labelled("Cookie at 28dp") { Box(Modifier.size(28.dp).clip(CookieShape).background(colors.primary)) }
    }
}

@Composable
private fun Labelled(label: String, content: @Composable () -> Unit) {
    Column {
        content()
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IconStrip() {
    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 6) {
        Symbol.entries.forEach { symbol ->
            Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    SymbolIcon(symbol)
                    SymbolIcon(symbol, filled = true)
                }
                Text(symbol.ligature, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val c = MaterialTheme.colorScheme
        listOf(c.surfaceContainerHigh to c.primary, c.primaryContainer to c.onPrimaryContainer, c.primary to c.onPrimary).forEach { (bg, fg) ->
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(bg), contentAlignment = Alignment.Center) { AtticMarkIcon(size = 32.dp, tint = fg) }
        }
        listOf(18, 20, 24, 28, 32, 40).forEach { SymbolIcon(Symbol.QueueMusic, size = it.dp) }
    }
}

@Composable
private fun ButtonStrip() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveButton("Play", {}, leading = Symbol.PlayArrow)
            ExpressiveButton("Open lyrics", {}, variant = ButtonVariant.Tonal)
            ExpressiveButton("Download lyrics", {}, variant = ButtonVariant.Outlined)
            ExpressiveButton("Cancel", {}, variant = ButtonVariant.Text)
            ExpressiveButton("Create", {}, variant = ButtonVariant.Text, enabled = false)
        }
        var shuffle by remember { mutableStateOf(true) }
        var playingCollection by remember { mutableStateOf(false) }
        ButtonGroup(Modifier.fillMaxWidth(), fill = true) {
            val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ExpressiveButton(
                if (playingCollection) "Pause" else "Play",
                { playingCollection = !playingCollection },
                Modifier.groupItem(source, 2.2f),
                size = ButtonSize.Medium,
                leading = if (playingCollection) Symbol.Pause else Symbol.PlayArrow,
                squared = playingCollection,
                interactionSource = source,
            )
            ExpressiveToggleButton(Symbol.Shuffle, false, {}, "Shuffle", medium = true, tonalWhenOff = true)
            ExpressiveToggleButton(Symbol.Shuffle, shuffle, { shuffle = it }, "Shuffle", medium = true, tonalWhenOff = true)
            ExpressiveIconButton(Symbol.PlaylistAdd, "Add to playlist", {}, variant = IconButtonVariant.Tonal, medium = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpressiveIconButton(Symbol.Add, "Add", {}, variant = IconButtonVariant.Filled, medium = true, width = IconButtonWidth.Wide)
            ExpressiveIconButton(Symbol.MoreVert, "More", {}, width = IconButtonWidth.Narrow)
            ExpressiveIconButton(Symbol.Settings, "Settings", {}, variant = IconButtonVariant.Tonal)
            ExpressiveIconButton(Symbol.Add, "Add", {}, variant = IconButtonVariant.Filled)
            ExpressiveIconButton(Symbol.Sort, "Sort", {}, variant = IconButtonVariant.Outlined)
            var repeat by remember { mutableStateOf(true) }
            ExpressiveToggleButton(Symbol.Repeat, repeat, { repeat = it }, "Repeat")
            var fav by remember { mutableStateOf(false) }
            HeartButton(fav, { fav = !fav })
            HeartButton(true, {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SplitButton("Title", {}, {}, "Sort options", leading = Symbol.Sort)
            ExpressiveFab(Symbol.Add, "New playlist", {})
        }
        ExpressiveExtendedFab("New playlist", Symbol.Add, {})
        FloatingToolbar(Modifier.width(240.dp)) {
            var on by remember { mutableStateOf(true) }
            ExpressiveToggleButton(Symbol.Shuffle, on, { on = it }, "Shuffle")
            ExpressiveIconButton(Symbol.Repeat, "Repeat", {})
            HeartButton(false, {})
            ExpressiveIconButton(Symbol.QueueMusic, "Open queue", {})
        }
    }
}

@Composable
private fun FilterStrip() {
    var seg by remember { mutableIntStateOf(0) }
    ConnectedButtonGroup(listOf("All", "Albums", "Artists", "Folders"), seg, { seg = it })
    Spacer(Modifier.height(8.dp))
    var chip by remember { mutableIntStateOf(1) }
    ChipRow {
        listOf("Playlists", "Albums", "Artists", "Songs").forEachIndexed { i, label ->
            ExpressiveFilterChip(label, chip == i, { chip = if (chip == i) -1 else i })
        }
    }
    Spacer(Modifier.height(8.dp))
    var query by remember { mutableStateOf("") }
    SearchField(query, { query = it }, "Search your music", Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(8.dp))
    var typed by remember { mutableStateOf("dhur") }
    SearchField(typed, { typed = it }, "Search your music", Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SongRowStrip() {
    ListGroup(8) { i ->
        when (i) {
            0 -> SongRow("Aari Aari", "Bombay Rockers, Khan Saab, Jasmine Sandlas, Reble", RowLead.Cover(Art(Samples.revenge)), onClick = {}) { RowMoreButton("Aari Aari") {} }
            1 -> SongRow("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", RowLead.Cover(Art(Samples.revenge)), current = true, playing = true, onClick = {}) { RowMoreButton("Jaan Se Guzarte Hain") {} }
            2 -> SongRow("Aakhri Ishq", "Jubin Nautiyal", RowLead.Number(4), onClick = {}) { RowMoreButton("Aakhri Ishq") {} }
            3 -> SongRow("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", RowLead.Number(3), current = true, playing = false, onClick = {}) { RowMoreButton("Jaan Se Guzarte Hain") {} }
            4 -> SongRow("Kun Faya Kun", "A. R. Rahman, Javed Ali, Mohit Chauhan", RowLead.RankedCover(2, Art(Samples.rockstar)), onClick = {}) { RowCount("187 plays") }
            5 -> SongRow("Phir Se", "Arijit Singh", RowLead.Cover(Art(Samples.revenge))) { RowDragHandle("Phir Se") }
            6 -> { var added by remember { mutableStateOf(false) }; SongRow("Malare", "Vijay Yesudas", RowLead.Cover(Art(Samples.premam))) { RowAddToggle("Malare", added) { added = !added } } }
            else -> SongRow("Viva La Vida", "Coldplay", RowLead.Cover(Art(Samples.viva))) { RowAddToggle("Viva La Vida", true) {} }
        }
    }
}

@Composable
private fun CollectionStrip() {
    ListGroup(5) { i ->
        when (i) {
            0 -> CollectionRow("Favourites", "31 songs", { FavouritesArt(size = 56.dp) }, {})
            1 -> CollectionRow("Late drive", "Playlist, 12 songs", { Mosaic(listOf(Art(Samples.viva), Art(Samples.oso), Art(Samples.anbe), Art(Samples.rockstar)), size = 56.dp) }, {})
            2 -> CollectionRow("Hindi", "Playlist, 46 songs", { Mosaic(listOf(Art(Samples.oso), Art(Samples.revenge), Art(Samples.premam), Art(Samples.rockstar)), size = 56.dp) }, {})
            3 -> CollectionRow("A. R. Rahman", "Artist", { ArtistCover(Art(Samples.anbe), size = 56.dp) }, {})
            else -> CollectionRow("Dhurandhar: The Revenge", "Album, Shashwat Sachdev", { Cover(Art(Samples.revenge), size = 56.dp) }, {})
        }
    }
    Spacer(Modifier.height(12.dp))
    val tiles = listOf("English" to Samples.seeds[1].second, "Hindi" to Samples.seeds[3].second, "Dhurandhar: The Revenge" to Samples.seeds[0].second, "Anbe Aaruyire" to Samples.seeds[2].second)
    TileGrid(tiles) { (title, pair) -> BrowseTile(title, pair.first, {}, Modifier.fillMaxWidth()) { m -> Cover(Art(pair.second), m) } }
}

@Composable
private fun TileStrip() {
    HeroCarousel(
        count = 5,
        leading = { FavouritesPill("31", "songs", "Favourites", {}) },
        caption = { HeroCaption(listOf("Dhurandhar: The Revenge", "Dhurandhar", "Yeh Jawaani Hai Deewani", "Rockstar", "Viva la Vida")[it], "Shashwat Sachdev, 2026") },
    ) { index ->
        Cover(Art(listOf(Samples.revenge, Samples.dhurandhar, Samples.oso, Samples.rockstar, Samples.viva)[index]), Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
    }
    Spacer(Modifier.height(12.dp))
    RecapCard("Your 2026 so far", "18,460 minutes. See your top songs and cards to share.", {}) { m ->
        Mosaic(listOf(Art(Samples.revenge), Art(Samples.rockstar), Art(Samples.premam), Art(Samples.viva)), m)
    }
    SectionHeader("Instant mixes")
    InstantMixes(listOf("Kun Faya Kun" to Samples.rockstar, "Malare" to Samples.premam, "Viva La Vida" to Samples.viva, "Udd Gaye" to Samples.anbe, "Khwaja Mere Khwaja" to Samples.oso, "Monta Re" to Samples.dhurandhar)) { (title, art) ->
        InstantMixPill(title, Art(art), {}, {}, now = title == "Malare")
    }
    SectionHeader("Recently played")
    Shelf {
        item { Tile("Jaan Se Guzarte Hain", {}, { Cover(Art(Samples.revenge), it) }, subtitle = "Khan Saab, Shashwat Sachdev", width = 112.dp, now = true, playing = true) }
        item { Tile("Kabira", {}, { Cover(Art(Samples.oso), it) }, subtitle = "Tochi Raina, Rekha Bhardwaj", width = 112.dp) }
        item { Tile("Hymn for the Weekend", {}, { Cover(Art(Samples.viva), it) }, subtitle = "Coldplay", width = 112.dp) }
        item { Tile("Tum Tak", {}, { Cover(Art(Samples.rockstar), it) }, subtitle = "Javed Ali", width = 112.dp) }
    }
    SectionHeader("Top songs", overline = "Picked at random from your songs", trailing = { PlayButton(false, {}, size = PlaySize.Large) })
    SectionHeader("Top artists")
    Shelf {
        item { Tile("A. R. Rahman", {}, { ArtistCover(Art(Samples.anbe), it) }, width = 112.dp, artist = true) }
        item { Tile("Shashwat Sachdev", {}, { ArtistCover(Art(Samples.revenge), it) }, width = 112.dp, artist = true) }
        item { Tile("Coldplay", {}, { ArtistCover(Art(Samples.viva), it) }, width = 112.dp, artist = true) }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("18,460", "Minutes", Modifier.weight(1f), accent = true)
        StatTile("6,212", "Plays", Modifier.weight(1f))
    }
}

@Composable
private fun NavStrip() {
    var tab by remember { mutableIntStateOf(0) }
    val items = listOf(NavItem("Home", Symbol.Home), NavItem("Search", Symbol.Search), NavItem("Library", Symbol.LibraryMusic))
    Column {
        FromArt(Samples.seeds[0].second.first) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { MiniPlayerSample("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", Samples.revenge, playing = true) }
        }
        ExpressiveNavigationBar(items, tab, { tab = it }, windowInsets = WindowInsets(0, 0, 0, 0))
    }
}

@Composable
private fun SeekStrip() {
    FromArt(Samples.seeds[0].second.first) {
        val colors = MaterialTheme.colorScheme
        var playing by remember { mutableStateOf(true) }
        var position by remember { mutableFloatStateOf(0.323f) }
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(colors.surface).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(if (playing) "Playing" else "Paused", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            SeekBar(position, playing, { position = it }, "Seek in Jaan Se Guzarte Hain")
            SeekTimes("1:47", "5:31")
            Spacer(Modifier.height(16.dp))
            Transport(playing, {}, { playing = !playing }, {})
            Spacer(Modifier.height(12.dp))
            SeekBar(position, false, { position = it }, "Seek, paused")
            SeekTimes("1:47", "5:31")
        }
    }
}

@Composable
private fun AppBarStrip() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            RootAppBar("Library", Modifier, 0f, windowInsets = NoInsets) {
                ExpressiveIconButton(Symbol.Search, "Search your library", {})
                ExpressiveIconButton(Symbol.Add, "Create playlist", {})
                ExpressiveIconButton(Symbol.Settings, "Settings", {})
            }
        }
        RootAppBar("Attic", Modifier, 0f, titleLeading = { AtticMarkIcon(size = 32.dp, tint = MaterialTheme.colorScheme.primary) }, windowInsets = NoInsets) {
            ExpressiveIconButton(Symbol.Settings, "Settings", {})
        }
        RootAppBar("Library", Modifier, 1f, windowInsets = NoInsets) { ExpressiveIconButton(Symbol.Settings, "Settings", {}) }
        DetailAppBar("Settings", {}, windowInsets = NoInsets)
        FromArt(Samples.seeds[0].second.first) {
            DetailAppBar("Dhurandhar: The Revenge", {}, surface = AppBarSurface.Scrolled, windowInsets = NoInsets) { ExpressiveIconButton(Symbol.MoreVert, "Playlist options", {}) }
        }
        FromArt(Samples.seeds[0].second.first) {
            Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                DetailAppBar("Over art", {}, surface = AppBarSurface.OverArt, windowInsets = NoInsets) { ExpressiveIconButton(Symbol.MoreVert, "Playlist options", {}) }
            }
        }
    }
}

@Composable
private fun ArtCardsStrip() {
    FromArt(Samples.seeds[1].second.first) {
        val colors = MaterialTheme.colorScheme
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(colors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(title = "Lyrics", symbol = Symbol.Lyrics) {
                LyricLine("I carry the lamp you left alight", LyricState.Current)
                LyricLine("Every window on this street is yours", LyricState.Upcoming)
                LyricLine("I pass them all and count the doors", LyricState.Upcoming)
                LyricLine("♪", LyricState.Upcoming)
                ExpressiveButton("Open lyrics", {}, variant = ButtonVariant.Tonal, trailing = Symbol.OpenInFull)
            }
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArtistCover(Art(Samples.viva), size = 88.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("About the artist", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Text("Coldplay", style = Expressive.type.titleLargeEmphasized)
                        Text("12 songs in your library", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    SymbolIcon(Symbol.ArrowForward)
                }
            }
            InfoCard(title = "Credits") {
                KeyValue("Performed by", "Coldplay")
                KeyValue("Album", "Viva la Vida")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    FromArt(Samples.seeds[0].second.first) {
        val colors = MaterialTheme.colorScheme
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(colors.surface).padding(vertical = 8.dp)) {
            ListLabel("Now playing")
            ListGroup(1) { SongRow("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev", RowLead.Cover(Art(Samples.revenge)), current = true, playing = true) { RowMoreButton("Jaan Se Guzarte Hain") {} } }
            ListLabel("Up next, 4 songs")
            ListGroup(4) { i ->
                when (i) {
                    0 -> SongRow("Aakhri Ishq", "Jubin Nautiyal", RowLead.Cover(Art(Samples.revenge))) { RowDragHandle("Aakhri Ishq") }
                    1 -> SwipeToRemove({}) { SongRow("Vaari Jaavan", "Jyoti Nooran, Jasmine Sandlas, Reble", RowLead.Cover(Art(Samples.revenge))) { RowDragHandle("Vaari Jaavan") } }
                    2 -> SongRow("Phir Se", "Arijit Singh", RowLead.Cover(Art(Samples.revenge)), lifted = true) { RowDragHandle("Phir Se") }
                    else -> SongRow("Kun Faya Kun", "A. R. Rahman, Javed Ali, Mohit Chauhan", RowLead.Cover(Art(Samples.rockstar))) { RowDragHandle("Kun Faya Kun") }
                }
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                LyricLine("The road keeps my footsteps for the night", LyricState.Sung, large = true)
                LyricLine("I carry the lamp you left alight", LyricState.Current, large = true)
                LyricLine("Every window on this street is yours", LyricState.Upcoming, large = true)
            }
        }
    }
}

@Composable
private fun HeaderStrip() {
    FromArt(Samples.seeds[0].second.first) {
        val colors = MaterialTheme.colorScheme
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).background(colors.surfaceContainer).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)) {
            Cover(Art(Samples.revenge), size = 208.dp)
            Spacer(Modifier.height(20.dp))
            Text("Dhurandhar: The Revenge", style = Expressive.type.headlineLargeEmphasized.copy(lineHeight = 36.sp, letterSpacing = (-0.4).sp))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtistCover(Art(Samples.revenge), size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text("Shashwat Sachdev", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.primary)
            }
            Text("Album • 2026", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(18.dp))
            CollectionActions(playing = false, shuffle = false)
        }
    }
    Spacer(Modifier.height(12.dp))
    FromArt(Samples.seeds[2].second.first) {
        val colors = MaterialTheme.colorScheme
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).background(colors.surfaceContainer).padding(16.dp)) {
            ArtistCover(Art(Samples.anbe), size = 208.dp)
            Spacer(Modifier.height(20.dp))
            Text("A. R. Rahman", style = Expressive.type.displayMediumEmphasized.copy(lineHeight = 48.sp, letterSpacing = (-0.8).sp))
            Text("14 songs, 1 hr 13 min", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(18.dp))
            CollectionActions(playing = true, shuffle = true)
        }
    }
}

@Composable
private fun CollectionActions(playing: Boolean, shuffle: Boolean) {
    var isPlaying by remember { mutableStateOf(playing) }
    var isShuffle by remember { mutableStateOf(shuffle) }
    ButtonGroup(Modifier.fillMaxWidth(), fill = true) {
        val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ExpressiveButton(
            if (isPlaying) "Pause" else "Play",
            { isPlaying = !isPlaying },
            Modifier.groupItem(source, 3f),
            size = ButtonSize.Medium,
            leading = if (isPlaying) Symbol.Pause else Symbol.PlayArrow,
            squared = isPlaying,
            interactionSource = source,
        )
        ExpressiveToggleButton(Symbol.Shuffle, isShuffle, { isShuffle = it }, "Shuffle", medium = true, tonalWhenOff = true)
        ExpressiveIconButton(Symbol.PlaylistAdd, "Add to a playlist", {}, variant = IconButtonVariant.Tonal, medium = true)
    }
}

@Composable
private fun SettingsStrip() {
    var autoplay by remember { mutableStateOf(true) }
    var theme by remember { mutableIntStateOf(1) }
    ListGroup(2) { i ->
        if (i == 0) {
            SetSwitchRow("Autoplay", autoplay, { autoplay = it }, "When a queue reaches its last song, radio keeps going with similar songs.")
        } else {
            SetSwitchRow("Autoplay", false, {}, "Off")
        }
    }
    SettingsSection("Appearance")
    ListGroup(3) { i -> SetRadioRow(listOf("Match phone", "Dark", "Light")[i], theme == i, { theme = i }) }
    SettingsSection("Song radio")
    ListGroup(2) { i ->
        if (i == 0) {
            SetNote("Radio finds similar songs by listening to your files. This happens on this phone, and nothing is uploaded.") {
                Spacer(Modifier.height(12.dp))
                WavyLinearProgress({ 79f / 127f })
                SetNoteStatus("Analysing songs: 79 of 127")
                ExpressiveButton("Analyse now without a charger", {}, variant = ButtonVariant.Text)
            }
        } else {
            SetNote("112 of 127 songs have lyrics saved on this phone. Saved lyrics work offline.") {
                Spacer(Modifier.height(12.dp))
                LinearProgress({ 112f / 127f })
                SetNoteStatus("Checked 112 of 127, found 96")
            }
        }
    }
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgress(progress = { 0.62f })
        CircularProgress()
        CircularProgress(small = true)
        LoadingIndicator()
        LoadingIndicator(contained = true)
        NowPlayingWave(true)
        NowPlayingWave(false)
    }
}

@Composable
private fun SurfacesStrip() {
    val colors = MaterialTheme.colorScheme
    var sheet by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveButton("Sheet", { sheet = true }, variant = ButtonVariant.Tonal)
        ExpressiveButton("Dialog", { dialog = true }, variant = ButtonVariant.Tonal)
        Box {
            ExpressiveButton("Menu", { menu = true }, variant = ButtonVariant.Tonal)
            ExpressiveMenu(menu, { menu = false }) {
                ExpressiveMenuItem(Symbol.Edit, "Rename", { menu = false })
                ExpressiveMenuItem(Symbol.Delete, "Delete playlist", { menu = false })
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    // The sheet's content drawn in place, as the strip shows it.
    Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerLow).padding(bottom = 16.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { app.attic.player.ui.expressive.SheetHandle() }
        SheetBody()
    }
    Spacer(Modifier.height(12.dp))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StaticSnackbar("Added to Late drive", Modifier.fillMaxWidth())
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer).padding(8.dp).width(196.dp)) {
            Row(Modifier.height(48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { SymbolIcon(Symbol.Edit, tint = colors.onSurfaceVariant); Spacer(Modifier.width(12.dp)); Text("Rename", style = MaterialTheme.typography.labelLarge) }
            Row(Modifier.height(48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { SymbolIcon(Symbol.Delete, tint = colors.onSurfaceVariant); Spacer(Modifier.width(12.dp)); Text("Delete playlist", style = MaterialTheme.typography.labelLarge) }
        }
    }
    EmptyState("No results for “dhurx”", "Check the spelling, or try fewer words.", symbol = Symbol.SearchOff)
    EmptyState("Attic plays the music on this phone", "Allow audio access so it can read your songs in Music/Offline. Notifications show the playback controls.", art = { AtticMarkIcon(size = 56.dp) }) {
        ExpressiveButton("Allow access", {})
    }

    if (sheet) {
        ExpressiveSheet({ sheet = false }) { SheetBody() }
    }
    if (dialog) {
        var name by remember { mutableStateOf("Late drive") }
        ExpressiveDialog(
            "Name this playlist",
            onDismiss = { dialog = false },
            confirmButton = { ExpressiveButton("Create", { dialog = false }, variant = ButtonVariant.Text, enabled = name.isNotBlank()) },
            dismissButton = { ExpressiveButton("Cancel", { dialog = false }, variant = ButtonVariant.Text) },
        ) { DialogTextField(name, { name = it }, "Playlist name") }
    }
}

@Composable
private fun SheetBody() {
    SheetHeader("Jaan Se Guzarte Hain", "Khan Saab, Shashwat Sachdev") { Cover(Art(Samples.revenge), it) }
    SheetDivider()
    SheetRow(Symbol.Sensors, "Start radio", {})
    SheetRow(Symbol.Favorite, "Add to favourites", {})
    SheetRow(Symbol.PlaylistPlay, "Play next", {})
    SheetRow(Symbol.Language, "Language: Hindi", {})
    SheetRow(Symbol.Check, "Hindi", {}, selected = true)
}
