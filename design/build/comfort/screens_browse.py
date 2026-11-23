"""Screens: Home, Search, Library, album, artist, playlist, folder, Favourites.

Owner: browse screens agent. Builds four areas on the Screens page (Home, Search, Library, Collections), each a
Light row with its Dark copy under it, from the reference render keys in REFS. Every screen stays on Color mode
Base: the code tints nothing in these screens (Search's Browse cards get no seed, so none is washed), apart from the
mini player in the dock, which tints itself inside the Dock component.

COMPONENTS MADE HERE (each with a description naming its Compose source and Code Connect)
  Components / Lists and cards
    Label control        Kind=View grid|View list|Sort. props Sort (text). ComfortLibrary.kt ViewToggle, SortControl
                         (`.lctl`): the control at the right end of a section label, 48dp target, 14dp past the label's end.
    Shuffle all row      (single) a Collection row (Favourites kind, small) with the tonal shuffle box and "Shuffle all
                         songs": the first row of Library's Songs card. ComfortLibrary.kt songs()
    Playlist tools       Reorder=True|False. "Add songs" and "Reorder songs" as two halves of a playlist card's first row,
                         a short divider between, a hairline under. ComfortCollections.kt PlaylistTools, ToolHalf
  Components / Navigation
    Index letter         Current=False|True. props Letter. Controls.kt IndexScrubber (one letter)
    Index scrubber       (single) A to Z and # as 27 Index letter instances, 22dp wide. Controls.kt IndexScrubber
    Index pop            (single) props Letter. Controls.kt IndexPop
    Keyboard (system)    (single) the system keyboard as the render harness stands it in (300dp). Attic does not draw it.
Empty states use components_states' `Empty state` (swap the exposed Icon box's Icon). It has no action, so the gone
state adds its Back button under the instance (bottom padding overridden to 0, the button 24 below, 48 under it).

The keyboard's colours (Color/system/keyboard, Color/system/key) and the Component/Sort and Component/Playlist tool
text styles are made in foundations.py.

Prototype triggers go in ctx["triggers"][(src_key, dst_key)] = Light layer id; (src, "back") for back buttons.
"""
from __future__ import annotations

from figbuild.kit import Doc, V, SCREEN_W, SCREEN_H

from .components_core import COVER, cover_image, K, SetBuilder, Components, _props, PKG, SRC as CSRC
from .foundations import superellipse_path, page_header

KEYS = ["home", "home_full", "home_albums", "home_artists", "home_folders", "mini_player_paused", "mini_player_open_with",
        "search_browse", "search_typing", "search_results", "search_no_results",
        "library_list", "library_grid", "library_songs", "library_playlists", "library_albums", "library_artists",
        "album", "album_collapsed", "artist", "playlist", "playlist_reorder", "folder", "favourites", "item_missing"]
REFS: dict[str, str] = {k: f"comfort-{{theme}}-{k}" for k in KEYS}

UI = "ui/comfort/"
def _full_h() -> float:
    """home_full's height: the stitched reference's own (the page down to its last row, then the dock)."""
    try:
        from PIL import Image
        return round(Image.open("/tmp/attic-dev/design-refs/comfort-light-home_full.png").height / 2.625, 1)
    except Exception:
        return round(6795 / 2.625, 1)


FULL_H = _full_h()

# ======================================================================== the demo library (DemoLibrary.kt)
# (album, artist, year, folder, cover index, [(title, length)])
ALBUMS = [
    ("Paper Lanterns", "Mira Okafor", 2025, "English", 0, [
        ("Lights Over the Water", "3:52"), ("Porch Light", "3:21"), ("Moth Letters", "4:05"), ("Second Bus Home", "3:37"),
        ("Salt in the Radio", "2:58"), ("Paper Lanterns", "4:41"), ("Small Hours, Big Sky", "3:12"), ("Coming Back Slow", "5:03")]),
    ("Weather Report", "Mira Okafor", 2022, "English", 11, [
        ("Pressure Front", "3:44"), ("Umbrella Song", "3:05"), ("Clear by Morning", "4:12"), ("Fog on Garden Road", "3:58"),
        ("Heatwave Postcard", "3:26"), ("Low Tide Hymn", "4:49")]),
    ("Iravu Rail", "Kavya Raman", 2024, "Tamil", 8, [
        ("Iravu Rail", "4:22"), ("Platform Ettu", "3:48"), ("Nila Paadhai", "4:05"), ("Theneer Kadai", "3:16"),
        ("Kadaisi Vandi", "4:37"), ("Ooru Thirumbum", "5:02")]),
    ("Kolam", "Kavya Raman", 2021, "Tamil", 5, [
        ("Vaasal Kolam", "3:34"), ("Maarghazhi Kaalai", "4:18"), ("Thinnai Pechu", "3:51"), ("Neer Kudam", "3:09"),
        ("Saalai Ooram", "4:26")]),
    ("Chhat Pe Dhoop", "Ruhaan Sethi", 2023, "Hindi", 9, [
        ("Chhat Pe Dhoop", "3:47"), ("Patang", "3:22"), ("Chai Ka Doosra Cup", "4:01"), ("Purani Diary", "4:33"),
        ("Sheher Ki Shaam", "3:55"), ("Khidki Wala Ghar", "3:40"), ("Aadhi Neend", "4:14")]),
    ("Monsoon Tapes", "Ruhaan Sethi", 2020, "Hindi", 6, [
        ("Bheegi Sadak", "3:29"), ("Chhatri", "3:03"), ("Geeli Mitti", "4:20"), ("Bijli Gayi", "2:47"),
        ("Tapri Pe Milna", "3:58"), ("Baadal Ka Khat", "4:36")]),
    ("Glasshouse Summer", "Juniper Vale", 2023, "English", 3, [
        ("Glasshouse", "3:39"), ("Neon Peaches", "3:14"), ("Swimming Pool Static", "4:02"), ("Sunburn Choir", "3:31"),
        ("Late Bloom", "4:27")]),
    ("Afterglow Protocol", "Velvet Circuit", 2025, "Late Night", 10, [
        ("Afterglow Protocol", "3:18"), ("Night Drive Firmware", "2:54"), ("Chrome Lungs", "3:07"), ("404 Heartbeat", "2:41"),
        ("Midnight Checksum", "3:26"), ("Drift Mode", "2:59")]),
    ("Tea at 2 AM", "Small Hours Tape Club", 2024, "Late Night", 1, [
        ("Kettle On", "2:16"), ("Rain on the Skylight", "2:48"), ("Cat on the Keyboard", "2:05"),
        ("Library Closing Soon", "2:37"), ("Unsent Drafts", "3:02")]),
    ("Marea Lenta", "Paloma Ferrer", 2022, "Español", 2, [
        ("Marea Lenta", "3:53"), ("Azulejo", "3:24"), ("Verano en la Azotea", "4:08"), ("Calle de los Naranjos", "3:46"),
        ("Faro Apagado", "4:31")]),
    ("Room Tone", "Odile Marchetti", 2021, "English", 4, [
        ("Room Tone", "5:12"), ("Brushes", "4:38"), ("Third Floor Walk-Up", "6:04"), ("Slow Elevator", "4:57"),
        ("Velvet Rope", "5:26")]),
    ("Kadalkarai Sessions", "Tide & Tamarind", 2025, "Tamil", 7, [
        ("Kadalkarai", "4:09"), ("Tamarind Skies", "3:44"), ("Auto Rickshaw Waltz", "3:17"), ("Filter Coffee Funk", "3:36"),
        ("Marina at Six", "4:52")]),
]


class T:
    def __init__(self, title, length, album, artist, year, folder, cover):
        self.title, self.length, self.album, self.artist = title, length, album, artist
        self.year, self.folder, self.cover = year, folder, cover
        m, s = length.split(":")
        self.ms = (int(m) * 60 + int(s)) * 1000


TRACKS = [T(t, l, a, ar, y, f, c) for a, ar, y, f, c, songs in ALBUMS for t, l in songs]
BY_TITLE = sorted(TRACKS, key=lambda t: t.title.lower())
ALBUM = {a[0]: a for a in ALBUMS}
NOW = "Lights Over the Water"
FOLDERS = ["English", "Tamil", "Hindi", "Español", "Late Night"]


def track(title) -> T:
    return next(t for t in TRACKS if t.title == title)


def album_tracks(name):
    return [t for t in TRACKS if t.album == name]


def folder_tracks(name):
    return [t for t in TRACKS if t.folder == name]


def artists():
    """Artists in the library's order (albums grouped by artist), with their tracks."""
    out = {}
    for t in TRACKS:
        out.setdefault(t.artist, []).append(t)
    return out


def mosaic(tracks):
    """mosaicIds(): one cover per album, first come first; Mosaic draws four, or the first cover alone."""
    seen = []
    for t in tracks:
        if t.cover not in seen:
            seen.append(t.cover)
    return seen[:4] if len(seen) >= 4 else seen[:1]


def songs(n):
    return f"{n} song" if n == 1 else f"{n} songs"


def length(tracks):
    m = sum(t.ms for t in tracks) // 60000
    return f"{m // 60} hr {m % 60} min" if m >= 60 else f"{m} min"


def songs_and_length(tracks):
    return f"{songs(len(tracks))}, {length(tracks)}"


BIGGEST_ALBUMS = sorted(ALBUMS, key=lambda a: -len(a[5]))
BIGGEST_ARTISTS = sorted(artists().items(), key=lambda kv: -len(kv[1]))
# Counts are the songs each playlist holds in DesignStates.kt (Long drive's 12, the picker's 5 and 4), which the
# playlist, picker and Favourites renders show; the library render's 25, 32, 39 and 42 are DemoStates.library's
# placeholders (songCount = 18 + id x 7, favourites 42). LIBRARY_COUNTS says so on the Library frames.
PLAYLISTS = [("Long drive", ["Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak"], 12),
             ("Sunday cleaning", ["Glasshouse", "Patang", "Azulejo", "Filter Coffee Funk"], 5),
             ("Focus", ["Kettle On", "Room Tone", "Brushes", "Unsent Drafts"], 4)]
LIBRARY_COUNTS = ("The Library reference render shows Favourites 42 songs, Long drive 25, Sunday cleaning 32 and Focus 39: "
                  "DemoStates.library gives every playlist a placeholder count (18 + id x 7) and Favourites 42. The "
                  "Favourites, playlist and picker renders count the songs they list (12, 12, 5 and 4), so this file uses "
                  "those counts on every screen.")
LONG_DRIVE = ["Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak", "Kadaisi Vandi", "Drift Mode",
              "Sheher Ki Shaam", "Swimming Pool Static", "Faro Apagado", "Midnight Checksum", "Auto Rickshaw Waltz",
              "Second Bus Home"]
LIKED = ["Lights Over the Water", "Iravu Rail", "Chai Ka Doosra Cup", "Neon Peaches", "Marea Lenta", "Night Drive Firmware",
         "Kettle On", "Filter Coffee Funk", "Platform Ettu", "Patang", "Room Tone", "Bheegi Sadak"]
RECENT = ["Lights Over the Water", "Iravu Rail", "Chai Ka Doosra Cup", "Neon Peaches", "Marea Lenta",
          "Night Drive Firmware", "Kettle On", "Filter Coffee Funk"]
MIXES = ["Porch Light", "Platform Ettu", "Patang", "Glasshouse", "Azulejo", "Drift Mode"]
START = ["Moth Letters", "Kadalkarai", "Sheher Ki Shaam", "Rain on the Skylight"]
# DemoStates.artist ranks Kavya Raman's songs by hashCode; the render shows this order.
KAVYA_TOP = ["Iravu Rail", "Saalai Ooram", "Thinnai Pechu", "Neer Kudam", "Nila Paadhai"]


def search(q):
    q = q.lower()

    def matching(items, name):
        hits = [x for x in items if q in name(x).lower()]
        return sorted(hits, key=lambda x: 0 if name(x).lower().startswith(q) else 1)
    return {"tracks": matching(BY_TITLE, lambda t: t.title),
            "artists": matching(list(artists().items()), lambda kv: kv[0])[:5],
            "albums": matching(ALBUMS, lambda a: a[0])[:10],
            "folders": matching(FOLDERS, lambda f: f)}


# ======================================================================== the builder
class B:
    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.k = K(d, ctx["tokens"])
        self.trig = ctx.setdefault("triggers", {})
        self.key = None
        self.notes = {}

    # ---------------------------------------------------------------- basics
    def inst(self, comp, parent, name=None, props=None, **kw):
        d = self.d
        iid = d.instance(comp, parent, name=name)
        if props:
            d.cmd("instance.setProperties", ids=[iid], properties=props)
        if kw:
            d.set(iid, **kw)
        return iid

    def props(self, iid, **props):
        self.d.cmd("instance.setProperties", ids=[iid], properties=props)

    def nested(self, root, name, typ="INSTANCE"):
        d = self.d
        for i in d.find(root, name=name, limit=200):
            g = d.get(i)
            if g.get("name") == name and (typ is None or g.get("type") == typ):
                return i
        raise KeyError(f"no {typ} {name!r} under {root}")

    def swap_icon(self, iid, icon, prop="Icon"):
        self.d.cmd("instance.setProperties", ids=[iid], properties={prop: self.d.comp[icon]})

    def gap(self, parent, h, name=None):
        return self.d.frame(parent, name=name or f"Space {h}", w=SCREEN_W, h=h)

    def trigger(self, layer, dst, src=None):
        if (src or self.key, dst) in self.trig:
            self.more(layer, dst, src)
            return
        self.trig[(src or self.key, dst)] = layer

    # ---------------------------------------------------------------- art
    def art(self, cover, covers, size, kind="album"):
        """Make the cover instance `cover` show `covers` at `size`: kind album | artist | favourites.
        Four covers make a mosaic, one a cover (Artwork.kt Mosaic falls back to Cover under four)."""
        d = self.d
        cur = d.get(cover).get("mainComponentName", "")
        comp_of = {d.comp[n]: n for n in ("Cover/Album", "Cover/Artist", "Cover/Mosaic", "Icon box")}
        main = d.get(cover).get("mainComponent")
        cur_set = None
        for n in ("Cover/Album", "Cover/Artist", "Cover/Mosaic", "Icon box"):
            kids = [c["id"] for c in d.get(d.comp[n], depth=1).get("children", []) if c.get("type") == "COMPONENT"]
            if main in kids:
                cur_set = n
        if kind == "favourites":
            want, vprops = "Icon box", {"Style": "Tonal"}
        elif kind == "artist":
            want, vprops = "Cover/Artist", {"Art": "Image"}
        elif len(covers) >= 4:
            want, vprops = "Cover/Mosaic", {"Shape": "Squircle large" if size >= 100 else "Squircle"}
        else:
            want = "Cover/Album"
            vprops = {"Shape": "Squircle large" if size >= 100 else "Squircle", "Art": "Image" if covers else "None"}
        if cur_set != want:
            d.cmd("instance.swap", ids=[cover], component=d.comp[want])
        d.cmd("instance.setProperties", ids=[cover], properties=vprops)
        if kind == "favourites":
            self.swap_icon(cover, "Icon/favorite (filled)")
        d.set(cover, width=size, height=size)
        if kind == "favourites":
            return cover
        if want == "Cover/Mosaic":
            for i, c in enumerate(covers[:4]):
                cover_image(d, cover, COVER[c], f"Art {i + 1}")
        elif covers:
            cover_image(d, cover, COVER[covers[0]])
        return cover

    # ---------------------------------------------------------------- screens
    def phone(self, a, key, name, source, h=SCREEN_H, top=40):
        """A screen frame with a vertical `Content` column (the scrolling list) starting at `top`."""
        self.key = key
        fid = a.screen(key, name=name, source=source, h=h)
        content = self.d.frame(fid, name="Content", x=0, y=top, w=SCREEN_W, layout="v", cross="center",
                               sizing=("fixed", "hug"))
        return fid, content

    def chrome(self, fid, h=SCREEN_H, tab="Home", dock=True):
        d = self.d
        if dock:
            dk = self.inst("Dock", fid, name="Dock", props={"Tab": tab, "Mini player": "True"})
            dh = d.get(dk)["height"]
            d.set(dk, x=0, y=h - dh, constraints={"horizontal": "leftRight", "vertical": "bottom"})
            for t, dst in (("Home", "home"), ("Search", "search_browse"), ("Library", "library_list")):
                if t != tab:
                    self.trigger(self.nested(dk, f"Tab: {t}"), dst)
        sb = self.inst("Status bar", fid, name="Status bar")
        d.set(sb, x=0, y=0, constraints={"horizontal": "leftRight", "vertical": "top"})
        gb = self.inst("Gesture bar", fid, name="Gesture bar")
        d.set(gb, x=0, y=h - 24, constraints={"horizontal": "leftRight", "vertical": "bottom"})

    def finish(self, key, source, notes=()):
        d = self.d
        self.notes[key] = list(notes)
        fid = d.screens[key]["light"]
        d.annotate(fid, f"Compose: {UI}{source}", "Source")
        for cat, text in notes:
            d.annotate(fid, text, cat)
        d.ready_for_dev(fid)

    def finish_dark(self, keys):
        """The Dark copy is a duplicate of the annotated Light frame, so it already carries every annotation."""
        d = self.d
        for key in keys:
            d.ready_for_dev(d.screens[key]["dark"])

    def more(self, layer, dst, src=None):
        """A second trigger for the same (src, dst) pair: ctx["triggers_more"] (flows.py wires both)."""
        self.ctx.setdefault("triggers_more", []).append((src or self.key, dst, layer))

    # ---------------------------------------------------------------- parts
    def hero(self, parent, title, mark=False):
        return self.inst("Hero", parent, name="Hero", props={"Mark": str(mark), "Short": "False", "Title": title,
                                                            "Show subtitle": False})

    def label(self, parent, text, small=None, tight=False):
        p = {"Spacing": "Tight" if tight else "Default", "Label": text, "Show small": bool(small), "Show end": False}
        if small:
            p["Small"] = small
        return self.inst("Section label", parent, name=f"Label: {text}", props=p)

    def label_with(self, parent, text, kind, sort=None):
        """A section label with a Label control at its right end (ComfortLibrary.kt LabelControl): a 48dp target
        centred on the label's text line, pushed 14dp past the label's 28dp inset."""
        d = self.d
        box = d.frame(parent, name=f"Label row: {text}", layout="v", w=SCREEN_W, sizing=("fixed", "hug"))
        self.label(box, text)
        p = {"Kind": kind}
        if sort:
            p["Sort"] = sort
        c = self.inst("Label control", box, name="View toggle" if kind != "Sort" else "Sort", props=p)
        cw = d.get(c)["width"]
        d.set(c, absolute=True)
        d.set(c, x=SCREEN_W - 28 + 14 - cw, y=24 + 9 - 24, constraints={"horizontal": "right", "vertical": "top"})
        return box, c

    def card(self, parent, pad=False, width=None, name="Card"):
        d = self.d
        g = self.inst("Group card", parent, name=name, props={"Pad": str(pad)})
        if width:
            d.set(g, width=width)
        return g

    def fill_slot(self, card, nodes):
        """Replace a Group card's default content with `nodes` (layers already built elsewhere)."""
        d = self.d
        slot = self.nested(card, "Content", typ=None)
        r = d.cmd("instance.slotInsert", id=slot, component=d.comp["Note"], replace=True)
        for n in nodes:
            d.cmd("node.reparent", ids=[n], parent=slot)
            d.set(n, sizingH="fill")
        d.cmd("edit.delete", ids=[r["id"]])
        return slot

    def rows_card(self, parent, makers, width=None, pad=False, name="Card"):
        """A Group card holding rows; `makers` are callables(parent) -> row id, built in a holding frame first."""
        d = self.d
        hold = d.frame(parent, name="Rows", layout="v", w=width or 379, sizing=("fixed", "hug"))
        rows = [m(hold) for m in makers]
        g = self.card(parent, pad=pad, width=width, name=name)
        self.fill_slot(g, rows)
        d.cmd("edit.delete", ids=[hold])
        return g, rows

    def song_row(self, parent, t: T, i=0, lead="Cover", trailing="More", meta=False, current=None, number=None,
                 rank=None):
        cur = (t.title == NOW) if current is None else current
        p = {"Lead": lead, "Current": str(cur), "Title": t.title, "Subtitle": t.artist, "Show hairline": i > 0,
             "Show meta": meta, "Show trailing": True}
        if meta:
            p["Meta"] = t.length
        if lead == "Number" and not cur:
            p["Number"] = str(number if number is not None else i + 1)
        if lead == "Rank":
            p["Number"] = str(rank if rank is not None else i + 1)
        r = self.inst("Song row", parent, name=f"Row: {t.title}", props=p)
        tr = self.nested(r, "Trailing")
        self.props(tr, Kind=trailing, Tone="Current" if cur and trailing in ("More", "Drag handle", "Radio") else "Default")
        if lead in ("Cover", "Rank"):
            cover_image(self.d, self.nested(r, "Cover"), COVER[t.cover])
        return r

    def coll_row(self, parent, kind, title, subtitle=None, covers=(), i=0, size="Medium", chevron=False, overline=None):
        p = {"Kind": kind, "Size": size, "Title": title, "Show subtitle": subtitle is not None, "Show hairline": i > 0,
             "Show value": False, "Show chevron": chevron, "Show overline": overline is not None}
        if subtitle is not None:
            p["Subtitle"] = subtitle
        if overline:
            p["Overline"] = overline
        r = self.inst("Collection row", parent, name=f"Row: {title}", props=p)
        art = {"Small": 48, "Medium": 56, "Large": 64}[size]
        if kind != "Favourites":
            self.art(self.nested(r, "Lead"), list(covers), art, "artist" if kind == "Artist" else "album")
        return r

    def tile(self, parent, kind, title, subtitle=None, covers=(), art_kind="album", size=None, now=False, name=None,
             fill=False, lines=None):
        d = self.d
        p = {"Kind": kind, "Current": str(now), "Title": title}
        if subtitle is not None:
            p["Subtitle"] = subtitle
        t = self.inst("Tile", parent, name=name or f"Tile: {title}", props=p)
        if kind == "Shortcut":
            self.props(t, **{"Show icon box": art_kind == "favourites"})
        if fill:
            d.set(t, sizingH="fill")
        cv = self.nested(t, "Cover")
        if size:
            d.set(self.nested(t, "Art", typ="FRAME"), width=size, height=size)
        if not (kind == "Shortcut" and art_kind == "favourites"):
            cur = d.get(cv)["width"]
            self.art(cv, list(covers), size or cur, art_kind)
        if lines:
            for nm, n in lines.items():
                d.set(self.nested(t, nm, typ="TEXT"), maxLines=n)
        return t

    def shelf(self, parent, makers, pad=24, name="Shelf"):
        d = self.d
        s = d.frame(parent, name=name, layout="h", gap=14, pad=[0, pad, 0, pad], w=SCREEN_W, clip=True,
                    sizing=("fixed", "hug"), cross="min")
        for m in makers:
            m(s)
        return s

    def grid(self, parent, makers, cols=3, side=24, col_gap=14, row_gap=20, name="Grid", width=SCREEN_W):
        d = self.d
        g = d.frame(parent, name=name, layout="v", gap=row_gap, pad=[0, side, 0, side], w=width, sizing=("fixed", "hug"))
        for r in range(0, len(makers), cols):
            row = d.frame(g, name=f"Grid row {r // cols + 1}", layout="h", gap=col_gap, sizing=("fill", "hug"), cross="min")
            for m in makers[r:r + cols]:
                tid = m(row)
                d.set(tid, sizingH="fill")
            for _ in range(cols - len(makers[r:r + cols])):
                d.frame(row, name="Empty cell", h=1, sizing=("fill", "fixed"))
        return g


# ======================================================================== components
def _below(d: Doc, page: str) -> float:
    """The y under everything on a page, so new spec cards start below what's there."""
    pid = d.page(page)
    kids = d.get(pid, depth=1).get("children", [])
    return max((c.get("y", 0) + c.get("height", 0) for c in kids), default=0) + 200


def _Cards(d: Doc, page: str) -> Components:
    """Spec cards that continue a component page laid out by components_core, before its Dark preview."""
    return Components.extend(d, page)


def _components(d: Doc, ctx: dict):
    k = K(d, ctx["tokens"])
    made = {}

    # -------------------------------------------------------------- Lists and cards
    cc = _Cards(d, "Components / Lists and cards")
    sb = SetBuilder(cc, "Label control", "ComfortLibrary.kt · ViewToggle, SortControl", "ViewToggle",
                    "The control at the right end of a section label, acting on the list under it (`.lctl`): a 48dp target "
                    "centred on the label's text line, 14dp past the label's end, not making the label taller. View grid "
                    "and View list: the 22dp grid_view or view_list glyph in on (Library's first label). Sort: the order "
                    "(14/18 semibold) and a 22dp expand_more, 10dp start and 6 end; it opens the three orders in a menu.")
    for kind, lig in (("View grid", "grid_view"), ("View list", "view_list"), ("Sort", None)):
        if lig:
            c = sb.variant({"Kind": kind}, w=48, h=48, layout="h", main="center", cross="center")
            k.icon(c, lig, size=22, color="Color/on", name="Icon")
        else:
            c = sb.variant({"Kind": kind}, layout="h", h=48, pad=[0, 6, 0, 10], gap=2, cross="center", sizing=("hug", "fixed"))
            k.bind_radius(c, "pill")
            t = k.text(c, "Title", "Component/Sort", "Color/on", "Order")
            sb.text("Sort", t, "Title")
            k.icon(c, "expand_more", size=22, color="Color/on", name="Expand icon")
    sb.finish()

    c = cc.single("Shuffle all row", "ComfortLibrary.kt · songs", "CollectionRow", layout="v", w=379, sizing=("fixed", "hug"))
    r = d.instance("Collection row", c, name="Row")
    d.cmd("instance.setProperties", ids=[r], properties={"Kind": "Favourites", "Size": "Small", "Title": "Shuffle all songs",
                                                          "Show subtitle": False, "Show hairline": False})
    d.set(r, sizingH="fill")
    lead = None
    for i in d.find(r, name="Lead", limit=20):
        if d.get(i).get("type") == "INSTANCE":
            lead = i
    d.cmd("instance.setProperties", ids=[lead], properties={"Icon": d.comp["Icon/shuffle"]})
    cc.finish_single("Shuffle all row", c, "ComfortLibrary.kt · songs", "CollectionRow",
                     "The first row of Library's Songs card: CollectionRow(RowSize.Small) with IconBox(Symbol.Shuffle), the "
                     "tonal box, and \"Shuffle all songs\". Plays every song from a random one.")

    sb = SetBuilder(cc, "Playlist tools", "ComfortCollections.kt · PlaylistTools", "PlaylistTools",
                    "`.pl-tools`: the first row of a playlist's song card, 56dp: \"Add songs\" and \"Reorder songs\" as two "
                    "halves (a 22dp glyph in on-2, 8dp, the label 14.5/20 semibold in on), split by a 1dp fill-2 divider 18dp "
                    "from top and bottom, with a line hairline under them 20dp in. Reorder=False (fewer than two songs) draws "
                    "that half at 38 %.")
    for on in ("True", "False"):
        c = sb.variant({"Reorder": on}, layout="h", w=379, h=56, sizing=("fixed", "fixed"))
        for nm, lig, lab in (("Add songs", "add", "Add songs"), ("Reorder songs", "swap_vert", "Reorder songs")):
            half = d.frame(c, name=nm, layout="h", gap=8, main="center", cross="center", sizing=("fill", "fill"))
            k.icon(half, lig, size=22, color="Color/on-2", name="Icon")
            k.text(half, lab, "Component/Playlist tool", "Color/on", "Label", lines=1)
            if nm == "Reorder songs" and on == "False":
                d.set(half, opacity=0.38)
        dv = d.rect(c, name="Divider", w=1, h=20, fill="Color/fill-2")
        k.absolute(dv, 189, 18, 1, 20, {"horizontal": "center", "vertical": "topBottom"})
        hl = d.rect(c, name="Hairline", w=339, h=1, fill="Color/line")
        k.absolute(hl, 20, 55, 339, 1, {"horizontal": "leftRight", "vertical": "bottom"})
    sb.finish()
    cc.dark_preview()

    # -------------------------------------------------------------- Navigation
    cc = _Cards(d, "Components / Navigation")
    sb = SetBuilder(cc, "Index letter", "Controls.kt · IndexScrubber", "IndexScrubber",
                    "One letter of the index: 22 x 15.5, 10.5/15.5 semibold in on-3. Current: the letter in on-accent on an "
                    "accent pill (18dp wide, the row's 15.5 tall).")
    for cur in (False, True):
        c = sb.variant({"Current": str(cur)}, w=22, h=15.5)
        if cur:
            p = d.rect(c, name="Pill", w=18, h=15.5, x=2, y=0, fill="Color/accent")
            k.bind_radius(p, "pill")
        t = k.text(c, "A", "Component/Index letter", "Color/on-accent" if cur else "Color/on-3", "Letter", align="center", w=22)
        d.set(t, x=0, y=0, constraints={"horizontal": "leftRight", "vertical": "center"})
        sb.text("Letter", t, "A")
    sb.finish()

    c = cc.single("Index scrubber", "Controls.kt · IndexScrubber", "IndexScrubber", layout="v", w=22, sizing=("fixed", "hug"))
    letters = [chr(x) for x in range(ord("A"), ord("Z") + 1)] + ["#"]
    for i, L in enumerate(letters):
        li = d.instance("Index letter", c, name=f"Letter {L}")
        d.cmd("instance.setProperties", ids=[li], properties={"Current": str(i == 0), "Letter": L})
    cc.finish_single("Index scrubber", c, "Controls.kt · IndexScrubber", "IndexScrubber",
                     "Library's index, A to Z then #: shown only under Songs, sorted by title, with more than 30 songs, once "
                     "the title has collapsed. It sits 4dp from the right edge, 16dp under the pinned chips, and the list's "
                     "cards make 28dp of room for it (200 ms). Set Current on the letter in view; dragging shows Index pop "
                     "34dp to its left, level with the letter.")
    c = cc.single("Index pop", "Controls.kt · IndexPop", "IndexPop", w=56, h=56)
    k.sq(c, 4, 56, 56, "Color/accent", "Container")
    t = k.text(c, "K", "Component/Index pop", "Color/on-accent", "Letter", align="center", w=56)
    d.set(t, x=0, y=12, constraints={"horizontal": "leftRight", "vertical": "center"})
    cc.finish_single("Index pop", c, "Controls.kt · IndexPop", "IndexPop",
                     "The letter under the thumb while scrubbing: 26/32 bold on-accent in a 56dp accent superellipse.",
                     binds=[("Letter", "text", t, "K")])

    c = cc.single("Keyboard (system)", "storeShots designrefs DesignHarness.kt · Keyboard", "Keyboard", w=411, h=300,
                  fill="Color/system/keyboard")
    pad, top, row_h = 6, 44, 52
    for r, n in enumerate((10, 9, 9, 5)):
        y = top + r * (row_h + 4)
        inset = 411 / 20 if r == 1 else 0
        w = (411 - 2 * inset - pad * (n + 1)) / (10 if r == 3 else n)
        x = inset + pad
        for i in range(n):
            kw = w * 6 + pad * 5 if (r == 3 and i == 2) else w
            key = d.rect(c, name=f"Key {r + 1}.{i + 1}" if not (r == 3 and i == 2) else "Space bar", x=round(x, 2), y=y,
                         w=round(kw, 2), h=row_h - 8, fill="Color/system/key", radius=6)
            x += kw + pad
    cc.finish_single("Keyboard (system)", c, "storeShots designrefs DesignHarness.kt · Keyboard", "Keyboard",
                     "The phone's own keyboard, drawn by the system, not by Attic. This is the neutral stand-in the render "
                     "harness draws (300dp: suggestion strip, four rows of keys, gesture area) so screens can show the room "
                     "Attic's layout makes for it. Its colours are the harness's (Color/system/keyboard, Color/system/key).")
    harness = "app/src/storeShots/kotlin/app/attic/player/designrefs/DesignHarness.kt · Keyboard"
    d.set(c, description=f"Compose: {harness}. Drawn by the system on a phone; the render harness's stand-in.")
    cc.dark_preview()



# ======================================================================== Home
def _home_all(b: B, content, full=False):
    d = b.d
    # Shortcuts tray: 3 x 2 in a padded card (TileGrid tray: 14 down, 6 across, 80dp art)
    tray = d.frame(content, name="Tray grid", layout="v", gap=14, w=351, sizing=("fixed", "hug"))
    shortcuts = [("Favourites", None), ("Paper Lanterns", 0), ("Iravu Rail", 8), ("Chhat Pe Dhoop", 9),
                 ("Glasshouse Summer", 3), ("Afterglow Protocol", 10)]
    for r in range(2):
        row = d.frame(tray, name=f"Tray row {r + 1}", layout="h", gap=6, sizing=("fill", "hug"), cross="min")
        for title, cv in shortcuts[r * 3:r * 3 + 3]:
            t = b.tile(row, "Shortcut", title, covers=[cv] if cv is not None else [],
                       art_kind="favourites" if cv is None else "album", fill=True, name=f"Shortcut: {title}")
            if title == "Favourites":
                b.trigger(t, "favourites")
            elif title == "Paper Lanterns":
                b.trigger(t, "album")
    g = b.card(content, pad=True, name="Shortcuts")
    b.fill_slot(g, [tray])

    b.gap(content, 12)
    re = b.inst("Recap entry", content, name="Recap entry")

    b.label(content, "Instant mixes")
    pager = d.frame(content, name="Pager", layout="h", gap=10, pad=[0, 0, 0, 16], w=SCREEN_W, clip=True,
                    sizing=("fixed", "hug"), cross="min")
    for p in range(2):
        b.rows_card(pager, [lambda par, t=track(n), i=i: b.song_row(par, t, i, trailing="Radio", current=False)
                            for i, n in enumerate(MIXES[p * 3:p * 3 + 3])], width=312, name=f"Page {p + 1}")

    b.label(content, "Recently played")
    b.shelf(content, [lambda par, t=track(n): b.tile(par, "Recent", t.title, t.artist, [t.cover], now=t.title == NOW)
                      for n in RECENT], name="Recently played shelf")

    b.label(content, "Start listening", small="Picked at random from your songs")
    b.rows_card(content, [lambda par, t=track(n), i=i: b.song_row(par, t, i) for i, n in enumerate(START)],
                name="Start listening")

    b.label(content, "Your folders")
    clipf = d.frame(content, name="Folder tray", w=351, h=10, clip=True)
    row = d.frame(clipf, name="Folder row", layout="h", gap=10, x=0, y=0, sizing=("hug", "hug"), cross="min")
    w = (351 - 30) / 4
    for f in FOLDERS:
        ft = folder_tracks(f)
        t = b.tile(row, "Folder", f, songs(len(ft)), mosaic(ft), name=f"Folder: {f}")
        d.set(t, width=w)
        if f == "Tamil":
            b.trigger(t, "folder")
    d.set(clipf, height=d.get(row)["height"])
    g = b.card(content, pad=True, name="Your folders")
    b.fill_slot(g, [clipf])

    b.label(content, "Albums")
    b.shelf(content, [lambda par, a=a: b.tile(par, "Album", a[0], a[1], [a[4]], name=f"Album: {a[0]}") for a in BIGGEST_ALBUMS],
            name="Albums shelf")
    b.gap(content, 28)
    # Spotlight: "More from" Mira Okafor, a row and a shelf inside one card
    hold = d.frame(content, name="Rows", layout="v", w=379, sizing=("fixed", "hug"))
    r1 = b.coll_row(hold, "Artist", "Mira Okafor", None, [0], size="Small", chevron=True, overline="More from")
    inset = d.frame(hold, name="Inset", layout="v", pad=[14, 0, 16, 0], sizing=("fill", "hug"))
    hl = d.rect(inset, name="Hairline", w=339, h=1, fill="Color/line")
    b.k.absolute(hl, 20, 0, 339, 1, {"horizontal": "leftRight", "vertical": "top"})
    sh = d.frame(inset, name="Shelf", layout="h", gap=14, pad=[0, 16, 0, 16], sizing=("fill", "hug"), clip=True, cross="min")
    for a in ("Paper Lanterns", "Weather Report"):
        al = ALBUM[a]
        b.tile(sh, "More from", a, str(al[2]), [al[4]], name=f"Album: {a}")
    g = b.card(content, name="More from")
    b.fill_slot(g, [r1, inset])
    d.cmd("edit.delete", ids=[hold])

    b.label(content, "Top artists")
    ta = b.shelf(content, [lambda par, n=n, ts=ts: b.tile(par, "Artist", n, covers=[ts[0].cover], art_kind="artist",
                                                          name=f"Artist: {n}")
                           for n, ts in BIGGEST_ARTISTS], name="Top artists shelf")
    b.trigger(b.nested(ta, "Artist: Kavya Raman"), "artist")
    b.gap(content, 160, name="Space for the dock")


def _home_tools(b: B, content, sel):
    tl = b.inst("Tools", content, name="Tools", props={"Leading": "Sub-tabs"})
    for lab in ("All", "Albums", "Artists", "Folders"):
        st = b.nested(tl, f"Sub-tab: {lab}")
        b.props(st, Selected=str(lab == sel))
        if lab != sel:
            b.trigger(st, {"All": "home", "Albums": "home_albums", "Artists": "home_artists", "Folders": "home_folders"}[lab])
    return tl


def _home(d: Doc, b: B):
    a = d.area("Home", note="Home, ui/comfort/ComfortHome.kt. All: shortcuts tray, Recap entry, Instant mixes, Recently "
                            "played, Start listening, Your folders, Albums, More from, Top artists. A sub-tab leaves its one "
                            "section as a three-column grid. Every screen is Color mode Base; the mini player tints itself.")
    src = "ComfortHome.kt · ComfortHome"
    for key, name, h in (("home", "Home / All", SCREEN_H), ("home_full", "Home / All (full length)", FULL_H)):
        fid, content = b.phone(a, key, name, src, h=h)
        b.hero(content, "Home", mark=True)
        _home_tools(b, content, "All")
        _home_all(b, content, full=key == "home_full")
        b.chrome(fid, h=h, tab="Home")
        b.finish(key, src)
    # From 24 December the Recap entry names the year's Recap (RecapEntry: teaser.yearEnded, home_recap_title_year).
    fid, content = b.phone(a, "home_recap_year", "Home / All, Recap entry from 24 December", src)
    b.hero(content, "Home", mark=True)
    _home_tools(b, content, "All")
    _home_all(b, content)
    b.props(b.nested(content, "Recap entry"), Title="Your 2026 Recap")
    b.chrome(fid, tab="Home")
    b.finish("home_recap_year", src, notes=[(
        "Note", "Drawn from the code; there is no reference render. From 24 December to the end of the year the Recap "
                "entry's headline names the year's Recap, \"Your 2026 Recap\" (home_recap_title_year; HomeModel sets "
                "yearEnded from the date). The rest of the year it reads \"Your 2026 so far\". The mosaic and the "
                "minutes line don't change.")])
    for key, sel in (("home_albums", "Albums"), ("home_artists", "Artists"), ("home_folders", "Folders")):
        fid, content = b.phone(a, key, f"Home / {sel}", src)
        b.hero(content, "Home", mark=True)
        _home_tools(b, content, sel)
        b.gap(content, 8)
        if sel == "Albums":
            makers = [lambda par, a=al: b.tile(par, "Grid", a[0], a[1], [a[4]], name=f"Album: {a[0]}") for al in BIGGEST_ALBUMS]
        elif sel == "Artists":
            makers = [lambda par, n=n, ts=ts: b.tile(par, "Artist", n, covers=[ts[0].cover], art_kind="artist", size=111.67,
                                                     name=f"Artist: {n}", lines={"Title": 2})
                      for n, ts in BIGGEST_ARTISTS]
        else:
            makers = [lambda par, f=f: b.tile(par, "Grid", f, songs(len(folder_tracks(f))), mosaic(folder_tracks(f)),
                                              name=f"Folder: {f}") for f in FOLDERS]
        g = b.grid(content, makers, name=f"{sel} grid")
        dst = {"Albums": ("Album: Paper Lanterns", "album"), "Artists": ("Artist: Kavya Raman", "artist"),
               "Folders": ("Folder: Tamil", "folder")}[sel]
        b.trigger(b.nested(g, dst[0]), dst[1])
        b.chrome(fid, tab="Home")
        b.finish(key, src)
    # The mini player paused, and playing a file opened from another app (DesignStates miniPaused, miniOpenWith).
    play = b.nested(b.nested(b.nested(d.screens["home"]["light"], "Dock"), "Mini player"), "Play or pause", typ="FRAME")
    b.trigger(play, "mini_player_paused", src="home")
    for key, name, note in (
            ("mini_player_paused", "Home / Mini player paused",
             "Paused, the mini player shows play_arrow in place of pause; the played part, the cover's tint and the heart "
             "stay. Dock.kt MiniPlayer."),
            ("mini_player_open_with", "Home / Mini player, opened with Attic",
             "A file opened from another app (playback/OpenWith.kt) isn't a library song: no cover, so no seed and no tint "
             "(Mini player Art=None, Color mode Base), the note box for a cover, and no artist. The empty artist line keeps "
             "its height, so the title sits high. The heart starts off. The played part is the file's 0:41 of 3:07.")):
        fid, content = b.phone(a, key, name, src)
        b.hero(content, "Home", mark=True)
        _home_tools(b, content, "All")
        _home_all(b, content)
        b.chrome(fid, tab="Home")
        mini = b.nested(b.nested(fid, "Dock"), "Mini player")
        if key == "mini_player_paused":
            b.props(mini, Playing="False")
            b.trigger(b.nested(mini, "Play or pause", typ="FRAME"), "home")
        else:
            b.props(mini, Art="None", Favourite="False", Playing="True")
            b.props(mini, Title="band-practice", Artist="")
            d.set_mode(mini, "Color", "Base")
            d.set(b.nested(mini, "Played", typ=None), width=round(387 * 41 / 187, 1))
            b.trigger(mini, "open_with")
        b.finish(key, src, notes=[("Note", note)])
    a.close()
    b.finish_dark(a.keys)


# ======================================================================== Search
def _result_shelf(b: B, content, res):
    makers = []
    for n, ts in res["artists"]:
        makers.append(lambda par, n=n, ts=ts: b.tile(par, "More from", n, "Artist", [ts[0].cover], art_kind="artist",
                                                     name=f"Artist: {n}", lines={"Title": 2, "Subtitle": 2}))
    for f in res["folders"]:
        makers.append(lambda par, f=f: b.tile(par, "More from", f, "Playlist", mosaic(folder_tracks(f)),
                                              name=f"Folder: {f}", lines={"Title": 2, "Subtitle": 2}))
    for al in res["albums"]:
        makers.append(lambda par, a=al: b.tile(par, "More from", a[0], f"Album, {a[1]}", [a[4]], name=f"Album: {a[0]}",
                                               lines={"Title": 2, "Subtitle": 2}))
    sh = b.shelf(content, makers, name="Results shelf")
    b.d.set(sh, paddingTop=16)
    return sh


def _results(b: B, content, query, n_rows=8):
    res = search(query)
    sh = _result_shelf(b, content, res)
    b.trigger(b.nested(sh, "Artist: Kavya Raman"), "artist")
    b.label(content, "Songs")
    b.rows_card(content, [lambda par, t=t, i=i: b.song_row(par, t, i) for i, t in enumerate(res["tracks"][:n_rows])],
                name="Songs")


def _search_bar(b: B, fid, lift):
    bar = b.inst("Top bar", fid, name="Bar", props={"Back": "None", "Lift": str(lift), "Count": "False", "Title": "Search",
                                                     "Show action 1": True, "Show action 2": False})
    b.swap_icon(b.nested(bar, "Action 1"), "Icon/settings")
    b.d.set(bar, x=0, y=40)
    return bar


def _search(d: Doc, b: B):
    a = d.area("Search", note="Search, ui/comfort/ComfortSearch.kt. At rest: the title, the field with the gear, Browse as "
                              "cards two across. Typing: the field rides on the keyboard in a card panel. With a query: the "
                              "bar, the field pinned under it, matching artists, folders and albums as a shelf, then Songs.")
    src = "ComfortSearch.kt · ComfortSearch"

    # -- browse
    fid, content = b.phone(a, "search_browse", "Search / Browse", src)
    b.hero(content, "Search")
    row = d.frame(content, name="Field row", w=SCREEN_W, h=50)
    fld = b.inst("Search field", row, name="Search field", props={"State": "Empty", "Container": "Ground"})
    d.set(fld, x=16, y=0, width=SCREEN_W - 40 - 32)
    gear = b.inst("Icon button", row, name="Settings")
    b.swap_icon(gear, "Icon/settings")
    d.set(gear, x=SCREEN_W - 8 - 48, y=1)
    b.trigger(fld, "search_typing")
    b.label(content, "Browse")
    tiles = [(f, songs(len(folder_tracks(f))), mosaic(folder_tracks(f))) for f in FOLDERS]
    tiles += [(al[0], al[1], [al[4]]) for al in BIGGEST_ALBUMS[:8]]
    rows = d.frame(content, name="Browse cards", layout="v", gap=12, pad=[0, 16, 0, 16], w=SCREEN_W, sizing=("fixed", "hug"))
    for i in range(0, len(tiles), 2):
        r = d.frame(rows, name=f"Browse row {i // 2 + 1}", layout="h", gap=12, sizing=("fill", "hug"))
        for title, sub, covers in tiles[i:i + 2]:
            t = b.inst("Browse tile", r, name=f"Browse: {title}", props={"Tinted": "False", "Title": title, "Subtitle": sub})
            d.set(t, sizingH="fill")
            b.art(b.nested(t, "Art"), covers, 56)
            if title == "Tamil":
                b.trigger(t, "folder")
            if title == "Paper Lanterns":
                b.trigger(t, "album")
    b.chrome(fid, tab="Search")
    b.finish("search_browse", src)

    # -- typing: "Ka", keyboard up
    fid, content = b.phone(a, "search_typing", "Search / Typing", src, top=96)
    _results(b, content, "ka", n_rows=6)
    _search_bar(b, fid, lift=True)
    kb = b.inst("Keyboard (system)", fid, name="Keyboard (system)")
    d.set(kb, x=0, y=SCREEN_H - 300, constraints={"horizontal": "leftRight", "vertical": "bottom"})
    panel = d.frame(fid, name="Field panel", layout="v", pad=[12, 0, 12, 0], w=SCREEN_W, x=0, y=SCREEN_H - 300 - 74,
                    fill="Color/card", sizing=("fixed", "hug"), stroke="Color/edge", stroke_w=1, strokeAlign="inside",
                    cross="center", cornerRadius=[30, 30, 0, 0])
    d.cmd("style.apply", ids=[panel], style=d.effect_styles["Shadow/Floating"])
    fld = b.inst("Search field", panel, name="Search field", props={"State": "Query", "Container": "Card", "Query": "Ka"})
    caret = d.rect(panel, name="Caret", w=2, h=22, fill="Color/accent", radius=1)
    b.k.absolute(caret, 16 + 18 + 24 + 12, 12 + 14, 2, 22, {"horizontal": "left", "vertical": "top"})
    b.trigger(fld, "search_results")
    b.chrome(fid, tab="Search", dock=False)
    b.finish("search_typing", src, notes=[("Note", "The keyboard is the system's, not Attic's: Keyboard (system) is the "
                                                     "render harness's stand-in. The field leaves the top and rides on it "
                                                     "in a card panel (top corners 30, floating shadow); the bar stays.")])

    # -- results: "ka", keyboard down
    for key, q, name in (("search_results", "ka", "Search / Results"), ("search_no_results", "lullaby", "Search / No results")):
        fid, content = b.phone(a, key, name, src, top=160)
        if key == "search_results":
            _results(b, content, q)
        else:
            es = b.inst("Empty state", content, name="Empty state",
                        props={"Title": "No results for “lullaby”", "Body": "Check the spelling, or try fewer words.",
                               "Show body": True})
            b.swap_icon(b.nested(es, "Icon box"), "Icon/search")
            d.set(content, height=SCREEN_H - 160 - 112, sizingV="fixed", mainAlign="center")
        pin = d.frame(fid, name="Pinned field", w=SCREEN_W, h=64, x=0, y=96, fill="Color/ground", stroke="Color/line",
                      stroke_w=1, strokeAlign="inside", strokeSides=[False, False, True, False])
        fld = b.inst("Search field", pin, name="Search field", props={"State": "Query", "Container": "Ground", "Query": q})
        d.set(fld, x=16, y=2)
        _search_bar(b, fid, lift=False)
        b.chrome(fid, tab="Search")
        b.finish(key, src)
    a.close()
    b.finish_dark(a.keys)


# ======================================================================== Library
LIB_PLAYLISTS = ([("Favourites", "Favourites", songs(len(LIKED)), [])]
                 + [(n, "Playlist", songs(c), mosaic([track(s) for s in ss])) for n, ss, c in PLAYLISTS]
                 + [(f, "Folder", songs(len(folder_tracks(f))), mosaic(folder_tracks(f))) for f in FOLDERS])
LIB_ARTISTS = [(n, "Artist", None, [ts[0].cover]) for n, ts in artists().items()]
LIB_ALBUMS = [(al[0], "Album", al[1], [al[4]]) for al in ALBUMS]
MOCK_LABELS = ("The mock's Artists and Albums labels add \"Recently played first\" as a small note. The code's labels carry "
               "only the name (ComfortLibrary.kt SectionLabel), so the drawing has none.")
ROW_DST = {"Favourites": "favourites", "Long drive": "playlist", "Tamil": "folder", "Paper Lanterns": "album",
           "Kavya Raman": "artist"}


def _library_top(b: B, content, sel):
    d = b.d
    b.hero(content, "Library")
    tl = b.inst("Tools", content, name="Tools", props={"Leading": "None"})
    b.trigger(b.nested(tl, "Add"), "dialog_name_playlist")
    chips = b.inst("Chip row", content, name="Chips")
    dst = {"Playlists": "library_playlists", "Albums": "library_albums", "Artists": "library_artists", "Songs": "library_songs"}
    for lab in ("Playlists", "Albums", "Artists", "Songs"):
        ch = b.nested(chips, f"Chip: {lab}")
        b.props(ch, Selected=str(lab == sel), Container="Ground", Label=lab)
        b.trigger(ch, "library_list" if lab == sel else dst[lab])


def _library(d: Doc, b: B):
    a = d.area("Library", note="Library, ui/comfort/ComfortLibrary.kt. No chip: Playlists (Favourites, playlists, folders), "
                               "Artists and Albums as three labelled cards, or grids. Library has no Folders chip; folders "
                               "list under Playlists. Songs: the count, the sort, Shuffle all songs, then every song.")
    src = "ComfortLibrary.kt · ComfortLibrary"

    def rows(content, items, label, toggle=True):
        if toggle:
            box, c = b.label_with(content, label, "View grid")
            if b.key == "library_list":
                b.trigger(c, "library_grid")
        else:
            b.label(content, label)
        makers = [lambda par, it=it, i=i: b.coll_row(par, it[1], it[0], it[2], it[3], i) for i, it in enumerate(items)]
        g, rs = b.rows_card(content, makers, name=label)
        for it, r in zip(items, rs):
            if it[0] in ROW_DST and not (it[0] == "Paper Lanterns" and it[1] != "Album"):
                b.trigger(r, ROW_DST[it[0]])

    for key, name, sel in (("library_list", "Library / All, list", None), ("library_grid", "Library / All, grid", None),
                           ("library_playlists", "Library / Playlists", "Playlists"),
                           ("library_albums", "Library / Albums", "Albums"), ("library_artists", "Library / Artists", "Artists")):
        fid, content = b.phone(a, key, name, src)
        _library_top(b, content, sel)
        if key == "library_grid":
            box, c = b.label_with(content, "Playlists", "View list")
            b.trigger(c, "library_list")
            makers = []
            for t, kind, sub, covers in LIB_PLAYLISTS:
                makers.append(lambda par, t=t, kind=kind, sub=sub, covers=covers: b.tile(
                    par, "Grid", t, sub, covers, art_kind="favourites" if kind == "Favourites" else "album", size=111.67,
                    name=f"Tile: {t}"))
            g = b.grid(content, makers, name="Playlists grid")
            for t, dst in ROW_DST.items():
                if t in ("Favourites", "Long drive", "Tamil"):
                    b.trigger(b.nested(g, f"Tile: {t}"), dst)
        elif sel is None:
            rows(content, LIB_PLAYLISTS, "Playlists")
            rows(content, LIB_ARTISTS[:4], "Artists", toggle=False)
            rows(content, LIB_ALBUMS, "Albums", toggle=False)
        else:
            items = {"Playlists": LIB_PLAYLISTS, "Albums": LIB_ALBUMS, "Artists": LIB_ARTISTS}[sel]
            rows(content, items, sel)
        b.gap(content, 160, name="Space for the dock")
        b.chrome(fid, tab="Library")
        notes = [("Code differs from mock", MOCK_LABELS)] if key in ("library_list", "library_albums", "library_artists") else []
        if key in ("library_list", "library_grid", "library_playlists"):
            notes.append(("Reference render differs", LIBRARY_COUNTS))
        b.finish(key, src, notes=notes)

    # -- Songs
    key = "library_songs"
    fid, content = b.phone(a, key, "Library / Songs", src)
    _library_top(b, content, "Songs")
    box, c = b.label_with(content, f"{len(TRACKS)} songs", "Sort", sort="Title")
    makers = [lambda par: b.inst("Shuffle all row", par, name="Shuffle all songs")]
    makers += [lambda par, t=t, i=i: b.song_row(par, t, i + 1) for i, t in enumerate(BY_TITLE[:8])]
    b.rows_card(content, makers, name="Songs")
    b.gap(content, 160, name="Space for the dock")
    b.chrome(fid, tab="Library")
    b.finish(key, src, notes=[("Note", "The index scrubber (Components / Navigation, Index scrubber) appears only once "
                                         "the title has collapsed, sorted by title; this unscrolled state has none.")])
    a.close()
    b.finish_dark(a.keys)


# ======================================================================== Collections
COLL_SRC = {"album": "ComfortCollections.kt · ComfortAlbum", "artist": "ComfortCollections.kt · ComfortArtist",
            "playlist": "ComfortCollections.kt · ComfortPlaylist", "folder": "ComfortCollections.kt · ComfortFolder",
            "favourites": "ComfortCollections.kt · ComfortFavourites"}


def _coll_bar(b: B, fid, title="", lift=False, more=False):
    # CollectionFrame: the bar sits in a box on the ground that also covers the status bar's inset, so the list
    # never shows above it.
    b.d.rect(fid, name="Bar ground", w=SCREEN_W, h=40, x=0, y=0, fill="Color/ground")
    bar = b.inst("Top bar", fid, name="Bar", props={"Back": "Arrow", "Lift": str(lift), "Count": "False", "Title": title,
                                                     "Show action 1": False, "Show action 2": more})
    b.d.set(bar, x=0, y=40)
    if not lift:
        b.d.set(bar, fill=V("Color/ground"))
    b.trigger(b.nested(bar, "Back"), "back")
    return bar


def _header(b: B, content, title, meta, covers=(), byline=None, kind="album", person=False):
    d = b.d
    h = b.inst("Collection header", content, name="Header", props={"Kind": "Person" if person else "Album", "Title": title,
                                                                   "Meta": meta})
    if byline:
        b.props(h, Byline=byline)
    elif not person:
        b.props(h, **{"Show byline": False})
    b.art(b.nested(h, "Art"), list(covers), 148 if person else 176, kind)
    return h


def _play_row(b: B, content, playing):
    return b.inst("Collection actions", content, name="Play row", props={"Playing": str(playing)})


def _album_content(b: B, content):
    al = ALBUM["Paper Lanterns"]
    ts = album_tracks("Paper Lanterns")
    _header(b, content, "Paper Lanterns", "Album • 2025", [al[4]], byline="Mira Okafor")
    _play_row(b, content, True)
    b.gap(content, 16)
    b.rows_card(content, [lambda par, t=t, i=i: b.song_row(par, t, i, lead="Number", meta=True, number=i + 1)
                          for i, t in enumerate(ts)], name="Songs")
    b.inst("Note", content, name="Footer", props={"Text": f"{songs(len(ts))} • {length(ts)}"})
    b.gap(content, 160, name="Space for the dock")


def _collections(d: Doc, b: B):
    a = d.area("Collections", note="Album, artist, playlist, folder and Favourites, ui/comfort/ComfortCollections.kt: a plain "
                                   "bar over the ground, the cover with the name centred under it, the Play row, the songs "
                                   "in one card. Once the title scrolls under the bar, the bar lifts and shows it.")
    # -- album
    src = COLL_SRC["album"]
    fid, content = b.phone(a, "album", "Album", src, top=96)
    _album_content(b, content)
    _coll_bar(b, fid)
    b.chrome(fid, tab="Home")
    b.finish("album", src)

    fid, content = b.phone(a, "album_collapsed", "Album / Scrolled, bar lifted", src, top=96)
    _album_content(b, content)
    hh = d.get(b.nested(content, "Header"))["height"]
    d.set(content, y=96 - hh - 14.6)
    _coll_bar(b, fid, title="Paper Lanterns", lift=True)
    b.chrome(fid, tab="Home")
    b.finish("album_collapsed", src)

    # -- artist
    src = COLL_SRC["artist"]
    fid, content = b.phone(a, "artist", "Artist", src, top=96)
    kav = artists()["Kavya Raman"]
    _header(b, content, "Kavya Raman", songs_and_length(kav), [kav[0].cover], kind="artist", person=True)
    _play_row(b, content, False)
    b.label(content, "Top songs")
    b.rows_card(content, [lambda par, t=track(n), i=i: b.song_row(par, t, i, lead="Rank", rank=i + 1)
                          for i, n in enumerate(KAVYA_TOP)], name="Top songs")
    b.label(content, "Albums")
    b.shelf(content, [lambda par, n=n: b.tile(par, "Album", n, str(ALBUM[n][2]), [ALBUM[n][4]], name=f"Album: {n}")
                      for n in ("Iravu Rail", "Kolam")], name="Albums shelf")
    b.gap(content, 160, name="Space for the dock")
    _coll_bar(b, fid)
    b.chrome(fid, tab="Home")
    b.finish("artist", src)

    # -- playlist and reorder
    src = COLL_SRC["playlist"]
    ld = [track(n) for n in LONG_DRIVE]
    fid, content = b.phone(a, "playlist", "Playlist", src, top=96)
    _header(b, content, "Long drive", songs_and_length(ld), mosaic(ld))
    _play_row(b, content, False)
    b.gap(content, 16)
    makers = [lambda par: b.inst("Playlist tools", par, name="Playlist tools", props={"Reorder": "True"})]
    makers += [lambda par, t=t, i=i: b.song_row(par, t, i) for i, t in enumerate(ld[:6])]
    g, rs = b.rows_card(content, makers, name="Songs")
    b.trigger(b.nested(rs[0], "Reorder songs", typ="FRAME"), "playlist_reorder")
    b.trigger(b.nested(rs[0], "Add songs", typ="FRAME"), "sheet_add_songs")
    more = b.nested(b.nested(g, "Row: Iravu Rail"), "Trailing")
    d.set(more, name="More")
    b.trigger(more, "sheet_track_menu_playlist")
    b.gap(content, 160, name="Space for the dock")
    bar = _coll_bar(b, fid, more=True)
    kebab = b.nested(bar, "Action 2")
    d.set(kebab, name="More")
    b.trigger(kebab, "menu_playlist_overflow")
    b.chrome(fid, tab="Library")
    b.finish("playlist", src)

    fid, content = b.phone(a, "playlist_reorder", "Playlist / Reorder", src + " · ReorderMode", top=112)
    b.rows_card(content, [lambda par, t=t, i=i: b.song_row(par, t, i, trailing="Drag handle") for i, t in enumerate(ld)],
                name="Songs")
    bar = b.inst("Top bar", fid, name="Bar", props={"Back": "None", "Lift": "True", "Count": "False", "Title": "Reorder songs",
                                                     "Show action 1": False, "Show action 2": False})
    d.set(b.nested(bar, "Title text", typ="TEXT"), fill=V("Color/accent-ink"))
    d.set(bar, x=0, y=40)
    fade = d.frame(fid, name="Fade", w=SCREEN_W, h=118 + 24, x=0, y=SCREEN_H - 118 - 24)
    d.set(fade, fills=[{"type": "linearGradient", "stops": [{"color": "#00000000", "position": 0},
                                                           {"color": "#000000", "position": 36 / 142}],
                        "from": [0.5, 0], "to": [0.5, 1]}])
    d.bind(fade, "fills/0/stops/0/color", "Color/derived/ground-clear")
    d.bind(fade, "fills/0/stops/1/color", "Color/ground")
    ab = b.inst("Action bar", fid, name="Action bar")
    done = b.nested(ab, "Item: Done")
    b.props(done, Label="Done reordering")
    b.trigger(b.nested(ab, "Item: Add songs"), "sheet_add_songs")
    d.set(ab, x=12, y=SCREEN_H - 24 - 22 - 68, constraints={"horizontal": "leftRight", "vertical": "bottom"})
    b.trigger(done, "playlist")
    b.chrome(fid, tab="Library", dock=False)
    b.finish("playlist_reorder", src + " · ReorderMode", notes=[(
        "Note", "Reorder mode is a full-screen dialog over everything, dock included: the bar names the mode in "
                  "accent-ink, every row ends in a drag handle and the action bar holds Add songs and Done reordering. "
                  "The reference render can't show it (Robolectric draws no dialog window), so it matches "
                  "playlist; this frame is drawn from ReorderMode in the code.")])

    # -- folder
    src = COLL_SRC["folder"]
    ft = folder_tracks("Tamil")
    fid, content = b.phone(a, "folder", "Folder", src, top=96)
    _header(b, content, "Tamil", "Playlist from a folder on this phone\n" + songs_and_length(ft), mosaic(ft))
    _play_row(b, content, False)
    b.gap(content, 16)
    b.rows_card(content, [lambda par, t=t, i=i: b.song_row(par, t, i) for i, t in enumerate(ft[:7])], name="Songs")
    b.gap(content, 160, name="Space for the dock")
    _coll_bar(b, fid)
    b.chrome(fid, tab="Library")
    b.finish("folder", src)

    # -- favourites
    src = COLL_SRC["favourites"]
    lk = [track(n) for n in LIKED]
    fid, content = b.phone(a, "favourites", "Favourites", src, top=96)
    _header(b, content, "Favourites", songs_and_length(lk), kind="favourites")
    _play_row(b, content, False)
    b.gap(content, 16)
    b.rows_card(content, [lambda par, t=t, i=i: b.song_row(par, t, i) for i, t in enumerate(lk[:7])], name="Songs")
    b.gap(content, 160, name="Space for the dock")
    _coll_bar(b, fid)
    b.chrome(fid, tab="Library")
    b.finish("favourites", src)

    # -- empty Favourites: dim play row, the message without art (ComfortFavourites: EmptyState(art = {}))
    src = COLL_SRC["favourites"]
    fid, content = b.phone(a, "favourites_empty", "Favourites / Empty", src, top=96)
    _header(b, content, "Favourites", songs_and_length([]), kind="favourites")
    d.set(_play_row(b, content, False), opacity=0.38)
    es = b.inst("Empty state", content, name="Empty state",
                props={"Title": "Songs you like will appear here", "Body": "Tap the heart on a song to save it.",
                       "Show body": True})
    d.set(b.nested(es, "Icon box"), visible=False)
    b.gap(content, 160, name="Space for the dock")
    _coll_bar(b, fid)
    b.chrome(fid, tab="Library")
    b.finish("favourites_empty", src, notes=[("Note", "With no favourites the Play row is off (38 %) and the message has no "
                                                       "art: EmptyState(art = {}). The title keeps its 22dp top padding.")])

    # -- empty playlist: a new one, named in the dialog from Library
    src = COLL_SRC["playlist"]
    fid, content = b.phone(a, "playlist_empty", "Playlist / Empty", src, top=96)
    _header(b, content, "Road trip", songs_and_length([]), [])
    d.set(_play_row(b, content, False), opacity=0.38)
    b.gap(content, 16)
    g, rs = b.rows_card(content, [lambda par: b.inst("Playlist tools", par, name="Playlist tools",
                                                     props={"Reorder": "False"})], name="Songs")
    es = b.inst("Empty state", content, name="Empty state",
                props={"Title": "Add songs to this playlist", "Body": "Tap the plus above to add songs from your library.",
                       "Show body": True})
    d.set(b.nested(es, "Icon box"), visible=False)
    b.gap(content, 160, name="Space for the dock")
    bar = _coll_bar(b, fid, more=True)
    d.set(b.nested(bar, "Action 2"), name="More")
    b.chrome(fid, tab="Library")
    b.finish("playlist_empty", src, notes=[("Note", "A playlist with no songs: the mosaic has nothing to show, so the header "
                                                     "draws the note box; Play is off, Reorder songs is off (fewer than two "
                                                     "songs), and the sentence points at Add songs above it.")])

    # -- gone: an album, artist or playlist that was removed (GoneState: the message centred, a plain Back under it)
    src = "ComfortCollections.kt · GoneState"
    for key, name, title, icon in (("item_missing", "Album / Gone", "This album is gone", "Icon/album"),
                                   ("item_missing_artist", "Artist / Gone", "This artist is gone", "Icon/person"),
                                   ("item_missing_playlist", "Playlist / Gone", "This playlist is gone", "Icon/queue_music")):
        fid, content = b.phone(a, key, name, src, top=96)
        es = b.inst("Empty state", content, name="Empty state",
                    props={"Title": title, "Body": "It may have been removed from this phone.", "Show body": True})
        b.swap_icon(b.nested(es, "Icon box"), icon)
        d.set(es, paddingBottom=0)
        act = d.frame(content, name="Action", layout="v", pad=[24, 0, 48, 0], cross="center", sizing=("hug", "hug"))
        btn = b.inst("Button", act, name="Back button", props={"Style": "Plain", "Size": "Medium", "State": "Default",
                                                              "Tone": "Default", "Label": "Back", "Show leading icon": False})
        b.trigger(btn, "back")
        d.set(content, height=SCREEN_H - 96 - 112, sizingV="fixed", mainAlign="center")
        _coll_bar(b, fid)
        b.chrome(fid, tab="Home")
        b.finish(key, src, notes=[] if key == "item_missing" else [(
            "Note", "Drawn from the code; there is no reference render. The same GoneState as Album / Gone, with the "
                    + ("Person" if key == "item_missing_artist" else "QueueMusic") + " symbol.")])
    a.close()
    b.finish_dark(a.keys)


def build(d: Doc, ctx: dict):
    _components(d, ctx)
    page_header(d, "Screens", "Screens",
                "Every Comfort screen as the code draws it with the demo library, one section per area: the Light row on top, "
                "its Dark copy under it. Frames are 411 x 914dp (the reference renders' phone); a \"(full length)\" frame "
                "shows a whole scrolled page. Each frame names its Compose source in its annotations, and the prototype "
                "links between them start at Home (Flow overview has the map).")
    b = B(d, ctx)
    _home(d, b)
    _search(d, b)
    _library(d, b)
    _collections(d, b)
