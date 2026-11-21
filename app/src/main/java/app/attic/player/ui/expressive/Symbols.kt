package app.attic.player.ui.expressive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.attic.player.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Material Symbols Rounded (Apache 2.0), bundled as res/font/material_symbols_rounded.ttf: a subset
 * holding only the icons the pixel/ and oneui/ sheets use, with the FILL (0–1), weight (300–700) and
 * optical size (20–48) axes kept. Each icon is addressed by its codepoint, so a wrong name cannot
 * render as a word. [ligature] is the name the mocks write in `<span class="ms">`.
 *
 * To add an icon: find its codepoint in the font's `.codepoints` file, add it here, and re-run the
 * subset command in third_party/fonts-NOTICE.txt with the new codepoint added.
 */
enum class Symbol(val ligature: String, val codepoint: Int) {
    Add("add", 0xE145),
    AirplanemodeActive("airplanemode_active", 0xE53D),
    Alarm("alarm", 0xE855),
    Album("album", 0xE019),
    ArrowBack("arrow_back", 0xE5C4),
    ArrowForward("arrow_forward", 0xE5C8),
    Autoplay("autoplay", 0xF6B5),
    Backspace("backspace", 0xE14A),
    Bedtime("bedtime", 0xF159),
    Bluetooth("bluetooth", 0xE1A7),
    CalendarMonth("calendar_month", 0xEBCC),
    Call("call", 0xF0D4),
    ChatBubble("chat_bubble", 0xE0CB),
    Check("check", 0xE668),
    ChevronRight("chevron_right", 0xE5CC),
    ClearAll("clear_all", 0xE0B8),
    Close("close", 0xE5CD),
    Delete("delete", 0xE92E),
    DoNotDisturbOn("do_not_disturb_on", 0xF08F),
    Download("download", 0xF090),
    DragHandle("drag_handle", 0xE25D),
    Edit("edit", 0xF097),
    EditNote("edit_note", 0xE745),
    Error("error", 0xF8B6),
    ExpandLess("expand_less", 0xE5CE),
    ExpandMore("expand_more", 0xE5CF),
    Favorite("favorite", 0xE87E),
    FlashlightOn("flashlight_on", 0xF00B),
    Folder("folder", 0xE2C7),
    FolderOpen("folder_open", 0xE2C8),
    GraphicEq("graphic_eq", 0xE1B8),
    GridView("grid_view", 0xE9B0),
    Home("home", 0xE9B2),
    Image("image", 0xE3F4),
    Info("info", 0xE88E),
    IosShare("ios_share", 0xE6B8),
    KeyboardArrowDown("keyboard_arrow_down", 0xE313),
    KeyboardArrowUp("keyboard_arrow_up", 0xE316),
    Language("language", 0xEA07),
    LibraryMusic("library_music", 0xE030),
    LightMode("light_mode", 0xE518),
    LocalFireDepartment("local_fire_department", 0xEF55),
    Lock("lock", 0xE899),
    Lyrics("lyrics", 0xEC0B),
    Map("map", 0xE55B),
    Mic("mic", 0xE31D),
    MoreVert("more_vert", 0xE5D4),
    MusicNote("music_note", 0xE405),
    MusicOff("music_off", 0xE440),
    NewReleases("new_releases", 0xEF76),
    Notifications("notifications", 0xE7F5),
    OpenInFull("open_in_full", 0xF1CE),
    OpenInNew("open_in_new", 0xE89E),
    Palette("palette", 0xE40A),
    PartlyCloudyDay("partly_cloudy_day", 0xF172),
    Pause("pause", 0xE034),
    Person("person", 0xF0D3),
    PhotoCamera("photo_camera", 0xE412),
    PlayArrow("play_arrow", 0xE037),
    PlaylistAdd("playlist_add", 0xE03B),
    PlaylistPlay("playlist_play", 0xE05F),
    QueueMusic("queue_music", 0xE03D),
    Refresh("refresh", 0xE5D5),
    Repeat("repeat", 0xE040),
    RepeatOne("repeat_one", 0xE041),
    Schedule("schedule", 0xEFD6),
    ScreenRotation("screen_rotation", 0xF2D5),
    Search("search", 0xEF7A),
    SearchOff("search_off", 0xEA76),
    Sensors("sensors", 0xE51E),
    Settings("settings", 0xE8B8),
    Share("share", 0xE80D),
    Shift("shift", 0xE5F2),
    Shuffle("shuffle", 0xE043),
    ShuffleOn("shuffle_on", 0xE9E1),
    SkipNext("skip_next", 0xE044),
    SkipPrevious("skip_previous", 0xE045),
    Smartphone("smartphone", 0xE7BA),
    Sort("sort", 0xE164),
    SwapVert("swap_vert", 0xE8D5),
    Today("today", 0xE8DF),
    UnfoldMore("unfold_more", 0xE5D7),
    ViewList("view_list", 0xE8EF),
    VolumeUp("volume_up", 0xE050),
    Wifi("wifi", 0xE63E),
    ;

    internal val text: String get() = String(Character.toChars(codepoint))

    companion object {
        /** The symbol a mock names, e.g. "queue_music". */
        fun named(ligature: String): Symbol? = entries.firstOrNull { it.ligature == ligature }
    }
}

private object SymbolFont {
    private data class Key(val filled: Boolean, val weight: Int, val opticalSize: Int)

    private val families = ConcurrentHashMap<Key, FontFamily>()

    fun family(filled: Boolean, weight: Int, opticalSize: Int): FontFamily =
        families.getOrPut(Key(filled, weight, opticalSize)) {
            FontFamily(
                Font(
                    R.font.material_symbols_rounded,
                    weight = FontWeight(weight),
                    variationSettings = FontVariation.Settings(
                        FontVariation.Setting("FILL", if (filled) 1f else 0f),
                        FontVariation.weight(weight),
                        FontVariation.Setting("opsz", opticalSize.toFloat()),
                    ),
                ),
            )
        }
}

/** The optical sizes the icon families are built at; a size in between uses the one below. */
private fun opticalSizeFor(size: Dp): Int = when {
    size < 24.dp -> 20
    size < 40.dp -> 24
    else -> 40
}

/**
 * A Material Symbols Rounded icon. [filled] is the selected state (the current tab, a favourite, a
 * toggle that is on) and the transport glyphs. Sizes in the kit are 18, 20, 24 (default), 28, 32, 36
 * and 40dp. Expressive draws at weight 400; Comfort can reuse it at 300.
 *
 * Pass [contentDescription] only when the icon stands alone; inside a button, describe the button.
 */
@Composable
fun SymbolIcon(
    symbol: Symbol,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    filled: Boolean = false,
    tint: Color = LocalContentColor.current,
    weight: Int = 400,
    contentDescription: String? = null,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    val style = TextStyle(
        fontFamily = SymbolFont.family(filled, weight, opticalSizeFor(size)),
        fontWeight = FontWeight(weight),
        fontSize = fontSize,
        lineHeight = 1.em,
        textAlign = TextAlign.Center,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Box(
        modifier
            .size(size)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol.text,
            style = style,
            color = tint,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
