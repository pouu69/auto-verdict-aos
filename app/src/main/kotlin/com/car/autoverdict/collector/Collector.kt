package com.car.autoverdict.collector

import com.car.autoverdict.result.CollectStage

/** One Encar collection session. Implemented by [CollectorWebView]; faked in tests. Main thread only. */
interface Collector {
    /** [callback] fires at most once per call; [onStage] reports progress before it. */
    fun collect(
        url: String,
        carId: String,
        onStage: (CollectStage) -> Unit = {},
        callback: (CollectorWebView.Result) -> Unit,
    )

    fun destroy()
}
