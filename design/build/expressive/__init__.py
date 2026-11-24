"""Attic Expressive: the design file for the Expressive style (ui/expressive/)."""
from figbuild.kit import Doc

TITLE = "Attic Expressive"
VERSION = "1.0.0"
CODE_PACKAGE = "app.attic.player.ui.expressive"

# Build order. Each module owns the pages its docstring names.
MODULES = ['foundations', 'components_core', 'components_overlays', 'components_states', 'screens_browse', 'screens_playback', 'screens_settings', 'equaliser', 'system_surfaces', 'cover', 'flows', 'handoff']
# 'equaliser' (1.1.0) adds to the saved file rather than to a fresh build: run it on its own,
# `python3 -m expressive.equaliser "../Attic Expressive.figcraft" OUT`. In a build from scratch it is skipped.
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
    # Every collection with Light and Dark modes (all made in foundations.py): Dark copies and Dark previews
    # switch each of them.
    d.mode_collections = ("Wallpaper", "Cover", "System", "Art", "Comfort preview", "Phone")
