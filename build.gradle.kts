buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9.2's built-in Kotlin bundles KGP 2.2.10. Upgrade it to 2.3.x so it
        // matches the decoupled KSP 2.3.x line — required because the older KSP
        // (2.2.10-2.0.2) registers generated sources via kotlin.sourceSets, which
        // built-in Kotlin disallows, while KSP 2.3.x uses android.sourceSets.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    // AGP 9 provides built-in Kotlin, so the org.jetbrains.kotlin.android plugin
    // is no longer applied. The Compose compiler plugin must match the Kotlin
    // version above (2.3.21); KSP 2.3.9 targets that same Kotlin line.
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
}
