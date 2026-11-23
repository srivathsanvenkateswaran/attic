#!/usr/bin/env bash
# Renders every store screenshot on the JVM and frames them. No phone or emulator.
#
#   scripts/store_shots/run.sh            raw, Play, Product Hunt and README images under /tmp/attic-dev/store-shots
#   scripts/store_shots/run.sh --readme   also replaces the images in docs/screenshots
#
# Needs JDK 17 (JAVA_HOME, default /opt/homebrew/opt/openjdk@17) and Python 3 with Pillow.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
out="${STORE_SHOTS_DIR:-/tmp/attic-dev/store-shots}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"

rm -rf "$out/raw"
"$root/gradlew" -p "$root" testDebugUnitTest -PstoreShots -PstoreShotsOut="$out/raw" --tests 'app.attic.player.storeshots.*'

readme=()
if [[ "${1:-}" == "--readme" ]]; then
  rm -f "$root"/docs/screenshots/*.jpg
  readme=(--readme "$root/docs/screenshots")
fi
python3 "$root/scripts/store_shots/frame.py" --shots "$out" ${readme[@]+"${readme[@]}"}
