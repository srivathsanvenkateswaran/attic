"""Smart transitions in Attic Comfort: a proposal, drawn before the code (version 1.1.0). Pages: Components /
Playback, Screens, Cover, Changelog.

Unlike the modules that built 1.0.0 this one works on a file that already exists: `attach()` reads the components,
styles, variables and screens out of the opened document, and `build()` adds to them. Run it on a copy of the file,
look at the result, then copy it back:

    cd design/build
    python3 -m comfort.transitions "../Attic Comfort.figcraft" /tmp/attic-design/comfort-transitions.figcraft \
        [--export /tmp/attic-dev/smart-transitions-design]

It refuses a file that already has the Transitions section. It touches nothing the equaliser's module on
feature/per-song-equaliser adds, so it runs on a file with or without the equaliser. In a build from scratch
(`figbuild.run comfort`) it does nothing but say so: build 1.0.0 first, then run this module on the saved file.

The feature is docs/features/smart-transitions.md; section 4 is the brief. Nothing here is in the app yet, so every
new frame carries a `Proposed` annotation and none is marked ready for development.

COMPONENTS / PLAYBACK
  Seek bar    gains the layer "Next song mark" (hidden) and the property Show next song mark: a notch rising 7dp
              from the track's lower edge, under the place the fill's tick has when the next song starts coming in. NowPlayingParts.kt SeekBar

SCREENS (section "Transitions", Light above Dark, keys in KEYS)
  Settings, off; Settings, Crossfade; Settings, Smart; Transitions pop-over; Crossfade length pop-over; Now Playing,
  next song marked. The settings frames are Settings scrolled to Playback, which the 1.0.0 file doesn't draw: Volume
  levelling, Transitions, Crossfade length and Equaliser, built from Settings row (Control=Switch on, Value, Chevron)
  and the Menu, Menu caption and Menu row the file has.

PROTOTYPE
  The flow "Transitions" starts at Settings, off: the Transitions row opens its pop-over, Smart shows the length
  row, which opens its own.

HANDOFF
  A Source and a Proposed annotation on every new frame; Cover 1.1.0; the Changelog entry.
"""
from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path

from figbuild.kit import Area, Doc, DENSITY

from .components_core import K
from .components_states import block, group, row, setp
from .foundations import MODE_COLLECTIONS

VERSION = "1.1.0"
UPDATED = "2026-11-25"
SECTION = "Transitions"
DOC = "docs/features/smart-transitions.md"
PROPOSED = "Proposed"
SETTINGS_SRC = ("ui/comfort/ComfortSettings.kt · ComfortSettings (CrossfadeRow, which becomes a Transitions row and a "
                "Crossfade length row)")
NP_SRC = "ui/comfort/ComfortNowPlaying.kt · ComfortNowPlaying (Seek; the bar is NowPlayingParts.kt · SeekBar)"
W, H = 411, 914

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


def summary(mode: str) -> str:
    """The Transitions row's summary: what the mode does, then (while songs overlap) what it leaves alone."""
    if mode == S["off"]:
        return S["off_body"]
    return (S["crossfade_body"] if mode == S["crossfade"] else S["smart_body"]) + " " + S["albums"]


# The Now Playing mark. The demo song is 3:52 at 1:18; the planned overlap starts at 3:41.
SONG_MS, MARK_MS = 232_000, 221_000
SEEK_W, THUMB, TICK_IN = 351.0, 64.0, 12.0
MARK_W, MARK_RISE = 3.0, 7.0            # the tick's width, and how far it rises from the track's lower edge


def fill_width(fraction: float, width: float = SEEK_W) -> float:
    """SeekBar's fill at a fraction of the song (NowPlayingParts.kt: thumb + (width - thumb) * shown)."""
    return THUMB + (width - THUMB) * fraction


# The notch sits under the fill's tick as it will be when the next song comes in: 12dp inside the fill's end.
MARK_X = round(fill_width(MARK_MS / SONG_MS) - TICK_IN, 2)


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


_PROBE = Path("/tmp/attic-design/.comfort-transitions-probe.figcraft")


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


# ============================================================================== the seek bar's mark
SEEK_NOTE = ("Proposed, not in the code yet: Show next song mark draws a notch 3dp wide that rises 7dp from the "
             "track's lower edge (top corners 1.5), under the place the fill's tick has when the next song starts "
             "coming in: its left edge at 64 + (351 - 64) x fraction - 12. On the track it is on-3; once the fill "
             "covers it, the tick's own on-accent at 40 %. It shows only while Smart has a plan for the song. Shown "
             "at 3:41 of 3:52.")


def _seek_bar(d: Doc, ctx: dict):
    """Seek bar gains the next song's notch, hidden unless Show next song mark is on."""
    d.page("Components / Playback")
    sid = d.comp["Seek bar"]
    marks = []
    for v in d.get(sid, depth=1)["children"]:
        if v["type"] != "COMPONENT":
            continue
        covered = fill_width({"Progress=Start": 0.0, "Progress=Playing": 78 / 232, "Progress=End": 220 / 232}[v["name"]]) > MARK_X
        # Two dp taller than it shows: the track clips it, so its foot is the track's own edge.
        m = d.rect(v["id"], name="Next song mark", w=MARK_W, h=MARK_RISE + 2, radius=1.5,
                   fill="Color/derived/seek-tick" if covered else "Color/on-3")
        d.set(m, absolute=True, x=MARK_X, y=48 - MARK_RISE)
        d.set(m, visible=False)
        marks.append(m)
    prop = d.prop(sid, "Show next song mark", "boolean", layer=marks[0], default=False)
    for m in marks[1:]:
        d.bind_prop(m, prop, "visible")
    ctx["seek_mark_prop"] = prop
    # The spec card's notes, the set's description and a Dark preview with the mark on.
    spec = next(c["id"] for c in d.get(d.pages["Components / Playback"], depth=1)["children"] if c["name"] == "Spec: Seek bar")
    notes = d.child(spec, "Notes")
    d.set(notes, characters=d.get(notes)["characters"] + " " + SEEK_NOTE)
    cur = ctx["descriptions"].get(sid, "")
    if cur:
        d.set(sid, description=f"{cur} {SEEK_NOTE}")
    preview = next(c["id"] for c in d.get(d.pages["Components / Playback"], depth=1)["children"] if c["name"] == "Dark preview")
    kids = d.get(preview, depth=1)["children"]
    last = max(i for i, k in enumerate(kids) if k["name"] == "Preview: Seek bar")
    for n, variant in enumerate(("Playing", "End")):
        pv = d.instance("Seek bar", preview, name="Preview: Seek bar")
        setp(d, pv, Progress=variant, Elapsed={"Playing": "1:18", "End": "3:40"}[variant], Total="3:52",
             **{"Show next song mark": True})
        d.cmd("node.reorder", id=pv, index=last + 1 + n)
    d.ann_category(PROPOSED, "#7C3AED")
    for n in (sid, spec):
        d.annotate(n, "Next song mark and Show next song mark are a proposal for smart transitions "
                      f"({DOC}, section 4). The seek bar in the app has no mark yet.", PROPOSED)
    # A longer note must not run into the row of cards under it.
    sb = d.get(spec)
    for c in d.get(d.pages["Components / Playback"], depth=1)["children"]:
        if c["name"].startswith("Spec: ") and c["id"] != spec and c["y"] > sb["y"] and c["y"] < sb["y"] + sb["height"] + 40 \
                and c["x"] < sb["x"] + sb["width"]:
            raise RuntimeError(f"Spec: Seek bar now overlaps {c['name']} on Components / Playback")


# ============================================================================== screens
KEYS = ["transitions_settings_off", "transitions_settings_crossfade", "transitions_settings_smart",
        "transitions_picker", "transitions_length_picker", "transitions_now_playing"]
# key -> the file name of its PNG after "<style>-<theme>-"
EXPORT_NAMES = {"transitions_settings_off": "settings-off", "transitions_settings_crossfade": "settings-crossfade",
                "transitions_settings_smart": "settings-smart", "transitions_picker": "picker-transitions",
                "transitions_length_picker": "picker-length", "transitions_now_playing": "now-playing-mark"}

ROWS_NOTE = ("Proposed, not in the code yet. Today this card has a Crossfade row whose pop-over is Off or 2 to 12 "
             "seconds. It becomes Transitions, with Off, Crossfade and Smart in the same pop-over, the choice as the "
             "row's value and what it does as its summary.")
NOTES = {
    "transitions_settings_off": [
        ROWS_NOTE + " Off is the default, as now, and hides the length row. Equaliser is the row the code has "
        "today, shown when the phone has an equaliser."],
    "transitions_settings_crossfade": [
        ROWS_NOTE + " Crossfade is today's fixed fade. Crossfade length takes over today's pop-over without Off: "
        "2 to 12 seconds, 6 to start with. The row is there only while the choice is Crossfade or Smart."],
    "transitions_settings_smart": [
        ROWS_NOTE + " With Smart the length row stays, with a summary saying what it is for: the songs Smart has no "
        "analysis for yet play a crossfade of this length."],
    "transitions_picker": [
        "Proposed, not in the code yet. ComfortMenu over the Transitions row toward its value, as today's Crossfade "
        "pop-over opens (offset: the card's width - 214, up by the row's height): MenuCaption, then a MenuRow per "
        "choice with the current one ticked."],
    "transitions_length_picker": [
        "Proposed, not in the code yet. ComfortMenu over the Crossfade length row: today's lengths without Off, the "
        "current one ticked."],
    "transitions_now_playing": [
        "Proposed, not in the code yet: a notch in on-3, 3dp wide, rising 7dp from the seek bar's lower edge under "
        "the place the fill's tick has when the next song starts coming in, here at 3:41 of 3:52. It is drawn only while Smart has "
        "a plan for this song, and nothing else on the screen changes.",
        "The bar hands over to the next song when its tick is over the notch, before the fill reaches the end, "
        "because the overlap belongs to the next song's card. For most songs that is in the last few seconds, so the "
        "notch sits under the total time, which is why it is on the lower edge and not at the height of the tick."],
}


def _copy(d: Doc, a: Area, ctx: dict, key: str, name: str, source: str, of: str) -> int:
    """A copy of the screen frame named `of` as the next Light screen of the section, without its handoff marks and
    prototype links."""
    fid = d.cmd("edit.duplicate", ids=[ctx["frames"][f"{of} · Light"]], dx=0, dy=0)["ids"][0]
    d.cmd("node.reparent", ids=[fid], parent=d.pages["Screens"])
    d.set(fid, name=f"{name} · Light", x=a.cursor, y=a.y0 + a.PAD + a.TITLE, devStatus=None)
    if d.cmd("annotation.list", ids=[fid]).get("annotations"):
        d.cmd("annotation.remove", id=fid)
    tree = d.get(fid, depth=60, fields=["interactions"])
    ids, stack = [], [tree]
    while stack:
        n = stack.pop()
        if n.get("interactions"):
            ids.append(n["id"])
        stack.extend(n.get("children") or [])
    if ids:
        d.cmd("proto.interaction.clear", ids=ids)
    d.screens[key] = {"light": fid, "dark": None, "map": {}, "name": name, "source": source, "page": "Screens",
                      "note": None, "status": "proposed", "h": H}
    a.cursor += W + a.GAP_X
    a.keys.append(key)
    a.row_h = max(a.row_h, H)
    return fid


def _settings(d: Doc, ss, a: Area, key: str, name: str, mode: str) -> dict:
    """Settings scrolled to Playback (ComfortSettings.kt): Volume levelling, the proposed rows and the Equaliser row,
    then Lyrics and Song radio as the Settings frames have them."""
    f, col = ss.phone(a, key, name, SETTINGS_SRC)
    d.screens[key]["status"] = "proposed"
    out = {"frame": f}
    ss.label(col, S["playback"])
    g, _ = group(d, col, "Playback")
    row(d, g, "Settings row", "Volume levelling", Control="Switch on", Title=S["volume"], Summary=S["volume_body"],
        **{"Show summary": True})
    out["transitions"] = row(d, g, "Settings row", "Transitions", Control="Value", Title=S["transitions"], Value=mode,
                             Summary=summary(mode), **{"Show summary": True, "Show hairline": True})
    if mode != S["off"]:
        smart = mode == S["smart"]
        out["length"] = row(d, g, "Settings row", "Crossfade length", Control="Value", Title=S["length"],
                            Value=seconds(LENGTH), **{"Show summary": smart, "Show hairline": True},
                            **({"Summary": S["length_smart_body"]} if smart else {}))
    eq = row(d, g, "Settings row", "Equaliser", Control="Chevron", Title=S["equaliser"], Summary=S["equaliser_body"],
             **{"Show summary": True, "Show hairline": True})
    # SetRow's trailing slot: the code puts open_in_new there for this row, in the chevron's on-3.
    chev = d.child(eq, "Chevron")
    d.cmd("instance.swap", ids=[chev], component=d.comp["Icon/open_in_new"])
    d.set(chev, name="Opens outside Attic")
    d.set(d.child(chev, "Glyph"), fill={"$var": "Color/on-3"})
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
    ss.collapsed_bar(f, "Settings")
    ss.chrome(f)
    return out


def _popover(d: Doc, f: int, anchor: int, name: str, caption: str, options: list[str], selected: str) -> dict:
    """ComfortMenu over `anchor` (a Settings row on screen `f`) as CrossfadeRow places it: DpOffset(the row's width -
    214, minus the row's height), so its top is the row's top and its end 14dp inside the card's."""
    fb, ab = d.get(f)["absoluteBounds"], d.get(anchor)["absoluteBounds"]
    m = d.instance("Menu", f, name=name, x=round(ab["x"] - fb["x"] + ab["width"] - 214, 2), y=round(ab["y"] - fb["y"], 2))
    setp(d, m, **{"On sheet": "False"})
    slot = next(p["name"] for p in d.cmd("component.properties", id=m)["properties"] if p["type"] == "slot")

    def insert(comp, first=False, **values):
        r = d.cmd("instance.slotInsert", id=m, slot=slot, component=d.comp[comp], replace=first)
        iid = r["id"] if isinstance(r, dict) and "id" in r else r["ids"][0]
        d.set(iid, sizingH="fill")
        setp(d, iid, **values)
        return iid

    d.set(insert("Menu caption", first=True, Text=caption), name="Caption")
    rows = {}
    for o in options:
        r = insert("Menu row", Kind="Selected" if o == selected else "Default", Label=o,
                   **{"Show icon": False, "Show check": o == selected})
        d.set(r, name=f"Row: {o}")
        rows[o] = r
    # A menu is a window over the app: above the dock, under the system bars.
    kids = [k["name"] for k in d.get(f, depth=1)["children"]]
    d.cmd("node.reorder", id=m, index=kids.index("Status bar"))
    return {"menu": m, "rows": rows}


def _screens(d: Doc, ctx: dict) -> dict:
    from .screens_settings import S as SS
    ss = SS(d, ctx)
    a = d.area(SECTION, note=(
        "A proposal for smart transitions, drawn before the code. Settings > Playback's Crossfade row becomes "
        "Transitions: Off, Crossfade (today's fixed fade) or Smart, which starts the next song where this one winds "
        "down. Crossfade length sits under it while songs overlap. On Now Playing a notch on the seek bar marks where "
        f"the next song comes in while Smart has a plan. The feature is described in {DOC}."))
    t = {}
    t["off"] = _settings(d, ss, a, "transitions_settings_off", "Transitions / Settings, off", S["off"])
    t["crossfade"] = _settings(d, ss, a, "transitions_settings_crossfade", "Transitions / Settings, Crossfade", S["crossfade"])
    t["smart"] = _settings(d, ss, a, "transitions_settings_smart", "Transitions / Settings, Smart", S["smart"])
    p = _settings(d, ss, a, "transitions_picker", "Transitions / Transitions pop-over", S["off"])
    p.update(_popover(d, p["frame"], p["transitions"], "Transitions pop-over", S["transitions"], MODES, S["off"]))
    t["picker"] = p
    p = _settings(d, ss, a, "transitions_length_picker", "Transitions / Crossfade length pop-over", S["smart"])
    p.update(_popover(d, p["frame"], p["length"], "Crossfade length pop-over", S["length"],
                      [seconds(n) for n in LENGTHS], seconds(LENGTH)))
    t["length_picker"] = p
    f = _copy(d, a, ctx, "transitions_now_playing", "Transitions / Now Playing, next song marked", NP_SRC,
              "Now Playing / Playing")
    setp(d, d.child(f, "Seek bar"), **{"Show next song mark": True})
    a.close()
    d.ann_category(PROPOSED, "#7C3AED")
    for key in a.keys:
        s = d.screens[key]
        for fid in (s["light"], s["dark"]):
            d.annotate(fid, f"Compose: {s['source']}", "Source")
            for n in NOTES[key]:
                d.annotate(fid, n, PROPOSED)
    ctx["transitions_screens"] = list(a.keys)
    return t


# ============================================================================== prototype
FLOW = ("transitions_settings_off", "Transitions",
        "Settings > Playback with transitions off. The Transitions row opens its pop-over; Smart shows Crossfade "
        "length, whose row opens the lengths. A proposal: none of this is in the app yet.")


def _prototype(d: Doc, t: dict) -> list:
    fade = dict(transition="dissolve", duration=200, easing="easeOut")
    links = [
        ("transitions_settings_off", t["off"]["transitions"], "transitions_picker"),
        ("transitions_picker", t["picker"]["rows"][S["off"]], "transitions_settings_off"),
        ("transitions_picker", t["picker"]["rows"][S["crossfade"]], "transitions_settings_crossfade"),
        ("transitions_picker", t["picker"]["rows"][S["smart"]], "transitions_settings_smart"),
        ("transitions_settings_crossfade", t["crossfade"]["transitions"], "transitions_picker"),
        ("transitions_settings_smart", t["smart"]["transitions"], "transitions_picker"),
        ("transitions_settings_smart", t["smart"]["length"], "transitions_length_picker"),
        ("transitions_length_picker", t["length_picker"]["rows"][seconds(LENGTH)], "transitions_settings_smart"),
    ]
    for src, layer, dst in links:
        d.link(src, layer, dst, **fade)
    start = d.screens[FLOW[0]]["light"]
    d.cmd("proto.flow.add", start=start, name=FLOW[1])
    d.cmd("proto.flow.rename", start=start, name=FLOW[1], description=FLOW[2])
    return links


# ============================================================================== cover and changelog
CHANGES = [
    "A proposal for smart transitions, drawn before the code (docs/features/smart-transitions.md). Nothing in it is "
    "in the app yet: its frames carry a Proposed annotation and aren't marked ready for development.",
    "Screens: the section Transitions, 6 screens in Light and Dark: Settings at Playback with transitions off, on "
    "Crossfade and on Smart, the Transitions pop-over, the Crossfade length pop-over, and Now Playing with the next "
    "song marked on the seek bar.",
    "Settings > Playback, which 1.0.0 didn't draw: the Crossfade row becomes Transitions (Off, Crossfade, Smart). "
    "Crossfade length (2 to 12 seconds, 6 to start with) is a row under it while the choice is Crossfade or Smart.",
    "Changed component: Seek bar gains Show next song mark, a notch on the track's lower edge under the place the "
    "fill's tick has when the next song starts coming in.",
    "Prototype: the flow Transitions, from Settings at Playback.",
]


def _cover(d: Doc):
    """Version and date on the Cover, and the entry on top of the Changelog."""
    cover = d.child(d.page("Cover"), "Cover")
    for meta, value in (("Meta: Version", VERSION), ("Meta: Last updated", UPDATED)):
        d.set(d.child(d.child(cover, meta), "Value"), characters=value)
    pid = d.page("Changelog")
    tops = {c["name"]: c for c in d.get(pid, depth=1)["children"]}
    entries, howto = tops["Entries"], tops["How to add an entry"]
    i = d.instance("Doc/Changelog entry", entries["id"], name=f"Entry {VERSION}")
    d.cmd("node.reorder", id=i, index=0)
    d.cmd("instance.setProperties", ids=[i], properties={"Version": VERSION, "Date": UPDATED, "Author": "Attic maintainers",
                                                         "Changes": "\n".join(CHANGES)})
    d.set(i, sizingH="fill")
    d.set(d.child(i, "Changes"), listStyle="bulleted", paragraphSpacing=10)
    grown = d.get(entries["id"])["height"] - entries["height"]
    d.set(howto["id"], y=howto["y"] + grown)


# ============================================================================== build
def build(d: Doc, ctx: dict):
    if not ctx.get("opened"):
        print("    transitions: skipped in a build from scratch. Run `python3 -m comfort.transitions OUT.figcraft "
              "OUT-1.1.0.figcraft` on the saved file to add it.", flush=True)
        return
    frames = attach(d, ctx)
    if "Transitions / Settings, off · Light" in frames:
        raise SystemExit("this file already has the Transitions section; nothing to add")
    ctx["descriptions"] = _descriptions(d)
    _seek_bar(d, ctx)
    t = _screens(d, ctx)
    ctx["transitions_links"] = _prototype(d, t)
    _cover(d)
    d.page("Cover")


def export(d: Doc, ctx: dict, out: Path, style: str = "comfort"):
    """Every new frame at the renders' density, named <style>-<light|dark>-<frame>.png, and the changed spec card."""
    for key in ctx["transitions_screens"]:
        for theme in ("light", "dark"):
            d.export_png(d.screens[key][theme], out / f"{style}-{theme}-{EXPORT_NAMES[key]}.png", scale=DENSITY)
    page = d.page("Components / Playback")
    spec = next(c["id"] for c in d.get(page, depth=1)["children"] if c["name"] == "Spec: Seek bar")
    d.export_png(spec, out / f"{style}-component-seek-bar.png", scale=2)


def main(argv=None):
    ap = argparse.ArgumentParser(description="Add the smart transitions proposal to an existing Attic Comfort file.")
    ap.add_argument("src", type=Path)
    ap.add_argument("dst", type=Path)
    ap.add_argument("--export", type=Path, help="write every new frame here at 2.625x")
    a = ap.parse_args(argv)
    d = Doc("Attic Comfort", read_roots=[a.src.resolve().parent], write_roots=[a.dst.resolve().parent])
    try:
        d.tool("open_file", path=str(a.src.resolve()))
        d.cmd("text.refit")
        ctx = {"style": "comfort", "opened": True}
        build(d, ctx)
        a.dst.parent.mkdir(parents=True, exist_ok=True)
        d.save(a.dst.resolve())
        print(f"saved {a.dst} ({a.dst.stat().st_size // 1024} KB): {len(ctx['transitions_screens'])} screens, "
              f"{len(ctx['transitions_links'])} links")
        if a.export:
            export(d, ctx, a.export)
            print(f"exported {len(KEYS) * 2} frames to {a.export}")
    finally:
        d.close()


if __name__ == "__main__":
    sys.exit(main())
