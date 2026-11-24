"""Foundations of Attic Comfort. Pages: Colour, Type, Spacing, radii and shapes, Elevation and motion, Icons.

Owner: foundations agent. Everything below is a stable name other modules build on. Source of truth is
app/src/main/java/app/attic/player/ui/comfort/ (ComfortColors.kt, Tinted.kt, Oklch.kt, ComfortType.kt,
ComfortShapes.kt, Groups.kt); computed colours come from /tmp/attic-dev/design-refs/colors/.

VARIABLES
=========
Bind components and screens to `Color/*` only. The other collections feed it.

`Palette` (modes Light, Dark): the fixed Comfort palette, ComfortLight / ComfortDark. Code syntax
`ComfortColors.<name>`.
    ground card raised fill fill-2 line edge on on-2 on-3 danger on-art knob toast on-toast scrim
    shadow shadow-small mini-base mini-fill mini-ink-2
    lamplight/accent lamplight/on-accent lamplight/accent-ink lamplight/accent-soft lamplight/on-accent-soft
    error/on-error error/container error/on-container      (toColorScheme's own three)
    ground-toggle                                          (card in Light, fill in Dark: PlayRow's toggles)
    system/ink system/gesture                              (the render harness's status and gesture bars)
    system/keyboard system/key                             (the render harness's keyboard stand-in)
    recap/language-2 recap/language-3                      (Recap's language steps: the accent at OKLCH L -0.13, -0.26)

`Tinted` (modes Light, Dark): `Tinted(seed, wash)` for the example seed, the Paper Lanterns swatch.
    seed (colour), wash (boolean)
    tinted/accent tinted/on-accent tinted/accent-ink tinted/accent-soft tinted/on-accent-soft
    tinted/mini-base tinted/mini-fill tinted/mini-ink-2            (.tinted)
    wash/ground wash/card wash/raised wash/fill wash/fill-2 wash/line wash/on-2 wash/on-3   (.tinted.wash)
  Each is oklch(L, seed.c * k, seed.h), stored for the example seed; the description quotes the rule.

`Color` (modes Base, Tinted, Tinted wash): the roles components read, `Comfort.colors.<name>`. Every
value is an alias (or a colour calculation), so the mode a frame sets picks the source:
    Base -> Palette; Tinted -> Tinted/tinted/* for the eight moved tokens; Tinted wash -> also Tinted/wash/*.
    ground card raised fill fill-2 line edge on on-2 on-3 accent on-accent accent-ink accent-soft
    on-accent-soft danger on-art knob toast on-toast scrim shadow shadow-small mini-base mini-fill mini-ink-2
    derived/lyric-sung   mix(on-2 -> on-3, 0.72)        ComfortLyrics.kt SyncedLine, a sung line
    derived/seek-tick    on-accent at 40 %              NowPlayingParts.kt SeekBar's tick
    derived/ground-clear ground at 0 %                  the start of every fade to the ground (dock, queue foot)
    derived/art-dim      black at 60 %                  Cover(dimmed), Tile(now)
    derived/ground-toggle card (light) / fill (dark)    a tonal toggle on a collection's ground (PlayRow)
    derived/language-2 derived/language-3               aliases of Palette/recap/language-* (ComfortRecapCards.kt languageSteps)
    system/ink system/gesture system/keyboard system/key   aliases of Palette/system/*
    material/<role>   every MaterialTheme.colorScheme role toColorScheme() sets (primary ... surfaceContainerHighest)
  Where the app applies it: set mode `Tinted wash` on Now Playing, Queue, full-screen Lyrics, sheets over
  the player and the tinted Browse tile; the mini player in the dock sets `Tinted` on itself
  (components_core does this on `Mini player`). Everything else stays `Base` (the default).

`Elevation` (modes Light, Dark): numbers the shadow styles bind.
    shadow/float-blur shadow/float-y shadow/small-blur shadow/small-y

`Dimension` (mode Value): radius/float 30, radius/group 26, radius/control 18, radius/menu 22,
    radius/extra-small 8, radius/pill 999; space/2 ... space/48 and role aliases space/group-margin,
    space/group-gap, space/row-gap, space/screen-edge, space/sheet-margin, space/label-inset,
    space/player-inset, space/dock-inset, space/dock-bottom, space/shelf-gap; size/* (see SIZES).

`Motion` (mode Value): duration/* in ms and spring/* (see MOTION).

`Brand` (mode Value): headroom/ground, headroom/cabinet, headroom/cone, the launcher icon's fixed colours (colors.xml).

`Expressive preview` (modes Light, Dark): the ten Material roles the Expressive look card's preview draws with
    (OnboardingPreview.kt ExpressiveLookPreview), from colors/expressive-base.json.

MODE_COLLECTIONS = ("Palette", "Tinted", "Elevation", "Expressive preview"): setup() makes these d.mode_collections,
so Area.close() and _modes() switch all four to Dark.

TEXT STYLES (Onest; fontStyle names pin the weight as ComfortType.kt pins the axis)
===========
`Scale/*`, ComfortTypography: Hero, Title, Person title, Bar title, Row title, Row title large, Body,
    Sub, Caption, Label, Button, Stat, Lyric, Label medium, Label small (the last two: Material slots only).
`Component/*`, inline `onest(...)` calls in the kit, named for where they are drawn (see TEXT_STYLES):
    Button small, Button large, Sub-tab, Sub-tab selected, Chip, Chip selected, Search, Hero subtitle,
    Tab, Tab selected, Mini title, Mini artist, Settings title, Settings summary, Settings value,
    Settings block, Small print, Stat label, Note, Label small, Row value, Row rank, Row number, Row meta,
    Row title current, Key, Tile title, Tile title centred, Browse title, Header byline, Header meta,
    Player context, Player artist, Player lyric, Seek time, Recap entry title, Lyric line,
    Lyric line current, Lyrics plain, Swipe action, Action, Action go, Index letter, Index pop,
    Quick action, Onboarding title, Onboarding body, Status time, Sort, Playlist tool.
  OVERLAY_TYPE (Overlays.kt): Sheet header title, Overlay row, Overlay row selected, Sheet value, Menu caption,
    Field label, Field, Toast.
`Recap/*` (RECAP_TYPE), the Recap card drawing's own onest() and headline() calls.
`Widget/*`, `Preview/*` (OTHER_TYPE): the home-screen widget's XML styles; Expressive's look-card preview (Google Sans Flex).
`System/*` (SYSTEM_TYPE): Android's own chrome on System surfaces, Google Sans Flex standing in. Not Attic's type.
The Doc/* styles from canvas_basics are re-saved here with their weights (see the report on kit.py).

EFFECT STYLES
=============
Shadow/Floating   Groups.kt floatingShadow(): dock, sheet, dialog, menu, action bar. Pair it with a 1 px
                  inside stroke in Color/edge. Colour Color/shadow, blur and y from Elevation.
Shadow/Small      floatingShadow(small = true): a lifted row, the toast, the switch knob, the queue's Clear pill.

COMPONENTS MADE HERE
====================
Icon/<ligature> for every glyph in Symbols.kt (90 today) at weight 300 (ComfortIconWeight), Icon/<ligature> (filled) for FILLED,
Icon/<ligature> (filled, 400) for the transport glyphs (PlayButton, SkipButton draw weight 400).
Shape/Squircle, Shape/Squircle large, Shape/Squircle soft: 100 x 100 superellipses, n = 4, 5, 2.6.
Shape/Floating, Shape/Group, Shape/Control, Shape/Menu, Shape/Pill, Shape/Circle: the rectangles.

HELPERS other modules may import
================================
superellipse_path(n, w, h), text_style(d, ...), Sheet (documentation page layout), hexa(), EXAMPLE_SEED.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from figbuild.pathfit import fit_closed
from figbuild.kit import Doc, V
from figbuild import icons as _icons

REFS: dict[str, str] = {}

COLORS_DIR = Path("/tmp/attic-dev/design-refs/colors")
COVERS_DIR = Path("/tmp/attic-dev/design-refs/covers")
SRC = "app/src/main/java/app/attic/player/ui/comfort"

# The example seed: Paper Lanterns, the now-playing album in every render (covers/README.txt).
EXAMPLE_SEED = "#F07858"
EXAMPLE_COVER = COVERS_DIR / "0-paper-lanterns.jpg"


# ======================================================================== OKLCH (Oklch.kt, line for line)
def _lin(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gam(c):
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055


def oklch_from_hex(h: str):
    h = h.lstrip("#")
    r, g, b = (_lin(int(h[i:i + 2], 16) / 255) for i in (0, 2, 4))
    l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
    m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
    s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
    L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    hue = math.degrees(math.atan2(bb, a))
    return L, math.hypot(a, bb), hue + 360 if hue < 0 else hue


def _lrgb(L, c, h):
    a, b = c * math.cos(math.radians(h)), c * math.sin(math.radians(h))
    l1 = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m1 = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s1 = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return [4.0767416621 * l1 - 3.3077115913 * m1 + 0.2309699292 * s1,
            -1.2684380046 * l1 + 2.6097574011 * m1 - 0.3413193965 * s1,
            -0.0041960863 * l1 - 0.7034186147 * m1 + 1.7076147010 * s1]


def oklch_to_hex(L, c, h) -> str:
    """Oklch.toArgb: out of gamut, chroma is halved down (24 steps) at the same L and h."""
    def ok(rgb):
        return all(-1e-7 <= x <= 1 + 1e-7 for x in rgb)
    rgb = _lrgb(L, c, h)
    if not ok(rgb):
        lo, hi = 0.0, c
        for _ in range(24):
            mid = (lo + hi) / 2
            if ok(_lrgb(L, mid, h)):
                lo = mid
            else:
                hi = mid
        rgb = _lrgb(L, lo, h)
    return "#" + "".join(f"{min(255, max(0, round(_gam(min(1, max(0, x))) * 255))):02X}" for x in rgb)


# ======================================================================== the Comfort colour data
# Kotlin name -> (variable name, kit name / what it is for)
TOKENS = [
    ("ground", "ground", "Night (dark) / Plaster (light): the screen."),
    ("card", "card", "Loft / Paper: groups, cards, the deck."),
    ("raised", "raised", "Rafter: things that float (dock, sheets, dialogs, menus)."),
    ("fill", "fill", "Slate / Putty: control tracks, tonal icon buttons, chips on a card."),
    ("fill2", "fill-2", "Switch-off track, progress track, grab handle."),
    ("line", "line", "Seam: inset hairlines."),
    ("edge", "edge", "A 1dp ring around floating pieces, drawn with their shadow."),
    ("on", "on", "Chalk / Ink: text and icons. Never changes inside Tinted."),
    ("on2", "on-2", "Dust: secondary text."),
    ("on3", "on-3", "Ash: hints, placeholders and trailing icons. Below 4.5:1 by design; never body text."),
    ("accent", "accent", "Lamplight: fills, the one filled button, switches, progress, selection."),
    ("onAccent", "on-accent", "Soot: ink on the accent."),
    ("accentInk", "accent-ink", "The accent as text or an icon on a neutral surface (Brass in light)."),
    ("accentSoft", "accent-soft", "The tonal container: selected tab, current row, tonal button."),
    ("onAccentSoft", "on-accent-soft", "Ink on accent-soft."),
    ("danger", "danger", "Ember: delete, remove."),
    ("onArt", "on-art", "Marks drawn on top of artwork (the playing bars over a dimmed cover)."),
    ("knob", "knob", "The switch knob."),
    ("toast", "toast", "The toast's ground."),
    ("onToast", "on-toast", "Ink on the toast."),
    ("scrim", "scrim", "Behind sheets and dialogs."),
    ("shadow", "shadow", "Shadow colour of floating pieces (--shadow)."),
    ("shadowSmall", "shadow-small", "Shadow colour of a lifted row, the knob, a toast (--shadow-sm)."),
    ("miniBase", "mini-base", "The mini player's ground (--mini-base)."),
    ("miniFill", "mini-fill", "The mini player's played part (--mini-fill)."),
    ("miniInk2", "mini-ink-2", "The mini player's second line: Attic's one addition to the kit's tokens."),
]
LAMPLIGHT = {"accent", "onAccent", "accentInk", "accentSoft", "onAccentSoft"}
KOTLIN_OF = {v: k for k, v, _ in TOKENS}

# ComfortTint tables (Tinted.kt), (L, k)
DARK_TINTED = {"accent": (0.76, 1.0), "onAccent": (0.2, 0.55), "accentInk": (0.82, 0.85), "accentSoft": (0.31, 0.6),
               "onAccentSoft": (0.89, 0.5), "miniBase": (0.25, 0.5), "miniFill": (0.4, 0.9), "miniInk2": (0.82, 0.3)}
DARK_WASH = {"ground": (0.115, 0.28), "card": (0.215, 0.34), "raised": (0.26, 0.34), "fill": (0.3, 0.4),
             "fill2": (0.38, 0.45), "line": (0.28, 0.36), "on2": (0.77, 0.3), "on3": (0.57, 0.26)}
LIGHT_TINTED = {"accent": (0.47, 1.0), "accentInk": (0.43, 1.0), "accentSoft": (0.9, 0.4), "onAccentSoft": (0.36, 1.0),
                "miniBase": (0.955, 0.16), "miniFill": (0.87, 0.42), "miniInk2": (0.45, 0.3)}
LIGHT_WASH = {"ground": (0.93, 0.2), "card": (0.978, 0.08), "raised": (0.985, 0.06), "fill": (0.905, 0.26),
              "fill2": (0.84, 0.34), "line": (0.915, 0.14), "on2": (0.45, 0.3), "on3": (0.62, 0.26)}
TINTED_TOKENS = ["accent", "onAccent", "accentInk", "accentSoft", "onAccentSoft", "miniBase", "miniFill", "miniInk2"]
WASH_TOKENS = ["ground", "card", "raised", "fill", "fill2", "line", "on2", "on3"]

# toColorScheme() (ComfortColors.kt): Material role -> Comfort token, or a literal per (light, dark)
MATERIAL = [
    ("primary", "accent"), ("onPrimary", "onAccent"), ("primaryContainer", "accentSoft"),
    ("onPrimaryContainer", "onAccentSoft"), ("inversePrimary", "accentInk"), ("secondary", "accentInk"),
    ("onSecondary", "ground"), ("secondaryContainer", "accentSoft"), ("onSecondaryContainer", "onAccentSoft"),
    ("tertiary", "accentInk"), ("onTertiary", "ground"), ("tertiaryContainer", "accentSoft"),
    ("onTertiaryContainer", "onAccentSoft"), ("background", "ground"), ("onBackground", "on"), ("surface", "ground"),
    ("onSurface", "on"), ("surfaceVariant", "fill"), ("onSurfaceVariant", "on2"), ("inverseSurface", "toast"),
    ("inverseOnSurface", "onToast"), ("error", "danger"), ("onError", "error/on-error"),
    ("errorContainer", "error/container"), ("onErrorContainer", "error/on-container"), ("outline", "on3"),
    ("outlineVariant", "line"), ("scrim", "scrim"), ("surfaceBright", "raised"), ("surfaceDim", "ground"),
    ("surfaceContainerLowest", "ground"), ("surfaceContainerLow", "card"), ("surfaceContainer", "card"),
    ("surfaceContainerHigh", "raised"), ("surfaceContainerHighest", "fill"),
]
ERROR_EXTRA = {  # ComfortColors.toColorScheme(): the three colours it defines itself
    "error/on-error": ("#FFFFFF", "#2A0A06", "onError = if (dark) Color(0xFF2A0A06) else Color.White"),
    "error/container": ("#FBE0DC", "#3D1A16", "errorContainer = if (dark) Color(0xFF3D1A16) else Color(0xFFFBE0DC)"),
    "error/on-container": ("#8C1D12", "#FFB4A9", "onErrorContainer = if (dark) Color(0xFFFFB4A9) else Color(0xFF8C1D12)"),
}


def _kebab(camel: str) -> str:
    out = ""
    for ch in camel:
        out += ("-" + ch.lower()) if ch.isupper() else ch
    return out


def hexa(h: str) -> str:
    """'#281E0A0F' -> '#281E0A 6 %'; opaque colours lose their alpha pair."""
    h = h.upper()
    if len(h) == 9:
        a = int(h[7:9], 16)
        return h[:7] if a == 255 else f"{h[:7]} {round(a / 2.55)} %"
    return h


def load_palette() -> dict:
    """{'light': {kotlin: '#RRGGBBAA'}, 'dark': {...}} from comfort-base.json, else from ComfortColors.kt values."""
    p = COLORS_DIR / "comfort-base.json"
    if p.exists():
        j = json.loads(p.read_text())
        return {"light": j["light"]["tokens"], "dark": j["dark"]["tokens"]}
    raise FileNotFoundError(f"{p} is missing: render the design colours first (DesignColors.kt)")


def tinted_values(seed: str) -> dict:
    """{'light'|'dark': {kotlin: hex}} for the tokens .tinted and .wash move, computed with the Oklch.kt port
    and checked against comfort-tinted-0.json when the seed is the example."""
    _, c, h = oklch_from_hex(seed)
    out = {}
    for theme, tables in (("light", (LIGHT_TINTED, LIGHT_WASH)), ("dark", (DARK_TINTED, DARK_WASH))):
        vals = {}
        for table in tables:
            for k, (L, kk) in table.items():
                vals[k] = oklch_to_hex(L, c * kk, h)
        if theme == "light":
            vals["onAccent"] = "#FFFFFF"
        out[theme] = vals
    ref = COLORS_DIR / "comfort-tinted-0.json"
    if ref.exists() and seed.upper() == EXAMPLE_SEED:
        j = json.loads(ref.read_text())
        for theme in ("light", "dark"):
            for k, x in j[theme]["tintedWash"]["changed"].items():
                if out[theme][k] != x["hex"][:7].upper():
                    raise ValueError(f"Tinted {theme}.{k}: port gives {out[theme][k]}, the code {x['hex']}")
    return out


# ======================================================================== text styles
WEIGHT_NAME = {400: "Regular", 500: "Medium", 600: "SemiBold", 700: "Bold", 800: "ExtraBold"}


def text_style(d: Doc, name: str, size: float, line: float, weight: int = 400, tracking: float = 0.0,
               tabular: bool = False, description: str | None = None, family: str = "Onest") -> str:
    """A text style as ComfortType.kt's onest(size, line, weight, tracking) builds it. Tracking is in em,
    as the kit writes it, stored as a percentage. The weight is the font's named instance (the code pins
    the wght axis to the same value)."""
    value = {"fontFamily": family, "fontStyle": WEIGHT_NAME[weight], "fontSize": size, "lineHeight": line,
             "letterSpacing": f"{round(tracking * 100, 2)}%"}
    if tabular:
        value["features"] = {"tnum": True}
    args = {"type": "text", "name": name, "value": value}
    if description:
        args["description"] = description
    sid = d.cmd("style.create", **args)["id"]
    d.text_styles[name] = sid
    return sid


# (name, size, line, weight, tracking em, tabular, code, where)
SCALE = [
    ("Scale/Hero", 40, 46, 700, -0.025, False, "Comfort.type.hero", "`.hero h1`, the expanded large title. Material displayLarge, displayMedium."),
    ("Scale/Title", 26, 32, 700, -0.02, False, "Comfort.type.title", "`.c-head h1`, `.np-title h1`: song and collection titles. Material headlineMedium."),
    ("Scale/Person title", 30, 36, 700, -0.02, False, "Comfort.type.personTitle", "`.c-head.person h1`: an artist's name. Material displaySmall, headlineLarge."),
    ("Scale/Bar title", 20, 26, 700, -0.015, False, "Comfort.type.barTitle", "`.bar h2`, sheet, dialog and empty-state titles. Material titleLarge."),
    ("Scale/Row title", 16, 22, 500, 0, False, "Comfort.type.rowTitle", "`.txt b`: row titles. Material titleMedium."),
    ("Scale/Row title large", 18, 24, 700, -0.01, False, "Comfort.type.rowTitleLarge", "`.row.lg .txt b`."),
    ("Scale/Body", 15, 21, 400, 0, False, "Comfort.type.body", "Dialog and empty-state text. Material bodyLarge."),
    ("Scale/Sub", 13.5, 18, 400, 0, False, "Comfort.type.sub", "`.txt span`: a row's second line. Material bodyMedium."),
    ("Scale/Caption", 12.5, 16, 400, 0, False, "Comfort.type.caption", "`.txt em`, `.kv em`, tile second lines. Material bodySmall."),
    ("Scale/Label", 14, 18, 600, 0, False, "Comfort.type.label", "`.label`, `.lyrics-card .hd`: section labels. Material titleSmall."),
    ("Scale/Button", 15.5, 20, 600, 0, False, "Comfort.type.button", "`.btn`. Material labelLarge."),
    ("Scale/Stat", 26, 32, 700, -0.02, True, "Comfort.type.stat", "`.stat b`, tabular figures."),
    ("Scale/Lyric", 22, 28, 700, -0.015, False, "Comfort.type.lyric", "`.lyrics-card .ln`. Material headlineSmall."),
    ("Scale/Label medium", 14, 18, 500, 0, False, "Comfort.type.material.labelMedium", "Material labelMedium only: onest(14f, 18f, 500)."),
    ("Scale/Label small", 12, 15, 500, 0, False, "Comfort.type.material.labelSmall", "Material labelSmall only: onest(12f, 15f, 500)."),
]

COMPONENT_TYPE = [
    ("Component/Button small", 14, 20, 600, 0, False, "Buttons.kt ComfortButton, ButtonSize.Small: onest(14f, 20f, 600)"),
    ("Component/Button large", 17, 20, 600, 0, False, "Buttons.kt ComfortButton, ButtonSize.Large: onest(17f, 20f, 600)"),
    ("Component/Sub-tab", 15, 20, 500, 0, False, "Bars.kt SubTabs: onest(15f, 20f, 500)"),
    ("Component/Sub-tab selected", 15, 20, 700, 0, False, "Bars.kt SubTabs, selected: onest(15f, 20f, 700)"),
    ("Component/Chip", 14, 18, 500, 0, False, "Bars.kt FilterChip: onest(14f, 18f, 500)"),
    ("Component/Chip selected", 14, 18, 600, 0, False, "Bars.kt FilterChip, selected: onest(14f, 18f, 600)"),
    ("Component/Search", 16, 22, 400, 0, False, "Bars.kt SearchField query and placeholder: onest(16f, 22f)"),
    ("Component/Hero subtitle", 15, 20, 400, 0, False, "Bars.kt Hero subtitle: onest(15f, 20f)"),
    ("Component/Tab", 14, 18, 500, 0, False, "Dock.kt TabBar: onest(14f, 18f, 500)"),
    ("Component/Tab selected", 14, 18, 700, 0, False, "Dock.kt TabBar, selected: onest(14f, 18f, 700)"),
    ("Component/Mini title", 15, 20, 600, 0, False, "Dock.kt MiniPlayer title: onest(15f, 20f, 600)"),
    ("Component/Mini artist", 13, 17, 400, 0, False, "Dock.kt MiniPlayer artist: onest(13f, 17f)"),
    ("Component/Settings title", 16.5, 22, 500, 0, False, "Controls.kt SetRow title: onest(16.5f, 22f, 500)"),
    ("Component/Settings summary", 13.5, 19, 400, 0, False, "Controls.kt SetRow summary: onest(13.5f, 19f)"),
    ("Component/Settings value", 14, 18, 400, 0, False, "Controls.kt SetRow value: onest(14f, 18f)"),
    ("Component/Settings block", 14, 20, 400, 0, False, "Controls.kt SetBlockText: onest(14f, 20f)"),
    ("Component/Small print", 12.5, 17, 400, 0, False, "Controls.kt SetBlockSmall, Rows.kt KeyValue label: onest(12.5f, 17f)"),
    ("Component/Stat label", 13, 18, 400, 0, False, "Controls.kt Stats label: onest(13f, 18f)"),
    ("Component/Note", 13, 18, 400, 0, False, "Groups.kt Note: onest(13f, 18f)"),
    ("Component/Label small", 12.5, 18, 400, 0, False, "Groups.kt Label's `small`: onest(12.5f, 18f)"),
    ("Component/Row value", 13.5, 18, 400, 0, True, "Rows.kt CollectionRow value: onest(13.5f, 18f).tabular()"),
    ("Component/Row rank", 17, 22, 700, 0, True, "Rows.kt SongRow rank: onest(17f, 22f, 700).tabular()"),
    ("Component/Row number", 15, 20, 400, 0, True, "Rows.kt SongRow track number: onest(15f, 20f).tabular()"),
    ("Component/Row meta", 13, 18, 400, 0, True, "Rows.kt SongRow meta: onest(13f, 18f).tabular()"),
    ("Component/Row title current", 16, 22, 700, 0, False, "Rows.kt SongRow, current: rowTitle with FontWeight.Bold"),
    ("Component/Tile title", 14, 18, 600, 0, False, "Tiles.kt Tile title: onest(14f, 18f, 600)"),
    ("Component/Tile title centred", 13.5, 18, 500, 0, False, "Tiles.kt Tile, centered: onest(13.5f, 18f, 500)"),
    ("Component/Browse title", 16, 21, 600, 0, False, "Tiles.kt BrowseTile title: onest(16f, 21f, 600)"),
    ("Component/Header byline", 15, 20, 600, 0, False, "Tiles.kt CollectionHeader byline: onest(15f, 20f, 600)"),
    ("Component/Header meta", 13.5, 19, 400, 0, False, "Tiles.kt CollectionHeader meta: onest(13.5f, 19f)"),
    ("Component/Player context", 15.5, 20, 600, 0, False, "NowPlayingParts.kt NowPlayingHeader context: onest(15.5f, 20f, 600)"),
    ("Component/Player artist", 16, 22, 400, 0, False, "NowPlayingParts.kt NowPlayingTitle artist: onest(16f, 22f)"),
    ("Component/Player lyric", 14, 20, 500, 0, False, "NowPlayingParts.kt NowPlayingLyric: onest(14f, 20f, 500)"),
    ("Component/Seek time", 14, 18, 600, 0, True, "NowPlayingParts.kt SeekBar times: onest(14f, 18f, 600).tabular()"),
    ("Component/Recap entry title", 17, 22, 700, 0, False, "ComfortHome.kt RecapEntry headline: onest(17f, 22f, 700)"),
    ("Component/Lyric line", 22, 30, 600, -0.012, False, "ComfortLyrics.kt SyncedLine, sung and to come: 22/30, weight 600, -0.012em"),
    ("Component/Lyric line current", 27, 34, 700, -0.02, False, "ComfortLyrics.kt SyncedLine, the line being sung: 27/34, weight 700, -0.02em"),
    ("Component/Lyrics plain", 20, 31, 500, -0.005, False, "ComfortLyrics.kt PlainLyrics: onest(20f, 31f, 500, tracking = -0.005f)"),
    ("Component/Swipe action", 14, 18, 600, 0, False, "Rows.kt SwipeToRemove's Remove: onest(14f, 18f, 600)"),
    ("Component/Action", 12, 15, 500, 0, False, "Controls.kt ActionBarItem: onest(12f, 15f, 500)"),
    ("Component/Action go", 12, 15, 700, 0, False, "Controls.kt ActionBarItem, go: onest(12f, 15f, 700)"),
    ("Component/Index letter", 10.5, 15.5, 600, 0, False, "Controls.kt IndexScrubber: onest(10.5f, 15.5f, 600)"),
    ("Component/Index pop", 26, 32, 700, 0, False, "Controls.kt IndexPop: onest(26f, 32f, 700)"),
    ("Component/Quick action", 12.5, 16, 500, 0, False, "ComfortSheets.kt QuickAction label: onest(12.5f, 16f, 500)"),
    ("Component/Onboarding title", 30, 36, 700, -0.022, False, "ComfortOnboarding.kt Title: onest(30f, 36f, 700, -0.022f)"),
    ("Component/Onboarding body", 16, 23, 400, 0, False, "ComfortOnboarding.kt Body: onest(16f, 23f)"),
    ("Component/Status time", 15, 20, 500, 0, False, "storeShots ShotHarness.kt SystemBars: labelLarge at 15sp, Medium (the render harness's clock)"),
    ("Component/Sort", 14, 18, 600, 0, False, "ComfortLibrary.kt SortControl: onest(14f, 18f, 600)"),
    ("Component/Playlist tool", 14.5, 20, 600, 0, False, "ComfortCollections.kt ToolHalf label (\"Add songs\", \"Reorder songs\"): onest(14.5f, 20f, 600)"),
]
# Sheets, menus, dialogs and the toast (Overlays.kt), drawn by components_overlays.
OVERLAY_TYPE = [
    ("Component/Sheet header title", 17, 22, 600, 0, False, "Overlays.kt SheetHeader title: onest(17f, 22f, 600)"),
    ("Component/Overlay row", 16, 22, 400, 0, False, "Overlays.kt SheetRow and MenuRow label: onest(16f, 22f, 400)"),
    ("Component/Overlay row selected", 16, 22, 600, 0, False, "Overlays.kt SheetRow and MenuRow, selected: onest(16f, 22f, 600)"),
    ("Component/Sheet value", 14, 18, 400, 0, False, "Overlays.kt SheetRow value (\"23 min left\"): onest(14f, 18f)"),
    ("Component/Menu caption", 13, 18, 600, 0, False, "Overlays.kt MenuCaption: onest(13f, 18f, 600)"),
    ("Component/Field label", 12.5, 16, 500, 0, False, "Overlays.kt DialogField label: onest(12.5f, 16f, 500)"),
    ("Component/Field", 17, 24, 400, 0, False, "Overlays.kt DialogField text and placeholder: onest(17f, 24f)"),
    ("Component/Toast", 14.5, 20, 400, 0, False, "Overlays.kt Toast: onest(14.5f, 20f)"),
]
# The Recap card drawing's own onest() calls (ComfortRecapCards.kt, ComfortRecapStory.kt, ComfortRecap.kt).
RECAP_TYPE = [  # name, size, line, weight, tracking (em), tabular, source
    ("Recap/Label", 15, 20, 600, 0, False, "CardLabel = onest(15f, 20f, 600): \"Your top song\""),
    ("Recap/Sub", 17, 24, 400, 0, False, "CardSub = onest(17f, 24f)"),
    ("Recap/Intro body", 19, 27, 400, 0, False, "IntroCard body: onest(19f, 27f)"),
    ("Recap/Headline 44", 44, 48, 800, -0.03, False, "headline(44f, 48f): top artist, listener type"),
    ("Recap/Headline 40", 40, 44, 800, -0.03, False, "headline(40f, 44f): intro line, top language"),
    ("Recap/Headline 34", 34, 38, 800, -0.03, False, "headline(34f, 38f): top songs, top artists, highlights"),
    ("Recap/Headline 32", 32, 36, 800, -0.03, False, "headline(32f, 36f): the top song's title"),
    ("Recap/Headline 28", 28, 32, 800, -0.03, False, "headline(28f, 32f): Summary's headline"),
    ("Recap/Big figure", 80, 80, 800, -0.05, False, "LampNumber: headline(80f, 80f, tracking = -0.05f), fitted to the width"),
    ("Recap/Minutes unit", 30, 36, 700, -0.02, False, "LampNumber unit: onest(30f, 36f, 700, tracking = -0.02f)"),
    ("Recap/Minutes line", 24, 31, 700, -0.015, False, "MinutesCard line: onest(24f, 31f, 700, tracking = -0.015f)"),
    ("Recap/Percent large", 104, 135, 800, -0.055, False, "LanguagesCard: headline(104f, 98f, tracking = -0.055f). The code asks for a "
     "98sp line; Android lays the one line out at the font's own height, about 135, so the style uses 135 (measured on the renders)"),
    ("Recap/Pill", 15, 20, 700, 0, False, "TopSongCard plays pill: onest(15f, 20f, 700)"),
    ("Recap/Rank", 20, 24, 800, 0, False, "RankRow rank: onest(20f, 24f, 800)"),
    ("Recap/Rank title", 16, 19, 700, 0, False, "RankRow title: onest(16f, 19f, 700)"),
    ("Recap/Rank subtitle", 13, 17, 400, 0, False, "RankRow subtitle: onest(13f, 17f); Summary labels"),
    ("Recap/Rank value", 15, 20, 700, 0, True, "RankRow count: onest(15f, 20f, 700).tabular()"),
    ("Recap/Language", 15, 20, 600, 0, False, "LanguagesCard row name; Summary list names: onest(15f, 20f, 600)"),
    ("Recap/Language share", 15, 20, 400, 0, True, "LanguagesCard row share: onest(15f, 20f).tabular()"),
    ("Recap/Highlight value", 30, 34, 800, -0.03, False, "HighlightsCard value: headline(30f, 34f)"),
    ("Recap/Highlight line", 15, 20, 400, 0, False, "HighlightsCard line: onest(15f, 20f)"),
    ("Recap/Summary stat", 26, 30, 800, -0.025, False, "SummaryStat: headline(26f, 30f, tracking = -0.025f), fitted"),
    ("Recap/Summary list label", 13, 17, 600, 0, False, "SummaryList label: onest(13f, 17f, 600)"),
    ("Recap/Summary rank", 15, 20, 600, 0, True, "SummaryList rank: onest(15f, 20f, 600).tabular()"),
    ("Recap/Footer name", 14, 18, 700, 0, False, "Footer \"Attic\": onest(14f, 18f, 700)"),
    ("Recap/Footer period", 13, 18, 500, 0, False, "Footer period: onest(13f, 18f, 500)"),
    ("Recap/Clock hour", 25.16, 30.19, 800, 0, False, "ListeningClock busiest hour: onest(17f x u, 800), u = 296/200 on the card"),
    ("Recap/Clock label", 10.95, 13.14, 500, 0, False, "ListeningClock midnight and noon: onest(7.4f x u, 500), u = 296/200"),
    ("Recap/Story position", 13, 18, 600, 0, True, "StoryDeck \"4 of 10\": onest(13f, 18f, 600).tabular()"),
    ("Recap/Play title", 18, 24, 700, -0.01, False, "PlayRecap: onest(18f, 24f, 700, tracking = -0.01f)"),
    ("Recap/Language row", 16, 22, 500, 0, False, "Languages row: onest(16f, 22f, 500)"),
    ("Recap/Percent", 14, 18, 400, 0, True, "Languages row share: onest(14f, 18f).tabular()"),
]
# The home-screen widget (RemoteViews) and Expressive's look-card preview, drawn at their own sizes.
OTHER_TYPE = [
    ("Widget/Title", 16, 20, 700, 0, False, "widget_styles.xml WidgetText.Comfort.Title: Onest, bold, 16sp"),
    ("Widget/Artist", 13, 16, 400, 0, False, "widget_styles.xml WidgetText.Comfort.Artist: Onest, 13sp"),
    ("Preview/Headline", 28, 36, 600, 0, False, "ExpressiveLookPreview \"Home\": headlineMediumEmphasized (drawn in Google Sans Flex, scaled 0.42)"),
    ("Preview/Label", 14, 20, 500, 0, False, "ExpressiveLookPreview's connected buttons: labelLarge"),
    ("Preview/Nav label", 12, 16, 500, 0, False, "ExpressiveLookPreview's navigation bar labels: labelMedium"),
]
# Android's own chrome on the System surfaces page. Not Attic's type.
SYSTEM_TYPE = [
    ("System/Clock large", 64, 72, 400, 0, False, "The lock screen clock (Android draws it)"),
    ("System/Title", 14, 20, 500, 0, False, "The notification's title (Android draws it)"),
    ("System/Body", 14, 20, 400, 0, False, "The notification's text and the shade's labels"),
    ("System/Small", 12, 16, 400, 0, False, "The notification's app line, quick tile labels"),
    ("System/Home label", 12, 16, 500, 0, False, "Launcher icon labels"),
]

DOC_WEIGHTS = {  # canvas_basics' Doc styles with the weights it asks for (see the kit report)
    "Doc/Page title": (96, 104, 600), "Doc/Section title": (48, 56, 600), "Doc/Heading": (28, 36, 600),
    "Doc/Body": (20, 30, 400), "Doc/Label": (16, 22, 500), "Doc/Caption": (13, 18, 400), "Doc/Code": (14, 20, 400),
}

# Variables first made by the screen modules, created here so every token has one home.
KEYBOARD = [("system/keyboard", "#E4E5EA", "#1F1F22", "the keyboard's ground"), ("system/key", "#FFFFFF", "#3A3A3F", "a key")]
LANGUAGE_STEPS = [(2, 0.13), (3, 0.26)]
BRAND = [("headroom/ground", "#C6533A", "headroom_ground: the adaptive icon's background layer"),
         ("headroom/cabinet", "#F5EDDD", "headroom_cabinet: the cabinet in the colour icon"),
         ("headroom/cone", "#2A211C", "headroom_cone: the woofer cone and the tweeter in the colour icon")]
EXPRESSIVE_ROLES = ["surface", "onSurface", "onSurfaceVariant", "primary", "onPrimary", "surfaceContainer",
                    "surfaceContainerHigh", "surfaceContainerHighest", "secondaryContainer", "onSecondaryContainer"]
# Every collection with Light and Dark modes: Area.close() and _modes() switch these together.
MODE_COLLECTIONS = ("Palette", "Tinted", "Elevation", "Expressive preview")

# ======================================================================== annotations shared by the screen modules
# Where a screen follows the code on a phone rather than the JVM reference render, it says so in this category, in
# these words, so the same gap reads the same everywhere.
REF_DIFFERS = "Reference render differs"
_HARNESS = ("The reference render comes from a JVM harness that gives dialog windows (sheets, dialogs, the Recap story) "
            "no system insets. ")
HARNESS_NOTES = {
    "sheet": _HARNESS + "There the sheet's foot ends 30dp under its last row, on the bottom edge. On a phone "
                        "ComfortSheet's navigationBarsPadding() adds the 24dp navigation inset under the last row, as drawn "
                        "here, so everything in the sheet sits 24dp higher than in the render.",
    "dialog": _HARNESS + "There the dialog sits 22dp above the bottom edge. On a phone ComfortDialog's "
                         "navigationBarsPadding() lifts it over the 24dp navigation inset, as drawn here: 46dp above the edge.",
    "story": _HARNESS + "There the story's header touches the top edge and its deck the bottom. On a phone "
                        "statusBarsPadding() and navigationBarsPadding() apply, as drawn here, and the card shrinks to fit "
                        "between them.",
}


# ======================================================================== dimensions and motion
RADII = [
    ("radius/float", 30, "ComfortShapes.FloatRadius", "Things that float: dock, deck, sheet, dialog, action bar (--r-float)."),
    ("radius/group", 26, "ComfortShapes.GroupRadius", "Groups and cards (--r-group)."),
    ("radius/control", 18, "ComfortShapes.ControlRadius", "A control nested in a card: seek bar, current row bed (--r-ctl)."),
    ("radius/menu", 22, "ComfortShapes.MenuRadius", "The pop-over menu."),
    ("radius/extra-small", 8, "ComfortShapes.material.extraSmall", "Material's extraSmall slot."),
    ("radius/pill", 999, "ComfortShapes.Pill", "RoundedCornerShape(50): buttons, chips, tabs, switch track."),
]
SPACE_SCALE = [2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 28, 30, 32, 40, 48]
SPACE_ROLES = [
    ("space/group-margin", 16, "GroupMargin", "A group's margin from the screen edges (Groups.kt)."),
    ("space/group-gap", 12, "GroupSpacing", "Between two groups in a column (Groups.kt)."),
    ("space/row-gap", 14, "Arrangement.spacedBy(14.dp)", "Between a row's art, text and trailing parts (Rows.kt)."),
    ("space/screen-edge", 24, "Shelf contentPadding", "A shelf's edge on the ground; tools row start (Tiles.kt, Bars.kt)."),
    ("space/sheet-margin", 24, "`.sheet .row`", "Rows in a sheet line up 24dp in (Rows.kt)."),
    ("space/label-inset", 28, "Label padding", "Section labels and notes start 28dp in (Groups.kt)."),
    ("space/player-inset", 30, "NowPlayingCover padding", "Now Playing's cover and titles (NowPlayingParts.kt)."),
    ("space/dock-inset", 12, "Dock padding", "The dock and the action bar sit 12dp in from the edges (Dock.kt)."),
    ("space/dock-bottom", 22, "Dock bottom", "The dock's gap to the bottom edge: max(22, navigation inset + 6) (Dock.kt)."),
    ("space/shelf-gap", 14, "Shelf spacedBy", "Between tiles on a shelf (Tiles.kt)."),
]
SIZES = [
    ("size/icon", 24, "ComfortIcon default"), ("size/target", 48, "ComfortIconButton"),
    ("size/tonal", 48, "TonalIconButton"), ("size/tonal-large", 54, "TonalIconButton(large = true)"),
    ("size/play-large", 76, "PlaySize.Large"), ("size/play-medium", 56, "PlaySize.Medium"), ("size/play-small", 44, "PlaySize.Small"),
    ("size/skip", 64, "SkipButton"), ("size/button-small", 36, "ButtonSize.Small"), ("size/button", 48, "ButtonSize.Medium"),
    ("size/button-large", 56, "ButtonSize.Large"), ("size/chip", 36, "FilterChip"), ("size/search", 50, "SearchField"),
    ("size/song-row", 64, "SongRow min height"), ("size/row-small", 64, "RowSize.Small"), ("size/row", 72, "RowSize.Medium"),
    ("size/row-large", 92, "RowSize.Large"), ("size/settings-row", 60, "SetRow min height"),
    ("size/art-song", 48, "SongRow cover"), ("size/art-row-small", 48, "RowSize.Small art"), ("size/art-row", 56, "RowSize.Medium art"),
    ("size/art-row-large", 64, "RowSize.Large art"), ("size/art-mini", 44, "MiniPlayer cover"),
    ("size/art-header", 196, "CollectionHeader art"), ("size/art-person", 148, "CollectionHeader(person) art"),
    ("size/large-art-from", 100, "ComfortShapes.LargeArtFrom: .sq-lg from here"),
    ("size/mini-player", 64, "MiniPlayer"), ("size/mini-player-top", 60, "MiniPlayerPlacement.Top"),
    ("size/tab-bar", 60, "TabBar"), ("size/tab", 44, "TabBar item"), ("size/action-bar", 68, "ActionBar"),
    ("size/seek", 48, "SeekBar"), ("size/seek-thumb", 64, "SeekBar: the fill is never narrower"),
    ("size/bar", 56, "Bar"), ("size/tools", 50, "Tools"), ("size/player-header", 60, "NowPlayingHeader"),
    ("size/hero", 228, "Hero"), ("size/hero-short", 150, "Hero(short = true)"),
    ("size/switch-width", 52, "ComfortSwitch track"), ("size/switch-height", 30, "ComfortSwitch track"),
    ("size/knob", 24, "ComfortSwitch knob"), ("size/radio", 24, "ComfortRadio"),
    ("size/progress", 10, "ComfortProgress"), ("size/progress-thick", 16, "ComfortProgress(thick = true)"),
    ("size/status-bar", 40, "the render harness's status bar inset"), ("size/gesture-bar", 24, "the render harness's navigation inset"),
]
# (variable, value, what, source)
MOTION = [
    ("duration/tint", 450, "Every token tweens when the seed changes (a new song)", "Tinted.kt TintSpec: tween(durationMillis = 450)"),
    ("duration/control", 160, "Switch track colour and knob slide; radio ring", "Controls.kt ComfortSwitch, ComfortRadio: tween(160)"),
    ("duration/cover-pause", 260, "Now Playing cover scales to 86 % when paused", "NowPlayingParts.kt NowPlayingCover: tween(260)"),
    ("duration/lyric-line", 240, "A lyric line grows onto its bed; the list scrolls the line to a third of the way down", "ComfortLyrics.kt LineMs = 240"),
    ("duration/nav-detail-enter", 300, "A detail screen slides in from 1/8 of the width at the end, fading in over 220", "ComfortShell.kt ComfortNavTransitions: slideInHorizontally(tween(300)) { it / 8 } + fadeIn(tween(220))"),
    ("duration/nav-tab-enter", 220, "A tab root fades through, starting 60 ms late", "ComfortShell.kt: fadeIn(tween(220, delayMillis = 60))"),
    ("duration/nav-exit", 120, "The screen left behind fades out", "ComfortShell.kt: exit = fadeOut(tween(120))"),
    ("duration/nav-pop-exit", 260, "Back: the detail screen slides out by 1/8 while fading over 180", "ComfortShell.kt: slideOutHorizontally(tween(260)) { it / 8 } + fadeOut(tween(180))"),
    ("duration/mini-expand", 240, "The mini player row unfolds into the dock when a song starts", "ComfortShell.kt: expandVertically(tween(240)) + fadeIn(tween(240))"),
    ("duration/mini-collapse", 180, "and folds away when the queue empties", "ComfortShell.kt: shrinkVertically(tween(180)) + fadeOut(tween(180))"),
    ("duration/bar-fade", 150, "A root screen's collapsed bar fades in once the hero scrolls away", "ComfortRoot.kt: fadeIn(tween(150)), fadeOut(tween(150))"),
    ("duration/compact-header-in", 160, "Now Playing's compact header (Mini player, Top) fades in; out over 120", "ComfortNowPlaying.kt CompactHeader: fadeIn(tween(160)), fadeOut(tween(120))"),
    ("duration/index", 200, "Library's index scrubber makes room (28dp) and fades over 150", "ComfortLibrary.kt: animateDpAsState(tween(200)), fadeIn/fadeOut(tween(150))"),
    ("duration/story-card", 220, "Recap story cards crossfade", "ComfortRecapStory.kt: Crossfade(tween(220))"),
    ("duration/recap-count", 1600, "Recap figures count up, FastOutSlowIn", "ComfortRecapCards.kt: tween(1600, easing = FastOutSlowInEasing)"),
    ("duration/spinner-turn", 900, "The spinner turns once, linear, repeating", "Controls.kt Spinner: infiniteRepeatable(tween(900, LinearEasing))"),
    ("spring/overlay-stiffness", 380, "Now Playing, Queue and Lyrics rise from the bottom and leave the same way; damping 1 (no overshoot)", "ComfortShell.kt overlaySpring(): spring(dampingRatio = 1f, stiffness = 380f)"),
    ("spring/drag-back-stiffness", 500, "Now Playing springs back when a pull-down is let go short of closing; damping 1", "ComfortNowPlaying.kt: spring(dampingRatio = 1f, stiffness = 500f)"),
]
MOTION_EXTRA = [
    ("Playing bars", "Three bars bounce between 26 % and 100 % with periods 420, 560 and 340 ms, linear, reversing, staggered by 120 ms; at rest all three are 26 %.", "Rows.kt PlayingBars"),
    ("Default easing", "Compose's tween() default, FastOutSlowInEasing = cubic-bezier(0.4, 0, 0.2, 1), wherever no easing is named.", "androidx.compose.animation.core"),
    ("Swipe to skip", "Swiping the mini player's text past 28 % of its width skips; it snaps back otherwise.", "Dock.kt SwipeText"),
    ("Pull down to close", "Now Playing follows a pull down; past the dismiss distance it closes (240 ms delay before resetting).", "ComfortNowPlaying.kt"),
]

# Glyphs the Comfort code draws filled (ComfortIcon(filled = true)): the selected tab, a favourite that is on,
# play and pause, and every tonal toggle while checked.
# How far below the 24dp box's centre Compose draws a glyph (SymbolIcon: the symbol as 1em text in a centred line box),
# measured on the renders' back arrows, more buttons, the settings gear and the tabs. Each Icon/* draws its Glyph this
# much lower, overflowing the box's foot as the app's does; resized instances scale it.
ICON_DROP = 2.6

FILLED = ["favorite", "play_arrow", "pause", "home", "search", "library_music", "shuffle", "repeat", "repeat_one",
          "playlist_add", "skip_previous", "skip_next"]
WEIGHT_400 = ["play_arrow", "pause", "skip_previous", "skip_next"]


# ======================================================================== shapes
def superellipse_points(n: float, w: float, h: float, samples: int = 720, step: int = 2):
    """SuperellipseShape.unitPoints scaled to w x h: x = 0.5 + 0.5 sign(cos t)|cos t|^(2/n), the same for y
    with sin, t = 2 pi i / 720. `step` keeps every step-th sample, so every vertex lies on the Compose outline."""
    p = 2.0 / n
    pts = []
    for i in range(0, samples, step):
        t = 2 * math.pi * i / samples
        c, s = math.cos(t), math.sin(t)
        pts.append(((0.5 + 0.5 * math.copysign(abs(c) ** p, c)) * w, (0.5 + 0.5 * math.copysign(abs(s) ** p, s)) * h))
    return pts


def superellipse_path(n: float, w: float = 100, h: float = 100) -> str:
    """The traced outline fitted with cubic Béziers to within 0.1 % of its size (figbuild.pathfit),
    so each instance stores 8 to 24 curves instead of 360 straight segments."""
    return fit_closed(superellipse_points(n, w, h))


SHAPES = [
    ("Shape/Squircle", 4.0, "ComfortShapes.Squircle = SuperellipseShape(4.0), kit.css --sq. Covers, icon boxes, tonal buttons and the play button drawn under 100dp."),
    ("Shape/Squircle large", 5.0, "ComfortShapes.SquircleLarge = SuperellipseShape(5.0), kit.css --sq-lg. Covers drawn at 100dp and up (ArtShape picks by size)."),
    ("Shape/Squircle soft", 2.6, "ComfortShapes.SquircleSoft = SuperellipseShape(2.6), kit.css --sq-soft. Artists, at every size. Never a circle."),
]


# ======================================================================== documentation layout
class Sheet:
    """A documentation page laid out as rows of white cards under a title and an intro.

        s = Sheet(d, "Colour", "Colour", "intro ...")
        c = s.card("Palette", 1800)      # an auto layout card at the cursor
        s.heading(c, "Palette"); ...
        s.done(c)                         # measures it and moves the cursor right
        s.newrow()
    """
    MARGIN_TOP, GAP = 340, 80

    def __init__(self, d: Doc, page: str, title: str, intro: str, intro_w: int = 1400):
        self.d = d
        self.pid = d.page(page)
        d.text(None, title, style="Doc/Page title", color="Canvas/label", name="Page title", x=0, y=0)
        d.text(None, intro, style="Doc/Body", color="Canvas/label", name="Intro", x=0, y=140,
               width=intro_w, textAutoResize="height")
        self.x, self.y, self.row_h = 0, self.MARGIN_TOP, 0

    def card(self, name: str, w: float, *, pad=48, gap=24, fill="Canvas/card") -> int:
        c = self.d.frame(None, name=name, x=self.x, y=self.y, w=w, layout="v", gap=gap, pad=pad, fill=fill,
                         radius=32, sizing=("fixed", "hug"))
        return c

    def done(self, card: int):
        g = self.d.get(card)
        self.x += g["width"] + self.GAP
        self.row_h = max(self.row_h, g["height"])

    def newrow(self):
        self.x = 0
        self.y += self.row_h + self.GAP
        self.row_h = 0

    # text helpers (all on the Canvas colours: documentation, not product)
    def heading(self, parent, text, name=None):
        return self.d.text(parent, text, style="Doc/Heading", color="Canvas/label", name=name or f"Heading: {text[:40]}")

    def body(self, parent, text, w=None, name=None, muted=False):
        kw = {}
        if w:
            kw.update(width=w, textAutoResize="height")
        return self.d.text(parent, text, style="Doc/Body", color="Canvas/label-muted" if muted else "Canvas/label",
                           name=name or f"Note: {text[:40]}", **kw)

    def label(self, parent, text, name=None, style="Doc/Label", muted=False, w=None):
        kw = {}
        if w:
            kw.update(width=w, textAutoResize="height")
        return self.d.text(parent, text, style=style, color="Canvas/label-muted" if muted else "Canvas/label",
                           name=name or f"Label: {text[:40]}", **kw)

    def row(self, parent, name, gap=16, cross="center", pad=None, **kw):
        return self.d.frame(parent, name=name, layout="h", gap=gap, cross=cross, pad=pad, sizing=("hug", "hug"), **kw)

    def col(self, parent, name, gap=8, pad=None, **kw):
        return self.d.frame(parent, name=name, layout="v", gap=gap, pad=pad, sizing=("hug", "hug"), **kw)


def page_header(d: Doc, page: str, title: str, intro: str):
    """The title and intro every documentation page opens with (as Sheet draws them), for a page of screen areas:
    the areas then start where Sheet's cards start, under the intro."""
    from figbuild.kit import Area
    d.page(page)
    d.text(None, title, style="Doc/Page title", color="Canvas/label", name="Page title", x=0, y=0)
    d.text(None, intro, style="Doc/Body", color="Canvas/label", name="Intro", x=0, y=140, width=1700, textAutoResize="height")
    Area._next_y[(id(d), page)] = max(Area._next_y.get((id(d), page), 0), Sheet.MARGIN_TOP)


def _modes(d: Doc, ids, theme: str, color_mode: str | None = None):
    """Light or Dark on every Light/Dark collection, and optionally a Color mode."""
    for c in MODE_COLLECTIONS:
        d.set_mode(ids, c, theme)
    if color_mode:
        d.set_mode(ids, "Color", color_mode)


# ======================================================================== build
# Annotation categories, each in its own colour (FigCraft hands out the next unused colour otherwise, and the fifth
# one repeats the built-in Development blue).
ANN_CATEGORIES = [("Known issue", "#D63A3A"), ("Source", "#D13F9B"), ("Code differs from mock", "#1192A8"),
                  ("Note", "#6B7280"), (REF_DIFFERS, "#65A30D")]


def build(d: Doc, ctx: dict):
    for name, colour in ANN_CATEGORIES:
        d.ann_category(name, color=colour)
    _doc_weights(d)
    ids = _variables(d, ctx)
    _type_styles(d)
    _effect_styles(d, ids)
    _colour_page(d, ctx, ids)
    _type_page(d)
    _shape_page(d, ctx, ids)
    _elevation_page(d)
    _icons_page(d)
    ctx["tokens"] = ids


def _doc_weights(d: Doc):
    """canvas_basics creates Doc/* with the weight in fontVariations, which style values ignore (they come out
    Regular). Re-save each with its named weight so the documentation reads as intended."""
    names = {400: "Regular", 500: "Medium", 600: "SemiBold", 700: "Bold"}
    for name, (size, line, w) in DOC_WEIGHTS.items():
        sid = d.text_styles.get(name)
        if sid:
            d.cmd("style.update", style=sid, value={"fontFamily": "Google Sans Flex", "fontStyle": names[w],
                                                    "fontSize": size, "lineHeight": line})


# ------------------------------------------------------------------------ variables
def _var(d: Doc, ids: dict, collection: str, name: str, type_: str, *, values=None, value=None,
         description=None, android=None, scopes=None) -> str:
    args = {"collection": collection, "name": name, "type": type_}
    if values is not None:
        args["values"] = values
    if value is not None:
        args["value"] = value
    if description:
        args["description"] = description
    vid = d.cmd("variable.create", **args)["id"]
    upd = {}
    if android:
        upd["codeSyntax"] = {"ANDROID": android}
    if scopes is not None:
        upd["scopes"] = scopes
    if upd:
        d.cmd("variable.update", variable=vid, **upd)
    ids[f"{collection}/{name}"] = vid
    return vid


def _variables(d: Doc, ctx: dict) -> dict:
    ids: dict[str, str] = {}
    pal = load_palette()
    tint = tinted_values(EXAMPLE_SEED)
    ctx["palette"], ctx["tinted"] = pal, tint
    L, C, H = oklch_from_hex(EXAMPLE_SEED)

    # -- Palette
    d.collection("Palette", modes=["Light", "Dark"])
    for kotlin, name, what in TOKENS:
        vname = f"lamplight/{name}" if kotlin in LAMPLIGHT else name
        lv, dv = pal["light"][kotlin], pal["dark"][kotlin]
        _var(d, ids, "Palette", vname, "color", values={"Light": lv, "Dark": dv},
             description=f"{what} ComfortLight.{kotlin} {hexa(lv)}, ComfortDark.{kotlin} {hexa(dv)} (ComfortColors.kt).",
             android=f"ComfortColors.{kotlin}")
    for name, (lv, dv, code) in ERROR_EXTRA.items():
        _var(d, ids, "Palette", name, "color", values={"Light": lv, "Dark": dv},
             description=f"Only in the Material scheme: {code} (ComfortColors.kt toColorScheme).",
             android=f"MaterialTheme.colorScheme.{code.split(' =')[0]}")
    _var(d, ids, "Palette", "ground-toggle", "color",
         values={"Light": {"alias": ids["Palette/card"]}, "Dark": {"alias": ids["Palette/fill"]}},
         description="A tonal toggle standing on the ground of a collection screen: card in light, fill in dark "
                     "(ComfortCollections.kt PlayRow: container = if (colors.dark) colors.fill else colors.card). Live alias per mode.",
         android="if (colors.dark) colors.fill else colors.card")
    _var(d, ids, "Palette", "system/ink", "color", values={"Light": "#1B1B1F", "Dark": "#FFFFFF"},
         description="The status bar and gesture handle the render harness draws (storeShots ShotHarness.kt SystemBars): "
                     "Color(0xFF1B1B1F) light, Color.White dark. On a phone the system draws these.",
         android="SystemBars ink (ShotHarness.kt)")
    _var(d, ids, "Palette", "system/gesture", "color", value={"alpha": {"variable": ids["Palette/system/ink"], "opacity": 0.85}},
         description="The gesture handle: system/ink at 85 % (ink.copy(alpha = 0.85f), ShotHarness.kt). A live alpha of system/ink.")
    for name, lv, dv, what in KEYBOARD:
        _var(d, ids, "Palette", name, "color", values={"Light": lv, "Dark": dv},
             description=f"The system keyboard the render harness stands in ({what}): designrefs DesignHarness.kt Keyboard, "
                         f"{lv} light, {dv} dark. Attic does not draw a keyboard.", android="Keyboard (DesignHarness.kt)")
    for n, drop in LANGUAGE_STEPS:
        vals = {}
        for theme, mode in (("light", "Light"), ("dark", "Dark")):
            l_, c_, h_ = oklch_from_hex(pal[theme]["accent"][:7])
            vals[mode] = oklch_to_hex(max(0.0, l_ - drop), c_, h_)
        _var(d, ids, "Palette", f"recap/language-{n}", "color", values=vals,
             description=f"Recap's language step {n}: the accent at OKLCH lightness -{drop} (ComfortRecapCards.kt languageSteps; "
                         f"light {vals['Light']}, dark {vals['Dark']}).", android=f"languageSteps()[{n - 1}]")

    # -- Tinted
    d.collection("Tinted", modes=["Light", "Dark"])
    _var(d, ids, "Tinted", "seed", "color", values={"Light": EXAMPLE_SEED, "Dark": EXAMPLE_SEED},
         description=f"The cover's swatch, not deepened: rememberArtColor(mediaId, Color.Unspecified, deepen = false) "
                     f"(ui/Art.kt coverSwatch: androidx Palette vibrant ?: muted ?: dominant). Example: Paper Lanterns "
                     f"{EXAMPLE_SEED}, OKLCH L {L:.4f}, C {C:.4f}, h {H:.2f}. Only its hue and chroma are read.",
         android="rememberArtColor(mediaId, Color.Unspecified, deepen = false)")
    _var(d, ids, "Tinted", "wash", "boolean", values={"Light": True, "Dark": True},
         description="Tinted(seed, wash). True for the example (Now Playing, Queue, Lyrics are washed). In the file the "
                     "choice is the Color mode: `Tinted` (wash = false) or `Tinted wash` (wash = true).",
         android="Tinted(seed, wash = true)")
    for group, keys, tables in (("tinted", TINTED_TOKENS, (LIGHT_TINTED, DARK_TINTED)), ("wash", WASH_TOKENS, (LIGHT_WASH, DARK_WASH))):
        for k in keys:
            name = dict((a, b) for a, b, _ in TOKENS)[k]
            lt, dt = tables[0].get(k), tables[1][k]
            light_rule = (f"light `\"{k}\" to Spec({lt[0]}, {lt[1]})` = oklch({lt[0]}, seed.c x {lt[1]}, seed.h)"
                          if lt else "light: Color.White (Tinted.kt apply: `onAccent = if (base.dark) token(\"onAccent\", base.onAccent) else Color.White`)")
            dark_rule = f"dark `\"{k}\" to Spec({dt[0]}, {dt[1]})` = oklch({dt[0]}, seed.c x {dt[1]}, seed.h)"
            table = "darkTinted / lightTinted" if group == "tinted" else "darkWash / lightWash"
            _var(d, ids, "Tinted", f"{group}/{name}", "color",
                 values={"Light": tint["light"][k], "Dark": tint["dark"][k]},
                 description=(f"ComfortTint.{table} (Tinted.kt): {dark_rule}; {light_rule}. Stored for the example seed "
                              f"(light {tint['light'][k]}, dark {tint['dark'][k]}): FigCraft variables can't keep a hue "
                              f"and set an OKLCH lightness, so recompute with the seed (Oklch.kt; out of gamut, chroma drops "
                              f"at the same L and h)."),
                 android=f"ComfortTint.apply(base, seed, wash).{k}")

    # -- Color: the roles, all aliases
    d.collection("Color", modes=["Base", "Tinted", "Tinted wash"])
    for kotlin, name, what in TOKENS:
        base = ids[f"Palette/lamplight/{name}" if kotlin in LAMPLIGHT else f"Palette/{name}"]
        tinted = ids[f"Tinted/tinted/{name}"] if kotlin in TINTED_TOKENS else base
        washed = ids[f"Tinted/wash/{name}"] if kotlin in WASH_TOKENS else tinted
        src = ("Base: Palette" + (", Tinted and Tinted wash: Tinted/tinted" if kotlin in TINTED_TOKENS else "")
               + (", Tinted wash: Tinted/wash" if kotlin in WASH_TOKENS else ""))
        if kotlin not in TINTED_TOKENS and kotlin not in WASH_TOKENS:
            src = "Every mode: Palette (Tinted leaves it alone)"
        _var(d, ids, "Color", name, "color",
             values={"Base": {"alias": base}, "Tinted": {"alias": tinted}, "Tinted wash": {"alias": washed}},
             description=f"{what} {src}.", android=f"Comfort.colors.{kotlin}")
    _var(d, ids, "Color", "derived/lyric-sung", "color",
         value={"mix": {"from": {"variable": ids["Color/on-2"]}, "to": {"variable": ids["Color/on-3"]}, "t": 0.72}},
         description="A lyric line already sung: lerp(colors.on2, colors.on3, 0.72f) (ComfortLyrics.kt SyncedLine). Live mix.",
         android="lerp(Comfort.colors.on2, Comfort.colors.on3, 0.72f)")
    _var(d, ids, "Color", "derived/seek-tick", "color",
         value={"alpha": {"variable": ids["Color/on-accent"], "opacity": 0.4}},
         description="The seek bar's tick at the end of the fill: colors.onAccent.copy(alpha = 0.4f) (NowPlayingParts.kt SeekBar). Live alpha.",
         android="Comfort.colors.onAccent.copy(alpha = 0.4f)")
    _var(d, ids, "Color", "derived/ground-clear", "color",
         value={"alpha": {"variable": ids["Color/ground"], "opacity": 0.0}},
         description="The transparent end of every fade to the ground (Dock.kt, ComfortQueue.kt ClearFoot, ComfortLyrics.kt). "
                     "Compose fades from Color.Transparent; this is the ground at 0 % so the fade keeps its hue. Live alpha.",
         android="Color.Transparent")
    _var(d, ids, "Color", "derived/ground-toggle", "color", value={"alias": ids["Palette/ground-toggle"]},
         description="Container of a tonal toggle on a collection's ground (Play row): card in light, fill in dark. "
                     "Alias of Palette/ground-toggle.", android="if (colors.dark) colors.fill else colors.card")
    _var(d, ids, "Color", "derived/art-dim", "color", value="#00000099",
         description="Over the current song's cover and a playing tile: Color.Black.copy(alpha = 0.6f) (Artwork.kt Cover(dimmed), Tiles.kt Tile(now)).",
         android="Color.Black.copy(alpha = 0.6f)")
    for name in ("ink", "gesture"):
        _var(d, ids, "Color", f"system/{name}", "color", value={"alias": ids[f"Palette/system/{name}"]},
             description="Status bar and gesture handle ink (render harness). Alias of Palette/system/" + name + ".")
    for name, _, _, what in KEYBOARD:
        _var(d, ids, "Color", name, "color", value={"alias": ids[f"Palette/{name}"]},
             description=f"System keyboard stand-in ({what}). Alias of Palette/{name}.")
    for n, _ in LANGUAGE_STEPS:
        _var(d, ids, "Color", f"derived/language-{n}", "color", value={"alias": ids[f"Palette/recap/language-{n}"]},
             description=f"Step {n} of Recap's languages: the largest share takes the accent, then -0.13 and -0.26 in OKLCH "
                         f"lightness, a fourth language -0.38; No vocals takes on-3. Alias of Palette/recap/language-{n}.",
             android=f"languageSteps()[{n - 1}]")
    for role, tok in MATERIAL:
        src = ids[f"Palette/{tok}"] if "/" in tok else ids[f"Color/{dict((a, b) for a, b, _ in TOKENS)[tok]}"]
        _var(d, ids, "Color", f"material/{_kebab(role)}", "color", value={"alias": src},
             description=f"MaterialTheme.colorScheme.{role} = {tok if '/' in tok else 'Comfort.colors.' + tok} "
                         f"(ComfortColors.kt toColorScheme). Stock Material components draw with it.",
             android=f"MaterialTheme.colorScheme.{role}")

    # -- Elevation
    d.collection("Elevation", modes=["Light", "Dark"])
    for name, lv, dv, code in [("shadow/float-blur", 28, 36, "Shadow(radius = 36.dp dark, 28.dp light)"),
                               ("shadow/float-y", 8, 12, "offset = DpOffset(0.dp, 12.dp dark, 8.dp light)"),
                               ("shadow/small-blur", 12, 14, "small: Shadow(radius = 14.dp dark, 12.dp light)"),
                               ("shadow/small-y", 3, 4, "small: offset = DpOffset(0.dp, 4.dp dark, 3.dp light)")]:
        _var(d, ids, "Elevation", name, "number", values={"Light": lv, "Dark": dv},
             description=f"Groups.kt floatingShadow(): {code}.", android="Modifier.floatingShadow")

    # -- Dimension
    d.collection("Dimension", modes=["Value"])
    for name, v, code, what in RADII:
        _var(d, ids, "Dimension", name, "number", value=v, description=f"{what} {code}.", android=code)
    for v in SPACE_SCALE:
        _var(d, ids, "Dimension", f"space/{v}", "number", value=v, description=f"{v}dp.", android=f"{v}.dp")
    for name, v, code, what in SPACE_ROLES:
        _var(d, ids, "Dimension", name, "number", value={"alias": ids[f"Dimension/space/{v}"]},
             description=f"{what} Alias of space/{v}.", android=code)
    for name, v, code in SIZES:
        _var(d, ids, "Dimension", name, "number", value=v, description=f"{v}dp: {code}.", android=code)

    # -- Motion
    d.collection("Motion", modes=["Value"])
    for name, v, what, src in MOTION:
        _var(d, ids, "Motion", name, "number", value=v, description=f"{what}. {src}.")

    # -- Brand: the launcher icon's fixed colours
    d.collection("Brand", modes=["Value"])
    for name, hx, what in BRAND:
        _var(d, ids, "Brand", name, "color", value=hx,
             description=f"{what} (res/values/colors.xml). Fixed: the icon doesn't follow the theme.",
             android=f"R.color.{name.replace('headroom/', 'headroom_')}")

    # -- Expressive preview: the Material roles Expressive's look card draws with (first run, Settings' Style group)
    j = json.loads((COLORS_DIR / "expressive-base.json").read_text())
    d.collection("Expressive preview", modes=["Light", "Dark"])
    for role in EXPRESSIVE_ROLES:
        _var(d, ids, "Expressive preview", _kebab(role), "color",
             values={"Light": j["light"][role][:7], "Dark": j["dark"][role][:7]},
             description=f"MaterialTheme.colorScheme.{role} in the Expressive style, as the look card's preview draws it "
                         f"(OnboardingPreview.kt LookPreview -> ExpressiveLookPreview). Expressive's scheme is the phone's "
                         f"wallpaper scheme; these are the render harness's values (colors/expressive-base.json).",
             android=f"MaterialTheme.colorScheme.{role}")

    d.mode_collections = MODE_COLLECTIONS
    return ids


def _type_styles(d: Doc):
    for name, size, line, w, tr, tab, code, where in SCALE:
        text_style(d, name, size, line, w, tr, tab, description=f"{code}: onest({size:g}f, {line:g}f, {w}"
                   + (f", tracking = {tr:g}f" if tr else "") + ")" + (".tabular()" if tab else "") + f". {where}")
    for name, size, line, w, tr, tab, src in COMPONENT_TYPE + OVERLAY_TYPE + RECAP_TYPE:
        text_style(d, name, size, line, w, tr, tab, description=src + ".")
    for name, size, line, w, tr, tab, src in OTHER_TYPE:
        text_style(d, name, size, line, w, tr, tab, description=src + ".",
                   family="Google Sans Flex" if name.startswith("Preview/") else "Onest")
    for name, size, line, w, tr, tab, src in SYSTEM_TYPE:
        text_style(d, name, size, line, w, tr, tab, family="Google Sans Flex",
                   description=src + ": Google Sans Flex stands in for the system font. Not Attic's type.")


def _effect_styles(d: Doc, ids: dict):
    fl = d.effect_style("Shadow/Floating", [{"type": "dropShadow", "color": "#000000B3", "offset": [0, 12], "radius": 36}],
                        description="Groups.kt Modifier.floatingShadow(shape): dock, sheet, dialog, menu, action bar. Dark: blur 36, "
                                    "y 12, Color/shadow (black 70 %); light: blur 28, y 8, #3C2D14 at 14 %. Add a 1 px inside "
                                    "stroke in Color/edge (the code's border(1.dp, colors.edge)). Cards never take a shadow.")
    sm = d.effect_style("Shadow/Small", [{"type": "dropShadow", "color": "#00000073", "offset": [0, 4], "radius": 14}],
                        description="Groups.kt floatingShadow(small = true): a lifted (dragged) row, the toast, the switch knob, the "
                                    "queue's Clear pill. Dark: blur 14, y 4, black 45 %; light: blur 12, y 3, #3C2D14 at 16 %. No edge "
                                    "ring except where the code asks for it (the Clear pill).")
    for sid, col, blur, y in ((fl, "Color/shadow", "Elevation/shadow/float-blur", "Elevation/shadow/float-y"),
                              (sm, "Color/shadow-small", "Elevation/shadow/small-blur", "Elevation/shadow/small-y")):
        d.cmd("style.bind", style=sid, field="effects/0/color", variable=ids[col])
        d.cmd("style.bind", style=sid, field="effects/0/radius", variable=ids[blur])
        d.cmd("style.bind", style=sid, field="effects/0/offsetY", variable=ids[y])


# ------------------------------------------------------------------------ Colour page
def _swatch(d: Doc, parent, var_id: str, *, w=56, h=40, name="Swatch", radius=10):
    r = d.rect(parent, name=name, w=w, h=h, fill={"$var": var_id}, radius=radius,
               stroke="Canvas/divider", stroke_w=1, strokeAlign="inside")
    return r


def _colour_page(d: Doc, ctx: dict, ids: dict):
    pal, tint = ctx["palette"], ctx["tinted"]
    s = Sheet(d, "Colour", "Colour",
              "Comfort has one fixed palette in two modes, a warm amber accent called Lamplight, and Tinted, which "
              "takes the accent and, washed, the surfaces from the album art. Components and screens bind only to "
              "Color/*, whose modes pick the source: Base (Palette), Tinted, or Tinted wash. Light and Dark are modes "
              "of Palette, Tinted and Elevation. Source: ui/comfort/ComfortColors.kt, Tinted.kt, Oklch.kt.", intro_w=1700)

    # -- 1. Palette: every token, Light and Dark
    c = s.card("Palette", 1500)
    s.heading(c, "Palette: ComfortLight and ComfortDark")
    s.body(c, "Every token the code reads, by its Kotlin name in kebab case. Code syntax is ComfortColors.<name>; "
              "components read them through Color/<name>.", w=1380, muted=True)
    head = s.row(c, "Header row", gap=24)
    for txt, w in (("Token", 240), ("What it is for", 520), ("Light", 260), ("Dark", 260)):
        s.label(head, txt, name=f"Column: {txt}", style="Doc/Caption", w=w, muted=True)
    for kotlin, name, what in TOKENS + [(None, k, v[2]) for k, v in ERROR_EXTRA.items()] + [(None, "ground-toggle", "Container of a tonal toggle on a collection's ground: card in light, fill in dark (alias per mode)."),
                                                                                            (None, "system/ink", "Render harness status bar ink."), (None, "system/gesture", "Render harness gesture handle.")]:
        vname = f"lamplight/{name}" if kotlin in LAMPLIGHT else name
        vid = ids[f"Palette/{vname}"]
        r = s.row(c, f"Token: {vname}", gap=24)
        s.label(r, vname, name="Name", w=240)
        s.label(r, (what + (f"  ComfortColors.{kotlin}" if kotlin else "")), name="Use", style="Doc/Caption", w=520, muted=True)
        for theme in ("Light", "Dark"):
            cell = s.row(r, f"{theme} value", gap=12)
            d.set(cell, width=260, sizingH="fixed")
            _swatch(d, cell, vid)
            if kotlin:
                hx = hexa(pal[theme.lower()][kotlin])
            elif name in ERROR_EXTRA:
                hx = ERROR_EXTRA[name][0 if theme == "Light" else 1]
            elif name == "ground-toggle":
                hx = "= card" if theme == "Light" else "= fill"
            elif name == "system/ink":
                hx = "#1B1B1F" if theme == "Light" else "#FFFFFF"
            else:
                hx = "ink at 85 %"
            s.label(cell, hx, name="Hex", style="Doc/Code")
            _modes(d, cell, theme)
    s.done(c)

    # -- 2. Lamplight
    c = s.card("Lamplight", 900)
    s.heading(c, "Lamplight: the accent")
    s.body(c, "One accent, amber, for fills, the one filled button on a screen, switches, progress and selection. "
              "In light, amber as text or an icon is accent-ink (Brass), never accent. Titles and row text stay in on. "
              "Inside Tinted the cover's colour replaces all five.", w=800, muted=True)
    names = {"accent": "Lamplight", "onAccent": "Soot", "accentInk": "Brass (light)", "accentSoft": "Tonal container", "onAccentSoft": "Ink on tonal"}
    for theme in ("Light", "Dark"):
        r = s.row(c, f"Lamplight · {theme}", gap=16, cross="min")
        _modes(d, r, theme)
        for kotlin in ("accent", "onAccent", "accentInk", "accentSoft", "onAccentSoft"):
            name = dict((a, b) for a, b, _ in TOKENS)[kotlin]
            col = s.col(r, f"Chip: {name}", gap=6)
            _swatch(d, col, ids[f"Palette/lamplight/{name}"], w=148, h=96, radius=18, name="Swatch")
            s.label(col, names[kotlin], name="Kit name", style="Doc/Label")
            s.label(col, f"lamplight/{name}", name="Variable", style="Doc/Caption", muted=True)
            s.label(col, hexa(pal[theme.lower()][kotlin]) + f" · {theme}", name="Hex", style="Doc/Code", muted=True)
    s.done(c)
    s.newrow()

    # -- 3. Tinted
    c = s.card("Tinted", 2560)
    s.heading(c, "Tinted(seed, wash): colour from the album art")
    s.body(c, "Each moved token keeps the seed's OKLCH hue, scales its chroma by k and sets its own lightness L: "
              "oklch(L, seed.c x k, seed.h). The seed's lightness is ignored, so contrast is the same for a pale cover and a dark one. "
              "Tinted moves the accent family and the mini player's fills; wash also moves the surfaces and the two quieter inks. "
              "on never changes. FigCraft can't express this rule, so Tinted/* stores the values for the example seed and each "
              "description quotes the rule; Color's modes pick them up by alias. Recompute them for another seed with Oklch.kt.",
           w=2380, muted=True)
    top = s.row(c, "Seed", gap=24, cross="center")
    d.image(top, str(EXAMPLE_COVER), name="Example cover: Paper Lanterns", w=120, h=120, radius=24)
    _swatch(d, top, ids["Tinted/seed"], w=120, h=120, radius=24, name="Seed swatch")
    L, C, H = oklch_from_hex(EXAMPLE_SEED)
    s.label(top, f"seed = {EXAMPLE_SEED}   (Paper Lanterns, the now-playing album in every render)\n"
                 f"OKLCH L {L:.4f}  C {C:.4f}  h {H:.2f}\n"
                 f"rememberArtColor(mediaId, Color.Unspecified, deepen = false): Palette's vibrant swatch", style="Doc/Code", w=900)
    tables = s.row(c, "Rule tables", gap=48, cross="min")
    for title, keys, lt, dt in (("Tinted (.tinted)", TINTED_TOKENS, LIGHT_TINTED, DARK_TINTED),
                                ("Wash (.tinted.wash, on top of Tinted)", WASH_TOKENS, LIGHT_WASH, DARK_WASH)):
        tcol = s.col(tables, f"Table: {title}", gap=10)
        s.label(tcol, title, name="Table title", style="Doc/Heading")
        s.label(tcol, ("Tinted.kt ComfortTint.darkTinted / lightTinted: `\"accent\" to Spec(0.76, 1.0)` = oklch(L, seed.c x k, seed.h). "
                       "Light onAccent is Color.White." if title.startswith("Tinted") else
                       "Tinted.kt ComfortTint.darkWash / lightWash, added on top of the Tinted table when wash = true."),
                style="Doc/Code", muted=True, w=900, name="Source")
        hr = s.row(tcol, "Header", gap=16)
        for txt, w in (("Token", 170), ("Dark L, k", 110), ("Dark", 200), ("Light L, k", 110), ("Light", 200)):
            s.label(hr, txt, name=f"Column: {txt}", style="Doc/Caption", w=w, muted=True)
        for k in keys:
            name = dict((a, b) for a, b, _ in TOKENS)[k]
            group = "tinted" if title.startswith("Tinted") else "wash"
            vid = ids[f"Tinted/{group}/{name}"]
            r = s.row(tcol, f"Rule: {name}", gap=16)
            s.label(r, name, name="Token", w=170)
            s.label(r, f"{dt[k][0]}, {dt[k][1]}", name="Dark rule", style="Doc/Code", w=110)
            cell = s.row(r, "Dark value", gap=10)
            d.set(cell, width=200, sizingH="fixed")
            _swatch(d, cell, vid, w=44, h=32)
            s.label(cell, tint["dark"][k], name="Hex", style="Doc/Code")
            _modes(d, cell, "Dark")
            s.label(r, f"{lt[k][0]}, {lt[k][1]}" if k in lt else "white", name="Light rule", style="Doc/Code", w=110)
            cell = s.row(r, "Light value", gap=10)
            d.set(cell, width=200, sizingH="fixed")
            _swatch(d, cell, vid, w=44, h=32)
            s.label(cell, tint["light"][k], name="Hex", style="Doc/Code")
            _modes(d, cell, "Light")
    # other covers: static documentation of the same rule, values from comfort-tinted-<i>.json (raw, as computed by the code)
    s.label(c, "The same rule for other covers (stored values from the code, comfort-tinted-<i>.json; documentation only, not variables)",
            style="Doc/Heading", name="Heading: other covers")
    others = s.col(c, "Other covers", gap=14)
    keys = TINTED_TOKENS + WASH_TOKENS
    hr = s.row(others, "Header", gap=8)
    s.label(hr, "Cover, seed, mode", style="Doc/Caption", w=330, muted=True, name="Column: cover")
    for k_ in keys:
        s.label(hr, dict((a, b) for a, b, _ in TOKENS)[k_], style="Doc/Caption", w=110, muted=True, name=f"Column: {k_}")
    for idx in (3, 6, 8, 9):
        ref = COLORS_DIR / f"comfort-tinted-{idx}.json"
        if not ref.exists():
            continue
        j = json.loads(ref.read_text())
        for theme in ("dark", "light"):
            r = s.row(others, f"Cover {idx} · {theme}", gap=8)
            head = s.row(r, "Cover", gap=12)
            d.set(head, width=330, sizingH="fixed")
            cover = next(COVERS_DIR.glob(f"{idx}-*.jpg"))
            d.image(head, str(cover), name=f"Cover: {j['album']}", w=48, h=48, radius=10)
            d.rect(head, name="Seed", w=24, h=24, fill=j["seed"]["hex"][:7], radius=6, allow_hex=True)
            s.label(head, f"{j['album']}\n{j['seed']['hex'][:7]} · {theme}", style="Doc/Caption", w=200, name="Album")
            toks = j[theme]["tintedWash"]["tokens"]
            for k_ in keys:
                cell = s.col(r, f"Value: {k_}", gap=2)
                d.set(cell, width=110, sizingH="fixed")
                d.rect(cell, name="Swatch", w=110, h=28, fill=toks[k_][:7], radius=6, allow_hex=True,
                       stroke="Canvas/divider", stroke_w=1, strokeAlign="inside")
                s.label(cell, toks[k_][:7], style="Doc/Code", muted=True, name="Hex")

    # live demo: the same frame in each Color mode
    s.label(c, "The same component frame in each Color mode (bound to Color/*; the mode on the frame is the only difference)", style="Doc/Heading")
    demo_row = s.row(c, "Color modes", gap=32, cross="min")
    for theme in ("Light", "Dark"):
        for mode in ("Base", "Tinted", "Tinted wash"):
            f = d.frame(demo_row, name=f"{mode} · {theme}", w=360, layout="v", gap=14, pad=20, fill="Color/ground",
                        radius=26, sizing=("fixed", "hug"))
            _modes(d, f, theme, mode)
            d.text(f, f"{mode} · {theme}", style="Scale/Label", color="Color/on-2", name="Mode name")
            card = d.frame(f, name="Card", layout="v", gap=10, pad=16, fill="Color/card", radius=18, sizing=("fill", "hug"))
            d.text(card, "Lights Over the Water", style="Scale/Row title", color="Color/on", name="Title")
            d.text(card, "Mira Okafor", style="Scale/Sub", color="Color/on-2", name="Subtitle")
            pills = d.frame(card, name="Pills", layout="h", gap=8, sizing=("hug", "hug"))
            p1 = d.frame(pills, name="Filled button", layout="h", pad=[0, 16, 0, 16], h=36, fill="Color/accent", radius=999,
                         cross="center", sizing=("hug", "fixed"))
            d.text(p1, "Play", style="Component/Button small", color="Color/on-accent", name="Label")
            p2 = d.frame(pills, name="Tonal button", layout="h", pad=[0, 16, 0, 16], h=36, fill="Color/accent-soft", radius=999,
                         cross="center", sizing=("hug", "fixed"))
            d.text(p2, "Shuffle", style="Component/Button small", color="Color/on-accent-soft", name="Label")
            mini = d.frame(f, name="Mini player fill", layout="h", h=40, fill="Color/mini-base", radius=12, sizing=("fill", "fixed"), clip=True)
            d.rect(mini, name="Played", w=110, h=40, fill="Color/mini-fill")
            d.text(f, "accent-ink sample", style="Scale/Sub", color="Color/accent-ink", name="Accent ink")
    s.done(c)
    s.newrow()

    # -- 4. Color roles
    c = s.card("Color roles", 1900)
    s.heading(c, "Color: the roles components bind")
    s.body(c, "Each role is an alias whose target depends on the Color mode. Light and Dark come from the Palette and "
              "Tinted modes on the same frame. Code syntax is Comfort.colors.<name>, the value in effect inside Tinted.", w=1800, muted=True)
    hr = s.row(c, "Header row", gap=16)
    s.label(hr, "Role", style="Doc/Caption", w=200, muted=True, name="Column: Role")
    s.label(hr, "Source", style="Doc/Caption", w=420, muted=True, name="Column: Source")
    for theme in ("Light", "Dark"):
        for mode in ("Base", "Tinted", "Tinted wash"):
            s.label(hr, f"{mode}\n{theme}", style="Doc/Caption", w=150, muted=True, name=f"Column: {mode} {theme}")
    roles = [(name, kotlin) for kotlin, name, _ in TOKENS] + [("derived/lyric-sung", None), ("derived/seek-tick", None),
                                                              ("derived/ground-clear", None), ("derived/art-dim", None)]
    for name, kotlin in roles:
        r = s.row(c, f"Role: {name}", gap=16)
        s.label(r, name, name="Role", w=200)
        if kotlin is None:
            src = {"derived/lyric-sung": "mix(on-2, on-3, 0.72)", "derived/seek-tick": "on-accent at 40 %",
                   "derived/ground-clear": "ground at 0 %", "derived/art-dim": "black at 60 %"}[name]
        else:
            pname = f"Palette/lamplight/{name}" if kotlin in LAMPLIGHT else f"Palette/{name}"
            src = pname + (" | Tinted/tinted/" + name if kotlin in TINTED_TOKENS else "") + (" | Tinted/wash/" + name if kotlin in WASH_TOKENS else "")
        s.label(r, src, name="Source", style="Doc/Code", w=420, muted=True)
        for theme in ("Light", "Dark"):
            for mode in ("Base", "Tinted", "Tinted wash"):
                cell = d.frame(r, name=f"{mode} · {theme}", w=150, h=40, layout="h", sizing=("fixed", "fixed"))
                _swatch(d, cell, ids[f"Color/{name}"], w=150, h=40, radius=8)
                _modes(d, cell, theme, mode)
    s.body(c, "Also in Color: material/* (every MaterialTheme.colorScheme role toColorScheme() sets, aliasing the roles above, "
              "for stock Material components) and system/ink, system/gesture (the render harness's status and gesture bars).",
           w=1800, muted=True)
    s.done(c)

    c = s.card("Where Tinted applies", 760)
    s.heading(c, "Where Tinted applies")
    s.body(c, "Set the Color mode on the frame, as the code wraps the composable.\n\n"
              "Tinted wash: Now Playing (ComfortNowPlaying.kt), Queue (ComfortQueue.kt), full-screen Lyrics (ComfortLyrics.kt), "
              "sheets opened over the player (Sheets.kt), Recap cards with a cover (ComfortRecapCards.kt), a Browse tile "
              "given a seed (Tiles.kt).\n\n"
              "Tinted: the mini player in the dock (Dock.kt MiniPlayer tints itself), one Recap card.\n\n"
              "Base: everything else, Home, Search, Library, collections, Settings. Amber does not appear inside a tinted frame.\n\n"
              "A song with no art (seed unspecified) keeps the palette around it. When the seed changes every token tweens over 450 ms.",
           w=660)
    s.done(c)
    s.newrow()
    _extras_card(d, s, ids, pal)


def _value_rows(d: Doc, s: "Sheet", c, rows, themes=("Light", "Dark")):
    """Rows of (variable path, what it is, {theme: hex}) with a swatch per theme."""
    hr = s.row(c, "Header row", gap=24)
    for txt, w in (("Variable", 340), ("What it is for", 560)) + tuple((t, 260) for t in themes):
        s.label(hr, txt, name=f"Column: {txt}", style="Doc/Caption", w=w, muted=True)
    for path, what, hexes in rows:
        r = s.row(c, f"Token: {path}", gap=24)
        s.label(r, path, name="Name", w=340)
        s.label(r, what, name="Use", style="Doc/Caption", w=560, muted=True)
        for theme in themes:
            cell = s.row(r, f"{theme} value", gap=12)
            d.set(cell, width=260, sizingH="fixed")
            _swatch(d, cell, d.var_id(path))
            s.label(cell, hexes[theme], name="Hex", style="Doc/Code")
            if theme in ("Light", "Dark"):
                _modes(d, cell, theme)


def _extras_card(d: Doc, s: "Sheet", ids: dict, pal: dict):
    """Tokens for one screen or surface: the keyboard stand-in, Recap's language steps, the launcher icon's colours and
    the Expressive look card's palette."""
    c = s.card("Screen and surface tokens", 1900)
    s.heading(c, "Tokens for one screen or surface")
    s.body(c, "Palette and Color hold a few tokens that only one screen reads, and two small collections hold colours that "
              "aren't Comfort's: the launcher icon's (Brand) and the Expressive style's, for its look card (Expressive "
              "preview). Bind screens to the Color aliases, as everywhere else.", w=1800, muted=True)
    kb = {n: (lv, dv, what) for n, lv, dv, what in KEYBOARD}
    rows = [(f"Color/{n}", f"Render harness keyboard stand-in: {what}. Attic draws no keyboard (DesignHarness.kt).",
             {"Light": lv, "Dark": dv}) for n, (lv, dv, what) in kb.items()]
    for n, drop in LANGUAGE_STEPS:
        vals = {}
        for theme, mode in (("light", "Light"), ("dark", "Dark")):
            l_, c_, h_ = oklch_from_hex(pal[theme]["accent"][:7])
            vals[mode] = oklch_to_hex(max(0.0, l_ - drop), c_, h_)
        rows.append((f"Color/derived/language-{n}", f"Recap's language step {n}: the accent at OKLCH lightness -{drop} "
                                                    f"(ComfortRecapCards.kt languageSteps).", vals))
    s.label(c, "Palette and Color", style="Doc/Heading", name="Heading: palette extras")
    _value_rows(d, s, c, rows)
    s.label(c, "Brand (one mode): the launcher icon, res/values/colors.xml", style="Doc/Heading", name="Heading: Brand")
    _value_rows(d, s, c, [(f"Brand/{n}", what, {"Value": hx}) for n, hx, what in BRAND], themes=("Value",))
    j = json.loads((COLORS_DIR / "expressive-base.json").read_text())
    s.label(c, "Expressive preview (Light, Dark): the Expressive look card's Material roles", style="Doc/Heading",
            name="Heading: Expressive preview")
    _value_rows(d, s, c, [(f"Expressive preview/{_kebab(r)}", f"MaterialTheme.colorScheme.{r} in Expressive (render harness values)",
                           {"Light": j["light"][r][:7], "Dark": j["dark"][r][:7]}) for r in EXPRESSIVE_ROLES])
    s.done(c)


# ------------------------------------------------------------------------ Type page
def _type_page(d: Doc):
    s = Sheet(d, "Type", "Type",
              "Onest, bundled as res/font/onest.ttf (OFL 1.1, weights 400 to 800, Latin). Each style pins its weight, so "
              "600 is a true semibold. Sizes and line heights are sp; tracking is the kit's em value. Styles centre text in "
              "the line box as CSS does. Scale/* is ComfortTypography (Comfort.type); Component/* are the onest(...) calls "
              "components make themselves; Recap/* the Recap cards' own; Widget/* the home-screen widget's XML styles. "
              "Preview/* and System/* are Google Sans Flex: Expressive's look card, and Android's chrome on System "
              "surfaces. Every style is made here, once. Source: ui/comfort/ComfortType.kt.", intro_w=1700)
    c = s.card("Scale", 2000)
    s.heading(c, "Scale: Comfort.type")
    for name, size, line, w, tr, tab, code, where in SCALE:
        r = s.row(c, f"Specimen: {name}", gap=32, cross="center")
        meta = s.col(r, "Meta", gap=4)
        d.set(meta, width=520, sizingH="fixed")
        s.label(meta, name, name="Style name")
        s.label(meta, f"{size:g}/{line:g} · {WEIGHT_NAME[w]} {w}" + (f" · {tr * 100:g} %" if tr else "") + (" · tabular" if tab else ""),
                name="Metrics", style="Doc/Code", muted=True)
        s.label(meta, code, name="Code", style="Doc/Code", muted=True)
        sample = {"Scale/Hero": "Home", "Scale/Stat": "18,420", "Scale/Lyric": "carry what we couldn't say"}.get(name, "Lights Over the Water")
        cell = d.frame(r, name="Sample cell", w=760, layout="h", cross="center", sizing=("fixed", "hug"))
        d.text(cell, sample, style=name, color="Color/on", name="Sample")
        s.label(r, where, name="Where", style="Doc/Caption", muted=True, w=560)
    s.done(c)
    _metrics_card(d, s)
    s.newrow()
    _specimens(d, s, "Component styles", "Component styles: onest(...) inside the kit", COMPONENT_TYPE + OVERLAY_TYPE)
    _specimens(d, s, "Recap styles", "Recap/*: the Recap cards' own type", RECAP_TYPE,
               note="The cards are drawn at 360 x 640 and scaled as a whole, so these sizes are the card's own, before scaling.")
    s.newrow()
    _specimens(d, s, "Widget and preview styles", "Widget/* and Preview/*", OTHER_TYPE,
               note="Widget/* are the home-screen widget's XML text appearances (RemoteViews draws them, not Compose). "
                    "Preview/* is Expressive's type in its look card, Google Sans Flex, drawn at full size and scaled to 0.42.")
    _specimens(d, s, "System stand-ins", "System/*: Android's chrome, not Attic's", SYSTEM_TYPE,
               note="Only on System surfaces, for what Android draws (lock screen clock, notification text, launcher "
                    "labels). Google Sans Flex stands in for the phone's system font.")


TEXT_METRICS_NOTE = (
    "The styles hold the code's values; two things make Android's text land slightly differently from FigCraft's, "
    "and the file doesn't compensate for either.\n\n"
    "Line height. Compose rounds each line up to whole pixels (LineHeightStyle's span takes ceil of the line height), "
    "so at the renders' 2.625 px/dp a 31sp line takes 82 px, 31.24dp, and a 22sp line 22.1dp. A paragraph of 20 lines "
    "ends about 4dp lower on the phone than in the file. Tried here: line heights set to the rounded values change "
    "nothing in FigCraft, which lays text out on whole line heights, and the compare scores of all 98 screens stayed "
    "the same (mean change 0.00).\n\n"
    "Width. FigCraft draws Onest 1 to 2.5 % wider than the renders (a 400dp lyric line runs about 5dp longer), so "
    "hugging parts (sub-tabs, chips) come out 1 to 4dp wider when their label changes, and a long line can wrap a "
    "word earlier. Matching it would mean tracking the code doesn't have, so the styles keep the kit's tracking.\n\n"
    "Icons are the exception: the Icon/* glyphs are drawn 2.6dp below centre, as Compose places them (see Icons).")


def _metrics_card(d: Doc, s: "Sheet"):
    c = s.card("Known difference: text metrics", 1100)
    s.heading(c, "Known difference: text metrics")
    s.body(c, TEXT_METRICS_NOTE, w=1000)
    s.done(c)
    return c


def _specimens(d: Doc, s: "Sheet", card: str, heading: str, styles, note: str | None = None):
    c = s.card(card, 2000)
    s.heading(c, heading)
    if note:
        s.body(c, note, w=1900, muted=True)
    for name, size, line, w, tr, tab, src in styles:
        r = s.row(c, f"Specimen: {name}", gap=32, cross="center")
        meta = s.col(r, "Meta", gap=2)
        d.set(meta, width=520, sizingH="fixed")
        s.label(meta, name, name="Style name")
        fam = "Google Sans Flex" if name.startswith(("Preview/", "System/")) else "Onest"
        s.label(meta, f"{size:g}/{line:g} · {WEIGHT_NAME[w]} {w}" + (f" · {tr * 100:g} %" if tr else "") + (" · tabular" if tab else "")
                + ("" if fam == "Onest" else f" · {fam}"), name="Metrics", style="Doc/Code", muted=True)
        sample = ("1:18  3:52" if tab else "10:24" if name in ("Component/Status time", "System/Clock large")
                  else "18,420" if name in ("Recap/Big figure",) else "41%" if name == "Recap/Percent large" else "Paper Lanterns")
        cell = d.frame(r, name="Sample cell", w=560 if size > 60 else 420, layout="h", cross="center", sizing=("fixed", "hug"))
        d.text(cell, sample, style=name, color="Color/on", name="Sample")
        s.label(r, src, name="Source", style="Doc/Caption", muted=True, w=900)
    s.done(c)
    return c


# ------------------------------------------------------------------------ Spacing, radii and shapes
def _shape_page(d: Doc, ctx: dict, ids: dict):
    s = Sheet(d, "Spacing, radii and shapes", "Spacing, radii and shapes",
              "Every image and icon container is a superellipse |x|^n + |y|^n = 1 stretched to its box: never a rounded "
              "rectangle, never a circle. Rectangles take three radii (float 30, group 26, control 18) plus the menu's 22; "
              "buttons and chips are pills; the only circles are the radio, the switch knob and the spinner. "
              "Source: ui/comfort/ComfortShapes.kt, Groups.kt.", intro_w=1700)
    # superellipse components
    c = s.card("Superellipses", 1500)
    s.heading(c, "Superellipses (main components)")
    s.body(c, "Traced with SuperellipseShape's own maths: x = 0.5 + 0.5 sign(cos t)|cos t|^(2/n), y likewise with sin, at "
              "t = 2 pi i / 720, then fitted with cubic Béziers that stay within 0.1 % of that outline (8 to 24 curves, "
              "so the path stays editable). Covers below 100dp take n = 4, from 100dp n = 5 (ArtShape); artists take n = 2.6 at every size.",
           w=1400, muted=True)
    r = s.row(c, "Shapes", gap=48, cross="min")
    for name, n, desc in SHAPES:
        col = s.col(r, f"Cell: {name}", gap=10)
        comp = d.component(col, name=name, w=100, h=100, description=desc)
        d.vector(comp, name="Outline", path=superellipse_path(n), fill="Color/fill", w=100, h=100,
                 constraints={"horizontal": "scale", "vertical": "scale"})
        d.register(name, comp)
        s.label(col, name, name="Name")
        s.label(col, f"n = {n:g}", name="Exponent", style="Doc/Code", muted=True)
    s.done(c)

    c = s.card("Against corner smoothing", 900)
    s.heading(c, "Against a smoothed rounded rectangle")
    s.body(c, "Corner smoothing gets close: rasterised at 400 px, the best smoothed rectangle differs from the n = 4 "
              "outline in 0.22 % of its area. The file still uses the vectors, so a cover's outline is the code's own "
              "curve at any size and nobody has to remember a radius per size.", w=800, muted=True)
    ov = d.frame(c, name="Overlay: n = 4 vs smoothed rectangle", w=320, h=320, fill="Canvas/section", radius=16)
    near = SMOOTH_MATCH
    rr = d.rect(ov, name="Smoothed rectangle", x=40, y=40, w=240, h=240, fill="Canvas/accent",
                radius=near["radius"] * 2.4, smoothing=near["smoothing"], opacity=0.35)
    d.vector(ov, name="Superellipse n = 4", path=superellipse_path(4, 240, 240), x=40, y=40, w=240, h=240,
             stroke="Color/accent", stroke_w=2)
    s.label(c, f"Best match for n = 4: radius {near['radius']:g} % of the side with smoothing {near['smoothing']:g}: "
               f"{near['mismatch']:.2f} % of the area differs. n = 5: radius {SMOOTH_MATCH_5['radius']:g} %, smoothing "
               f"{SMOOTH_MATCH_5['smoothing']:g}, {SMOOTH_MATCH_5['mismatch']:.2f} %. n = 2.6: radius {SMOOTH_MATCH_SOFT['radius']:g} %, "
               f"smoothing {SMOOTH_MATCH_SOFT['smoothing']:g}, {SMOOTH_MATCH_SOFT['mismatch']:.2f} %.", style="Doc/Caption", w=800, muted=True)
    s.done(c)
    s.newrow()

    c = s.card("Radii", 1800)
    s.heading(c, "Radii (Dimension/radius/*)")
    r = s.row(c, "Radius samples", gap=40, cross="min")
    for (name, v, code, what), comp_name in zip(RADII, ["Shape/Floating", "Shape/Group", "Shape/Control", "Shape/Menu", None, "Shape/Pill"]):
        col = s.col(r, f"Cell: {name}", gap=8)
        if comp_name:
            comp = d.component(col, name=comp_name, w=160, h=100 if comp_name != "Shape/Pill" else 48,
                               description=f"{code}: {what}", fill="Color/fill")
            d.bind(comp, "cornerRadius", ids[f"Dimension/{name}"])
            d.register(comp_name, comp)
        else:
            rr = d.rect(col, name="Sample", w=160, h=100, fill="Color/fill")
            d.bind(rr, "cornerRadius", ids[f"Dimension/{name}"])
        s.label(col, f"{name} · {v}", name="Name")
        s.label(col, code, name="Code", style="Doc/Code", muted=True, w=200)
    col = s.col(r, "Cell: circle", gap=8)
    comp = d.component(col, name="Shape/Circle", w=48, h=48, fill="Color/fill", radius=24,
                       description="ComfortShapes.Circle: the radio, the switch knob and the spinner. Nothing else.")
    d.register("Shape/Circle", comp)
    s.label(col, "Circle", name="Name")
    s.done(c)
    s.newrow()

    c = s.card("Spacing", 1500)
    s.heading(c, "Spacing (Dimension/space/*)")
    s.body(c, "The values the kit's paddings and gaps use, and the named roles (aliases of the scale).", w=1400, muted=True)
    for v in SPACE_SCALE:
        r = s.row(c, f"Space {v}", gap=24)
        s.label(r, f"space/{v}", name="Name", w=200)
        bar = d.rect(r, name="Bar", w=v, h=16, fill="Canvas/accent", radius=3)
        d.bind(bar, "width", ids[f"Dimension/space/{v}"])
    s.label(c, "Roles", style="Doc/Heading")
    for name, v, code, what in SPACE_ROLES:
        r = s.row(c, f"Role {name}", gap=24)
        s.label(r, name, name="Name", w=300)
        bar = d.rect(r, name="Bar", w=v, h=16, fill="Canvas/accent", radius=3)
        d.bind(bar, "width", ids[f"Dimension/{name}"])
        s.label(r, f"{v}dp · {what}", name="Use", style="Doc/Caption", muted=True, w=900)
    s.done(c)

    c = s.card("Sizes", 900)
    s.heading(c, "Sizes (Dimension/size/*)")
    for name, v, code in SIZES:
        r = s.row(c, f"Size {name}", gap=16)
        s.label(r, name, name="Name", w=280)
        s.label(r, f"{v}", name="Value", style="Doc/Code", w=60)
        s.label(r, code, name="Code", style="Doc/Caption", muted=True, w=420)
    s.done(c)


# Measured once in FigCraft (2026-11-23): each superellipse and a grid of rounded rectangles (radius 12 to 50 % of
# the side in steps of 2, cornerSmoothing 0 to 1) rasterised at 400 px; the pair with the fewest differing pixels,
# as a share of the superellipse's area.
SMOOTH_MATCH = {"radius": 28, "smoothing": 0.8, "mismatch": 0.22}
SMOOTH_MATCH_5 = {"radius": 22, "smoothing": 1.0, "mismatch": 0.07}
SMOOTH_MATCH_SOFT = {"radius": 40, "smoothing": 0.6, "mismatch": 0.94}


# ------------------------------------------------------------------------ Elevation and motion
def _elevation_page(d: Doc):
    s = Sheet(d, "Elevation and motion", "Elevation and motion",
              "Only floating pieces cast a shadow: the dock, sheets, dialogs, menus and the action bar, each with a 1dp "
              "edge ring. Groups, cards and the deck never do. Motion is short tweens and critically damped springs; "
              "nothing overshoots. Sources: ui/comfort/Groups.kt floatingShadow, ComfortShell.kt, Controls.kt, Tinted.kt.",
              intro_w=1700)
    c = s.card("Shadows", 1500)
    s.heading(c, "Effect styles")
    r = s.row(c, "Shadow samples", gap=48, cross="min")
    for theme in ("Light", "Dark"):
        f = d.frame(r, name=f"Shadows · {theme}", w=660, h=420, fill="Color/ground", radius=26, layout="h", gap=48,
                    pad=60, cross="center", sizing=("fixed", "fixed"))
        _modes(d, f, theme)
        a = d.frame(f, name="Floating", w=260, h=180, fill="Color/raised", radius=30, stroke="Color/edge", stroke_w=1,
                    strokeAlign="inside", layout="v", pad=20, sizing=("fixed", "fixed"))
        d.cmd("style.apply", ids=[a], style=d.effect_styles["Shadow/Floating"])
        d.text(a, "Shadow/Floating\n+ edge stroke", style="Scale/Label", color="Color/on", name="Label")
        b = d.frame(f, name="Small", w=200, h=72, fill="Color/raised", radius=18, layout="v", pad=16, sizing=("fixed", "fixed"))
        d.cmd("style.apply", ids=[b], style=d.effect_styles["Shadow/Small"])
        d.text(b, "Shadow/Small", style="Scale/Label", color="Color/on", name="Label")
    s.body(c, "Shadow/Floating: Light blur 28, y 8, #3C2D14 at 14 %; Dark blur 36, y 12, black at 70 %. Shadow/Small: "
              "Light blur 12, y 3, #3C2D14 at 16 %; Dark blur 14, y 4, black at 45 %. Colour, blur and y are bound to "
              "Color/shadow*, Elevation/shadow/*, so the styles switch with the mode. Compose draws these with dropShadow "
              "and the CSS values; Android's blur is close to, not identical with, a browser's.", w=1400, muted=True)
    s.done(c)
    s.newrow()
    c = s.card("Motion", 2200)
    s.heading(c, "Motion spec (Motion/*)")
    hr = s.row(c, "Header", gap=24)
    for txt, w in (("Variable", 340), ("Value", 100), ("What moves", 760), ("Source", 900)):
        s.label(hr, txt, style="Doc/Caption", w=w, muted=True, name=f"Column: {txt}")
    for name, v, what, src in MOTION:
        r = s.row(c, f"Motion: {name}", gap=24)
        s.label(r, name, name="Variable", w=340)
        s.label(r, f"{v} ms" if name.startswith("duration") else f"{v}", name="Value", style="Doc/Code", w=100)
        s.label(r, what, name="What", style="Doc/Caption", w=760)
        s.label(r, src, name="Source", style="Doc/Code", w=900, muted=True)
    for what, spec, src in MOTION_EXTRA:
        r = s.row(c, f"Motion: {what}", gap=24)
        s.label(r, what, name="Variable", w=340)
        s.label(r, "", name="Value", style="Doc/Code", w=100)
        s.label(r, spec, name="What", style="Doc/Caption", w=760)
        s.label(r, src, name="Source", style="Doc/Code", w=900, muted=True)
    s.done(c)


# ------------------------------------------------------------------------ Icons
def _enum(lig: str) -> str:
    return "".join(p.capitalize() for p in lig.split("_"))


def _round(path: str) -> str:
    import re
    return re.sub(r"-?\d+\.\d+", lambda m: f"{float(m.group()):.3f}".rstrip("0").rstrip("."), path)


def build_comfort_icons(d: Doc, parent_for, *, color="Color/on") -> dict:
    """Like Doc.build_icons, at Comfort's weight 300 (ComfortIconWeight), with filled variants for FILLED and
    weight-400 filled variants for WEIGHT_400. parent_for(name) gives the frame each icon goes in."""
    made = {}
    specs = [(lig, cp, 0, 300) for _, lig, cp in _icons.symbols()]
    specs += [(lig, cp, 1, 300) for _, lig, cp in _icons.symbols() if lig in FILLED]
    specs += [(lig, cp, 1, 400) for _, lig, cp in _icons.symbols() if lig in WEIGHT_400]
    for lig, cp, fill, wght in specs:
        nm = f"Icon/{lig}" + ("" if not fill else (" (filled)" if wght == 300 else " (filled, 400)"))
        parent = parent_for(nm)
        c = d.component(parent, name=nm, w=24, h=24,
                        description=f"Material Symbols Rounded `{lig}` (U+{cp:04X}), FILL {fill}, wght {wght}, opsz 24. "
                                    f"Compose: ComfortIcon(Symbol.{_enum(lig)}" + (", filled = true" if fill else "")
                                    + (", weight = 400" if wght == 400 else "") + ").")
        d.set(c, clip=False)
        g = d.vector(c, name="Glyph", path=_round(_icons.path(cp, fill=fill, wght=wght)), fill=color,
                     constraints={"horizontal": "scale", "vertical": "scale"})
        # Compose draws the glyph as text a little below the box's centre (SymbolIcon); measured 2.6dp at 24.
        d.set(g, y=d.get(g)["y"] + ICON_DROP)
        d.register(nm, c)
        made[nm] = c
    d.icon_names |= set(made)
    return made


def _icons_page(d: Doc):
    s = Sheet(d, "Icons", "Icons",
              "Material Symbols Rounded, the app's own subset font (res/font/material_symbols_rounded.ttf), at Comfort's "
              f"weight 300. Only the {len(_icons.symbols())} glyphs in ui/expressive/Symbols.kt exist. Filled is the selected tab, a favourite "
              "that is on, play and pause, and a tonal toggle that is on. The transport glyphs (play, pause, previous, next "
              "in PlayButton and SkipButton) are filled at weight 400. Instances take their colour from a fill override on "
              "Glyph; sizes in the kit are 18, 20, 24, 28, 30, 32, 42. Compose draws a symbol as text, which sits it 2.6dp "
              "below the 24dp box's centre (measured on the renders), so each Glyph is drawn that much low and may "
              "overflow the box's foot.", intro_w=1700)
    c = s.card("Icon set", 2200)
    s.heading(c, "Icon/<ligature>")
    grids = {}

    def grid(title):
        if title not in grids:
            s.label(c, title, style="Doc/Heading", name=f"Group: {title}")
            g = d.frame(c, name=f"Grid: {title}", layout="h", wrap=True, gap=12, crossGap=12, sizing=("fill", "hug"))
            grids[title] = g
        return grids[title]

    def parent_for(nm):
        title = ("Outlined, weight 300" if "(" not in nm else "Filled, weight 300" if nm.endswith("(filled)") else "Filled, weight 400 (transport)")
        g = grid(title)
        cell = d.frame(g, name=f"Cell: {nm[5:]}", w=164, layout="v", gap=10, pad=[16, 8, 12, 8], cross="center",
                       fill="Canvas/section", radius=12, sizing=("fixed", "hug"))
        d.text(cell, nm[5:], style="Doc/Caption", color="Canvas/label", name="Name", width=148, textAutoResize="height",
               textAlign="center")
        return cell

    made = build_comfort_icons(d, parent_for)
    # the icon sits above its name
    for nm, cid in made.items():
        d.cmd("node.reorder", id=cid, index=0)
    s.done(c)
