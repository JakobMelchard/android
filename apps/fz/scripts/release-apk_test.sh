#!/usr/bin/env bash
# Runs release-apk.sh against stub keytool and gradlew and checks what reaches Gradle.
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/release-apk.sh"
fail() {
  echo "FAIL: $*" >&2
  exit 1
}

# run <alias or empty>: prints the recorded gradlew arguments
run() {
  local tmp
  tmp=$(mktemp -d)
  mkdir -p "$tmp/bin"
  printf '#!/bin/sh\necho "Your keystore contains 1 entry"\necho "upload, Jan 1, 2026, PrivateKeyEntry,"\n' >"$tmp/bin/keytool"
  # shellcheck disable=SC2016 # expanded by the stub, not here
  printf '#!/bin/sh\necho "$KEYSTORE_PATH $ANDROID_KEY_ALIAS $*" >args\nmkdir -p app/build/outputs/apk/release\necho apk >app/build/outputs/apk/release/app-release.apk\n' >"$tmp/gradlew"
  chmod +x "$tmp/bin/keytool"
  (
    cd "$tmp"
    PATH="$tmp/bin:$PATH" VERSION=1.2.3 ANDROID_KEYSTORE_PASSWORD=pw ANDROID_KEY_ALIAS="$1" \
      ANDROID_KEYSTORE_B64=$(printf 'jks' | base64) "$script" >/dev/null
    [ -f fz-1.2.3.apk ] || fail "fz-1.2.3.apk missing"
    read -r keystore _ <args
    [ ! -e "$keystore" ] || fail "keystore $keystore left behind"
    cut -d' ' -f2- args
  )
  rm -rf "$tmp"
}

want="upload --no-daemon assembleRelease -PversionName=1.2.3 -PversionCode=10203"
got=$(run "")
[ "$got" = "$want" ] || fail "alias from keystore: got '$got'"

got=$(run "fz")
[ "$got" = "${want/upload/fz}" ] || fail "alias from env: got '$got'"

echo "release-apk: ok"
