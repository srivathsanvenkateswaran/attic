"""Empty, loading, permission and first-run parts; Recap cards; widget, notification, splash, launcher icon.
Pages: Components / States, Components / Recap, Components / System.

Owner: settings screens agent. Built from ui/comfort/ (each description names file and composable), colours bound
to Color/* (plus the Expressive preview collection below), text on text styles, icons as Icon/* instances.

Variables and text styles these components use are all made in foundations.py: Color/derived/language-2 and -3,
the `Brand` collection (launcher icon), the `Expressive preview` collection (the Expressive look card), and the
Recap/*, Widget/*, System/* and Preview/* text styles.

COMPONENTS / STATES
  Mark box              Style=Tonal|Solid|Neutral. 56dp; resize the instance. Artwork.kt MarkBox
  Empty state           (single) props Title, Body, Show body; nested Icon box exposed (Icon). Controls.kt EmptyState
  Prompt                Kind=Permission|Welcome. props Title, Body. ComfortStates.kt ComfortPermission, ComfortOnboarding WelcomeStep
  Onboarding top bar    Step=1|2|3|4 (1 has no back). ComfortOnboarding.kt (the 64dp row)
  Onboarding heading    (single) props Title, Body, Show body. ComfortOnboarding.kt Title, Body
  Look preview          Style=Comfort|Expressive. The mini Home each style draws, at 0.42. ComfortLookPreview, ExpressiveLookPreview
  Look card             Style=Expressive|Comfort, Selected=False|True. ComfortOnboarding.kt LookCard
  Access row            Status=Not asked|Allow|Allowed|Off|Off required. props Title, Summary, Show hairline;
                        nested Icon box exposed. ComfortOnboarding.kt AccessRow
  Library scan          (single) the 44dp spinner over "Finding your songs…". ComfortOnboarding.kt LibraryStep

COMPONENTS / RECAP
  Recap card footer     (single) props Period. ComfortRecapCards.kt Footer
  Rank row              Kind=Song|Artist. props Rank, Title, Subtitle, Value (Song). Layer "Bed": its width is
                        88 + share x 260; its fill is the row cover's Tinted(seed).miniFill. ComfortRecapCards.kt RankRow
  Highlight row         (single) props Value, Line, Show hairline; nested Icon box exposed. HighlightsCard
  Language track        (single) props Language, Share; layer "Fill" width = share x 276. LanguagesCard
  Listening clock       Labels=True|False. The demo library's 24 hours (DemoStates.kt). ListeningClock
  Recap card            Card=Intro|Minutes|Top song|Top songs|Top artist|Top artists|Languages|Listening clock|
                        Highlights|Summary. 360 x 640, always the dark tokens; Top song, Top artist and Summary set
                        Color mode Tinted wash. Scale instances (transform.scale) as the code scales the drawing.
                        ComfortRecapCards.kt ComfortRecapCard
  Story header          (single) props Headline. ComfortRecapStory.kt StoryHead
  Story deck            (single) props Position; layer "Fill" of the nested Progress for how far. StoryDeck
  Play recap            (single) props Body. ComfortRecap.kt PlayRecap
  Stat cell             (single) props Value, Label. Controls.kt Stats (one cell)
  Language row          (single) props Language, Share; layer "Swatch" takes the step colour. ComfortRecap.kt Languages
  Clock summary         (single) props Title, Line. ComfortRecap.kt (When you listen)

COMPONENTS / SYSTEM
  Widget                Size=Small|Wide|Large, State=Playing|Empty (Empty: Large only). Playing sets Color mode Tinted
                        wash (the widget washes its card with the cover). PlayerWidget.kt, res/layout/widget_comfort_*.xml
  Media notification    (single) Android's MediaStyle card with Attic's content. PlaybackService.kt
  Splash                (single) the window background with the launcher icon. res/values/themes.xml
  Launcher icon         Layer=Colour|Monochrome. res/mipmap-anydpi-v26/ic_launcher.xml

HELPERS other modules import: setp(), group(), block(), scaled(), COVERS, SONG_COVER, ARTIST_COVER, recap_data.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from figbuild.kit import Doc, V

from .components_core import (Components, SetBuilder, K, COVER, cover_image, _cover, glyph_drop, _arc_cubic, _circle, MARK_PATH,
                              drawable_icon)
from .foundations import Sheet, superellipse_path, COVERS_DIR, COLORS_DIR, _modes, text_style, oklch_from_hex, oklch_to_hex, load_palette

REFS: dict[str, str] = {}

SRC = "ui/comfort/"

# Demo covers by song and artist (covers/README.txt, DemoLibrary.kt).
SONG_COVER = {"Lights Over the Water": 0, "Iravu Rail": 8, "Chai Ka Doosra Cup": 9, "Neon Peaches": 3,
              "Night Drive Firmware": 10, "Kadalkarai": 7, "Marea Lenta": 2, "Kettle On": 1, "Platform Ettu": 8, "Patang": 9}
ARTIST_COVER = {"Mira Okafor": 0, "Kavya Raman": 8, "Ruhaan Sethi": 9, "Juniper Vale": 3, "Velvet Circuit": 10,
                "Small Hours Tape Club": 1, "Paloma Ferrer": 2, "Tide & Tamarind": 7}

# DemoStates.kt `wrapped`: the demo Recap (year 2026 so far).
TOP_SONGS = [("Lights Over the Water", "Mira Okafor", 96), ("Iravu Rail", "Kavya Raman", 81),
             ("Chai Ka Doosra Cup", "Ruhaan Sethi", 74), ("Neon Peaches", "Juniper Vale", 69),
             ("Night Drive Firmware", "Velvet Circuit", 63), ("Kadalkarai", "Tide & Tamarind", 57),
             ("Marea Lenta", "Paloma Ferrer", 52), ("Kettle On", "Small Hours Tape Club", 49),
             ("Platform Ettu", "Kavya Raman", 44), ("Patang", "Ruhaan Sethi", 41)]
TOP_ARTISTS = [("Mira Okafor", 412), ("Kavya Raman", 356), ("Ruhaan Sethi", 298), ("Juniper Vale", 201), ("Velvet Circuit", 177)]
LANGUAGES = [("English", 0.41), ("Tamil", 0.29), ("Hindi", 0.21), ("No vocals", 0.09)]
INTRO_COVERS = [0, 8, 9, 3, 10, 7, 2, 1, 8]


def artist_minutes(plays: int) -> int:
    return plays * 225_000 // 60_000


def demo_hours() -> list[float]:
    """DemoStates.kt `hours`, in minutes."""
    out = []
    for h in range(24):
        evening = 3.2 if 19 <= h <= 23 else 1.0
        night = 2.0 if h in (0, 1) else 1.0
        asleep = 0.05 if 2 <= h <= 6 else 1.0
        out.append((40 + 30 * math.sin(h / 24.0 * math.pi)) * evening * night * asleep * 9)
    return out


# ======================================================================== helpers other modules use
def props_of(d: Doc, inst: int) -> dict:
    return {p["name"].split("#")[0]: p for p in d.cmd("component.properties", id=inst).get("properties", [])}


def setp(d: Doc, inst: int, **values):
    """Set component properties by display name: variants first (a switch keeps the other values), then text,
    booleans and swaps. A swap takes a component name (`"Icon/check"`)."""
    have = props_of(d, inst)
    variants = {k: str(v) for k, v in values.items() if have.get(k, {}).get("type") == "variant"}
    if variants:
        d.cmd("instance.setProperties", ids=[inst], properties=variants)
        have = props_of(d, inst)
    rest = {}
    for k, v in values.items():
        if k in variants:
            continue
        if k not in have:
            raise KeyError(f"{k!r} isn't a property of {d.get(inst).get('name')}: {sorted(have)}")
        p = have[k]
        if p["type"] == "instanceSwap" and isinstance(v, str):
            v = d.comp[v]
        rest[p["name"]] = v
    if rest:
        d.cmd("instance.setProperties", ids=[inst], properties=rest)
    return inst


def inst(d: Doc, comp: str, parent, name: str, fill_w: bool = False, **values) -> int:
    i = d.instance(comp, parent, name=name)
    if values:
        setp(d, i, **values)
    if fill_w:
        d.set(i, sizingH="fill")
    return i


def group(d: Doc, parent, name: str, pad: bool = False) -> tuple[int, int]:
    """A Group card instance and its Content slot, emptied. Add rows with `row()`."""
    g = d.instance("Group card", parent, name=name)
    if pad:
        setp(d, g, Pad="True")
    slot = d.child(g, "Content")
    return g, slot


def row(d: Doc, g: int, comp: str, name: str, **values) -> int:
    """Insert an instance of `comp` into a Group card instance's Content slot (the first call clears it)."""
    slot_prop = next(p["name"] for p in d.cmd("component.properties", id=g)["properties"] if p["type"] == "slot")
    first = not getattr(d, "_slot_used", {}).get(g)
    d.__dict__.setdefault("_slot_used", {})[g] = True
    r = d.cmd("instance.slotInsert", id=g, slot=slot_prop, component=d.comp[comp], replace=first)["id"]
    d.set(r, name=name, sizingH="fill")
    if values:
        setp(d, r, **values)
    return r


def block(d: Doc, g: int, name: str, *, text=None, bold=None, small_above=None, small=None, progress=None, button=None,
          after_row=False) -> int:
    """A Settings block in a group, its parts switched on as ComfortSettings.kt calls them (the component carries the
    spacing: SetBlock, BlockSmall, SetBlockProgressPadding, BlockButton). `bold` is the run BlockText sets semibold."""
    b = row(d, g, "Settings block", name)
    vals = {"After row": str(after_row), "Show text": text is not None, "Show small print above": small_above is not None,
            "Show progress": progress is not None, "Show small print": small is not None, "Show button": button is not None}
    if text is not None:
        vals["Text"] = text
    if small_above is not None:
        vals["Small print above"] = small_above
    if small is not None:
        vals["Small print"] = small
    if button is not None:
        vals["Show button"] = True
    setp(d, b, **vals)
    if text is not None and bold:
        para = d.child(b, "Paragraph")
        s = text.index(bold)
        d.cmd("text.style", id=para, start=s, end=s + len(bold), fontStyle="SemiBold")
        # BlockText also inks the run in on; a text range can take a colour but not a variable binding, so the run keeps
        # the paragraph's on-2 and switches with the mode.
    if small is not None and text is None and progress is None and small_above is None:
        d.set(d.child(b, "Small print"), paddingTop=0)   # BlockSmall(text, Modifier): no top padding when it stands alone
    if progress is not None:
        bar = d.child(b, "Progress bar")
        d.override(bar, "Fill", width=round(339 * progress, 2))
    if button is not None:
        setp(d, d.child(b, "Text button"), Label=button)
    return b


def scaled(d: Doc, comp: str, parent, name: str, factor: float, x=0, y=0, **values) -> int:
    """An instance drawn smaller or larger, as the code scales a fixed drawing (graphicsLayer scale)."""
    i = d.instance(comp, parent, name=name, x=x, y=y)
    if values:
        setp(d, i, **values)
    d.cmd("transform.scale", ids=[i], factor=factor)
    d.set(i, x=x, y=y)
    return i


def tint_hex(cover: int, theme: str, key: str) -> str:
    """A token of Tinted(cover's seed) from the design colours (colors/comfort-tinted-N.json)."""
    j = json.loads((COLORS_DIR / f"comfort-tinted-{cover}.json").read_text())
    t = j[theme]
    for section in ("tinted", "tintedWash"):
        if section in t and key in t[section].get("changed", {}):
            return t[section]["changed"][key]["hex"][:7].upper()
    raise KeyError(f"{key} in comfort-tinted-{cover}.json {theme}")


# ======================================================================== build
def build(d: Doc, ctx: dict):
    k = K(d, ctx["tokens"])
    _states(d, k)
    _recap(d, k)
    _system(d, k)


def _dark_stages(d: Doc, page: str, names):
    """Components that only live on the dark cards and story: show them on a dark stage."""
    pid = d.pages[page]
    for n in names:
        for spec in d.find(pid, name=f"Spec: {n}", limit=5):
            if d.get(spec)["name"] == f"Spec: {n}":
                _modes(d, d.child(spec, "Stage"), "Dark")


def _hide(d, node):
    d.set(node, visible=False)


# ------------------------------------------------------------------------ States
def _states(d: Doc, k: K):
    cc = Components(d, "Components / States",
                    "What a screen shows before there is music to show: the permission prompt, the first scan, an empty "
                    "library, and the first run's four steps. Every part sits on the ground and is centred, or uses the "
                    "settings card and row of ui/comfort/Controls.kt. Source: ui/comfort/ComfortStates.kt, ComfortOnboarding.kt, "
                    "Controls.kt EmptyState, Artwork.kt MarkBox.")

    # -- Mark box
    sb = SetBuilder(cc, "Mark box", "Artwork.kt · MarkBox", "MarkBox",
                    "The Headroom mark at half the box's height in an icon box's superellipse (n = 4). Tonal (accent-soft, "
                    "accent-ink): permission, welcome, Settings' About. Solid (accent, on-accent): a Recap card's footer. "
                    "Neutral (fill, on-2): the widget's empty art. Resize the instance; the mark scales with it.")
    for style, bg, ink in (("Tonal", "Color/accent-soft", "Color/accent-ink"), ("Solid", "Color/accent", "Color/on-accent"),
                           ("Neutral", "Color/fill", "Color/on-2")):
        c = sb.variant({"Style": style}, w=56, h=56)
        k.sq(c, 4, 56, 56, bg, "Container")
        mh = 28
        mw = round(mh * 36 / 51.5, 3)
        m = d.instance("Mark", c, name="Mark", x=(56 - mw) / 2, y=(56 - mh) / 2)
        d.set(m, width=mw, height=mh, constraints={"horizontal": "scale", "vertical": "scale"})
        d.override(m, "Headroom", fill=ink)
    sb.finish()

    # -- Empty state
    c = cc.single("Empty state", "Controls.kt · EmptyState", "EmptyState", layout="v", w=411, pad=[48, 40, 48, 40],
                  cross="center", sizing=("fixed", "hug"))
    ib = d.instance("Icon box", c, name="Icon box")
    setp(d, ib, Style="Neutral", Icon="Icon/folder_open")
    d.set(ib, width=76, height=76)
    d.cmd("component.exposeInstance", id=ib)
    tf = d.frame(c, name="Title", layout="v", pad=[22, 0, 0, 0], cross="center", sizing=("fill", "hug"))
    t = k.text(tf, "No songs on this phone", "Scale/Bar title", "Color/on", "Title text", fill=True, align="center")
    bf = d.frame(c, name="Body", layout="v", pad=[8, 0, 0, 0], cross="center", sizing=("fill", "hug"))
    b = k.text(bf, "Copy music files onto this phone, in Music or any other folder, and they will show up here.",
               "Scale/Body", "Color/on-2", "Body text", fill=True, align="center")
    cc.finish_single("Empty state", c, "Controls.kt · EmptyState", "EmptyState",
                     "`.empty`: a 76dp neutral icon box, the title 22 below (20/26 bold) and one sentence 8 below (15/21 on-2), "
                     "centred, 40dp sides and 48 above and below. Home uses folder_open, the first run music_note.",
                     binds=[("Title", "text", t, "No songs on this phone"),
                            ("Body", "text", b, "Copy music files onto this phone, in Music or any other folder, and they will show up here."),
                            ("Show body", "boolean", bf, True)])

    # -- Prompt (permission, welcome)
    sb = SetBuilder(cc, "Prompt", "ComfortStates.kt · ComfortPermission", "ComfortPermission",
                    "The centred block of the permission screen and the first run's welcome: the mark box (96, or 112 on "
                    "Welcome), the title 30 below (32 on Welcome, 30/36 bold) and the sentence 14 below (16/23 on-2), 36dp sides.")
    for kind, box, gap, title, body in (
            ("Permission", 96, 30, "Attic plays the music on this phone",
             "Allow audio access so it can find your songs in any folder. Notifications show the playback controls."),
            ("Welcome", 112, 32, "Welcome to Attic",
             "Attic plays the music files on this phone. There's no account, and everything works offline.")):
        c = sb.variant({"Kind": kind}, layout="v", w=411, pad=[0, 36, 0, 36], cross="center", sizing=("fixed", "hug"))
        mb = d.instance("Mark box", c, name="Mark box")
        d.set(mb, width=box, height=box)
        tf = d.frame(c, name="Title", layout="v", pad=[gap, 0, 0, 0], sizing=("fill", "hug"))
        t = k.text(tf, title, "Component/Onboarding title", "Color/on", "Title text", fill=True, align="center")
        bf = d.frame(c, name="Body", layout="v", pad=[14, 0, 0, 0], sizing=("fill", "hug"))
        bt = k.text(bf, body, "Component/Onboarding body", "Color/on-2", "Body text", fill=True, align="center")
        sb.text("Title", t, title)
        sb.text("Body", bt, body)
    sb.finish()

    # -- Onboarding top bar
    sb = SetBuilder(cc, "Onboarding top bar", "ComfortOnboarding.kt · ComfortOnboarding", "ComfortOnboarding",
                    "The first run's top row, 64dp under the status bar, 8dp sides: back (none on the first step: a 48dp "
                    "space), then the step pips at the end, 16dp in.")
    for step in (1, 2, 3, 4):
        c = sb.variant({"Step": str(step)}, layout="h", w=411, h=64, pad=[0, 8, 0, 8], cross="center", sizing=("fixed", "fixed"))
        if step > 1:
            bk = d.instance("Icon button", c, name="Back")
            setp(d, bk, Icon="Icon/arrow_back")
        else:
            d.frame(c, name="Back space", w=48, h=48)
        d.frame(c, name="Spacer", h=1, sizing=("fill", "fixed"))
        pf = d.frame(c, name="Pips", layout="h", pad=[0, 16, 0, 0], sizing=("hug", "hug"))
        p = d.instance("Step pips", pf, name="Step pips")
        setp(d, p, Step=str(step))
    sb.finish()

    # -- Onboarding heading
    c = cc.single("Onboarding heading", "ComfortOnboarding.kt · Title, Body", "Title", layout="v", w=411, sizing=("fixed", "hug"))
    tf = d.frame(c, name="Title", layout="v", pad=[16, 36, 0, 36], sizing=("fill", "hug"))
    t = k.text(tf, "Pick your look", "Component/Onboarding title", "Color/on", "Title text", fill=True, align="center")
    bf = d.frame(c, name="Body", layout="v", pad=[14, 36, 0, 36], sizing=("fill", "hug"))
    b = k.text(bf, "You can change it any time in Settings.", "Component/Onboarding body", "Color/on-2", "Body text", fill=True,
               align="center")
    cc.finish_single("Onboarding heading", c, "ComfortOnboarding.kt · Title, Body", "Title",
                     "A step's title (30/36 bold, centred, 36dp sides, 16 above) and its sentence (16/23 on-2, 14 above).",
                     binds=[("Title", "text", t, "Pick your look"), ("Body", "text", b, "You can change it any time in Settings."),
                            ("Show body", "boolean", bf, True)])

    # -- Look preview
    _look_preview(d, k, cc)

    # -- Look card
    sb = SetBuilder(cc, "Look card", "ComfortOnboarding.kt · LookCard", "LookCard",
                    "A look as a selectable card (half the width less 6, here 183.5): 8dp inside, radius 26, the card colour; "
                    "the live preview clipped to radius 18; then the radio and the name (16/22 medium), 52dp min, 8 in and 4 "
                    "above. Selected: a 2dp accent ring.")
    for style in ("Expressive", "Comfort"):
        for sel in (False, True):
            c = sb.variant({"Style": style, "Selected": str(sel)}, layout="v", w=183.5, pad=8, fill="Color/card",
                           sizing=("fixed", "hug"), clip=True)
            k.bind_radius(c, "group")
            if sel:
                d.set(c, stroke=V("Color/accent"), strokeWeight=2, strokeAlign="inside")
            pv = d.instance("Look preview", c, name="Preview")
            setp(d, pv, Style=style)
            r = d.frame(c, name="Choice", layout="h", gap=12, pad=[4, 0, 0, 8], cross="center", sizing=("fill", "hug"))
            d.set(r, minHeight=56)
            rd = d.instance("Radio", r, name="Radio")
            setp(d, rd, Selected=str(sel))
            k.text(r, style, "Scale/Row title", "Color/on", "Name", lines=1)
    sb.finish()

    # -- Access row
    sb = SetBuilder(cc, "Access row", "ComfortOnboarding.kt · AccessRow", "AccessRow",
                    "One permission as a settings row: a 40dp tonal icon box, the title and why (SetRow's 16.5/22 and 13.5/19), "
                    "then where it stands. Not asked: nothing (audio, which the screen's button asks for). Allow: a small "
                    "tonal button. Allowed: check and \"Allowed\" in accent-ink (14/18 semibold). Off: on-2; Off required: danger.")
    for status in ("Not asked", "Allow", "Allowed", "Off", "Off required"):
        c = sb.variant({"Status": status}, layout="h", w=379, pad=[11, 20, 11, 20], gap=16, cross="center", sizing=("fixed", "hug"))
        d.set(c, minHeight=60)
        hl = d.rect(c, name="Hairline", w=279, h=1, fill="Color/line")
        k.absolute(hl, 80, 0, 279, 1, {"horizontal": "leftRight", "vertical": "top"})
        _hide(d, hl)
        sb.bool("Show hairline", hl, False)
        ib = d.instance("Icon box", c, name="Icon box")
        setp(d, ib, Style="Tonal", Icon="Icon/music_note")
        d.set(ib, width=40, height=40)
        d.cmd("component.exposeInstance", id=ib)
        txt = d.frame(c, name="Labels", layout="v", gap=2, sizing=("fill", "hug"))
        t1 = k.text(txt, "Music and audio", "Component/Settings title", "Color/on", "Title", fill=True)
        t2 = k.text(txt, "Required. Attic needs it to find the songs on this phone.", "Component/Settings summary", "Color/on-2",
                    "Summary", fill=True)
        sb.text("Title", t1, "Music and audio")
        sb.text("Summary", t2, "Required. Attic needs it to find the songs on this phone.")
        if status == "Allow":
            bt = d.instance("Button", c, name="Allow")
            setp(d, bt, Style="Tonal", Size="Small", State="Default", Tone="Default", Label="Allow")
        elif status == "Allowed":
            r = d.frame(c, name="Allowed", layout="h", gap=4, cross="center", sizing=("hug", "hug"))
            k.icon(r, "check", size=20, color="Color/accent-ink", name="Check")
            k.text(r, "Allowed", "Scale/Label", "Color/accent-ink", "Label", lines=1)
        elif status.startswith("Off"):
            k.text(c, "Off", "Scale/Label", "Color/danger" if status == "Off required" else "Color/on-2", "Label", lines=1)
    sb.finish()

    # -- Library scan
    c = cc.single("Library scan", "ComfortOnboarding.kt · LibraryStep", "LibraryStep", layout="v", w=411, cross="center",
                  sizing=("fixed", "hug"))
    sp = d.frame(c, name="Spinner box", w=44, h=44)
    s = d.instance("Spinner", sp, name="Spinner")
    d.cmd("instance.setProperties", ids=[s], properties={"Size": "Large"})
    d.set(s, x=0, y=0)
    tf = d.frame(c, name="Title", layout="v", pad=[24, 0, 0, 0], sizing=("fill", "hug"))
    k.text(tf, "Finding your songs…", "Scale/Bar title", "Color/on", "Title text", fill=True, align="center")
    cc.finish_single("Library scan", c, "ComfortOnboarding.kt · LibraryStep", "LibraryStep",
                     "The first scan, centred: the Large spinner (44dp, 4dp stroke) and \"Finding your songs…\" 24 below "
                     "(20/26 bold). Loading uses the same spinner alone.")

    f = cc.dark_preview()
    d.set_mode(f, "Expressive preview", "Dark")


def _look_preview(d: Doc, k: K, cc: Components):
    """The two live pictures, drawn at full size (398.8dp, the card's inner width / 0.42) and scaled to 0.42."""
    W = 167.5 / 0.42
    sb = SetBuilder(cc, "Look preview", "ComfortOnboarding.kt · ComfortLookPreview", "LookPreview",
                    "OnboardingPreview.kt LookPreview: each style's mini Home laid out at 1/0.42 of the card's inner width and "
                    "drawn at 0.42, in the theme picked. Comfort: \"Home\" in the hero type, the sub-tabs, three empty covers and "
                    "the dock's tab bar. Expressive: the mark and \"Home\", the connected filter buttons, three empty covers "
                    "and the navigation bar, in the Expressive preview colours. The picture is a scaled drawing, so its text "
                    "keeps the sizes but not the links of its styles.")
    for style in ("Comfort", "Expressive"):
        c = sb.variant({"Style": style}, w=167.5, h=100, clip=True)
        k.bind_radius(c, "control")
        if style == "Comfort":
            pic = d.frame(c, name="Picture", layout="v", w=W, fill="Color/ground", sizing=("fixed", "hug"))
            tf = d.frame(pic, name="Title", layout="v", pad=[28, 24, 16, 24], sizing=("fill", "hug"))
            k.text(tf, "Home", "Scale/Hero", "Color/on", "Title text")
            stf = d.frame(pic, name="Sub-tabs row", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
            st = d.instance("Sub-tab row", stf, name="Sub-tabs")
            for i, lab in enumerate(("All", "Albums", "Artists")):
                setp(d, d.child(st, f"Sub-tab: {['All', 'Albums', 'Artists'][i]}"), Label=lab)
            d.set(d.child(st, "Sub-tab: Folders"), visible=False)
            cr = d.frame(pic, name="Covers", layout="h", gap=14, pad=[16, 24, 0, 24], sizing=("fill", "hug"))
            side = (W - 48 - 28) / 3
            for i in range(3):
                cv = _cover(d, k, cr, side, art="None", name=f"Cover {i + 1}")
            dk = d.frame(pic, name="Dock", layout="v", pad=[28, 12, 22, 12], sizing=("fill", "hug"))
            card = d.frame(dk, name="Card", layout="v", fill="Color/raised", clip=True, stroke="Color/edge", stroke_w=1,
                           strokeAlign="inside", sizing=("fill", "hug"))
            k.bind_radius(card, "float")
            tb = d.instance("Tab bar", card, name="Tab bar")
            d.set(tb, sizingH="fill")
        else:
            P = "Expressive preview/"
            pic = d.frame(c, name="Picture", layout="v", w=W, fill=P + "surface", sizing=("fixed", "hug"))
            hd = d.frame(pic, name="Title", layout="h", gap=10, pad=[20, 16, 8, 16], cross="center", sizing=("fill", "hug"))
            m = d.instance("Mark", hd, name="Mark")
            d.set(m, width=round(32 * 36 / 51.5, 2), height=32)
            d.override(m, "Headroom", fill=P + "primary")
            k.text(hd, "Home", "Preview/Headline", P + "on-surface", "Title text")
            # ConnectedButtonGroup: three buttons 2dp apart, the selected one primary with a check.
            grp = d.frame(pic, name="Filters", layout="h", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
            for i, lab in enumerate(("All", "Albums", "Artists")):
                sel = i == 0
                bt = d.frame(grp, name=f"Filter: {lab}", layout="h", gap=6, h=40, main="center", cross="center",
                             fill=P + ("primary" if sel else "surface-container-high"), sizing=("fill", "fixed"))
                d.set(bt, cornerRadius=[20 if i == 0 else 8, 20 if i == 2 else 8, 20 if i == 2 else 8, 20 if i == 0 else 8])
                if sel:
                    k.icon(bt, "check", size=18, color=P + "on-primary", name="Check")
                k.text(bt, lab, "Preview/Label", P + ("on-primary" if sel else "on-surface-variant"), "Label", lines=1)
            cr = d.frame(pic, name="Covers", layout="h", gap=12, pad=[16, 16, 20, 16], sizing=("fill", "hug"))
            side = (W - 32 - 24) / 3
            for i in range(3):
                box = d.frame(cr, name=f"Cover {i + 1}", w=side, h=side, fill=P + "surface-container-highest", radius=16)
                g = side * 0.4
                ic = k.icon(box, "music_note", size=g, color=P + "on-surface-variant", name="No art")
                d.set(ic, x=(side - g) / 2, y=(side - g) / 2)
            nav = d.frame(pic, name="Navigation bar", layout="h", pad=[12, 8, 16, 8], fill=P + "surface-container",
                          sizing=("fill", "hug"))
            for i, (lab, lig) in enumerate((("Home", "home"), ("Search", "search"), ("Library", "library_music"))):
                sel = i == 0
                it = d.frame(nav, name=f"Item: {lab}", layout="v", gap=4, cross="center", sizing=("fill", "hug"))
                ind = d.frame(it, name="Indicator", layout="h", w=56, h=32, main="center", cross="center",
                              fill=P + "secondary-container" if sel else None, sizing=("fixed", "fixed"))
                k.bind_radius(ind, "pill")
                k.icon(ind, lig, filled=sel, color=P + ("on-secondary-container" if sel else "on-surface-variant"), name="Icon")
                k.text(it, lab, "Preview/Nav label", P + ("on-surface" if sel else "on-surface-variant"), "Label", lines=1)
        h = d.get(pic)["height"]
        d.cmd("transform.scale", ids=[pic], factor=0.42)
        d.set(pic, x=0, y=0)
        d.set(c, height=round(h * 0.42, 2))
    sb.finish()


# ------------------------------------------------------------------------ Recap
def _recap(d: Doc, k: K):
    cc = Components(d, "Components / Recap",
                    "Recap's ten cards are one drawing on a fixed 360 x 640 canvas, scaled to wherever they show: the story "
                    "(about 353dp wide), the page's thumbnails (118) and the shared 1080 x 1920 image. Cards always take the "
                    "dark tokens, whatever the phone's theme; the cards about one song or one artist take that cover's wash. "
                    "Source: ui/comfort/ComfortRecapCards.kt, ComfortRecapStory.kt, ComfortRecap.kt.")

    # -- Footer
    c = cc.single("Recap card footer", "ComfortRecapCards.kt · Footer", "Footer", layout="h", w=360, pad=[0, 24, 22, 24], gap=9,
                  cross="center", sizing=("fixed", "hug"))
    mb = d.instance("Mark box", c, name="Mark box")
    setp(d, mb, Style="Solid")
    d.set(mb, width=26, height=26)
    k.text(c, "Attic", "Recap/Footer name", "Color/on", "Name", fill=True, lines=1)
    pt = k.text(c, "2026 so far", "Recap/Footer period", "Color/on-2", "Period", lines=1)
    _modes(d, c, "Dark")
    cc.finish_single("Recap card footer", c, "ComfortRecapCards.kt · Footer", "Footer",
                     "The foot of every card: the mark in a 26dp solid box, \"Attic\" (14/18 bold) and the period (13/18 medium, "
                     "on-2) at the end, 24dp sides, 22 from the bottom.", binds=[("Period", "text", pt, "2026 so far")])

    # -- Rank row
    sb = SetBuilder(cc, "Rank row", "ComfortRecapCards.kt · RankRow", "RankRow",
                    "`.rk`, 78dp: the rank (20/24 extra bold, 16 wide), the 52dp picture, two lines (16/19 bold, 13/17) and the "
                    "count (Song only, 15/20 bold tabular), 20 start, 18 end, 12 between, all in on. Under it the Bed: 6 in, "
                    "4 from top and bottom, radius 18, 88 + share x (width - 100) long, in the row cover's "
                    "Tinted(seed).miniFill. The variant's bed is the example cover's; set each row's from its cover.")
    for kind in ("Song", "Artist"):
        c = sb.variant({"Kind": kind}, layout="h", w=312, h=78, pad=[0, 18, 0, 20], gap=12, cross="center", sizing=("fixed", "fixed"))
        d.set_mode(c, "Color", "Tinted")
        bed = d.rect(c, name="Bed", w=300, h=70, fill="Color/mini-fill", radius=18)
        k.absolute(bed, 6, 4, 300, 70)
        k.text(c, "1", "Recap/Rank", "Color/on", "Rank", w=16, align="center")
        _cover(d, k, c, 52, kind="Artist" if kind == "Artist" else "Album", name="Picture")
        txt = d.frame(c, name="Labels", layout="v", gap=1, sizing=("fill", "hug"))
        t1 = k.text(txt, "Lights Over the Water" if kind == "Song" else "Mira Okafor", "Recap/Rank title", "Color/on", "Title",
                    fill=True, lines=2)
        t2 = k.text(txt, "Mira Okafor" if kind == "Song" else "1,545 minutes", "Recap/Rank subtitle", "Color/on", "Subtitle",
                    fill=True, lines=1)
        sb.text("Title", t1, d.get(t1)["characters"])
        sb.text("Subtitle", t2, d.get(t2)["characters"])
        rk = d.child(c, "Rank")
        sb.text("Rank", rk, "1")
        if kind == "Song":
            v = k.text(c, "96×", "Recap/Rank value", "Color/on", "Value", lines=1)
            sb.text("Value", v, "96×")
    sid = sb.finish()
    _modes(d, sid, "Dark")
    d.set(sid, fill=V("Color/fill"))

    # -- Highlight row
    c = cc.single("Highlight row", "ComfortRecapCards.kt · HighlightsCard", "HighlightsCard", layout="h", w=312,
                  pad=[12, 20, 12, 20], gap=16, cross="center", sizing=("fixed", "hug"))
    d.set(c, minHeight=104)
    hl = d.rect(c, name="Hairline", w=212, h=1, fill="Color/fill-2")
    k.absolute(hl, 80, 0, 212, 1, {"horizontal": "leftRight", "vertical": "top"})
    _hide(d, hl)
    ib = d.instance("Icon box", c, name="Icon box")
    setp(d, ib, Style="Tonal", Icon="Icon/today")
    d.set(ib, width=44, height=44)
    d.cmd("component.exposeInstance", id=ib)
    txt = d.frame(c, name="Labels", layout="v", gap=2, sizing=("fill", "hug"))
    v = k.text(txt, "342 min", "Recap/Highlight value", "Color/on", "Value", fill=True, lines=1)
    ln = k.text(txt, "on Saturday 15 August, your biggest day", "Recap/Highlight line", "Color/on-2", "Caption", fill=True)
    _modes(d, c, "Dark")
    d.set(c, fill=V("Color/fill"))
    cc.finish_single("Highlight row", c, "ComfortRecapCards.kt · HighlightsCard", "HighlightsCard",
                     "A highlight as a settings row on the card's deck: a 44dp tonal icon box, the value (30/34 extra bold) and "
                     "the line (15/20 on-2), 104dp min, 20 sides, 12 above and below. Rows after the first: a hairline in "
                     "fill-2 from 80.",
                     binds=[("Value", "text", v, "342 min"), ("Line", "text", ln, "on Saturday 15 August, your biggest day"),
                            ("Show hairline", "boolean", hl, False)])

    # -- Language track
    c = cc.single("Language track", "ComfortRecapCards.kt · LanguagesCard", "LanguagesCard", layout="v", w=276, sizing=("fixed", "hug"))
    r = d.frame(c, name="Name and share", layout="h", pad=[0, 0, 6, 0], sizing=("fill", "hug"))
    ln = k.text(r, "English", "Recap/Language", "Color/on", "Language", fill=True, lines=1)
    sh = k.text(r, "41%", "Recap/Language share", "Color/on-2", "Share", lines=1)
    tr = d.frame(c, name="Track", w=276, h=16, fill="Color/fill-2", clip=True, sizing=("fill", "fixed"))
    k.bind_radius(tr, "pill")
    f = d.rect(tr, name="Fill", w=113.16, h=16, fill="Color/accent", radius=8)
    _modes(d, c, "Dark")
    cc.finish_single("Language track", c, "ComfortRecapCards.kt · LanguagesCard", "LanguagesCard",
                     "A language on the card: its name (15/20 semibold) and share (15/20 tabular, on-2), 6 above a 16dp fill-2 "
                     "capsule filled to the share (never under 16dp) in its step: accent, derived/language-2, "
                     "derived/language-3; No vocals in on-3.",
                     binds=[("Language", "text", ln, "English"), ("Share", "text", sh, "41%")])

    # -- Listening clock
    sb = SetBuilder(cc, "Listening clock", "ComfortRecapCards.kt · ListeningClock", "ListeningClock",
                    "A 200-unit dial: 24 round-capped bars from radius 41 to 84, midnight at the top, clockwise, each a fill "
                    "track with the accent drawn as far as that hour's share of the busiest (72 % opacity; the busiest at "
                    "full). Labels: midnight and noon in on-3, the busiest hour in the middle. Drawn with the demo library's "
                    "hours (DemoStates.kt); 296dp on the card, 132 on the page.")
    hours = demo_hours()
    peak = max(range(24), key=lambda h: hours[h])
    for labels in (True, False):
        side = 296 if labels else 132
        u = side / 200
        c = sb.variant({"Labels": str(labels)}, w=side, h=side)
        cx = cy = side / 2
        for h in range(24):
            a = math.radians(h * 15)
            dx, dy = math.sin(a), -math.cos(a)

            def at(rr):
                return cx + dx * rr * u, cy + dy * rr * u
            x0, y0 = at(41)
            x1, y1 = at(84)
            tr = d.vector(c, name=f"Hour {h} track", path=f"M{x0:.3f} {y0:.3f} L{x1:.3f} {y1:.3f}", stroke="Color/fill",
                          stroke_w=6.6 * u, strokeCap="round")
            d.set(tr, x=min(x0, x1), y=min(y0, y1))
            frac = hours[h] / hours[peak]
            x2, y2 = at(41 + 43 * frac)
            bar = d.vector(c, name=f"Hour {h}", path=f"M{x0:.3f} {y0:.3f} L{x2:.3f} {y2:.3f}", stroke="Color/accent",
                           stroke_w=6.6 * u, strokeCap="round")
            d.set(bar, x=min(x0, x2), y=min(y0, y2))
            if h != peak:
                d.set(bar, opacity=0.72)
        if labels:
            for txt, style, ink, cyy in (("midnight", "Recap/Clock label", "Color/on-3", -93.5), ("noon", "Recap/Clock label", "Color/on-3", 94.5),
                                         ("7 pm", "Recap/Clock hour", "Color/on", 0)):
                t = k.text(c, txt, style, ink, f"Label: {txt}" if txt != "7 pm" else "Busiest hour", align="center")
                g = d.get(t)
                d.set(t, x=cx - g["width"] / 2, y=cy + cyy * u - g["height"] / 2)
                if txt == "7 pm":
                    sb.text("Busiest hour", t, "7 pm")
    sid = sb.finish()

    # -- the ten cards
    _cards(d, k, cc)

    # -- Story header and deck
    c = cc.single("Story header", "ComfortRecapStory.kt · StoryHead", "StoryHead", layout="h", w=411, h=60, pad=[0, 8, 0, 8],
                  cross="center", sizing=("fixed", "fixed"))
    cb = d.instance("Icon button", c, name="Close")
    setp(d, cb, Icon="Icon/close")
    col = d.frame(c, name="Labels", layout="v", pad=[0, 0, 0, 4], sizing=("fill", "hug"))
    k.text(col, "Recap", "Scale/Caption", "Color/on-2", "Overline", fill=True, lines=1)
    hl = k.text(col, "Your 2026 so far", "Component/Player context", "Color/on", "Headline", fill=True, lines=1)
    _modes(d, c, "Dark")
    cc.finish_single("Story header", c, "ComfortRecapStory.kt · StoryHead", "StoryHead",
                     "`.np-head`: Close, then \"Recap\" (12.5/16 on-2) over the period's headline (15.5/20 semibold), 60dp, 8 sides.",
                     binds=[("Headline", "text", hl, "Your 2026 so far")])

    c = cc.single("Story deck", "ComfortRecapStory.kt · StoryDeck", "StoryDeck", layout="v", w=411, pad=[10, 16, 12, 16],
                  sizing=("fixed", "hug"))
    card = d.frame(c, name="Card", layout="v", pad=14, fill="Color/card", sizing=("fill", "hug"))
    k.bind_radius(card, "float")
    pr = d.frame(card, name="Progress row", layout="h", gap=12, pad=[0, 4, 0, 4], cross="center", sizing=("fill", "hug"))
    p = d.instance("Progress", pr, name="Progress")
    setp(d, p, Thick="False")
    d.set(p, sizingH="fill")
    d.override(p, "Fill", width=round(267 * 0.1, 2))
    pos = k.text(pr, "1 of 10", "Recap/Story position", "Color/on-2", "Position", lines=1)
    br = d.frame(card, name="Buttons", layout="h", gap=10, pad=[14, 0, 0, 0], sizing=("fill", "hug"))
    for nm, style, lig in (("Save", "Plain", "download"), ("Share", "Filled", "ios_share")):
        b = d.instance("Button", br, name=nm)
        setp(d, b, Style=style, Size="Medium", State="Default", Tone="Default", Label=nm, **{"Show leading icon": True})
        setp(d, b, **{"Leading icon": f"Icon/{lig}"})
        d.set(b, sizingH="fill")
    _modes(d, c, "Dark")
    cc.finish_single("Story deck", c, "ComfortRecapStory.kt · StoryDeck", "StoryDeck",
                     "`.st-deck`: one card (radius 30, 14 inside) under the story card, 16 from the sides, 10 above and 12 below. "
                     "A single 10dp progress bar filling steadily through every card's seven seconds (resize Fill: (page + "
                     "progress) / count), \"4 of 10\" beside it (13/18 semibold tabular, on-2), then Save (plain, download) and "
                     "Share (filled, ios_share) sharing the width 10 apart, 14 below. Comfort has no per-card segments.",
                     binds=[("Position", "text", pos, "1 of 10")])

    # -- Recap page parts
    c = cc.single("Play recap", "ComfortRecap.kt · PlayRecap", "PlayRecap", layout="h", w=379, pad=[14, 16, 14, 20], gap=14,
                  cross="center", fill="Color/card", sizing=("fixed", "hug"))
    d.set(c, minHeight=92)
    k.bind_radius(c, "group")
    col = d.frame(c, name="Labels", layout="v", gap=2, sizing=("fill", "hug"))
    k.text(col, "Play recap", "Recap/Play title", "Color/on", "Title", fill=True, lines=1)
    bd = k.text(col, "18,420 minutes of music, told in 10 cards you can share.", "Component/Settings summary", "Color/on-2", "Body", fill=True)
    pb = d.instance("Play button", c, name="Play button")
    setp(d, pb, Size="Medium", Playing="False")
    cc.finish_single("Play recap", c, "ComfortRecap.kt · PlayRecap", "PlayRecap",
                     "`.playrow`: the story as the page's first card, a one-row group: \"Play recap\" (18/24 bold) over the minutes "
                     "sentence (13.5/19 on-2), ending in the medium play button. 92dp min; the whole row opens the story.",
                     binds=[("Body", "text", bd, "18,420 minutes of music, told in 10 cards you can share.")])

    c = cc.single("Stat cell", "Controls.kt · Stats", "Stats", layout="v", w=189, pad=[15, 20, 16, 20], sizing=("fixed", "hug"))
    v = k.text(c, "18,420", "Scale/Stat", "Color/on", "Value", fill=True, lines=1)
    lb = k.text(c, "Minutes", "Component/Stat label", "Color/on-2", "Label", fill=True, lines=1)
    cc.finish_single("Stat cell", c, "Controls.kt · Stats", "Stats",
                     "`.stats`: cells two across in a group, split by 1dp line hairlines (between the cells and above every row "
                     "after the first): a 26/32 bold tabular figure over a 13/18 on-2 label, 20 sides, 15 above, 16 below.",
                     binds=[("Value", "text", v, "18,420"), ("Label", "text", lb, "Minutes")])

    c = cc.single("Language row", "ComfortRecap.kt · Languages", "Languages", layout="h", w=339, h=44, gap=12, cross="center",
                  sizing=("fixed", "fixed"))
    hl = d.rect(c, name="Hairline", w=315, h=1, fill="Color/line")
    k.absolute(hl, 24, 0, 315, 1, {"horizontal": "leftRight", "vertical": "top"})
    _hide(d, hl)
    k.sq(c, 4, 12, 12, "Color/accent", "Swatch")
    ln = k.text(c, "English", "Recap/Language row", "Color/on", "Language", fill=True, lines=1)
    sh = k.text(c, "41%", "Recap/Percent", "Color/on-2", "Share", lines=1)
    cc.finish_single("Language row", c, "ComfortRecap.kt · Languages", "Languages",
                     "One language under the page's split capsule: a 12dp squircle in its step, the name (16/22 medium) and the "
                     "share (14/18 tabular, on-2), 44dp tall; after the first, a line hairline from 24.",
                     binds=[("Language", "text", ln, "English"), ("Share", "text", sh, "41%"), ("Show hairline", "boolean", hl, False)])

    c = cc.single("Clock summary", "ComfortRecap.kt · ListeningClock", "ListeningClock", layout="h", w=379, pad=[18, 20, 18, 18],
                  gap=18, cross="center", fill="Color/card", sizing=("fixed", "hug"))
    k.bind_radius(c, "group")
    cl = d.instance("Listening clock", c, name="Listening clock")
    setp(d, cl, Labels="False")
    col = d.frame(c, name="Labels", layout="v", gap=4, sizing=("fill", "hug"))
    t1 = k.text(col, "Evening unwinder", "Scale/Bar title", "Color/on", "Title", fill=True)
    t2 = k.text(col, "Your evenings have a soundtrack. Your busiest hour starts at 7 pm.", "Component/Settings summary", "Color/on-2",
                "Sentence", fill=True)
    cc.finish_single("Clock summary", c, "ComfortRecap.kt · ListeningClock", "ListeningClock",
                     "When you listen, as one group: the 132dp clock without labels, then the listener type (20/26 bold) over its "
                     "line (13.5/19 on-2), 18 apart.",
                     binds=[("Title", "text", t1, "Evening unwinder"),
                            ("Line", "text", t2, "Your evenings have a soundtrack. Your busiest hour starts at 7 pm.")])

    _dark_stages(d, "Components / Recap", ["Recap card footer", "Highlight row", "Language track", "Story header", "Story deck"])
    cc.dark_preview()


CARD_NAMES = ["Intro", "Minutes", "Top song", "Top songs", "Top artist", "Top artists", "Languages", "Listening clock",
              "Highlights", "Summary"]
WASH_CARDS = {"Top song", "Top artist", "Summary"}


def _cards(d: Doc, k: K, cc: Components):
    sb = SetBuilder(cc, "Recap card", "ComfortRecapCards.kt · ComfortRecapCard", "ComfortRecapCard",
                    "The ten cards, each 360 x 640 dp: the card colour, content 24dp from the sides and 28 from the top, the "
                    "footer at the foot. Always the dark tokens (ProvideComfortColors(ComfortDark)): every variant sets "
                    "Palette, Tinted and Elevation to Dark. Top song, Top artist and Summary are on the cover's wash (Color "
                    "mode Tinted wash). Scale an instance to the width it shows at (story 353, thumbnail 118, share image 1080 "
                    "px = 411dp frame here). Summary currently throws on render (see its annotation).")
    for name in CARD_NAMES:
        c = sb.variant({"Card": name}, w=360, h=640, fill="Color/card", clip=True)
        _modes(d, c, "Dark")
        if name in WASH_CARDS:
            d.set_mode(c, "Color", "Tinted wash")
        body = d.frame(c, name="Content", layout="v", w=312, x=24, y=28, sizing=("fixed", "hug"))
        _card_content(d, k, name, body)
        ft = d.instance("Recap card footer", c, name="Footer")
        g = d.get(ft)
        d.set(ft, x=0, y=640 - g["height"], constraints={"horizontal": "leftRight", "vertical": "bottom"})
        if name == "Summary":
            d.annotate(c, "Known issue: SummaryCard throws on render. SummaryStat's FitText is a BoxWithConstraints (a "
                          "SubcomposeLayout) inside Row(Modifier.height(IntrinsicSize.Min)), which can't answer intrinsic "
                          "measurements (IllegalStateException, ui/comfort/ComfortRecapCards.kt around line 640). The story crashes "
                          "on its last card, and Save or Share of it fails. Drawn here from the code; no reference render exists.",
                       "Known issue")
    sb.finish([{"Card": n} for n in CARD_NAMES])


def _t(d, k, parent, text, style, ink, name, top=0, lines=None, w=312):
    if top:
        f = d.frame(parent, name=f"{name} space", layout="v", pad=[top, 0, 0, 0], sizing=("fill", "hug"))
        return k.text(f, text, style, ink, name, fill=True, lines=lines)
    return k.text(parent, text, style, ink, name, fill=True, lines=lines)


def _deck(d, k, parent, name, top, pad=None, gap=0):
    f = d.frame(parent, name=f"{name} space", layout="v", pad=[top, 0, 0, 0], sizing=("fill", "hug"))
    dk = d.frame(f, name=name, layout="v", pad=pad, gap=gap, fill="Color/fill", clip=True, sizing=("fill", "hug"))
    k.bind_radius(dk, "group")
    return dk


def _card_content(d: Doc, k: K, name: str, body: int):
    if name == "Intro":
        grid = d.frame(body, name="Covers", layout="h", wrap=True, gap=12, crossGap=12, w=312, sizing=("fixed", "hug"))
        for i, cv in enumerate(INTRO_COVERS):
            ci = _cover(d, k, grid, 96, name=f"Cover {i + 1}")
            cover_image(d, ci, COVER[cv])
        _t(d, k, body, "Your year so far", "Recap/Headline 40", "Color/on", "Headline", top=24)
        _t(d, k, body, "18,420 minutes across 69 songs. Here's what you played.", "Recap/Intro body", "Color/on-2", "Body", top=12)
    elif name == "Minutes":
        _t(d, k, body, "You listened for", "Recap/Label", "Color/on-2", "Label")
        f = d.frame(body, name="Lamp space", layout="v", pad=[16, 0, 0, 0], sizing=("fill", "hug"))
        lamp = d.frame(f, name="Lamp", w=312, h=312)
        k.sq(lamp, 5, 312, 312, "Color/accent", "Lamp superellipse")
        num = d.frame(lamp, name="Number", layout="v", w=312, h=312, pad=[0, 20, 0, 20], main="center", cross="center",
                      sizing=("fixed", "fixed"))
        k.text(num, "18,420", "Recap/Big figure", "Color/on-accent", "Figure", lines=1, align="center")
        k.text(num, "minutes", "Recap/Minutes unit", "Color/on-accent", "Unit", lines=1, align="center")
        _t(d, k, body, "That's about 307 hours across 301 days.", "Recap/Minutes line", "Color/on", "Sentence", top=28)
    elif name == "Top song":
        _t(d, k, body, "Your top song", "Recap/Label", "Color/on-2", "Label")
        f = d.frame(body, name="Cover space", layout="v", pad=[16, 0, 0, 0], sizing=("fill", "hug"))
        cover_image(d, _cover(d, k, f, 312, name="Cover"), COVER[0])
        _t(d, k, body, "Lights Over the Water", "Recap/Headline 32", "Color/on", "Title", top=20, lines=2)
        _t(d, k, body, "Mira Okafor", "Recap/Sub", "Color/on-2", "Artist", top=6, lines=1)
        f = d.frame(body, name="Plays space", layout="v", pad=[16, 0, 0, 0], sizing=("fill", "hug"))
        pill = d.frame(f, name="Plays", layout="h", h=40, pad=[0, 18, 0, 18], cross="center", fill="Color/accent",
                       sizing=("hug", "fixed"))
        k.bind_radius(pill, "pill")
        k.text(pill, "Played 96 times", "Recap/Pill", "Color/on-accent", "Plays text", lines=1)
    elif name in ("Top songs", "Top artists"):
        songs = name == "Top songs"
        _t(d, k, body, "Songs you kept coming back to" if songs else "The artists on repeat", "Recap/Headline 34", "Color/on", "Headline")
        dk = _deck(d, k, body, "Deck", 20, pad=[4, 0, 4, 0])
        rows = TOP_SONGS[:5] if songs else TOP_ARTISTS
        top = rows[0][2] if songs else rows[0][1]
        for i, r in enumerate(rows):
            if songs:
                title, sub, val = r[0], r[1], f"{r[2]}×"
                share, cover = r[2] / top, SONG_COVER[r[0]]
            else:
                title, sub, val = r[0], f"{artist_minutes(r[1]):,} minutes", None
                share, cover = r[1] * 225_000 / (top * 225_000), ARTIST_COVER[r[0]]
            rr = d.instance("Rank row", dk, name=f"Row {i + 1}: {title}")
            vals = {"Kind": "Song" if songs else "Artist", "Rank": str(i + 1), "Title": title, "Subtitle": sub}
            if val:
                vals["Value"] = val
            setp(d, rr, **vals)
            d.set(rr, sizingH="fill")
            bed = d.child(rr, "Bed")
            d.set(bed, width=round(88 + (312 - 100) * share, 2))
            if cover != 0:
                # The row cover's Tinted(seed).miniFill, computed per cover (colors/comfort-tinted-N.json, dark): cover
                # content, not a token. The layer's name says so; Color/mini-fill is the Paper Lanterns cover's value.
                d.set(bed, fill=tint_hex(cover, "dark", "miniFill"), name=f"Bed (cover tint: miniFill of cover {cover})")
            cover_image(d, d.child(rr, "Picture"), COVER[cover])
    elif name == "Top artist":
        _t(d, k, body, "Your top artist", "Recap/Label", "Color/on-2", "Label")
        f = d.frame(body, name="Picture space", layout="v", pad=[16, 0, 0, 0], sizing=("fill", "hug"))
        cover_image(d, _cover(d, k, f, 312, kind="Artist", name="Picture"), COVER[0])
        _t(d, k, body, "Mira Okafor", "Recap/Headline 44", "Color/on", "Name", top=22, lines=2)
        _t(d, k, body, "1,545 minutes over 412 plays. Number one of the 9 artists you played.", "Recap/Sub", "Color/on-2", "Body", top=10)
    elif name == "Languages":
        _t(d, k, body, "What you listened in", "Recap/Label", "Color/on-2", "Label")
        _t(d, k, body, "41%", "Recap/Percent large", "Color/on", "Top share", top=10)
        _t(d, k, body, "English", "Recap/Headline 40", "Color/on", "Top language")
        dk = _deck(d, k, body, "Deck", 20, pad=[16, 18, 18, 18], gap=13)
        steps = ["Color/accent", "Color/derived/language-2", "Color/derived/language-3", "Color/on-3"]
        for i, (lang, share) in enumerate(LANGUAGES):
            t = d.instance("Language track", dk, name=f"Language: {lang}")
            setp(d, t, Language=lang, Share=f"{round(share * 100)}%")
            d.set(t, sizingH="fill")
            d.override(t, "Fill", width=round(max(16, 276 * share), 2), fill=steps[i])
    elif name == "Listening clock":
        _t(d, k, body, "Your listening clock", "Recap/Label", "Color/on-2", "Label")
        f = d.frame(body, name="Clock space", layout="v", pad=[8, 0, 0, 0], cross="center", sizing=("fill", "hug"))
        cl = d.instance("Listening clock", f, name="Listening clock")
        setp(d, cl, Labels="True")
        _t(d, k, body, "Evening unwinder", "Recap/Headline 44", "Color/on", "Title", top=12)
        _t(d, k, body, "Your evenings have a soundtrack. Your busiest hour starts at 7 pm.", "Recap/Sub", "Color/on-2", "Sentence", top=8)
    elif name == "Highlights":
        _t(d, k, body, "The good bits", "Recap/Headline 34", "Color/on", "Headline")
        dk = _deck(d, k, body, "Deck", 20)
        for i, (lig, val, line) in enumerate((("today", "342 min", "on Saturday 15 August, your biggest day"),
                                              ("repeat", "14×", "Iravu Rail in a single day"),
                                              ("local_fire_department", "46 days", "in a row with music"),
                                              ("new_releases", "38", "songs played for the first time"))):
            h = d.instance("Highlight row", dk, name=f"Highlight: {val}")
            setp(d, h, Value=val, Line=line, **{"Show hairline": i > 0})
            setp(d, d.child(h, "Icon box"), Icon=f"Icon/{lig}")
            d.set(h, sizingH="fill", fills=[])
    elif name == "Summary":
        hd = d.frame(body, name="Head", layout="h", gap=16, cross="center", sizing=("fill", "hug"))
        cover_image(d, _cover(d, k, hd, 88, name="Cover"), COVER[0])
        k.text(hd, "Your 2026 so far", "Recap/Headline 28", "Color/on", "Headline", fill=True, lines=3)
        dk = _deck(d, k, body, "Stats", 16)
        rr = d.frame(dk, name="Stat row", layout="h", sizing=("fill", "hug"))
        for i, (v, lab) in enumerate((("18,420", "Minutes"), ("English", "Mostly in"))):
            if i:
                d.rect(rr, name="Divider", w=1, h=70, fill="Color/fill-2", sizingV="fill")
            cell = d.frame(rr, name=f"Stat: {lab}", layout="v", pad=[11, 18, 12, 18], sizing=("fill", "hug"))
            k.text(cell, v, "Recap/Summary stat", "Color/on", "Value", lines=1)
            k.text(cell, lab, "Recap/Rank subtitle", "Color/on-2", "Label", lines=1)
        for label, rows, person in (("Top artists", [(a, ARTIST_COVER[a]) for a, _ in TOP_ARTISTS], True),
                                    ("Top songs", [(s, SONG_COVER[s]) for s, _, _ in TOP_SONGS[:5]], False)):
            dk = _deck(d, k, body, label, 10, pad=[12, 18, 11, 18])
            lf = d.frame(dk, name="Label space", layout="v", pad=[0, 0, 5, 0], sizing=("fill", "hug"))
            k.text(lf, label, "Recap/Summary list label", "Color/on-2", "Label", lines=1)
            for i, (nm, cv) in enumerate(rows):
                r = d.frame(dk, name=f"Row {i + 1}: {nm}", layout="h", h=25, gap=10, cross="center", sizing=("fill", "fixed"))
                k.text(r, str(i + 1), "Recap/Summary rank", "Color/on-2", "Rank", w=9)
                ci = _cover(d, k, r, 20, kind="Artist" if person else "Album", name="Picture")
                cover_image(d, ci, COVER[cv])
                k.text(r, nm, "Recap/Language", "Color/on", "Name", fill=True, lines=1)


# ------------------------------------------------------------------------ System
WIDGET = {"Small": (170, 170), "Wide": (352, 96), "Large": (352, 200)}


def _system(d: Doc, k: K):
    cc = Components(d, "Components / System",
                    "What Attic draws outside its own window: the home-screen widget (RemoteViews from res/layout/widget_comfort_*.xml, "
                    "coloured by PlayerWidget.kt), the media notification's content (Android draws the card from MediaStyle), "
                    "the splash and the launcher icon. Source: widget/PlayerWidget.kt, WidgetColors.kt, playback/PlaybackService.kt, "
                    "res/mipmap-anydpi-v26/ic_launcher.xml.")

    # -- Widget
    sb = SetBuilder(cc, "Widget", "widget/PlayerWidget.kt · PlayerWidget", "PlayerWidget",
                    "The Comfort widget (the HTML mock's sheet 10, frame A): a card at radius 30 washed toward the cover "
                    "(WidgetColors.comfort: ComfortTint.apply(base, swatch, wash = true)), so Playing sets Color mode Tinted "
                    "wash; the play button and the mark take the accent. Small (2 x 2): the cover cut to the large superellipse, "
                    "12dp in, the play button set into its corner on a 58dp ring of the card. Wide (4 x 1): 56dp cover, title "
                    "16sp bold and artist 13sp in Onest, previous, play, next. Large (4 x 2): 88dp cover, the song and controls, "
                    "the mark top right, a line hairline and five 56dp album covers. Empty: before anything plays, the neutral "
                    "card, \"Attic\" / \"Tap to open\", neutral boxes with the monochrome mark. Sizes are the renders' launcher cells.")
    for size in ("Small", "Wide", "Large"):
        for state in (("Playing", "Empty") if size == "Large" else ("Playing",)):
            w, h = WIDGET[size]
            empty = state == "Empty"
            c = sb.variant({"Size": size, "State": state}, w=w, h=h, fill="Color/card", clip=True)
            k.bind_radius(c, "float")
            if not empty:
                d.set_mode(c, "Color", "Tinted wash")
            _widget(d, k, c, size, empty)
    sb.finish()

    # -- Media notification, analysis notification, launcher icon
    _notification(d, k, cc)
    _analysis_notification(d, k, cc)
    _launcher(d, k, cc)

    # -- Splash
    c = cc.single("Splash", "res/values/themes.xml · Theme.Attic", "Splash", w=411, h=914, fill="Color/ground", clip=True)
    li = d.instance("Launcher icon", c, name="Icon", x=(411 - 96) / 2, y=(914 - 96) / 2)
    d.set(li, constraints={"horizontal": "center", "vertical": "center"})
    cc.finish_single("Splash", c, "res/values/themes.xml · Theme.Attic", "Splash",
                     "Android 12+'s splash: the window background (comfort_window #F2F1EE light, #000000 dark, which are the "
                     "Comfort ground) with the launcher icon in the middle, masked to a circle, until the first frame.")

    cc.dark_preview()


CABINET = "M35.2 78.5 L35.2 47.78 L71.2 27 L71.2 78.5 Z"
CONE = (_circle(53.2, 60.5, 12) + " " + _circle(53.2, 60.5, 4.8) + " " + _circle(61, 42, 3.8))


def _analysis_notification(d: Doc, k: K, cc: Components):
    c = cc.single("Analysis notification", "analysis/AnalysisWorker.kt · progressNotification", "progressNotification", layout="v",
                  w=379, pad=[16, 20, 12, 20], gap=4, fill="#2D2F36", allow_hex=True, sizing=("fixed", "hug"))
    d.set(c, cornerRadius=24)
    head = d.frame(c, name="Header (system)", layout="h", gap=8, cross="center", sizing=("fill", "hug"))
    ib = d.frame(head, name="Small icon", w=24, h=24, layout="h", main="center", cross="center", fill="Color/accent")
    d.set(ib, cornerRadius=12)
    drawable_icon(d, k.icon(ib, "graphic_eq", size=16, color="Color/on-accent", name="Glyph"), wrap=True)
    d.text(head, "Attic · Song analysis", style="System/Small", color="#C4C6CF", allow_hex=True, name="App line (system)")
    d.text(c, "Analysing your songs", style="System/Title", color="#E3E3E8", allow_hex=True, name="Title (Attic)")
    d.text(c, "41 of 69", style="System/Body", color="#C4C6CF", allow_hex=True, name="Text (Attic)")
    bar = d.frame(c, name="Progress (system)", layout="v", pad=[8, 0, 8, 0], sizing=("fill", "hug"))
    tr = d.frame(bar, name="Track", h=4, fill="#FFFFFF33", allow_hex=True, radius=2, clip=True, sizing=("fill", "fixed"))
    d.rect(tr, name="Done", w=round(339 * 41 / 69, 2), h=4, fill="Color/accent", radius=2)
    d.text(c, "Stop", style="System/Title", color="#B7CAFF", allow_hex=True, name="Action: Stop (Attic)")
    cc.finish_single("Analysis notification", c, "analysis/AnalysisWorker.kt · progressNotification", "progressNotification",
                     "A standard progress notification, ongoing and silent, on the \"Song analysis\" channel. Attic supplies the "
                     "small icon (ic_comfort_analysis, graphic_eq), its colour (setColor: Comfort's accent, which fills the icon's "
                     "circle), the title \"Analysing your songs\", \"41 of 69\", the progress value and the Stop action. The card, "
                     "type, bar and action colours are the system's (layers marked (system)).")


def _launcher(d: Doc, k: K, cc: Components):
    sb = SetBuilder(cc, "Launcher icon", "res/mipmap-anydpi-v26/ic_launcher.xml · adaptive-icon", "ic_launcher",
                    "One adaptive icon for both styles, drawn on the 108dp canvas (shown at 96, the launcher's mask a circle here). "
                    "Colour: the terracotta background layer with Headroom in cream and dark brown (ic_launcher_foreground.xml). "
                    "Monochrome: the one-colour mark (ic_launcher_monochrome.xml) that Android 13+ tints for themed icons; the "
                    "tints are the system's, shown here in its light-theme pair. Brand colours are in the Brand collection.")
    s = 96 / 108
    for layer in ("Colour", "Monochrome"):
        c = sb.variant({"Layer": layer}, w=96, h=96, clip=True)
        d.set(c, cornerRadius=48)
        if layer == "Colour":
            d.rect(c, name="Background", w=96, h=96, fill="Brand/headroom/ground")
            cab = d.vector(c, name="Cabinet", path=CABINET, fill="Brand/headroom/cabinet")
            cone = d.vector(c, name="Cone", path=CONE, fill="Brand/headroom/cone")
            d.cmd("vector.fillRule", ids=[cone], rule="evenOdd")
        else:
            d.rect(c, name="Background (system tint)", w=96, h=96, fill="#D8E2FF", allow_hex=True)
            cab = d.vector(c, name="Mark (system tint)", path=CABINET + " " + CONE, fill="#2A4174", allow_hex=True)
            d.cmd("vector.fillRule", ids=[cab], rule="evenOdd")
            cone = None
        d.set(cab, width=36 * s, height=51.5 * s, x=35.2 * s, y=27 * s)
        if cone is not None:
            d.set(cone, width=24 * s, height=34.3 * s, x=41.2 * s, y=38.2 * s)
    sb.finish()


def _notification(d: Doc, k: K, cc: Components):
    W, H = 379, 184
    c = cc.single("Media notification", "playback/PlaybackService.kt · MediaSession", "PlaybackService", w=W, h=H, clip=True,
                  fill="#3B2A3A", allow_hex=True)
    d.set(c, cornerRadius=28)
    bg = d.rect(c, name="Cover (system crop)", w=W, h=H, fill="Color/fill")
    d.cmd("image.place", path=COVER[0], target=bg, mode="fill")
    d.rect(c, name="Scrim (system)", w=W, h=H, fills=[{"type": "linearGradient", "stops": ["#000000A6", "#00000066"],
                                                         "from": [0, 0.5], "to": [1, 0.5]}])
    ic = d.frame(c, name="App icon (system)", w=28, h=28, x=16, y=16, fill="#FFFFFF33", allow_hex=True, radius=14)
    m = d.instance("Mark", ic, name="Small icon", x=(28 - 10.5) / 2, y=(28 - 15) / 2)
    d.set(m, width=10.48, height=15)
    d.override(m, "Headroom", fill="#FFFFFF", allow_hex=True)
    chip = d.frame(c, name="Output (system)", layout="h", gap=4, pad=[0, 10, 0, 8], h=28, cross="center", fill="#FFFFFF33",
                   allow_hex=True, x=W - 16 - 104, y=16, sizing=("hug", "fixed"))
    d.set(chip, cornerRadius=14)
    sp = d.instance("Icon/volume_up", chip, name="Speaker")
    d.set(sp, width=16, height=16)
    drawable_icon(d, sp, wrap=True)
    d.override(sp, "Glyph", fill="#FFFFFF", allow_hex=True)
    d.text(chip, "This phone", style="System/Small", color="#FFFFFF", allow_hex=True, name="Output label")
    txt = d.frame(c, name="Song", layout="v", gap=2, x=16, y=64, w=260, sizing=("fixed", "hug"))
    d.text(txt, "Lights Over the Water", style="System/Title", color="#FFFFFF", allow_hex=True, name="Title (Attic)")
    d.text(txt, "Mira Okafor", style="System/Body", color="#FFFFFFB3", allow_hex=True, name="Artist (Attic)")
    pb = d.frame(c, name="Play (system)", w=52, h=52, x=W - 16 - 52, y=60, fill="#F9B4A0", allow_hex=True, radius=16)
    pi = d.instance("Icon/pause (filled, 400)", pb, name="Pause", x=14, y=14)
    drawable_icon(d, pi)
    d.override(pi, "Glyph", fill="#3B1D12", allow_hex=True)
    bar = d.frame(c, name="Actions", layout="h", gap=0, x=4, y=H - 56, w=W - 8, h=48, cross="center", sizing=("fixed", "fixed"))
    for nm, lig in (("Shuffle (Attic)", "shuffle"), ("Previous", "skip_previous")):
        b = d.frame(bar, name=nm, w=48, h=48, layout="h", main="center", cross="center")
        g = d.instance(f"Icon/{lig}", b, name="Icon")
        drawable_icon(d, g, wrap=True)
        d.override(g, "Glyph", fill="#FFFFFF", allow_hex=True)
    sk = d.frame(bar, name="Seek bar (system)", h=48, layout="v", main="center", sizing=("fill", "fixed"))
    tr = d.frame(sk, name="Track", h=4, fill="#FFFFFF4D", allow_hex=True, radius=2, sizing=("fill", "fixed"), clip=True)
    d.rect(tr, name="Played", w=70, h=4, fill="#FFFFFF", allow_hex=True, radius=2)
    for nm, lig, filled in (("Next", "skip_next", False), ("Favourite (Attic)", "favorite", True)):
        b = d.frame(bar, name=nm, w=48, h=48, layout="h", main="center", cross="center")
        g = d.instance(f"Icon/{lig}" + (" (filled)" if filled else ""), b, name="Icon")
        drawable_icon(d, g, wrap=True)
        d.override(g, "Glyph", fill="#FFFFFF", allow_hex=True)
    cc.finish_single("Media notification", c, "playback/PlaybackService.kt · MediaSession", "PlaybackService",
                     "Android draws this card from Attic's MediaSession (MediaStyle); only the content is Attic's: the title, "
                     "artist and cover, the Headroom small icon (media3_notification_small_icon.xml, the mark in white) and two "
                     "custom buttons in the overflow slots, shuffle (ic_comfort_shuffle_on/off) and favourite "
                     "(ic_comfort_favourite_on/off). The card shape, scrim, colours (taken from the cover by the system), output "
                     "chip, seek bar and button layout are the system's and change with the Android version: layers marked "
                     "(system) are not Attic's design.")


def _widget(d: Doc, k: K, c: int, size: str, empty: bool):
    def art(parent, side, name, n=4, image=0):
        if empty:
            mb = d.instance("Mark box", parent, name=name)
            setp(d, mb, Style="Neutral")
            d.set(mb, width=side, height=side)
            return mb
        a = k.sq(parent, n, side, side, "Color/fill", name)
        d.cmd("image.place", path=COVER[image], target=a, mode="fill")
        return a

    def play(parent, name="Play"):
        f = d.frame(parent, name=name, w=44, h=44)
        k.sq(f, 4, 44, 44, "Color/accent", "Container")
        ic = k.icon(f, "play_arrow" if empty else "pause", size=26, w400=True, color="Color/on-accent", name="Icon")
        d.set(ic, x=9, y=9)
        drawable_icon(d, ic)
        return f

    def skip(parent, lig, w=44, pad=7):
        f = d.frame(parent, name="Previous" if "prev" in lig else "Next", w=w, h=44)
        g = 30
        ic = k.icon(f, lig, size=g, w400=True, filled=False, color="Color/on", name="Icon")
        d.set(ic, x=(w - g) / 2, y=7)
        drawable_icon(d, ic)
        return f

    if size == "Small":
        a = art(c, 146, "Cover", n=5)
        d.set(a, x=12, y=12)
        ring = d.frame(c, name="Play ring", w=58, h=58, x=170 - 7 - 58, y=170 - 7 - 58)
        k.sq(ring, 4, 58, 58, "Color/card", "Ring")
        p = play(ring)
        d.set(p, x=7, y=7)
        return
    if size == "Wide":
        d.set(c, layout="horizontal", padding=[0, 8, 0, 12], crossAlign="center")
        art(c, 56, "Cover")
        txt = d.frame(c, name="Labels", layout="v", pad=[0, 0, 0, 12], sizing=("fill", "hug"))
        k.text(txt, "Lights Over the Water", "Widget/Title", "Color/on", "Title", fill=True, lines=1)
        af = d.frame(txt, name="Artist space", layout="v", pad=[3, 0, 0, 0], sizing=("fill", "hug"))
        k.text(af, "Mira Okafor", "Widget/Artist", "Color/on-2", "Artist", fill=True, lines=1)
        sp = d.frame(c, name="Gap", w=4, h=1)
        skip(c, "skip_previous", w=40)
        pw = d.frame(c, name="Play space", layout="h", pad=[0, 2, 0, 2], sizing=("hug", "hug"))
        play(pw)
        skip(c, "skip_next", w=40)
        return
    # Large: the song row (weight 1), the hairline, the album tray
    row_ = d.frame(c, name="Song", layout="h", w=352, h=117, x=0, y=0, pad=[14, 14, 12, 14], cross="center",
                   sizing=("fixed", "fixed"))
    art(row_, 88, "Cover")
    txt = d.frame(row_, name="Labels", layout="v", pad=[0, 0, 0, 14], sizing=("fill", "hug"))
    k.text(txt, "Attic" if empty else "Lights Over the Water", "Widget/Title", "Color/on", "Title", fill=True, lines=1)
    af = d.frame(txt, name="Artist space", layout="v", pad=[3, 30, 0, 0], sizing=("fill", "hug"))
    k.text(af, "Tap to open" if empty else "Mira Okafor", "Widget/Artist", "Color/on-2", "Artist", fill=True, lines=1)
    # The controls row keeps its place in the column (Controls space) but sits 11dp left of the text
    # (layout_marginStart="-11dp"), so the previous glyph lines up with the title.
    d.frame(txt, name="Controls space", w=10, h=47)
    d.set(txt, clip=False)
    ctl = d.frame(txt, name="Controls", layout="h", pad=[3, 0, 0, 0], cross="center", sizing=("hug", "hug"))
    skip(ctl, "skip_previous")
    pw = d.frame(ctl, name="Play space", layout="h", pad=[0, 8, 0, 8], sizing=("hug", "hug"))
    play(pw)
    skip(ctl, "skip_next")
    d.set(ctl, absolute=True)
    d.set(ctl, x=3, y=39)
    mk = d.instance("Mark", c, name="Mark", x=352 - 16 - 12.58, y=15)
    d.set(mk, width=12.58, height=18)
    d.override(mk, "Headroom", fill="Color/accent")
    d.rect(c, name="Hairline", w=320, h=1, x=16, y=117, fill="Color/line")
    tray = d.frame(c, name="Albums", layout="h", w=352, h=82, x=0, y=118, pad=[12, 16, 14, 16], main="spaceBetween",
                   sizing=("fixed", "fixed"))
    for i, cv in enumerate((0, 8, 9, 3, 10)):
        art(tray, 56, f"Album {i + 1}", image=cv)
