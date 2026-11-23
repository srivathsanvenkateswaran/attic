"""Foundations of Attic Expressive: colour variables, text styles, effect styles, spacing and radius
variables, the Expressive shapes, motion notes and the icon set.

Pages: Colour, Type, Spacing, radii and shapes, Elevation and motion, Icons.

Everything below is a stable name other modules may bind to. Source of truth is the Compose code in
app/src/main/java/app/attic/player/ui/expressive/ (ExpressiveTheme.kt, Type.kt, Shapes.kt,
ArtScheme.kt, FromArt.kt, Symbols.kt) and the computed colours in
/tmp/attic-dev/design-refs/colors/expressive-*.json, embedded below so the build does not need them.

How colour works (read this before binding anything)
----------------------------------------------------
The app's scheme is the phone's wallpaper scheme (dynamicLight/DarkColorScheme). Player surfaces
(mini player, Now Playing, queue, full-screen lyrics, collection and artist headers and their bar,
browse tiles) rebuild every role from the cover with FromArt(seed). Both share one set of role
names, so the file has three colour collections:

- ``Wallpaper`` (modes Light, Dark): raw values of every role, as the JVM renders draw them.
- ``Cover`` (modes Light, Dark): the cover scheme for the demo now-playing cover (Paper Lanterns),
  plus ``seed`` and ``palette-swatch``. Error roles and ``scrim`` alias Wallpaper (ArtScheme keeps the
  app scheme's); ``background``, ``on-background``, ``surface-variant``, ``surface-tint`` alias the
  Cover roles ArtScheme.colorScheme maps them to.
- ``Color`` (modes Wallpaper, Cover): what layers bind to. Each role aliases Wallpaper/<role> in
  mode Wallpaper and Cover/<role> in mode Cover. Code syntax is MaterialTheme.colorScheme.<role>.

Bind fills to ``Color/<role>``. A frame (or main component) on a player surface sets
``d.set_mode(id, "Color", "Cover")``; everything inside then takes the cover's colours, the way
FromArt re-provides MaterialTheme.colorScheme; another album's surface sets that album's mode. Light and Dark come
from the modes of every two-mode collection, which expressive/__init__.py lists in ``d.mode_collections``
(Wallpaper, Cover, System, Art, Comfort preview, Phone). Every variable, collection and text style of this file is
made here.

Variables
---------
Collection ``Color`` (modes Wallpaper, Cover), every one an alias, code syntax
MaterialTheme.colorScheme.<camelCase>:
  primary, on-primary, primary-container, on-primary-container, inverse-primary,
  secondary, on-secondary, secondary-container, on-secondary-container,
  tertiary, on-tertiary, tertiary-container, on-tertiary-container,
  error, on-error, error-container, on-error-container,
  background, on-background, surface, on-surface, surface-variant, on-surface-variant,
  surface-tint, surface-dim, surface-bright, surface-container-lowest, surface-container-low,
  surface-container, surface-container-high, surface-container-highest,
  outline, outline-variant, inverse-surface, inverse-on-surface, scrim
  and colour calculations on those roles:
  state/disabled-container   on-surface at 12 % (disabled filled and tonal buttons)
  state/disabled-content     on-surface at 38 % (disabled labels and icons)
  state/over-art-scrim       surface at 62 % (DetailAppBar OverArt button scrims)
  state/now-playing-veil     secondary-container at 80 % (over the current row's cover)
Collection ``Wallpaper`` (Light, Dark): the same 36 role names, raw values.
Collection ``Cover`` (Light, Dark): the same 36 role names, plus seed (#7C311D) and
  palette-swatch (#F07858).
  Color also has one mode per other demo album ("Iravu Rail", "Glasshouse Summer"...; COLOR_MODE[i] maps a cover
  index to its mode, "Cover" for Paper Lanterns): every role aliases Art/<album>/<role>, as Cover aliases the Cover
  collection. Set a frame's Color mode to its album to draw FromArt(seed) for that cover. Recap's mixes are Color
  variables too: recap/story-ground, recap/dial-bar, recap/language-2..5, recap/tile-2..4.
Collection ``Art`` (Light, Dark): the other eleven covers' roles, ``<album-slug>/<role>``, and ``<slug>/story-ground``.
Collection ``System`` (Light, Dark): status-bar-ink (#1B1B1F / #FFFFFF), gesture-handle (ink at 85 %),
  keyboard-ground and keyboard-key (the render harness's stand-in keyboard). The phone's chrome, not an Attic role.
Collection ``Phone`` (Light, Dark): the stand-in home screen, shade and lock screen on the System surfaces page.
Collection ``Brand`` (Value): headroom/ground, headroom/cabinet, headroom/cone (res/values/colors.xml).
Collection ``Comfort preview`` (Light, Dark): Comfort's colours for the first run's Comfort picture only.
Collection ``Dimension`` (one mode, Value), numbers:
  radius/extra-small 4, radius/small 8, radius/medium 12, radius/large 16,
  radius/large-increased 20, radius/extra-large 28, radius/extra-large-increased 32,
  radius/extra-extra-large 48, radius/full 1000, radius/play-small-playing 11,
  radius/play-large-playing 18
  space/2 space/4 space/6 space/8 space/10 space/12 space/16 space/20 space/24 space/28 space/32
  space/48
  layout/screen-margin 16, layout/player-margin 24, layout/group-gap 2, layout/group-spacing 12,
  layout/shelf-gap 12, layout/mini-player-inset 12, layout/mini-player-gap 8,
  layout/dock-clearance 24
  size/touch-target 48, size/icon 24, size/button-small 40, size/button-medium 56,
  size/row 64, size/collection-row 72, size/app-bar 64, size/root-app-bar 72,
  size/mini-player 64, size/navigation-bar 64, size/status-bar 40, size/gesture-bar 24

Text styles (Google Sans Flex; axes wght, wdth 100, ROND 0 or 100, opsz as Type.kt sets them)
----------------------------------------------------------------------------------------------
Material roles: Display/Large, Display/Medium, Display/Small, Headline/Large, Headline/Medium,
Headline/Small, Title/Large, Title/Medium, Title/Small, Body/Large, Body/Medium, Body/Small,
Label/Large, Label/Medium, Label/Small.
Emphasised partners (Expressive.type.<role>Emphasized): the same fifteen names with " Emphasized"
appended, e.g. Title/Large Emphasized.
Kit styles, each a .copy() the code makes of a role (exact expression in the style description):
Kit/Button, Kit/Button medium, Kit/Quick action, Kit/Nav label selected, Kit/Row title,
Kit/Row title current, Kit/Row number, Kit/Row rank, Kit/Figures, Kit/Setting title,
Kit/Sheet header title, Kit/Tile title, Kit/Context, Kit/Caption title, Kit/Compact title,
Kit/Bar title, Kit/Root title, Kit/Player title, Kit/Collection title, Kit/Artist title,
Kit/Player title w75, Kit/Player title w50, Kit/Collection title w75,
Kit/Recap card title, Kit/Favourites count, Kit/Browse title, Kit/Browse title large,
Kit/Stat value, Kit/FAB label, Kit/Play label, Kit/Lyric large, Kit/Lyric current,
Kit/Lyric line, Kit/Status bar time.
Recap/* (the cards' cardText sizes), First run/* (the look card; Comfort's in Onest) and System/* (Android's text).
Line height: Compose draws one line at the font's natural height (LineHeightStyle.Default trims the leading), so
every style but Doc/*, Recap/* and Kit/Lyric* has Auto line height, and a text that wraps takes the style's
"<name> Paragraph" partner with the code's line height (paragraph_line_heights; documented on the Type page).
(Onest is bundled for Comfort and the first run's Comfort picture; Expressive's own UI never sets it.)

Effect styles
-------------
Elevation/Level 2 (3dp: ExpressiveMenu shadowElevation), Elevation/Level 3 (6dp: a lifted row,
the FAB, the snackbar, a floating toolbar).

Components (main components on the pages above)
-----------------------------------------------
Shape/Cookie 9, Shape/Clover 4, Shape/Sunny 8, Shape/Puff, Shape/Burst (exact outlines from
Shapes.kt), Shape/Loader pentagon, Shape/Loader pill, Shape/Loader sunny, Shape/Loader four leaf,
Shape/Loader oval (the loading indicator's drawn approximations). Each is 96 x 96 with one vector
layer "Outline" filled Color/primary; scale the instance.
Icon/<ligature> for every glyph in Symbols.kt (90 on 2026-11-23) and Icon/<ligature> (filled) for FILLED_GLYPHS.

Helpers other modules may import
--------------------------------
tstyle(d, name, ...)       a text style that keeps its variable-font axes (see the kit note)
DocPage(d, page, ...)      a documentation page: title, intro, stacked white cards
shape_path(name, size)     SVG path data of an Expressive shape at a size
COVERS / cover_path(i)     the demo cover JPEGs
ALBUMS, COLOR_MODE, slug   demo albums by cover index and their Color modes
DocPage.below(d, page)     cards stacked under what a page already holds
paragraph_line_heights     wrapped text onto Paragraph partners (run before positioning anything from a height)
dialog_y, RENDER_DIFFERS, SHEET_INSET_NOTE, DIALOG_INSET_NOTE   overlay geometry on a phone and its shared notes
tidy_doc_pages, bind_section_strokes, document_paragraph_styles   end-of-build tidying (system_surfaces calls them)
"""
from __future__ import annotations

import math
import re
from pathlib import Path

from figbuild.pathfit import fit_closed
from figbuild.kit import Doc, V
from figbuild import icons as _icons

REFS: dict[str, str] = {}

REPO = Path(__file__).resolve().parents[3]
KT = REPO / "app/src/main/java/app/attic/player/ui/expressive"
COVERS = Path("/tmp/attic-dev/design-refs/covers")
COVER_FILES = {
    0: "0-paper-lanterns.jpg", 1: "1-tea-at-2-am.jpg", 2: "2-marea-lenta.jpg", 3: "3-glasshouse-summer.jpg",
    4: "4-room-tone.jpg", 5: "5-kolam.jpg", 6: "6-monsoon-tapes.jpg", 7: "7-kadalkarai-sessions.jpg",
    8: "8-iravu-rail.jpg", 9: "9-chhat-pe-dhoop.jpg", 10: "10-afterglow-protocol.jpg", 11: "11-weather-report.jpg",
}


COVER_CACHE = Path("/tmp/attic-design/covers")


def cover_path(index: int) -> str:
    """A demo cover (DemoCovers index, see covers/README.txt) as a 720 px JPEG. The reference folder is
    rewritten while renders are regenerated, so a copy is kept in /tmp/attic-design/covers and used when
    the folder is briefly missing."""
    name = COVER_FILES[index]
    src, cached = COVERS / name, COVER_CACHE / name
    if src.exists():
        if not cached.exists() or cached.stat().st_size != src.stat().st_size:
            COVER_CACHE.mkdir(parents=True, exist_ok=True)
            cached.write_bytes(src.read_bytes())
        return str(src)
    if cached.exists():
        return str(cached)
    raise FileNotFoundError(f"demo cover {name} is in neither {COVERS} nor {COVER_CACHE}")


# ───────────────────────────────────────────────────────────────── colour values
# expressive-base.json: dynamicLight/DarkColorScheme as the JVM harness (Robolectric SDK 35, AOSP
# default system_accent / system_neutral resources) returns them. (light, dark)
WALLPAPER = {
    "primary": ("#2A4174", "#B7CAFF"),
    "onPrimary": ("#FFFFFF", "#00143B"),
    "primaryContainer": ("#5E73A9", "#7A90C8"),
    "onPrimaryContainer": ("#FFFFFF", "#000000"),
    "inversePrimary": ("#B7CAFF", "#2A4174"),
    "secondary": ("#3C4255", "#C4CAE1"),
    "onSecondary": ("#FFFFFF", "#0F1626"),
    "secondaryContainer": ("#6E7488", "#8A90A5"),
    "onSecondaryContainer": ("#FFFFFF", "#000000"),
    "tertiary": ("#553A55", "#E4BFE2"),
    "onTertiary": ("#FFFFFF", "#240D26"),
    "tertiaryContainer": ("#8A6A89", "#A886A6"),
    "onTertiaryContainer": ("#FFFFFF", "#000000"),
    "error": ("#B3261E", "#F2B8B5"),
    "onError": ("#FFFFFF", "#601410"),
    "errorContainer": ("#F9DEDC", "#8C1D18"),
    "onErrorContainer": ("#410E0B", "#F9DEDC"),
    "background": ("#FAF8FF", "#121318"),
    "onBackground": ("#1A1B20", "#E2E2E9"),
    "surface": ("#FAF8FF", "#121318"),
    "onSurface": ("#1A1B20", "#FCFAFF"),
    "surfaceVariant": ("#E1E2EC", "#44464F"),
    "onSurfaceVariant": ("#40434B", "#C9CAD4"),
    "surfaceTint": ("#2A4174", "#B7CAFF"),
    "surfaceDim": ("#DAD9E0", "#121318"),
    "surfaceBright": ("#FAF8FF", "#38393F"),
    "surfaceContainerLowest": ("#FFFFFF", "#0C0E13"),
    "surfaceContainerLow": ("#F4F3FA", "#1A1B20"),
    "surfaceContainer": ("#EEEDF4", "#1E1F25"),
    "surfaceContainerHigh": ("#E8E7EF", "#282A2F"),
    "surfaceContainerHighest": ("#E2E2E9", "#33343A"),
    "outline": ("#5D5F67", "#A1A2AC"),
    "outlineVariant": ("#797A83", "#81838C"),
    "inverseSurface": ("#121318", "#FAF8FF"),
    "inverseOnSurface": ("#FCFAFF", "#1A1B20"),
    "scrim": ("#000000", "#000000"),
}

# expressive-cover-0.json: ArtScheme.roles(seed, dark) for Paper Lanterns.
# role: (palette, light hex, light tone, light chroma, dark hex, dark tone, dark chroma)
COVER_ROLES = {
    "primary": ("Primary", "#93432E", 38.5, 43.6, "#FFC3B4", 83.8, 34.6),
    "onPrimary": ("Primary", "#FFFBFA", 98.8, 4.5, "#3E1910", 14.2, 25.0),
    "primaryContainer": ("Primary", "#FFC7B9", 84.9, 28.4, "#783C2C", 32.7, 33.8),
    "onPrimaryContainer": ("Primary", "#5C2416", 22.3, 35.0, "#FFE2DA", 91.9, 18.1),
    "inversePrimary": ("Primary", "#FFC3B4", 83.8, 34.6, "#954732", 39.7, 42.1),
    "secondary": ("Secondary", "#794D41", 37.4, 23.0, "#F4BEB1", 81.4, 20.5),
    "onSecondary": ("Secondary", "#FFFBFA", 98.8, 4.5, "#3A1F18", 15.3, 17.6),
    "secondaryContainer": ("Secondary", "#FFD2C7", 87.8, 18.6, "#5A3328", 25.8, 23.1),
    "onSecondaryContainer": ("Secondary", "#573026", 24.6, 23.1, "#FFD9D0", 89.6, 16.7),
    "tertiary": ("Tertiary", "#645000", 35.0, 35.5, "#FAE7B3", 91.9, 21.0),
    "onTertiary": ("Tertiary", "#FFFBFA", 98.8, 4.5, "#524000", 28.1, 32.2),
    "tertiaryContainer": ("Tertiary", "#F3DC9D", 88.4, 26.1, "#F3DC9D", 88.4, 26.1),
    "onTertiaryContainer": ("Tertiary", "#453600", 23.4, 32.2, "#453600", 23.4, 32.2),
    "surface": ("Neutral", "#FFF2EF", 96.3, 8.5, "#200E09", 5.7, 13.6),
    "surfaceDim": ("Neutral", "#F7D3CA", 87.2, 13.0, "#200E09", 5.7, 13.6),
    "surfaceBright": ("Neutral", "#FFF2EF", 96.3, 8.5, "#54352D", 25.8, 17.6),
    "surfaceContainerLowest": ("Neutral", "#FDFDFD", 99.4, 3.6, "#130604", 2.5, 11.4),
    "surfaceContainerLow": ("Neutral", "#FFEAE5", 94.2, 10.2, "#29140E", 8.9, 15.2),
    "surfaceContainer": ("Neutral", "#FFE3DC", 92.2, 12.2, "#331A13", 12.4, 16.8),
    "surfaceContainerHigh": ("Neutral", "#FFDBD2", 90.1, 13.7, "#3C2019", 15.9, 18.3),
    "surfaceContainerHighest": ("Neutral", "#FDD3C9", 87.8, 15.3, "#482820", 20.0, 19.5),
    "onSurface": ("Neutral", "#291510", 9.5, 13.7, "#FFE6DF", 93.0, 9.2),
    "inverseSurface": ("Neutral", "#341F19", 14.2, 13.7, "#FFE6DF", 93.0, 9.2),
    "inverseOnSurface": ("Neutral", "#FFEAE5", 94.2, 9.2, "#3F2822", 18.8, 13.7),
    "onSurfaceVariant": ("NeutralVariant", "#604037", 30.4, 17.5, "#E3BBB1", 79.1, 15.0),
    "outline": ("NeutralVariant", "#977168", 51.3, 17.3, "#A7847B", 58.2, 15.3),
    "outlineVariant": ("NeutralVariant", "#E6BAAF", 79.1, 16.9, "#604037", 30.4, 17.5),
}
# What ArtScheme.colorScheme fills from other roles, and what it leaves to the app scheme.
COVER_FROM_ROLE = {"background": "surface", "onBackground": "onSurface",
                   "surfaceVariant": "surfaceContainerHighest", "surfaceTint": "primary"}
COVER_FROM_APP = ["error", "onError", "errorContainer", "onErrorContainer", "scrim"]
SEED = "#7C311D"            # deepenTint(swatch): HCT hue 33.09, chroma 44.37, tone 30.81
PALETTE_SWATCH = "#F07858"  # Palette vibrant swatch of the cover scaled to 96 x 96

# ArtScheme.roles(seed, dark) for every demo cover other than Paper Lanterns (the Cover collection's), from
# colors/expressive-cover-N.json: (album, seed, seed HCT hue, light hexes, dark hexes) in ART_ROLE_ORDER.
ART_ROLE_ORDER = ["primary", "onPrimary", "primaryContainer", "onPrimaryContainer", "inversePrimary", "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer", "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer", "surface", "surfaceDim", "surfaceBright", "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest", "onSurface", "inverseSurface", "inverseOnSurface", "onSurfaceVariant", "outline", "outlineVariant"]
ART = {
    1: ("Tea at 2 AM", "#1F7A6B", 181.41,
        "006759 F9FCFA A5E0D2 003D34 92E0CE 366057 F9FCFA C2E3DA 164139 17577C FBFBFF C0E2FF 003C59 ECF7F2 CADFD9 ECF7F2 FCFEFB E3F2EC DAEDE7 D1E8E1 C8E2DA 0A1E1A 142823 E4F1EC 314D47 648079 ADCAC2",
        "92E0CE 002A23 00574B CEEFE6 006A5C ACD2C8 0E2B26 19443C CAE8DF D4EBFF 0C4766 C0E2FF 003C59 031612 031612 26423C 010C09 061D19 092520 0E2D27 15362F E1EEE9 E1EEE9 1E312D B0C9C2 79918A 314D47"),
    2: ("Marea Lenta", "#7C581D", 75.96,
        "7C5200 FFFBFA FACD8E 4B3000 FDC87B 6E532C FFFBFA F7D8AF 4D3611 3C5B25 FCFCF4 CCE6B5 253E12 FFF2E6 EED7BC FFF2E6 FDFDFD FFECD6 FBE5CB F7DFC2 F2D8B7 241807 2F220F FDECD8 584428 8E7757 DBC09D",
        "FDC87B 342000 694604 FFE4C1 805500 E6C59A 34230A 503913 F9DDB9 DBEEC7 2F491C CCE6B5 253E12 1B1102 1B1102 4D3A1F 100801 241704 2C1E06 35250A 402D0F FAE8D5 FAE8D5 392B18 D9C0A1 9E896C 584428"),
    3: ("Glasshouse Summer", "#3D1F7A", 300.31,
        "644E97 FFFBFA DDCCFE 3C2C61 DAC9FF 5D5374 FFFBFA E3D7F6 3F3554 7D3E54 FFFBFA FFD3DE 5A2739 F9F2FE E0D7EB F9F2FE FDFDFD F3EBFB EEE5F8 E8DFF4 E2D8F0 1D1827 262231 F3ECF9 4B445B 7F768F CAC0DA",
        "DAC9FF 281E3F 544478 EEE3FF 665198 D1C5E7 292338 423857 E8DDF8 FFE0E7 673144 FFD3DE 5A2739 15101F 15101F 413A50 0B0713 1C1728 231D31 2B243A 342C45 F0E8F6 F0E8F6 302B3B C9C0D7 90889E 4B445B"),
    4: ("Room Tone", "#1D2F7C", 277.32,
        "47569D FEFBFF CBD2FF 273264 C7CEFF 515676 FEFBFF D7DAF8 333956 73416A FFFBFA FFD0F1 53294C F4F3FF D8D9EC F4F3FF FDFDFD EEEDFC E7E7F9 E0E1F6 D9DBF2 171A28 212332 EEEDFA 43465C 767991 C0C2DC",
        "C7CEFF 1B2241 404A7C E4E6FF 4B599D C5C8E8 212539 363B59 DEE0F9 FFDFF4 5F3357 FFD0F1 53294C 0F1220 0F1220 383C51 070913 151829 1C1F32 22263C 2B2F47 EBEAF7 EBEAF7 2A2D3C C1C3D9 888BA0 43465C"),
    5: ("Kolam", "#7C421D", 48.43,
        "8F4717 FFFBFA FFC9AB 592705 FFC5A5 774F37 FFFBFA FFD3BD 55321C 58550A FFFBF9 E5E1A2 3C3900 FFF2EC F5D4C3 FFF2EC FDFDFD FFEAE1 FFE3D6 FFDCCA FBD4C0 28160C 332014 FFEAE1 5F4130 967360 E4BCA6",
        "FFC5A5 3C1B06 753F1C FFE2D4 914B1C F1C0A6 392012 58341F FFDAC7 EEEAB7 474404 E5E1A2 3C3900 1F0E05 1F0E05 533726 130602 281409 311B0D 3B2112 462918 FFE6DA FFE6DA 3E291D E1BDAA A68574 5F4130"),
    6: ("Monsoon Tapes", "#1D7C4A", 157.2,
        "0B683B FAFCF7 B0DFBB 003E20 A2DFB3 3F5F49 FAFCF7 C8E3CC 21412C 005B6B F9FCFD B5E6F4 003E4A EEF7EC CEDFCF EEF7EC FDFEF9 E6F2E5 DDEDDE D5E8D7 CCE2CF 0F1E13 18271D E7F1E6 374D3D 69806E B2CAB6",
        "A2DFB3 052A16 1F5736 D4EFD8 196B3F B3D2B9 152B1D 24442F CFE8D3 CAEEF9 004957 B5E6F4 003E4A 07160C 07160C 2C4232 020B05 0B1D11 102417 162C1D 1D3625 E3EEE2 E3EEE2 223126 B5C9B7 7D9180 374D3D"),
    7: ("Kadalkarai Sessions", "#1D6A7C", 218.52,
        "006477 F9FCFD A7DDEE 003B47 94DCF1 375E69 F9FCFD C3E1EB 16404A 454F84 FEFBFF D8DCFF 2C3462 ECF6FA CBDEE4 ECF6FA FCFDFE E4F1F5 DBECF2 D2E6ED C9E0E8 0A1D22 14272C E5F0F4 324B53 657E87 AEC8D1",
        "94DCF1 002831 005565 CFEDF7 00677A AED0DB 0E2A31 19424D CCE6EF E5E6FF 373F6D D8DCFF 2C3462 03151A 03151A 274149 010B0F 061C22 09242A 0E2B33 15343D E2EDF1 E2EDF1 1E3036 B1C8CF 7A9097 324B53"),
    8: ("Iravu Rail", "#362E6B", 291.75,
        "5A519B FFFAFF D6CEFF 352E63 D3CBFF 595475 FFFAFF DED8F7 3B3756 7A3F5D FFFBFA FFD2E4 582740 F7F2FF DDD8EC F7F2FF FDFDFD F1ECFB EBE6F9 E5E0F5 DFD9F1 1B1928 242232 F1ECFA 48455C 7B7790 C6C1DB",
        "D3CBFF 242041 4D467A EAE4FF 5D549B CCC6E8 262439 3D3959 E4DEF9 FFE0EB 64324B FFD2E4 582740 13111F 13111F 3E3B51 090813 191728 201E32 28253B 302D46 EEE9F6 EEE9F6 2E2C3C C6C1D8 8D899F 48455C"),
    9: ("Chhat Pe Dhoop", "#7C1D21", 20.67,
        "934140 FFFBFA FFC6C3 5C2322 FFC2BF 794C4A FFFBFA FFD2CF 572F2E 6D4C01 FFFBFA FED99C 4C3300 FFF1F0 F6D2D0 FFF1F0 FDFDFD FFEAE8 FFE2E0 FFDAD8 FCD3D0 291514 341E1D FFEAE8 603F3E 97716F E6BAB7",
        "FFC2BF 3E1818 783B39 FFE1DF 954543 F3BDBB 3A1F1E 5A3230 FFD8D6 FFE4BC 593D00 FED99C 4C3300 200D0D 200D0D 543534 130605 291312 331918 3C201F 482726 FFE5E3 FFE5E3 3F2827 E2BBB8 A78381 603F3E"),
    10: ("Afterglow Protocol", "#1D7C5E", 169.32,
        "00684C F9FCF8 A9E0C7 003E2C 98E0C1 3A6050 F9FCF8 C4E4D3 1B4133 005975 FAFCFE B9E4FB 003D51 ECF7EF CCDFD4 ECF7EF FCFEFA E4F2E9 DBEDE2 D3E8DC CAE2D5 0C1E17 162820 E5F1E9 334D42 668073 AFCABC",
        "98E0C1 002A1D 0A5841 D0EFDF 006B4F AFD2C1 112B21 1E4436 CCE8D9 CDEDFE 00485F B9E4FB 003D51 05160F 05160F 294237 010C07 081D15 0C251C 112D22 18362A E2EEE6 E2EEE6 20312A B2C9BD 7A9186 334D42"),
    11: ("Weather Report", "#462A6F", 305.4,
        "694C95 FFFBFA E1CBFD 402B5F DFC7FF 605273 FFFBFA E5D7F5 413553 7E3E4E FFFBFA FFD3DA 5B2734 FAF1FE E2D7EA FAF1FE FDFDFD F5EBFA F0E5F8 EADEF4 E4D8F0 1E1827 282131 F4EBF9 4D445A 81768E CCBFD9",
        "DFC7FF 2B1E3E 584376 F1E2FF 6C5096 D4C4E5 2B2237 443756 EADCF7 FFE0E5 68313F FFD3DA 5B2734 16101E 16101E 42394F 0C0712 1D1627 251D30 2C243A 362C44 F1E8F5 F1E8F5 322B3B CBC0D6 92889E 4D445A"),
}
# Comfort.colors tokens the first run's look card draws its Comfort picture in (colors/comfort-base.json).
COMFORT_PREVIEW = {
    "ground": ("#F2F1EE", "#000000"),
    "card": ("#FFFFFF", "#171716"),
    "raised": ("#FFFFFF", "#232221"),
    "fill": ("#EEECE8", "#2C2B29"),
    "on": ("#191817", "#F5F4F1"),
    "on2": ("#67645F", "#AAA7A2"),
    "accent": ("#F2A024", "#FFB347"),
    "onAccent": ("#2A1800", "#2A1800"),
    "accentSoft": ("#FBE8C9", "#3A2A13"),
    "onAccentSoft": ("#7A4500", "#FFCD85"),
}

# The demo albums by cover index (DemoCovers.kt), and the Color mode that draws each one's scheme.
ALBUMS = {i: a for i, (a, *_) in ART.items()}
ALBUMS[0] = "Paper Lanterns"
ALBUMS = dict(sorted(ALBUMS.items()))
COLOR_MODE = {i: ("Cover" if i == 0 else a) for i, a in ALBUMS.items()}
# The roles the Colour page shows for each cover.
OTHER_ROLES = ["primary", "primary-container", "secondary-container", "tertiary-container", "surface-container-high", "surface"]


def slug(album: str) -> str:
    """An album's variable group in the Art collection: "Iravu Rail" -> "iravu-rail"."""
    return album.lower().replace(" ", "-").replace("&", "and")


def art_hex(index: int, role: str, dark: bool) -> str:
    """A role's raw value in cover `index`'s scheme (0 is Paper Lanterns, the Cover collection's)."""
    if index == 0:
        r = COVER_FROM_ROLE.get(role, role)
        if r in COVER_ROLES:
            return COVER_ROLES[r][4 if dark else 1]
        return WALLPAPER[role][1 if dark else 0]
    album, seed, hue, light, dk = ART[index]
    r = COVER_FROM_ROLE.get(role, role)
    if r not in ART_ROLE_ORDER:
        return WALLPAPER[role][1 if dark else 0]
    return "#" + (dk if dark else light).split()[ART_ROLE_ORDER.index(r)]


# Brand colours (res/values/colors.xml): the launcher icon and splash.
BRAND = [("headroom/ground", "#C6533A", "headroom_ground", "the launcher icon's background and the splash icon's circle"),
         ("headroom/cabinet", "#F5EDDD", "headroom_cabinet", "the mark's cabinet in colour"),
         ("headroom/cone", "#2A211C", "headroom_cone", "the woofer cone and tweeter in colour")]

# Android's own chrome, drawn only to show Attic's surfaces in place (System surfaces page).
PHONE = [
    ("backdrop", "#C9CED6", "#2E3138", "the plain ground the widget renders sit on (16dp around the widget)"),
    ("wallpaper", "#B9C3D9", "#262B36", "a stand-in home-screen and lock-screen wallpaper"),
    ("wallpaper-deep", "#8E9BBA", "#3A4256", "the wallpaper's second tone"),
    ("shade", "#ECEEF4", "#16181D", "the notification shade's ground"),
    ("tile", "#DCE1EC", "#2C3038", "a quick settings tile that is off"),
    ("tile-on", "#2A4174", "#B7CAFF", "a quick settings tile that is on (the system accent)"),
    ("on-tile", "#1B1B1F", "#E3E2E9", "glyphs and text on an off tile and on the shade"),
    ("on-tile-on", "#FFFFFF", "#00143B", "glyphs on an on tile"),
    ("card", "#FFFFFF", "#22252B", "another app's notification card"),
    ("icon", "#E2E5EE", "#3A3E47", "another app's launcher icon"),
    ("on-icon", "#5C6274", "#C4C8D4", "the glyph on another app's icon"),
    ("on-wallpaper", "#1B1B1F", "#FFFFFF", "clock, labels and the date over the wallpaper"),
]

# The phone's chrome as the render harnesses draw it (System collection).
SYSTEM = [
    ("status-bar-ink", "#1B1B1F", "#FFFFFF", "Status bar glyphs and clock as the render harness draws them (storeShots "
     "ShotHarness.kt SystemBars). Phone chrome, not an Attic role."),
    ("keyboard-ground", "#E4E5EA", "#1F1F22", "The stand-in keyboard's ground as the render harness draws it "
     "(designrefs DesignHarness.kt Keyboard). Phone chrome, not an Attic role."),
    ("keyboard-key", "#FFFFFF", "#3A3A3F", "The stand-in keyboard's keys (DesignHarness.kt Keyboard). Phone chrome."),
]

# Recap's colour mixes: lerp(surfaceContainer, primary, t) in the card's scheme.
RECAP_MIXES = [("recap/dial-bar", 0.58, "The listening clock's bars other than the busiest hour.",
                "lerp(surfaceContainer, primary, 0.58f) in ListeningDial")]
RECAP_MIXES += [(f"recap/language-{n}", t, f"Recap's languages bar, language {n}.",
                 f"lerp(surfaceContainer, primary, {t}f) in ExpressiveRecap.kt Languages")
                for n, t in zip(range(2, 6), (0.7, 0.48, 0.32, 0.2))]
RECAP_MIXES += [(f"recap/tile-{n}", t, f"The languages card's treemap, tile {n}.",
                 f"lerp(surfaceContainer, primary, {t}f) in ExpressiveRecapCards.kt LanguagesCard")
                for n, t in zip(range(2, 5), (0.68, 0.34, 0.21))]

FAMILIES = [
    ("Primary", ["primary", "onPrimary", "primaryContainer", "onPrimaryContainer", "inversePrimary"]),
    ("Secondary", ["secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer"]),
    ("Tertiary", ["tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer"]),
    ("Error", ["error", "onError", "errorContainer", "onErrorContainer"]),
    ("Surface", ["surface", "onSurface", "surfaceVariant", "onSurfaceVariant", "surfaceDim", "surfaceBright", "surfaceTint"]),
    ("Surface containers", ["surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest"]),
    ("Outline, inverse and scrim", ["outline", "outlineVariant", "inverseSurface", "inverseOnSurface", "background", "onBackground", "scrim"]),
]

# Where the code spends each role, from kit.css's rules and KIT.md. Shown on the Colour page.
ROLE_USE = {
    "primary": "the one filled action, selected states, the seek wave",
    "secondaryContainer": "nav indicator, selected chip, the playing row, tonal buttons",
    "tertiaryContainer": "Favourites and nothing else",
    "surface": "every screen",
    "surfaceContainer": "group cells, the nav bar, Now Playing tools",
    "surfaceContainerHigh": "pills, search bar, mini player, unselected segments",
    "surfaceContainerHighest": "no-art covers, a lifted row, segments on a cell",
    "onSurfaceVariant": "subtitles, standard icon buttons",
    "outlineVariant": "outlined buttons, chip borders, the rest of the seek track",
}


# Where a screen follows the phone rather than the reference render: one annotation category and one wording.
# The render harness opens dialog and sheet windows with no window insets; the phone gives them the system bars'.
RENDER_DIFFERS = "Reference render differs"
STATUS_BAR, NAV_BAR = 40, 24   # the system bars' heights in this file (size/status-bar, size/gesture-bar)
SHEET_INSET_NOTE = ("The reference render gives the sheet's window no insets, so there the sheet ends at the screen's "
                    "foot. On a phone ModalBottomSheet pads its content by the navigation bar (BottomSheetDefaults."
                    "windowInsets), as drawn here: the sheet is 24dp taller and its rows sit 24dp higher than in the render.")
DIALOG_INSET_NOTE = ("The reference render gives the dialog's window no insets, so there the dialog is centred on the "
                     "whole screen. On a phone the dialog window fits between the status bar and the navigation bar and "
                     "centres there, as drawn here: 8dp lower than in the render.")


def dialog_y(height: float, screen_h: float = 914) -> float:
    """A dialog's top on a phone: centred between the status bar and the navigation bar."""
    return round(STATUS_BAR + (screen_h - STATUS_BAR - NAV_BAR - height) / 2, 1)


DIM: dict[str, str] = {}   # Dimension variable name -> id, filled by build_variables


def bind_dim(d: Doc, ids, field: str, name: str):
    """Bind a layer field (cornerRadius, gap, width...) to a Dimension variable by name, e.g. "radius/large"."""
    d.bind(ids, field, DIM[name])


def kebab(camel: str) -> str:
    return re.sub(r"([A-Z])", lambda m: "-" + m.group(1).lower(), camel)


# ───────────────────────────────────────────────────────────────── text styles
def _opsz(size: float) -> int:
    """GoogleSansFlex.opticalSizeFor in Type.kt."""
    for limit, o in ((12, 12), (14, 14), (18, 16), (24, 22), (30, 28), (40, 36)):
        if size <= limit:
            return o
    return 45


def tstyle(d: Doc, name: str, *, size: float, line: float, weight: int, tracking: float = 0.0,
           round: bool = False, width: int = 100, opsz: int | None = None, features=None,
           family="Google Sans Flex", description: str | None = None, update: str | None = None) -> str:
    """A text style that keeps its variable-font axes.

    FigCraft's `style.create {value}` drops `fontVariations` (kit.text_style passes them there, so its
    styles all render at the default weight). A style created `from` a text layer keeps them, so the
    style is made from a temporary layer that is deleted afterwards. With `update`, the existing style
    of that id is redefined instead."""
    variations = {"wght": weight, "wdth": width, "ROND": 100 if round else 0, "opsz": opsz or _opsz(size)}
    code_line = line
    if natural_line(name):
        # Compose draws one line at the font's own height (LineHeightStyle.Default trims the leading): Auto here.
        line = "auto"
        if description:
            description += (f" Line height Auto here: Compose draws a single line at the font's natural height; the "
                            f"code's {code_line:g} applies between wrapped lines.")
    props = dict(fontFamily=family, fontSize=size, lineHeight=line, letterSpacing=tracking,
                 fontVariations=variations)
    if features:
        props["features"] = features
    tmp = d.node("text", None, name="Style source", text="Ag", **props)
    try:
        if update:
            d.cmd("style.update", style=update, **{"from": tmp})
            sid = update
            if description:
                d.cmd("style.update", style=sid, description=description)
        else:
            args = {"type": "text", "name": name, "from": tmp}
            if description:
                args["description"] = description
            sid = d.cmd("style.create", **args)["id"]
    finally:
        d.cmd("edit.delete", ids=[tmp])
    d.text_styles[name] = sid
    if natural_line(name):
        LINE_METRICS[sid] = (name, code_line)
    return sid


# Text styles whose line height stays the code's value: documentation styles, Recap's card type (pictures, set
# against the renders) and the lyric lines, which run as paragraphs. Every other style is Auto (see natural_line).
KEEP_LINE = ("Doc/", "Recap/", "Kit/Lyric")
LINE_METRICS: dict[str, tuple] = {}   # style id -> (name, the code's line height), for Auto styles


def natural_line(name: str) -> bool:
    """Compose sets text with LineHeightStyle.Default (Alignment.Proportional, Trim.Both) when a style names none, as
    Type.kt's do: a single line is the font's natural height (about 1.23 x the size for Google Sans Flex), and the
    style's line height applies only between the lines of a wrapped text. Figma can't trim, so these styles use Auto
    (the natural height) and paragraph_line_heights() gives a text that wraps the code's line height."""
    return not name.startswith(KEEP_LINE)


_NATURAL: dict[str, float] = {}
PARAGRAPH: dict[str, str] = {}        # base style id -> its Paragraph partner's id
PARAGRAPH_BASE: dict[str, str] = {}   # partner id -> base style id


def paragraph_style(d: Doc, base: str) -> str:
    """`<base> Paragraph`: the base style with the code's line height, for text that wraps. Made from a layer set in
    the base style, as tstyle does, so it keeps the variable-font axes."""
    if base in PARAGRAPH:
        return PARAGRAPH[base]
    name, line = LINE_METRICS[base]
    t = d.node("text", None, name="Style source", text="Ag")
    try:
        d.cmd("style.apply", ids=[t], style=base)
        d.set(t, lineHeight=line)
        sid = d.cmd("style.create", type="text", name=f"{name} Paragraph", **{"from": t},
                    description=f"{name} for text that wraps onto more than one line: the code's {line:g} between lines. "
                                "Compose also trims the leading above the first line and below the last, which Figma "
                                "can't, so a wrapped block here is one half-leading pair taller than on the phone.")["id"]
    finally:
        d.cmd("edit.delete", ids=[t])
    d.text_styles[f"{name} Paragraph"] = sid
    PARAGRAPH[base], PARAGRAPH_BASE[sid] = sid, base
    return sid


def paragraph_line_heights(d: Doc, pages=None, roots=None):
    """Put every text layer that wraps onto more than one line on its style's Paragraph partner (the code's line
    height), and a single line back on the base style (Auto), so a wrapped paragraph is spaced as on the phone. Runs on
    the component pages before the screens are built, on a screen's content before it is positioned from its height,
    and once over everything at the end. `roots`: layer ids instead of pages. Returns how many layers changed."""
    if not LINE_METRICS:
        return 0
    for sid in LINE_METRICS:
        if sid not in _NATURAL:
            t = d.node("text", None, name="Metric source", text="Ag")
            d.cmd("style.apply", ids=[t], style=sid)
            _NATURAL[sid] = d.get(t)["height"]
            d.cmd("edit.delete", ids=[t])
    if roots is None:
        pages = pages or [n for n in d.pages if n.startswith("Components / ") or n in ("Screens", "System surfaces")]
        roots = [d.pages[p] for p in pages]
    changed = 0
    for _ in range(2):   # twice: a main component's change reaches its instances before they are measured again
        changed += _paragraph_pass(d, roots)
    return changed


def _paragraph_pass(d: Doc, roots) -> int:
    changed = 0
    for root in roots:
        tree = d.tool("get_node", id=root, depth=80, fields=["type", "height", "styles"])
        stack = [tree]
        while stack:
            n = stack.pop()
            stack.extend(n.get("children") or [])
            if n.get("type") != "TEXT":
                continue
            sid = (n.get("styles") or {}).get("text")
            base = PARAGRAPH_BASE.get(sid, sid)
            if base not in LINE_METRICS:
                continue
            wrapped = n["height"] > _NATURAL[base] * 1.5
            want = paragraph_style(d, base) if wrapped else base
            if want != sid:
                d.cmd("style.apply", ids=[n["id"]], style=want)
                changed += 1
    return changed


def document_paragraph_styles(d: Doc):
    """A Type page card for the Paragraph partners the build made (run after the last paragraph pass)."""
    if not PARAGRAPH:
        return
    p = DocPage.below(d, "Type")
    c = p.card("Paragraph partners",
               "The styles text takes when it wraps: the same type with the code's line height between lines. "
               "paragraph_line_heights() in foundations.py puts a wrapped text layer on its partner and a single line on "
               "the base style; do the same when a text in a design starts or stops wrapping.",
               "Type.kt · style(size, line, weight)")
    rows = vcol(d, c, "Specimens", gap=20)
    for base, sid in sorted(PARAGRAPH.items(), key=lambda kv: LINE_METRICS[kv[0]][0]):
        name, line = LINE_METRICS[base]
        t = _specimen(d, rows, f"{name} Paragraph", "Saves favourites, playlists, listening history, saved lyrics and "
                      "settings in one file. Keep it somewhere other than this phone.",
                      f"line height {line:g}; otherwise {name}", f"{name}, wrapped")
        d.set(t, width=480)
    p.done(c)


# (name, Compose, size, line, weight, tracking, round, opsz)
M3_STYLES = [
    ("Display/Large", "displayLarge", 57, 64, 400, -0.25),
    ("Display/Medium", "displayMedium", 45, 52, 400, -0.5),
    ("Display/Small", "displaySmall", 36, 44, 400, -0.3),
    ("Headline/Large", "headlineLarge", 32, 40, 400, 0.0),
    ("Headline/Medium", "headlineMedium", 28, 36, 400, 0.0),
    ("Headline/Small", "headlineSmall", 24, 32, 400, 0.0),
    ("Title/Large", "titleLarge", 22, 28, 400, 0.0),
    ("Title/Medium", "titleMedium", 16, 24, 500, 0.1),
    ("Title/Small", "titleSmall", 14, 20, 500, 0.1),
    ("Body/Large", "bodyLarge", 16, 24, 400, 0.0),
    ("Body/Medium", "bodyMedium", 14, 20, 400, 0.0),
    ("Body/Small", "bodySmall", 12, 16, 400, 0.0),
    ("Label/Large", "labelLarge", 14, 20, 500, 0.0),
    ("Label/Medium", "labelMedium", 12, 16, 500, 0.0),
    ("Label/Small", "labelSmall", 11, 16, 500, 0.0),
]
EM_WEIGHT = {"displayLarge": 700, "displayMedium": 700, "displaySmall": 700, "headlineLarge": 700,
             "headlineMedium": 700, "headlineSmall": 700, "titleLarge": 700, "titleMedium": 600,
             "titleSmall": 600, "bodyLarge": 600, "bodyMedium": 600, "bodySmall": 700,
             "labelLarge": 600, "labelMedium": 600, "labelSmall": 700}

# (name, size, line, weight, tracking, round, opsz, features, Compose expression, where)
KIT_STYLES = [
    ("Kit/Button", 14, 20, 600, 0, False, 14, None, "labelLarge.copy(fontWeight = SemiBold)",
     "ExpressiveButton (Small), SplitButton, InstantMixPill, mini player title, SettingsSection, ListLabel, a selected ConnectedButtonGroup segment"),
    ("Kit/Button medium", 16, 24, 600, 0, False, 14, None, "labelLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = SemiBold)",
     "ExpressiveButton (Medium)"),
    ("Kit/Quick action", 12, 16, 600, 0, False, 12, None, "labelMedium.copy(fontWeight = SemiBold)",
     "the track menu's quick actions (ExpressiveSheets.kt QuickAction)"),
    ("Kit/Nav label selected", 12, 16, 700, 0, False, 12, None, "labelMedium.copy(fontWeight = Bold)",
     "the selected navigation bar label"),
    ("Kit/Row title", 16, 22, 500, 0, False, 16, None, "bodyLarge.copy(fontWeight = Medium, lineHeight = 22.sp)",
     "SongRow and CollectionRow titles"),
    ("Kit/Row title current", 16, 22, 700, 0, False, 16, None, "bodyLarge.copy(fontWeight = Bold, lineHeight = 22.sp)",
     "the current song's row title"),
    ("Kit/Row number", 14, 20, 500, 0, False, 14, {"tnum": True}, "labelLarge.copy(fontFeatureSettings = \"tnum\")",
     "a track number leading a row"),
    ("Kit/Row rank", 14, 20, 600, 0, False, 14, {"tnum": True}, "labelLarge.copy(fontWeight = SemiBold, fontFeatureSettings = \"tnum\")",
     "a rank leading a row"),
    ("Kit/Figures", 12, 16, 500, 0, False, 12, {"tnum": True}, "labelMedium.copy(fontFeatureSettings = \"tnum\")",
     "SeekTimes, RowCount"),
    ("Kit/Setting title", 16, 24, 500, 0, False, 16, None, "bodyLarge.copy(fontWeight = Medium)",
     "SetRow and the settings rows' titles"),
    ("Kit/Sheet header title", 16, 22, 600, 0, False, 16, None, "bodyLarge.copy(fontWeight = SemiBold, lineHeight = 22.sp)",
     "SheetHeader's song title"),
    ("Kit/Tile title", 14, 20, 500, 0, False, 14, None, "titleSmall.copy(letterSpacing = 0.sp)",
     "Tile titles"),
    ("Kit/Context", 14, 20, 600, 0, False, 14, None, "titleSmall.copy(fontWeight = SemiBold, letterSpacing = 0.sp)",
     "Now Playing's \"Playing from\" context and the line sung over the title"),
    ("Kit/Caption title", 16, 22, 600, 0.1, False, 16, None, "titleMedium.copy(fontWeight = SemiBold, lineHeight = 22.sp)",
     "HeroCaption"),
    ("Kit/Compact title", 16, 22, 600, 0, False, 16, None, "titleMedium.copy(fontWeight = SemiBold, lineHeight = 22.sp, letterSpacing = 0.sp)",
     "Now Playing's compact header, the lyrics screen's bar"),
    ("Kit/Bar title", 22, 28, 500, 0, False, 22, None, "titleLarge.copy(fontWeight = Medium)",
     "DetailAppBar and the collapsed collection bar"),
    ("Kit/Root title", 36, 44, 700, -0.4, True, 36, None, "Expressive.type.displaySmallEmphasized.copy(letterSpacing = (-0.4).sp)",
     "RootAppBar before it scrolls, the Settings title"),
    ("Kit/Player title", 32, 40, 700, -0.3, True, 36, None, "Expressive.type.headlineLargeEmphasized.copy(letterSpacing = (-0.3).sp)",
     "Now Playing's song title"),
    ("Kit/Player title w75", 32, 40, 700, -0.3, True, 36, None, "Kit/Player title .narrowed(75)",
     "Now Playing's title when it does not fit at full width (NarrowingText)"),
    ("Kit/Player title w50", 32, 40, 700, -0.3, True, 36, None, "Kit/Player title .narrowed(50)",
     "Now Playing's title when it does not fit at width 75 either (NarrowingText)"),
    ("Kit/Collection title", 32, 36, 700, -0.4, True, 36, None, "headlineLargeEmphasized.copy(lineHeight = 36.sp, letterSpacing = (-0.4).sp)",
     "an album or playlist header"),
    ("Kit/Collection title w75", 32, 36, 700, -0.4, True, 36, None, "Kit/Collection title .narrowed(75)",
     "a collection title too wide for one line at full width (HeaderTitle), wrapping to three lines at most"),
    ("Kit/Artist title", 45, 48, 700, -0.8, True, 45, None, "displayMediumEmphasized.copy(lineHeight = 48.sp, letterSpacing = (-0.8).sp)",
     "an artist header"),
    ("Kit/Recap card title", 24, 28, 700, -0.2, True, 22, None, "headlineSmallEmphasized.copy(lineHeight = 28.sp, letterSpacing = (-0.2).sp)",
     "Home's Recap card"),
    ("Kit/Favourites count", 22, 24, 700, 0, True, 22, None, "titleLargeEmphasized.copy(lineHeight = 24.sp)",
     "FavouritesPill's count"),
    ("Kit/Browse title", 16, 20, 600, 0, False, 16, None, "titleMedium.copy(fontWeight = SemiBold, lineHeight = 20.sp, letterSpacing = 0.sp)",
     "BrowseTile"),
    ("Kit/Browse title large", 18, 24, 700, 0, True, 16, None, "titleMediumEmphasized.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = Bold, letterSpacing = 0.sp)",
     "BrowseTile with a subtitle (Search's folder tile)"),
    ("Kit/Stat value", 28, 36, 700, -0.3, True, 28, None, "headlineMediumEmphasized.copy(letterSpacing = (-0.3).sp)",
     "StatTile"),
    ("Kit/FAB label", 16, 24, 600, 0.1, False, 16, None, "titleMedium.copy(fontWeight = SemiBold)",
     "ExpressiveExtendedFab"),
    ("Kit/Play label", 16, 24, 600, 0, False, 16, None, "bodyLarge.copy(fontWeight = SemiBold)",
     "the collection header's Play / Pause"),
    ("Kit/Lyric large", 28, 36, 700, 0, True, 22, None, "titleLargeEmphasized.copy(fontSize = 28.sp, lineHeight = 36.sp)",
     "LyricLine(large = true)"),
    ("Kit/Lyric current", 34, 40, 700, -0.3, True, 36, None, "displaySmallEmphasized family, 34/40, weight 700, letterSpacing -0.3",
     "the line being sung on the lyrics screen (ExpressiveLyrics.kt SyncedLine)"),
    ("Kit/Lyric line", 24, 32, 500, 0, False, 22, None, "headlineSmall family, 24/32, weight 500 (also headlineSmall.copy(fontWeight = Medium) for plain lyrics)",
     "lines before and after the current one on the lyrics screen, plain lyrics"),
    ("Kit/Status bar time", 15, 20, 500, 0, False, 14, None, "labelLarge.copy(fontSize = 15.sp, fontWeight = Medium)",
     "the clock in the render harness's status bar (storeShots ShotHarness.kt SystemBars), not app UI"),
]


RECAP_STYLES = {
    # name: (size, line, weight, width, round, tracking em, features, where)
    "Intro headline": (54, 52, 800, 100, True, -0.02, None, "IntroCard: headline(54f, 52f)"),
    "Intro body": (17, 24, 500, 100, False, 0, None, "IntroCard body"),
    "Intro digit": (104, 104, 800, 100, True, 0, None, "IntroCard: one character per shape"),
    "Label": (18, 24, 600, 100, False, 0, None, "CardLabel: \"Your top song\""),
    "Minutes figure": (156, 162, 800, 50, True, -0.01, None, "MinutesCard: the minutes. The code asks for a 128 line; a "
                       "single line set smaller than the font's own height lays out at that height (162), as the renders show"),
    "Minutes unit": (49, 52, 800, 100, True, 0, None, "MinutesCard: \"minutes\""),
    "Body large": (18, 26, 500, 100, False, 0, None, "MinutesCard: hours and days"),
    "Song headline": (34, 36, 800, 100, True, -0.02, None, "TopSongCard: the title (NarrowingText)"),
    "Caption": (15, 20, 500, 100, False, 0, None, "TopSongCard: the artist"),
    "Badge figure": (36, 34, 800, 100, True, 0, None, "TopSongCard: plays in the sunny badge"),
    "Badge word": (13, 16, 700, 100, True, 0, None, "TopSongCard: \"plays\""),
    "List headline": (36, 38, 800, 100, True, -0.02, None, "TopSongsCard and TopArtistsCard headlines"),
    "Rank": (22, 22, 800, 100, True, 0, None, "a rank on the top songs and top artists cards"),
    "Row title large": (20, 23, 800, 100, True, 0, None, "TopSongsCard: the first song"),
    "Row title": (16, 21, 700, 100, False, 0, None, "TopSongsCard and TopArtistsCard rows"),
    "Row subtitle": (13, 17, 500, 100, False, 0, None, "an artist or minutes under a row title"),
    "Row figure": (19, 20, 800, 100, False, 0, {"tnum": True}, "TopSongsCard: plays"),
    "Row figure word": (10, 12, 600, 100, False, 0, None, "TopSongsCard: \"plays\""),
    "Artist headline": (52, 60, 800, 82, True, -0.02, None, "TopArtistCard: the name, narrowed to 82"),
    "Body": (16, 23, 500, 100, False, 0, None, "TopArtistCard and ClockCard body"),
    "Artist name": (24, 28, 800, 100, True, 0, None, "TopArtistsCard: number one"),
    "Row body": (14, 20, 500, 100, False, 0, None, "TopArtistsCard: number one's minutes"),
    "Language 1": (68, 66, 800, 50, True, 0, None, "LanguagesCard: the first tile's share"),
    "Language 2": (60, 56, 800, 72, True, 0, None, "LanguagesCard: the second tile's share"),
    "Language 3": (44, 42, 800, 72, True, 0, None, "LanguagesCard: the third tile's share"),
    "Language 4": (24, 26, 800, 72, True, 0, None, "LanguagesCard: the fourth tile's share"),
    "Language name 1": (28, 34, 800, 100, True, 0, None, "LanguagesCard: the first tile's language"),
    "Language name 2": (20, 26, 700, 100, True, 0, None, "LanguagesCard: the second tile's language"),
    "Language name 3": (16, 22, 700, 100, True, 0, None, "LanguagesCard: the third tile's language"),
    "Language name 4": (12, 16, 600, 100, True, 0, None, "LanguagesCard: the fourth tile's language"),
    "Clock headline": (56, 58, 800, 100, True, -0.02, None, "ClockCard: the listener type"),
    "Dial label": (12.4, 12.4, 600, 100, False, 0, None, "ListeningDial on the card: 11 units of 288 at 324dp"),
    "Dial label strong": (12.4, 12.4, 800, 100, False, 0, None, "ListeningDial on the card: the busiest hour"),
    "Dial label page": (10.2, 10.2, 600, 100, False, 0, None, "ListeningDial on the page: 17 units of 288 at 172dp"),
    "Dial label page strong": (10.2, 10.2, 800, 100, False, 0, None, "ListeningDial on the page: the busiest hour"),
    "Highlights headline": (48, 60, 800, 100, True, -0.02, None, "HighlightsCard headline. The code asks for a 48 line; "
                            "one line set at the font's size lays out at its own height (60), as the renders show"),
    "Highlight value": (40, 42, 800, 80, True, 0, None, "HighlightsCard: a figure"),
    "Highlight line": (14, 19, 500, 100, False, 0, None, "HighlightsCard: the line under a figure"),
    "Summary headline": (33, 38, 800, 100, True, -0.02, None, "SummaryCard headline"),
    "Small label": (13, 16, 600, 100, False, 0, None, "SummaryCard labels, the footer's period"),
    "Summary name": (19, 24, 800, 100, True, 0, None, "SummaryCard: top song and artist"),
    "Summary minutes": (56, 58, 800, 66, True, 0, None, "SummaryCard: minutes"),
    "Summary language": (30, 34, 800, 100, True, 0, None, "SummaryCard: \"Mostly in\""),
    "Footer name": (16, 20, 700, 100, True, 0, None, "CardFrame: \"Attic\" in the footer"),
    "Page minutes": (57, 60, 800, 88, True, -0.0175, {"tnum": True}, "ExpressiveRecap.kt Stats: the minutes"),
    "Page stat": (28, 32, 700, 100, True, -0.3 / 28, {"tnum": True}, "ExpressiveRecap.kt StatShape and the streak"),
    "Page hero": (32, 36, 700, 100, True, -0.3 / 32, None, "ExpressiveRecap.kt Headline: headlineLargeEmphasized, lineHeight 36"),
    "Page clock title": (24, 28, 700, 100, True, 0, None, "ExpressiveRecap.kt WhenYouListen: headlineSmallEmphasized, lineHeight 28"),
}


# (name, size, line, weight, family, description): the first run's look card and Android's own text.
EXTRA_STYLES = [
    ("First run/Look label", 16, 24, 600, "Google Sans Flex",
     "titleMedium.copy(fontWeight = SemiBold): the look card's style name (ExpressiveOnboarding.kt LookCard)."),
    ("First run/Comfort hero", 34, 40, 700, "Onest",
     "Comfort.type.hero, for the Comfort picture on the look card only (ComfortLookPreview)."),
    ("First run/Comfort tab", 15, 20, 500, "Onest",
     "onest(15f, 20f, 500): Comfort's SubTabs label, for the Comfort picture on the look card only."),
    ("First run/Comfort tab selected", 15, 20, 700, "Onest",
     "onest(15f, 20f, 700): the selected SubTabs label, for the Comfort picture on the look card only."),
    ("First run/Comfort dock", 14, 18, 500, "Onest",
     "onest(14f, 18f, 500): Comfort's TabBar label, for the Comfort picture on the look card only."),
    ("First run/Comfort dock selected", 14, 18, 700, "Onest",
     "onest(14f, 18f, 700): the selected TabBar label, for the Comfort picture on the look card only."),
    ("System/Title", 16, 22, 500, "Google Sans Flex",
     "Android's own text (Roboto on the phone): the media notification's title and widget text. System, not Attic's type."),
    ("System/Body", 14, 20, 400, "Google Sans Flex",
     "Android's own text (Roboto on the phone): the notification's artist line and the shade. System, not Attic's type."),
    ("System/Small", 12, 16, 400, "Google Sans Flex",
     "Android's own small text: app name and time in the notification, the lock screen's date. System."),
    ("System/Clock", 72, 80, 400, "Google Sans Flex",
     "The lock screen clock as Android draws it. System, not Attic's type."),
    ("System/Widget title", 16, 20, 700, "Google Sans Flex",
     "WidgetText.Expressive.Title: the system font in bold (16sp wide, 18sp large, 13sp small; set per size)."),
    ("System/Widget title large", 18, 22, 700, "Google Sans Flex",
     "WidgetText.Expressive.Title at 18sp (widget_expressive_large.xml)."),
    ("System/Widget title small", 13, 16, 700, "Google Sans Flex",
     "WidgetText.Expressive.Title at 13sp, two lines (widget_expressive_small.xml)."),
    ("System/Widget artist", 13, 16, 400, "Google Sans Flex",
     "WidgetText.Expressive.Artist at 13sp (wide) and 14sp (large)."),
    ("System/Widget artist large", 14, 18, 400, "Google Sans Flex",
     "WidgetText.Expressive.Artist at 14sp (widget_expressive_large.xml)."),
]
EXTRA_TRACKING = {"First run/Look label": 0.1}
EXTRA_SAMPLES = {"First run/Look label": "Expressive", "First run/Comfort hero": "Good evening",
                 "First run/Comfort tab": "Songs", "First run/Comfort dock": "Library", "System/Clock": "10:24",
                 "System/Small": "Attic · now", "System/Body": "Mira Okafor"}
RECAP_SAMPLES = {"Intro digit": "2026", "Minutes figure": "18,420", "Minutes unit": "minutes", "Badge figure": "96",
                 "Badge word": "plays", "Rank": "1", "Row figure": "96", "Row figure word": "plays",
                 "Artist headline": "Mira Okafor", "Artist name": "Mira Okafor", "Language 1": "41%", "Language 2": "29%",
                 "Language 3": "21%", "Language 4": "9%", "Language name 1": "English", "Language name 2": "Tamil",
                 "Language name 3": "Hindi", "Language name 4": "No vocals", "Clock headline": "Night owl",
                 "Dial label": "12", "Dial label strong": "9pm", "Dial label page": "12", "Dial label page strong": "9pm",
                 "Highlight value": "14", "Summary minutes": "18,420", "Summary language": "English", "Footer name": "Attic",
                 "Page minutes": "18,420", "Page stat": "312", "Small label": "Top song", "Label": "Your top song",
                 "Intro headline": "Your 2026 so far", "Highlights headline": "Highlights", "Summary headline": "Your 2026"}



def build_extra_styles(d: Doc):
    """Recap's card type, the first run's look card and Android's own text (System/*)."""
    for name, (size, line, weight, width, rnd, track, feats, where) in RECAP_STYLES.items():
        expr = f"cardText({size:g}f, {line:g}f, {weight}" + (f", width = {width}" if width != 100 else "") + \
               (", round = false" if not rnd else "") + (f", tracking = {track:g}f" if track else "") + ")"
        tstyle(d, f"Recap/{name}", size=size, line=line, weight=weight, width=width, round=rnd,
               tracking=round(track * size, 2), features=feats,
               description=f"{expr}: {where}. Cards are pictures: they ignore the phone's text size.")
    for name, size, line, weight, family, desc in EXTRA_STYLES:
        tstyle(d, name, size=size, line=line, weight=weight, tracking=EXTRA_TRACKING.get(name, 0.0), family=family,
               description=desc)


def build_text_styles(d: Doc):
    # The documentation styles kit.canvas_basics made lost their weights (see tstyle); redefine them
    # from layers with the same values so page titles render at 600.
    for name, size, line, weight in [("Doc/Page title", 96, 104, 600), ("Doc/Section title", 48, 56, 600),
                                     ("Doc/Heading", 28, 36, 600), ("Doc/Body", 20, 30, 400),
                                     ("Doc/Label", 16, 22, 500), ("Doc/Caption", 13, 18, 400),
                                     ("Doc/Code", 14, 20, 400)]:
        if name in d.text_styles:
            tstyle(d, name, size=size, line=line, weight=weight, opsz=_opsz(size), update=d.text_styles[name])
    for name, compose, size, line, weight, tracking in M3_STYLES:
        tstyle(d, name, size=size, line=line, weight=weight, tracking=tracking,
               description=f"MaterialTheme.typography.{compose} (Expressive.type.{compose}). Type.kt: style({size}, {line}, {weight}"
                           f"{', ' + str(tracking) if tracking else ''}). Google Sans Flex, opsz {_opsz(size)}, ROND 0.")
    for name, compose, size, line, weight, tracking in M3_STYLES:
        w = EM_WEIGHT[compose]
        tstyle(d, name + " Emphasized", size=size, line=line, weight=w, tracking=tracking, round=True,
               description=f"Expressive.type.{compose}Emphasized: style({size}, {line}, {w}"
                           f"{', ' + str(tracking) if tracking else ''}, round = true). Heavier and fully rounded (ROND 100).")
    for name, size, line, weight, tracking, rnd, opsz, feats, expr, where in KIT_STYLES:
        width = 75 if name.endswith("w75") else (50 if name.endswith("w50") else 100)
        tstyle(d, name, size=size, line=line, weight=weight, tracking=tracking, round=rnd, opsz=opsz, width=width,
               features=feats, description=f"{expr}. Used by {where}.")


# ───────────────────────────────────────────────────────────────── variables
def build_variables(d: Doc, ctx: dict):
    wall, cover, color = {}, {}, {}
    d.collection("Wallpaper", modes=["Light", "Dark"])
    d.collection("Cover", modes=["Light", "Dark"])
    d.collection("System", modes=["Light", "Dark"])
    d.collection("Color", modes=["Wallpaper", "Cover"])
    d.collection("Dimension", modes=["Value"])

    for role, (light, dark) in WALLPAPER.items():
        wall[role] = d.variable("Wallpaper", kebab(role), "color", values={"Light": light, "Dark": dark},
                                description=f"The app scheme's {role}: expressiveColorScheme(dark) in ExpressiveTheme.kt, "
                                            "dynamicLight/DarkColorScheme(context). Values as the JVM renders draw them; on a phone they follow the wallpaper.",
                                android=f"expressiveColorScheme(dark).{role}")
    seed = d.variable("Cover", "seed", "color", values={"Light": SEED, "Dark": SEED},
                      description="The demo cover's seed: rememberArtColor(mediaId) in ui/Art.kt, the Palette swatch deepened "
                                  "(deepenTint: HSL saturation at most 0.62, lightness 0.2 to 0.3). HCT hue 33.09, chroma 44.37, tone 30.81. "
                                  "ArtScheme reads only its hue.",
                      android="rememberArtColor(mediaId)")
    d.variable("Cover", "palette-swatch", "color", values={"Light": PALETTE_SWATCH, "Dark": PALETTE_SWATCH},
               description="androidx.palette's vibrant swatch of the cover scaled to 96 x 96 (coverSwatch in ui/Art.kt), before deepenTint.",
               android="coverSwatch(context, mediaId)")
    for role in WALLPAPER:
        if role in COVER_ROLES:
            pal, lh, lt, lc, dh, dt, dc = COVER_ROLES[role]
            cover[role] = d.variable(
                "Cover", kebab(role), "color", values={"Light": lh, "Dark": dh},
                description=f"ArtScheme {pal} palette: Hct(hue{' + 60' if pal == 'Tertiary' else ''}, chroma {lc}, tone {lt}) light, "
                            f"Hct(hue{' + 60' if pal == 'Tertiary' else ''}, {dc}, {dt}) dark, hue from the seed. "
                            "Computed per cover at runtime by ArtScheme.colorScheme(seed, dark, fallback) (ArtScheme.kt), provided by FromArt (FromArt.kt).",
                android=f"ArtScheme.colorScheme(seed, dark).{role}")
    for role, src in COVER_FROM_ROLE.items():
        cover[role] = d.variable("Cover", kebab(role), "color",
                                 values={"Light": {"alias": cover[src]}, "Dark": {"alias": cover[src]}},
                                 description=f"ArtScheme.colorScheme sets {role} = {src}.",
                                 android=f"ArtScheme.colorScheme(seed, dark).{role}")
    for role in COVER_FROM_APP:
        cover[role] = d.variable("Cover", kebab(role), "color",
                                 values={"Light": {"alias": wall[role]}, "Dark": {"alias": wall[role]}},
                                 description=f"ArtScheme leaves {role} to the app scheme (its fallback), so it matches the rest of the app.",
                                 android=f"ArtScheme.colorScheme(seed, dark, fallback = app scheme).{role}")
    for role in WALLPAPER:
        color[role] = d.variable(
            "Color", kebab(role), "color", values={"Wallpaper": {"alias": wall[role]}, "Cover": {"alias": cover[role]}},
            description=f"{ROLE_USE.get(role, role)}. Wallpaper mode: the app scheme. Cover mode: the cover's scheme, as inside FromArt(seed).",
            android=f"MaterialTheme.colorScheme.{role}")
    for name, src, alpha, code, use in [
        ("state/disabled-container", "onSurface", 0.12, "onSurface.copy(alpha = 0.12f)", "Material's disabled container for filled and tonal buttons"),
        ("state/disabled-content", "onSurface", 0.38, "onSurface.copy(alpha = 0.38f)", "disabled labels and icons (ExpressiveIconButton, ExpressiveToggleButton)"),
        ("state/over-art-scrim", "surface", 0.62, "surface.copy(alpha = 0.62f)", "the scrim behind DetailAppBar buttons over art (OverArtScrim)"),
        ("state/now-playing-veil", "secondaryContainer", 0.8, "secondaryContainer.copy(alpha = 0.8f)", "over the current song's cover in a row (Lists.kt CoverLead)"),
    ]:
        expr = {"alpha": {"variable": color[src], "opacity": alpha}}
        color[name] = d.variable("Color", name, "color", values={"Wallpaper": expr, "Cover": expr},
                                 description=f"{use}.", android=f"MaterialTheme.colorScheme.{code}")
    system = {}
    for name, light, dark, desc in SYSTEM:
        system[name] = d.variable("System", name, "color", values={"Light": light, "Dark": dark}, description=desc)
    hexpr = {"alpha": {"variable": system["status-bar-ink"], "opacity": 0.85}}
    d.variable("System", "gesture-handle", "color", values={"Light": hexpr, "Dark": hexpr},
               description="The gesture handle: the status bar ink at 85 % (ShotHarness.kt SystemBars).")

    dims = {}
    for name, value, code, scope in [
        ("radius/extra-small", 4, "ExpressiveShapes.ExtraSmall", "CORNER_RADIUS"),
        ("radius/small", 8, "ExpressiveShapes.Small", "CORNER_RADIUS"),
        ("radius/medium", 12, "ExpressiveShapes.Medium", "CORNER_RADIUS"),
        ("radius/large", 16, "ExpressiveShapes.Large", "CORNER_RADIUS"),
        ("radius/large-increased", 20, "ExpressiveShapes.LargeIncreased", "CORNER_RADIUS"),
        ("radius/extra-large", 28, "ExpressiveShapes.ExtraLarge", "CORNER_RADIUS"),
        ("radius/extra-large-increased", 32, "ExpressiveShapes.ExtraLargeIncreased", "CORNER_RADIUS"),
        ("radius/extra-extra-large", 48, "ExpressiveShapes.ExtraExtraLarge", "CORNER_RADIUS"),
        ("radius/full", 1000, "ExpressiveShapes.Pill", "CORNER_RADIUS"),
        ("radius/play-small-playing", 11, "ExpressiveShapes.onCorner(36.dp)", "CORNER_RADIUS"),
        ("radius/play-large-playing", 18, "PlayButton(size = PlaySize.Large) while playing", "CORNER_RADIUS"),
        *[(f"space/{n}", n, f"{n}.dp", "GAP") for n in (2, 4, 6, 8, 10, 12, 16, 20, 24, 28, 32, 48)],
        ("layout/screen-margin", 16, "16.dp: lists, shelves and headers start 16 in", "GAP"),
        ("layout/player-margin", 24, "SideMargin in ExpressiveNowPlaying.kt", "GAP"),
        ("layout/group-gap", 2, "ExpressiveShapes.GroupGap", "GAP"),
        ("layout/group-spacing", 12, "ExpressiveShapes.GroupSpacing", "GAP"),
        ("layout/shelf-gap", 12, "Shelf: Arrangement.spacedBy(12.dp)", "GAP"),
        ("layout/mini-player-inset", 12, "ExpressiveShell: the mini player 12 in from the edges", "GAP"),
        ("layout/mini-player-gap", 8, "MiniPlayerGap in ExpressiveShell.kt", "GAP"),
        ("layout/dock-clearance", 24, "LocalBottomPadding: the dock's height plus 24.dp", "GAP"),
        ("size/touch-target", 48, "minimumInteractiveComponentSize()", "WIDTH_HEIGHT"),
        ("size/icon", 24, "SymbolIcon default size", "WIDTH_HEIGHT"),
        ("size/button-small", 40, "ButtonSize.Small", "WIDTH_HEIGHT"),
        ("size/button-medium", 56, "ButtonSize.Medium", "WIDTH_HEIGHT"),
        ("size/row", 64, "SongRow heightIn(min = 64.dp)", "WIDTH_HEIGHT"),
        ("size/collection-row", 72, "CollectionRow heightIn(min = 72.dp)", "WIDTH_HEIGHT"),
        ("size/app-bar", 64, "DetailAppBar height", "WIDTH_HEIGHT"),
        ("size/root-app-bar", 72, "RootAppBar height before scrolling", "WIDTH_HEIGHT"),
        ("size/mini-player", 64, "MiniPlayerHeight in ExpressiveShell.kt", "WIDTH_HEIGHT"),
        ("size/navigation-bar", 64, "NavBarHeight (Material's short navigation bar)", "WIDTH_HEIGHT"),
        ("size/status-bar", 40, "STATUS_BAR_DP in the render harness", "WIDTH_HEIGHT"),
        ("size/gesture-bar", 24, "GESTURE_BAR_DP in the render harness", "WIDTH_HEIGHT"),
    ]:
        dims[name] = d.variable("Dimension", name, "number", value=value, android=code, scopes=[scope],
                                description=code)
    DIM.update(dims)
    ctx["vars"] = {"wallpaper": wall, "cover": cover, "color": color, "dimension": dims, "seed": seed}


def build_art(d: Doc, ctx: dict):
    """The other covers' schemes (Art) and a Color mode for each, Recap's mixes, Brand, Comfort preview and Phone."""
    color, wall, cover = ctx["vars"]["color"], ctx["vars"]["wallpaper"], ctx["vars"]["cover"]
    d.collection("Art", modes=["Light", "Dark"])
    art: dict[int, dict] = {}
    for i, (album, seed, hue, light, dark) in ART.items():
        s = slug(album)
        art[i] = {}
        for role in WALLPAPER:
            if role in COVER_FROM_APP:
                continue
            src = COVER_FROM_ROLE.get(role, role)
            art[i][role] = d.variable(
                "Art", f"{s}/{kebab(role)}", "color",
                values={"Light": art_hex(i, role, False), "Dark": art_hex(i, role, True)},
                description=f"ArtScheme.colorScheme(seed {seed}, dark).{role} for {album} (cover {i}): Hct(seed hue "
                            f"{hue:g}, the role's chroma, the role's tone)." + (f" ArtScheme sets {role} = {src}." if src != role else ""),
                android=f"ArtScheme.colorScheme(seed, dark).{role}")
        art[i]["storyGround"] = d.variable(
            "Art", f"{s}/story-ground", "color",
            values={"Light": art_hex(i, "surfaceContainer", False), "Dark": art_hex(i, "surfaceContainerLowest", True)},
            description=f"Recap's story ground under a {album} card: surface container in light, surface container "
                        "lowest in dark (ExpressiveRecapStory.kt).")
    pl = d.variable("Art", "paper-lanterns/story-ground", "color",
                    values={"Light": {"alias": cover["surfaceContainer"]}, "Dark": {"alias": cover["surfaceContainerLowest"]}},
                    description="Recap's story ground under a Paper Lanterns card: Cover surface container in light, "
                                "surface container lowest in dark.")
    wg = d.variable("Art", "wallpaper/story-ground", "color",
                    values={"Light": {"alias": wall["surfaceContainer"]}, "Dark": {"alias": wall["surfaceContainerLowest"]}},
                    description="The story ground in the wallpaper scheme, for completeness (a card always has a cover).")
    # Recap's variables first, so variable.addMode copies them into each album's mode.
    color["recapStoryGround"] = d.variable(
        "Color", "recap/story-ground", "color", values={"Wallpaper": {"alias": wg}, "Cover": {"alias": pl}},
        description="Recap's story screen: if (dark) surfaceContainerLowest else surfaceContainer, in the card's cover "
                    "scheme (ExpressiveRecapStory.kt).",
        android="if (dark) colorScheme.surfaceContainerLowest else colorScheme.surfaceContainer")
    for name, t, desc, code in RECAP_MIXES:
        expr = {"mix": {"from": {"variable": color["surfaceContainer"]}, "to": {"variable": color["primary"]}, "t": t}}
        color[name] = d.variable("Color", name, "color", values={"Wallpaper": expr, "Cover": expr},
                                 description=desc, android=code)
    for i, (album, *_rest) in ART.items():
        d.cmd("variable.addMode", collection="Color", name=album)
        for role in WALLPAPER:
            target = wall[role] if role in COVER_FROM_APP else art[i][role]
            d.cmd("variable.set", variable=color[role], mode=album, value={"alias": target})
        d.cmd("variable.set", variable=color["recapStoryGround"], mode=album, value={"alias": art[i]["storyGround"]})
    ctx["art"] = art

    d.collection("Brand", modes=["Value"])
    for n, v, res, what in BRAND:
        d.variable("Brand", n, "color", value=v, description=f"{res}: {what}. res/values/colors.xml.",
                   android=f"R.color.{res}")

    d.collection("Comfort preview", modes=["Light", "Dark"])
    for tok, (lv, dv) in COMFORT_PREVIEW.items():
        d.variable("Comfort preview", kebab(tok).replace("on2", "on-2"), "color", values={"Light": lv, "Dark": dv},
                   description=f"Comfort.colors.{tok}: the Comfort kit's colour, for the Comfort picture on the first run's "
                               "look card only (ComfortLookPreview). The Comfort file owns these.",
                   android=f"Comfort.colors.{tok}")

    d.collection("Phone", modes=["Light", "Dark"])
    for name, light, dark, what in PHONE:
        d.variable("Phone", name, "color", values={"Light": light, "Dark": dark},
                   description=f"System, not Attic: {what}. Drawn only to show Attic's surfaces in place.")
    d._vars = {}


# ───────────────────────────────────────────────────────────────── effect styles
def build_effects(d: Doc):
    d.effect_style("Elevation/Level 2", [
        {"type": "dropShadow", "color": "#0000004D", "offsetX": 0, "offsetY": 1, "radius": 2, "spread": 0},
        {"type": "dropShadow", "color": "#00000026", "offsetX": 0, "offsetY": 2, "radius": 6, "spread": 2},
    ], description="3dp: ExpressiveMenu (shadowElevation = 3.dp, Surfaces.kt). Material 3's level 2 shadow pair.")
    d.effect_style("Elevation/Level 3", [
        {"type": "dropShadow", "color": "#0000004D", "offsetX": 0, "offsetY": 1, "radius": 3, "spread": 0},
        {"type": "dropShadow", "color": "#00000026", "offsetX": 0, "offsetY": 4, "radius": 8, "spread": 3},
    ], description="6dp: a lifted row (Modifier.shadow(6.dp), Lists.kt), ExpressiveExtendedFab and ExpressiveFab (Material's 6dp), "
                   "the snackbar, FloatingToolbar(floating = true). Material 3's level 3 shadow pair.")


# ───────────────────────────────────────────────────────────────── shapes (Shapes.kt)
SAMPLES = 180


def _polygon_from_kotlin(val_name: str) -> str:
    text = (KT / "Shapes.kt").read_text()
    m = re.search(rf"val {val_name}: RadialShape = RadialShape\.fromPolygon\((.*?)\)\n", text, re.S)
    if not m:
        raise ValueError(f"{val_name} not found in Shapes.kt")
    return "".join(re.findall(r'"([^"]*)"', m.group(1)))


def _from_polygon(percent_points: str) -> list[float]:
    """RadialShape.fromPolygon: polar radii sampled at 180 even angles, linear between points."""
    vals = [float(v) / 50.0 - 1.0 for v in re.findall(r"(-?[0-9.]+)%", percent_points)]
    pts = list(zip(vals[0::2], vals[1::2]))
    polar = []
    for x, y in pts:
        a = math.atan2(y, x)
        if a < 0:
            a += 2 * math.pi
        polar.append((a, math.hypot(x, y)))
    polar.sort(key=lambda p: p[0])
    two = 2 * math.pi
    radii = []
    for i in range(SAMPLES):
        angle = two * i / SAMPLES
        after = next((k for k, p in enumerate(polar) if p[0] >= angle), -1)
        after = 0 if after < 0 else after
        before = (after - 1 + len(polar)) % len(polar)
        a0, r0 = polar[before]
        a1, r1 = polar[after]
        span = ((a1 - a0) + two) % two
        into = ((angle - a0) + two) % two
        radii.append(r0 if span == 0 else r0 + (r1 - r0) * (into / span))
    return radii


def _of(fn) -> list[float]:
    """RadialShape.of: a function of angle, scaled so its widest point touches the bounds."""
    raw = [fn(2 * math.pi * i / SAMPLES) for i in range(SAMPLES)]
    mx = max(raw)
    return [r / mx for r in raw]


def _loader_shapes() -> dict[str, list[float]]:
    def lobed(lobes, depth):
        return _of(lambda a: 1.0 - depth * (1.0 - math.cos(lobes * a)) / 2.0)

    def rounded_polygon(sides, softness):
        def f(a):
            sector = 2 * math.pi / sides
            local = ((a + math.pi / 2) % sector + sector) % sector - sector / 2
            poly = math.cos(sector / 2) / math.cos(local)
            return poly * (1 - softness) + softness * math.cos(sector / 2) * 1.08
        return _of(f)

    def pill(a):
        t = a - math.pi / 4
        x, y = math.cos(t), math.sin(t)
        half, straight = 0.62, 1 - 0.62
        lo, hi = 0.0, 2.0
        for _ in range(30):
            r = (lo + hi) / 2
            px, py = abs(x * r), abs(y * r)
            dx = max(px - straight, 0.0)
            inside = py <= half if px <= straight else dx * dx + py * py <= half * half
            lo, hi = (r, hi) if inside else (lo, r)
        return lo

    return {
        "Loader pentagon": rounded_polygon(5, 0.35),
        "Loader pill": _of(pill),
        "Loader sunny": lobed(8, 0.10),
        "Loader four leaf": lobed(4, 0.22),
        "Loader oval": _of(lambda a: 1.0 / math.sqrt(math.cos(a) ** 2 + (math.sin(a) / 0.82) ** 2)),
    }


_RADII: dict[str, list[float]] = {}


def shape_radii(name: str) -> list[float]:
    if not _RADII:
        for nm, val in [("Cookie 9", "CookieShape"), ("Clover 4", "CloverShape"), ("Sunny 8", "SunnyShape"),
                        ("Puff", "PuffShape"), ("Burst", "BurstShape")]:
            _RADII[nm] = _from_polygon(_polygon_from_kotlin(val))
        _RADII.update(_loader_shapes())
    return _RADII[name]


def shape_path(name: str, size: float, rotation: float = 0.0) -> str:
    """RadialShape.path(Size(size, size), rotation) as SVG path data: the 180 points fitted with cubic Béziers."""
    radii = shape_radii(name)
    c = size / 2
    pts = []
    for i, r in enumerate(radii):
        a = 2 * math.pi * i / len(radii) + rotation
        pts.append((c + math.cos(a) * r * c, c + math.sin(a) * r * c))
    # Fitted with cubic Béziers within 0.1 % of the size, so instances don't each store 180 segments.
    return fit_closed(pts, segments_hint=18)


SHAPES = [
    ("Cookie 9", "CookieShape", "Artists, at every size (ArtistCover). Material's Cookie9Sided, the kit's --shape-cookie."),
    ("Clover 4", "CloverShape", "\"Nothing here yet\": EmptyState(clover = true), 128dp. Material's Clover4Leaf."),
    ("Sunny 8", "SunnyShape", "Recap's play-count badge on the top song card. Material's Sunny."),
    ("Puff", "PuffShape", "One of the digit shapes on Recap's intro card. Material's Puff, a soft square inset 6.5 %."),
    ("Burst", "BurstShape", "The loading indicator's first shape and the kit's --shape-burst."),
    ("Loader pentagon", "LoaderShapes[2]", "Loading indicator: roundedPolygon(5, 0.35), drawn here as Material's pentagon is internal."),
    ("Loader pill", "LoaderShapes[3]", "Loading indicator: a stadium 1 x 0.62 set diagonally."),
    ("Loader sunny", "LoaderShapes[4]", "Loading indicator: lobed(8, 0.10)."),
    ("Loader four leaf", "LoaderShapes[5]", "Loading indicator: lobed(4, 0.22)."),
    ("Loader oval", "LoaderShapes[6]", "Loading indicator: an ellipse 1 x 0.82."),
]

FILLED_GLYPHS = [
    # navigation bar, selected tab (Navigation.kt)
    "home", "search", "library_music",
    # transport and play controls (PlayControls.kt, ExpressiveCollections.kt CollectionPlayButton)
    "play_arrow", "pause", "skip_previous", "skip_next",
    # toggles while on (ExpressiveToggleButton): shuffle, repeat, repeat one, grid, Add songs' tick
    "shuffle", "repeat", "repeat_one", "grid_view", "check",
    # favourites (HeartButton, FavouritesArt, FavouritesPill, the track menu)
    "favorite",
    # sleep timer while set (ExpressiveSheets.kt), Recap listener types (ExpressiveRecapCards.kt)
    "bedtime", "wb_twilight", "wb_sunny", "nights_stay",
]


# ───────────────────────────────────────────────────────────────── documentation pages
class DocPage:
    """A documentation page: a title, an intro, then white cards stacked down the page."""

    CARD_GAP = 72

    def __init__(self, d: Doc, page: str, title: str, intro: str, *, intro_width=1200):
        self.d = d
        d.page(page)
        self.page = page
        d.text(None, title, style="Doc/Page title", color="Canvas/label", name="Page title", x=0, y=0)
        i = d.text(None, intro, style="Doc/Body", color="Canvas/label", name="Intro", x=0, y=136,
                   width=intro_width, textAutoResize="height")
        self.y = 136 + d.get(i)["height"] + 80

    def card(self, title: str, body: str | None = None, source: str | None = None, *, gap=32, width=None) -> int:
        d = self.d
        c = d.frame(None, name=f"Card · {title}", x=0, y=self.y, layout="v", gap=gap, pad=56,
                    fill="Canvas/card", radius=32, sizing=("hug", "hug"))
        if width:
            d.set(c, sizingH="fixed", width=width)
        head = d.frame(c, name="Heading", layout="v", gap=12, sizing=("hug", "hug"))
        d.text(head, title, style="Doc/Section title", color="Canvas/label", name="Title")
        if body:
            d.text(head, body, style="Doc/Body", color="Canvas/label", name="Description", width=1200,
                   textAutoResize="height")
        if source:
            d.text(head, source, style="Doc/Code", color="Canvas/label-muted", name="Source")
        return c

    def done(self, card: int):
        self.y += self.d.get(card)["height"] + self.CARD_GAP


    @classmethod
    def below(cls, d: Doc, page: str) -> "DocPage":
        """Cards stacked under what a page already holds (a later module adding to an earlier module's page)."""
        self = cls.__new__(cls)
        self.d, self.page = d, page
        d.page(page)
        bottom = 0
        for n in d.get(d.pages[page], depth=1).get("children", []):
            g = d.get(n["id"])
            bottom = max(bottom, g.get("y", 0) + g.get("height", 0))
        self.y = bottom + self.CARD_GAP
        return self


DOC_PAGES = ["Colour", "Type", "Spacing, radii and shapes", "Elevation and motion", "Icons"]


def tidy_doc_pages(d: Doc):
    """One card width per documentation and component page: every card as wide as the page's widest, so cards read
    as one column whichever module added them. Run once every module has filled its pages."""
    for name, pid in d.pages.items():
        if not (name in DOC_PAGES or name.startswith("Components / ")):
            continue
        cards = [n["id"] for n in d.get(pid, depth=1).get("children", []) if n.get("name", "").startswith("Card · ")]
        if not cards:
            continue
        wmax = max(d.get(c)["width"] for c in cards)
        d.set(cards, sizingH="fixed", width=wmax)


def bind_section_strokes(d: Doc):
    """Sections come with the canvas's default grey border as a raw colour: bind it to Canvas/divider."""
    for page in ("Screens", "System surfaces"):
        if page not in d.pages:
            continue
        secs = [n["id"] for n in d.get(d.pages[page], depth=1).get("children", []) if n.get("type") == "SECTION"]
        if secs:
            d.set(secs, stroke=V("Canvas/divider"))


def note(d: Doc, parent, text: str, *, width=1200, style="Doc/Body", name="Note", color="Canvas/label") -> int:
    return d.text(parent, text, style=style, color=color, name=name, width=width, textAutoResize="height")


def hrow(d: Doc, parent, name: str, gap=24, wrap=None, cross="min", **kw) -> int:
    """A horizontal auto layout row that hugs. With `wrap` (a width), it is that wide and wraps: FigCraft
    only wraps a row of fixed width (a hugging row with a max width stays on one line)."""
    f = d.frame(parent, name=name, layout="h", gap=gap, sizing=("hug", "hug"), cross=cross, **kw)
    if wrap:
        d.set(f, wrap=True, crossGap=gap, sizingH="fixed", width=wrap)
    return f


def vcol(d: Doc, parent, name: str, gap=12, cross="min", **kw) -> int:
    return d.frame(parent, name=name, layout="v", gap=gap, sizing=("hug", "hug"), cross=cross, **kw)


# ───────────────────────────────────────────────────────────────── Colour page
def _swatch(d: Doc, parent, collection: str, role: str, hex_value: str, *, extra: str | None = None, w=168):
    cell = vcol(d, parent, f"Swatch · {kebab(role)}", gap=4)
    d.rect(cell, name="Colour", w=w, h=88, fill=f"{collection}/{kebab(role)}", radius=14,
           stroke="Canvas/divider", stroke_w=1)
    for text, style, color, nm in [(kebab(role), "Doc/Label", "Canvas/label", "Name"),
                                   (hex_value, "Doc/Code", "Canvas/label", "Hex"),
                                   (f"colorScheme.{role}", "Doc/Caption", "Canvas/label-muted", "Code name"),
                                   *([(extra, "Doc/Caption", "Canvas/label-muted", "Detail")] if extra else [])]:
        d.text(cell, text, style=style, color=color, name=nm, width=w, textAutoResize="height")
    return cell


def _scheme_panels(d: Doc, card, collection: str, values: dict, extra=None, roles_filter=None):
    """Two panels, Light and Dark, each with the role families as rows of swatches."""
    panels = hrow(d, card, "Light and Dark", gap=48)
    for mi, mode in enumerate(("Light", "Dark")):
        panel = vcol(d, panels, f"{mode}", gap=28)
        d.set_mode(panel, collection, mode)
        if collection == "Cover":
            d.set_mode(panel, "Wallpaper", mode)
        d.text(panel, mode, style="Doc/Heading", color="Canvas/label", name="Mode")
        for family, roles in FAMILIES:
            fam = vcol(d, panel, f"Family · {family}", gap=10)
            d.text(fam, family, style="Doc/Label", color="Canvas/label-muted", name="Family")
            row = hrow(d, fam, "Swatches", gap=12)
            for role in roles:
                _swatch(d, row, collection, role, values(role, mi), extra=extra(role, mi) if extra else None)
    return panels


def build_colour(d: Doc, ctx: dict):
    p = DocPage(d, "Colour", "Colour",
                "Attic Expressive has no fixed palette. Screens take the phone's wallpaper scheme; player surfaces "
                "rebuild every Material role from the cover that is playing. Both use the same role names, so layers bind "
                "to one Color collection and a mode picks the scheme. Light and Dark follow the app's theme in both.")

    c = p.card("How the collections fit",
               "Bind every fill and stroke to Color/<role>. Color has two modes: Wallpaper (the default) aliases the "
               "Wallpaper collection, Cover aliases the Cover collection. A frame on a player surface sets its Color mode to "
               "Cover, which is what FromArt(seed) does in code: it provides a new MaterialTheme.colorScheme to everything "
               "inside it. Wallpaper and Cover each have Light and Dark modes; a Dark screen switches both.",
               "ExpressiveTheme.kt · expressiveColorScheme(dark)   FromArt.kt · FromArt(seed)   ArtScheme.kt · ArtScheme.colorScheme(seed, dark, fallback)")
    table = vcol(d, c, "Collections", gap=16)
    for name, modes, what in [
        ("Color", "Wallpaper · Cover · one per album", "What layers bind to. 36 roles as aliases, four colour calculations under "
                                       "state/ and Recap's under recap/. Code syntax MaterialTheme.colorScheme.<role>."),
        ("Wallpaper", "Light · Dark", "The app scheme: dynamicLightColorScheme / dynamicDarkColorScheme. The values are the ones the JVM "
                                      "renders use (AOSP's default system palette); on a phone every role follows the wallpaper."),
        ("Cover", "Light · Dark", "The cover scheme for the example seed, Paper Lanterns, the demo now-playing album. The app computes it "
                                  "for every cover at runtime. Error roles and scrim alias Wallpaper, as ArtScheme keeps the app's."),
        ("Art", "Light · Dark", "The other eleven demo covers' schemes, Art/<album>/<role>, and each one's Recap story ground. "
                                "Color's album modes alias them (see Every demo cover, one Color mode each)."),
        ("System", "Light · Dark", "The phone's status bar, gesture handle and keyboard as the render harness draws them. Not part of "
                                   "either scheme."),
        ("Phone", "Light · Dark", "The stand-in home screen, shade and lock screen around Attic's widget and notification. Android's."),
        ("Brand", "Value", "The launcher icon's and splash's colours (res/values/colors.xml)."),
        ("Comfort preview", "Light · Dark", "The Comfort style's colours, for the Comfort picture on the first run's look card only."),
        ("Dimension", "Value", "Radius, spacing and size numbers from Shapes.kt and the layout code (see Spacing, radii and shapes)."),
    ]:
        r = hrow(d, table, f"Collection · {name}", gap=32)
        d.text(r, name, style="Doc/Heading", color="Canvas/label", name="Collection", width=220, textAutoResize="height")
        d.text(r, modes, style="Doc/Code", color="Canvas/label-muted", name="Modes", width=200, textAutoResize="height")
        note(d, r, what, width=1100, name="What it holds")
    where = vcol(d, c, "Where the cover scheme applies", gap=8)
    d.text(where, "Where the Cover mode applies", style="Doc/Heading", color="Canvas/label", name="Heading")
    note(d, where,
         "Cover (FromArt / FromCover): the mini player, Now Playing and its compact header, the queue, full-screen lyrics, "
         "collection and artist headers with the bar over them (ExpressiveCollections.kt CollectionFrame), browse tiles "
         "(BrowseTile wraps itself), sheets and the snackbar opened over the player, Recap's story screen and cards (the "
         "card's seed). "
         "Wallpaper: every screen, the navigation bar, lists, tiles, shelves, sheets, dialogs and menus, including the list "
         "under a collection header. Recap cards always use the cover scheme in Dark, whatever the theme.", width=1500)
    p.done(c)

    c = p.card("Wallpaper scheme, example wallpaper",
               "The app has no fixed palette: it takes every role from the phone's wallpaper at runtime "
               "(dynamicLightColorScheme / dynamicDarkColorScheme). These are the values for one example wallpaper, the "
               "one the JVM renders use (AOSP's default system palette, a muted blue). Swatches are bound to the "
               "Wallpaper collection, so editing a value there changes every screen in this file.",
               "app/src/main/java/app/attic/player/ui/expressive/ExpressiveTheme.kt · expressiveColorScheme(dark)")
    _scheme_panels(d, c, "Wallpaper", lambda role, mi: WALLPAPER[role][mi])
    p.done(c)

    c = p.card("Cover scheme, example seed",
               "The app builds these roles for each cover while it plays. Only the seed's HCT hue is read: each role has a "
               "fixed HCT tone and chroma (from kit.css's OKLCH values), primary, secondary and the neutrals take the cover's "
               "hue, tertiary takes the hue plus 60°. So a dull cover and a loud one give the same contrast. The example is "
               "Paper Lanterns, the album the demo library plays. The swatches show its values; the captions give tone and chroma.",
               "ArtScheme.kt · ArtScheme.roles(seedArgb, dark) and ArtScheme.colorScheme(seedArgb, dark, fallback)   FromArt.kt · FromArt(seed), rememberArtScheme   ui/Art.kt · rememberArtColor(mediaId)")
    seedrow = hrow(d, c, "Seed", gap=40, cross="center")
    img = d.image(seedrow, cover_path(0), name="Cover: Paper Lanterns", w=208, h=208, radius=28)
    bind_dim(d, img, "cornerRadius", "radius/extra-large")
    sw = vcol(d, seedrow, "Seed swatches", gap=16)
    for var, hexv, label, detail in [
        ("palette-swatch", PALETTE_SWATCH, "palette-swatch", "Palette's vibrant swatch at 96 x 96 (coverSwatch)"),
        ("seed", SEED, "seed", "deepenTint(swatch): the FromArt seed. HCT hue 33.09, chroma 44.37, tone 30.81"),
    ]:
        r = hrow(d, sw, f"Swatch · {label}", gap=16, cross="center")
        d.rect(r, name="Colour", w=88, h=88, radius=44, fill=f"Cover/{var}")
        col = vcol(d, r, "Text column", gap=2)
        d.text(col, label, style="Doc/Label", color="Canvas/label", name="Name")
        d.text(col, hexv, style="Doc/Code", color="Canvas/label", name="Hex")
        d.text(col, detail, style="Doc/Caption", color="Canvas/label-muted", name="Code name")
    note(d, seedrow,
         "At runtime: rememberArtColor(track.mediaId) reads the cover, Palette picks a swatch, deepenTint darkens it, "
         "and FromArt(seed) calls ArtScheme.colorScheme(seed, dark, MaterialTheme.colorScheme), then animates every role to "
         "the new values on the slow effects spring when the song changes. Roles ArtScheme does not define (error, "
         "on-error, error-container, on-error-container, scrim) stay the wallpaper scheme's. background, on-background, "
         "surface-variant and surface-tint copy surface, on-surface, surface-container-highest and primary.",
         width=900)

    def cover_value(role, mi):
        if role in COVER_ROLES:
            return COVER_ROLES[role][1 if mi == 0 else 4]
        if role in COVER_FROM_ROLE:
            return COVER_ROLES[COVER_FROM_ROLE[role]][1 if mi == 0 else 4]
        return WALLPAPER[role][mi]

    def cover_extra(role, mi):
        if role in COVER_ROLES:
            pal, _, lt, lc, _, dt, dc = COVER_ROLES[role]
            t, ch = (lt, lc) if mi == 0 else (dt, dc)
            return f"{pal} · T {t} · C {ch}"
        if role in COVER_FROM_ROLE:
            return f"= {kebab(COVER_FROM_ROLE[role])}"
        return "from the wallpaper scheme"

    _scheme_panels(d, c, "Cover", cover_value, extra=cover_extra)
    p.done(c)

    c = p.card("Every demo cover, one Color mode each",
               "Each cover keeps only its seed's HCT hue; tone and chroma come from the role, so every cover's scheme has "
               "the same contrast. Tertiary is the hue plus 60°. The Art collection holds the other eleven covers' roles "
               "(Art/<album>/<role>, Light and Dark), and Color has one mode per album that aliases them, the way Cover "
               "aliases the Cover collection. Set a frame's Color mode to the album's name to draw it on that cover: a "
               "browse tile, an artist or playlist header, a Recap card. Paper Lanterns is the Cover mode. The swatches "
               "below are bound to those variables.",
               "ArtScheme.kt · ArtScheme.roles(seedArgb, dark)   FromArt.kt · FromArt(seed)   values: colors/expressive-cover-N.json")
    tbl = vcol(d, c, "Covers", gap=10)
    head = hrow(d, tbl, "Header", gap=12, cross="center")
    d.text(head, "Cover and Color mode", style="Doc/Label", color="Canvas/label-muted", name="Cover", width=340,
           textAutoResize="height")
    for mode in ("Light", "Dark"):
        for role in OTHER_ROLES:
            d.text(head, f"{role}", style="Doc/Caption", color="Canvas/label-muted", name=f"{mode} {role}", width=76,
                   textAutoResize="height")
        if mode == "Light":
            d.frame(head, name="Mode gap", w=24, h=1)
    for idx, album in ALBUMS.items():
        seed, hue = (SEED, 33.09) if idx == 0 else ART[idx][1:3]
        row = hrow(d, tbl, f"Cover row · {album}", gap=12, cross="center")
        info = hrow(d, row, "Cover info", gap=12, cross="center")
        d.set(info, sizingH="fixed", width=340)
        d.image(info, cover_path(idx), name=f"Cover: {album}", w=56, h=56, radius=12)
        d.rect(info, name="Seed", w=24, h=24, radius=12, fill=seed, allow_hex=True)
        txt_ = vcol(d, info, "Text column", gap=0)
        d.text(txt_, album, style="Doc/Label", color="Canvas/label", name="Album")
        d.text(txt_, f"Color mode {COLOR_MODE[idx]} · seed {seed} · hue {hue:g}°", style="Doc/Caption",
               color="Canvas/label-muted", name="Seed hue")
        for mode in ("Light", "Dark"):
            sw = hrow(d, row, f"{mode} swatches", gap=12, cross="center")
            d.set_mode(sw, "Color", COLOR_MODE[idx])
            for coll in ("Wallpaper", "Cover", "Art"):
                d.set_mode(sw, coll, mode)
            for role in OTHER_ROLES:
                d.rect(sw, name=f"{mode} {role}", w=76, h=40, radius=8, fill=f"Color/{role}",
                       stroke="Canvas/divider", stroke_w=1)
            if mode == "Light":
                d.frame(row, name="Mode gap", w=24, h=1)
    p.done(c)

    c = p.card("Recap's mixes",
               "Recap blends its card's container towards primary for the listening clock, the languages bar and the "
               "languages card. Each is a Color variable with the same expression in every mode, so it follows the card's "
               "album. Shown here in Iravu Rail's dark scheme, as a Recap card draws it.",
               "ExpressiveRecapCards.kt · LanguagesCard, ListeningDial   ExpressiveRecap.kt · Languages")
    r = hrow(d, c, "Mix swatches", gap=12, wrap=1500)
    d.set_mode(r, "Color", COLOR_MODE[8])
    for coll in ("Wallpaper", "Cover", "Art"):
        d.set_mode(r, coll, "Dark")
    for name, t, desc, code in [("recap/story-ground", None, "", "story ground"), *RECAP_MIXES]:
        cell = vcol(d, r, f"Swatch · {name}", gap=4)
        d.rect(cell, name="Colour", w=168, h=88, fill=f"Color/{name}", radius=14, stroke="Canvas/divider", stroke_w=1)
        d.text(cell, name, style="Doc/Label", color="Canvas/label", name="Name", width=168, textAutoResize="height")
        d.text(cell, "dark ? lowest : container" if t is None else f"lerp(container, primary, {t:g})", style="Doc/Caption",
               color="Canvas/label-muted", name="Code name", width=168, textAutoResize="height")
    p.done(c)

    c = p.card("Color roles in each mode",
               "The collection layers bind to. Each row is one variable seen four ways: the wallpaper and cover schemes, "
               "Light and Dark. The state/ variables are colour calculations on these roles.",
               "Code syntax: MaterialTheme.colorScheme.<role>")
    grid = vcol(d, c, "Roles", gap=6)
    head = hrow(d, grid, "Header", gap=12)
    d.text(head, "Variable", style="Doc/Label", color="Canvas/label-muted", name="Variable", width=300, textAutoResize="height")
    for lbl in ("Wallpaper · Light", "Cover · Light", "Wallpaper · Dark", "Cover · Dark"):
        d.text(head, lbl, style="Doc/Label", color="Canvas/label-muted", name=lbl, width=150, textAutoResize="height")
    roles = [r for _, rs in FAMILIES for r in rs]
    names = [kebab(r) for r in roles] + ["state/disabled-container", "state/disabled-content",
                                         "state/over-art-scrim", "state/now-playing-veil"]
    for nm in names:
        r = hrow(d, grid, f"Row · {nm}", gap=12, cross="center")
        d.text(r, nm, style="Doc/Code", color="Canvas/label", name="Variable", width=300, textAutoResize="height")
        for scheme, theme in (("Wallpaper", "Light"), ("Cover", "Light"), ("Wallpaper", "Dark"), ("Cover", "Dark")):
            cell = d.frame(r, name=f"{scheme} · {theme}", w=150, h=36, radius=8, fill="Canvas/card",
                           stroke="Canvas/divider", stroke_w=1, clip=True)
            d.rect(cell, name="Colour", w=150, h=36, fill=f"Color/{nm}", x=0, y=0)
            d.set_mode(cell, "Color", scheme)
            d.set_mode(cell, "Wallpaper", theme)
            d.set_mode(cell, "Cover", theme)
    p.done(c)

    c = p.card("System chrome",
               "The status bar, gesture handle and keyboard belong to the phone. They are drawn as the JVM render harness "
               "draws them, in ink that follows the theme, including over Now Playing's cover colours. The Phone "
               "collection holds the stand-in home screen, shade and lock screen around Attic's widget and notification "
               "on the System surfaces page. Neither is part of Attic's schemes.",
               "app/src/storeShots/kotlin/app/attic/player/storeshots/ShotHarness.kt · SystemBars   designrefs/DesignHarness.kt · Keyboard")
    for coll, entries in (("System", [(n, l, dk) for n, l, dk, _ in SYSTEM] + [("gesture-handle", "#1B1B1FD9", "#FFFFFFD9")]),
                          ("Phone", [(n, l, dk) for n, l, dk, _ in PHONE])):
        d.text(c, coll, style="Doc/Heading", color="Canvas/label", name=f"Heading · {coll}")
        r = hrow(d, c, f"{coll} swatches", gap=48)
        for mi, mode in enumerate(("Light", "Dark")):
            panel = hrow(d, r, mode, gap=12, wrap=6 * 168 + 5 * 12)
            d.set_mode(panel, coll, mode)
            for var, lv, dv in entries:
                cell = vcol(d, panel, f"Swatch · {var}", gap=4)
                d.rect(cell, name="Colour", w=168, h=88, fill=f"{coll}/{var}", radius=14, stroke="Canvas/divider", stroke_w=1)
                d.text(cell, f"{var} · {mode}", style="Doc/Label", color="Canvas/label", name="Name", width=168,
                       textAutoResize="height")
                d.text(cell, (lv, dv)[mi], style="Doc/Code", color="Canvas/label", name="Hex")
    p.done(c)

    c = p.card("Brand and the Comfort preview",
               "Brand holds the launcher icon's and splash's colours from res/values/colors.xml, one mode. Comfort preview "
               "holds the Comfort style's colours, used only by the Comfort picture on the first run's look card; the "
               "Comfort design file owns them.",
               "res/values/colors.xml   ui/expressive/ExpressiveOnboarding.kt · ComfortLookPreview")
    r = hrow(d, c, "Brand swatches", gap=12)
    for n, v, res, what in BRAND:
        cell = vcol(d, r, f"Swatch · {n}", gap=4)
        d.rect(cell, name="Colour", w=168, h=88, fill=f"Brand/{n}", radius=14, stroke="Canvas/divider", stroke_w=1)
        d.text(cell, n, style="Doc/Label", color="Canvas/label", name="Name", width=168, textAutoResize="height")
        d.text(cell, v, style="Doc/Code", color="Canvas/label", name="Hex")
        d.text(cell, f"R.color.{res}", style="Doc/Caption", color="Canvas/label-muted", name="Code name")
    r = hrow(d, c, "Comfort preview swatches", gap=48)
    for mi, mode in enumerate(("Light", "Dark")):
        panel = hrow(d, r, mode, gap=12, wrap=5 * 140 + 4 * 12)
        d.set_mode(panel, "Comfort preview", mode)
        for tok, vals in COMFORT_PREVIEW.items():
            n = kebab(tok).replace("on2", "on-2")
            cell = vcol(d, panel, f"Swatch · {n}", gap=4)
            d.rect(cell, name="Colour", w=140, h=72, fill=f"Comfort preview/{n}", radius=14, stroke="Canvas/divider",
                   stroke_w=1)
            d.text(cell, f"{n} · {mode}", style="Doc/Label", color="Canvas/label", name="Name", width=140,
                   textAutoResize="height")
            d.text(cell, vals[mi], style="Doc/Code", color="Canvas/label", name="Hex")
    p.done(c)


# ───────────────────────────────────────────────────────────────── Type page
SAMPLES_BY_ROLE = {
    "Display": "Paper Lanterns", "Headline": "Lights Over the Water", "Title": "Lights Over the Water",
    "Body": "the moths are writing letters on the glass", "Label": "Instant mixes",
}
KIT_SAMPLES = {
    "Kit/Button": "Play", "Kit/Button medium": "Shuffle all songs", "Kit/Quick action": "Play next",
    "Kit/Nav label selected": "Home", "Kit/Row title": "Porch Light", "Kit/Row title current": "Lights Over the Water",
    "Kit/Row number": "1 2 3 10 11", "Kit/Row rank": "1 2 3", "Kit/Figures": "1:18   3:52",
    "Kit/Setting title": "Autoplay", "Kit/Sheet header title": "Lights Over the Water", "Kit/Tile title": "Weather Report",
    "Kit/Context": "Paper Lanterns", "Kit/Caption title": "Paper Lanterns", "Kit/Compact title": "Lights Over the Water",
    "Kit/Bar title": "Paper Lanterns", "Kit/Root title": "Attic", "Kit/Player title": "Lights Over the Water",
    "Kit/Collection title": "Paper Lanterns", "Kit/Player title w75": "Small Hours, Big Sky",
    "Kit/Player title w50": "Small Hours, Big Sky", "Kit/Collection title w75": "Kadalkarai Sessions", "Kit/Artist title": "Mira Okafor", "Kit/Recap card title": "Your 2026 so far",
    "Kit/Favourites count": "42", "Kit/Browse title": "English", "Kit/Browse title large": "Late Night",
    "Kit/Stat value": "18,420", "Kit/FAB label": "New playlist", "Kit/Play label": "Play",
    "Kit/Lyric large": "carry what we couldn't say", "Kit/Lyric current": "carry what we couldn't say",
    "Kit/Lyric line": "let them drift until the morning", "Kit/Status bar time": "10:24",
}


def _specimen(d: Doc, parent, name: str, sample: str, values: str, code: str):
    row = hrow(d, parent, f"Specimen · {name}", gap=40, cross="min")
    meta = vcol(d, row, "Meta", gap=4)
    d.set(meta, sizingH="fixed", width=420)
    d.text(meta, name, style="Doc/Label", color="Canvas/label", name="Style name")
    note(d, meta, values, width=420, style="Doc/Caption", color="Canvas/label-muted", name="Values")
    note(d, meta, code, width=420, style="Doc/Code", color="Canvas/label-muted", name="Compose")
    s = d.text(row, sample, style=name, color="Color/on-surface", name="Sample", width=1100, textAutoResize="height")
    d.rect(parent, name="Rule", w=1560, h=1, fill="Canvas/divider")
    return s


def build_type(d: Doc, ctx: dict):
    p = DocPage(d, "Type", "Type",
                "One variable font, Google Sans Flex, bundled at weight 300 to 800, width 50 to 100, roundness (ROND) 0 to 100 "
                "and optical size 12 to 48. Each Material role is a style; its emphasised partner is heavier and fully rounded, "
                "for the one line a block is about. Optical size is fixed per style near its size, as Android does not apply it "
                "on its own. The Kit styles are the copies the code makes of a role, named for where they appear.")
    for title, group, desc in [
        ("Material type scale", "plain", "MaterialTheme.typography: the kit's scale. ExpressiveTypography in Type.kt."),
        ("Emphasised", "em", "Expressive.type.<role>Emphasized. Material 3 1.4 keeps its own emphasised styles internal, so Type.kt defines all fifteen."),
        ("Kit styles", "kit", "Copies of a role the code makes with .copy(); each style's description holds the exact expression."),
    ]:
        c = p.card(title, desc, "app/src/main/java/app/attic/player/ui/expressive/Type.kt · ExpressiveTypography, GoogleSansFlex")
        rows = vcol(d, c, "Specimens", gap=20)
        if group in ("plain", "em"):
            for name, compose, size, line, weight, tracking in M3_STYLES:
                nm = name if group == "plain" else name + " Emphasized"
                w = weight if group == "plain" else EM_WEIGHT[compose]
                rnd = 0 if group == "plain" else 100
                vals = (f"{size}/{line} · wght {w} · ROND {rnd} · opsz {_opsz(size)} · wdth 100"
                        f" · tracking {tracking:g}")
                code = f"MaterialTheme.typography.{compose}" if group == "plain" else f"Expressive.type.{compose}Emphasized"
                _specimen(d, rows, nm, SAMPLES_BY_ROLE[name.split('/')[0]], vals, code)
        else:
            for name, size, line, weight, tracking, rnd, opsz, feats, expr, where in KIT_STYLES:
                wd = 75 if name.endswith("w75") else (50 if name.endswith("w50") else 100)
                vals = (f"{size}/{line} · wght {weight} · ROND {100 if rnd else 0} · opsz {opsz} · wdth {wd} · tracking {tracking:g}"
                        + (" · tnum" if feats else "") + f". {where}.")
                _specimen(d, rows, name, KIT_SAMPLES[name], vals, expr)
        p.done(c)

    c = p.card("Line height: one line takes the font's own height",
               "Type.kt gives every style a line height but no LineHeightStyle, so Compose applies LineHeightStyle.Default "
               "(Alignment.Proportional, Trim.Both) and trims the leading above the first line and below the last. A single "
               "line is the font's natural height, about 1.23 times its size: a 16/22 row title draws about 20 tall and a "
               "14/20 subtitle about 17, so a two-line row is 4 to 5dp shorter than the line heights add up to. The code's "
               "line height applies only between the lines of wrapped text. Figma can't trim leading, so here every style "
               "except Doc, Recap and the lyric lines has Auto line height (the natural height), and a text layer that "
               "wraps takes the style's Paragraph partner, which has the code's value (paragraph_line_heights() in "
               "foundations.py sets them; see Paragraph partners below). A wrapped paragraph is then one half-leading pair "
               "taller than on the phone. Each style's description gives the code's value. Recap's card type and the lyric lines keep the code's value; they "
               "were matched to the reference renders that way.",
               "Type.kt · style(size, line, weight)   androidx.compose.ui.text.style.LineHeightStyle.Default")
    r = hrow(d, c, "Row example", gap=48, cross="min")
    for label, boxes in (("Auto, as on the phone", None), ("The code's line heights added up: 22 + 20", (22, 20))):
        col = vcol(d, r, f"Example · {label}", gap=8)
        d.text(col, label, style="Doc/Label", color="Canvas/label-muted", name="Caption")
        box = d.frame(col, name="Text column", layout="v", w=320, fill="Canvas/divider", sizing=("fixed", "hug"))
        for (text, style, color, nm), h in zip((("Lights Over the Water", "Kit/Row title", "Color/on-surface", "Title"),
                                               ("Mira Okafor", "Body/Medium", "Color/on-surface-variant", "Subtitle")),
                                              boxes or (None, None)):
            holder = box
            if h:
                holder = d.frame(box, name=f"{nm} line box", layout="v", w=320, h=h, main="center")
            d.text(holder, text, style=style, color=color, name=nm)
    p.done(c)

    c = p.card("Recap card type",
               "Recap's cards are pictures, 360 x 640, that Save and Share write out as images, so their type ignores the phone's "
               "text size. Each style is one cardText(...) call in ExpressiveRecapCards.kt; its description names the card.",
               "app/src/main/java/app/attic/player/ui/expressive/ExpressiveRecapCards.kt · cardText")
    rows = vcol(d, c, "Specimens", gap=20)
    for name, (size, line, weight, width, rnd, track, feats, where) in RECAP_STYLES.items():
        vals = (f"{size:g}/{line:g} · wght {weight} · ROND {100 if rnd else 0} · wdth {width}"
                + (f" · tracking {track:g} em" if track else "") + (" · tnum" if feats else "") + f". {where}.")
        sample = RECAP_SAMPLES.get(name, "Lights Over the Water")
        _specimen(d, rows, f"Recap/{name}", sample, vals, "cardText(...)")
    p.done(c)

    c = p.card("First run and system text",
               "The first run's look card names each style and draws a small picture of Comfort in Comfort's own font, "
               "Onest. System/* is Android's text around Attic (the media notification, the widget's RemoteViews, the lock "
               "screen), Roboto on a phone: set here in Google Sans Flex and named as system so nobody takes it for "
               "Attic's type.", "ExpressiveOnboarding.kt · LookCard, ComfortLookPreview   res/layout/widget_expressive_*.xml")
    rows = vcol(d, c, "Specimens", gap=20)
    for name, size, line, weight, family, desc in EXTRA_STYLES:
        _specimen(d, rows, name, EXTRA_SAMPLES.get(name, "Lights Over the Water"),
                  f"{size:g}/{line:g} · wght {weight} · {family}", desc)
    p.done(c)

    c = p.card("Narrowing", "A title that does not fit narrows before it truncates: it is set at width 100, else 75, "
                            "else 50, and only then ellipsised (NarrowingText). Collection headers stop at 75 and wrap to three lines.",
               "Type.kt · NarrowingText, TextStyle.narrowed(width)")
    r = vcol(d, c, "Widths", gap=12)
    for wdth, style in ((100, "Kit/Player title"), (75, "Kit/Player title w75"), (50, "Kit/Player title w50")):
        d.text(r, f"Small Hours, Big Sky · {style}", style=style, color="Color/on-surface", name=f"Width {wdth}")
    p.done(c)


# ───────────────────────────────────────────────────────────────── Spacing, radii and shapes
def build_spacing(d: Doc, ctx: dict):
    p = DocPage(d, "Spacing, radii and shapes", "Spacing, radii and shapes",
                "Corners come from one scale; covers take theirs from their size; buttons, bars and pills are fully round; "
                "squared off means on. Artists are the nine-lobed cookie at every size, never a circle. A list is a group: rows "
                "2dp apart, outer corners 20, inner corners 4. The shapes below are drawn from the same polygons as Shapes.kt.")

    c = p.card("Corner scale", "Number variables in Dimension/radius. Bind a layer's corner radius to them.",
               "Shapes.kt · ExpressiveShapes")
    r = hrow(d, c, "Radii", gap=32, wrap=1600)
    for name, value, code in [("extra-small", 4, "ExtraSmall"), ("small", 8, "Small"), ("medium", 12, "Medium"),
                              ("large", 16, "Large"), ("large-increased", 20, "LargeIncreased"),
                              ("extra-large", 28, "ExtraLarge"), ("extra-large-increased", 32, "ExtraLargeIncreased"),
                              ("extra-extra-large", 48, "ExtraExtraLarge"), ("full", 1000, "Pill")]:
        cell = vcol(d, r, f"Radius · {name}", gap=6)
        box = d.rect(cell, name="Sample", w=112, h=112, fill="Color/primary-container")
        bind_dim(d, box, "cornerRadius", f"radius/{name}")
        d.text(cell, f"radius/{name}", style="Doc/Label", color="Canvas/label", name="Name")
        d.text(cell, f"{value if value < 1000 else 'full'} · ExpressiveShapes.{code}", style="Doc/Caption",
               color="Canvas/label-muted", name="Value")
    p.done(c)

    c = p.card("Covers by size", "A cover's corner follows its size: 8 up to 48dp, 12 up to 64, 16 up to 128, 20 up to 180, 28 above. "
                                 "CoverShape measures itself, so a cover at any size is right.",
               "Shapes.kt · ExpressiveShapes.coverRadius(size), CoverShape")
    r = hrow(d, c, "Covers", gap=32, cross="max")
    for size, rad, tok in [(48, 8, "small"), (56, 12, "medium"), (64, 12, "medium"), (112, 16, "large"),
                           (148, 20, "large-increased"), (208, 28, "extra-large")]:
        cell = vcol(d, r, f"Cover · {size}", gap=6)
        img = d.image(cell, cover_path(0), name="Cover", w=size, h=size, radius=rad)
        bind_dim(d, img, "cornerRadius", f"radius/{tok}")
        d.text(cell, f"{size}dp · {rad}", style="Doc/Label", color="Canvas/label", name="Size")
    p.done(c)

    c = p.card("Squared off means on", "A control's corner when it is on or playing. Off, it is fully round.",
               "Shapes.kt · ExpressiveShapes.onCorner(height); PlayControls.kt · PlayButton")
    r = hrow(d, c, "On corners", gap=40, cross="max")
    for label, w, h, off, on in [("Play, 36dp", 36, 36, 18, 11), ("Toggle, 40dp", 40, 40, 20, 12), ("Play, 56dp", 56, 56, 28, 18),
                                 ("Toggle, 56dp", 56, 56, 28, 16), ("Play, wide", 128, 80, 40, 28)]:
        cell = vcol(d, r, f"On · {label}", gap=8)
        pair = hrow(d, cell, "Pair", gap=12, cross="center")
        d.rect(pair, name="Off", w=152 if label == "Play, wide" else w, h=h, radius=off, fill="Color/secondary-container")
        d.rect(pair, name="On", w=w, h=h, radius=on, fill="Color/primary")
        d.text(cell, f"{label} · off {off} → on {on}", style="Doc/Label", color="Canvas/label", name="Label")
    p.done(c)

    c = p.card("Group cells", "Rows of a list sit in cells 2dp apart on surface container: the group's outer corners 20, "
                              "the inner ones 4. Groups are 12dp apart and 16dp in from the screen edges.",
               "Shapes.kt · ExpressiveShapes.groupItemShape(index, count), GroupGap, GroupSpacing; Lists.kt · ListGroup")
    r = hrow(d, c, "Groups", gap=64)
    for count in (1, 3):
        g = vcol(d, r, f"Group of {count}", gap=2)
        for i in range(count):
            top = 20 if i == 0 else 4
            bottom = 20 if i == count - 1 else 4
            cell = d.rect(g, name=f"Cell {i + 1}", w=343, h=64, fill="Color/surface-container",
                          cornerRadius=[top, top, bottom, bottom])
    p.done(c)

    c = p.card("Expressive shapes", "Main components drawn from the code's polygons: RadialShape.fromPolygon samples each "
                                    "outline as 180 radii around the centre; this file fits those points with cubic Béziers within 0.1 % of the size. Use an "
                                    "instance as a mask (artist covers) or a filled shape (empty states, badges).",
               "Shapes.kt · CookieShape, CloverShape, SunnyShape, PuffShape, BurstShape, LoaderShapes")
    r = hrow(d, c, "Shapes", gap=40, wrap=1600)
    shapes = {}
    for name, code, use in SHAPES:
        cell = vcol(d, r, f"Shape cell · {name}", gap=8)
        d.set(cell, sizingH="fixed", width=280)
        comp = d.component(cell, name=f"Shape/{name}", w=96, h=96,
                           description=f"{use} Compose: Shapes.kt · {code}.")
        d.vector(comp, name="Outline", path=shape_path(name, 96), fill="Color/primary", w=96, h=96, x=0, y=0,
                 constraints={"horizontal": "scale", "vertical": "scale"})
        d.register(f"Shape/{name}", comp)
        shapes[name] = comp
        d.text(cell, f"Shape/{name}", style="Doc/Label", color="Canvas/label", name="Name")
        note(d, cell, f"{code}. {use}", width=280, style="Doc/Caption", color="Canvas/label-muted", name="Use")
    ctx["shapes"] = shapes
    p.done(c)

    c = p.card("Spacing and sizes", "Number variables in Dimension. The code writes dp values directly; these name the ones "
                                    "that recur.", "Dimension collection")
    r = vcol(d, c, "Spacing", gap=10)
    for n in (2, 4, 6, 8, 10, 12, 16, 20, 24, 28, 32, 48):
        row = hrow(d, r, f"space/{n}", gap=24, cross="center")
        d.text(row, f"space/{n}", style="Doc/Code", color="Canvas/label", name="Name", width=140, textAutoResize="height")
        bar = d.rect(row, name="Bar", w=n * 4, h=16, fill="Color/primary", radius=2)
        d.text(row, f"{n}dp", style="Doc/Caption", color="Canvas/label-muted", name="Value")
    t = vcol(d, c, "Layout tokens", gap=6)
    for name, value, code in [
        ("layout/screen-margin", 16, "lists, shelves, chips and headers start 16 in"),
        ("layout/player-margin", 24, "Now Playing's side margin (SideMargin)"),
        ("layout/group-gap", 2, "between the cells of a group (GroupGap)"),
        ("layout/group-spacing", 12, "between two groups (GroupSpacing)"),
        ("layout/shelf-gap", 12, "between tiles in a shelf"),
        ("layout/mini-player-inset", 12, "the mini player's distance from the screen edges"),
        ("layout/mini-player-gap", 8, "between the mini player and the navigation bar"),
        ("layout/dock-clearance", 24, "added below a scrolling screen's last item, over the dock"),
        ("size/touch-target", 48, "every control's touch target"),
        ("size/row", 64, "a song row's minimum height; collection rows 72"),
        ("size/mini-player", 64, "the mini player's height"),
        ("size/navigation-bar", 64, "the navigation bar before the gesture area (88 with it)"),
    ]:
        d.text(t, f"{name}  {value}   {code}", style="Doc/Code", color="Canvas/label", name=name)
    p.done(c)


# ───────────────────────────────────────────────────────────────── Elevation and motion
SPRINGS = [
    ("fastSpatial", 0.6, 800, "Position, size and shape that should snap: corner morphs (play control, toggles, segments, "
                              "a lifted cell), the heart's pop, the cover stepping back when paused."),
    ("defaultSpatial", 0.8, 380, "Moves across the screen: the mini player sliding in, the shared X axis, the compact header, "
                                 "the seek wave's amplitude, lyric lines growing."),
    ("slowSpatial", 0.8, 200, "Defined for larger moves; no screen uses it yet."),
    ("fastEffects", 1.0, 3800, "Colour and alpha that should switch at once: the play glyph's cross-fade, toggle colours, exits."),
    ("defaultEffects", 1.0, 1600, "Most fades: app bars taking their container colour, row fills, the hero caption."),
    ("slowEffects", 1.0, 800, "FromArt: every role of the cover scheme moving to a new cover."),
]


def _spring_curve(zeta: float, k: float, t: float) -> float:
    w0 = math.sqrt(k)
    if zeta < 1:
        wd = w0 * math.sqrt(1 - zeta * zeta)
        return 1 - math.exp(-zeta * w0 * t) * (math.cos(wd * t) + zeta * w0 / wd * math.sin(wd * t))
    return 1 - math.exp(-w0 * t) * (1 + w0 * t)


def build_elevation_motion(d: Doc, ctx: dict):
    p = DocPage(d, "Elevation and motion", "Elevation and motion",
                "Expressive separates surfaces by tone, not shadow: a screen is surface, a group cell surface container, a pill "
                "surface container high. Only four things cast a shadow. Motion is springs: spatial ones overshoot, effects "
                "ones do not.")

    c = p.card("Tonal elevation", "Containers step up in tone instead of lifting. Material's tonalElevation is 0 everywhere "
                                  "the kit sets it; the container roles do the work.",
               "KIT.md (Theme); Lists.kt · groupCellColor; Surfaces.kt · ExpressiveSheet (surfaceContainerLow), ExpressiveDialog (surfaceContainerHigh)")
    r = hrow(d, c, "Tones", gap=0)
    for role, use in [("surface", "screens"), ("surface-container-lowest", ""), ("surface-container-low", "sheets"),
                      ("surface-container", "group cells, nav bar, menus"), ("surface-container-high", "pills, search, mini player, dialogs"),
                      ("surface-container-highest", "no-art covers, lifted row")]:
        cell = vcol(d, r, f"Tone · {role}", gap=8, pad=20, fill=f"Color/{role}")
        d.set(cell, sizingH="fixed", width=240, sizingV="fixed", height=200)
        d.text(cell, role, style="Label/Large", color="Color/on-surface", name="Role")
        d.text(cell, use, style="Body/Small", color="Color/on-surface-variant", name="Use", width=200, textAutoResize="height")
    p.done(c)

    c = p.card("Shadows", "Effect styles for the four surfaces that cast one: a row being dragged, the FAB, the menu and the "
                          "snackbar (and the floating toolbar, which no screen uses yet).",
               "Lists.kt · liftableCell, SongRow(lifted); Buttons.kt · ExpressiveFab, ExpressiveExtendedFab, FloatingToolbar; Surfaces.kt · ExpressiveMenu, ExpressiveSnackbar")
    r = hrow(d, c, "Shadow samples", gap=56, pad=[24, 24, 40, 24])
    for style, label in [("Elevation/Level 2", "Level 2 · 3dp · menu"), ("Elevation/Level 3", "Level 3 · 6dp · lifted row, FAB, snackbar")]:
        cell = vcol(d, r, f"Sample · {style}", gap=16)
        box = d.frame(cell, name="Surface", w=280, h=120, fill="Color/surface-container", radius=16)
        d.cmd("style.apply", ids=[box], style=d.effect_styles[style])
        d.text(cell, style, style="Doc/Label", color="Canvas/label", name="Style")
        d.text(cell, label, style="Doc/Caption", color="Canvas/label-muted", name="Use")
    p.done(c)

    c = p.card("Springs", "The motion scheme, written out because Material 3 1.4 keeps MotionScheme internal. Spatial springs "
                          "(position, size, shape) overshoot; effects springs (colour, alpha) do not. Curves show a step from 0 "
                          "to 1 over 600 ms. For prototypes: customSpring mass 1, the stiffness, damping = 2·ζ·√stiffness.",
               "ExpressiveTheme.kt · ExpressiveMotion")
    rows = vcol(d, c, "Spring rows", gap=24)
    for name, zeta, k, use in SPRINGS:
        row = hrow(d, rows, f"Spring · {name}", gap=40, cross="center")
        graph = d.frame(row, name="Curve", w=300, h=120, fill="Color/surface-container", radius=12, clip=True)
        d.rect(graph, name="Target line", w=300, h=1, x=0, y=30, fill="Color/outline-variant")
        pts = []
        settle = None
        for i in range(0, 121):
            t = i / 120 * 0.6
            v = _spring_curve(zeta, k, t)
            if settle is None and all(abs(1 - _spring_curve(zeta, k, t2 / 1000)) < 0.01 for t2 in range(int(t * 1000), 1500, 5)):
                settle = int(t * 1000)
            pts.append((i / 120 * 300, 110 - v * 80))
        d.vector(graph, name="Response", path="M" + " L".join(f"{x:.1f} {y:.1f}" for x, y in pts),
                 stroke="Color/primary", stroke_w=3, x=0, y=0, strokeCap="round", strokeJoin="round")
        meta = vcol(d, row, "Values", gap=4)
        d.text(meta, f"ExpressiveMotion.{name}()", style="Doc/Heading", color="Canvas/label", name="Name")
        d.text(meta, f"dampingRatio {zeta:g} · stiffness {k:g} · within 1 % by about {settle} ms · "
                     f"prototype damping {2 * zeta * math.sqrt(k):.1f}", style="Doc/Code", color="Canvas/label-muted", name="Values")
        note(d, meta, use, width=1000, name="Use")
    p.done(c)

    c = p.card("Transitions", "What moves where, from the code. Times are springs, so they depend on distance.",
               "ExpressiveShell.kt, ExpressiveNowPlaying.kt, Buttons.kt, PlayControls.kt, Lists.kt, Navigation.kt, FromArt.kt")
    items = [
        ("Tab to tab", "The new tab root fades in on defaultEffects while growing from 92 % on defaultSpatial; the old one fades out on fastEffects.",
         "ExpressiveShell.kt · ExpressiveNavTransitions"),
        ("Into a detail screen", "Shared X axis: slides in a tenth of the width from the end on defaultSpatial while fading in; back reverses it.",
         "ExpressiveShell.kt · ExpressiveNavTransitions"),
        ("Now Playing, queue, lyrics", "Rise from the bottom over the dock on spring(dampingRatio 1, stiffness 380): no overshoot, so no gap opens under them. Dragging the player down 160dp closes it.",
         "ExpressiveShell.kt · overlaySpring; ExpressiveNowPlaying.kt · dismissDrag"),
        ("Mini player", "Slides up from below the navigation bar and fades in when a song is queued (defaultSpatial, defaultEffects). A sideways swipe past 28 % of its width skips.",
         "ExpressiveShell.kt · ExpressiveShell, ExpressiveMiniPlayer"),
        ("Play and pause", "The play control squares off (corner, and the wide one's width 152 to 128) on fastSpatial; the glyph cross-fades on fastEffects. The seek wave flattens on defaultSpatial; the cover steps back to 86 % and rounds 28 to 48.",
         "PlayControls.kt · PlayButton, SeekBar; ExpressiveNowPlaying.kt · CoverPager"),
        ("Wave", "The seek wave and the row's playing wave travel one wavelength per second while playing.",
         "PlayControls.kt · rememberWavePhase(periodMs = 1000)"),
        ("Toggles and segments", "Corner morphs on fastSpatial, colours on fastEffects. A pressed button in a group widens 15 %; the selected segment widens to 1.15 of the others.",
         "Buttons.kt · ExpressiveToggleButton, ButtonGroup; Navigation.kt · ConnectedButtonGroup"),
        ("Favourite", "The heart pops from 80 % to full size on fastSpatial when it changes.",
         "Buttons.kt · HeartButton"),
        ("Lifting a row", "A row picked up in the queue or a playlist rounds all four corners to 20 on fastSpatial and takes the 6dp shadow.",
         "Lists.kt · liftableCell, SongRow(lifted)"),
        ("App bars", "Root and detail bars take the surface container colour on defaultEffects as the list scrolls; the root title shrinks 36 to 22 and Home's mark 32 to 24. A collection bar's play control scales in from 60 % on fastSpatial.",
         "Navigation.kt · RootAppBar, DetailAppBar; ExpressiveCollections.kt · CollectionBar"),
        ("New cover", "Every role of the cover scheme animates to the new song's on slowEffects.",
         "FromArt.kt · animateColorScheme"),
        ("Loading indicator", "Morphs through seven shapes, one every 650 ms (the morph takes the first 70 % on FastOutSlowInEasing), while turning.",
         "PlayControls.kt · LoadingIndicator; Shapes.kt · LoaderShapes"),
    ]
    rows = vcol(d, c, "Transition rows", gap=18)
    for title, what, src in items:
        row = hrow(d, rows, f"Transition · {title}", gap=32)
        d.text(row, title, style="Doc/Heading", color="Canvas/label", name="Title", width=360, textAutoResize="height")
        col = vcol(d, row, "Detail", gap=4)
        note(d, col, what, width=1100, name="What")
        d.text(col, src, style="Doc/Code", color="Canvas/label-muted", name="Source")
    p.done(c)


# ───────────────────────────────────────────────────────────────── Icons
def build_icons_page(d: Doc, ctx: dict):
    p = DocPage(d, "Icons", "Icons",
                f"Material Symbols Rounded, from the app's own subset font: only the {len(_icons.symbols())} glyphs in Symbols.kt exist, so the app "
                "cannot show any other. Weight 400, optical size 24. Filled is the selected state and the transport glyphs; a "
                "filled main component exists for each glyph the code draws filled. Recolour an instance by overriding the "
                "Glyph layer's fill with a Color variable.")
    c = p.card("Outlined", "Icon/<ligature>. Compose: SymbolIcon(Symbol.<Name>, size, filled = false, tint).",
               "app/src/main/java/app/attic/player/ui/expressive/Symbols.kt · Symbol, SymbolIcon")
    grid = hrow(d, c, "Icon grid", gap=8, wrap=1680)
    holder = d.frame(None, name="Icon build", x=-2000, y=0, w=10, h=10)
    made = d.build_icons(holder, color="Color/on-surface", filled=FILLED_GLYPHS)
    # SymbolIcon sets the glyph as text: a line box 1em tall with the font's 1056/96 ascent and descent, so
    # the glyph sits 0.1em (96 of 960 units) below the box the font draws it in. The renders show it there
    # (2.4dp low at 24dp, 3.6 at 36), so the glyph moves down by that much; it scales with the instance.
    for nm, cid in made.items():
        g = d.find(cid, name="Glyph", limit=1)[0]
        d.set(g, y=round(d.get(g)["y"] + 2.4, 3))
        d.set(cid, description=d.get(cid).get("description", "") +
              " Drawn 0.1em below the font box, where SymbolIcon's text layout puts it.")
    c2 = None
    for nm, cid in made.items():
        filled = nm.endswith("(filled)")
        if filled and c2 is None:
            p.done(c)
            c2 = p.card("Filled", "Icon/<ligature> (filled): FILL 1. The glyphs the code draws filled: the selected tab, "
                                  "transport, toggles while on, favourites, the sleep timer while set, Recap's listener types.",
                        "Navigation.kt, PlayControls.kt, Buttons.kt, Cover.kt, Tiles.kt, ExpressiveSheets.kt, ExpressiveRecapCards.kt")
            grid = hrow(d, c2, "Icon grid", gap=8, wrap=1680)
        lig = nm[len("Icon/"):].replace(" (filled)", "")
        cell = vcol(d, grid, f"Icon cell · {lig}{' filled' if filled else ''}", gap=10, cross="center",
                    pad=[20, 8, 16, 8], fill="Color/surface", radius=16)
        d.set(cell, sizingH="fixed", width=132)
        d.cmd("node.reparent", ids=[cid], parent=cell)
        d.text(cell, lig, style="Doc/Caption", color="Canvas/label", name="Ligature", textAlign="center")
    d.cmd("edit.delete", ids=[holder])
    p.done(c2 or c)


# ───────────────────────────────────────────────────────────────── build
def build(d: Doc, ctx: dict):
    ctx["covers"] = {i: cover_path(i) for i in COVER_FILES}
    build_variables(d, ctx)
    build_art(d, ctx)
    build_text_styles(d)
    build_extra_styles(d)
    build_effects(d)
    build_colour(d, ctx)
    build_type(d, ctx)
    build_spacing(d, ctx)
    build_elevation_motion(d, ctx)
    build_icons_page(d, ctx)


# ───────────────────────────────────────────────────────────────── dimension tokens on components
SPACE = {2, 4, 6, 8, 10, 12, 16, 20, 24, 28, 32, 48}
# (component name prefix, variant name must contain, field, value, token)
# Icon/* stay unbound: icons are placed at many sizes and drawn scaled (the look card's pictures), and a bound
# width or height doesn't follow transform.scale.
SIZE_RULES = [
    ("Mini player", "", "height", 64, "size/mini-player"),
    ("Status bar", "", "height", 40, "size/status-bar"),
    ("Gesture bar", "", "height", 24, "size/gesture-bar"),
    ("Button", "Size=Small", "height", 40, "size/button-small"),
    ("Button", "Size=Medium", "height", 56, "size/button-medium"),
    ("Icon button", "Size=Default", "width", 48, "size/touch-target"),
    ("Icon button", "Size=Default", "height", 48, "size/touch-target"),
    ("List row/Song", "", "minHeight", 64, "size/row"),
    ("List row/Collection", "", "minHeight", 72, "size/collection-row"),
    ("List row/Setting", "", "minHeight", 64, "size/row"),
]


def bind_dimension_tokens(d: Doc, ctx: dict, roots: list | None = None) -> int:
    """Bind gaps, paddings and named fixed sizes inside main components (Components / * and Icons pages) to the
    Dimension tokens they equal: space/*, layout/screen-margin for horizontal 16 on a full-width 411dp component,
    layout/group-gap for a 2dp gap in a group, and size/* for the fixed sizes SIZE_RULES names. Only unbound fields
    are touched. `roots` limits it to those components (Spec.make passes the set it just made). Bind a component
    before anything places instances of it: binding a main component afterwards resets its instances' size and image
    overrides. Without `roots` it only counts what is left unbound and binds nothing. Returns how many it bound."""
    jobs: dict = {}

    def add(nid, field, token):
        jobs.setdefault((field, token), []).append(nid)

    # Recap's cards and the launcher icon are drawn scaled (the story, the thumbnails, the share image, the icon on a
    # home screen): a binding would put the unscaled token back into the scaled copies.
    skip = ("Recap/", "Launcher icon")

    def visit(n, comp_name, variant, root_w):
        if comp_name.startswith(skip):
            return
        t = n.get("type")
        if t in ("INSTANCE", "TEXT", "VECTOR", "RECTANGLE", "ELLIPSE"):
            return
        bound = n.get("boundVariables") or {}
        lay = n.get("layout") or {}
        if t in ("FRAME", "COMPONENT") and lay.get("mode") in ("horizontal", "vertical", "grid"):
            gap = lay.get("gap")
            kids = n.get("children") or []
            if gap in SPACE and "gap" not in bound and len(kids) > 1 and lay.get("mainAlign") not in (
                    "spaceBetween", "spaceAround", "spaceEvenly"):
                add(n["id"], "gap", "layout/group-gap" if gap == 2 and "group" in n["name"].lower() else f"space/{int(gap)}")
            pad = lay.get("padding") or [0, 0, 0, 0]
            for side, v in zip(("paddingTop", "paddingRight", "paddingBottom", "paddingLeft"), pad):
                if v in SPACE and side not in bound:
                    tok = f"space/{int(v)}"
                    if v == 16 and side in ("paddingLeft", "paddingRight") and round(n.get("width", 0)) == 411:
                        tok = "layout/screen-margin"
                    add(n["id"], side, tok)
        if t == "COMPONENT":
            for prefix, vneed, field, value, token in SIZE_RULES:
                if comp_name.startswith(prefix) and vneed in variant and field not in bound:
                    v = n.get(field)
                    if v is not None and abs(v - value) < 0.01:
                        add(n["id"], field, token)
        for c in n.get("children") or []:
            visit(c, comp_name, variant, root_w)

    fields = ["name", "type", "layout", "width", "height", "minHeight", "boundVariables"]
    if roots is None:
        pages = [pid for name, pid in d.pages.items() if name.startswith("Components / ") or name == "Icons"]
        targets = pages
    else:
        targets = roots
    for pid in targets:
        tree = d.get(pid, depth=40, fields=fields)

        def find_comps(n):
            t = n.get("type")
            if t == "COMPONENT_SET":
                for v in n.get("children") or []:
                    if v.get("type") == "COMPONENT":
                        visit(v, n["name"], v["name"], v.get("width", 0))
                return
            if t == "COMPONENT":
                visit(n, n["name"], "", n.get("width", 0))
                return
            if t == "INSTANCE":
                return
            for c in n.get("children") or []:
                find_comps(c)
        find_comps(tree)
    if roots is None:
        left = sum(len(v) for v in jobs.values())
        ctx["dimension_unbound"] = {f"{f} {t}": len(v) for (f, t), v in jobs.items()}
        return left
    count = 0
    for (field, token), ids in sorted(jobs.items()):
        d.bind(ids, field, DIM[token])
        count += len(ids)
        tally = ctx.setdefault("dimension_bindings", {})
        tally[f"{field} {token}"] = tally.get(f"{field} {token}", 0) + len(ids)
    return count
