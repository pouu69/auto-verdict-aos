#!/bin/bash
set -euo pipefail

export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"

cd "$(dirname "$0")"

echo "=== Building WebView bundle ==="
cd webview-bundle
npm ci
npm run build
npm run copy-to-assets
cd ..

echo "=== Running TypeScript tests ==="
cd webview-bundle
npx vitest run
cd ..

echo "=== Building Android APK ==="
./gradlew :app:assembleDebug

echo "=== Running Kotlin tests ==="
./gradlew :app:testDebugUnitTest

echo "=== Done ==="
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
