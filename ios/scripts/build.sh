#!/bin/bash
# Builds the app and runs the unit tests, the gate for every merge into ios/app.
# Usage: ios/scripts/build.sh [build|test]   (default: test, which also builds)
# At most three xcodebuilds run at once on this Mac: the script waits for a free slot, and each slot
# has its own simulator so parallel test runs don't fight over one device.
set -uo pipefail
cd "$(dirname "$0")/.."
action="${1:-test}"
sims=("iPhone 17 Pro" "iPhone 17" "iPhone 17 Pro Max")
slot=""
# The orchestrator's merge gate has its own lane and simulator, so it doesn't queue behind branch builds.
if [ "${ATTIC_GATE:-}" = 1 ]; then slot=gate; sims[3]="iPhone Air"; mkdir -p /tmp/attic-ios-xcb-slot-gate; fi
while [ -z "$slot" ]; do
  for i in 0 1 2; do
    if mkdir "/tmp/attic-ios-xcb-slot-$i" 2>/dev/null; then slot=$i; break; fi
  done
  [ -z "$slot" ] && sleep 5
done
# Disk is tight: a used simulator keeps 1-2 GB of data, so the test app is removed afterwards.
trap 'xcrun simctl uninstall "$sim" app.attic.ios >/dev/null 2>&1; rmdir "/tmp/attic-ios-xcb-slot-$slot"' EXIT
if [ "$slot" = gate ]; then sim="iPhone Air"; else sim="${sims[$slot]}"; fi
echo "Using slot $slot, simulator: $sim"
xcodegen generate --quiet || exit 1
if [ "$action" = "build" ]; then steps="build"; else steps="build test"; fi
log=".build/last-build.log"
mkdir -p .build
xcodebuild -project Attic.xcodeproj -scheme Attic \
  -destination "platform=iOS Simulator,name=$sim" \
  -derivedDataPath .build $steps >"$log" 2>&1
status=$?
grep -E "error:|\*\* (BUILD|TEST) |Executed [0-9]+ tests|failed \(" "$log" | sort -u | head -60
echo "Full log: ios/$log (exit $status)"
exit $status
