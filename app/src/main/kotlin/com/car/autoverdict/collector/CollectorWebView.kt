package com.car.autoverdict.collector

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.car.autoverdict.result.CollectErrorKind
import com.car.autoverdict.result.CollectStage
import com.car.autoverdict.util.EncarUrl
import org.json.JSONObject

class CollectorWebView(context: Context) : Collector {
    sealed class Result {
        data class Success(val json: String) : Result()
        /** [kind] drives the error screen; [message] is for logs only. */
        data class Error(val kind: CollectErrorKind, val message: String) : Result()

        /**
         * The listing can't be analyzed as asked: sold, no longer existing on Encar, or the page
         * shows a different car than requested. See [ListingAvailability].
         */
        data object Deleted : Result()
    }

    private val webView: WebView = WebView(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var callbackFired = false
    private var apiResultHandler: ((String) -> Unit)? = null

    // collect() 호출마다 증가. 이전 수집에서 예약된 재시도/JS 콜백이 새 수집에
    // 끼어들지 않도록 비교용으로 쓴다.
    private var sessionId = 0

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
        webView.addJavascriptInterface(ApiBridge(), "ApiCallback")
    }

    inner class ApiBridge {
        @JavascriptInterface
        fun onResult(json: String) {
            handler.post { apiResultHandler?.invoke(json) }
        }
    }

    /**
     * [onStage] reports progress on the main thread: [CollectStage.OpeningPage] right before the
     * page load, [CollectStage.FetchingHistory] right before the record/diagnosis/inspection calls.
     */
    override fun collect(
        url: String,
        carId: String,
        onStage: (CollectStage) -> Unit,
        callback: (Result) -> Unit,
    ) {
        cancelTimeout()
        apiResultHandler = null
        callbackFired = false
        val session = ++sessionId
        val fullUrl = if (url.startsWith("http")) url else EncarUrl.buildDetailUrl(carId)

        val guardedCallback = { result: Result ->
            if (session == sessionId && !callbackFired) {
                callbackFired = true
                cancelTimeout()
                callback(result)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            private var extractionStarted = false
            private var pageFinished = false

            // __PRELOADED_STATE__는 SSR HTML에 인라인으로 들어 있어 문서가 커밋되면
            // 이미 읽을 수 있다. onPageFinished는 광고/트래커 등 서브리소스까지 기다려
            // 느린 망에서는 수십 초 늦어지므로, 커밋 시점부터 추출을 시작한다.
            override fun onPageCommitVisible(view: WebView?, loadedUrl: String?) {
                super.onPageCommitVisible(view, loadedUrl)
                startExtraction()
            }

            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                super.onPageFinished(view, loadedUrl)
                pageFinished = true
                startExtraction()
            }

            private fun startExtraction() {
                if (extractionStarted || callbackFired) return
                extractionStarted = true
                extractPreloadedState(session, carId, { pageFinished }, onStage, guardedCallback)
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?,
            ) {
                guardedCallback(Result.Error(CollectErrorKind.Network, "페이지 로드 실패: $description"))
            }

            // 렌더러 프로세스는 앱의 WebView들이 공유하므로 false를 반환하면 앱 전체가 죽는다.
            // 이 수집은 실패로 끝내고, WebView 파기는 호출자(destroy)에게 맡긴다.
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                guardedCallback(Result.Error(CollectErrorKind.PageUnreadable, "렌더러 종료 (crash=${detail?.didCrash()})"))
                return true
            }
        }

        timeoutRunnable = Runnable {
            guardedCallback(Result.Error(CollectErrorKind.Timeout, "시간 초과 — 네트워크를 확인하세요"))
            webView.stopLoading()
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT_MS)
        Log.d(TAG, "loading: $fullUrl")
        onStage(CollectStage.OpeningPage)
        webView.loadUrl(fullUrl)
    }

    private fun extractPreloadedState(
        session: Int,
        carId: String,
        isPageFinished: () -> Boolean,
        onStage: (CollectStage) -> Unit,
        callback: (Result) -> Unit,
        attempt: Int = 0,
    ) {
        if (session != sessionId || callbackFired) return
        val js = """
        (function() {
            try {
                var state = window.__PRELOADED_STATE__;
                if (!state) {
                    return JSON.stringify({ error: 'empty_state' });
                }
                if (!state.cars || !state.cars.base || Object.keys(state.cars.base).length === 0) {
                    // Pass Encar's fetch-failure flags so Kotlin can tell a removed listing apart.
                    return JSON.stringify({
                        error: 'empty_state',
                        state: {
                            pender: { failure: (state.pender && state.pender.failure) || null },
                            cars: { base: (state.cars && state.cars.base) || null }
                        }
                    });
                }
                // Sold/replaced detection happens in Kotlin (ListingAvailability).
                return JSON.stringify({ ok: true, cars: state.cars });
            } catch(e) { return JSON.stringify({ error: e.message }); }
        })();
        """.trimIndent()

        // 로드 완료 전에는 상태 스크립트가 아직 실행되지 않았을 수 있으므로
        // 재시도 횟수를 소모하지 않고 계속 폴링한다 (상한은 TIMEOUT_MS).
        fun retryOr(error: Result.Error) {
            val nextAttempt = if (isPageFinished()) attempt + 1 else attempt
            if (nextAttempt <= MAX_RETRIES) {
                handler.postDelayed(
                    { extractPreloadedState(session, carId, isPageFinished, onStage, callback, nextAttempt) },
                    RETRY_DELAY_MS,
                )
            } else {
                callback(error)
            }
        }

        webView.evaluateJavascript(js) { raw ->
            if (session != sessionId || callbackFired) return@evaluateJavascript

            val parsed = raw?.removeSurrounding("\"")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")

            if (parsed == null || parsed == "null") {
                // 로드 완료 후의 null은 기존대로 즉시 실패 처리
                val error = Result.Error(CollectErrorKind.PageUnreadable, "페이지 데이터 추출 실패")
                if (isPageFinished()) callback(error) else retryOr(error)
                return@evaluateJavascript
            }

            try {
                val obj = JSONObject(parsed)
                if (obj.has("error")) {
                    // Only trust "removed" once the page is fully loaded; until then keep polling.
                    val state = obj.optJSONObject("state")
                    if (isPageFinished() && state != null && ListingAvailability.isMissing(state)) {
                        // Suspected removal: confirm with the base API's HTTP status.
                        probeBaseApi(session, carId, callback)
                        return@evaluateJavascript
                    }
                    retryOr(Result.Error(CollectErrorKind.PageUnreadable, "매물 데이터를 찾을 수 없습니다"))
                    return@evaluateJavascript
                }
                val cars = obj.getJSONObject("cars")
                val base = cars.optJSONObject("base")
                if (base != null && ListingAvailability.isGone(base)) {
                    callback(Result.Deleted)
                    return@evaluateJavascript
                }
                // A dummy-ad URL resolves to the original vehicle: the APIs use base.vehicleId,
                // while the payload's carId (cache key) stays the id the user opened. Saved rows
                // key on the original id instead (SavedCarKey).
                fetchApis(session, carId, cars, onStage, callback)
            } catch (e: Exception) {
                callback(Result.Error(CollectErrorKind.PageUnreadable, "데이터 파싱 오류: ${e.message}"))
            }
        }
    }

    /**
     * The page's base fetch failed, which Encar flags the same way for a 404 and a transient
     * 5xx/network error. Re-request the base API the page uses and decide from the HTTP status
     * ([ListingAvailability.isConfirmedMissing]): 404 → [Result.Deleted], anything else →
     * retryable [CollectErrorKind.PageUnreadable]. The probe gives up after [PROBE_TIMEOUT_MS]
     * and always stays under the collection timeout.
     */
    private fun probeBaseApi(session: Int, carId: String, callback: (Result) -> Unit) {
        apiResultHandler = onProbe@{ json ->
            if (session != sessionId) return@onProbe
            apiResultHandler = null
            Log.d(TAG, "base probe: $json")
            if (ListingAvailability.isConfirmedMissing(json)) {
                callback(Result.Deleted)
            } else {
                callback(Result.Error(CollectErrorKind.PageUnreadable, "매물 기본 정보 조회 실패: $json"))
            }
        }
        // Same endpoint and include list as the page's own base request (cars.base.requestUrl).
        val probeJs = """
        (function() {
            var done = false;
            function report(r) {
                if (done) return;
                done = true;
                ApiCallback.onResult(JSON.stringify(r));
            }
            var ctrl = typeof AbortController === 'function' ? new AbortController() : null;
            var timer = setTimeout(function() {
                if (ctrl) ctrl.abort();
                report({ error: 'timeout' });
            }, $PROBE_TIMEOUT_MS);
            var url = 'https://api.encar.com/v1/readside/vehicle/' +
                encodeURIComponent(${JSONObject.quote(carId)}) + '?include=$BASE_INCLUDE';
            fetch(url, ctrl ? { signal: ctrl.signal } : {})
                .then(function(r) { clearTimeout(timer); report({ status: r.status }); })
                .catch(function(e) { clearTimeout(timer); report({ error: String((e && e.message) || e) }); });
        })();
        """.trimIndent()
        webView.evaluateJavascript(probeJs, null)
    }

    private fun fetchApis(
        session: Int,
        carId: String,
        carsJson: JSONObject,
        onStage: (CollectStage) -> Unit,
        callback: (Result) -> Unit,
    ) {
        val base = carsJson.optJSONObject("base")
        val vehicleIdRaw = base?.opt("vehicleId")
        Log.d(TAG, "vehicleId raw: $vehicleIdRaw (${vehicleIdRaw?.javaClass?.simpleName})")
        val vehicleId = when (vehicleIdRaw) {
            is Number -> vehicleIdRaw.toLong()
            is String -> vehicleIdRaw.toLongOrNull() ?: 0L
            else -> 0L
        }
        val vehicleNo = base?.optString("vehicleNo", "") ?: ""
        Log.d(TAG, "vehicleId=$vehicleId hasVehicleNo=${vehicleNo.isNotEmpty()}")

        if (vehicleId == 0L || vehicleNo.isEmpty()) {
            callback(
                Result.Success(
                    buildResultJson(carId, carsJson, null, null, null, emptyMap()),
                ),
            )
            return
        }

        apiResultHandler = onApiResult@{ json ->
            if (session != sessionId) return@onApiResult
            Log.d(TAG, "API result received (${json.length} chars)")
            try {
                val apiResponse = JSONObject(json)
                val results = apiResponse.optJSONObject("results")
                val statuses = apiResponse.optJSONObject("statuses")
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
                Log.e(TAG, "API result parse failed", e)
                callback(
                    Result.Success(
                        buildResultJson(carId, carsJson, null, null, null, emptyMap()),
                    ),
                )
            }
            webView.loadUrl("about:blank")
            apiResultHandler = null
        }

        val vnoEscaped = vehicleNo.replace("\\", "\\\\").replace("'", "\\'")
        val apiJs = """
        (function() {
            var vid = $vehicleId;
            var vno = '$vnoEscaped';
            var base = 'https://api.encar.com/v1/readside';
            var results = {};
            var statuses = {};
            function fetchApi(name, url) {
                return fetch(url)
                    .then(function(r) {
                        statuses[name] = r.ok ? 'ok' : (r.status === 404 ? 'not_found' : (r.status === 401 ? 'unauthorized' : 'error'));
                        return r.ok ? r.json() : null;
                    })
                    .then(function(data) { results[name] = data; })
                    .catch(function(e) { statuses[name] = 'error'; results[name] = null; });
            }
            Promise.all([
                fetchApi('recordJson', base + '/record/vehicle/' + vid + '/open?vehicleNo=' + encodeURIComponent(vno)),
                fetchApi('diagnosisJson', base + '/diagnosis/vehicle/' + vid),
                fetchApi('inspectionJson', base + '/inspection/vehicle/' + vid)
            ]).then(function() {
                ApiCallback.onResult(JSON.stringify({ results: results, statuses: statuses }));
            }).catch(function(e) {
                ApiCallback.onResult(JSON.stringify({ results: {}, statuses: {}, error: e.message }));
            });
        })();
        """.trimIndent()

        onStage(CollectStage.FetchingHistory)
        webView.evaluateJavascript(apiJs, null)
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

    override fun destroy() {
        cancelTimeout()
        // 예약된 추출 재시도/브리지 콜백이 파괴된 WebView에 접근하지 않도록 무효화
        sessionId++
        handler.removeCallbacksAndMessages(null)
        apiResultHandler = null
        webView.removeJavascriptInterface("ApiCallback")
        webView.stopLoading()
        webView.destroy()
    }

    companion object {
        private const val TAG = "CollectorWebView"
        // 콜드 스타트/느린 망에서는 엔카 페이지 로드만 30초 가까이 걸리기도 한다.
        private const val TIMEOUT_MS = 45_000L
        private const val RETRY_DELAY_MS = 1_000L
        private const val MAX_RETRIES = 5
        private const val PROBE_TIMEOUT_MS = 10_000L
        private const val BASE_INCLUDE =
            "ADVERTISEMENT,CATEGORY,CONDITION,CONTACT,MANAGE,OPTIONS,PHOTOS,SPEC,PARTNERSHIP,CENTER,VIEW"
    }
}
