package com.car.autoverdict.engine

import androidx.annotation.VisibleForTesting
import com.car.autoverdict.report.ReportParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App-scoped headless rule engine (spec 3.3).
 *
 * The engine page is created lazily on the first request and reused afterwards.
 * Requests sent before the page signals `onReady` are queued and flushed on ready.
 * Each request is matched by its own requestId, so several may be in flight at once.
 *
 * Lifecycle rules:
 * - init timeout ([initTimeoutMs], from host creation): host destroyed, every pending request
 *   fails, the next request creates a new host.
 * - request timeout ([requestTimeoutMs], from dispatch to the page): only that request fails.
 * - renderer gone: same as init timeout.
 * - caller cancellation: the request is dropped; a late report for its id is ignored.
 *
 * All state lives on [mainDispatcher]; host callbacks must arrive on the same thread.
 */
class RuleEngine(
    private val hostFactory: () -> EngineHost,
    private val initTimeoutMs: Long = 10_000,
    private val requestTimeoutMs: Long = 10_000,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ReportEvaluator {

    private class Request(val rawJson: String) {
        val result = CompletableDeferred<EngineResult>()
        var dispatched = false
        var timeoutJob: Job? = null
    }

    /** Timers live here so they are not tied to any single caller coroutine. */
    private val engineScope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private var host: EngineHost? = null
    private var ready = false
    private var initTimeoutJob: Job? = null
    private var nextRequestId = 0L

    /** Insertion-ordered so queued requests are flushed in arrival order. */
    private val pending = LinkedHashMap<String, Request>()

    @get:VisibleForTesting
    internal val pendingCount: Int get() = pending.size

    override suspend fun evaluate(rawJson: String): EngineResult = withContext(mainDispatcher) {
        val requestId = (++nextRequestId).toString()
        val request = Request(rawJson)
        pending[requestId] = request
        try {
            val current = host ?: createHost()
            if (current != null && ready) dispatch(current, requestId, request)
            request.result.await()
        } finally {
            // Normal completion already removed it; this covers caller cancellation.
            pending.remove(requestId)?.timeoutJob?.cancel()
        }
    }

    /** Returns the new host, or null if it failed to start (pending requests are then failed). */
    private fun createHost(): EngineHost? {
        val created = try {
            hostFactory()
        } catch (e: Exception) {
            failAll("engine create failed: ${e.message}")
            return null
        }
        host = created
        ready = false
        initTimeoutJob = engineScope.launch {
            delay(initTimeoutMs)
            if (host === created && !ready) tearDown("init timeout")
        }
        try {
            created.start(HostCallbacks(created))
        } catch (e: Exception) {
            tearDown("engine start failed: ${e.message}")
            return null
        }
        return created
    }

    private fun dispatch(target: EngineHost, requestId: String, request: Request) {
        request.dispatched = true
        request.timeoutJob = engineScope.launch {
            delay(requestTimeoutMs)
            complete(requestId, EngineResult.Failed("request timeout"))
        }
        try {
            target.evaluate(requestId, request.rawJson)
        } catch (e: Exception) {
            complete(requestId, EngineResult.Failed("dispatch failed: ${e.message}"))
        }
    }

    private fun complete(requestId: String, result: EngineResult) {
        val request = pending.remove(requestId) ?: return // unknown, cancelled or already finished
        request.timeoutJob?.cancel()
        request.result.complete(result)
    }

    /** Discards the current host; the next request creates a new one. */
    private fun tearDown(reason: String) {
        initTimeoutJob?.cancel()
        initTimeoutJob = null
        val old = host
        host = null
        ready = false
        try {
            old?.destroy()
        } finally {
            failAll(reason)
        }
    }

    private fun failAll(reason: String) {
        val requests = pending.values.toList()
        pending.clear()
        requests.forEach {
            it.timeoutJob?.cancel()
            it.result.complete(EngineResult.Failed(reason))
        }
    }

    /** Bound to one host instance so signals from a discarded host are ignored. */
    private inner class HostCallbacks(private val owner: EngineHost) : EngineCallbacks {
        private val isCurrent: Boolean get() = host === owner

        override fun onReady() {
            if (!isCurrent || ready) return
            ready = true
            initTimeoutJob?.cancel()
            initTimeoutJob = null
            pending.entries.toList()
                .filter { !it.value.dispatched }
                .forEach { (id, request) -> if (isCurrent) dispatch(owner, id, request) }
        }

        override fun onReport(requestId: String, reportJson: String) {
            if (!isCurrent || requestId !in pending) return
            val result = when (val parsed = ReportParser.parse(reportJson)) {
                is ReportParser.Result.Ok -> EngineResult.Success(parsed.report)
                is ReportParser.Result.Invalid -> EngineResult.Failed(parsed.reason)
            }
            complete(requestId, result)
        }

        override fun onEngineError(requestId: String, message: String) {
            if (!isCurrent) return
            complete(requestId, EngineResult.Failed(message))
        }

        override fun onRenderProcessGone() {
            if (!isCurrent) return
            tearDown("render process gone")
        }
    }
}
