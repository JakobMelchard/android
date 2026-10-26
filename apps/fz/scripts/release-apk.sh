#!/usr/bin/env bash
# Build the signed release APK fz-$VERSION.apk in the repository root.
# Needs VERSION (x.y.z), ANDROID_KEYSTORE_B64 and ANDROID_KEYSTORE_PASSWORD; ANDROID_KEY_ALIAS is optional.
# Run from the repository root. The decoded keystore lives in a temp dir that is removed on exit.
set -euo pipefail

: "${VERSION:?}" "${ANDROID_KEYSTORE_B64:?}" "${ANDROID_KEYSTORE_PASSWORD:?}"

dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT
export KEYSTORE_PATH="$dir/release.jks"
printf '%s' "$ANDROID_KEYSTORE_B64" | base64 -d >"$KEYSTORE_PATH"

# The keystore has no "fz" alias: unless ANDROID_KEY_ALIAS is set, sign with its only key.
if [ -z "${ANDROID_KEY_ALIAS:-}" ]; then
  ANDROID_KEY_ALIAS=$(keytool -list -keystore "$KEYSTORE_PATH" -storepass "$ANDROID_KEYSTORE_PASSWORD" |
    awk -F', ' '/PrivateKeyEntry/ { print $1; exit }')
fi
export ANDROID_KEY_ALIAS
echo "signing with alias: $ANDROID_KEY_ALIAS"

IFS=. read -r major minor patch <<<"$VERSION"
chmod +x gradlew
./gradlew --no-daemon assembleRelease \
  -PversionName="$VERSION" -PversionCode="$((major * 10000 + minor * 100 + patch))"
cp app/build/outputs/apk/release/app-release.apk "fz-$VERSION.apk"
