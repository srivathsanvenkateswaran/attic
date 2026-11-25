#!/usr/bin/env bash
# Renders the screens the launch images use on the JVM and frames them. No phone or emulator.
#
#   scripts/store_shots/launch_shots.sh --out <folder>
#
# The renders go under /tmp/attic-dev/launch (LAUNCH_SHOTS_DIR): every store shot, the design references
# for the radio queue, the Now Playing menu, the Smart mark and the equaliser, and the 10-inch tablet's
# Library and Now Playing. frame_launch.py then writes product-hunt/, linkedin/, social/ and press/ into
# <folder>; its arguments are passed through.
# Needs JDK 17 (JAVA_HOME, default /opt/homebrew/opt/openjdk@17) and Python 3 with Pillow.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
renders="${LAUNCH_SHOTS_DIR:-/tmp/attic-dev/launch}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
# A fresh worktree has no local.properties; fall back to the usual macOS SDK location.
if [[ ! -f "$root/local.properties" && -z "${ANDROID_HOME:-}" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

render() {
  local out="$1"
  shift
  "$root/gradlew" -p "$root" testDebugUnitTest -PstoreShots -PstoreShotsOut="$out" "$@"
}

rm -rf "$renders"
render "$renders/store-shots/raw" --tests 'app.attic.player.storeshots.*'

refs=()
for key in queue_radio sheet_track_menu_now_playing now_playing_transition_mark sheet_equaliser now_playing_equaliser; do
  refs+=(--tests "app.attic.player.designrefs.DesignRefs.*[*-$key]")
done
render "$renders/design-refs" "${refs[@]}"

tablets=()
for key in library_albums now_playing; do
  tablets+=(--tests "app.attic.player.designrefs.TabletRefs.*[TenInch-*-$key]")
done
render "$renders/tablet-renders" "${tablets[@]}"

python3 "$root/scripts/store_shots/frame_launch.py" --renders "$renders" "$@"
