"""What the two styles' tablet modules share: the frame sizes of docs/design/2026-11-25-tablet/SPEC.md,
the Tablet page, the Light-over-Dark sections, the annotations and the ready-for-development status every
frame carries now that the layouts are in the code, removal of what an earlier run added (so a module can run
twice), the retirement of the mood mixes' placeholder tablet frames on the Screens page, and the layer hygiene check.

Lengths are dp. A frame is one window: 1152 x 720 (Landscape, the Pad 6 sideways), 720 x 1152 (Portrait,
the Pad 6 upright, a medium width) or 1440 x 900 (Large landscape).
"""
from __future__ import annotations

from .kit import Doc, Area

PAGE = "Tablet"
STATUS_H, GESTURE_H = 24, 20
MARGIN = 24                      # screen margin on medium and expanded windows
SIZES = {
    "landscape": (1152, 720, "Landscape"),          # the Pad 6 sideways at its stock density: expanded width
    "portrait": (720, 1152, "Portrait"),            # the Pad 6 upright: medium width
    "large": (1440, 900, "Large landscape"),        # a smaller display size, or a bigger tablet
}
EXPORT_SCALE = 2.5               # the Pad 6's density
DEFAULT_NAMES = ("Frame", "Rectangle", "Group", "Ellipse", "Vector", "Text", "Line", "Polygon", "Star", "Section",
                 "Component", "Instance", "Image")


def adaptive_columns(available: float, min_cell: float, gap: float = 0) -> int:
    """ui/adaptive/AdaptiveLayout.kt adaptiveColumns: how many cells at least `min_cell` wide fit."""
    return max(1, int((available + gap) // (min_cell + gap)))


def cell_width(available: float, columns: int, gap: float) -> float:
    return round((available - gap * (columns - 1)) / columns, 2)


def reset_page(d: Doc, after: str = "Screens") -> int:
    """Delete the Tablet page if a previous run made it, add it again and put it after `after`."""
    if PAGE in d.pages:
        d.cmd("page.delete", id=d.pages.pop(PAGE))
    for k in [k for k in Area._next_y if k[1] == PAGE]:
        del Area._next_y[k]
    pid = d.page(PAGE)
    order = [p["name"] for p in d.cmd("page.list")]
    d.cmd("page.move", id=pid, index=order.index(after) + 1)
    return pid


def remove_components(d: Doc, ctx: dict, added: dict[str, list[tuple[str, str]]]):
    """Delete components an earlier run added, with the spec card around each, so they can be made again.
    `added` is {page: [(component name, card title)]}; a card is the page's top-level frame `Card · <title>`."""
    for page, items in added.items():
        tops = {n.get("name"): n["id"] for n in d.get(d.pages[page], depth=1).get("children", [])}
        for name, title in items:
            d.comp.pop(name, None)
            ctx["components"].pop(name, None)
            d.props.pop(name, None)
            card = tops.get(f"Card · {title}")
            if card is not None:
                d.cmd("edit.delete", ids=[card])


PLACEHOLDER_MARK = "· In progress ·"
PLACEHOLDER_NOTE = ("The tablet frames at the end wait for the shell on feature/tablet-layouts.",
                    "The tablet frames are on the Tablet page, in its Mood mixes section.")


def retire_placeholders(d: Doc, page: str = "Screens", section: str = "Moods") -> int:
    """Delete the tablet frames the moods modules drew before the tablet components existed (their names hold
    "· In progress ·") from the Moods section of the Screens page: the Tablet page draws those screens now. The
    section shrinks back to its phone rows and its note says where the tablet frames are. Whatever sits under the
    section on the page (the Transitions section, since 1.4.0) moves up by what the section shrank, so the page keeps
    its 200 between sections; nothing inside those layers changes. Returns how many frames went: 0 on a file that
    has none, so a second run changes nothing."""
    pid = d.page(page)
    gone: list[int] = []
    tops = d.get(pid, depth=2).get("children", [])
    for sec in tops:
        if sec.get("type") != "SECTION" or sec.get("name") != section:
            continue
        kids = sec.get("children", [])
        gone = [k["id"] for k in kids if k.get("type") == "FRAME" and PLACEHOLDER_MARK in k.get("name", "")]
        if gone:
            d.cmd("edit.delete", ids=gone)
            keep = [k for k in kids if k.get("type") == "FRAME" and k["id"] not in gone]
            d.set(sec["id"], width=max(k["x"] + k["width"] for k in keep) + Area.PAD,
                  height=max(k["y"] + k["height"] for k in keep) + Area.PAD)
            shrank = sec["height"] - d.get(sec["id"])["height"]
            for below in tops:
                if shrank > 0 and below["id"] != sec["id"] and below.get("y", 0) >= sec["y"] + sec["height"] - 1:
                    d.set(below["id"], y=below["y"] - shrank)
        for k in kids:
            if k.get("type") == "TEXT" and k.get("name") == "Note":
                chars = d.get(k["id"]).get("characters", "")
                if PLACEHOLDER_NOTE[0] in chars:
                    d.set(k["id"], characters=chars.replace(*PLACEHOLDER_NOTE))
    return len(gone)


class Frames:
    """One section of the Tablet page: frames left to right, Light row above the Dark copies."""

    def __init__(self, d: Doc, ctx: dict, name: str, key: str, note: str | None = None):
        self.d, self.ctx, self.key = d, ctx, key
        self.area = d.area(name, page=PAGE, note=note)

    def frame(self, key: str, screen: str, size: str, source: str, *, fill=None, h=None, suffix="") -> tuple[int, int, int]:
        """A frame named `<screen> / <Landscape|Portrait|Large landscape>`. Returns (id, width, height). `h` and
        `suffix` are for a frame that shows a whole scrolled page (` (full length)`)."""
        w, h0, label = SIZES[size]
        h = h or h0
        kw = {"fill": fill} if fill is not None else {}
        fid = self.area.screen(key, name=f"{screen} / {label}{suffix}", source=source, w=w, h=h, status="ready", **kw)
        s = self.d.screens[key]
        s["area"], s["size"] = self.key, size
        return fid, w, h

    def annotate(self, key: str, *notes: str, shell: str | None = None):
        """Source on a finished Light frame (the Dark copy takes it with it), plus Content notes. `shell` is a second
        Source line for a frame that shows the wide shell around its destination."""
        d = self.d
        s = d.screens[key]
        d.annotate(s["light"], f"Compose: {s['source']}", "Source")
        if shell:
            d.annotate(s["light"], f"Shell: {shell}", "Source")
        for n in notes:
            d.annotate(s["light"], n, "Content")

    def close(self) -> int:
        """Dark copies under the Light row, then every frame of the section marked ready for development: the
        layouts are in the code (feature/tablet-layouts)."""
        sec = self.area.close()
        d = self.d
        d.ready_for_dev([d.screens[k][t] for k in self.area.keys for t in ("light", "dark") if d.screens[k].get(t)])
        return sec


def hygiene(d: Doc, roots: dict[str, int]) -> dict:
    """Default-named layers under each root ({label: layer id}); every list should be empty."""
    report = {}
    for label, root in roots.items():
        bad = []
        for word in DEFAULT_NAMES:
            for i in d.find(root, name=word, limit=2000):
                nm = d.get(i).get("name", "")
                if nm == word or (nm.startswith(word + " ") and nm[len(word) + 1:].isdigit()):
                    bad.append((i, nm))
        report[label] = bad
    return report
