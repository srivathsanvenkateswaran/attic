"""Edit a saved design file with one module, instead of regenerating the file.

    cd design/build
    python3 -m figbuild.edit expressive tablet --in /tmp/attic-design/tablet/expressive-orig.figcraft \
        --out /tmp/attic-design/tablet/expressive.figcraft [--only home,library] \
        [--export /tmp/attic-dev/tablet-design/expressive] [--sheet /tmp/attic-dev/tablet-design/expressive-sheet.jpg]

Opens `--in` in a headless session (`Doc.open_existing`), sets what the style's `setup()` set while
the file was generated (the collections with Light and Dark modes, the screen fill, the icon ink),
rebuilds `ctx["components"]` from the file, runs `<style>.<module>.build(d, ctx)` and saves to
`--out`. The module must be idempotent: it deletes what it added before drawing it again.

`--only` is passed to the module as `ctx["only"]` (a set of area keys) so one area can be redrawn
while iterating; a module skips rebuilding its components when `--only` leaves out `components` and
the input file already has them. `--export DIR` writes every frame in `d.screens` as DIR/<key>-<light|dark>.png at
`--scale` (2.5 by default, the Pad 6's density), plus the layers the module lists in `ctx["exports"]`
({file stem: layer id}). `--sheet` writes a contact sheet of the exported frames.

Never point `--out` at the files in design/ while iterating: work on a copy and copy the result over
when it has been checked.
"""
from __future__ import annotations

import argparse
import importlib
import sys
import time
from pathlib import Path

from .kit import Doc


def style_setup(d: Doc, style: str):
    """What <style>.setup() sets on the Doc besides making pages."""
    if style == "expressive":
        d.mode_collections = ("Wallpaper", "Cover", "System", "Art", "Comfort preview", "Phone")
    elif style == "comfort":
        from comfort.foundations import MODE_COLLECTIONS
        d.mode_collections = MODE_COLLECTIONS
        d.screen_fill = "Color/ground"
        d.icon_color = "Color/on"
    else:
        raise SystemExit(f"unknown style {style!r}")


def component_registry(d: Doc) -> dict:
    """ctx["components"] as the generator kept it: {name: {id, variants: {variant name: id}, props: {}}}.
    Property names in a saved file are the plain names (Title, Subtitle), so `props` maps nothing."""
    reg: dict = {}
    listing = d.cmd("component.list")
    for c in listing:
        if c.get("type") == "COMPONENT_SET":
            reg[c["name"]] = {"id": c["id"], "variants": {}, "props": {}}
    for c in listing:
        if c.get("type") == "COMPONENT" and "=" in c.get("name", ""):
            owner = (c.get("folder") or [None])[-1]
            if owner in reg:
                reg[owner]["variants"][c["name"]] = c["id"]
        elif c.get("type") == "COMPONENT":
            reg.setdefault(c["name"], {"id": c["id"], "variants": {}, "props": {}})
    return reg


def open_doc(style: str, path: str | Path, log=None) -> tuple[Doc, dict]:
    pkg = importlib.import_module(style)
    d = Doc(pkg.TITLE, log=log).open_existing(path)
    style_setup(d, style)
    ctx: dict = {"style": style, "components": component_registry(d), "exports": {}}
    # Dimension tokens by name, which the generator's bind_dim() helpers read from module state.
    listing = d.cmd("variable.list")
    cols = {c["id"]: c["name"] for c in listing["collections"]}
    dims = {v["name"]: v["id"] for v in listing["variables"] if cols[v["collection"]] == "Dimension"}
    ctx["dimensions"] = dims
    try:
        f = importlib.import_module(f"{style}.foundations")
        if hasattr(f, "DIM"):
            f.DIM.update(dims)
    except Exception:
        pass
    return d, ctx


def export(d: Doc, ctx: dict, out: Path, scale: float = 2.5, only=None) -> list[Path]:
    out.mkdir(parents=True, exist_ok=True)
    written = []
    for key, s in d.screens.items():
        if only and s.get("area") not in only:
            continue
        for theme in ("light", "dark"):
            if s.get(theme):
                p = out / f"{key}-{theme}.png"
                d.export_png(s[theme], p, scale=scale)
                written.append(p)
    for stem, nid in (ctx.get("exports") or {}).items():
        p = out / "components" / f"{stem}.png"
        d.export_png(nid, p, scale=scale)
    return written


def contact_sheet(pngs: list[Path], path: Path, *, cell_h=300, per_row=6, pad=12):
    """A downscaled sheet of the exported frames, each scaled to `cell_h` tall, with its file stem under it."""
    from PIL import Image, ImageDraw
    thumbs = []
    for p in pngs:
        im = Image.open(p).convert("RGB")
        w = max(1, round(im.width * cell_h / im.height))
        thumbs.append((p.stem, im.resize((w, cell_h))))
    rows, row, x = [], [], 0
    max_w = max(sum(t.width + pad for _, t in thumbs[i:i + per_row]) for i in range(0, len(thumbs), per_row)) + pad
    for i in range(0, len(thumbs), per_row):
        rows.append(thumbs[i:i + per_row])
    sheet = Image.new("RGB", (max_w, len(rows) * (cell_h + 22 + pad) + pad), "#E4E4E7")
    draw = ImageDraw.Draw(sheet)
    y = pad
    for r in rows:
        x = pad
        for stem, t in r:
            sheet.paste(t, (x, y))
            draw.text((x, y + cell_h + 4), stem, fill="#3F3F46")
            x += t.width + pad
        y += cell_h + 22 + pad
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path, quality=85)
    return path


def run(style: str, module: str, src: Path, out: Path, *, only=None, export_dir=None, sheet=None, scale=2.5):
    t0 = time.time()
    d, ctx = open_doc(style, src)
    try:
        ctx["only"] = set(only) if only else None
        mod = importlib.import_module(f"{style}.{module}")
        mod.build(d, ctx)
        out.parent.mkdir(parents=True, exist_ok=True)
        d.save(out)
        print(f"saved {out} ({out.stat().st_size // 1024} KB) in {time.time() - t0:.0f}s", flush=True)
        if export_dir:
            pngs = export(d, ctx, Path(export_dir), scale)
            print(f"exported {len(pngs)} frames to {export_dir}", flush=True)
            if sheet:
                print("sheet", contact_sheet(pngs, Path(sheet)), flush=True)
        return ctx
    finally:
        d.close()


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("style")
    ap.add_argument("module")
    ap.add_argument("--in", dest="src", required=True, type=Path)
    ap.add_argument("--out", required=True, type=Path)
    ap.add_argument("--only")
    ap.add_argument("--export")
    ap.add_argument("--sheet")
    ap.add_argument("--scale", type=float, default=2.5)
    a = ap.parse_args(argv)
    design = Path(__file__).resolve().parents[2]
    if a.out.resolve().parent == design:
        raise SystemExit("write to a copy in /tmp/attic-design/ and copy the checked file over design/ by hand")
    run(a.style, a.module, a.src, a.out, only=a.only.split(",") if a.only else None, export_dir=a.export,
        sheet=a.sheet, scale=a.scale)


if __name__ == "__main__":
    sys.exit(main())
