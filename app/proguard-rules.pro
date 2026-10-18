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

# === AndroidX Lifecycle ===
-keep class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <methods>;
}
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# === Compose ===
-dontwarn androidx.compose.**
# No keep rule for Compose: its libraries ship their own consumer rules, and a blanket
# -keep on androidx.compose.runtime.** retained ~650 classes un-shrunk and un-obfuscated.

# === SplashScreen ===
-keep class androidx.core.splashscreen.** { *; }

# === General ===
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
