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
        val escaped = """{"message":"${message.replace("\"", "\\\"")}"}""".replace("'", "\\'")
        webView.evaluateJavascript("window.receiveError?.('$escaped')", null)
    }

    fun destroy() {
        webView.removeJavascriptInterface(NativeBridge.BRIDGE_NAME)
        webView.destroy()
    }
}
