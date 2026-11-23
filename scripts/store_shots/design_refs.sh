#!/usr/bin/env bash
# Renders the design references on the JVM: demo covers, brand marks, colour data and every screen and
# state of both styles in light and dark. No phone or emulator.
#
#   scripts/store_shots/design_refs.sh                 everything, into /tmp/attic-dev/design-refs
#   scripts/store_shots/design_refs.sh '*[*-album]'  only the screens whose test name matches (a Gradle --tests pattern)
#
# Output: covers/, brand/, colors/ and <style>-<theme>-<key>.png. The store screenshots (run.sh) are
# separate and untouched. Needs JDK 17 (JAVA_HOME, default /opt/homebrew/opt/openjdk@17) and Python 3 with Pillow.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
out="${DESIGN_REFS_DIR:-/tmp/attic-dev/design-refs}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
# A fresh worktree has no local.properties; fall back to the usual macOS SDK location.
if [[ ! -f "$root/local.properties" && -z "${ANDROID_HOME:-}" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

if [[ $# -gt 0 ]]; then
  tests=(--tests "app.attic.player.designrefs.DesignRefs.$1")
else
  mkdir -p "$out"
  find "$out" -maxdepth 1 -name '*.png' -delete
  rm -rf "$out/covers" "$out/brand" "$out/colors"
  tests=(--tests 'app.attic.player.designrefs.*')
fi

# The design refs live beside the store shots in src/storeShots, so they share its switch and its
# output property; storeShotsOut points the shared ShotsDir at this folder.
# --continue renders every key it can; a failed key still fails the script after post-processing.
status=0
"$root/gradlew" -p "$root" testDebugUnitTest --continue -PstoreShots -PstoreShotsOut="$out" "${tests[@]}" || status=$?

python3 "$root/scripts/store_shots/design_refs_post.py" "$out" "$root"
exit "$status"
