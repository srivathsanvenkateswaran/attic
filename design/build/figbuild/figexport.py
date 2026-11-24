"""Export a .figcraft file as .fig for Figma users.

    python3 -m figbuild.figexport "design/Attic Comfort.figcraft" "design/Attic Comfort.fig"

FigCraft's .fig writer drops layers placed into an instance's slot (Figma's file format has no
place for them), so a sheet, menu or grouped card would lose its rows. Before exporting, this
detaches every instance of a component that has a slot property (or, when that instance sits
inside another instance, the outermost one), in an unsaved copy: the slot's rows become plain
children of a frame and stay instances of their own components. The .figcraft
file keeps its slots. Everything else is exported as it is, and FigCraft's own warnings are
printed and returned.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from .kit import Doc


def slot_components(d: Doc) -> dict[int, str]:
    """{component or variant id: name} for every component whose properties include a slot."""
    out = {}
    for c in d.cmd("component.list"):
        if c["type"] not in ("COMPONENT", "COMPONENT_SET"):
            continue
        try:
            props = d.cmd("component.properties", id=c["id"]).get("properties", [])
        except Exception:
            continue
        if any(p.get("type") == "slot" for p in props):
            out[c["id"]] = c["name"]
            if c["type"] == "COMPONENT_SET":
                for v in d.get(c["id"], depth=1).get("children", []):
                    out[v["id"]] = c["name"]
    return out


FLATTEN_PAGES = ("Cover", "Screens", "Flow overview", "System surfaces")


def flatten(d: Doc, page_names=FLATTEN_PAGES) -> int:
    """Detach every instance on these pages, outermost first, until none is left."""
    pages = [p["id"] for p in d.cmd("page.list") if p["name"] in page_names]
    total = 0
    for _ in range(40):
        outer = []
        for pid in pages:
            stack = [d.get(pid, depth=200, fields=["type"])]
            while stack:
                n = stack.pop()
                if n.get("type") == "INSTANCE":
                    outer.append(n["id"])
                    continue
                stack.extend(n.get("children") or [])
        if not outer:
            break
        for i in range(0, len(outer), 500):
            d.cmd("component.detach", ids=outer[i:i + 500])
        total += len(outer)
    return total


def export(src: Path, dst: Path, read_roots=(), write_roots=(), flat: bool = False) -> dict:
    d = Doc("fig export", read_roots=[src.parent, *read_roots], write_roots=[dst.parent, *write_roots])
    try:
        d.tool("open_file", path=str(src.resolve()))
        d.cmd("text.refit")
        slots = slot_components(d)
        pages = [p["id"] for p in d.cmd("page.list") if not p.get("divider")]
        detached = 0
        for _ in range(10):
            found = set()
            for pid in pages:
                # One read of the page tree; a name search misses instances of variants.
                # A nested instance can't be detached alone, so a slot holder inside another
                # instance detaches that outermost instance instead.
                tree = d.get(pid, depth=200, fields=["type", "mainComponent"])
                stack = [(tree, None)]
                while stack:
                    n, outer = stack.pop()
                    if n.get("type") == "INSTANCE":
                        if n.get("mainComponent") in slots:
                            found.add(outer or n["id"])
                        outer = outer or n["id"]
                    stack.extend((c, outer) for c in n.get("children") or [])
            if not found:
                break
            d.cmd("component.detach", ids=sorted(found))
            detached += len(found)
        flattened = flatten(d) if flat else 0
        r = d.tool("export", format="fig", path=str(dst.resolve()))
        r["detached"] = detached
        r["flattened"] = flattened
        return r
    finally:
        d.close()


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("src", type=Path)
    ap.add_argument("dst", type=Path)
    ap.add_argument("--flat", action="store_true",
                    help="also detach every instance on the Cover, Screens, Flow overview and System surfaces pages")
    a = ap.parse_args(argv)
    r = export(a.src, a.dst, flat=a.flat)
    print(f"wrote {a.dst} ({a.dst.stat().st_size // 1024} KB); detached {r['detached']} slot instances, "
          f"flattened {r['flattened']} screen instances")
    for w in r.get("warnings", []):
        print(f"warning: {w}")


if __name__ == "__main__":
    sys.exit(main())
