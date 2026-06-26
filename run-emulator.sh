#!/bin/bash
# Build the debug APK and run it on an Android emulator in one shot.
#
# Usage:
#   ./run-emulator.sh                 # build + install + launch on default AVD
#   ./run-emulator.sh -a Pixel_7      # use a specific AVD
#   ./run-emulator.sh -s              # skip the Gradle build (reuse existing APK)
#   ./run-emulator.sh --shot          # also capture a screenshot when done
#
# Options:
#   -a, --avd NAME    AVD to boot (default: $AVD or Pixel_8)
#   -s, --skip-build  Don't rebuild; install the existing app-debug.apk
#       --shot [PATH] Capture a screenshot after launch (default: ./emulator-shot.png)
#   -h, --help        Show this help
set -euo pipefail

cd "$(dirname "$0")"

AVD_NAME="${AVD:-Pixel_8}"
SKIP_BUILD=false
TAKE_SHOT=false
SHOT_PATH="emulator-shot.png"
APK="app/build/outputs/apk/debug/app-debug.apk"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -a|--avd) AVD_NAME="$2"; shift 2 ;;
    -s|--skip-build) SKIP_BUILD=true; shift ;;
    --shot)
      TAKE_SHOT=true
      if [[ "${2:-}" && "${2:0:1}" != "-" ]]; then SHOT_PATH="$2"; shift; fi
      shift ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

# --- Resolve toolchain --------------------------------------------------------
# JAVA_HOME: honor the environment, else Android Studio's bundled JBR, else brew.
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  elif [[ -d "/opt/homebrew/opt/openjdk@17" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  fi
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

# `android` CLI: PATH first, then the default install location.
ANDROID_CLI="$(command -v android || true)"
[[ -z "$ANDROID_CLI" && -x "$HOME/.local/bin/android" ]] && ANDROID_CLI="$HOME/.local/bin/android"
if [[ -z "$ANDROID_CLI" ]]; then
  echo "ERROR: 'android' CLI not found. Install it from https://dl.google.com/android/cli/" >&2
  exit 1
fi

ADB="$ANDROID_HOME/platform-tools/adb"
[[ -x "$ADB" ]] || ADB="$(command -v adb || echo adb)"

# --- Boot the emulator (only if nothing is connected yet) ---------------------
if "$ADB" devices | grep -qE "device$"; then
  echo "==> A device/emulator is already connected — skipping boot."
else
  echo "==> Starting emulator: $AVD_NAME"
  "$ANDROID_CLI" emulator start "$AVD_NAME"
fi

# --- Build --------------------------------------------------------------------
if [[ "$SKIP_BUILD" == true ]]; then
  echo "==> Skipping build (--skip-build)"
  [[ -f "$APK" ]] || { echo "ERROR: $APK not found; run without -s first." >&2; exit 1; }
else
  echo "==> Building debug APK (JAVA_HOME=$JAVA_HOME)"
  ./gradlew :app:assembleDebug
fi

# --- Install + launch ---------------------------------------------------------
echo "==> Installing and launching on emulator"
"$ANDROID_CLI" run --apks "$APK"

# --- Optional screenshot ------------------------------------------------------
if [[ "$TAKE_SHOT" == true ]]; then
  echo "==> Capturing screenshot -> $SHOT_PATH"
  sleep 3
  "$ANDROID_CLI" screen capture -o "$SHOT_PATH"
fi

echo "==> Done."
