#!/usr/bin/env bash

set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$ROOT/final-qa/android-$STAMP"

mkdir -p "$OUT"

SUMMARY="$OUT/SUMMARY.txt"

PASS=0
FAIL=0
SKIP=0

touch "$SUMMARY"


pass() {
    PASS=$((PASS + 1))
    echo "PASS | $*" | tee -a "$SUMMARY"
}


fail() {
    FAIL=$((FAIL + 1))
    echo "FAIL | $*" | tee -a "$SUMMARY"
}


skip() {
    SKIP=$((SKIP + 1))
    echo "SKIP | $*" | tee -a "$SUMMARY"
}


run_test() {

    local name="$1"
    shift

    echo
    echo "=================================================="
    echo "$name"
    echo "=================================================="

    "$@" >"$OUT/$name.log" 2>&1

    local rc=$?

    cat "$OUT/$name.log"

    if [[ $rc -eq 0 ]]; then
        pass "$name"
    else
        fail "$name rc=$rc"
    fi

    return 0
}


echo "PPIS Android Final QA" > "$SUMMARY"
echo "Generated: $(date -Is)" >> "$SUMMARY"
echo >> "$SUMMARY"


# ==========================================================
# STATIC ACCEPTANCE CHECKS
# ==========================================================

if grep -Rqs \
    'DebugEndOfDayReceiver' \
    app/src
then
    fail "No exported debug EOD receiver"
else
    pass "No exported debug EOD receiver"
fi


if grep -RqsE \
    'play-services-fitness|FitnessOptions|GoogleFit' \
    app/src/main \
    app/build.gradle.kts
then
    fail "No obsolete Google Fit implementation"
else
    pass "No obsolete Google Fit implementation"
fi


if grep -qs \
    'android.permission.READ_CALENDAR' \
    app/src/main/AndroidManifest.xml
then
    pass "READ_CALENDAR declared"
else
    fail "READ_CALENDAR declared"
fi


if grep -qs \
    'android.permission.POST_NOTIFICATIONS' \
    app/src/main/AndroidManifest.xml
then
    pass "POST_NOTIFICATIONS declared"
else
    fail "POST_NOTIFICATIONS declared"
fi


if grep -qs \
    'android.permission.RECEIVE_BOOT_COMPLETED' \
    app/src/main/AndroidManifest.xml
then
    pass "RECEIVE_BOOT_COMPLETED declared"
else
    fail "RECEIVE_BOOT_COMPLETED declared"
fi


if grep -qs \
    'TopAppUsage' \
    app/src/main/java/com/thevirtualtrust/ppis/data/usagestats/DeviceScreenTimeSnapshot.kt &&
   grep -qs \
    'screen_time_top_apps_title' \
    app/src/main/java/com/thevirtualtrust/ppis/feature/track/ScreenTimeSection.kt
then
    pass "Top Used Apps implemented"
else
    fail "Top Used Apps implemented"
fi


for file in \
    TrackViewModel.kt \
    ActivityViewModel.kt \
    ScreenTimeViewModel.kt
do

    TARGET="app/src/main/java/com/thevirtualtrust/ppis/feature/track/$file"

    if grep -qs \
        'ZoneId.of' \
        "$TARGET"
    then
        pass "$file resolves profile timezone"
    else
        fail "$file resolves profile timezone"
    fi
done


if grep -qs \
    'ZoneId.systemDefault()' \
    app/src/main/java/com/thevirtualtrust/ppis/data/usagestats/UsageStatsDataSource.kt
then
    fail "UsageStats has no implicit system timezone"
else
    pass "UsageStats has no implicit system timezone"
fi


# ==========================================================
# GRADLE
# ==========================================================

run_test \
    "01_compile_debug" \
    ./gradlew compileDebugKotlin

run_test \
    "02_unit_tests" \
    ./gradlew testDebugUnitTest

run_test \
    "03_lint_debug" \
    ./gradlew lintDebug

run_test \
    "04_assemble_debug" \
    ./gradlew assembleDebug

run_test \
    "05_assemble_release" \
    ./gradlew assembleRelease


# ==========================================================
# BUILD OUTPUT
# ==========================================================

find \
    app/build/outputs \
    -type f \
    \( -name "*.apk" -o -name "*.aab" \) \
    -print \
    > "$OUT/build-artifacts.txt" \
    2>/dev/null || true


if [[ -s "$OUT/build-artifacts.txt" ]]; then
    pass "Build artifacts generated"
else
    fail "Build artifacts generated"
fi


# ==========================================================
# GOOGLE AUTH CONFIG
# ==========================================================

GENERATED_BUILD_CONFIG="$(
    find app/build/generated \
      -path '*debug*BuildConfig.java' \
      -print \
      2>/dev/null \
      | head -1
)"

if [[ -n "$GENERATED_BUILD_CONFIG" ]] &&
   grep -q \
    'GOOGLE_WEB_CLIENT_ID = "1081019880315-' \
    "$GENERATED_BUILD_CONFIG"
then
    pass "Google Web Client ID generated"
else
    fail "Google Web Client ID generated"
fi


# ==========================================================
# REAL DEVICE
# ==========================================================

DEVICE="$(
    adb devices \
      | awk '$2 == "device" {print $1; exit}'
)"

if [[ -z "$DEVICE" ]]; then

    skip "Real-device checks: no adb device"

else

    echo "Device: $DEVICE" \
      > "$OUT/device.txt"

    run_test \
        "06_install_debug" \
        ./gradlew installDebug

    adb logcat -c

    adb shell monkey \
      -p com.thevirtualtrust.ppis \
      -c android.intent.category.LAUNCHER \
      1 \
      > "$OUT/device-launch.txt" \
      2>&1

    sleep 20

    adb logcat -d \
      > "$OUT/logcat-full.txt"

    grep -E \
      'PPIS-|AndroidRuntime|FATAL EXCEPTION' \
      "$OUT/logcat-full.txt" \
      > "$OUT/logcat-ppis.txt" \
      || true


    if grep -qE \
        'FATAL EXCEPTION|Process: com.thevirtualtrust.ppis.*FATAL' \
        "$OUT/logcat-full.txt"
    then
        fail "No startup crash"
    else
        pass "No startup crash"
    fi


    adb shell dumpsys alarm \
      > "$OUT/dumpsys-alarm.txt"

    if grep -q \
        'com.thevirtualtrust.ppis/.sync.work.EndOfDayReceiver' \
        "$OUT/dumpsys-alarm.txt"
    then
        pass "End-of-day alarm scheduled"
    else
        fail "End-of-day alarm scheduled"
    fi


    adb shell appops get \
      com.thevirtualtrust.ppis \
      GET_USAGE_STATS \
      > "$OUT/usage-appops.txt" \
      2>&1 || true

    if grep -qi \
        'allow' \
        "$OUT/usage-appops.txt"
    then
        pass "Usage Access granted"
    else
        skip "Usage Access not currently granted"
    fi


    adb shell dumpsys package \
      com.thevirtualtrust.ppis \
      > "$OUT/package.txt"

    for permission in \
        android.permission.READ_CALENDAR \
        android.permission.POST_NOTIFICATIONS
    do

        if grep -q \
            "$permission: granted=true" \
            "$OUT/package.txt"
        then
            pass "$permission granted"
        else
            skip "$permission not currently granted"
        fi
    done


    if grep -q \
        'android.permission.health.READ_STEPS: granted=true' \
        "$OUT/package.txt"
    then
        pass "Health Connect READ_STEPS granted"
    else
        skip "Health Connect READ_STEPS not currently granted"
    fi


    if grep -q \
        'Rolling sync complete' \
        "$OUT/logcat-ppis.txt"
    then
        pass "Automatic rolling telemetry executed"
    else
        skip "Rolling telemetry log not observed in 20s window"
    fi


    if find app/src/androidTest \
        -type f \
        \( -name '*.kt' -o -name '*.java' \) \
        2>/dev/null \
        | grep -q .
    then

        run_test \
            "07_connected_android_tests" \
            ./gradlew connectedDebugAndroidTest

    else

        skip "No androidTest suite currently present"
    fi
fi


# ==========================================================
# FINAL
# ==========================================================

{
    echo
    echo "=================================================="
    echo "PPIS ANDROID FINAL QA SUMMARY"
    echo "=================================================="
    echo "PASS=$PASS"
    echo "FAIL=$FAIL"
    echo "SKIP=$SKIP"
    echo "OUTPUT=$OUT"
} | tee -a "$SUMMARY"


echo
echo "Summary file:"
echo "$SUMMARY"

if [[ $FAIL -gt 0 ]]; then
    exit 1
fi

exit 0
