#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."


# ==========================================================
# PPIS RELEASE SIGNING CONFIG
# ==========================================================

SIGNING_ENV="$HOME/.config/ppis/release/signing.env"

if [[ ! -f "$SIGNING_ENV" ]]; then

    echo
    echo "ERROR: PPIS release signing configuration not found:"
    echo
    echo "$SIGNING_ENV"
    echo
    echo "Create it first:"
    echo
    echo "tools/create-release-keystore.sh"
    echo
    exit 1
fi


# shellcheck disable=SC1090
source "$SIGNING_ENV"


# ==========================================================
# VALIDATE REQUIRED VALUES
# ==========================================================

required_vars=(
    PPIS_RELEASE_STORE_FILE
    PPIS_RELEASE_STORE_PASSWORD
    PPIS_RELEASE_KEY_ALIAS
    PPIS_RELEASE_KEY_PASSWORD
)

for variable in "${required_vars[@]}"; do

    if [[ -z "${!variable:-}" ]]; then

        echo "ERROR: Missing signing variable:"
        echo "$variable"

        exit 1
    fi
done


if [[ ! -f "$PPIS_RELEASE_STORE_FILE" ]]; then

    echo "ERROR: Release keystore does not exist:"
    echo "$PPIS_RELEASE_STORE_FILE"

    exit 1
fi


# ==========================================================
# BUILD SIGNED APK + AAB
# ==========================================================

echo
echo "=================================================="
echo "BUILDING SIGNED PPIS RELEASE"
echo "=================================================="

./gradlew \
    --offline \
    --build-cache \
    --no-configuration-cache \
    :app:assembleRelease \
    :app:bundleRelease


# ==========================================================
# SHOW OUTPUTS
# ==========================================================

echo
echo "=================================================="
echo "RELEASE ARTIFACTS"
echo "=================================================="

find \
    app/build/outputs/apk/release \
    app/build/outputs/bundle/release \
    -maxdepth 1 \
    -type f \
    -printf '%p  %k KB\n' \
    2>/dev/null \
    || true


# ==========================================================
# VERIFY APK SIGNATURE
# ==========================================================

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"

APKSIGNER="$(
    find "$SDK_ROOT/build-tools" \
        -mindepth 2 \
        -maxdepth 2 \
        -type f \
        -name apksigner \
        2>/dev/null \
        | sort -V \
        | tail -n 1
)"


APK="app/build/outputs/apk/release/app-release.apk"


if [[ -n "$APKSIGNER" && -f "$APK" ]]; then

    echo
    echo "=================================================="
    echo "APK SIGNATURE VERIFICATION"
    echo "=================================================="

    "$APKSIGNER" \
        verify \
        --verbose \
        --print-certs \
        "$APK"

else

    echo
    echo "WARNING:"
    echo "apksigner or signed APK not found."
fi


# ==========================================================
# REMOVE PASSWORDS FROM SCRIPT ENVIRONMENT
# ==========================================================

unset PPIS_RELEASE_STORE_PASSWORD
unset PPIS_RELEASE_KEY_PASSWORD


echo
echo "=================================================="
echo "SIGNED RELEASE COMPLETE"
echo "=================================================="
