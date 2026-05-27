package com.daksin.autoverdict.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.daksin.autoverdict.util.JsEscape

class EvalWebView(context: Context) {
    val webView: WebView = WebView(context.applicationContext)

    init {
        setup()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setup() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            allowFileAccess = true
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
        val escaped = JsEscape.escapeForSingleQuotedString(json)
        webView.evaluateJavascript("window.receiveEncarData?.('$escaped')", null)
    }

    fun sendError(message: String) {
        val escaped = JsEscape.escapeForSingleQuotedString("""{"message":"${message.replace("\"", "\\\"")}"}""")
        webView.evaluateJavascript("window.receiveError?.('$escaped')", null)
    }

    fun destroy() {
        webView.removeJavascriptInterface(NativeBridge.BRIDGE_NAME)
        webView.destroy()
    }
}
