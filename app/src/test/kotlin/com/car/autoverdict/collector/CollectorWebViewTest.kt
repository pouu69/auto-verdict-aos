package com.car.autoverdict.collector

import android.os.Looper
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.result.CollectErrorKind
import com.car.autoverdict.result.CollectStage
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowWebView
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class CollectorWebViewTest {

    private lateinit var collector: CollectorWebView
    private lateinit var shadow: ShadowWebView
    private val results = mutableListOf<CollectorWebView.Result>()
    private val stages = mutableListOf<CollectStage>()

    @Before
    fun setup() {
        collector = CollectorWebView(ApplicationProvider.getApplicationContext())
        val field = CollectorWebView::class.java.getDeclaredField("webView")
        field.isAccessible = true
        shadow = shadowOf(field.get(collector) as WebView)
    }

    @After
    fun tearDown() {
        collector.destroy()
    }

    private fun collect(carId: String = CAR_ID, sink: MutableList<CollectorWebView.Result> = results) {
        collector.collect(URL, carId, onStage = { stages += it }) { sink += it }
    }

    private fun idle(seconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    }

    private fun commitVisible() = shadow.webViewClient.onPageCommitVisible(null, URL)

    private fun pageFinished() = shadow.webViewClient.onPageFinished(null, URL)

    // evaluateJavascript는 JS 문자열 반환값을 JSON 인코딩된 문자열로 돌려준다
    private fun respond(json: String) {
        shadow.lastEvaluatedJavascriptCallback.onReceiveValue(JSONObject.quote(json))
    }

    @Test
    fun timeout_firesOnceAfter45Seconds() {
        collect()

        idle(30)
        assertTrue("slow page load must not time out at 30s", results.isEmpty())

        idle(15)
        assertEquals(1, results.size)
        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.Timeout, error.kind)
        assertTrue(error.message.contains("시간 초과"))

        idle(60)
        assertEquals(1, results.size)
    }

    @Test
    fun extraction_startsOnCommitVisible_beforePageFinished() {
        collect()
        commitVisible()

        assertTrue(shadow.lastEvaluatedJavascript.contains("__PRELOADED_STATE__"))
        respond(SOLD_STATE)
        assertEquals(listOf(CollectorWebView.Result.Deleted), results)

        // 늦게 도착한 onPageFinished나 타임아웃이 두 번째 콜백을 만들면 안 된다
        val callbackBefore = shadow.lastEvaluatedJavascriptCallback
        pageFinished()
        idle(60)
        assertEquals(callbackBefore, shadow.lastEvaluatedJavascriptCallback)
        assertEquals(1, results.size)
    }

    @Test
    fun emptyStateBeforePageFinished_pollsWithoutConsumingRetries() {
        collect()
        commitVisible()

        // 로드 완료 전에는 MAX_RETRIES(5)를 넘겨도 실패하지 않고 계속 폴링
        repeat(10) {
            respond("""{"error":"empty_state"}""")
            idle(1)
        }
        assertTrue(results.isEmpty())

        // 로드 완료 후에는 기존과 같이 최초 1회 + 재시도 5회 후 실패
        pageFinished()
        repeat(6) {
            assertTrue(results.isEmpty())
            respond("""{"error":"empty_state"}""")
            idle(1)
        }
        assertEquals(1, results.size)
        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.PageUnreadable, error.kind)
        assertEquals("매물 데이터를 찾을 수 없습니다", error.message)
    }

    @Test
    fun extractionJs_reportsBaseFetchFailureWithEmptyState() {
        collect()
        commitVisible()

        assertTrue(shadow.lastEvaluatedJavascript.contains("pender"))
    }

    // The base-API probe answers through the ApiCallback bridge.
    private fun probeResponds(json: String) {
        val bridge = shadow.getJavascriptInterface("ApiCallback") as CollectorWebView.ApiBridge
        bridge.onResult(json)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun missingListing_afterPageFinished_probes404_reportsDeleted() {
        collect(carId = "30000000")
        pageFinished()
        respond(MISSING_STATE)

        assertTrue("deletion needs a confirmed 404", results.isEmpty())
        assertTrue(shadow.lastEvaluatedJavascript.contains("api.encar.com/v1/readside/vehicle/"))
        probeResponds("""{"status":404}""")

        assertEquals(listOf(CollectorWebView.Result.Deleted), results)
        assertEquals(listOf(CollectStage.OpeningPage), stages)
    }

    @Test
    fun missingListing_probeServerError_reportsRetryablePageUnreadable() {
        collect(carId = "30000000")
        pageFinished()
        respond(MISSING_STATE)
        probeResponds("""{"status":503}""")

        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.PageUnreadable, error.kind)
    }

    @Test
    fun missingListing_beforePageFinished_keepsPollingThenReportsDeleted() {
        collect(carId = "30000000")
        commitVisible()

        repeat(10) {
            respond(MISSING_STATE)
            idle(1)
        }
        assertTrue(results.isEmpty())

        pageFinished()
        respond(MISSING_STATE)
        probeResponds("""{"status":404}""")
        assertEquals(listOf(CollectorWebView.Result.Deleted), results)
    }

    @Test
    fun emptyBaseWithoutFailureFlag_afterPageFinished_reportsPageUnreadable() {
        collect()
        pageFinished()
        val emptyBaseNoFailure =
            """{"error":"empty_state","state":{"pender":{"failure":{}},"cars":{"base":{}}}}"""
        repeat(6) {
            assertTrue(results.isEmpty())
            respond(emptyBaseNoFailure)
            idle(1)
        }

        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.PageUnreadable, error.kind)
    }

    @Test
    fun recollect_dropsStaleSessionCallbacks() {
        val first = mutableListOf<CollectorWebView.Result>()
        collect(sink = first)
        commitVisible()
        val staleCallback = shadow.lastEvaluatedJavascriptCallback

        collect(carId = "99999999")
        staleCallback.onReceiveValue(JSONObject.quote(SOLD_STATE))
        assertTrue(first.isEmpty())
        assertTrue(results.isEmpty())

        idle(45)
        assertTrue("first session's timeout must be cancelled", first.isEmpty())
        assertEquals(1, results.size)
        assertTrue(results.single() is CollectorWebView.Result.Error)
    }

    @Test
    @Suppress("DEPRECATION")
    fun receivedError_reportsNetworkKind() {
        collect()
        shadow.webViewClient.onReceivedError(null, -2, "net::ERR_INTERNET_DISCONNECTED", URL)

        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.Network, error.kind)
    }

    @Test
    fun renderProcessGone_endsWithPageUnreadableAndKeepsAppAlive() {
        collect()
        commitVisible()

        // false would let the shared renderer crash take the whole app down
        assertTrue(shadow.webViewClient.onRenderProcessGone(null, null))
        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.PageUnreadable, error.kind)

        idle(60)
        assertEquals(1, results.size)
    }

    @Test
    fun unparseableState_reportsPageUnreadable() {
        collect()
        commitVisible()
        respond("not json")

        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.PageUnreadable, error.kind)
    }

    @Test
    fun nullStateAfterPageFinished_reportsPageUnreadable() {
        collect()
        pageFinished()
        shadow.lastEvaluatedJavascriptCallback.onReceiveValue("null")

        val error = results.single() as CollectorWebView.Result.Error
        assertEquals(CollectErrorKind.PageUnreadable, error.kind)
    }

    @Test
    fun stages_openingPageOnLoad_thenFetchingHistoryBeforeApiCalls() {
        collect()
        assertEquals(listOf(CollectStage.OpeningPage), stages)

        commitVisible()
        // State extraction alone is not the history step.
        assertEquals(listOf(CollectStage.OpeningPage), stages)

        respond("""{"ok":true,"cars":{"base":{"vehicleId":123,"vehicleNo":"12가3456"}}}""")
        assertEquals(listOf(CollectStage.OpeningPage, CollectStage.FetchingHistory), stages)
        assertTrue(shadow.lastEvaluatedJavascript.contains("api.encar.com"))
    }

    @Test
    fun stages_noFetchingHistoryWithoutVehicleId() {
        collect()
        commitVisible()
        respond("""{"ok":true,"cars":{"base":{"vehicleNo":""}}}""")

        assertEquals(listOf(CollectStage.OpeningPage), stages)
        assertTrue(results.single() is CollectorWebView.Result.Success)
    }

    @Test
    fun dummyAdOfLiveCar_fetchesOriginalVehicleApis() {
        // fem.encar.com/cars/detail/42753994 serves vehicle 42751994 (same car) as a dummy ad.
        collect(carId = "42753994")
        commitVisible()
        respond(dummyAdState(vehicleNo = "12가3456"))

        assertTrue(results.isEmpty())
        assertEquals(listOf(CollectStage.OpeningPage, CollectStage.FetchingHistory), stages)
        assertTrue(shadow.lastEvaluatedJavascript.contains("var vid = 42751994;"))
    }

    @Test
    fun dummyAdOfLiveCar_keepsRequestedCarIdForSave() {
        collect(carId = "42753994")
        commitVisible()
        respond(dummyAdState(vehicleNo = ""))

        val success = results.single() as CollectorWebView.Result.Success
        val json = JSONObject(success.json)
        assertEquals("42753994", json.getString("carId"))
        assertEquals("https://fem.encar.com/cars/detail/42753994", json.getString("url"))
    }

    @Test
    fun soldListing_reportsDeleted() {
        collect()
        commitVisible()
        respond(SOLD_STATE)

        assertEquals(listOf(CollectorWebView.Result.Deleted), results)
        assertEquals(listOf(CollectStage.OpeningPage), stages)
    }

    @Test
    fun differentCarThanRequested_reportsDeleted() {
        collect(carId = "42000002")
        commitVisible()
        respond(
            """{"ok":true,"cars":{"base":{"vehicleId":41000001,"queryCarId":42000002,""" +
                """"vehicleNo":"12가3456","manage":{"dummy":true,"dummyVehicleId":null},""" +
                """"advertisement":{"status":"ADVERTISE"}}}}""",
        )

        assertEquals(listOf(CollectorWebView.Result.Deleted), results)
    }

    @Test
    fun destroy_cancelsTimeoutAndPendingRetries() {
        collect()
        commitVisible()
        respond("""{"error":"empty_state"}""")

        collector.destroy()
        idle(60)
        assertTrue(results.isEmpty())
    }

    companion object {
        private const val CAR_ID = "38000000"
        private const val URL = "https://fem.encar.com/cars/detail/$CAR_ID"

        // Shape of /cars/detail/38000000 (sold), reduced to the fields the check reads.
        private const val SOLD_STATE =
            """{"ok":true,"cars":{"base":{"vehicleId":38000000,"queryCarId":38000000,""" +
                """"vehicleNo":"12가3456","manage":{"dummy":false,"dummyVehicleId":null},""" +
                """"advertisement":{"status":"SOLD"}}}}"""

        // Shape of /cars/detail/30000000 (no longer exists): empty base, base fetch failed.
        private const val MISSING_STATE =
            """{"error":"empty_state","state":{"pender":{"failure":{"cars/GET_BASE":true}},""" +
                """"cars":{"base":{}}}}"""

        private fun dummyAdState(vehicleNo: String) =
            """{"ok":true,"cars":{"base":{"vehicleId":42751994,"queryCarId":42753994,""" +
                """"vehicleNo":"$vehicleNo","manage":{"dummy":true,"dummyVehicleId":42753994,""" +
                """"reRegistered":false},"advertisement":{"status":"ADVERTISE"}}}}"""
    }
}
