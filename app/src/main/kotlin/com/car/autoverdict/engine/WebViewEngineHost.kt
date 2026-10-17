package com.car.autoverdict.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * Off-screen WebView running the IIFE engine bundle (spec 3.3).
 * The page is a plain `<script src>` load from assets, so file access stays off.
 * All methods must be called on the main thread.
 */
class WebViewEngineHost(context: Context) : EngineHost {
    private val appContext = context.applicationContext
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun start(callbacks: EngineCallbacks) {
        check(webView == null) { "already started" }
        val view = WebView(appContext)
        view.settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            // android_asset is readable regardless of these flags.
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            // The engine never needs the network.
            blockNetworkLoads = true
        }
        view.webViewClient = object : WebViewClient() {
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                Log.w(TAG, "engine renderer gone (crash=${detail?.didCrash()})")
                // The engine destroys this host and recreates it on the next request.
                if (view != null && view === webView) callbacks.onRenderProcessGone()
                return true
            }
        }
        view.addJavascriptInterface(EngineBridge(callbacks), EngineBridge.BRIDGE_NAME)
        webView = view
        view.loadUrl(ENGINE_URL)
    }

    override fun evaluate(requestId: String, rawJson: String) {
        val view = webView ?: return
        view.evaluateJavascript(
            "window.avEvaluate(${JSONObject.quote(requestId)}, ${JSONObject.quote(rawJson)})",
            null,
        )
    }

    override fun destroy() {
        val view = webView ?: return
        webView = null
        view.removeJavascriptInterface(EngineBridge.BRIDGE_NAME)
        view.stopLoading()
        view.destroy()
    }

    companion object {
        const val ENGINE_URL = "file:///android_asset/eval-ui/engine.html"
        private const val TAG = "WebViewEngineHost"
    }
}
