#!/bin/bash
# Builds the app and runs the unit tests, the gate for every merge into ios/app.
# Usage: ios/scripts/build.sh [build|test]   (default: test, which also builds)
# At most three xcodebuilds run at once on this Mac: the script waits for a free slot. Only one
# simulator is used ("iPhone Air"): builds run in parallel, test runs take turns on it.
set -uo pipefail
cd "$(dirname "$0")/.."
action="${1:-test}"
sims=("iPhone Air" "iPhone Air" "iPhone Air")
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
log=".build/last-build.log"
mkdir -p .build
xcodebuild -project Attic.xcodeproj -scheme Attic \
  -destination "platform=iOS Simulator,name=$sim" \
  -derivedDataPath .build build-for-testing >"$log" 2>&1
status=$?
if [ $status = 0 ] && [ "$action" != build ]; then
  # One simulator, so one test run at a time.
  until mkdir /tmp/attic-ios-test-lock 2>/dev/null; do sleep 3; done
  xcodebuild -project Attic.xcodeproj -scheme Attic \
    -destination "platform=iOS Simulator,name=$sim" \
    -derivedDataPath .build test-without-building >>"$log" 2>&1
  status=$?
  rmdir /tmp/attic-ios-test-lock
fi
grep -E "error:|\*\* (BUILD|TEST) |Executed [0-9]+ tests|failed \(" "$log" | sort -u | head -60
echo "Full log: ios/$log (exit $status)"
exit $status
