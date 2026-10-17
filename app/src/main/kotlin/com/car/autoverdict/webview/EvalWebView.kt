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

/**
 * Hosts the React eval-ui bundle.
 *
 * Payloads are delivered only once BOTH the page has finished loading AND the JS
 * side has signalled (via [NativeBridge.onReady]) that all `window.receive*`
 * handlers are registered. `onPageFinished` alone is not enough: React registers
 * the handlers in a post-commit `useEffect`, which can run after the page load
 * event, so an early `window.receiveEncarData?.(...)` would silently no-op.
 * Anything sent before then is queued (latest value wins) and flushed on ready.
 *
 * All methods must be called on the main thread.
 */
class EvalWebView(context: Context) {
    val webView: WebView = WebView(context.applicationContext)
    private var pageLoaded = false
    private var jsReady = false
    private var destroyed = false
    private var pendingJson: String? = null
    private var pendingError: String? = null
    private var pendingAlreadySaved: Boolean? = null
    private var pendingDeleted: Pair<String, Boolean>? = null

    private val isReady: Boolean
        get() = pageLoaded && jsReady && !destroyed

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
                flushPending()
            }
        }
    }

    fun addBridge(bridge: NativeBridge) {
        bridge.setOnReadyListener(::onJsReady)
        webView.addJavascriptInterface(bridge, NativeBridge.BRIDGE_NAME)
    }

    fun loadEvalUi() {
        pageLoaded = false
        jsReady = false
        webView.loadUrl("file:///android_asset/eval-ui/index.html")
    }

    /** Called (on the main thread) when the JS side has registered its handlers. */
    fun onJsReady() {
        if (destroyed) return
        Log.d(TAG, "eval-ui JS ready")
        jsReady = true
        flushPending()
    }

    private fun flushPending() {
        if (!isReady) return
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

    fun sendData(json: String) {
        if (!isReady) {
            Log.d(TAG, "eval-ui not ready, queuing data (${json.length} bytes)")
            pendingJson = json
            return
        }
        val escaped = JsEscape.escapeForSingleQuotedString(json)
        webView.evaluateJavascript("window.receiveEncarData?.('$escaped')", null)
    }

    fun sendError(message: String) {
        if (!isReady) {
            Log.d(TAG, "eval-ui not ready, queuing error: $message")
            pendingError = message
            return
        }
        val escaped = JsEscape.escapeForSingleQuotedString("""{"message":"${message.replace("\"", "\\\"")}"}""")
        webView.evaluateJavascript("window.receiveError?.('$escaped')", null)
    }

    fun sendDeleted(carId: String, isSaved: Boolean) {
        if (!isReady) {
            Log.d(TAG, "eval-ui not ready, queuing deleted: $carId")
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
        if (!isReady) {
            pendingAlreadySaved = saved
            return
        }
        webView.evaluateJavascript("window.setAlreadySaved?.($saved)", null)
    }

    fun destroy() {
        destroyed = true
        webView.removeJavascriptInterface(NativeBridge.BRIDGE_NAME)
        webView.destroy()
    }

    companion object {
        private const val TAG = "EvalWebView"
    }
}
