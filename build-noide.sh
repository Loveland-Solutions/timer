#!/usr/bin/env bash
# Builds a signed, installable TileTimer.apk WITHOUT Android Studio or Gradle.
#
# Needs: a JDK (javac, keytool, java, zip) plus the tools from tools/fetch-tools.sh
# (aapt2, dx.jar, android.jar, uber-apk-signer.jar). If a real Android SDK is
# available instead, point AAPT2 / ANDROID_JAR / D8_JAR / APKSIGNER at it.
#
# Output: build/TileTimer.apk
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOLS="${TOOLS:-$ROOT/tools}"
OUT="$ROOT/build/noide"

AAPT2="${AAPT2:-$TOOLS/aapt2}"
ANDROID_JAR="${ANDROID_JAR:-$TOOLS/android.jar}"
DX_JAR="${DX_JAR:-$TOOLS/dx.jar}"
D8_JAR="${D8_JAR:-}"                       # optional r8/d8 jar; used instead of dx when set
SIGNER_JAR="${SIGNER_JAR:-$TOOLS/uber-apk-signer.jar}"
APKSIGNER="${APKSIGNER:-}"                 # optional: SDK apksigner (needs ZIPALIGN too)
ZIPALIGN="${ZIPALIGN:-}"

KEYSTORE="${KEYSTORE:-$ROOT/keystore/tiletimer.jks}"
KS_ALIAS="${KS_ALIAS:-tiletimer}"
KS_PASS="${KS_PASS:-tiletimer}"

PKG="mn.lls.tiletimer"
MIN_SDK=33
TARGET_SDK=35
VERSION_CODE="${VERSION_CODE:-1}"
VERSION_NAME="${VERSION_NAME:-1.0}"

SRC="$ROOT/app/src/main"

for f in "$AAPT2" "$ANDROID_JAR"; do
  [ -e "$f" ] || { echo "Missing $f — run tools/fetch-tools.sh first (or set the env vars)."; exit 1; }
done
[ -n "$D8_JAR" ] || [ -e "$DX_JAR" ] || { echo "Missing $DX_JAR (or set D8_JAR)."; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$ROOT/build"

echo "== 1/6 compile resources"
"$AAPT2" compile --dir "$SRC/res" -o "$OUT/res.zip"

echo "== 2/6 link resources + manifest"
# Gradle/AGP sets the package via `namespace`; aapt2 wants it in the manifest.
sed "s|<manifest |<manifest package=\"$PKG\" |" "$SRC/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
"$AAPT2" link \
  -o "$OUT/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$OUT/AndroidManifest.xml" \
  --java "$OUT/gen" \
  --min-sdk-version "$MIN_SDK" \
  --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay \
  "$OUT/res.zip"

echo "== 3/6 javac"
find "$SRC/java" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -source 8 -target 8 -Xlint:-options -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" \
  -d "$OUT/classes" @"$OUT/sources.txt"

echo "== 4/6 dex"
if [ -n "$D8_JAR" ]; then
  mkdir -p "$OUT/dex"
  find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
  java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api "$MIN_SDK" \
    --lib "$ANDROID_JAR" --output "$OUT/dex" @"$OUT/classes.txt"
  cp "$OUT/dex/classes.dex" "$OUT/classes.dex"
else
  java -cp "$DX_JAR" com.android.dx.command.Main --dex \
    --min-sdk-version="$MIN_SDK" --output="$OUT/classes.dex" "$OUT/classes"
fi

echo "== 5/6 package"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
( cd "$OUT" && zip -q -u unsigned.apk classes.dex )

echo "== 6/6 sign"
if [ ! -e "$KEYSTORE" ]; then
  echo "   (no keystore yet — generating $KEYSTORE; keep it, updates must be signed with the same key)"
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair -keystore "$KEYSTORE" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=Tile Timer, O=Loveland, C=MN" >/dev/null 2>&1
fi

if [ -n "$APKSIGNER" ] && [ -n "$ZIPALIGN" ]; then
  "$ZIPALIGN" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
  "$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$KS_ALIAS" \
    --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
    --out "$ROOT/build/TileTimer.apk" "$OUT/aligned.apk"
else
  [ -e "$SIGNER_JAR" ] || { echo "Missing $SIGNER_JAR"; exit 1; }
  java -jar "$SIGNER_JAR" --apks "$OUT/unsigned.apk" --out "$OUT/signed" \
    --ks "$KEYSTORE" --ksAlias "$KS_ALIAS" --ksPass "$KS_PASS" --ksKeyPass "$KS_PASS" \
    --allowResign --overwrite >/dev/null
  cp "$OUT"/signed/*-aligned-signed.apk "$ROOT/build/TileTimer.apk"
fi

echo
echo "Built: $ROOT/build/TileTimer.apk"
