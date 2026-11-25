"""Tablet layouts of Attic Expressive: the Tablet page and the components it needs.

Run on a saved file, never as part of a regeneration (it is not in MODULES):

    cd design/build
    python3 -m figbuild.edit expressive tablet --in <copy of design/Attic Expressive.figcraft> --out <copy> \
        [--only home,library,...] [--export DIR --sheet FILE]

It draws docs/design/2026-11-25-tablet/SPEC.md: every frame of the table "Frames to draw" at Landscape (1152 x 720,
the Pad 6 sideways), Portrait (720 x 1152, the Pad 6 upright, a medium width) and Large landscape (1440 x 900),
Light above Dark, on a page named Tablet after Screens. Nothing here is in the code yet, so each frame carries a `Proposed` annotation and no ready-for-development
status. Running it again deletes the Tablet page and the components below and draws them again.

Components / Navigation
  Navigation rail        ExpressiveNavigationRail (ExpressiveRail.kt)  Selected=Home|Search|Library. The Headroom mark
                         and three Navigation item instances, 96 wide on surface.
  Player bar             ExpressivePlayerBar (ExpressivePlayerBar.kt)  State=Playing|Paused.  Cover mode.
                         Props: Title, Artist, Elapsed, Total.  Drawn 1056 wide; a frame sets the Controls width.
Components / Playback
  Now playing/Side panel the Up next | Lyrics switch of ExpressiveNowPlayingWide (ExpressiveNowPlayingWide.kt)
                         Selected=Up next|Lyrics.  Cover mode.

Areas (ctx["only"] keys): home, library, search, collections, now_playing, lyrics, settings, overlays, first_run.
"""
from __future__ import annotations

from figbuild.kit import Doc, V
from figbuild import tabletkit as tk
from figbuild.tabletkit import STATUS_H, GESTURE_H, adaptive_columns, cell_width
from expressive.foundations import DocPage, bind_dim, COLOR_MODE
from expressive.components_core import Spec, comp, frame, inst, txt, set_cover, spacer, cover_radius, set_progress, PROGRESS
from expressive.components_states import mark
from expressive import screens_browse as sb
from expressive import screens_playback as sp
from expressive import screens_settings as ss
from expressive.components_overlays import set_nested_text

SRC = "ui/expressive/"
RAIL_W = 96
BAR_H = 80                       # the player bar's row; its surface runs on behind the gesture inset
MINI_W, MINI_H = 560, 64
EXTRA = tk.MARGIN - 16           # what a destination adds to the 16 its components carry

ADDED = {
    "Components / Navigation": [("Navigation rail", "Navigation rail"), ("Player bar", "Player bar")],
    "Components / Playback": [("Now playing/Side panel", "Now Playing side panel switch")],
}
DESTS = {"Home": "home", "Search": "search", "Library": "library_music"}

# Taste mixes (HomeState.tasteMixes, empty in DemoStates): named as home_mix_name_two words them, 25 songs each
# (TasteMixes.MIX_SIZE), the mosaic from four of the mix's albums.
MIXES = [("Mix: Mira Okafor, Juniper Vale and more", [0, 3, 11, 4]),
         ("Mix: Kavya Raman, Tide & Tamarind and more", [8, 7, 5, 2]),
         ("Mix: Ruhaan Sethi, Paloma Ferrer and more", [9, 6, 2, 1]),
         ("Mix: Velvet Circuit, Odile Marchetti and more", [10, 4, 1, 3])]
# The shelves from your listening (HomeShelves.kt), also empty in DemoStates: songs from the demo library.
ON_REPEAT = sb.tracks("Iravu Rail", "Lights Over the Water", "Patang", "Neon Peaches", "Kadalkarai", "Drift Mode",
                      "Azulejo", "Room Tone")
NOT_LATELY = sb.tracks("Brushes", "Geeli Mitti", "Vaasal Kolam", "Faro Apagado", "Low Tide Hymn", "Chrome Lungs",
                       "Late Bloom", "Tamarind Skies")
NEWLY_ADDED = sb.tracks("Kadalkarai", "Tamarind Skies", "Afterglow Protocol", "Night Drive Firmware", "Lights Over the Water",
                        "Porch Light", "Marina at Six", "404 Heartbeat")


# ───────────────────────────────────────────────────────────────── small helpers
def setp(d: Doc, iid: int, **values):
    d.cmd("instance.setProperties", ids=[iid], properties={k.replace("_", " "): v for k, v in values.items()})


def fill_w(d: Doc, ids):
    d.set(ids if isinstance(ids, list) else [ids], sizingH="fill")


def stretch(d: Doc, iid: int, width: float | None, *children: str):
    """Give an instance a width (None: fill its auto layout parent) and let the named fixed-width layers follow."""
    if width is None:
        d.set(iid, sizingH="fill")
    else:
        d.set(iid, width=width)
    for name in children:
        d.set(d.child(iid, name), sizingH="fill")


def swap_icon(d: Doc, iid: int, lig: str, filled=False, layer="Icon"):
    sb.swap_icon(d, iid, lig, filled=filled, layer=layer)


def vstack(d, parent, name, *, w=None, gap=0, pad=None, fill=None, x=None, y=None, sizing=None, **kw):
    return d.frame(parent, name=name, layout="v", w=w, x=x, y=y, gap=gap, pad=pad, fill=fill,
                   sizing=sizing or (("fixed" if w else "fill"), "hug"), **kw)


def hstack(d, parent, name, *, w=None, gap=0, pad=None, fill=None, sizing=None, **kw):
    return d.frame(parent, name=name, layout="h", w=w, gap=gap, pad=pad, fill=fill,
                   sizing=sizing or (("fixed" if w else "fill"), "hug"), **kw)


def controls_width(bar: float) -> float:
    """The player bar's centre group: 36 % of the bar, 320 to 480, on the 8dp grid."""
    return float(min(480, max(320, round(bar * 0.36 / 8) * 8)))


# ───────────────────────────────────────────────────────────────── components
def build_components(d: Doc, ctx: dict):
    names = [n for items in ADDED.values() for n, _ in items]
    only = ctx.get("only")
    if only and "components" not in only and all(n in d.comp for n in names):
        ctx["exports"].update({"navigation-rail": d.comp["Navigation rail"], "player-bar": d.comp["Player bar"],
                               "now-playing-side-panel": d.comp["Now playing/Side panel"]})
        return
    tk.remove_components(d, ctx, ADDED)

    # Navigation rail and player bar -----------------------------------------------------------
    S = Spec(d, ctx, DocPage.below(d, "Components / Navigation"))

    def rail(vp):
        c = comp(d, "Navigation rail", layout="v", w=RAIL_W, h=856, pad=[20, 0, 0, 0], cross="center", fill="Color/surface")
        mark(d, c, 32, "Color/primary")
        spacer(d, c, "Mark spacing", w=1, h=28)
        for dest in DESTS:
            i = inst(d, "Navigation item", c, name=f"Item {dest}", Destination=dest, Selected=str(dest == vp["Selected"]))
            d.set(i, width=RAIL_W)
        return c, {}

    S.make("Navigation rail", "Navigation rail",
           "Material 3 Expressive's collapsed navigation rail, on medium and expanded windows in place of the navigation "
           "bar: 96dp wide on surface with no divider, from under the status bar to the bottom edge. The Headroom mark "
           "(32dp, primary) is centred on the top bar's title line, 20dp below the status bar. Home, Search and Library "
           "follow 28dp below it as Navigation item instances 96 wide: the 56 x 32 indicator pill, the label 4dp under "
           "it, 12dp between one label and the next pill. Proposed; not in the code yet.",
           "ExpressiveRail.kt", "ExpressiveNavigationRail", [dict(Selected=s) for s in DESTS], rail, preview_wrap=600)

    def player_bar(vp):
        playing = vp["State"] == "Playing"
        c = comp(d, "Player bar", layout="v", w=1056, sizing=("fixed", "hug"), fill="Color/surface-container", clip=True)
        d.set(c, cornerRadius=[28, 28, 0, 0])
        row = frame(d, c, "Bar", layout="h", h=BAR_H, pad=[0, 16, 0, 16], gap=16, cross="center", sizing=("fill", "fixed"))
        song = frame(d, row, "Song", layout="h", gap=12, cross="center", sizing=("fill", "hug"))
        inst(d, "Cover", song, name="Cover", Kind="Album", Size="56")
        col = frame(d, song, "Text column", layout="v", sizing=("fill", "hug"))
        d.set(col, maxWidth=320)
        t = txt(d, col, "Lights Over the Water", "Kit/Sheet header title", "Color/on-surface", "Title", fill=True, lines=1)
        a = txt(d, col, "Mira Okafor", "Body/Medium", "Color/on-surface-variant", "Artist", fill=True, lines=1)
        mid = frame(d, row, "Controls", layout="v", w=controls_width(1056), gap=0, cross="center", sizing=("fixed", "hug"))
        tr = frame(d, mid, "Transport", layout="h", gap=8, cross="center", sizing=("hug", "hug"))
        ib = dict(Style="Standard", Size="Default", Width="Default", State="Enabled")
        prev = inst(d, "Icon button", tr, name="Previous", **ib)
        swap_icon(d, prev, "skip_previous", filled=True)
        play = inst(d, "Play button", tr, name="Play", Size="Large", Playing=str(playing), Ring="False", Tonal="False")
        d.set(play, width=48, height=48)
        d.set(d.child(play, "Glyph"), width=28, height=28)
        if playing:
            bind_dim(d, play, "cornerRadius", "radius/large")
        nxt = inst(d, "Icon button", tr, name="Next", **ib)
        swap_icon(d, nxt, "skip_next", filled=True)
        seek = frame(d, mid, "Seek", layout="h", gap=12, cross="center", sizing=("fill", "hug"))
        el = txt(d, seek, "1:18", "Kit/Figures", "Color/on-surface-variant", "Elapsed")
        pr = inst(d, "Progress/Wavy", seek, name="Progress", Running=str(playing))
        d.set(pr, sizingH="fill")
        to = txt(d, seek, "3:52", "Kit/Figures", "Color/on-surface-variant", "Total")
        tools = frame(d, row, "Tools", layout="h", gap=0, cross="center", main="max", sizing=("fill", "hug"))
        inst(d, "Heart button", tools, name="Heart", Favourite="True")
        q = inst(d, "Icon button", tools, name="Queue", **ib)
        swap_icon(d, q, "queue_music")
        ly = inst(d, "Icon button", tools, name="Lyrics", **ib)
        swap_icon(d, ly, "lyrics")
        g = spacer(d, c, "Gesture inset", w=1, h=GESTURE_H)
        d.set(g, sizingH="fill")
        return c, {"t": t, "a": a, "el": el, "to": to, "progress": pr}

    S.make("Player bar", "Player bar",
           "The playing song on expanded windows, in place of the mini player: an 80dp row on the cover scheme's surface "
           "container, from the rail to the end edge, its top corners 28dp, its surface running on behind the gesture "
           "inset. Start: the cover at 56dp, title over artist (at most 320dp). Centre: previous, play or pause (the "
           "large play button at 48dp, without a ring since the seek bar shows progress) and next, over the elapsed "
           "time, a wavy progress bar and the length. End: favourite, queue, lyrics. The centre group is 36 % of the "
           "bar's width, at least 320 and at most 480dp (384 on the Pad sideways); the start and end groups share the "
           "rest equally, so it sits in the middle. Tapping the cover or the titles opens Now Playing. Proposed; not in "
           "the code yet.",
           "ExpressivePlayerBar.kt", "ExpressivePlayerBar", [dict(State=s) for s in ("Playing", "Paused")], player_bar,
           cover=True,
           props=[("Title", "text", "t", None, None), ("Artist", "text", "a", None, None),
                  ("Elapsed", "text", "el", None, None), ("Total", "text", "to", None, None)], preview_wrap=1600)
    for v in ctx["components"]["Player bar"]["variants"].values():
        set_progress(d, d.child(v, "Progress"), PROGRESS)

    # Now Playing's side panel switch -------------------------------------------------------------
    S = Spec(d, ctx, DocPage.below(d, "Components / Playback"))

    def side_switch(vp):
        c = comp(d, "Now playing/Side panel", layout="h", w=320, h=48, sizing=("fixed", "fixed"), cross="center")
        g = inst(d, "Connected button group", c, name="Switch", Options="2", Selected="1" if vp["Selected"] == "Up next" else "2",
                 On_cell="False")
        d.set(g, sizingH="fill")
        d.set(g, padding=[4, 0, 4, 0])
        for i, label in enumerate(("Up next", "Lyrics")):
            setp(d, d.child(g, f"Segment {i + 1}"), Label=label)
        return c, {}

    S.make("Now playing/Side panel", "Now Playing side panel switch",
           "The switch at the top of Now Playing's end half on an expanded landscape window: a two-option connected "
           "button group, Up next (the queue list) and Lyrics (the synced lyrics), 320dp wide, the selected segment in "
           "primary. Proposed; not in the code yet.",
           "ExpressiveNowPlayingWide.kt", "ExpressiveNowPlayingWide", [dict(Selected=s) for s in ("Up next", "Lyrics")],
           side_switch, cover=True, preview_wrap=760)
    # The new cards take the width of the cards already on their page, as tidy_doc_pages gave those.
    for page, items in ADDED.items():
        tops = d.get(d.pages[page], depth=1).get("children", [])
        mine = {f"Card · {title}" for _, title in items}
        wmax = max(n.get("width", 0) for n in tops if n.get("name", "").startswith("Card · ") and n["name"] not in mine)
        d.set([n["id"] for n in tops if n.get("name") in mine], sizingH="fixed", width=wmax)
    ctx["exports"].update({"navigation-rail": d.comp["Navigation rail"], "player-bar": d.comp["Player bar"],
                           "now-playing-side-panel": d.comp["Now playing/Side panel"]})


# ───────────────────────────────────────────────────────────────── a tablet frame
class Screen:
    """One window: the content first, then the shell on top (player bar or mini player, rail, system bars)."""

    def __init__(self, B: "Builder", fr: tk.Frames, key: str, screen: str, size: str, source: str, *, tab="Home",
                 fill=None, h=None, suffix=""):
        self.B, self.d, self.fr, self.key, self.size, self.tab = B, B.d, fr, key, size, tab
        self.f, self.W, self.H = fr.frame(key, screen, size, source, fill=fill, h=h, suffix=suffix)
        self.wh = tk.SIZES[size][1]             # the window's height (H is taller on a full-length frame)
        self.x0 = RAIL_W                        # the content area: from the rail to the end edge
        self.cw = self.W - RAIL_W
        self.expanded = self.W >= 840           # Material's expanded width: the player bar; below it the mini player
        self.notes: list[str] = []

    # the content area's bottom inset, as LocalBottomPadding gives it to a destination
    @property
    def bottom(self) -> float:
        return (BAR_H + GESTURE_H) if self.expanded else (MINI_H + 12 + GESTURE_H)

    def content(self, name="Content") -> int:
        """The destination's column over the whole content area: 8dp in at the start, so the 16dp its components
        carry make the 24dp margin; shelves run on to the end edge, blocks stop 24dp before it (`block`)."""
        d = self.d
        return d.frame(self.f, name=name, layout="v", x=self.x0, y=0, w=self.cw, h=self.H, pad=[0, 0, 0, EXTRA],
                       clip=True, sizing=("fixed", "fixed"))

    @property
    def inner(self) -> float:
        """The width of a block in the content column: the content area less the extra margin at both edges."""
        return self.cw - 2 * EXTRA

    def shell(self, *, player=True, rail=True, links=True):
        d, f, W, H = self.d, self.f, self.W, self.H
        L = self.B.links if links else []
        if player and self.expanded:
            b = inst(d, "Player bar", f, name="Player bar", State="Playing", x=self.x0, y=H - BAR_H - GESTURE_H)
            d.set(b, width=self.cw)
            d.set(d.child(b, "Controls"), width=controls_width(self.cw))
            set_progress(d, d.child(b, "Progress"), PROGRESS)
            L.append((self.key, d.child(b, "Song"), f"now_playing_up_next_{self.size}", "open"))
        elif player:
            mw = min(MINI_W, self.cw - 2 * tk.MARGIN)
            # The gesture inset is a band of the page colour, so nothing shows under the handle.
            d.rect(f, name="Gesture inset", w=self.cw, h=GESTURE_H, x=self.x0, y=H - GESTURE_H, fill=self.d.screen_fill)
            m = inst(d, "Mini player", f, name="Mini player", State="Playing", Title_length="Default",
                     x=self.x0 + round((self.cw - mw) / 2, 1), y=H - GESTURE_H - 12 - MINI_H)
            d.set(m, width=mw)
            L.append((self.key, m, f"now_playing_{self.size}", "open"))
        if rail:
            r = inst(d, "Navigation rail", f, name="Navigation rail", Selected=self.tab, x=0, y=STATUS_H)
            d.set(r, height=H - STATUS_H)
            for dest, stem in (("Home", "home"), ("Search", "search"), ("Library", "library")):
                if f"{stem}_{self.size}" != self.key:
                    L.append((self.key, d.child(r, f"Item {dest}"), f"{stem}_{self.size}", "tab"))
        self.bars()

    def bars(self):
        d, f, W, H = self.d, self.f, self.W, self.H
        g = d.instance("Gesture bar", f, name="Gesture bar", x=0, y=H - GESTURE_H)
        d.set(g, width=W, height=GESTURE_H)
        s = d.instance("Status bar", f, name="Status bar", x=0, y=0)
        d.set(s, width=W, height=STATUS_H)

    def done(self):
        self.fr.annotate(self.key, *self.notes)


class Builder:
    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.P = sb.Builder(d, ctx)            # the phone module's row and tile helpers
        self.links: list[tuple] = []           # (source key, Light layer, destination key or None, tab|open|back)

    def wire(self) -> int:
        """Prototype links between the tablet frames that exist: rail tabs, the player to Now Playing, and its
        close button back. Run once every section is closed, so the Dark copies take the links too."""
        d, n = self.d, 0
        for src, layer, dst, kind in self.links:
            if src not in d.screens or (dst and dst not in d.screens):
                continue
            if kind == "tab":
                d.link(src, layer, dst, transition="dissolve", duration=200)
            elif kind == "open":
                d.link(src, layer, dst, transition="moveIn", direction="top", duration=400)
            else:
                d.link(src, layer, None, action="back")
            n += 1
        for key, name in (("home_landscape", "Tablet, landscape"), ("home_portrait", "Tablet, portrait")):
            if key in d.screens:
                d.flow_start(key, name)
        return n

    # top bars -----------------------------------------------------------------------------------
    def root_bar(self, parent, title: str, *, search=False, grid=False):
        """RootAppBar under a 24dp status bar. The rail carries the mark, so Home's bar is the plain kind."""
        d = self.d
        bar = inst(d, "Top app bar/Root", parent, name="Top app bar", Kind="Plain", Scrolled="False")
        setp(d, bar, Title=title, Show_search=search, Show_grid_toggle=grid)
        stretch(d, bar, None, "Status bar inset", "Bar")
        d.set(d.child(bar, "Status bar inset"), height=STATUS_H)
        return bar

    # sections -----------------------------------------------------------------------------------
    def header(self, parent, title, overline=None, width=None):
        h = self.P.section(parent, title, overline)
        if width is None:
            fill_w(self.d, h)
        else:
            self.d.set(h, width=width)
        return h

    def shelf(self, parent, name, gap=12):
        """A shelf that scrolls sideways: it starts at the margin and runs on to the window's end edge."""
        d = self.d
        return d.frame(parent, name=name, layout="h", gap=gap, pad=[0, 0, 0, 16], clip=True, sizing=("fill", "hug"))

    def song_shelf(self, parent, title, tracks, width):
        self.header(parent, title, width=width)
        sh = self.shelf(parent, f"{title} shelf")
        for t in tracks:
            self.P.tile(sh, "Recent", t.title, t.artist, [t.cover], now=t.title == sb.NOW)
        return sh

    def groups(self, parent, rows, columns: int, width: float, name="Group", gap=12):
        """Rows of a grouped list in `columns` columns: each column its own group, filled top to bottom."""
        d = self.d
        wrap = d.frame(parent, name=name, layout="h", w=width, gap=gap, pad=[0, 16, 0, 16], sizing=("fixed", "hug"))
        per = -(-len(rows) // columns)
        made = []
        for c in range(columns):
            part = rows[c * per:(c + 1) * per]
            if not part:
                continue
            g, cells = self.P.group(wrap, part, name=f"{name} column {c + 1}")
            d.set(g, padding=[0, 0, 0, 0], sizingH="fill")
            made += cells
        return wrap, made


# ───────────────────────────────────────────────────────────────── Home
def hero_sizes(available: float, preferred: float) -> list[tuple[str, float]]:
    """Material's multi-browse arrangement for a carousel `available` wide, 8dp spacing and a small item of 56: as
    many large items as keep their width nearest `preferred`, then one medium (halfway between large and small) and
    one small."""
    best = None
    for n in (1, 2, 3, 4):
        small = 56
        large = (available - 8 * (n + 1) - 1.5 * small) / (n + 0.5)
        if large < 120:
            continue
        if best is None or abs(large - preferred) < abs(best[1] - preferred):
            best = (n, large, small)
    n, large, small = best
    large = round(large)
    medium = round(available - 8 * (n + 1) - n * large - small)
    return [("Large", large)] * n + [("Medium", medium), ("Small", small)]


def hero_height(window_h: float) -> tuple[float, float]:
    """The carousel's height and preferred item width: 280 and 360 in a window 840dp tall or taller, the phone's 200
    with 260 in a shorter one (the Pad sideways, 720dp)."""
    return (280, 360) if window_h >= 840 else (200, 260)


def home_content(B: Builder, s: Screen, col, *, full: bool):
    d, P = B.d, B.P
    W = s.inner
    side_by_side = s.expanded           # the Recap card beside Instant mixes
    HERO_H, preferred = hero_height(s.wh)
    filt = inst(d, "Connected button group", col, name="Home filter", Options="4", Selected="1", On_cell="False")
    d.set(filt, width=min(W, 480 + 32))

    # Places: Favourites pinned as the pill, then the carousel to the end edge.
    places = d.frame(col, name="Places", layout="h", gap=8, pad=[12, 0, 0, 16], clip=True, sizing=("fill", "hug"))
    pill = inst(d, "Favourites pill", places, name="Favourites pill", Height="248")
    d.set(pill, height=HERO_H + 48)
    car = d.frame(places, name="Carousel", layout="v", sizing=("hug", "hug"))
    row = d.frame(car, name="Carousel items", layout="h", gap=8, sizing=("hug", "hug"))
    sizes = hero_sizes(s.cw - EXTRA - 16 - 64 - 8, preferred)
    for (size, w), album in zip(sizes, sb.SHORTCUTS):
        # A slot of the carousel at the width the arrangement gives it. The phone's Hero carousel item has three fixed
        # widths and a picture that can't move inside an instance, so the slot is drawn here: a clipped frame with the
        # cover filling it, as maskClip and requiredSize do in HeroCarousel.
        it = d.frame(row, name=f"Carousel item · {album}", w=w, h=HERO_H, clip=True, fill="Color/surface-container-highest")
        bind_dim(d, it, "cornerRadius", "radius/extra-large")
        side = max(w, HERO_H)
        cv = inst(d, "Cover", it, name="Cover", Kind="Album", Size="208", x=round((w - side) / 2, 1),
                  y=round((HERO_H - side) / 2, 1))
        d.set(cv, width=side, height=side, cornerRadius=0)
        set_cover(d, cv, sb.ALBUM_INFO[album][4])
    cap = d.instance("Hero caption", car, name="Hero caption")
    setp(d, cap, Title="Paper Lanterns", Subtitle="Mira Okafor, 2025")
    d.set(cap, width=sizes[0][1])

    # Recap and Instant mixes: side by side from 1200dp, stacked below it; the pills in three equal columns.
    def mixes(parent, width):
        h = B.header(parent, "Instant mixes")
        grid = d.frame(parent, name="Instant mixes", layout="v", gap=8, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        for r in range(0, 6, 3):
            rw = d.frame(grid, name=f"Mix row {r // 3 + 1}", layout="h", gap=8, sizing=("fill", "hug"))
            for t in sb.SPEED_DIAL[r:r + 3]:
                m = inst(d, "Instant mix pill", rw, name=f"Instant mix · {t.title}", Now="False")
                setp(d, m, Title=t.title)
                set_cover(d, m, t.cover)
                d.set(m, sizingH="fill")
        return h

    if side_by_side:
        # The card takes 40 % of the row, at most 480dp, and is as tall as the mixes beside it.
        card_w = min(480, round((W - 32) * 0.4 / 8) * 8)
        pair = d.frame(col, name="Recap and instant mixes", layout="h", w=W, gap=8, sizing=("fixed", "hug"))
        rc = d.instance("Recap entry", pair, name="Recap entry")
        mx = d.frame(pair, name="Instant mixes column", layout="v", sizing=("fill", "hug"))
        mixes(mx, W - card_w - 40)
        d.set(rc, width=card_w + 32, height=d.get(mx)["height"])
        d.set(d.child(rc, "Card"), sizingV="fill")
    else:
        rc = d.instance("Recap entry", col, name="Recap entry")
        d.set(rc, width=W)
        mixes(col, W)
        for n in ("Section header · Instant mixes", "Instant mixes"):
            d.set(d.child(col, n), sizingH="fixed", width=W)

    # Mixes: a shelf of mosaic tiles, the title on two lines.
    B.header(col, "Mixes", "Songs you play, grouped by sound", width=W)
    sh = B.shelf(col, "Mixes shelf")
    for name, covers in MIXES:
        t = P.tile(sh, "Folder", name, "25 songs", covers, name=f"Tile · {name}")
        d.set(d.child(t, "Title"), maxLines=2, textAutoResize="height")
    B.song_shelf(col, "Recently played", sb.RECENT, W)
    if not full:
        return
    B.song_shelf(col, "On repeat", ON_REPEAT, W)
    B.song_shelf(col, "Haven't heard in a while", NOT_LATELY, W)
    B.song_shelf(col, "Newly added", NEWLY_ADDED, W)
    B.header(col, "Start listening", "Picked at random from your songs", width=W)
    B.groups(col, [dict(title=t.title) for t in sb.START], 2 if s.expanded else 1, W, name="Start listening group")
    B.header(col, "Your folders", width=W)
    sh = B.shelf(col, "Folders shelf")
    for f in sb.FOLDERS:
        P.tile(sh, "Folder", f, sb.songs(len(sb.FOLDER_TRACKS[f])), sb.mosaic(sb.FOLDER_TRACKS[f]))
    B.header(col, "Albums", width=W)
    sh = B.shelf(col, "Albums shelf")
    for a in sb.BIGGEST_ALBUMS:
        P.tile(sh, "Album", a, sb.ALBUM_INFO[a][0], [sb.ALBUM_INFO[a][4]])
    sp = d.instance("Spotlight", col, name="Spotlight")
    d.set(sp, width=min(W, 720 + 32))
    B.header(col, "Top artists", width=W)
    sh = B.shelf(col, "Top artists shelf")
    for a in sb.BIGGEST_ARTISTS:
        P.tile(sh, "Artist", a, None, [sb.ARTIST_COVER[a]])


def build_home(B: Builder):
    d = B.d
    fr = tk.Frames(d, B.ctx, "Home", "home",
                   note="Home on a tablet. The rail carries the mark, so the bar is the title and settings. The filter is "
                        "at most 480dp wide. The carousel's large items are near 260dp in a window under 840dp tall and "
                        "near 360dp, 280dp tall, in a taller one; shelves run on to the end edge and show more. On "
                        "expanded widths the Recap card sits beside Instant mixes, whose pills take three equal columns, "
                        "and Start listening takes two columns.")
    src = SRC + "ExpressiveHome.kt · ExpressiveHome"
    demo = ("Mixes, On repeat, Haven't heard in a while and Newly added are empty in the demo state; the frame fills "
            "them from the demo library so the layout shows them.")
    for size in ("landscape", "portrait", "large"):
        s = Screen(B, fr, f"home_{size}", "Home", size, src)
        col = s.content()
        B.root_bar(col, "Attic")
        home_content(B, s, col, full=False)
        s.shell()
        s.notes.append(demo)
        s.done()
    # The whole page, so the sections below the fold have a drawing too.
    row_h = fr.area.row_h
    s = Screen(B, fr, "home_large_full", "Home", "large", src, h=2600, suffix=" (full length)")
    col = s.content()
    B.root_bar(col, "Attic")
    home_content(B, s, col, full=True)
    d.set(col, sizingV="hug")
    h = d.get(col)["height"] + s.bottom + 16
    d.set(col, sizingV="fixed", height=h)
    d.set(s.f, height=h)
    d.screens[s.key]["h"] = h
    fr.area.row_h = max(row_h, h)           # the Dark row sits under the frame's real height
    s.H = h
    s.shell()
    s.notes += [demo, "The whole page from the filter to Top artists, then the player bar, on a 1440dp window."]
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── collections (album, artist, playlist)
ALBUM = sb.ALBUM_TRACKS["Paper Lanterns"]
KAVYA = sb.ranked("Kavya Raman")
COLL = SRC + "ExpressiveCollectionPanes.kt · CollectionPanes"


def album_spec() -> dict:
    return dict(kind="Album", title="Paper Lanterns", artist="Mira Okafor",
                metas=["Album • 2025", sb.songs_and_duration(ALBUM)], cover=[0], seed=0, playing=True, options=False,
                sections=[dict(rows=[dict(title=t.title, lead="Number", number=t.number) for t in ALBUM],
                               name="Tracks group", footer=sb.songs_and_duration(ALBUM))])


def artist_spec() -> dict:
    albums = [a for a in sb.ALBUMS if a[0] == "Kavya Raman"]
    return dict(kind="Artist", title="Kavya Raman", metas=[sb.songs_and_duration(sb.ARTIST_TRACKS["Kavya Raman"])],
                cover=[8], seed=sb.ARTIST_SEED["Kavya Raman"], playing=False, options=False,
                sections=[dict(header="Top songs", name="Top songs group",
                               rows=[dict(title=t.title, lead="Ranked cover", number=i + 1) for i, t in enumerate(KAVYA[:5])]),
                          dict(header="Albums", tiles=[(a[1], f"{a[2]} • Album", a[4]) for a in albums]),
                          dict(header="All songs", name="All songs group", rows=[dict(title=t.title) for t in KAVYA[5:]])])


def playlist_spec() -> dict:
    return dict(kind="Playlist", title="Long drive", metas=[sb.songs_and_duration(sb.LONG_DRIVE)],
                cover=sb.mosaic(sb.LONG_DRIVE), seed=sb.PLAYLIST_SEED["Long drive"], playing=False, options=True, tools=True,
                sections=[dict(rows=[dict(title=t.title) for t in sb.LONG_DRIVE], name="Songs group")])


def header_cover(d: Doc, parent, spec: dict, side: float) -> int:
    kind, cover = spec["kind"], spec["cover"]
    if kind == "Artist":
        cv = inst(d, "Cover", parent, name="Cover", Kind="Artist", Size="112")
        sb.resize_cover(d, cv, side, "Artist")
        set_cover(d, cv, cover[0])
    elif len(cover) >= 4:
        cv = inst(d, "Cover", parent, name="Cover", Kind="Mosaic", Size="148")
        sb.resize_cover(d, cv, side, "Mosaic")
        set_cover(d, cv, *cover[:4])
    else:
        cv = inst(d, "Cover", parent, name="Cover", Kind="Album", Size="208")
        sb.resize_cover(d, cv, side, "Album")
        set_cover(d, cv, cover[0])
    return cv


def header_texts(d: Doc, parent, spec: dict) -> int:
    """Title, an album's artist byline and the meta lines, spaced as CollectionHeader spaces them."""
    col = d.frame(parent, name="Titles", layout="v", sizing=("fill", "hug"))
    txt(d, col, spec["title"], "Kit/Artist title" if spec["kind"] == "Artist" else "Kit/Collection title",
        "Color/on-surface", "Title", fill=True, lines=3)
    if spec.get("artist"):
        by = d.frame(col, name="Byline", layout="h", gap=8, pad=[10, 0, 0, 0], cross="center", sizing=("hug", "hug"))
        a = inst(d, "Cover", by, name="Artist picture", Kind="Artist", Size="28")
        set_cover(d, a, spec["cover"][0])
        txt(d, by, spec["artist"], "Kit/Play label", "Color/primary", "Artist name", lines=1)
    for i, m in enumerate(spec["metas"]):
        nm = "Meta" if i == 0 else "Second meta"
        box = d.frame(col, name=nm, layout="v", pad=[6, 0, 0, 0], sizing=("fill", "hug"))
        txt(d, box, m, "Body/Medium", "Color/on-surface-variant", f"{nm} text", fill=True)
    return col


def header_actions(d: Doc, parent, spec: dict, *, fill=True) -> int:
    """Play (squared and reading Pause while this collection plays), shuffle and add to playlist, in one row."""
    row = d.frame(parent, name="Actions", layout="h", gap=8, cross="center", sizing=("fill", "hug"))
    playing = spec["playing"]
    p = inst(d, "Button", row, name="Play", Style="Filled", Size="Medium", Icon="Leading", State="Enabled",
             Squared=str(playing))
    setp(d, p, Label="Pause" if playing else "Play")
    swap_icon(d, p, "pause" if playing else "play_arrow", filled=True, layer="Leading icon")
    if fill:
        d.set(p, sizingH="fill")
    inst(d, "Toggle button", row, name="Shuffle", Checked="False", Size="Medium", Off_style="Tonal")
    add = inst(d, "Icon button", row, name="Add to playlist", Style="Tonal", Size="Medium", Width="Default", State="Enabled")
    swap_icon(d, add, "playlist_add")
    return row


def header_card(B: "Builder", parent, spec: dict, *, x, y, w, h) -> int:
    """The two-pane header: a card on the cover scheme's surface container holding the cover, the titles and the
    actions. Drawn here from the header's parts, since the phone's Collection header stacks them the other way."""
    d = B.d
    card = d.frame(parent, name="Collection header", layout="v", x=x, y=y, w=w, pad=16, fill="Color/surface-container",
                   clip=True, sizing=("fixed", "hug"))
    d.set(card, maxHeight=h)
    bind_dim(d, card, "cornerRadius", "radius/extra-large-increased")
    header_cover(d, card, spec, w - 32)
    spacer(d, card, "Cover spacing", w=1, h=16)
    header_texts(d, card, spec)
    spacer(d, card, "Actions spacing", w=1, h=16)
    header_actions(d, card, spec)
    sb.album_mode(d, card, spec["seed"])
    return card


def header_row(B: "Builder", parent, spec: dict) -> int:
    """The header in a pane under 760dp: the cover at 200dp with the titles and the actions beside it."""
    d = B.d
    wrap = d.frame(parent, name="Header row", layout="v", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    card = d.frame(wrap, name="Collection header", layout="h", gap=20, pad=16, fill="Color/surface-container", clip=True,
                   sizing=("fill", "hug"))
    bind_dim(d, card, "cornerRadius", "radius/extra-large-increased")
    header_cover(d, card, spec, 200)
    side = d.frame(card, name="Details", layout="v", h=200, main="spaceBetween", sizing=("fill", "fixed"))
    header_texts(d, side, spec)
    header_actions(d, side, spec, fill=False)
    sb.album_mode(d, card, spec["seed"])
    return wrap


def collection(B: "Builder", s: "Screen", spec: dict, *, pane_x: float, pane_w: float, lead: float, own: bool):
    """A collection in a pane from `pane_x`, `pane_w` wide. `lead` is the margin before its header (24 as its own
    screen, 8 beside the Library list). `own`: it is its own screen, so the bar with back (and a playlist's menu)
    sits above the songs."""
    d, P = B.d, B.P
    two = pane_w >= 760
    top = STATUS_H + 8
    end = pane_x + pane_w - EXTRA
    if two:
        start = 360 if pane_w >= 1000 else 320
        header_card(B, s.f, spec, x=pane_x + lead, y=top, w=start - lead - 16, h=s.H - top - s.bottom - 8)
        col_x = pane_x + start - 8
    else:
        col_x = pane_x + lead - 16
    col_w = end - col_x
    col = d.frame(s.f, name="Songs pane" if two else "Collection", layout="v", x=col_x, y=0, w=col_w, h=s.H, clip=True,
                  sizing=("fixed", "fixed"))
    if own:
        bar = inst(d, "Top app bar/Collection", col, name="Collection bar", Collapsed="False")
        stretch(d, bar, None, "Status bar inset", "Bar")
        d.set(d.child(bar, "Status bar inset"), height=STATUS_H)
        d.set_mode([bar] + d.find(bar, type="INSTANCE", limit=50), "Color", "Wallpaper")
        if not spec["options"]:
            d.set(d.child(bar, "Options"), visible=False)
    else:
        spacer(d, col, "Status bar inset", w=1, h=top)
    if not two:
        header_row(B, col, spec)
        spacer(d, col, "Header spacing", w=1, h=4)
    if spec.get("tools"):
        tools = d.frame(col, name="Tools", layout="h", gap=8, pad=[0 if two and own else 12, 16, 12, 16], sizing=("fill", "hug"))
        b1 = inst(d, "Button", tools, name="Add songs", Style="Tonal", Size="Small", Icon="Leading", State="Enabled",
                  Squared="False")
        setp(d, b1, Label="Add songs")
        swap_icon(d, b1, "add", layer="Leading icon")
        b2 = inst(d, "Button", tools, name="Reorder songs", Style="Outlined", Size="Small", Icon="Leading", State="Enabled",
                  Squared="False")
        setp(d, b2, Label="Reorder songs")
        swap_icon(d, b2, "swap_vert", layer="Leading icon")
    rows_w = col_w - 32
    cols = adaptive_columns(rows_w, 360, 12)
    first = True
    for sec in spec["sections"]:
        if sec.get("header"):
            h = B.header(col, sec["header"])
            if first and (own or not two):
                d.set(h, paddingTop=12)
        elif not own and two:
            pass
        elif not spec.get("tools"):
            spacer(d, col, "List spacing", w=1, h=8 if (own and two) else 12)
        first = False
        if "tiles" in sec:
            n = adaptive_columns(rows_w, 160, 12)
            tw = cell_width(rows_w, n, 12)
            grid = d.frame(col, name=f"{sec['header']} grid", layout="h", gap=12, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
            for title, sub, cover in sec["tiles"][:n]:
                P.tile(grid, "Album", title, sub, [cover], width=tw)
            continue
        B.groups(col, sec["rows"], cols, col_w, name=sec["name"])
        if sec.get("footer"):
            ft = d.frame(col, name="Footer", layout="h", pad=[14, 32, 0, 32], sizing=("fill", "hug"))
            txt(d, ft, sec["footer"], "Label/Large", "Color/on-surface-variant", "Summary", fill=True)
    return col


def collection_screen(B: "Builder", fr: tk.Frames, key: str, screen: str, size: str, spec: dict, tab: str, *, shell=True):
    s = Screen(B, fr, key, screen, size, COLL, tab=tab)
    collection(B, s, spec, pane_x=s.x0, pane_w=s.cw, lead=tk.MARGIN, own=True)
    if shell:
        s.shell()
    return s


def build_collections(B: "Builder"):
    fr = tk.Frames(B.d, B.ctx, "Collections", "collections",
                   note="Album, artist and playlist lay themselves out for the pane they are given. 760dp or wider: the "
                        "header is a card in a start pane (360dp, 320 below 1000) on the cover's scheme, the cover as "
                        "wide as the card allows, and the songs scroll in the end pane under the bar with back, in "
                        "columns at least 360dp wide. Narrower (the Pad upright): the header is a row, the cover at "
                        "200dp with the titles and actions beside it, then the songs.")
    for size in ("landscape", "portrait", "large"):
        s = collection_screen(B, fr, f"album_{size}", "Album", size, album_spec(), "Home")
        s.notes.append("The header card runs FromArt with Paper Lanterns' seed (Color mode Cover); the bar and the list "
                       "stay on the app scheme. Play reads Pause and is squared because this album is playing.")
        s.done()
    s = collection_screen(B, fr, "artist_landscape", "Artist", "landscape", artist_spec(), "Home")
    s.notes.append("The header card is on Iravu Rail's scheme, the artist's first cover. Albums are a grid in the end "
                   "pane, cells at least 160dp wide, in place of the phone's shelf.")
    s.done()
    s = collection_screen(B, fr, "playlist_landscape", "Playlist", "landscape", playlist_spec(), "Library")
    s.notes.append("The header card is on Afterglow Protocol's scheme, the playlist's first cover. Add songs and "
                   "Reorder songs sit above the list, under the bar with back and the menu.")
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Library
LIST_PANE = 400                  # the chip row with a chip on needs 372 after the 24dp margin


def library_chrome(B: "Builder", col, filt):
    d = B.d
    bar = B.root_bar(col, "Library", search=True, grid=True)
    box = d.frame(col, name="Chips", layout="v", pad=[8, 0, 20, 0], clip=True, sizing=("fill", "hug"))
    inst(d, "Chip row", box, name="Chip row", Selected=filt or "None")
    return bar


def library_list_pane(B: "Builder", s: "Screen", filt: str, selected: str):
    """The Library's list in its 400dp start pane under the chip `filt`, the row titled `selected` selected."""
    d, P = B.d, B.P
    col = d.frame(s.f, name="Library list", layout="v", x=s.x0, y=0, w=LIST_PANE, h=s.H, pad=[0, 0, 0, EXTRA], clip=True,
                  sizing=("fixed", "fixed"))
    library_chrome(B, col, filt)
    rows = sb.lib_rows(filt)[0][1]
    _, cells = P.group(col, rows, name=f"{filt} group")
    # The selected row takes the current song row's colours: secondary container, the subtitle in its ink.
    c, row = cells[[r["title"] for r in rows].index(selected)]
    d.set(c, fill=V("Color/secondary-container"))
    d.set(d.child(row, "Subtitle"), fill=V("Color/on-secondary-container"))
    if filt == "Playlists":
        fab = inst(d, "Extended FAB", s.f, name="New playlist", Expanded="True")
        setp(d, fab, Label="New playlist")
        d.set(fab, x=s.x0 + LIST_PANE - 16 - d.get(fab)["width"], y=s.H - s.bottom - 12 - 56)
    return col


def library_landscape(B: "Builder", s: "Screen", filt="Albums", selected="Paper Lanterns", spec=None):
    library_list_pane(B, s, filt, selected)
    collection(B, s, spec or album_spec(), pane_x=s.x0 + LIST_PANE, pane_w=s.cw - LIST_PANE, lead=8, own=False)


def build_library(B: "Builder"):
    d, P = B.d, B.P
    fr = tk.Frames(d, B.ctx, "Library", "library",
                   note="Expanded and landscape: list and detail. The list keeps its bar and chips in a 400dp start pane, "
                        "the selected row on secondary container; the album it opened fills the rest and lays itself "
                        "out for that width (a header row at 656dp, two panes at 944dp). Upright, the list has the "
                        "window and a row opens its own screen.")
    for size in ("landscape", "large"):
        s = Screen(B, fr, f"library_{size}", "Library", size, "ui/LibraryPanes.kt · LibraryListDetail", tab="Library")
        library_landscape(B, s)
        s.shell()
        s.notes.append("Albums chip on; Paper Lanterns, the first album, is selected on arrival and open beside the "
                       "list. The detail has no back: the list is beside it.")
        s.done()
    s = Screen(B, fr, "library_portrait", "Library", "portrait", SRC + "ExpressiveLibrary.kt · ExpressiveLibrary",
               tab="Library")
    col = s.content()
    library_chrome(B, col, None)
    W = s.inner
    cols = adaptive_columns(W - 32, 360, 12)
    for i, (label, rows) in enumerate(sb.lib_rows(None)):
        if label:
            lab = P.list_label(col, label)
            d.set(lab, width=W)
        elif i:
            spacer(d, col, "Group spacing", w=1, h=12)
        B.groups(col, rows[:9], cols, W, name=f"{label or 'Playlists'} group")
    fab = inst(d, "Extended FAB", s.f, name="New playlist", Expanded="True")
    setp(d, fab, Label="New playlist")
    d.set(fab, x=s.W - tk.MARGIN - d.get(fab)["width"], y=s.H - s.bottom - 12 - 56)
    s.shell()
    s.notes.append("No chip, list view: 624dp of content holds one column of rows (two need 732). New playlist floats "
                   "above the mini player.")
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Search
def build_search(B: "Builder"):
    d, P = B.d, B.P
    fr = tk.Frames(d, B.ctx, "Search", "search",
                   note="Results for \"ka\". The docked field is at most 720dp wide. What you open (artists, albums) and "
                        "the songs keep their sections; rows sit in columns at least 360dp wide, each column its own "
                        "group, filled top to bottom.")
    src = SRC + "ExpressiveSearch.kt · ExpressiveSearch"
    res = sb.search("ka")
    opens = [dict(kind="coll", type="Artist", title=a, sub="Artist", covers=[sb.ARTIST_COVER[a]]) for a in res["artists"]]
    opens += [dict(kind="coll", type="Album", title=a, sub=f"Album, {sb.ALBUM_INFO[a][0]}", covers=[sb.ALBUM_INFO[a][4]])
              for a in res["albums"]]
    opens += [dict(kind="coll", type="Folder", title=f, sub="Playlist", covers=sb.mosaic(sb.FOLDER_TRACKS[f]))
              for f in res["folders"]]
    for size in ("landscape", "portrait"):
        s = Screen(B, fr, f"search_{size}", "Search, results", size, src, tab="Search")
        col = s.content()
        W = s.inner
        spacer(d, col, "Status bar inset", w=1, h=STATUS_H + 8)
        fw = d.frame(col, name="Field row", layout="h", w=min(W, 720 + 32), pad=[0, 16, 0, 16], sizing=("fixed", "hug"))
        fld = inst(d, "Search field", fw, name="Search field", State="Docked")
        d.set(fld, sizingH="fill")
        setp(d, fld, Text="ka")
        spacer(d, col, "Results spacing", w=1, h=12)
        cols = adaptive_columns(W - 32, 360, 12)
        B.groups(col, opens, cols, W, name="Open group")
        lab = P.list_label(col, "Songs")
        d.set(lab, width=W)
        n = 8 if cols > 1 else 9
        B.groups(col, [dict(title=t.title) for t in res["tracks"][:n]], cols, W, name="Songs group")
        s.shell()
        s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Now Playing
NPW = SRC + "ExpressiveNowPlayingWide.kt · ExpressiveNowPlayingWide"


def cover_pager(d: Doc, parent, side: float, peek: float = 72):
    """The cover with the next song's cover peeking after it. The phone's Now playing/Cover pager is 411 wide with a
    323dp cover and can't be laid out again inside an instance, so the pager is drawn here from Cover instances."""
    row = d.frame(parent, name="Cover pager", layout="h", gap=8, sizing=("hug", "hug"))
    cv = inst(d, "Cover", row, name="Cover", Kind="Album", Size="208")
    d.set(cv, width=side, height=side)
    bind_dim(d, cv, "cornerRadius", "radius/extra-large")
    set_cover(d, cv, 0)
    nx = d.frame(row, name="Next cover", w=peek, h=side, clip=True, fill="Color/surface-container-highest")
    bind_dim(d, nx, "cornerRadius", "radius/large-increased")
    art = inst(d, "Cover", nx, name="Next cover art", Kind="Album", Size="208", x=round((peek - side) / 2, 1), y=0)
    d.set(art, width=side, height=side, cornerRadius=0)
    set_cover(d, art, 0)
    return row


def player_column(B: "Builder", parent, *, x, y, col_w, side, lyric: bool, compact: bool, context="Paper Lanterns"):
    """Cover, title block, seek bar, times, transport and tools, as the phone's player stacks them. `compact` (a 720dp
    window) takes the inline lyric out and tightens the gaps so all of it stays above the gesture area."""
    d = B.d
    col = d.frame(parent, name="Player", layout="v", x=x - 24, y=y, w=col_w + 48, sizing=("fixed", "hug"))
    pbox = d.frame(col, name="Cover pager box", layout="v", pad=[0 if compact else 4, 24, 0, 24], sizing=("fill", "hug"))
    cover_pager(d, pbox, side, peek=col_w - side - 8)
    tb = inst(d, "Now playing/Title block", col, name="Title block", Lyric=str(lyric))
    d.set(tb, sizingH="fill")
    if lyric:
        setp(d, tb, Lyric_line="carry what we couldn't say")
    setp(d, tb, Title="Lights Over the Water", Artist="Mira Okafor")
    if compact:
        d.set(tb, paddingTop=16)
    for name, comp_name, pad, vp in (
            ("Seek bar", "Seek bar", [8 if compact else 10, 24, 0, 24], dict(Playing="True")),
            ("Seek times", "Seek times", [0, 24, 0, 24], None),
            ("Transport", "Transport", [12 if compact else 22, 24, 0, 24], dict(Playing="True")),
            ("Tools", "Now playing tools", [12 if compact else 20, 24, 8 if compact else 24, 24], dict(Shuffle="Off", Repeat="Off"))):
        box = d.frame(col, name=f"{name} box", layout="v", pad=pad, sizing=("fill", "hug"))
        i = inst(d, comp_name, box, name=name, **(vp or {}))
        d.set(i, sizingH="fill")
        if name == "Seek bar":
            set_progress(d, i, PROGRESS)
    return col


def queue_list(B: "Builder", parent, n=11):
    """The queue as ExpressiveQueue lists it: Now playing, then Up next with Clear and the rows with drag handles."""
    d = B.d
    lab = d.instance("List label", parent, name="Now playing label")
    d.set(lab, sizingH="fill")
    setp(d, lab, Label="Now playing")
    box = d.frame(parent, name="Now playing group", layout="v", pad=[0, 16, 0, 16], sizing=("fill", "hug"))

    def cell(holder, pos, title, artist, cov, state="Default", drag=True):
        c = inst(d, "List group/Cell", holder, name=f"{title} cell", Position=pos, Surface="Default")
        d.set(c, sizingH="fill")
        r = d.child(c, "Row")
        d.set(r, name=title)
        setp(d, r, Lead="Cover", State=state)
        setp(d, r, Title=title, Subtitle=artist)
        if drag:
            setp(d, r, Trailing=d.comp["List row/Drag handle"])
        set_cover(d, r, cov)
        return c

    cell(box, "Single", "Lights Over the Water", "Mira Okafor", 0, state="Current playing", drag=False)
    rows = sp.QUEUE[:n]
    head = d.frame(parent, name="Up next heading", layout="h", pad=[0, 20, 0, 0], cross="max", sizing=("fill", "hug"))
    ul = d.instance("List label", head, name="Up next label")
    d.set(ul, sizingH="fill")
    setp(d, ul, Label=f"Up next, {len(sp.QUEUE)} songs")
    cl = inst(d, "Button", head, name="Clear", Style="Text", Size="Small", Icon="None", State="Enabled", Squared="False")
    setp(d, cl, Label="Clear")
    grp = d.frame(parent, name="Up next", layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    for i, (t, ar, cv) in enumerate(rows):
        cell(grp, "Top" if i == 0 else ("Bottom" if i == len(rows) - 1 else "Middle"), t, ar, cv)


def lyric_lines(B: "Builder", parent, *, w, anchor: float, height: float, x=0):
    """Synced lines in a column `w` wide, the line being sung `anchor` down from the parent's top. The parent is the
    viewport, `height` tall; a line it would cut through is left out of the drawing (opacity 0), so the frame ends on
    whole lines where the app fades them."""
    d = B.d
    col = d.frame(parent, name="Lines", layout="v", w=w, x=x, y=0, sizing=("fixed", "hug"))
    cur, lines = None, []
    for i, line in enumerate(sp.LYRICS):
        st = "Current" if i == sp.CURRENT else ("Sung" if i < sp.CURRENT else "Upcoming")
        li = inst(d, "Lyrics/Screen line", col, name=f"Lyric {i + 1}", State=st)
        d.set(li, sizingH="fill")
        setp(d, li, Text=line)
        lines.append(li)
        if i == sp.CURRENT:
            cur = li
    y0 = round(anchor - d.get(cur)["y"], 1)
    d.set(col, y=y0)
    for li in lines:
        g = d.get(li)
        top = y0 + g["y"]
        if (top < 0 < top + g["height"]) or (top < height < top + g["height"] - 16):
            d.set(li, opacity=0)
    return col


def now_playing_wide(B: "Builder", s: "Screen", panel: str):
    d = B.d
    d.set_mode(s.f, "Color", "Cover")
    half = s.W / 2
    compact = s.H < 840
    avail = s.H - GESTURE_H - STATUS_H - 64
    fixed = (16 + 62 + 8 + 44 + 15 + 12 + 80 + 12 + 56 + 8) if compact else (4 + 118 + 10 + 44 + 15 + 22 + 80 + 20 + 56 + 24)
    side = float(min(440, int((avail - fixed) // 8) * 8))
    col_w = side + 80
    hd = inst(d, "Now playing header", s.f, name="Header", x=0, y=STATUS_H)
    d.set(hd, width=half)
    setp(d, hd, Context="Paper Lanterns")
    B.links.append((s.key, d.child(hd, "Close"), None, "back"))
    player_column(B, s.f, x=round((half - col_w) / 2), y=STATUS_H + 64, col_w=col_w, side=side, lyric=not compact and panel == "Up next",
                  compact=compact)
    # The end half: the switch, then the queue or the lyrics.
    pane = d.frame(s.f, name="Side panel", layout="v", x=half, y=STATUS_H, w=half - EXTRA, h=s.H - STATUS_H, clip=True,
                   sizing=("fixed", "fixed"))
    top = d.frame(pane, name="Switch row", layout="h", pad=[8, 16, 8, 16], cross="center", sizing=("fill", "hug"))
    inst(d, "Now playing/Side panel", top, name="Side panel switch", Selected=panel)
    sp_ = spacer(d, top, "Space", w=1, h=1, fill_h=True)
    if panel == "Up next":
        queue_list(B, pane)
    else:
        op = inst(d, "Icon button", top, name="Open lyrics", Style="Standard", Size="Default", Width="Default", State="Enabled")
        swap_icon(d, op, "open_in_full")
        view = d.frame(pane, name="Lyrics", clip=True, sizing=("fill", "fill"))
        ph = s.H - STATUS_H - 64
        lyric_lines(B, view, w=half - EXTRA - 48, x=24, anchor=round(ph / 3), height=ph)
    s.bars()


def build_now_playing(B: "Builder"):
    d = B.d
    fr = tk.Frames(d, B.ctx, "Now Playing", "now_playing",
                   note="Now Playing covers the window, on the cover's scheme (Color mode Cover). Expanded and landscape: "
                        "the player in the start half, the cover as large as the height allows with the next cover "
                        "after it, and a panel in the end half that switches between Up next and Lyrics. Upright: the "
                        "phone's player in a centred column 560dp wide.")
    for size in ("landscape", "large"):
        s = Screen(B, fr, f"now_playing_up_next_{size}", "Now Playing, Up next", size, NPW)
        now_playing_wide(B, s, "Up next")
        if size == "landscape":
            s.notes.append("At 720dp tall the cover is 280dp, the title block leaves out the line being sung and the "
                           "gaps tighten, so the tools row ends above the gesture area.")
        s.done()
    s = Screen(B, fr, "now_playing_lyrics_landscape", "Now Playing, Lyrics", "landscape", NPW)
    now_playing_wide(B, s, "Lyrics")
    s.notes.append("The panel shows the synced lyrics, the line being sung a third of the way down; the button at the "
                   "end of the switch row opens them full screen. The words are the demo library's.")
    s.done()
    # Upright: the phone layout in a centred column.
    s = Screen(B, fr, "now_playing_portrait", "Now Playing", "portrait", SRC + "ExpressiveNowPlaying.kt · ExpressiveNowPlaying")
    d.set_mode(s.f, "Color", "Cover")
    col_w = 560
    x = round((s.W - col_w) / 2)
    hd = inst(d, "Now playing header", s.f, name="Header", x=x - 24, y=STATUS_H)
    d.set(hd, width=col_w + 48)
    setp(d, hd, Context="Paper Lanterns")
    B.links.append((s.key, d.child(hd, "Close"), None, "back"))
    pl = player_column(B, s.f, x=x, y=STATUS_H + 64, col_w=col_w, side=480, lyric=True, compact=False)
    below = d.frame(s.f, name="Below the player", layout="v", x=x - 8, y=STATUS_H + 64 + d.get(pl)["height"], w=col_w + 16,
                    gap=12, pad=[0, 16, 0, 16], sizing=("fixed", "hug"))
    lc = inst(d, "Lyrics/Card", below, name="Lyrics card", State="Synced")
    d.set(lc, sizingH="fill")
    # As in the shell on a medium window, the gesture inset is a band of the page colour.
    d.rect(s.f, name="Gesture inset", w=s.W, h=GESTURE_H, x=0, y=s.H - GESTURE_H, fill="Color/surface")
    s.bars()
    s.notes.append("The cover is 480dp with the next cover after it; the lyrics card, About the artist and Credits "
                   "follow below as on the phone.")
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── full-screen lyrics
def build_lyrics(B: "Builder"):
    d = B.d
    fr = tk.Frames(d, B.ctx, "Lyrics", "lyrics",
                   note="Full-screen lyrics on an expanded landscape window: a 360dp start pane with the cover, title, "
                        "artist, seek bar and transport, and the lines in the rest in a column 640dp wide.")
    s = Screen(B, fr, "lyrics_landscape", "Lyrics", "landscape", SRC + "ExpressiveLyrics.kt · ExpressiveLyrics")
    d.set_mode(s.f, "Color", "Cover")
    top = STATUS_H + 8
    card_w = 360 - tk.MARGIN - 8
    card = d.frame(s.f, name="Player pane", layout="v", x=tk.MARGIN, y=top, w=card_w, h=s.H - top - GESTURE_H - 8, pad=16,
                   fill="Color/surface-container", clip=True, sizing=("fixed", "fixed"))
    bind_dim(d, card, "cornerRadius", "radius/extra-large")
    side = card_w - 32
    close = inst(d, "Icon button", card, name="Close", Style="Standard", Size="Default", Width="Default", State="Enabled")
    swap_icon(d, close, "keyboard_arrow_down")
    spacer(d, card, "Close spacing", w=1, h=4)
    cv = inst(d, "Cover", card, name="Cover", Kind="Album", Size="208")
    d.set(cv, width=side, height=side)
    bind_dim(d, cv, "cornerRadius", "radius/extra-large")
    set_cover(d, cv, 0)
    titles = d.frame(card, name="Titles", layout="v", pad=[16, 0, 0, 0], sizing=("fill", "hug"))
    txt(d, titles, "Lights Over the Water", "Title/Large Emphasized", "Color/on-surface", "Title", fill=True, lines=1)
    txt(d, titles, "Mira Okafor", "Body/Large", "Color/on-surface-variant", "Artist", fill=True, lines=1)
    spacer(d, card, "Seek spacing", w=1, h=8)
    sk = inst(d, "Seek bar", card, name="Seek bar", Playing="True")
    d.set(sk, sizingH="fill")
    set_progress(d, sk, PROGRESS)
    st = d.instance("Seek times", card, name="Seek times")
    d.set(st, sizingH="fill")
    spacer(d, card, "Transport spacing", w=1, h=16)
    tr = inst(d, "Transport", card, name="Transport", Playing="True")
    d.set(tr, sizingH="fill")
    # The lines, in a viewport that stops under the status bar and above the gesture area.
    x0 = 360
    view = d.frame(s.f, name="Lyrics", x=x0, y=top, w=s.W - x0, h=s.H - top - GESTURE_H, clip=True)
    lw = 640
    lyric_lines(B, view, w=lw, x=round((s.W - x0 - lw) / 2), anchor=round((s.H - top) / 3), height=s.H - top - GESTURE_H)
    s.bars()
    s.notes.append("The words are the demo library's, written for the store shots. The line being sung sits a third of "
                   "the way down.")
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Settings
def settings_screen(B: "Builder", s: "Screen"):
    d = B.d
    col = s.content()
    bar = inst(d, "Top app bar/Detail", col, name="App bar", Surface="Plain")
    setp(d, bar, Show_action=False)
    d.set(d.child(bar, "Title"), visible=False)
    stretch(d, bar, None, "Status bar inset", "Bar")
    d.set(d.child(bar, "Status bar inset"), height=STATUS_H)
    w = min(s.inner, 720 + 32)
    holder = d.frame(col, name="Settings column", layout="h", main="center", sizing=("fill", "hug"))
    lst, _ = ss.settings_content(d, B.ctx, holder, 0)
    d.set(lst, width=w)
    kids = [n["id"] for n in d.get(lst, depth=1).get("children", [])]
    d.set(kids, sizingH="fill")
    d.set(d.child(d.child(lst, "Large title"), "Title"), sizingH="fill")
    for name in ("Theme cell",):
        row = d.child(d.child(lst, name), "Row")
        d.set(row, sizingH="fill")
    return col


def build_settings(B: "Builder"):
    d = B.d
    fr = tk.Frames(d, B.ctx, "Settings", "settings",
                   note="Settings keeps the rail and the player. Its bar with back spans the content area; the list is a "
                        "centred column at most 720dp wide.")
    src = SRC + "ExpressiveSettings.kt · ExpressiveSettings"
    for size in ("landscape", "portrait"):
        s = Screen(B, fr, f"settings_{size}", "Settings", size, src)
        settings_screen(B, s)
        s.shell()
        s.notes.append("The list is the one on the Screens page (ui-redesign at 4fe4dab). The Playback section this "
                       "branch adds is not in the design file yet.")
        s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── overlays
def build_overlays(B: "Builder"):
    d = B.d
    fr = tk.Frames(d, B.ctx, "Overlays", "overlays",
                   note="Sheets are at most 640dp wide, centred on the window and anchored to its bottom edge. Dialogs "
                        "are centred; a dialog with one field is 400dp wide. Each is drawn over the screen it opens "
                        "from, rebuilt from instances under the scrim.")
    # Track menu over the album.
    s = Screen(B, fr, "sheet_track_menu_landscape", "Track menu over the album", "landscape",
               SRC + "ExpressiveSheets.kt · ExpressiveTrackMenu")
    collection(B, s, album_spec(), pane_x=s.x0, pane_w=s.cw, lead=tk.MARGIN, own=True)
    s.shell(links=False)
    sc = inst(d, "Scrim", s.f, name="Scrim", Kind="Sheet", x=0, y=0)
    d.set(sc, width=s.W, height=s.H)
    d.set(d.child(sc, "Dim"), width=s.W, height=s.H)
    m = inst(d, "Sheet/Track menu", s.f, name="Track menu", **{"Opened from": "Library", "Sleep timer": "None"})
    sw = 640
    d.set(m, width=sw)
    for cell_ in ("Favourite cell", "Add to playlist cell", "Language cell", "Go to album cell", "Go to artist cell", "Share cell"):
        d.set(d.child(m, cell_), sizingH="fill")
    set_nested_text(d, m, "Header/Title", "Porch Light")
    set_nested_text(d, m, "Header/Subtitle", "Mira Okafor")
    d.set(d.child(m, "Navigation bar inset"), height=GESTURE_H)
    d.set(m, x=round((s.W - sw) / 2), y=round(s.H - d.get(m)["height"], 1))
    g = d.instance("Gesture bar", s.f, name="Gesture bar over the sheet", x=0, y=s.H - GESTURE_H)
    d.set(g, width=s.W, height=GESTURE_H)
    s.notes.append("Opened from Porch Light's row in the album. The scrim covers the rail and the player bar too.")
    s.done()
    # New playlist over the Library.
    s = Screen(B, fr, "dialog_new_playlist_landscape", "New playlist over the Library", "landscape",
               SRC + "ExpressiveSheets.kt · ExpressivePlaylistNameDialog", tab="Library")
    library_landscape(B, s, "Playlists", "Long drive", playlist_spec())
    s.shell(links=False)
    sc = inst(d, "Scrim", s.f, name="Scrim", Kind="Dialog", x=0, y=0)
    d.set(sc, width=s.W, height=s.H)
    d.set(d.child(sc, "Dim"), width=s.W, height=s.H)
    dl = inst(d, "Dialog", s.f, name="Dialog", Content="Text field", Buttons="Two", Confirm="Disabled")
    dw = 400
    d.set(dl, width=dw)
    setp(d, dl, Title="New playlist")
    tf = d.child(dl, "Text field")
    setp(d, tf, Label="Playlist name")
    d.set(tf, sizingH="fill")
    d.set(d.child(tf, "Container"), width=dw - 48)
    d.cmd("instance.setProperties", ids=[d.child(dl, "Confirm")], properties={"Label": "Create"})
    d.cmd("instance.setProperties", ids=[d.child(dl, "Dismiss")], properties={"Label": "Cancel"})
    d.set(dl, x=round((s.W - dw) / 2), y=round((s.H - d.get(dl)["height"]) / 2))
    s.notes.append("Opened from New playlist in the list pane, the Playlists chip on and Long drive open beside the "
                   "list. Create stays disabled until there is a name. The dialog is centred on the window.")
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── First run
def look_card(d: Doc, parent, style: str, selected: bool, width: float):
    """A look card at the width its row gives it. LookPreview lays the picture out at the card's width over 0.42 and
    draws it at 0.42, so a wider card shows a wider picture with larger covers; the picture's layers follow."""
    c = inst(d, "First run/Look card", parent, name=style, Style=style, Selected=str(selected))
    d.set(c, width=width)
    inner = width - 16
    pic = d.child(c, f"{style} picture")
    pad = 6.72 if style == "Expressive" else 10.08
    gap = 5.04 if style == "Expressive" else 5.88
    old = d.get(d.child(pic, "Cover 1"))["height"]
    side = round((inner - 2 * pad - 2 * gap) / 3, 2)
    d.set(pic, width=inner)
    for n in (1, 2, 3):
        d.set(d.child(pic, f"Cover {n}"), height=side)
    pv = d.child(c, "Preview")
    d.set(pv, height=round(d.get(pv)["height"] + side - old, 2))
    return c


def build_first_run(B: "Builder"):
    d = B.d
    fr = tk.Frames(d, B.ctx, "First run", "first_run",
                   note="The first run has no rail and no player. Each step is a centred column at most 560dp wide; the "
                        "two look cards stay side by side and share its width, and the button sits at the column's "
                        "width above the gesture area.")
    src = SRC + "ExpressiveOnboarding.kt · ExpressiveOnboarding"
    for size in ("landscape", "portrait"):
        s = Screen(B, fr, f"first_run_look_{size}", "First run, Look", size, src)
        cw = 560
        x = round((s.W - cw) / 2)
        col = d.frame(s.f, name="Step", layout="v", x=x, y=STATUS_H, w=cw, sizing=("fixed", "hug"))
        fb = inst(d, "Top app bar/First run", col, name="First run bar", Step="2")
        d.set(fb, sizingH="fill")
        for text, style, color, top, nm in (("Pick your look", "Kit/Player title", "Color/on-surface", 8, "Headline"),
                                            ("You can change it any time in Settings.", "Body/Large",
                                             "Color/on-surface-variant", 14, "Body")):
            box = d.frame(col, name=nm, layout="v", pad=[top, 24, 0, 24], sizing=("fill", "hug"))
            txt(d, box, text, style, color, nm, fill=True)
        row = d.frame(col, name="Styles", layout="h", gap=12, pad=[24, 16, 0, 16], sizing=("fill", "hug"))
        cardw = round((cw - 32 - 12) / 2, 2)
        look_card(d, row, "Expressive", True, cardw)
        look_card(d, row, "Comfort", False, cardw)
        sec = inst(d, "Settings section", col, name="Section Theme")
        setp(d, sec, Label="Theme")
        d.set(sec, sizingH="fill")
        th = inst(d, "Connected button group", col, name="Theme", Options="3", Selected="3", On_cell="False")
        d.set(th, sizingH="fill")
        bb = d.frame(s.f, name="Bottom button", layout="h", pad=[8, 16, 24, 16], w=cw, x=x, y=s.H - GESTURE_H - 24 - 56 - 8,
                     sizing=("fixed", "hug"))
        b = inst(d, "Button", bb, name="Continue", Style="Filled", Size="Medium", Icon="None", State="Enabled", Squared="False")
        setp(d, b, Label="Continue")
        d.set(b, sizingH="fill")
        s.bars()
        s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Cover and Changelog
VERSION, UPDATED = "1.1.0", "2026-11-25"
CHANGES = ("Tablet layouts, proposed. Nothing in this entry is in the code yet; the frames are the drawing the "
           "layouts will be built to (docs/design/2026-11-25-tablet/SPEC.md).\n"
           "• Tablet page, after Screens: {frames} frames in Light and Dark at Landscape (1152 × 720, the Pad sideways), "
           "Portrait (720 × 1152) and Large landscape (1440 × 900): Home, Library as list and detail, Search results, "
           "album, artist and playlist in two panes, Now Playing with Up next or Lyrics beside the player, full-screen "
           "lyrics, Settings, the track menu, the New playlist dialog and the first run's look step. Each frame carries "
           "a Proposed annotation and no ready-for-development status.\n"
           "• Components / Navigation: Navigation rail and Player bar.\n"
           "• Components / Playback: Now playing/Side panel, the Up next and Lyrics switch.\n"
           "The phone screens are unchanged and still match ui-redesign at 4fe4dab.")


def build_cover_changelog(d: Doc, ctx: dict):
    """Version 1.1.0 on the Cover and at the top of the Changelog. An entry an earlier run added is replaced."""
    d.page("Cover")
    for i in d.find(d.pages["Cover"], name="Chip · ", limit=20):
        nm = d.get(i).get("name", "")
        if nm.startswith("Chip · Version"):
            d.cmd("instance.setProperties", ids=[i], properties={"Label": f"Version {VERSION}"})
            d.set(i, name=f"Chip · Version {VERSION}")
        elif nm.startswith("Chip · Last updated"):
            d.cmd("instance.setProperties", ids=[i], properties={"Label": f"Last updated {UPDATED}"})
            d.set(i, name=f"Chip · Last updated {UPDATED}")
    d.page("Changelog")
    col = [n["id"] for n in d.get(d.pages["Changelog"], depth=1).get("children", []) if n.get("name") == "Entries"][0]
    for n in d.get(col, depth=1).get("children", []):
        if n.get("name") == f"Entry · {VERSION}":
            d.cmd("edit.delete", ids=[n["id"]])
    before = d.get(col)["height"]
    e = d.instance("Changelog entry", col, name=f"Entry · {VERSION}")
    frames = sum(1 for s in d.screens.values() if s.get("page") == tk.PAGE)
    d.cmd("instance.setProperties", ids=[e], properties={
        "Version": VERSION, "Date": UPDATED, "Author": "Attic maintainers", "Changes": CHANGES.format(frames=frames)})
    d.set(e, sizingH="fixed", width=1400)
    d.cmd("node.reorder", id=e, index=0)
    # The card under the entries moves down by what the column grew.
    grew = d.get(col)["height"] - before
    cy = d.get(col)["y"]
    for n in d.get(d.pages["Changelog"], depth=1).get("children", []):
        if n["id"] != col and n.get("y", 0) > cy + before - 1 and n.get("x", 0) < 1500:
            d.set(n["id"], y=n["y"] + grew)
    ctx["exports"]["changelog-entry"] = e
    ctx["exports"]["cover"] = [n["id"] for n in d.get(d.pages["Cover"], depth=1).get("children", [])][0]


AREAS = {"home": build_home, "library": build_library, "search": build_search, "collections": build_collections,
         "now_playing": build_now_playing, "lyrics": build_lyrics, "settings": build_settings, "overlays": build_overlays,
         "first_run": build_first_run}


class _Identity(dict):
    """Component property names in a saved file are their plain labels; the generator's helpers look them up."""

    def __missing__(self, key):
        return key


def build(d: Doc, ctx: dict):
    import time
    only = ctx.get("only")
    for c in ctx["components"].values():
        c["props"] = _Identity(c.get("props") or {})
    t = time.time()
    build_components(d, ctx)
    print(f"  components {time.time() - t:6.1f}s", flush=True)
    tk.reset_page(d)
    B = Builder(d, ctx)
    for key, fn in AREAS.items():
        if only and key not in only:
            continue
        t = time.time()
        fn(B)
        print(f"  {key:12s} {time.time() - t:6.1f}s", flush=True)
    print(f"  prototype links: {B.wire()}", flush=True)
    if not only or "cover" in only:
        build_cover_changelog(d, ctx)
    roots = {"Tablet page": d.pages[tk.PAGE]}
    roots.update({f"component {n}": d.comp[n] for items in ADDED.values() for n, _ in items})
    report = tk.hygiene(d, roots)
    ctx["hygiene"] = report
    for label, bad in report.items():
        print(f"  hygiene {label}: {len(bad)} default-named layers {bad[:8]}", flush=True)
