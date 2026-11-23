#!/usr/bin/env bash
# Makes the upload key that signs Attic's builds for Google Play. Play App Signing holds the key that
# signs what users install; this one only proves an upload came from you. Losing it means asking Play
# support for a reset, so back the file and its password up somewhere other than this laptop.
#
# Usage: scripts/make-upload-key.sh [keystore path]   (default ~/.android/attic-upload.jks)
set -euo pipefail

store="${1:-$HOME/.android/attic-upload.jks}"
alias="attic-upload"

if [[ -e "$store" ]]; then
  echo "$store already exists. Not overwriting it." >&2
  exit 1
fi

keytool="keytool"
for home in "${JAVA_HOME:-}" /opt/homebrew/opt/openjdk@17 /opt/homebrew/opt/openjdk; do
  if [[ -n "$home" && -x "$home/bin/keytool" ]]; then
    keytool="$home/bin/keytool"
    break
  fi
done

mkdir -p "$(dirname "$store")"
echo "keytool asks for a password, then your name and organisation (only the name is needed)."
echo "PKCS12 keystores use the keystore password for the key too."
# 10,000 days runs past the 2033 minimum Play sets for signing keys.
"$keytool" -genkeypair -v \
  -keystore "$store" -storetype PKCS12 \
  -alias "$alias" -keyalg RSA -keysize 4096 -validity 10000
chmod 600 "$store"

cat <<NEXT

Made $store. Add these lines to ~/.gradle/gradle.properties, with the password you just chose in
both password lines (that file stays outside the repo):

ATTIC_UPLOAD_STORE_FILE=$store
ATTIC_UPLOAD_STORE_PASSWORD=
ATTIC_UPLOAD_KEY_ALIAS=$alias
ATTIC_UPLOAD_KEY_PASSWORD=

Then build the bundle for Play with ./gradlew bundleRelease
NEXT
