package com.car.autoverdict.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.car.autoverdict.util.JsEscape
import org.json.JSONObject

class EvalWebView(context: Context) {
    val webView: WebView = WebView(context.applicationContext)
    private var pageLoaded = false
    private var pendingJson: String? = null
    private var pendingError: String? = null
    private var pendingAlreadySaved: Boolean? = null
    private var pendingDeleted: Pair<String, Boolean>? = null

    init {
        setup()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setup() {
        // Disable the Android 12+ stretch overscroll: on bounce it stretches the
        // whole WebView (including the position:fixed 저장하기 bar), pushing it
        // down into the native ad banner below.
        webView.overScrollMode = View.OVER_SCROLL_NEVER
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
                pendingDeleted?.let { (carId, isSaved) ->
                    pendingDeleted = null
                    sendDeleted(carId, isSaved)
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

    fun sendDeleted(carId: String, isSaved: Boolean) {
        if (!pageLoaded) {
            Log.d(TAG, "page not loaded yet, queuing deleted: $carId")
            pendingDeleted = carId to isSaved
            return
        }
        val payload = JSONObject().apply {
            put("carId", carId)
            put("isSaved", isSaved)
        }.toString()
        val escaped = JsEscape.escapeForSingleQuotedString(payload)
        webView.evaluateJavascript("window.receiveDeleted?.('$escaped')", null)
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
