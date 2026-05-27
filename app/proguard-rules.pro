# Keep @JavascriptInterface methods — called by name from WebView JS
-keepclassmembers class com.daksin.autoverdict.webview.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Room entities
-keep class com.daksin.autoverdict.db.*Entity { *; }
