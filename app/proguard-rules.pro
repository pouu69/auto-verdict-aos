# === JavascriptInterface (WebView bridge) ===
-keepclassmembers class com.car.autoverdict.engine.EngineBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.car.autoverdict.collector.CollectorWebView$ApiBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# === kotlinx.serialization (EvaluationReport contract) ===
# The library ships consumer rules for the runtime; these keep the generated serializers of
# our @Serializable report classes reachable through Companion.serializer().
-keepclassmembers class com.car.autoverdict.report.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.car.autoverdict.report.**$$serializer { *; }

# === kotlinx.serialization (open-source notices, spec 7.3) ===
-keepclassmembers class com.car.autoverdict.licenses.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.car.autoverdict.licenses.**$$serializer { *; }

# === Room ===
-keep class com.car.autoverdict.db.*Entity { *; }
-keep class com.car.autoverdict.db.*Dao { *; }
-keep class com.car.autoverdict.db.AppDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# === Kotlin Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**

# === Compose ===
-dontwarn androidx.compose.**
# No keep rules for Compose, AndroidX Lifecycle or core-splashscreen: Compose and Lifecycle
# ship their own consumer rules (Lifecycle covers the reflective @OnLifecycleEvent /
# ViewModel <init> cases), the app has no ViewModel or LifecycleObserver of its own, and
# core-splashscreen uses no reflection. Blanket -keep rules here kept hundreds of library
# classes un-shrunk and un-obfuscated.

# === General ===
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
