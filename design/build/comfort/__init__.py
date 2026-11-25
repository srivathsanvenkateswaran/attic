"""Attic Comfort: the design file for the Comfort style (ui/comfort/)."""
from figbuild.kit import Doc

TITLE = "Attic Comfort"
VERSION = "1.1.0"
CODE_PACKAGE = "app.attic.player.ui.comfort"

# Build order. Each module owns the pages its docstring names. `equaliser` (1.1.0) adds to a finished file: it reads
# what it needs back out of the document, so it also runs on its own against the saved .figcraft
# (python3 -m comfort.equaliser SRC DST), which is how the equaliser got into the file.
MODULES = ['foundations', 'components_core', 'components_overlays', 'components_states', 'screens_browse', 'screens_playback', 'screens_settings', 'system_surfaces', 'cover', 'flows', 'handoff', 'equaliser']
# Built before any module passed to --only, since screens are made of these.
FOUNDATION_MODULES = ["foundations", "components_core", "components_overlays", "components_states"]

PAGES = [
    "Cover", "Changelog",
    "-Foundations", "Colour", "Type", "Spacing, radii and shapes", "Elevation and motion", "Icons",
    "-Components", "Components / Actions", "Components / Lists and cards", "Components / Navigation",
    "Components / Playback", "Components / Overlays", "Components / States", "Components / Recap",
    "Components / System",
    "-Screens", "Screens", "Flow overview", "System surfaces",
]


def setup(d: Doc, ctx: dict):
    """Canvas chrome and every page, in order, so modules fill pages rather than create them."""
    d.canvas_basics()
    for name in PAGES:
        if name.startswith("-"):
            d.divider(name)
        else:
            d.page(name)
    d.page("Cover")
    # Every collection with Light and Dark modes (comfort/foundations.py MODE_COLLECTIONS). Color has Base, Tinted,
    # Tinted wash. "Expressive preview" is the Expressive look card's palette (first run, Settings' Style group).
    from .foundations import MODE_COLLECTIONS
    d.mode_collections = MODE_COLLECTIONS
    d.screen_fill = "Color/ground"
    d.icon_color = "Color/on"
