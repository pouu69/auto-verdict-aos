package com.car.autoverdict.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.car.autoverdict.util.JsEscape

class EvalWebView(context: Context) {
    val webView: WebView = WebView(context.applicationContext)
    private var pageLoaded = false
    private var pendingJson: String? = null
    private var pendingError: String? = null
    private var pendingAlreadySaved: Boolean? = null

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
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "eval-ui loaded: $url")
                pageLoaded = true
                pendingJson?.let { json ->
                    pendingJson = null
                    sendData(json)
                }
                pendingError?.let { msg ->
                    pendingError = null
                    sendError(msg)
                }
                pendingAlreadySaved?.let { saved ->
                    pendingAlreadySaved = null
                    setAlreadySaved(saved)
                }
            }
        }
    }

    fun addBridge(bridge: NativeBridge) {
        webView.addJavascriptInterface(bridge, NativeBridge.BRIDGE_NAME)
    }

    fun loadEvalUi() {
        pageLoaded = false
        webView.loadUrl("file:///android_asset/eval-ui/index.html")
    }

    fun sendData(json: String) {
        if (!pageLoaded) {
            Log.d(TAG, "page not loaded yet, queuing data (${json.length} bytes)")
            pendingJson = json
            return
        }
        val escaped = JsEscape.escapeForSingleQuotedString(json)
        webView.evaluateJavascript("window.receiveEncarData?.('$escaped')", null)
    }

    fun sendError(message: String) {
        if (!pageLoaded) {
            Log.d(TAG, "page not loaded yet, queuing error: $message")
            pendingError = message
            return
        }
        val escaped = JsEscape.escapeForSingleQuotedString("""{"message":"${message.replace("\"", "\\\"")}"}""")
        webView.evaluateJavascript("window.receiveError?.('$escaped')", null)
    }

    fun setAlreadySaved(saved: Boolean) {
        if (!pageLoaded) {
            pendingAlreadySaved = saved
            return
        }
        webView.evaluateJavascript("window.setAlreadySaved?.($saved)", null)
    }

    fun destroy() {
        webView.removeJavascriptInterface(NativeBridge.BRIDGE_NAME)
        webView.destroy()
    }

    companion object {
        private const val TAG = "EvalWebView"
    }
}
