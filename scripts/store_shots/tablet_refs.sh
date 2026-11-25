#!/usr/bin/env bash
# Renders the tablet layouts on the JVM, as the design files' Tablet pages draw them: both styles,
# light and dark, on the Xiaomi Pad 6's 2880x1800 px at 1152x720 dp landscape and 720x1152 dp portrait
# (its stock 400 dpi), and 1440x900 dp landscape (a smaller display size), and the other window sizes in
# TabletRefs.kt; TenInch and SevenInch are the Play listing's tablet screenshots, which frame_tablets.py
# frames. No phone or emulator. Keys are listed in TabletRefs.kt.
#
#   scripts/store_shots/tablet_refs.sh                              everything, into /tmp/attic-dev/tablet-renders
#   scripts/store_shots/tablet_refs.sh '*[Landscape-Comfort-*]'     only the tests whose name matches
#
# Output: tablet-<size>-<style>-<theme>-<key>.png, and sheet-<size>-<style>-<theme>.jpg contact sheets.
# Then scripts/store_shots/tablet_compare.py puts each render beside its design frame.
# Needs JDK 17 (JAVA_HOME, default /opt/homebrew/opt/openjdk@17) and Python 3 with Pillow.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
out="${TABLET_REFS_DIR:-/tmp/attic-dev/tablet-renders}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
if [[ ! -f "$root/local.properties" && -z "${ANDROID_HOME:-}" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

mkdir -p "$out"
if [[ $# -gt 0 ]]; then
  pattern="app.attic.player.designrefs.TabletRefs.$1"
else
  find "$out" -maxdepth 1 \( -name 'tablet-*.png' -o -name 'sheet-*.jpg' \) -delete
  pattern='app.attic.player.designrefs.TabletRefs'
fi

status=0
"$root/gradlew" -p "$root" testDebugUnitTest --continue -PstoreShots -PstoreShotsOut="$out" --tests "$pattern" || status=$?

python3 - "$out" <<'PY'
import sys, glob, os, collections
from PIL import Image
out = sys.argv[1]
groups = collections.defaultdict(list)
for f in sorted(glob.glob(os.path.join(out, "tablet-*.png"))):
    # Sizes such as ten-inch hold a hyphen, so the style is found by name.
    parts = os.path.basename(f).split("-")
    at = next(i for i, p in enumerate(parts) if p in ("expressive", "comfort"))
    size, style, theme = "-".join(parts[1:at]), parts[at], parts[at + 1]
    groups[(size, style, theme)].append(f)
for (size, style, theme), files in groups.items():
    thumbs = []
    for f in files:
        im = Image.open(f).convert("RGB")
        im.thumbnail((720, 720))
        thumbs.append(im)
    cols = 3
    w = max(t.width for t in thumbs); h = max(t.height for t in thumbs)
    rows = (len(thumbs) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (w + 12), rows * (h + 12)), "white")
    for i, t in enumerate(thumbs):
        sheet.paste(t, ((i % cols) * (w + 12), (i // cols) * (h + 12)))
    sheet.save(os.path.join(out, f"sheet-{size}-{style}-{theme}.jpg"), quality=80)
PY
exit "$status"
