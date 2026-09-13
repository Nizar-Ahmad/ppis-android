#!/usr/bin/env bash

set -euo pipefail

# ==========================================================
# PPIS PERMANENT RELEASE SIGNING IDENTITY
# ==========================================================

KEY_DIR="$HOME/.config/ppis/release"

KEYSTORE="$KEY_DIR/ppis-release.jks"
ENV_FILE="$KEY_DIR/signing.env"
CERT_INFO="$KEY_DIR/certificate-info.txt"

ALIAS="ppis-release"


# ==========================================================
# CREATE SECURE DIRECTORY
# ==========================================================

mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"


# ==========================================================
# NEVER OVERWRITE EXISTING PRODUCTION KEY
# ==========================================================

if [[ -e "$KEYSTORE" ]]; then

    echo
    echo "ERROR: PPIS release keystore already exists:"
    echo
    echo "$KEYSTORE"
    echo
    echo "Refusing to overwrite permanent signing identity."
    echo
    exit 1
fi


if [[ -e "$ENV_FILE" ]]; then

    echo
    echo "ERROR: Signing credentials file already exists:"
    echo
    echo "$ENV_FILE"
    echo
    echo "Refusing to overwrite it."
    echo
    exit 1
fi


# ==========================================================
# REQUIRED COMMANDS
# ==========================================================

command -v keytool >/dev/null 2>&1 || {
    echo "ERROR: keytool not found"
    exit 1
}

command -v openssl >/dev/null 2>&1 || {
    echo "ERROR: openssl not found"
    exit 1
}


# ==========================================================
# GENERATE STRONG PASSWORDS AUTOMATICALLY
# ==========================================================

STORE_PASSWORD="$(
    openssl rand -hex 32
)"

KEY_PASSWORD="$(
    openssl rand -hex 32
)"


# ==========================================================
# CREATE PERMANENT RELEASE KEY
# ==========================================================

echo
echo "=================================================="
echo "CREATING PPIS RELEASE KEY"
echo "=================================================="
echo
echo "Keystore:"
echo "$KEYSTORE"
echo
echo "Alias:"
echo "$ALIAS"
echo


keytool \
    -genkeypair \
    -v \
    -storetype JKS \
    -keystore "$KEYSTORE" \
    -storepass "$STORE_PASSWORD" \
    -alias "$ALIAS" \
    -keypass "$KEY_PASSWORD" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=PPIS, OU=PPIS Mobile, O=The Virtual Trust, L=Unknown, ST=Unknown, C=AE"


# ==========================================================
# LOCK KEYSTORE PERMISSIONS
# ==========================================================

chmod 600 "$KEYSTORE"


# ==========================================================
# STORE SIGNING CONFIG OUTSIDE REPOSITORY
# ==========================================================

cat > "$ENV_FILE" <<ENV
export PPIS_RELEASE_STORE_FILE="$KEYSTORE"
export PPIS_RELEASE_STORE_PASSWORD="$STORE_PASSWORD"
export PPIS_RELEASE_KEY_ALIAS="$ALIAS"
export PPIS_RELEASE_KEY_PASSWORD="$KEY_PASSWORD"
ENV

chmod 600 "$ENV_FILE"


# ==========================================================
# CERTIFICATE FINGERPRINTS
# ==========================================================

{
    echo "PPIS RELEASE SIGNING CERTIFICATE"
    echo "================================"
    echo
    echo "Keystore:"
    echo "$KEYSTORE"
    echo
    echo "Alias:"
    echo "$ALIAS"
    echo
    echo "Package:"
    echo "com.thevirtualtrust.ppis"
    echo
    echo "Fingerprints:"
    echo

    keytool \
        -list \
        -v \
        -keystore "$KEYSTORE" \
        -storepass "$STORE_PASSWORD" \
        -alias "$ALIAS" \
        | grep -E 'SHA1:|SHA256:'

} > "$CERT_INFO"

chmod 600 "$CERT_INFO"


# ==========================================================
# VERIFY KEY
# ==========================================================

keytool \
    -list \
    -keystore "$KEYSTORE" \
    -storepass "$STORE_PASSWORD" \
    -alias "$ALIAS" \
    >/dev/null


# ==========================================================
# CLEAR PASSWORD VARIABLES FROM CURRENT SCRIPT
# ==========================================================

unset STORE_PASSWORD
unset KEY_PASSWORD


# ==========================================================
# RESULT
# ==========================================================

echo
echo "=================================================="
echo "PPIS RELEASE SIGNING KEY CREATED"
echo "=================================================="
echo
echo "Keystore:"
echo "$KEYSTORE"
echo
echo "Signing environment:"
echo "$ENV_FILE"
echo
echo "Certificate information:"
echo "$CERT_INFO"
echo
echo "Permissions:"
ls -ld "$KEY_DIR"
ls -l \
    "$KEYSTORE" \
    "$ENV_FILE" \
    "$CERT_INFO"

echo
echo "Certificate fingerprints:"
echo
cat "$CERT_INFO"

echo
echo "IMPORTANT:"
echo "Back up the entire directory securely:"
echo
echo "$KEY_DIR"
echo
echo "Do NOT commit it to Git."
echo "Do NOT lose the keystore or signing credentials."
echo
