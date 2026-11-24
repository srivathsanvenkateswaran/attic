"""Side-by-side and difference images: a design export against a reference render.

    python3 -m figbuild.compare REFERENCE.png DESIGN.png OUT.png [--label TEXT]
    python3 -m figbuild.compare --pairs pairs.tsv --out DIR      (one "ref<TAB>design<TAB>name" per line)

The reference is a 1080 x 2400 JVM render (or a phone screenshot); the design is a FigCraft export
of a 411 x 914 frame at any scale. Both are scaled to 540 px wide. The output shows reference,
design, a 50 % overlay and a difference heat map, and prints the mean absolute difference
(0 = identical, 255 = opposite) so a fix can be checked by number as well as by eye.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageOps

W = 540


def _fit(img: Image.Image) -> Image.Image:
    img = img.convert("RGB")
    h = round(img.height * W / img.width)
    return img.resize((W, h), Image.LANCZOS)


def compare(ref_path, design_path, out_path, label: str | None = None) -> float:
    ref = _fit(Image.open(ref_path))
    des = _fit(Image.open(design_path))
    h = max(ref.height, des.height)
    ref = ImageOps.pad(ref, (W, h), color="white", centering=(0, 0))
    des = ImageOps.pad(des, (W, h), color="white", centering=(0, 0))
    overlay = Image.blend(ref, des, 0.5)
    diff = ImageChops.difference(ref, des)
    gray = ImageOps.grayscale(diff)
    score = sum(i * n for i, n in enumerate(gray.histogram())) / (gray.width * gray.height)
    heat = ImageOps.colorize(ImageOps.autocontrast(gray), black="black", white="#ff3b30")
    pad, top = 16, 44
    sheet = Image.new("RGB", (W * 4 + pad * 5, h + top + pad), "#f4f4f5")
    d = ImageDraw.Draw(sheet)
    for i, (img, cap) in enumerate([(ref, "reference"), (des, "design"), (overlay, "overlay 50%"),
                                    (heat, f"difference  mean {score:.1f}")]):
        x = pad + i * (W + pad)
        sheet.paste(img, (x, top))
        d.text((x, 14), cap, fill="#18181b")
    if label:
        d.text((pad, 2), label, fill="#52525b")
    Path(out_path).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)
    return score


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("ref", nargs="?")
    ap.add_argument("design", nargs="?")
    ap.add_argument("out", nargs="?")
    ap.add_argument("--label")
    ap.add_argument("--pairs")
    ap.add_argument("--out-dir")
    a = ap.parse_args(argv)
    if a.pairs:
        for line in Path(a.pairs).read_text().splitlines():
            if not line.strip():
                continue
            ref, des, name = line.split("\t")
            s = compare(ref, des, Path(a.out_dir) / f"{name}.png", name)
            print(f"{s:6.1f}  {name}")
    else:
        print(f"{compare(a.ref, a.design, a.out, a.label):.1f}")


if __name__ == "__main__":
    sys.exit(main())
