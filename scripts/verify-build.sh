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
"${GRADLE_CMD[@]}" --no-daemon test --stacktrace
"${GRADLE_CMD[@]}" --no-daemon lintDebug --stacktrace
"${GRADLE_CMD[@]}" --no-daemon assembleDebug --stacktrace
"${GRADLE_CMD[@]}" --no-daemon assembleRelease --stacktrace
"${GRADLE_CMD[@]}" --no-daemon bundleRelease --stacktrace

echo "Build verification completed successfully."
