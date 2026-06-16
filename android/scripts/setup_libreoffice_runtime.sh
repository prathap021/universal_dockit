#!/usr/bin/env bash
# Extract LibreOffice native runtime (.so + assets) from a LibreOffice/Collabora APK
# into android/libreoffice-runtime/ for universal_dockit.
#
# Usage:
#   ./android/scripts/setup_libreoffice_runtime.sh /path/to/libreoffice.apk
#
# Download APK examples:
#   F-Droid: https://f-droid.org/packages/org.documentfoundation.libreoffice/
#   Play Store: Collabora Office (com.collabora.libreoffice)

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 /path/to/libreoffice.apk" >&2
  exit 1
fi

APK="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$(cd "$SCRIPT_DIR/.." && pwd)/libreoffice-runtime"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

echo "Extracting LibreOffice runtime from: $APK"
unzip -oq "$APK" -d "$TMP_DIR"

mkdir -p "$RUNTIME_DIR/jniLibs" "$RUNTIME_DIR/assets"

for abi in arm64-v8a armeabi-v7a; do
  if [[ -d "$TMP_DIR/lib/$abi" ]]; then
    mkdir -p "$RUNTIME_DIR/jniLibs/$abi"
    cp -f "$TMP_DIR/lib/$abi"/*.so "$RUNTIME_DIR/jniLibs/$abi/"
    echo "  copied lib/$abi/*.so"
  fi
done

if [[ -d "$TMP_DIR/assets" ]]; then
  rm -rf "$RUNTIME_DIR/assets"
  cp -a "$TMP_DIR/assets" "$RUNTIME_DIR/assets"
  echo "  copied assets/"
fi

if [[ ! -f "$RUNTIME_DIR/jniLibs/arm64-v8a/liblo-native-code.so" ]] \
  && [[ ! -f "$RUNTIME_DIR/jniLibs/armeabi-v7a/liblo-native-code.so" ]]; then
  echo "ERROR: liblo-native-code.so not found in APK. Use a full LibreOffice/Collabora APK." >&2
  exit 1
fi

echo ""
echo "LibreOffice runtime installed to:"
echo "  $RUNTIME_DIR"
echo ""
echo "Rebuild the Flutter app. Word/Excel/PowerPoint will render via LibreOfficeKit → PDF."
