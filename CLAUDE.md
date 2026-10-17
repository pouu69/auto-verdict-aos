# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AutoVerdict is a hybrid Android app (Kotlin + Compose) that evaluates used car listings from Encar (엔카). The native layer handles data collection and navigation; a React/TypeScript WebView bundle runs the rule-based evaluation engine and renders results.

## Build & Test Commands

```bash
# Full build pipeline (webview bundle → Android APK → all tests)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./build-all.sh

# TypeScript (webview-bundle)
cd webview-bundle && npm run build          # Build + copy to Android assets
cd webview-bundle && npx vitest run          # Run all tests
cd webview-bundle && npx vitest run tests/orchestrate-mobile.test.ts  # Single test file

# Android (requires JAVA_HOME set to Android Studio JBR)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug                # Build debug APK
./gradlew :app:testDebugUnitTest            # Run Kotlin unit tests
```

The WebView bundle must be built before the Android APK — the build copies `webview-bundle/dist/` into `app/src/main/assets/eval-ui/` (git-ignored).

## Architecture

### Data Flow

```
User shares Encar URL (or pastes in app)
  → MainActivity receives intent, navigates to ResultScreen
    → CollectorWebView loads Encar page, extracts __PRELOADED_STATE__
    → CollectorWebView fetches record/diagnosis/inspection APIs via JS
    → JSON passed to EvalWebView: window.receiveEncarData(json)
      → React App.tsx → orchestrateMobile() → rule engine → renders results
      → User saves → Android.saveCar(json) → NativeBridge → Room DB
```

### Native ↔ WebView Bridge

- **Native → JS**: `EvalWebView.sendData(json)` calls `window.receiveEncarData()` via `evaluateJavascript`. Queues data if page not yet loaded.
- **JS → Native**: `NativeBridge` exposes `@JavascriptInterface` methods as `window.Android` — `saveCar()`, `closeOverlay()`, `showToast()`.
- **TypeScript side**: `android-bridge.ts` wraps `window.Android` with type-safe calls and existence checks.

### Core Library (vendored)

The TypeScript webview imports `@core`, which maps to the in-repo copy at `webview-bundle/src/core/` (see `tsconfig.json` paths + `vite.config.ts` alias). Contains: `ParsedData`/`ChecklistFacts`/`RuleTypes`/`FieldStatus` types, the Encar parsers (`parsers/encar/*`), the `encarToFacts()` bridge, and the rule engine (`rules/index.ts`).

This is a vendored subset (the transitive closure aos actually uses) of the shared core that originates in the sibling `daksin-car` repo. It is self-contained — no `dexie`/`zod`/`background` dependencies. When the upstream core changes, re-sync the relevant files manually rather than re-pointing `@core` at the sibling repo (the old setup broke the build whenever `daksin-car` was absent at the expected path).

### Database (Room)

Two entities: `SavedCarEntity` (user-saved evaluations with score/verdict/counts) and `CacheEntity` (24h TTL raw analysis cache). Both use `carId` as primary key with `@Upsert` for insert-or-update.

## Key Conventions

- UI follows the Toss-style design system in `DESIGN.md`: tokens via `AvTheme.colors/typography/shapes/spacing` (`ui/theme`), shared `Av*` components in `ui/components`. Screens never use raw colors.
- Navigation is a small saveable back stack, not a navigation library: `ui/navigation/Route` destinations, the pure `AppNavigator` reducer (unit-tested), and the `AppRoot` shell. Screens receive a `ScreenHost` and hold no navigation state.
- `MainActivity` only parses share intents (`ShareIntentParser`, skipped on restore) and hosts `AppRoot`; app-level state (back stack, last analyzed URL) lives in `AppRoot` via `rememberSaveable`.
- Two tabs (홈, 저장한 차); Settings is a sub-screen opened from the home top bar. The tab bar and banner render only on tab roots.
- `EncarUrl` utility validates and extracts car IDs from multiple Encar URL formats
- ProGuard keeps `@JavascriptInterface` methods and Room classes — update `proguard-rules.pro` when adding new bridge methods or entities
