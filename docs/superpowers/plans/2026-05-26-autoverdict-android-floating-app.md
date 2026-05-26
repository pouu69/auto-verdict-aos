# AutoVerdict Android Floating App — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the AutoVerdict Chrome Extension into an Android Floating App that evaluates Encar used car listings via a floating button overlay, reusing the existing TypeScript evaluation engine (12 rules, 0–100 scoring).

**Architecture:** Native Kotlin shell (FloatingService, MainActivity, Room DB) + two WebViews: a hidden one for data collection from Encar pages, and a visible one rendering the evaluation UI (React/TypeScript). The TS core logic (parsers, bridge, rules) is consumed via Vite path alias pointing to the original `daksin-car` repo — no code duplication.

**Tech Stack:** Kotlin · Gradle (KTS) · Jetpack Compose · Room · WebView · React 18 · TypeScript 5.6 · Vite 5 · Vitest

**Spec:** `docs/superpowers/specs/2026-05-26-autoverdict-android-floating-app-design.md`

---

## File Structure

```
daksin-car-aos/
├── build.gradle.kts                          # Root Gradle build
├── settings.gradle.kts                       # Include :app
├── gradle.properties                         # JVM args, AndroidX opt-in
├── gradlew / gradlew.bat                     # Gradle wrapper
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── .gitignore
├── app/
│   ├── build.gradle.kts                      # App module: AGP, Compose, Room, KSP
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/eval-ui/               # Built WebView bundle (copied by build script)
│       │   ├── kotlin/com/daksin/autoverdict/
│       │   │   ├── AutoVerdictApp.kt         # Application class
│       │   │   ├── MainActivity.kt           # 3-tab host (Compose)
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt
│       │   │   │   │   ├── Theme.kt
│       │   │   │   │   └── Type.kt
│       │   │   │   └── screen/
│       │   │   │       ├── AnalyzeScreen.kt  # Tab 1: URL input + recent
│       │   │   │       ├── SavedListScreen.kt# Tab 2: saved cars
│       │   │   │       └── SettingsScreen.kt # Tab 3: settings
│       │   │   ├── floating/
│       │   │   │   ├── FloatingService.kt    # Foreground Service + button + overlay
│       │   │   │   └── OverlayManager.kt     # Manages overlay window lifecycle
│       │   │   ├── collector/
│       │   │   │   └── CollectorWebView.kt   # Hidden WebView for encar data extraction
│       │   │   ├── webview/
│       │   │   │   ├── EvalWebView.kt        # Evaluation UI WebView
│       │   │   │   └── NativeBridge.kt       # @JavascriptInterface methods
│       │   │   ├── db/
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── SavedCarEntity.kt
│       │   │   │   ├── SavedCarDao.kt
│       │   │   │   ├── CacheEntity.kt
│       │   │   │   └── CacheDao.kt
│       │   │   └── util/
│       │   │       ├── EncarUrl.kt           # URL parsing, carId extraction
│       │   │       └── ClipboardUtil.kt      # Clipboard read helper
│       │   └── res/
│       │       ├── drawable/ic_av_button.xml
│       │       ├── values/strings.xml
│       │       ├── values/colors.xml
│       │       └── values/themes.xml
│       └── test/
│           └── kotlin/com/daksin/autoverdict/
│               └── util/EncarUrlTest.kt
├── webview-bundle/
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── index.html                            # Entry HTML
│   ├── src/
│   │   ├── main.tsx                          # React entry
│   │   ├── App.tsx                           # Root: receives data, runs pipeline, renders
│   │   ├── orchestrate-mobile.ts             # Mobile orchestrator (parsers → bridge → rules)
│   │   ├── android-bridge.ts                 # Native ↔ WebView communication
│   │   ├── rule-meta.ts                      # Category/icon metadata (from sidepanel)
│   │   ├── tokens.ts                         # Design system tokens
│   │   ├── types/
│   │   │   └── android.d.ts                  # window.Android type declarations
│   │   └── overlay/
│   │       ├── OverlayPage.tsx               # Full overlay layout
│   │       ├── ScoreCard.tsx                  # Score + verdict + vehicle info
│   │       ├── SummaryRow.tsx                 # 4-cell severity summary
│   │       ├── CategoryAccordion.tsx          # Collapsible category group
│   │       └── RuleLine.tsx                   # Single rule result line
│   └── tests/
│       ├── setup.ts
│       ├── orchestrate-mobile.test.ts
│       ├── android-bridge.test.ts
│       └── overlay/
│           └── OverlayPage.test.tsx
└── docs/
    └── superpowers/
        ├── specs/2026-05-26-autoverdict-android-floating-app-design.md
        └── plans/2026-05-26-autoverdict-android-floating-app.md  # This file
```

---

## Part A: Foundation

### Task 1: Development Environment Setup

**Files:**
- Modify: `~/.zshrc` (ANDROID_HOME export)

> This task installs JDK 17, Android SDK, and Gradle on macOS arm64 via Homebrew.

- [ ] **Step 1: Install JDK 17**

```bash
brew install openjdk@17
```

Verify:

```bash
java -version
# Expected: openjdk version "17.x.x"
```

If `java -version` still fails, symlink:

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

- [ ] **Step 2: Install Android SDK command-line tools**

```bash
brew install --cask android-commandlinetools
```

Set environment variables — add to `~/.zshrc`:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Then reload:

```bash
source ~/.zshrc
```

- [ ] **Step 3: Install SDK packages**

```bash
sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" \
  "build-tools;34.0.0" \
  "platforms;android-34"
```

Accept licenses:

```bash
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
```

- [ ] **Step 4: Install Gradle (for wrapper generation)**

```bash
brew install gradle
```

Verify:

```bash
gradle --version
# Expected: Gradle 8.x
```

- [ ] **Step 5: Verify full toolchain**

```bash
java -version && sdkmanager --version && gradle --version
```

All three should print version info without errors.

---

### Task 2: Android Project Scaffold

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/AutoVerdictApp.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `.gitignore`

- [ ] **Step 1: Generate Gradle wrapper**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos
gradle wrapper --gradle-version 8.11.1
```

Verify `gradlew` exists:

```bash
ls -la gradlew
# Expected: -rwxr-xr-x ... gradlew
```

- [ ] **Step 2: Create root build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
```

- [ ] **Step 3: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "daksin-car-aos"
include(":app")
```

- [ ] **Step 4: Create gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.daksin.autoverdict"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.daksin.autoverdict"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WebView
    implementation("androidx.webkit:webkit:1.12.1")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")

    // Test
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 6: Create AndroidManifest.xml**

```xml
<!-- app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".AutoVerdictApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AutoVerdict">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.AutoVerdict">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
        </activity>

        <service
            android:name=".floating.FloatingService"
            android:exported="false"
            android:foregroundServiceType="specialUse" />
    </application>
</manifest>
```

- [ ] **Step 7: Create AutoVerdictApp.kt**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/AutoVerdictApp.kt
package com.daksin.autoverdict

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.daksin.autoverdict.db.AppDatabase

class AutoVerdictApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AutoVerdict 서비스",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "플로팅 버튼 서비스 실행 중"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "autoverdict_floating"
    }
}
```

- [ ] **Step 8: Create resource files**

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AutoVerdict</string>
    <string name="tab_analyze">분석하기</string>
    <string name="tab_saved">저장목록</string>
    <string name="tab_settings">설정</string>
    <string name="floating_notification_title">AutoVerdict 실행 중</string>
    <string name="floating_notification_text">플로팅 버튼이 활성화되어 있습니다</string>
</resources>
```

`app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#0064FF</color>
    <color name="background">#F7F8FA</color>
    <color name="surface">#FFFFFF</color>
    <color name="danger">#E53935</color>
    <color name="warning">#F57C00</color>
    <color name="success">#2E7D32</color>
    <color name="text_primary">#1A1A1A</color>
    <color name="text_secondary">#888888</color>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AutoVerdict" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">@color/background</item>
        <item name="android:navigationBarColor">@color/surface</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>
</resources>
```

- [ ] **Step 9: Create .gitignore**

```gitignore
# .gitignore
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
/app/build
/app/src/main/assets/eval-ui/
node_modules/
webview-bundle/dist/
.omc/
.superpowers/
```

- [ ] **Step 10: Create directory structure and verify build**

```bash
mkdir -p app/src/main/kotlin/com/daksin/autoverdict/{ui/theme,ui/screen,floating,collector,webview,db,util}
mkdir -p app/src/main/res/{drawable,mipmap-hdpi,mipmap-mdpi,mipmap-xhdpi,mipmap-xxhdpi,mipmap-xxxhdpi}
mkdir -p app/src/main/assets/eval-ui
mkdir -p app/src/test/kotlin/com/daksin/autoverdict/util
```

Create placeholder for launcher icon (required for build):

```bash
# Create minimal adaptive icon files — will be replaced with proper icon later
```

Run:

```bash
./gradlew assembleDebug 2>&1 | tail -20
# Expected: BUILD SUCCESSFUL
```

> Note: First build downloads dependencies (~5 min). If launcher icon is missing, the build may warn but should still succeed. We'll add proper icons in a later task.

- [ ] **Step 11: Initialize git and commit**

```bash
git init
git add -A
git commit -m "feat: android project scaffold

Kotlin + Jetpack Compose + Room project with FloatingService
declaration, permissions (SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE),
and app shell.

Constraint: Min SDK 26 — no backwards compat for notification channels
Confidence: high
Scope-risk: narrow"
```

---

## Part B: WebView Bundle

### Task 3: WebView Bundle — Project Setup

**Files:**
- Create: `webview-bundle/package.json`
- Create: `webview-bundle/tsconfig.json`
- Create: `webview-bundle/vite.config.ts`
- Create: `webview-bundle/index.html`
- Create: `webview-bundle/src/main.tsx` (placeholder)

- [ ] **Step 1: Create package.json**

```json
{
  "name": "autoverdict-webview",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc --noEmit && vite build",
    "test": "vitest run",
    "test:watch": "vitest",
    "copy-to-assets": "rm -rf ../app/src/main/assets/eval-ui && cp -r dist/ ../app/src/main/assets/eval-ui/"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
  },
  "devDependencies": {
    "@testing-library/react": "^16.1.0",
    "@testing-library/jest-dom": "^6.6.3",
    "@types/react": "^18.3.12",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.4",
    "jsdom": "^25.0.1",
    "typescript": "~5.6.3",
    "vite": "^5.4.11",
    "vitest": "^2.1.8"
  }
}
```

- [ ] **Step 2: Create tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "paths": {
      "@core/*": ["../../daksin-car/src/core/*"]
    }
  },
  "include": ["src", "tests"]
}
```

- [ ] **Step 3: Create vite.config.ts**

```typescript
// webview-bundle/vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@core': path.resolve(__dirname, '../../daksin-car/src/core'),
    },
  },
  build: {
    outDir: 'dist',
    assetsInlineLimit: 0,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './tests/setup.ts',
  },
});
```

- [ ] **Step 4: Create index.html**

```html
<!-- webview-bundle/index.html -->
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>AutoVerdict</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
      background: #f7f8fa;
      -webkit-text-size-adjust: 100%;
      overflow-x: hidden;
    }
  </style>
</head>
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.tsx"></script>
</body>
</html>
```

- [ ] **Step 5: Create placeholder main.tsx and test setup**

`webview-bundle/src/main.tsx`:

```tsx
import { createRoot } from 'react-dom/client';

function App() {
  return <div>AutoVerdict loading...</div>;
}

createRoot(document.getElementById('root')!).render(<App />);
```

`webview-bundle/tests/setup.ts`:

```typescript
import '@testing-library/jest-dom';
```

- [ ] **Step 6: Install dependencies and verify**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npm install
npx tsc --noEmit
```

Expected: no errors.

```bash
npx vite build
```

Expected: `dist/index.html` and `dist/assets/` created.

- [ ] **Step 7: Commit**

```bash
git add webview-bundle/
git commit -m "feat: webview bundle project setup

Vite + React 18 + TypeScript with @core alias pointing to
daksin-car/src/core for zero-copy reuse of evaluation engine.

Constraint: @core alias avoids importing parsers/encar/index.ts (Chrome Extension import chain)
Confidence: high
Scope-risk: narrow"
```

---

### Task 4: Design Tokens + Rule Meta

**Files:**
- Create: `webview-bundle/src/tokens.ts`
- Create: `webview-bundle/src/rule-meta.ts`
- Create: `webview-bundle/src/types/android.d.ts`

- [ ] **Step 1: Create design tokens**

```typescript
// webview-bundle/src/tokens.ts
export const color = {
  background: '#f7f8fa',
  surface: '#ffffff',
  primary: '#0064FF',
  textPrimary: '#1a1a1a',
  textSecondary: '#888888',
  danger: '#E53935',
  dangerBg: '#FFEBEE',
  warning: '#F57C00',
  warningBg: '#FFF3E0',
  success: '#2E7D32',
  successBg: '#E8F5E9',
  unknownBg: '#F5F5F5',
  border: '#f0f0f0',
} as const;

export const radius = {
  card: '12px',
  button: '8px',
  badge: '6px',
} as const;

export const shadow = {
  card: '0 1px 4px rgba(0,0,0,0.04)',
} as const;
```

- [ ] **Step 2: Create rule-meta (adapted from sidepanel)**

```typescript
// webview-bundle/src/rule-meta.ts
export type Category = '투명성' | '차량 상태' | '이력' | '사고' | '가격';

export interface RuleMeta {
  icon: string;
  category: Category;
  shortTitle: string;
}

export const RULE_META: Record<string, RuleMeta> = {
  R01: { icon: '📋', category: '투명성', shortTitle: '보험이력 공개' },
  R02: { icon: '🔍', category: '투명성', shortTitle: '성능점검 공개' },
  R03: { icon: '🏅', category: '차량 상태', shortTitle: '엔카진단' },
  R04: { icon: '🛠', category: '차량 상태', shortTitle: '프레임/외판' },
  R05: { icon: '🚕', category: '이력', shortTitle: '렌트·택시 이력' },
  R06: { icon: '💦', category: '이력', shortTitle: '전손·침수·도난' },
  R07: { icon: '👤', category: '이력', shortTitle: '소유자 변경' },
  R08: { icon: '🛡', category: '이력', shortTitle: '자차보험 공백' },
  R09: { icon: '📝', category: '사고', shortTitle: '수리비 확정' },
  R10: { icon: '🔨', category: '사고', shortTitle: '보험처리 규모' },
  R11: { icon: '💰', category: '가격', shortTitle: '가격 적정성' },
  R12: { icon: '🛢', category: '차량 상태', shortTitle: '누유 여부' },
};

export const CATEGORY_ORDER: Category[] = [
  '차량 상태',
  '이력',
  '사고',
  '가격',
  '투명성',
];
```

- [ ] **Step 3: Create Android type declarations**

```typescript
// webview-bundle/src/types/android.d.ts
interface AndroidBridgeInterface {
  saveCar(json: string): void;
  closeOverlay(): void;
  showToast(message: string): void;
  getClipboardUrl(): string | null;
}

interface Window {
  Android?: AndroidBridgeInterface;
  receiveEncarData?: (json: string) => void;
  receiveError?: (json: string) => void;
}
```

- [ ] **Step 4: Verify TypeScript**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add webview-bundle/src/tokens.ts webview-bundle/src/rule-meta.ts webview-bundle/src/types/
git commit -m "feat: design tokens, rule metadata, and Android bridge types

Encar-matched color system and rule category mapping ported from
Chrome Extension sidepanel.

Confidence: high
Scope-risk: narrow"
```

---

### Task 5: Mobile Orchestrator

**Files:**
- Create: `webview-bundle/src/orchestrate-mobile.ts`
- Test: `webview-bundle/tests/orchestrate-mobile.test.ts`

> The original `parsers/encar/index.ts` imports `FetchStatus` from outside `src/core/` (Chrome Extension background script). This mobile orchestrator reimplements the same logic using only `@core/` imports.

- [ ] **Step 1: Write the failing test**

```typescript
// webview-bundle/tests/orchestrate-mobile.test.ts
import { describe, it, expect } from 'vitest';
import { orchestrateMobile } from '../src/orchestrate-mobile';

describe('orchestrateMobile', () => {
  it('produces a valid RuleReport from minimal preloaded state', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/41623743',
      carId: '41623743',
      preloadedState: {
        cars: {
          base: {
            category: {
              manufacturerName: '현대',
              modelName: '투싼',
              yearMonth: '202201',
              domestic: true,
            },
            advertisement: { price: 2500 },
            spec: { mileage: 35000 },
            contact: { userType: 'DEALER' },
          },
          detailFlags: {
            isInsuranceExist: true,
            isHistoryView: true,
            isDiagnosisExist: false,
            isDealer: true,
          },
        },
      },
    });

    expect(result.report.score).toBeGreaterThanOrEqual(0);
    expect(result.report.score).toBeLessThanOrEqual(100);
    expect(result.report.verdict).toBeDefined();
    expect(result.parsed.carId).toBe('41623743');
    expect(result.parsed.source).toBe('encar');
    expect(result.facts.schemaVersion).toBe(1);
    expect(result.facts.insuranceHistoryDisclosed).toEqual({
      kind: 'value',
      value: true,
    });
  });

  it('handles record API data for R05-R10', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/123',
      carId: '123',
      preloadedState: {
        cars: {
          base: {
            category: { manufacturerName: '기아', modelName: '스포티지', yearMonth: '202101', domestic: true },
            advertisement: { price: 2000 },
            spec: { mileage: 50000 },
            contact: { userType: 'DEALER' },
          },
          detailFlags: { isInsuranceExist: true, isHistoryView: true, isDiagnosisExist: false, isDealer: true },
        },
      },
      recordJson: {
        myAccidentCnt: 0,
        otherAccidentCnt: 0,
        ownerChangeCnt: 1,
        robberCnt: 0,
        totalLossCnt: 0,
        floodTotalLossCnt: 0,
        floodPartLossCnt: 0,
        government: 0,
        business: 0,
        loan: 1,
        carNoChangeCnt: 0,
        myAccidentCost: 0,
        otherAccidentCost: 0,
      },
      httpStatus: { recordJson: 'ok' },
    });

    expect(result.facts.usageHistory).toEqual({
      kind: 'value',
      value: { rental: true, taxi: false, business: false },
    });
    const r05 = result.report.results.find((r) => r.ruleId === 'R05');
    expect(r05?.severity).toBe('killer');
  });

  it('handles missing preloaded state gracefully', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/999',
      carId: '999',
      preloadedState: null,
    });

    expect(result.report.verdict).toBe('UNKNOWN');
    expect(result.parsed.raw.base.kind).toBe('parse_failed');
  });

  it('marks personal listing diagnosis as not applicable', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/456',
      carId: '456',
      preloadedState: {
        cars: {
          base: {
            category: { manufacturerName: 'BMW', modelName: '530i', yearMonth: '201901', domestic: false },
            advertisement: { price: 3500 },
            spec: { mileage: 60000 },
            contact: { userType: 'CLIENT' },
          },
          detailFlags: { isInsuranceExist: true, isHistoryView: false, isDiagnosisExist: false, isDealer: false },
        },
      },
      httpStatus: { diagnosisJson: 'not_found', inspectionJson: 'not_found' },
    });

    expect(result.facts.hasEncarDiagnosis).toEqual({
      kind: 'parse_failed',
      reason: 'not_applicable_personal',
    });
  });
});
```

- [ ] **Step 2: Run test — verify it fails**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npx vitest run tests/orchestrate-mobile.test.ts
```

Expected: FAIL — `orchestrate-mobile` module not found.

- [ ] **Step 3: Implement orchestrate-mobile.ts**

```typescript
// webview-bundle/src/orchestrate-mobile.ts
import type { EncarParsedData } from '@core/types/ParsedData.js';
import type { ChecklistFacts } from '@core/types/ChecklistFacts.js';
import type { RuleReport } from '@core/types/RuleTypes.js';
import type { FieldStatus } from '@core/types/FieldStatus.js';
import { failed, isValue } from '@core/types/FieldStatus.js';
import { extractBase, extractDetailFlags } from '@core/parsers/encar/state.js';
import { parseRecordApi } from '@core/parsers/encar/api-record.js';
import { parseDiagnosisApi } from '@core/parsers/encar/api-diagnosis.js';
import { parseInspectionApi } from '@core/parsers/encar/api-inspection.js';
import { encarToFacts } from '@core/bridge/encar-to-facts.js';
import { evaluate } from '@core/rules/index.js';

type FetchStatus = 'ok' | 'not_found' | 'unauthorized' | 'error' | 'skipped';

export interface MobileOrchestratorInput {
  url: string;
  carId: string;
  preloadedState: unknown;
  recordJson?: unknown;
  diagnosisJson?: unknown;
  inspectionJson?: unknown;
  httpStatus?: {
    recordJson?: FetchStatus;
    diagnosisJson?: FetchStatus;
    inspectionJson?: FetchStatus;
  };
}

export interface MobileOrchestratorResult {
  parsed: EncarParsedData;
  facts: ChecklistFacts;
  report: RuleReport;
}

const reasonForStatus = (status: FetchStatus): string => {
  switch (status) {
    case 'not_found': return 'no_report_for_personal';
    case 'unauthorized': return 'login_required';
    case 'error': return 'api_fetch_error';
    case 'skipped': return 'not_fetched';
    default: return 'not_fetched';
  }
};

const resolveApi = <T>(
  json: unknown,
  status: FetchStatus | undefined,
  parse: (j: unknown) => FieldStatus<T>,
): FieldStatus<T> => {
  if (status && status !== 'ok' && status !== 'skipped') {
    return failed<T>(reasonForStatus(status));
  }
  if (json === undefined || json === null) {
    return failed<T>(status ? reasonForStatus(status) : 'not_fetched');
  }
  return parse(json);
};

export const orchestrateMobile = (
  input: MobileOrchestratorInput,
): MobileOrchestratorResult => {
  const root = (input.preloadedState ?? {}) as {
    __PRELOADED_STATE__?: unknown;
    __NEXT_DATA__?: unknown;
    cars?: unknown;
  };

  const stateRoot = root.cars
    ? { __PRELOADED_STATE__: { cars: root.cars } }
    : (root as Parameters<typeof extractBase>[0]);

  const base = extractBase(stateRoot);
  const detailFlags = extractDetailFlags(stateRoot);

  const recordApi = resolveApi(
    input.recordJson,
    input.httpStatus?.recordJson,
    parseRecordApi,
  );
  const diagnosisApi = resolveApi(
    input.diagnosisJson,
    input.httpStatus?.diagnosisJson,
    parseDiagnosisApi,
  );
  const inspectionApi = resolveApi(
    input.inspectionJson,
    input.httpStatus?.inspectionJson,
    parseInspectionApi,
  );

  const baseValue = isValue(base) ? base.value : undefined;
  const vehicleId =
    baseValue && typeof (baseValue as { vehicleId?: unknown }).vehicleId === 'number'
      ? (baseValue as { vehicleId?: number }).vehicleId
      : undefined;
  const vehicleNo = baseValue?.vehicleNo;

  const parsed: EncarParsedData = {
    schemaVersion: 1,
    source: 'encar',
    url: input.url,
    carId: input.carId,
    ...(vehicleId !== undefined ? { vehicleId } : {}),
    ...(vehicleNo !== undefined ? { vehicleNo } : {}),
    fetchedAt: Date.now(),
    loginState: 'unknown',
    raw: { base, detailFlags, recordApi, diagnosisApi, inspectionApi },
  };

  const facts = encarToFacts(parsed);
  const report = evaluate(facts);

  return { parsed, facts, report };
};
```

- [ ] **Step 4: Run test — verify it passes**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npx vitest run tests/orchestrate-mobile.test.ts
```

Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add webview-bundle/src/orchestrate-mobile.ts webview-bundle/tests/orchestrate-mobile.test.ts
git commit -m "feat: mobile orchestrator — parsers → bridge → rules pipeline

Reimplements the Chrome Extension orchestrator using only @core/ imports,
avoiding the background-script FetchStatus import chain. Same logic:
resolveApi for HTTP status handling, extractBase/extractDetailFlags for
preloaded state, encarToFacts bridge, evaluate() rules engine.

Rejected: Import original orchestrator via Vite alias shim | imports chrome extension background script
Confidence: high
Scope-risk: narrow"
```

---

### Task 6: Android Bridge Adapter

**Files:**
- Create: `webview-bundle/src/android-bridge.ts`
- Test: `webview-bundle/tests/android-bridge.test.ts`

- [ ] **Step 1: Write the failing test**

```typescript
// webview-bundle/tests/android-bridge.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AndroidBridge } from '../src/android-bridge';

describe('AndroidBridge', () => {
  beforeEach(() => {
    (window as Record<string, unknown>).Android = undefined;
  });

  it('detects native bridge availability', () => {
    expect(AndroidBridge.isAvailable()).toBe(false);

    (window as Record<string, unknown>).Android = {
      saveCar: vi.fn(),
      closeOverlay: vi.fn(),
      showToast: vi.fn(),
      getClipboardUrl: vi.fn(),
    };

    expect(AndroidBridge.isAvailable()).toBe(true);
  });

  it('calls native saveCar with JSON', () => {
    const mockSave = vi.fn();
    (window as Record<string, unknown>).Android = {
      saveCar: mockSave,
      closeOverlay: vi.fn(),
      showToast: vi.fn(),
      getClipboardUrl: vi.fn(),
    };

    AndroidBridge.saveCar({ carId: '123', score: 85 });
    expect(mockSave).toHaveBeenCalledWith(
      JSON.stringify({ carId: '123', score: 85 }),
    );
  });

  it('calls closeOverlay', () => {
    const mockClose = vi.fn();
    (window as Record<string, unknown>).Android = {
      saveCar: vi.fn(),
      closeOverlay: mockClose,
      showToast: vi.fn(),
      getClipboardUrl: vi.fn(),
    };

    AndroidBridge.closeOverlay();
    expect(mockClose).toHaveBeenCalled();
  });

  it('gracefully handles missing bridge', () => {
    expect(() => AndroidBridge.closeOverlay()).not.toThrow();
    expect(() => AndroidBridge.saveCar({ carId: '1' })).not.toThrow();
  });
});
```

- [ ] **Step 2: Run test — verify it fails**

```bash
npx vitest run tests/android-bridge.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement**

```typescript
// webview-bundle/src/android-bridge.ts
export const AndroidBridge = {
  isAvailable(): boolean {
    return typeof window.Android !== 'undefined';
  },

  saveCar(data: Record<string, unknown>): void {
    window.Android?.saveCar(JSON.stringify(data));
  },

  closeOverlay(): void {
    window.Android?.closeOverlay();
  },

  showToast(message: string): void {
    window.Android?.showToast(message);
  },

  getClipboardUrl(): string | null {
    return window.Android?.getClipboardUrl() ?? null;
  },
} as const;
```

- [ ] **Step 4: Run test — verify it passes**

```bash
npx vitest run tests/android-bridge.test.ts
```

Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add webview-bundle/src/android-bridge.ts webview-bundle/tests/android-bridge.test.ts
git commit -m "feat: Android bridge adapter for WebView ↔ Native communication

Thin wrapper around window.Android JavascriptInterface. Gracefully
no-ops when running outside Android WebView (browser dev mode).

Confidence: high
Scope-risk: narrow"
```

---

### Task 7: Overlay UI Components

**Files:**
- Create: `webview-bundle/src/overlay/RuleLine.tsx`
- Create: `webview-bundle/src/overlay/SummaryRow.tsx`
- Create: `webview-bundle/src/overlay/ScoreCard.tsx`
- Create: `webview-bundle/src/overlay/CategoryAccordion.tsx`

- [ ] **Step 1: Create RuleLine component**

```tsx
// webview-bundle/src/overlay/RuleLine.tsx
import type { RuleResult } from '@core/types/RuleTypes.js';
import { RULE_META } from '../rule-meta';
import { color } from '../tokens';

const severityStyle: Record<string, { bg: string; text: string; label: string }> = {
  pass: { bg: color.successBg, text: color.success, label: '통과' },
  warn: { bg: color.warningBg, text: color.warning, label: '주의' },
  fail: { bg: color.dangerBg, text: color.danger, label: '위험' },
  killer: { bg: color.dangerBg, text: color.danger, label: '위험' },
  unknown: { bg: color.unknownBg, text: color.textSecondary, label: '미확인' },
};

export function RuleLine({ result }: { result: RuleResult }) {
  const meta = RULE_META[result.ruleId];
  const style = severityStyle[result.severity] ?? severityStyle.unknown;

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      padding: '10px 0',
      borderBottom: `1px solid ${color.border}`,
      gap: '10px',
    }}>
      <span style={{
        width: '28px',
        height: '28px',
        borderRadius: '50%',
        background: color.primary,
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '11px',
        fontWeight: 700,
        flexShrink: 0,
      }}>
        {result.ruleId.replace('R0', '').replace('R', '')}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: '14px', fontWeight: 500, color: color.textPrimary }}>
          {meta?.icon} {result.title}
        </div>
        <div style={{
          fontSize: '12px',
          color: color.textSecondary,
          marginTop: '2px',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}>
          {result.message}
        </div>
      </div>
      <span style={{
        padding: '3px 8px',
        borderRadius: '4px',
        background: style.bg,
        color: style.text,
        fontSize: '11px',
        fontWeight: 600,
        flexShrink: 0,
      }}>
        {style.label}
      </span>
    </div>
  );
}
```

- [ ] **Step 2: Create SummaryRow component**

```tsx
// webview-bundle/src/overlay/SummaryRow.tsx
import type { RuleResult } from '@core/types/RuleTypes.js';
import { color } from '../tokens';

interface SummaryRowProps {
  results: RuleResult[];
}

export function SummaryRow({ results }: SummaryRowProps) {
  const danger = results.filter((r) => r.severity === 'killer' || r.severity === 'fail').length;
  const caution = results.filter((r) => r.severity === 'warn').length;
  const pass = results.filter((r) => r.severity === 'pass').length;
  const unknown = results.filter((r) => r.severity === 'unknown').length;

  const cells: Array<{ label: string; count: number; bg: string; text: string }> = [
    { label: '위험', count: danger, bg: color.dangerBg, text: color.danger },
    { label: '주의', count: caution, bg: color.warningBg, text: color.warning },
    { label: '통과', count: pass, bg: color.successBg, text: color.success },
    { label: '미확인', count: unknown, bg: color.unknownBg, text: color.textSecondary },
  ];

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(4, 1fr)',
      gap: '8px',
      padding: '12px 16px',
    }}>
      {cells.map((cell) => (
        <div key={cell.label} style={{
          background: cell.bg,
          borderRadius: '8px',
          padding: '10px 8px',
          textAlign: 'center',
        }}>
          <div style={{ fontSize: '20px', fontWeight: 700, color: cell.text }}>
            {cell.count}
          </div>
          <div style={{ fontSize: '11px', color: cell.text, marginTop: '2px' }}>
            {cell.label}
          </div>
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 3: Create ScoreCard component**

```tsx
// webview-bundle/src/overlay/ScoreCard.tsx
import type { Verdict } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';
import { color } from '../tokens';

interface ScoreCardProps {
  score: number;
  verdict: Verdict;
  carBase: EncarCarBase | null;
}

const verdictDisplay: Record<Verdict, { label: string; bg: string; text: string }> = {
  OK: { label: 'OK', bg: color.successBg, text: color.success },
  CAUTION: { label: 'CAUTION', bg: color.warningBg, text: color.warning },
  NEVER: { label: 'NEVER', bg: color.dangerBg, text: color.danger },
  UNKNOWN: { label: 'UNKNOWN', bg: color.unknownBg, text: color.textSecondary },
};

const formatPrice = (price: number): string => {
  if (price >= 10000) return `${(price / 10000).toFixed(1)}억`;
  return `${price}만원`;
};

export function ScoreCard({ score, verdict, carBase }: ScoreCardProps) {
  const v = verdictDisplay[verdict];
  const title = carBase
    ? `${carBase.category.yearMonth?.slice(0, 4) ?? ''} ${carBase.category.manufacturerName} ${carBase.category.modelName}`
    : '차량 정보 없음';
  const specs = carBase
    ? [
        carBase.spec.mileage ? `${(carBase.spec.mileage / 10000).toFixed(1)}만km` : null,
        carBase.category.yearMonth ? `${carBase.category.yearMonth.slice(0, 4)}년` : null,
        carBase.advertisement.price ? formatPrice(carBase.advertisement.price) : null,
      ].filter(Boolean).join(' · ')
    : '';

  return (
    <div style={{
      background: color.primary,
      borderRadius: '12px',
      padding: '20px 16px',
      margin: '0 16px',
      color: '#ffffff',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontSize: '16px', fontWeight: 600 }}>{title}</div>
          <div style={{ fontSize: '12px', opacity: 0.8, marginTop: '4px' }}>{specs}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: '32px', fontWeight: 800, lineHeight: 1 }}>{score}</div>
          <div style={{ fontSize: '11px', opacity: 0.7 }}>/ 100</div>
        </div>
      </div>
      <div style={{ marginTop: '12px' }}>
        <span style={{
          display: 'inline-block',
          padding: '4px 12px',
          borderRadius: '4px',
          background: v.bg,
          color: v.text,
          fontSize: '13px',
          fontWeight: 700,
        }}>
          {v.label}
        </span>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Create CategoryAccordion component**

```tsx
// webview-bundle/src/overlay/CategoryAccordion.tsx
import { useState } from 'react';
import type { RuleResult } from '@core/types/RuleTypes.js';
import type { Category } from '../rule-meta';
import { RULE_META } from '../rule-meta';
import { RuleLine } from './RuleLine';
import { color } from '../tokens';

interface CategoryAccordionProps {
  category: Category;
  results: RuleResult[];
}

export function CategoryAccordion({ category, results }: CategoryAccordionProps) {
  const hasDanger = results.some((r) => r.severity === 'killer' || r.severity === 'fail');
  const hasWarning = results.some((r) => r.severity === 'warn');
  const [open, setOpen] = useState(hasDanger || hasWarning);

  const passCount = results.filter((r) => r.severity === 'pass').length;
  const borderColor = hasDanger ? color.danger : hasWarning ? color.warning : 'transparent';

  return (
    <div style={{
      background: color.surface,
      borderRadius: '12px',
      margin: '0 16px 8px',
      borderLeft: `3px solid ${borderColor}`,
      boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
      overflow: 'hidden',
    }}>
      <button
        onClick={() => setOpen((prev) => !prev)}
        style={{
          width: '100%',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '14px 16px',
          border: 'none',
          background: 'transparent',
          cursor: 'pointer',
          fontSize: '15px',
          fontWeight: 600,
          color: color.textPrimary,
        }}
      >
        <span>{category}</span>
        <span style={{ fontSize: '13px', color: color.textSecondary }}>
          {passCount}/{results.length} {open ? '▲' : '▼'}
        </span>
      </button>
      {open && (
        <div style={{ padding: '0 16px 8px' }}>
          {results.map((r) => (
            <RuleLine key={r.ruleId} result={r} />
          ))}
        </div>
      )}
    </div>
  );
}

export function groupByCategory(results: RuleResult[]): Array<{ category: Category; results: RuleResult[] }> {
  const map = new Map<Category, RuleResult[]>();
  for (const r of results) {
    const meta = RULE_META[r.ruleId];
    if (!meta) continue;
    const list = map.get(meta.category) ?? [];
    list.push(r);
    map.set(meta.category, list);
  }
  return Array.from(map.entries()).map(([category, results]) => ({ category, results }));
}
```

- [ ] **Step 5: Verify TypeScript compiles**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add webview-bundle/src/overlay/
git commit -m "feat: overlay UI components — ScoreCard, SummaryRow, CategoryAccordion, RuleLine

Encar-matched design system: soft neutral background, white cards,
blue primary accent, Material-inspired severity colors. Accordion
auto-opens categories with danger/warning results.

Confidence: high
Scope-risk: narrow"
```

---

### Task 8: Overlay Page + Entry Point + Build

**Files:**
- Create: `webview-bundle/src/overlay/OverlayPage.tsx`
- Modify: `webview-bundle/src/App.tsx`
- Modify: `webview-bundle/src/main.tsx`
- Test: `webview-bundle/tests/overlay/OverlayPage.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
// webview-bundle/tests/overlay/OverlayPage.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OverlayPage } from '../../src/overlay/OverlayPage';
import type { RuleReport } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';

const mockReport: RuleReport = {
  verdict: 'CAUTION',
  score: 72,
  results: [
    { ruleId: 'R01', title: '보험이력 공개', severity: 'pass', message: '딜러가 보험이력을 공개했습니다', evidence: [], acknowledgeable: false },
    { ruleId: 'R05', title: '렌트/택시 이력', severity: 'warn', message: '⚠ 관용 이력', evidence: [], acknowledgeable: false },
  ],
  killers: [],
  warns: [
    { ruleId: 'R05', title: '렌트/택시 이력', severity: 'warn', message: '⚠ 관용 이력', evidence: [], acknowledgeable: false },
  ],
};

const mockBase: EncarCarBase = {
  category: { manufacturerName: '현대', modelName: '투싼', yearMonth: '202201', domestic: true },
  advertisement: { price: 2500, preVerified: false, trust: [] },
  spec: { mileage: 35000 },
};

describe('OverlayPage', () => {
  it('renders score and verdict', () => {
    render(<OverlayPage report={mockReport} carBase={mockBase} onClose={() => {}} onSave={() => {}} />);
    expect(screen.getByText('72')).toBeInTheDocument();
    expect(screen.getByText('CAUTION')).toBeInTheDocument();
  });

  it('renders vehicle title', () => {
    render(<OverlayPage report={mockReport} carBase={mockBase} onClose={() => {}} onSave={() => {}} />);
    expect(screen.getByText(/현대 투싼/)).toBeInTheDocument();
  });

  it('renders summary counts', () => {
    render(<OverlayPage report={mockReport} carBase={mockBase} onClose={() => {}} onSave={() => {}} />);
    expect(screen.getByText('통과')).toBeInTheDocument();
    expect(screen.getByText('주의')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — verify it fails**

```bash
npx vitest run tests/overlay/OverlayPage.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 3: Create OverlayPage**

```tsx
// webview-bundle/src/overlay/OverlayPage.tsx
import type { RuleReport } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';
import { ScoreCard } from './ScoreCard';
import { SummaryRow } from './SummaryRow';
import { CategoryAccordion, groupByCategory } from './CategoryAccordion';
import { CATEGORY_ORDER } from '../rule-meta';
import { color } from '../tokens';

interface OverlayPageProps {
  report: RuleReport;
  carBase: EncarCarBase | null;
  onClose: () => void;
  onSave: () => void;
}

export function OverlayPage({ report, carBase, onClose, onSave }: OverlayPageProps) {
  const groups = groupByCategory(report.results);
  const sortedGroups = [...groups].sort(
    (a, b) => CATEGORY_ORDER.indexOf(a.category) - CATEGORY_ORDER.indexOf(b.category),
  );

  return (
    <div style={{
      minHeight: '100vh',
      background: color.background,
      paddingBottom: '80px',
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '16px',
        background: color.surface,
      }}>
        <div style={{ fontSize: '17px', fontWeight: 700, color: color.textPrimary }}>
          점검 리포트
        </div>
        <button
          onClick={onClose}
          style={{
            border: 'none',
            background: 'transparent',
            fontSize: '24px',
            color: color.textSecondary,
            cursor: 'pointer',
            padding: '4px',
          }}
        >
          ✕
        </button>
      </div>

      {/* Score Card */}
      <div style={{ paddingTop: '12px' }}>
        <ScoreCard score={report.score} verdict={report.verdict} carBase={carBase} />
      </div>

      {/* Summary */}
      <SummaryRow results={report.results} />

      {/* Category Accordions */}
      <div style={{ paddingTop: '4px' }}>
        {sortedGroups.map(({ category, results }) => (
          <CategoryAccordion key={category} category={category} results={results} />
        ))}
      </div>

      {/* Save Button */}
      <div style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        padding: '12px 16px',
        background: color.surface,
        borderTop: `1px solid ${color.border}`,
      }}>
        <button
          onClick={onSave}
          style={{
            width: '100%',
            padding: '14px',
            background: color.primary,
            color: '#fff',
            border: 'none',
            borderRadius: '12px',
            fontSize: '16px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          저장하기
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Create App.tsx (root with data receiver)**

```tsx
// webview-bundle/src/App.tsx
import { useState, useEffect, useCallback } from 'react';
import type { RuleReport } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';
import { isValue } from '@core/types/FieldStatus.js';
import { orchestrateMobile } from './orchestrate-mobile';
import type { MobileOrchestratorInput } from './orchestrate-mobile';
import { OverlayPage } from './overlay/OverlayPage';
import { AndroidBridge } from './android-bridge';
import { color } from './tokens';

interface EvalState {
  report: RuleReport;
  carBase: EncarCarBase | null;
  carId: string;
  url: string;
  rawInput: MobileOrchestratorInput;
}

export function App() {
  const [state, setState] = useState<EvalState | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    window.receiveEncarData = (json: string) => {
      setLoading(true);
      setError(null);
      try {
        const input: MobileOrchestratorInput = JSON.parse(json);
        const { parsed, facts, report } = orchestrateMobile(input);
        const carBase = isValue(parsed.raw.base) ? parsed.raw.base.value : null;
        setState({ report, carBase, carId: input.carId, url: input.url, rawInput: input });
      } catch (e) {
        setError(e instanceof Error ? e.message : '평가 중 오류가 발생했습니다');
      } finally {
        setLoading(false);
      }
    };

    window.receiveError = (json: string) => {
      try {
        const { message } = JSON.parse(json);
        setError(message ?? '알 수 없는 오류');
      } catch {
        setError('알 수 없는 오류');
      }
      setLoading(false);
    };
  }, []);

  const handleClose = useCallback(() => {
    AndroidBridge.closeOverlay();
  }, []);

  const handleSave = useCallback(() => {
    if (!state) return;
    AndroidBridge.saveCar({
      carId: state.carId,
      url: state.url,
      title: state.carBase
        ? `${state.carBase.category.yearMonth?.slice(0, 4) ?? ''} ${state.carBase.category.manufacturerName} ${state.carBase.category.modelName}`
        : '차량 정보 없음',
      year: state.carBase?.category.yearMonth ? parseInt(state.carBase.category.yearMonth.slice(0, 4)) : null,
      mileageKm: state.carBase?.spec.mileage ?? null,
      priceWon: state.carBase?.advertisement.price ? state.carBase.advertisement.price * 10000 : null,
      fuelType: state.carBase?.spec.fuelName ?? null,
      score: state.report.score,
      verdict: state.report.verdict,
      dangerCount: state.report.results.filter((r) => r.severity === 'killer' || r.severity === 'fail').length,
      cautionCount: state.report.warns.length,
      passCount: state.report.results.filter((r) => r.severity === 'pass').length,
      unknownCount: state.report.results.filter((r) => r.severity === 'unknown').length,
      rawJson: JSON.stringify(state.rawInput),
    });
    AndroidBridge.showToast('저장되었습니다');
  }, [state]);

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: color.background,
        gap: '12px',
      }}>
        <div style={{
          width: '36px',
          height: '36px',
          border: `3px solid ${color.border}`,
          borderTop: `3px solid ${color.primary}`,
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }} />
        <div style={{ fontSize: '14px', color: color.textSecondary }}>분석 중...</div>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: color.background,
        padding: '24px',
        textAlign: 'center',
      }}>
        <div style={{ fontSize: '36px', marginBottom: '12px' }}>⚠️</div>
        <div style={{ fontSize: '16px', fontWeight: 600, color: color.textPrimary }}>{error}</div>
        <button
          onClick={handleClose}
          style={{
            marginTop: '20px',
            padding: '10px 24px',
            background: color.primary,
            color: '#fff',
            border: 'none',
            borderRadius: '8px',
            fontSize: '14px',
            cursor: 'pointer',
          }}
        >
          닫기
        </button>
      </div>
    );
  }

  if (!state) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: color.background,
        color: color.textSecondary,
        fontSize: '14px',
      }}>
        데이터 대기 중...
      </div>
    );
  }

  return (
    <OverlayPage
      report={state.report}
      carBase={state.carBase}
      onClose={handleClose}
      onSave={handleSave}
    />
  );
}
```

- [ ] **Step 5: Update main.tsx**

```tsx
// webview-bundle/src/main.tsx
import { createRoot } from 'react-dom/client';
import { App } from './App';

createRoot(document.getElementById('root')!).render(<App />);
```

- [ ] **Step 6: Run test — verify it passes**

```bash
npx vitest run tests/overlay/OverlayPage.test.tsx
```

Expected: 3 tests PASS.

- [ ] **Step 7: Build and copy to Android assets**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npm run build && npm run copy-to-assets
```

Verify:

```bash
ls ../app/src/main/assets/eval-ui/
# Expected: index.html, assets/
```

- [ ] **Step 8: Commit**

```bash
git add webview-bundle/src/App.tsx webview-bundle/src/main.tsx webview-bundle/src/overlay/OverlayPage.tsx webview-bundle/tests/overlay/
git commit -m "feat: overlay page + app entry point with data receiver pipeline

App registers window.receiveEncarData callback. When Kotlin calls it
with raw JSON, the pipeline runs: orchestrate → render OverlayPage.
Loading spinner, error state, and close/save actions via AndroidBridge.

Confidence: high
Scope-risk: narrow"
```

---

## Part C: Android Core

### Task 9: EncarUrl Utility

**Files:**
- Create: `app/src/main/kotlin/com/daksin/autoverdict/util/EncarUrl.kt`
- Test: `app/src/test/kotlin/com/daksin/autoverdict/util/EncarUrlTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/kotlin/com/daksin/autoverdict/util/EncarUrlTest.kt
package com.daksin.autoverdict.util

import org.junit.Assert.*
import org.junit.Test

class EncarUrlTest {

    @Test
    fun `extractCarId from standard URL`() {
        assertEquals("41623743", EncarUrl.extractCarId("https://fem.encar.com/cars/detail/41623743"))
    }

    @Test
    fun `extractCarId from URL with query params`() {
        assertEquals("41623743", EncarUrl.extractCarId("https://fem.encar.com/cars/detail/41623743?utm_source=app"))
    }

    @Test
    fun `extractCarId returns null for non-encar URL`() {
        assertNull(EncarUrl.extractCarId("https://google.com/search"))
    }

    @Test
    fun `extractCarId returns null for null input`() {
        assertNull(EncarUrl.extractCarId(null))
    }

    @Test
    fun `extractCarId returns null for empty string`() {
        assertNull(EncarUrl.extractCarId(""))
    }

    @Test
    fun `isEncarDetail returns true for valid URL`() {
        assertTrue(EncarUrl.isEncarDetail("https://fem.encar.com/cars/detail/41623743"))
    }

    @Test
    fun `isEncarDetail returns false for non-detail URL`() {
        assertFalse(EncarUrl.isEncarDetail("https://fem.encar.com/cars/list"))
    }

    @Test
    fun `buildDetailUrl creates correct URL`() {
        assertEquals(
            "https://fem.encar.com/cars/detail/41623743",
            EncarUrl.buildDetailUrl("41623743")
        )
    }
}
```

- [ ] **Step 2: Run test — verify it fails**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos
./gradlew :app:testDebugUnitTest --tests "com.daksin.autoverdict.util.EncarUrlTest" 2>&1 | tail -10
```

Expected: FAIL — class not found.

- [ ] **Step 3: Implement**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/util/EncarUrl.kt
package com.daksin.autoverdict.util

object EncarUrl {

    private val CAR_DETAIL_RE = Regex("""/cars/detail/(\d+)""")
    private const val BASE_URL = "https://fem.encar.com"

    fun extractCarId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return CAR_DETAIL_RE.find(url)?.groupValues?.get(1)
    }

    fun isEncarDetail(url: String?): Boolean {
        return extractCarId(url) != null
    }

    fun buildDetailUrl(carId: String): String {
        return "$BASE_URL/cars/detail/$carId"
    }
}
```

- [ ] **Step 4: Run test — verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.daksin.autoverdict.util.EncarUrlTest" 2>&1 | tail -10
```

Expected: all 8 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daksin/autoverdict/util/EncarUrl.kt app/src/test/kotlin/com/daksin/autoverdict/util/EncarUrlTest.kt
git commit -m "feat: EncarUrl utility — carId extraction and URL validation

Mirrors Chrome Extension's url.ts regex: /cars/detail/{digits}.

Confidence: high
Scope-risk: narrow"
```

---

### Task 10: Room Database

**Files:**
- Create: `app/src/main/kotlin/com/daksin/autoverdict/db/SavedCarEntity.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/db/SavedCarDao.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/db/CacheEntity.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/db/CacheDao.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/db/AppDatabase.kt`

- [ ] **Step 1: Create SavedCarEntity**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/db/SavedCarEntity.kt
package com.daksin.autoverdict.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cars")
data class SavedCarEntity(
    @PrimaryKey val carId: String,
    val url: String,
    val title: String,
    val year: Int?,
    val mileageKm: Int?,
    val priceWon: Long?,
    val fuelType: String?,
    val score: Int,
    val verdict: String,
    val dangerCount: Int,
    val cautionCount: Int,
    val passCount: Int,
    val unknownCount: Int,
    val rawJson: String,
    val savedAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 2: Create SavedCarDao**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/db/SavedCarDao.kt
package com.daksin.autoverdict.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCarDao {

    @Query("SELECT * FROM saved_cars ORDER BY savedAt DESC")
    fun getAllFlow(): Flow<List<SavedCarEntity>>

    @Query("SELECT * FROM saved_cars WHERE carId = :carId")
    suspend fun getByCarId(carId: String): SavedCarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(car: SavedCarEntity)

    @Query("DELETE FROM saved_cars WHERE carId = :carId")
    suspend fun deleteByCarId(carId: String)

    @Query("SELECT COUNT(*) FROM saved_cars")
    suspend fun count(): Int
}
```

- [ ] **Step 3: Create CacheEntity + CacheDao**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/db/CacheEntity.kt
package com.daksin.autoverdict.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val carId: String,
    val url: String,
    val title: String,
    val score: Int,
    val verdict: String,
    val resultJson: String,
    val rawInputJson: String,
    val cachedAt: Long,
    val expiresAt: Long,
)
```

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/db/CacheDao.kt
package com.daksin.autoverdict.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {

    @Query("SELECT * FROM cache WHERE expiresAt > :now ORDER BY cachedAt DESC")
    fun getRecentFlow(now: Long = System.currentTimeMillis()): Flow<List<CacheEntity>>

    @Query("SELECT * FROM cache WHERE carId = :carId AND expiresAt > :now")
    suspend fun getValid(carId: String, now: Long = System.currentTimeMillis()): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: CacheEntity)

    @Query("DELETE FROM cache WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cache")
    suspend fun clearAll()
}
```

- [ ] **Step 4: Create AppDatabase**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/db/AppDatabase.kt
package com.daksin.autoverdict.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedCarEntity::class, CacheEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedCarDao(): SavedCarDao
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoverdict.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

- [ ] **Step 5: Verify build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/daksin/autoverdict/db/
git commit -m "feat: Room database — SavedCar + Cache entities and DAOs

SavedCarEntity stores user-saved evaluations with structured fields
for list display + rawJson for overlay re-rendering. CacheEntity
stores analysis results with 24h TTL.

Confidence: high
Scope-risk: narrow"
```

---

## Part D: Floating Service + Overlay

### Task 11: FloatingService + Floating Button

**Files:**
- Create: `app/src/main/kotlin/com/daksin/autoverdict/floating/FloatingService.kt`
- Create: `app/src/main/res/drawable/ic_av_button.xml`

- [ ] **Step 1: Create floating button drawable**

```xml
<!-- app/src/main/res/drawable/ic_av_button.xml -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#0064FF" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step 2: Create FloatingService**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/floating/FloatingService.kt
package com.daksin.autoverdict.floating

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.R
import com.daksin.autoverdict.util.EncarUrl

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: View? = null
    private var overlayManager: OverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        showFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val urlFromIntent = intent?.getStringExtra(EXTRA_URL)
        if (urlFromIntent != null) {
            startAnalysis(urlFromIntent)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeFloatingButton()
        overlayManager?.dismiss()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, AutoVerdictApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_notification_title))
            .setContentText(getString(R.string.floating_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showFloatingButton() {
        val button = TextView(this).apply {
            text = "AV"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.ic_av_button)
            setPadding(0, 0, 0, 0)
        }

        val params = WindowManager.LayoutParams(
            dpToPx(48),
            dpToPx(48),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(200)
        }

        setupDragAndTap(button, params)
        windowManager.addView(button, params)
        floatingButton = button
    }

    private fun setupDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 25) isDragging = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) onFloatingButtonTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun onFloatingButtonTap() {
        val clipUrl = getClipboardEncarUrl()
        if (clipUrl != null) {
            startAnalysis(clipUrl)
        } else {
            android.widget.Toast.makeText(
                this,
                "엔카 매물 URL을 복사한 후 다시 눌러주세요",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startAnalysis(url: String) {
        val carId = EncarUrl.extractCarId(url) ?: return
        if (overlayManager == null) {
            val app = application as AutoVerdictApp
            overlayManager = OverlayManager(this, windowManager, app.database)
        }
        overlayManager?.show(url, carId)
    }

    private fun getClipboardEncarUrl(): String? {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        return if (EncarUrl.isEncarDetail(text)) text else null
    }

    private fun removeFloatingButton() {
        floatingButton?.let {
            windowManager.removeView(it)
            floatingButton = null
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val NOTIFICATION_ID = 1
        const val EXTRA_URL = "extra_url"
    }
}
```

- [ ] **Step 3: Verify build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL (OverlayManager not yet created — will fail; create stub first).

Create a stub `OverlayManager.kt` to pass compilation:

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/floating/OverlayManager.kt
package com.daksin.autoverdict.floating

import android.content.Context
import android.view.WindowManager
import com.daksin.autoverdict.db.AppDatabase

class OverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val database: AppDatabase,
) {
    fun show(url: String, carId: String) { /* Task 12 */ }
    fun dismiss() { /* Task 12 */ }
}
```

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/daksin/autoverdict/floating/ app/src/main/res/drawable/ic_av_button.xml
git commit -m "feat: FloatingService with draggable button and clipboard URL detection

Foreground Service with SYSTEM_ALERT_WINDOW overlay. Button is 48dp
rounded blue square, draggable, taps read clipboard for encar URL.
START_STICKY for auto-restart. OverlayManager stubbed for Task 12.

Constraint: startForeground() called immediately to avoid ANR
Confidence: high
Scope-risk: moderate"
```

---

### Task 12: Overlay Window + EvalWebView + NativeBridge

**Files:**
- Modify: `app/src/main/kotlin/com/daksin/autoverdict/floating/OverlayManager.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/webview/EvalWebView.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/webview/NativeBridge.kt`

- [ ] **Step 1: Create NativeBridge**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/webview/NativeBridge.kt
package com.daksin.autoverdict.webview

import android.webkit.JavascriptInterface
import android.widget.Toast
import com.daksin.autoverdict.db.AppDatabase
import com.daksin.autoverdict.db.SavedCarEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class NativeBridge(
    private val database: AppDatabase,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
    private val toastContext: android.content.Context,
) {
    @JavascriptInterface
    fun saveCar(json: String) {
        scope.launch(Dispatchers.IO) {
            val obj = JSONObject(json)
            val now = System.currentTimeMillis()
            val entity = SavedCarEntity(
                carId = obj.getString("carId"),
                url = obj.getString("url"),
                title = obj.optString("title", ""),
                year = if (obj.has("year") && !obj.isNull("year")) obj.getInt("year") else null,
                mileageKm = if (obj.has("mileageKm") && !obj.isNull("mileageKm")) obj.getInt("mileageKm") else null,
                priceWon = if (obj.has("priceWon") && !obj.isNull("priceWon")) obj.getLong("priceWon") else null,
                fuelType = obj.optString("fuelType", null),
                score = obj.getInt("score"),
                verdict = obj.getString("verdict"),
                dangerCount = obj.getInt("dangerCount"),
                cautionCount = obj.getInt("cautionCount"),
                passCount = obj.getInt("passCount"),
                unknownCount = obj.getInt("unknownCount"),
                rawJson = obj.optString("rawJson", json),
                savedAt = now,
                updatedAt = now,
            )
            database.savedCarDao().upsert(entity)
        }
    }

    @JavascriptInterface
    fun closeOverlay() {
        scope.launch(Dispatchers.Main) { onClose() }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(toastContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getClipboardUrl(): String? = null

    companion object {
        const val BRIDGE_NAME = "Android"
    }
}
```

- [ ] **Step 2: Create EvalWebView**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/webview/EvalWebView.kt
package com.daksin.autoverdict.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class EvalWebView(context: Context) {

    val webView: WebView = WebView(context)

    init {
        setup()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setup() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.webViewClient = WebViewClient()
    }

    fun addBridge(bridge: NativeBridge) {
        webView.addJavascriptInterface(bridge, NativeBridge.BRIDGE_NAME)
    }

    fun loadEvalUi() {
        webView.loadUrl("file:///android_asset/eval-ui/index.html")
    }

    fun sendData(json: String) {
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        webView.evaluateJavascript("window.receiveEncarData?.('$escaped')", null)
    }

    fun sendError(message: String) {
        val json = """{"message":"${message.replace("\"", "\\\"")}"}"""
        val escaped = json.replace("'", "\\'")
        webView.evaluateJavascript("window.receiveError?.('$escaped')", null)
    }

    fun destroy() {
        webView.removeJavascriptInterface(NativeBridge.BRIDGE_NAME)
        webView.destroy()
    }
}
```

- [ ] **Step 3: Implement OverlayManager**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/floating/OverlayManager.kt
package com.daksin.autoverdict.floating

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import com.daksin.autoverdict.collector.CollectorWebView
import com.daksin.autoverdict.db.AppDatabase
import com.daksin.autoverdict.db.CacheEntity
import com.daksin.autoverdict.util.EncarUrl
import com.daksin.autoverdict.webview.EvalWebView
import com.daksin.autoverdict.webview.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val database: AppDatabase,
) {
    private var overlayView: FrameLayout? = null
    private var evalWebView: EvalWebView? = null
    private var collector: CollectorWebView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val overlayParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    fun show(url: String, carId: String) {
        scope.launch {
            val cached = database.cacheDao().getValid(carId)
            if (cached != null) {
                showOverlay()
                evalWebView?.sendData(cached.rawInputJson)
                return@launch
            }

            showOverlay()

            if (collector == null) {
                collector = CollectorWebView(context)
            }
            collector?.collect(url, carId) { result ->
                scope.launch(Dispatchers.Main) {
                    when (result) {
                        is CollectorWebView.Result.Success -> {
                            evalWebView?.sendData(result.json)
                            cacheResult(carId, url, result.json)
                        }
                        is CollectorWebView.Result.Error -> {
                            evalWebView?.sendError(result.message)
                        }
                    }
                }
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val container = FrameLayout(context)
        val eval = EvalWebView(context)
        val bridge = NativeBridge(database, scope, onClose = { dismiss() }, toastContext = context)
        eval.addBridge(bridge)
        eval.loadEvalUi()
        container.addView(eval.webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        windowManager.addView(container, overlayParams)
        overlayView = container
        evalWebView = eval
    }

    fun dismiss() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        evalWebView?.destroy()
        evalWebView = null
    }

    fun destroy() {
        dismiss()
        collector?.destroy()
        collector = null
        scope.cancel()
    }

    private fun cacheResult(carId: String, url: String, rawInputJson: String) {
        scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val ttl = 24 * 60 * 60 * 1000L
            database.cacheDao().upsert(
                CacheEntity(
                    carId = carId,
                    url = url,
                    title = "",
                    score = 0,
                    verdict = "",
                    resultJson = "",
                    rawInputJson = rawInputJson,
                    cachedAt = now,
                    expiresAt = now + ttl,
                )
            )
        }
    }
}
```

- [ ] **Step 4: Verify build** (CollectorWebView stub needed)

Create stub:

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/collector/CollectorWebView.kt
package com.daksin.autoverdict.collector

import android.content.Context

class CollectorWebView(private val context: Context) {

    sealed class Result {
        data class Success(val json: String) : Result()
        data class Error(val message: String) : Result()
    }

    fun collect(url: String, carId: String, callback: (Result) -> Unit) {
        /* Task 13 */
    }

    fun destroy() { /* Task 13 */ }
}
```

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daksin/autoverdict/webview/ app/src/main/kotlin/com/daksin/autoverdict/floating/OverlayManager.kt app/src/main/kotlin/com/daksin/autoverdict/collector/
git commit -m "feat: overlay window with EvalWebView + NativeBridge

OverlayManager creates full-screen overlay, loads eval-ui WebView,
and wires NativeBridge for save/close/toast. Checks Room cache before
collecting. CollectorWebView stubbed for Task 13.

Constraint: TYPE_APPLICATION_OVERLAY requires SYSTEM_ALERT_WINDOW permission
Directive: EvalWebView.sendData escapes JSON for JS injection — do not simplify
Confidence: high
Scope-risk: moderate"
```

---

## Part E: Data Collection

### Task 13: CollectorWebView (Hidden WebView)

**Files:**
- Modify: `app/src/main/kotlin/com/daksin/autoverdict/collector/CollectorWebView.kt`

> This is the critical component that loads the Encar page in a hidden WebView, executes JavaScript to extract `__PRELOADED_STATE__`, then makes 3 API calls from within the page context (same-origin cookies included automatically).

- [ ] **Step 1: Implement CollectorWebView**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/collector/CollectorWebView.kt
package com.daksin.autoverdict.collector

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.daksin.autoverdict.util.EncarUrl
import org.json.JSONObject

class CollectorWebView(context: Context) {

    sealed class Result {
        data class Success(val json: String) : Result()
        data class Error(val message: String) : Result()
    }

    private val webView: WebView = WebView(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    init {
        setup()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setup() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = webView.settings.userAgentString.replace(
                "; wv",
                ""
            )
        }
        webView.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
    }

    fun collect(url: String, carId: String, callback: (Result) -> Unit) {
        cancelTimeout()
        val fullUrl = if (url.startsWith("http")) url else EncarUrl.buildDetailUrl(carId)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                super.onPageFinished(view, loadedUrl)
                extractPreloadedState(carId, callback)
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                cancelTimeout()
                callback(Result.Error("페이지 로드 실패: $description"))
            }
        }

        timeoutRunnable = Runnable {
            callback(Result.Error("시간 초과 — 네트워크를 확인하세요"))
            webView.stopLoading()
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT_MS)

        webView.loadUrl(fullUrl)
    }

    private fun extractPreloadedState(carId: String, callback: (Result) -> Unit) {
        val js = """
        (function() {
            try {
                var state = window.__PRELOADED_STATE__;
                if (!state || !state.cars || !state.cars.base || Object.keys(state.cars.base).length === 0) {
                    return JSON.stringify({ error: 'empty_state' });
                }
                return JSON.stringify({
                    ok: true,
                    cars: state.cars
                });
            } catch(e) {
                return JSON.stringify({ error: e.message });
            }
        })();
        """.trimIndent()

        webView.evaluateJavascript(js) { raw ->
            val parsed = raw?.removeSurrounding("\"")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")
            if (parsed == null || parsed == "null") {
                cancelTimeout()
                callback(Result.Error("페이지 데이터 추출 실패"))
                return@evaluateJavascript
            }

            try {
                val obj = JSONObject(parsed)
                if (obj.has("error")) {
                    retryExtraction(carId, callback, attempt = 1)
                    return@evaluateJavascript
                }

                val carsJson = obj.getJSONObject("cars")
                fetchApis(carId, carsJson, callback)
            } catch (e: Exception) {
                cancelTimeout()
                callback(Result.Error("데이터 파싱 오류: ${e.message}"))
            }
        }
    }

    private fun retryExtraction(carId: String, callback: (Result) -> Unit, attempt: Int) {
        if (attempt >= MAX_RETRIES) {
            cancelTimeout()
            callback(Result.Error("매물 데이터를 찾을 수 없습니다"))
            return
        }
        handler.postDelayed({
            extractPreloadedState(carId, callback)
        }, RETRY_DELAY_MS)
    }

    private fun fetchApis(carId: String, carsJson: JSONObject, callback: (Result) -> Unit) {
        val base = carsJson.optJSONObject("base")
        val vehicleId = base?.optInt("vehicleId", 0) ?: 0
        val vehicleNo = base?.optString("vehicleNo", "") ?: ""

        if (vehicleId == 0 || vehicleNo.isEmpty()) {
            cancelTimeout()
            val result = buildResultJson(carId, carsJson, null, null, null, emptyMap())
            callback(Result.Success(result))
            return
        }

        val apiJs = """
        (function() {
            var vid = $vehicleId;
            var vno = '$vehicleNo';
            var base = 'https://api.encar.com/v1/readside';
            var results = {};
            var statuses = {};

            function fetchApi(name, url) {
                return fetch(url, { credentials: 'include' })
                    .then(function(r) {
                        statuses[name] = r.ok ? 'ok' : (r.status === 404 ? 'not_found' : 'error');
                        return r.ok ? r.json() : null;
                    })
                    .then(function(data) { results[name] = data; })
                    .catch(function() { statuses[name] = 'error'; results[name] = null; });
            }

            return Promise.all([
                fetchApi('recordJson', base + '/record/vehicle/' + vid + '/open?vehicleNo=' + encodeURIComponent(vno)),
                fetchApi('diagnosisJson', base + '/diagnosis/vehicle/' + vid),
                fetchApi('inspectionJson', base + '/inspection/vehicle/' + vid)
            ]).then(function() {
                return JSON.stringify({ results: results, statuses: statuses });
            });
        })();
        """.trimIndent()

        webView.evaluateJavascript(apiJs) { raw ->
            cancelTimeout()
            val parsed = raw?.removeSurrounding("\"")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")
            try {
                val apiResponse = if (parsed != null && parsed != "null") JSONObject(parsed) else null
                val results = apiResponse?.optJSONObject("results")
                val statuses = apiResponse?.optJSONObject("statuses")

                val httpStatus = mutableMapOf<String, String>()
                statuses?.keys()?.forEach { key ->
                    httpStatus[key] = statuses.getString(key)
                }

                val result = buildResultJson(
                    carId,
                    carsJson,
                    results?.optJSONObject("recordJson")?.toString(),
                    results?.optJSONObject("diagnosisJson")?.toString(),
                    results?.optJSONObject("inspectionJson")?.toString(),
                    httpStatus,
                )
                callback(Result.Success(result))
            } catch (e: Exception) {
                val result = buildResultJson(carId, carsJson, null, null, null, emptyMap())
                callback(Result.Success(result))
            }

            webView.loadUrl("about:blank")
        }
    }

    private fun buildResultJson(
        carId: String,
        carsJson: JSONObject,
        recordJson: String?,
        diagnosisJson: String?,
        inspectionJson: String?,
        httpStatus: Map<String, String>,
    ): String {
        val obj = JSONObject().apply {
            put("url", EncarUrl.buildDetailUrl(carId))
            put("carId", carId)
            put("preloadedState", JSONObject().put("cars", carsJson))
            if (recordJson != null) put("recordJson", JSONObject(recordJson))
            if (diagnosisJson != null) put("diagnosisJson", JSONObject(diagnosisJson))
            if (inspectionJson != null) put("inspectionJson", JSONObject(inspectionJson))
            if (httpStatus.isNotEmpty()) {
                put("httpStatus", JSONObject(httpStatus as Map<*, *>))
            }
        }
        return obj.toString()
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    fun destroy() {
        cancelTimeout()
        webView.stopLoading()
        webView.destroy()
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
        private const val RETRY_DELAY_MS = 1_000L
        private const val MAX_RETRIES = 5
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/daksin/autoverdict/collector/CollectorWebView.kt
git commit -m "feat: hidden WebView data collector — encar page load + API extraction

Loads encar detail page in 0x0 WebView, waits for client-side JS to
populate __PRELOADED_STATE__, extracts cars data, then fetches 3 APIs
(record, diagnosis, inspection) from page context (same-origin cookies
auto-included). Retries state extraction up to 5 times for CSR hydration.

Constraint: __PRELOADED_STATE__ is empty in raw HTML — must wait for client JS execution
Constraint: API calls need same-site cookies — must execute fetch() inside WebView context
Rejected: OkHttp direct API calls | encar returns empty state, APIs need cookies
Directive: Do not simplify JS string escaping in evaluateJavascript — breakage is subtle
Confidence: medium
Scope-risk: broad
Not-tested: Encar API rate limiting behavior from Android WebView"
```

---

## Part F: Main App

### Task 14: MainActivity (3 Tabs)

**Files:**
- Modify: `app/src/main/kotlin/com/daksin/autoverdict/MainActivity.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/ui/theme/Color.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/ui/theme/Theme.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/ui/theme/Type.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/ui/screen/AnalyzeScreen.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/ui/screen/SavedListScreen.kt`
- Create: `app/src/main/kotlin/com/daksin/autoverdict/ui/screen/SettingsScreen.kt`

- [ ] **Step 1: Create theme files**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/ui/theme/Color.kt
package com.daksin.autoverdict.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF0064FF)
val Background = Color(0xFFF7F8FA)
val Surface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF888888)
val Danger = Color(0xFFE53935)
val DangerBg = Color(0xFFFFEBEE)
val Warning = Color(0xFFF57C00)
val WarningBg = Color(0xFFFFF3E0)
val Success = Color(0xFF2E7D32)
val SuccessBg = Color(0xFFE8F5E9)
val Border = Color(0xFFF0F0F0)
```

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/ui/theme/Type.kt
package com.daksin.autoverdict.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)
```

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/ui/theme/Theme.kt
package com.daksin.autoverdict.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    background = Background,
    surface = Surface,
    onPrimary = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border,
    error = Danger,
)

@Composable
fun AutoVerdictTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
```

- [ ] **Step 2: Create AnalyzeScreen**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/ui/screen/AnalyzeScreen.kt
package com.daksin.autoverdict.ui.screen

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.db.CacheEntity
import com.daksin.autoverdict.floating.FloatingService
import com.daksin.autoverdict.ui.theme.*
import com.daksin.autoverdict.util.EncarUrl
import kotlinx.coroutines.flow.Flow

@Composable
fun AnalyzeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = (context.applicationContext as AutoVerdictApp).database
    var urlInput by remember { mutableStateOf("") }
    val recentItems by db.cacheDao().getRecentFlow().collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().background(Background).padding(16.dp)) {
        Text(
            "AutoVerdict",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("엔카 매물 URL 입력", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (EncarUrl.isEncarDetail(clip)) urlInput = clip
                        },
                        label = { Text("클립보드에서 붙여넣기") },
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val carId = EncarUrl.extractCarId(urlInput)
                        if (carId != null) {
                            val intent = Intent(context, FloatingService::class.java).apply {
                                putExtra(FloatingService.EXTRA_URL, urlInput)
                            }
                            context.startForegroundService(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = EncarUrl.isEncarDetail(urlInput),
                ) {
                    Text("분석", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (recentItems.isNotEmpty()) {
            Text("최근 분석", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentItems, key = { it.carId }) { item ->
                    RecentCard(item)
                }
            }
        }
    }
}

@Composable
private fun RecentCard(item: CacheEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title.ifEmpty { "Car #${item.carId}" }, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(item.verdict.ifEmpty { "—" }, fontSize = 12.sp, color = TextSecondary)
            }
            if (item.score > 0) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                item.score >= 80 -> SuccessBg
                                item.score >= 50 -> WarningBg
                                else -> DangerBg
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${item.score}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = when {
                            item.score >= 80 -> Success
                            item.score >= 50 -> Warning
                            else -> Danger
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create SavedListScreen**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/ui/screen/SavedListScreen.kt
package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.db.SavedCarEntity
import com.daksin.autoverdict.ui.theme.*

@Composable
fun SavedListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = (context.applicationContext as AutoVerdictApp).database
    val savedCars by db.savedCarDao().getAllFlow().collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().background(Background).padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("저장목록", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${savedCars.size}대", fontSize = 14.sp, color = TextSecondary)
        }

        Spacer(Modifier.height(16.dp))

        if (savedCars.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("저장된 차량이 없습니다", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(savedCars, key = { it.carId }) { car ->
                    SavedCarCard(car)
                }
            }
        }
    }
}

@Composable
private fun SavedCarCard(car: SavedCarEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(car.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                car.score >= 80 -> SuccessBg
                                car.score >= 50 -> WarningBg
                                else -> DangerBg
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${car.score}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = when {
                            car.score >= 80 -> Success
                            car.score >= 50 -> Warning
                            else -> Danger
                        },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val specs = listOfNotNull(
                car.year?.let { "${it}년" },
                car.mileageKm?.let { "${it / 10000}만km" },
                car.priceWon?.let { "${it / 10000}만원" },
            ).joinToString(" · ")
            if (specs.isNotEmpty()) {
                Text(specs, fontSize = 12.sp, color = TextSecondary)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CountChip("위험 ${car.dangerCount}", DangerBg, Danger)
                CountChip("주의 ${car.cautionCount}", WarningBg, Warning)
                CountChip("통과 ${car.passCount}", SuccessBg, Success)
            }
        }
    }
}

@Composable
private fun CountChip(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}
```

- [ ] **Step 4: Create SettingsScreen**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/ui/screen/SettingsScreen.kt
package com.daksin.autoverdict.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.floating.FloatingService
import com.daksin.autoverdict.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as AutoVerdictApp).database
    var floatingEnabled by remember { mutableStateOf(false) }
    val canDrawOverlays = Settings.canDrawOverlays(context)

    Column(modifier = modifier.fillMaxSize().background(Background).padding(16.dp)) {
        Text("설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("플로팅 버튼", fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("플로팅 버튼 활성화", fontSize = 14.sp)
                    Switch(
                        checked = floatingEnabled,
                        onCheckedChange = { enabled ->
                            floatingEnabled = enabled
                            if (enabled) {
                                if (canDrawOverlays) {
                                    context.startForegroundService(
                                        Intent(context, FloatingService::class.java)
                                    )
                                } else {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                    floatingEnabled = false
                                }
                            } else {
                                context.stopService(Intent(context, FloatingService::class.java))
                            }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("권한", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("다른 앱 위에 표시", fontSize = 14.sp)
                    Text(
                        if (canDrawOverlays) "허용됨" else "설정 필요",
                        fontSize = 14.sp,
                        color = if (canDrawOverlays) Success else Warning,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("데이터", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.cacheDao().clearAll()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("캐시 삭제")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "AutoVerdict v1.0.0",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
```

- [ ] **Step 5: Implement MainActivity**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/MainActivity.kt
package com.daksin.autoverdict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.daksin.autoverdict.floating.FloatingService
import com.daksin.autoverdict.ui.screen.AnalyzeScreen
import com.daksin.autoverdict.ui.screen.SavedListScreen
import com.daksin.autoverdict.ui.screen.SettingsScreen
import com.daksin.autoverdict.ui.theme.AutoVerdictTheme
import com.daksin.autoverdict.util.EncarUrl

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        setContent {
            AutoVerdictTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val carId = EncarUrl.extractCarId(sharedText)
            if (carId != null && sharedText != null) {
                val serviceIntent = Intent(this, FloatingService::class.java).apply {
                    putExtra(FloatingService.EXTRA_URL, sharedText)
                }
                startForegroundService(serviceIntent)
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Analyze("분석하기", Icons.Default.Search),
    Saved("저장목록", Icons.Default.List),
    Settings("설정", Icons.Default.Settings),
}

@Composable
private fun MainScreen() {
    var selectedTab by remember { mutableStateOf(Tab.Analyze) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            Tab.Analyze -> AnalyzeScreen(Modifier.padding(padding))
            Tab.Saved -> SavedListScreen(Modifier.padding(padding))
            Tab.Settings -> SettingsScreen(Modifier.padding(padding))
        }
    }
}
```

- [ ] **Step 6: Verify build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/daksin/autoverdict/MainActivity.kt app/src/main/kotlin/com/daksin/autoverdict/ui/
git commit -m "feat: MainActivity with 3-tab Compose UI

Tab 1 (분석하기): URL input with clipboard paste, recent analyses from cache.
Tab 2 (저장목록): Saved cars with score badges and severity counts.
Tab 3 (설정): Floating toggle, overlay permission check, cache clear.
Share Intent handling triggers FloatingService analysis.

Confidence: high
Scope-risk: moderate"
```

---

### Task 15: ClipboardUtil + Share Intent Polish

**Files:**
- Create: `app/src/main/kotlin/com/daksin/autoverdict/util/ClipboardUtil.kt`

- [ ] **Step 1: Create ClipboardUtil**

```kotlin
// app/src/main/kotlin/com/daksin/autoverdict/util/ClipboardUtil.kt
package com.daksin.autoverdict.util

import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {

    fun getEncarUrl(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        return if (EncarUrl.isEncarDetail(text)) text else null
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/daksin/autoverdict/util/ClipboardUtil.kt
git commit -m "feat: ClipboardUtil for encar URL extraction from clipboard

Confidence: high
Scope-risk: narrow"
```

---

## Part G: Integration

### Task 16: End-to-End Wiring + Build

**Files:**
- No new files — verification task.

- [ ] **Step 1: Build WebView bundle and copy to assets**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npm run build && npm run copy-to-assets
```

Verify assets exist:

```bash
ls -la ../app/src/main/assets/eval-ui/index.html
```

- [ ] **Step 2: Build Android APK**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos
./gradlew :app:assembleDebug 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Run all TypeScript tests**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos/webview-bundle
npx vitest run
```

Expected: all tests PASS.

- [ ] **Step 4: Run all Kotlin tests**

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos
./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```

Expected: all tests PASS.

- [ ] **Step 5: Manual test checklist** (on emulator or device)

Install APK:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Test matrix:

| # | Scenario | Expected | Pass? |
|---|----------|----------|-------|
| 1 | Launch app | 3-tab UI displays correctly | ☐ |
| 2 | Paste encar URL in Tab 1 | URL recognized, "분석" button enables | ☐ |
| 3 | Tap "분석" | FloatingService starts, overlay shows loading then results | ☐ |
| 4 | Score card | Correct vehicle name, year, mileage, price, score, verdict | ☐ |
| 5 | Severity summary | Danger/caution/pass/unknown counts match rule results | ☐ |
| 6 | Category accordions | 5 categories in correct order, rules grouped correctly | ☐ |
| 7 | Accordion expand/collapse | Tapping toggles visibility | ☐ |
| 8 | Tap "저장하기" | Toast "저장되었습니다", car appears in Tab 2 | ☐ |
| 9 | Tap ✕ close | Overlay dismisses, floating button remains | ☐ |
| 10 | Tab 3 toggle | Floating button appears/disappears | ☐ |
| 11 | Copy encar URL, tap floating button | Reads clipboard, starts analysis | ☐ |
| 12 | Share URL from browser | MainActivity receives intent, starts analysis | ☐ |
| 13 | Re-analyze same car | Cache hit — instant display | ☐ |
| 14 | Personal listing (CLIENT) | R03 shows "해당없음", not KILLER | ☐ |
| 15 | Invalid URL | Toast "엔카 매물 URL을 복사한 후 다시 눌러주세요" | ☐ |

- [ ] **Step 6: Final commit**

```bash
git add -A
git commit -m "chore: build verification and E2E test checklist

WebView bundle built and copied to Android assets.
Both TypeScript and Kotlin test suites pass.
Manual test checklist covers 15 scenarios including
cache hit, personal listing, and share intent flows.

Confidence: high
Scope-risk: narrow"
```

---

### Task 17: Create Build Script

**Files:**
- Create: `build-all.sh`

- [ ] **Step 1: Create unified build script**

```bash
#!/bin/bash
# build-all.sh — Build WebView bundle + Android APK
set -euo pipefail

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
```

```bash
chmod +x build-all.sh
```

- [ ] **Step 2: Run it**

```bash
./build-all.sh
```

Expected: all steps succeed.

- [ ] **Step 3: Commit**

```bash
git add build-all.sh
git commit -m "chore: unified build script for webview bundle + Android APK

Confidence: high
Scope-risk: narrow"
```
