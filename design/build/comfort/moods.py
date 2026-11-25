"""Mood mixes in Attic Comfort: the Moods section, its components, links and the handoff marks.

This module adds to a file that already exists. `Attic Comfort.figcraft` is the source, so nothing here rebuilds
what the file holds: `attach` reads the opened file's pages, styles and components, `remove` deletes whatever an
earlier run of this module added, and `build` draws it again. Run it on a copy, look at the exports, then copy
the result over the file:

    cd design/build
    python3 -m comfort.moods "../Attic Comfort.figcraft" /tmp/attic-design/moods/comfort.figcraft \
        [--export /tmp/attic-mood/design/comfort] [--no-tablet]

It can be run again on its own output, and on a later version of the file: the Cover's minor version goes up
once, the first time, and a second run keeps the version its Changelog entry already has.

The feature is a proposal: the code lands on feature/mood-mixes. Every frame carries a `Proposed` annotation
and is not marked ready for development. Drawn from the architecture note (mood-mixes-architecture.md, "Your
picks" and "Screens") and from the code on tier2-integration at dffdd29, which the 1.0.0 frames predate: the
taste Mixes shelf (ComfortHome.kt), Search's Genres group (ComfortSearch.kt) and the genre page
(ComfortCollections.kt · ComfortGenre), which the mood page is modelled on.

New components (page Components / Lists and cards, card "Spec: Moods")
---------------------------------------------------------------------
Mood tile             ComfortMoods.kt · MoodTile. BrowseTile's card, 200 x 133: the mosaic in the corner with the
                      song count across from it, the mood and its description at the foot. [Title, Description,
                      Count]
Mood picks card       ComfortMoods.kt · MoodPicksCard. A group card with the question, a line of body, "Choose"
                      and "Not now". [Title, Body]

The mood page uses components the file had: Collection header with the mosaic in its art, and Collection actions
with radio (sensors) where add to playlist was. The picker is chips on group cards.

Screens (section "Moods" on the Screens page, Light and Dark)
------------------------------------------------------------
moods_home, moods_home_picks, moods_page, moods_page_playing, moods_browse, moods_browse_full, moods_results,
moods_picker_empty, moods_picker_picked, moods_picker_no_genres, moods_settings, moods_settings_empty, moods_now_playing, and with
the tablet shell from feature/tablet-layouts (names end "· In progress"): moods_home_expanded,
moods_home_medium, moods_page_expanded, moods_page_medium, moods_picker_expanded, moods_picker_medium.

Prototype
---------
Flow "Mood mixes" starts at Home with the picks card. Tile -> mood page; Choose and the Settings row -> picker;
the picker's back and Save -> Home; the mood page's radio -> Now Playing; Browse's and Results' mood -> page.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from figbuild.kit import Doc, V, Area, DENSITY

SECTION = "Moods"
SCREENS = "Screens"
BRANCH = "feature/mood-mixes"
TABLET_BRANCH = "feature/tablet-layouts"
UPDATED = "2026-11-25"
AUTHOR = "Attic maintainers"
MOODS_KT = "ui/comfort/ComfortMoods.kt"
CARD = "Spec: Moods"
ENTRY_MARK = "Mood mixes, proposed"
LISTS_PAGE = "Components / Lists and cards"

COVERS = Path("/tmp/attic-dev/design-refs/covers")
COVER_FILES = ["0-paper-lanterns.jpg", "1-tea-at-2-am.jpg", "2-marea-lenta.jpg", "3-glasshouse-summer.jpg",
               "4-room-tone.jpg", "5-kolam.jpg", "6-monsoon-tapes.jpg", "7-kadalkarai-sessions.jpg",
               "8-iravu-rail.jpg", "9-chhat-pe-dhoop.jpg", "10-afterglow-protocol.jpg", "11-weather-report.jpg"]

# ───────────────────────────────────────────────────────────────── copy (to be res/values/strings.xml)
S = {
    "shelf": "Moods",
    "shelf_overline": "Your songs, sorted by how they feel",
    "picks_title": "What do you like to listen to?",
    "picks_body": "Pick moods, genres and languages, and the Moods shelf follows them.",
    "choose": "Choose",
    "not_now": "Not now",
    "kind": "Mood mix · 25 songs, 1 hr 38 min",
    "note": "Picked from how your songs sound and what you play. New each day.",
    "play": "Play",
    "radio": "Mood radio",
    "search_group": "Moods",
    "result_kind": "Mood",
    "picker": "Moods and interests",
    "intro": "The Moods shelf on Home follows what you pick here. It all stays on this phone.",
    "group_moods": "Moods you like",
    "group_genres": "Genres",
    "group_languages": "Languages",
    "genres_empty": "Genres show up once your songs have genre tags or have been analysed.",
    "save": "Save",
    "settings_section": "Moods",
    "settings_row": "Moods and interests",
    "summary_picked": "Chill, Focus, Romantic · 3 genres · Tamil, English",
    "summary_empty": "Nothing picked yet",
    "context": "Radio · Chill",
    # Strings the app has already (strings.xml at dffdd29).
    "mixes": "Mixes",
    "mixes_overline": "Songs you play, grouped by sound",
    "genres": "Genres",
    "search": "Search",
    "placeholder": "Search your music",
}

# Mood, description, songs in today's mix, the four covers of its mosaic (the first is the first song's). The
# order is the app's fixed order.
MOODS = [
    ("Chill", "Slow and easy", 25, (1, 0, 8, 4)),
    ("Happy", "Bright and cheerful", 25, (9, 3, 7, 2)),
    ("Energetic", "Fast and driving", 21, (10, 7, 9, 3)),
    ("Focus", "Mostly instrumental, steady", 14, (4, 1, 10, 6)),
    ("Party", "Made for dancing", 18, (3, 10, 7, 9)),
    ("Melancholy", "Sad and reflective", 16, (11, 8, 6, 4)),
    ("Romantic", "Tender and warm", 25, (5, 0, 2, 9)),
]
MOOD = {m[0]: m for m in MOODS}
MOODS_ORDER = [m[0] for m in MOODS]
PICKED_MOODS = ["Chill", "Focus", "Romantic"]
# The library's genres, biggest first, and the languages LanguageLabels finds in the demo library.
GENRES = ["Pop", "Film", "Electronic", "Folk, World, & Country", "Jazz", "Latin", "Hip hop", "Funk / Soul",
          "Classical"]
GENRE_TILES = [("Pop", 22, (0, 3, 11, 9)), ("Film", 18, (9, 8, 6, 5)), ("Electronic", 11, (10, 1, 3, 4)),
               ("Folk, World, & Country", 10, (7, 5, 2, 8))]
PICKED_GENRES = ["Pop", "Film", "Jazz"]
LANGUAGES = ["Tamil", "Hindi", "English", "No vocals"]
PICKED_LANGUAGES = ["Tamil", "English"]
# Today's Chill mix as the page lists it: title, artist, cover. No artist follows itself.
CHILL = [("Rain on the Skylight", "Small Hours Tape Club", 1), ("Lights Over the Water", "Mira Okafor", 0),
         ("Nila Paadhai", "Kavya Raman", 8), ("Brushes", "Odile Marchetti", 4), ("Marea Lenta", "Paloma Ferrer", 2),
         ("Low Tide Hymn", "Mira Okafor", 11), ("Geeli Mitti", "Ruhaan Sethi", 6),
         ("Kettle On", "Small Hours Tape Club", 1), ("Neer Kudam", "Kavya Raman", 5),
         ("Slow Elevator", "Odile Marchetti", 4), ("Faro Apagado", "Paloma Ferrer", 2),
         ("Marina at Six", "Tide & Tamarind", 7)]
PLAYING = 1     # Lights Over the Water, the song every player surface in the file shows
# The taste mixes above the Moods shelf (home_mix_name_one), with their mosaics.
MIXES = [("Mix: Mira Okafor and more", 18, (0, 3, 11, 4)),
         ("Mix: Kavya Raman and more", 14, (8, 9, 6, 5)),
         ("Mix: Velvet Circuit and more", 12, (10, 1, 3, 7))]
# The last of Browse's album cards, which show above the Moods group once the page has scrolled.
LAST_ALBUMS = [("Room Tone", "Odile Marchetti", 4), ("Kadalkarai Sessions", "Tide & Tamarind", 7)]

SRC_HOME = f"ui/comfort/ComfortHome.kt · ComfortHome (the Moods shelf; MoodTile and MoodPicksCard are in {MOODS_KT})"
SRC_PAGE = f"{MOODS_KT} · ComfortMood"
SRC_PICKER = f"{MOODS_KT} · ComfortInterests"
SRC_BROWSE = "ui/comfort/ComfortSearch.kt · ComfortSearch (Browse: the Moods group)"
SRC_RESULTS = "ui/comfort/ComfortSearch.kt · ComfortSearch (Results: the mood tile)"
SRC_SETTINGS = "ui/comfort/ComfortSettings.kt · ComfortSettings (the Moods section)"
SRC_NP = "ui/comfort/ComfortNowPlaying.kt · ComfortNowPlaying"
PROPOSED = (f"Proposed: mood mixes. The code lands on {BRANCH}; this frame is not ready for development until "
            "that branch merges.")


def songs(n: int) -> str:
    return f"{n} song" if n == 1 else f"{n} songs"


def cover_path(index: int) -> str:
    p = COVERS / COVER_FILES[index]
    if not p.exists():
        raise SystemExit(f"demo cover {p} is missing: run scripts/store_shots/design_refs.sh")
    return str(p)


# ───────────────────────────────────────────────────────────────── attaching to an opened file
def attach(d: Doc, ctx: dict):
    """Fill the Doc's registries (pages, styles, components and their variants) from the opened file."""
    d.pages = {p["name"]: p["id"] for p in d.cmd("page.list") if not p.get("divider")}
    d._first_page = False
    d.mode_collections = ("Palette", "Tinted", "Elevation", "Expressive preview")
    d.screen_fill, d.icon_color = "Color/ground", "Color/on"
    d.text_styles, d.effect_styles, d.comp, d.screens = {}, {}, {}, {}
    for s in d.cmd("style.list"):
        (d.text_styles if s["type"] == "text" else d.effect_styles if s["type"] == "effect" else {})[s["name"]] = s["id"]
    C = ctx["components"] = {}
    comps = d.cmd("component.list")
    in_set = set()
    for c in comps:
        if c["type"] != "COMPONENT_SET":
            continue
        variants = [k for k in d.get(c["id"], depth=1).get("children", []) if k["type"] == "COMPONENT"]
        in_set |= {k["id"] for k in variants}
        d.comp[c["name"]] = c["id"]
        C[c["name"]] = {"id": c["id"], "variants": {k["name"]: k["id"] for k in variants}}
    for c in comps:
        if c["type"] == "COMPONENT" and c["id"] not in in_set:
            d.comp[c["name"]] = c["id"]
            if c["name"].startswith("Icon/"):
                d.icon_names.add(c["name"])
            else:
                C[c["name"]] = {"id": c["id"], "variants": {}}
    ctx["attached"] = True


def variant(ctx: dict, set_name: str, name: str = "") -> int:
    """A variant by its full name ("Lead=Cover, Current=True"), or a single component."""
    c = ctx["components"][set_name]
    return c["variants"][name] if name else c["id"]


def kids(d: Doc, parent: int) -> list[dict]:
    return d.get(parent, depth=1).get("children", []) or []


def top_named(d: Doc, parent: int, name: str) -> int:
    for n in kids(d, parent):
        if n.get("name") == name:
            return n["id"]
    raise KeyError(f"no layer {name!r} directly under {parent}")


def sections(d: Doc) -> list[dict]:
    return [n for n in kids(d, d.pages[SCREENS]) if n.get("type") == "SECTION"]


def screen_frame(d: Doc, name: str, theme: str = "Light") -> int:
    """A screen frame on the Screens page by its name, outside the Moods section."""
    full = f"{name} · {theme}"
    for sec in sections(d):
        if sec.get("name") == SECTION:
            continue
        for n in kids(d, sec["id"]):
            if n.get("name") == full:
                return n["id"]
    raise KeyError(f"no screen frame {full!r}")


def setp(d: Doc, inst: int, **values):
    """Set component properties by their plain names (Title="Chill"); underscores stand for spaces."""
    have = {p["name"].split("#")[0]: p["name"] for p in d.cmd("component.properties", id=inst).get("properties", [])}
    props = {}
    for k, v in values.items():
        k = k.replace("_", " ")
        if k not in have:
            raise KeyError(f"{inst} has no property {k!r}; it has {sorted(have)}")
        props[have[k]] = v
    d.cmd("instance.setProperties", ids=[inst], properties=props)


def swap(d: Doc, inst: int, component: int):
    d.cmd("instance.swap", ids=[inst], component=component)


def pictures(d: Doc, root: int, *indices: int):
    """Put demo covers into the art layers under `root`, in layer order: a cover's "Art" vector, or a mosaic's
    "Art 1" to "Art 4"."""
    arts = sorted(i for i in d.find(root, name="Art", limit=60) if d.get(i).get("type") in ("VECTOR", "RECTANGLE"))
    for pid, idx in zip(arts, indices if len(indices) > 1 else indices * len(arts)):
        d.cmd("image.replace", ids=[pid], path=cover_path(idx))


def mosaic(d: Doc, ctx: dict, cover: int, covers, size: float | None = None, large: bool = False):
    """Turn a nested Cover/Album instance into the mosaic of `covers`, at `size`."""
    swap(d, cover, variant(ctx, "Cover/Mosaic", "Shape=Squircle large" if large else "Shape=Squircle"))
    if size:
        d.set(cover, width=size, height=size)
    pictures(d, cover, *covers)


def text(d: Doc, parent, chars: str, style: str, color: str, name: str, *, w=None, **kw) -> int:
    """A text layer on a style; with `w` it has that width and wraps."""
    t = d.text(parent, chars, style=style, color=color, name=name, **kw)
    if w == "fill":
        d.set(t, sizingH="fill", textAutoResize="height")
    elif w:
        d.set(t, width=w, textAutoResize="height")
    return t


def slot(d: Doc, card: int) -> int:
    """A Group card instance's Content slot, emptied of the main's example rows."""
    content = d.child(card, "Content")
    old = [k["id"] for k in kids(d, content)]
    if old:
        d.cmd("edit.delete", ids=old)
    return content


# ───────────────────────────────────────────────────────────────── removing an earlier run
def remove(d: Doc, ctx: dict) -> str | None:
    """Delete what an earlier run added. Returns the version that run gave the file, if there was one."""
    version = None
    d.page(SCREENS)
    for sec in sections(d):
        if sec.get("name") == SECTION:
            d.cmd("edit.delete", ids=[sec["id"]])
    page = d.page(LISTS_PAGE)
    for n in kids(d, page):
        if n.get("name") == CARD:
            d.cmd("edit.delete", ids=[n["id"]])
    page = d.page("Changelog")
    entries = top_named(d, page, "Entries")
    for n in kids(d, entries):
        changes = d.find(n["id"], type="TEXT", name="Changes", limit=5)
        if changes and d.get(changes[0]).get("characters", "").startswith(ENTRY_MARK):
            before = d.get(entries)["height"]
            version = n["name"].split()[-1].strip()
            d.cmd("edit.delete", ids=[n["id"]])
            how = top_named(d, page, "How to add an entry")
            d.set(how, y=d.get(how)["y"] - (before - d.get(entries)["height"]))
    attach(d, ctx)
    return version


# ───────────────────────────────────────────────────────────────── components
def build_components(d: Doc, ctx: dict):
    C = ctx["components"]
    page = d.page(LISTS_PAGE)
    bottom = max(d.get(n["id"])["y"] + d.get(n["id"])["height"] for n in kids(d, page))
    card = d.frame(None, name=CARD, x=0, y=bottom + 80, w=1500, layout="v", gap=16, pad=48, fill="Canvas/card",
                   radius=32, sizing=("fixed", "hug"), clip=True)
    d.text(card, "Moods", style="Doc/Heading", color="Canvas/label", name="Heading: Moods")
    d.text(card, f"{MOODS_KT} · MoodTile, MoodPicksCard", style="Doc/Code", color="Canvas/label-muted", name="Source")
    text(d, card, "The Home shelf's parts for mood mixes, proposed with the feature. The tile is BrowseTile's card with "
         "the song count across from the mosaic and the mood's description under its name; it sits in a row that "
         "scrolls sideways, 200dp wide and 10dp apart, like the Instant mixes pager. The card asks for picks once, as "
         "the first thing in the Moods section, until it is saved or dismissed.", "Doc/Body", "Canvas/label", "Notes",
         w=1200)
    row = d.frame(card, name="Components", layout="h", gap=48, sizing=("hug", "hug"), cross="min")

    tile = d.component(row, name="Mood tile", w=200, h=133, layout="v", pad=14, main="spaceBetween",
                       fill="Color/card", clip=True,
                       description=("A mood mix on Home's Moods shelf: BrowseTile's card (card colour, the group radius, "
                                    "14dp padding) at 200 x 133, with a 56dp mosaic of four covers from different albums, "
                                    "the song count across from it, and the mood over its one-line description at the "
                                    f"foot. Compose: {MOODS_KT} · MoodTile."))
    d.set(tile, sizingH="fixed", sizingV="fixed")
    d.bind(tile, "cornerRadius", "Dimension/radius/group")
    top = d.frame(tile, name="Art row", layout="h", sizing=("fill", "hug"), main="spaceBetween", cross="min")
    art = d.instance(variant(ctx, "Cover/Mosaic", "Shape=Squircle"), top, name="Art")
    t_count = d.text(top, "25 songs", style="Scale/Caption", color="Color/on-2", name="Count")
    labels = d.frame(tile, name="Labels", layout="v", gap=0, sizing=("fill", "hug"))
    t_title = d.text(labels, "Chill", style="Component/Browse title", color="Color/on", name="Title")
    t_desc = d.text(labels, "Slow and easy", style="Scale/Caption", color="Color/on-2", name="Description")
    for t in (t_title, t_desc):
        d.set(t, sizingH="fill", textAutoResize="height")
        d.cmd("text.maxLines", ids=[t], lines=1)
    for layer, prop, default in ((t_title, "Title", "Chill"), (t_count, "Count", "25 songs"),
                                 (t_desc, "Description", "Slow and easy")):
        d.prop(tile, prop, "text", layer=layer, default=default)
    d.cmd("component.exposeInstance", id=art)
    d.register("Mood tile", tile)
    C["Mood tile"] = {"id": tile, "variants": {}}

    picks = d.component(row, name="Mood picks card", w=379, layout="v", gap=0, pad=[18, 20, 12, 20],
                        fill="Color/card",
                        description=("Asks for moods, genres and languages once: the first thing in Home's Moods section, "
                                     "under its label and above the tiles, until it is saved or dismissed. Choose opens "
                                     f"Moods and interests; Not now dismisses it. Compose: {MOODS_KT} · MoodPicksCard."))
    d.set(picks, sizingH="fixed", sizingV="hug")
    d.bind(picks, "cornerRadius", "Dimension/radius/group")
    p_title = text(d, picks, S["picks_title"], "Component/Settings title", "Color/on", "Title", w="fill")
    d.frame(picks, name="Gap 2", w=1, h=2)
    p_body = text(d, picks, S["picks_body"], "Component/Settings summary", "Color/on-2", "Body", w="fill")
    d.frame(picks, name="Gap 12", w=1, h=12)
    buttons = d.frame(picks, name="Buttons", layout="h", gap=4, sizing=("hug", "hug"), cross="center")
    b1 = d.instance(variant(ctx, "Button", "Style=Filled, Size=Medium, State=Default, Tone=Default"), buttons,
                    name="Choose")
    setp(d, b1, Label=S["choose"], Show_leading_icon=False)
    b2 = d.instance(variant(ctx, "Button", "Style=Text, Size=Medium, State=Default, Tone=Default"), buttons,
                    name="Not now")
    setp(d, b2, Label=S["not_now"], Show_leading_icon=False)
    d.prop(picks, "Title", "text", layer=p_title, default=S["picks_title"])
    d.prop(picks, "Body", "text", layer=p_body, default=S["picks_body"])
    d.register("Mood picks card", picks)
    C["Mood picks card"] = {"id": picks, "variants": {}}

    for cid, name in ((tile, "MoodTile"), (picks, "MoodPicksCard")):
        d.cmd("codeConnect.set", id=cid, language="compose", importPath="app.attic.player.ui.comfort", name=name)

    for mode in ("Light", "Dark"):
        prev = d.frame(card, name=f"{mode} preview", layout="h", gap=24, pad=24, fill="Color/ground", radius=24,
                       sizing=("hug", "hug"), cross="min")
        for c in d.mode_collections:
            d.set_mode(prev, c, mode)
        d.text(prev, mode, style="Doc/Label", color="Color/on-2", name="Mode label")
        for name in ("Chill", "Focus", "Melancholy"):
            mood_tile(d, ctx, prev, name)
        d.instance("Mood picks card", prev, name="Mood picks card")


def mood_tile(d: Doc, ctx: dict, parent, name: str) -> int:
    _, desc, count, covers = MOOD[name]
    t = d.instance("Mood tile", parent, name=f"Mood tile: {name}")
    setp(d, t, Title=name, Count=songs(count), Description=desc)
    pictures(d, t, *covers)
    return t


# ───────────────────────────────────────────────────────────────── screens
class Screens:
    """Builds the Moods section from copies of frames the file already has."""

    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        bottom = 0
        for n in kids(d, d.pages[SCREENS]):
            g = d.get(n["id"])
            bottom = max(bottom, g.get("y", 0) + g.get("height", 0))
        Area._next_y[(id(d), SCREENS)] = bottom + 200
        self.area = d.area(SECTION, note=(
            f"Mood mixes, proposed: the code lands on {BRANCH}. Each day Attic builds one mix per mood from how the "
            "songs sound, what you picked and what you play. Home gains a Moods shelf after the taste Mixes; before "
            "picks are saved or dismissed, a card under the shelf's label asks for them. A tile opens the mood's "
            "page, which is the genre page's layout with radio in the row of actions. Search's Browse lists the moods "
            "above Genres, and a query that matches a mood's name finds it. Moods and interests, from the card and "
            "from Settings, holds the picks. The 1.0.0 frames predate the Mixes shelf, Browse's Genres and the genre "
            "page, which are drawn here from the code at dffdd29. The tablet frames at the end wait for the shell on "
            f"{TABLET_BRANCH}."))
        self.triggers: dict[str, dict[str, int]] = {}
        self.keys: list[str] = []
        self.tablet_keys: list[str] = []

    # ---- frames
    def copy(self, key: str, name: str, source: str, of: str, *, notes=()) -> int:
        """A copy of screen `of` as the next Light frame of the section, without its handoff marks and links."""
        d, a = self.d, self.area
        src = screen_frame(d, of)
        fid = d.cmd("edit.duplicate", ids=[src], dx=0, dy=0)["ids"][0]
        d.cmd("node.reparent", ids=[fid], parent=d.pages[SCREENS])
        d.set(fid, name=f"{name} · Light", x=a.cursor, y=a.y0 + a.PAD + a.TITLE, devStatus=None)
        if d.cmd("annotation.list", ids=[fid]).get("annotations"):
            d.cmd("annotation.remove", id=fid)
        self.unlink(fid)
        self.register(key, fid, name, source, 411, 914, notes)
        return fid

    def register(self, key, fid, name, source, w, h, notes=(), status="proposed"):
        d, a = self.d, self.area
        d.screens[key] = {"light": fid, "dark": None, "map": {}, "name": name, "source": source, "page": SCREENS,
                          "note": None, "status": status, "h": h, "notes": list(notes)}
        a.cursor += w + a.GAP_X
        a.keys.append(key)
        a.row_h = max(a.row_h, h)
        (self.tablet_keys if status == "in-progress" else self.keys).append(key)
        self.triggers[key] = {}

    def unlink(self, root: int):
        """Drop the prototype links a copy brought with it: they lead into the copied frame's own flow."""
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

    def trig(self, key: str, name: str, layer: int):
        self.triggers[key][name] = layer

    # ---- shared parts
    def label(self, parent, label: str, small: str | None = None) -> int:
        d = self.d
        lab = d.instance(variant(self.ctx, "Section label", "Spacing=Default"), parent, name=f"Label: {label}")
        d.set(lab, sizingH="fill")
        setp(d, lab, Label=label, Show_small=bool(small), Small=small or "", Show_end=False)
        return lab

    def group(self, parent, name: str, pad: bool) -> tuple[int, int]:
        """A Group card with an empty slot. Returns (card, its Content)."""
        d = self.d
        c = d.instance(variant(self.ctx, "Group card", f"Pad={pad}"), parent, name=name)
        return c, slot(d, c)

    def lifted_bar(self, fid: int, title: str, *, back: bool, action: bool = False) -> int:
        """The bar a page shows once it has scrolled, over the status bar's ground. Returns the bar."""
        d = self.d
        scrolled = screen_frame(d, "Album / Scrolled, bar lifted")
        ground, bar = (d.cmd("edit.duplicate", ids=[top_named(d, scrolled, n)], dx=0, dy=0)["ids"][0]
                       for n in ("Bar ground", "Bar"))
        d.cmd("node.reparent", ids=[ground, bar], parent=fid)
        d.set(ground, x=0, y=0)
        d.set(bar, x=0, y=40)
        swap(d, bar, variant(self.ctx, "Top bar", f"Back={'Arrow' if back else 'None'}, Lift=True, Count=False"))
        setp(d, bar, Title=title, Show_action_1=action, Show_action_2=False)
        if action:
            swap(d, d.child(d.child(bar, "Action 1"), "Icon"), d.comp["Icon/settings"])
        return bar

    def raise_chrome(self, fid: int):
        """The dock and the system bars go back on top of what was added."""
        d = self.d
        for name in ("Dock", "Status bar", "Gesture bar"):
            try:
                d.cmd("node.reorder", id=top_named(d, fid, name), index=len(kids(d, fid)) - 1)
            except KeyError:
                pass

    def mixes_shelf(self, parent):
        d = self.d
        self.label(parent, S["mixes"], S["mixes_overline"])
        shelf = d.frame(parent, name="Mixes shelf", layout="h", gap=14, pad=[0, 24, 0, 24], sizing=("fill", "hug"),
                        cross="min")
        for title, count, covers in MIXES:
            t = d.instance(variant(self.ctx, "Tile", "Kind=Album, Current=False"), shelf, name=f"Mix: {title}")
            setp(d, t, Title=title, Subtitle=songs(count))
            mosaic(d, self.ctx, d.child(t, "Cover"), covers, 132, large=True)
            # twoLines = true: a mix's name wraps.
            tt = d.child(t, "Title")
            d.set(tt, sizingH="fill", textAutoResize="height")
            d.cmd("text.maxLines", ids=[tt], lines=2)

    def moods_shelf(self, parent, key: str, names, *, picks: bool):
        d = self.d
        self.label(parent, S["shelf"], S["shelf_overline"])
        if picks:
            box = d.frame(parent, name="Picks", layout="v", pad=[0, 16, 12, 16], sizing=("fill", "hug"))
            card = d.instance("Mood picks card", box, name="Mood picks card")
            d.set(card, sizingH="fill")
            self.trig(key, "choose", d.child(card, "Choose"))
            self.trig(key, "not_now", d.child(card, "Not now"))
        shelf = d.frame(parent, name="Moods shelf", layout="h", gap=10, pad=[0, 16, 0, 16], sizing=("fill", "hug"),
                        cross="min")
        for n in names:
            t = mood_tile(d, self.ctx, shelf, n)
            if n == "Chill":
                self.trig(key, "chill", t)

    # ---- phone frames
    def home(self, key: str, name: str, *, picks: bool):
        d = self.d
        notes = [("Content", "The shelf shows the moods you picked, in the chips' order (here Chill, Focus, Romantic), "
                  "or every mood that has a mix when none is picked. Counts are the day's mix: at most 25.")]
        if picks:
            notes = [("Content", "Nothing is picked yet, so the shelf shows every mood that has a mix, in the fixed "
                      "order. The card goes for good once Save or Not now is used.")]
        notes.append(("Note", "The 1.0.0 Home frames predate the taste Mixes shelf; it is drawn here from "
                      "ComfortHome.kt at dffdd29 (Tile with a mosaic, the name on two lines). The page has scrolled: "
                      "the bar has lifted with the title and the filters are pinned under it."))
        fid = self.copy(key, name, SRC_HOME, "Home / All", notes=notes)
        content = top_named(d, fid, "Content")
        keep = ["Label: Instant mixes", "Pager"]
        after = ["Label: Recently played", "Recently played shelf", "Space for the dock"]
        for n in kids(d, content):
            if n["name"] not in keep + after:
                d.cmd("edit.delete", ids=[n["id"]])
        d.set(content, sizingV="hug")
        self.mixes_shelf(content)
        self.moods_shelf(content, key, MOODS_ORDER if picks else PICKED_MOODS, picks=picks)
        for n in after:
            d.cmd("node.reorder", id=top_named(d, content, n), index=len(kids(d, content)) - 1)
        self.lifted_bar(fid, "Home", back=False, action=True)
        tabs = d.instance(variant(self.ctx, "Sub-tab row", "Lift=True"), fid, name="Pinned filters", x=0, y=96)
        d.set(tabs, width=411)
        self.raise_chrome(fid)
        # Scrolled: the Mixes label sits just under the pinned filters; with the card, the Mixes art has gone under.
        mixes = top_named(d, content, f"Label: {S['mixes']}")
        offset = d.get(mixes)["y"] - 2
        if picks:
            offset = d.get(top_named(d, content, "Mixes shelf"))["y"] + 124
        d.set(content, y=145 - offset)
        return fid

    def page(self, key: str, name: str, *, playing: bool):
        d = self.d
        notes = [("Content", "The mosaic is the first four covers from different albums. The line under the name is "
                  "the kind with the count and length, then the note, as ComfortGenre's meta has two lines."),
                 ("Interaction", f"The sensors button is radio: its content description is \"{S['radio']}\", and it "
                  "starts radio from the mix (RadioController.startFromMood). It takes the place add to playlist has "
                  "on a folder: PlayRow(radio = ...) without add to playlist, since the mix is new each day."),
                 ("Note", "Modelled on ComfortGenre (ComfortCollections.kt at dffdd29), which the 1.0.0 file doesn't "
                  "draw. Collection header and Collection actions are the file's components with the art swapped for "
                  "the mosaic and the third button's glyph for sensors.")]
        if playing:
            notes.insert(1, ("Content", "This mix is playing: Play reads Pause, and the current row has the bars over "
                             "its cover and the accent title, as in every song list."))
        fid = self.copy(key, name, SRC_PAGE, "Folder", notes=notes)
        content = top_named(d, fid, "Content")
        head = top_named(d, content, "Header")
        setp(d, head, Title="Chill", Meta=S["kind"] + "\n" + S["note"], Show_byline=False)
        mosaic(d, self.ctx, d.child(head, "Art"), MOOD["Chill"][3], 176, large=True)
        row = top_named(d, content, "Play row")
        if playing:
            swap(d, row, variant(self.ctx, "Collection actions", "Playing=True"))
        radio = d.child(row, "Add to playlist")
        swap(d, d.child(radio, "Icon"), d.comp["Icon/sensors"])
        d.set(radio, name="Radio")
        songs_card = top_named(d, content, "Songs")
        rows = [n["id"] for n in kids(d, d.child(songs_card, "Content")) if n["name"].startswith("Row: ")]
        for i, (rid, (title, artist, cover)) in enumerate(zip(rows, CHILL)):
            if playing and i == PLAYING:
                swap(d, rid, variant(self.ctx, "Song row", "Lead=Cover, Current=True"))
            setp(d, rid, Title=title, Subtitle=artist, Show_subtitle=True, Show_hairline=i > 0, Show_meta=False,
                 Show_trailing=True)
            d.set(rid, name=f"Row: {title}")
            pictures(d, d.child(rid, "Lead"), cover)
        dock = top_named(d, fid, "Dock")
        swap(d, dock, variant(self.ctx, "Dock", f"Tab=Home, Mini player={playing}"))
        d.set(dock, y=914 - d.get(dock)["height"])
        bar = top_named(d, fid, "Bar")
        self.trig(key, "radio", radio)
        self.trig(key, "play", d.child(row, "Play"))
        self.trig(key, "back", d.child(bar, "Back"))
        return fid

    def browse_rows(self, key: str, parent, prefix: str, tiles, album=False):
        """Browse cards two across in `parent`: (title, second line, covers)."""
        d = self.d
        for r in range(0, len(tiles), 2):
            row = d.frame(parent, name=f"{prefix} row {r // 2 + 1}", layout="h", gap=12, sizing=("fill", "hug"))
            for title, sub, covers in tiles[r:r + 2]:
                t = d.instance(variant(self.ctx, "Browse tile", "Tinted=False"), row, name=f"Browse: {title}")
                d.set(t, sizingH="fill")
                setp(d, t, Title=title, Subtitle=sub)
                if album:
                    swap(d, d.child(t, "Art"), variant(self.ctx, "Cover/Album", "Shape=Squircle, Art=Image, Dimmed=False"))
                    d.set(d.child(t, "Art"), width=56, height=56)
                pictures(d, d.child(t, "Art"), *covers)
                if title == "Chill":
                    self.trig(key, "chill", t)
            if len(tiles[r:r + 2]) == 1:
                # Two fill layers don't share a row evenly here, so the lone card keeps the others' width.
                d.set(t, sizingH="fixed", width=183.5)
                d.frame(row, name="Empty column", w=183.5, h=133)

    def browse_groups(self, key: str, content: int) -> int:
        """The Moods and Genres groups at the end of Browse's list. Returns the Moods label."""
        d = self.d

        def cards(group_name: str) -> int:
            return d.frame(content, name=group_name, layout="v", gap=12, pad=[0, 16, 0, 16], sizing=("fill", "hug"))

        moods_label = self.label(content, S["search_group"])
        self.browse_rows(key, cards("Mood cards"), "Moods", [(m[0], songs(m[2]), m[3]) for m in MOODS])
        self.label(content, S["genres"])
        self.browse_rows(key, cards("Genre cards"), "Genres", [(g[0], songs(g[1]), g[2]) for g in GENRE_TILES])
        d.frame(content, name="Space for the dock", w=411, h=160)
        return moods_label

    BROWSE_NOTE = ("Note", "The 1.0.0 Browse frame predates the Genres group, which ComfortSearch.kt draws at dffdd29; "
                   "the genres here are invented for the demo library.")

    def browse(self, key: str, name: str):
        d = self.d
        notes = [("Content", "Browse lists every mood that has a mix, picked or not, in the fixed order, in the card "
                  "the folders, albums and genres use: the mosaic, the mood and its song count. The page has scrolled "
                  "to the Moods group, so the bar shows the title and the field is pinned. Genres follows the fourth "
                  "row, under the dock here; the full-length frame shows the whole page."), self.BROWSE_NOTE]
        fid = self.copy(key, name, SRC_BROWSE, "Search / Results", notes=notes)
        content = top_named(d, fid, "Content")
        for n in kids(d, content):
            d.cmd("edit.delete", ids=[n["id"]])
        d.set(content, sizingV="hug")
        d.cmd("edit.delete", ids=[top_named(d, fid, "Bar")])
        self.lifted_bar(fid, S["search"], back=False, action=True)
        pinned = top_named(d, fid, "Pinned field")
        d.cmd("node.reorder", id=pinned, index=len(kids(d, fid)) - 1)
        field = d.child(pinned, "Search field")
        swap(d, field, variant(self.ctx, "Search field", "State=Empty, Container=Ground"))
        setp(d, field, Placeholder=S["placeholder"])
        self.raise_chrome(fid)
        last = d.frame(content, name="Browse cards", layout="v", gap=12, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        self.browse_rows(key, last, "Browse", [(t, a, (c,)) for t, a, c in LAST_ALBUMS], album=True)
        moods_label = self.browse_groups(key, content)
        d.set(content, y=160 - d.get(moods_label)["y"] + 6)
        return fid

    def browse_full(self, key: str, name: str):
        d = self.d
        notes = [("Content", "The whole Browse page: the folders and the biggest albums, then Moods, then Genres, all "
                  "in the same card."), self.BROWSE_NOTE]
        fid = self.copy(key, name, SRC_BROWSE, "Search / Browse", notes=notes)
        content = top_named(d, fid, "Content")
        d.set(content, sizingV="hug")
        self.browse_groups(key, content)
        g = d.get(content)
        h = g["y"] + g["height"]
        d.set(fid, height=h)
        d.set(top_named(d, fid, "Dock"), y=h - 184)
        d.set(top_named(d, fid, "Gesture bar"), y=h - 24)
        self.area.row_h = max(self.area.row_h, h)
        d.screens[key]["h"] = h
        return fid

    def results(self, key: str, name: str):
        d = self.d
        notes = [("Content", "A query that matches a mood's name puts the mood on the shelf of what you open "
                  "(artists, folders, albums, genres), as a tile with its mosaic and the kind \"Mood\" under its name. "
                  "Nothing else in the demo library holds \"chi\", so the mood is the only result and there is no "
                  "Songs group.")]
        fid = self.copy(key, name, SRC_RESULTS, "Search / Results", notes=notes)
        field = d.child(top_named(d, fid, "Pinned field"), "Search field")
        setp(d, field, Query="chi")
        content = top_named(d, fid, "Content")
        shelf = top_named(d, content, "Results shelf")
        for n in kids(d, content):
            if n["id"] != shelf:
                d.cmd("edit.delete", ids=[n["id"]])
        for n in kids(d, shelf):
            d.cmd("edit.delete", ids=[n["id"]])
        t = d.instance(variant(self.ctx, "Tile", "Kind=More from, Current=False"), shelf, name="Mood: Chill")
        setp(d, t, Title="Chill", Subtitle=S["result_kind"])
        mosaic(d, self.ctx, d.child(t, "Cover"), MOOD["Chill"][3], 104, large=True)
        d.set(content, sizingV="hug")
        self.trig(key, "chill", t)
        return fid

    def chips(self, parent, name: str, labels, picked=()) -> dict[str, int]:
        d = self.d
        box = d.frame(parent, name=name, layout="h", gap=8, sizing=("fill", "hug"), wrap=True, crossGap=8)
        made = {}
        for lab in labels:
            v = f"Selected={lab in picked}, Container=Card"
            c = d.instance(variant(self.ctx, "Chip", v), box, name=f"Chip: {lab}")
            setp(d, c, Label=lab)
            made[lab] = c
        return made

    def picker_content(self, key: str, parent, *, moods=(), genres=(), languages=(), no_genres=False, width=379):
        """The page under its title: the intro, the three groups and Save."""
        d = self.d
        intro = d.instance(variant(self.ctx, "Note"), parent, name="Intro")
        setp(d, intro, Text=S["intro"])
        d.set(intro, sizingH="fill")

        def chip_group(label: str, name: str, labels, picked):
            self.label(parent, label)
            card, content = self.group(parent, name, True)
            d.set(card, width=width)
            return self.chips(content, f"{name} chips", labels, picked)

        made = chip_group(S["group_moods"], "Moods you like", MOODS_ORDER, moods)
        self.trig(key, "chip", made["Chill"])
        if no_genres:
            self.label(parent, S["group_genres"])
            card, content = self.group(parent, "Genres", True)
            d.set(card, width=width)
            text(d, content, S["genres_empty"], "Component/Settings summary", "Color/on-2", "Empty line", w="fill")
        else:
            chip_group(S["group_genres"], "Genres", GENRES, genres)
        chip_group(S["group_languages"], "Languages", LANGUAGES, languages)

    def bottom_button(self, key: str, fid: int, y: float = 794, width: float | None = None) -> int:
        """Save in the first-run pages' bottom button, pinned over the list, which fades out behind it."""
        d = self.d
        fade_src = top_named(d, screen_frame(d, "Playlist / Reorder"), "Fade")
        src = top_named(d, screen_frame(d, "First run / Look"), "Bottom")
        fade, box = (d.cmd("edit.duplicate", ids=[n], dx=0, dy=0)["ids"][0] for n in (fade_src, src))
        d.cmd("node.reparent", ids=[fade, box], parent=fid)
        d.set(fade, x=0, y=y - 22)
        d.set(box, name="Save bar", x=0, y=y)
        if width:
            d.set(fade, width=width)
            d.set(box, width=width)
        save = d.child(box, "Button")
        d.set(save, name="Save")
        setp(d, save, Label=S["save"], Show_leading_icon=False)
        self.trig(key, "save", save)
        return box

    def picker(self, key: str, name: str, scrolled: bool = False, **state):
        d = self.d
        notes = [("Content", "Every group is optional and takes any number of chips. Moods are in the app's fixed "
                  "order; genres are the library's own, biggest first, up to 24; languages are the ones found in the "
                  "library, with No vocals when it has instrumentals. The hero is the short one, as on a licence's "
                  "page."),
                 ("Interaction", "Save keeps what is chosen and goes back; back keeps what was there. The page is full "
                  "screen, as the first-run pages are, with no dock, so Save stays pinned at the bottom (the first "
                  "run's Continue button) and the list fades out behind it, as it does behind the action bar.")]
        if state.get("no_genres"):
            notes.insert(1, ("Content", "No genre exists yet (no genre tags, and analysis hasn't run), so the group's "
                             "card holds its line and no chips."))
        if scrolled:
            notes.append(("Note", "The page has scrolled past the hero, so the bar has lifted with the title and back."))
        fid = self.copy(key, name, SRC_PICKER, "Licences / List", notes=notes)
        d.cmd("edit.delete", ids=[top_named(d, fid, "Dock")])
        content = top_named(d, fid, "Content")
        hero = top_named(d, content, "Hero")
        swap(d, hero, variant(self.ctx, "Hero", "Mark=False, Short=True"))
        setp(d, hero, Title=S["picker"], Show_subtitle=False)
        d.cmd("edit.delete", ids=[top_named(d, content, "Notices")])
        d.set(content, sizingV="hug")
        self.picker_content(key, content, **state)
        d.frame(content, name="Space for the button", w=411, h=120)
        self.trig(key, "back", d.child(top_named(d, content, "Tools"), "Back"))
        if scrolled:
            bar = self.lifted_bar(fid, S["picker"], back=True)
            self.trig(key, "back", d.child(bar, "Back"))
            d.set(content, y=96 - d.get(top_named(d, content, "Intro"))["y"])
        self.bottom_button(key, fid)
        self.raise_chrome(fid)
        return fid

    def settings(self, key: str, name: str, summary: str):
        d = self.d
        notes = [("Content", f"The summary lists the picked moods, the number of genres and the languages; with none it "
                  f"reads \"{S['summary_empty']}\". The Moods section sits just above Song radio."),
                 ("Note", "The page has scrolled, so the bar has lifted with the title.")]
        fid = self.copy(key, name, SRC_SETTINGS, "Settings / Top", notes=notes)
        content = top_named(d, fid, "Content")
        at = [n["name"] for n in kids(d, content)].index("Label: Song radio")
        lab = self.label(content, S["settings_section"])
        card, slot_ = self.group(content, "Moods", False)
        row = d.instance(variant(self.ctx, "Settings row", "Control=Chevron"), slot_, name="Moods and interests")
        d.set(row, sizingH="fill")
        setp(d, row, Title=S["settings_row"], Summary=summary, Show_summary=True, Show_hairline=False)
        d.cmd("node.reorder", id=lab, index=at)
        d.cmd("node.reorder", id=card, index=at + 1)
        self.lifted_bar(fid, "Settings", back=True)
        self.raise_chrome(fid)
        lyrics = top_named(d, content, "Label: Lyrics")
        d.set(content, y=96 - d.get(lyrics)["y"])
        self.trig(key, "row", row)
        return fid

    def now_playing(self, key: str, name: str):
        d = self.d
        notes = [("Content", f"Radio started from the Chill mix: the context reads \"{S['context']}\", as radio from a "
                  "song or a genre does. Nothing else on the screen changes.")]
        fid = self.copy(key, name, SRC_NP, "Now Playing / Playing", notes=notes)
        head = top_named(d, fid, "Header")
        setp(d, head, Context=S["context"])
        self.trig(key, "collapse", head)
        return fid

    def build_phone(self):
        self.home("moods_home", "Home / Moods shelf", picks=False)
        self.home("moods_home_picks", "Home / Moods shelf, asking for picks", picks=True)
        self.page("moods_page", "Mood / Chill", playing=False)
        self.page("moods_page_playing", "Mood / Chill, playing", playing=True)
        self.browse("moods_browse", "Search / Browse with moods")
        self.browse_full("moods_browse_full", "Search / Browse with moods (full length)")
        self.results("moods_results", "Search / Results for \"chi\"")
        self.picker("moods_picker_empty", "Moods and interests / Nothing picked")
        self.picker("moods_picker_picked", "Moods and interests / Picked", scrolled=True, moods=PICKED_MOODS,
                    genres=PICKED_GENRES, languages=PICKED_LANGUAGES)
        self.picker("moods_picker_no_genres", "Moods and interests / No genres yet", moods=PICKED_MOODS,
                    languages=PICKED_LANGUAGES, no_genres=True)
        self.settings("moods_settings", "Settings / Moods and interests row", S["summary_picked"])
        self.settings("moods_settings_empty", "Settings / Moods and interests row, nothing picked", S["summary_empty"])
        self.now_playing("moods_now_playing", "Now Playing / Mood radio")

    # ---- closing the section
    def close(self):
        d = self.d
        sec = self.area.close()
        d.ann_category("Proposed", "#7C3AED")
        d.ann_category("In progress", "#F59E0B")
        for key in self.keys + self.tablet_keys:
            s = d.screens[key]
            for theme in ("light", "dark"):
                fid = s[theme]
                d.annotate(fid, f"Compose: {s['source']}", "Source")
                d.annotate(fid, PROPOSED, "Proposed")
                if s["status"] == "in-progress":
                    d.annotate(fid, f"In progress: the tablet shell (side dock, player bar, panes) is on {TABLET_BRANCH} "
                               "and isn't merged. This frame follows docs/design/2026-11-25-tablet/SPEC.md and is "
                               "redrawn from that branch's components once it lands.", "In progress")
                for category, note in s["notes"]:
                    d.annotate(fid, note, category)
        ctx = self.ctx
        ctx["moods_screens"] = self.keys
        ctx["moods_tablet_screens"] = self.tablet_keys
        ctx["moods_triggers"] = self.triggers
        return sec


# ───────────────────────────────────────────────────────────────── prototype
def wire(d: Doc, ctx: dict):
    T = ctx["moods_triggers"]
    made = 0

    def link(src: str, trigger: str, dst: str | None, direction: str | None = "left"):
        nonlocal made
        if src not in d.screens or trigger not in T.get(src, {}) or (dst and dst not in d.screens):
            return
        s = d.screens[src]
        layer = T[src][trigger]
        for theme in ("light", "dark"):
            lid = layer if theme == "light" else s["map"].get(layer)
            if lid is None:
                continue
            params = {"id": lid, "trigger": {"type": "onClick"}}
            if dst is None:
                params["action"] = {"type": "back"}
            else:
                params["action"] = {"type": "navigate", "dest": d.screens[dst][theme]}
                params["transition"] = {"type": "slideIn", "direction": direction} if direction else {"type": "dissolve"}
                params["durationMs"] = 400
                params["easing"] = "easeOut"
            d.cmd("proto.interaction.add", **params)
            made += 1

    link("moods_home_picks", "choose", "moods_picker_empty")
    link("moods_home_picks", "not_now", "moods_home", None)
    link("moods_home_picks", "chill", "moods_page")
    link("moods_home", "chill", "moods_page")
    link("moods_page", "radio", "moods_now_playing", "top")
    link("moods_page", "play", "moods_page_playing", None)
    link("moods_page", "back", "moods_home", "right")
    link("moods_page_playing", "radio", "moods_now_playing", "top")
    link("moods_page_playing", "play", "moods_page", None)
    link("moods_page_playing", "back", "moods_home", "right")
    link("moods_browse", "chill", "moods_page")
    link("moods_results", "chill", "moods_page")
    link("moods_settings", "row", "moods_picker_picked")
    link("moods_settings_empty", "row", "moods_picker_empty")
    for key in ("moods_picker_empty", "moods_picker_picked", "moods_picker_no_genres"):
        link(key, "back", "moods_home", "right")
        link(key, "save", "moods_home", "right")
    link("moods_picker_empty", "chip", "moods_picker_picked", None)
    link("moods_now_playing", "collapse", "moods_page_playing", "bottom")
    link("moods_home_expanded", "chill", "moods_page_expanded")
    link("moods_home_medium", "chill", "moods_page_medium")
    for size in ("expanded", "medium"):
        link(f"moods_picker_{size}", "save", f"moods_home_{size}", "right")
        link(f"moods_picker_{size}", "back", f"moods_home_{size}", "right")
    d.cmd("proto.flow.add", start=d.screens["moods_home_picks"]["light"], name="Mood mixes")
    ctx["moods_links"] = made


# ───────────────────────────────────────────────────────────────── Cover and Changelog
def bump(version: str) -> str:
    major, minor, _ = (int(x) for x in version.split("."))
    return f"{major}.{minor + 1}.0"


def update_cover(d: Doc, ctx: dict, kept: str | None) -> str:
    """Set the Cover's version and date. `kept` is the version an earlier run of this module gave the file."""
    page = d.page("Cover")
    cover = top_named(d, page, "Cover")
    value = d.child(d.child(cover, "Meta: Version"), "Value")
    version = kept or bump(d.get(value)["characters"].strip())
    d.set(value, characters=version)
    d.set(d.child(d.child(cover, "Meta: Last updated"), "Value"), characters=UPDATED)
    ctx["moods_version"] = version
    return version


def update_changelog(d: Doc, ctx: dict, version: str):
    page = d.page("Changelog")
    entries = top_named(d, page, "Entries")
    before = d.get(entries)["height"]
    tablet = ctx.get("moods_tablet_screens") or []
    changes = (
        f"{ENTRY_MARK}: the code lands on {BRANCH}. Every frame below is marked Proposed and none is ready for "
        "development.\n"
        "• Components: Mood tile (the Home shelf's tile: BrowseTile's card with the count across from the mosaic and "
        "the description under the name) and Mood picks card.\n"
        f"• Screens: a new Moods section of {len(ctx['moods_screens'])} phone states in Light and Dark: Home with the "
        "Moods shelf after the taste Mixes, the same with the card that asks for picks, the mood page at rest and "
        "playing, Browse with a Moods group above Genres, results for \"chi\", Moods and interests with nothing picked, "
        "with picks and with no genres yet, Settings with its Moods section (with picks and without), and Now Playing "
        "from Radio · Chill."
        + (f" {len(tablet)} tablet frames (Home, the mood page and the picker at 1440 x 900 and 700 x 900) are "
           f"marked In progress: they wait for the tablet shell on {TABLET_BRANCH}.\n" if tablet else "\n") +
        "• Prototype: flow Mood mixes from Home with the card. A tile opens the mood page; Choose and the Settings row "
        "open Moods and interests; its back and Save return to Home; the page's radio button opens Now Playing.")
    e = d.instance("Doc/Changelog entry", entries, name=f"Entry {version}")
    setp(d, e, Version=version, Date=UPDATED, Author=AUTHOR, Changes=changes)
    d.set(e, sizingH="fixed", width=960)
    d.cmd("node.reorder", id=e, index=0)
    grew = d.get(entries)["height"] - before
    how = top_named(d, page, "How to add an entry")
    d.set(how, y=d.get(how)["y"] + grew)


# ───────────────────────────────────────────────────────────────── build
def build(d: Doc, ctx: dict, tablet: bool = True):
    if not ctx.get("attached"):
        print("    moods: skipped in a build from scratch. Run `python3 -m comfort.moods SRC OUT` on the saved file.",
              flush=True)
        return
    kept = remove(d, ctx)
    build_components(d, ctx)
    s = Screens(d, ctx)
    s.build_phone()
    if tablet:
        build_tablet(s)
    s.close()
    wire(d, ctx)
    version = update_cover(d, ctx, kept)
    update_changelog(d, ctx, version)
    d.page("Cover")


def build_tablet(s: "Screens"):
    pass


def export(d: Doc, ctx: dict, out: Path):
    """Every new frame at 2 px per dp: <key>-<light|dark>.png, tablet ones <key>-<size>-<light|dark>.png."""
    out.mkdir(parents=True, exist_ok=True)
    d.page(SCREENS)
    made = []
    for key in ctx["moods_screens"] + ctx["moods_tablet_screens"]:
        stem = key.replace("moods_", "").replace("_", "-")
        for theme in ("light", "dark"):
            p = out / f"{stem}-{theme}.png"
            d.export_png(d.screens[key][theme], p, scale=2)
            made.append(p)
    return made


def run(src: Path, out: Path, export_dir: Path | None = None, tablet: bool = True, log=None) -> dict:
    d = Doc("Attic Comfort", read_roots=[src.parent], write_roots=[out.parent], log=log)
    ctx: dict = {"style": "comfort"}
    try:
        d.tool("open_file", path=str(src.resolve()))
        attach(d, ctx)
        build(d, ctx, tablet)
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
    ap.add_argument("--no-tablet", action="store_true")
    a = ap.parse_args(argv)
    ctx = run(a.src, a.out, a.export, tablet=not a.no_tablet)
    print(f"saved {a.out} ({a.out.stat().st_size // 1024} KB): version {ctx['moods_version']}, "
          f"{len(ctx['moods_screens'])} phone and {len(ctx['moods_tablet_screens'])} tablet screens, "
          f"{ctx['moods_links']} links")


if __name__ == "__main__":
    sys.exit(main())
