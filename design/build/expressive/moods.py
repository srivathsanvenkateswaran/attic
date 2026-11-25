"""Mood mixes in Attic Expressive: the Moods section, its components, links and the handoff marks.

This module adds to a file that already exists. `Attic Expressive.figcraft` is the source, so nothing here
rebuilds what the file holds: `attach` reads the opened file's pages, styles and components, `remove` deletes
whatever an earlier run of this module added, and `build` draws it again. Run it on a copy, look at the
exports, then copy the result over the file:

    cd design/build
    python3 -m expressive.moods "../Attic Expressive.figcraft" /tmp/attic-design/moods/expressive.figcraft \
        [--export /tmp/attic-mood/design/expressive] [--no-tablet]

It can be run again on its own output, and on a later version of the file: the Cover's minor version goes up
once, the first time, and a second run keeps the version its Changelog entry already has.

The feature is a proposal: the code lands on feature/mood-mixes. Every frame carries a `Proposed` annotation
and is not marked ready for development. Drawn from the architecture note (mood-mixes-architecture.md, "Your
picks" and "Screens") and from the code on tier2-integration at dffdd29, which the 1.0.0 frames predate: the
taste Mixes shelf (ExpressiveHome.kt), Search's Genres group (ExpressiveSearch.kt) and the genre page
(ExpressiveCollections.kt · ExpressiveGenre), which the mood page is modelled on.

New components (page Components / Lists and cards, card "Moods")
---------------------------------------------------------------
Mood tile             ExpressiveMoods.kt · MoodTile. BrowseTile's tonal container in the hue of the mix's first
                      cover, 200 x 116: the name over the count, a 64dp mosaic, the description along the foot.
                      [Title, Count, Description]; the mosaic is exposed. Color mode: the first song's album.
Mood picks card       ExpressiveMoods.kt · MoodPicksCard. The question, a line of body, "Choose" and "Not now".
                      [Title, Body]

New variants of components the file had
---------------------------------------
Cover                 Kind=Mosaic, Size=208: the mosaic at the collection header's size.
Collection header     Kind=Mood: the folder's header with the mosaic, and radio (sensors) where the folder has
                      add to playlist, as CollectionHeader(radio = ...) draws a genre. Its second meta line wraps.

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
import re
import sys
from pathlib import Path

from figbuild.kit import Doc, V, Area, DENSITY

SECTION = "Moods"
SCREENS = "Screens"
BRANCH = "feature/mood-mixes"
TABLET_BRANCH = "feature/tablet-layouts"
UPDATED = "2026-11-25"
AUTHOR = "Attic maintainers"
MOODS_KT = "ui/expressive/ExpressiveMoods.kt"
CARD = "Card · Moods"
ENTRY_MARK = "Mood mixes, proposed"
LISTS_PAGE = "Components / Lists and cards"

COVERS = Path("/tmp/attic-dev/design-refs/covers")
COVER_FILES = ["0-paper-lanterns.jpg", "1-tea-at-2-am.jpg", "2-marea-lenta.jpg", "3-glasshouse-summer.jpg",
               "4-room-tone.jpg", "5-kolam.jpg", "6-monsoon-tapes.jpg", "7-kadalkarai-sessions.jpg",
               "8-iravu-rail.jpg", "9-chhat-pe-dhoop.jpg", "10-afterglow-protocol.jpg", "11-weather-report.jpg"]
# The Color collection's mode for each demo cover (Cover is the playing album, Paper Lanterns).
COVER_MODE = {0: "Cover", 1: "Tea at 2 AM", 2: "Marea Lenta", 3: "Glasshouse Summer", 4: "Room Tone", 5: "Kolam",
              6: "Monsoon Tapes", 7: "Kadalkarai Sessions", 8: "Iravu Rail", 9: "Chhat Pe Dhoop",
              10: "Afterglow Protocol", 11: "Weather Report"}

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
    "recent": "Recently played",
    "song_radio": "Song radio",
}

# Mood, description, songs in today's mix, the four covers of its mosaic (the first is the first song's, whose
# hue the tile and the page take). The order is the app's fixed order.
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
# The taste mixes above the Moods shelf (home_mix_name_two / _one), with their mosaics.
MIXES = [("Mix: Mira Okafor and more", 18, (0, 3, 11, 4)),
         ("Mix: Kavya Raman and more", 14, (8, 9, 6, 5)),
         ("Mix: Velvet Circuit and more", 12, (10, 1, 3, 7))]
RECENT = [("Lights Over the Water", "Mira Okafor", 0), ("Iravu Rail", "Kavya Raman", 8),
          ("Chai Ka Doosra Cup", "Ruhaan Sethi", 9), ("Neon Peaches", "Juniper Vale", 3)]

SRC_HOME = f"ui/expressive/ExpressiveHome.kt · ExpressiveHome (the Moods shelf; MoodTile and MoodPicksCard are in {MOODS_KT})"
SRC_PAGE = f"{MOODS_KT} · ExpressiveMood"
SRC_PICKER = f"{MOODS_KT} · ExpressiveInterests"
SRC_BROWSE = "ui/expressive/ExpressiveSearch.kt · ExpressiveSearch (Browse: the Moods group)"
SRC_RESULTS = "ui/expressive/ExpressiveSearch.kt · ExpressiveSearch (Results: the mood row)"
SRC_SETTINGS = "ui/expressive/ExpressiveSettings.kt · ExpressiveSettings (the Moods section)"
SRC_NP = "ui/expressive/ExpressiveNowPlaying.kt · ExpressiveNowPlaying"
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
    d.mode_collections = ("Wallpaper", "Cover", "System", "Art", "Comfort preview", "Phone")
    d.screen_fill, d.icon_color = "Color/surface", "Color/on-surface"
    d.text_styles, d.effect_styles, d.comp, d.screens = {}, {}, {}, {}
    for s in d.cmd("style.list"):
        (d.text_styles if s["type"] == "text" else d.effect_styles if s["type"] == "effect" else {})[s["name"]] = s["id"]
    C = ctx["components"] = {}
    comps = d.cmd("component.list")
    in_set = set()
    for c in comps:
        if c["type"] != "COMPONENT_SET":
            continue
        kids = [k for k in d.get(c["id"], depth=1).get("children", []) if k["type"] == "COMPONENT"]
        in_set |= {k["id"] for k in kids}
        d.comp[c["name"]] = c["id"]
        C[c["name"]] = {"id": c["id"], "variants": {k["name"]: k["id"] for k in kids}}
    for c in comps:
        if c["type"] == "COMPONENT" and c["id"] not in in_set:
            d.comp[c["name"]] = c["id"]
            if c["name"].startswith("Icon/"):
                d.icon_names.add(c["name"])
            else:
                C[c["name"]] = {"id": c["id"], "variants": {}}
    ctx["attached"] = True


def variant(ctx: dict, set_name: str, name: str = "") -> int:
    """A variant by its full name ("Kind=Folder, Mosaic=True"), or a single component."""
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
    """Put demo covers into the Picture layers under `root`, in layer order (a mosaic takes four)."""
    pics = sorted((i for i in d.find(root, name="Picture", limit=40) if d.get(i).get("type") == "RECTANGLE"))
    for pid, idx in zip(pics, indices if len(indices) > 1 else indices * len(pics)):
        d.cmd("image.replace", ids=[pid], path=cover_path(idx))


def color_mode(d: Doc, root: int, cover: int):
    """FromArt with that cover's seed: the album's Color mode on `root` and on every instance inside it. Components
    drawn on a cover's colours carry Color = Cover on their main, which a mode on an outer layer doesn't override."""
    d.set_mode([root] + d.find(root, type="INSTANCE", limit=2000), "Color", COVER_MODE[cover])


def narrow(d: Doc, title: int, style: str, max_w: float):
    """NarrowingText: the title at full width if it fits one line of `max_w`, else at 75, else at 50."""
    chars = d.get(title)["characters"]
    for width in (100, 75, 50):
        probe = d.text(None, chars, style=style, color="Color/on-surface", name="Probe", x=-6000, y=-6000)
        if width != 100:
            d.cmd("text.setVariations", ids=[probe], variations={"wdth": width})
        fits = d.get(probe)["width"] <= max_w
        d.cmd("edit.delete", ids=[probe])
        if fits or width == 50:
            if width != 100:
                d.cmd("text.setVariations", ids=[title], variations={"wdth": width})
            return


def text(d: Doc, parent, chars: str, style: str, color: str, name: str, *, w=None, **kw) -> int:
    """A text layer on a style; with `w` it has that width and wraps."""
    t = d.text(parent, chars, style=style, color=color, name=name, **kw)
    if w == "fill":
        d.set(t, sizingH="fill", textAutoResize="height")
    elif w:
        d.set(t, width=w, textAutoResize="height")
    return t


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
    # The variants this module adds to sets the file had.
    for set_name, vname in (("Collection header", "Kind=Mood"), ("Cover", "Kind=Mosaic, Size=208")):
        vid = ctx["components"].get(set_name, {}).get("variants", {}).get(vname)
        if vid:
            d.page(d_page_of(d, vid))
            d.cmd("edit.delete", ids=[vid])
    for name in ("Mood tile", "Mood picks card"):
        if name in d.comp:
            try:
                d.cmd("edit.delete", ids=[d.comp[name]])
            except Exception:
                pass
    page = d.page("Changelog")
    entries = top_named(d, page, "Entries")
    for n in kids(d, entries):
        changes = [t for t in d.find(n["id"], type="TEXT", name="Changes", limit=5)]
        if changes and d.get(changes[0]).get("characters", "").startswith(ENTRY_MARK):
            before = d.get(entries)["height"]
            version = n["name"].split("·")[-1].strip()
            d.cmd("edit.delete", ids=[n["id"]])
            how = top_named(d, page, "Card · Adding a version")
            d.set(how, y=d.get(how)["y"] - (before - d.get(entries)["height"]))
    attach(d, ctx)
    return version


def d_page_of(d: Doc, node: int) -> str:
    """The name of the page a component is on."""
    for c in d.cmd("component.list"):
        if c["id"] == node:
            return next(n for n, i in d.pages.items() if i == c["page"])
    raise KeyError(node)


# ───────────────────────────────────────────────────────────────── components
def build_components(d: Doc, ctx: dict):
    C = ctx["components"]
    # Cover, Kind=Mosaic at the collection header's size.
    d.page(d_page_of(d, C["Cover"]["id"]))
    mosaic = d.cmd("component.addVariant", id=variant(ctx, "Cover", "Kind=Mosaic, Size=148"), property="Size",
                   value="208")["id"]
    d.set(mosaic, width=208, height=208)
    d.bind(mosaic, "cornerRadius", "Dimension/radius/extra-large")
    C["Cover"]["variants"]["Kind=Mosaic, Size=208"] = mosaic

    # Collection header, Kind=Mood: the folder's header as the code draws a genre's.
    d.page(d_page_of(d, C["Collection header"]["id"]))
    head = d.cmd("component.addVariant", id=variant(ctx, "Collection header", "Kind=Folder"), property="Kind",
                 value="Mood")["id"]
    swap(d, d.child(head, "Cover"), mosaic)
    add = d.child(head, "Add to playlist")
    swap(d, d.child(add, "Icon"), d.comp["Icon/sensors"])
    d.set(add, name="Radio")
    for frame_name, text_name in (("Meta", "Meta text"), ("Second meta", "Second meta text")):
        t = d.child(head, text_name)
        d.set(t, sizingH="fill", textAutoResize="height")
    C["Collection header"]["variants"]["Kind=Mood"] = head

    page = d.page(LISTS_PAGE)
    bottom = max(d.get(n["id"])["y"] + d.get(n["id"])["height"] for n in kids(d, page))
    card = d.frame(None, name=CARD, x=0, y=bottom + 72, w=1712, layout="v", gap=32, pad=56, fill="Canvas/card",
                   radius=32, sizing=("fixed", "hug"), clip=True)
    heading = d.frame(card, name="Heading", layout="v", gap=12, sizing=("hug", "hug"))
    d.text(heading, "Moods", style="Doc/Section title", color="Canvas/label", name="Title")
    text(d, heading, "The Home shelf's parts for mood mixes, proposed with the feature. The tile is BrowseTile's tonal "
         "container, in the hue of the mix's first cover, with the description along its foot; it sits in a Shelf, "
         "200dp wide. The card asks for picks once, as the first thing in the Moods section, until it is saved or "
         "dismissed. The mood page uses Collection header, Kind=Mood, and the mosaic is Cover, Kind=Mosaic, Size=208.",
         "Doc/Body", "Canvas/label", "Description", w=1200)
    d.text(heading, f"Compose: {MOODS_KT} · MoodTile, MoodPicksCard", style="Doc/Code", color="Canvas/label-muted",
           name="Source")
    row = d.frame(card, name="Components", layout="h", gap=48, sizing=("hug", "hug"), cross="min")

    tile = d.component(row, name="Mood tile", w=200, h=108, layout="v", gap=8, pad=[12, 12, 14, 16],
                       fill="Color/secondary-container", clip=True,
                       description=("A mood mix on Home's Moods shelf: BrowseTile's container (secondary container of the "
                                    "first cover's scheme, 20dp corners) at 200 x 108, with the mood over its song count, "
                                    "a 56dp mosaic of four covers from different albums, and the mood's description on "
                                    "one line along the foot. The name is BrowseTile's NarrowingText. Set Color mode to "
                                    "the first song's album. "
                                    f"Compose: {MOODS_KT} · MoodTile."))
    d.set(tile, sizingH="fixed", sizingV="fixed")
    d.set_mode(tile, "Color", "Cover")
    d.bind(tile, "cornerRadius", "Dimension/radius/large-increased")
    top = d.frame(tile, name="Name row", layout="h", gap=8, cross="center", sizing=("fill", "hug"))
    col = d.frame(top, name="Text column", layout="v", gap=2, sizing=("fill", "hug"))
    t_title = d.text(col, "Chill", style="Kit/Browse title large", color="Color/on-secondary-container", name="Title")
    t_count = d.text(col, "25 songs", style="Label/Medium", color="Color/on-surface-variant", name="Count")
    art = d.instance(variant(ctx, "Cover", "Kind=Mosaic, Size=56"), top, name="Mosaic")
    t_desc = d.text(tile, "Slow and easy", style="Body/Small", color="Color/on-secondary-container", name="Description")
    d.set(t_desc, sizingH="fill", textAutoResize="height")
    d.cmd("text.maxLines", ids=[t_desc], lines=1)
    for layer, prop, default in ((t_title, "Title", "Chill"), (t_count, "Count", "25 songs"),
                                 (t_desc, "Description", "Slow and easy")):
        d.prop(tile, prop, "text", layer=layer, default=default)
    d.cmd("component.exposeInstance", id=art)
    d.register("Mood tile", tile)
    C["Mood tile"] = {"id": tile, "variants": {}}

    picks = d.component(row, name="Mood picks card", w=379, layout="v", gap=0, pad=[20, 20, 16, 20],
                        fill="Color/surface-container-high",
                        description=("Asks for moods, genres and languages once: the first thing in Home's Moods section, "
                                     "under its header and above the tiles, until it is saved or dismissed. Choose opens "
                                     f"Moods and interests; Not now dismisses it. Compose: {MOODS_KT} · MoodPicksCard."))
    d.set(picks, sizingH="fixed", sizingV="hug")
    d.bind(picks, "cornerRadius", "Dimension/radius/extra-large")
    p_title = text(d, picks, S["picks_title"], "Title/Medium Emphasized", "Color/on-surface", "Title", w="fill")
    d.frame(picks, name="Gap 6", w=1, h=6)
    p_body = text(d, picks, S["picks_body"], "Body/Medium", "Color/on-surface-variant", "Body", w="fill")
    d.frame(picks, name="Gap 14", w=1, h=14)
    buttons = d.frame(picks, name="Buttons", layout="h", gap=8, sizing=("hug", "hug"), cross="center")
    b1 = d.instance(variant(ctx, "Button", "Style=Filled, Size=Small, Icon=None, State=Enabled, Squared=False"), buttons,
                    name="Choose")
    setp(d, b1, Label=S["choose"])
    b2 = d.instance(variant(ctx, "Button", "Style=Text, Size=Small, Icon=None, State=Enabled, Squared=False"), buttons,
                    name="Not now")
    setp(d, b2, Label=S["not_now"])
    d.prop(picks, "Title", "text", layer=p_title, default=S["picks_title"])
    d.prop(picks, "Body", "text", layer=p_body, default=S["picks_body"])
    d.register("Mood picks card", picks)
    C["Mood picks card"] = {"id": picks, "variants": {}}

    for cid, name in ((tile, "MoodTile"), (picks, "MoodPicksCard")):
        d.cmd("codeConnect.set", id=cid, language="compose", importPath="app.attic.player.ui.expressive", name=name)

    # Both modes side by side, as every component page ends.
    for mode in ("Light", "Dark"):
        prev = d.frame(card, name=f"{mode} preview", layout="h", gap=24, pad=24, fill="Color/surface", radius=24,
                       sizing=("hug", "hug"), cross="min")
        for c in d.mode_collections:
            d.set_mode(prev, c, mode)
        d.text(prev, mode, style="Doc/Label", color="Color/on-surface-variant", name="Mode label")
        for name in ("Chill", "Focus", "Melancholy"):
            mood_tile(d, ctx, prev, name)
        d.instance("Mood picks card", prev, name="Mood picks card")
        h = d.instance(head, prev, name="Collection header")
        mood_header(d, ctx, h)


def mood_tile(d: Doc, ctx: dict, parent, name: str) -> int:
    _, desc, count, covers = MOOD[name]
    t = d.instance("Mood tile", parent, name=f"Mood tile · {name}")
    setp(d, t, Title=name, Count=songs(count), Description=desc)
    pictures(d, t, *covers)
    color_mode(d, t, covers[0])
    return t


def mood_header(d: Doc, ctx: dict, inst: int, playing: bool = False):
    """Fill a Collection header, Kind=Mood, with Chill."""
    setp(d, inst, Title="Chill", Meta=S["kind"], Second_meta=S["note"])
    pictures(d, d.child(inst, "Cover"), *MOOD["Chill"][3])
    color_mode(d, inst, MOOD["Chill"][3][0])
    if playing:
        setp(d, d.child(inst, "Play"), Playing="True")


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
            "picks are saved or dismissed, a card under the shelf's header asks for them. A tile opens the mood's "
            "page, which is the genre page's layout with radio in the cluster. Search's Browse lists the moods above "
            "Genres, and a query that matches a mood's name finds it. Moods and interests, from the card and from "
            "Settings, holds the picks. The 1.0.0 frames predate the Mixes shelf, Browse's Genres and the genre page, "
            "which are drawn here from the code at dffdd29. The tablet frames at the end wait for the shell on "
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
    def cell(self, parent, position: str, row_variant: int, name: str) -> tuple[int, int]:
        """A List group/Cell holding `row_variant`. Returns (cell, row)."""
        d = self.d
        c = d.instance(variant(self.ctx, "List group/Cell", f"Position={position}, Surface=Default"), parent, name=name)
        d.set(c, sizingH="fill")
        row = d.child(c, "Row")
        swap(d, row, row_variant)
        return c, d.child(c, "Row")

    def song_rows(self, parent, rows, *, current: int | None = None):
        d = self.d
        for i, (title, artist, cover) in enumerate(rows):
            pos = "Single" if len(rows) == 1 else "Top" if i == 0 else "Bottom" if i == len(rows) - 1 else "Middle"
            state = "Current playing" if i == current else "Default"
            c, row = self.cell(parent, pos, variant(self.ctx, "List row/Song", f"Lead=Cover, State={state}"),
                               f"Cell · {title}")
            setp(d, row, Title=title, Subtitle=artist, Show_subtitle=True)
            pictures(d, row, cover)

    def header(self, parent, title: str, overline: str | None = None) -> int:
        d = self.d
        v = "Overline=True, Trailing=False" if overline else "Overline=False, Trailing=False"
        h = d.instance(variant(self.ctx, "Section header", v), parent, name=f"Section header · {title}")
        d.set(h, sizingH="fill")
        if overline:
            setp(d, h, Title=title, Overline_text=overline)
        else:
            setp(d, h, Title=title)
        return h

    def shelf(self, parent, name: str, gap=12) -> int:
        return self.d.frame(parent, name=name, layout="h", gap=gap, pad=[0, 16, 0, 16], sizing=("fill", "hug"),
                            cross="min", clip=False)

    def mixes_shelf(self, parent):
        d = self.d
        self.header(parent, S["mixes"], S["mixes_overline"])
        shelf = self.shelf(parent, "Mixes shelf")
        for title, count, covers in MIXES:
            t = d.instance(variant(self.ctx, "Tile", "Kind=Folder, Now playing=False"), shelf, name=f"Tile · {title}")
            setp(d, t, Title=title, Subtitle=songs(count))
            pictures(d, t, *covers)
            # titleLines = 2: a mix's name wraps.
            tt = d.child(t, "Title")
            d.set(d.child(t, "Title row"), sizingV="hug")
            d.set(tt, sizingH="fill", textAutoResize="height")
            d.cmd("text.maxLines", ids=[tt], lines=2)

    def moods_shelf(self, parent, key: str, names, *, picks: bool):
        d = self.d
        self.header(parent, S["shelf"], S["shelf_overline"])
        if picks:
            box = d.frame(parent, name="Picks", layout="v", pad=[0, 16, 12, 16], sizing=("fill", "hug"))
            card = d.instance("Mood picks card", box, name="Mood picks card")
            d.set(card, sizingH="fill")
            self.trig(key, "choose", d.child(card, "Choose"))
            self.trig(key, "not_now", d.child(card, "Not now"))
        shelf = self.shelf(parent, "Moods shelf")
        for n in names:
            t = mood_tile(d, self.ctx, shelf, n)
            if n == "Chill":
                self.trig(key, "chill", t)

    def recent_shelf(self, parent):
        d = self.d
        self.header(parent, S["recent"])
        shelf = self.shelf(parent, "Recently played shelf")
        for i, (title, artist, cover) in enumerate(RECENT):
            v = "Kind=Recent, Now playing=True" if i == 0 else "Kind=Recent, Now playing=False"
            t = d.instance(variant(self.ctx, "Tile", v), shelf, name=f"Tile · {title}")
            setp(d, t, Title=title, Subtitle=artist)
            pictures(d, t, cover)

    # ---- phone frames
    def home(self, key: str, name: str, *, picks: bool):
        d = self.d
        notes = [("Content", "The shelf shows the moods you picked, in the chips' order (here Chill, Focus, Romantic), "
                  "or every mood that has a mix when none is picked. Counts are the day's mix: at most 25.")]
        if picks:
            notes = [("Content", "Nothing is picked yet, so the shelf shows every mood that has a mix, in the fixed "
                      "order. The card goes for good once Save or Not now is used.")]
        notes.append(("Note", "The 1.0.0 Home frames predate the taste Mixes shelf; it is drawn here from "
                      "ExpressiveHome.kt at dffdd29 (Tile with a mosaic, the name on two lines)."))
        fid = self.copy(key, name, SRC_HOME, "Home / All", notes=notes)
        bar = top_named(d, fid, "Top app bar")
        swap(d, bar, variant(self.ctx, "Top app bar/Root", "Kind=Home, Scrolled=True"))
        setp(d, bar, Title="Attic", Show_search=True, Show_grid_toggle=True)
        content = top_named(d, fid, "Content")
        keep = {"Section header · Instant mixes", "Instant mixes"}
        for n in kids(d, content):
            if n["name"] not in keep:
                d.cmd("edit.delete", ids=[n["id"]])
        d.set(content, sizingV="hug")
        self.mixes_shelf(content)
        self.moods_shelf(content, key, MOODS_ORDER if picks else PICKED_MOODS, picks=picks)
        self.recent_shelf(content)
        # Scrolled: the Mixes header sits just under the bar; with the card, the Mixes tiles have gone under it.
        mixes = top_named(d, content, f"Section header · {S['mixes']}")
        offset = d.get(mixes)["y"] - 4
        if picks:
            offset = d.get(top_named(d, content, "Mixes shelf"))["y"] + 132
        d.set(content, y=104 - offset)
        return fid

    def page(self, key: str, name: str, *, playing: bool):
        d = self.d
        notes = [("Content", "The header and bar run FromArt with the first song's cover as the seed (Tea at 2 AM), so "
                  "both are on Color mode Tea at 2 AM. The mosaic is the first four covers from different albums."),
                 ("Interaction", f"The sensors button is radio: its content description is \"{S['radio']}\", and it "
                  "starts radio from the mix (RadioController.startFromMood). Shuffle is beside it, as on the genre "
                  "page. There is no add to playlist: the mix is new each day."),
                 ("Note", "Modelled on ExpressiveGenre (ExpressiveCollections.kt at dffdd29), which the 1.0.0 file "
                  "doesn't draw: the folder's header with a mosaic, the kind line and a second line under the title.")]
        if playing:
            notes.insert(1, ("Content", "This mix is playing: Play reads Pause with the cover's 28dp corners, and the "
                             "current row is on the secondary container with the wave, as in every song list."))
        fid = self.copy(key, name, SRC_PAGE, "Collections / Folder", notes=notes)
        lst = top_named(d, fid, "List")
        head = top_named(d, lst, "Collection header")
        swap(d, head, variant(self.ctx, "Collection header", "Kind=Mood"))
        mood_header(d, self.ctx, head, playing)
        bar = top_named(d, fid, "Collection bar")
        setp(d, bar, Title="Chill")
        color_mode(d, bar, MOOD["Chill"][3][0])
        group = top_named(d, lst, "Songs group")
        for n in kids(d, group):
            d.cmd("edit.delete", ids=[n["id"]])
        self.song_rows(group, CHILL[:8], current=PLAYING if playing else None)
        d.set(lst, sizingV="hug")
        if not playing:
            # Nothing is playing, so there is no mini player and the list runs to the navigation bar.
            d.cmd("edit.delete", ids=[top_named(d, fid, "Mini player")])
        swap(d, top_named(d, fid, "Navigation bar"), variant(self.ctx, "Navigation bar", "Selected=Home"))
        self.trig(key, "radio", d.child(head, "Radio"))
        self.trig(key, "play", d.child(head, "Play"))
        self.trig(key, "back", d.child(bar, "Back"))
        return fid

    def browse_groups(self, key: str, content: int):
        """The Moods and Genres groups, added to Browse's list above its Albums shelf."""
        d = self.d
        albums = [top_named(d, content, "Albums label"), top_named(d, content, "Albums shelf")]

        def label(chars: str) -> int:
            lab = d.cmd("edit.duplicate", ids=[albums[0]], dx=0, dy=0)["ids"][0]
            d.cmd("node.reparent", ids=[lab], parent=content)
            d.set(lab, name=f"{chars} label")
            d.set(d.child(lab, "Label"), characters=chars)
            return lab

        def rows(prefix: str, tiles):
            for r in range(0, len(tiles), 2):
                row = d.frame(content, name=f"{prefix} row {r // 2 + 1}", layout="h", gap=8, pad=[0, 16, 8, 16],
                              sizing=("fill", "hug"))
                for title, count, covers in tiles[r:r + 2]:
                    t = d.instance(variant(self.ctx, "Browse tile", "Size=Folder"), row, name=f"Browse tile · {title}")
                    d.set(t, sizingH="fill")
                    setp(d, t, Title=title, Subtitle=songs(count))
                    pictures(d, t, *covers)
                    color_mode(d, t, covers[0])
                    narrow(d, d.child(t, "Title"), "Kit/Browse title large", 85.5)
                    if title == "Chill":
                        self.trig(key, "chill", t)
                if len(tiles[r:r + 2]) == 1:
                    d.set(d.frame(row, name="Empty column", w=185.5, h=88), sizingH="fill")

        label(S["search_group"])
        rows("Moods", [(m[0], m[2], m[3]) for m in MOODS])
        label(S["genres"])
        rows("Genres", GENRE_TILES)
        for n in albums:
            d.cmd("node.reorder", id=n, index=len(kids(d, content)) - 1)
        d.set(content, sizingV="hug")

    BROWSE_NOTE = ("Note", "The 1.0.0 Browse frame predates the Genres group; it is drawn here from ExpressiveSearch.kt "
                   "at dffdd29 with invented genres for the demo library.")

    def browse(self, key: str, name: str):
        d = self.d
        notes = [("Content", "Browse lists every mood that has a mix, picked or not, in the fixed order, in the tile "
                  "the folders and genres use: the mood, its song count and its mosaic, on the secondary container of "
                  "the first cover's scheme. A long name narrows before it truncates (NarrowingText). The page has "
                  "scrolled past the folders."), self.BROWSE_NOTE]
        fid = self.copy(key, name, SRC_BROWSE, "Search / Browse", notes=notes)
        bar = top_named(d, fid, "Top app bar")
        swap(d, bar, variant(self.ctx, "Top app bar/Root", "Kind=Plain, Scrolled=True"))
        setp(d, bar, Title="Search", Show_search=False, Show_grid_toggle=False)
        content = top_named(d, fid, "Content")
        field = top_named(d, content, "Field row")
        # The field stays under the bar; the list scrolls beneath both.
        d.cmd("node.reparent", ids=[field], parent=fid)
        d.set(field, x=0, y=104, width=411, fill=V("Color/surface"))
        for i, layer in enumerate((content, field, bar)):
            d.cmd("node.reorder", id=layer, index=i)
        self.browse_groups(key, content)
        moods_label = top_named(d, content, f"{S['search_group']} label")
        d.set(content, y=160 + 28 - d.get(moods_label)["y"])
        return fid

    def browse_full(self, key: str, name: str):
        d = self.d
        notes = [("Content", "The whole Browse page: the folders, then Moods, then Genres, then the biggest albums. "
                  "Moods and Genres use the folders' tile."), self.BROWSE_NOTE]
        fid = self.copy(key, name, SRC_BROWSE, "Search / Browse", notes=notes)
        content = top_named(d, fid, "Content")
        self.browse_groups(key, content)
        g = d.get(content)
        h = g["y"] + g["height"] + 24 + 64 + 8 + 88
        d.set(fid, height=h)
        d.set(top_named(d, fid, "Mini player"), y=h - 160)
        d.set(top_named(d, fid, "Navigation bar"), y=h - 88)
        d.set(top_named(d, fid, "Gesture bar"), y=h - 24)
        self.area.row_h = max(self.area.row_h, h)
        d.screens[key]["h"] = h
        return fid

    def results(self, key: str, name: str):
        d = self.d
        notes = [("Content", "A query that matches a mood's name lists the mood with what you open (artists, albums, "
                  "folders, genres), as a collection row with its mosaic and the kind \"Mood\". Nothing else in the "
                  "demo library holds \"chi\", so the mood is the only result and there is no Songs group.")]
        fid = self.copy(key, name, SRC_RESULTS, "Search / Results", notes=notes)
        field = d.child(top_named(d, fid, "Field row"), "Search field")
        setp(d, field, Text="chi")
        res = top_named(d, fid, "Results")
        group = top_named(d, res, "Open group")
        for n in kids(d, res):
            if n["id"] != group:
                d.cmd("edit.delete", ids=[n["id"]])
        for n in kids(d, group):
            d.cmd("edit.delete", ids=[n["id"]])
        c, row = self.cell(group, "Single", variant(self.ctx, "List row/Collection", "Kind=Folder, Mosaic=True"),
                           "Cell · Chill")
        setp(d, row, Title="Chill", Subtitle=S["result_kind"])
        pictures(d, row, *MOOD["Chill"][3])
        d.set(res, sizingV="hug")
        self.trig(key, "chill", c)
        return fid

    def chips(self, parent, name: str, labels, picked=()) -> dict[str, int]:
        d = self.d
        box = d.frame(parent, name=name, layout="h", gap=8, pad=[4, 16, 0, 16], sizing=("fill", "hug"), wrap=True,
                      crossGap=8)
        made = {}
        for lab in labels:
            v = "Selected=True" if lab in picked else "Selected=False"
            c = d.instance(variant(self.ctx, "Filter chip", v), box, name=f"Chip · {lab}")
            setp(d, c, Label=lab)
            made[lab] = c
        return made

    def picker_content(self, key: str, parent, *, moods=(), genres=(), languages=(), no_genres=False):
        """The page under its title: the intro and the three groups."""
        d = self.d
        intro = d.frame(parent, name="Intro", layout="v", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        text(d, intro, S["intro"], "Body/Medium", "Color/on-surface-variant", "Intro text", w="fill")

        def section(label: str) -> int:
            s = d.instance(variant(self.ctx, "Settings section"), parent, name=f"Section {label}")
            d.set(s, sizingH="fill")
            setp(d, s, Label=label)
            return s

        section(S["group_moods"])
        made = self.chips(parent, "Mood chips", [m[0] for m in MOODS], moods)
        self.trig(key, "chip", made["Chill"])
        section(S["group_genres"])
        if no_genres:
            box = d.frame(parent, name="Genres empty", layout="v", pad=[4, 32, 0, 32], sizing=("fill", "hug"))
            text(d, box, S["genres_empty"], "Body/Medium", "Color/on-surface-variant", "Empty line", w="fill")
        else:
            self.chips(parent, "Genre chips", GENRES, genres)
        section(S["group_languages"])
        self.chips(parent, "Language chips", LANGUAGES, languages)

    def bottom_button(self, key: str, fid: int, y: float | None = None, width: float | None = None) -> int:
        """Save in the first-run pages' bottom button, pinned over the list. Returns the frame that holds it."""
        d = self.d
        src = top_named(d, screen_frame(d, "First run / Look"), "Bottom button")
        box = d.cmd("edit.duplicate", ids=[src], dx=0, dy=0)["ids"][0]
        d.cmd("node.reparent", ids=[box], parent=fid)
        d.set(box, name="Save bar", x=0, y=802 if y is None else y, fill=V("Color/surface"), padding=[8, 16, 48, 16])
        if width:
            d.set(box, width=width)
        save = d.child(box, "Continue")
        d.set(save, name="Save")
        setp(d, save, Label=S["save"])
        self.trig(key, "save", save)
        return box

    def picker(self, key: str, name: str, **state):
        d = self.d
        notes = [("Content", "Every group is optional and takes any number of chips. Moods are in the app's fixed "
                  "order; genres are the library's own, biggest first, up to 24; languages are the ones found in the "
                  "library, with No vocals when it has instrumentals."),
                 ("Interaction", "Save keeps what is chosen and goes back; back keeps what was there. The page is full "
                  "screen, as the first-run pages are, with no navigation bar or mini player, so Save stays pinned at "
                  "the bottom (the first run's Continue button) while the groups scroll under it.")]
        if state.get("no_genres"):
            notes.insert(1, ("Content", "No genre exists yet (no genre tags, and analysis hasn't run), so the group "
                             "shows its line and no chips."))
        fid = self.copy(key, name, SRC_PICKER, "Licences / List", notes=notes)
        for layer in ("Mini player", "Navigation bar"):
            d.cmd("edit.delete", ids=[top_named(d, fid, layer)])
        lst = top_named(d, fid, "Licences list")
        d.set(lst, name="Picker list")
        big = top_named(d, lst, "Large title")
        d.set(d.child(big, "Title"), characters=S["picker"])
        d.set(big, padding=[8, 16, 12, 16])
        d.cmd("edit.delete", ids=[top_named(d, lst, "Notices")])
        self.picker_content(key, lst, **state)
        d.set(lst, sizingV="hug")
        bar = top_named(d, fid, "App bar")
        setp(d, bar, Title=S["picker"])
        self.trig(key, "back", d.child(bar, "Back"))
        self.bottom_button(key, fid)
        for layer in ("Status bar", "Gesture bar"):
            d.cmd("node.reorder", id=top_named(d, fid, layer), index=len(kids(d, fid)) - 1)
        return fid

    def settings(self, key: str, name: str, summary: str):
        d = self.d
        notes = [("Content", f"The summary lists the picked moods, the number of genres and the languages; with none it "
                  f"reads \"{S['summary_empty']}\". The Moods section sits just above Song radio."),
                 ("Note", "The page has scrolled, so the bar shows its title.")]
        fid = self.copy(key, name, SRC_SETTINGS, "Settings / Top", notes=notes)
        lst = top_named(d, fid, "Settings list")
        order = [n["name"] for n in kids(d, lst)]
        at = order.index("Section Song radio")
        sec = d.instance(variant(self.ctx, "Settings section"), lst, name="Section Moods")
        d.set(sec, sizingH="fill")
        setp(d, sec, Label=S["settings_section"])
        group = d.frame(lst, name="Moods group", layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        c, row = self.cell(group, "Single", variant(
            self.ctx, "List row/Setting", "Leading=Icon, Lines=Two, Trailing=None, Status line=False"), "Moods and interests")
        setp(d, row, Title=S["settings_row"], Summary=summary, Icon=d.comp["Icon/partly_cloudy_day"])
        d.cmd("node.reorder", id=sec, index=at)
        d.cmd("node.reorder", id=group, index=at + 1)
        bar = top_named(d, fid, "App bar")
        swap(d, bar, variant(self.ctx, "Top app bar/Detail", "Surface=Scrolled"))
        setp(d, bar, Title="Settings", Show_action=False)
        d.set(d.child(bar, "Title box"), visible=True)
        lyrics = top_named(d, lst, "Section Lyrics")
        d.set(lst, y=104 - d.get(lyrics)["y"] + 0)
        self.trig(key, "row", c)
        return fid

    def now_playing(self, key: str, name: str):
        d = self.d
        notes = [("Content", f"Radio started from the Chill mix: the context reads \"{S['context']}\" with the radio "
                  "glyph, as radio from a song or a genre does. Nothing else on the screen changes.")]
        fid = self.copy(key, name, SRC_NP, "Now Playing / Playing", notes=notes)
        head = d.child(top_named(d, fid, "Player"), "Header")
        setp(d, head, Context=S["context"], Context_icon=d.comp["Icon/sensors"], Show_context_icon=True)
        self.trig(key, "collapse", d.child(head, "Collapse") if d.find(head, name="Collapse", limit=1) else head)
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
        self.picker("moods_picker_picked", "Moods and interests / Picked", moods=PICKED_MOODS, genres=PICKED_GENRES,
                    languages=PICKED_LANGUAGES)
        self.picker("moods_picker_no_genres", "Moods and interests / No genres yet", moods=PICKED_MOODS,
                    languages=PICKED_LANGUAGES, no_genres=True)
        self.settings("moods_settings", "Settings / Moods and interests row", S["summary_picked"])
        self.settings("moods_settings_empty", "Settings / Moods and interests row, nothing picked", S["summary_empty"])
        self.now_playing("moods_now_playing", "Now Playing / Mood radio")

    # ---- closing the section
    def close(self):
        d = self.d
        sec = self.area.close()
        proposed = d.ann_category("Proposed", "#7C3AED")
        d.ann_category("In progress", "#F59E0B")
        for key in self.keys + self.tablet_keys:
            s = d.screens[key]
            for theme in ("light", "dark"):
                fid = s[theme]
                d.annotate(fid, f"Compose: {s['source']}", "Source")
                d.annotate(fid, PROPOSED, "Proposed")
                if s["status"] == "in-progress":
                    d.annotate(fid, f"In progress: the tablet shell (rail, player bar, panes) is on {TABLET_BRANCH} and "
                               "isn't merged. This frame follows docs/design/2026-11-25-tablet/SPEC.md and is redrawn "
                               "from that branch's components once it lands.", "In progress")
                for category, note in s["notes"]:
                    d.annotate(fid, note, category)
        ctx = self.ctx
        ctx["moods_screens"] = self.keys
        ctx["moods_tablet_screens"] = self.tablet_keys
        ctx["moods_triggers"] = self.triggers
        return sec


MOODS_ORDER = [m[0] for m in MOODS]


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
def counts(d: Doc) -> dict:
    """What the Cover's Read me counts, read from the file."""
    names = d.cmd("component.list")
    in_set = set()
    sets = [c for c in names if c["type"] == "COMPONENT_SET"]
    for c in sets:
        in_set |= {k["id"] for k in kids(d, c["id"])}
    singles = [c for c in names if c["type"] == "COMPONENT" and c["id"] not in in_set]
    other = [c for c in singles if not c["name"].startswith(("Icon/", "Shape/")) and c["name"] != "Changelog entry"]
    frames = 0
    for sec in sections(d):
        if sec.get("name") != "Outside Attic":
            frames += sum(1 for n in kids(d, sec["id"]) if n.get("name", "").endswith("· Light"))
    return {"components": len(sets) + len(other), "screens": frames}


def bump(version: str) -> str:
    major, minor, _ = (int(x) for x in version.split("."))
    return f"{major}.{minor + 1}.0"


def update_cover(d: Doc, ctx: dict, kept: str | None) -> str:
    """Set the Cover's version and date. `kept` is the version an earlier run of this module gave the file."""
    page = d.page("Cover")
    c = counts(d)
    version = kept
    for n in d.find(page, type="INSTANCE", name="Chip · ", limit=20):
        nm = d.get(n)["name"]
        new = None
        if nm.startswith("Chip · Version "):
            version = version or bump(nm.split("Version ")[1].strip())
            new = f"Version {version}"
        elif nm.startswith("Chip · Last updated "):
            new = f"Last updated {UPDATED}"
        if new:
            setp(d, n, Label=new)
            d.set(n, name=f"Chip · {new}")
    for t in d.find(page, type="TEXT", name="What", limit=40):
        chars = d.get(t)["characters"]
        new = re.sub(r"^\d+ components and sets", f"{c['components']} components and sets", chars)
        new = re.sub(r"^\d+ screen states", f"{c['screens']} screen states", new)
        if new != chars:
            d.set(t, characters=new)
    ctx["moods_counts"] = c
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
        "• Components: Mood tile (the Home shelf's tile: BrowseTile's tonal container with the count, a mosaic and the "
        "description) and Mood picks card.\n"
        "• New variants: Collection header, Kind=Mood (the folder's header with a mosaic and radio in the cluster, as a "
        "genre has), and Cover, Kind=Mosaic, Size=208.\n"
        f"• Screens: a new Moods section of {len(ctx['moods_screens'])} phone states in Light and Dark: Home with the "
        "Moods shelf after the taste Mixes, the same with the card that asks for picks, the mood page at rest and "
        "playing, Browse with a Moods group above Genres, results for \"chi\", Moods and interests with nothing picked, "
        "with picks and with no genres yet, Settings with its Moods section (with picks and without), and Now Playing "
        "from Radio · Chill."
        + (f" {len(tablet)} tablet frames (Home, the mood page and the picker at 1440 x 900 and 700 x 900) are "
           f"marked In progress: they wait for the tablet shell on {TABLET_BRANCH}.\n" if tablet else "\n") +
        "• Prototype: flow Mood mixes from Home with the card. A tile opens the mood page; Choose and the Settings row "
        "open Moods and interests; its back and Save return to Home; the page's radio button opens Now Playing.")
    e = d.instance("Changelog entry", entries, name=f"Entry · {version}")
    setp(d, e, Version=version, Date=UPDATED, Author=AUTHOR, Changes=changes)
    d.set(e, sizingH="fixed", width=1400)
    d.cmd("node.reorder", id=e, index=0)
    grew = d.get(entries)["height"] - before
    how = top_named(d, page, "Card · Adding a version")
    d.set(how, y=d.get(how)["y"] + grew)


# ───────────────────────────────────────────────────────────────── build
def build(d: Doc, ctx: dict, tablet: bool = True):
    if not ctx.get("attached"):
        print("    moods: skipped in a build from scratch. Run `python3 -m expressive.moods SRC OUT` on the saved file.",
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
    d = Doc("Attic Expressive", read_roots=[src.parent], write_roots=[out.parent], log=log)
    ctx: dict = {"style": "expressive"}
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
          f"{ctx['moods_links']} links, counts {ctx['moods_counts']}")


if __name__ == "__main__":
    sys.exit(main())
