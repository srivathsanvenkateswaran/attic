"""Pages Cover and Changelog.

Owner: finishing agent.

COVER
  `Cover` frame, 1920 x 1080, in the Comfort palette (Light): the file's name, the style in one line, a dock
  and a washed Now Playing drawn from instances of the real components (Mark box, Dock, Now playing cover,
  Now playing title, Seek bar, Transport) with the demo cover Paper Lanterns, and the version, status, date
  and the commit the file matches. Beside it on the page, `Read me`: what each page holds, the fonts to install,
  that the code wins, and where the design docs live.

CHANGELOG
  `Doc/Changelog entry`, a main component with text properties Version, Date, Author and Changes, and one
  instance of it per release, newest on top, in the `Entries` column. A note says how to add the next entry and
  bump the version on the Cover.

TEXT STYLES ADDED HERE (Onest, the style's own face; only the cover uses them)
  Cover/Title 132/136 800, Cover/Lede 34/46 400, Cover/Label 18/24 500, Cover/Value 28/34 600, Cover/Status 20/24 600.

When the version changes: bump VERSION in comfort/__init__.py, add an entry to CHANGES below (newest first), and
set UPDATED and COMMIT. The Cover reads all four.
"""
from __future__ import annotations

from figbuild.kit import Doc, V

from . import VERSION
from .components_core import COVER, cover_image

REFS: dict[str, str] = {}

UPDATED = "2026-11-23"
COMMIT = "4fe4dab"
AUTHOR = "Attic maintainers"
STYLE_LINE = "Comfort: superellipse cards, the Lamplight amber accent, a bottom dock, colour from the album art"

# Newest first. Each entry: (version, date, changes as lines).
CHANGES = [
    ("1.0.0", "2026-11-23", [
        "First version, drawn from ui/comfort/ on ui-redesign at 4fe4dab.",
        "Foundations: the Palette, Tinted and Color variables (Light and Dark; Base, Tinted and Tinted wash), "
        "Onest type styles, radius, spacing and motion tokens, the two shadow styles, and every Material Symbols "
        "glyph the app ships.",
        "Components on eight pages, each set described with its Compose source and linked to code "
        "(app.attic.player.ui.comfort).",
        "Every screen in Light and Dark on the Screens page: Home, Search, Library, collections, Now Playing, "
        "queue, lyrics, sheets and dialogs, sleep timer, Share, Open with, Settings with backup and restore, "
        "licences, Recap with its story and share images, the first run, permission, empty and loading states.",
        "Widget, notification, lock screen, splash and launcher icon on System surfaces.",
        "Prototype links for the main flows, flow starting points, and the Flow overview diagram.",
    ]),
]

PAGES = [
    ("Cover", "This page: name, version, status and the commit the file matches."),
    ("Changelog", "One entry per version, newest on top."),
    ("Colour", "Palette (Light, Dark), Tinted (colour from the cover) and the Color roles components bind."),
    ("Type", "The Onest text styles, from ComfortType.kt and the kit's inline styles."),
    ("Spacing, radii and shapes", "Spacing and radius tokens, the superellipse shapes."),
    ("Elevation and motion", "The two shadows, and the durations and springs the code animates with."),
    ("Icons", "Every Material Symbols Rounded glyph the app ships, outlined and filled."),
    ("Components / ...", "Eight pages of component sets, each with a Dark preview."),
    ("Screens", "Every screen, Light row above Dark row, one section per area. Prototype links live here."),
    ("Flow overview", "How the screens connect, with the action that leads from one to the next."),
    ("System surfaces", "Widget, notification shade, lock screen, splash and launcher icon."),
]


def _styles(d: Doc):
    for name, size, line, weight, tracking in (
        ("Cover/Title", 132, 136, 800, -4),
        ("Cover/Lede", 34, 46, 400, 0),
        ("Cover/Label", 18, 24, 500, 0),
        ("Cover/Value", 28, 34, 600, 0),
        ("Cover/Status", 20, 24, 600, 0),
    ):
        if name not in d.text_styles:
            d.text_style(name, family="Onest", size=size, line=line, weight=weight, tracking=tracking,
                         description="Cover page only (comfort/cover.py).")


def _light(d: Doc, fid: int, color_mode: str | None = None):
    for c in d.mode_collections:
        d.set_mode(fid, c, "Light")
    if color_mode:
        d.set_mode(fid, "Color", color_mode)


def _scaled(d: Doc, comp: str, parent, name: str, factor: float, x: float, y: float, **props) -> int:
    i = d.instance(comp, parent, name=name, x=x, y=y, props=props or None)
    d.cmd("transform.scale", ids=[i], factor=factor)
    d.set(i, x=x, y=y)
    return i


def _cover(d: Doc) -> int:
    W, H, M = 1920, 1080, 128
    f = d.frame(None, name="Cover", x=0, y=0, w=W, h=H, fill="Color/ground", clip=True)
    _light(d, f)

    # ---- left: mark, name, the style in one line
    mb = d.instance("Mark box", f, name="Mark box", x=M, y=100, props={"Style": "Solid"})
    d.set(mb, width=88, height=88)
    d.text(f, "Attic Comfort", style="Cover/Title", color="Color/on", name="Title", x=M - 6, y=240)
    d.text(f, STYLE_LINE, style="Cover/Lede", color="Color/on-2", name="Style", x=M, y=404, width=720,
           textAutoResize="height")

    # ---- the dock, floating on the ground as it does on every root screen
    _scaled(d, "Dock", f, "Dock", 1.35, M - 16, 590, **{"Tab": "Home", "Mini player": "True"})

    # ---- meta: status, version, date, commit
    status = d.frame(f, name="Status", layout="h", gap=10, pad=[14, 22, 14, 18], cross="center",
                     fill="Color/accent", radius=999, sizing=("hug", "hug"), x=M, y=874)
    d.ellipse(status, name="Dot", w=10, h=10, fill="Color/on-accent")
    d.text(status, "Ready for development", style="Cover/Status", color="Color/on-accent", name="Status text")
    meta = d.frame(f, name="Meta", layout="h", gap=56, x=M, y=944, sizing=("hug", "hug"))
    for label, value in (("Version", VERSION), ("Last updated", UPDATED), ("Matches", f"ui-redesign at {COMMIT}")):
        col = d.frame(meta, name=f"Meta: {label}", layout="v", gap=4, sizing=("hug", "hug"))
        d.text(col, label, style="Cover/Label", color="Color/on-3", name="Label")
        d.text(col, value, style="Cover/Value", color="Color/on", name="Value")

    # ---- right: Now Playing washed with the cover's colour (Color mode Tinted wash, the Paper Lanterns seed)
    px, py, pw, ph = 1056, 56, 808, 968
    panel = d.frame(f, name="Now Playing, washed", x=px, y=py, w=pw, h=ph, fill="Color/ground", radius=72,
                    smoothing=0.6, clip=True)
    _light(d, panel, "Tinted wash")
    k = 1.5
    x0 = (pw - 411 * k) / 2
    y0 = (ph - (355 + 74 + 8 + 48 + 4 + 90) * k) / 2
    cv = _scaled(d, "Now playing cover", panel, "Cover", k, x0, y0, Paused="False")
    cover_image(d, d.child(cv, "Cover"), COVER[0])
    tt = _scaled(d, "Now playing title", panel, "Title", k, x0, y0 + 355 * k)
    d.cmd("instance.setProperties", ids=[tt], properties={"Title": "Lights Over the Water", "Artist": "Mira Okafor"})
    sk = _scaled(d, "Seek bar", panel, "Seek bar", k, (pw - 351 * k) / 2, y0 + (355 + 74 + 8) * k, Progress="Playing")
    d.cmd("instance.setProperties", ids=[sk], properties={"Elapsed": "1:18", "Total": "3:52"})
    _scaled(d, "Transport", panel, "Transport", k, (pw - 351 * k) / 2, y0 + (355 + 74 + 8 + 48 + 4) * k, Playing="True")
    return f


def _readme(d: Doc, x: float):
    c = d.frame(None, name="Read me", x=x, y=0, w=760, layout="v", gap=28, pad=56, fill="Canvas/card", radius=32,
                sizing=("fixed", "hug"))

    def heading(t):
        d.text(c, t, style="Doc/Heading", color="Canvas/label", name=f"Heading: {t}")

    def body(t, muted=False, style="Doc/Body", name=None):
        return d.text(c, t, style=style, color="Canvas/label-muted" if muted else "Canvas/label",
                      name=name or f"Note: {t[:40]}", width=648, textAutoResize="height")

    heading("Read me")
    body("The design file for Attic's Comfort style, the code in app/src/main/java/app/attic/player/ui/comfort/. "
         "Comfort and Expressive are separate files; the app switches between them in Settings.")
    heading("Pages")
    tbl = d.frame(c, name="Pages", layout="v", gap=12, sizing=("fill", "hug"))
    for page, what in PAGES:
        r = d.frame(tbl, name=f"Page: {page}", layout="h", gap=24, sizing=("fill", "hug"))
        d.text(r, page, style="Doc/Label", color="Canvas/label", name="Page", width=220, textAutoResize="height")
        t = d.text(r, what, style="Doc/Body", color="Canvas/label", name="What", textAutoResize="height")
        d.set(t, sizingH="fill")
    heading("Fonts")
    body("Install Onest, Google Sans Flex and Material Symbols Rounded before opening the file. The repository "
         "carries all three in app/src/main/res/font/. Screens and components are set in Onest, icons are "
         "Material Symbols glyphs, and the documentation is set in Google Sans Flex.")
    heading("The code wins")
    body("The app's Compose code is the source of truth. Where this file and the code disagree, the code is "
         "right and the file gets fixed. Each screen names its composable in a Source annotation; notes in "
         "the category \"Code differs from mock\" say where the code departs from the HTML mocks, and name the "
         "mock file they compare with.")
    heading("Design docs")
    body("design/README.md in the repository, github.com/srivathsanvenkateswaran/attic, explains how this "
         "file is built (design/build/) and lists what it covers. The HTML mocks Comfort started from are in "
         "docs/design/2026-11-21-ui-directions/oneui/, one file per area: 01-home.html to 10-system.html, with "
         "kit.css and KIT.md for the shared parts. Open them in a browser; they use real artists and songs, so "
         "take layout from them, not content.")
    return c


def _changelog(d: Doc):
    pid = d.page("Changelog")
    d.text(None, "Changelog", style="Doc/Page title", color="Canvas/label", name="Page title", x=0, y=0)
    d.text(None, "What changed in this file, newest version on top. The version on the Cover is always the top "
           "entry's.", style="Doc/Body", color="Canvas/label", name="Intro", x=0, y=140, width=1200,
           textAutoResize="height")

    # The entry block, a main component kept to the right of the entries.
    comp = d.component(None, name="Doc/Changelog entry", x=1160, y=340, w=960, layout="v", gap=20, pad=48,
                       fill="Canvas/card", radius=32, sizing=("fixed", "hug"),
                       description="One release in the changelog. Place an instance at the top of Entries and set "
                                   "Version, Date, Author and Changes.")
    head = d.frame(comp, name="Head", layout="h", gap=24, cross="baseline", sizing=("fill", "hug"))
    tv = d.text(head, "0.0.0", style="Doc/Section title", color="Canvas/label", name="Version")
    td = d.text(head, "YYYY-MM-DD", style="Doc/Heading", color="Canvas/label-muted", name="Date")
    ta = d.text(comp, AUTHOR, style="Doc/Label", color="Canvas/label-muted", name="Author")
    d.rect(comp, name="Rule", w=864, h=1, fill="Canvas/divider")
    tc = d.text(comp, "What changed, one line per change.", style="Doc/Body", color="Canvas/label", name="Changes",
                width=864, textAutoResize="height")
    d.set(tc, sizingH="fill")
    for prop, layer, default in (("Version", tv, "0.0.0"), ("Date", td, "YYYY-MM-DD"), ("Author", ta, AUTHOR),
                                 ("Changes", tc, "What changed, one line per change.")):
        name = d.prop(comp, prop, "text", default=default)
        d.bind_prop(layer, name, "characters")
    d.register("Doc/Changelog entry", comp)
    d.text(None, "Entry block (main component)", style="Doc/Label", color="Canvas/label-muted",
           name="Label: Entry block", x=1160, y=300)

    col = d.frame(None, name="Entries", x=0, y=340, w=960, layout="v", gap=40, sizing=("fixed", "hug"))
    for version, date, lines in CHANGES:
        i = d.instance(comp, col, name=f"Entry {version}")
        d.cmd("instance.setProperties", ids=[i], properties={
            "Version": version, "Date": date, "Author": AUTHOR,
            "Changes": "\n".join(lines)})
        d.set(i, sizingH="fill")
        # One paragraph per change, as a bulleted list.
        d.set(d.child(i, "Changes"), listStyle="bulleted", paragraphSpacing=10)

    g = d.get(col)
    howto = d.frame(None, name="How to add an entry", x=0, y=340 + g["height"] + 80, w=960, layout="v", gap=16,
                    pad=48, fill="Canvas/section", radius=32, sizing=("fixed", "hug"))
    d.text(howto, "Adding the next version", style="Doc/Heading", color="Canvas/label", name="Heading")
    for i, t in enumerate((
        "Pick the number. A fix to a drawing that already matched the code is a patch (1.0.1); new screens, "
        "components or states are a minor version (1.1.0); a change of style, structure or naming that breaks "
        "links and overrides is a major version (2.0.0).",
        "Duplicate the top entry in Entries (or place a new instance of Doc/Changelog entry there), keep it on "
        "top, and set its Version, Date and Changes. Author stays Attic maintainers.",
        "On the Cover, change Version and Last updated, and the commit in Matches if the file now follows a "
        "newer commit.",
        "In design/build/comfort/, bump VERSION in __init__.py and add the entry to CHANGES in cover.py, so "
        "the next generated file carries it too.",
    ), 1):
        d.text(howto, f"{i}. {t}", style="Doc/Body", color="Canvas/label", name=f"Step {i}", width=864,
               textAutoResize="height")
    return pid


def build(d: Doc, ctx: dict):
    _styles(d)
    d.page("Cover")
    _cover(d)
    _readme(d, 1920 + 120)
    _changelog(d)
