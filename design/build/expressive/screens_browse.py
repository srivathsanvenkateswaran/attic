"""Screens: Home, Search, Library, album, artist, playlist, folder, Favourites.

Owner: browse screens agent. Page: Screens (areas Home, Search, Library, Collections).

Every screen is built from the core components (components_core.py) plus the ones this module adds:

Components / Lists and cards
  Hero carousel item     one slot of Home's HeroCarousel (Tiles.kt)  Size=Large|Medium|Small (175 / 92 / 40 x 200).
                         The 200dp picture is centred and cropped by the slot, as maskClip does.
  Collection header      CollectionHeader (ExpressiveCollections.kt)  Kind=Album|Artist|Playlist|Folder|Favourites.
                         Props: Title, Artist (Album only), Meta, Second meta (Album, Folder). Drawn inside FromArt:
                         set Color mode Cover on the instance (not on Favourites, whose seed is unspecified).
  Gone state             GoneState (ExpressiveCollections.kt)  Kind=Album|Artist|Playlist.
Components / States
  Empty state            EmptyState (Surfaces.kt)  Art=Symbol|Clover|None.  Props: Title, Body, Icon (swap).
                         Built only if components_states has not made one already.
Components / Navigation
  System keyboard        the render harness's stand-in keyboard (designrefs DesignHarness.kt Keyboard), 411 x 300.
                         Not Attic UI: Android draws the keyboard. Binds System/keyboard-ground and System/keyboard-key
                         (foundations.py).

Content is DemoLibrary.kt / DemoStates.kt / DesignStates.kt, computed below the same way the app does (biggest
albums, artist ranking, search matching), so counts and durations are the code's.

Prototype triggers go to ctx["triggers"][(src, dst)] = Light layer id; "back" as dst means the back action.
"""
from __future__ import annotations

from figbuild.kit import Doc, V
from expressive.foundations import DocPage, bind_dim, cover_path, shape_path, COLOR_MODE
from expressive.components_core import Spec, comp, frame, inst, txt, set_cover, cover_radius, ring_arcs

STEM = "expressive-{theme}-"
KEYS = ["home", "home_full", "home_albums", "home_artists", "home_folders", "mini_player_paused", "mini_player_open_with",
        "search_browse", "search_typing", "search_results", "search_no_results",
        "library_list", "library_grid", "library_songs", "library_playlists", "library_albums", "library_artists",
        "album", "album_collapsed", "artist", "playlist", "playlist_reorder", "folder", "favourites", "item_missing"]
REFS: dict[str, str] = {k: STEM + k for k in KEYS}

SRC = "ui/expressive/"
W, H = 411, 914
FULL_H = round(7023 / 2.625, 1)          # home_full's render height in dp
NAV_Y, MINI_Y, GESTURE_Y = 826, 754, 890
KEYBOARD_H = 300
COLLAPSED_SCROLL = 319.5               # album_collapsed: how far the list has moved in the render, in dp

# ───────────────────────────────────────────────────────────────── demo library (DemoLibrary.kt)
ALBUMS = [
    ("Mira Okafor", "Paper Lanterns", 2025, "English", 0, [
        ("Lights Over the Water", "3:52"), ("Porch Light", "3:21"), ("Moth Letters", "4:05"), ("Second Bus Home", "3:37"),
        ("Salt in the Radio", "2:58"), ("Paper Lanterns", "4:41"), ("Small Hours, Big Sky", "3:12"), ("Coming Back Slow", "5:03")]),
    ("Mira Okafor", "Weather Report", 2022, "English", 11, [
        ("Pressure Front", "3:44"), ("Umbrella Song", "3:05"), ("Clear by Morning", "4:12"), ("Fog on Garden Road", "3:58"),
        ("Heatwave Postcard", "3:26"), ("Low Tide Hymn", "4:49")]),
    ("Kavya Raman", "Iravu Rail", 2024, "Tamil", 8, [
        ("Iravu Rail", "4:22"), ("Platform Ettu", "3:48"), ("Nila Paadhai", "4:05"), ("Theneer Kadai", "3:16"),
        ("Kadaisi Vandi", "4:37"), ("Ooru Thirumbum", "5:02")]),
    ("Kavya Raman", "Kolam", 2021, "Tamil", 5, [
        ("Vaasal Kolam", "3:34"), ("Maarghazhi Kaalai", "4:18"), ("Thinnai Pechu", "3:51"), ("Neer Kudam", "3:09"),
        ("Saalai Ooram", "4:26")]),
    ("Ruhaan Sethi", "Chhat Pe Dhoop", 2023, "Hindi", 9, [
        ("Chhat Pe Dhoop", "3:47"), ("Patang", "3:22"), ("Chai Ka Doosra Cup", "4:01"), ("Purani Diary", "4:33"),
        ("Sheher Ki Shaam", "3:55"), ("Khidki Wala Ghar", "3:40"), ("Aadhi Neend", "4:14")]),
    ("Ruhaan Sethi", "Monsoon Tapes", 2020, "Hindi", 6, [
        ("Bheegi Sadak", "3:29"), ("Chhatri", "3:03"), ("Geeli Mitti", "4:20"), ("Bijli Gayi", "2:47"),
        ("Tapri Pe Milna", "3:58"), ("Baadal Ka Khat", "4:36")]),
    ("Juniper Vale", "Glasshouse Summer", 2023, "English", 3, [
        ("Glasshouse", "3:39"), ("Neon Peaches", "3:14"), ("Swimming Pool Static", "4:02"), ("Sunburn Choir", "3:31"),
        ("Late Bloom", "4:27")]),
    ("Velvet Circuit", "Afterglow Protocol", 2025, "Late Night", 10, [
        ("Afterglow Protocol", "3:18"), ("Night Drive Firmware", "2:54"), ("Chrome Lungs", "3:07"), ("404 Heartbeat", "2:41"),
        ("Midnight Checksum", "3:26"), ("Drift Mode", "2:59")]),
    ("Small Hours Tape Club", "Tea at 2 AM", 2024, "Late Night", 1, [
        ("Kettle On", "2:16"), ("Rain on the Skylight", "2:48"), ("Cat on the Keyboard", "2:05"),
        ("Library Closing Soon", "2:37"), ("Unsent Drafts", "3:02")]),
    ("Paloma Ferrer", "Marea Lenta", 2022, "Español", 2, [
        ("Marea Lenta", "3:53"), ("Azulejo", "3:24"), ("Verano en la Azotea", "4:08"), ("Calle de los Naranjos", "3:46"),
        ("Faro Apagado", "4:31")]),
    ("Odile Marchetti", "Room Tone", 2021, "English", 4, [
        ("Room Tone", "5:12"), ("Brushes", "4:38"), ("Third Floor Walk-Up", "6:04"), ("Slow Elevator", "4:57"),
        ("Velvet Rope", "5:26")]),
    ("Tide & Tamarind", "Kadalkarai Sessions", 2025, "Tamil", 7, [
        ("Kadalkarai", "4:09"), ("Tamarind Skies", "3:44"), ("Auto Rickshaw Waltz", "3:17"), ("Filter Coffee Funk", "3:36"),
        ("Marina at Six", "4:52")]),
]


class Track:
    def __init__(self, title, artist, album, folder, cover, seconds, number):
        self.title, self.artist, self.album, self.folder = title, artist, album, folder
        self.cover, self.seconds, self.number = cover, seconds, number


def _secs(t):
    m, s = t.split(":")
    return int(m) * 60 + int(s)


TRACKS: dict[str, Track] = {}
ALBUM_TRACKS: dict[str, list[Track]] = {}
for _artist, _name, _year, _folder, _cover, _songs in ALBUMS:
    ALBUM_TRACKS[_name] = []
    for _i, (_t, _d) in enumerate(_songs):
        tr = Track(_t, _artist, _name, _folder, _cover, _secs(_d), _i + 1)
        TRACKS[_t] = tr
        ALBUM_TRACKS[_name].append(tr)
ALL_TRACKS = sorted(TRACKS.values(), key=lambda t: t.title.lower())
ALBUM_INFO = {a[1]: a for a in ALBUMS}
ARTISTS: list[str] = []
for _a in ALBUMS:
    if _a[0] not in ARTISTS:
        ARTISTS.append(_a[0])
ARTIST_TRACKS = {n: [t for a in ALBUMS if a[0] == n for t in ALBUM_TRACKS[a[1]]] for n in ARTISTS}
ARTIST_COVER = {n: ARTIST_TRACKS[n][0].cover for n in ARTISTS}
FOLDERS = ["English", "Tamil", "Hindi", "Español", "Late Night"]
FOLDER_TRACKS = {f: [t for t in TRACKS.values() if t.folder == f] for f in FOLDERS}
BIGGEST_ALBUMS = sorted([a[1] for a in ALBUMS], key=lambda n: -len(ALBUM_TRACKS[n]))
BIGGEST_ARTISTS = sorted(ARTISTS, key=lambda n: -len(ARTIST_TRACKS[n]))
NOW = "Lights Over the Water"


def mosaic(tracks) -> list[int]:
    """mosaicIds: one cover per album, in order. Fewer than four draws the first cover alone (Mosaic)."""
    seen, out = set(), []
    for t in tracks:
        if t.album not in seen:
            seen.add(t.album)
            out.append(t.cover)
    return out


def songs(n):
    return f"{n:,} song" if n == 1 else f"{n:,} songs"


def songs_and_duration(tracks):
    minutes = sum(t.seconds for t in tracks) // 60
    length = f"{minutes // 60} hr {minutes % 60} min" if minutes >= 60 else f"{minutes} min"
    return f"{songs(len(tracks))}, {length}"


def java_hash(s: str) -> int:
    h = 0
    for ch in s:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    return h


def ranked(artist):
    return sorted(ARTIST_TRACKS[artist], key=lambda t: java_hash(t.title) & 0xFF)


def tracks(*titles):
    return [TRACKS[t] for t in titles]


RECENT = tracks("Lights Over the Water", "Iravu Rail", "Chai Ka Doosra Cup", "Neon Peaches", "Marea Lenta",
                "Night Drive Firmware", "Kettle On", "Filter Coffee Funk")
SPEED_DIAL = tracks("Porch Light", "Platform Ettu", "Patang", "Glasshouse", "Azulejo", "Drift Mode")
START = tracks("Moth Letters", "Kadalkarai", "Sheher Ki Shaam", "Rain on the Skylight")
SHORTCUTS = ["Paper Lanterns", "Iravu Rail", "Chhat Pe Dhoop", "Glasshouse Summer", "Afterglow Protocol"]
PLAYLISTS = [("Long drive", 25, tracks("Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak")),
             ("Sunday cleaning", 32, tracks("Glasshouse", "Patang", "Azulejo", "Filter Coffee Funk")),
             ("Focus", 39, tracks("Kettle On", "Room Tone", "Brushes", "Unsent Drafts"))]
LIKED = tracks("Lights Over the Water", "Iravu Rail", "Chai Ka Doosra Cup", "Neon Peaches", "Marea Lenta",
               "Night Drive Firmware", "Kettle On", "Filter Coffee Funk", "Platform Ettu", "Patang", "Room Tone", "Bheegi Sadak")
LONG_DRIVE = tracks("Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak", "Kadaisi Vandi", "Drift Mode",
                    "Sheher Ki Shaam", "Swimming Pool Static", "Faro Apagado", "Midnight Checksum", "Auto Rickshaw Waltz",
                    "Second Bus Home")


def search(q):
    q = q.lower()

    def match(items, name):
        hits = [i for i in items if q in name(i).lower()]
        return sorted(hits, key=lambda i: 0 if name(i).lower().startswith(q) else 1)

    return dict(tracks=match(ALL_TRACKS, lambda t: t.title), artists=match(ARTISTS, lambda a: a)[:5],
                albums=match([a[1] for a in ALBUMS], lambda a: a)[:10], folders=match(FOLDERS, lambda f: f))


# ───────────────────────────────────────────────────────────────── small helpers
def props(d: Doc, ctx: dict, iid: int, comp_name: str, **values):
    """Set component properties by their friendly names (Title, Subtitle...); variant names pass through."""
    names = ctx["components"].get(comp_name, {}).get("props", {})
    out = {}
    for k, v in values.items():
        k = k.replace("_", " ")
        out[names.get(k, k)] = v
    d.cmd("instance.setProperties", ids=[iid], properties=out)


def swap_icon(d: Doc, iid: int, lig: str, filled=False, layer="Icon", color=None):
    """Swap the glyph of an icon layer inside an instance, keeping the ink the slot had: a swapped icon instance
    comes in with the icon component's own ink (Color/on-surface)."""
    icon = d.child(iid, layer)
    if color is None:
        bv = d.get(d.child(icon, "Glyph")).get("boundVariables") or {}
        color = bv.get("fill")
    d.cmd("instance.swap", ids=[icon], component=d.comp[f"Icon/{lig}" + (" (filled)" if filled else "")])
    if color:
        d.set(d.child(icon, "Glyph"), fill=V(color))


def cover_variant(d: Doc, ctx: dict, nested: int, kind: str, size: str):
    """Switch a nested Cover instance to another variant. instance.setProperties leaves a nested instance whose
    component the parent's variant already overrides unchanged, so swap it to the variant's main component."""
    vid = ctx["components"]["Cover"]["variants"][f"Kind={kind}, Size={size}"]
    d.cmd("instance.swap", ids=[nested], component=vid)


def trig(ctx, src, dst, layer):
    """Record a prototype trigger. A second layer for the same (src, dst) is kept beside the first (a list), so the
    navigation bar's Home and the filter's All can both lead to Home."""
    T = ctx.setdefault("triggers", {})
    cur = T.get((src, dst))
    if cur is None:
        T[(src, dst)] = layer
    elif isinstance(cur, list):
        cur.append(layer)
    else:
        T[(src, dst)] = [cur, layer]


# The cover whose seed each FromArt surface takes (rememberArtColor of the first song's cover).
FOLDER_SEED = {"English": 0, "Tamil": 8, "Hindi": 9, "Español": 2, "Late Night": 10}
ARTIST_SEED = {"Kavya Raman": 8}
PLAYLIST_SEED = {"Long drive": 10}


def album_mode(d: Doc, root: int, index: int):
    """FromArt with cover `index`'s seed: that album's Color mode on `root` and every instance inside it. Cover-mode
    components carry Color = Cover on their main component, which a mode on an outer layer does not override."""
    d.set_mode([root] + d.find(root, type="INSTANCE", limit=2000), "Color", COLOR_MODE[index])


def resize_cover(d: Doc, cover: int, size: float, kind="Album"):
    d.set(cover, width=size, height=size)
    if kind != "Artist":
        bind_dim(d, cover, "cornerRadius", cover_radius(size))


def vstack(d, parent, name, *, x=None, y=None, w=W, gap=0, pad=None, fill=None):
    f = d.frame(parent, name=name, layout="v", w=w, x=x, y=y, gap=gap, pad=pad, fill=fill, sizing=("fixed", "hug"))
    return f


def fill_w(d, ids):
    d.set(ids if isinstance(ids, list) else [ids], sizingH="fill")


# ───────────────────────────────────────────────────────────────── components this module adds
def build_components(d: Doc, ctx: dict):
    ib = d.comp["Icon button"]
    ib_v = dict(Style="Standard", Size="Default", Width="Default", State="Enabled")

    # Lists and cards --------------------------------------------------------------------------------
    S = Spec(d, ctx, DocPage.below(d, "Components / Lists and cards"))

    def hero_item(vp):
        w = {"Large": 175, "Medium": 92, "Small": 40}[vp["Size"]]
        c = comp(d, "Hero carousel item", w=w, h=200, clip=True, fill="Color/surface-container-highest")
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        pic = d.image(c, cover_path(0), name="Picture", w=200, h=200, x=round((w - 200) / 2, 2), y=0)
        d.set(pic, constraints={"horizontal": "center", "vertical": "scale"})
        return c, {}

    S.make("Hero carousel item", "Hero carousel item",
           "One slot of Home's carousel (Material's multi-browse carousel): 200dp tall, 28dp corners, 8dp apart. An item "
           "changes width as it passes through the large, medium and small slots; its 200dp picture stays centred and the "
           "slot crops its sides. On a 411dp screen beside the Favourites pill the slots are 175, 92 and 40 wide. A folder "
           "shortcut fills the slot with its mosaic; the demo library's shortcuts are all albums.",
           "Tiles.kt", "HeroCarousel", [dict(Size=s) for s in ("Large", "Medium", "Small")], hero_item)

    titles = {"Album": "Paper Lanterns", "Artist": "Mira Okafor", "Playlist": "Long drive", "Folder": "English",
              "Favourites": "Favourites"}
    metas = {"Album": ("Album • 2025", "8 songs, 30 min"), "Artist": ("14 songs, 54 min", None),
             "Playlist": ("12 songs, 44 min", None), "Folder": ("Playlist from a folder on this phone", "24 songs, 1 hr 32 min"),
             "Favourites": ("12 songs, 43 min", None)}

    def chead(vp):
        kind = vp["Kind"]
        c = comp(d, "Collection header", layout="v", w=W, pad=[108, 16, 20, 16], fill="Color/surface-container",
                 sizing=("fixed", "hug"), cornerRadius=[0, 0, 32, 32])
        lay = {}
        lay["title"] = txt(d, c, titles[kind], "Kit/Artist title" if kind == "Artist" else "Kit/Collection title",
                           "Color/on-surface", "Title", fill=True, lines=3)
        if kind == "Album":
            by = d.frame(c, name="Byline", layout="h", gap=8, pad=[10, 0, 0, 0], cross="center", sizing=("hug", "hug"))
            a = inst(d, "Cover", by, name="Artist picture", Kind="Artist", Size="28")
            set_cover(d, a, 0)
            lay["artist"] = txt(d, by, "Mira Okafor", "Kit/Play label", "Color/primary", "Artist name", lines=1)
        m1, m2 = metas[kind]
        for key, text, nm in (("meta", m1, "Meta"), ("meta2", m2, "Second meta")):
            if text is None:
                continue
            box = d.frame(c, name=nm, layout="v", pad=[6, 0, 0, 0], sizing=("fill", "hug"))
            lay[key] = txt(d, box, text, "Body/Medium", "Color/on-surface-variant", f"{nm} text", fill=True)
        row = d.frame(c, name="Art row", layout="h", gap=12, pad=[18, 0, 0, 0], sizing=("fill", "hug"))
        cv_kind = {"Album": ("Album", "208"), "Artist": ("Artist", "112"), "Playlist": ("Mosaic", "148"),
                   "Folder": ("Mosaic", "148"), "Favourites": ("Favourites", "148")}[kind]
        cv = inst(d, "Cover", row, name="Cover", Kind=cv_kind[0], Size=cv_kind[1])
        if cv_kind[0] == "Artist":
            set_cover(d, cv, 0)
        if cv_kind[1] != "208":
            resize_cover(d, cv, 208, cv_kind[0])
        cl = d.frame(row, name="Play cluster", layout="v", gap=8, h=208, sizing=("fill", "fixed"))
        p = inst(d, "Collection play button", cl, name="Play", Playing="False")
        d.set(p, sizingH="fill", sizingV="fill")
        if kind == "Favourites":
            # Favourites has no cover, so FromArt leaves the app scheme in place: the play button too.
            d.set_mode(p, "Color", "Wallpaper")
        sec = d.frame(cl, name="Secondary actions", layout="h", gap=8, sizing=("fill", "hug"))
        sh = inst(d, "Toggle button", sec, name="Shuffle", Checked="False", Size="Medium", Off_style="Tonal")
        add = inst(d, ib, sec, name="Add to playlist", Style="Tonal", Size="Medium", Width="Default", State="Enabled")
        swap_icon(d, add, "playlist_add")
        fill_w(d, [sh, add])
        return c, lay

    S.make("Collection header", "Collection header",
           "The header block of an album, artist, playlist, folder or Favourites: the cover scheme's surface container "
           "with 32dp bottom corners, running under the status bar (40) with 68dp above the title. The title leads "
           "(32/36, an artist's name 45/48; too wide at full width it narrows to 75, then wraps to three lines), an "
           "album's artist byline in primary, the meta lines, then the 208dp cover beside the play cluster: Play "
           "(round until this collection plays, then squared and reading Pause), shuffle and add to playlist. An empty "
           "collection draws the cluster at 38 %. CollectionFrame wraps it in FromArt(seed): set Color mode Cover on the "
           "instance; Favourites has no seed and stays on the app scheme.",
           "ExpressiveCollections.kt", "CollectionHeader", [dict(Kind=k) for k in titles], chead,
           props=[("Title", "text", "title", None, None), ("Artist", "text", "artist", None, None),
                  ("Meta", "text", "meta", None, None), ("Second meta", "text", "meta2", None, None)])

    gone = {"Album": ("album", False, "This album is gone"), "Artist": ("person", True, "This artist is gone"),
            "Playlist": ("queue_music", False, "This playlist is gone")}

    def gone_state(vp):
        sym, cookie, title = gone[vp["Kind"]]
        c = comp(d, "Gone state", layout="v", w=W, pad=[120, 32, 48, 32], cross="center", sizing=("fixed", "hug"))
        if cookie:
            box = d.frame(c, name="Placeholder", w=112, h=112)
            m = d.vector(box, name="Mask", path=shape_path("Cookie 9", 112), fill="Color/surface-container-highest", x=0, y=0)
            d.set(m, isMask=True)
            d.rect(box, name="Placeholder fill", w=112, h=112, x=0, y=0, fill="Color/surface-container-highest")
            g = d.icon(box, sym, size=48, color="Color/on-surface-variant", name="Glyph")
            d.set(g, x=32, y=32)
        else:
            box = d.frame(c, name="Placeholder", layout="h", w=112, h=112, cross="center", main="center",
                          fill="Color/surface-container-highest")
            bind_dim(d, box, "cornerRadius", "radius/large")
            d.icon(box, sym, size=48, color="Color/on-surface-variant", name="Glyph")
        d.frame(c, name="Gap 24", w=1, h=24)
        t = txt(d, c, title, "Headline/Small Emphasized", "Color/on-surface", "Title", align="center")
        d.frame(c, name="Gap 8", w=1, h=8)
        b = txt(d, c, "It may have been removed from this phone.", "Body/Medium", "Color/on-surface-variant", "Body",
                align="center")
        d.frame(c, name="Gap 24 below", w=1, h=24)
        btn = inst(d, "Button", c, name="Back", Style="Tonal", Size="Small", Icon="Leading", State="Enabled", Squared="False")
        props(d, ctx, btn, "Button", Label="Back")
        swap_icon(d, btn, "arrow_back", layer="Leading icon")
        return c, {"t": t}

    S.make("Gone state", "Gone state",
           "An album, artist or playlist that has gone from the phone: a placeholder in the shape the item had (the "
           "cookie for an artist), the title, one line, and a tonal Back where the thumb is. It sits under a detail bar "
           "with no title and includes the code's padding (72 + 48 above, 32 at the sides).",
           "ExpressiveCollections.kt", "GoneState", [dict(Kind=k) for k in gone], gone_state,
           props=[("Title", "text", "t", None, None)])

    # States -----------------------------------------------------------------------------------------
    if "Empty state" not in d.comp:
        S2 = Spec(d, ctx, DocPage.below(d, "Components / States"))

        def empty(vp):
            art = vp["Art"]
            c = comp(d, "Empty state", layout="v", w=W, pad=[48, 32, 48, 32], cross="center", sizing=("fixed", "hug"))
            lay = {}
            if art == "Rounded":
                box = d.frame(c, name="Art", layout="h", w=112, h=112, cross="center", main="center",
                              fill="Color/secondary-container", radius=36)
                lay["icon"] = d.icon(box, "folder_open", size=48, color="Color/on-secondary-container", name="Icon")
            elif art == "Clover":
                box = d.frame(c, name="Art", w=128, h=128)
                m = d.vector(box, name="Mask", path=shape_path("Clover 4", 128), fill="Color/secondary-container", x=0, y=0)
                d.set(m, isMask=True)
                d.rect(box, name="Clover fill", w=128, h=128, x=0, y=0, fill="Color/secondary-container")
                lay["icon"] = d.icon(box, "folder_open", size=48, color="Color/on-secondary-container", name="Icon")
                d.set(lay["icon"], x=40, y=40)
            if art != "None":
                d.frame(c, name="Gap 24", w=1, h=24)
            lay["t"] = txt(d, c, "Songs you like will appear here" if art == "None" else "No results for “lullaby”",
                           "Headline/Small Emphasized", "Color/on-surface", "Title", align="center")
            d.set(lay["t"], sizingH="fill")
            bbox = d.frame(c, name="Body box", layout="v", pad=[8, 0, 0, 0], sizing=("fill", "hug"))
            lay["b"] = txt(d, bbox, "Tap the heart on a song to save it." if art == "None" else
                           "Check the spelling, or try fewer words.", "Body/Medium", "Color/on-surface-variant", "Body",
                           align="center")
            d.set(lay["b"], sizingH="fill")
            lay["bbox"] = bbox
            return c, lay

        S2.make("Empty state", "Empty state",
                "Nothing to show: an optional picture (a glyph in a 112dp box with 36dp corners, or a 128dp clover for "
                "the empty library), the title in the emphasised headline and a line of help, centred, 48dp above and "
                "below and 32 at the sides. Used by Search with no results, an empty playlist, Favourites with none, and "
                "Home with no songs.",
                "Surfaces.kt", "EmptyState", [dict(Art=a) for a in ("Clover", "Rounded", "None")], empty,
                props=[("Title", "text", "t", None, None), ("Body", "text", "b", None, None),
                       ("Show body", "boolean", "bbox", True, None), ("Icon", "instanceSwap", "icon", None, None)])
        # Binding the Icon swap property puts each variant's glyph back at the icon's own 24dp; restore 48.
        for vid in ctx["components"]["Empty state"]["variants"].values():
            for i in d.find(vid, name="Icon", limit=4):
                n = d.get(i)
                if n["name"] == "Icon" and n["type"] == "INSTANCE":
                    box = d.get(vid, depth=1)
                    d.set(i, width=48, height=48)
                    if d.get(d.child(vid, "Art")).get("layoutMode") in (None, "NONE"):
                        d.set(i, x=40, y=40)

    # Navigation: the stand-in keyboard (System/keyboard-* are made in foundations) --------------------
    S3 = Spec(d, ctx, DocPage.below(d, "Components / Navigation"))

    def keyboard(vp):
        c = comp(d, "System keyboard", layout="v", w=W, h=KEYBOARD_H, gap=4, pad=[44, 0, 0, 0], fill="System/keyboard-ground")
        rows = [10, 9, 9, 5]
        for r, n in enumerate(rows):
            inset = W / 20 if r == 1 else 0
            unit = (W - 2 * inset - 6 * (n + 1)) / (10 if r == 3 else n)
            row = d.frame(c, name=f"Key row {r + 1}", layout="h", gap=6, h=52, pad=[0, 6 + inset, 8, 6 + inset],
                          sizing=("fill", "fixed"))
            for i in range(n):
                kw = unit * 6 + 6 * 5 if (r == 3 and i == 2) else unit
                k = d.rect(row, name="Space bar" if (r == 3 and i == 2) else f"Key {i + 1}", w=round(kw, 2), h=44,
                           fill="System/keyboard-key")
                d.set(k, cornerRadius=6)
        return c, {}

    S3.make("System keyboard", "System keyboard (stand-in)",
            "The phone's keyboard is Android's, not Attic's: this plain stand-in is what the render harness draws so a "
            "screen with the keyboard up shows its real layout. 300dp tall with its suggestion strip; four rows of keys. "
            "Screens resize above it (imePadding) and hide the navigation bar.",
            "../../../storeShots/kotlin/app/attic/player/designrefs/DesignHarness.kt", "Keyboard", [{}], keyboard,
            single=True)
    # The stand-in is drawn by the render harness, not the app.
    d.cmd("codeConnect.set", id=d.comp["System keyboard"], language="compose", importPath="app.attic.player.designrefs",
          name="Keyboard")


# ───────────────────────────────────────────────────────────────── screen scaffolding
class Screen:
    """One phone frame: content first, then the bars on top, as the shell stacks them."""

    def __init__(self, B, a, key, name, source, *, h=H, tab="Home", mini="Playing", note=None):
        self.B, self.d, self.ctx = B, B.d, B.ctx
        self.key, self.h, self.tab, self.mini = key, h, tab, mini
        self.f = a.screen(key, name=name, source=source, h=h)
        self.source = source
        self.notes = [note] if note else []
        self.mock: list[str] = []          # "Code differs from mock" notes

    def finish(self, *, nav=True, mini_y=None, keyboard=False, mini_hook=None):
        d, f, h = self.d, self.f, self.h
        if keyboard:
            kb = d.instance("System keyboard", f, name="Keyboard (system)", x=0, y=h - KEYBOARD_H)
            self.notes.append("The keyboard is Android's; the frame shows the render harness's plain stand-in "
                              "(Components / Navigation, System keyboard).")
        if self.mini:
            y = mini_y if mini_y is not None else h - 160
            m = inst(d, "Mini player", f, name="Mini player", State=self.mini, Title_length="Default", x=12, y=y)
            trig(self.ctx, self.key, "now_playing", m)
            if mini_hook:
                mini_hook(m)
        if nav:
            n = inst(d, "Navigation bar", f, name="Navigation bar", Selected=self.tab, x=0, y=h - 88)
            for dest, key in (("Home", "home"), ("Search", "search_browse"), ("Library", "library_list")):
                if dest != self.tab or self.key != key:
                    item = d.child(n, f"Item {dest}")
                    if self.key != key:
                        trig(self.ctx, self.key, key, item)
        d.instance("Gesture bar", f, name="Gesture bar", x=0, y=h - 24)
        d.instance("Status bar", f, name="Status bar", x=0, y=0)
        d.annotate(f, f"Compose: {self.source}", "Source")
        for n_ in self.notes:
            d.annotate(f, n_, "Content")
        for n_ in self.mock:
            d.annotate(f, n_, "Code differs from mock")
        d.ready_for_dev(f)


class Builder:
    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx

    # rows ---------------------------------------------------------------------------------------------
    def group(self, parent, rows, name="Group", surface="Default"):
        """A list group: cells 2dp apart, 16 in from the edges. rows: dicts for song_row / coll_row."""
        d = self.d
        g = d.frame(parent, name=name, layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        n = len(rows)
        out = []
        for i, r in enumerate(rows):
            pos = "Single" if n == 1 else ("Top" if i == 0 else ("Bottom" if i == n - 1 else "Middle"))
            c = inst(d, "List group/Cell", g, name=f"Cell {i + 1}", Position=pos, Surface=surface)
            d.set(c, sizingH="fill")
            row = d.child(c, "Row")
            if r.get("kind") == "coll":
                d.cmd("instance.swap", ids=[row], component=d.comp["List row/Collection"])
                self.coll_row(row, **{k: v for k, v in r.items() if k != "kind"})
            else:
                self.song_row(row, **{k: v for k, v in r.items() if k != "kind"})
            d.set(c, name=f"Cell · {r['title']}")
            out.append((c, row))
        return g, out

    def song_row(self, row, *, title, sub=None, cover=None, lead="Cover", number=None, trailing=None):
        d, ctx = self.d, self.ctx
        tr = TRACKS.get(title)
        sub = sub if sub is not None else (tr.artist if tr else "")
        cover = cover if cover is not None else (tr.cover if tr else 0)
        current = title == NOW
        state = "Current playing" if current else "Default"
        if lead == "Ranked cover" and current:
            state = "Current playing"
        props(d, ctx, row, "List row/Song", Lead=lead, State=state, Title=title, Subtitle=sub)
        if lead in ("Cover", "Ranked cover"):
            set_cover(d, row, cover)
        if lead == "Number" and not current:
            d.set(d.child(row, "Number"), characters=str(number))
        if lead == "Ranked cover":
            d.set(d.child(row, "Rank"), characters=str(number))
        if trailing == "drag":
            d.cmd("instance.swap", ids=[d.child(row, "Trailing")], component=d.comp["List row/Drag handle"])

    def coll_row(self, row, *, title, sub, type="Album", covers=(0,)):
        d, ctx = self.d, self.ctx
        # Mosaic draws one cover when fewer than four albums are in the list: the row's Mosaic=False variant.
        mosaic_ = type in ("Playlist", "Folder") and len(covers) >= 4
        props(d, ctx, row, "List row/Collection", Kind=type, Mosaic=str(mosaic_), Title=title, Subtitle=sub)
        lead = d.child(row, "Lead")
        if mosaic_:
            set_cover(d, lead, *covers[:4])
        elif type != "Favourites":
            set_cover(d, lead, covers[0])

    # tiles --------------------------------------------------------------------------------------------
    def tile(self, parent, typ, title, sub, covers, *, width=None, now=False, name=None):
        d, ctx = self.d, self.ctx
        t = inst(d, "Tile", parent, name=name or f"Tile · {title}", Kind=typ, Now_playing=str(now))
        if sub is None:
            props(d, ctx, t, "Tile", Title=title)
        else:
            props(d, ctx, t, "Tile", Title=title, Subtitle=sub)
        art = d.child(t, "Art")
        if typ == "Folder" and len(covers) < 4:
            cover_variant(d, ctx, art, "Album", "148")
        if covers is not None:
            if typ == "Folder" and len(covers) >= 4:
                set_cover(d, art, *covers[:4])
            elif covers:
                set_cover(d, art, covers[0])
        if width is not None:
            d.set(t, width=width)
            resize_cover(d, art, width, "Artist" if typ == "Artist" else "Album")
        return t

    def shelf(self, parent, name, gap=12):
        d = self.d
        return d.frame(parent, name=name, layout="h", w=W, gap=gap, pad=[0, 16, 0, 16], clip=True, sizing=("fixed", "hug"))

    def section(self, parent, title, overline=None):
        d = self.d
        s = inst(d, "Section header", parent, name=f"Section header · {title}", Overline=str(bool(overline)), Trailing="False")
        if overline:
            props(d, self.ctx, s, "Section header", Title=title, Overline_text=overline)
        else:
            props(d, self.ctx, s, "Section header", Title=title)
        return s

    def list_label(self, parent, text):
        d = self.d
        l_ = d.instance("List label", parent, name=f"List label · {text}")
        props(d, self.ctx, l_, "List label", Label=text)
        return l_


# ───────────────────────────────────────────────────────────────── Home
def home_content(B: Builder, s: Screen, col, *, full: bool):
    d, ctx = B.d, B.ctx
    places = d.frame(col, name="Places", layout="h", w=W, gap=8, pad=[12, 0, 0, 16], clip=True, sizing=("fixed", "hug"))
    pill = inst(d, "Favourites pill", places, name="Favourites pill", Height="248")
    trig(ctx, s.key, "favourites", pill)
    car = d.frame(places, name="Carousel", layout="v", sizing=("hug", "hug"))
    row = d.frame(car, name="Carousel items", layout="h", gap=8, sizing=("hug", "hug"))
    for size, album in zip(("Large", "Medium", "Small"), SHORTCUTS):
        it = inst(d, "Hero carousel item", row, name=f"Carousel item · {album}", Size=size)
        set_cover(d, it, ALBUM_INFO[album][4])
        if size == "Large":
            trig(ctx, s.key, "album", it)
    cap = d.instance("Hero caption", car, name="Hero caption")
    props(d, ctx, cap, "Hero caption", Title="Paper Lanterns", Subtitle="Mira Okafor, 2025")
    rc = d.instance("Recap entry", col, name="Recap entry")
    trig(ctx, s.key, "recap", rc)
    B.section(col, "Instant mixes")
    mixes = d.frame(col, name="Instant mixes", layout="v", w=W, gap=8, pad=[0, 16, 0, 16], clip=True, sizing=("fixed", "hug"))
    for i in range(0, 6 if full else 4, 2):
        r = d.frame(mixes, name=f"Mix row {i // 2 + 1}", layout="h", gap=8, sizing=("hug", "hug"))
        for t in SPEED_DIAL[i:i + 2]:
            m = inst(d, "Instant mix pill", r, name=f"Instant mix · {t.title}", Now="False")
            props(d, ctx, m, "Instant mix pill", Title=t.title)
            set_cover(d, m, t.cover)
    if not full:
        # The render stops under the third row's top edge; keep the third row so the page reads as it scrolls.
        r = d.frame(mixes, name="Mix row 3", layout="h", gap=8, sizing=("hug", "hug"))
        for t in SPEED_DIAL[4:6]:
            m = inst(d, "Instant mix pill", r, name=f"Instant mix · {t.title}", Now="False")
            props(d, ctx, m, "Instant mix pill", Title=t.title)
            set_cover(d, m, t.cover)
        return
    B.section(col, "Recently played")
    sh = B.shelf(col, "Recently played shelf")
    for t in RECENT[:5]:
        B.tile(sh, "Recent", t.title, t.artist, [t.cover], now=t.title == NOW)
    B.section(col, "Start listening", overline="Picked at random from your songs")
    B.group(col, [dict(title=t.title) for t in START], name="Start listening group")
    B.section(col, "Your folders")
    sh = B.shelf(col, "Folders shelf")
    for i, f in enumerate(FOLDERS[:3]):
        t = B.tile(sh, "Folder", f, songs(len(FOLDER_TRACKS[f])), mosaic(FOLDER_TRACKS[f]))
        if i == 0:
            trig(ctx, s.key, "folder", t)
    B.section(col, "Albums")
    sh = B.shelf(col, "Albums shelf")
    for a in BIGGEST_ALBUMS[:3]:
        t = B.tile(sh, "Album", a, ALBUM_INFO[a][0], [ALBUM_INFO[a][4]])
    sp = d.instance("Spotlight", col, name="Spotlight")
    trig(ctx, s.key, "artist", sp)
    B.section(col, "Top artists")
    sh = B.shelf(col, "Top artists shelf")
    for a in BIGGEST_ARTISTS[:4]:
        B.tile(sh, "Artist", a, None, [ARTIST_COVER[a]])


def home_bar(B: Builder, s: Screen, filt: str):
    d, ctx = B.d, B.ctx
    bar = inst(d, "Top app bar/Root", s.f, name="Top app bar", Kind="Home", Scrolled="False", x=0, y=0)
    trig(ctx, s.key, "settings", d.child(bar, "Settings"))
    sel = {"All": 1, "Albums": 2, "Artists": 3, "Folders": 4}[filt]
    g = inst(d, "Connected button group", None, name="Home filter", Options="4", Selected=str(sel), On_cell="False")
    for i, k in enumerate(("home", "home_albums", "home_artists", "home_folders")):
        if k != s.key and not (s.key in ("home_full", "mini_player_paused", "mini_player_open_with") and k == "home"):
            trig(ctx, s.key, k, d.child(g, f"Segment {i + 1}"))
    return bar, g


def build_home(B: Builder):
    d, ctx = B.d, B.ctx
    a = d.area("Home", note="Home under each filter. All is the carousel with Favourites pinned, the Recap card, Instant "
                             "mixes, Recently played, Start listening, folders, albums, the More from block and top "
                             "artists; another filter gives its one kind of content the page as a grid.")
    src = SRC + "ExpressiveHome.kt · ExpressiveHome"

    for key, name, h, full, mini in [("home", "Home / All", H, False, "Playing"),
                                     ("home_full", "Home / All (full length)", FULL_H, True, "Playing"),
                                     ("mini_player_paused", "Home / All, mini player paused", H, False, "Paused")]:
        s = Screen(B, a, key, name, src if key != "mini_player_paused" else SRC + "ExpressiveShell.kt · ExpressiveMiniPlayer",
                   h=h, mini=mini)
        col = vstack(d, s.f, "Content", x=0, y=112)
        _, g = home_bar(B, s, "All")
        d.cmd("node.reparent", ids=[g], parent=col, index=0)
        home_content(B, s, col, full=full)
        if full:
            s.notes.append("The whole page from the filter to Top artists, then the dock, as home_full renders it. Shelves "
                           "run off the right edge; the page scrolls under the root bar, which takes surface container "
                           "once it has.")
        if key == "mini_player_paused":
            s.notes.append("Same as Home with playback paused: the mini player's control is round and shows play.")
        s.finish()

    # From 24 December the Recap card names the year's Recap (HomeModel yearEnded, home_recap_title_year).
    s = Screen(B, a, "home_recap_year", "Home / All, Recap card from 24 December", src)
    col = vstack(d, s.f, "Content", x=0, y=112)
    _, g = home_bar(B, s, "All")
    d.cmd("node.reparent", ids=[g], parent=col, index=0)
    home_content(B, s, col, full=False)
    props(d, ctx, d.child(col, "Recap entry"), "Recap entry", Title="Your 2026 Recap")
    s.notes.append("From 24 December to the end of the year the Recap card's title names the year's Recap "
                   "(home_recap_title_year, HomeModel yearEnded); the rest of the year it reads \"Your 2026 so far\".")
    s.finish()

    # A file opened from another app: no cover, no artist, not a favourite, and no seed, so the app scheme.
    s = Screen(B, a, "mini_player_open_with", "Home / All, mini player with a file opened from another app",
               SRC + "ExpressiveShell.kt · ExpressiveMiniPlayer")
    col = vstack(d, s.f, "Content", x=0, y=112)
    _, g = home_bar(B, s, "All")
    d.cmd("node.reparent", ids=[g], parent=col, index=0)
    home_content(B, s, col, full=False)

    def open_with(m):
        d.set_mode(m, "Color", "Wallpaper")
        props(d, ctx, m, "Mini player", Title="band-practice", Artist="")
        d.set(d.child(d.child(m, "Cover"), "Picture"), visible=False)
        d.cmd("instance.setProperties", ids=[d.child(m, "Heart")], properties={"Favourite": "False"})
        ring_arcs(d, d.child(m, "Play"), 41 / 187)

    s.notes.append("\"band-practice\", a voice note opened with Attic from a file manager: it is not in the library, so "
                   "the mini player shows the no-art note, no artist line and an empty heart, and FromArt has no seed, "
                   "so the pill is on the app scheme. The ring shows 0:41 of 3:07.")
    s.finish(mini_hook=open_with)
    # This mini player opens the same file's Now Playing.
    T = ctx["triggers"]
    T[("mini_player_open_with", "open_with")] = T.pop(("mini_player_open_with", "now_playing"))

    for key, filt, title in [("home_albums", "Albums", "Albums"), ("home_artists", "Artists", "Artists"),
                             ("home_folders", "Folders", "Folders")]:
        s = Screen(B, a, key, f"Home / {title}", src)
        col = vstack(d, s.f, "Content", x=0, y=112)
        _, g = home_bar(B, s, filt)
        d.cmd("node.reparent", ids=[g], parent=col, index=0)
        cols = 3 if filt == "Artists" else 2
        grid = d.frame(col, name=f"{title} grid", layout="v", w=W, gap=16 if cols == 3 else 20, pad=[16, 16, 0, 16],
                       sizing=("fixed", "hug"))
        tw = round((W - 32 - 12 * (cols - 1)) / cols, 2)
        if filt == "Albums":
            items = [("Album", n, ALBUM_INFO[n][0], [ALBUM_INFO[n][4]], "album") for n in BIGGEST_ALBUMS[:6]]
        elif filt == "Artists":
            items = [("Artist", n, None, [ARTIST_COVER[n]], "artist") for n in BIGGEST_ARTISTS]
        else:
            items = [("Folder", f, songs(len(FOLDER_TRACKS[f])), mosaic(FOLDER_TRACKS[f]), "folder") for f in FOLDERS]
        for r in range(0, len(items), cols):
            row = d.frame(grid, name=f"Grid row {r // cols + 1}", layout="h", gap=12, sizing=("fill", "hug"))
            for typ, t, sub, cv, dst in items[r:r + cols]:
                tl = B.tile(row, typ, t, sub, cv, width=tw)
                if r == 0 and t == items[0][1]:
                    trig(ctx, key, dst, tl)
        s.finish()
    a.close()


# ───────────────────────────────────────────────────────────────── Search
def build_search(B: Builder):
    d, ctx = B.d, B.ctx
    a = d.area("Search", note="At rest: the root bar, the search field and Browse (folders as tiles in their own covers' "
                              "colours, then the biggest albums). With focus or a query the field docks under the status "
                              "bar behind a back arrow and the page becomes results.")
    src = SRC + "ExpressiveSearch.kt · ExpressiveSearch"
    browse_note = ("Each browse tile runs FromArt with its first song's cover as the seed, so each is on that album's "
                   "Color mode: English on Paper Lanterns (Cover), Tamil on Iravu Rail, Hindi on Chhat Pe Dhoop, Español "
                   "on Marea Lenta, Late Night on Afterglow Protocol.")

    # Browse
    s = Screen(B, a, "search_browse", "Search / Browse", src, tab="Search")
    bar = inst(d, "Top app bar/Root", s.f, name="Top app bar", Kind="Plain", Scrolled="False", x=0, y=0)
    props(d, ctx, bar, "Top app bar/Root", Title="Search", Show_search=False, Show_grid_toggle=False)
    trig(ctx, s.key, "settings", d.child(bar, "Settings"))
    col = vstack(d, s.f, "Content", x=0, y=112)
    fw = d.frame(col, name="Field row", layout="h", w=W, pad=[0, 16, 0, 16], sizing=("fixed", "hug"))
    fld = inst(d, "Search field", fw, name="Search field", State="Empty")
    d.set(fld, sizingH="fill")
    trig(ctx, s.key, "search_typing", fld)
    B.section(col, "Browse")
    for r in range(0, len(FOLDERS), 2):
        row = d.frame(col, name=f"Browse row {r // 2 + 1}", layout="h", w=W, gap=8, pad=[0, 16, 8, 16], sizing=("fixed", "hug"))
        for f in FOLDERS[r:r + 2]:
            t = inst(d, "Browse tile", row, name=f"Browse tile · {f}", Size="Folder")
            props(d, ctx, t, "Browse tile", Title=f, Subtitle=songs(len(FOLDER_TRACKS[f])))
            if len(FOLDERS[r:r + 2]) == 2:
                d.set(t, sizingH="fill")
            else:
                # Alone in its row the tile keeps half the width (a fill child would grow to fit its title).
                d.set(t, width=round((W - 32 - 8) / 2, 2))
            # The tile runs FromArt itself with its first song's cover as the seed.
            album_mode(d, t, FOLDER_SEED[f])
            pic = d.child(t, "Picture frame")
            mo = mosaic(FOLDER_TRACKS[f])
            if len(mo) < 4:
                cover_variant(d, ctx, pic, "Album", "64")
                bind_dim(d, pic, "cornerRadius", "radius/large")
                set_cover(d, pic, mo[0])
            else:
                set_cover(d, pic, *mo[:4])
            if f == "English":
                trig(ctx, s.key, "folder", t)
        if len(FOLDERS[r:r + 2]) == 1:
            sp = d.frame(row, name="Empty column", w=round((W - 32 - 8) / 2, 2), h=88)
    lab = d.frame(col, name="Albums label", layout="h", w=W, pad=[20, 20, 8, 20], sizing=("fixed", "hug"))
    txt(d, lab, "Albums", "Kit/Button", "Color/on-surface-variant", "Label", fill=True)
    sh = B.shelf(col, "Albums shelf")
    for i, n in enumerate(BIGGEST_ALBUMS[:3]):
        t = B.tile(sh, "Album", n, ALBUM_INFO[n][0], [ALBUM_INFO[n][4]], width=120)
        if i == 0:
            trig(ctx, s.key, "album", t)
    s.notes.append(browse_note)
    s.mock.append("The mock shows the biggest albums as a carousel of bare covers. The code draws a shelf of 120dp tiles "
                  "with the title and artist under each, under a small \"Albums\" label.")
    s.finish()

    def docked(s, query, *, caret=False):
        fw = d.frame(s.f, name="Field row", layout="h", w=W, x=0, y=48, pad=[0, 16, 0, 16], sizing=("fixed", "hug"))
        fld = inst(d, "Search field", fw, name="Search field", State="Docked")
        d.set(fld, sizingH="fill")
        props(d, ctx, fld, "Search field", Text=query)
        trig(ctx, s.key, "search_browse", d.child(fld, "Back"))
        if caret:
            q = d.child(fld, "Query")
            c = d.rect(s.f, name="Caret", w=2, h=22, x=16 + 56 + 1, y=48 + 17, fill="Color/primary")
        return fld

    def results(s, col, q):
        res = search(q)
        rows = []
        for ar in res["artists"]:
            rows.append(dict(kind="coll", type="Artist", title=ar, sub="Artist", covers=[ARTIST_COVER[ar]]))
        for al in res["albums"]:
            rows.append(dict(kind="coll", type="Album", title=al, sub=f"Album, {ALBUM_INFO[al][0]}", covers=[ALBUM_INFO[al][4]]))
        for f in res["folders"]:
            rows.append(dict(kind="coll", type="Folder", title=f, sub="Playlist", covers=mosaic(FOLDER_TRACKS[f])))
        _, cells = B.group(col, rows, name="Open group")
        for (c, row), r in zip(cells, rows):
            if r["type"] == "Artist" and r["title"] == "Kavya Raman":
                trig(ctx, s.key, "artist", c)
            if r["type"] == "Album":
                trig(ctx, s.key, "album", c)
        B.list_label(col, "Songs")
        B.group(col, [dict(title=t.title) for t in res["tracks"][:7]], name="Songs group")

    s = Screen(B, a, "search_typing", "Search / Typing", src, tab="Search")
    col = vstack(d, s.f, "Results", x=0, y=104, pad=[12, 0, 0, 0])
    results(s, col, "Ka")
    docked(s, "Ka", caret=True)
    trig(ctx, "search_typing", "search_results", d.child(s.f, "Search field"))
    s.notes.append("The page resizes above the keyboard (imePadding): the navigation bar is hidden and the mini player "
                   "rides on the keyboard's top edge.")
    s.finish(nav=False, mini_y=H - KEYBOARD_H - 64 - 8, keyboard=True)

    s = Screen(B, a, "search_results", "Search / Results", src, tab="Search")
    col = vstack(d, s.f, "Results", x=0, y=104, pad=[12, 0, 0, 0])
    results(s, col, "ka")
    docked(s, "ka")
    s.finish()

    s = Screen(B, a, "search_no_results", "Search / No results", src, tab="Search")
    e = inst(d, "Empty state", s.f, name="Empty state", Art="Rounded", x=0, y=104)
    props(d, ctx, e, "Empty state", Title="No results for “lullaby”", Body="Check the spelling, or try fewer words.")
    props(d, ctx, e, "Empty state", Icon=d.comp["Icon/search_off"])
    icon = d.child(e, "Icon")
    if d.get(icon).get("width") != 48:
        d.set(icon, width=48, height=48)
    d.set(d.child(icon, "Glyph"), fill=V("Color/on-secondary-container"))
    docked(s, "lullaby")
    s.finish()
    a.close()


# ───────────────────────────────────────────────────────────────── Library
def lib_rows(filt):
    favs = dict(kind="coll", type="Favourites", title="Favourites", sub="Playlist, 42 songs", covers=())
    pls = [dict(kind="coll", type="Playlist", title=n, sub=f"Playlist, {c} songs", covers=mosaic(t)) for n, c, t in PLAYLISTS]
    fls = [dict(kind="coll", type="Folder", title=f, sub=f"Playlist, {len(FOLDER_TRACKS[f])} songs",
                covers=mosaic(FOLDER_TRACKS[f])) for f in FOLDERS]
    ars = [dict(kind="coll", type="Artist", title=n, sub="Artist", covers=[ARTIST_COVER[n]]) for n in ARTISTS]
    als = [dict(kind="coll", type="Album", title=a[1], sub=f"Album, {a[0]}", covers=[a[4]]) for a in ALBUMS]
    if filt is None:
        return [(None, [favs] + pls + fls), ("Artists", ars[:4]), ("Albums", als)]
    return [(None, {"Playlists": [favs] + pls + fls, "Albums": als, "Artists": ars}[filt])]


def build_library(B: Builder):
    d, ctx = B.d, B.ctx
    a = d.area("Library", note="Library: no filter as a list and as a grid, then each chip. Folders are listed with the "
                               "playlists; there is no Folders chip.")
    src = SRC + "ExpressiveLibrary.kt · ExpressiveLibrary"
    chip_keys = {"Playlists": "library_playlists", "Albums": "library_albums", "Artists": "library_artists",
                 "Songs": "library_songs"}

    def chrome(s, filt, grid=False):
        bar = inst(d, "Top app bar/Root", s.f, name="Top app bar", Kind="Plain", Scrolled="False", x=0, y=0)
        props(d, ctx, bar, "Top app bar/Root", Title="Library")
        trig(ctx, s.key, "settings", d.child(bar, "Settings"))
        trig(ctx, s.key, "search_typing", d.child(bar, "Search"))
        tg = d.child(bar, "Grid toggle")
        if grid:
            props(d, ctx, tg, "Toggle button", Checked="True", Size="Default", Off_style="Standard")
            swap_icon(d, tg, "grid_view", filled=True, color="Color/on-primary")
            trig(ctx, s.key, "library_list", tg)
        elif filt == "Songs":
            d.set(d.child(tg, "Icon"), opacity=0.38)
        else:
            trig(ctx, s.key, "library_grid", tg)
        box = d.frame(None, name="Chips", layout="v", w=W, pad=[8, 0, 20, 0], sizing=("fixed", "hug"))
        cr = inst(d, "Chip row", box, name="Chip row", Selected=filt or "None")
        for lab, k in chip_keys.items():
            if k != s.key:
                trig(ctx, s.key, k, d.child(cr, f"Chip {lab}"))
            elif filt:
                trig(ctx, s.key, "library_list", d.child(cr, f"Chip {lab}"))
        return box

    def fab(s):
        f_ = inst(d, "Extended FAB", s.f, name="New playlist", Expanded="True")
        props(d, ctx, f_, "Extended FAB", Label="New playlist")
        w = d.get(f_)["width"]
        d.set(f_, x=W - 16 - w, y=MINI_Y - 12 - 56)

    dests = {"Favourites": "favourites", "Playlist": "playlist", "Folder": "folder", "Artist": "artist", "Album": "album"}

    for key, name, filt in [("library_list", "Library / List", None), ("library_playlists", "Library / Playlists", "Playlists"),
                            ("library_albums", "Library / Albums", "Albums"), ("library_artists", "Library / Artists", "Artists")]:
        s = Screen(B, a, key, name, src, tab="Library")
        col = vstack(d, s.f, "Content", x=0, y=112)
        box = chrome(s, filt)
        d.cmd("node.reparent", ids=[box], parent=col, index=0)
        done = set()
        for i, (label, rows) in enumerate(lib_rows(filt)):
            if label:
                B.list_label(col, label)
            elif i:
                d.frame(col, name="Group spacing", w=W, h=12)
            _, cells = B.group(col, rows[:9], name=f"{label or 'Playlists'} group")
            for (c, _r), r in zip(cells, rows):
                dst = dests[r["type"]]
                if dst not in done and (dst != "playlist" or r["title"] == "Long drive") and (dst != "folder" or r["title"] == "Tamil"):
                    trig(ctx, key, dst, c)
                    done.add(dst)
        if filt in (None, "Playlists"):
            s.mock.append("The mock's rows read \"31 songs\" for Favourites, \"Folder, 24 songs\" for a folder and "
                          "\"2 songs\" for an artist. The code keeps today's wording: \"Playlist, 42 songs\" for "
                          "Favourites, playlists and folders alike, \"Artist\", and \"Album, {artist}\".")
            fab(s)
            trig(ctx, key, "dialog_name_playlist", d.child(s.f, "New playlist"))
        s.finish()

    # Grid
    s = Screen(B, a, "library_grid", "Library / Grid", src, tab="Library")
    col = vstack(d, s.f, "Content", x=0, y=112)
    box = chrome(s, None, grid=True)
    d.cmd("node.reparent", ids=[box], parent=col, index=0)
    grid = d.frame(col, name="Grid", layout="v", w=W, gap=16, pad=[0, 16, 0, 16], sizing=("fixed", "hug"))
    tw = round((W - 32 - 24) / 3, 2)
    for label, rows in lib_rows(None)[:2]:
        if label:
            lb = d.frame(grid, name=f"Label · {label}", layout="h", pad=[4, 0, 0, 4], sizing=("fill", "hug"))
            txt(d, lb, label, "Kit/Button", "Color/on-surface-variant", "Label", fill=True)
            rows = rows[:3]
        for r0 in range(0, len(rows), 3):
            row = d.frame(grid, name=f"Grid row", layout="h", gap=12, sizing=("fill", "hug"))
            for r in rows[r0:r0 + 3]:
                typ = {"Favourites": "Folder", "Playlist": "Folder", "Folder": "Folder", "Artist": "Artist", "Album": "Album"}[r["type"]]
                t = B.tile(row, typ, r["title"], r["sub"] if typ != "Artist" else None, list(r["covers"]), width=tw)
                if r["type"] == "Favourites":
                    cover_variant(d, ctx, d.child(t, "Art"), "Favourites", "148")
                    resize_cover(d, d.child(t, "Art"), tw)
                    trig(ctx, s.key, "favourites", t)
    fab(s)
    s.finish()

    # Songs
    s = Screen(B, a, "library_songs", "Library / Songs", src, tab="Library")
    col = vstack(d, s.f, "Content", x=0, y=112)
    box = chrome(s, "Songs")
    d.cmd("node.reparent", ids=[box], parent=col, index=0)
    sb = d.frame(col, name="Shuffle all row", layout="h", w=W, pad=[4, 16, 0, 16], sizing=("fixed", "hug"))
    btn = inst(d, "Button", sb, name="Shuffle all songs", Style="Filled", Size="Medium", Icon="Leading", State="Enabled",
               Squared="False")
    props(d, ctx, btn, "Button", Label="Shuffle all songs")
    swap_icon(d, btn, "shuffle", filled=True, layer="Leading icon")
    d.set(btn, sizingH="fill")
    trig(ctx, s.key, "now_playing", btn)
    sr = d.frame(col, name="Sort row", layout="h", w=W, pad=[12, 16, 8, 32], cross="center", sizing=("fixed", "hug"))
    txt(d, sr, songs(len(ALL_TRACKS)), "Kit/Button", "Color/on-surface-variant", "Count", fill=True)
    sp = d.instance("Split button", sr, name="Sort")
    props(d, ctx, sp, "Split button", Label="Title")
    B.group(col, [dict(title=t.title) for t in ALL_TRACKS[:8]], name="Songs group")
    s.notes.append("Songs shows every song: Shuffle all songs, the count with the sort as a split button (the label cycles "
                   "Title, Artist, Recently added; the arrow opens the menu), then the rows. The grid toggle is disabled "
                   "here.")
    s.finish()
    a.close()


# ───────────────────────────────────────────────────────────────── Collections
def build_collections(B: Builder):
    d, ctx = B.d, B.ctx
    a = d.area("Collections", note="Album, artist, playlist, folder and Favourites share one frame: a header on the "
                                   "cover's scheme, the list on the app scheme, and a bar that takes the header's "
                                   "container, the title and a play control once the title has scrolled under it.")
    cover_note = ("The header and bar run FromArt with the first song's cover as the seed ({what}), so both are on "
                  "Color mode {what}.")

    def header(parent, kind, *, title, meta, meta2=None, artist=None, cover=None, playing=False, cover_mode=True, seed=0):
        h = inst(d, "Collection header", parent, name="Collection header", Kind=kind)
        vals = dict(Title=title, Meta=meta)
        if meta2:
            vals["Second meta"] = meta2
        if artist:
            vals["Artist"] = artist
        props(d, ctx, h, "Collection header", **vals)
        if cover_mode:
            album_mode(d, h, seed)
        elif kind != "Favourites":
            # No seed (a playlist with no songs): the app scheme throughout, the play button too.
            d.set_mode(d.child(h, "Play"), "Color", "Wallpaper")
        cv = d.child(h, "Cover")
        if isinstance(cover, list) and len(cover) >= 4:
            set_cover(d, cv, *cover[:4])
        elif cover is not None:
            if kind in ("Playlist", "Folder"):
                cover_variant(d, ctx, cv, "Album", "208")
            set_cover(d, cv, cover[0] if isinstance(cover, list) else cover)
        if kind == "Album":
            set_cover(d, d.child(h, "Artist picture"), 0)
        if playing:
            props(d, ctx, d.child(h, "Play"), "Collection play button", Playing="True")
        return h

    def cbar(s, title, *, collapsed=False, options=False, playing=False, cover_mode=True, seed=0):
        b = inst(d, "Top app bar/Collection", s.f, name="Collection bar", Collapsed=str(collapsed), x=0, y=0)
        props(d, ctx, b, "Top app bar/Collection", Title=title)
        if not options:
            d.set(d.child(b, "Options"), visible=False)
        if collapsed and playing:
            props(d, ctx, d.child(b, "Play"), "Play button", Size="Small", Playing="True", Ring="False", Tonal="False")
        if not cover_mode:
            d.set_mode(b, "Color", "Wallpaper")
        else:
            album_mode(d, b, seed)
        trig(ctx, s.key, "back", d.child(b, "Back"))
        return b

    src = SRC + "ExpressiveCollections.kt · "
    album = ALBUM_TRACKS["Paper Lanterns"]

    def album_body(col):
        d.frame(col, name="Gap", w=W, h=12)
        rows = [dict(title=t.title, lead="Number", number=t.number) for t in album]
        B.group(col, rows, name="Tracks group")
        ft = d.frame(col, name="Footer", layout="h", w=W, pad=[14, 32, 0, 32], sizing=("fixed", "hug"))
        txt(d, ft, songs_and_duration(album), "Label/Large", "Color/on-surface-variant", "Summary", fill=True)

    for key, name, collapsed in [("album", "Collections / Album", False), ("album_collapsed", "Collections / Album, collapsed", True)]:
        s = Screen(B, a, key, name, src + "ExpressiveAlbum", tab="Home")
        col = vstack(d, s.f, "List", x=0, y=0 if not collapsed else -COLLAPSED_SCROLL)
        h = header(col, "Album", title="Paper Lanterns", artist="Mira Okafor", meta="Album • 2025",
                   meta2=songs_and_duration(album), cover=0, playing=True)
        trig(ctx, key, "artist", d.child(h, "Byline"))
        album_body(col)
        cbar(s, "Paper Lanterns", collapsed=collapsed, playing=True)
        s.notes.append("Header and bar run FromArt with Paper Lanterns' seed: Color mode Cover.")
        s.mock.append("The mock sets the album's meta on one line (\"Album, 2026. 6 songs, 26 min\"). The code puts the "
                      "artist's cookie and name in primary under the title (it opens the artist), then \"Album • 2025\" "
                      "and \"8 songs, 30 min\" on lines of their own.")
        if collapsed:
            s.notes.append("Scrolled until the cover's foot is under the bar (the render's scroll leaves the list about "
                           f"{COLLAPSED_SCROLL:.0f}dp down): the title has gone under the bar, which takes the cover scheme's "
                           "surface container, the title and the small play control, squared off because this album "
                           "is playing.")
        s.finish()

    # Artist
    kav = ranked("Kavya Raman")
    s = Screen(B, a, "artist", "Collections / Artist", src + "ExpressiveArtist", tab="Home")
    col = vstack(d, s.f, "List", x=0, y=0)
    header(col, "Artist", title="Kavya Raman", meta=songs_and_duration(ARTIST_TRACKS["Kavya Raman"]), cover=None,
           seed=ARTIST_SEED["Kavya Raman"])
    set_cover(d, d.child(d.child(col, "Collection header"), "Cover"), 8)
    B.section(col, "Top songs")
    _, cells = B.group(col, [dict(title=t.title, lead="Ranked cover", number=i + 1) for i, t in enumerate(kav[:5])],
                       name="Top songs group")
    cbar(s, "Kavya Raman", seed=ARTIST_SEED["Kavya Raman"])
    s.notes.append(cover_note.format(what="Iravu Rail"))
    s.finish()

    # Playlist
    for key, name, reorder in [("playlist", "Collections / Playlist", False), ("playlist_reorder", "Collections / Playlist, reorder", True)]:
        s = Screen(B, a, key, name, src + "ExpressivePlaylist", tab="Library")
        if not reorder:
            col = vstack(d, s.f, "List", x=0, y=0)
            header(col, "Playlist", title="Long drive", meta=songs_and_duration(LONG_DRIVE), cover=mosaic(LONG_DRIVE),
                   seed=PLAYLIST_SEED["Long drive"])
            tools = d.frame(col, name="Tools", layout="h", w=W, gap=8, pad=[12, 16, 12, 16], sizing=("fixed", "hug"))
            b1 = inst(d, "Button", tools, name="Add songs", Style="Tonal", Size="Small", Icon="Leading", State="Enabled",
                      Squared="False")
            props(d, ctx, b1, "Button", Label="Add songs")
            swap_icon(d, b1, "add", layer="Leading icon")
            b2 = inst(d, "Button", tools, name="Reorder songs", Style="Outlined", Size="Small", Icon="Leading",
                      State="Enabled", Squared="False")
            props(d, ctx, b2, "Button", Label="Reorder songs")
            swap_icon(d, b2, "swap_vert", layer="Leading icon")
            trig(ctx, key, "playlist_reorder", b2)
            trig(ctx, key, "sheet_add_songs", b1)
            B.group(col, [dict(title=t.title) for t in LONG_DRIVE[:6]], name="Songs group")
            b = cbar(s, "Long drive", options=True, seed=PLAYLIST_SEED["Long drive"])
            trig(ctx, key, "menu_playlist_overflow", d.child(b, "Options"))
        else:
            bar = d.frame(s.f, name="Reorder bar", layout="v", w=W, x=0, y=0, fill="Color/surface-container", sizing=("fixed", "hug"))
            d.frame(bar, name="Status bar inset", w=W, h=40)
            row = d.frame(bar, name="Bar", layout="h", w=W, h=64, gap=4, pad=[0, 12, 0, 4], cross="center")
            x = inst(d, "Icon button", row, name="Close", Style="Standard", Size="Default", Width="Default", State="Enabled")
            swap_icon(d, x, "close")
            tb = d.frame(row, name="Title box", layout="h", pad=[0, 4, 0, 4], sizing=("fill", "hug"))
            txt(d, tb, "Reorder songs", "Kit/Bar title", "Color/on-surface", "Title", fill=True, lines=1)
            ok = inst(d, "Icon button", row, name="Done reordering", Style="Filled", Size="Default", Width="Wide",
                      State="Enabled")
            swap_icon(d, ok, "check")
            album_mode(d, bar, PLAYLIST_SEED["Long drive"])
            trig(ctx, key, "playlist", x)
            trig(ctx, key, "back", ok)
            col = vstack(d, s.f, "List", x=0, y=104)
            d.cmd("node.reparent", ids=[col], parent=s.f, index=0)
            B.list_label(col, f"Long drive ({songs(len(LONG_DRIVE))})")
            B.group(col, [dict(title=t.title, trailing="drag") for t in LONG_DRIVE[:10]], name="Songs group")
            s.mock.append("The mock labels the list \"Late drive, 12 songs\" and finishes with a labelled Done. The code "
                          "labels it \"Long drive (12 songs)\" (playlist_picker_row) and finishes with a filled wide "
                          "icon button holding a tick, described as \"Done reordering\".")
            s.notes.append("Reorder mode: a contextual bar on the cover scheme's surface container replaces the header, "
                           "and each row's menu button becomes a drag handle. A row being dragged is the Lifted song row.")
        s.notes.append(cover_note.format(what="Afterglow Protocol"))
        s.finish()

    # Folder
    tamil = FOLDER_TRACKS["Tamil"]
    s = Screen(B, a, "folder", "Collections / Folder", src + "ExpressiveFolder", tab="Library")
    col = vstack(d, s.f, "List", x=0, y=0)
    header(col, "Folder", title="Tamil", meta="Playlist from a folder on this phone", meta2=songs_and_duration(tamil),
           cover=mosaic(tamil)[:1], seed=FOLDER_SEED["Tamil"])
    d.frame(col, name="Gap", w=W, h=12)
    B.group(col, [dict(title=t.title) for t in tamil[:6]], name="Songs group")
    cbar(s, "Tamil", seed=FOLDER_SEED["Tamil"])
    s.notes.append(cover_note.format(what="Iravu Rail"))
    s.mock.append("The mock's folder line reads \"Playlist from your Music/Offline folder\"; the code says \"Playlist from "
                  "a folder on this phone\" and adds the count and length on a second line.")
    s.finish()

    # Favourites
    s = Screen(B, a, "favourites", "Collections / Favourites", src + "ExpressiveFavourites", tab="Library")
    col = vstack(d, s.f, "List", x=0, y=0)
    header(col, "Favourites", title="Favourites", meta=songs_and_duration(LIKED), cover_mode=False)
    d.frame(col, name="Gap", w=W, h=12)
    B.group(col, [dict(title=t.title) for t in LIKED[:6]], name="Songs group")
    cbar(s, "Favourites", cover_mode=False)
    s.notes.append("Favourites has no cover, so the header and bar stay on the app scheme (Color mode Wallpaper) with the "
                   "heart on tertiary container.")
    s.finish()

    # Missing: ExpressiveAlbum, ExpressiveArtist and ExpressivePlaylist each return GoneState when the item has gone.
    for key, name, kind, tab, composable, glyph in (
            ("item_missing", "Collections / Album gone", "Album", "Home", "ExpressiveAlbum", None),
            ("item_missing_artist", "Collections / Artist gone", "Artist", "Home", "ExpressiveArtist", "Person"),
            ("item_missing_playlist", "Collections / Playlist gone", "Playlist", "Library", "ExpressivePlaylist",
             "QueueMusic")):
        s = Screen(B, a, key, name, src + "GoneState", tab=tab)
        bar = inst(d, "Top app bar/Detail", s.f, name="Top app bar", Surface="Plain", x=0, y=0)
        props(d, ctx, bar, "Top app bar/Detail", Title="", Show_action=False)
        trig(ctx, s.key, "back", d.child(bar, "Back"))
        g = inst(d, "Gone state", s.f, name="Gone state", Kind=kind, x=0, y=104)
        # The set's one Title property keeps the Album variant's text across a variant switch: set it every time.
        props(d, ctx, g, "Gone state", Title=f"This {kind.lower()} is gone")
        trig(ctx, s.key, "back", d.child(g, "Back"))
        if glyph:
            s.notes.append(f"No reference render: drawn from the code. {composable} returns GoneState when its "
                           f"{kind.lower()} has gone from the phone, the same frame as Album gone with "
                           f"collection_{kind.lower()}_gone and the {glyph} symbol"
                           + (" in the artist's cookie." if kind == "Artist" else
                              "; a folder playlist that has gone shows the same frame."))
        s.finish()

    # Empty collections (no reference render: the demo library has none)
    s = Screen(B, a, "favourites_empty", "Collections / Favourites, empty", src + "ExpressiveFavourites", tab="Library")
    col = vstack(d, s.f, "List", x=0, y=0)
    h = header(col, "Favourites", title="Favourites", meta=songs_and_duration([]), cover_mode=False)
    d.set(d.child(h, "Play cluster"), opacity=0.38)
    e = inst(d, "Empty state", col, name="Empty state", Art="None")
    props(d, ctx, e, "Empty state", Title="Songs you like will appear here", Body="Tap the heart on a song to save it.")
    cbar(s, "Favourites", cover_mode=False)
    s.notes.append("No reference render: the demo library always has favourites. With none, the cluster is disabled "
                   "(38 %) and the empty state follows the header.")
    s.finish()

    s = Screen(B, a, "playlist_empty", "Collections / Playlist, empty", src + "ExpressivePlaylist", tab="Library")
    col = vstack(d, s.f, "List", x=0, y=0)
    h = header(col, "Playlist", title="Road trip", meta=songs_and_duration([]), cover=None, cover_mode=False)
    cover_variant(d, ctx, d.child(h, "Cover"), "Album", "208")
    d.set(d.child(d.child(h, "Cover"), "Picture"), visible=False)
    d.set(d.child(h, "Play cluster"), opacity=0.38)
    tools = d.frame(col, name="Tools", layout="h", w=W, gap=8, pad=[12, 16, 12, 16], sizing=("fixed", "hug"))
    b1 = inst(d, "Button", tools, name="Add songs", Style="Filled", Size="Small", Icon="Leading", State="Enabled", Squared="False")
    props(d, ctx, b1, "Button", Label="Add songs")
    swap_icon(d, b1, "add", layer="Leading icon")
    trig(ctx, s.key, "sheet_add_songs", b1)
    b2 = inst(d, "Button", tools, name="Reorder songs", Style="Outlined", Size="Small", Icon="Leading", State="Disabled",
              Squared="False")
    props(d, ctx, b2, "Button", Label="Reorder songs")
    swap_icon(d, b2, "swap_vert", layer="Leading icon")
    e = inst(d, "Empty state", col, name="Empty state", Art="None")
    props(d, ctx, e, "Empty state", Title="Add songs to this playlist", Body="Tap the plus above to add songs from your library.")
    cbar(s, "Road trip", options=True, cover_mode=False)
    s.notes.append("No reference render. A new playlist (\"Road trip\", from Library's +) has no songs, so no cover and no "
                   "seed: the header stays on the app scheme, the cover shows the no-art note, Add songs is the filled "
                   "button and Reorder songs is disabled.")
    s.finish()
    a.close()


def build(d: Doc, ctx: dict):
    build_components(d, ctx)
    B = Builder(d, ctx)
    build_home(B)
    build_search(B)
    build_library(B)
    build_collections(B)
