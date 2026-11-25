#!/usr/bin/env python3
"""Puts each tablet render beside the design frame it implements, as figbuild/compare.py does for phones.

    python3 scripts/store_shots/tablet_compare.py [--renders /tmp/attic-dev/tablet-renders]
        [--designs /tmp/attic-dev/tablet-design] [--out /tmp/attic-dev/tablet-compare] [--style comfort]

Design frames are exported by `python3 -m figbuild.edit <style> tablet --export` as
<designs>/<style>/<frame>-<theme>.png at 2.5 px per dp; renders come from tablet_refs.sh as
<renders>/tablet-<size>-<style>-<theme>-<key>.png. Both are scaled to 1 px per dp and written side by
side (design left, render right) to <out>/<style>-<theme>-<frame>.jpg, with a difference score
(mean absolute difference per channel, 0 to 255) in <out>/scores.txt.
"""
import argparse
import os
import sys

from PIL import Image, ImageChops, ImageDraw, ImageStat

# Design frame -> (render size, render key). Sizes in dp, for scaling both images to 1 px per dp.
PAIRS = {
    "home_landscape": ("landscape", "home"),
    "home_portrait": ("portrait", "home"),
    "home_large": ("large", "home"),
    "library_landscape": ("landscape", "library_albums"),
    "library_portrait": ("portrait", "library_list"),
    "library_large": ("large", "library_albums"),
    "search_landscape": ("landscape", "search_results"),
    "search_portrait": ("portrait", "search_results"),
    "album_landscape": ("landscape", "album"),
    "album_portrait": ("portrait", "album"),
    "album_large": ("large", "album"),
    "artist_landscape": ("landscape", "artist"),
    "playlist_landscape": ("landscape", "playlist"),
    "now_playing_up_next_landscape": ("landscape", "now_playing"),
    "now_playing_up_next_large": ("large", "now_playing"),
    "now_playing_lyrics_landscape": ("landscape", "now_playing_lyrics"),
    "now_playing_portrait": ("portrait", "now_playing"),
    "lyrics_landscape": ("landscape", "lyrics_synced"),
    "settings_landscape": ("landscape", "settings"),
    "settings_portrait": ("portrait", "settings"),
    "sheet_track_menu_landscape": ("landscape", "sheet_track_menu_album"),
    "dialog_new_playlist_landscape": ("landscape", "dialog_new_playlist_library"),
    "first_run_look_landscape": ("landscape", "onboarding_look"),
    "first_run_look_portrait": ("portrait", "onboarding_look"),
}
DP = {"landscape": (1152, 720), "portrait": (720, 1152), "large": (1440, 900)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--renders", default="/tmp/attic-dev/tablet-renders")
    ap.add_argument("--designs", default="/tmp/attic-dev/tablet-design")
    ap.add_argument("--out", default="/tmp/attic-dev/tablet-compare")
    ap.add_argument("--style", choices=["expressive", "comfort"])
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    lines, missing = [], []
    for style in [a.style] if a.style else ["expressive", "comfort"]:
        for theme in ("light", "dark"):
            for frame, (size, key) in PAIRS.items():
                design = os.path.join(a.designs, style, f"{frame}-{theme}.png")
                render = os.path.join(a.renders, f"tablet-{size}-{style}-{theme}-{key}.png")
                if not (os.path.exists(design) and os.path.exists(render)):
                    missing.append(f"{style}-{theme}-{frame}")
                    continue
                w, h = DP[size]
                left = Image.open(design).convert("RGB").resize((w, h), Image.LANCZOS)
                right = Image.open(render).convert("RGB").resize((w, h), Image.LANCZOS)
                score = sum(ImageStat.Stat(ImageChops.difference(left, right)).mean) / 3
                sheet = Image.new("RGB", (w * 2 + 16, h + 28), "white")
                sheet.paste(left, (0, 28))
                sheet.paste(right, (w + 16, 28))
                draw = ImageDraw.Draw(sheet)
                draw.text((8, 8), f"design: {frame} ({theme})", fill="black")
                draw.text((w + 24, 8), f"render: {key} at {size}   difference {score:.1f}", fill="black")
                sheet.save(os.path.join(a.out, f"{style}-{theme}-{frame}.jpg"), quality=82)
                lines.append(f"{score:6.1f}  {style}-{theme}-{frame}")
    with open(os.path.join(a.out, "scores.txt"), "w") as f:
        f.write("\n".join(sorted(lines, reverse=True)) + "\n")
    print("\n".join(sorted(lines, reverse=True)))
    if missing:
        print(f"missing a design or a render: {', '.join(missing)}", file=sys.stderr)


if __name__ == "__main__":
    main()
