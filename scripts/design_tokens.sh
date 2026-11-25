#!/usr/bin/env bash
# Rewrites docs/design-system/tokens.json from the Kotlin token code, then runs the check on tokens.md.
# DesignTokensTest fails in the everyday test run when either file disagrees with the code.
#
# Needs JDK 17 (JAVA_HOME, default /opt/homebrew/opt/openjdk@17).
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"

"$root/gradlew" -p "$root" :app:testDebugUnitTest -PwriteDesignTokens --tests 'app.attic.player.ui.DesignTokensTest'
