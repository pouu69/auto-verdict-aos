package com.daksin.autoverdict.floating

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import com.daksin.autoverdict.collector.CollectorWebView
import com.daksin.autoverdict.db.AppDatabase
import com.daksin.autoverdict.db.CacheEntity
import com.daksin.autoverdict.webview.EvalWebView
import com.daksin.autoverdict.webview.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val database: AppDatabase,
) {
    private var overlayView: FrameLayout? = null
    private var evalWebView: EvalWebView? = null
    private var collector: CollectorWebView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val overlayParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    fun show(url: String, carId: String) {
        scope.launch {
            val cached = database.cacheDao().getValid(carId)
            if (cached != null) {
                showOverlay()
                evalWebView?.sendData(cached.rawInputJson)
                return@launch
            }
            showOverlay()
            if (collector == null) collector = CollectorWebView(context)
            collector?.collect(url, carId) { result ->
                scope.launch(Dispatchers.Main) {
                    when (result) {
                        is CollectorWebView.Result.Success -> {
                            evalWebView?.sendData(result.json)
                            cacheResult(carId, url, result.json)
                        }
                        is CollectorWebView.Result.Error -> {
                            evalWebView?.sendError(result.message)
                        }
                    }
                }
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        val container = FrameLayout(context)
        val eval = EvalWebView(context)
        val bridge = NativeBridge(database, scope, onClose = { dismiss() }, toastContext = context)
        eval.addBridge(bridge)
        eval.loadEvalUi()
        container.addView(
            eval.webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        windowManager.addView(container, overlayParams)
        overlayView = container
        evalWebView = eval
    }

    fun dismiss() {
        overlayView?.let { windowManager.removeView(it); overlayView = null }
        evalWebView?.destroy()
        evalWebView = null
    }

    fun destroy() {
        dismiss()
        collector?.destroy()
        collector = null
        scope.cancel()
    }

    private fun cacheResult(carId: String, url: String, rawInputJson: String) {
        scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            database.cacheDao().upsert(
                CacheEntity(
                    carId = carId,
                    url = url,
                    title = "",
                    score = 0,
                    verdict = "",
                    resultJson = "",
                    rawInputJson = rawInputJson,
                    cachedAt = now,
                    expiresAt = now + CACHE_TTL_MS,
                ),
            )
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
