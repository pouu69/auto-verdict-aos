package com.car.autoverdict.webview

import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
class EvalWebViewTest {

    private lateinit var db: AppDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var eval: EvalWebView
    private lateinit var bridge: NativeBridge
    private lateinit var shadow: ShadowWebView

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        eval = EvalWebView(ApplicationProvider.getApplicationContext())
        bridge = NativeBridge(
            database = db,
            scope = scope,
            onClose = {},
            appContext = ApplicationProvider.getApplicationContext(),
        )
        eval.addBridge(bridge)
        eval.loadEvalUi()
        shadow = shadowOf(eval.webView)
    }

    @After
    fun teardown() {
        scope.cancel()
        eval.destroy()
        db.close()
    }

    @Test
    fun `data sent before page load is held until both page load and JS ready`() {
        eval.sendData("""{"carId":"1"}""")
        assertNull(shadow.lastEvaluatedJavascript)

        pageFinished()
        // The old bug: flushing here would no-op because React hasn't registered handlers.
        assertNull(shadow.lastEvaluatedJavascript)

        jsReady()
        assertEquals("""window.receiveEncarData?.('{"carId":"1"}')""", shadow.lastEvaluatedJavascript)
    }

    @Test
    fun `JS ready arriving before page load still waits for page load`() {
        eval.sendData("""{"carId":"2"}""")
        jsReady()
        assertNull(shadow.lastEvaluatedJavascript)

        pageFinished()
        assertEquals("""window.receiveEncarData?.('{"carId":"2"}')""", shadow.lastEvaluatedJavascript)
    }

    @Test
    fun `queue keeps only the latest payload`() {
        eval.sendData("""{"carId":"old"}""")
        eval.sendData("""{"carId":"new"}""")
        pageFinished()
        jsReady()
        assertEquals("""window.receiveEncarData?.('{"carId":"new"}')""", shadow.lastEvaluatedJavascript)
    }

    @Test
    fun `pending alreadySaved is flushed after data`() {
        eval.sendData("""{"carId":"3"}""")
        eval.setAlreadySaved(true)
        pageFinished()
        jsReady()
        assertEquals("window.setAlreadySaved?.(true)", shadow.lastEvaluatedJavascript)
    }

    @Test
    fun `pending error and deleted are flushed on ready`() {
        eval.sendError("boom")
        pageFinished()
        jsReady()
        assertTrue(shadow.lastEvaluatedJavascript!!.startsWith("window.receiveError?.("))

        eval.loadEvalUi()
        eval.sendDeleted("99", isSaved = true)
        pageFinished()
        jsReady()
        assertEquals(
            """window.receiveDeleted?.('{"carId":"99","isSaved":true}')""",
            shadow.lastEvaluatedJavascript,
        )
    }

    @Test
    fun `after ready, sends go straight through`() {
        pageFinished()
        jsReady()
        eval.sendData("""{"carId":"4"}""")
        assertEquals("""window.receiveEncarData?.('{"carId":"4"}')""", shadow.lastEvaluatedJavascript)
    }

    @Test
    fun `loadEvalUi resets readiness so sends are queued again`() {
        pageFinished()
        jsReady()
        eval.sendData("""{"carId":"5"}""")

        eval.loadEvalUi()
        eval.sendData("""{"carId":"6"}""")
        assertEquals("""window.receiveEncarData?.('{"carId":"5"}')""", shadow.lastEvaluatedJavascript)

        pageFinished()
        assertEquals("""window.receiveEncarData?.('{"carId":"5"}')""", shadow.lastEvaluatedJavascript)
        jsReady()
        assertEquals("""window.receiveEncarData?.('{"carId":"6"}')""", shadow.lastEvaluatedJavascript)
    }

    @Test
    fun `bridge onReady from a background thread is delivered on main`() {
        eval.sendData("""{"carId":"7"}""")
        pageFinished()

        val t = Thread { bridge.onReady() }
        t.start()
        t.join()
        assertNull(shadow.lastEvaluatedJavascript)

        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("""window.receiveEncarData?.('{"carId":"7"}')""", shadow.lastEvaluatedJavascript)
    }

    private fun pageFinished() {
        shadow.webViewClient.onPageFinished(eval.webView, "file:///android_asset/eval-ui/index.html")
    }

    /** Simulates the JS `Android.onReady()` call (JavaBridge thread → main). */
    private fun jsReady() {
        bridge.onReady()
        shadowOf(Looper.getMainLooper()).idle()
    }
}
