# === JavascriptInterface (WebView bridge) ===
-keepclassmembers class com.daksin.autoverdict.webview.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.daksin.autoverdict.collector.CollectorWebView$ApiBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# === Room ===
-keep class com.daksin.autoverdict.db.*Entity { *; }
-keep class com.daksin.autoverdict.db.*Dao { *; }
-keep class com.daksin.autoverdict.db.AppDatabase { *; }
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
-keep class androidx.compose.runtime.** { *; }

# === SplashScreen ===
-keep class androidx.core.splashscreen.** { *; }

# === General ===
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
