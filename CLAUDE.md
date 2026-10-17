# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AutoVerdict is a hybrid Android app (Kotlin + Compose) that evaluates used car listings from Encar (엔카). The native layer handles data collection, navigation, storage and all UI, including the Compose result screen. A TypeScript WebView bundle runs headless (never shown): it hosts the rule-based evaluation engine plus a report adapter that turns engine output into a render-ready `EvaluationReport` JSON.

## Build & Test Commands

```bash
# Full build pipeline (webview bundle → Android APK → all tests)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./build-all.sh

# TypeScript (webview-bundle)
cd webview-bundle && npm run build           # tsc + vite → dist/engine.html + dist/engine.js (IIFE)
cd webview-bundle && npm run copy-to-assets  # dist/ → app/src/main/assets/eval-ui/
cd webview-bundle && npx vitest run          # Run all tests
cd webview-bundle && npx vitest run tests/orchestrate-mobile.test.ts  # Single test file
cd webview-bundle && npm run gen:contract    # Regenerate app/src/test/resources/report/encar-*.json

# Android (requires JAVA_HOME set to Android Studio JBR)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug                # Build debug APK
./gradlew :app:testDebugUnitTest            # Run Kotlin unit tests
```

Gradle's `preBuild` runs `npm run build` + `copy-to-assets`, so `assembleDebug` always ships the current engine in `app/src/main/assets/eval-ui/` (git-ignored). The bundle targets `es2019` so old System WebViews can parse it.

The contract JSON under `app/src/test/resources/report/` is generated from the TS adapter and read by the Kotlin `ReportParser`/Compose tests. A plain `vitest run` fails when it is stale — after any adapter change, run `npm run gen:contract` and commit the result.

## Architecture

### Data Flow

```
User shares Encar URL (or pastes in app)
  → MainActivity parses intent → AppRoot → ResultRoute → ResultController
    → source: saved car (then silent background refresh) → 24h cache → CollectorWebView
      (CollectorWebView loads the Encar page, extracts __PRELOADED_STATE__, fetches record/diagnosis/inspection APIs)
    → RuleEngine.evaluate(rawJson) → window.avEvaluate(requestId, rawJson)
      → orchestrateMobile() (vendored core) + buildReport() (src/report adapter)
      → Android.onReport(requestId, json) → ReportParser → EvaluationReport
    → Compose ResultScreen (result/sections/*)
    → save/unsave via ResultRepository (DefaultResultRepository) → Room
```

`ResultController` is a plain main-confined class (not a ViewModel) created by `ResultRoute`; it owns `ResultUiState` (Loading/Ready/Error/Deleted) and one-off `ResultEvent`s. The cache is written only when the report has no `dataGaps`.

### Headless Engine Bridge

- `RuleEngine` (app-scoped in `AutoVerdictApp`) lazily creates one `WebViewEngineHost` (off-screen WebView, network and file access off) loading `eval-ui/engine.html`, and reuses it.
- **Native → JS**: `window.avEvaluate(requestId, rawJson)` via `evaluateJavascript`. Requests before `onReady` are queued; each request has its own `requestId`, so several can be in flight.
- **JS → Native**: `EngineBridge` exposes `window.Android` — `onReady()`, `onReport(requestId, json)`, `onEngineError(requestId, message)`; calls are posted to the main thread. TS side: `engine-bridge.ts`. Exactly one report or error comes back per request; evaluation in JS must stay synchronous (off-screen timers are throttled).
- **Timeouts/recreate**: init timeout (10s) or renderer gone → host destroyed, all pending requests fail, next request creates a new host. Request timeout (10s) fails only that request; 2 in a row with no response discards the host. Callbacks from a discarded host are ignored.
- **Contract**: `webview-bundle/src/report/types.ts` and `report/EvaluationReport.kt` mirror each other — change both together and bump `SCHEMA_VERSION` on breaking changes. `ReportParser` ignores unknown fields but rejects any other mismatch or a different `schemaVersion` as an engine error.

### Core Library (vendored)

The TypeScript webview imports `@core`, which maps to the in-repo copy at `webview-bundle/src/core/` (see `tsconfig.json` paths + `vite.config.ts` alias). Contains: `ParsedData`/`ChecklistFacts`/`RuleTypes`/`FieldStatus` types, the Encar parsers (`parsers/encar/*`), the `encarToFacts()` bridge, and the rule engine (`rules/index.ts`).

This is a vendored subset (the transitive closure aos actually uses) of the shared core that originates in the sibling `daksin-car` repo. It is self-contained — no `dexie`/`zod`/`background` dependencies. When the upstream core changes, re-sync the relevant files manually rather than re-pointing `@core` at the sibling repo (the old setup broke the build whenever `daksin-car` was absent at the expected path).

### Database (Room)

Two entities: `SavedCarEntity` (user-saved evaluations with score/verdict/counts + collector `rawJson`) and `CacheEntity` (24h TTL cache; `rawInputJson` is re-evaluated on open). Both use `carId` as primary key with `@Upsert` for insert-or-update. Reports are never stored — saved and cached rows are re-run through the engine.

## Key Conventions

- UI follows the Toss-style design system in `DESIGN.md`: tokens via `AvTheme.colors/typography/shapes/spacing` (`ui/theme`), shared `Av*` components in `ui/components`. Screens never use raw colors.
- Navigation is a small saveable back stack, not a navigation library: `ui/navigation/Route` destinations, the pure `AppNavigator` reducer (unit-tested), and the `AppRoot` shell. Screens receive a `ScreenHost` and hold no navigation state.
- `MainActivity` only parses share intents (`ShareIntentParser`, skipped on restore) and hosts `AppRoot`; app-level state (back stack, plus a share link held while first-run onboarding is open) lives in `AppRoot` via `rememberSaveable`.
- Two tabs (홈, 저장한 차); Settings is a sub-screen opened from the home top bar. The tab bar and banner render only on tab roots.
- Copy split: domain copy (verdict headline, rule messages/why, seller questions, timeline labels, share text) is built by the TS adapter (`webview-bundle/src/report/*`) and tested with vitest; screen chrome copy (section titles, buttons, banners, errors) lives in `strings.xml`. Compose renders report strings as-is.
- Verdict/score logic is off-limits: do not modify the vendored core (`webview-bundle/src/core/`); fix wording in the adapter instead.
- Do not change the Room schema without a real migration — `AppDatabase` uses `fallbackToDestructiveMigration()`, so a version bump wipes saved cars.
- `EncarUrl` utility validates and extracts car IDs from multiple Encar URL formats
- ProGuard keeps `@JavascriptInterface` methods (`EngineBridge`, `CollectorWebView$ApiBridge`), the `report.**` serializers and Room classes — update `proguard-rules.pro` when adding new bridge methods, `@Serializable` report types outside `report/`, or entities
