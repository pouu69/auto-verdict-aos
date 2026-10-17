package com.car.autoverdict.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RuleEngineTest {

    private val sample: String =
        requireNotNull(javaClass.classLoader?.getResource("report/sample-caution.json")) {
            "missing report/sample-caution.json"
        }.readText()

    /** Records every host call and lets the test fire engine callbacks by hand. */
    private class FakeHost : EngineHost {
        lateinit var callbacks: EngineCallbacks
        var started = false
        var destroyed = false
        var throwOnStart = false
        var throwOnEvaluate = false
        val sent = mutableListOf<Pair<String, String>>()

        override fun start(callbacks: EngineCallbacks) {
            this.callbacks = callbacks
            if (throwOnStart) error("start boom")
            started = true
        }

        override fun evaluate(requestId: String, rawJson: String) {
            if (throwOnEvaluate) error("destroyed")
            sent += requestId to rawJson
        }

        override fun destroy() {
            destroyed = true
        }
    }

    private class Harness(val engine: RuleEngine, val hosts: List<FakeHost>) {
        val host: FakeHost get() = hosts.last()
    }

    /** The engine dispatcher must share runTest's scheduler so advanceTimeBy drives its timers. */
    private fun TestScope.harness(configure: (index: Int, host: FakeHost) -> Unit = { _, _ -> }): Harness {
        val hosts = mutableListOf<FakeHost>()
        val engine = RuleEngine(
            hostFactory = { FakeHost().also { configure(hosts.size, it); hosts += it } },
            mainDispatcher = StandardTestDispatcher(testScheduler),
        )
        return Harness(engine, hosts)
    }

    private fun EngineResult.assertFailed(): EngineResult.Failed {
        assertTrue("expected Failed but was $this", this is EngineResult.Failed)
        return this as EngineResult.Failed
    }

    @Test
    fun `request after ready resolves with the parsed report`() = runTest {
        val h = harness()
        val first = async { h.engine.evaluate("""{"raw":1}""") }
        runCurrent()
        assertTrue(h.host.started)
        assertTrue("nothing is sent before onReady", h.host.sent.isEmpty())

        h.host.callbacks.onReady()
        runCurrent()
        val (firstId, firstRaw) = h.host.sent.single()
        assertEquals("""{"raw":1}""", firstRaw)
        h.host.callbacks.onReport(firstId, sample)
        runCurrent()
        val report = (first.await() as EngineResult.Success).report
        assertEquals("41878112", report.carId)

        // Engine is ready now: the next request goes straight to the same host.
        val second = async { h.engine.evaluate("""{"raw":2}""") }
        runCurrent()
        assertEquals(1, h.hosts.size)
        val (secondId, secondRaw) = h.host.sent[1]
        assertEquals("""{"raw":2}""", secondRaw)
        h.host.callbacks.onReport(secondId, sample)
        runCurrent()
        assertTrue(second.await() is EngineResult.Success)
        assertEquals(0, h.engine.pendingCount)
    }

    @Test
    fun `requests queued before ready are flushed and matched by requestId`() = runTest {
        val h = harness()
        val a = async { h.engine.evaluate("A") }
        val b = async { h.engine.evaluate("B") }
        runCurrent()
        assertEquals(1, h.hosts.size)
        assertTrue(h.host.sent.isEmpty())

        h.host.callbacks.onReady()
        runCurrent()
        assertEquals(listOf("A", "B"), h.host.sent.map { it.second })
        val idA = h.host.sent[0].first
        val idB = h.host.sent[1].first
        assertFalse(idA == idB)

        // Answer out of order: B fails, A succeeds.
        h.host.callbacks.onEngineError(idB, "boom B")
        h.host.callbacks.onReport(idA, sample)
        runCurrent()
        assertTrue(a.await() is EngineResult.Success)
        assertEquals("boom B", b.await().assertFailed().reason)
    }

    @Test
    fun `missing onReady times out after 10s, destroys the host and recreates it next time`() = runTest {
        val h = harness()
        val first = async { h.engine.evaluate("A") }
        runCurrent()
        val oldHost = h.host

        advanceTimeBy(9_999)
        runCurrent()
        assertFalse(first.isCompleted)

        advanceTimeBy(2)
        runCurrent()
        assertEquals("init timeout", first.await().assertFailed().reason)
        assertTrue(oldHost.destroyed)
        assertEquals(0, h.engine.pendingCount)

        // A late onReady from the discarded host is ignored.
        oldHost.callbacks.onReady()
        runCurrent()

        val second = async { h.engine.evaluate("B") }
        runCurrent()
        assertEquals(2, h.hosts.size)
        assertTrue(oldHost.sent.isEmpty())
        h.host.callbacks.onReady()
        runCurrent()
        h.host.callbacks.onReport(h.host.sent.single().first, sample)
        runCurrent()
        assertTrue(second.await() is EngineResult.Success)
    }

    @Test
    fun `request times out 10s after dispatch and a late report is dropped`() = runTest {
        val h = harness()
        val result = async { h.engine.evaluate("A") }
        runCurrent()

        // Queued time before ready does not count toward the request timeout.
        advanceTimeBy(9_000)
        h.host.callbacks.onReady()
        runCurrent()
        val id = h.host.sent.single().first

        advanceTimeBy(9_999)
        runCurrent()
        assertFalse(result.isCompleted)

        advanceTimeBy(2)
        runCurrent()
        assertEquals("request timeout", result.await().assertFailed().reason)
        assertFalse("request timeout keeps the engine", h.host.destroyed)

        // Late report for a finished request: no exception, no effect.
        h.host.callbacks.onReport(id, sample)
        runCurrent()
        assertEquals(0, h.engine.pendingCount)

        val next = async { h.engine.evaluate("B") }
        runCurrent()
        assertEquals(1, h.hosts.size)
        h.host.callbacks.onReport(h.host.sent[1].first, sample)
        runCurrent()
        assertTrue(next.await() is EngineResult.Success)
    }

    @Test
    fun `render process gone fails pending requests and recreates the host`() = runTest {
        val h = harness()
        val a = async { h.engine.evaluate("A") }
        runCurrent()
        h.host.callbacks.onReady()
        runCurrent()
        val b = async { h.engine.evaluate("B") }
        runCurrent()
        val oldHost = h.host

        oldHost.callbacks.onRenderProcessGone()
        runCurrent()
        assertTrue(a.await() is EngineResult.Failed)
        assertTrue(b.await() is EngineResult.Failed)
        assertTrue(oldHost.destroyed)
        assertEquals(0, h.engine.pendingCount)

        val c = async { h.engine.evaluate("C") }
        runCurrent()
        assertEquals(2, h.hosts.size)
        h.host.callbacks.onReady()
        runCurrent()
        h.host.callbacks.onReport(h.host.sent.single().first, sample)
        runCurrent()
        assertTrue(c.await() is EngineResult.Success)
    }

    @Test
    fun `invalid JSON or an unknown schemaVersion fails the request`() = runTest {
        val h = harness()
        val broken = async { h.engine.evaluate("A") }
        val future = async { h.engine.evaluate("B") }
        runCurrent()
        h.host.callbacks.onReady()
        runCurrent()
        val (idA, _) = h.host.sent[0]
        val (idB, _) = h.host.sent[1]

        h.host.callbacks.onReport(idA, "{not json")
        h.host.callbacks.onReport(idB, sample.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"))
        runCurrent()
        assertTrue(broken.await().assertFailed().reason.startsWith("contract"))
        assertEquals("schemaVersion 2", future.await().assertFailed().reason)
    }

    @Test
    fun `engine error fails the request with its message`() = runTest {
        val h = harness()
        val result = async { h.engine.evaluate("A") }
        runCurrent()
        h.host.callbacks.onReady()
        runCurrent()
        h.host.callbacks.onEngineError(h.host.sent.single().first, "TypeError: x is undefined")
        runCurrent()
        assertEquals("TypeError: x is undefined", result.await().assertFailed().reason)
        assertFalse(h.host.destroyed)
    }

    @Test
    fun `cancelling the caller removes the request from the pending list`() = runTest {
        val h = harness()

        // Cancelled while still queued: never dispatched.
        val queued = launch { h.engine.evaluate("queued") }
        runCurrent()
        queued.cancel()
        runCurrent()
        assertEquals(0, h.engine.pendingCount)
        h.host.callbacks.onReady()
        runCurrent()
        assertTrue(h.host.sent.isEmpty())

        // Cancelled after dispatch: a report for that id later does nothing.
        val inFlight = launch { h.engine.evaluate("in-flight") }
        runCurrent()
        val id = h.host.sent.single().first
        inFlight.cancel()
        runCurrent()
        assertEquals(0, h.engine.pendingCount)
        h.host.callbacks.onReport(id, sample)
        h.host.callbacks.onEngineError(id, "late")
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(0, h.engine.pendingCount)
        assertFalse(h.host.destroyed)
        assertEquals(1, h.hosts.size)
    }

    /** Starts a request on a ready host and lets its request timeout elapse. */
    private suspend fun TestScope.timeOutOneRequest(h: Harness): EngineResult {
        val result = async { h.engine.evaluate("slow") }
        runCurrent()
        advanceTimeBy(10_001)
        runCurrent()
        return result.await()
    }

    @Test
    fun `two consecutive request timeouts discard the unresponsive host`() = runTest {
        val h = harness()
        val warmUp = async { h.engine.evaluate("A") }
        runCurrent()
        h.host.callbacks.onReady()
        runCurrent()
        h.host.callbacks.onReport(h.host.sent.single().first, sample)
        runCurrent()
        assertTrue(warmUp.await() is EngineResult.Success)
        val oldHost = h.host

        assertEquals("request timeout", timeOutOneRequest(h).assertFailed().reason)
        assertFalse("one timeout keeps the host", oldHost.destroyed)
        assertEquals("request timeout", timeOutOneRequest(h).assertFailed().reason)
        assertTrue("second consecutive timeout discards the host", oldHost.destroyed)

        val next = async { h.engine.evaluate("B") }
        runCurrent()
        assertEquals(2, h.hosts.size)
        h.host.callbacks.onReady()
        runCurrent()
        h.host.callbacks.onReport(h.host.sent.single().first, sample)
        runCurrent()
        assertTrue(next.await() is EngineResult.Success)
    }

    @Test
    fun `a response between timeouts resets the unresponsive count`() = runTest {
        val h = harness()
        val warmUp = async { h.engine.evaluate("A") }
        runCurrent()
        h.host.callbacks.onReady()
        runCurrent()
        h.host.callbacks.onReport(h.host.sent.single().first, sample)
        runCurrent()
        warmUp.await()

        timeOutOneRequest(h).assertFailed()
        val ok = async { h.engine.evaluate("B") }
        runCurrent()
        h.host.callbacks.onEngineError(h.host.sent.last().first, "js said no")
        runCurrent()
        ok.await().assertFailed()
        timeOutOneRequest(h).assertFailed()

        assertFalse(h.host.destroyed)
        assertEquals(1, h.hosts.size)
    }

    @Test
    fun `host start failure fails the request immediately and the next one recreates it`() = runTest {
        val h = harness { index, host -> host.throwOnStart = index == 0 }
        val first = async { h.engine.evaluate("A") }
        runCurrent()
        assertTrue("fails without waiting for a timeout", first.isCompleted)
        assertTrue(first.await().assertFailed().reason.startsWith("engine start failed"))
        assertTrue(h.hosts[0].destroyed)
        assertEquals(0, h.engine.pendingCount)

        val second = async { h.engine.evaluate("B") }
        runCurrent()
        assertEquals(2, h.hosts.size)
        h.host.callbacks.onReady()
        runCurrent()
        h.host.callbacks.onReport(h.host.sent.single().first, sample)
        runCurrent()
        assertTrue(second.await() is EngineResult.Success)
    }

    @Test
    fun `host evaluate failure fails the request immediately and the next one still works`() = runTest {
        val h = harness()
        val queued = async { h.engine.evaluate("A") }
        runCurrent()
        h.host.throwOnEvaluate = true
        h.host.callbacks.onReady()
        runCurrent()
        assertTrue("flush failure fails without waiting for a timeout", queued.isCompleted)
        assertTrue(queued.await().assertFailed().reason.startsWith("dispatch failed"))

        val direct = async { h.engine.evaluate("B") }
        runCurrent()
        assertTrue("direct dispatch failure fails without waiting for a timeout", direct.isCompleted)
        assertTrue(direct.await().assertFailed().reason.startsWith("dispatch failed"))
        assertEquals(0, h.engine.pendingCount)

        h.host.throwOnEvaluate = false
        val next = async { h.engine.evaluate("C") }
        runCurrent()
        h.host.callbacks.onReport(h.host.sent.single().first, sample)
        runCurrent()
        assertTrue(next.await() is EngineResult.Success)
        assertEquals(1, h.hosts.size)
    }
}
