package com.car.autoverdict.result

import com.car.autoverdict.engine.EngineResult
import com.car.autoverdict.engine.ReportEvaluator
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.GapReason
import com.car.autoverdict.util.EncarUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Result screen state machine (spec 5.1). Data source order: saved row (then a silent
 * background refresh) → 24h cache → collector. Saved status always comes from the DB.
 *
 * Plain class, not a ViewModel: the screen creates it with `remember(route)` and a composition
 * scope. [scope] must be main-confined — all fields are touched without locking.
 */
class ResultController(
    private val url: String,
    private val repository: ResultRepository,
    private val evaluator: ReportEvaluator,
    private val scope: CoroutineScope,
    private val carIdOf: (String) -> String? = EncarUrl::extractCarId,
) {
    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Loading(LoadingStage.OpeningPage))
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    private val _events = Channel<ResultEvent>(Channel.BUFFERED)
    val events: Flow<ResultEvent> = _events.receiveAsFlow()

    /** A report and the raw collector payload it was built from (what save/cache store). */
    private data class Evaluated(val report: EvaluationReport, val rawJson: String)

    private val carId: String? = carIdOf(url)
    private var started = false
    private var loadJob: Job? = null

    /** Bumped per load so a cancelled collect's late progress callbacks are dropped. */
    private var generation = 0

    private var shown: Evaluated? = null
    private var pendingUpdate: Evaluated? = null
    private var saved = false
    private var savedAt: Long? = null
    private var saving = false
    private var refreshing = false
    private var soldOut = false
    private var userScrolled = false

    /** Serialises saved-row writes so a refresh never races a save/unsave. */
    private val savedRowLock = Mutex()

    fun start() {
        if (started) return
        started = true
        val id = carId ?: run {
            _state.value = ResultUiState.Error(ResultErrorKind.InvalidUrl)
            return
        }
        val gen = ++generation
        loadJob = scope.launch {
            val entity = readOrNull { repository.savedCar(id) }
            saved = entity != null
            savedAt = entity?.savedAt
            if (entity != null) {
                val report = evaluate(entity.rawJson)
                if (report != null) {
                    display(Evaluated(report, entity.rawJson))
                    refreshSaved(id)
                    return@launch
                }
                // A saved row the engine can't read: collect again (plan Task 7, step ①).
                collectFresh(id, gen)
                return@launch
            }
            val cached = readOrNull { repository.cachedRaw(id) }
            if (cached != null) {
                val report = evaluate(cached)
                if (report != null) {
                    display(Evaluated(report, cached))
                    return@launch
                }
            }
            collectFresh(id, gen)
        }
    }

    /**
     * Skips the cache and collects again. With a report on screen it re-collects in place
     * (`Ready.refreshing`) and keeps that report if the retry does not produce a better one;
     * otherwise (error screen) it starts over with the loading steps. Ignored while refreshing.
     */
    fun retry() {
        val id = carId ?: return
        if (refreshing) return
        started = true
        loadJob?.cancel()
        pendingUpdate = null
        val gen = ++generation
        val before = shown
        if (before != null && _state.value is ResultUiState.Ready) {
            refreshing = true
            publishReady()
            loadJob = scope.launch { retryInPlace(id, before) }
        } else {
            _state.value = ResultUiState.Loading(LoadingStage.OpeningPage)
            loadJob = scope.launch { collectFresh(id, gen) }
        }
    }

    /** From now on a background refresh waits behind the UpdateAvailable banner. */
    fun onUserScrolled() {
        userScrolled = true
    }

    fun applyUpdate() {
        val update = pendingUpdate ?: return
        pendingUpdate = null
        soldOut = false
        display(update)
        scope.launch { persist(update) }
    }

    fun save() {
        if (_state.value !is ResultUiState.Ready || saving || saved) return
        saving = true
        publishReady()
        scope.launch {
            val ok = write {
                savedRowLock.withLock {
                    // Read under the lock: an update may have swapped the report since the tap.
                    val current = checkNotNull(shown)
                    savedAt = repository.save(current.report, current.rawJson)
                    saved = true
                }
            }
            saving = false
            // Screen gone: the row is written, but there is nothing left to update or notify.
            if (!isActive) return@launch
            publishReady()
            _events.trySend(if (ok) ResultEvent.Saved else ResultEvent.SaveFailed)
        }
    }

    fun unsave() {
        if (_state.value !is ResultUiState.Ready || saving || !saved) return
        val id = carId ?: return
        saving = true
        publishReady()
        scope.launch {
            val ok = write {
                savedRowLock.withLock {
                    repository.delete(id)
                    saved = false
                    savedAt = null
                }
            }
            saving = false
            if (!isActive) return@launch
            publishReady()
            _events.trySend(if (ok) ResultEvent.Unsaved else ResultEvent.SaveFailed)
        }
    }

    // --- loading ---

    /** Step ③ with no report on screen: visible loading steps, then Ready / Error / Deleted. */
    private suspend fun collectFresh(id: String, gen: Int) {
        shown = null
        _state.value = ResultUiState.Loading(LoadingStage.OpeningPage)
        val outcome = collect(id) { stage ->
            if (gen == generation) _state.value = ResultUiState.Loading(stage.toLoadingStage())
        }
        when (outcome) {
            is CollectOutcome.Success -> {
                _state.value = ResultUiState.Loading(LoadingStage.Evaluating)
                val report = evaluate(outcome.rawJson)
                if (report == null) {
                    _state.value = ResultUiState.Error(ResultErrorKind.EngineFailed)
                } else {
                    val fresh = Evaluated(report, outcome.rawJson)
                    soldOut = false
                    display(fresh)
                    persist(fresh)
                }
            }
            is CollectOutcome.Failed -> _state.value = ResultUiState.Error(outcome.kind.toResultErrorKind())
            CollectOutcome.Deleted -> _state.value = ResultUiState.Deleted
        }
    }

    /**
     * User retry with [before] on screen. A usable result replaces it right away (no scroll
     * rule — the user asked); failure, engine failure or more data gaps keep it + RetryFailed.
     */
    private suspend fun retryInPlace(id: String, before: Evaluated) {
        val outcome: CollectOutcome
        val fresh: Evaluated?
        var finished = false
        try {
            outcome = collect(id) {}
            fresh = (outcome as? CollectOutcome.Success)?.let { success ->
                evaluate(success.rawJson)
                    ?.takeIf { it.dataGaps.size <= before.report.dataGaps.size }
                    ?.let { Evaluated(it, success.rawJson) }
            }
            finished = true
        } finally {
            refreshing = false
            // Cancelled mid-retry: keep the shown report, no longer marked refreshing.
            if (!finished) publishReady()
        }
        when {
            fresh != null -> {
                soldOut = false
                display(fresh)
                persist(fresh)
            }
            outcome == CollectOutcome.Deleted ->
                if (saved) {
                    soldOut = true
                    publishReady()
                } else {
                    _state.value = ResultUiState.Deleted
                }
            else -> {
                publishReady()
                _events.trySend(ResultEvent.RetryFailed)
            }
        }
    }

    /** Silent re-collect for a saved car already on screen (spec 5.1 step 1). */
    private suspend fun refreshSaved(id: String) {
        when (val outcome = collect(id) {}) {
            is CollectOutcome.Failed -> Unit
            CollectOutcome.Deleted -> {
                soldOut = true
                publishReady()
            }
            is CollectOutcome.Success -> {
                val report = evaluate(outcome.rawJson) ?: return
                val current = shown ?: return
                // More data gaps than the saved result (e.g. logged out): keep the saved one.
                if (report.dataGaps.size > current.report.dataGaps.size) return
                val fresh = Evaluated(report, outcome.rawJson)
                if (userScrolled) {
                    pendingUpdate = fresh
                    publishReady()
                } else {
                    display(fresh)
                    persist(fresh)
                }
            }
        }
    }

    /** Caches gap-free results and refreshes the saved row (keeping savedAt) if still saved. */
    private suspend fun persist(fresh: Evaluated) {
        // One non-cancellable block: leaving the screen between the two writes can't skip the second.
        withContext(NonCancellable) {
            if (fresh.report.dataGaps.isEmpty()) {
                tryWrite { repository.writeCache(fresh.report, fresh.rawJson) }
            }
            tryWrite {
                savedRowLock.withLock {
                    if (saved) savedAt = repository.save(fresh.report, fresh.rawJson, savedAt = savedAt)
                }
            }
        }
    }

    // --- state ---

    private fun display(evaluated: Evaluated) {
        shown = evaluated
        publishReady(force = true)
    }

    /**
     * Rebuilds Ready from the fields. Without [force] it only refreshes an existing Ready,
     * so a late save/refresh never replaces the loading steps or an error.
     */
    private fun publishReady(force: Boolean = false) {
        val current = shown ?: return
        if (!force && _state.value !is ResultUiState.Ready) return
        _state.value = ResultUiState.Ready(
            report = current.report,
            saved = saved,
            banner = banner(current.report),
            saving = saving,
            refreshing = refreshing,
        )
    }

    private fun banner(report: EvaluationReport): ResultBanner? = when {
        soldOut -> ResultBanner.SoldOut
        pendingUpdate != null -> ResultBanner.UpdateAvailable
        report.dataGaps.any { it.reason == GapReason.LoginRequired } -> ResultBanner.LoginRequired
        report.dataGaps.any { it.reason == GapReason.Failed } -> ResultBanner.PartialData
        else -> null
    }

    // --- helpers ---

    private suspend fun collect(id: String, onStage: (CollectStage) -> Unit): CollectOutcome =
        try {
            repository.collect(url, id, onStage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // e.g. the system WebView failed to start.
            CollectOutcome.Failed(CollectErrorKind.PageUnreadable)
        }

    private suspend fun evaluate(rawJson: String): EvaluationReport? =
        try {
            (evaluator.evaluate(rawJson) as? EngineResult.Success)?.report
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private suspend fun <T> readOrNull(block: suspend () -> T?): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    /**
     * Runs the DB write [block]; false if it threw. Non-cancellable: once started, a save,
     * delete or cache write finishes even if the user leaves the screen (the scope is cancelled).
     * Callers skip their UI follow-up when cancelled.
     */
    private suspend fun write(block: suspend () -> Unit): Boolean =
        withContext(NonCancellable) { tryWrite(block) }

    /** Runs [block]; false if it threw (cancellation still propagates). Callers own the context. */
    private suspend fun tryWrite(block: suspend () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }

    private fun CollectStage.toLoadingStage(): LoadingStage = when (this) {
        CollectStage.OpeningPage -> LoadingStage.OpeningPage
        CollectStage.FetchingHistory -> LoadingStage.FetchingHistory
    }

    private fun CollectErrorKind.toResultErrorKind(): ResultErrorKind = when (this) {
        CollectErrorKind.Network -> ResultErrorKind.Network
        CollectErrorKind.Timeout -> ResultErrorKind.Timeout
        CollectErrorKind.PageUnreadable -> ResultErrorKind.PageUnreadable
    }
}
