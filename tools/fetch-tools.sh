#!/usr/bin/env bash
# Downloads the four pieces build-noide.sh needs, without the Android SDK:
#   aapt2        - resource compiler/linker, taken from the Apktool jar (Apache 2.0)
#   dx.jar       - Java bytecode -> dex, taken from the dex2jar "dex-tools" bundle
#   android.jar  - Android API 34 compile stubs (mirror of the SDK platform jar)
#   uber-apk-signer.jar - zipalign + v1/v2/v3 signing in one jar
#
# Everything comes from GitHub. Works on Linux and macOS.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"

APKTOOL_URL="https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar"
DEXTOOLS_URL="https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip"
ANDROID_JAR_URL="https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar"
SIGNER_URL="https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar"

case "$(uname -s)" in
  Darwin) AAPT2_IN_JAR="prebuilt/macosx/aapt2_64" ;;
  Linux)  AAPT2_IN_JAR="prebuilt/linux/aapt2" ;;
  *) echo "Unsupported OS: $(uname -s)"; exit 1 ;;
esac

echo "-> apktool (for aapt2)"
curl -sSL -o apktool.jar "$APKTOOL_URL"
unzip -o -q apktool.jar "$AAPT2_IN_JAR"
cp "$AAPT2_IN_JAR" aapt2 && chmod +x aapt2
rm -rf prebuilt apktool.jar

echo "-> dex-tools (for dx.jar)"
curl -sSL -o dex-tools.zip "$DEXTOOLS_URL"
unzip -o -q dex-tools.zip 'dex-tools-v2.4/lib/dx-*.jar'
cp dex-tools-v2.4/lib/dx-*.jar dx.jar
rm -rf dex-tools-v2.4 dex-tools.zip

echo "-> android.jar (API 34)"
curl -sSL -o android.jar "$ANDROID_JAR_URL"

echo "-> uber-apk-signer"
curl -sSL -o uber-apk-signer.jar "$SIGNER_URL"

echo
echo "Done. Tools in $HERE:"
ls -la aapt2 dx.jar android.jar uber-apk-signer.jar
