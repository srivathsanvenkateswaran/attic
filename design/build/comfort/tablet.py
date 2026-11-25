"""Tablet layouts of Attic Comfort: the Tablet page and the components it needs.

Run on a saved file, never as part of a regeneration (it is not in MODULES):

    cd design/build
    python3 -m figbuild.edit comfort tablet --in <copy of design/Attic Comfort.figcraft> --out <copy> \
        [--only home,library,...] [--export DIR --sheet FILE]

It draws docs/design/2026-11-25-tablet/SPEC.md: every frame of the table "Frames to draw" at Landscape (1152 x 720,
the Pad 6 sideways), Portrait (720 x 1152, the Pad 6 upright, a medium width) and Large landscape (1440 x 900),
Light above Dark, on a page named Tablet after Screens, and the tablet layouts of mood mixes and the equaliser, which
the code gained after the spec. The layouts are in the code on feature/tablet-layouts, so each frame carries its
Source annotation and is marked ready for development; where the code and a frame disagree, the code is right. It
runs on the 1.3.0 file (the equaliser and mood mixes) and gives 1.4.0: the Cover's version, a Tablet row in its Read
me and one Changelog entry on top of the entries the file has. Running it again deletes the Tablet page, the
components below, that row and that entry, and draws them again.

On the Screens page it does one thing: it deletes the Moods section's tablet frames marked In progress, which
comfort/moods.py drew as layout before the components below existed (figbuild.tabletkit.retire_placeholders). Run
comfort/moods.py with --no-tablet from now on, or it draws them again. It changes no other page.

Components / Navigation
  Side dock/Tab          one tab of ComfortSideDock (SideDock.kt)  Selected=False|True.  props Label, Icon (swap).
                         A 64 x 56 cell, the icon over its label; selected on accent-soft.
  Side dock              ComfortSideDock (SideDock.kt)  Selected=Home|Search|Library. The dock's card on its side, 80
                         wide: the Mark, then three Side dock/Tab instances.
  Player bar             ComfortPlayerBar (ComfortPlayerBar.kt)  State=Playing|Paused.  props Title, Artist, Elapsed,
                         Total.  Color mode Tinted on itself, as the mini player. Drawn 1012 wide; a frame sets the
                         Played width.
Components / Playback
  Now playing/Side panel the Up next | Lyrics switch of ComfortNowPlayingWide (ComfortNowPlayingWide.kt)
                         Selected=Up next|Lyrics. Two Sub-tab instances.

Areas (ctx["only"] keys): home, library, search, collections, now_playing, lyrics, settings, overlays, first_run,
moods (Home scrolled to the Moods shelf, the mood page sideways and upright, Search's Browse with the Moods group,
Moods and interests) and equaliser (the sheet over Now Playing, Settings > Equaliser). `components`, `retire` and
`cover` are also keys: a run with --only that leaves them out keeps what the input file has.

Geometry (dp). The side dock is 80 wide, 12 from the start edge, 12 under the status bar and 12 over the gesture
inset. A destination's column starts at 100 and stops 8 before the end edge; Comfort's components carry the phone's
16dp card margin, so cards run from 116 (24 after the dock) to 24 before the end edge. Shelves start 8 further in, as
on the phone, and stop where the column does, 8 before the end edge.
"""
from __future__ import annotations

from figbuild.kit import Doc, V
from figbuild import tabletkit as tk
from figbuild.tabletkit import STATUS_H, GESTURE_H, adaptive_columns

from comfort import screens_browse as cb
from comfort import screens_playback as cp
from comfort import screens_settings as cs
from comfort import equaliser as ceq
from comfort import moods as cm
from comfort.components_core import K, SetBuilder, Components, COVER, cover_image, glyph_drop, _set_text_prop, _swap_icon
from comfort.components_states import setp, inst, group, row
from comfort.foundations import _modes

SRC = "ui/comfort/"
# 1.4.0 is the Tablet page and its components on top of 1.3.0 (the equaliser, then mood mixes), with frames for both.
# The Tablet page was first drawn on 1.0.0 as 1.1.0 and 1.1.1, then on 1.2.0 as 1.3.0, on feature/tablet-layouts
# before each rebase; 1.1.0 and 1.2.0 are now the equaliser's numbers and 1.3.0 the mood mixes'.
VERSION, UPDATED = "1.4.0", "2026-11-25"
DOCK_X, DOCK_W, DOCK_GAP = 12, 80, 12
X0 = 100                         # a destination's column: 8 after the dock's end edge
END = 8                          # and 8 before the window's end edge
BAR_H = 76                       # the player bar's card
BAR_GAP = 12                     # over the gesture inset
MINI_W = 576                     # MiniPlayerMaxWidthDp: the floating mini player's widest
FRACTION = 78.5 / 232            # the demo song at 1:18.5 of 3:52
WASH = "Tinted wash"

NAV_PAGE, PLAY_PAGE = "Components / Navigation", "Components / Playback"
ADDED = {NAV_PAGE: ["Side dock/Tab", "Side dock", "Player bar"], PLAY_PAGE: ["Now playing/Side panel"]}
TABS = (("Home", "home"), ("Search", "search"), ("Library", "library_music"))

# Taste mixes (HomeState.tasteMixes, empty in DemoStates): named as home_mix_name_two words them, 25 songs each
# (TasteMixes.MIX_SIZE), the mosaic from four of the mix's albums.
MIXES = [("Mix: Mira Okafor, Juniper Vale and more", [0, 3, 11, 4]),
         ("Mix: Kavya Raman, Tide & Tamarind and more", [8, 7, 5, 2]),
         ("Mix: Ruhaan Sethi, Paloma Ferrer and more", [9, 6, 2, 1]),
         ("Mix: Velvet Circuit, Odile Marchetti and more", [10, 4, 1, 3])]
# The shelves from your listening (HomeShelves.kt), also empty in DemoStates: songs from the demo library.
ON_REPEAT = ["Iravu Rail", "Lights Over the Water", "Patang", "Neon Peaches", "Kadalkarai", "Drift Mode", "Azulejo",
             "Room Tone", "Kettle On", "Chhatri"]
NOT_LATELY = ["Brushes", "Geeli Mitti", "Vaasal Kolam", "Faro Apagado", "Low Tide Hymn", "Chrome Lungs", "Late Bloom",
              "Tamarind Skies", "Purani Diary", "Unsent Drafts"]
NEWLY_ADDED = ["Kadalkarai", "Tamarind Skies", "Afterglow Protocol", "Night Drive Firmware", "Lights Over the Water",
               "Porch Light", "Marina at Six", "404 Heartbeat", "Auto Rickshaw Waltz", "Chrome Lungs"]
DEMO_NOTE = ("Mixes, On repeat, Haven't heard in a while and Newly added are empty in the demo state; the frame fills "
             "them from the demo library so the layout shows them.")


def tokens(d: Doc) -> dict:
    """ctx["tokens"] as comfort/foundations.py kept it: {"Collection/name": variable id}."""
    listing = d.cmd("variable.list")
    cols = {c["id"]: c["name"] for c in listing["collections"]}
    return {f"{cols[v['collection']]}/{v['name']}": v["id"] for v in listing["variables"]}


def top_children(d: Doc, page: str) -> list[dict]:
    return d.get(d.pages[page], depth=1).get("children", [])


# ───────────────────────────────────────────────────────────────── components
def remove_components(d: Doc):
    """Delete what an earlier run added: each `Spec: <name>` card, its cells in the page's Dark preview, and the
    registrations. The Dark preview goes back up to where the first removed card stood."""
    for page, names in ADDED.items():
        kids = top_children(d, page)
        ys = []
        for name in names:
            d.comp.pop(name, None)
            d.props.pop(name, None)
            for c in kids:
                if c.get("name") == f"Spec: {name}":
                    ys.append(d.get(c["id"])["y"])
                    d.cmd("edit.delete", ids=[c["id"]])
        pv = next((c["id"] for c in kids if c.get("name") == "Dark preview"), None)
        if pv is not None:
            cells = [c["id"] for c in d.get(pv, depth=1).get("children", [])
                     if c.get("name") in {f"Preview: {n}" for n in names}]
            if cells:
                d.cmd("edit.delete", ids=cells)
            if ys:
                d.set(pv, y=min(ys))


def build_components(d: Doc, ctx: dict):
    names = [n for items in ADDED.values() for n in items]
    only = ctx.get("only")
    exports = lambda: ctx["exports"].update({"side-dock": d.comp["Side dock"], "player-bar": d.comp["Player bar"],
                                             "now-playing-side-panel": d.comp["Now playing/Side panel"]})
    if only and "components" not in only and all(n in d.comp for n in names):
        exports()
        return
    remove_components(d)
    k = K(d, ctx["tokens"])

    # ---------------------------------------------------------------- Navigation: the side dock and the player bar
    cc = Components.extend(d, NAV_PAGE)
    sb = SetBuilder(cc, "Side dock/Tab", "SideDock.kt · ComfortSideDock", "ComfortSideDock",
                    "One tab of the side dock: a 64 x 56 cell, the icon over its 12/15 label, 2dp apart, centred. "
                    "Selected: accent-soft (the selection fill of the bottom dock's tabs) with the control radius, a "
                    "filled icon and a bold label in on-accent-soft; else on-2.")
    for sel in (False, True):
        c = sb.variant({"Selected": str(sel)}, layout="v", w=64, h=56, gap=2, main="center", cross="center",
                       fill="Color/accent-soft" if sel else None, sizing=("fixed", "fixed"))
        k.bind_radius(c, "control")
        ink = "Color/on-accent-soft" if sel else "Color/on-2"
        ic = k.icon(c, "home", filled=sel, color=ink, name="Icon")
        t = k.text(c, "Home", "Component/Action go" if sel else "Component/Action", ink, "Label", lines=1)
        sb.text("Label", t, "Home")
        sb.swap("Icon", ic, "Icon/home (filled)" if sel else "Icon/home")
    sb.finish()

    sb = SetBuilder(cc, "Side dock", "SideDock.kt · ComfortSideDock", "ComfortSideDock",
                    "The dock on medium and expanded windows: its floating card turned on its side. 80dp wide on raised "
                    "with radius 30, the edge ring and Shadow/Floating, 12dp from the start edge, 12 under the status "
                    "bar and 12 over the gesture inset, so it is as tall as the window allows (652 at 720). The mark "
                    "(30dp) is 20dp from the top; Home, Search and Library follow 20dp under it as Side dock/Tab cells, "
                    "4dp apart. The player is not in it: expanded windows show the Player bar, medium ones the mini "
                    "player.")
    for tab, _ in TABS:
        c = sb.variant({"Selected": tab}, layout="v", w=DOCK_W, h=652, pad=[20, 8, 16, 8], gap=4, cross="center",
                       fill="Color/raised", stroke="Color/edge", stroke_w=1, strokeAlign="inside", clip=True,
                       sizing=("fixed", "fixed"))
        k.bind_radius(c, "float")
        d.cmd("style.apply", ids=[c], style=d.effect_styles["Shadow/Floating"])
        mf = d.frame(c, name="Mark space", layout="v", pad=[0, 0, 16, 0], cross="center", sizing=("hug", "hug"))
        d.instance("Mark", mf, name="Mark")
        for lab, lig in TABS:
            sel = lab == tab
            t = d.instance("Side dock/Tab", c, name=f"Tab: {lab}")
            d.cmd("instance.setProperties", ids=[t], properties={"Selected": str(sel)})
            _set_text_prop(d, t, "Label", lab)
            _swap_icon(d, t, f"Icon/{lig}" + (" (filled)" if sel else ""))
    sb.finish()

    sb = SetBuilder(cc, "Player bar", "ComfortPlayerBar.kt · ComfortPlayerBar", "ComfortPlayerBar",
                    "The playing song on expanded windows, in place of the dock's mini player: a floating card 76dp tall "
                    "with radius 30, the edge ring and Shadow/Floating, in line with the cards above it and 12dp over the "
                    "gesture inset. It tints itself (Color mode Tinted) and its played part is filled, mini-fill over "
                    "mini-base, as the mini player's: that fill is the bar's progress, so there is no second seek line. "
                    "Start: the cover at 52dp, title over artist. Centre: the elapsed time, previous, play or pause and "
                    "next (30, 36 and 30dp glyphs in on, 48dp targets), the length. End: favourite, queue, lyrics. The "
                    "start and end groups share the width equally, so the centre group sits in the middle. Tapping the "
                    "cover or the titles opens Now Playing.")
    for playing in (True, False):
        w = 1012
        c = sb.variant({"State": "Playing" if playing else "Paused"}, layout="h", w=w, h=BAR_H, pad=[0, 12, 0, 12], gap=16,
                       cross="center", fill="Color/mini-base", stroke="Color/edge", stroke_w=1, strokeAlign="inside",
                       clip=True, sizing=("fixed", "fixed"))
        k.bind_radius(c, "float")
        d.cmd("style.apply", ids=[c], style=d.effect_styles["Shadow/Floating"])
        d.set_mode(c, "Color", "Tinted")
        pw = round(w * FRACTION, 1)
        pr = d.rect(c, name="Played", w=pw, h=BAR_H, fill="Color/mini-fill", cornerRadius=[0, 22, 22, 0])
        k.absolute(pr, 0, 0, pw, BAR_H, {"horizontal": "left", "vertical": "topBottom"})
        song = d.frame(c, name="Song", layout="h", gap=12, cross="center", sizing=("fill", "hug"))
        cv = d.instance("Cover/Album", song, name="Cover")
        d.set(cv, width=52, height=52)
        lab = d.frame(song, name="Labels", layout="v", sizing=("fill", "hug"))
        t1 = k.text(lab, "Lights Over the Water", "Component/Mini title", "Color/on", "Title", fill=True, lines=1)
        t2 = k.text(lab, "Mira Okafor", "Component/Mini artist", "Color/mini-ink-2", "Artist", fill=True, lines=1)
        mid = d.frame(c, name="Controls", layout="h", gap=16, cross="center", sizing=("hug", "hug"))
        el = k.text(mid, "1:18", "Component/Seek time", "Color/on", "Elapsed")
        tr = d.frame(mid, name="Transport", layout="h", gap=8, cross="center", sizing=("hug", "hug"))
        for nm, lig, size in (("Previous", "skip_previous", 30), ("Play or pause", "pause" if playing else "play_arrow", 36),
                              ("Next", "skip_next", 30)):
            box = d.frame(tr, name=nm, w=48, h=48)
            ic = k.icon(box, lig, size=size, filled=True, color="Color/on", name="Icon")
            d.set(ic, x=round((48 - size) / 2, 1), y=round((48 - size) / 2 + glyph_drop(size), 2))
        to = k.text(mid, "3:52", "Component/Seek time", "Color/mini-ink-2", "Total")
        tools = d.frame(c, name="Tools", layout="h", cross="center", main="max", sizing=("fill", "hug"))
        hb = d.instance("Heart button", tools, name="Heart")
        d.cmd("instance.setProperties", ids=[hb], properties={"Tonal": "False", "Favourite": "True"})
        for nm, lig in (("Queue", "queue_music"), ("Lyrics", "lyrics")):
            ib = d.instance("Icon button", tools, name=nm)
            _swap_icon(d, ib, f"Icon/{lig}")
        sb.text("Title", t1, "Lights Over the Water")
        sb.text("Artist", t2, "Mira Okafor")
        sb.text("Elapsed", el, "1:18")
        sb.text("Total", to, "3:52")
    sb.finish()
    for v in d.get(d.comp["Player bar"], depth=1).get("children", []):
        if v.get("type") == "COMPONENT":
            cover_image(d, d.child(v["id"], "Cover"), COVER[0])
    cc.dark_preview()

    # ---------------------------------------------------------------- Playback: Now Playing's side panel switch
    cc = Components.extend(d, PLAY_PAGE)
    sb = SetBuilder(cc, "Now playing/Side panel", "ComfortNowPlayingWide.kt · ComfortNowPlayingWide", "ComfortNowPlayingWide",
                    "The switch at the top of Now Playing's end half on an expanded landscape window: two sub-tabs, Up "
                    "next (the queue) and Lyrics (the synced lyrics), the selected one bold with the accent underline. "
                    "It starts 8dp into the half, in a 60dp row under the status bar whose last 6dp are empty.")
    for sel in ("Up next", "Lyrics"):
        c = sb.variant({"Selected": sel}, layout="h", gap=2, sizing=("hug", "hug"))
        for lab in ("Up next", "Lyrics"):
            st = d.instance("Sub-tab", c, name=f"Sub-tab: {lab}")
            d.cmd("instance.setProperties", ids=[st], properties={"Selected": str(lab == sel)})
            _set_text_prop(d, st, "Label", lab)
    sb.finish()
    cc.dark_preview()
    exports()


# ───────────────────────────────────────────────────────────────── a tablet frame
class Screen:
    """One window: the content first, then the shell on top (fade, player bar or mini player, side dock, system bars)."""

    def __init__(self, T: "Tablet", fr: tk.Frames, key: str, screen: str, size: str, source: str, *, tab="Home",
                 h=None, suffix="", wash=False):
        self.T, self.d, self.fr, self.key, self.size, self.tab = T, T.d, fr, key, size, tab
        self.f, self.W, self.H = fr.frame(key, screen, size, source, h=h, suffix=suffix)
        self.wh = tk.SIZES[size][1]             # the window's height (H is taller on a full-length frame)
        self.expanded = self.W >= 840           # Material's expanded width: the player bar; below it the mini player
        self.cw = self.W - X0 - END             # the column's width: a phone screen this wide
        self.notes: list[str] = []
        self.shell_src: str | None = None       # set by shell(): the frame shows the dock and the player
        if wash:
            self.d.set_mode(self.f, "Color", WASH)

    @property
    def card(self) -> float:
        return self.cw - 32

    @property
    def bottom(self) -> float:
        """What the shell takes at the bottom (LocalBottomPadding): the bar or the mini player, its gap and the inset."""
        return (BAR_H if self.expanded else 66) + BAR_GAP + GESTURE_H

    def column(self, name="Content", *, x=X0, y=STATUS_H, w=None, parent=None) -> int:
        """A destination's scrolling column: a phone screen `w` wide, its blocks centred as the phone's are."""
        return self.d.frame(parent or self.f, name=name, layout="v", x=x, y=y, w=w or self.cw, cross="center",
                            sizing=("fixed", "hug"))

    def shell(self, *, player=True, dock=True, bars=True):
        d, f, W, H = self.d, self.f, self.W, self.H
        T = self.T
        if player or dock:
            self.shell_src = SRC + "ComfortShellWide.kt · ComfortWideShell"
        if player and self.expanded:
            # Content fades to the ground over the 36dp above the bar, as it does above the phone's dock.
            top = H - GESTURE_H - BAR_GAP - BAR_H
            T.fade(f, x=DOCK_X + DOCK_W, y=top - 36, w=W - DOCK_X - DOCK_W, h=H - top + 36, stop=36 / (H - top + 36),
                   name="Fade under the player bar")
            bw = W - (X0 + 16) - 24
            b = d.instance("Player bar", f, name="Player bar", x=X0 + 16, y=top)
            d.set(b, width=bw)
            T.player_bar(b, bw)
            T.triggers.append((self.key, self.size, d.child(b, "Song"), "player", None))
        elif player:
            # The dock's card alone, holding the mini player, over the dock's own fade.
            dk = d.instance("Dock", f, name="Mini player dock")
            d.cmd("instance.setProperties", ids=[dk], properties={"Tab": "None", "Mini player": "True"})
            area = W - DOCK_X - DOCK_W
            side = round((area - min(MINI_W, area - 48)) / 2, 1)
            d.set(dk, width=area, paddingLeft=side, paddingRight=side, paddingBottom=GESTURE_H + BAR_GAP)
            m = d.child(dk, "Mini player")
            d.cmd("instance.setProperties", ids=[m], properties={"Placement": "Dock", "Playing": "True", "Favourite": "True",
                                                                  "Art": "Image"})
            d.set(m, sizingH="fill")
            cover_image(d, d.child(m, "Cover"), COVER[0])
            mw = d.get(m)["width"]
            d.set(d.child(m, "Played"), width=round(mw * FRACTION, 1))
            d.set(dk, x=DOCK_X + DOCK_W, y=H - d.get(dk)["height"])
            T.triggers.append((self.key, self.size, m, "player", None))
        if dock:
            sd = d.instance("Side dock", f, name="Side dock", x=DOCK_X, y=STATUS_H + DOCK_GAP)
            d.cmd("instance.setProperties", ids=[sd], properties={"Selected": self.tab})
            d.set(sd, height=self.wh - STATUS_H - GESTURE_H - 2 * DOCK_GAP)
            for lab, _ in TABS:
                T.triggers.append((self.key, self.size, d.child(sd, f"Tab: {lab}"), "tab", lab))
        if bars:
            self.bars()

    def bars(self):
        d, f, W, H = self.d, self.f, self.W, self.H
        g = d.instance("Gesture bar", f, name="Gesture bar", x=0, y=H - GESTURE_H)
        d.set(g, width=W, height=GESTURE_H)
        s = d.instance("Status bar", f, name="Status bar", x=0, y=0)
        d.set(s, width=W, height=STATUS_H)

    def done(self):
        self.fr.annotate(self.key, *self.notes, shell=self.shell_src)


class Tablet:
    """Builders shared by the areas, over the phone modules' helpers."""

    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.b = cb.B(d, ctx)                  # screens_browse's builder: rows, tiles, cards, art
        self.ps = cp.S(d, ctx)                 # screens_playback's: slots, covers
        self.k = self.b.k
        self.triggers: list[tuple] = []        # (frame key, size, Light layer, "tab" | "player" | "open", target)

    # ---------------------------------------------------------------- small parts
    def fade(self, parent, *, x, y, w, h, stop=1.0, top_clear=True, name="Fade"):
        return self.ps.fade(parent, x=x, y=y, w=w, h=h, top_clear=top_clear, stop=stop, name=name)

    def player_bar(self, bar: int, width: float):
        d = self.d
        d.set(d.child(bar, "Played"), width=round(width * FRACTION, 1))
        cover_image(d, d.child(bar, "Cover"), COVER[0])

    def wide(self, node: int, width: float):
        self.d.set(node, width=width)
        return node

    def hero(self, col, title: str, s: Screen):
        """Hero without the mark (the dock carries it): the short one in a window under 840dp tall."""
        h = self.b.inst("Hero", col, name="Hero", props={"Mark": "False", "Short": str(s.wh < 840), "Title": title,
                                                         "Show subtitle": False})
        return self.wide(h, s.cw)

    def label(self, col, text, width, small=None, tight=False):
        return self.wide(self.b.label(col, text, small=small, tight=tight), width)

    def label_with(self, col, text, kind, width, sort=None):
        """b.label_with at a width: the label, and the Label control 14dp past the label's 28dp inset."""
        d, b = self.d, self.b
        box = d.frame(col, name=f"Label row: {text}", layout="v", w=width, sizing=("fixed", "hug"))
        self.wide(b.label(box, text), width)
        p = {"Kind": kind}
        if sort:
            p["Sort"] = sort
        c = b.inst("Label control", box, name="View toggle" if kind != "Sort" else "Sort", props=p)
        cw = d.get(c)["width"]
        d.set(c, absolute=True)
        d.set(c, x=width - 28 + 14 - cw, y=24 + 9 - 24, constraints={"horizontal": "right", "vertical": "top"})
        return box, c

    def gap(self, col, h, name=None):
        return self.d.frame(col, name=name or f"Space {h}", w=10, h=h)

    def shelf(self, col, makers, width, name="Shelf", pad=24):
        """A shelf that scrolls sideways: it starts 24 in and is cut where the column ends, 8 before the window's end
        edge (the code's shelves are rows of the destination's column and don't bleed past it)."""
        d = self.d
        s = d.frame(col, name=name, layout="h", gap=14, pad=[0, 0, 0, pad], w=width, clip=True, sizing=("fixed", "hug"),
                    cross="min")
        for m in makers:
            m(s)
        return s

    def cards(self, col, makers, columns: int, width: float, name="Card", gap=12):
        """Rows in `columns` group cards side by side, filled top to bottom; `makers` are callables(parent, index in
        its column) -> row. `width` is the cards' whole width (the column less its margins)."""
        d, b = self.d, self.b
        if columns <= 1:
            return b.rows_card(col, [lambda par, m=m, i=i: m(par, i) for i, m in enumerate(makers)], width=width, name=name)[0]
        wrap = d.frame(col, name=f"{name} columns", layout="h", gap=gap, w=width, sizing=("fixed", "hug"), cross="min")
        per = -(-len(makers) // columns)
        cw = round((width - gap * (columns - 1)) / columns, 2)
        for c in range(columns):
            part = makers[c * per:(c + 1) * per]
            if part:
                b.rows_card(wrap, [lambda par, m=m, i=i: m(par, i) for i, m in enumerate(part)], width=cw,
                            name=f"{name} {c + 1}")
        return wrap

    def song_shelf(self, col, title, names, s: Screen):
        b = self.b
        self.label(col, title, s.cw)
        return self.shelf(col, [lambda par, t=cb.track(n): b.tile(par, "Recent", t.title, t.artist, [t.cover],
                                                                    now=t.title == cb.NOW) for n in names],
                          s.cw, name=f"{title} shelf")

    def tray(self, col, items, width, art, name, columns=6):
        """A tray card: tiles in equal cells, `columns` across. items: (kind, title, subtitle, covers, art kind)."""
        d, b = self.d, self.b
        tray = d.frame(col, name="Tray grid", layout="v", gap=14, w=width - 28, sizing=("fixed", "hug"))
        for r in range(0, len(items), columns):
            rw = d.frame(tray, name=f"Tray row {r // columns + 1}", layout="h", gap=6, sizing=("fill", "hug"), cross="min")
            part = items[r:r + columns]
            for kind, title, sub, covers, art_kind in part:
                t = b.tile(rw, kind, title, sub, covers=covers, art_kind=art_kind, fill=True, size=art,
                           name=f"{kind}: {title}")
                if art_kind == "favourites":
                    d.set(b.nested(t, "Icon box"), width=art, height=art)
                    d.set(b.nested(t, "Cover"), visible=False)
            for _ in range(columns - len(part)):
                d.frame(rw, name="Empty cell", h=1, sizing=("fill", "fixed"))
        g = b.card(col, pad=True, width=width, name=name)
        b.fill_slot(g, [tray])
        return g


# ───────────────────────────────────────────────────────────────── Home
def home_content(T: Tablet, s: Screen, col, *, full: bool):
    d, b = T.d, T.b
    W, CARD = s.cw, s.card
    T.hero(col, "Home", s)
    tl = T.wide(b.inst("Tools", col, name="Tools", props={"Leading": "Sub-tabs"}), W)
    for lab in ("All", "Albums", "Artists", "Folders"):
        b.props(b.nested(tl, f"Sub-tab: {lab}"), Selected=str(lab == "All"))
    art = 112 if s.expanded else 72
    shortcuts = [("Shortcut", "Favourites", None, [], "favourites"), ("Shortcut", "Paper Lanterns", None, [0], "album"),
                 ("Shortcut", "Iravu Rail", None, [8], "album"), ("Shortcut", "Chhat Pe Dhoop", None, [9], "album"),
                 ("Shortcut", "Glasshouse Summer", None, [3], "album"),
                 ("Shortcut", "Afterglow Protocol", None, [10], "album")]
    T.tray(col, shortcuts, CARD, art, "Shortcuts")
    T.gap(col, 12)
    T.wide(b.inst("Recap entry", col, name="Recap entry"), CARD)

    # Instant mixes: the pager's pages side by side, three equal columns.
    T.label(col, "Instant mixes", W)
    T.cards(col, [lambda par, i, t=cb.track(n): b.song_row(par, t, i, trailing="Radio", current=False) for n in cb.MIXES],
            3 if s.expanded else 2, CARD, name="Instant mixes")

    T.label(col, "Mixes", W, small="Songs you play, grouped by sound")
    T.shelf(col, [lambda par, name=name, covers=covers: b.tile(par, "Album", name, "25 songs", covers, name=f"Mix: {name[5:]}",
                                                                 lines={"Title": 2}) for name, covers in MIXES],
            W, name="Mixes shelf")
    T.song_shelf(col, "Recently played", cb.RECENT, s)
    if not full:
        return
    T.song_shelf(col, "On repeat", ON_REPEAT, s)
    T.song_shelf(col, "Haven't heard in a while", NOT_LATELY, s)
    T.song_shelf(col, "Newly added", NEWLY_ADDED, s)
    T.label(col, "Start listening", W, small="Picked at random from your songs")
    T.cards(col, [lambda par, i, t=cb.track(n): b.song_row(par, t, i) for n in cb.START], 2 if s.expanded else 1, CARD,
            name="Start listening")
    T.label(col, "Your folders", W)
    T.tray(col, [("Folder", f, cb.songs(len(cb.folder_tracks(f))), cb.mosaic(cb.folder_tracks(f)), "album")
                 for f in cb.FOLDERS], CARD, art, "Your folders")
    T.label(col, "Albums", W)
    T.shelf(col, [lambda par, a=a: b.tile(par, "Album", a[0], a[1], [a[4]], name=f"Album: {a[0]}") for a in cb.BIGGEST_ALBUMS],
            W, name="Albums shelf")
    T.gap(col, 28)
    hold = d.frame(col, name="Rows", layout="v", w=CARD, sizing=("fixed", "hug"))
    r1 = b.coll_row(hold, "Artist", "Mira Okafor", None, [0], size="Small", chevron=True, overline="More from")
    inset = d.frame(hold, name="Inset", layout="v", pad=[14, 0, 16, 0], sizing=("fill", "hug"))
    hl = d.rect(inset, name="Hairline", w=CARD - 40, h=1, fill="Color/line")
    T.k.absolute(hl, 20, 0, CARD - 40, 1, {"horizontal": "leftRight", "vertical": "top"})
    sh = d.frame(inset, name="Shelf", layout="h", gap=14, pad=[0, 16, 0, 16], sizing=("fill", "hug"), clip=True, cross="min")
    for a in ("Paper Lanterns", "Weather Report"):
        al = cb.ALBUM[a]
        b.tile(sh, "More from", a, str(al[2]), [al[4]], name=f"Album: {a}")
    g = b.card(col, width=CARD, name="More from")
    b.fill_slot(g, [r1, inset])
    d.cmd("edit.delete", ids=[hold])
    T.label(col, "Top artists", W)
    T.shelf(col, [lambda par, n=n, ts=ts: b.tile(par, "Artist", n, covers=[ts[0].cover], art_kind="artist",
                                                 name=f"Artist: {n}") for n, ts in cb.BIGGEST_ARTISTS],
            W, name="Top artists shelf")


def build_home(T: Tablet):
    d = T.d
    fr = tk.Frames(d, T.ctx, "Home", "home",
                   note="Home on a tablet. The dock carries the mark, so the hero is the title alone, the short one in a "
                        "window under 840dp tall. The shortcuts tray is six across. Instant mixes sits in three equal "
                        "cards (two on a medium width), filled top to bottom, so the first songs head the first card; the "
                        "Recap entry stays a row across the page. Shelves show more and stop where the page's column "
                        "does, 8dp before the end edge. Start listening takes two columns on expanded widths. Upright, "
                        "a medium width, the mini player floats in the dock's card, 576dp wide.")
    src = SRC + "ComfortHome.kt · ComfortHome"
    for size in ("landscape", "portrait", "large"):
        s = Screen(T, fr, f"home_{size}", "Home", size, src)
        col = s.column()
        home_content(T, s, col, full=False)
        s.shell()
        s.notes.append(DEMO_NOTE)
        s.done()
    # The whole page, so the sections below the fold have a drawing too.
    s = Screen(T, fr, "home_large_full", "Home", "large", src, h=3000, suffix=" (full length)")
    col = s.column()
    home_content(T, s, col, full=True)
    h = round(STATUS_H + d.get(col)["height"] + s.bottom + 24)
    d.set(s.f, height=h)
    d.screens[s.key]["h"] = h
    fr.area.row_h = max(tk.SIZES[k][1] for k in ("landscape", "portrait", "large"))
    fr.area.row_h = max(fr.area.row_h, h)
    s.H = h
    s.shell()
    s.notes += [DEMO_NOTE, "The whole page from the hero to Top artists, then the player bar, on a 1440dp window."]
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Library
LIB_PANE = 400                   # the list pane of the list and detail layout: the chip row needs 372


def chips(T: Tablet, col, width, sel):
    b = T.b
    ch = T.wide(b.inst("Chip row", col, name="Chips"), width)
    for lab in ("Playlists", "Albums", "Artists", "Songs"):
        b.props(b.nested(ch, f"Chip: {lab}"), Selected=str(lab == sel), Container="Ground", Label=lab)
    return ch


def coll_rows(T: Tablet, col, items, label, width, *, toggle=True, selected=None):
    """A labelled card of collection rows, in adaptive columns at least 360 wide. `selected` gets the bed a current
    song row has, since the detail pane shows it."""
    d, b = T.d, T.b
    if toggle:
        T.label_with(col, label, "View grid", width)
    else:
        T.label(col, label, width)
    card = width - 32
    cols = adaptive_columns(card, 360, 12)

    def maker(it):
        def make(par, i):
            if it[0] != selected:
                return b.coll_row(par, it[1], it[0], it[2], it[3], i)
            # CollectionRow has no selected state on the phone: the row sits on the bed SongRow draws for the current
            # song (accent-soft, 6dp in at the sides, 4 above and below, the control radius).
            wrap = d.frame(par, name=f"Selected row: {it[0]}", layout="v", sizing=("fill", "hug"))
            r = b.coll_row(wrap, it[1], it[0], it[2], it[3], 0)
            d.set(r, sizingH="fill")
            g = d.get(wrap)
            bed = d.rect(wrap, name="Bed", w=g["width"] - 12, h=g["height"] - 8, fill="Color/accent-soft")
            T.k.bind_radius(bed, "control")
            T.k.absolute(bed, 6, 4, g["width"] - 12, g["height"] - 8, {"horizontal": "leftRight", "vertical": "topBottom"})
            d.cmd("node.reorder", id=bed, index=0)
            return wrap
        return make

    return T.cards(col, [maker(it) for it in items], cols, card, name=label)


def library_list_pane(T: Tablet, s: Screen, x: float, width: float):
    """The list pane of list and detail: the collapsed bar with Library's tools, the chips, then Albums as one column
    with the open album on its bed."""
    d, b = T.d, T.b
    pane = d.frame(s.f, name="List pane", x=x, y=0, w=width, h=s.H, clip=True)
    col = s.column("List", x=0, y=STATUS_H, w=width, parent=pane)
    top = d.frame(col, name="Bar", w=width, h=56)
    bar = b.inst("Top bar", top, name="Top bar", props={"Back": "None", "Lift": "False", "Count": "False",
                                                         "Title": "Library", "Show action 1": False, "Show action 2": False})
    d.set(bar, x=0, y=0, width=width)
    tl = b.inst("Tools", top, name="Tools", props={"Leading": "None"})
    d.set(tl, x=0, y=4, width=width)
    box = d.frame(col, name="Chips scroller", w=width, h=48, clip=True)
    ch = chips(T, box, 411, "Albums")
    d.set(ch, x=0, y=0)
    coll_rows(T, col, cb.LIB_ALBUMS, "Albums", width, selected="Paper Lanterns")
    return pane


def build_library(T: Tablet):
    d, b = T.d, T.b
    fr = tk.Frames(d, T.ctx, "Library", "library",
                   note="Library on a tablet. Sideways it is list and detail: the list in a 400dp pane under the collapsed "
                        "bar (the hero would leave four rows at 720dp), the open album beside it, laid out for the width "
                        "of that pane, and the open row on the bed a playing song has. In the pane's grid view the open "
                        "tile gets a bed too, accent-soft reaching 6dp past the tile on every side with the control "
                        "radius; no frame shows that view. Upright it is the phone's page, its rows in one column "
                        "because two would be under 360dp each.")
    for size in ("landscape", "large"):
        s = Screen(T, fr, f"library_{size}", "Library", size, "ui/LibraryPanes.kt · LibraryListDetail", tab="Library")
        library_list_pane(T, s, X0, LIB_PANE)
        collection(T, s, X0 + LIB_PANE, s.cw - LIB_PANE, "album", bar=False)
        s.shell()
        s.notes.append("Albums chip on; Paper Lanterns, the first album, is selected on arrival and open beside the "
                       "list. The detail has no back: the list is beside it.")
        s.notes.append("The list pane is ui/comfort/ComfortLibraryPanes.kt · ComfortLibraryListPane. Its open row sits on "
                       "selectedBed(). With the grid view on, the open tile sits on selectedTileBed(): accent-soft, 6dp "
                       "past the tile on every side, the control radius.")
        s.done()
    s = Screen(T, fr, "library_portrait", "Library", "portrait", SRC + "ComfortLibrary.kt · ComfortLibrary", tab="Library")
    col = s.column()
    T.hero(col, "Library", s)
    T.wide(b.inst("Tools", col, name="Tools", props={"Leading": "None"}), s.cw)
    chips(T, col, s.cw, None)
    coll_rows(T, col, cb.LIB_PLAYLISTS, "Playlists", s.cw)
    coll_rows(T, col, cb.LIB_ARTISTS[:4], "Artists", s.cw, toggle=False)
    s.shell()
    s.notes.append(cb.LIBRARY_COUNTS)
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Collections
COLL = {kind: SRC + f"ComfortCollectionPanes.kt · Comfort{kind.capitalize()}Panes" for kind in ("album", "artist", "playlist", "mood")}
TWO_PANE = 760


def song_columns(card: float) -> int:
    """Adaptive columns at least 360 wide, and one more while a column would be wider than 720."""
    n = adaptive_columns(card, 360, 12)
    while (card - 12 * (n - 1)) / n > 720:
        n += 1
    return n


def collection_data(kind):
    if kind == "album":
        al = cb.ALBUM["Paper Lanterns"]
        return dict(title="Paper Lanterns", byline="Mira Okafor", meta="Album • 2025", covers=[al[4]], person=False,
                    playing=True, art="album")
    if kind == "artist":
        kav = cb.artists()["Kavya Raman"]
        return dict(title="Kavya Raman", byline=None, meta=cb.songs_and_length(kav), covers=[kav[0].cover], person=True,
                    playing=False, art="artist")
    if kind == "mood":
        name, _, count, covers = MOODS[0]
        return dict(title=name, byline=None, covers=list(covers), person=False, playing=False, art="album", radio=True,
                    meta=f"Mood mix · {cb.songs(count)}, 1 hr 32 min\nPicked from how your songs sound and what you play. "
                         "New each day.")
    ld = [cb.track(n) for n in cb.LONG_DRIVE]
    return dict(title="Long drive", byline=None, meta=cb.songs_and_length(ld), covers=cb.mosaic(ld), person=False,
                playing=False, art="album")


def collection_body(T: Tablet, col, kind, width):
    """What scrolls under or beside the header. `width` is the pane's, as a phone screen's would be."""
    d, b = T.d, T.b
    card = width - 32
    if kind == "album":
        ts = cb.album_tracks("Paper Lanterns")
        T.cards(col, [lambda par, i, t=t, n=n: b.song_row(par, t, i, lead="Number", meta=True, number=n + 1)
                      for n, t in enumerate(ts)], song_columns(card), card, name="Songs")
        T.wide(b.inst("Note", col, name="Footer", props={"Text": f"{cb.songs(len(ts))} • {cb.length(ts)}"}), width)
    elif kind == "artist":
        T.label(col, "Top songs", width, tight=True)
        T.cards(col, [lambda par, i, t=cb.track(n), r=r: b.song_row(par, t, i, lead="Rank", rank=r + 1)
                      for r, n in enumerate(cb.KAVYA_TOP)], song_columns(card), card, name="Top songs")
        T.label(col, "Albums", width)
        cols = adaptive_columns(width - 48, 160, 14)
        b.grid(col, [lambda par, n=n: b.tile(par, "Grid", n, str(cb.ALBUM[n][2]), [cb.ALBUM[n][4]], name=f"Album: {n}")
                     for n in ("Iravu Rail", "Kolam")], cols=cols, name="Albums grid", width=width)
        side = round((width - 48 - 14 * (cols - 1)) / cols, 2)
        for n in ("Iravu Rail", "Kolam"):
            t = b.nested(col, f"Album: {n}")
            d.set(b.nested(t, "Art", typ="FRAME"), width=side, height=side)
            d.set(b.nested(t, "Cover"), width=side, height=side)
    elif kind == "mood":
        T.cards(col, [lambda par, i, t=cb.track(n): b.song_row(par, t, i) for n in CHILL], song_columns(card), card,
                name="Songs")
    else:
        ld = [cb.track(n) for n in cb.LONG_DRIVE]
        n_cols = song_columns(card)
        if n_cols == 1:
            makers = [lambda par, i: b.inst("Playlist tools", par, name="Playlist tools", props={"Reorder": "True"})]
            makers += [lambda par, i, t=t: b.song_row(par, t, i) for t in ld]
            T.cards(col, makers, 1, card, name="Songs")
        else:
            tools = d.frame(col, name="Rows", layout="v", w=card, sizing=("fixed", "hug"))
            pt = b.inst("Playlist tools", tools, name="Playlist tools", props={"Reorder": "True"})
            g = b.card(col, width=card, name="Playlist tools card")
            b.fill_slot(g, [pt])
            d.cmd("edit.delete", ids=[tools])
            T.gap(col, 12)
            T.cards(col, [lambda par, i, t=t: b.song_row(par, t, i) for t in ld], n_cols, card, name="Songs")
    T.gap(col, 24, name="Space at the end")


def radio_action(T: Tablet, actions: int):
    """PlayRow(radio = ..., addToPlaylist = false): a mood's Play row has radio where a folder's has add to playlist."""
    d = T.d
    r = d.child(actions, "Add to playlist")
    d.cmd("instance.swap", ids=[d.child(r, "Icon")], component=d.comp["Icon/sensors"])
    d.set(r, name="Radio")


def header_stacked(T: Tablet, col, data, width, cover):
    d, b = T.d, T.b
    h = b.inst("Collection header", col, name="Header", props={"Kind": "Person" if data["person"] else "Album",
                                                               "Title": data["title"], "Meta": data["meta"]})
    if data["byline"]:
        b.props(h, Byline=data["byline"])
    elif not data["person"]:
        b.props(h, **{"Show byline": False})
    d.set(h, width=width)
    b.art(b.nested(h, "Art"), list(data["covers"]), cover, data["art"])
    act = T.wide(b.inst("Collection actions", col, name="Play row", props={"Playing": str(data["playing"])}), width)
    if data.get("radio"):
        radio_action(T, act)


def header_row(T: Tablet, col, data, width):
    """The header as a row, for a pane under 760dp: the cover at 200 at the start, then the title, the byline, the
    details line and the Play row beside it. The phone's Collection header stacks and centres its parts and an
    instance can't rearrange them, so the row is laid out here from the same parts and text styles."""
    d, b, k = T.d, T.b, T.k
    rw = d.frame(col, name="Header row", layout="h", gap=24, pad=[6, 16, 0, 16], w=width, sizing=("fixed", "hug"))
    cv = d.instance("Cover/Album", rw, name="Art")
    b.art(cv, list(data["covers"]), 200, data["art"])
    side = d.frame(rw, name="Title and actions", layout="v", main="spaceBetween", sizing=("fill", "fill"))
    txt = d.frame(side, name="Title block", layout="v", gap=4, pad=[8, 0, 0, 0], sizing=("fill", "hug"))
    k.text(txt, data["title"], "Scale/Title", "Color/on", "Title", fill=True, lines=2)
    if data["byline"]:
        by = d.frame(txt, name="Byline", layout="h", gap=2, pad=[2, 0, 0, 0], cross="center", sizing=("hug", "hug"))
        k.text(by, data["byline"], "Component/Header byline", "Color/accent-ink", "Byline text")
        k.icon(by, "chevron_right", size=18, color="Color/accent-ink", name="Chevron")
    k.text(txt, data["meta"], "Component/Header meta", "Color/on-2", "Meta text", fill=True)
    act = b.inst("Collection actions", side, name="Play row", props={"Playing": str(data["playing"])})
    d.set(act, sizingH="fill", paddingLeft=0, paddingRight=0, paddingBottom=0)
    if data.get("radio"):
        radio_action(T, act)
    return rw


def collection(T: Tablet, s: Screen, x: float, width: float, kind: str, *, bar=True, more=False):
    """A collection in a pane `width` wide starting at `x`: two panes from 760dp, else the header as a row."""
    d, b = T.d, T.b
    data = collection_data(kind)
    top = STATUS_H + (56 if bar else 16)
    if width >= TWO_PANE:
        sp = 360 if width >= 1000 else 320
        start = s.column("Header pane", x=x, y=top, w=sp)
        header_stacked(T, start, data, sp, min(296, sp - 64) if not data["person"] else 200)
        end = s.column("Songs pane", x=x + sp, y=top, w=width - sp)
        T.gap(end, 6)
        collection_body(T, end, kind, width - sp)
    else:
        col = s.column("Collection", x=x, y=top, w=width)
        header_row(T, col, data, width)
        T.gap(col, 20)
        collection_body(T, col, kind, width)
    if bar:
        # The bar spans the pane on the ground: back at the start, the menu at the end.
        d.rect(s.f, name="Bar ground", w=width + END, h=STATUS_H + 56, x=x, y=0, fill="Color/ground")
        tb = b.inst("Top bar", s.f, name="Bar", props={"Back": "Arrow", "Lift": "False", "Count": "False", "Title": "",
                                                       "Show action 1": False, "Show action 2": more})
        d.set(tb, x=x, y=STATUS_H, width=width)


def build_collections(T: Tablet):
    d = T.d
    fr = tk.Frames(d, T.ctx, "Collections", "collections",
                   note="Album, artist and playlist on a tablet. A pane 760dp or wider is two panes under one bar: the "
                        "header (cover up to 296, name, details, the Play row) in a start pane of 360, or 320 under "
                        "1000, and the songs beside it in columns at least 360 wide. A narrower pane, a tablet held upright, "
                        "puts the header in a row: the cover at 200, the name, details and Play row beside it. The bar "
                        "holds back and, for a playlist or Favourites, the menu. It never shows the collection's name: "
                        "over two panes the header stays in view, and it doesn't lift when the songs scroll.")
    for size in ("landscape", "portrait", "large"):
        s = Screen(T, fr, f"album_{size}", "Album", size, COLL["album"], tab="Library")
        collection(T, s, X0, s.cw, "album")
        s.shell()
        s.done()
    for kind, name in (("artist", "Artist"), ("playlist", "Playlist")):
        s = Screen(T, fr, f"{kind}_landscape", name, "landscape", COLL[kind], tab="Home" if kind == "artist" else "Library")
        collection(T, s, X0, s.cw, kind, more=kind == "playlist")
        s.shell()
        s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Search
def build_search(T: Tablet):
    d, b = T.d, T.b
    fr = tk.Frames(d, T.ctx, "Search", "search",
                   note="Search results for \"ka\" on a tablet: the bar, the field at most 720dp wide at the start, what "
                        "you open as a shelf, then Songs in columns at least 360 wide.")
    src = SRC + "ComfortSearch.kt · ComfortSearch"
    res = cb.search("ka")
    for size in ("landscape", "portrait"):
        s = Screen(T, fr, f"search_{size}", "Search, results", size, src, tab="Search")
        col = s.column(y=STATUS_H + 56 + 64)
        sh = cb._result_shelf(b, col, res)
        d.set(sh, width=s.cw, clip=True, paddingRight=0)
        T.label(col, "Songs", s.cw)
        card = s.card
        cols = song_columns(card)
        tracks = res["tracks"][:4 * cols if cols > 1 else 10]
        T.cards(col, [lambda par, i, t=t: b.song_row(par, t, i) for t in tracks], cols, card, name="Songs")
        pin = d.frame(s.f, name="Pinned field", w=s.cw + END, h=64, x=X0, y=STATUS_H + 56, fill="Color/ground",
                      stroke="Color/line", stroke_w=1, strokeAlign="inside", strokeSides=[False, False, True, False])
        fw = min(720, s.card)
        fld = b.inst("Search field", pin, name="Search field", props={"State": "Query", "Container": "Ground", "Query": "ka"})
        d.set(fld, x=16, y=2, width=fw)
        d.rect(s.f, name="Bar ground", w=s.cw + END, h=STATUS_H + 56, x=X0, y=0, fill="Color/ground")
        bar = b.inst("Top bar", s.f, name="Bar", props={"Back": "None", "Lift": "False", "Count": "False", "Title": "Search",
                                                         "Show action 1": True, "Show action 2": False})
        b.swap_icon(b.nested(bar, "Action 1"), "Icon/settings")
        d.set(bar, x=X0, y=STATUS_H, width=s.cw)
        s.shell()
        s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Now Playing
NP_WIDE = SRC + "ComfortNowPlayingWide.kt · ComfortNowPlayingWide"
NP_PHONE = SRC + "ComfortNowPlaying.kt · ComfortNowPlaying"
DECK_H = 232


def seek(T: Tablet, sk: int):
    """A Seek bar instance at the demo position: the played part is never narrower than 64 (NowPlayingParts.kt)."""
    d = T.d
    T.ps.props(sk, {"Progress": "Playing", "Elapsed": "1:18", "Total": "3:52"})
    w = d.get(sk)["width"]
    d.set(d.child(sk, "Played"), width=round(64 + (w - 64) * FRACTION, 1))


def deck(T: Tablet, parent, x, y, width):
    d = T.d
    dk = T.ps.inst("Deck", parent, x=x, y=y, name="Deck", props={"Playing": True}, w=width)
    for n in ("Seek bar", "Transport", "Toggles"):
        d.set(d.child(dk, n), sizingH="fill")
    seek(T, d.child(dk, "Seek bar"))
    return dk


def np_player(T: Tablet, s: Screen, x: float, width: float, cover: float, *, header_x=None, header_w=None, lyric=False,
              spread=False):
    """Now Playing's first page in a column `width` wide (cover + 60, as the phone's 411 is 351 + 60) starting at x:
    the header, the cover, the title, and the deck at the bottom over the gesture inset. `spread` puts the cover and
    title halfway between the header and the deck, where a tall window leaves room: the code puts one weighted space
    over the cover and one under the line being sung, counts the deck's 16dp margin with the deck, and drops the 4dp
    the cover otherwise has over it."""
    d, ps = T.d, T.ps
    f = s.f
    hd = ps.inst("Now playing header", f, x=x if header_x is None else header_x, y=STATUS_H, name="Header",
                 props={"Context": "Paper Lanterns"}, w=header_w or width)
    deck_y = s.H - GESTURE_H - 16 - DECK_H
    top = STATUS_H + 60
    lead = 4
    if spread:
        top += round((deck_y - 16 - top - (cover + 74 + (28 if lyric else 0))) / 2)
        lead = 0
    cv = d.instance("Cover/Album", f, name="Cover", x=x + 30, y=top + lead)
    T.b.art(cv, [0], cover)
    tt = ps.inst("Now playing title", f, x=x, y=top + lead + cover, name="Title", w=width)
    if lyric:
        ps.inst("Now playing lyric", f, x=x, y=top + lead + cover + 74, name="Lyric", w=width)
    deck(T, f, x + 16, deck_y, width - 32)


def np_switch(T: Tablet, s: Screen, x: float, selected: str):
    """The switch in the panel's 60dp head: 8dp into the half, centred in the 54dp the head's 6dp bottom padding leaves."""
    sw = T.d.instance("Now playing/Side panel", s.f, name="Side panel switch", x=x + 8, y=STATUS_H + 3)
    T.d.cmd("instance.setProperties", ids=[sw], properties={"Selected": selected})
    return sw


def np_queue(T: Tablet, s: Screen, x: float, width: float):
    """Up next in the end half: the queue screen's list (ComfortQueue.kt) under the switch."""
    d, ps = T.d, T.ps
    f = s.f
    col = d.frame(f, name="Queue list", x=x, y=STATUS_H + 60, w=width, layout="v", sizing=("fixed", "hug"))
    lab = ps.inst("Section label", col, name="Label: Now playing", fill_w=True,
                  props={"Spacing": "Tight", "Label": "Now playing", "Show small": False, "Show end": True,
                         "End": "Paper Lanterns", "End icon": "Icon/album"})
    d.override(lab, "End icon", width=18, height=18)
    nb = d.frame(col, name="Now playing row", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    now = ps.inst("Queue row", nb, name="Now playing", props={"State": "Current"}, fill_w=True)
    ps.cover(d.child(now, "Cover"), 0)
    ps.inst("Section label", col, name="Label: Up next", fill_w=True,
            props={"Spacing": "Default", "Label": f"Up next, {len(cp.QUEUE)} songs", "Show small": False, "Show end": False})
    g = d.frame(col, name="Up next", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    gc = ps.inst("Group card", g, name="Up next group", props={"Pad": "False"}, fill_w=True)
    for i, t in enumerate(cp.QUEUE):
        artist, c = cp.SONGS[t]
        r = ps.slot(gc, "Song row", first=i == 0, name=f"Row: {t}",
                    props={"Lead": "Cover", "Current": "False", "Title": t, "Subtitle": artist, "Show hairline": i > 0,
                           "Show meta": False, "Show trailing": True})
        ps.props(d.child(r, "Trailing"), {"Kind": "Drag handle", "Tone": "Default"})
        ps.cover(d.child(r, "Cover"), c)
    T.fade(f, x=x, y=s.H - GESTURE_H - 118, w=s.W - x, h=118 + GESTURE_H, stop=62 / 118, name="Clear fade")
    cl = ps.inst("Queue clear", f, name="Clear")
    g = d.get(cl)
    d.set(cl, x=round(x + (width - g["width"]) / 2, 1), y=s.H - GESTURE_H - 28 - g["height"])


def lyric_lines(T: Tablet, parent, x, y, width, first=8):
    d, ps = T.d, T.ps
    col = d.frame(parent, name="Lines", x=x, y=y, w=width, layout="v", sizing=("fixed", "hug"))
    for i in range(first, len(cp.LYRICS)):
        ln = cp.LYRICS[i]
        st = "Blank" if not ln else ("Current" if i == cp.CURRENT_LINE else ("Sung" if i < cp.CURRENT_LINE else "Upcoming"))
        li = ps.inst("Lyrics line", col, name=f"Lyric {i}", props={"State": st}, fill_w=True)
        if ln:
            ps.props(li, {"Line": ln})
        elif i < cp.CURRENT_LINE:
            d.override(li, "Glyph", fill="Color/derived/lyric-sung")
    return col


def np_lyrics(T: Tablet, s: Screen, x: float, width: float):
    """Lyrics in the end half: the synced lines, the one being sung a third of the way down, fading at both ends, and
    the control that opens them full screen."""
    d = T.d
    f = s.f
    top = STATUS_H + 60
    clip = d.frame(f, name="Lyrics list", x=x, y=top, w=s.W - x, h=s.H - top, clip=True)
    lyric_lines(T, clip, 16, 20, width - 32)
    T.fade(f, x=x, y=top, w=s.W - x, h=56, top_clear=False, name="Fade under the switch")
    T.fade(f, x=x, y=s.H - GESTURE_H - 96, w=s.W - x, h=96 + GESTURE_H, stop=96 / (96 + GESTURE_H), name="Fade at the end")
    ob = T.ps.inst("Icon button", f, x=x + width - 8 - 48, y=STATUS_H + 3, name="Open full screen")
    T.b.swap_icon(ob, "Icon/open_in_full")


def build_now_playing(T: Tablet):
    d = T.d
    fr = tk.Frames(d, T.ctx, "Now Playing", "now_playing",
                   note="Now Playing on a tablet, washed with the cover's colour as on the phone. Sideways it is two "
                        "halves: the player at the start, its cover as large as the height leaves (280 at 720dp, 440 at "
                        "900), and at the end a panel that switches between Up next and Lyrics. The line sung over the "
                        "title is left out there, since Lyrics is one tap away. Upright it is the phone's page in a "
                        "column 540 wide (it may be up to 576; the cover stops at 480 and the column is the cover plus "
                        "60), the cover and title halfway between the header and the deck.")
    for size, cover in (("landscape", 280), ("large", 440)):
        s = Screen(T, fr, f"now_playing_up_next_{size}", "Now Playing, Up next", size, NP_WIDE, wash=True)
        half = s.W / 2
        pc = cover + 60
        np_queue(T, s, half, half - END)
        np_switch(T, s, half, "Up next")
        np_player(T, s, round((half - pc) / 2, 1), pc, cover, header_x=0, header_w=half)
        s.bars()
        s.done()
    s = Screen(T, fr, "now_playing_portrait", "Now Playing", "portrait", NP_PHONE, wash=True)
    pc = 540
    np_player(T, s, (s.W - pc) / 2, pc, 480, lyric=True, spread=True)
    s.bars()
    s.done()
    s = Screen(T, fr, "now_playing_lyrics_landscape", "Now Playing, Lyrics", "landscape", NP_WIDE, wash=True)
    half = s.W / 2
    np_lyrics(T, s, half, half - END)
    np_switch(T, s, half, "Lyrics")
    np_player(T, s, round((half - 340) / 2, 1), 340, 280, header_x=0, header_w=half)
    s.bars()
    s.notes.append("The lines are DemoLibrary's invented lyrics.")
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Lyrics
def build_lyrics(T: Tablet):
    d, ps, b = T.d, T.ps, T.b
    fr = tk.Frames(d, T.ctx, "Lyrics", "lyrics",
                   note="Full-screen lyrics sideways: a 360dp start pane with the bar, the cover, the title, and the seek "
                        "bar over previous, play or pause and next in one card, and the lines beside it in a column 640 "
                        "wide. The phone's mini player row at the bottom is not needed: the start pane has the controls.")
    s = Screen(T, fr, "lyrics_landscape", "Lyrics", "landscape", SRC + "ComfortLyricsWide.kt · ComfortLyricsWide", wash=True)
    f, pane = s.f, 360
    rest = s.W - pane
    lw = min(640, rest - 48)
    clip = d.frame(f, name="Lyrics list", x=pane, y=0, w=rest, h=s.H, clip=True)
    lyric_lines(T, clip, round((rest - lw) / 2, 1), 56, lw)
    T.fade(f, x=pane, y=0, w=rest, h=72, top_clear=False, name="Fade at the top")
    T.fade(f, x=pane, y=s.H - GESTURE_H - 96, w=rest, h=96 + GESTURE_H, stop=96 / (96 + GESTURE_H), name="Fade at the end")
    bar = ps.inst("Top bar", f, y=STATUS_H, name="Bar", w=pane,
                  props={"Back": "Close", "Lift": "False", "Count": "False", "Title": "Lyrics", "Show action 1": False,
                         "Show action 2": False})
    d.cmd("instance.swap", ids=[d.child(d.child(bar, "Back"), "Icon")], component=d.comp["Icon/expand_more"])
    cover = 264
    cv = d.instance("Cover/Album", f, name="Cover", x=30, y=STATUS_H + 56 + 8)
    b.art(cv, [0], cover)
    ps.inst("Now playing title", f, x=0, y=STATUS_H + 56 + 8 + cover, name="Title", w=pane)
    # The deck without its toggles: the seek bar and the transport in one card.
    ctl = d.frame(f, name="Controls", layout="v", pad=14, x=16, y=s.H - GESTURE_H - 16 - 166, w=pane - 32, fill="Color/card",
                  sizing=("fixed", "hug"))
    T.k.bind_radius(ctl, "float")
    sk = ps.inst("Seek bar", ctl, name="Seek bar", fill_w=True)
    seek(T, sk)
    ps.inst("Transport", ctl, name="Transport", props={"Playing": True}, fill_w=True)
    s.bars()
    s.notes.append("The lines are DemoLibrary's invented lyrics.")
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── Settings
def build_settings(T: Tablet):
    d = T.d
    fr = tk.Frames(d, T.ctx, "Settings", "settings",
                   note="Settings on a tablet: the phone's list in a column at most 720dp wide in the middle of the "
                        "content area, the dock and the player staying where they are.")
    ss = cs.S(d, T.ctx)
    keys = []
    for size in ("landscape", "portrait"):
        s = Screen(T, fr, f"settings_{size}", "Settings", size, cs.SETTINGS_SRC)
        keys.append(s.key)
        colw = min(720 + 32, s.cw)
        col = s.column(x=X0 + round((s.cw - colw) / 2, 1), w=colw)
        cs._settings_content(ss, col)
        setp(d, d.child(col, "Hero"), Short=str(s.wh < 840))
        for c in d.get(col, depth=1).get("children", []):
            w = d.get(c["id"]).get("width", 0)
            if abs(w - 379) < 1:
                d.set(c["id"], width=colw - 32)
            elif abs(w - 411) < 1:
                d.set(c["id"], width=colw)
        s.shell()
        s.notes.append("Drawn from the phone frame's list (ui-redesign at 4fe4dab), so the Playback group the code has "
                       "since gained is missing here as it is there.")
        s.done()
    fr.close()
    ss.dark_theme(keys)


# ───────────────────────────────────────────────────────────────── Overlays
def build_overlays(T: Tablet):
    d, ps, b = T.d, T.ps, T.b
    fr = tk.Frames(d, T.ctx, "Overlays", "overlays",
                   note="A sheet and a dialog on a tablet, over the scrim. The sheet is at most 640dp wide, in the middle "
                        "of the window at its bottom edge. The dialog is 400dp wide in the middle of the window.")
    # -- the track menu over the album
    s = Screen(T, fr, "sheet_track_menu_landscape", "Track menu over the album", "landscape",
               SRC + "ComfortSheets.kt · ComfortTrackMenu", tab="Library")
    collection(T, s, X0, s.cw, "album")
    s.shell(bars=False)
    sbar = d.instance("Status bar", s.f, name="Status bar", x=0, y=0)
    d.set(sbar, width=s.W, height=STATUS_H)
    ps.inst("Scrim", s.f, name="Scrim", w=s.W, h=s.H)
    sh = ps.inst("Sheet", s.f, name="Track menu")
    d.override(sh, "Foot", height=30 + GESTURE_H)
    hd = ps.slot(sh, "Sheet header", first=True, name="Sheet header", props={"Title": "Porch Light", "Show subtitle": True,
                                                                             "Subtitle": "Mira Okafor"})
    ps.cover(d.child(hd, "Cover"), 0)
    ps.slot(sh, "Quick actions", name="Quick actions")
    for i, (label, lig, end) in enumerate((("Add to playlist", "playlist_add", "Chevron"),
                                           ("Language: English", "language", "Pop-over"), ("Go to album", "album", "None"),
                                           ("Go to artist", "person", "None"), ("Share", "share", "None"))):
        ps.slot(sh, "Sheet row", name=f"Row: {label}",
                props={"Kind": "Default", "End": end, "Label": label, "Icon": f"Icon/{lig}", "Show icon": True,
                       "Show hairline": i > 0, "Show value": False})
    d.set(sh, width=640)
    d.set(sh, x=(s.W - 640) / 2, y=s.H - d.get(sh)["height"])
    g = d.instance("Gesture bar", s.f, name="Gesture bar", x=0, y=s.H - GESTURE_H)
    d.set(g, width=s.W, height=GESTURE_H)
    s.done()
    # -- the New playlist dialog over the Library
    s = Screen(T, fr, "dialog_new_playlist_landscape", "New playlist over the Library", "landscape",
               SRC + "ComfortSheets.kt · ComfortPlaylistNameDialog", tab="Library")
    library_list_pane(T, s, X0, LIB_PANE)
    collection(T, s, X0 + LIB_PANE, s.cw - LIB_PANE, "album", bar=False)
    s.shell(bars=False)
    sbar = d.instance("Status bar", s.f, name="Status bar", x=0, y=0)
    d.set(sbar, width=s.W, height=STATUS_H)
    ps.inst("Scrim", s.f, name="Scrim", w=s.W, h=s.H)
    dl = cp._dialog(d, ps, s.f, content="Field", confirm="Disabled", title="New playlist")
    d.set(dl, width=400)
    d.set(dl, x=(s.W - 400) / 2, y=round((s.H - d.get(dl)["height"]) / 2))
    g = d.instance("Gesture bar", s.f, name="Gesture bar", x=0, y=s.H - GESTURE_H)
    d.set(g, width=s.W, height=GESTURE_H)
    s.notes.append("On the phone Comfort's dialog sits at the bottom edge; on a tablet it is in the middle of the window. "
                   "Create stays disabled until there is a name.")
    s.done()
    fr.close()
    cp._dark_sheet_fix(d, ps, "sheet_track_menu_landscape")


# ───────────────────────────────────────────────────────────────── First run
def build_first_run(T: Tablet):
    d = T.d
    fr = tk.Frames(d, T.ctx, "First run", "first_run",
                   note="The first run's look step on a tablet, with no dock: a column 560dp wide in the middle. Upright "
                        "it is the phone's page. Sideways the 720dp height can't hold the cards over the theme list, so "
                        "the two sit side by side under the heading, and Continue keeps the column's 560.")
    ss = cs.S(d, T.ctx)
    keys = []

    def looks(parent):
        row_ = d.frame(parent, name="Looks", layout="h", gap=12, sizing=("fill", "hug"))
        for style in ("Expressive", "Comfort"):
            c = inst(d, "Look card", row_, f"Look: {style}", Style=style, Selected=str(style == "Comfort"))
            d.set(c, sizingH="fill")
        return row_

    def fit_previews(row_):
        """LookCard draws its preview to the card's width: scale the nested preview as the code scales the drawing.
        Run it once the cards have their final width."""
        for style in ("Expressive", "Comfort"):
            c = d.child(row_, f"Look: {style}")
            pv = d.child(c, "Preview")
            factor = (d.get(c)["width"] - 16) / d.get(pv)["width"]
            if abs(factor - 1) > 0.01:
                d.cmd("transform.scale", ids=[pv], factor=round(factor, 4))

    def theme(parent, width):
        l = ss.label(parent, "Theme")
        d.set(l, width=width + 32)
        g, _ = group(d, parent, "Theme")
        d.set(g, width=width)
        for i, (t, on) in enumerate((("Match phone", False), ("Dark", False), ("Light", True))):
            row(d, g, "Settings row", f"Theme: {t}", Control="Radio on" if on else "Radio off", Title=t,
                **{"Show hairline": i > 0})

    for size in ("landscape", "portrait"):
        s = Screen(T, fr, f"first_run_look_{size}", "First run, Look", size, cs.ONB_SRC)
        keys.append(s.key)
        f = s.f
        wide = size == "landscape"
        colw = 560
        blockw = 840 if wide else colw
        x = (s.W - blockw) / 2
        tb = d.instance("Onboarding top bar", f, name="Top bar", x=x, y=STATUS_H)
        setp(d, tb, Step="2")
        d.set(tb, width=blockw)
        body = d.frame(f, name="Step", layout="v", w=blockw, x=x, y=STATUS_H + 64, cross="center", sizing=("fixed", "hug"))
        h = inst(d, "Onboarding heading", body, "Heading", Title="Pick your look", Body="You can change it any time in Settings.")
        d.set(h, width=colw)
        if wide:
            pair = d.frame(body, name="Look and theme", layout="h", gap=24, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
            left = d.frame(pair, name="Look", layout="v", pad=[51, 0, 0, 0], sizing=("fill", "hug"))
            lk = looks(left)
            right = d.frame(pair, name="Theme", layout="v", cross="center", sizing=("fill", "hug"))
            theme(right, (blockw - 32 - 24) / 2)
            d.set(d.child(right, "Label: Theme"), sizingH="fill")
            fit_previews(lk)
        else:
            box = d.frame(body, name="Look", layout="v", pad=[28, 16, 0, 16], sizing=("fill", "hug"))
            fit_previews(looks(box))
            theme(body, colw - 32)
        bf = d.frame(f, name="Bottom", layout="v", w=colw, x=(s.W - colw) / 2, y=s.H - GESTURE_H - 96, pad=[8, 24, 32, 24],
                     sizing=("fixed", "hug"))
        bt = d.instance("Button", bf, name="Button")
        setp(d, bt, Style="Filled", Size="Large", State="Default", Tone="Default", Label="Continue",
             **{"Show leading icon": False})
        d.set(bt, sizingH="fill")
        s.bars()
        s.done()
    fr.close()
    ss.dark_theme(keys)


# ───────────────────────────────────────────────────────────────── mood mixes
# The demo state as the tablet renders show it (scripts/store_shots/tablet_refs.sh): each mood's line, the songs in
# today's mix and the four covers of its mosaic.
MOODS = [("Chill", "Slow and easy", 25, (1, 0, 8, 4)), ("Happy", "Bright and cheerful", 23, (6, 1, 0, 7)),
         ("Energetic", "Fast and driving", 23, (10, 11, 7, 3)), ("Focus", "Mostly instrumental, steady", 14, (6, 8, 1, 5)),
         ("Party", "Made for dancing", 23, (9, 0, 2, 10)), ("Melancholy", "Sad and reflective", 23, (7, 5, 8, 9)),
         ("Romantic", "Tender and warm", 23, (8, 9, 5, 3))]
# The taste mixes above the Moods shelf, which the demo state now has.
MOOD_MIXES = [("Mix: Mira Okafor, Juniper Vale and more", 24, [0, 3, 11, 4]),
              ("Mix: Kavya Raman, Tide & Tamarind and more", 21, [8, 7, 5, 2]),
              ("Mix: Ruhaan Sethi, Paloma Ferrer and more", 23, [9, 2, 6, 3]),
              ("Mix: Velvet Circuit, Small Hours Tape Club and more", 22, [10, 1, 4, 11])]
# Today's Chill mix, in the page's order.
CHILL = ["Rain on the Skylight", "Lights Over the Water", "Nila Paadhai", "Brushes", "Marea Lenta", "Low Tide Hymn",
         "404 Heartbeat", "Aadhi Neend", "Afterglow Protocol", "Auto Rickshaw Waltz", "Azulejo", "Baadal Ka Khat"]
# Moods and interests: each group's choices, the ones picked, and the chips in each row of a 660dp card (FlowRow's
# breaks at 1152dp, set here because FigCraft's text is a little wider than Android's).
INTERESTS = [("Moods you like", [m[0] for m in MOODS], ("Chill", "Focus", "Romantic"), (6, 1)),
             ("Genres", ["Pop", "Film", "Electronic", "Folk, World, & Country", "Jazz", "Latin", "Hip hop", "Funk / Soul",
                         "Classical"], ("Pop", "Film", "Jazz"), (6, 3)),
             ("Languages", ["Tamil", "Hindi", "English", "No vocals"], ("Tamil", "English"), (4,))]
MOODS_KT = SRC + "ComfortMoods.kt"


def mood_tile(T: Tablet, parent, mood) -> int:
    d = T.d
    name, line, count, covers = mood
    t = d.instance("Mood tile", parent, name=f"Mood tile: {name}")
    cm.setp(d, t, Title=name, Count=cb.songs(count), Description=line)
    cm.pictures(d, t, *covers)
    return t


def collapsed_bar(T: Tablet, s: Screen, title: str) -> int:
    """A root page's bar once its hero has scrolled away: the title and settings on the ground, across the column."""
    d, b = T.d, T.b
    d.rect(s.f, name="Bar ground", w=s.cw + END, h=STATUS_H + 56, x=X0, y=0, fill="Color/ground")
    bar = b.inst("Top bar", s.f, name="Bar", props={"Back": "None", "Lift": "False", "Count": "False", "Title": title,
                                                     "Show action 1": True, "Show action 2": False})
    b.swap_icon(b.nested(bar, "Action 1"), "Icon/settings")
    d.set(bar, x=X0, y=STATUS_H, width=s.cw)
    return bar


def browse_cards(T: Tablet, col, prefix: str, tiles, columns: int, width: float, album=False) -> int:
    """Browse's cards in `columns` equal cells 12dp apart: (title, second line, covers). An album's card shows its
    cover where a mood's shows its mosaic."""
    d = T.d
    box = d.frame(col, name=f"{prefix} cards", layout="v", gap=12, w=width, sizing=("fixed", "hug"))
    cell = round((width - 12 * (columns - 1)) / columns, 2)
    for r in range(0, len(tiles), columns):
        rw = d.frame(box, name=f"{prefix} row {r // columns + 1}", layout="h", gap=12, sizing=("fill", "hug"))
        for title, sub, covers in tiles[r:r + columns]:
            t = d.instance(cm.variant(T.ctx, "Browse tile", "Tinted=False"), rw, name=f"Browse: {title}")
            d.set(t, width=cell)
            cm.setp(d, t, Title=title, Subtitle=sub)
            if album:
                cm.swap(d, d.child(t, "Art"), cm.variant(T.ctx, "Cover/Album", "Shape=Squircle, Art=Image, Dimmed=False"))
                d.set(d.child(t, "Art"), width=56, height=56)
            cm.pictures(d, d.child(t, "Art"), *covers)
    return box


def build_moods(T: Tablet):
    d, b = T.d, T.b
    fr = tk.Frames(d, T.ctx, "Mood mixes", "moods",
                   note="Mood mixes on a tablet. Home's Moods shelf scrolls sideways like the other shelves and stops "
                        "where the page's column does. A mood's page has the genre page's panes: two from 760dp, the "
                        "header (the mosaic up to 296, the name, the two lines, the Play row with radio where a folder "
                        "has add to playlist) beside the songs; narrower, a tablet held upright, the header is a row. "
                        "Search's Browse sets the moods in the browse card, in cells at least 180dp wide. Moods and "
                        "interests is full screen, a column at most 720dp wide with Save at its foot.")
    # Home, scrolled to the Moods shelf.
    s = Screen(T, fr, "moods_home_landscape", "Home, Moods shelf", "landscape", SRC + "ComfortHome.kt · ComfortHome")
    W, CARD = s.cw, s.card
    col = s.column()
    T.label(col, "Instant mixes", W)
    T.cards(col, [lambda par, i, t=cb.track(n): b.song_row(par, t, i, trailing="Radio", current=False) for n in cb.MIXES],
            3, CARD, name="Instant mixes")
    T.label(col, "Mixes", W, small="Songs you play, grouped by sound")
    T.shelf(col, [lambda par, name=name, count=count, covers=covers:
                  b.tile(par, "Album", name, cb.songs(count), covers, name=f"Mix: {name[5:]}", lines={"Title": 2})
                  for name, count, covers in MOOD_MIXES], W, name="Mixes shelf")
    T.label(col, "Moods", W, small="Your songs, sorted by how they feel")
    shelf = T.shelf(col, [lambda par, m=m: mood_tile(T, par, m) for m in MOODS], W, name="Moods shelf")
    T.song_shelf(col, "Recently played", cb.RECENT, s)
    d.set(col, y=round(475 - d.get(shelf)["y"], 1))          # scrolled until the Moods shelf clears the player bar
    pin = d.frame(s.f, name="Pinned tabs", w=s.cw + END, h=48, x=X0, y=STATUS_H + 56, fill="Color/ground",
                  stroke="Color/line", stroke_w=1, strokeAlign="inside", strokeSides=[False, False, True, False])
    tl = b.inst("Tools", pin, name="Tools", props={"Leading": "Sub-tabs"})
    d.set(tl, x=0, y=0, width=W)
    for lab in ("All", "Albums", "Artists", "Folders"):
        b.props(b.nested(tl, f"Sub-tab: {lab}"), Selected=str(lab == "All"))
    d.set(b.nested(tl, "Settings"), visible=False)           # settings has gone up into the bar
    collapsed_bar(T, s, "Home")
    T.triggers.append((s.key, s.size, d.child(shelf, "Mood tile: Chill"), "open", "mood_page_landscape"))
    s.shell()
    s.notes += ["The page has scrolled past the hero: the bar holds the title, and the sub-tabs stay under it over a "
                f"hairline. Each tile is {MOODS_KT} · MoodTile, Search's browse card at 200 x 133 with the count across "
                "from the mosaic and the mood's line under its name, 14dp apart in a shelf that starts 24dp in.",
                "The shelf shows every mood that has a mix today, in the app's order: nothing is picked in the demo "
                "state. The counts are the day's mixes."]
    s.done()

    # The mood's page: two panes sideways, a header row upright.
    for size in ("landscape", "portrait"):
        s = Screen(T, fr, f"mood_page_{size}", "Mood", size, COLL["mood"])
        collection(T, s, X0, s.cw, "mood")
        s.shell()
        s.notes.append(f"{MOODS_KT} · ComfortMood hands a medium or expanded window to ComfortMoodPanes. The Play row "
                       "has radio (the sensors glyph, \"Mood radio\") and no add to playlist: the mix is new each day. "
                       "The song that is playing is in the mix, so its row lies on the current song's bed; Play still "
                       "reads Play because the queue came from the album.")
        s.done()

    # Search: Browse, scrolled to the Moods group.
    s = Screen(T, fr, "moods_browse_landscape", "Search, Browse", "landscape", SRC + "ComfortSearch.kt · ComfortSearch",
               tab="Search")
    col = s.column()
    n = adaptive_columns(s.card, 180, 12)
    T.label(col, "Albums", s.cw)
    browse_cards(T, col, "Album", [(a[0], a[1], [a[4]]) for a in cb.BIGGEST_ALBUMS[:8]], n, s.card, album=True)
    T.gap(col, 14, name="Space under the albums")
    T.label(col, "Moods", s.cw)
    moods = browse_cards(T, col, "Mood", [(m[0], cb.songs(m[2]), m[3]) for m in MOODS], n, s.card)
    d.set(col, y=round(306 - d.get(moods)["y"], 1))
    fld = b.inst("Search field", s.f, name="Search field", props={"State": "Empty", "Container": "Ground"})
    d.set(fld, x=X0 + 16, y=STATUS_H + 56, width=min(720, s.card))
    collapsed_bar(T, s, "Search")
    T.triggers.append((s.key, s.size, d.child(moods, "Browse: Chill"), "open", "mood_page_landscape"))
    s.shell()
    s.notes += [f"Browse has scrolled past the folders and most of the albums to Moods. The field stays under the bar, "
                f"at most 720dp wide, over the list. The cards are Search's browse card, {n} across in cells at least "
                "180dp wide, 12dp apart; a mood's shows its mosaic and its song count.",
                "Every mood that has a mix is listed, picked or not. Genres would follow Moods; the demo library has "
                "none."]
    s.done()

    # Moods and interests: full screen.
    s = Screen(T, fr, "moods_picker_landscape", "Moods and interests", "landscape", MOODS_KT + " · ComfortInterests")
    colw = 720
    foot = GESTURE_H + 16 + 56 + 8                    # the gesture inset, Save's 16 below and 8 above, its 56
    page = d.frame(s.f, name="Page", x=round((s.W - colw) / 2), y=0, w=colw, h=s.H - foot, clip=True)
    col = s.column("Content", x=0, y=STATUS_H, w=colw, parent=page)
    T.wide(b.inst("Hero", col, name="Hero", props={"Mark": "False", "Short": "False", "Title": "Moods and interests",
                                                   "Show subtitle": False}), colw)
    tools = d.frame(col, name="Tools", layout="h", w=colw, h=50, pad=[0, 0, 2, 8], cross="center", sizing=("fixed", "fixed"))
    back = d.instance("Icon button", tools, name="Back")
    setp(d, back, Icon="Icon/arrow_back")
    intro = d.frame(col, name="Intro", layout="v", w=colw, pad=[4, 28, 0, 28], sizing=("fixed", "hug"))
    T.k.text(intro, "The Moods shelf on Home follows what you pick here. It all stays on this phone.", "Component/Toast",
             "Color/on-2", "Intro text", fill=True)                # onest(14.5f, 20f), the toast's type
    for label, names, picked, rows in INTERESTS:
        T.label(col, label, colw)
        hold = d.frame(col, name=f"{label} chips", layout="v", gap=8, w=colw - 32, sizing=("fixed", "hug"))
        at = 0
        for r, count in enumerate(rows):
            rw = d.frame(hold, name=f"Chip row {r + 1}", layout="h", gap=8, sizing=("hug", "hug"))
            for nm in names[at:at + count]:
                c = d.instance(cm.variant(T.ctx, "Chip", f"Selected={nm in picked}, Container=Card"), rw, name=f"Chip: {nm}")
                cm.setp(d, c, Label=nm)
            at += count
        g = b.card(col, pad=True, width=colw - 32, name=label)
        b.fill_slot(g, [hold])
    sv = d.frame(s.f, name="Save bar", layout="v", x=round((s.W - colw) / 2), y=s.H - foot, w=colw, pad=[8, 24, 16, 24],
                 fill="Color/ground", sizing=("fixed", "hug"))
    bt = d.instance("Button", sv, name="Save")
    setp(d, bt, Style="Filled", Size="Large", State="Default", Tone="Default", Label="Save", **{"Show leading icon": False})
    d.set(bt, sizingH="fill")
    T.triggers.append((s.key, s.size, bt, "open", "moods_home_landscape"))
    s.bars()
    s.notes += ["Full screen, as the first-run pages are: no dock and no player, so Save stays at the foot. The hero, "
                "the back row, the groups and Save keep to a column at most 720dp wide in the middle, as Settings does "
                "on a tablet; the hero is the tall one. The list ends where Save's 8dp of padding begins.",
                "Each group's chips sit on a card under its label, 8dp apart in rows 8dp apart. Chill, Focus and "
                "Romantic, three genres and two languages are picked."]
    s.done()
    fr.close()


# ───────────────────────────────────────────────────────────────── equaliser
def equaliser_sheet(T: Tablet, s: Screen, width: float = 640) -> int:
    """ComfortEqualiserSheet at a tablet sheet's width, with the default (Bass boost): the title and switch, the scope
    head, the chips and the bands' plot, the two save rows and their note. Returns the scope head's lead box."""
    d, ps = T.d, T.ps
    sh = ps.inst("Sheet", s.f, name="Equaliser sheet")
    d.override(sh, "Foot", height=30 + GESTURE_H)
    d.set(sh, width=width)
    ps.slot(sh, "Equaliser title", first=True, name="Title", props={"On": True})
    head = ps.slot(sh, "Scope head", name="Scope head", props={"Scope": "Default"})
    content = d.child(sh, "Content")
    ed = ceq._editor(d, s.f, width=width, selected="Bass boost", chip_side=16, plot_side=16)
    space = d.frame(s.f, name="Space above the rows", w=width, h=ceq.ROWS_ABOVE)
    d.cmd("node.reparent", ids=ed["pieces"] + [space], parent=content)
    d.set(ed["pieces"] + [space], sizingH="fill")
    for i, (label, icon) in enumerate((("Save for this song", "music_note"), ("Save for this album", "album"))):
        ps.slot(sh, "Sheet row", name=f"Row: {label}",
                props={"Kind": "Default", "End": "None", "Label": label, "Icon": f"Icon/{icon}", "Show icon": True,
                       "Show hairline": i > 0, "Show value": False})
    note = d.frame(s.f, name="Note", layout="v", w=width, pad=[4, 24, 0, 24], sizing=("fixed", "hug"))
    t = d.text(note, "Saved settings come back whenever the song plays.", style="Component/Note", color="Color/on-2",
               name="Note text")
    d.set(t, sizingH="fill", textAutoResize="height")
    d.cmd("node.reparent", ids=[note], parent=content)
    d.set(note, sizingH="fill")
    d.set(sh, x=(s.W - width) / 2, y=s.H - d.get(sh)["height"])
    return d.child(head, "Lead")


def build_equaliser(T: Tablet):
    d, ps = T.d, T.ps
    fr = tk.Frames(d, T.ctx, "Equaliser", "equaliser",
                   note="The equaliser on a tablet. The sheet is a sheet like any other there: at most 640dp wide, in the "
                        "middle of the window at its bottom edge, over both halves of Now Playing, on the cover's wash. "
                        "The bands' plot takes the width: 568dp of plot, a band every 56.8dp. Settings > Equaliser "
                        "keeps the dock and the player; it is the phone's page in a column at most 720dp wide in the "
                        "middle of the content area, with a 664dp plot.")
    EQ = SRC + "ComfortEqualiser.kt"
    # The sheet over Now Playing.
    key = "sheet_equaliser_landscape"
    s = Screen(T, fr, key, "Equaliser sheet over Now Playing", "landscape", EQ + " · ComfortEqualiserSheet", wash=True)
    half, pc = s.W / 2, 340
    np_queue(T, s, half, half - END)
    np_switch(T, s, half, "Up next")
    np_player(T, s, round((half - pc) / 2, 1), pc, 280, header_x=0, header_w=half)
    sbar = d.instance("Status bar", s.f, name="Status bar", x=0, y=0)
    d.set(sbar, width=s.W, height=STATUS_H)
    ps.inst("Scrim", s.f, name="Scrim", w=s.W, h=s.H)
    lead = equaliser_sheet(T, s)
    g = d.instance("Gesture bar", s.f, name="Gesture bar", x=0, y=s.H - GESTURE_H)
    d.set(g, width=s.W, height=GESTURE_H)
    s.notes.append("Opened from Equaliser in the track menu over Now Playing. The song plays with the default, Bass "
                   "boost, so the scope head reads For all songs and the rows are Save for this song and Save for this "
                   "album. Band sliders is the phone's component at 608dp: its tracks, response and knobs follow the "
                   "width.")
    d.annotate(d.screens[key]["light"],
               "The reference render gives the sheet's window no insets, so there the sheet ends at the screen's foot. On "
               "a tablet ComfortSheet's navigationBarsPadding() puts the 20dp navigation inset under the 30dp foot, as "
               "drawn here: the sheet is 20dp taller and its rows sit 20dp higher than in the render.",
               "Reference render differs")
    s.done()

    # Settings > Equaliser.
    s = Screen(T, fr, "settings_equaliser_landscape", "Equaliser page", "landscape", EQ + " · ComfortEqualiserPage")
    ss = cs.S(d, T.ctx)
    colw = min(720 + 32, s.cw)
    card = colw - 32
    col = s.column(x=X0 + round((s.cw - colw) / 2, 1), w=colw)
    ss.hero(col, "Equaliser", short=True)
    ss.lead_tools(col)
    g, _ = group(d, col, "Use equaliser")
    row(d, g, "Settings row", "Use equaliser", Control="Switch on", Title="Use equaliser")
    ss.label(col, "Default")
    g, slot = group(d, col, "Default")
    d.set(g, width=card)
    editor = d.frame(s.f, name="Editor", layout="v", w=card, pad=[10, 0, 8, 0], sizing=("fixed", "hug"), clip=False)
    ed = ceq._editor(d, editor, width=card, selected="Bass boost", chip_side=12, plot_side=8)
    d.set(ed["pieces"], sizingH="fill")
    for c in d.get(slot, depth=1).get("children", []):
        d.cmd("edit.delete", ids=[c["id"]])
    d.__dict__.setdefault("_slot_used", {})[g] = True
    d.cmd("node.reparent", ids=[editor], parent=slot)
    d.set(editor, sizingH="fill")
    inst(d, "Note", col, "Note", Text="Every song without its own setting plays with this.")
    ss.label(col, "Albums with their own setting")
    ceq._own_rows(d, col)
    ss.label(col, "Songs with their own setting")
    ceq._own_songs(d, col)
    for c in d.get(col, depth=1).get("children", []):
        w = d.get(c["id"]).get("width", 0)
        if abs(w - 379) < 1:
            d.set(c["id"], width=card)
        elif abs(w - 411) < 1:
            d.set(c["id"], width=colw)
    s.shell()
    s.notes.append("Settings > Playback > Equaliser. The page is the phone's in a column at most 720dp wide: the short "
                   "hero, the back row, Use equaliser, then the default's chips and plot on one card, and under the "
                   "player bar the albums and songs with their own setting. The page has no scope head: it only ever "
                   "edits the default.")
    s.done()
    fr.close()
    # On a dark sheet a neutral box is one step up (fill-2), as on the phone's sheet.
    dark = d.screens[key]["map"].get(lead)
    if dark:
        d.override(dark, "Container", fill="Color/fill-2")


# ───────────────────────────────────────────────────────────────── cover, changelog, links, hygiene
CHANGES = [
    "The tablet layouts: the Tablet page and its components, put back on 1.3.0, with frames for mood mixes and the "
    "equaliser. Every Tablet frame, Light and Dark, matches the code on feature/tablet-layouts and is marked ready for "
    "development.",
    "Tablet page, after Screens: Home, Library, Search, album, artist, playlist, Now Playing, lyrics, Settings, a sheet, "
    "a dialog and the first run's look step, at 1152 x 720 (a tablet sideways), 720 x 1152 (upright) and 1440 x 900, "
    "Light and Dark.",
    "Mood mixes on the Tablet page: Home scrolled to the Moods shelf, the mood page in two panes and, upright, with its "
    "header as a row, Search's Browse with the Moods group, and Moods and interests as a column at most 720dp wide.",
    "The equaliser on the Tablet page: the sheet over Now Playing, 640dp wide, and Settings > Equaliser as a column at "
    "most 720dp wide. Band sliders is the phone's component, resized.",
    "Components: Side dock and Side dock/Tab, Player bar (Components / Navigation); Now playing/Side panel "
    "(Components / Playback).",
    "Each frame carries its Source annotation: ComfortAlbumPanes, ComfortArtistPanes, ComfortPlaylistPanes and "
    "ComfortMoodPanes for the collections, ComfortLyricsWide for full-screen lyrics. Frames that show the dock and the "
    "player carry a second Source line for ComfortWideShell.",
    "Screens page: the Moods section's 12 tablet frames marked In progress (Home, the mood page and the picker at "
    "1440 x 900 and 700 x 900, drawn as layout before the side dock and the player bar were components) are deleted. "
    "The Tablet page has those screens, and the section's note says so.",
    "Notes record what no frame shows: in the Library's list pane the open tile of the grid view sits on a selection "
    "bed, and a collection's bar never shows its name.",
    "Left as drawn where the difference from the renders is demo data: Home's shelves that are empty in the demo "
    "state, the Playback group in Settings, and which line of the lyrics is being sung.",
    "The phone screens, the equaliser's and the mood mixes' are unchanged from 1.3.0.",
]
# Oldest first. An entry already in the file stays as it is unless it is the current version, which is written again.
ENTRIES = [(VERSION, UPDATED, CHANGES)]


def build_cover(d: Doc, ctx: dict):
    """The version on the Cover, the Tablet row in its Read me and the version's entry at the top of the Changelog.
    The entry is replaced if an earlier run added it; the entries the file came with (1.0.0, 1.1.0 and 1.2.0 for the
    equaliser, 1.3.0 for mood mixes) are left alone."""
    d.page("Cover")
    cover = next(c["id"] for c in top_children(d, "Cover") if c.get("name") == "Cover")
    for label, value in (("Version", VERSION), ("Last updated", UPDATED)):
        col = d.child(cover, f"Meta: {label}")
        d.set(d.child(col, "Value"), characters=value)
    # Read me: the Tablet page in the list of pages, after Screens.
    readme = next((c["id"] for c in top_children(d, "Cover") if c.get("name") == "Read me"), None)
    if readme is not None:
        tbl = d.child(readme, "Pages")
        rows = d.get(tbl, depth=1).get("children", [])
        for c in rows:
            if c.get("name") == "Page: Tablet":
                d.cmd("edit.delete", ids=[c["id"]])
        names = [c.get("name") for c in d.get(tbl, depth=1).get("children", [])]
        r = d.frame(tbl, name="Page: Tablet", layout="h", gap=24, sizing=("fill", "hug"))
        d.text(r, "Tablet", style="Doc/Label", color="Canvas/label", name="Page", width=220, textAutoResize="height")
        t = d.text(r, "The screens on medium and expanded windows (tablets), Light row above Dark row.",
                   style="Doc/Body", color="Canvas/label", name="What", textAutoResize="height")
        d.set(t, sizingH="fill")
        if "Page: Screens" in names:
            d.cmd("node.reorder", id=r, index=names.index("Page: Screens") + 1)
    d.page("Changelog")
    kids = {c.get("name"): c["id"] for c in top_children(d, "Changelog")}
    col, howto = kids["Entries"], kids.get("How to add an entry")
    for version, date, changes in ENTRIES:
        have = [c["id"] for c in d.get(col, depth=1).get("children", []) if c.get("name") == f"Entry {version}"]
        if have and version != VERSION:
            continue
        if have:
            d.cmd("edit.delete", ids=have)
        i = d.instance("Doc/Changelog entry", col, name=f"Entry {version}")
        d.cmd("instance.setProperties", ids=[i], properties={"Version": version, "Date": date, "Author": "Attic maintainers",
                                                              "Changes": "\n".join(changes)})
        d.set(i, sizingH="fill")
        d.set(d.child(i, "Changes"), listStyle="bulleted", paragraphSpacing=10)
        # Newest on top: the column lists its layers bottom first, so the top entry is the last index.
        first = d.get(col, depth=1)["children"][0]["id"]
        if d.get(first)["y"] <= d.get(i)["y"] and first != i:
            d.cmd("node.reorder", id=i, index=0)
            if d.get(i)["y"] > 1:
                d.cmd("node.reorder", id=i, index=len(d.get(col, depth=1)["children"]) - 1)
    if howto:
        g = d.get(col)
        d.set(howto, y=g["y"] + g["height"] + 80)
    ctx["exports"]["changelog"] = col
    ctx["exports"]["cover"] = cover


def build_links(T: Tablet):
    """A few prototype links on the frames that exist: dock tabs between the root screens of one size, and the player
    bar or mini player to Now Playing."""
    d = T.d
    roots = {"Home": "home", "Search": "search", "Library": "library"}
    np = {"landscape": "now_playing_up_next_landscape", "large": "now_playing_up_next_large", "portrait": "now_playing_portrait"}
    made = 0
    for key, size, layer, what, target in T.triggers:
        if key not in d.screens or not d.screens[key].get("dark"):
            continue
        dst = f"{roots[target]}_{size}" if what == "tab" else target if what == "open" else np.get(size)
        if not dst or dst not in d.screens or dst == key:
            continue
        try:
            d.link(key, layer, dst)
            made += 1
        except Exception as e:           # a link is a nicety: the frames stand without it
            print("    link skipped:", key, "->", dst, str(e)[:80], flush=True)
    return made


def hygiene(d: Doc, roots: dict) -> dict:
    """tk.hygiene's check from one read of each tree: layers still carrying a default name (Frame, Rectangle 3...).
    One read doesn't reach every layer of a page as large as Tablet, so build() checks that page with tk.hygiene."""
    def walk(n, bad):
        nm = n.get("name", "")
        for word in tk.DEFAULT_NAMES:
            if nm == word or (nm.startswith(word + " ") and nm[len(word) + 1:].isdigit()):
                bad.append((n.get("id"), nm))
        for c in n.get("children", []):
            walk(c, bad)
        return bad
    return {label: walk(d.get(root, depth=40), []) for label, root in roots.items()}


# Response curve (comfort/equaliser.py) names its stroke `Line`, which the check takes for a default name. A Band
# sliders instance brings one with it: a frame's own layers never carry that name.
COMPONENT_NAMED = {"Line": "Response curve's stroke, inside each Band sliders instance"}


AREAS = {"home": build_home, "library": build_library, "search": build_search, "collections": build_collections,
         "now_playing": build_now_playing, "lyrics": build_lyrics, "settings": build_settings, "overlays": build_overlays,
         "first_run": build_first_run, "moods": build_moods, "equaliser": build_equaliser}


def build(d: Doc, ctx: dict):
    import time
    only = ctx.get("only")
    ctx["tokens"] = tokens(d)
    t = time.time()
    build_components(d, ctx)
    print(f"  components {time.time() - t:6.1f}s", flush=True)
    if not only or "retire" in only:
        print(f"  retired      {tk.retire_placeholders(d)} In progress frames of the Moods section", flush=True)
    tk.reset_page(d)
    T = Tablet(d, ctx)
    for key, fn in AREAS.items():
        if only and key not in only:
            continue
        t = time.time()
        fn(T)
        print(f"  {key:12s} {time.time() - t:6.1f}s", flush=True)
    print(f"  links        {build_links(T)}", flush=True)
    if not only or "cover" in only:
        build_cover(d, ctx)
    roots = {"Tablet page": d.pages[tk.PAGE]}
    for page, names in ADDED.items():
        for c in top_children(d, page):
            if c.get("name") in {f"Spec: {n}" for n in names}:
                roots[c["name"]] = c["id"]
    page = roots.pop("Tablet page")
    report = hygiene(d, roots)
    d.page(tk.PAGE)
    found = tk.hygiene(d, {"Tablet page": page})["Tablet page"]
    report["Tablet page"] = [(i, n) for i, n in found if n not in COMPONENT_NAMED]
    ctx["hygiene"] = report
    for label, bad in report.items():
        print(f"  hygiene {label}: {len(bad)} default-named layers {bad[:8] if bad else ''}", flush=True)
    for name, what in COMPONENT_NAMED.items():
        n = sum(1 for _, nm in found if nm == name)
        if n:
            print(f"  hygiene Tablet page: {n} layers named {name}, each the {what}", flush=True)
