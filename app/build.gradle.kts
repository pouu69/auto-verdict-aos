import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.car.autoverdict"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.car.autoverdict"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        val keystorePropsFile = rootProject.file("keystore.properties")
        if (keystorePropsFile.exists()) {
            create("release") {
                val props = Properties().apply { load(keystorePropsFile.inputStream()) }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePropsFile = rootProject.file("keystore.properties")
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // With built-in Kotlin, kotlin compilerOptions.jvmTarget defaults to
    // compileOptions.targetCompatibility (17), so no kotlinOptions block needed.

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.room:room-testing:2.8.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// --- WebView bundle wired into the Android build ---------------------------
// The React/TS evaluation UI lives in webview-bundle/ and is bundled into
// app/src/main/assets/eval-ui. Wiring it into preBuild guarantees that
// `assembleDebug` and Android Studio's Run button always ship the latest bundle.
val webViewDir = file("${rootProject.projectDir}/webview-bundle")
val evalUiAssetsDir = file("${rootProject.projectDir}/app/src/main/assets/eval-ui")

// Resolve an npm executable that works whether Gradle is launched from a
// terminal or from Android Studio's GUI. A GUI launch does NOT inherit the
// shell PATH, and nvm installs node outside the standard locations — so relying
// on `bash -lc` / a bare `npm` fails silently when run from the IDE.
// Override with -PnpmPath=/abs/path/to/npm or a `npmPath=` line in local.properties.
val npmExecutable: String by lazy {
    val candidates = sequenceOf(
        findProperty("npmPath") as String?,
        // latest nvm-installed node
        File(System.getProperty("user.home"), ".nvm/versions/node")
            .takeIf { it.isDirectory }
            ?.listFiles { f -> f.isDirectory }
            ?.maxByOrNull { it.name }
            ?.let { "${it.absolutePath}/bin/npm" },
        "/opt/homebrew/bin/npm",
        "/usr/local/bin/npm",
    )
    candidates.filterNotNull().firstOrNull { File(it).exists() } ?: "npm"
}

// npm is itself a script that re-resolves `node` via PATH, so the node bin dir
// must be on the Exec environment PATH (a GUI launch otherwise lacks it → exit 127).
fun Exec.withNodeOnPath() {
    val nodeBinDir = File(npmExecutable).parent
    if (nodeBinDir != null) {
        val current = System.getenv("PATH") ?: ""
        environment("PATH", nodeBinDir + File.pathSeparator + current)
    }
}

val buildWebView = tasks.register<Exec>("buildWebView") {
    workingDir = webViewDir
    withNodeOnPath()
    // Re-run only when bundle sources or config change (src/ includes vendored src/core).
    inputs.dir(file("$webViewDir/src")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(
        file("$webViewDir/package.json"),
        file("$webViewDir/package-lock.json"),
        file("$webViewDir/tsconfig.json"),
        file("$webViewDir/vite.config.ts"),
        file("$webViewDir/index.html"),
    )
    outputs.dir(file("$webViewDir/dist"))
    commandLine(npmExecutable, "run", "build")
}

val copyWebViewAssets = tasks.register<Exec>("copyWebViewAssets") {
    dependsOn(buildWebView)
    workingDir = webViewDir
    withNodeOnPath()
    inputs.dir(file("$webViewDir/dist")).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(evalUiAssetsDir)
    commandLine(npmExecutable, "run", "copy-to-assets")
}

tasks.named("preBuild") {
    dependsOn(copyWebViewAssets)
}
