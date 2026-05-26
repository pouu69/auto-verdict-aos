package com.daksin.autoverdict.collector

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.daksin.autoverdict.util.EncarUrl
import org.json.JSONObject

class CollectorWebView(context: Context) {
    sealed class Result {
        data class Success(val json: String) : Result()
        data class Error(val message: String) : Result()
    }

    private val webView: WebView = WebView(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    init {
        setup()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setup() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = webView.settings.userAgentString.replace("; wv", "")
        }
        webView.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
    }

    fun collect(url: String, carId: String, callback: (Result) -> Unit) {
        cancelTimeout()
        val fullUrl = if (url.startsWith("http")) url else EncarUrl.buildDetailUrl(carId)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                super.onPageFinished(view, loadedUrl)
                extractPreloadedState(carId, callback)
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?,
            ) {
                cancelTimeout()
                callback(Result.Error("페이지 로드 실패: $description"))
            }
        }

        timeoutRunnable = Runnable {
            callback(Result.Error("시간 초과 — 네트워크를 확인하세요"))
            webView.stopLoading()
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT_MS)
        webView.loadUrl(fullUrl)
    }

    private fun extractPreloadedState(
        carId: String,
        callback: (Result) -> Unit,
        attempt: Int = 0,
    ) {
        val js = """
        (function() {
            try {
                var state = window.__PRELOADED_STATE__;
                if (!state || !state.cars || !state.cars.base || Object.keys(state.cars.base).length === 0) {
                    return JSON.stringify({ error: 'empty_state' });
                }
                return JSON.stringify({ ok: true, cars: state.cars });
            } catch(e) { return JSON.stringify({ error: e.message }); }
        })();
        """.trimIndent()

        webView.evaluateJavascript(js) { raw ->
            val parsed = raw?.removeSurrounding("\"")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")

            if (parsed == null || parsed == "null") {
                cancelTimeout()
                callback(Result.Error("페이지 데이터 추출 실패"))
                return@evaluateJavascript
            }

            try {
                val obj = JSONObject(parsed)
                if (obj.has("error")) {
                    if (attempt < MAX_RETRIES) {
                        handler.postDelayed(
                            { extractPreloadedState(carId, callback, attempt + 1) },
                            RETRY_DELAY_MS,
                        )
                    } else {
                        cancelTimeout()
                        callback(Result.Error("매물 데이터를 찾을 수 없습니다"))
                    }
                    return@evaluateJavascript
                }
                fetchApis(carId, obj.getJSONObject("cars"), callback)
            } catch (e: Exception) {
                cancelTimeout()
                callback(Result.Error("데이터 파싱 오류: ${e.message}"))
            }
        }
    }

    private fun fetchApis(
        carId: String,
        carsJson: JSONObject,
        callback: (Result) -> Unit,
    ) {
        val base = carsJson.optJSONObject("base")
        val vehicleId = base?.optInt("vehicleId", 0) ?: 0
        val vehicleNo = base?.optString("vehicleNo", "") ?: ""

        if (vehicleId == 0 || vehicleNo.isEmpty()) {
            cancelTimeout()
            callback(
                Result.Success(
                    buildResultJson(carId, carsJson, null, null, null, emptyMap()),
                ),
            )
            return
        }

        val apiJs = """
        (function() {
            var vid = $vehicleId; var vno = '$vehicleNo';
            var base = 'https://api.encar.com/v1/readside';
            var results = {}; var statuses = {};
            function fetchApi(name, url) {
                return fetch(url, { credentials: 'include' })
                    .then(function(r) { statuses[name] = r.ok ? 'ok' : (r.status === 404 ? 'not_found' : 'error'); return r.ok ? r.json() : null; })
                    .then(function(data) { results[name] = data; })
                    .catch(function() { statuses[name] = 'error'; results[name] = null; });
            }
            return Promise.all([
                fetchApi('recordJson', base + '/record/vehicle/' + vid + '/open?vehicleNo=' + encodeURIComponent(vno)),
                fetchApi('diagnosisJson', base + '/diagnosis/vehicle/' + vid),
                fetchApi('inspectionJson', base + '/inspection/vehicle/' + vid)
            ]).then(function() { return JSON.stringify({ results: results, statuses: statuses }); });
        })();
        """.trimIndent()

        webView.evaluateJavascript(apiJs) { raw ->
            cancelTimeout()
            val parsed = raw?.removeSurrounding("\"")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")
            try {
                val apiResponse =
                    if (parsed != null && parsed != "null") JSONObject(parsed) else null
                val results = apiResponse?.optJSONObject("results")
                val statuses = apiResponse?.optJSONObject("statuses")
                val httpStatus = mutableMapOf<String, String>()
                statuses?.keys()?.forEach { key -> httpStatus[key] = statuses.getString(key) }
                callback(
                    Result.Success(
                        buildResultJson(
                            carId, carsJson,
                            results?.optJSONObject("recordJson")?.toString(),
                            results?.optJSONObject("diagnosisJson")?.toString(),
                            results?.optJSONObject("inspectionJson")?.toString(),
                            httpStatus,
                        ),
                    ),
                )
            } catch (e: Exception) {
                callback(
                    Result.Success(
                        buildResultJson(carId, carsJson, null, null, null, emptyMap()),
                    ),
                )
            }
            webView.loadUrl("about:blank")
        }
    }

    private fun buildResultJson(
        carId: String,
        carsJson: JSONObject,
        recordJson: String?,
        diagnosisJson: String?,
        inspectionJson: String?,
        httpStatus: Map<String, String>,
    ): String {
        return JSONObject().apply {
            put("url", EncarUrl.buildDetailUrl(carId))
            put("carId", carId)
            put("preloadedState", JSONObject().put("cars", carsJson))
            if (recordJson != null) put("recordJson", JSONObject(recordJson))
            if (diagnosisJson != null) put("diagnosisJson", JSONObject(diagnosisJson))
            if (inspectionJson != null) put("inspectionJson", JSONObject(inspectionJson))
            if (httpStatus.isNotEmpty()) put("httpStatus", JSONObject(httpStatus as Map<*, *>))
        }.toString()
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    fun destroy() {
        cancelTimeout()
        webView.stopLoading()
        webView.destroy()
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
        private const val RETRY_DELAY_MS = 1_000L
        private const val MAX_RETRIES = 5
    }
}
