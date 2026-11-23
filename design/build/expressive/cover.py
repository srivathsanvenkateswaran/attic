"""Pages Cover and Changelog.

Owner: finishing agent. Runs after every screen module, so it can count what the file holds.

Cover
-----
``Cover`` is a 1920 x 1080 frame drawn in the style's own parts: Color mode Cover (the Paper Lanterns scheme),
the Now Playing cover pager, title block, wavy seek bar and transport over a Cookie 9 shape, the style picker
as a connected button group, filter chips for the status line, and the launcher icon. Beside it, ``Read me``
says what each page holds, which fonts to install, where colour comes from, that the code wins, and where the
design docs live.

Changelog
---------
``Changelog entry`` is a main component on the Changelog page (text properties Version, Date, Author,
Changes). Each release is an instance of it, newest at the top of the ``Entries`` column. The page says how to
add the next one and bump the version on the Cover.
"""
from __future__ import annotations

from figbuild.kit import Doc, V
from expressive import VERSION
from expressive.foundations import cover_path, tstyle

REFS: dict[str, str] = {}

UPDATED = "2026-11-23"
CODE_REF = "ui-redesign at 4fe4dab"
AUTHOR = "Attic maintainers"
REPO_URL = "github.com/srivathsanvenkateswaran/attic"
DESCRIPTION = ("Material 3 Expressive for Attic: colour from the wallpaper and from the cover that is playing, "
               "Google Sans Flex, shapes that morph, a wavy seek bar and springs for motion.")

W, H = 1920, 1080


def _inst(d: Doc, comp: str, parent, *, name=None, x=None, y=None, **variant) -> int:
    props = {k.replace("_", " "): str(v) for k, v in variant.items()}
    i = d.instance(comp, parent, name=name, props=props or None)
    if x is not None or y is not None:
        d.set(i, x=x or 0, y=y or 0)
    return i


def _prop(d: Doc, comp: str, short: str) -> str:
    """The full property name (FigCraft adds an id suffix) of a component's text or swap property."""
    for p in d.props.get(comp, []):
        if p == short or p.split("#")[0] == short:
            return p
    return short


def _segment_label(d: Doc, group: int, n: int, text: str):
    seg = d.child(group, f"Segment {n}")
    d.set(d.child(seg, "Label"), characters=text)


def _counts(d: Doc) -> dict:
    screens = d.screens.values()
    sets = [n for n in d.comp if not n.startswith(("Icon/", "Shape/"))]
    return {
        "screens": sum(1 for s in screens if s["page"] == "Screens"),
        "system": sum(1 for s in screens if s["page"] == "System surfaces"),
        "frames": sum(1 for s in screens for t in ("light", "dark") if s.get(t)),
        "components": len(sets),
        "icons": len([n for n in d.icon_names if not n.endswith("(filled)")]),
        "filled": len([n for n in d.icon_names if n.endswith("(filled)")]),
    }


# ───────────────────────────────────────────────────────────────── Cover
def build_cover(d: Doc, ctx: dict):
    pid = d.page("Cover")
    f = d.frame(None, name="Cover", x=0, y=0, w=W, h=H, fill="Color/surface-container-low", clip=True)
    d.set_mode(f, "Color", "Cover")
    for c in d.mode_collections:
        d.set_mode(f, c, "Light")
    ctx["cover_frame"] = f

    # Right: the playing song over a Cookie 9, the way Now Playing sits on the cover's colours.
    cookie = _inst(d, "Shape/Cookie 9", f, name="Cookie 9", x=1010, y=40)
    d.set(cookie, width=1000, height=1000, rotation=0)
    d.set(d.child(cookie, "Outline"), fill=V("Color/primary-container"))
    sunny = _inst(d, "Shape/Sunny 8", f, name="Sunny 8", x=1700, y=830)
    d.set(sunny, width=260, height=260)
    d.set(d.child(sunny, "Outline"), fill=V("Color/tertiary-container"))

    player = d.frame(f, name="Now playing", x=1240, y=150, w=540, layout="v", gap=28, pad=[0, 0, 0, 0],
                     cross="center", sizing=("fixed", "hug"))
    pager = _inst(d, "Now playing/Cover pager", player, name="Cover pager", Playing="True", Art="Cover")
    tb = _inst(d, "Now playing/Title block", player, name="Title block", Lyric="False")
    seek = _inst(d, "Seek bar", player, name="Seek bar", Playing="True")
    times = _inst(d, "Seek times", player, name="Seek times")
    tr = _inst(d, "Transport", player, name="Transport", Playing="True")
    d.set(tb, sizingH="fixed", width=460)

    # Left: what the file is.
    left = d.frame(f, name="Title block", x=120, y=96, w=1000, layout="v", gap=36, sizing=("fixed", "hug"))
    brand = d.frame(left, name="Brand", layout="h", gap=24, cross="center", sizing=("hug", "hug"))
    _inst(d, "Launcher icon", brand, name="Launcher icon", Layer="Colour")
    # The cover's own sizes as text styles (documentation chrome, so under Doc/), in the app's emphasised type.
    tstyle(d, "Doc/Cover app name", size=44, line=52, weight=700, round=True,
           description="The app name on the Cover: Title/Large Emphasized at 44/52.")
    tstyle(d, "Doc/Cover title", size=116, line=124, weight=700, round=True, tracking=-2,
           description="The file's title on the Cover: Display/Large Emphasized at 116/124.")
    tstyle(d, "Doc/Cover description", size=32, line=44, weight=400,
           description="The style's one-line description on the Cover: Title/Large at 32/44.")
    app = d.text(brand, "Attic", style="Doc/Cover app name", color="Color/on-surface", name="App name")
    title = d.text(left, "Attic Expressive", style="Doc/Cover title", color="Color/on-surface", name="File title")
    d.set(title, width=1000, textAutoResize="height")
    desc = d.text(left, DESCRIPTION, style="Doc/Cover description", color="Color/on-surface-variant",
                  name="Style description")
    d.set(desc, width=820, textAutoResize="height")

    meta = d.frame(left, name="Status", layout="h", gap=12, wrap=True, crossGap=12, w=1000, sizing=("fixed", "hug"))
    label = _prop(d, "Filter chip", "Label")
    for text, sel in [(f"Version {VERSION}", False), ("Ready for development", True),
                      (f"Last updated {UPDATED}", False), (f"Matches {CODE_REF}", False)]:
        chip = _inst(d, "Filter chip", meta, name=f"Chip · {text}", Selected=str(sel))
        d.cmd("instance.setProperties", ids=[chip], properties={label: text})


    style_row = d.frame(left, name="Style", layout="v", gap=12, sizing=("hug", "hug"))
    d.text(style_row, "Style", style="Label/Large", color="Color/on-surface-variant", name="Style label")
    grp = _inst(d, "Connected button group", style_row, name="Style switch", Options="2", Selected="1", On_cell="True")
    _segment_label(d, grp, 1, "Expressive")
    _segment_label(d, grp, 2, "Comfort")
    d.set(grp, sizingH="fixed", width=360)

    # Demo covers in the shapes the kit clips art to (album squircle, artist cookie), then the wavy progress.
    shelf = d.frame(left, name="Demo covers", layout="h", gap=20, cross="center", sizing=("hug", "hug"))
    for n, (idx, kind) in enumerate([(3, "Album"), (8, "Album"), (6, "Artist"), (2, "Album"), (9, "Album"),
                                     (11, "Album")]):
        cv = _inst(d, "Cover", shelf, name=f"Cover {idx}", Kind=kind, Size="112")
        for pic in d.find(cv, name="Picture", limit=4):
            d.cmd("image.replace", ids=[pic], path=cover_path(idx))
    wave = _inst(d, "Progress/Wavy", f, name="Wavy progress", x=120, y=980, Running="True")
    d.set(wave, width=812, height=22)

    # Read me, beside the frame.
    build_readme(d, ctx, x=W + 120, y=0)
    return f


def build_readme(d: Doc, ctx: dict, *, x: float, y: float):
    c = _counts(d)
    card = d.frame(None, name="Read me", x=x, y=y, w=960, layout="v", gap=36, pad=64, fill="Canvas/card",
                   radius=32, sizing=("fixed", "hug"))
    d.text(card, "Read me", style="Doc/Section title", color="Canvas/label", name="Title")

    def block(heading: str, *lines: str, code: str | None = None):
        b = d.frame(card, name=f"Block · {heading}", layout="v", gap=10, sizing=("fill", "hug"))
        d.text(b, heading, style="Doc/Heading", color="Canvas/label", name="Heading")
        for n, line in enumerate(lines):
            d.text(b, line, style="Doc/Body", color="Canvas/label",
                   name=f"{heading} text" + (f" {n + 1}" if len(lines) > 1 else ""), width=832,
                   textAutoResize="height")
        if code:
            d.text(b, code, style="Doc/Code", color="Canvas/label-muted", name="Path", width=832,
                   textAutoResize="height")
        return b

    pages = [
        ("Cover, Changelog", "this page, and what changed in each version."),
        ("Colour", "the Wallpaper, Cover and Color collections, with every role in Light and Dark."),
        ("Type", "the type scale and its emphasised partners in Google Sans Flex."),
        ("Spacing, radii and shapes", "spacing and radius tokens and the Expressive shapes."),
        ("Elevation and motion", "tonal elevation, the four shadows, the springs and every transition."),
        ("Icons", f"the {c['icons']} Material Symbols glyphs the app ships, {c['filled']} of them also filled."),
        ("Components / …", f"{c['components']} components and sets, each with a Dark preview, its Compose "
                           "source and a Code Connect link."),
        ("Screens", f"{c['screens']} screen states in Light and Dark, wired as a prototype."),
        ("Flow overview", "the user-flow diagram: every screen and the action that leads to it."),
        ("System surfaces", "the widget, the media notification, the lock screen, the splash and the launcher icon."),
    ]
    b = d.frame(card, name="Block · Pages", layout="v", gap=10, sizing=("fill", "hug"))
    d.text(b, "Pages", style="Doc/Heading", color="Canvas/label", name="Heading")
    for n, (page, what) in enumerate(pages):
        row = d.frame(b, name=f"Page · {page}", layout="h", gap=16, sizing=("fill", "hug"))
        d.text(row, page, style="Doc/Label", color="Canvas/label", name="Page", width=260, textAutoResize="height")
        d.text(row, what, style="Doc/Body", color="Canvas/label", name="What", width=556, textAutoResize="height")

    block("Fonts",
          "Install Google Sans Flex, Onest and Material Symbols Rounded before editing. Google Sans Flex sets "
          "every Attic text style; Onest sets only the First run/Comfort * styles, for the Comfort picture on the "
          "first run's look card; Material Symbols Rounded draws the icon glyphs. All three are in the repository.",
          code="app/src/main/res/font/")
    block("Colour",
          "On a phone the colours come from the wallpaper, and on player surfaces (mini player, Now Playing, "
          "queue, lyrics, collection headers) from the cover of the song. Both are computed at runtime, so the "
          "file shows one example of each: the Wallpaper collection holds one wallpaper scheme, the Cover "
          "collection the scheme of the Paper Lanterns cover.",
          "Bind to Color/<role>. Set Color mode Cover on a frame to draw it in the cover's scheme. Light and Dark "
          "are modes of Wallpaper and Cover.")
    block("The code wins",
          "This file follows the Compose code in app/src/main/java/app/attic/player/ui/expressive/. Each screen "
          "and component names its composable in a Source annotation or its description. Where they disagree, "
          "the code is right: change the file to match it, or change the code first.")
    block("Design docs",
          "design/README.md in the repository explains how the design files are built and kept.",
          code=REPO_URL)
    return card


# ───────────────────────────────────────────────────────────────── Changelog
def build_changelog(d: Doc, ctx: dict):
    from expressive.foundations import DocPage
    c = _counts(d)
    p = DocPage(d, "Changelog", "Changelog",
                "Every version of this file, newest first. The version and date on the Cover match the top entry.")

    # The entry block: a main component with a text property for each field.
    entry = d.component(None, name="Changelog entry", x=0, y=p.y, w=1400, layout="v", gap=20, pad=48,
                        fill="Canvas/card", radius=28, sizing=("fixed", "hug"),
                        description="One release of this design file. Duplicate an instance for the next version "
                                    "and fill in Version, Date, Author and Changes.")
    head = d.frame(entry, name="Head", layout="h", gap=24, cross="baseline", sizing=("fill", "hug"))
    t_ver = d.text(head, "0.0.0", style="Doc/Section title", color="Canvas/label", name="Version")
    t_date = d.text(head, "YYYY-MM-DD", style="Doc/Heading", color="Canvas/label-muted", name="Date")
    t_auth = d.text(entry, "Author", style="Doc/Label", color="Canvas/label-muted", name="Author")
    rule = d.rect(entry, name="Rule", w=1304, h=1, fill="Canvas/divider")
    d.set(rule, sizingH="fill")
    t_chg = d.text(entry, "What changed.", style="Doc/Body", color="Canvas/label", name="Changes", width=1304,
                   textAutoResize="height")
    d.set(t_chg, sizingH="fill")
    names = {}
    for label, layer, default in [("Version", t_ver, "0.0.0"), ("Date", t_date, "YYYY-MM-DD"),
                                  ("Author", t_auth, "Author"), ("Changes", t_chg, "What changed.")]:
        pn = d.prop(entry, label, "text", layer=layer, default=default)
        d.bind_prop(layer, pn, "characters")
        names[label] = pn

    # Entries column: newest first.
    col = d.frame(None, name="Entries", x=0, y=p.y, layout="v", gap=40, sizing=("hug", "hug"))
    changes = (
        "First version, drawn from the Compose code on ui-redesign at 4fe4dab.\n"
        "• Foundations: Wallpaper, Cover and Color collections in Light and Dark, the type scale and its "
        "emphasised partners, spacing and radius tokens, the Expressive shapes, shadows, springs and transitions, "
        f"{c['icons']} icons.\n"
        f"• Components: {c['components']} components and sets with Dark previews, Compose sources and Code Connect.\n"
        f"• Screens: {c['screens']} screen states in Light and Dark: first run, Home, Search, Library, album, "
        "artist, playlist, folder, Favourites, Now Playing, queue, lyrics, every sheet, dialog and message, "
        "sleep timer, Share, Open with, Settings, backup and restore, licences, Recap and its story.\n"
        "• Prototype: the main flows wired with the app's transitions, with named starting points; the Flow "
        "overview diagram.\n"
        f"• System surfaces: widget sizes, media notification, lock screen, splash and launcher icon "
        f"({c['system']} frames).")
    first = d.instance(entry, col, name=f"Entry · {VERSION}")
    d.cmd("instance.setProperties", ids=[first], properties={
        names["Version"]: VERSION, names["Date"]: UPDATED, names["Author"]: AUTHOR, names["Changes"]: changes})
    d.set(first, sizingH="fixed", width=1400)
    p.done(col)

    # How to add the next entry.
    how = p.card("Adding a version",
                 "1. Copy the top instance of Changelog entry, paste it at the top of Entries, and fill in Version, "
                 "Date, Author and Changes. Write what changed for someone building the app: which screens, "
                 "components or tokens, and the code commit the file now matches.\n"
                 "2. Bump the version: a fix to match the code is a patch (1.0.1), new screens or components a minor "
                 "version (1.1.0), a change to how the file is organised or to token names a major version (2.0.0).\n"
                 "3. On the Cover, change the Version and Last updated chips and the commit in \"Matches …\".\n"
                 "4. Change VERSION in design/build/expressive/__init__.py if the file is rebuilt from the generator.",
                 None, width=1400)
    p.done(how)

    # The main component sits to the right, labelled.
    d.set(entry, x=1600, y=d.get(col)["y"])
    d.text(None, "Entry template (main component)", style="Doc/Label", color="Canvas/label-muted",
           name="Label: Entry template", x=1600, y=d.get(col)["y"] - 36)
    ctx["changelog_entry"] = entry


def build(d: Doc, ctx: dict):
    build_cover(d, ctx)
    build_changelog(d, ctx)
