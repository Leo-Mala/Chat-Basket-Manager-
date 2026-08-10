#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="${GRADLE_VERSION:-9.3.1}"

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is required once to bootstrap the wrapper." >&2
  echo "Install Gradle ${GRADLE_VERSION}, then rerun this script." >&2
  exit 1
fi

gradle --no-daemon wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
chmod +x gradlew

test -s gradlew
test -s gradle/wrapper/gradle-wrapper.jar
test -s gradle/wrapper/gradle-wrapper.properties

echo "Gradle Wrapper ${GRADLE_VERSION} generated successfully."
