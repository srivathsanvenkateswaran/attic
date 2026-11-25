"""The equaliser in Attic Expressive: components, screens, prototype links and the handoff (1.1.0, then 1.2.0).

Unlike the other modules this one adds to a file that already exists. `Attic Expressive.figcraft` is the
source now, so nothing here rebuilds what the file holds: `attach` reads the opened file's pages, styles,
tokens and components into the Doc, and `build` adds to them.

    cd design/build
    python3 -m expressive.equaliser "../Attic Expressive.figcraft" /tmp/attic-design/expressive-eq.figcraft \
        [--export /tmp/attic-eq/figcraft/expressive]

Run it on a file at 1.0.0 only: it refuses a file that already has the Equaliser section. The result is 1.2.0:
1.1.0 drew the equaliser from the spec, and 1.2.0 is the review of the built one (the Changelog gets both entries).
Frames ahead of the code would carry a Proposed annotation (PROPOSALS); the code followed 1.2.0, so none do.

Icons (page Icons)
------------------
Icon/equalizer, Icon/transition_fade, Icon/equalizer (filled), Icon/album (filled), Icon/music_note (filled):
the glyphs Symbols.kt gained since 1.0.0 and the filled ones the equaliser draws.

New component sets
------------------
Components / Actions
  Equaliser/Preset chip       PresetChip (ExpressiveEqualiser.kt)
    Kind=Preset|Saved preset|Save action, Selected=False|True, Equaliser=On|Off.  [Label]
    Selected is primary while the equaliser is on (1.2.0), tonal while it is off.
Components / Lists and cards
  Equaliser/Scope cell        EqualiserScopeCell (ExpressiveEqualiser.kt)  Scope=Default|Album|Song.  [Subtitle]
    Album and Song fill the cell with primary container (1.2.0).
  Equaliser/Own setting row   the Settings page's rows (ExpressiveEqualiser.kt · ownRows and OwnSetting)
    Kind=Album|Song.  [Title, Subtitle]; the nested count (the preset's name) is exposed
Components / Playback
  Equaliser/Band graph        BandGraph (BandGraph.kt), with its axis and frequency labels
    Curve=Bass boost|Custom|Vocal|Car speakers, State=On|Off|Dragging (the states each screen needs)
  Now playing/Equaliser indicator   EqualiserIndicator (ExpressiveNowPlaying.kt)  Scope=Song|Album.  Cover mode
Components / Overlays
  Equaliser/Sheet title       EqualiserTitle and the Off line  Checked=True|False
  Equaliser/Editor            EqualiserEditor: the preset chips over the band graph
    Curve=Bass boost|Custom|Vocal|Car speakers, State=On|Off|Dragging
  Equaliser/Actions           the sheet's last block (1.2.0, in place of 1.1.0's save row, note and Removal row):
    one button group and one caption line in every state, so the sheet keeps its height when the scope changes
    Playing with=Default|Default without album|Album|Song|Song over album
  Sheet/Equaliser             ExpressiveEqualiserSheet  Scope=Default|Album|Song,
    State=On|Off|Band dragged|Saved preset.  Color mode Cover

Changed components
------------------
Now playing header    gains the indicator (hidden) and the boolean "Show equaliser"; the indicator is exposed.
Sheet/Track menu      the Now playing and Open with variants carry "Sleep timer" and "Equaliser" in one group.
List row/Setting      gains Status line=Plain (Crossfade's note, the Equaliser row's own-setting count) and
                      Trailing=Icon (the Phone equaliser row).
Components / Overlays gains a card showing the track menu's Equaliser row in its four states.

Screens (section "Equaliser" on the Screens page, Light and Dark)
----------------------------------------------------------------
eq_settings_playback, eq_page, eq_page_full, eq_page_empty, eq_now_playing, eq_track_menu, eq_sheet_default,
eq_sheet_song, eq_sheet_album, eq_sheet_off, eq_sheet_dragging, eq_save_preset, eq_preset_menu.

Prototype
---------
Flow "12 Equaliser" starts at Settings, Playback. The existing track menus' Equaliser row opens the sheet too.
The Flow overview gains an Equaliser band with its cards and arrows.

Handoff
-------
A Source annotation on every new frame; ready-for-dev on the frames the code matches and a Proposed annotation on
the others (PROPOSALS); Cover 1.2.0; Changelog entries for 1.1.0 and 1.2.0, both dated 2026-11-25.

In a build from scratch (`figbuild.run expressive`) this module does nothing but say so: it copies frames, flows
and pages that only a finished file has. Build 1.0.0 first, then run this module on the saved file.
"""
from __future__ import annotations

import argparse
import json
import math
import re
import sys
import zipfile
from pathlib import Path

from figbuild.kit import Doc, V, Area, DENSITY, _round, _enum
from figbuild import icons as _icons
from expressive import foundations
from expressive.foundations import DocPage, bind_dim, cover_path, dialog_y
from expressive.components_core import Spec, comp, frame, txt, inst, set_text, set_cover, spacer, vname, stroke_vec
from expressive.components_overlays import set_glyph
from expressive.flows import TRANSITIONS

REFS: dict[str, str] = {}

VERSION = "1.2.0"
UPDATED = "2026-11-25"
AUTHOR = "Attic maintainers"
EQ = "ExpressiveEqualiser.kt"
MOCK = "docs/design/2026-11-21-ui-directions/pixel/11-equaliser.html"
SECTION = "Equaliser"
SCREENS = "Screens"

# ───────────────────────────────────────────────────────────────── copy (res/values/strings.xml)
S = {
    "equaliser": "Equaliser",
    "use": "Use equaliser",
    "off_note": "Off. Choose a preset or move a band to turn it on.",
    "scope_default": "For all songs",
    "scope_default_body": "Every song without its own setting",
    "scope_album": "For this album",
    "scope_song": "For this song only",
    "save_song": "Save for this song",
    "save_album": "Save for this album",
    "saved_note": "Saved settings come back whenever the song plays.",
    "use_default": "Use default",
    "use_album": "Use album setting",
    "removes_song": "Removes this song's setting",
    "removes_album": "Removes this album's setting",
    "custom": "Custom",
    "save_preset": "Save as preset",
    "preset_name": "Preset name",
    "save": "Save",
    "cancel": "Cancel",
    "rename": "Rename",
    "delete_preset": "Delete preset",
    "indicator_song": "This song",
    "indicator_album": "This album",
    "default": "Default",
    "own_albums": "Albums with their own setting",
    "own_songs": "Songs with their own setting",
    "own_none_title": "Songs and albums with their own setting",
    "own_none": "None yet. Open Equaliser from the menu on Now Playing and choose “Save for this song”.",
    "phone": "Phone equaliser",
    "phone_body": "Opens the equaliser on this phone.",
    "playback": "Playback",
    "volume": "Volume levelling",
    "volume_body": ("Plays songs at a similar loudness, from their ReplayGain tags or the level measured during "
                    "analysis. Songs not measured yet play as they are."),
    "crossfade": "Crossfade",
    "crossfade_off": "Off",
    "crossfade_body": "Songs from one album played in order still run straight into each other.",
    "own_summary": "With their own setting: 4 songs, 2 albums",
}

# The demo library (app/src/storeShots, DemoLibrary.kt and EqualiserDemo.kt).
SONG, ARTIST, ALBUM = "Lights Over the Water", "Mira Okafor", "Paper Lanterns"
SAVED = ["Car speakers", "Night, low"]
BUILT_IN = ["Flat", "Bass boost", "Bass reducer", "Treble boost", "Vocal", "Loudness", "Acoustic", "Electronic",
            "Hip-hop", "Classical", "Spoken word"]
# EqualiserDemo.page(): title, artist, demo cover index, preset.
OWN_ALBUMS = [("Afterglow Protocol", "Velvet Circuit", 10, "Electronic"), ("Room Tone", "Odile Marchetti", 4, "Custom")]
OWN_SONGS = [("Chai Ka Doosra Cup", "Ruhaan Sethi", 9, "Vocal"), ("Kettle On", "Small Hours Tape Club", 1, "Night, low"),
             ("Lights Over the Water", "Mira Okafor", 0, "Custom"), ("Marea Lenta", "Paloma Ferrer", 2, "Acoustic")]


# ───────────────────────────────────────────────────────────────── the engine's response, for the drawing
BANDS = ["31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"]
FREQS = [31.25 * 2 ** i for i in range(10)]
RATE = 48_000
PEAKING_Q, TREBLE_BAND, SHELF_HZ = 0.8, 9, math.sqrt(8000.0 * 16000.0)
PROTOTYPE_DB, ITERATIONS, MIDPOINT_WEIGHT, MAX_FILTER_DB = 17.0, 2, 0.25, 40.0


def on_grid(g: float) -> float:
    """EqPresets.onGrid: the nearest half dB, ties towards +infinity as Kotlin's roundToInt does."""
    return math.floor(g / 0.5 + 0.5) * 0.5


# EqPresets.kt (on the slider grid) and EqualiserDemo.kt's own curves.
CURVES = {
    "Bass boost": [on_grid(g) for g in (5.5, 4.25, 3.5, 2.5, 1.25, 0, 0, 0, 0, 0)],
    "Vocal": [on_grid(g) for g in (-1.5, -3, -3, 1.5, 3.75, 3.75, 3, 1.5, 0, -1.5)],
    "Custom": [3, 4, 2.5, 0.5, -1.5, -2.5, -0.5, 2, 3.5, 2.5],
    "Car speakers": [4, 3.5, 2, 0, -1, 0, 1, 2.5, 3, 2],
}
DRAG_BAND, DRAG_TO = 7, 3.0     # the Dragging state: 4 kHz on its way from +2 to +3 dB


def _design_hz(b: int) -> float:
    return SHELF_HZ if b == TREBLE_BAND else FREQS[b]


def _coefficients(b: int, gain_db: float):
    """EqDesign.coefficients: RBJ peaking at Q 0.8, and a high shelf (slope 1) for the 16 kHz band."""
    w0 = 2 * math.pi * _design_hz(b) / RATE
    cw, sw = math.cos(w0), math.sin(w0)
    a = 10 ** (gain_db / 40)
    if b == TREBLE_BAND:
        alpha = sw / 2 * math.sqrt(2)
        k = 2 * math.sqrt(a) * alpha
        a0 = (a + 1) - (a - 1) * cw + k
        return (a * ((a + 1) + (a - 1) * cw + k) / a0, -2 * a * ((a - 1) + (a + 1) * cw) / a0,
                a * ((a + 1) + (a - 1) * cw - k) / a0, 2 * ((a - 1) - (a + 1) * cw) / a0,
                ((a + 1) - (a - 1) * cw - k) / a0)
    alpha = sw / (2 * PEAKING_Q)
    a0 = 1 + alpha / a
    return ((1 + alpha * a) / a0, -2 * cw / a0, (1 - alpha * a) / a0, -2 * cw / a0, (1 - alpha / a) / a0)


def _db(c, cos_w: float) -> float:
    b0, b1, b2, a1, a2 = c
    cos2 = 2 * cos_w * cos_w - 1
    num = b0 * b0 + b1 * b1 + b2 * b2 + 2 * (b0 * b1 + b1 * b2) * cos_w + 2 * b0 * b2 * cos2
    den = 1 + a1 * a1 + a2 * a2 + 2 * (a1 + a1 * a2) * cos_w + 2 * a2 * cos2
    return 10 * math.log10(max(num / den, 1e-30))


def _solve(matrix, targets, weights):
    """EqDesign.solveInto: weighted least squares through the normal equations."""
    n = 10
    a = [[0.0] * (n + 1) for _ in range(n)]
    for p, row in enumerate(matrix):
        w2 = weights[p] ** 2
        for i in range(n):
            mi = row[i] * w2
            for j in range(n):
                a[i][j] += mi * row[j]
            a[i][n] += mi * targets[p]
    for col in range(n):
        pivot = max(range(col, n), key=lambda r: abs(a[r][col]))
        a[col], a[pivot] = a[pivot], a[col]
        dgn = a[col][col]
        if abs(dgn) < 1e-12:
            continue
        for r in range(col + 1, n):
            f = a[r][col] / dgn
            if f:
                for k in range(col, n + 1):
                    a[r][k] -= f * a[col][k]
    x = [0.0] * n
    for i in range(n - 1, -1, -1):
        s = a[i][n] - sum(a[i][k] * x[k] for k in range(i + 1, n))
        x[i] = 0.0 if abs(a[i][i]) < 1e-12 else s / a[i][i]
    return x


def filter_gains(curve: list[float]) -> list[float]:
    """EqDesign.filterGainsDb: the filter gains whose cascade meets the sliders (Välimäki and Liski)."""
    if not any(curve):
        return [0.0] * 10
    positions, weights = [], []
    for b in range(10):
        positions.append(float(b)); weights.append(1.0)
        if b < 9:
            positions.append(b + 0.5); weights.append(MIDPOINT_WEIGHT)
    cosines = [math.cos(2 * math.pi * FREQS[0] * 2 ** p / RATE) for p in positions]

    def slider(p):
        low, frac = int(p), p - int(p)
        return curve[low] if not frac or low >= 9 else curve[low] * (1 - frac) + curve[low + 1] * frac

    targets = [slider(p) for p in positions]
    proto = [[_db(_coefficients(b, PROTOTYPE_DB), c) / PROTOTYPE_DB for b in range(10)] for c in cosines]
    gains = _solve(proto, targets, weights)
    for _ in range(ITERATIONS):
        matrix = [row[:] for row in proto]
        for b in range(10):
            if abs(gains[b]) >= 0.01:
                co = _coefficients(b, gains[b])
                for p, c in enumerate(cosines):
                    matrix[p][b] = _db(co, c) / gains[b]
        gains = _solve(matrix, targets, weights)
    return [max(-MAX_FILTER_DB, min(MAX_FILTER_DB, g)) for g in gains]


def response(curve: list[float], points: int = 96) -> list[float]:
    """EqualiserBands.response: dB at `points` frequencies over the drawing's ten octaves."""
    fg = filter_gains(curve)
    co = [_coefficients(b, g) for b, g in enumerate(fg) if g]
    lo, hi = FREQS[0] / math.sqrt(2), FREQS[-1] * math.sqrt(2)
    out = []
    for i in range(points):
        f = math.exp(math.log(lo) + (math.log(hi) - math.log(lo)) * i / (points - 1))
        cw = math.cos(2 * math.pi * f / RATE)
        out.append(sum(_db(c, cw) for c in co))
    return out


# ───────────────────────────────────────────────────────────────── band graph geometry (UI-SPEC 2.3)
PLOT_W, PLOT_H, PX_DB, ZERO = 339.0, 192.0, 8.0, 96.0
GUTTER, HEAD = 30, 20            # axis labels before the plot; room above it for the value bubble
GRAPH_W, GRAPH_H = 379, HEAD + 192 + 8 + 16


def _n(v: float) -> str:
    return f"{v:.2f}".rstrip("0").rstrip(".")


def band_x(i: int) -> float:
    return (i + 0.5) * PLOT_W / 10


def response_paths(curve: list[float]) -> tuple[str, str]:
    """(the curve, the area between it and the 0 dB line), clamped to ±14.5 dB."""
    db = response(curve)
    pts = [(PLOT_W * i / (len(db) - 1), ZERO - max(-14.5, min(14.5, v)) * PX_DB) for i, v in enumerate(db)]
    line = "M" + " L".join(f"{_n(x)} {_n(y)}" for x, y in pts)
    return line, f"{line} L{_n(PLOT_W)} {_n(ZERO)} L0 {_n(ZERO)} Z"


def fmt_db(g: float) -> str:
    """EqualiserBands.formatDb with R.string.equaliser_db."""
    r = round(g * 2) / 2
    m = str(int(abs(r))) if abs(r) % 1 == 0 else str(abs(r))
    return f"{'+' if r > 0 else '−' if r < 0 else ''}{m} dB"


# ───────────────────────────────────────────────────────────────── attaching to an opened file
def attach(d: Doc, ctx: dict):
    """Fill the Doc's registries (pages, styles, tokens, components and their properties) from the opened file,
    so the helpers the other modules use work on it."""
    d.pages = {p["name"]: p["id"] for p in d.cmd("page.list") if not p.get("divider")}
    d._first_page = False
    d.mode_collections = ("Wallpaper", "Cover", "System", "Art", "Comfort preview", "Phone")
    for s in d.cmd("style.list"):
        (d.text_styles if s["type"] == "text" else d.effect_styles if s["type"] == "effect" else {})[s["name"]] = s["id"]
    listing = d.cmd("variable.list")
    cols = {c["id"]: c["name"] for c in listing["collections"]}
    for v in listing["variables"]:
        if cols[v["collection"]] == "Dimension":
            foundations.DIM[v["name"]] = v["id"]
    C = ctx.setdefault("components", {})

    def props_of(cid):
        got = d.cmd("component.properties", id=cid).get("properties", [])
        return {p["name"].split("#")[0]: p["name"] for p in got if p.get("type") != "variant"}

    comps = d.cmd("component.list")
    in_set = set()
    for c in comps:
        if c["type"] != "COMPONENT_SET":
            continue
        kids = [k for k in d.get(c["id"], depth=1).get("children", []) if k["type"] == "COMPONENT"]
        in_set |= {k["id"] for k in kids}
        d.comp[c["name"]] = c["id"]
        C[c["name"]] = {"id": c["id"], "variants": {k["name"]: k["id"] for k in kids}, "props": props_of(c["id"])}
        d.props[c["name"]] = list(C[c["name"]]["props"].values())
    for c in comps:
        if c["type"] != "COMPONENT" or c["id"] in in_set:
            continue
        d.comp[c["name"]] = c["id"]
        if c["name"].startswith("Icon/"):
            d.icon_names.add(c["name"])
        elif not c["name"].startswith("Shape/"):
            C[c["name"]] = {"id": c["id"], "variants": {}, "props": props_of(c["id"])}
            d.props[c["name"]] = list(C[c["name"]]["props"].values())
    # Screens stack under the last section on the page.
    bottom = 0
    for n in d.get(d.pages[SCREENS], depth=1).get("children", []):
        g = d.get(n["id"])
        bottom = max(bottom, g.get("y", 0) + g.get("height", 0))
    Area._next_y[(id(d), SCREENS)] = bottom + 200
    ctx["attached"] = True


def variant(ctx: dict, set_name: str, **vp) -> int:
    return ctx["components"][set_name]["variants"][vname({k.replace("_", " "): v for k, v in vp.items()})]


def top_named(d: Doc, parent: int, name: str, type: str | None = None) -> int:
    """A direct child of `parent` by exact name."""
    for n in d.get(parent, depth=1).get("children", []):
        if n.get("name") == name and (type is None or n.get("type") == type):
            return n["id"]
    raise KeyError(f"no layer {name!r} directly under {parent}")


def screen_frame(d: Doc, name: str, theme: str = "Light") -> int:
    """A screen frame on the Screens page by its name."""
    full = f"{name} · {theme}"
    for sec in d.get(d.pages[SCREENS], depth=1).get("children", []):
        if sec.get("type") != "SECTION":
            if sec.get("name") == full:
                return sec["id"]
            continue
        for n in d.get(sec["id"], depth=1).get("children", []):
            if n.get("name") == full:
                return n["id"]
    raise KeyError(f"no screen frame {full!r}")


def cards(d: Doc, page: str) -> list[dict]:
    got = [d.get(n["id"]) for n in d.get(d.pages[page], depth=1).get("children", [])
           if n.get("name", "").startswith("Card · ")]
    return sorted(got, key=lambda g: g["y"])


def restack(d: Doc, page: str):
    """Put a page's cards back in one column, 72 apart, after a card grew, and give them one width."""
    cs = cards(d, page)
    if not cs:
        return
    width = max(c["width"] for c in cs)
    y = cs[0]["y"]
    for c in cs:
        if abs(c["y"] - y) > 0.01:
            d.set(c["id"], y=y)
        if abs(c["width"] - width) > 0.01:
            d.set(c["id"], sizingH="fixed", width=width)
        y += d.get(c["id"])["height"] + DocPage.CARD_GAP


# ───────────────────────────────────────────────────────────────── icons
NEW_ICONS = [("equalizer", 0), ("transition_fade", 0), ("equalizer", 1), ("album", 1), ("music_note", 1)]


def build_icons(d: Doc, ctx: dict):
    """The glyphs the file lacks, made as foundations.build_icons_page makes them and added to its grids."""
    page = d.page("Icons")
    points = {lig: cp for _, lig, cp in _icons.symbols()}
    made = []
    for lig, fill in NEW_ICONS:
        nm = f"Icon/{lig}" + (" (filled)" if fill else "")
        if nm in d.comp:
            continue
        cp = points[lig]
        grid = d.child(top_named(d, page, "Card · Filled" if fill else "Card · Outlined"), "Icon grid")
        c = d.component(None, name=nm, w=24, h=24, x=-2000, y=0,
                        description=f"Material Symbols Rounded `{lig}` (U+{cp:04X}), FILL {fill}, wght 400, opsz 24. "
                                    f"Compose: Symbol.{_enum(lig)}. Drawn 0.1em below the font box, where SymbolIcon's "
                                    f"text layout puts it.")
        g = d.vector(c, name="Glyph", path=_round(_icons.path(cp, fill=fill)), fill="Color/on-surface",
                     constraints={"horizontal": "scale", "vertical": "scale"})
        d.set(g, y=round(d.get(g)["y"] + 2.4, 3))
        cell = d.frame(grid, name=f"Icon cell · {lig}{' filled' if fill else ''}", layout="v", gap=10, cross="center",
                       pad=[20, 8, 16, 8], fill="Color/surface", radius=16, sizing=("hug", "hug"))
        d.set(cell, sizingH="fixed", width=132)
        d.cmd("node.reparent", ids=[c], parent=cell)
        d.text(cell, lig, style="Doc/Caption", color="Canvas/label", name="Ligature", textAlign="center")
        d.register(nm, c)
        d.icon_names.add(nm)
        made.append(nm)
    # The page's intro counts the glyphs.
    intro = top_named(d, page, "Intro")
    text = d.get(intro)["characters"]
    count = len([n for n in d.icon_names if not n.endswith("(filled)")])
    new = re.sub(r"only the \d+ glyphs", f"only the {count} glyphs", text)
    if new != text:
        d.set(intro, characters=new)
    restack(d, "Icons")
    ctx["eq_icons"] = made


# ───────────────────────────────────────────────────────────────── components
def build_components(d: Doc, ctx: dict):
    C = ctx["components"]
    btn = C["Button"]["props"]

    def spec(page: str) -> Spec:
        return Spec(d, ctx, DocPage.below(d, page))

    # Preset chip (Actions) -------------------------------------------------------------------------
    def chip(vp):
        kind, sel, live = vp["Kind"], vp["Selected"] == "True", vp["Equaliser"] == "On"
        save, more = kind == "Save action", kind == "Saved preset" and sel
        lead = sel or save
        # Selected is the kit's one-of-N look (the connected group's): primary. Tonal is too close to a group cell
        # to read as a fill in Light (secondary container on surface container high), so it is kept for off only.
        on_fill = ("Color/primary", "Color/on-primary") if live else ("Color/secondary-container", "Color/on-secondary-container")
        ink = on_fill[1] if sel else "Color/primary" if save else "Color/on-surface-variant"
        c = comp(d, "Equaliser/Preset chip", layout="h", h=32, gap=8, pad=[0, 6 if more else 16, 0, 8 if lead else 16],
                 cross="center", sizing=("hug", "fixed"), fill=on_fill[0] if sel else None,
                 **({} if sel else {"stroke": "Color/outline-variant", "stroke_w": 1}))
        bind_dim(d, c, "cornerRadius", "radius/small")
        if lead:
            d.icon(c, "add" if save else "check", size=18, color=ink, name="Add" if save else "Check")
        label = S["save_preset"] if save else "Car speakers" if kind == "Saved preset" else "Bass boost"
        # PresetChip puts 8dp after the leading glyph and 6dp either side of the more glyph.
        box = frame(d, c, "Label and more", layout="h", gap=6, cross="center", sizing=("hug", "hug")) if more else c
        lay = {"l": txt(d, box, label, "Label/Large", ink, "Label")}
        if more:
            d.icon(box, "more_vert", size=18, color=ink, name="More")
        return c, lay

    spec("Components / Actions").make(
        "Equaliser/Preset chip", "Equaliser preset chip",
        "A chip of the equaliser's single-choice preset row, to the filter chip's measure: 32dp tall in a 48dp "
        "target, 8dp corners, outlined while unselected. The selected chip is filled with primary, its tick and label "
        "in on primary, as the connected button group marks its one choice: on a group cell the filter chip's tonal "
        "fill is too close to the cell to be seen in Light. A saved preset that is selected ends in the more glyph "
        "(18dp, 6dp either side): tapping it again, or a long press on any saved chip, opens Rename and Delete "
        "preset. An unselected saved preset looks like any preset. Save action is \"Save as preset\", outlined with "
        "the add glyph and a primary label, shown only next to Custom. Equaliser=Off is the selected chip while the "
        "switch is off: tonal, with the whole row at 50 %, so nothing in the editor keeps the accent. In the code "
        "the built-in presets are the kit's ExpressiveFilterChip (Navigation.kt) and PresetChip draws the saved ones "
        "and the save action.",
        EQ, "PresetChip",
        [dict(Kind="Preset", Selected="False", Equaliser="On"), dict(Kind="Preset", Selected="True", Equaliser="On"),
         dict(Kind="Saved preset", Selected="True", Equaliser="On"),
         {"Kind": "Save action", "Selected": "False", "Equaliser": "On"},
         dict(Kind="Preset", Selected="True", Equaliser="Off")],
        chip, props=[("Label", "text", "l", None, None)])
    chip_label = C["Equaliser/Preset chip"]["props"]["Label"]

    # Scope cell and the Settings page's rows (Lists and cards) --------------------------------------
    scopes = {"Default": (S["scope_default"], S["scope_default_body"], "library_music"),
              "Album": (S["scope_album"], ALBUM, "album"), "Song": (S["scope_song"], SONG, "music_note")}

    def scope_cell(vp):
        title, sub, glyph = scopes[vp["Scope"]]
        own = vp["Scope"] != "Default"
        c = comp(d, "Equaliser/Scope cell", layout="h", w=379, gap=16, pad=[12, 16, 12, 16], cross="center",
                 sizing=("fixed", "hug"), fill="Color/primary-container" if own else None)
        d.set(c, minHeight=72)
        ink = "Color/on-primary-container" if own else None
        lead = frame(d, c, "Lead", layout="h", w=40, h=40, cross="center", main="center",
                     fill="Color/primary" if own else "Color/secondary-container")
        bind_dim(d, lead, "cornerRadius", "radius/medium" if own else "radius/full")
        d.icon(lead, glyph, color="Color/on-primary" if own else "Color/on-secondary-container", filled=own, name="Icon")
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        txt(d, col, title, "Title/Large Emphasized", ink or "Color/on-surface", "Title", fill=True, lines=1)
        s = txt(d, col, sub, "Body/Medium", ink or "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        return c, {"s": s}

    lists = spec("Components / Lists and cards")
    lists.make(
        "Equaliser/Scope cell", "Equaliser scope cell",
        "Which setting the equaliser is editing, as the first cell of its group: a 40dp lead, the scope in the emphasised "
        "title (22/28) and what it covers under it on one line. Default is every song without its own setting: a round "
        "tonal lead on the group's own cell colour. A song or an album with its own setting squares the lead off to "
        "12dp and fills it with primary, the glyph filled (the kit's squared off means on), and the whole cell turns "
        "primary container with both lines in on primary container, so the scope is seen from the corner of the eye "
        "while a band is dragged; the line names the album or the song. The cell's colour and the lead's change on "
        "default effects, the lead's corner on the fast spatial spring. At least 72dp tall, 16dp in. Put it in List "
        "group/Cell by swapping its Row.",
        EQ, "EqualiserScopeCell", [dict(Scope="Default"), dict(Scope="Album"), dict(Scope="Song")], scope_cell,
        props=[("Subtitle", "text", "s", None, None)])

    def own_row(vp):
        album = vp["Kind"] == "Album"
        title, artist, cover, preset = OWN_ALBUMS[0] if album else OWN_SONGS[0]
        c = comp(d, "Equaliser/Own setting row", layout="h", w=379, gap=12, pad=[8, 4, 8, 12], cross="center",
                 sizing=("fixed", "hug"))
        d.set(c, minHeight=72 if album else 64)
        cv = inst(d, "Cover", c, name="Lead", Kind="Album", Size="56" if album else "48")
        set_cover(d, cv, cover)
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        t = txt(d, col, title, "Kit/Row title", "Color/on-surface", "Title", fill=True, lines=1)
        s = txt(d, col, artist, "Body/Medium", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        cnt = d.instance("List row/Count", c, name="Preset")
        set_text(d, cnt, C["List row/Count"]["props"]["Count"], preset)
        b = inst(d, "Icon button", c, name="Remove", Style="Standard", Size="Default", Width="Default", State="Enabled")
        d.cmd("instance.setProperties", ids=[b], properties={C["Icon button"]["props"]["Icon"]: d.comp["Icon/close"]})
        return c, {"t": t, "s": s, "cnt": cnt}

    lists.make(
        "Equaliser/Own setting row", "Equaliser own setting row",
        "A song or an album that has its own equaliser setting, on Settings > Equaliser: the album row (56dp cover, "
        "72dp tall) or the song row (48dp cover, 64dp tall) with the preset its setting is, or Custom, as the row's "
        "figures, then the close button that removes the setting. Only the button is clickable; the row leaves "
        "without a confirmation. The row's end is OwnSetting. The preset is the nested List row/Count's Count (the Preset layer is exposed). Put "
        "the row in List group/Cell by swapping its Row.",
        EQ, "ownRows", [dict(Kind="Album"), dict(Kind="Song")], own_row,
        props=[("Title", "text", "t", None, None), ("Subtitle", "text", "s", None, None)], expose=("cnt",))

    # Band graph and the Now Playing indicator (Playback) --------------------------------------------
    def band_graph(vp):
        gains = list(CURVES[vp["Curve"]])
        on, drag = vp["State"] != "Off", vp["State"] == "Dragging"
        if drag:
            gains[DRAG_BAND] = DRAG_TO
        ink = "Color/primary" if on else "Color/outline"
        c = comp(d, "Equaliser/Band graph", w=GRAPH_W, h=GRAPH_H, clip=False)
        plot = frame(d, c, "Plot", w=PLOT_W, h=PLOT_H, x=GUTTER, y=HEAD, clip=False)
        stroke_vec(d, plot, "Band tracks", " ".join(f"M{_n(band_x(i))} 0 V{_n(PLOT_H)}" for i in range(10)),
                   "Color/outline-variant", 2)
        d.rect(plot, name="Zero line", w=PLOT_W, h=1, x=0, y=ZERO - 0.5, fill="Color/outline")
        if on:
            line, area = response_paths(gains)
            d.set(d.vector(plot, name="Response fill", path=area, fill="Color/primary"), opacity=0.16)
            d.set(stroke_vec(d, plot, "Response curve", line, "Color/primary", 2), opacity=0.8)
        bars = frame(d, plot, "Bands", w=PLOT_W, h=PLOT_H, x=0, y=0, clip=False)
        for i, g in enumerate(gains):
            length, cx = abs(g) * PX_DB, band_x(i)
            if length < 3:
                d.rect(bars, name=f"Band {BANDS[i]}", w=16, h=6, x=cx - 8, y=ZERO - g * PX_DB - 3, fill=ink, radius=3)
                continue
            w = 18 if drag and i == DRAG_BAND else 14
            end, foot = min(w / 2, length / 2), min(2, length / 2)
            d.rect(bars, name=f"Band {BANDS[i]}", w=w, h=length, x=cx - w / 2, y=ZERO - length if g > 0 else ZERO,
                   fill=ink, cornerRadius=[end, end, foot, foot] if g > 0 else [foot, foot, end, end])
        if drag:
            g = gains[DRAG_BAND]
            top = ZERO - max(g, 0) * PX_DB
            b = frame(d, plot, "Value bubble", layout="h", w=58, h=26, cross="center", main="center",
                      fill="Color/inverse-surface", x=band_x(DRAG_BAND) - 29, y=top - 8 - 26)
            bind_dim(d, b, "cornerRadius", "radius/full")
            v = txt(d, b, fmt_db(g), "Label/Medium Emphasized", "Color/inverse-on-surface", "Value")
            d.cmd("text.setFeatures", ids=[v], features={"tnum": True})
        axis = frame(d, c, "Axis labels", w=24, h=PLOT_H + 16, x=0, y=HEAD - 8, clip=False)
        for label, y in (("+12", 0), ("0", ZERO), ("−12", PLOT_H)):
            box = frame(d, axis, f"Axis {label}", layout="h", w=24, h=16, x=0, y=y, main="max", cross="center", clip=False)
            a = txt(d, box, label, "Label/Small", "Color/on-surface-variant", "Value")
            d.cmd("text.setFeatures", ids=[a], features={"tnum": True})
        freq = frame(d, c, "Frequency labels", layout="h", w=PLOT_W, h=16, x=GUTTER, y=HEAD + PLOT_H + 8, cross="center")
        for label in BANDS:
            txt(d, freq, label, "Label/Small", "Color/on-surface-variant", f"Band label {label}", fill=True, align="center")
        if not on:
            d.set([axis, freq], opacity=0.6)
        return c, {}

    graph_variants = [dict(Curve="Bass boost", State="On"), dict(Curve="Bass boost", State="Off"),
                      dict(Curve="Custom", State="On"), dict(Curve="Custom", State="Dragging"),
                      dict(Curve="Vocal", State="On"), {"Curve": "Car speakers", "State": "On"}]
    playback = spec("Components / Playback")
    playback.make(
        "Equaliser/Band graph", "Equaliser band graph",
        "The ten bands and the response they give, in one drawing. Each band is a 2dp track with a 14dp bar from the "
        "0 dB line to its value, 8dp per dB over ±12 dB: the bar is the slider, there is no thumb. Its moving end is "
        "round (7dp) and its end on the line 2dp; within ±0.375 dB it is a 16 x 6 pill on the line, so a band at 0 "
        "still shows a handle. Behind the bars is the engine's real response over ten octaves (22 Hz to 22.6 kHz, so "
        "each bump sits over its band), stroked 2dp in primary at 80 % and filled to the 0 dB line at 16 %. Dragging: "
        "that bar is 18dp wide and its value shows in a bubble 8dp above it. Off: the bars are outline, the response "
        "is gone (the flat line is what plays) and the labels are at 60 %. The plot is 339 x 192 with 30dp before it "
        "for +12, 0 and −12 and 20dp above for the bubble; the frequency labels sit 8dp under it. Curve holds "
        "the curves the screens show: the built-in Bass boost and Vocal (EqPresets.kt) and the demo's Custom and Car "
        "speakers (EqualiserDemo.kt).",
        "BandGraph.kt", "BandGraph", graph_variants, band_graph)

    def indicator(vp):
        c = comp(d, "Now playing/Equaliser indicator", layout="h", h=32, gap=6, pad=[0, 12, 0, 10], cross="center",
                 sizing=("hug", "fixed"), fill="Color/surface-container-high")
        bind_dim(d, c, "cornerRadius", "radius/full")
        d.icon(c, "equalizer", size=18, color="Color/primary", name="Icon")
        txt(d, c, S["indicator_song"] if vp["Scope"] == "Song" else S["indicator_album"], "Label/Medium",
            "Color/on-surface-variant", "Label")
        return c, {}

    playback.make(
        "Now playing/Equaliser indicator", "Now Playing equaliser indicator",
        "Shown in Now Playing's header, between the context and More, while the equaliser is on and the song plays with "
        "its own setting or its album's: a 32dp pill on surface container high with the equaliser glyph (18dp) in "
        "primary and \"This song\" or \"This album\". Tapping it opens the equaliser sheet. It fades in and out; the "
        "context name gives up width to it. The compact header that pins on scroll does not carry it.",
        "ExpressiveNowPlaying.kt", "EqualiserIndicator", [dict(Scope="Song"), dict(Scope="Album")], indicator,
        cover=True)

    # Sheet title, editor, removal row and the sheet (Overlays) ---------------------------------------
    def sheet_title(vp):
        on = vp["Checked"] == "True"
        c = comp(d, "Equaliser/Sheet title", layout="v", w=411, gap=2, pad=[4, 24, 12 if on else 16, 24],
                 sizing=("fixed", "hug"))
        row = frame(d, c, "Title row", layout="h", gap=16, cross="center", sizing=("fill", "hug"))
        txt(d, row, S["equaliser"], "Headline/Small Emphasized", "Color/on-surface", "Title", fill=True, lines=1)
        inst(d, "Switch", row, name="Switch", Checked=str(on))
        if not on:
            n = txt(d, c, S["off_note"], "Body/Medium", "Color/on-surface-variant", "Off line", fill=True)
            d.set(n, textAutoResize="height")
        return c, {}

    overlays = spec("Components / Overlays")
    overlays.make(
        "Equaliser/Sheet title", "Equaliser sheet title",
        "The equaliser sheet's title row: \"Equaliser\" in the emphasised headline (24/32) with the master switch at "
        "its end, 24dp in. The whole row toggles. Off adds the line that says how to turn it back on, 2dp under the "
        "title with 16 below; every control still works while off, and a chip or the first band movement turns the "
        "switch on. The Off line is the kit's SheetSubtitle (ExpressiveSheets.kt).", EQ, "EqualiserTitle",
        [dict(Checked="True"), dict(Checked="False")], sheet_title)

    def chips_for(curve):
        """[(label, kind, selected)] in the row's order: Custom and its save action, saved presets, built-ins."""
        row = []
        if curve == "Custom":
            row += [(S["custom"], "Preset", True), (S["save_preset"], "Save action", False)]
        row += [(n, "Saved preset" if n == curve else "Preset", n == curve) for n in SAVED]
        row += [(n, "Preset", n == curve) for n in BUILT_IN]
        return row

    def editor(vp):
        curve, state = vp["Curve"], vp["State"]
        c = comp(d, "Equaliser/Editor", layout="v", w=379, pad=[12, 0, 12, 0], sizing=("fixed", "hug"))
        strip = frame(d, c, "Preset chips", w=379, h=32, clip=True)
        d.set(strip, sizingH="fill")
        row = frame(d, strip, "Chip row", layout="h", gap=8, pad=[0, 12, 0, 12], sizing=("hug", "hug"), x=0, y=0)
        chosen, made = 0, []
        for label, kind, sel in chips_for(curve):
            ch = inst(d, "Equaliser/Preset chip", row, name=f"Chip {label}", Kind=kind, Selected=str(sel),
                      Equaliser="Off" if sel and state == "Off" else "On")
            set_text(d, ch, chip_label, label)
            made.append(ch)
            if sel:
                chosen = len(made) - 1
        # PresetChips opens with the chip before the selected one first in the row. From the third chip on that chip
        # starts 8dp in, 4dp short of the row's padding, so the chip before it is wholly out and leaves no sliver.
        if chosen >= 2:
            before = d.get(made[chosen - 1])["x"]
            d.set(row, x=-round(min(before - 8, d.get(row)["width"] - 379)))
        if state == "Off":
            d.set(row, opacity=0.5)
        inst(d, "Equaliser/Band graph", c, name="Band graph", Curve=curve, State=state)
        return c, {}

    overlays.make(
        "Equaliser/Editor", "Equaliser editor",
        "The second cell of the equaliser's group, the same on the sheet and on Settings > Equaliser: the preset chips "
        "in one row that scrolls sideways (12dp in, 8dp apart), then the band graph, with 12dp above and below. The "
        "chips run Custom (only while the bands match no preset), Save as preset (only next to Custom), the saved "
        "presets in the order they were saved, then the eleven built-in ones. The row opens with the selected chip "
        "second and the chip before it first, that chip 8dp in so the one before it is wholly out of sight. Choosing "
        "a chip applies it at once and the row scrolls until it is in view; Custom scrolls it to the start. Off draws "
        "the chips at 50 % and the selected one tonal. Put it in List group/Cell by swapping its Row.",
        EQ, "EqualiserEditor", graph_variants, editor)

    # What follows the editor, by what the song is playing with: (buttons, caption, which button the caption is under).
    # A button is (label, style, layer name).
    save_song, save_album = (S["save_song"], "Filled", S["save_song"]), (S["save_album"], "Tonal", S["save_album"])
    actions = {
        "Default": ([save_song, save_album], S["saved_note"], None),
        "Default without album": ([save_song], S["saved_note"], None),
        "Album": ([save_song, (S["use_default"], "Outlined", "Remove")], S["removes_album"], 1),
        "Song": ([(S["use_default"], "Outlined", "Remove")], S["removes_song"], None),
        "Song over album": ([(S["use_album"], "Outlined", "Remove")], S["removes_song"], None),
    }

    def actions_block(vp):
        buttons, caption, under = actions[vp["Playing with"]]
        c = comp(d, "Equaliser/Actions", layout="v", w=411, gap=10, pad=[16, 16, 0, 16], sizing=("fixed", "hug"))
        row = frame(d, c, "Buttons", layout="h", gap=8, sizing=("fill", "hug"))
        for label, style, name in buttons:
            b = inst(d, "Button", row, name=name, Style=style, Size="Small", Icon="None", State="Enabled", Squared="False")
            set_text(d, b, btn["Label"], label)
            d.set(b, sizingH="fill")
        line = frame(d, c, "Caption row", layout="h", gap=8, sizing=("fill", "hug"))
        if under is not None:
            for i in range(len(buttons)):
                if i != under:
                    d.set(spacer(d, line, "Under the other button", h=16), sizingH="fill")
                else:
                    n = txt(d, line, caption, "Body/Small", "Color/on-surface-variant", "Caption", fill=True, align="center")
                    d.set(n, textAutoResize="height")
        else:
            n = txt(d, line, caption, "Body/Small", "Color/on-surface-variant", "Caption", fill=True, align="center")
            d.set(n, textAutoResize="height")
        return c, {}

    overlays.make(
        "Equaliser/Actions", "Equaliser actions",
        "The last block of the equaliser sheet: the actions that change which setting the song plays with. Every "
        "state is one button group that fills the width (small 40dp buttons, 8dp apart, 16dp in, 16dp under the "
        "editor) and one caption line 10dp under it (body small 12/16, on surface variant, centred), so the block is the "
        "same 82dp tall whatever the scope and the sheet does not move when a setting is saved or removed. Playing "
        "with the default: Save for this song (filled) and Save for this album (tonal), or the first alone for a song "
        "without an album; the caption says saved settings come back. With the album's setting: Save for this song "
        "and Use default (outlined), the caption under Use default saying it removes the album's setting. With the "
        "song's own: Use default alone, or Use album setting when its album has one, and what it removes. TalkBack "
        "reads a removal and its caption as one button. The scope cell changes in place. At 1.1.0 this was a save "
        "row with a note, or an outlined button with its consequence beside it (SaveActions and RemovalRow), which "
        "made the sheet 26dp shorter or 22dp taller as the scope changed.",
        EQ, "ExpressiveEqualiserSheet", [{"Playing with": k} for k in actions],
        actions_block)

    def group(parent, name):
        return frame(d, parent, name, layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))

    # What each sheet state shows: scope, editor variant, the scope cell's second line, what follows the editor.
    sheets = [
        (dict(Scope="Default", State="On"), ("Bass boost", "On"), None, "Default"),
        (dict(Scope="Default", State="Off"), ("Bass boost", "Off"), None, "Default"),
        ({"Scope": "Default", "State": "Saved preset"}, ("Car speakers", "On"), None, "Default"),
        (dict(Scope="Song", State="On"), ("Custom", "On"), SONG, "Song"),
        ({"Scope": "Song", "State": "Band dragged"}, ("Custom", "Dragging"), SONG, "Song"),
        (dict(Scope="Album", State="On"), ("Vocal", "On"), ALBUM, "Album"),
    ]
    by_name = {vname(vp): rest for vp, *rest in sheets}

    def sheet(vp):
        (curve, state), sub, tail = by_name[vname(vp)]
        c = comp(d, "Sheet/Equaliser", layout="v", w=411, sizing=("fixed", "hug"))
        d.set_mode(c, "Color", "Cover")
        s = d.instance("Sheet", c, name="Sheet")
        d.set(s, sizingH="fill")
        slot = d.find(s, name="Content", limit=1)[0]
        h = frame(d, None, "Slot content", layout="v", w=411, sizing=("fixed", "hug"), x=-6000, y=0)
        inst(d, "Equaliser/Sheet title", h, name="Title", Checked=str(state != "Off"))
        g = group(h, "Setting group")
        scope_row(d, ctx, g, "Sheet", vp["Scope"], sub)
        cell(d, ctx, g, "Bottom", "Sheet", variant(ctx, "Equaliser/Editor", Curve=curve, State=state), "Editor")
        inst(d, "Equaliser/Actions", h, name="Actions", Playing_with=tail)
        kids = [k["id"] for k in d.get(h, depth=1)["children"]]
        old = [k["id"] for k in d.get(slot, depth=1).get("children", [])]
        d.cmd("node.reparent", ids=kids, parent=slot)
        if old:
            d.cmd("edit.delete", ids=old)
        d.set(kids, sizingH="fill")
        d.cmd("edit.delete", ids=[h])
        return c, {}

    overlays.make(
        "Sheet/Equaliser", "Equaliser sheet",
        "Opened from Equaliser in the track menu over Now Playing, in place of the menu, and from the indicator in Now "
        "Playing's header. It edits the setting the song is playing with: the title and the master switch, the scope "
        "cell and the editor as one group, then Equaliser/Actions, the actions that change scope. Playing with the "
        "default: Save for this song and Save for this album. With the album's setting: Save for this song and Use "
        "default. With the song's own: Use default, or Use album setting when its album has one. Saving or removing "
        "changes the scope cell in place, from the group's cell colour to primary container and back; the sheet stays "
        "open and keeps its height. Saved preset is the default with a saved preset chosen, the state its Rename and "
        "Delete preset menu opens over. Color mode Cover, as it only opens over the player.",
        EQ, "ExpressiveEqualiserSheet", [vp for vp, *_ in sheets], sheet)


def cell(d: Doc, ctx: dict, parent, position: str, surface: str, row_variant: int, name: str) -> tuple[int, int]:
    """A List group/Cell whose Row is swapped to `row_variant` (a component or variant id). Returns (cell, row)."""
    c = inst(d, "List group/Cell", parent, name=f"{name} cell", Position=position, Surface=surface)
    row = d.find(c, name="Row", limit=1)[0]
    d.cmd("instance.swap", ids=[row], component=row_variant)
    # The swap keeps the song row's 64dp minimum height; take the new row's own.
    d.set(row, name=name, sizingH="fill", minHeight=d.get(row_variant).get("minHeight") or 0)
    d.set(c, sizingH="fill")
    return c, row


def scope_row(d: Doc, ctx: dict, parent, surface: str, scope: str, subtitle: str | None = None) -> int:
    """The scope cell as the first cell of a group. A swapped Row keeps the Subtitle of the song row it replaced,
    so the line is always set."""
    line = subtitle or {"Default": S["scope_default_body"], "Album": ALBUM, "Song": SONG}[scope]
    _, row = cell(d, ctx, parent, "Top", surface, variant(ctx, "Equaliser/Scope cell", Scope=scope), "Scope")
    d.cmd("instance.setProperties", ids=[row],
          properties={ctx["components"]["Equaliser/Scope cell"]["props"]["Subtitle"]: line})
    return row


def setting_row(d: Doc, ctx: dict, parent, position: str, name: str, *, title: str, icon: str, summary: str | None = None,
                status: str | None = None, trailing: str = "None", surface: str = "Default") -> tuple[int, int]:
    """A List row/Setting in a group cell, every property set (a swapped row keeps values it never had). `status`
    is the plain third line, Status() in ExpressiveSettings.kt."""
    P = ctx["components"]["List row/Setting"]["props"]
    v = variant(ctx, "List row/Setting", Leading="Icon", Lines="Two" if summary else "One", Trailing=trailing,
                Status_line="Plain" if status else "False")
    c, row = cell(d, ctx, parent, position, surface, v, name)
    values = {P["Title"]: title, P["Icon"]: d.comp[f"Icon/{icon}"]}
    if summary:
        values[P["Summary"]] = summary
    if status:
        values[P["Status text"]] = status
    d.cmd("instance.setProperties", ids=[row], properties=values)
    return c, row


# ───────────────────────────────────────────────────────────────── changes to components the file had
HEADER_NOTE = (" While the equaliser is on and the song plays with its own setting or its album's, the equaliser "
               "indicator sits between the context and More (Show equaliser; the nested indicator's Scope says which).")
TRACK_MENU_OLD = "from Now Playing, the sleep timer in a group of its own (running: the filled moon in primary and the time left)"
TRACK_MENU_NEW = ("from Now Playing, the sleep timer (running: the filled moon in primary and the time left) and the "
                  "equaliser in one group, the equaliser with what is playing under it: Off, the default's preset, or "
                  "\"This song · Custom\" and \"This album · Vocal\" for a setting of its own, its glyph in primary while on")
TRACK_MENU_OLD_2 = "A song opened from another app stops after the sleep timer."
TRACK_MENU_NEW_2 = "A song opened from another app stops after that group."
SETTING_NOTE = (" Status line Plain is Status(): a third line in body small on-surface-variant, straight under the "
                "summary (Crossfade's note, the Equaliser row's count of songs and albums with their own setting). "
                "Trailing Icon is a row that leaves the app: a 20dp glyph in on-surface-variant, 10dp down (Settings > "
                "Equaliser's Phone equaliser).")


def card_of(d: Doc, page: str, component: int) -> int:
    """The spec card on `page` that holds a component or set."""
    for c in cards(d, page):
        if any(k["id"] == component for k in d.get(c["id"], depth=1).get("children", [])):
            return c["id"]
    raise KeyError(f"no card on {page} holds {component}")


def _described(d: Doc, set_name: str, page: str, ctx: dict, change):
    """Rewrite a component's description, on the set's variants (FigCraft keeps none on a set) or on the single
    component, and the copy of it on the component's spec card."""
    info = ctx["components"][set_name]
    holders = list(info["variants"].values()) or [info["id"]]
    body, sep, tail = d.get(holders[0], fields=["description"])["description"].partition(" Compose: ")
    new = change(body)
    d.set(holders, description=new + sep + tail)
    d.set(d.child(top_named(d, card_of(d, page, info["id"]), "Heading"), "Description"), characters=new)


def update_header(d: Doc, ctx: dict):
    C = ctx["components"]
    d.page("Components / Playback")
    hdr = C["Now playing header"]["id"]
    kids = [k["name"] for k in d.get(hdr, depth=1)["children"]]
    ind = inst(d, "Now playing/Equaliser indicator", hdr, name="Equaliser indicator", Scope="Song")
    d.cmd("node.reorder", id=ind, index=kids.index("More"))
    d.set(ind, visible=False)
    C["Now playing header"]["props"]["Show equaliser"] = d.prop(hdr, "Show equaliser", "boolean", layer=ind, default=False)
    d.cmd("component.exposeInstance", id=ind)
    _described(d, "Now playing header", "Components / Playback", ctx, lambda t: t + HEADER_NOTE)


TRACK_MENU_VARIANTS = ["Opened from=Now playing, Sleep timer=Off", "Opened from=Now playing, Sleep timer=Running",
                       "Opened from=Open with, Sleep timer=Off"]
TRACK_MENU_FRAMES = ["Track menu / From Now Playing", "Sleep timer / Running, in the track menu",
                     "Track menu / Opened from another app"]


def menu_row(d: Doc, ctx: dict, parent, position: str, *, on: bool, summary: str) -> tuple[int, int]:
    """The track menu's Equaliser cell: MenuCell with CellSymbol(Symbol.Equalizer, on)."""
    P = ctx["components"]["Sheet/Menu row"]["props"]
    v = variant(ctx, "Sheet/Menu row", Lead="Icon on" if on else "Icon", Lines="Two", Selected="False", Trailing="None")
    c, row = cell(d, ctx, parent, position, "Sheet", v, "Equaliser")
    d.cmd("instance.setProperties", ids=[row], properties={P["Label"]: S["equaliser"], P["Supporting"]: summary})
    set_glyph(d, ctx, row, "Sheet/Menu row", "equalizer", on, "Color/primary" if on else "Color/on-surface-variant")
    return c, row


def update_track_menu(d: Doc, ctx: dict):
    C = ctx["components"]
    d.page("Components / Overlays")
    ids = []
    for vn in TRACK_MENU_VARIANTS:
        v = C["Sheet/Track menu"]["variants"][vn]
        ids.append(v)
        g = d.child(v, "Sleep timer group")
        first = d.child(g, "Sleep timer cell")
        d.cmd("instance.setProperties", ids=[first], properties={"Position": "Top"})
        # The switch rebuilds the row's layers from its main, which drops the glyph's colour: give it again.
        running = vn.endswith("Running")
        set_glyph(d, ctx, d.child(first, "Sleep timer"), "Sheet/Menu row", "bedtime", running,
                  "Color/primary" if running else "Color/on-surface-variant")
        menu_row(d, ctx, g, "Bottom", on=True, summary="Bass boost")
    _described(d, "Sheet/Track menu", "Components / Overlays", ctx,
               lambda t: t.replace(TRACK_MENU_OLD, TRACK_MENU_NEW).replace(TRACK_MENU_OLD_2, TRACK_MENU_NEW_2))
    # The sheet is 66dp taller: keep it on the foot of the screens that show it.
    d.page(SCREENS)
    moved = []
    for name in TRACK_MENU_FRAMES:
        for theme in ("Light", "Dark"):
            f = screen_frame(d, name, theme)
            for k in d.get(f, depth=1)["children"]:
                if k.get("type") == "INSTANCE" and k.get("mainComponent") in ids:
                    d.set(k["id"], y=d.get(f)["height"] - d.get(k["id"])["height"])
                    moved.append(f"{name} · {theme}")
    ctx["eq_changed_frames"] = moved

    # The row's four states, as a card of their own.
    p = DocPage.below(d, "Components / Overlays")
    card = p.card("Track menu: the Equaliser row",
                  "The second cell of the track menu's first group over Now Playing, always two lines (64dp): MenuCell "
                  "with CellSymbol(Symbol.Equalizer, on). Its second line is what is playing: Off; the default's preset "
                  "(a saved preset's name, or Custom); \"This song · \" or \"This album · \" and the preset when the song "
                  "or its album has a setting of its own. Off wins over scope. The glyph is filled in primary while the "
                  "equaliser is on. Tapping the row replaces the menu with the equaliser sheet, as Sleep timer does. "
                  "These are Sheet/Menu row instances (Lead=Icon on or Icon, Lines=Two) in List group/Cell.",
                  "Compose: ui/expressive/ExpressiveSheets.kt · ExpressiveTrackMenu")
    states = [(False, "Off"), (True, "Bass boost"), (True, "This song · Custom"), (True, "This album · Vocal")]
    for theme in ("Light", "Dark"):
        box = d.frame(card, name="Dark preview" if theme == "Dark" else "States", layout="v", gap=12, pad=24,
                      fill="Color/surface-container-low", radius=12, sizing=("hug", "hug"))
        d.set_mode(box, "Color", "Cover")
        if theme == "Dark":
            for m in d.mode_collections:
                d.set_mode(box, m, "Dark")
            d.text(box, "Dark", style="Doc/Label", color="Color/on-surface-variant", name="Mode label")
        for on, summary in states:
            c, _ = menu_row(d, ctx, box, "Single", on=on, summary=summary)
            d.set(c, sizingH="fixed", width=379, name=f"Equaliser · {summary}")
    p.done(card)


def update_setting_row(d: Doc, ctx: dict):
    """List row/Setting gains Status line=Plain (the code's Status()) and Trailing=Icon (a row that leaves the app)."""
    C = ctx["components"]
    d.page("Components / Lists and cards")
    info, P = C["List row/Setting"], C["List row/Setting"]["props"]
    preview = top_named(d, card_of(d, "Components / Lists and cards", info["id"]), "Dark preview")

    def add(base: dict, prop: str, value: str) -> int:
        before = {k["id"] for k in d.get(info["id"], depth=1)["children"]}
        d.cmd("component.addVariant", id=variant(ctx, "List row/Setting", **base), property=prop, value=value)
        new = [k["id"] for k in d.get(info["id"], depth=1)["children"] if k["id"] not in before][0]
        name = vname({**{k.replace("_", " "): v for k, v in base.items()}, prop: value})
        d.set(new, name=name)
        info["variants"][name] = new
        return new

    base = dict(Leading="Icon", Lines="Two", Trailing="None", Status_line="False")
    plain = add({**base, "Status_line": "True"}, "Status line", "Plain")
    box = d.child(plain, "Status box")
    d.bind(box, "paddingTop", None)
    d.set(box, padding=[0, 0, 0, 0])
    d.set(d.child(box, "Status"), fill=V("Color/on-surface-variant"), characters=S["own_summary"])
    d.set(d.child(plain, "Title"), characters=S["equaliser"])
    d.set(d.child(plain, "Summary"), characters="Bass boost")
    pv = d.instance(plain, preview)
    d.cmd("instance.setProperties", ids=[pv], properties={
        P["Title"]: S["equaliser"], P["Summary"]: "Bass boost", P["Status text"]: S["own_summary"],
        P["Icon"]: d.comp["Icon/equalizer"]})

    icon = add(base, "Trailing", "Icon")
    box = frame(d, icon, "Trailing", layout="v", pad=[10, 0, 0, 0], sizing=("hug", "hug"))
    d.icon(box, "open_in_new", size=20, color="Color/on-surface-variant", name="Trailing icon")
    bind_dim(d, box, "paddingTop", "space/10")
    d.set(d.child(icon, "Title"), characters=S["phone"])
    d.set(d.child(icon, "Summary"), characters=S["phone_body"])
    pv = d.instance(icon, preview)
    d.cmd("instance.setProperties", ids=[pv], properties={P["Title"]: S["phone"], P["Summary"]: S["phone_body"],
                                                          P["Icon"]: d.comp["Icon/smartphone"]})
    _described(d, "List row/Setting", "Components / Lists and cards", ctx, lambda t: t + SETTING_NOTE)


# ───────────────────────────────────────────────────────────────── screens
SRC_SHEET = f"ui/expressive/{EQ} · ExpressiveEqualiserSheet"
SRC_PAGE = f"ui/expressive/{EQ} · ExpressiveEqualiserPage"
SRC_MENU = "ui/expressive/ExpressiveSheets.kt · ExpressiveTrackMenu"
SRC_NP = "ui/expressive/ExpressiveNowPlaying.kt · ExpressiveNowPlaying (the indicator is in Header)"
SRC_SETTINGS = "ui/expressive/ExpressiveSettings.kt · ExpressiveSettings (EqualiserRow)"
SHEET_NOTE = (f"Mock: {MOCK}. The mock ends the sheet at the screen's foot. On a phone ModalBottomSheet pads its content "
              "by the navigation bar (BottomSheetDefaults.windowInsets), as drawn here: the sheet is 24dp taller.")
CHIPS_NOTE = (f"Mock: {MOCK}. The mock draws the preset row without saved presets. The demo library has two (Car "
              "speakers and Night, low), which come before the built-in ones, so the row opens scrolled: the chosen "
              "chip second, the chip before it first.")
DIALOG_NOTE = (f"Mock: {MOCK}. The mock shows a name typed and the keyboard up. The code opens the dialog with the "
               "field empty and Save disabled until there is a name no other preset has; the keyboard is Android's.")
DIFFERS = "Code differs from mock"
PROPOSED = "Proposed"
# What 1.2.0 draws that feature/per-song-equaliser does not do yet. A frame with any of these is not marked ready.
P_CHIP = ("The selected preset chip is filled with primary, its tick and label on primary. The code draws it tonal "
          "(secondary container), which is two tones from the group cell in Light and reads as no fill.")
P_CHIP_ROW = ("The chip row opens with the chip before the selected one 8dp in, so no third chip shows at the cell's "
              "edge. The code leaves 4dp of it showing.")
P_SCOPE = ("The scope cell of a song's or an album's own setting is primary container, with both lines on primary "
           "container. The code changes only the 40dp lead.")
P_ACTIONS = ("Under the editor every state is one button group and one centred caption line (Equaliser/Actions), 82dp "
             "in all, so the sheet keeps its height when a setting is saved or removed. The code draws a save row and "
             "a note, or an outlined button with its consequence beside it, and the sheet's height changes with the "
             "scope.")
P_CAPTION = ("The note under the save buttons is centred, as every caption of Equaliser/Actions is. The code starts "
             "it 24dp in.")
PROPOSALS: dict[str, list[str]] = {}  # The code followed 1.2.0 on feature/per-song-equaliser; nothing is ahead of it.


class Screens:
    """Builds the Equaliser section from copies of frames the file already has."""

    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.C = ctx["components"]
        self.area = d.area(SECTION, note=(
            "The equaliser: ten bands and presets, with a setting a song or an album can keep. Settings > Playback "
            "has its row, which opens the page that edits the default and lists what has a setting of its own. Over "
            "Now Playing the track menu's Equaliser row and the header's indicator open the sheet, which edits "
            "whichever setting the song is playing with: the default, its album's, or its own. The curves are the "
            "engine's real response to the demo's settings (EqPresets.kt, EqualiserDemo.kt)."))
        self.notes: dict[str, list[tuple[str, str]]] = {}

    def copy(self, key: str, name: str, source: str, of: int, *, h: float = 914) -> int:
        """A copy of frame `of` as the next Light screen of the section, without its handoff marks and links."""
        d, a = self.d, self.area
        fid = d.cmd("edit.duplicate", ids=[of], dx=0, dy=0)["ids"][0]
        d.cmd("node.reparent", ids=[fid], parent=d.pages[SCREENS])
        d.set(fid, name=f"{name} · Light", x=a.cursor, y=a.y0 + a.PAD + a.TITLE, devStatus=None)
        if d.cmd("annotation.list", ids=[fid]).get("annotations"):
            d.cmd("annotation.remove", id=fid)
        self.unlink(fid)
        d.screens[key] = {"light": fid, "dark": None, "map": {}, "name": name, "source": source, "page": SCREENS,
                          "note": None, "status": "ready", "h": h}
        a.cursor += 411 + a.GAP_X
        a.keys.append(key)
        a.row_h = max(a.row_h, h)
        return fid

    def unlink(self, root: int):
        """Drop the prototype links a copy brought with it: they lead out of the copied frame's own flow."""
        d = self.d
        tree = d.get(root, depth=60, fields=["interactions"])
        ids, stack = [], [tree]
        while stack:
            n = stack.pop()
            if n.get("interactions"):
                ids.append(n["id"])
            stack.extend(n.get("children") or [])
        if ids:
            d.cmd("proto.interaction.clear", ids=ids)

    def note(self, key: str, text: str, category: str = DIFFERS):
        self.notes.setdefault(key, []).append((category, text))

    # ---- Settings ------------------------------------------------------------------------------------------------
    def settings_playback(self):
        d, ctx = self.d, self.ctx
        f = self.copy("eq_settings_playback", "Equaliser / Settings, Playback", SRC_SETTINGS,
                      screen_frame(d, "Settings / Top"))
        lst = top_named(d, f, "Settings list")
        names = [k["name"] for k in d.get(lst, depth=1)["children"]]
        at = names.index("Section Lyrics")
        sec = d.instance("Settings section", lst, name="Section Playback")
        d.cmd("instance.setProperties", ids=[sec], properties={self.C["Settings section"]["props"]["Label"]: S["playback"]})
        d.cmd("node.reorder", id=sec, index=at)
        g = frame(d, lst, "Playback group", layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        d.cmd("node.reorder", id=g, index=at + 1)
        setting_row(d, ctx, g, "Top", "Volume levelling", title=S["volume"], icon="volume_up", summary=S["volume_body"],
                    trailing="Switch on")
        setting_row(d, ctx, g, "Middle", "Crossfade", title=S["crossfade"], icon="transition_fade",
                    summary=S["crossfade_off"], status=S["crossfade_body"])
        eq, _ = setting_row(d, ctx, g, "Bottom", "Equaliser", title=S["equaliser"], icon="equalizer",
                            summary="Bass boost", status=S["own_summary"])
        # Scrolled until Playback is under the bar, which has taken the title.
        top = d.get(sec)["y"]
        d.set(lst, y=104 - top)
        bar = top_named(d, f, "App bar")
        P = self.C["Top app bar/Detail"]["props"]
        d.cmd("instance.setProperties", ids=[bar], properties={"Surface": "Scrolled", P["Title"]: "Settings",
                                                               P["Show action"]: False})
        d.set(d.child(bar, "Title box"), visible=True)
        self.triggers["settings_row"] = eq
        self.triggers["settings_back"] = d.child(bar, "Back")
        self.triggers["settings_mini"] = top_named(d, f, "Mini player")

    def page_list(self, lst: int, *, empty: bool):
        """Settings > Equaliser's items under the large title."""
        d, ctx, C = self.d, self.ctx, self.C

        def group(name):
            return frame(d, lst, name, layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))

        def section(label):
            s = d.instance("Settings section", lst, name=f"Section {label}")
            d.cmd("instance.setProperties", ids=[s], properties={C["Settings section"]["props"]["Label"]: label})
            d.set(s, sizingH="fill")

        setting_row(d, ctx, group("Use equaliser group"), "Single", "Use equaliser", title=S["use"], icon="equalizer",
                    trailing="Switch on")
        section(S["default"])
        g = group("Default group")
        scope_row(d, ctx, g, "Default", "Default")
        cell(d, ctx, g, "Bottom", "Default", variant(ctx, "Equaliser/Editor", Curve="Bass boost", State="On"), "Editor")
        if empty:
            section(S["own_none_title"])
            _, row = cell(d, ctx, group("Nothing saved group"), "Single", "Default", d.comp["List row/Setting note"],
                          "Nothing saved")
            d.cmd("instance.setProperties", ids=[row],
                  properties={C["List row/Setting note"]["props"]["Text"]: S["own_none"]})
        else:
            P = C["Equaliser/Own setting row"]["props"]
            for label, kind, rows in ((S["own_albums"], "Album", OWN_ALBUMS), (S["own_songs"], "Song", OWN_SONGS)):
                section(label)
                g = group(f"{kind}s group")
                for i, (title, artist, cover, preset) in enumerate(rows):
                    pos = "Top" if i == 0 else "Bottom" if i == len(rows) - 1 else "Middle"
                    _, row = cell(d, ctx, g, pos, "Default", variant(ctx, "Equaliser/Own setting row", Kind=kind), title)
                    d.cmd("instance.setProperties", ids=[row], properties={P["Title"]: title, P["Subtitle"]: artist})
                    d.cmd("instance.setProperties", ids=[d.child(row, "Preset")],
                          properties={C["List row/Count"]["props"]["Count"]: preset})
                    set_cover(d, row, cover)
        spacer(d, lst, "Group spacing", w=411, h=24)
        setting_row(d, ctx, group("Phone equaliser group"), "Single", "Phone equaliser", title=S["phone"],
                    icon="smartphone", summary=S["phone_body"], trailing="Icon")

    def page(self, key: str, name: str, *, empty: bool = False, full: bool = False) -> int:
        d = self.d
        f = self.copy(key, name, SRC_PAGE, screen_frame(d, "Licences / List"))
        lst = top_named(d, f, "Licences list")
        d.set(lst, name="Equaliser list")
        d.set(d.child(top_named(d, lst, "Large title"), "Title"), characters=S["equaliser"])
        d.cmd("edit.delete", ids=[top_named(d, lst, "Notices")])
        self.page_list(lst, empty=empty)
        if full:
            # The whole page: the list's end is 24dp above the mini player (16 of content padding, 8 of gap).
            mini_y = 104 + d.get(lst)["height"] + 24
            h = mini_y + 64 + 8 + 88
            d.set(f, height=h)
            d.set(top_named(d, f, "Mini player"), y=mini_y)
            d.set(top_named(d, f, "Navigation bar"), y=h - 88)
            d.set(top_named(d, f, "Gesture bar"), y=h - 24)
            d.screens[key]["h"] = h
            self.area.row_h = max(self.area.row_h, h)
        self.triggers[f"{key}_back"] = d.child(top_named(d, f, "App bar"), "Back")
        return f

    # ---- Now Playing ---------------------------------------------------------------------------------------------
    def show_indicator(self, f: int, scope: str = "Song") -> int:
        d = self.d
        hdr = d.child(top_named(d, f, "Player"), "Header")
        d.cmd("instance.setProperties", ids=[hdr],
              properties={self.C["Now playing header"]["props"]["Show equaliser"]: True})
        ind = d.child(hdr, "Equaliser indicator")
        if scope != "Song":
            d.cmd("instance.setProperties", ids=[ind], properties={"Scope": scope})
        return ind

    def now_playing(self):
        d = self.d
        f = self.copy("eq_now_playing", "Equaliser / Now Playing, indicator", SRC_NP, screen_frame(d, "Now Playing / Playing"))
        self.triggers["np_indicator"] = self.show_indicator(f)
        hdr = d.child(top_named(d, f, "Player"), "Header")
        self.triggers["np_more"] = d.child(hdr, "More")
        self.triggers["np_close"] = d.child(hdr, "Close")

    def track_menu(self):
        d = self.d
        f = self.copy("eq_track_menu", "Equaliser / Track menu, song's own setting", SRC_MENU,
                      screen_frame(d, "Track menu / From Now Playing"))
        self.show_indicator(f)
        row = d.child(f, "Equaliser")
        d.set(d.child(row, "Supporting"), characters="This song · Custom")
        self.triggers["menu_row"] = row
        self.triggers["menu_scrim"] = top_named(d, f, "Scrim")
        self.note("eq_track_menu", SHEET_NOTE.replace("the sheet is 24dp taller", "the menu is 24dp taller"))

    def sheet(self, key: str, name: str, *, scope: str, state: str, of: int | None = None) -> tuple[int, int]:
        """The equaliser sheet over Now Playing. Returns (frame, sheet instance)."""
        d = self.d
        f = self.copy(key, name, SRC_SHEET, of or screen_frame(d, "Track menu / From Now Playing"))
        if of is None:
            d.cmd("edit.delete", ids=[top_named(d, f, "Track menu")])
            if scope != "Default":
                self.show_indicator(f, scope)
            sh = inst(d, "Sheet/Equaliser", f, name="Equaliser sheet", Scope=scope, State=state)
            d.set(sh, x=0, y=914 - d.get(sh)["height"])
        else:
            sh = top_named(d, f, "Equaliser sheet")
        self.note(key, SHEET_NOTE)
        if state != "Off" and scope != "Song":
            self.note(key, CHIPS_NOTE)
        return f, sh

    def sheets(self):
        d, T = self.d, self.triggers
        f, sh = self.sheet("eq_sheet_default", "Equaliser / Sheet, default", scope="Default", state="On")
        T["default_switch"], T["default_save_song"] = d.child(sh, "Switch"), d.child(sh, S["save_song"])
        T["default_scrim"] = top_named(d, f, "Scrim")
        f, sh = self.sheet("eq_sheet_song", "Equaliser / Sheet, song's own setting", scope="Song", state="On")
        song = f
        T["song_save_preset"], T["song_remove"] = d.child(sh, f"Chip {S['save_preset']}"), d.child(sh, "Remove")
        T["song_scrim"] = top_named(d, f, "Scrim")
        f, sh = self.sheet("eq_sheet_album", "Equaliser / Sheet, album's setting", scope="Album", state="On")
        T["album_save_song"], T["album_remove"] = d.child(sh, S["save_song"]), d.child(sh, "Remove")
        T["album_scrim"] = top_named(d, f, "Scrim")
        f, sh = self.sheet("eq_sheet_off", "Equaliser / Sheet, off", scope="Default", state="Off")
        T["off_switch"], T["off_scrim"] = d.child(sh, "Switch"), top_named(d, f, "Scrim")
        f, sh = self.sheet("eq_sheet_dragging", "Equaliser / Sheet, band dragged", scope="Song", state="Band dragged")
        T["dragging_scrim"] = top_named(d, f, "Scrim")
        self.note("eq_sheet_dragging",
                  "The 4 kHz band on its way from +2 to +3 dB: the value follows the finger from where it is, snapping "
                  "to 1 dB, and the response follows. A tap without movement changes nothing; a double tap sets the "
                  "band to 0.", "Interaction")

        # Save as preset: the name dialog over the song's sheet.
        f, sh = self.sheet("eq_save_preset", "Equaliser / Save as preset", scope="Song", state="On", of=song)
        inst(d, "Scrim", f, name="Dialog scrim", Kind="Dialog", x=0, y=0)
        dlg = inst(d, "Dialog", f, name="Dialog", Content="Text field", Buttons="Two", Confirm="Disabled")
        d.cmd("instance.setProperties", ids=[dlg], properties={self.C["Dialog"]["props"]["Title"]: S["save_preset"]})
        d.cmd("instance.setProperties", ids=[d.child(dlg, "Text field")],
              properties={self.C["Text field"]["props"]["Label"]: S["preset_name"]})
        btn = self.C["Button"]["props"]["Label"]
        d.cmd("instance.setProperties", ids=[d.child(dlg, "Confirm")], properties={btn: S["save"]})
        d.cmd("instance.setProperties", ids=[d.child(dlg, "Dismiss")], properties={btn: S["cancel"]})
        d.set(dlg, x=0, y=dialog_y(d.get(dlg)["height"]))
        T["dialog_cancel"] = d.child(dlg, "Dismiss")
        self.notes["eq_save_preset"] = [(DIFFERS, SHEET_NOTE), (DIFFERS, DIALOG_NOTE)]

        # A saved preset's menu, anchored under the chip's start.
        f, sh = self.sheet("eq_preset_menu", "Equaliser / Preset menu, rename or delete", scope="Default",
                           state="Saved preset")
        chip = d.child(sh, "Chip Car speakers")
        cb = d.get(chip)["absoluteBounds"]
        fb = d.get(f)["absoluteBounds"]
        m = d.instance("Menu", f, name="Menu", x=cb["x"] - fb["x"], y=cb["y"] - fb["y"] + cb["height"] + 4)
        item = d.child(m, "Delete playlist")
        d.set(item, name="Delete preset")
        d.set(d.child(item, "Label"), characters=S["delete_preset"])
        T["menu_scrim2"] = top_named(d, f, "Scrim")
        self.notes["eq_preset_menu"] = [
            (DIFFERS, SHEET_NOTE),
            ("Interaction", "Opens from a second tap on the selected saved chip, or a long press on any saved chip. "
                            "Rename opens the name dialog titled \"Rename preset\", prefilled; Delete preset asks "
                            "\"Delete Car speakers?\" and says songs and albums saved with it keep their sound and "
                            "show as Custom. Built-in chips have no menu.")]

    def build(self):
        self.triggers: dict[str, int] = {}
        self.d.page(SCREENS)
        self.settings_playback()
        self.page("eq_page", "Equaliser / Settings page")
        self.page("eq_page_full", "Equaliser / Settings page (full length)", full=True)
        self.page("eq_page_empty", "Equaliser / Settings page, nothing saved", empty=True)
        self.now_playing()
        self.track_menu()
        self.sheets()
        self.area.close()
        d = self.d
        for key in self.area.keys:
            s = d.screens[key]
            for c in d.mode_collections:          # Light follows the default, so the theme switch can turn it Dark
                d.set_mode(s["light"], c, None)
            for theme in ("light", "dark"):
                d.annotate(s[theme], f"Compose: {s['source']}", "Source")
                for category, text in self.notes.get(key, []):
                    d.annotate(s[theme], text, category)
                for text in PROPOSALS.get(key, []):
                    d.annotate(s[theme], text, PROPOSED)
                if key not in PROPOSALS:
                    d.ready_for_dev(s[theme])
        self.ctx["eq_triggers"] = self.triggers
        self.ctx["eq_screens"] = list(self.area.keys)


# ───────────────────────────────────────────────────────────────── prototype
FLOW = ("eq_settings_playback", "12 Equaliser",
        "Settings, Playback: the Equaliser row opens the page that edits the default. The mini player opens Now Playing, "
        "where the song has its own setting: the indicator opens the sheet, and More opens the track menu with its "
        "Equaliser row. Use default goes to the default's sheet; its switch turns the equaliser off and on.")


def wire(d: Doc, ctx: dict):
    T = ctx["eq_triggers"]
    made = []

    def add(layer: int, dest: int | None, how: str | None):
        params = {"id": layer, "trigger": {"type": "onClick"}}
        if dest is None:
            params["action"] = {"type": "back"}
        else:
            t = TRANSITIONS[how]
            params["action"] = {"type": "navigate", "dest": dest}
            params["transition"] = {"type": t["transition"], **({"direction": t["direction"]} if "direction" in t else {})}
            params["durationMs"] = t["duration"]
            params["easing"] = t["easing"]
        d.cmd("proto.interaction.add", **params)

    def link(src: str, trigger: str, dst: str | None, how: str | None = None, label: str = ""):
        s = d.screens[src]
        layer = T[trigger]
        for theme in ("light", "dark"):
            lay = layer if theme == "light" else s["map"].get(layer)
            if lay is None:
                raise KeyError(f"{src}: no Dark layer for {trigger}")
            dest = None if dst is None else d.screens[dst][theme] if dst in d.screens else screen_frame(d, dst, theme.title())
            add(lay, dest, how)
        made.append((d.screens[src]["name"], label, dst or "back"))

    link("eq_settings_playback", "settings_row", "eq_page", "detail", "Tap Equaliser")
    link("eq_settings_playback", "settings_mini", "eq_now_playing", "overlay", "Tap the mini player")
    link("eq_settings_playback", "settings_back", None)
    for key in ("eq_page", "eq_page_full", "eq_page_empty"):
        link(key, f"{key}_back", None)
    link("eq_now_playing", "np_indicator", "eq_sheet_song", "overlay", "Tap the indicator")
    link("eq_now_playing", "np_more", "eq_track_menu", "overlay", "Tap More")
    link("eq_now_playing", "np_close", None)
    link("eq_track_menu", "menu_row", "eq_sheet_song", "overlay", "Tap Equaliser")
    link("eq_track_menu", "menu_scrim", "eq_now_playing", "fade", "Tap outside")
    link("eq_sheet_song", "song_save_preset", "eq_save_preset", "fade", "Tap Save as preset")
    link("eq_sheet_song", "song_remove", "eq_sheet_default", "fade", "Tap Use default")
    link("eq_sheet_song", "song_scrim", "eq_now_playing", "fade", "Tap outside")
    link("eq_save_preset", "dialog_cancel", "eq_sheet_song", "fade", "Tap Cancel")
    link("eq_sheet_default", "default_switch", "eq_sheet_off", "fade", "Switch off")
    link("eq_sheet_default", "default_save_song", "eq_sheet_song", "fade", "Tap Save for this song")
    link("eq_sheet_default", "default_scrim", "Now Playing / Playing", "fade", "Tap outside")
    link("eq_sheet_off", "off_switch", "eq_sheet_default", "fade", "Switch on")
    link("eq_sheet_off", "off_scrim", "Now Playing / Playing", "fade", "Tap outside")
    link("eq_sheet_album", "album_save_song", "eq_sheet_song", "fade", "Tap Save for this song")
    link("eq_sheet_album", "album_remove", "eq_sheet_default", "fade", "Tap Use default")
    for trigger, key in (("album_scrim", "eq_sheet_album"), ("dragging_scrim", "eq_sheet_dragging"),
                         ("menu_scrim2", "eq_preset_menu")):
        link(key, trigger, None)
    # The track menus the file already had: their new Equaliser row opens the default's sheet.
    for name in TRACK_MENU_FRAMES[:2]:
        for theme in ("light", "dark"):
            row = d.child(screen_frame(d, name, theme.title()), "Equaliser")
            add(row, d.screens["eq_sheet_default"][theme], "overlay")
        made.append((name, "Tap Equaliser", "eq_sheet_default"))
    key, name, desc = FLOW
    start = d.screens[key]["light"]
    d.cmd("proto.flow.add", start=start, name=name)
    d.cmd("proto.flow.rename", start=start, name=name, description=desc)
    ctx["eq_links"] = made


# ───────────────────────────────────────────────────────────────── Flow overview, Cover, Changelog
CARD_W, CARD_H, PX, PY, MARGIN, LABEL_ZONE = 360, 176, 1000, 300, 120, 270
ROW0 = 15                      # the first free row under the diagram's cards
# key, column, row, area colour, caption
FLOW_NODES = [
    ("eq_settings_playback", 2, ROW0, "settings", "Flow 12 starts here"),
    ("eq_page", 3, ROW0, "settings", "Edits the default; lists own settings"),
    ("eq_now_playing", 2, ROW0 + 1, "player", "The song has its own setting"),
    ("eq_sheet_song", 3, ROW0 + 1, "sheets", None),
    ("eq_sheet_default", 4, ROW0 + 1, "sheets", None),
    ("eq_sheet_off", 5, ROW0 + 1, "sheets", None),
    ("eq_track_menu", 2, ROW0 + 2, "sheets", None),
    ("eq_save_preset", 3, ROW0 + 2, "sheets", None),
]
# src, dst, label
FLOW_EDGES = [
    ("eq_settings_playback", "eq_page", "Tap Equaliser"),
    ("eq_settings_playback", "eq_now_playing", "Tap the mini player"),
    ("eq_now_playing", "eq_sheet_song", "Tap the indicator"),
    ("eq_now_playing", "eq_track_menu", "Tap More"),
    ("eq_track_menu", "eq_sheet_song", "Tap Equaliser"),
    ("eq_sheet_song", "eq_sheet_default", "Use default"),
    ("eq_sheet_default", "eq_sheet_off", "Switch off"),
    ("eq_sheet_song", "eq_save_preset", "Save as preset"),
]
AREA_VAR = {"settings": "flow/settings", "player": "flow/player", "sheets": "flow/sheets"}


def update_overview(d: Doc, ctx: dict):
    """An Equaliser band under the diagram's cards, and an arrow from the track menu over Now Playing."""
    page = d.page("Flow overview")
    dia = top_named(d, page, "Diagram")
    g = d.get(dia)
    rows = ROW0 + 3
    H = MARGIN * 2 + 80 + (rows - 1) * PY + CARD_H
    d.set(dia, height=H)
    arrows, labels = top_named(d, dia, "Arrows"), top_named(d, dia, "Action labels")
    d.set([arrows, labels], height=H)

    def box(c, r):
        x, y = MARGIN + c * PX, MARGIN + 80 + r * PY
        return {"x": x, "y": y, "r": x + CARD_W, "b": y + CARD_H, "cx": x + CARD_W / 2, "cy": y + CARD_H / 2}

    pos = {k: box(c, r) for k, c, r, _, _ in FLOW_NODES}
    # The band, under the arrows.
    x0, y0 = MARGIN + 2 * PX - 32, MARGIN + 80 + ROW0 * PY - 76
    x1, y1 = MARGIN + 5 * PX + CARD_W + 32, MARGIN + 80 + (ROW0 + 2) * PY + CARD_H + 28
    band = d.frame(dia, name="Area · Equaliser", x=x0, y=y0, w=x1 - x0, h=y1 - y0, fill="Canvas/section", radius=24)
    d.rect(band, name="Area colour", x=0, y=0, w=x1 - x0, h=6, fill="Canvas/flow/sheets")
    d.text(band, "Equaliser", style="Doc/Heading", color="Canvas/label", name="Title", x=32, y=22)
    kids = [k["id"] for k in d.get(dia, depth=1)["children"]]
    d.cmd("node.reorder", id=band, index=kids.index(arrows))

    def arrow(name, pts, label, at):
        path = "M " + " L ".join(f"{x:.1f} {y:.1f}" for x, y in pts)
        v = d.vector(arrows, name=f"Arrow · {name}", path=path, stroke="Canvas/label-muted", stroke_w=3)
        d.set(v, endCap="triangleArrow", strokeJoin="round")
        p = d.frame(labels, name=f"Label · {label}", layout="h", pad=[4, 14, 4, 14], fill="Canvas/card",
                    stroke="Canvas/divider", stroke_w=1, radius=19, sizing=("hug", "hug"))
        d.text(p, label, style="Doc/Body", color="Canvas/label", name="Action")
        b = d.get(p)
        d.set(p, x=round(at[0] - b["width"] / 2), y=round(at[1] - b["height"] / 2))

    for src, dst, label in FLOW_EDGES:
        s, t = pos[src], pos[dst]
        name = f"{src} to {dst}"
        if abs(s["cx"] - t["cx"]) < 1:                       # straight down
            arrow(name, [(s["cx"], s["b"]), (t["cx"], t["y"])], label, (s["cx"], (s["b"] + t["y"]) / 2))
        elif abs(s["cy"] - t["cy"]) < 1:                     # straight across; a card with two arrivals takes this one high
            y = t["y"] + CARD_H / 3 if dst == "eq_sheet_song" else t["cy"]
            arrow(name, [(s["r"], y), (t["x"], y)], label, (t["x"] - LABEL_ZONE / 2, y))
        else:                                                # up and across, through the gutter
            lane, y = t["x"] - LABEL_ZONE - 48, t["y"] + CARD_H * 2 / 3
            arrow(name, [(s["r"], s["cy"]), (lane, s["cy"]), (lane, y), (t["x"], y)], label, (t["x"] - LABEL_ZONE / 2, y))
    # From the track menu the file already had (column 4, row 8), down the gutter beside it.
    tm, dst = box(4, 8), pos["eq_sheet_default"]
    lane, turn = tm["r"] + 300, dst["y"] - 50
    arrow("sheet_track_menu_now_playing to eq_sheet_default",
          [(tm["r"], tm["y"] + CARD_H * 0.9), (lane, tm["y"] + CARD_H * 0.9), (lane, turn), (dst["cx"], turn),
           (dst["cx"], dst["y"])], "Tap Equaliser", (lane, turn - 90))

    for k, c, r, area, caption in FLOW_NODES:
        b = pos[k]
        card = d.frame(dia, name=f"Card · {k}", x=b["x"], y=b["y"], w=CARD_W, h=CARD_H, layout="h", fill="Canvas/card",
                       stroke="Canvas/divider", stroke_w=2, radius=18, clip=True)
        stripe = d.rect(card, name="Area colour", w=12, h=CARD_H, fill=f"Canvas/{AREA_VAR[area]}")
        d.set(stripe, sizingV="fill")
        col = d.frame(card, name="Card text", layout="v", gap=6, pad=[18, 20, 18, 18], sizing=("fill", "fill"))
        t = d.text(col, d.screens[k]["name"], style="Doc/Heading", color="Canvas/label", name="Screen")
        d.set(t, sizingH="fill", textAutoResize="height", maxLines=2)
        d.text(col, k, style="Doc/Label", color="Canvas/label-muted", name="Key")
        if caption:
            cp = d.text(col, caption, style="Doc/Label", color="Canvas/label", name="Note")
            d.set(cp, sizingH="fill", textAutoResize="height", maxLines=2)
    kids = [k["id"] for k in d.get(dia, depth=1)["children"]]
    d.cmd("node.reorder", id=labels, index=len(kids) - 1)
    ctx["eq_overview"] = {"cards": len(FLOW_NODES), "arrows": len(FLOW_EDGES) + 1}


def counts(d: Doc) -> dict:
    """What the Cover's Read me and the Changelog count, read from the file."""
    names = [c for c in d.cmd("component.list")]
    in_set = set()
    sets = [c for c in names if c["type"] == "COMPONENT_SET"]
    for c in sets:
        in_set |= {k["id"] for k in d.get(c["id"], depth=1).get("children", [])}
    singles = [c for c in names if c["type"] == "COMPONENT" and c["id"] not in in_set]
    icons = [c["name"] for c in singles if c["name"].startswith("Icon/")]
    # As cover.py counted them: not the glyphs, the shapes, or the Changelog's entry template.
    other = [c for c in singles if not c["name"].startswith(("Icon/", "Shape/")) and c["name"] != "Changelog entry"]
    frames = 0
    for sec in d.get(d.pages[SCREENS], depth=1).get("children", []):
        if sec.get("type") == "SECTION" and sec.get("name") != "Outside Attic":
            frames += sum(1 for n in d.get(sec["id"], depth=1).get("children", []) if n.get("name", "").endswith("· Light"))
    return {"components": len(sets) + len(other), "icons": len([n for n in icons if not n.endswith("(filled)")]),
            "filled": len([n for n in icons if n.endswith("(filled)")]), "screens": frames}


def update_cover(d: Doc, ctx: dict):
    page = d.page("Cover")
    c = counts(d)
    label = ctx["components"]["Filter chip"]["props"]["Label"]
    for n in d.find(page, type="INSTANCE", name="Chip · ", limit=20):
        nm = d.get(n)["name"]
        new = (f"Version {VERSION}" if nm.startswith("Chip · Version ") else
               f"Last updated {UPDATED}" if nm.startswith("Chip · Last updated ") else None)
        if new:
            d.cmd("instance.setProperties", ids=[n], properties={label: new})
            d.set(n, name=f"Chip · {new}")
    for t in d.find(page, type="TEXT", name="What", limit=40):
        text = d.get(t)["characters"]
        new = re.sub(r"the \d+ Material Symbols glyphs the app ships, \d+ of them",
                     f"the {c['icons']} Material Symbols glyphs the app ships, {c['filled']} of them", text)
        new = re.sub(r"^\d+ components and sets", f"{c['components']} components and sets", new)
        new = re.sub(r"^\d+ screen states", f"{c['screens']} screen states", new)
        if new != text:
            d.set(t, characters=new)
    ctx["eq_counts"] = c


def update_changelog(d: Doc, ctx: dict):
    page = d.page("Changelog")
    entries = top_named(d, page, "Entries")
    before = d.get(entries)["height"]
    P = ctx["components"]["Changelog entry"]["props"]
    drawn = (
        "The equaliser, drawn from feature/per-song-equaliser: ten bands and presets, with a setting a song or an "
        "album can keep.\n"
        "• Components: Equaliser/Preset chip, Scope cell, Own setting row, Band graph, Sheet title, Editor and Removal "
        "row; Sheet/Equaliser in six states; Now playing/Equaliser indicator.\n"
        "• Changed components: Sheet/Track menu carries Sleep timer and Equaliser in one group over Now Playing and for "
        "a file opened from another app; Now playing header takes the indicator (Show equaliser); List row/Setting "
        "gains Status line=Plain and Trailing=Icon.\n"
        "• Icons: equalizer and transition_fade, and equalizer, album and music_note filled.\n"
        "• Screens: a new Equaliser section of 13 states in Light and Dark: Settings > Playback with its Equaliser row, "
        "Settings > Equaliser (top, whole page, nothing saved), Now Playing with the indicator, the track menu, the "
        "sheet on the default, a song's own setting, an album's, off and with a band dragged, Save as preset, and a "
        "saved preset's menu. The three track menu frames over Now Playing show the new row.\n"
        "• Prototype: flow 12 Equaliser from Settings > Playback; the track menu's Equaliser row and the indicator "
        "open the sheet. The Flow overview has an Equaliser band.")
    reviewed = (
        "The equaliser after a review of the built one (feature/per-song-equaliser at b7e9e90). The ten frames that "
        "show the editor or the sheet were redrawn, and the code was then matched to them on the same branch.\n"
        "• Equaliser/Preset chip: the selected chip is filled with primary. Tonal is kept for the equaliser being off "
        "(new property Equaliser=On|Off). A selected saved preset has 6dp either side of its more glyph, as the code "
        "does.\n"
        "• Equaliser/Scope cell: a song's or an album's own setting turns the whole cell primary container.\n"
        "• New Equaliser/Actions, in place of Equaliser/Removal row and the sheet's loose save row and note: one "
        "button group and one caption line in every state, so Sheet/Equaliser is the same height whatever the scope.\n"
        "• Equaliser/Editor: the chip row opens as the code opens it, the selected chip second, without a sliver of a "
        "third chip at the cell's edge.\n"
        "• Descriptions name the composables the code has: EqualiserTitle, EqualiserIndicator, ownRows and "
        "OwnSetting, PresetChip beside the kit's ExpressiveFilterChip.\n"
        "• Screens: Settings > Playback, Now Playing with the indicator and the track menu are unchanged.")
    for version, changes in (("1.1.0", drawn), (VERSION, reviewed)):
        e = d.instance("Changelog entry", entries, name=f"Entry · {version}")
        d.cmd("instance.setProperties", ids=[e], properties={P["Version"]: version, P["Date"]: UPDATED,
                                                             P["Author"]: AUTHOR, P["Changes"]: changes})
        d.set(e, sizingH="fixed", width=1400)
        d.cmd("node.reorder", id=e, index=0)
    grew = d.get(entries)["height"] - before
    how = top_named(d, page, "Card · Adding a version")
    d.set(how, y=d.get(how)["y"] + grew)


def finish(d: Doc, ctx: dict):
    update_overview(d, ctx)
    update_cover(d, ctx)
    update_changelog(d, ctx)


# ───────────────────────────────────────────────────────────────── build
def build(d: Doc, ctx: dict):
    if not ctx.get("attached"):
        print("    equaliser: skipped in a build from scratch. Run `python3 -m expressive.equaliser OUT.figcraft "
              "OUT-1.1.0.figcraft` on the saved file to add it.", flush=True)
        return
    try:
        screen_frame(d, "Equaliser / Sheet, default")
    except KeyError:
        pass
    else:
        raise SystemExit("equaliser: this file already has the Equaliser section")
    build_icons(d, ctx)
    build_components(d, ctx)
    update_header(d, ctx)
    update_track_menu(d, ctx)
    update_setting_row(d, ctx)
    for page in ("Components / Actions", "Components / Lists and cards", "Components / Playback",
                 "Components / Overlays"):
        restack(d, page)
    Screens(d, ctx).build()
    wire(d, ctx)
    finish(d, ctx)
    d.page("Cover")


def run(src: Path, out: Path, export: Path | None = None, log=None) -> dict:
    """Open `src`, add the equaliser, save as `out` (and export the new frames at the renders' density)."""
    d = Doc("Attic Expressive", read_roots=[src.parent], write_roots=[out.parent], log=log)
    ctx: dict = {"style": "expressive"}
    try:
        d.tool("open_file", path=str(src.resolve()))
        attach(d, ctx)
        build(d, ctx)
        out.parent.mkdir(parents=True, exist_ok=True)
        d.save(out)
        if export:
            for key in ctx["eq_screens"]:
                for theme in ("light", "dark"):
                    d.export_png(d.screens[key][theme], export / f"{key}-{theme}.png", scale=DENSITY)
        return ctx
    finally:
        d.close()


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("src", type=Path)
    ap.add_argument("out", type=Path)
    ap.add_argument("--export", type=Path)
    a = ap.parse_args(argv)
    ctx = run(a.src, a.out, a.export)
    print(f"saved {a.out} ({a.out.stat().st_size // 1024} KB): {len(ctx['eq_screens'])} screens, "
          f"{len(ctx['eq_links'])} links, counts {ctx['eq_counts']}")
    if ctx.get("eq_changed_frames"):
        print("existing frames that follow the track menu:", ", ".join(ctx["eq_changed_frames"]))


if __name__ == "__main__":
    sys.exit(main())
