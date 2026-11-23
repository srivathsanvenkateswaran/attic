"""Render pages of a built file to PNG with the app's fonts loaded.

    python3 -m figbuild.pages FILE.figcraft OUT_DIR [--pages "Type,Components / Actions"] [--scale 0.5]
"""
import argparse
from pathlib import Path
from .kit import Doc


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("file"); ap.add_argument("out")
    ap.add_argument("--pages"); ap.add_argument("--scale", type=float, default=0.5)
    ap.add_argument("--extra-font", action="append", default=[])
    a = ap.parse_args(argv)
    d = Doc("render", extra_fonts=a.extra_font, read_roots=[Path(a.file).parent])
    try:
        d.tool("open_file", path=str(Path(a.file).resolve()))
        d.cmd("text.refit")
        want = set(a.pages.split(",")) if a.pages else None
        out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
        for p in d.cmd("page.list"):
            if p.get("divider") or (want and p["name"] not in want):
                continue
            d.cmd("page.select", id=p["id"])
            tops = [n["id"] for n in d.tool("get_node", id=p["id"], depth=1).get("children", [])]
            if not tops:
                continue
            path = out / (p["name"].replace("/", "-").replace(" ", "_") + ".png")
            d.tool("export", ids=tops, format="png", scale=a.scale, path=str(path))
            print(path)
    finally:
        d.close()


if __name__ == "__main__":
    main()
