#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.."
  pwd
)"

cd "$ROOT_DIR"

APP_ID="com.thevirtualtrust.ppis"
APK="app/build/outputs/apk/debug/app-debug.apk"
VALIDATION_STAMP=".gradle/ppis-last-successful-validation"

if [[ "${PPIS_ONLINE:-0}" == "1" ]]; then
  GRADLE=(
    ./gradlew
    --configuration-cache
    --build-cache
  )
else
  GRADLE=(
    ./gradlew
    --offline
    --configuration-cache
    --build-cache
  )
fi

count_tests() {
  python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(
    "app/build/test-results/testDebugUnitTest"
)

total = 0
failed = 0
errors = 0
skipped = 0

if root.exists():
    for p in root.glob("TEST-*.xml"):
        suite = ET.parse(p).getroot()

        total += int(
            suite.attrib.get(
                "tests",
                0
            )
        )

        failed += int(
            suite.attrib.get(
                "failures",
                0
            )
        )

        errors += int(
            suite.attrib.get(
                "errors",
                0
            )
        )

        skipped += int(
            suite.attrib.get(
                "skipped",
                0
            )
        )

print()
print("================================")
print("PPIS TEST SUMMARY")
print("================================")
print(f"Tests:    {total}")
print(f"Failures: {failed}")
print(f"Errors:   {errors}")
print(f"Skipped:  {skipped}")
print("================================")

if failed or errors:
    raise SystemExit(1)
PY
}

case "${1:-}" in

  quick)

    echo "================================"
    echo "PPIS QUICK CHECK"
    echo "================================"

    "${GRADLE[@]}" \
      :app:compileDebugKotlin \
      :app:assembleDebug

    mkdir -p "$(dirname "$VALIDATION_STAMP")"
    touch "$VALIDATION_STAMP"

    ;;


  test)

    if [[ $# -lt 2 ]]; then
      echo "Usage:"
      echo "  tools/dev.sh test <TestClass>"
      exit 2
    fi

    TEST_CLASS="$2"

    echo "================================"
    echo "PPIS TARGETED TEST"
    echo "$TEST_CLASS"
    echo "================================"

    "${GRADLE[@]}" \
      testDebugUnitTest \
      --tests "$TEST_CLASS"

    count_tests

    ;;

  full)

    echo "================================"
    echo "PPIS FULL VALIDATION"
    echo "================================"

    "${GRADLE[@]}" \
      testDebugUnitTest \
      assembleDebug

    mkdir -p "$(dirname "$VALIDATION_STAMP")"
    touch "$VALIDATION_STAMP"

    count_tests

    echo
    echo "APK:"
    ls -lh "$APK"

    ;;

  install)

    if [[ ! -f "$APK" ]]; then
      echo "APK not found."
      echo "Run:"
      echo "  tools/dev.sh quick"
      exit 1
    fi

    if [[ ! -f "$VALIDATION_STAMP" ]]; then

      echo "================================"
      echo "BUILD VALIDATION REQUIRED"
      echo "================================"
      echo
      echo "Run:"
      echo "  tools/dev.sh quick"
      exit 1
    fi

    STALE_SOURCE="$(
      find app/src/main \
        -type f \
        -newer "$VALIDATION_STAMP" \
        -print \
        -quit
    )"

    STALE_CONFIG=""

    for CONFIG_FILE in \
      app/build.gradle.kts \
      build.gradle.kts \
      settings.gradle.kts \
      gradle.properties \
      gradle/libs.versions.toml
    do
      if [[ -f "$CONFIG_FILE" ]] && \
         [[ "$CONFIG_FILE" -nt "$VALIDATION_STAMP" ]]; then

        STALE_CONFIG="$CONFIG_FILE"
        break
      fi
    done

    if [[ -n "$STALE_SOURCE" ]] || \
       [[ -n "$STALE_CONFIG" ]]; then

      echo "================================"
      echo "SOURCE CHANGED AFTER VALIDATION"
      echo "================================"
      echo

      if [[ -n "$STALE_SOURCE" ]]; then
        echo "Newer source:"
        echo "  $STALE_SOURCE"
      fi

      if [[ -n "$STALE_CONFIG" ]]; then
        echo "Newer build config:"
        echo "  $STALE_CONFIG"
      fi

      echo
      echo "Run:"
      echo "  tools/dev.sh quick"
      exit 1
    fi

    echo "================================"
    echo "INSTALL PPIS"
    echo "================================"

    adb install -r "$APK"

    adb shell am force-stop \
      "$APP_ID"

    adb shell am start \
      -n "$APP_ID/.MainActivity"

    ;;

  run)

    adb shell am force-stop \
      "$APP_ID"

    adb shell am start \
      -n "$APP_ID/.MainActivity"

    ;;

  ui)

    PATTERN="${2:-PPIS|Productivity|Activity|Screen time|Reports|Track}"

    adb shell uiautomator dump \
      /sdcard/ppis-ui.xml \
      >/dev/null

    adb shell cat \
      /sdcard/ppis-ui.xml \
      | sed 's/></>\n</g' \
      | grep -oE 'text="[^"]+"' \
      | grep -Ei "$PATTERN" \
      || true

    ;;

  logs)

    adb logcat -d \
      | grep -Ei \
      'AndroidRuntime|FATAL EXCEPTION|thevirtualtrust|ppis' \
      | tail -n 300

    ;;

  clean-logs)

    adb logcat -c

    ;;

  all)

    "$0" full
    "$0" install

    ;;

  *)

    cat <<'TXT'
PPIS developer commands

  tools/dev.sh quick
      Compile + APK only.
      Use after normal code changes.

  tools/dev.sh test <class>
      Run one test class.

  tools/dev.sh full
      Full JVM regression + APK.

  tools/dev.sh install
      Install existing APK + restart app.

  tools/dev.sh all
      Full regression + build + install.

  tools/dev.sh run
      Restart installed app.

  tools/dev.sh ui [regex]
      Dump visible UI text.

  tools/dev.sh logs
      Show relevant crash/app logs.

  tools/dev.sh clean-logs
      Clear logcat.

Default mode is OFFLINE for speed.

If a new dependency must be downloaded:

  PPIS_ONLINE=1 tools/dev.sh full
TXT
    ;;

esac
