package com.car.autoverdict.engine

/**
 * One engine page instance (the WebView-dependent part of [RuleEngine]).
 * All calls happen on the main thread.
 */
interface EngineHost {
    /** Load the engine page. [callbacks] may be invoked any time after (from the main thread). */
    fun start(callbacks: EngineCallbacks)

    /** Calls `window.avEvaluate(requestId, rawJson)`. */
    fun evaluate(requestId: String, rawJson: String)

    fun destroy()
}

/** Signals from the engine page. Always delivered on the main thread. */
interface EngineCallbacks {
    fun onReady()
    fun onReport(requestId: String, reportJson: String)
    fun onEngineError(requestId: String, message: String)
    fun onRenderProcessGone()
}
