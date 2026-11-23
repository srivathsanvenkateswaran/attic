"""Empty, loading, permission and first-run parts; Recap cards and story chrome; widget, notification,
splash and launcher icon. Pages: Components / States, Components / Recap, Components / System, plus the
settings rows below what is already on Components / Lists and cards.

Owner: settings screens agent. Screens in screens_settings.py and system_surfaces.py import the helpers
at the top of this module.

Colours and text styles
-----------------------
Recap's cards and story take the scheme of one of the period's covers (ExpressiveRecapCards.kt
recapCardCover). The Art collection, the Color mode per album (COLOR_MODE[i]), Color/recap/*, Brand,
Comfort preview and the Recap/*, First run/* and System/* text styles are made in foundations.py and
documented on the Colour and Type pages. A Recap card is drawn in its cover's dark scheme in both
themes, so every card variant sets Color = its cover's mode and Art = Dark, Cover = Dark.

Components
----------
Components / States
  Empty state                 EmptyState (Surfaces.kt)  Art=Clover|Rounded|None.  Props: Title, Body, Show body, Icon
  Loading indicator           LoadingIndicator (PlayControls.kt)  Contained=False|True
  Shapes card                 GateArt / WelcomeArt  Kind=Permission|Refused|Welcome
  First run/Look card         LookCard + LookPreview  Style=Expressive|Comfort, Selected=True|False
  First run/Access row        AccessRow  State=None|Allow|Allowed|Off|Off required.  Props: Title, Body, Icon
  First run/Lyrics row        SetSwitchRow with a leading glyph  Checked=False|True
  First run/Radio note        SetNote with "Song radio settings"
  First run/Scanning          the library step while it scans
Components / Lists and cards (appended)
  List row/Setting text       SavedLyricsRow and the About row  Lead=Icon|Mark
  List row/Setting progress   AnalysisRow and DownloadLyricsRow  Kind=Analysis done|Analysis running|Analysis partial|Lyrics running
  List row/Style picker       StyleCell: the Style row over the two-way group  Selected=Expressive|Comfort
Components / Recap
  Recap/Card                  ExpressiveRecapCard, 360 x 640  Kind=Intro|Minutes|Top song|Top songs|Top artist|Top artists|
                              Languages|Listening clock|Highlights|Summary
  Recap/Listening dial        ListeningDial  Size=Card|Page
  Recap/Story bar             the story's TopBar.  Props: Period, Position
  Recap/Story progress        StoryProgress  Card=1..10
  Recap/Story actions         Save and Share
  Recap/Hero                  the page's headline block with the deck
  Recap/Stats                 the six stats
  Recap/Languages             the languages bar and legend
  Recap/When you listen       the clock block
Components / System
  Widget                      PlayerWidget layouts  Size=Small|Wide|Large, State=Playing|Empty
  System/Media notification   the shade's media control for Attic's session (drawn by Android)
  Splash                      Theme.Attic.Splash.Expressive
  Launcher icon               ic_launcher  Layer=Colour|Monochrome
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from figbuild.kit import Doc, V
from expressive.foundations import (DocPage, hrow, vcol, note, bind_dim, cover_path, shape_path, tstyle,
                                    ALBUMS, COLOR_MODE, slug, paragraph_line_heights,
                                    kebab, WALLPAPER, COVER_FROM_ROLE, COVER_FROM_APP)
from expressive import components_core as cc
from expressive.components_core import inst, set_text, set_cover, txt, frame, comp, spacer, MARK_PATH

REFS: dict[str, str] = {}

SRC = "ui/expressive/"
MARK_W, MARK_H = 36.0, 51.5

# The demo library's Recap (DemoStates.wrapped): songs with their cover index, artists with theirs.
TOP_SONGS = [("Lights Over the Water", "Mira Okafor", 0, 96), ("Iravu Rail", "Kavya Raman", 8, 81),
             ("Chai Ka Doosra Cup", "Ruhaan Sethi", 9, 74), ("Neon Peaches", "Juniper Vale", 3, 69),
             ("Night Drive Firmware", "Velvet Circuit", 10, 63), ("Kadalkarai", "Tide & Tamarind", 7, 57),
             ("Marea Lenta", "Paloma Ferrer", 2, 52), ("Kettle On", "Small Hours Tape Club", 1, 49),
             ("Platform Ettu", "Kavya Raman", 8, 44), ("Patang", "Ruhaan Sethi", 9, 41)]
TOP_ARTISTS = [("Mira Okafor", 0, 412), ("Kavya Raman", 8, 356), ("Ruhaan Sethi", 9, 298), ("Juniper Vale", 3, 201),
               ("Velvet Circuit", 10, 177)]
LANGUAGES = [("English", 41), ("Tamil", 29), ("Hindi", 21), ("No vocals", 9)]
# WrappedCard order, the cover each card is coloured by (recapCardCover), and its key suffix.
CARDS = [("Intro", 0, "intro"), ("Minutes", 3, "minutes"), ("Top song", 0, "top_song"), ("Top songs", 8, "top_songs"),
         ("Top artist", 0, "top_artist"), ("Top artists", 10, "top_artists"), ("Languages", 9, "languages"),
         ("Listening clock", 3, "listening_clock"), ("Highlights", 7, "highlights"), ("Summary", 0, "summary")]


def recap_hours() -> list[float]:
    """DemoStates.wrapped's hours, in the units the dial divides by the busiest hour."""
    out = []
    for h in range(24):
        evening = 3.2 if 19 <= h <= 23 else 1.0
        night = 2.0 if h in (0, 1) else 1.0
        asleep = 0.05 if 2 <= h <= 6 else 1.0
        out.append((40 + 30 * math.sin(h / 24.0 * math.pi)) * evening * night * asleep)
    return out


# ───────────────────────────────────────────────────────────────── helpers other modules use
def prop(ctx, comp_name, p):
    return ctx["components"][comp_name]["props"][p]


def set_props(d: Doc, i: int, **props):
    d.cmd("instance.setProperties", ids=[i], properties={k.replace("_", " "): v for k, v in props.items()})


def swap(d: Doc, i: int, comp_name: str):
    d.cmd("instance.swap", ids=[i], component=d.comp[comp_name])


def swap_icon(d: Doc, instance: int, lig: str, filled=False, layer="Icon"):
    icon = d.find(instance, name=layer, limit=1)[0]
    d.cmd("instance.swap", ids=[icon], component=d.comp[f"Icon/{lig}" + (" (filled)" if filled else "")])
    return icon


def mark(d: Doc, parent, height: float, color: str, *, x=None, y=None, name="Mark") -> int:
    """The Headroom mark (Mark.kt AtticMarkIcon) at a height; its width is height x 36 / 51.5."""
    w = round(height * MARK_W / MARK_H, 2)
    box = d.frame(parent, name=name, w=w, h=height, x=x, y=y)
    d.vector(box, name="Headroom", path=MARK_PATH, fill=color, x=0, y=0, w=w, h=height,
             constraints={"horizontal": "scale", "vertical": "scale"})
    return box


def scaled_card(d: Doc, parent, card_type: str, width: float, *, x=0, y=0, radius=0.0, name="Card") -> int:
    """A Recap/Card instance scaled to `width` inside a clipping frame with `radius` corners (the story,
    thumbnails and the share image all draw the one 360 x 640 card scaled, as the code does)."""
    h = round(width * 16 / 9, 2)
    box = d.frame(parent, name=name, w=width, h=h, x=x, y=y, clip=True, radius=radius or None)
    c = inst(d, "Recap/Card", box, name=f"Recap card · {card_type}", Kind=card_type, x=0, y=0)
    d.cmd("transform.scale", ids=[c], factor=width / 360.0)
    d.set(c, x=0, y=0)
    return box


def fit(d: Doc, t: int, max_w: float):
    """FitText: one line that shrinks to `max_w` rather than wrap or cut (the code scales the style's size and
    line height by the same factor). Overrides the text style's size only where the code does."""
    g = d.get(t)
    w = g["width"]
    if w > max_w + 0.5:
        k = max_w / w
        lh = g.get("lineHeight")
        size = g["fontSize"] * k
        d.set(t, fontSize=round(size, 2))
        try:
            d.set(t, lineHeight=round(float(str(lh).rstrip("px")) * k, 2))
        except (TypeError, ValueError):
            pass
    return t


def narrow(d: Doc, t: int, max_w: float, widths=(100, 75, 50)):
    """NarrowingText (Type.kt): one line that narrows along the width axis (100, then 75, then 50) before it would
    overflow. Overrides the style's wdth only where the code does."""
    g = d.get(t)
    if g["width"] <= max_w + 0.5:
        return t
    var = dict(g.get("fontVariations") or {})
    for w in widths[1:]:
        var["wdth"] = w
        d.set(t, fontVariations=var)
        if d.get(t)["width"] <= max_w + 0.5:
            break
    return t


def center_rotated(d: Doc, layer: int, parent: int, cx: float, cy: float):
    """Move a rotated layer so its visible box is centred on (cx, cy) in its parent."""
    pb, lb = d.get(parent)["absoluteBounds"], d.get(layer)["absoluteBounds"]
    g = d.get(layer)
    dx = (pb["x"] + cx) - (lb["x"] + lb["width"] / 2)
    dy = (pb["y"] + cy) - (lb["y"] + lb["height"] / 2)
    d.set(layer, x=g["x"] + dx, y=g["y"] + dy)


def wave_path(x0, x1, cy, wavelength, amplitude, phase=0.0, step=1.5):
    """drawWave in PlayControls.kt as SVG path data."""
    pts, x = [], x0
    while x <= x1:
        pts.append((x, cy + amplitude * math.sin(2 * math.pi * ((x - x0) / wavelength - phase))))
        x += step
    return "M" + " L".join(f"{a:.2f} {b:.2f}" for a, b in pts)


def stroke(d: Doc, parent, name, path, color, width, opacity=None):
    v = d.vector(parent, name=name, path=path, stroke=color, stroke_w=width, x=0, y=0)
    d.set(v, strokeCap="round", strokeJoin="round")
    if opacity is not None:
        d.set(v, opacity=opacity)
    return v


def place_vector(d: Doc, parent, name, path, fill, x, y, w, h):
    """A vector whose own box is (x, y, w, h): the path is drawn in those absolute coordinates."""
    v = d.vector(parent, name=name, path=path, fill=fill, x=0, y=0)
    d.set(v, x=x, y=y)
    return v


class Spec(cc.Spec):
    """components_core.Spec that also takes a source path under app/src/main/ (java/... or res/...)."""

    def make(self, name, title, description, file, *a, **kw):
        """`file` under ui/expressive/, or a path from app/src/main/ (java/... or res/...)."""
        old = cc.SRC
        if file.startswith(("java/", "res/")):
            cc.SRC = "app/src/main/"
        try:
            sid = super().make(name, title, description, file, *a, **kw)
        finally:
            cc.SRC = old
        return sid


# ───────────────────────────────────────────────────────────────── Components / States
def build_states(d: Doc, ctx: dict):
    page = DocPage(d, "Components / States", "States",
                   "What a screen shows when there is nothing yet, while it loads, before access is granted, and the "
                   "parts of the first run. The permission screen and the welcome step share one picture made of "
                   "Material shapes; the first run's rows are settings rows with a status at the end.")
    S = Spec(d, ctx, page)

    # Empty state -------------------------------------------------------------------------------
    def empty(vp):
        art = vp["Art"]
        c = comp(d, "Empty state", layout="v", w=411, pad=[48, 32, 48, 32], cross="center", sizing=("fixed", "hug"))
        lay = {}
        if art != "None":
            size = 128 if art == "Clover" else 112
            # The picture centres its glyph (contentAlignment = Center); the shape behind is out of the flow, and the
            # glyph fills a 48dp box so a swapped glyph keeps its size and its place.
            box = frame(d, c, "Art", layout="h", w=size, h=size, main="center", cross="center")
            if art == "Clover":
                sh = inst(d, "Shape/Clover 4", box, name="Clover", x=0, y=0)
                d.set(sh, absolute=True)
                d.set(sh, x=0, y=0, width=size, height=size, constraints={"horizontal": "scale", "vertical": "scale"})
                d.override(sh, "Outline", fill="Color/secondary-container")
            else:
                r = d.rect(box, name="Rounded square", w=size, h=size, x=0, y=0, fill="Color/secondary-container", radius=36)
                d.set(r, absolute=True)
                d.set(r, x=0, y=0, constraints={"horizontal": "scale", "vertical": "scale"})
            gb = frame(d, box, "Glyph box", layout="h", w=48, h=48)
            ic = d.icon(gb, "folder_open", size=48, color="Color/on-secondary-container", name="Icon")
            d.set(ic, sizingH="fill", sizingV="fill")
            lay["icon"] = ic
            spacer(d, c, "Gap art", w=1, h=24)
        lay["title"] = txt(d, c, "No songs on this phone", "Headline/Small Emphasized", "Color/on-surface", "Title",
                           fill=True, align="center")
        bbox = frame(d, c, "Body box", layout="v", pad=[8, 0, 0, 0], sizing=("fill", "hug"))
        lay["body"] = txt(d, bbox, "Copy music files onto this phone, in Music or any other folder, and they will show up here.",
                          "Body/Medium", "Color/on-surface-variant", "Body", fill=True, align="center")
        lay["bbox"] = bbox
        return c, lay

    S.make("Empty state", "Empty state",
           "Nothing to show: an optional picture (a 128dp clover, or a 112dp square with 36dp corners, in secondary "
           "container with a 48dp glyph), the emphasised headline and a sentence, centred, 32dp from the sides and 48 "
           "above and below. Home with no songs, an empty queue, no search results, an empty playlist. Swap the glyph "
           "with Icon; it stays 48dp and centred.",
           "Surfaces.kt", "EmptyState", [dict(Art=a) for a in ("Clover", "Rounded", "None")], empty,
           props=[("Title", "text", "title", None, None), ("Body", "text", "body", None, None),
                  ("Show body", "boolean", "bbox", True, None), ("Icon", "instanceSwap", "icon", None, None)])

    # Loading indicator ---------------------------------------------------------------------------
    def loading(vp):
        cont = vp["Contained"] == "True"
        size, active = (72, 30) if cont else (48, 38)
        c = comp(d, "Loading indicator", w=size, h=size)
        if cont:
            d.ellipse(c, name="Container", w=size, h=size, x=0, y=0, fill="Color/primary-container")
        sh = inst(d, "Shape/Loader four leaf", c, name="Four-leaf shape", x=(size - active) / 2, y=(size - active) / 2)
        d.set(sh, width=active, height=active)
        # The frame the renders catch: the four-leaf shape turned 45 degrees, reading as a rounded square.
        d.set(sh, rotation=45)
        center_rotated(d, sh, c, size / 2, size / 2)
        d.override(sh, "Outline", fill="Color/on-primary-container" if cont else "Color/primary")
        return c, {}

    S.make("Loading indicator", "Loading indicator",
           "Material's loading indicator: seven shapes morphing one into the next every 650ms while the whole turns. "
           "This frame shows one of them. Plain is 38dp in primary in a 48dp box (licences, Recap, lyrics); contained puts "
           "a 30dp shape in on primary container on a primary container disc (the first run's scan, 72dp there).",
           "PlayControls.kt", "LoadingIndicator", [dict(Contained="False"), dict(Contained="True")], loading)

    # Shapes card ---------------------------------------------------------------------------------
    def shapes(vp):
        kind = vp["Kind"]
        h = 300 if kind == "Welcome" else 320
        c = comp(d, "Shapes card", w=379, h=h, fill="Color/surface-container", clip=True)
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        g = frame(d, c, "Shapes", w=284, h=216, x=47.5, y=(h - 216) / 2)
        arch = frame(d, g, "Arch", w=164, h=200, x=0, y=16, fill="Color/primary-container", cornerRadius=[82, 82, 28, 28])
        mark(d, arch, 72, "Color/on-primary-container", x=round((164 - 72 * MARK_W / MARK_H) / 2, 2), y=71)
        refused = kind == "Refused"
        cl = inst(d, "Shape/Clover 4", g, name="Clover", x=176, y=0)
        d.set(cl, width=108, height=108)
        d.override(cl, "Outline", fill="Color/surface-container-highest" if refused else "Color/secondary-container")
        lig = {"Permission": "music_note", "Refused": "music_off", "Welcome": "folder"}[kind]
        ic = d.icon(g, lig, size=36, color="Color/error" if refused else "Color/on-secondary-container", name="Clover icon")
        d.set(ic, x=176 + 36, y=36)
        pill = frame(d, g, "Pill", w=108, h=80, x=176, y=136, fill="Color/surface-container-highest")
        bind_dim(d, pill, "cornerRadius", "radius/full")
        ic2 = d.icon(g, "airplanemode_active" if kind == "Welcome" else "notifications", size=36,
                     color="Color/on-surface-variant", name="Pill icon")
        d.set(ic2, x=176 + 36, y=136 + 22)
        return c, {}

    S.make("Shapes card", "Shapes card",
           "The picture at the head of the permission screen and the first run's welcome: an arch holding the mark, a "
           "clover and a pill, 284 x 216 on a surface container card with 28dp corners (320dp tall, 300 on the welcome). "
           "Permission: a note in the clover for audio, a bell in the pill for notifications. Refused: the clover goes "
           "surface container highest and the note becomes music_off in error, on defaultEffects. Welcome: a folder for "
           "your own files and an aeroplane for playing offline.",
           "ExpressiveStates.kt", "GateArt",
           [dict(Kind=k) for k in ("Permission", "Refused", "Welcome")], shapes)

    # Look card -----------------------------------------------------------------------------------
    def expressive_preview(parent):
        p = frame(d, parent, "Expressive picture", layout="v", w=399, fill="Color/surface", sizing=("fixed", "hug"))
        head = frame(d, p, "Title row", layout="h", gap=10, pad=[20, 16, 8, 16], cross="center", sizing=("fill", "hug"))
        mark(d, head, 32, "Color/primary")
        txt(d, head, "Home", "Headline/Medium Emphasized", "Color/on-surface", "Title")
        g = inst(d, "Connected button group", p, name="Filters", Options="3", Selected="1", On_cell="False")
        d.set(g, sizingH="fill")
        seg_label = prop(ctx, "Connected button group/Segment", "Label")
        for n, lab in enumerate(("All", "Albums", "Artists")):
            set_text(d, d.child(g, f"Segment {n + 1}"), seg_label, lab)
        row = frame(d, p, "Covers", layout="h", gap=12, pad=[16, 16, 20, 16], sizing=("fill", "hug"))
        for n in range(3):
            cv = inst(d, "Cover", row, name=f"Cover {n + 1}", Kind="Album", Size="112")
            d.set(cv, sizingH="fill", height=114.33)
            for pic in d.find(cv, name="Picture", limit=4):
                d.set(pic, visible=False)
        nb = inst(d, "Navigation bar", p, name="Navigation bar", Selected="Home")
        d.set(nb, paddingBottom=0, height=64, sizingH="fill")
        return p

    def comfort_preview(parent):
        """ComfortLookPreview (ComfortOnboarding.kt): the hero title, SubTabs, three no-art covers and the Dock's
        TabBar, in the Comfort kit's colours and Onest."""
        cp = "Comfort preview/"
        p = frame(d, parent, "Comfort picture", layout="v", w=399, fill=cp + "ground", sizing=("fixed", "hug"))
        t = frame(d, p, "Title row", layout="h", pad=[28, 24, 16, 24], sizing=("fill", "hug"))
        txt(d, t, "Home", "First run/Comfort hero", cp + "on", "Title")
        # SubTabs: 48dp tabs 2dp apart with 10dp sides, 16dp in; the selected one bold over a 3dp accent line
        # 7dp above its foot, inset 10 at each side.
        tabs = frame(d, p, "Sub tabs", layout="h", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        for n, lab in enumerate(("All", "Albums", "Artists")):
            sel = n == 0
            tab = frame(d, tabs, f"Tab {lab}", layout="h", h=48, pad=[0, 10, 0, 10], cross="center", main="center",
                        sizing=("hug", "fixed"))
            lt = txt(d, tab, lab, "First run/Comfort tab selected" if sel else "First run/Comfort tab",
                     cp + ("on" if sel else "on-2"), "Label")
            if sel:
                tw = d.get(lt)["width"]
                u = d.rect(tab, name="Underline", w=tw, h=3, fill=cp + "accent", radius=2)
                d.set(u, absolute=True, x=10, y=48 - 7 - 3)
        row = frame(d, p, "Covers", layout="h", gap=14, pad=[16, 24, 0, 24], sizing=("fill", "hug"))
        side = round((399 - 48 - 2 * 14) / 3, 2)
        for n in range(3):
            cv = frame(d, row, f"Cover {n + 1}", layout="h", h=side, cross="center", main="center", fill=cp + "fill",
                       sizing=("fill", "fixed"), radius=22)
            d.icon(cv, "music_note", size=48, color=cp + "on-2", name="No art")
        # Dock with no insets: 28dp above the card, 12 at the sides, 22 below; the TabBar is 60dp with 8dp padding
        # and 44dp pill items 4dp apart.
        dockbox = frame(d, p, "Dock box", layout="h", pad=[28, 12, 22, 12], sizing=("fill", "hug"))
        dock = frame(d, dockbox, "Dock", layout="h", gap=4, pad=8, h=60, cross="center", fill=cp + "raised",
                     sizing=("fill", "fixed"), radius=30)
        for n, (lab, lig) in enumerate((("Home", "home"), ("Search", "search"), ("Library", "library_music"))):
            sel = n == 0
            it = frame(d, dock, f"Tab {lab}", layout="h", gap=7, h=44, cross="center", main="center",
                       fill=cp + "accent-soft" if sel else None, sizing=("fill", "fixed"), radius=22)
            ink = cp + ("on-accent-soft" if sel else "on-2")
            d.icon(it, lig, size=24, color=ink, filled=sel, name="Icon")
            txt(d, it, lab, "First run/Comfort dock selected" if sel else "First run/Comfort dock", ink, "Label")
        return p

    def look(vp):
        style, sel = vp["Style"], vp["Selected"] == "True"
        c = comp(d, "First run/Look card", layout="v", w=183.5, pad=8, sizing=("fixed", "hug"),
                 fill="Color/secondary-container" if sel else "Color/surface-container",
                 stroke="Color/primary" if sel else None, stroke_w=2 if sel else None)
        if sel:
            d.set(c, strokeAlign="inside")
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        clip = frame(d, c, "Preview", w=167.5, h=10, sizing=("fill", "fixed"), clip=True)
        bind_dim(d, clip, "cornerRadius", "radius/large-increased")
        pic = expressive_preview(clip) if style == "Expressive" else comfort_preview(clip)
        d.set(pic, x=0, y=0)
        # The picture is drawn at 0.42, and a field bound to a Dimension token keeps the token's value through
        # transform.scale: the instances in it let go of their gap, padding and size tokens first.
        inner = d.find(pic, limit=5000)
        for field in ("gap", "paddingTop", "paddingRight", "paddingBottom", "paddingLeft", "width", "height", "minHeight"):
            try:
                d.bind(inner, field, None)
            except Exception:
                pass
        d.cmd("transform.scale", ids=[pic], factor=0.42)
        d.set(pic, x=0, y=0)
        d.set(clip, height=round(d.get(pic)["height"], 2))
        row = frame(d, c, "Choice", layout="h", gap=4, pad=[4, 0, 0, 4], cross="center", sizing=("fill", "hug"))
        d.set(row, minHeight=48)
        inst(d, "Radio", row, name="Radio", Selected=str(sel))
        txt(d, row, style, "First run/Look label", "Color/on-secondary-container" if sel else "Color/on-surface", "Label")
        return c, {}

    S.make("First run/Look card", "Look card",
           "One style on the first run's look step: a live picture of that style's Home, drawn at 0.42 and clipped to "
           "20dp corners, over a radio and the style's name. Selected takes secondary container and a 2dp primary edge. "
           "The pictures follow the theme being picked; the Comfort one uses the Comfort kit's colours.",
           "ExpressiveOnboarding.kt", "LookCard",
           [dict(Style=s, Selected=v) for s in ("Expressive", "Comfort") for v in ("True", "False")], look)

    # Access row -----------------------------------------------------------------------------------
    def access(vp):
        st = vp["State"]
        c = comp(d, "First run/Access row", layout="h", w=379, gap=16, pad=[14, 16, 14, 16], cross="center",
                 sizing=("fixed", "hug"))
        d.set(c, minHeight=72)
        circ = frame(d, c, "Icon circle", layout="h", w=40, h=40, cross="center", main="center", fill="Color/secondary-container")
        bind_dim(d, circ, "cornerRadius", "radius/full")
        lay = {"icon": d.icon(circ, "notifications", size=20, color="Color/on-secondary-container", name="Icon")}
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        lay["title"] = txt(d, col, "Notifications", "Kit/Setting title", "Color/on-surface", "Title", fill=True)
        lay["body"] = txt(d, col, "Optional. Shows the playback controls in the notification shade and on the lock screen.",
                          "Body/Medium", "Color/on-surface-variant", "Body", fill=True)
        if st == "Allowed":
            t = frame(d, c, "Allowed", layout="h", gap=4, cross="center", sizing=("hug", "hug"))
            d.icon(t, "check", size=18, color="Color/primary", name="Check")
            txt(d, t, "Allowed", "Label/Large", "Color/primary", "Label")
        elif st == "Allow":
            b = inst(d, "Button", c, name="Allow", Style="Tonal", Size="Small", Icon="None", State="Enabled", Squared="False")
            set_text(d, b, prop(ctx, "Button", "Label"), "Allow")
        elif st.startswith("Off"):
            txt(d, c, "Off", "Label/Large", "Color/error" if st == "Off required" else "Color/on-surface-variant", "Off")
        return c, lay

    S.make("First run/Access row", "Access row",
           "One permission on the first run's access step, as a settings row (72dp minimum): the glyph in a tonal "
           "circle, what it is and why, then where it stands. Allow is a tonal button while notifications have not been "
           "asked; audio has none, because the step's own button asks for it. Allowed is a tick and the word in primary. "
           "Off is in error for audio, which Attic needs, and on surface variant for notifications.",
           "ExpressiveOnboarding.kt", "AccessRow",
           [dict(State=s) for s in ("None", "Allow", "Allowed", "Off", "Off required")], access,
           props=[("Title", "text", "title", None, None), ("Body", "text", "body", None, None),
                  ("Icon", "instanceSwap", "icon", None, None)])

    # Lyrics row, radio note, scanning ------------------------------------------------------------
    def lyrics_row(vp):
        on = vp["Checked"] == "True"
        c = comp(d, "First run/Lyrics row", layout="h", w=379, gap=16, pad=[12, 16, 12, 16], cross="center",
                 sizing=("fixed", "hug"))
        d.set(c, minHeight=64)
        d.icon(c, "lyrics", color="Color/on-surface-variant", name="Leading icon")
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        txt(d, col, "Download lyrics for all songs", "Kit/Setting title", "Color/on-surface", "Title", fill=True)
        txt(d, col, "Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are never sent.",
            "Body/Medium", "Color/on-surface-variant", "Summary", fill=True)
        inst(d, "Switch", c, name="Switch", Checked=str(on))
        return c, {}

    S.make("First run/Lyrics row", "Lyrics row",
           "The library step's offer to download lyrics: Surfaces.kt SetSwitchRow with a plain leading glyph (SetRow's "
           "leading symbol, no circle), the lrclib.net note as its summary and a switch. Off until the person turns it on.",
           "Surfaces.kt", "SetSwitchRow", [dict(Checked="False"), dict(Checked="True")], lyrics_row)

    def radio_note(vp):
        c = comp(d, "First run/Radio note", layout="v", w=379, pad=16, sizing=("fixed", "hug"))
        txt(d, c, "Radio analyses your songs while the phone charges. It all happens on this phone.", "Body/Medium",
            "Color/on-surface-variant", "Note text", fill=True)
        bb = frame(d, c, "Button box", layout="h", pad=[8, 0, 0, 0], sizing=("hug", "hug"))
        b = inst(d, "Button", bb, name="Song radio settings", Style="Text", Size="Small", Icon="Leading", State="Enabled",
                 Squared="False")
        set_text(d, b, prop(ctx, "Button", "Label"), "Song radio settings")
        set_props(d, b, **{"Leading icon": d.comp["Icon/graphic_eq"]})
        # .offset(x = (-12).dp): the button's label lines up with the text.
        d.set(bb, paddingLeft=0)
        return c, {}

    S.make("First run/Radio note", "Radio note",
           "The library step's note on song radio, a SetNote with a text button that opens Settings at Song radio. The "
           "button sits 8dp under the text and 12dp to the left, so its label lines up with the text.",
           "ExpressiveOnboarding.kt", "LibraryStep", [{}], radio_note, single=True)

    def scanning(vp):
        c = comp(d, "First run/Scanning", layout="v", w=411, pad=[96, 0, 0, 0], cross="center", sizing=("fixed", "hug"))
        inst(d, "Loading indicator", c, name="Loading indicator", Contained="True")
        tb = frame(d, c, "Title box", layout="v", pad=[24, 0, 0, 0], sizing=("hug", "hug"))
        txt(d, tb, "Finding your songs…", "Title/Large", "Color/on-surface", "Title")
        return c, {}

    S.make("First run/Scanning", "Scanning",
           "The library step while the first scan runs: the contained loading indicator at 72dp, 96dp under the bar, "
           "and \"Finding your songs…\" in title large 24dp under it.",
           "ExpressiveOnboarding.kt", "LibraryStep", [{}], scanning, single=True)


# ───────────────────────────────────────────────────────────────── settings rows (Lists and cards)
def build_setting_rows(d: Doc, ctx: dict):
    page = DocPage.below(d, "Components / Lists and cards")
    # Rows only Settings draws, next to List row/Setting: the saved-lyrics and About rows (text with no title), the
    # rows that carry a progress line, and the Style cell.
    S = Spec(d, ctx, page)

    def circle(parent, lig="lyrics"):
        circ = frame(d, parent, "Icon circle", layout="h", w=40, h=40, cross="center", main="center",
                     fill="Color/secondary-container")
        bind_dim(d, circ, "cornerRadius", "radius/full")
        if lig == "mark":
            mark(d, circ, 22, "Color/on-secondary-container", name="Icon")
            return circ, None
        return circ, d.icon(circ, lig, size=20, color="Color/on-secondary-container", name="Icon")

    def text_row(vp):
        lead = vp["Lead"]
        c = comp(d, "List row/Setting text", layout="h", w=379, gap=16, pad=[12, 16, 12, 16], sizing=("fixed", "hug"))
        d.set(c, minHeight=64)
        circ, ic = circle(c, "mark" if lead == "Mark" else "lyrics")
        if lead == "Mark":
            t = txt(d, c, "Attic plays your own music files, from any folder. It has no account and no streaming. "
                          "69 songs on this phone.", "Body/Medium", "Color/on-surface-variant", "Paragraph", fill=True)
        else:
            s = "51 of 69 songs have lyrics saved on this phone. Saved lyrics work offline."
            t = txt(d, c, s, "Body/Medium", "Color/on-surface-variant", "Paragraph", fill=True)
            n = len("51 of 69 songs")
            d.cmd("style.apply", ids=[t], style=d.text_styles["Label/Large"], start=0, end=n)
        lay = {"t": t}
        if ic:
            lay["icon"] = ic
        return c, lay

    S.make("List row/Setting text", "Setting text row",
           "A settings row that is one paragraph and no title: the glyph (or the mark at 22dp) in a tonal circle, then "
           "body medium in on surface variant. The saved-lyrics row picks out \"51 of 69 songs\" at medium weight; the "
           "code also draws that span in on surface, which a text range can't bind to a variable here, so the drawing "
           "keeps it in on surface variant (SavedLyricsRow). The About row reads the library's size.",
           "ExpressiveSettings.kt", "SavedLyricsRow", [dict(Lead="Icon"), dict(Lead="Mark")], text_row,
           props=[("Text", "text", "t", None, None), ("Icon", "instanceSwap", "icon", None, None)])

    
    def progress_bar(parent, fraction, wavy):
        """LinearProgress / WavyLinearProgress across the text column, at `fraction`."""
        p = inst(d, "Progress/Wavy" if wavy else "Progress/Linear", parent, name="Progress",
                 **({"Running": "True"} if wavy else {}))
        d.set(p, sizingH="fill")
        cc.set_progress(d, p, fraction)
        return p

    def prog_row(vp):
        kind = vp["Kind"]
        lyr = kind.startswith("Lyrics")
        c = comp(d, "List row/Setting progress", layout="h", w=379, gap=16, pad=[12, 16, 12, 16], sizing=("fixed", "hug"))
        d.set(c, minHeight=64)
        circle(c, "download" if lyr else "graphic_eq")
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        if lyr:
            txt(d, col, "Download lyrics for all songs", "Kit/Setting title", "Color/on-surface", "Title", fill=True)
            txt(d, col, "Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are "
                        "never sent.", "Body/Medium", "Color/on-surface-variant", "Summary", fill=True)
        else:
            txt(d, col, "Radio finds similar songs by listening to your files. This happens on this phone, and nothing is "
                        "uploaded.", "Body/Medium", "Color/on-surface-variant", "Summary", fill=True)
        pb = frame(d, col, "Progress box", layout="v", pad=[14, 0, 6, 0], sizing=("fill", "hug"))
        frac = {"Analysis done": 1.0, "Analysis running": 41 / 69, "Analysis partial": 41 / 69, "Lyrics running": 23 / 69}[kind]
        progress_bar(pb, frac, kind == "Analysis running")
        status = {"Analysis done": "All 69 songs analysed.", "Analysis running": "Analysing songs: 41 of 69",
                  "Analysis partial": "41 of 69 songs analysed. The rest continue next time the phone charges.",
                  "Lyrics running": "Checked 23 of 69, found 17"}[kind]
        st = txt(d, col, status, "Body/Small", "Color/on-surface-variant", "Status", fill=True)
        if kind == "Analysis partial":
            bb = frame(d, col, "Button box", layout="h", pad=[14, 0, 0, 0], sizing=("hug", "hug"))
            b = inst(d, "Button", bb, name="Analyse now", Style="Tonal", Size="Small", Icon="None", State="Enabled",
                     Squared="False")
            set_text(d, b, prop(ctx, "Button", "Label"), "Analyse now without a charger")
        return c, {"status": st}

    S.make("List row/Setting progress", "Setting progress row",
           "Settings rows that report a job: song analysis (no title, the privacy note, a line that is wavy while songs "
           "are analysed and flat otherwise, the count, and \"Analyse now without a charger\" when it can start) and the "
           "bulk lyrics download while it runs (title, the lrclib.net note, the line and \"Checked n of m, found k\"). "
           "The line starts 14dp under the text and the status 6dp under it.",
           "ExpressiveSettings.kt", "AnalysisRow",
           [dict(Kind=k) for k in ("Analysis done", "Analysis running", "Analysis partial", "Lyrics running")], prog_row,
           props=[("Status", "text", "status", None, None)])

    def style_cell(vp):
        sel = vp["Selected"]
        c = comp(d, "List row/Style picker", layout="v", w=379, pad=[0, 0, 12, 0], sizing=("fixed", "hug"))
        r = inst(d, "List row/Setting", c, name="Style row", Leading="Icon", Lines="One", Trailing="None", Status_line="False")
        d.set(r, sizingH="fill")
        set_text(d, r, prop(ctx, "List row/Setting", "Title"), "Style")
        set_props(d, r, Icon=d.comp["Icon/palette"])
        g = inst(d, "Connected button group", c, name="Style group", Options="2",
                 Selected="1" if sel == "Expressive" else "2", On_cell="True")
        fit_group(d, g, 379)
        return c, {}

    S.make("List row/Style picker", "Style picker",
           "Settings' Style cell: a one-line row with the palette glyph, then Expressive and Comfort as a connected "
           "group on surface container highest segments, 12dp above the cell's foot. Switching restyles the whole app "
           "at once; the setting is the app's, not the theme's.",
           "ExpressiveSettings.kt", "StyleCell", [dict(Selected="Expressive"), dict(Selected="Comfort")], style_cell)


def fit_group(d: Doc, g: int, width: float):
    """A Connected button group instance set to `width` (its 16dp side padding kept). The group is a grid of fr
    columns, so the segments share the width themselves."""
    d.set(g, width=width)
    return g


# ───────────────────────────────────────────────────────────────── Components / Recap
def build_recap(d: Doc, ctx: dict):
    page = DocPage(d, "Components / Recap", "Recap",
                   "Recap's ten cards, the story's chrome and the Recap page's blocks. A card is one 360 x 640 drawing "
                   "that the story, the page's thumbnails, the headline's deck and the shared 1080 x 1920 image all "
                   "scale; place a Recap/Card instance and scale it (K), never redraw it. Each card takes the dark scheme "
                   "of one of the period's covers in both themes (its variant sets the Color mode: Paper Lanterns is "
                   "Cover, the others are the album modes this module adds). The share image is the card itself, square "
                   "cornered, at 3x: the screens page shows it as a 411 x 731 frame.")
    S = Spec(d, ctx, page)
    hours = recap_hours()
    peak = max(range(24), key=lambda h: hours[h])
    most = max(hours)

    # Listening dial --------------------------------------------------------------------------------
    def dial(vp):
        card = vp["Size"] == "Card"
        size = 324 if card else 172
        u = size / 288
        c = comp(d, "Recap/Listening dial", w=size, h=size, clip=False)
        cx = cy = 144 * u
        inner, outer, sw = 34 * u, 100 * u, 8.5 * u
        tracks, bars = [], []
        for h in range(24):
            a = h / 24 * 2 * math.pi - math.pi / 2
            dx, dy = math.cos(a), math.sin(a)
            tracks.append(f"M{cx + dx * inner:.2f} {cy + dy * inner:.2f} L{cx + dx * outer:.2f} {cy + dy * outer:.2f}")
            ln = inner + (outer - inner) * hours[h] / most
            if h != peak:
                bars.append(f"M{cx + dx * inner:.2f} {cy + dy * inner:.2f} L{cx + dx * ln:.2f} {cy + dy * ln:.2f}")
            else:
                peak_path = f"M{cx + dx * inner:.2f} {cy + dy * inner:.2f} L{cx + dx * ln:.2f} {cy + dy * ln:.2f}"
                peak_at = (cx + dx * 115 * u, cy + dy * 115 * u)
        box = frame(d, c, "Drawing", w=size, h=size, x=0, y=0)
        for name, p, col in (("Tracks", " ".join(tracks), "Color/surface-container-highest"),
                             ("Bars", " ".join(bars), "Color/recap/dial-bar"), ("Busiest hour", peak_path, "Color/primary")):
            v = d.vector(box, name=name, path=p, stroke=col, stroke_w=sw, x=0, y=0)
            d.set(v, strokeCap="round")
        sty, strong = ("Recap/Dial label", "Recap/Dial label strong") if card else ("Recap/Dial label page", "Recap/Dial label page strong")

        def label(text, x, y, anchor, is_peak):
            t = txt(d, c, text, strong if is_peak else sty, "Color/primary" if is_peak else "Color/on-surface-variant",
                    f"Hour {text}")
            g = d.get(t)
            w, hh = g["width"], g["height"]
            xx = x - w if anchor < 0 else (x - w / 2 if anchor == 0 else x)
            d.set(t, x=round(xx, 2), y=round(y - hh / 2, 2))

        label("12 am", 144 * u, 30 * u, 0, False)
        label("6 am", 254 * u, 144 * u, 1, False)
        label("12 pm", 144 * u, 260 * u, 0, False)
        label("6 pm", 34 * u, 144 * u, -1, False)
        anchor = -1 if peak_at[0] < cx - 8 * u else (1 if peak_at[0] > cx + 8 * u else 0)
        label(f"{peak - 12} pm" if peak > 12 else f"{peak} am", peak_at[0], peak_at[1], anchor, True)
        hub = frame(d, c, "Hub", layout="h", w=size * 0.23, h=size * 0.23, x=size * 0.385, y=size * 0.385, cross="center",
                    main="center", fill="Color/secondary-container")
        bind_dim(d, hub, "cornerRadius", "radius/full")
        d.icon(hub, "nights_stay", size=20 if not card else 20, color="Color/on-secondary-container", filled=True, name="Listener glyph")
        return c, {}

    S.make("Recap/Listening dial", "Listening dial",
           "The listening clock: 24 round-capped bars from a hub, midnight at the top, each as long as that hour's "
           "listening against the busiest. Tracks in surface container highest, bars in primary mixed 58 % into the "
           "ground, the busiest hour in primary with its time beside it, the listener type's glyph in the hub (secondary "
           "container). Card is 324dp with 11-unit labels; Page is 172dp with 17-unit labels (of the drawing's 288).",
           "ExpressiveRecapCards.kt", "ListeningDial", [dict(Size="Card"), dict(Size="Page")], dial)

    # Card ------------------------------------------------------------------------------------------
    def footer(c, ground, ink):
        f = frame(d, c, "Footer", layout="h", w=312, h=28, x=24, y=640 - 20 - 28, gap=8, cross="center")
        mb = frame(d, f, "Mark box", layout="h", w=28, h=28, cross="center", main="center", fill=ink, radius=9)
        mark(d, mb, 16, ground)
        txt(d, f, "Attic", "Recap/Footer name", ink, "Name", fill=True)
        pill = frame(d, f, "Period", layout="h", pad=[4, 10, 4, 10], sizing=("hug", "hug"))
        bind_dim(d, pill, "cornerRadius", "radius/full")
        bg = d.rect(pill, name="Pill ink", w=10, h=10, fill=ink)
        d.set(bg, absolute=True)
        d.set(bg, x=0, y=0, opacity=0.14, constraints={"horizontal": "leftRight", "vertical": "topBottom"})
        t = txt(d, pill, "2026 so far", "Recap/Small label", ink, "Period")
        g = d.get(t)
        d.set(bg, width=g["width"] + 20, height=g["height"] + 8)
        d.set(pill, clip=True)
        return f

    def cover_img(parent, idx, size, radius, name="Cover", x=None, y=None, artist=False):
        if artist:
            cv = inst(d, "Cover", parent, name=name, Kind="Artist", Size="112")
            d.set(cv, width=size, height=size)
        else:
            cv = inst(d, "Cover", parent, name=name, Kind="Album", Size="208")
            d.set(cv, width=size, height=size, cornerRadius=radius)
        set_cover(d, cv, idx)
        if x is not None:
            d.set(cv, x=x, y=y)
        return cv

    def card(vp):
        kind = vp["Kind"]
        cover_idx = {t: ci for t, ci, _ in CARDS}[kind]
        grounds = {"Intro": ("Color/surface-container", "Color/on-surface"), "Minutes": ("Color/primary", "Color/on-primary"),
                   "Top song": ("Color/primary-container", "Color/on-primary-container"),
                   "Top songs": ("Color/primary", "Color/on-primary"),
                   "Top artist": ("Color/primary-container", "Color/on-primary-container"),
                   "Top artists": ("Color/surface-container", "Color/on-surface"),
                   "Languages": ("Color/surface-container", "Color/on-surface"),
                   "Listening clock": ("Color/surface-container", "Color/on-surface"),
                   "Highlights": ("Color/tertiary-container", "Color/on-tertiary-container"),
                   "Summary": ("Color/surface-container", "Color/on-surface")}
        ground, ink = grounds[kind]
        c = comp(d, "Recap/Card", w=360, h=640, fill=ground, clip=True)
        d.set_mode(c, "Color", COLOR_MODE[cover_idx])
        d.set_mode(c, "Art", "Dark")
        d.set_mode(c, "Cover", "Dark")
        if kind == "Intro":
            col = frame(d, c, "Text block", layout="v", gap=14, w=312, x=24, y=40, sizing=("fixed", "hug"))
            txt(d, col, "Your year so far", "Recap/Intro headline", ink, "Headline", fill=True)
            txt(d, col, "18,420 minutes across 69 songs. Here's what you played.", "Recap/Intro body",
                "Color/on-surface-variant", "Body", fill=True)
            cell = (304 - 8) / 2
            specs = [("2", "Circle", "Color/primary", "Color/on-primary"), ("0", "Cookie 9", "Color/primary-container", "Color/on-primary-container"),
                     ("2", "Arch", "Color/surface-container-highest", "Color/primary"), ("6", "Puff", "Color/tertiary-container", "Color/on-tertiary-container")]
            grid = frame(d, c, "Year", w=304, h=304, x=28, y=640 - 74 - 304)
            for n, (ch, shp, fill_, ink_) in enumerate(specs):
                x, y = (n % 2) * (cell + 8), (n // 2) * (cell + 8)
                box = frame(d, grid, f"Digit {ch} · {shp}", w=cell, h=cell, x=x, y=y)
                if shp == "Circle":
                    d.ellipse(box, name="Circle", w=cell, h=cell, x=0, y=0, fill=fill_)
                elif shp == "Arch":
                    d.rect(box, name="Arch", w=cell, h=cell, x=0, y=0, fill=fill_)
                    d.set(d.find(box, name="Arch")[0], cornerRadius=[74, 74, 24, 24])
                else:
                    s = inst(d, f"Shape/{shp}", box, name=shp, x=0, y=0)
                    d.set(s, width=cell, height=cell)
                    d.override(s, "Outline", fill=fill_)
                t = txt(d, box, ch, "Recap/Intro digit", ink_, "Character", align="center")
                d.set(t, textAutoResize="none", width=cell, height=104, x=0, y=(cell - 104) / 2)
        elif kind == "Minutes":
            dr = frame(d, c, "Seek line", w=360, h=112, x=0, y=72, clip=True)
            prog = 325 / 365
            thumb = 348 * prog
            stroke(d, dr, "Wave", wave_path(-48, thumb - 22, 56, 96, 26, 0.5), "Color/on-primary", 18)
            d.rect(dr, name="Thumb", w=12, h=112, x=thumb, y=0, fill="Color/on-primary", radius=6)
            d.rect(dr, name="Rest", w=372 - (thumb + 26), h=18, x=thumb + 26, y=47, fill="Color/inverse-primary", radius=9)
            col = frame(d, c, "Text block", layout="v", w=312, x=24, y=0, sizing=("fixed", "hug"))
            txt(d, col, "You listened for", "Recap/Label", ink, "Label", fill=True)
            fb = frame(d, col, "Figure box", layout="h", pad=[4, 0, 0, 0], sizing=("fill", "hug"))
            fig = fit(d, txt(d, fb, "18,420", "Recap/Minutes figure", ink, "Minutes"), 312)
            txt(d, col, "minutes", "Recap/Minutes unit", ink, "Unit")
            spacer(d, col, "Gap", w=1, h=14)
            txt(d, col, "That's about 307 hours across 301 days.", "Recap/Body large", ink, "Detail", fill=True)
            d.set(col, y=640 - 80 - d.get(col)["height"])
        elif kind == "Top song":
            col = frame(d, c, "Content", layout="v", w=312, x=24, y=24, sizing=("fixed", "hug"))
            cover_img(col, 0, 312, 28)
            row = frame(d, col, "Song", layout="h", gap=8, pad=[18, 0, 0, 0], cross="center", sizing=("fill", "hug"))
            tc = frame(d, row, "Text column", layout="v", sizing=("fill", "hug"))
            txt(d, tc, "Your top song", "Recap/Label", ink, "Label", fill=True)
            spacer(d, tc, "Gap 1", w=1, h=4)
            txt(d, tc, "Lights Over the Water", "Recap/Song headline", ink, "Title", fill=True)
            spacer(d, tc, "Gap 2", w=1, h=6)
            txt(d, tc, "Mira Okafor", "Recap/Caption", "Color/on-surface-variant", "Artist", fill=True)
            badge = frame(d, row, "Plays badge", w=112, h=112)
            s = inst(d, "Shape/Sunny 8", badge, name="Sunny", x=0, y=0)
            d.set(s, width=112, height=112)
            d.override(s, "Outline", fill="Color/primary")
            bc = frame(d, badge, "Figure", layout="v", w=112, h=112, x=0, y=0, cross="center", main="center")
            txt(d, bc, "96", "Recap/Badge figure", "Color/on-primary", "Number")
            txt(d, bc, "plays", "Recap/Badge word", "Color/on-primary", "Word")
        elif kind == "Top songs":
            col = frame(d, c, "Content", layout="v", w=312, x=24, y=32, sizing=("fixed", "hug"))
            txt(d, col, "Songs you kept coming back to", "Recap/List headline", ink, "Headline", fill=True)
            spacer(d, col, "Gap", w=1, h=20)
            rows = frame(d, col, "Rows", layout="v", gap=3, sizing=("fill", "hug"))
            for n, (title, artist, ci, plays) in enumerate(TOP_SONGS[:5]):
                first, last = n == 0, n == 4
                fill_ = "Color/on-primary" if first else "Color/inverse-surface"
                ink_ = "Color/primary" if first else "Color/on-primary"
                r = frame(d, rows, f"Row {n + 1}", layout="h", h=104 if first else 72, gap=10, pad=[0, 16, 0, 12],
                          cross="center", fill=fill_, sizing=("fill", "fixed"),
                          cornerRadius=[28 if first else 6, 28 if first else 6, 28 if last else 6, 28 if last else 6])
                t = txt(d, r, str(n + 1), "Recap/Rank", ink_, "Rank", align="center")
                d.set(t, textAutoResize="height", width=20)
                cover_img(r, ci, 72 if first else 48, 16 if first else 12)
                tc = frame(d, r, "Text column", layout="v", sizing=("fill", "hug"))
                if first:
                    txt(d, tc, title, "Recap/Row title large", ink_, "Title", fill=True)
                    spacer(d, tc, "Gap", w=1, h=2)
                else:
                    txt(d, tc, title, "Recap/Row title", ink_, "Title", fill=True, lines=1)
                a = txt(d, tc, artist, "Recap/Row subtitle", ink_, "Artist", fill=True, lines=1)
                d.set(a, opacity=0.76)
                pc = frame(d, r, "Plays", layout="v", cross="max", sizing=("hug", "hug"))
                txt(d, pc, str(plays), "Recap/Row figure", ink_, "Number")
                w2 = txt(d, pc, "plays", "Recap/Row figure word", ink_, "Word")
                d.set(w2, opacity=0.76)
        elif kind == "Top artist":
            box = frame(d, c, "Artist art", w=300, h=300, x=30, y=36)
            ck = d.vector(box, name="Second cookie", path=shape_path("Cookie 9", 316, math.radians(20)), fill="Color/primary",
                          x=0, y=0)
            d.set(ck, x=-8, y=-8)
            cover_img(box, 0, 300, 0, name="Artist cover", x=0, y=0, artist=True)
            col = frame(d, c, "Text block", layout="v", w=312, x=24, y=36 + 300 + 34, sizing=("fixed", "hug"))
            txt(d, col, "Your top artist", "Recap/Label", ink, "Label", fill=True)
            txt(d, col, "Mira Okafor", "Recap/Artist headline", ink, "Name", fill=True, lines=1)
            spacer(d, col, "Gap", w=1, h=8)
            txt(d, col, "1,545 minutes over 412 plays. Number one of the 9 artists you played.", "Recap/Body", ink,
                "Body", fill=True)
        elif kind == "Top artists":
            col = frame(d, c, "Content", layout="v", w=312, x=24, y=32, sizing=("fixed", "hug"))
            txt(d, col, "The artists\non repeat", "Recap/List headline", "Color/primary", "Headline", fill=True)
            spacer(d, col, "Gap", w=1, h=20)
            top = frame(d, col, "Number one", layout="h", h=132, gap=16, pad=14, cross="center", fill="Color/primary",
                        sizing=("fill", "fixed"), cornerRadius=[28, 28, 6, 6])
            cover_img(top, 0, 104, 0, artist=True)
            tc = frame(d, top, "Text column", layout="v", sizing=("fill", "hug"))
            txt(d, tc, "1", "Recap/Rank", "Color/on-primary", "Rank")
            nb = frame(d, tc, "Name box", layout="h", pad=[2, 0, 0, 0], sizing=("fill", "hug"))
            txt(d, nb, "Mira Okafor", "Recap/Artist name", "Color/on-primary", "Name", fill=True)
            txt(d, tc, "1,545 minutes", "Recap/Row body", "Color/on-primary", "Minutes", fill=True)
            boxes = []
            for row in range(2):
                spacer(d, col, f"Gap {row + 1}", w=1, h=3)
                r = frame(d, col, f"Row {row + 1}", layout="h", gap=3, sizing=("fill", "hug"))
                for k in range(2):
                    n = 1 + row * 2 + k
                    name, ci, plays = TOP_ARTISTS[n]
                    last = row == 1
                    b = frame(d, r, f"Artist {n + 1}", layout="v", pad=14, fill="Color/surface-container-highest",
                              sizing=("fill", "hug"), cornerRadius=[6, 6, 28 if last and k == 1 else 6, 28 if last and k == 0 else 6])
                    inner = b
                    cover_img(inner, ci, 60, 0, artist=True)
                    nb = frame(d, inner, "Name box", layout="v", pad=[10, 0, 0, 0], sizing=("fill", "hug"))
                    txt(d, nb, name, "Recap/Row title", ink, "Name", fill=True, lines=1)
                    txt(d, inner, f"{plays * 225 // 60:,} minutes", "Recap/Row subtitle", "Color/on-surface-variant",
                        "Minutes", fill=True, lines=1)
                    boxes.append((b, n))
                # Ranks sit in each box's top end corner, placed once both boxes of the row have their width.
                for b, n in boxes:
                    rk = txt(d, b, str(n + 1), "Recap/Rank", "Color/primary", "Rank")
                    d.set(rk, absolute=True)
                    d.set(rk, x=d.get(b)["width"] - 14 - 2 - d.get(rk)["width"], y=14,
                          constraints={"horizontal": "right", "vertical": "top"})
                boxes.clear()
        elif kind == "Languages":
            lab = txt(d, c, "What you listened in", "Recap/Label", ink, "Label", x=24, y=32)
            boxy = 32 + 24 + 18
            area = frame(d, c, "Treemap", w=312, h=444, x=24, y=boxy)
            tiles = [(0, 0, 126.69, 444), (129.69, 0, 182.31, 216.76), (129.69, 219.76, 182.31, 154.87),
                     (129.69, 377.63, 182.31, 66.37)]
            fills = ["Color/primary", "Color/recap/tile-2", "Color/recap/tile-3", "Color/recap/tile-4"]
            for n, ((x, y, w, h), (lang, share)) in enumerate(zip(tiles, LANGUAGES)):
                def corner(ax, ay):
                    return 28 if ax and ay else 6
                left, topp = x < 0.5, y < 0.5
                right, bottom = abs(x + w - 312) < 0.5, abs(y + h - 444) < 0.5
                pad = [16, 14, 16, 14] if n < 3 else [10, 10, 10, 10]
                t = frame(d, area, f"Tile {n + 1}", layout="v", w=w, h=h, x=x, y=y, pad=pad, fill=fills[n], sizing=("fixed", "fixed"),
                          cornerRadius=[corner(left, topp), corner(right, topp), corner(right, bottom), corner(left, bottom)])
                ink_ = "Color/on-primary" if n < 2 else "Color/on-surface"
                fit(d, txt(d, t, f"{share}%", f"Recap/Language {n + 1}", ink_, "Share"), w - pad[1] - pad[3])
                fit(d, txt(d, t, lang, f"Recap/Language name {n + 1}", ink_, "Language"), w - pad[1] - pad[3])
        elif kind == "Listening clock":
            txt(d, c, "Your listening clock", "Recap/Label", "Color/primary", "Label", x=24, y=32)
            dl = inst(d, "Recap/Listening dial", c, name="Listening dial", Size="Card", x=18, y=32 + 24)
            col = frame(d, c, "Text block", layout="v", w=312, x=24, y=32 + 24 + 324 + 2, sizing=("fixed", "hug"))
            txt(d, col, "Evening unwinder", "Recap/Clock headline", ink, "Listener type", fill=True)
            spacer(d, col, "Gap", w=1, h=8)
            txt(d, col, "Your evenings have a soundtrack. Your busiest hour starts at 7 pm.", "Recap/Body",
                "Color/on-surface-variant", "Caption", fill=True)
        elif kind == "Highlights":
            col = frame(d, c, "Content", layout="v", w=312, x=24, y=34, sizing=("fixed", "hug"))
            txt(d, col, "The good bits", "Recap/Highlights headline", ink, "Headline", fill=True)
            spacer(d, col, "Gap", w=1, h=22)
            items = [("342 min", "on Saturday 15 August, your biggest day"), ("14×", "Iravu Rail in a single day"),
                     ("46 days", "in a row with music"), ("38", "songs played for the first time")]
            for row in range(2):
                if row:
                    spacer(d, col, "Row gap", w=1, h=3)
                r = frame(d, col, f"Row {row + 1}", layout="h", gap=3, sizing=("fill", "hug"))
                for k in range(2):
                    n = row * 2 + k
                    tonal = n in (1, 2)
                    fill_ = "Color/primary-container" if tonal else "Color/on-tertiary-container"
                    ink_ = "Color/on-primary-container" if tonal else "Color/tertiary-container"
                    rad = [64 if row == 0 and k == 0 else 8, 64 if row == 0 and k == 1 else 8,
                           64 if row == 1 and k == 1 else 8, 64 if row == 1 and k == 0 else 8]
                    b = frame(d, r, f"Highlight {n + 1}", layout="v", h=204, pad=[30, 14, 0, 26 if k == 0 else 20],
                              fill=fill_, sizing=("fill", "fixed"), cornerRadius=rad)
                    fit(d, txt(d, b, items[n][0], "Recap/Highlight value", ink_, "Value"), 154.5 - (26 if k == 0 else 20) - 14)
                    lb = frame(d, b, "Caption box", layout="v", pad=[8, 0, 0, 0], sizing=("fill", "hug"))
                    txt(d, lb, items[n][1], "Recap/Highlight line", ink_, "Caption", fill=True)
        elif kind == "Summary":
            col = frame(d, c, "Content", layout="v", w=312, x=24, y=28, sizing=("fixed", "hug"))
            txt(d, col, "Your 2026 so far", "Recap/Summary headline", "Color/primary", "Headline", fill=True)
            spacer(d, col, "Gap", w=1, h=16)
            blocks = frame(d, col, "Blocks", layout="v", gap=3, sizing=("fill", "hug"))
            for n, (label, name, lead, more, artist) in enumerate([
                    ("Top songs", "Lights Over the Water", 0, [8, 9, 3, 10], False),
                    ("Top artists", "Mira Okafor", 0, [8, 9, 3, 10], True)]):
                b = frame(d, blocks, label, layout="h", h=124, gap=14, pad=16, cross="center",
                          fill="Color/surface-container-highest", sizing=("fill", "fixed"),
                          cornerRadius=[28 if n == 0 else 6, 28 if n == 0 else 6, 6, 6])
                cover_img(b, lead, 92, 16, artist=artist)
                tc = frame(d, b, "Text column", layout="v", sizing=("fill", "hug"))
                txt(d, tc, label, "Recap/Small label", "Color/on-surface-variant", "Label", fill=True)
                nb = frame(d, tc, "Name box", layout="v", pad=[2, 0, 0, 0], sizing=("fill", "hug"))
                nm = txt(d, nb, name, "Recap/Summary name", ink, "Name")
                narrow(d, nm, 312 - 32 - 92 - 14)
                mr = frame(d, tc, "More", layout="h", gap=6, pad=[10, 0, 0, 0], sizing=("hug", "hug"))
                for m in more:
                    cover_img(mr, m, 38, 10, artist=artist)
            r = frame(d, blocks, "Figures", layout="h", gap=3, h=124, sizing=("fill", "fixed"))
            m1 = frame(d, r, "Minutes", layout="v", pad=[0, 18, 0, 18], main="center", fill="Color/primary",
                       sizing=("fill", "fill"), cornerRadius=[6, 6, 6, 28])
            txt(d, m1, "Minutes", "Recap/Small label", "Color/on-primary", "Label")
            fit(d, txt(d, m1, "18,420", "Recap/Summary minutes", "Color/on-primary", "Figure"), 312 - 3 - 112 - 36)
            m2 = frame(d, r, "Mostly in", layout="v", w=112, pad=[0, 18, 0, 18], main="center", fill="Color/primary-container",
                       sizing=("fixed", "fill"), cornerRadius=[6, 6, 28, 6])
            txt(d, m2, "Mostly in", "Recap/Small label", "Color/on-primary-container", "Label")
            lb = frame(d, m2, "Language box", layout="v", pad=[4, 0, 0, 0], sizing=("hug", "hug"))
            fit(d, txt(d, lb, "English", "Recap/Summary language", "Color/on-primary-container", "Language"), 112 - 36)
            wv = frame(d, c, "Wave", w=312, h=24, x=24, y=640 - 72 - 24)
            stroke(d, wv, "Wave stroke", wave_path(6, 306, 12, 48, 7.5, 0.5), "Color/primary-container", 8)
        footer(c, ground, ink)
        return c, {}

    S.make("Recap/Card", "Recap entry",
           "One of Recap's ten cards, 360 x 640 with no corners of its own (the story, thumbnails and deck clip it; "
           "the shared image keeps it square). Its colours are the dark scheme of one of the period's covers in both "
           "themes, so the image someone receives is the one the sender saw: Intro, Top song, Top artist and Summary "
           "take the top song's (Paper Lanterns), Minutes and the clock the fourth song's, Top songs the second's, Top "
           "artists the fifth's, Languages the third's, Highlights the sixth's. Every card has the footer: the mark on a "
           "9dp tile, \"Attic\" and the period in a pill of the ink at 14 %. Content is the demo library's 2026 so far.",
           "ExpressiveRecapCards.kt", "ExpressiveRecapCard", [dict(Kind=t) for t, _, _ in CARDS], card,
           preview_wrap=1640)

    # Story chrome ------------------------------------------------------------------------------------
    def story_bar(vp):
        c = comp(d, "Recap/Story bar", layout="h", w=411, h=64, gap=4, pad=[0, 8, 0, 8], cross="center")
        b = inst(d, "Icon button", c, name="Close", Style="Standard", Size="Default", Width="Default", State="Enabled")
        swap_icon(d, b, "close")
        d.override(b, "Glyph", fill="Color/on-surface")
        col = frame(d, c, "Title column", layout="v", sizing=("fill", "hug"))
        txt(d, col, "Recap", "Label/Medium", "Color/on-surface-variant", "Overline")
        per = txt(d, col, "2026 so far", "Title/Small Emphasized", "Color/on-surface", "Period", fill=True, lines=1)
        cb = frame(d, c, "Position box", layout="h", pad=[0, 16, 0, 8], sizing=("hug", "hug"))
        pos = txt(d, cb, "1 of 10", "Label/Large Emphasized", "Color/on-surface-variant", "Position")
        return c, {"period": per, "pos": pos, "close": b}

    S.make("Recap/Story bar", "Story bar",
           "The story's bar under the status bar: close, \"Recap\" over the period, and \"n of 10\" in tabular figures. "
           "It takes the scheme of the card on show.", "ExpressiveRecapStory.kt", "TopBar", [{}], story_bar, single=True,
           props=[("Period", "text", "period", None, None), ("Position", "text", "pos", None, None)])

    def story_progress(vp):
        k = int(vp["Card"])
        W, H = 363, 44
        c = comp(d, "Recap/Story progress", w=W, h=H)
        track, thumbw, gap, cy = 4, 4, 6, H / 2
        shown = (k - 1 + 0.68) / 10
        tx = (track / 2 + gap + thumbw / 2) + (W - track - 2 * gap - thumbw) * shown
        stroke(d, c, "Played", wave_path(track / 2, tx - thumbw / 2 - gap, cy, 32, 3.5), "Color/primary", track)
        off = tx + thumbw / 2 + gap
        if off < W - track / 2:
            stroke(d, c, "Rest", f"M{off:.2f} {cy} L{W - track / 2} {cy}", "Color/outline-variant", track)
        for s in range(1, 10):
            x = W * s / 10
            if x > off + 2:
                d.ellipse(c, name=f"Stop {s}", w=4, h=4, x=x - 2, y=cy - 2, fill="Color/on-surface-variant")
        d.rect(c, name="Thumb", w=thumbw, h=28, x=tx - thumbw / 2, y=cy - 14, fill="Color/primary", radius=2)
        return c, {}

    S.make("Recap/Story progress", "Story progress",
           "One wavy line for the whole story (StoryProgress): the played part waves (3.5dp, 32dp long) while it runs "
           "and lies flat when held, a 4 x 28 thumb, the rest in outline variant with a 2dp stop at each card still to "
           "come. Card n shows the line two thirds of the way through that card, as the renders catch it. 44dp tall, "
           "24dp in from the sides.", "PlayControls.kt", "StoryProgress",
           [dict(Card=str(i)) for i in range(1, 11)], story_progress)

    def story_actions(vp):
        c = comp(d, "Recap/Story actions", layout="h", w=363, gap=8)
        s = inst(d, "Button", c, name="Save", Style="Tonal", Size="Medium", Icon="Leading", State="Enabled", Squared="False")
        set_text(d, s, prop(ctx, "Button", "Label"), "Save")
        set_props(d, s, **{"Leading icon": d.comp["Icon/download"]})
        sh = inst(d, "Button", c, name="Share", Style="Filled", Size="Medium", Icon="Leading", State="Enabled", Squared="False")
        set_text(d, sh, prop(ctx, "Button", "Label"), "Share")
        set_props(d, sh, **{"Leading icon": d.comp["Icon/share"]})
        d.set(sh, sizingH="fill")
        return c, {"save": s, "share": sh}

    S.make("Recap/Story actions", "Story actions",
           "Save (tonal) and Share (filled, taking the rest of the row), 56dp, 8dp apart: Save writes the card as a "
           "1080 x 1920 image to Pictures, Share hands the same image to the share sheet.",
           "ExpressiveRecapStory.kt", "ExpressiveRecapStory", [{}], story_actions, single=True)

    # Page parts -----------------------------------------------------------------------------------------
    def hero(vp):
        c = comp(d, "Recap/Hero", layout="v", w=379, pad=24, fill="Color/primary-container", sizing=("fixed", "hug"))
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        txt(d, c, "Your 2026 so far", "Recap/Page hero", "Color/on-primary-container", "Headline", fill=True)
        row = frame(d, c, "Body row", layout="h", gap=12, cross="max", sizing=("fill", "hug"))
        col = frame(d, row, "Text column", layout="v", sizing=("fill", "hug"))
        bb = frame(d, col, "Body box", layout="v", pad=[8, 0, 0, 0], sizing=("fill", "hug"))
        txt(d, bb, "18,420 minutes of music, told in 10 cards you can share.", "Body/Medium", "Color/on-primary-container",
            "Body", fill=True)
        pb = frame(d, col, "Button box", layout="h", pad=[20, 0, 0, 0], sizing=("hug", "hug"))
        b = inst(d, "Button", pb, name="Play recap", Style="Filled", Size="Medium", Icon="Leading", State="Enabled",
                 Squared="False")
        set_text(d, b, prop(ctx, "Button", "Label"), "Play recap")
        w, r = 61.2, 28 * 61.2 / 360
        deck_w = (w + 6) * 3 - 34 * 2
        deck = frame(d, row, "Deck", w=deck_w, h=16 + (w * 16 / 9) + 6 + 2)
        order = ["Minutes", "Top song", "Intro"]
        for i in reversed(range(3)):
            ring = frame(d, deck, f"Card {i + 1} ring", w=w + 6, h=w * 16 / 9 + 6, x=(w + 6 - 34) * i,
                         y=16 - 8 * i, fill="Color/primary-container", radius=r + 3)
            scaled_card(d, ring, order[i], w, x=3, y=3, radius=r, name=f"Card {order[i]}")
        return c, {}

    S.make("Recap/Hero", "Recap headline block",
           "The page's one primary container block: the headline, minutes and card count, Play recap, and the first "
           "three cards as a deck (Minutes in front, then the top song, then the intro), each 61.2dp wide in a 3dp "
           "ring of the block's colour, 34dp under the one before and 8dp higher. Tapping anywhere opens the story.",
           "ExpressiveRecap.kt", "Headline", [{}], hero, single=True)

    def stats(vp):
        c = comp(d, "Recap/Stats", layout="v", w=379, gap=8, sizing=("fixed", "hug"))
        r1 = frame(d, c, "Row 1", layout="h", gap=8, sizing=("fill", "hug"))
        m = frame(d, r1, "Minutes", layout="v", h=114, pad=[0, 24, 0, 24], main="center", fill="Color/secondary-container",
                  sizing=("fill", "fixed"))
        bind_dim(d, m, "cornerRadius", "radius/extra-large")
        fit(d, txt(d, m, "18,420", "Recap/Page minutes", "Color/on-surface", "Figure"), (379 - 8) * 2 / 3 - 48)
        txt(d, m, "Minutes", "Body/Medium", "Color/on-secondary-container", "Label")

        def stat(parent, value, label, shape, fill_, ink="Color/on-surface", label_ink="Color/on-surface-variant", w=None):
            b = frame(d, parent, label, h=114, w=w or 100, fill=None if shape == "Cookie" else fill_,
                      sizing=("fill" if w is None else "fixed", "fixed"))
            if shape == "Cookie":
                # the cookie stretched to the stat's box, as CookieShape is a Shape scaled to the size
                s = inst(d, "Shape/Cookie 9", b, name="Cookie", x=0, y=0)
                d.set(s, constraints={"horizontal": "leftRight", "vertical": "topBottom"})
                d.override(s, "Outline", fill=fill_)
            elif shape == "Circle":
                d.set(b, cornerRadius=57)
            elif shape == "Arch":
                d.set(b, cornerRadius=[57, 57, 20, 20])
            elif shape == "Pill":
                d.set(b, cornerRadius=57)
            col = frame(d, b, "Text block", layout="v", cross="center", main="center", x=0, y=0, w=100, h=114,
                        constraints={"horizontal": "leftRight", "vertical": "topBottom"})
            txt(d, col, value, "Recap/Page stat", ink, "Figure")
            txt(d, col, label, "Label/Medium", label_ink, "Label", lines=1)
            return b, col

        pl, plc = stat(r1, "4,870", "Plays", "Pill", "Color/surface-container-high", w=(379 - 8) / 3)
        r2 = frame(d, c, "Row 2", layout="h", gap=8, sizing=("fill", "hug"))
        out = []
        for v, l, s, f_, ink, li in [("69", "Songs", "Circle", "Color/surface-container", "Color/on-surface", "Color/on-surface-variant"),
                                     ("9", "Artists", "Cookie", "Color/primary-container", "Color/on-primary-container", "Color/on-primary-container"),
                                     ("301", "Days with music", "Arch", "Color/surface-container", "Color/on-surface", "Color/on-surface-variant")]:
            out.append(stat(r2, v, l, s, f_, ink, li, w=(379 - 16) / 3))
        for b, col in [(pl, plc)] + out:
            g = d.get(b)
            d.set(col, width=g["width"], height=114)
            for s in d.find(b, name="Cookie", limit=1):
                d.set(s, width=g["width"], height=114)
        st = frame(d, c, "Longest streak", layout="h", h=72, pad=[0, 28, 0, 28], cross="center",
                   fill="Color/surface-container-high", sizing=("fill", "fixed"), radius=36)
        txt(d, st, "46 days", "Recap/Page stat", "Color/on-surface", "Figure", fill=True)
        txt(d, st, "Longest streak", "Body/Medium", "Color/on-surface-variant", "Label")
        return c, {}

    S.make("Recap/Stats", "Recap stats",
           "Six stats, each in its own container: minutes wide on secondary container, plays a pill, songs a disc, "
           "artists the cookie in primary container, days an arch, the longest streak a full-width pill. 114dp tall, "
           "8dp apart.", "ExpressiveRecap.kt", "Stats", [{}], stats, single=True)

    def languages(vp):
        c = comp(d, "Recap/Languages", layout="v", w=379, pad=20, fill="Color/surface-container", sizing=("fixed", "hug"))
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        bar = frame(d, c, "Bar", layout="h", gap=2, h=32, sizing=("fill", "fixed"))
        total = 339 - 2 * 3
        tones = ["Color/primary", "Color/recap/language-2", "Color/recap/language-3", "Color/recap/language-4"]
        for n, (lang, share) in enumerate(LANGUAGES):
            first, last = n == 0, n == 3
            d.rect(bar, name=f"Share {lang}", w=round(total * share / 100, 2), h=32, fill=tones[n],
                   cornerRadius=[16 if first else 4, 16 if last else 4, 16 if last else 4, 16 if first else 4])
        spacer(d, c, "Gap", w=1, h=12)
        for n, (lang, share) in enumerate(LANGUAGES):
            r = frame(d, c, f"Legend {lang}", layout="h", h=36, cross="center", sizing=("fill", "fixed"))
            d.rect(r, name="Swatch", w=14, h=14, fill=tones[n], radius=5)
            spacer(d, r, "Gap", w=12, h=1)
            txt(d, r, lang, "Body/Large", "Color/on-surface", "Language", fill=True)
            txt(d, r, f"{share}%", "Kit/Row rank", "Color/on-surface-variant", "Share")
        return c, {}

    S.make("Recap/Languages", "Recap languages",
           "Languages as one connected bar (32dp, 2dp gaps, ends round) in tones stepping down from primary into the "
           "block's surface container, then the legend in the same tones, 36dp a line.",
           "ExpressiveRecap.kt", "Languages", [{}], languages, single=True)

    def when(vp):
        c = comp(d, "Recap/When you listen", layout="h", w=379, gap=16, pad=[12, 20, 12, 8], cross="center",
                 fill="Color/surface-container", sizing=("fixed", "hug"))
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        inst(d, "Recap/Listening dial", c, name="Listening dial", Size="Page")
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        txt(d, col, "Evening unwinder", "Recap/Page clock title", "Color/on-surface", "Listener type", fill=True)
        lb = frame(d, col, "Caption box", layout="v", pad=[6, 0, 0, 0], sizing=("fill", "hug"))
        txt(d, lb, "Your evenings have a soundtrack. Your busiest hour starts at 7 pm.", "Body/Medium",
            "Color/on-surface-variant", "Caption", fill=True)
        return c, {}

    S.make("Recap/When you listen", "When you listen",
           "The listening clock at 172dp beside the listener type and its line, on surface container.",
           "ExpressiveRecap.kt", "WhenYouListen", [{}], when, single=True)


# ───────────────────────────────────────────────────────────────── Components / System
def build_system(d: Doc, ctx: dict):
    page = DocPage(d, "Components / System", "System",
                   "What Attic shows outside its own window: the home-screen widget in its three layouts, the media "
                   "notification Android draws from the session, the splash and the launcher icon. Android draws the "
                   "notification's chrome; the parts marked System are the phone's, not Attic's.")
    S = Spec(d, ctx, page)

    def play_btn(parent, playing=True):
        b = frame(d, parent, "Play", w=52, h=52)
        r = d.rect(b, name="Container", w=36, h=36, x=8, y=8, fill="Color/primary", radius=11 if playing else 18)
        ic = d.icon(b, "pause" if playing else "play_arrow", size=22, color="Color/on-primary", filled=True, name="Glyph")
        d.set(ic, x=15, y=15)
        return b

    def skip_btn(parent, lig, narrow=False):
        w = 40 if narrow else 48
        b = frame(d, parent, "Previous" if lig == "skip_previous" else "Next", w=w, h=48)
        d.rect(b, name="Container", w=w - 8, h=40, x=4, y=4, fill="Color/secondary-container", radius=20)
        ic = d.icon(b, lig, size=20, color="Color/on-secondary-container", filled=True, name="Glyph")
        d.set(ic, x=(w - 20) / 2, y=14)
        return b

    def art(parent, size, radius, playing, name="Art"):
        if playing:
            a = d.image(parent, cover_path(0), name=name, w=size, h=size, radius=radius)
        else:
            a = frame(d, parent, name, layout="h", w=size, h=size, cross="center", main="center",
                      fill="Color/secondary-container", radius=radius)
            mark(d, a, round(size * 0.42, 1), "Color/on-secondary-container")
        return a

    def widget(vp):
        size, playing = vp["Size"], vp["State"] == "Playing"
        title = "Lights Over the Water" if playing else "Attic"
        artist = "Mira Okafor" if playing else "Tap to open"
        if size == "Small":
            c = comp(d, "Widget", layout="v", w=170, h=170, gap=6, pad=10, fill="Color/surface-container-high", clip=True)
            bind_dim(d, c, "cornerRadius", "radius/extra-large")
            a = art(c, 150, 18, playing)
            d.set(a, sizingH="fill", sizingV="fill")
            r = frame(d, c, "Song row", layout="h", h=52, gap=6, pad=[0, 0, 0, 6], cross="center", sizing=("fill", "fixed"))
            txt(d, r, title, "System/Widget title small", "Color/on-surface", "Title", fill=True, lines=2)
            play_btn(r, playing)
        elif size == "Wide":
            c = comp(d, "Widget", layout="h", w=352, h=96, gap=0, pad=[0, 8, 0, 12], cross="center",
                     fill="Color/surface-container-high", clip=True)
            bind_dim(d, c, "cornerRadius", "radius/extra-large")
            art(c, 64, 16, playing)
            col = frame(d, c, "Text column", layout="v", gap=2, pad=[0, 0, 0, 12], sizing=("fill", "hug"))
            txt(d, col, title, "System/Widget title", "Color/on-surface", "Title", fill=True, lines=1)
            txt(d, col, artist, "System/Widget artist", "Color/on-surface-variant", "Artist", fill=True, lines=1)
            spacer(d, c, "Gap", w=4, h=4)
            skip_btn(c, "skip_previous", True)
            play_btn(c, playing)
            skip_btn(c, "skip_next", True)
        else:
            c = comp(d, "Widget", w=352, h=200, fill="Color/surface-container-high", clip=True)
            bind_dim(d, c, "cornerRadius", "radius/extra-large")
            top = frame(d, c, "Song", layout="h", w=320, h=96, x=16, y=16, gap=16)
            art(top, 96, 20, playing)
            col = frame(d, top, "Text column", layout="v", pad=[0, 28, 0, 0], sizing=("fill", "fill"))
            txt(d, col, title, "System/Widget title large", "Color/on-surface", "Title", fill=True, lines=1)
            tb = frame(d, col, "Artist box", layout="v", pad=[2, 0, 0, 0], sizing=("fill", "hug"))
            txt(d, tb, artist, "System/Widget artist large", "Color/on-surface-variant", "Artist", fill=True, lines=1)
            spacer(d, col, "Space", w=1, h=1)
            d.set(d.find(col, name="Space")[0], sizingV="fill")
            ctl = frame(d, col, "Controls", layout="h", gap=2, cross="center", sizing=("hug", "hug"))
            skip_btn(ctl, "skip_previous")
            play_btn(ctl, playing)
            skip_btn(ctl, "skip_next")
            d.set(ctl, x=-4)
            tiles = frame(d, c, "Albums", layout="h", gap=8, w=320, h=58, x=16, y=16 + 96 + 14)
            for n, ci in enumerate((0, 8, 9, 3, 10)):
                if playing:
                    t = d.image(tiles, cover_path(ci), name=f"Album {n + 1}", w=57.6, h=58, radius=12)
                    d.set(t, sizingH="fill")
                else:
                    t = frame(d, tiles, f"Album {n + 1}", layout="h", h=58, cross="center", main="center",
                              fill="Color/secondary-container", radius=12, sizing=("fill", "fixed"))
                    mark(d, t, 24, "Color/on-secondary-container")
            mark(d, c, 18, "Color/on-surface-variant", x=round(352 - 16 - 2 - 1 - 18 * MARK_W / MARK_H, 2), y=16 + 3)
        if playing:
            d.set_mode(c, "Color", "Cover")
        return c, {}

    S.make("Widget", "Widget",
           "The home-screen widget as PlayerWidget builds it (RemoteViews, so the system font). One container with the "
           "system's widget corners in the cover scheme's surface container high, or the wallpaper scheme before anything "
           "has played. Small (about 2 x 2): the cover inset 10dp with 18dp corners, the title beside the play control. "
           "Wide (4 x 1): the cover, title and artist, then previous, play and next with narrow skip buttons. Large "
           "(4 x 2 and up): the cover, song and controls, five album covers under them, and the mark in the corner. The "
           "play control is 36dp in a 52dp box, round while paused and squared off (11dp) while playing.",
           "java/app/attic/player/widget/PlayerWidget.kt", "PlayerWidget",
           [dict(Size=s, State=st) for st in ("Playing", "Empty") for s in ("Small", "Wide", "Large")], widget)

    def notification(vp):
        c = comp(d, "System/Media notification", layout="v", w=379, gap=10, pad=[16, 16, 14, 16], clip=True,
                 fill="Color/primary-container", sizing=("fixed", "hug"))
        d.set_mode(c, "Color", "Cover")
        d.set(c, cornerRadius=24)
        head = frame(d, c, "Header", layout="h", gap=12, cross="center", sizing=("fill", "hug"))
        d.image(head, cover_path(0), name="Art", w=56, h=56, radius=12)
        col = frame(d, head, "Text column", layout="v", sizing=("fill", "hug"))
        txt(d, col, "Lights Over the Water", "System/Title", "Color/on-primary-container", "Title", fill=True, lines=1)
        txt(d, col, "Mira Okafor", "System/Body", "Color/on-primary-container", "Artist", fill=True, lines=1)
        app = frame(d, head, "App icon", layout="h", w=28, h=28, cross="center", main="center", fill="Color/primary", radius=14)
        mark(d, app, 16, "Color/on-primary")
        ctl = frame(d, c, "Controls", layout="h", cross="center", sizing=("fill", "hug"))
        for n, (lig, fil, name) in enumerate((("favorite", True, "Favourite"), ("skip_previous", True, "Previous"),
                                             ("pause", True, "Pause"), ("skip_next", True, "Next"), ("shuffle", False, "Shuffle"))):
            if name == "Pause":
                b = frame(d, ctl, name, layout="h", w=56, h=48, cross="center", main="center", fill="Color/primary", radius=16)
                d.icon(b, lig, size=24, color="Color/on-primary", filled=True, name="Glyph")
            else:
                b = frame(d, ctl, name, layout="h", w=48, h=48, cross="center", main="center")
                d.icon(b, lig, size=24, color="Color/on-primary-container", filled=fil, name="Glyph")
            d.set(b, sizingH="fill")
        seek = frame(d, c, "Seek", layout="h", gap=8, cross="center", sizing=("fill", "hug"))
        txt(d, seek, "1:18", "System/Small", "Color/on-primary-container", "Elapsed")
        line = frame(d, seek, "Progress line", h=12, w=200, sizing=("fill", "fixed"))
        stroke(d, line, "Played", wave_path(2, 88, 6, 20, 2.5), "Color/on-primary-container", 3)
        d.rect(line, name="Rest", w=150, h=3, x=94, y=4.5, fill="Color/on-primary-container", radius=2)
        d.set(d.find(line, name="Rest")[0], opacity=0.3, constraints={"horizontal": "leftRight", "vertical": "top"})
        txt(d, seek, "3:52", "System/Small", "Color/on-primary-container", "Total")
        return c, {}

    S.make("System/Media notification", "Media notification",
           "Android's media control for Attic's session (Media3 MediaStyle): the system draws this card from the "
           "session's metadata and artwork, tints it from the art, and lays out the buttons. Attic supplies the title, "
           "artist and cover, previous, play or pause and next, and two custom buttons in the overflow slots: the "
           "favourite heart and shuffle (ic_expressive_favourite_*, ic_expressive_shuffle_*), plus the mark as the small "
           "icon. This drawing tints with the cover scheme as a stand-in for the system's own.",
           "java/app/attic/player/playback/PlaybackService.kt", "PlaybackService", [{}], notification, single=True)

    def splash(vp):
        c = comp(d, "Splash", w=411, h=914, fill="Color/surface")
        circ = d.ellipse(c, name="Icon background", w=160, h=160, x=125.5, y=377, fill="Brand/headroom/ground")
        k = 240 / 108
        ox, oy = 205.5 - 120, 457 - 120
        cab = d.vector(c, name="Cabinet", path="M35.2 78.5 L35.2 47.78 L71.2 27 L71.2 78.5 Z",
                       fill="Brand/headroom/cabinet", x=0, y=0, w=36 * k, h=51.5 * k)
        d.set(cab, x=ox + 35.2 * k, y=oy + 27 * k)
        cone = d.vector(c, name="Cone", path=CONE_PATH, fill="Brand/headroom/cone", x=0, y=0, w=24 * k, h=34.3 * k)
        d.set(cone, x=ox + 41.2 * k, y=oy + 38.2 * k)
        d.cmd("vector.fillRule", ids=[cone], rule="evenOdd")
        return c, {}

    S.make("Splash", "Splash",
           "Android's splash for Attic (Theme.Attic.Splash.Expressive): the window ground, then the launcher icon's "
           "foreground on its terracotta circle, 160dp across with the 240dp icon canvas centred. The ground is the "
           "wallpaper scheme's neutral at tone 99 (light) and 10 (dark), system_neutral1_10 and _900, which this file "
           "shows with surface.", "res/values/themes.xml", "Theme.Attic.Splash.Expressive", [{}], splash,
           single=True, preview=False)

    def launcher(vp):
        mono = vp["Layer"] == "Monochrome"
        c = comp(d, "Launcher icon", w=108, h=108)
        d.ellipse(c, name="Background", w=108, h=108, x=0, y=0,
                  fill="Color/primary-container" if mono else "Brand/headroom/ground")
        if mono:
            v = d.vector(c, name="Mark", path=MONO_PATH, fill="Color/on-primary-container", x=0, y=0)
            d.cmd("vector.fillRule", ids=[v], rule="evenOdd")
        else:
            d.vector(c, name="Cabinet", path="M35.2 78.5 L35.2 47.78 L71.2 27 L71.2 78.5 Z", fill="Brand/headroom/cabinet", x=0, y=0)
            v = d.vector(c, name="Cone", path=CONE_PATH, fill="Brand/headroom/cone", x=0, y=0)
            d.cmd("vector.fillRule", ids=[v], rule="evenOdd")
        return c, {}

    S.make("Launcher icon", "Launcher icon",
           "The adaptive icon (mipmap-anydpi-v26/ic_launcher.xml) on its 108dp canvas, shown under a circle mask; "
           "each launcher applies its own mask. Colour: the cream cabinet and dark cone on "
           "terracotta (Brand collection). Monochrome: the one-colour mark (ic_launcher_monochrome) that a themed "
           "launcher tints from the wallpaper, shown here in the wallpaper scheme's primary container pair.",
           "res/mipmap-anydpi-v26/ic_launcher.xml", "ic_launcher",
           [dict(Layer="Colour"), dict(Layer="Monochrome")], launcher)


CONE_PATH = ("M41.2 60.5 a12 12 0 1 0 24 0 a12 12 0 1 0 -24 0 Z M48.4 60.5 a4.8 4.8 0 1 0 9.6 0 a4.8 4.8 0 1 0 -9.6 0 Z "
             "M57.2 42 a3.8 3.8 0 1 0 7.6 0 a3.8 3.8 0 1 0 -7.6 0 Z")
MONO_PATH = ("M35.2 78.5 L35.2 47.78 L71.2 27 L71.2 78.5 Z M41.2 60.5 a12 12 0 1 0 24 0 a12 12 0 1 0 -24 0 Z "
             "M48.4 60.5 a4.8 4.8 0 1 0 9.6 0 a4.8 4.8 0 1 0 -9.6 0 Z M57.2 42 a3.8 3.8 0 1 0 7.6 0 a3.8 3.8 0 1 0 -7.6 0 Z")


def build(d: Doc, ctx: dict):
    build_states(d, ctx)
    build_setting_rows(d, ctx)
    build_recap(d, ctx)
    build_system(d, ctx)
    # Every core component exists now: wrapped text in them takes the code's line height before screens use them.
    paragraph_line_heights(d, pages=[n for n in d.pages if n.startswith("Components / ")])
