package com.car.autoverdict.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Main-thread owner for one result screen. A lazy item's view never owns the loaded ad. */
internal class NativeAdSession<Ad : Any>(
    private val request: (onLoaded: (Ad) -> Unit, onFailed: (String) -> Unit) -> Unit,
    private val destroy: (Ad) -> Unit,
    private val log: (String) -> Unit = {},
) {
    var ad: Ad? by mutableStateOf(null)
        private set
    private var started = false
    private var disposed = false

    fun load() {
        if (started || disposed) return
        started = true
        request({ loaded ->
            if (disposed) {
                log("late_ad_destroy")
                destroy(loaded)
            } else {
                ad?.let(destroy)
                ad = loaded
            }
        }, { error -> log("load_failed $error") })
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        log("owner_dispose hasAd=${ad != null}")
        ad?.let(destroy)
        ad = null
    }
}
