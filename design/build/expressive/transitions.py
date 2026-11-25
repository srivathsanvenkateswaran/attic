"""Smart transitions in Attic Expressive: a proposal, drawn before the code (version 1.1.0).

Unlike the modules that built 1.0.0 this one adds to a file that already exists. `Attic Expressive.figcraft` is
the source now, so nothing here rebuilds what the file holds: `attach` reads the opened file's pages, styles,
tokens and components into the Doc, and `build` adds to them.

    cd design/build
    python3 -m expressive.transitions "../Attic Expressive.figcraft" /tmp/attic-design/expressive-transitions.figcraft \
        [--export /tmp/attic-dev/smart-transitions-design]

Look at the result, then copy it over the file in design/. It refuses a file that already has the Transitions
section. What it shares with the equaliser's module on feature/per-song-equaliser (the two glyphs, List row/Setting's
Status line=Plain and Trailing=Icon) it adds only when the file lacks it, so it runs on a file with or without
the equaliser.

The feature is docs/features/smart-transitions.md; section 4 is the brief. Nothing here is in the app yet, so
every new frame carries a `Proposed` annotation and none is marked ready for development.

Icons (page Icons)
------------------
Icon/transition_fade, Icon/equalizer: glyphs Symbols.kt has that the file lacked. Settings > Playback uses both.

Changed components
------------------
List row/Setting   gains Status line=Plain (Status() in ExpressiveSettings.kt: a third line in body small
                   on-surface-variant, straight under the summary) and Trailing=Icon (a row that leaves the app).
Seek bar           gains the layer "Next song mark" (hidden) and the boolean "Show next song mark": a 4dp dot in
                   on-surface-variant on the rest of the track, where the next song starts coming in.

Screens (section "Transitions" on the Screens page, Light and Dark)
------------------------------------------------------------------
transitions_settings_off, transitions_settings_crossfade, transitions_settings_smart, transitions_picker,
transitions_length_picker, transitions_now_playing.

Prototype
---------
Flow "12 Transitions" starts at Settings with transitions off: the Transitions row opens its menu, Smart shows
the length row, which opens its own menu.

Handoff
-------
A Source and a Proposed annotation on every new frame; Cover 1.1.0; a Changelog entry dated 2026-11-25.

In a build from scratch (`figbuild.run expressive`) this module does nothing but say so: it copies frames that
only a finished file has. Build 1.0.0 first, then run this module on the saved file.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from figbuild.kit import Doc, V, Area, DENSITY, _round, _enum
from figbuild import icons as _icons
from expressive import foundations
from expressive.foundations import DocPage, bind_dim
from expressive.components_core import frame, inst, vname

REFS: dict[str, str] = {}

VERSION = "1.1.0"
UPDATED = "2026-11-25"
AUTHOR = "Attic maintainers"
SECTION = "Transitions"
SCREENS = "Screens"
DOC = "docs/features/smart-transitions.md"
PROPOSED = "Proposed"

# Every string the proposal shows. The ones marked (today) are in res/values/strings.xml already.
S = {
    "playback": "Playback",                                                     # settings_playback (today)
    "volume": "Volume levelling",                                               # settings_volume_levelling (today)
    "volume_body": "Plays songs at a similar loudness, from their ReplayGain tags or the level measured during "
                   "analysis. Songs not measured yet play as they are.",        # settings_volume_levelling_body (today)
    "transitions": "Transitions",
    "off": "Off",                                                               # settings_crossfade_off (today)
    "crossfade": "Crossfade",                                                   # settings_crossfade (today)
    "smart": "Smart",
    "off_body": "The next song starts when this one ends.",
    "crossfade_body": "Fades each song into the next over the crossfade length.",
    "smart_body": "Starts the next song as this one winds down, skips silence and lines up the beats when the "
                  "tempos are close.",
    "albums": "Songs from one album played in order still run straight into each other.",   # settings_crossfade_body (today)
    "length": "Crossfade length",
    "length_smart_body": "Smart uses this for songs that aren't analysed yet.",
    "equaliser": "Equaliser",                                                   # settings_equaliser (today)
    "equaliser_body": "Opens the equaliser on this phone.",                     # settings_equaliser_body (today)
}
MODES = [S["off"], S["crossfade"], S["smart"]]
LENGTHS = [2, 4, 6, 8, 10, 12]
LENGTH = 6                                  # the default, and DemoStates.kt's crossfadeSeconds


def seconds(n: int) -> str:
    return f"{n} seconds"                   # plurals settings_crossfade_seconds; no choice is 1


def status(mode: str) -> str:
    """The Transitions row's third line: what the mode does, then (while songs overlap) what it leaves alone."""
    if mode == S["off"]:
        return S["off_body"]
    return (S["crossfade_body"] if mode == S["crossfade"] else S["smart_body"]) + " " + S["albums"]


# The Now Playing mark. The demo song is 3:52 at 1:18; the planned overlap starts at 3:41.
SONG_MS, MARK_MS = 232_000, 221_000
SEEK_W, TRACK, THUMB, GAP = 363.0, 4.0, 4.0, 6.0


def handle_x(fraction: float, width: float = SEEK_W) -> float:
    """The centre of SeekBar's handle at a fraction of the song (PlayControls.kt: thumbX)."""
    return (TRACK / 2 + GAP + THUMB / 2) + (width - TRACK - 2 * GAP - THUMB) * fraction


MARK_X = round(handle_x(MARK_MS / SONG_MS), 2)


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


def card_of(d: Doc, page: str, component: int) -> int:
    """The spec card on `page` that holds a component or set."""
    for c in cards(d, page):
        if any(k["id"] == component for k in d.get(c["id"], depth=1).get("children", [])):
            return c["id"]
    raise KeyError(f"no card on {page} holds {component}")


def _described(d: Doc, set_name: str, page: str, ctx: dict, more: str):
    """Add to a component's description, on the set's variants (FigCraft keeps none on a set) or on the single
    component, and to the copy of it on the component's spec card."""
    info = ctx["components"][set_name]
    holders = list(info["variants"].values()) or [info["id"]]
    body, sep, tail = d.get(holders[0], fields=["description"])["description"].partition(" Compose: ")
    d.set(holders, description=body + more + sep + tail)
    d.set(d.child(top_named(d, card_of(d, page, info["id"]), "Heading"), "Description"), characters=body + more)


# ───────────────────────────────────────────────────────────────── icons
NEW_ICONS = ["transition_fade", "equalizer"]


def build_icons(d: Doc, ctx: dict):
    """The glyphs the file lacks, made as foundations.build_icons_page makes them and added to its grid."""
    page = d.page("Icons")
    points = {lig: cp for _, lig, cp in _icons.symbols()}
    made = []
    for lig in NEW_ICONS:
        nm = f"Icon/{lig}"
        if nm in d.comp:
            continue
        cp = points[lig]
        grid = d.child(top_named(d, page, "Card · Outlined"), "Icon grid")
        c = d.component(None, name=nm, w=24, h=24, x=-2000, y=0,
                        description=f"Material Symbols Rounded `{lig}` (U+{cp:04X}), FILL 0, wght 400, opsz 24. "
                                    f"Compose: Symbol.{_enum(lig)}. Drawn 0.1em below the font box, where SymbolIcon's "
                                    f"text layout puts it.")
        g = d.vector(c, name="Glyph", path=_round(_icons.path(cp, fill=0)), fill="Color/on-surface",
                     constraints={"horizontal": "scale", "vertical": "scale"})
        d.set(g, y=round(d.get(g)["y"] + 2.4, 3))
        cell = d.frame(grid, name=f"Icon cell · {lig}", layout="v", gap=10, cross="center",
                       pad=[20, 8, 16, 8], fill="Color/surface", radius=16, sizing=("hug", "hug"))
        d.set(cell, sizingH="fixed", width=132)
        d.cmd("node.reparent", ids=[c], parent=cell)
        d.text(cell, lig, style="Doc/Caption", color="Canvas/label", name="Ligature", textAlign="center")
        d.register(nm, c)
        d.icon_names.add(nm)
        made.append(nm)
    if made:
        # The page's intro counts the glyphs.
        intro = top_named(d, page, "Intro")
        text = d.get(intro)["characters"]
        count = len([n for n in d.icon_names if not n.endswith("(filled)")])
        new = re.sub(r"only the \d+ glyphs", f"only the {count} glyphs", text)
        if new != text:
            d.set(intro, characters=new)
        restack(d, "Icons")
    ctx["transitions_icons"] = made


# ───────────────────────────────────────────────────────────────── changes to components the file had
SETTING_NOTE = (" Status line Plain is Status(): a third line in body small on-surface-variant, straight under the "
                "summary (what the chosen transition does). Trailing Icon is a row that leaves the app: a 20dp glyph "
                "in on-surface-variant, 10dp down (Equaliser).")
SEEK_NOTE = (" Proposed, not in the code yet: Show next song mark draws a 4dp dot in on-surface-variant on the rest "
             "of the track where the next song starts coming in, at the x the handle's centre has at that moment. It "
             "shows only while Smart has a plan for the song and the handle hasn't reached it, as StoryProgress draws "
             "a stop. Shown at 3:41 of 3:52.")


def update_setting_row(d: Doc, ctx: dict):
    """List row/Setting gains Status line=Plain (the code's Status()) and Trailing=Icon (a row that leaves the app),
    whichever the file lacks."""
    C = ctx["components"]
    d.page("Components / Lists and cards")
    info, P = C["List row/Setting"], C["List row/Setting"]["props"]
    preview = top_named(d, card_of(d, "Components / Lists and cards", info["id"]), "Dark preview")
    added = []

    def add(base: dict, prop: str, value: str) -> int:
        before = {k["id"] for k in d.get(info["id"], depth=1)["children"]}
        d.cmd("component.addVariant", id=variant(ctx, "List row/Setting", **base), property=prop, value=value)
        new = [k["id"] for k in d.get(info["id"], depth=1)["children"] if k["id"] not in before][0]
        name = vname({**{k.replace("_", " "): v for k, v in base.items()}, prop: value})
        d.set(new, name=name)
        info["variants"][name] = new
        added.append(name)
        return new

    base = dict(Leading="Icon", Lines="Two", Trailing="None", Status_line="False")
    if vname({"Leading": "Icon", "Lines": "Two", "Trailing": "None", "Status line": "Plain"}) not in info["variants"]:
        plain = add({**base, "Status_line": "True"}, "Status line", "Plain")
        box = d.child(plain, "Status box")
        d.bind(box, "paddingTop", None)
        d.set(box, padding=[0, 0, 0, 0])
        d.set(d.child(box, "Status"), fill=V("Color/on-surface-variant"), characters=status(S["smart"]))
        d.set(d.child(plain, "Title"), characters=S["transitions"])
        d.set(d.child(plain, "Summary"), characters=S["smart"])
        pv = d.instance(plain, preview)
        d.cmd("instance.setProperties", ids=[pv], properties={
            P["Title"]: S["transitions"], P["Summary"]: S["smart"], P["Status text"]: status(S["smart"]),
            P["Icon"]: d.comp["Icon/transition_fade"]})
    if vname({"Leading": "Icon", "Lines": "Two", "Trailing": "Icon", "Status line": "False"}) not in info["variants"]:
        icon = add(base, "Trailing", "Icon")
        box = frame(d, icon, "Trailing", layout="v", pad=[10, 0, 0, 0], sizing=("hug", "hug"))
        d.icon(box, "open_in_new", size=20, color="Color/on-surface-variant", name="Trailing icon")
        bind_dim(d, box, "paddingTop", "space/10")
        d.set(d.child(icon, "Title"), characters=S["equaliser"])
        d.set(d.child(icon, "Summary"), characters=S["equaliser_body"])
        pv = d.instance(icon, preview)
        d.cmd("instance.setProperties", ids=[pv], properties={P["Title"]: S["equaliser"], P["Summary"]: S["equaliser_body"],
                                                              P["Icon"]: d.comp["Icon/equalizer"]})
    if added:
        _described(d, "List row/Setting", "Components / Lists and cards", ctx, SETTING_NOTE)
    ctx["transitions_setting_variants"] = added


def update_seek_bar(d: Doc, ctx: dict):
    """Seek bar gains the next song's mark, hidden unless Show next song mark is on."""
    C = ctx["components"]
    d.page("Components / Playback")
    info = C["Seek bar"]
    marks = []
    for vid in info["variants"].values():
        m = d.ellipse(vid, name="Next song mark", w=4, h=4, fill="Color/on-surface-variant")
        d.set(m, absolute=True, x=MARK_X - 2, y=20)
        d.set(m, visible=False)
        marks.append(m)
    prop = d.prop(info["id"], "Show next song mark", "boolean", layer=marks[0], default=False)
    for m in marks[1:]:
        d.bind_prop(m, prop, "visible")
    info["props"]["Show next song mark"] = prop
    card = card_of(d, "Components / Playback", info["id"])
    preview = top_named(d, card, "Dark preview")
    pv = inst(d, "Seek bar", preview, name="Seek bar, next song marked", Playing="True")
    d.cmd("instance.setProperties", ids=[pv], properties={prop: True})
    _described(d, "Seek bar", "Components / Playback", ctx, SEEK_NOTE)
    for n in (info["id"], card):
        d.annotate(n, "Next song mark and Show next song mark are a proposal for smart transitions "
                      f"({DOC}, section 4). The seek bar in the app has no mark yet.", PROPOSED)


# ───────────────────────────────────────────────────────────────── screens
SRC_SETTINGS = ("ui/expressive/ExpressiveSettings.kt · ExpressiveSettings (CrossfadeRow, which becomes a Transitions "
                "row and a Crossfade length row)")
SRC_NP = "ui/expressive/ExpressiveNowPlaying.kt · ExpressiveNowPlaying (SeekSection; the bar is PlayControls.kt · SeekBar)"

KEYS = ["transitions_settings_off", "transitions_settings_crossfade", "transitions_settings_smart",
        "transitions_picker", "transitions_length_picker", "transitions_now_playing"]
# key -> the file name of its PNG after "<style>-<theme>-"
EXPORT_NAMES = {"transitions_settings_off": "settings-off", "transitions_settings_crossfade": "settings-crossfade",
                "transitions_settings_smart": "settings-smart", "transitions_picker": "picker-transitions",
                "transitions_length_picker": "picker-length", "transitions_now_playing": "now-playing-mark"}

ROWS_NOTE = ("Proposed, not in the code yet. Today this group has a Crossfade row whose menu is Off or 2 to 12 seconds. "
             "It becomes Transitions, with Off, Crossfade and Smart in the same menu, the choice as the row's second "
             "line and what it does as the third.")
NOTES = {
    "transitions_settings_off": [
        ROWS_NOTE + " Off is the default, as now, and hides the length row. Equaliser is the row the code has "
        "today, shown when the phone has an equaliser."],
    "transitions_settings_crossfade": [
        ROWS_NOTE + " Crossfade is today's fixed fade. Crossfade length takes over today's menu without Off: "
        "2 to 12 seconds, 6 to start with. The row is there only while the choice is Crossfade or Smart."],
    "transitions_settings_smart": [
        ROWS_NOTE + " With Smart the length row stays, with a third line saying what it is for: the songs Smart has "
        "no analysis for yet play a crossfade of this length."],
    "transitions_picker": [
        "Proposed, not in the code yet. ExpressiveMenu anchored to the Transitions row, as today's Crossfade menu is: "
        "under the row, its start edge on the row's. ExpressiveMenuItem ticks the current choice and keeps the "
        "glyph's space on the others."],
    "transitions_length_picker": [
        "Proposed, not in the code yet. ExpressiveMenu anchored to the Crossfade length row: today's lengths without "
        "Off, the current one ticked."],
    "transitions_now_playing": [
        "Proposed, not in the code yet: a 4dp dot in on-surface-variant on the rest of the track where the next song "
        "starts coming in, here at 3:41 of 3:52. It is drawn only while Smart has a plan for this song and the handle "
        "hasn't reached it, and nothing else on the screen changes.",
        "The bar hands over to the next song at the dot, before its own end, because the overlap belongs to the next "
        "song's card. The dot is what says so. For most songs it sits in the last few seconds of the track."],
}


class Screens:
    """Builds the Transitions section from copies of frames the file already has."""

    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.C = ctx["components"]
        self.area = d.area(SECTION, note=(
            "A proposal for smart transitions, drawn before the code. Settings > Playback's Crossfade row becomes "
            "Transitions: Off, Crossfade (today's fixed fade) or Smart, which starts the next song where this one "
            "winds down. Crossfade length sits under it while songs overlap. On Now Playing a dot on the seek bar "
            f"marks where the next song comes in while Smart has a plan. The feature is described in {DOC}."))
        self.t: dict = {}

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
                          "note": None, "status": "proposed", "h": h}
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

    # ---- Settings ------------------------------------------------------------------------------------------------
    def cell(self, parent, position: str, row_variant: int, name: str) -> tuple[int, int]:
        """A List group/Cell whose Row is swapped to `row_variant`. Returns (cell, row)."""
        d = self.d
        c = inst(d, "List group/Cell", parent, name=f"{name} cell", Position=position, Surface="Default")
        row = d.find(c, name="Row", limit=1)[0]
        d.cmd("instance.swap", ids=[row], component=row_variant)
        # The swap keeps the song row's 64dp minimum height; take the new row's own.
        d.set(row, name=name, sizingH="fill", minHeight=d.get(row_variant).get("minHeight") or 0)
        d.set(c, sizingH="fill")
        return c, row

    def row(self, parent, position: str, name: str, *, title: str, icon: str, summary: str,
            status: str | None = None, trailing: str = "None") -> tuple[int, int]:
        """A List row/Setting in a group cell, every property set (a swapped row keeps values it never had)."""
        d, ctx = self.d, self.ctx
        P = self.C["List row/Setting"]["props"]
        v = variant(ctx, "List row/Setting", Leading="Icon", Lines="Two", Trailing=trailing,
                    Status_line="Plain" if status else "False")
        c, row = self.cell(parent, position, v, name)
        values = {P["Title"]: title, P["Icon"]: d.comp[f"Icon/{icon}"], P["Summary"]: summary}
        if status:
            values[P["Status text"]] = status
        d.cmd("instance.setProperties", ids=[row], properties=values)
        return c, row

    def settings(self, key: str, name: str, mode: str) -> dict:
        """Settings scrolled until Playback is under the bar, the group in one of the three transition modes."""
        d = self.d
        f = self.copy(key, name, SRC_SETTINGS, screen_frame(d, "Settings / Top"))
        lst = top_named(d, f, "Settings list")
        names = [k["name"] for k in d.get(lst, depth=1)["children"]]
        at = names.index("Section Lyrics")
        sec = d.instance("Settings section", lst, name="Section Playback")
        d.cmd("instance.setProperties", ids=[sec], properties={self.C["Settings section"]["props"]["Label"]: S["playback"]})
        d.cmd("node.reorder", id=sec, index=at)
        g = frame(d, lst, "Playback group", layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        d.cmd("node.reorder", id=g, index=at + 1)
        out = {"frame": f}
        self.row(g, "Top", "Volume levelling", title=S["volume"], icon="volume_up", summary=S["volume_body"],
                 trailing="Switch on")
        out["transitions"], _ = self.row(g, "Middle", "Transitions", title=S["transitions"], icon="transition_fade",
                                         summary=mode, status=status(mode))
        if mode != S["off"]:
            out["length"], _ = self.row(g, "Middle", "Crossfade length", title=S["length"], icon="schedule",
                                        summary=seconds(LENGTH),
                                        status=S["length_smart_body"] if mode == S["smart"] else None)
        self.row(g, "Bottom", "Equaliser", title=S["equaliser"], icon="equalizer", summary=S["equaliser_body"],
                 trailing="Icon")
        # Scrolled until Playback is under the bar, which has taken the title.
        d.set(lst, y=104 - d.get(sec)["y"])
        bar = top_named(d, f, "App bar")
        P = self.C["Top app bar/Detail"]["props"]
        d.cmd("instance.setProperties", ids=[bar], properties={"Surface": "Scrolled", P["Title"]: "Settings",
                                                               P["Show action"]: False})
        d.set(d.child(bar, "Title box"), visible=True)
        return out

    def menu(self, f: int, anchor: int, name: str, labels: list[str], current: str) -> dict:
        """ExpressiveMenu under `anchor` (a cell on screen `f`), its start edge on the anchor's, one item per label
        with the current one ticked."""
        d = self.d
        P = self.C["Menu/Item"]["props"]
        fb, ab = d.get(f)["absoluteBounds"], d.get(anchor)["absoluteBounds"]
        m = d.instance("Menu", f, name=name, x=round(ab["x"] - fb["x"], 2), y=round(ab["y"] + ab["height"] - fb["y"], 2))
        # Above the list and the bar, under the shell's mini player? No: a menu is a window over everything but the
        # system bars, so it goes on top of the shell and under the status and gesture bars.
        kids = [k["name"] for k in d.get(f, depth=1)["children"]]
        d.cmd("node.reorder", id=m, index=kids.index("Status bar"))
        items = {}
        for i, label in enumerate(labels):
            on = label == current
            v = variant(self.ctx, "Menu/Item", Danger="False", Leading="Icon" if on else "Space")
            r = d.cmd("instance.slotInsert", id=m, slot=self.C["Menu"]["props"]["Items"], component=v, replace=i == 0)
            iid = r["id"] if isinstance(r, dict) and "id" in r else r["ids"][0]
            d.set(iid, name=f"Item {label}", sizingH="fill")
            values = {P["Label"]: label}
            if on:
                values[P["Icon"]] = d.comp["Icon/check"]
            d.cmd("instance.setProperties", ids=[iid], properties=values)
            if on:
                icon = d.find(iid, name="Icon", limit=1)[0]
                d.set(d.find(icon, name="Glyph", limit=1)[0], fill=V("Color/on-surface-variant"))
            items[label] = iid
        return {"menu": m, "items": items}

    def now_playing(self):
        d = self.d
        f = self.copy("transitions_now_playing", "Transitions / Now Playing, next song marked", SRC_NP,
                      screen_frame(d, "Now Playing / Playing"))
        bar = d.child(f, "Seek bar")
        d.cmd("instance.setProperties", ids=[bar], properties={self.C["Seek bar"]["props"]["Show next song mark"]: True})
        self.t["np_mark"] = d.child(bar, "Next song mark")

    def build(self):
        d, t = self.d, self.t
        t["off"] = self.settings("transitions_settings_off", "Transitions / Settings, off", S["off"])
        t["crossfade"] = self.settings("transitions_settings_crossfade", "Transitions / Settings, Crossfade", S["crossfade"])
        t["smart"] = self.settings("transitions_settings_smart", "Transitions / Settings, Smart", S["smart"])
        p = self.settings("transitions_picker", "Transitions / Transitions menu", S["off"])
        p.update(self.menu(p["frame"], p["transitions"], "Transitions menu", MODES, S["off"]))
        t["picker"] = p
        p = self.settings("transitions_length_picker", "Transitions / Crossfade length menu", S["smart"])
        p.update(self.menu(p["frame"], p["length"], "Crossfade length menu", [seconds(n) for n in LENGTHS], seconds(LENGTH)))
        t["length_picker"] = p
        self.now_playing()
        self.area.close()
        d.ann_category(PROPOSED, "#7C3AED")
        for key in self.area.keys:
            s = d.screens[key]
            for fid in (s["light"], s["dark"]):
                d.annotate(fid, f"Compose: {s['source']}", "Source")
                for n in NOTES[key]:
                    d.annotate(fid, n, PROPOSED)
        self.ctx["transitions_screens"] = list(self.area.keys)
        return t


# ───────────────────────────────────────────────────────────────── prototype
FLOW = ("transitions_settings_off", "12 Transitions",
        "Settings > Playback with transitions off. The Transitions row opens its menu; Smart shows Crossfade "
        "length, whose row opens the lengths. A proposal: none of this is in the app yet.")


def wire(d: Doc, ctx: dict, t: dict):
    fade = dict(transition="dissolve", duration=200, easing="easeOut")
    links = [
        ("transitions_settings_off", t["off"]["transitions"], "transitions_picker"),
        ("transitions_picker", t["picker"]["items"][S["off"]], "transitions_settings_off"),
        ("transitions_picker", t["picker"]["items"][S["crossfade"]], "transitions_settings_crossfade"),
        ("transitions_picker", t["picker"]["items"][S["smart"]], "transitions_settings_smart"),
        ("transitions_settings_crossfade", t["crossfade"]["transitions"], "transitions_picker"),
        ("transitions_settings_smart", t["smart"]["transitions"], "transitions_picker"),
        ("transitions_settings_smart", t["smart"]["length"], "transitions_length_picker"),
        ("transitions_length_picker", t["length_picker"]["items"][seconds(LENGTH)], "transitions_settings_smart"),
    ]
    for src, layer, dst in links:
        d.link(src, layer, dst, **fade)
    start = d.screens[FLOW[0]]["light"]
    d.cmd("proto.flow.add", start=start, name=FLOW[1])
    d.cmd("proto.flow.rename", start=start, name=FLOW[1], description=FLOW[2])
    ctx["transitions_links"] = links


# ───────────────────────────────────────────────────────────────── cover and changelog
def counts(d: Doc) -> dict:
    """What the Cover's Read me counts, read from the file."""
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
        new = re.sub(r"^\d+ screen states in Light and Dark, wired as a prototype\.$",
                     f"{c['screens']} screen states in Light and Dark, wired as a prototype. The Transitions section "
                     f"is a proposal that isn't in the app yet.", new)
        new = re.sub(r"^\d+ screen states", f"{c['screens']} screen states", new)
        if new != text:
            d.set(t, characters=new)
    ctx["transitions_counts"] = c


CHANGES = (
    "A proposal for smart transitions, drawn before the code (docs/features/smart-transitions.md). Nothing in it "
    "is in the app yet: its frames carry a Proposed annotation and aren't marked ready for development.\n"
    "• Screens: a new Transitions section of 6 states in Light and Dark. Settings > Playback with transitions off, "
    "on Crossfade and on Smart; the Transitions menu; the Crossfade length menu; Now Playing with the next song "
    "marked on the seek bar.\n"
    "• Settings > Playback, which 1.0.0 didn't draw: the Crossfade row becomes Transitions (Off, Crossfade, Smart). "
    "Crossfade length (2 to 12 seconds, 6 to start with) is a row under it while the choice is Crossfade or Smart.\n"
    "• Changed components: Seek bar gains Show next song mark, a 4dp dot on the rest of the track where the next "
    "song starts coming in. List row/Setting gains Status line=Plain and Trailing=Icon, which the code's Playback "
    "rows already use.\n"
    "• Icons: transition_fade and equalizer, which Symbols.kt has.\n"
    "• Prototype: flow 12 Transitions, from Settings > Playback.")


def update_changelog(d: Doc, ctx: dict):
    page = d.page("Changelog")
    entries = top_named(d, page, "Entries")
    before = d.get(entries)["height"]
    P = ctx["components"]["Changelog entry"]["props"]
    e = d.instance("Changelog entry", entries, name=f"Entry · {VERSION}")
    d.cmd("instance.setProperties", ids=[e], properties={P["Version"]: VERSION, P["Date"]: UPDATED, P["Author"]: AUTHOR,
                                                         P["Changes"]: CHANGES})
    d.set(e, sizingH="fixed", width=1400)
    d.cmd("node.reorder", id=e, index=0)
    grew = d.get(entries)["height"] - before
    how = top_named(d, page, "Card · Adding a version")
    d.set(how, y=d.get(how)["y"] + grew)


# ───────────────────────────────────────────────────────────────── build
def build(d: Doc, ctx: dict):
    if not ctx.get("attached"):
        print("    transitions: skipped in a build from scratch. Run `python3 -m expressive.transitions "
              "OUT.figcraft OUT-1.1.0.figcraft` on the saved file to add it.", flush=True)
        return
    try:
        screen_frame(d, "Transitions / Settings, off")
    except KeyError:
        pass
    else:
        raise SystemExit("transitions: this file already has the Transitions section")
    build_icons(d, ctx)
    update_setting_row(d, ctx)
    update_seek_bar(d, ctx)
    for page in ("Components / Lists and cards", "Components / Playback"):
        restack(d, page)
    t = Screens(d, ctx).build()
    wire(d, ctx, t)
    update_cover(d, ctx)
    update_changelog(d, ctx)
    d.page("Cover")


def export(d: Doc, ctx: dict, out: Path, style: str = "expressive"):
    """Every new frame at the renders' density, named <style>-<light|dark>-<frame>.png, and the changed spec cards."""
    for key in ctx["transitions_screens"]:
        for theme in ("light", "dark"):
            d.export_png(d.screens[key][theme], out / f"{style}-{theme}-{EXPORT_NAMES[key]}.png", scale=DENSITY)
    C = ctx["components"]
    for page, set_name, name in (("Components / Playback", "Seek bar", "seek-bar"),
                                 ("Components / Lists and cards", "List row/Setting", "setting-row")):
        d.page(page)
        d.export_png(card_of(d, page, C[set_name]["id"]), out / f"{style}-component-{name}.png", scale=1)


def run(src: Path, out: Path, export_dir: Path | None = None, log=None) -> dict:
    """Open `src`, add the proposal, save as `out` (and export the new frames)."""
    d = Doc("Attic Expressive", read_roots=[src.parent], write_roots=[out.parent], log=log)
    ctx: dict = {"style": "expressive"}
    try:
        d.tool("open_file", path=str(src.resolve()))
        attach(d, ctx)
        build(d, ctx)
        out.parent.mkdir(parents=True, exist_ok=True)
        d.save(out)
        if export_dir:
            export(d, ctx, export_dir)
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
    print(f"saved {a.out} ({a.out.stat().st_size // 1024} KB): {len(ctx['transitions_screens'])} screens, "
          f"{len(ctx['transitions_links'])} links, icons {ctx['transitions_icons']}, "
          f"setting row variants {ctx['transitions_setting_variants']}, counts {ctx['transitions_counts']}")


if __name__ == "__main__":
    sys.exit(main())
