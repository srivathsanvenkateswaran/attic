"""The equaliser, added to an existing Attic Comfort file (versions 1.1.0 and 1.2.0). Pages: Type, Icons, Components / Actions,
Components / Lists and cards, Components / Playback, Components / Overlays, Screens, Flow overview, Cover, Changelog.

Owner: equaliser design agent. Unlike the other modules this one works on a file that already exists: `attach()`
reads the components, styles, variables and screens out of the opened document, and `build()` adds to them. Run it
on a copy of the file, look at the result, then copy it back:

    cd design/build
    python3 -m comfort.equaliser "../Attic Comfort.figcraft" /tmp/attic-design/comfort-eq.figcraft \
        [--export /tmp/attic-eq/figcraft/comfort]

It refuses a file that already has the equaliser (a `Band slider` component). In MODULES it comes last, so a
deliberate regeneration adds the equaliser after the cover, the flows and the handoff pass.

Run on the 1.0.0 file it gives 1.2.0. Version 1.1.0 drew the response in a 120dp well above bare sliders, as the code
on feature/per-song-equaliser does; 1.2.0 is the design review's redraw, which puts the response behind the sliders in
one plot (`Band sliders`), which the code then followed. PROPOSED would list frames that differ from
the code, and the Changelog gets an entry for each version.

Drawn from /tmp/attic-eq/UI-SPEC.md sections 1 and 3, the mock docs/design/2026-11-21-ui-directions/oneui/11-equaliser.html
(layout only), ui/model/EqualiserModel.kt (state), storeShots EqualiserDemo.kt (demo states), playback/eq/EqPresets.kt
(preset values) and playback/eq/EqDesign.kt (the response, ported in `response()`).

TEXT STYLES (Type page, card "Equaliser styles")
  Component/Equaliser scope title   17/22 bold      EqualiserScopeHead title
  Component/Equaliser axis          11/16 tabular   BandSliders' +12, 0, −12
  Component/Equaliser frequency     12/16           the band labels under BandSliders
  Component/Equaliser value         12.5/16 semibold tabular   the value bubble of a dragged band
  Component/Equaliser indicator     13/18 medium    the Now Playing indicator

ICONS (Icons page)
  Icon/<ligature> for every glyph in Symbols.kt the file lacks (equalizer among them).

COMPONENTS / ACTIONS
  Preset chip        Kind=Built-in|Saved|Save as preset, Selected=False|True (Save as preset: False only). props Label.
                     ComfortEqualiser.kt PresetChip
  Response curve     Curve=Flat|Bass boost|Bass reducer|Treble boost|Vocal|Loudness|Acoustic|Electronic|Hip-hop|
                     Classical|Spoken word|Custom|Car speakers. 339 x 184 at the bands' own scale (6.5dp to a decibel),
                     drawn from EqPresets.kt by the engine's maths.
  Band slider        State=On|Off|Dragged, Gain=Boost|Cut|Zero. props Value (Dragged). One band's knob. Set the height
                     of "Reach" to |gain| x 6.5 + 6. ComfortEqualiser.kt BandSliders (one band)
  Band sliders       State=On|Off|Dragged. The plot: a fill card with the rules and their labels, ten tracks, the nested
                     "Curve" (Response curve, exposed; Off draws the flat line), ten Band slider instances "Band 31 Hz"
                     .. "Band 16 kHz" (exposed) and the frequency labels. ComfortEqualiser.kt BandSliders

COMPONENTS / LISTS AND CARDS
  Settings row       gains Control=Value and chevron (Settings > Playback > Equaliser). ComfortSettings.kt SetRow

COMPONENTS / PLAYBACK
  Equaliser indicator   Scope=Song|Album. NowPlayingParts.kt NowPlayingHeader (its trailing slot)
  Now playing header    gains the nested "Indicator" (exposed) and the property Show equaliser.

COMPONENTS / OVERLAYS
  Equaliser title    On=True|False (False adds the off line). ComfortEqualiser.kt ComfortEqualiserSheet
  Scope head         Scope=Default|Album|Song. props Subtitle; the nested "Cover" is exposed (Album, Song).
                     ComfortEqualiser.kt EqualiserScopeHead
  Sheet row          gains Kind=Two line (End=None) and the property Summary: the removal row. Overlays.kt SheetRow

SCREENS (section "Equaliser", Light above Dark, keys in KEYS)
  Track menu; Sheet, default; Sheet, this song; Sheet, this album; Sheet, off; Save as preset; Saved preset menu;
  Sheet, band dragged; Now Playing, this song; Settings, Playback; Settings page; Settings page (full length);
  Settings page, nothing saved; Settings page, off.
  The five existing track menus over Now Playing gain the Equaliser row after Sleep timer (UI-SPEC 3.1).

PROTOTYPE
  Track menu (existing and new) Equaliser -> Sheet, default; the indicator -> Sheet, this song; the sheet's save,
  removal, switch, Save as preset and saved-chip rows between its states; Settings, Playback's row -> Settings page.
  Flow starting points "Equaliser" and "Equaliser in Settings". Flow overview: the lane "Equaliser".

HANDOFF
  A Source annotation on every new frame; Ready for development on the frames that match the code and a Proposed
  annotation on the ones in PROPOSED; Cover 1.2.0; the Changelog entries for 1.1.0 and 1.2.0.
"""
from __future__ import annotations

import argparse
import json
import math
import re
import sys
import zipfile
from pathlib import Path

from figbuild import icons as _icons
from figbuild.kit import Area, Doc, DENSITY

from .components_core import COVER, Components, K, SetBuilder, _props, cover_image
from .components_states import block, group, inst, row, setp
from .foundations import ICON_DROP, MODE_COLLECTIONS, WEIGHT_NAME, text_style

VERSION = "1.2.0"
UPDATED = "2026-11-25"
SHEET_SRC = "ui/comfort/ComfortEqualiser.kt · ComfortEqualiserSheet"
PAGE_SRC = "ui/comfort/ComfortEqualiser.kt · ComfortEqualiserPage"
MENU_SRC = "ui/comfort/ComfortSheets.kt · ComfortTrackMenu"
NP_SRC = "ui/comfort/ComfortNowPlaying.kt · ComfortNowPlaying"
SETTINGS_SRC = "ui/comfort/ComfortSettings.kt · ComfortSettings"
WASH = "Tinted wash"
W, H, NAV = 411, 914, 24

# ============================================================================== the engine's numbers
LABELS = ["31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"]
BAND_NAMES = ["31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz", "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz"]
FREQS = [31.25 * 2 ** i for i in range(10)]
STEP_DB = 0.5


# playback/eq/EqPresets.kt, in its order; Kotlin's roundToInt rounds halves up, which `_kround` does.
def _kround(x: float) -> int:
    return math.floor(x + 0.5)


def _on_grid(*gains):
    return [_kround(g / STEP_DB) * STEP_DB for g in gains]


PRESETS = {
    "Flat": _on_grid(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    "Bass boost": _on_grid(5.5, 4.25, 3.5, 2.5, 1.25, 0, 0, 0, 0, 0),
    "Bass reducer": _on_grid(-5.5, -4.25, -3.5, -2.5, -1.25, 0, 0, 0, 0, 0),
    "Treble boost": _on_grid(0, 0, 0, 0, 0, 1.25, 2.5, 3.5, 4.25, 5.5),
    "Vocal": _on_grid(-1.5, -3, -3, 1.5, 3.75, 3.75, 3, 1.5, 0, -1.5),
    "Loudness": _on_grid(6, 4, 0, 0, -2, 0, -1, -5, 5, 1),
    "Acoustic": _on_grid(5, 4.9, 3.95, 1.05, 2.15, 1.75, 3.5, 4.1, 3.55, 2.15),
    "Electronic": _on_grid(4.25, 3.8, 1.2, 0, -2.15, 2.25, 0.85, 1.25, 3.95, 4.8),
    "Hip-hop": _on_grid(5, 4.25, 1.5, 3, -1, -1, 1.5, -0.5, 2, 3),
    "Classical": _on_grid(4.75, 3.75, 3, 2.5, -1.5, -1.5, 0, 2.25, 3.25, 3.75),
    "Spoken word": _on_grid(-3.46, -0.47, 0, 0.69, 3.46, 4.61, 4.84, 4.28, 2.54, 0),
}
BUILT_IN = list(PRESETS)
# storeShots EqualiserDemo.kt: the listener's own curve and the two presets they saved.
CUSTOM = [3, 4, 2.5, 0.5, -1.5, -2.5, -0.5, 2, 3.5, 2.5]
CAR_SPEAKERS = [4, 3.5, 2, 0, -1, 0, 1, 2.5, 3, 2]
SAVED = ["Car speakers", "Night, low"]
CURVES = dict(PRESETS, **{"Custom": CUSTOM, "Car speakers": CAR_SPEAKERS})
DRAGGED_BAND = 7        # 4 kHz, +2 dB in the Custom curve

# ---- playback/eq/EqDesign.kt at 48 kHz: nine peaking filters (Q 0.8) and a high shelf above 8 kHz, their gains
# solved so the summed response passes through the sliders.
_FS, _Q, _TREBLE = 48000.0, 0.8, 9
_SHELF_HZ = math.sqrt(8000.0 * 16000.0)
F_LO, F_HI = FREQS[0] / math.sqrt(2), FREQS[-1] * math.sqrt(2)


def _coeffs(band: int, gain_db: float):
    f = _SHELF_HZ if band == _TREBLE else FREQS[band]
    w = 2 * math.pi * f / _FS
    c, s = math.cos(w), math.sin(w)
    a = 10 ** (gain_db / 40)
    if band == _TREBLE:
        alpha = s / 2 * math.sqrt(2)
        k = 2 * math.sqrt(a) * alpha
        a0 = (a + 1) - (a - 1) * c + k
        return (a * ((a + 1) + (a - 1) * c + k) / a0, -2 * a * ((a - 1) + (a + 1) * c) / a0,
                a * ((a + 1) + (a - 1) * c - k) / a0, 2 * ((a - 1) - (a + 1) * c) / a0, ((a + 1) - (a - 1) * c - k) / a0)
    alpha = s / (2 * _Q)
    a0 = 1 + alpha / a
    return ((1 + alpha * a) / a0, -2 * c / a0, (1 - alpha * a) / a0, -2 * c / a0, (1 - alpha / a) / a0)


def _db(co, cos_w: float) -> float:
    b0, b1, b2, a1, a2 = co
    cos2 = 2 * cos_w * cos_w - 1
    num = b0 * b0 + b1 * b1 + b2 * b2 + 2 * (b0 * b1 + b1 * b2) * cos_w + 2 * b0 * b2 * cos2
    den = 1 + a1 * a1 + a2 * a2 + 2 * (a1 + a1 * a2) * cos_w + 2 * a2 * cos2
    return 10 * math.log10(max(num / den, 1e-30))


def _solve(matrix, weights, targets):
    n = 10
    a = [[0.0] * (n + 1) for _ in range(n)]
    for p, rowp in enumerate(matrix):
        w2 = weights[p] ** 2
        for i in range(n):
            mi = rowp[i] * w2
            for j in range(n):
                a[i][j] += mi * rowp[j]
            a[i][n] += mi * targets[p]
    for col in range(n):
        piv = max(range(col, n), key=lambda r: abs(a[r][col]))
        a[col], a[piv] = a[piv], a[col]
        if abs(a[col][col]) < 1e-12:
            continue
        for r in range(col + 1, n):
            f = a[r][col] / a[col][col]
            for k in range(col, n + 1):
                a[r][k] -= f * a[col][k]
    x = [0.0] * n
    for i in range(n - 1, -1, -1):
        s = a[i][n] - sum(a[i][k] * x[k] for k in range(i + 1, n))
        x[i] = 0.0 if abs(a[i][i]) < 1e-12 else s / a[i][i]
    return x


def filter_gains(curve):
    """EqDesign.filterGainsDb: least squares over the band centres (weight 1) and the midpoints (weight 0.25)."""
    if not any(curve):
        return [0.0] * 10
    pos, weights = [], []
    for b in range(10):
        pos.append(float(b)); weights.append(1.0)
        if b < 9:
            pos.append(b + 0.5); weights.append(0.25)
    cosw = [math.cos(2 * math.pi * FREQS[0] * 2 ** p / _FS) for p in pos]

    def slider(p):
        lo = int(p)
        fr = p - lo
        return curve[lo] if fr == 0 or lo >= 9 else curve[lo] * (1 - fr) + curve[lo + 1] * fr

    targets = [slider(p) for p in pos]
    proto = [[_db(_coeffs(b, 17.0), cw) / 17.0 for b in range(10)] for cw in cosw]
    gains = _solve(proto, weights, targets)
    for _ in range(2):
        m = [[proto[p][b] if abs(gains[b]) < 0.01 else _db(_coeffs(b, gains[b]), cosw[p]) / gains[b] for b in range(10)]
             for p in range(len(pos))]
        gains = _solve(m, weights, targets)
    return [max(-40.0, min(40.0, g)) for g in gains]


def response(curve, points: int = 96):
    """EqualiserBands.response: dB at `points` frequencies, log spaced over the ten octaves of the drawing."""
    fg = filter_gains(curve)
    cos_ = [_coeffs(b, g) for b, g in enumerate(fg) if g != 0.0]
    out = []
    for i in range(points):
        f = math.exp(math.log(F_LO) + (math.log(F_HI) - math.log(F_LO)) * i / (points - 1))
        cw = math.cos(2 * math.pi * f / _FS)
        out.append(sum(_db(co, cw) for co in cos_))
    return out


def _num(v: float) -> str:
    return f"{v:.2f}".rstrip("0").rstrip(".")


# The plot BandSliders draws: 184dp tall with 0 dB in the middle and 6.5dp to a decibel, so a knob's travel is 14 to 170
# and the response, drawn to 14 dB either way, has 1dp to spare.
PLOT_W, PLOT_H, PER_DB, RESPONSE_DB = 339.0, 184.0, 6.5, 14.0
CARD_W, CARD_TOP, CARD_FOOT = 379.0, 6.0, 10.0
GUTTER_START, GUTTER_END = 30.0, 10.0


def curve_paths(curve, width=PLOT_W, height=PLOT_H, clamp=RESPONSE_DB, points=96):
    """(line, area) SVG paths of the response in the bands' plot: 6.5dp to a decibel, 14 dB either way, the area
    closed on 0 dB."""
    mid, px = height / 2, PER_DB
    pts = [(width * i / (points - 1), mid - max(-clamp, min(clamp, db)) * px) for i, db in enumerate(response(curve, points))]
    line = "M " + " L ".join(f"{_num(x)} {_num(y)}" for x, y in pts)
    return line, f"{line} L {_num(width)} {_num(mid)} L 0 {_num(mid)} Z"


def fmt_db(gain: float) -> str:
    """EqualiserBands.formatDb with R.string.equaliser_db: "+4 dB", "−2.5 dB", "0 dB"."""
    r = round(gain * 2) / 2
    mag = str(int(abs(r))) if abs(r) % 1 == 0 else str(abs(r))
    return ("+" if r > 0 else "−" if r < 0 else "") + mag + " dB"


# ============================================================================== working on an opened file
class _Tokens:
    """ctx["tokens"] for a file that already exists: variable ids by `Collection/name`, looked up in the document."""

    def __init__(self, d: Doc):
        self.d = d

    def __getitem__(self, name: str) -> str:
        return self.d.var_id(name)


def attach(d: Doc, ctx: dict):
    """Fill the Doc's registries from the document, so the kit's helpers and the other modules' builders work on a
    file opened from disk as they do during a build."""
    comps = d.cmd("component.list")
    d.comp = {c["name"]: c["id"] for c in comps
              if c["type"] == "COMPONENT_SET" or (c["type"] == "COMPONENT" and "=" not in c["name"])}
    d.icon_names = {n for n in d.comp if n.startswith("Icon/")}
    d.text_styles, d.effect_styles = {}, {}
    for s in d.cmd("style.list"):
        if s["type"] == "text":
            d.text_styles[s["name"]] = s["id"]
        elif s["type"] == "effect":
            d.effect_styles[s["name"]] = s["id"]
    d.pages = {p["name"]: p["id"] for p in d.cmd("page.list") if not p.get("divider")}
    d._first_page = False
    d.mode_collections = MODE_COLLECTIONS
    d.screen_fill, d.icon_color = "Color/ground", "Color/on"
    ctx["tokens"] = _Tokens(d)
    ctx.setdefault("triggers", {})
    # The Screens page: where the next section goes, and the frames by name.
    page = d.get(d.pages["Screens"], depth=2)
    bottom = 0.0
    frames = {}
    for sec in page.get("children", []):
        bottom = max(bottom, sec.get("y", 0) + sec.get("height", 0))
        for c in sec.get("children") or []:
            if c.get("type") == "FRAME":
                frames[c["name"]] = c["id"]
    Area._next_y[(id(d), "Screens")] = bottom + 200
    ctx["frames"] = frames
    return frames


def _adopt(d: Doc, ctx: dict, key: str, name: str, source: str = ""):
    """Register a screen that is already in the file under `key`, so links and the flow lane can name it."""
    if key not in d.screens:
        fr = ctx["frames"]
        d.screens[key] = {"light": fr[f"{name} · Light"], "dark": fr.get(f"{name} · Dark"), "map": {}, "name": name,
                          "source": source, "page": "Screens", "note": None, "status": "ready", "h": H}
    return d.screens[key]


def _variant(d: Doc, set_name: str, variant: str) -> int:
    for c in d.get(d.comp[set_name], depth=1).get("children", []):
        if c["name"] == variant:
            return c["id"]
    raise KeyError(f"{set_name}: no variant {variant!r}")


# ============================================================================== text styles and icons
# (name, size, line, weight, tabular, source)
EQ_TYPE = [
    ("Component/Equaliser scope title", 17, 22, 700, False, "ComfortEqualiser.kt EqualiserScopeHead title: onest(17f, 22f, 700)"),
    ("Component/Equaliser axis", 11, 16, 400, True, "ComfortEqualiser.kt BandSliders axis labels: onest(11f, 16f).tabular()"),
    ("Component/Equaliser frequency", 12, 16, 400, False, "ComfortEqualiser.kt band labels under BandSliders: onest(12f, 16f)"),
    ("Component/Equaliser value", 12.5, 16, 600, True, "ComfortEqualiser.kt BandSliders value bubble: onest(12.5f, 16f, 600).tabular()"),
    ("Component/Equaliser indicator", 13, 18, 500, False, "NowPlayingParts.kt NowPlayingHeader's equaliser indicator: onest(13f, 18f, 500)"),
]


def _styles(d: Doc):
    for name, size, line, weight, tab, src in EQ_TYPE:
        text_style(d, name, size, line, weight, tabular=tab, description=src)
    # A specimen card on the Type page, in the gap under "Recap styles".
    pid = d.page("Type")
    tops = {c["name"]: c for c in d.get(pid, depth=1).get("children", [])}
    above, below = tops.get("Recap styles"), tops.get("System stand-ins")
    x = above["x"] if above else 2080
    y = (above["y"] + above["height"] + 80) if above else 0
    c = d.frame(None, name="Equaliser styles", x=x, y=y, w=2000, layout="v", gap=24, pad=48, fill="Canvas/card", radius=32,
                sizing=("fixed", "hug"))
    d.text(c, "Equaliser: onest(...) in ComfortEqualiser.kt", style="Doc/Heading", color="Canvas/label", name="Heading")
    d.text(c, "Added in 1.1.0. The equaliser's other text takes styles that were here: Scale/Bar title (the sheet's title), "
              "Scale/Sub (second lines, the off line), Component/Chip, Component/Overlay row and Component/Note.",
           style="Doc/Body", color="Canvas/label-muted", name="Note", width=1900, textAutoResize="height")
    for name, size, line, weight, tab, src in EQ_TYPE:
        r = d.frame(c, name=f"Specimen: {name}", layout="h", gap=32, cross="center", sizing=("hug", "hug"))
        meta = d.frame(r, name="Meta", layout="v", gap=2, w=520, sizing=("fixed", "hug"))
        d.text(meta, name, style="Doc/Label", color="Canvas/label", name="Style name")
        d.text(meta, f"{size:g}/{line:g} · {WEIGHT_NAME[weight]} {weight}" + (" · tabular" if tab else ""), style="Doc/Code",
               color="Canvas/label-muted", name="Metrics")
        sample = {"Component/Equaliser axis": "+12  0  −12", "Component/Equaliser frequency": "31  62  125  1k  16k",
                  "Component/Equaliser value": "+4 dB  −2.5 dB", "Component/Equaliser indicator": "This song"}.get(name, "For this song only")
        cell = d.frame(r, name="Sample cell", w=420, layout="h", cross="center", sizing=("fixed", "hug"))
        d.text(cell, sample, style=name, color="Color/on", name="Sample")
        d.text(r, src, style="Doc/Caption", color="Canvas/label-muted", name="Source", width=900, textAutoResize="height")
    if below and d.get(c)["y"] + d.get(c)["height"] + 40 > below["y"]:
        raise RuntimeError("Equaliser styles overlaps the card under it on the Type page")


def _icons_added(d: Doc) -> list[str]:
    """Icon/<ligature> for the glyphs Symbols.kt has gained since the file was drawn, each in its alphabetical place in
    the Icons page's outlined grid, as foundations.build_comfort_icons draws them (weight 300, 2.6dp low)."""
    missing = [(lig, cp) for _, lig, cp in _icons.symbols() if f"Icon/{lig}" not in d.comp]
    if not missing:
        return []
    pid = d.page("Icons")
    grid = d.child(pid, "Grid: Outlined, weight 300")
    for lig, cp in missing:
        nm = f"Icon/{lig}"
        cell = d.frame(grid, name=f"Cell: {lig}", w=164, layout="v", gap=10, pad=[16, 8, 12, 8], cross="center",
                       fill="Canvas/section", radius=12, sizing=("fixed", "hug"))
        enum = "".join(p.capitalize() for p in lig.split("_"))
        c = d.component(cell, name=nm, w=24, h=24,
                        description=f"Material Symbols Rounded `{lig}` (U+{cp:04X}), FILL 0, wght 300, opsz 24. "
                                    f"Compose: ComfortIcon(Symbol.{enum}).")
        d.set(c, clip=False)
        path = re.sub(r"-?\d+\.\d+", lambda m: f"{float(m.group()):.3f}".rstrip("0").rstrip("."), _icons.path(cp, fill=0, wght=300))
        g = d.vector(c, name="Glyph", path=path, fill="Color/on", constraints={"horizontal": "scale", "vertical": "scale"})
        d.set(g, y=d.get(g)["y"] + ICON_DROP)
        d.text(cell, lig, style="Doc/Caption", color="Canvas/label", name="Name", width=148, textAutoResize="height",
               textAlign="center")
        d.register(nm, c)
        d.icon_names.add(nm)
        names = [k["name"] for k in d.get(grid, depth=1)["children"]]
        before = sorted(n for n in names if n != f"Cell: {lig}")
        idx = sum(1 for n in before if n < f"Cell: {lig}")
        d.cmd("node.reorder", id=cell, index=idx)
    intro = d.child(pid, "Intro")
    chars = d.get(intro)["characters"]
    new = re.sub(r"Only the \d+ glyphs", f"Only the {len(_icons.symbols())} glyphs", chars)
    if new != chars:
        d.set(intro, characters=new)
    return [lig for lig, _ in missing]


# ============================================================================== components
_PROBE = Path("/tmp/attic-design/.comfort-equaliser-probe.figcraft")


def _descriptions(d: Doc) -> dict[int, str]:
    """Every component's and set's description, read from a saved copy: get_node leaves out a component set's
    description (FigCraft 0.18.1), as handoff.py found."""
    _PROBE.parent.mkdir(parents=True, exist_ok=True)
    d.save(_PROBE)
    try:
        doc = json.loads(zipfile.ZipFile(_PROBE).read("document.json"))
    finally:
        _PROBE.unlink(missing_ok=True)
    out = {}

    def walk(o):
        if isinstance(o, dict):
            if o.get("type") in ("component", "componentSet") and "id" in o:
                out[o["id"]] = o.get("description") or ""
            for v in o.values():
                if isinstance(v, (dict, list)):
                    walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)
    walk(doc)
    return out


def _append_description(d: Doc, ctx: dict, node: int, more: str):
    """Add to a component's or set's description, keeping what it says (ctx["descriptions"] holds the file's)."""
    cur = ctx["descriptions"].get(node, "")
    if not cur:
        raise RuntimeError(f"{d.get(node)['name']}: no description to add to")
    ctx["descriptions"][node] = f"{cur} {more}"
    d.set(node, description=ctx["descriptions"][node])


def _append_notes(d: Doc, page: str, set_name: str, more: str):
    """Add a sentence to the Notes text of a set's spec card (a card another module drew)."""
    card = next(c["id"] for c in d.get(d.pages[page], depth=1)["children"] if c["name"] == f"Spec: {set_name}")
    notes = d.child(card, "Notes")
    d.set(notes, characters=d.get(notes)["characters"] + " " + more)


def _actions(d: Doc, k: K):
    cc = Components.extend(d, "Components / Actions")

    # -- Preset chip
    sb = SetBuilder(cc, "Preset chip", "ComfortEqualiser.kt · PresetChip", "PresetChip",
                    "The equaliser's presets, one scrolling row, single choice: Bars.kt FilterChip on fill (36dp pill, "
                    "16dp sides, 14/18 medium), selected on accent-soft with the 18dp tick and a semibold label. A saved "
                    "preset, while selected, ends in more_vert (18dp, 8 from the end): tapping it again, or holding any "
                    "saved chip, opens Rename and Delete preset. Save as preset is an action, not a choice: add (18dp) "
                    "and the label in accent-ink, shown beside Custom. With the equaliser off the whole row is at 50 %.")
    for kind, sel, label in (("Built-in", False, "Flat"), ("Built-in", True, "Bass boost"), ("Saved", False, "Night, low"),
                             ("Saved", True, "Car speakers"), ("Save as preset", False, "Save as preset")):
        lead = sel or kind == "Save as preset"
        pad = [0, 8 if (kind == "Saved" and sel) else 20 if lead else 16, 0, 12 if lead else 16]
        c = sb.variant({"Kind": kind, "Selected": str(sel)}, layout="h", h=36, gap=6, pad=pad, cross="center",
                       fill="Color/accent-soft" if sel else "Color/fill", sizing=("hug", "fixed"))
        k.bind_radius(c, "pill")
        ink = "Color/on-accent-soft" if sel else "Color/accent-ink" if kind == "Save as preset" else "Color/on"
        if sel:
            k.icon(c, "check", size=18, color=ink, name="Tick")
        elif kind == "Save as preset":
            k.icon(c, "add", size=18, color=ink, name="Add")
        t = k.text(c, label, "Component/Chip selected" if sel else "Component/Chip", ink, "Label")
        sb.text("Label", t, label)
        if kind == "Saved" and sel:
            k.icon(c, "more_vert", size=18, color=ink, name="More")
    sb.finish(description_extra="Part of ComfortEqualiserSheet and ComfortEqualiserPage: the preset row, Bars.kt FilterChip "
                                "with a trailing glyph for a saved preset and the Save as preset action.")

    # -- Response curve
    sb = SetBuilder(cc, "Response curve", "ComfortEqualiser.kt · BandSliders", "BandSliders",
                    "The response BandSliders draws behind its knobs: ten octaves wide (22 Hz to 22.6 kHz, log), 339 x 184, "
                    "0 dB in the middle and 6.5dp to a decibel, which is the knobs' own scale, so the line runs through "
                    "every knob. It is drawn to 14 dB either way, since neighbouring boosts can sum past +12. The line is "
                    "2.5dp in accent with round ends; the area between it and 0 dB is accent at 20 %, and it is what "
                    "shows how far a band is from 0. One variant per built-in preset (playback/eq/EqPresets.kt), plus the "
                    "demo's Custom curve and its saved preset Car speakers (storeShots EqualiserDemo.kt). Each path is the "
                    "engine's own response (playback/eq/EqDesign.kt), computed by design/build/comfort/equaliser.py: draw "
                    "another curve there, not by hand.")
    for name, gains in CURVES.items():
        c = sb.variant({"Curve": name}, w=PLOT_W, h=PLOT_H)
        d.set(c, clip=False)
        line, area = curve_paths(gains)
        if any(gains):
            a = d.vector(c, name="Area", path=area, fill="Color/accent", constraints={"horizontal": "scale", "vertical": "scale"})
            d.set(a, opacity=0.2)
        d.vector(c, name="Line", path=line, stroke="Color/accent", stroke_w=2.5, strokeCap="round", strokeJoin="round",
                 constraints={"horizontal": "scale", "vertical": "scale"})
    sb.finish([{"Curve": "Bass boost"}, {"Curve": "Vocal"}, {"Curve": "Custom"}],
              description_extra="The response BandSliders draws, one variant per preset in playback/eq/EqPresets.kt plus the "
                                "demo's Custom and Car speakers curves. Used inside Band sliders.")

    # -- Band slider
    sb = SetBuilder(cc, "Band slider", "ComfortEqualiser.kt · BandSliders", "BandSliders",
                    "One band's knob in BandSliders: a column a tenth of the plot wide and 184dp tall, 0 dB at y = 92, 6.5dp "
                    "to a decibel, −12 to +12. The knob is 20dp in knob with a 2dp accent ring inside its edge and "
                    "Shadow/Small. Its track and the response behind it belong to Band sliders. To set a gain, pick Gain "
                    "(Boost above 0 dB, Cut below, Zero) and set the height of the Reach layer to |gain| x 6.5 + 6: the "
                    "knob rides on its end. Dragged: the knob is 26dp and the value shows in a 62 x 28 pill (toast, "
                    "12.5/16 semibold tabular on-toast), its foot 10dp above the knob; set Value (\"+4 dB\", \"−2.5 dB\", "
                    "\"0 dB\"). The pill may lie over the neighbouring knobs and, near +12 dB, over the chips above the "
                    "card: it is there only while the finger is. Off: the ring in on-3. A drag moves the value from "
                    "where it is, snapping to 1 dB; a tap does nothing; a double tap sets 0.")
    for state in ("On", "Off", "Dragged"):
        for gain, demo in (("Boost", 4.0), ("Cut", -2.5), ("Zero", 0.0)):
            c = sb.variant({"State": state, "Gain": gain}, w=34, h=PLOT_H)
            d.set(c, clip=False)
            ink = "Color/on-3" if state == "Off" else "Color/accent"
            reach_h = abs(demo) * PER_DB + 6
            up = gain != "Cut"
            tv = d.frame(c, name="Travel", x=14, y=0 if up else 89, w=6, h=95, layout="v", main="max" if up else "min",
                         cross="center", sizing=("fixed", "fixed"), clip=False)
            d.set(tv, constraints={"horizontal": "center", "vertical": "top"})
            reach = d.frame(tv, name="Reach", w=6, h=reach_h, clip=False)
            size = 26 if state == "Dragged" else 20
            ky = 3 - size / 2 if up else reach_h - 3 - size / 2
            kn = d.ellipse(reach, name="Knob", w=size, h=size, x=3 - size / 2, y=ky, fill="Color/knob", stroke={"$var": ink},
                           strokeWeight=2, strokeAlign="inside")
            d.set(kn, constraints={"horizontal": "center", "vertical": "top" if up else "bottom"})
            d.cmd("style.apply", ids=[kn], style=d.effect_styles["Shadow/Small"])
            if state == "Dragged":
                centre = 3 if up else reach_h - 3
                bub = d.frame(reach, name="Value bubble", w=62, h=28, x=3 - 31, y=centre - 13 - 10 - 28, layout="h", main="center",
                              cross="center", fill="Color/toast", sizing=("fixed", "fixed"))
                k.bind_radius(bub, "pill")
                d.set(bub, constraints={"horizontal": "center", "vertical": "top" if up else "bottom"})
                t = d.text(bub, fmt_db(demo), style="Component/Equaliser value", color="Color/on-toast", name="Value")
                sb.text("Value", t, fmt_db(demo))
    sb.finish([{"State": "On", "Gain": "Boost"}, {"State": "Off", "Gain": "Cut"}, {"State": "Dragged", "Gain": "Boost"}],
              description_extra="One band's knob in BandSliders. Part of ComfortEqualiserSheet and ComfortEqualiserPage.")

    # -- Band sliders: the plot
    sb = SetBuilder(cc, "Band sliders", "ComfortEqualiser.kt · BandSliders", "BandSliders",
                    "The equaliser's one drawing: the ten bands with the response they make behind them, on a card (fill, "
                    "radius 18, 218dp tall: 6 above the plot, the 184dp plot, 2, the 16dp frequency labels, 10). The plot "
                    "is inset 30 at the start for the labels and 10 at the end; a band's column is a tenth of it (33.9dp "
                    "in the sheet, 32.3 on the Settings card: resize the instance and everything follows). Back to "
                    "front: dashed rules at +12 and −12 dB (1dp fill-2, 2 on, 4 off) and a solid one at 0 dB; +12, 0 and "
                    "−12 in 11/16 tabular on-2, right-aligned in the 24dp before the plot; ten 6dp capsule tracks in "
                    "fill-2 from +12 to −12; the response (the exposed Curve: pick the preset); the ten knobs (Band 31 Hz "
                    "to Band 16 kHz, exposed: set each one's Gain and its Reach height); the frequency labels in 12/16 "
                    "on-2. The response and the knobs share one scale, so the line passes through every knob and the "
                    "labels at the start read for both. A knob has no accent run under it: the tinted area does that. "
                    "Off: the knobs' rings and the line are on-3, and the line is the flat 0 dB that is playing. On and "
                    "Off show Bass boost; Dragged shows the demo's Custom curve with 4 kHz held at +2 dB. In the sheet "
                    "the card has 16dp side margins and 4 above; in the Settings card 8, 4 above and 8 below.")
    mid = PLOT_H / 2
    for state in ("On", "Off", "Dragged"):
        # Auto layout carries the width down (a resized instance reflows its rows); what lies over the rows is absolute.
        c = sb.variant({"State": state}, layout="v", w=CARD_W, gap=2, pad=[CARD_TOP, GUTTER_END, CARD_FOOT, GUTTER_START],
                       fill="Color/fill", sizing=("fixed", "hug"))
        d.set(c, clip=False)
        k.bind_radius(c, "control")
        bands = d.frame(c, name="Bands", layout="h", h=PLOT_H, sizing=("fill", "fixed"), clip=False)
        for nm, db in (("Rule +12 dB", 12), ("Rule 0 dB", 0), ("Rule −12 dB", -12)):
            v = d.vector(bands, name=nm, path=f"M 0 0 L {_num(PLOT_W)} 0", stroke="Color/fill-2", stroke_w=1)
            k.absolute(v, 0, mid - db * PER_DB, constraints={"horizontal": "leftRight", "vertical": "top"})
            if db:
                d.set(v, dashes=[2, 4])
        for i, name in enumerate(BAND_NAMES):
            tr = d.rect(bands, name=f"Track {name}", w=6, h=156, fill="Color/fill-2")
            k.bind_radius(tr, "pill")
            k.absolute(tr, PLOT_W * (i + 0.5) / 10 - 3, 14, 6, 156, {"horizontal": "scale", "vertical": "top"})
        gains = CUSTOM if state == "Dragged" else PRESETS["Bass boost"]
        if state == "Off":
            fl = d.vector(bands, name="Flat line", path=f"M 0 0 L {_num(PLOT_W)} 0", stroke="Color/on-3", stroke_w=2.5,
                          strokeCap="round")
            k.absolute(fl, 0, mid, constraints={"horizontal": "leftRight", "vertical": "top"})
        else:
            cv = d.instance("Response curve", bands, name="Curve")
            d.cmd("instance.setProperties", ids=[cv], properties={"Curve": "Custom" if state == "Dragged" else "Bass boost"})
            k.absolute(cv, 0, 0, PLOT_W, PLOT_H, {"horizontal": "leftRight", "vertical": "top"})
            d.cmd("component.exposeInstance", id=cv)
        for i, g in enumerate(gains):
            b = d.instance("Band slider", bands, name=f"Band {BAND_NAMES[i]}")
            d.set(b, sizingH="fill")
            set_band(d, b, g, "Dragged" if (state == "Dragged" and i == DRAGGED_BAND) else "Off" if state == "Off" else "On")
            d.cmd("component.exposeInstance", id=b)
        fl = d.frame(c, name="Frequency labels", layout="h", sizing=("fill", "hug"))
        for lab in LABELS:
            t = d.text(fl, lab, style="Component/Equaliser frequency", color="Color/on-2", name=f"Label {lab}", textAlign="center")
            d.set(t, sizingH="fill", textAutoResize="height")
        for nm, chars, db in (("Axis +12", "+12", 12), ("Axis 0", "0", 0), ("Axis −12", "−12", -12)):
            t = d.text(c, chars, style="Component/Equaliser axis", color="Color/on-2", name=nm, width=24, textAutoResize="height",
                       textAlign="right")
            k.absolute(t, 0, CARD_TOP + mid - db * PER_DB - 8, constraints={"horizontal": "left", "vertical": "top"})
    sb.finish(description_extra="The bands and their response in one card. Part of ComfortEqualiserSheet and "
                                "ComfortEqualiserPage. TalkBack reads the ten bands as sliders and skips the drawing.")
    cc.dark_preview()


def set_band(d: Doc, band: int, gain: float, state: str = "On"):
    """Set a Band slider instance to `gain` dB: its variant, the run's height and, when dragged, the bubble's text."""
    kind = "Boost" if gain > 0 else "Cut" if gain < 0 else "Zero"
    d.cmd("instance.setProperties", ids=[band], properties={"State": state, "Gain": kind})
    d.override(band, "Reach", height=abs(gain) * PER_DB + 6)
    if state == "Dragged":
        setp(d, band, Value=fmt_db(gain))


def set_bands(d: Doc, sliders: int, gains, state: str = "On", dragged: int | None = None):
    """Set a Band sliders instance to a curve (State as the instance has it; `dragged` is the band held)."""
    for i, g in enumerate(gains):
        set_band(d, d.child(sliders, f"Band {BAND_NAMES[i]}"), g, "Dragged" if dragged == i else state)


def _add_variant(d: Doc, set_name: str, base: str, prop: str, value: str) -> int:
    """A new variant of an existing set, copied from `base` (component.addVariant), and its id."""
    before = {c["id"] for c in d.get(d.comp[set_name], depth=1)["children"]}
    d.cmd("component.addVariant", id=_variant(d, set_name, base), property=prop, value=value)
    new = [c for c in d.get(d.comp[set_name], depth=1)["children"] if c["id"] not in before]
    if len(new) != 1:
        raise RuntimeError(f"{set_name}: expected one new variant, got {[c['name'] for c in new]}")
    return new[0]["id"]


def _lists(d: Doc, ctx: dict, k: K):
    """Settings row gains Control=Value and chevron: SetRow with a value and a trailing chevron, which Settings >
    Playback > Equaliser is the first row to use."""
    v = _add_variant(d, "Settings row", "Control=Value", "Control", "Value and chevron")
    val = d.child(v, "Value")
    tr = d.frame(v, name="Trailing", layout="h", gap=10, cross="center", sizing=("hug", "hug"))
    d.cmd("node.reparent", ids=[val], parent=tr)
    k.icon(tr, "chevron_right", color="Color/on-3", name="Chevron")
    d.set(d.child(v, "Title"), characters="Equaliser")
    d.set(val, characters="Bass boost")
    _append_description(d, ctx, d.comp["Settings row"],
                        "Control=Value and chevron (1.1.0): the value in accent-ink, then chevron_right in on-3 10dp after "
                        "it, for a row that shows a state and opens a page. Compose: ui/comfort/ComfortSettings.kt · "
                        "ComfortSettings, the Equaliser row in Playback (SetRow with value and trailing).")
    _append_notes(d, "Components / Lists and cards", "Settings row",
                  "Value and chevron (1.1.0): a value and chevron_right 10dp after it, for a row that shows a state and opens "
                  "a page: Equaliser in Playback reads \"Off\" or the default's preset (ComfortSettings.kt).")
    cc = Components.extend(d, "Components / Lists and cards")
    cc.preview.append(("Settings row", [{"Control": "Value and chevron", "Title": "Equaliser", "Value": "Bass boost"}]))
    cc.dark_preview()


def _playback(d: Doc, ctx: dict, k: K):
    cc = Components.extend(d, "Components / Playback")
    sb = SetBuilder(cc, "Equaliser indicator", "NowPlayingParts.kt · NowPlayingHeader", "NowPlayingHeader",
                    "Shown in Now Playing's header, before the kebab, while the equaliser is on and the song plays with its "
                    "own setting (This song) or its album's (This album). 32dp pill on fill, 10dp at the start, 12 at the "
                    "end: equalizer at 18dp in accent-ink, 6dp, the label in 13/18 medium on-2. It opens the equaliser "
                    "sheet; the touch target is 48dp. It fades in and out over 200 ms, and nothing in the deck changes.")
    for scope, label in (("Song", "This song"), ("Album", "This album")):
        c = sb.variant({"Scope": scope}, layout="h", h=32, gap=6, pad=[0, 12, 0, 10], cross="center", fill="Color/fill",
                       sizing=("hug", "fixed"))
        k.bind_radius(c, "pill")
        k.icon(c, "equalizer", size=18, color="Color/accent-ink", name="Icon")
        k.text(c, label, "Component/Equaliser indicator", "Color/on-2", "Label")
    sb.finish(description_extra="The piece NowPlayingHeader's trailing slot holds for the equaliser. TalkBack reads "
                                "\"Equaliser. This song has its own setting.\" or \"Equaliser. This song plays with its "
                                "album's setting.\"")
    # Now playing header: the indicator between the context and the kebab, off by default.
    hd = d.comp["Now playing header"]
    slot = d.frame(hd, name="Equaliser", layout="h", pad=[0, 2, 0, 0], cross="center", sizing=("hug", "hug"))
    d.cmd("node.reorder", id=slot, index=2)
    ind = d.instance("Equaliser indicator", slot, name="Indicator")
    d.set(slot, visible=False)
    _props(d, hd, [("Show equaliser", "boolean", slot, False)])
    d.cmd("component.exposeInstance", id=ind)
    _append_description(d, ctx, hd, "Show equaliser (1.1.0) turns on the nested Indicator (Equaliser indicator, exposed: Scope=Song "
                               "or Album) between the context and the kebab, 2dp before it; the context gives up width to it "
                               "and ellipsizes. NowPlayingHeader's trailing slot.")
    _append_notes(d, "Components / Playback", "Now playing header",
                  "Show equaliser (1.1.0) puts the Equaliser indicator between the context and the kebab, 2dp before it; the "
                  "context gives up width to it.")
    cc.dark_preview()


def _overlays(d: Doc, ctx: dict, k: K):
    cc = Components.extend(d, "Components / Overlays")

    # -- Equaliser title
    sb = SetBuilder(cc, "Equaliser title", "ComfortEqualiser.kt · ComfortEqualiserSheet", "ComfortEqualiserSheet",
                    "The equaliser sheet's first row: \"Equaliser\" in 20/26 bold with the master switch at its end, 24dp "
                    "sides, 6 above, 8 below; the whole row toggles. Off adds the line under it in 13.5/18 on-2 (24dp "
                    "sides, 8 below). Off dims nothing but the chips: every control still works, and a chip or the first "
                    "band movement turns the switch back on.")
    for on in (True, False):
        c = sb.variant({"On": str(on)}, layout="v", w=411, sizing=("fixed", "hug"))
        r = d.frame(c, name="Title row", layout="h", pad=[6, 24, 8, 24], cross="center", sizing=("fill", "hug"))
        k.text(r, "Equaliser", "Scale/Bar title", "Color/on", "Title", fill=True, lines=1)
        sw = d.instance("Switch", r, name="Switch")
        d.cmd("instance.setProperties", ids=[sw], properties={"Checked": str(on), "State": "Default"})
        if not on:
            n = d.frame(c, name="Off line", layout="v", pad=[0, 24, 8, 24], sizing=("fill", "hug"))
            k.text(n, "Off. Choose a preset or move a band to turn it on.", "Scale/Sub", "Color/on-2", "Off text", fill=True)
    sb.finish(description_extra="The sheet's title row with the master switch; On=False adds the off line. On the "
                                "Settings page (ComfortEqualiserPage) the switch is a Settings row, \"Use equaliser\".")

    # -- Scope head
    sb = SetBuilder(cc, "Scope head", "ComfortEqualiser.kt · EqualiserScopeHead", "EqualiserScopeHead",
                    "What the bands below change, the first thing under the title: 16dp side margins, 4 below; 8dp padding, "
                    "at least 64 tall, a 48dp lead, 14dp, then the title in 17/22 bold and a second line in 13.5/18. "
                    "Default: the neutral icon box with library_music (fill-2 on a dark sheet: override Container) and "
                    "\"Every song without its own setting\" in on-2. A song or album with its own setting: its cover, and "
                    "the whole head on a bed of accent-soft with radius 18, the second line (Subtitle: the album's name "
                    "or the song's title) in on-accent-soft. This is the bed the current song's row lies on. The bed "
                    "fades in over 200 ms when a setting is saved, and the sheet stays open.")
    for scope, title, sub in (("Default", "For all songs", "Every song without its own setting"),
                              ("Album", "For this album", "Paper Lanterns"),
                              ("Song", "For this song only", "Lights Over the Water")):
        own = scope != "Default"
        c = sb.variant({"Scope": scope}, layout="v", w=411, pad=[0, 16, 4, 16], sizing=("fixed", "hug"))
        bed = d.frame(c, name="Bed", layout="h", gap=14, pad=8, cross="center", fill="Color/accent-soft" if own else None,
                      sizing=("fill", "hug"))
        d.set(bed, minHeight=64)
        k.bind_radius(bed, "control")
        if own:
            cv = d.instance("Cover/Album", bed, name="Cover")
            d.cmd("instance.setProperties", ids=[cv], properties={"Shape": "Squircle", "Art": "Image", "Dimmed": "False"})
            d.set(cv, width=48, height=48)
            cover_image(d, cv, COVER[0])
            d.cmd("component.exposeInstance", id=cv)
        else:
            ib = d.instance("Icon box", bed, name="Lead")
            setp(d, ib, Style="Neutral", Icon="Icon/library_music")
            d.set(ib, width=48, height=48)
        lab = d.frame(bed, name="Labels", layout="v", sizing=("fill", "hug"))
        k.text(lab, title, "Component/Equaliser scope title", "Color/on", "Title", fill=True, lines=1)
        t2 = k.text(lab, sub, "Scale/Sub", "Color/on-accent-soft" if own else "Color/on-2", "Subtitle", fill=True, lines=1)
        sb.text("Subtitle", t2, sub)
    sb.finish(description_extra="The first piece of ComfortEqualiserSheet under its title. The Settings page has none: "
                                "it only ever edits the default.")

    # -- Sheet row: the row with a second line
    v = _add_variant(d, "Sheet row", "Kind=Default, End=None", "Kind", "Two line")
    d.set(v, minHeight=64)
    label = d.child(v, "Label")
    lab = d.frame(v, name="Labels", layout="v", sizing=("fill", "hug"))
    d.cmd("node.reorder", id=lab, index=2)
    d.cmd("node.reparent", ids=[label], parent=lab)
    d.set(label, characters="Use default")
    summary = k.text(lab, "Removes this song's setting", "Scale/Sub", "Color/on-2", "Summary", fill=True, lines=1)
    d.cmd("instance.swap", ids=[d.child(v, "Icon")], component=d.comp["Icon/library_music"])
    _props(d, d.comp["Sheet row"], [("Summary", "text", summary, "Removes this song's setting")])
    _append_description(d, ctx, d.comp["Sheet row"],
                        "Kind=Two line (1.1.0): SheetRow with a summary, 64dp, the label over a second line in 13.5/18 on-2 "
                        "(Summary). The equaliser's removal row: \"Use default\" or \"Use album setting\" over what it "
                        "removes (ui/comfort/ComfortEqualiser.kt · ComfortEqualiserSheet). The track menu's Equaliser row "
                        "(ui/comfort/ComfortSheets.kt · ComfortTrackMenu) is Kind=Default, End=Chevron with Icon/equalizer "
                        "and Value: \"Off\", the preset's name, \"This song · Custom\" or \"This album · Vocal\".")
    _append_notes(d, "Components / Overlays", "Sheet row",
                  "Two line (1.1.0): 64dp, the label over a summary in 13.5/18 on-2: the equaliser's \"Use default\" over "
                  "\"Removes this song's setting\". The track menu's Equaliser row is Default with End=Chevron, "
                  "Icon/equalizer and a Value (\"Off\", \"Bass boost\", \"This song · Custom\").")
    cc.preview.append(("Sheet row", [{"Kind": "Two line", "End": "None", "Label": "Use default",
                                      "Summary": "Removes this song's setting"}]))
    cc.dark_preview()


# ============================================================================== screens
KEYS = ["sheet_track_menu_equaliser", "sheet_equaliser_default", "sheet_equaliser_song", "sheet_equaliser_album",
        "sheet_equaliser_off", "dialog_equaliser_save_preset", "menu_equaliser_preset", "sheet_equaliser_band_dragged",
        "now_playing_equaliser", "settings_equaliser_row", "settings_equaliser_page", "settings_equaliser_page_full",
        "settings_equaliser_page_empty", "settings_equaliser_page_off"]
# The code followed 1.2.0 on feature/per-song-equaliser, so no frame is ahead of it.
PROPOSED: dict[str, list[str]] = {}
# The track menus over Now Playing that were in the file before the equaliser: (key, frame name, the row's value).
OLD_MENUS = [("sheet_track_menu_now_playing", "Track menu / From Now Playing"),
             ("sheet_track_menu_open_with", "Track menu / Opened with Attic"),
             ("sheet_sleep_timer", "Sleep timer / Choices"), ("sleep_timer_active", "Sleep timer / Running"),
             ("sheet_sleep_timer_running", "Sleep timer / Choices, running")]
# EqualiserDemo.page(): albums and songs with their own setting, sorted by title. (title, artist, cover, preset)
OWN_ALBUMS = [("Afterglow Protocol", "Velvet Circuit", 10, "Electronic"), ("Room Tone", "Odile Marchetti", 4, "Custom")]
OWN_SONGS = [("Chai Ka Doosra Cup", "Ruhaan Sethi", 9, "Vocal"), ("Kettle On", "Small Hours Tape Club", 1, "Night, low"),
             ("Lights Over the Water", "Mira Okafor", 0, "Custom"), ("Marea Lenta", "Paloma Ferrer", 2, "Acoustic")]
NOTES = {
    "sheet_track_menu_equaliser": [
        "Equaliser is the row after Sleep timer, for a file opened from another app too. Its value is the state: \"Off\", "
        "the default's preset, \"This song · Custom\" or \"This album · Vocal\". The chevron says it opens a sheet, which "
        "takes the menu's place."],
    "sheet_equaliser_default": [
        "The sheet edits the setting the song plays with: here the default, Bass boost. Saved presets come first in the "
        "row, then the built-in eleven; the row has scrolled to show the selected chip. Over Now Playing the sheet takes "
        "the cover's colours (Color mode Tinted wash); in Settings it is Lamplight.",
        "The bands and their response are one drawing: the line passes through every knob, and the +12, 0 and −12 labels "
        "read for both. Without a well of its own for the response the sheet is over 110dp shorter, so the rows that "
        "save a setting stay on screen on a short phone, where the bands would otherwise take the drag meant to scroll "
        "to them."],
    "sheet_equaliser_song": [
        "After Save for this song: the head shows the cover and the song's title on the accent-soft bed, and edits stay "
        "with this song. The bands match no preset, so the row starts with Custom and Save as preset. Use default "
        "removes the song's setting; over an album's setting the row reads Use album setting."],
    "sheet_equaliser_album": [
        "The song plays with its album's setting (Vocal). A change here changes every song on the album. The song can "
        "still take its own; Use default removes the album's."],
    "sheet_equaliser_off": [
        "Off: the line under the title says so, the chips are at 50 %, the knobs lose the accent and stay where they "
        "were, and the response is the flat 0 dB line that is playing, in on-3. Everything still responds: a chip or the "
        "first band movement turns it on."],
    "dialog_equaliser_save_preset": [
        "Save as preset opens the name dialog over the sheet, above the keyboard: the field empty and focused. Save "
        "waits for a name no other preset has; a taken name shows \"Another preset has this name\" under the field. "
        "Rename preset is the same dialog, prefilled, confirming with Rename."],
    "menu_equaliser_preset": [
        "A saved preset, while selected, ends in more_vert. Tapping it again, or holding any saved chip, opens Rename and "
        "Delete preset over the chip (Menu, On sheet). Built-in presets have no menu. In the prototype one tap on Car "
        "speakers comes here; in the app the first tap selects it."],
    "sheet_equaliser_band_dragged": [
        "A band while it is held: the knob grows to 26dp and its value shows in a bubble above it, and the line moves "
        "with the knob. The bubble may lie over the knobs beside it and, near +12 dB, over the chips: it is there only "
        "while the finger is. The value follows the finger from where it was, snapping to 1 dB; a tap without movement "
        "does nothing, and a double tap sets 0 dB."],
    "now_playing_equaliser": [
        "While the equaliser is on and the song plays with its own setting, the header shows the indicator before the "
        "kebab (\"This album\" for its album's setting). It opens the equaliser sheet. Nothing in the deck changes."],
    "settings_equaliser_row": [
        "Playback's Equaliser row: \"Off\" or the default's preset at its end, and under the title how many songs and "
        "albums have their own setting, or \"Ten bands and presets. A song or album can keep its own setting.\" when none "
        "has. It opens the Equaliser page.",
        "The Settings frames of 1.0.0 have no Playback group: Volume levelling and Crossfade came to the code after them. "
        "This frame draws the group as ComfortSettings.kt has it on feature/per-song-equaliser."],
    "settings_equaliser_page": [
        "The page edits the default, in Lamplight. No scope head: the Default label and the note under the card say what "
        "the bands change. Scrolled, the title joins the back arrow in the bar."],
    "settings_equaliser_page_full": [
        "The whole page: the switch, the default's editor, the albums and the songs with their own setting (sorted by "
        "title, each with its preset and a button that removes the setting at once), and the phone's own equaliser when "
        "it has one."],
    "settings_equaliser_page_empty": [
        "Nothing has its own setting yet: one card says how to give a song one. Scrolled to the end of the page."],
    "settings_equaliser_page_off": [
        "Off on the page: the note is the switch row's summary (the sheet has it under its title), the chips are at "
        "50 %, the knobs lose the accent and the response is the flat 0 dB line. A chip or a band turns it on."],
}
MOCK_NOTES = {
    "sheet_equaliser_default": ["The mock's curves are placeholders and its row has no saved presets. Here the bands are "
                                "EqPresets.kt's values, the curve is the engine's response, and the demo's two saved "
                                "presets come first."],
    "settings_equaliser_page_off": ["The mock has no page with the equaliser off. The code puts the off note under Use "
                                    "equaliser as the row's summary (UseEqualiserCard), and so does this frame."],
    "settings_equaliser_page_full": ["The mock draws the albums' remove button stronger than the songs'. The spec has it "
                                     "the other way, and the code follows the spec. This frame draws both dim (see "
                                     "Proposed)."],
    "dialog_equaliser_save_preset": ["The mock shows a name being typed. The code opens the dialog with the field empty "
                                     "and Save disabled (EqPresetDialog.Save)."],
}


def _chip_items(selected: str):
    """The preset row in the spec's order: Custom and Save as preset (only while no preset matches), the saved presets,
    then the built-in eleven. (label, Kind, Selected)"""
    items = [("Custom", "Built-in", True), ("Save as preset", "Save as preset", False)] if selected == "Custom" else []
    items += [(n, "Saved", n == selected) for n in SAVED]
    return items + [(n, "Built-in", n == selected) for n in BUILT_IN]


def _chips(d: Doc, parent: int, selected: str, *, width: float, side: float, dim: bool = False) -> tuple[int, dict]:
    """The scrolling preset row as a clipped viewport: FilterChip's row (8 apart, 6 above and below), scrolled so the
    selected chip is in view with the next one showing after it."""
    vp = d.frame(parent, name="Presets", w=width, h=48, clip=True)
    rowf = d.frame(vp, name="Chip row", layout="h", gap=8, pad=[6, side, 6, side], cross="center", sizing=("hug", "hug"), x=0, y=0)
    chips = {}
    for label, kind, sel in _chip_items(selected):
        c = d.instance("Preset chip", rowf, name=f"Chip: {label}")
        d.cmd("instance.setProperties", ids=[c], properties={"Kind": kind, "Selected": str(sel)})
        setp(d, c, Label=label)
        chips[label] = c
    g = d.get(chips[selected])
    total = d.get(rowf)["width"]
    offset = max(0.0, min(g["x"] + g["width"] + 48 - width, total - width))
    d.set(rowf, x=-round(offset))
    if dim:
        d.set(rowf, opacity=0.5)
    return vp, chips


def _editor(d: Doc, parent: int, *, width: float, selected: str, on: bool = True, dragged: int | None = None,
            chip_side: float, plot_side: float) -> dict:
    """The editor both the sheet and the Settings card hold: the preset chips, then the bands' plot 4dp under them. Made
    as two frames in `parent`, for the caller to place."""
    gains = CURVES[selected]
    vp, chips = _chips(d, parent, selected, width=width, side=chip_side, dim=not on)
    bf = d.frame(parent, name="Bands", layout="v", w=width, pad=[4, plot_side, 0, plot_side], sizing=("fixed", "hug"), clip=False)
    sl = d.instance("Band sliders", bf, name="Band sliders")
    state = "Dragged" if dragged is not None else "On" if on else "Off"
    d.cmd("instance.setProperties", ids=[sl], properties={"State": state})
    d.set(sl, sizingH="fill")
    if on:
        d.cmd("instance.setProperties", ids=[d.child(sl, "Curve")], properties={"Curve": selected})
    set_bands(d, sl, gains, "On" if on else "Off", dragged)
    return {"pieces": [vp, bf], "chips": chips, "sliders": sl}


ROWS_ABOVE = 4         # EqualiserActionRows: 4dp between the plot's card and the first row


def _eq_sheet(d: Doc, ps, f: int, *, scope: str, selected: str, on: bool = True, dragged: int | None = None) -> dict:
    """ComfortEqualiserSheet over the screen in `f`: title and switch, scope head, the editor, the actions for the scope."""
    from .screens_playback import _place_bottom, _sheet
    ids = {}
    sh = _sheet(d, ps, f, "Equaliser sheet")
    title = ps.slot(sh, "Equaliser title", first=True, name="Title", props={"On": on})
    ids["switch"] = d.child(title, "Switch")
    head = ps.slot(sh, "Scope head", name="Scope head", props={"Scope": scope})
    if scope == "Default":
        ids["lead"] = d.child(head, "Lead")
    else:
        ps.props(head, {"Subtitle": "Paper Lanterns" if scope == "Album" else "Lights Over the Water"})
        cover_image(d, d.child(head, "Cover"), COVER[0])
    content = d.child(sh, "Content")
    ed = _editor(d, f, width=W, selected=selected, on=on, dragged=dragged, chip_side=16, plot_side=16)
    # No hairline over the first row: the plot's card ends just above it, and its edge is the separation.
    space = d.frame(f, name="Space above the rows", w=W, h=ROWS_ABOVE)
    d.cmd("node.reparent", ids=ed["pieces"] + [space], parent=content)
    for p in ed["pieces"] + [space]:
        d.set(p, sizingH="fill")
    ids.update(chips=ed["chips"], sliders=ed["sliders"])

    def action(key, label, icon, hairline, summary=None):
        props = {"Kind": "Two line" if summary else "Default", "End": "None", "Label": label, "Icon": f"Icon/{icon}",
                 "Show icon": True, "Show hairline": hairline, "Show value": False}
        if summary:
            props["Summary"] = summary
        ids[key] = ps.slot(sh, "Sheet row", name=f"Row: {label}", props=props)

    if scope == "Default":
        action("save_song", "Save for this song", "music_note", False)
        action("save_album", "Save for this album", "album", True)
        note = d.frame(f, name="Note", layout="v", w=W, pad=[4, 24, 0, 24], sizing=("fixed", "hug"))
        t = d.text(note, "Saved settings come back whenever the song plays.", style="Component/Note", color="Color/on-2",
                   name="Note text")
        d.set(t, sizingH="fill", textAutoResize="height")
        d.cmd("node.reparent", ids=[note], parent=content)
        d.set(note, sizingH="fill")
    elif scope == "Song":
        action("remove", "Use default", "library_music", False, "Removes this song's setting")
    else:
        action("save_song", "Save for this song", "music_note", False)
        action("remove", "Use default", "library_music", True, "Removes this album's setting")
    _place_bottom(d, sh)
    ps.gesture(f)
    ids["sheet"] = sh
    return ids


def _menu_row(d: Doc, ps, frame: int, value: str) -> int:
    """Add the Equaliser row to the track menu in `frame`, right after Sleep timer (UI-SPEC 3.1), and keep the sheet on
    the screen's bottom edge; a pop-over open on the Sleep timer row moves up with it."""
    sh = d.child(frame, "Track menu")
    content = d.child(sh, "Content")
    names = [c["name"] for c in d.get(content, depth=1)["children"]]
    before = d.get(sh)["height"]
    r = d.cmd("instance.slotInsert", id=sh, slot=ps._slot_name(sh), component=d.comp["Sheet row"],
              index=names.index("Row: Sleep timer") + 1)
    rid = r["id"] if "id" in r else r["ids"][0]
    d.set(rid, name="Row: Equaliser", sizingH="fill")
    ps.props(rid, {"Kind": "Default", "End": "Chevron", "Label": "Equaliser", "Icon": "Icon/equalizer", "Show icon": True,
                   "Show hairline": True, "Show value": True, "Value": value})
    d.override(d.child(rid, "Icon"), "Glyph", fill="Color/on-2")       # SheetRow's glyph; the swap to a new icon drops it
    g = d.get(sh)
    d.set(sh, y=H - g["height"])
    for c in d.get(frame, depth=1)["children"]:
        if c["name"] == "Sleep timer pop-over":
            d.set(c["id"], y=c["y"] - (g["height"] - before))
    return rid


def _own_rows(d: Doc, col: int):
    """The albums and the songs with their own setting, as the Settings page lists them."""
    g, _ = group(d, col, "Albums")
    for i, (title, artist, cov, preset) in enumerate(OWN_ALBUMS):
        r = row(d, g, "Collection row", f"Album: {title}", Kind="Album", Size="Medium", Title=title, Subtitle=artist,
                Value=preset, **{"Show subtitle": True, "Show value": True, "Show chevron": True, "Show overline": False,
                                 "Show hairline": i > 0})
        # Swap first: swapping a nested instance drops the row's other overrides.
        ic = d.child(r, "Chevron icon")
        d.cmd("instance.swap", ids=[ic], component=d.comp["Icon/close"])
        d.override(r, "Chevron icon", width=24, height=24, name="Remove")
        d.override(ic, "Glyph", fill="Color/on-2")        # ComfortIconButton(dim = true)
        cover_image(d, d.child(r, "Lead"), COVER[cov])
    return g


def _own_songs(d: Doc, col: int):
    g, _ = group(d, col, "Songs")
    for i, (title, artist, cov, preset) in enumerate(OWN_SONGS):
        r = row(d, g, "Song row", f"Song: {title}", Lead="Cover", Current="False", Title=title, Subtitle=artist, Meta=preset,
                **{"Show subtitle": True, "Show meta": True, "Show trailing": True, "Show hairline": i > 0})
        tr = d.child(r, "Trailing")
        d.cmd("instance.setProperties", ids=[tr], properties={"Kind": "Add", "Tone": "Default"})
        ic = d.child(tr, "Icon")
        d.cmd("instance.swap", ids=[ic], component=d.comp["Icon/close"])
        d.override(ic, "Glyph", fill="Color/on-2")        # ComfortIconButton(dim = true), as the album rows have it
        d.set(tr, name="Remove")
        cover_image(d, d.child(r, "Cover"), COVER[cov])
    return g


def _page(d: Doc, ss, a, key: str, name: str, *, h: float = H, empty: bool = False, scrolled: bool = False,
          on: bool = True) -> dict:
    """ComfortEqualiserPage: a root page like the licence text's, the default's editor in one card, then the lists."""
    ids = {}
    f, col = ss.phone(a, key, name, PAGE_SRC, h=h)
    ss.hero(col, "Equaliser", short=True)
    ids["back"] = ss.lead_tools(col)
    g, _ = group(d, col, "Use equaliser")
    if on:
        ids["switch"] = row(d, g, "Settings row", "Use equaliser", Control="Switch on", Title="Use equaliser")
    else:
        # ComfortEqualiserPage's UseEqualiserCard: the off note is the switch row's summary.
        ids["switch"] = row(d, g, "Settings row", "Use equaliser", Control="Switch off", Title="Use equaliser",
                            Summary="Off. Choose a preset or move a band to turn it on.", **{"Show summary": True})
    ss.label(col, "Default")
    g, slot = group(d, col, "Default")
    card = d.frame(f, name="Editor", layout="v", w=379, pad=[10, 0, 8, 0], sizing=("fixed", "hug"), clip=False)
    ed = _editor(d, card, width=379, selected="Bass boost", on=on, chip_side=12, plot_side=8)
    for p in ed["pieces"]:
        d.set(p, sizingH="fill")
    for c in d.get(slot, depth=1).get("children", []):
        d.cmd("edit.delete", ids=[c["id"]])
    d.__dict__.setdefault("_slot_used", {})[g] = True
    d.cmd("node.reparent", ids=[card], parent=slot)
    d.set(card, sizingH="fill")
    inst(d, "Note", col, "Note", Text="Every song without its own setting plays with this.")
    if empty:
        ss.label(col, "Songs and albums with their own setting")
        g, _ = group(d, col, "Nothing yet")
        block(d, g, "None yet", text="None yet. Open Equaliser from the menu on Now Playing and choose “Save for this song”.")
    else:
        ss.label(col, "Albums with their own setting")
        _own_rows(d, col)
        ss.label(col, "Songs with their own setting")
        _own_songs(d, col)
    ss.label(col, "On this phone")
    g, _ = group(d, col, "Phone equaliser")
    r = row(d, g, "Settings row", "Phone equaliser", Control="Chevron", Title="Phone equaliser",
            Summary="Opens the equaliser on this phone.", **{"Show summary": True})
    ic = d.child(r, "Chevron")
    d.cmd("instance.swap", ids=[ic], component=d.comp["Icon/open_in_new"])
    d.override(ic, "Glyph", fill="Color/on-3")
    d.set(ic, name="Open")
    ss.gap(col, 8 + 182 - 30, "Space for the dock")
    total = d.get(col)["height"] + 24       # the last card clears the dock by 28dp at the end of the scroll
    if h != H:
        d.set(f, height=total)
        h = total
    if scrolled:
        d.set(col, y=-(total - H))
        tb = ss.collapsed_bar(f, "Equaliser")
        ids["back"] = d.child(tb, "Back")
    ss.chrome(f, h=h)
    ids["frame"], ids["height"] = f, h
    return ids


def _playback_settings(d: Doc, ss, a, key: str, name: str) -> dict:
    """Settings scrolled to Playback (ComfortSettings.kt on feature/per-song-equaliser): Volume levelling, Crossfade and
    the Equaliser row, then Lyrics and Song radio as the Settings frames have them."""
    f, col = ss.phone(a, key, name, SETTINGS_SRC)
    ss.label(col, "Playback")
    g, _ = group(d, col, "Playback")
    row(d, g, "Settings row", "Volume levelling", Control="Switch on", Title="Volume levelling",
        Summary="Plays songs at a similar loudness, from their ReplayGain tags or the level measured during analysis. Songs "
                "not measured yet play as they are.", **{"Show summary": True})
    row(d, g, "Settings row", "Crossfade", Control="Value", Title="Crossfade", Value="6 seconds",
        Summary="Songs from one album played in order still run straight into each other.",
        **{"Show summary": True, "Show hairline": True})
    eq = row(d, g, "Settings row", "Equaliser", Control="Value and chevron", Title="Equaliser", Value="Bass boost",
             Summary="With their own setting: 4 songs, 2 albums", **{"Show summary": True, "Show hairline": True})
    ss.label(col, "Lyrics")
    g, _ = group(d, col, "Lyrics")
    block(d, g, "Lyrics: saved", text="51 of 69 songs have lyrics saved on this phone. Saved lyrics work offline.",
          bold="51 of 69 songs", small_above="Downloading sends each song's title, artist, album and length to lrclib.net. "
                                             "Your music files are never sent.", button="Download lyrics for all songs")
    ss.label(col, "Song radio")
    g, _ = group(d, col, "Song radio")
    row(d, g, "Settings row", "Autoplay", Control="Switch on", Title="Autoplay",
        Summary="When a queue reaches its last song, radio keeps going with similar songs.", **{"Show summary": True})
    block(d, g, "Song analysis", text="Radio finds similar songs by listening to your files. This happens on this phone, "
          "and nothing is uploaded.", progress=1.0, small="All 69 songs analysed.", after_row=True)
    ss.gap(col, 8 + 182 - 30, "Space for the dock")
    d.set(col, y=96 - 40)       # under the collapsed bar: the status bar and the 56dp Top bar
    tb = ss.collapsed_bar(f, "Settings")
    ss.chrome(f)
    return {"row": eq, "back": d.child(tb, "Back"), "frame": f}


def _screens(d: Doc, ctx: dict, k: K) -> dict:
    from .screens_playback import S as PS, _dark_sheet_fix, _dialog, _player, _track_menu
    from .screens_settings import S as SS
    ps, ss = PS(d, ctx), SS(d, ctx)
    t: dict[str, dict] = {}          # key -> the layers the prototype starts from
    a = d.area("Equaliser", note="Attic's own ten-band equaliser. The sheet opens from the track menu on Now Playing and "
               "edits the setting the song plays with: the default, the album's or the song's own. A song or album with "
               "its own setting shows in Now Playing's header and in Settings > Playback > Equaliser, which edits the "
               "default. Over Now Playing the sheet takes the cover's colours; in Settings it is Lamplight. Presets are "
               "playback/eq/EqPresets.kt's; the curves are the engine's response.")

    def over_player(key, name, source=SHEET_SRC):
        f = a.screen(key, name=f"Equaliser / {name}", source=source)
        _player(d, ps, f)
        ps.scrim(f)
        d.set_mode(f, "Color", WASH)
        return f

    # -- the track menu from Now Playing
    f = over_player("sheet_track_menu_equaliser", "Track menu", MENU_SRC)
    _track_menu(d, ps, f, title="Lights Over the Water", artist="Mira Okafor", cover=0, liked=True, now_playing=True)
    t["sheet_track_menu_equaliser"] = {"row": _menu_row(d, ps, f, "Bass boost"), "scrim": d.child(f, "Scrim")}

    # -- the sheet in each scope
    for key, name, kw in (("sheet_equaliser_default", "Sheet, default", {"scope": "Default", "selected": "Bass boost"}),
                          ("sheet_equaliser_song", "Sheet, this song", {"scope": "Song", "selected": "Custom"}),
                          ("sheet_equaliser_album", "Sheet, this album", {"scope": "Album", "selected": "Vocal"}),
                          ("sheet_equaliser_off", "Sheet, off", {"scope": "Default", "selected": "Bass boost", "on": False})):
        f = over_player(key, name)
        t[key] = _eq_sheet(d, ps, f, **kw)
        t[key]["scrim"] = d.child(f, "Scrim")

    # -- Save as preset
    f = over_player("dialog_equaliser_save_preset", "Save as preset")
    _eq_sheet(d, ps, f, scope="Song", selected="Custom")
    d.set(d.child(f, "Gesture bar"), visible=False)      # the keyboard draws over it; the bar goes on last
    ps.scrim(f)
    kb = ps.inst("Keyboard (system)", f, x=0, y=H - 300, name="Keyboard")
    dl = _dialog(d, ps, f, content="Field", confirm="Disabled", title="Save as preset", confirm_label="Save")
    fld = d.child(dl, "Field")
    ps.props(fld, {"State": "Empty", "Label": "Preset name", "Show caret": True})
    d.set(dl, y=H - 300 - 22 - d.get(dl)["height"])      # ComfortDialog rides above the keyboard, 22dp over it
    ps.gesture(f)
    t["dialog_equaliser_save_preset"] = {"cancel": d.child(dl, "Dismiss")}

    # -- a saved preset's menu
    f = over_player("menu_equaliser_preset", "Saved preset menu")
    ids = _eq_sheet(d, ps, f, scope="Default", selected="Car speakers")
    chip = d.get(ids["chips"]["Car speakers"])
    cy = chip["absoluteBounds"]["y"] - d.get(f)["absoluteBounds"]["y"]
    m = ps.inst("Menu", f, x=16, y=round(cy + chip["height"] + 4), name="Preset menu", props={"On sheet": "True"})
    ps.slot(m, "Menu row", first=True, name="Row: Rename", props={"Kind": "Default", "Label": "Rename", "Icon": "Icon/edit",
                                                                  "Show icon": True, "Show check": False})
    ps.slot(m, "Menu row", name="Row: Delete preset", props={"Kind": "Danger", "Label": "Delete preset", "Icon": "Icon/delete",
                                                             "Show icon": True, "Show check": False})
    ids["scrim"] = d.child(f, "Scrim")
    t["menu_equaliser_preset"] = ids

    # -- a band held
    f = over_player("sheet_equaliser_band_dragged", "Sheet, band dragged")
    t["sheet_equaliser_band_dragged"] = _eq_sheet(d, ps, f, scope="Song", selected="Custom", dragged=DRAGGED_BAND)

    # -- Now Playing with the indicator
    f = a.screen("now_playing_equaliser", name="Equaliser / Now Playing, this song", source=NP_SRC)
    pids = _player(d, ps, f)
    hd = d.child(f, "Header")
    setp(d, hd, **{"Show equaliser": True})
    ind = d.child(hd, "Indicator")
    d.cmd("instance.setProperties", ids=[ind], properties={"Scope": "Song"})
    d.set_mode(f, "Color", WASH)
    t["now_playing_equaliser"] = {"indicator": ind, "close": pids["close"]}

    # -- Settings
    t["settings_equaliser_row"] = _playback_settings(d, ss, a, "settings_equaliser_row", "Equaliser / Settings, Playback")
    t["settings_equaliser_page"] = _page(d, ss, a, "settings_equaliser_page", "Equaliser / Settings page")
    t["settings_equaliser_page_full"] = _page(d, ss, a, "settings_equaliser_page_full",
                                              "Equaliser / Settings page (full length)", h=2000)
    t["settings_equaliser_page_empty"] = _page(d, ss, a, "settings_equaliser_page_empty",
                                               "Equaliser / Settings page, nothing saved", empty=True, scrolled=True)
    t["settings_equaliser_page_off"] = _page(d, ss, a, "settings_equaliser_page_off", "Equaliser / Settings page, off",
                                             on=False)
    a.close()

    # On a dark sheet a neutral or tonal box is one step up (fill-2), as the playlist picker's is.
    _dark_sheet_fix(d, ps, "sheet_track_menu_equaliser")
    for key in ("sheet_equaliser_default", "sheet_equaliser_off", "menu_equaliser_preset"):
        lead = d.screens[key]["map"].get(t[key]["lead"])
        d.override(lead, "Container", fill="Color/fill-2")
    return t


# ============================================================================== the track menus that were here
REWORD = [
    ("the code shows the song and Sleep timer only.", "the code shows the song, Sleep timer and Equaliser."),
    ("so the menu keeps only the sleep timer (ComfortTrackMenu: state.external).",
     "so the menu keeps only the sleep timer and the equaliser, which plays it with the default (ComfortTrackMenu: "
     "state.external)."),
    ("Its menu has only Sleep timer.", "Its menu has only Sleep timer and Equaliser."),
    ("the code puts Sleep timer first among the rows and adds Share; the mock has neither.",
     "the code puts Sleep timer and Equaliser first among the rows and adds Share; the mock has none of them."),
]


def _old_menus(d: Doc, ctx: dict) -> dict:
    """The track menus over Now Playing drawn for 1.0.0 gain the Equaliser row (the code shows it wherever Sleep timer
    is), and their notes say so. Returns {key: {theme: row}}."""
    from .screens_playback import S as PS
    ps = PS(d, ctx)
    rows = {}
    for key, name in OLD_MENUS:
        s = _adopt(d, ctx, key, name, MENU_SRC)
        rows[key] = {th: _menu_row(d, ps, s[th], "Bass boost") for th in ("light", "dark")}
    frames = [d.screens[k][th] for k, _ in OLD_MENUS for th in ("light", "dark")]
    frames += [ctx["frames"][f"Now Playing / Opened with Attic · {th}"] for th in ("Light", "Dark")]
    for fid in frames:
        for an in d.cmd("annotation.list", id=fid).get("annotations", []):
            label = an["label"]
            for old, new in REWORD:
                label = label.replace(old, new)
            if label != an["label"]:
                d.cmd("annotation.edit", id=fid, index=an["index"], label=label)
    return rows


# ============================================================================== prototype
def _interaction(d: Doc, layer: int, dest: int | None, kind: str, trigger: str = "onClick"):
    from .flows import MOTION
    params = {"id": layer, "trigger": {"type": trigger}}
    if kind == "back":
        params["action"] = {"type": "back"}
    else:
        tr, ms, ease = MOTION[kind]
        params["actions"] = [{"type": "navigate", "dest": dest}]
        params["transition"] = dict(tr)
        if tr["type"] != "instant":
            params["durationMs"], params["easing"] = ms, ease
    d.cmd("proto.interaction.add", **params)


def _prototype(d: Doc, t: dict, old_rows: dict) -> list:
    """Wire the equaliser's screens, Light to Light and Dark to Dark, and give the flows their starting points."""
    made = []

    def link(src, layer, dst, kind="sheet", trigger="onClick"):
        s = d.screens[src]
        dark = s["map"].get(layer)
        if dark is None:
            raise KeyError(f"{src}: layer {layer} has no Dark twin")
        for th, lay in (("light", layer), ("dark", dark)):
            _interaction(d, lay, None if dst == "back" else d.screens[dst][th], "back" if dst == "back" else kind, trigger)
            made.append((src, dst, th, lay))

    link("sheet_track_menu_equaliser", t["sheet_track_menu_equaliser"]["row"], "sheet_equaliser_default")
    for key in ("sheet_track_menu_equaliser", "sheet_equaliser_default", "sheet_equaliser_song", "sheet_equaliser_album",
                "sheet_equaliser_off", "menu_equaliser_preset"):
        link(key, t[key]["scrim"], "back")
    df = t["sheet_equaliser_default"]
    link("sheet_equaliser_default", df["save_song"], "sheet_equaliser_song")
    link("sheet_equaliser_default", df["save_album"], "sheet_equaliser_album")
    link("sheet_equaliser_default", df["switch"], "sheet_equaliser_off")
    link("sheet_equaliser_default", df["chips"]["Car speakers"], "menu_equaliser_preset")
    link("sheet_equaliser_off", t["sheet_equaliser_off"]["switch"], "sheet_equaliser_default")
    sg = t["sheet_equaliser_song"]
    link("sheet_equaliser_song", sg["remove"], "sheet_equaliser_default")
    link("sheet_equaliser_song", sg["chips"]["Save as preset"], "dialog_equaliser_save_preset")
    link("sheet_equaliser_song", d.child(sg["sliders"], f"Band {BAND_NAMES[DRAGGED_BAND]}"), "sheet_equaliser_band_dragged",
         kind="instant", trigger="onPress")
    al = t["sheet_equaliser_album"]
    link("sheet_equaliser_album", al["save_song"], "sheet_equaliser_song")
    link("sheet_equaliser_album", al["remove"], "sheet_equaliser_default")
    link("dialog_equaliser_save_preset", t["dialog_equaliser_save_preset"]["cancel"], "back")
    link("now_playing_equaliser", t["now_playing_equaliser"]["indicator"], "sheet_equaliser_song")
    link("now_playing_equaliser", t["now_playing_equaliser"]["close"], "back")
    link("settings_equaliser_row", t["settings_equaliser_row"]["row"], "settings_equaliser_page", kind="detail")
    for key in ("settings_equaliser_row", "settings_equaliser_page", "settings_equaliser_page_full",
                "settings_equaliser_page_empty", "settings_equaliser_page_off"):
        link(key, t[key]["back"], "back")
    link("settings_equaliser_page", t["settings_equaliser_page"]["switch"], "settings_equaliser_page_off", kind="instant")
    link("settings_equaliser_page_off", t["settings_equaliser_page_off"]["switch"], "settings_equaliser_page", kind="instant")
    # The menus that were in the file: their new row opens the sheet too.
    for key, rows in old_rows.items():
        for th in ("light", "dark"):
            _interaction(d, rows[th], d.screens["sheet_equaliser_default"][th], "sheet")
            made.append((key, "sheet_equaliser_default", th, rows[th]))
    d.page("Screens")
    for key, name, desc in (
            ("sheet_track_menu_equaliser", "Equaliser",
             "The track menu on Now Playing, Equaliser, the sheet; save for this song, name a preset, hold a band, switch "
             "it off."),
            ("settings_equaliser_row", "Equaliser in Settings",
             "Settings at Playback: the Equaliser row opens the page that edits the default and lists what has its own "
             "setting.")):
        start = d.screens[key]["light"]
        d.cmd("proto.flow.add", start=start, name=name)
        d.cmd("proto.flow.rename", start=start, name=name, description=desc)
    return made


# ============================================================================== handoff, flow overview, cover
MOCK_FILE = "docs/design/2026-11-21-ui-directions/oneui/11-equaliser.html"


def _handoff(d: Doc):
    for key in KEYS:
        s = d.screens[key]
        for fid in (s["light"], s["dark"]):
            d.annotate(fid, f"Compose: {s['source']}", "Source")
            for n in MOCK_NOTES.get(key, []):
                d.annotate(fid, f"{n} Mock: {MOCK_FILE}", "Code differs from mock")
            for n in NOTES.get(key, []):
                d.annotate(fid, n, "Note")
            for n in PROPOSED.get(key, []):
                d.annotate(fid, n, "Proposed")
        if key not in PROPOSED:
            d.ready_for_dev([s["light"], s["dark"]])


def _flow_lane(d: Doc, ctx: dict) -> int:
    from .flows import _lane
    _adopt(d, ctx, "now_playing", "Now Playing / Playing", NP_SRC)
    _adopt(d, ctx, "sheet_track_menu_now_playing", "Track menu / From Now Playing", MENU_SRC)
    pid = d.page("Flow overview")
    y = max(c["y"] + c["height"] for c in d.get(pid, depth=1)["children"]) + 64
    again = "Equaliser / Sheet, this song (above)"
    lane = _lane(d, "Equaliser", "The sheet opens from the track menu on Now Playing and edits the setting the song plays "
                 "with; saving gives the song or its album its own. A song with its own setting shows the indicator on Now "
                 "Playing. Settings > Playback > Equaliser edits the default and lists what has its own setting.", [
        ("np", ["now_playing"], None, 0, 0, "ref"),
        ("menu", ["sheet_track_menu_now_playing"], None, 1, 0, "ref"),
        ("sheet", ["sheet_equaliser_default"], None, 2, 0, "screen"),
        ("song", ["sheet_equaliser_song"], None, 3, 0, "screen"),
        ("save", ["dialog_equaliser_save_preset"], None, 4, 0, "screen"),
        ("set", ["settings_equaliser_row"], None, 0, 1, "screen"),
        ("page", ["settings_equaliser_page", "settings_equaliser_page_full", "settings_equaliser_page_empty",
                  "settings_equaliser_page_off"], None, 1, 1, "screen"),
        ("off", ["sheet_equaliser_off"], None, 2, 1, "screen"),
        ("album", ["sheet_equaliser_album"], None, 3, 1, "screen"),
        ("drag", ["sheet_equaliser_band_dragged"], None, 4, 1, "screen"),
        ("npi", ["now_playing_equaliser"], None, 0, 2, "screen"),
        ("again", ["sheet_equaliser_song"], again, 1, 2, "ref"),
        ("pmenu", ["menu_equaliser_preset"], None, 3, 2, "screen"),
    ], [
        ("np", "menu", "Tap More", "go"),
        ("menu", "sheet", "Equaliser", "go"),
        ("sheet", "song", "Save for this song", "go"),
        ("song", "save", "Save as preset", "go"),
        ("sheet", "off", "The switch", "go"),
        ("sheet", "album", "Save for this album", "go"),
        ("sheet", "pmenu", "A saved preset, twice", "go"),
        ("song", "drag", "Hold a band", "go"),
        ("set", "page", "Equaliser", "go"),
        ("npi", "again", "Tap the indicator", "go"),
    ], y)
    card = d.child(lane, f"Card: {again}")
    d.set(d.child(card, "Key"), characters="Drawn in the first row of this lane")
    d.set(d.child(card, "Name"), characters="Equaliser / Sheet, this song")
    return lane


CHANGES_1_1_0 = [
    "The equaliser, drawn from its UI spec, ui/model/EqualiserModel.kt and the demo states in storeShots "
    "EqualiserDemo.kt on feature/per-song-equaliser. Presets are playback/eq/EqPresets.kt's values and every curve is "
    "the engine's response.",
    "New components: Preset chip, Response curve, Response well, Band slider and Band sliders (Actions); Equaliser "
    "title and Scope head (Overlays); Equaliser indicator (Playback).",
    "Changed components: Sheet row gains Kind=Two line and Summary; Settings row gains Control=Value and chevron; Now "
    "playing header gains Show equaliser and the nested Indicator.",
    "Screens: the section Equaliser, 13 screens in Light and Dark: the track menu, the sheet for the default, a song, "
    "an album and switched off, Save as preset, a saved preset's menu, a band held, Now Playing with the indicator, "
    "Settings at Playback, and the Equaliser page (top, full length, nothing saved).",
    "The track menus over Now Playing (From Now Playing, Opened with Attic and the three Sleep timer screens) gain the "
    "Equaliser row after Sleep timer.",
    "Type: five Component/Equaliser styles. Icons: equalizer and transition_fade, which Symbols.kt has gained.",
    "Prototype: the flows Equaliser and Equaliser in Settings. Flow overview: the lane Equaliser.",
]
CHANGES_1_2_0 = [
    "Design review of the equaliser as built. The code was matched to the changes below on the same branch.",
    "Band sliders is now the whole plot. The response is drawn behind the knobs, on one fill card, at the knobs' scale "
    "(6.5dp to a decibel), so the line passes through every knob and the +12, 0 and −12 labels read for both. Before, "
    "the same curve was drawn twice at two scales: in a 120dp well at 3.4dp to a decibel, and as the knobs under it.",
    "Response well is removed. Response curve is 339 x 184. Band slider is the knob alone: its track belongs to the "
    "plot, and the accent run from 0 dB to the knob is gone, since the tinted area under the line shows the same.",
    "The equaliser sheet is shorter by the well and its gaps, which keeps Save for this song and Save for this album on "
    "screen on a short phone. Its action rows start 4dp under the plot's card, with no hairline over the first one.",
    "Settings page: the editor card ends 8dp under the plot, and the remove button is dim on the song rows as on the "
    "album rows. New screen: Settings page, off, with the off note as the switch row's summary, as the code has it.",
    "Fix: the Equaliser row's glyph in the six track menus is on-2, like every other row's, as SheetRow draws it.",
    "Unchanged, because the code is right: the title row and switch, the scope head, the preset chips and their menu, "
    "the action rows, the dialogs, the Now Playing indicator, the track menu row and the Settings row.",
]
ENTRIES = [("1.1.0", CHANGES_1_1_0), (VERSION, CHANGES_1_2_0)]       # oldest first; each goes on top of the last


def _cover(d: Doc):
    """Version, date and what the file matches on the Cover, and an entry for each version on top of the Changelog."""
    cover = d.child(d.page("Cover"), "Cover")
    for meta, value in (("Meta: Version", VERSION), ("Meta: Last updated", UPDATED),
                        ("Meta: Matches", "ui-redesign at 4fe4dab + the equaliser")):
        d.set(d.child(d.child(cover, meta), "Value"), characters=value)
    pid = d.page("Changelog")
    tops = {c["name"]: c for c in d.get(pid, depth=1)["children"]}
    entries, howto = tops["Entries"], tops["How to add an entry"]
    for version, changes in ENTRIES:
        i = d.instance("Doc/Changelog entry", entries["id"], name=f"Entry {version}")
        d.cmd("node.reorder", id=i, index=0)
        d.cmd("instance.setProperties", ids=[i], properties={"Version": version, "Date": UPDATED, "Author": "Attic maintainers",
                                                             "Changes": "\n".join(changes)})
        d.set(i, sizingH="fill")
        d.set(d.child(i, "Changes"), listStyle="bulleted", paragraphSpacing=10)
    grown = d.get(entries["id"])["height"] - entries["height"]
    d.set(howto["id"], y=howto["y"] + grown)


# ============================================================================== build
def build(d: Doc, ctx: dict):
    attach(d, ctx)
    if "Band slider" in d.comp:
        raise SystemExit("this file already has the equaliser (a Band slider component); nothing to add")
    k = K(d, ctx["tokens"])
    ctx["descriptions"] = _descriptions(d)
    _styles(d)
    ctx["equaliser_icons"] = _icons_added(d)
    _actions(d, k)
    _lists(d, ctx, k)
    _playback(d, ctx, k)
    _overlays(d, ctx, k)
    t = _screens(d, ctx, k)
    old_rows = _old_menus(d, ctx)
    ctx["equaliser_links"] = _prototype(d, t, old_rows)
    _handoff(d)
    _flow_lane(d, ctx)
    _cover(d)
    d.page("Cover")


def main(argv=None):
    ap = argparse.ArgumentParser(description="Add the equaliser to an existing Attic Comfort file.")
    ap.add_argument("src", type=Path)
    ap.add_argument("dst", type=Path)
    ap.add_argument("--export", type=Path, help="write every new frame here at 2.625x")
    a = ap.parse_args(argv)
    d = Doc("Attic Comfort", read_roots=[a.src.resolve().parent], write_roots=[a.dst.resolve().parent])
    try:
        d.tool("open_file", path=str(a.src.resolve()))
        d.cmd("text.refit")
        ctx = {"style": "comfort"}
        build(d, ctx)
        a.dst.parent.mkdir(parents=True, exist_ok=True)
        d.save(a.dst.resolve())
        print(f"saved {a.dst} ({a.dst.stat().st_size // 1024} KB); icons added: {ctx['equaliser_icons']}; "
              f"{len(ctx['equaliser_links'])} interactions")
        if a.export:
            for key in KEYS:
                for th in ("light", "dark"):
                    d.export_png(d.screens[key][th], a.export / f"{key}-{th}.png", scale=DENSITY)
            print(f"exported {len(KEYS) * 2} frames to {a.export}")
    finally:
        d.close()


if __name__ == "__main__":
    sys.exit(main())
