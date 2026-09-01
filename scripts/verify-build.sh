#!/usr/bin/env bash
set -euo pipefail

if [[ -x ./gradlew ]]; then
  GRADLE_CMD=(./gradlew)
elif [[ -n "${GRADLE_CMD:-}" ]]; then
  GRADLE_CMD=("$GRADLE_CMD")
else
  GRADLE_CMD=(gradle)
fi

echo "== Basket Manager build verification =="
"${GRADLE_CMD[@]}" --version

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb was not found. Install Android platform-tools before running this verification."
  exit 1
fi

if ! adb devices | awk 'NR > 1 && $2 == "device" { found=1 } END { exit(found ? 0 : 1) }'; then
  echo "ERROR: connectedDebugAndroidTest requires at least one booted emulator or connected Android device visible to adb."
  echo "Start/connect a device first, then rerun scripts/verify-build.sh."
  exit 1
fi

"${GRADLE_CMD[@]}" --no-daemon test --stacktrace
"${GRADLE_CMD[@]}" --no-daemon lintDebug --stacktrace
"${GRADLE_CMD[@]}" --no-daemon assembleDebug --stacktrace
"${GRADLE_CMD[@]}" --no-daemon assembleRelease --stacktrace
"${GRADLE_CMD[@]}" --no-daemon bundleRelease --stacktrace
"${GRADLE_CMD[@]}" --no-daemon connectedDebugAndroidTest --stacktrace

echo "Build verification completed successfully."
