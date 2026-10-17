package com.car.autoverdict.engine

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * `window.Android` for the engine page. JS calls arrive on the JavaBridge thread,
 * so every call is posted to the main thread before reaching [callbacks].
 */
class EngineBridge(
    private val callbacks: EngineCallbacks,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {
    @JavascriptInterface
    fun onReady() {
        mainHandler.post { callbacks.onReady() }
    }

    @JavascriptInterface
    fun onReport(requestId: String, json: String) {
        mainHandler.post { callbacks.onReport(requestId, json) }
    }

    @JavascriptInterface
    fun onEngineError(requestId: String, message: String) {
        mainHandler.post { callbacks.onEngineError(requestId, message) }
    }

    companion object {
        const val BRIDGE_NAME = "Android"
    }
}
