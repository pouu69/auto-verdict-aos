package com.car.autoverdict.compare

import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.engine.EngineResult
import com.car.autoverdict.engine.ReportEvaluator
import com.car.autoverdict.report.EvaluationReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface CompareUiState {
    data object Loading : CompareUiState

    data class Ready(val table: CompareTable) : CompareUiState

    /** Fewer than two of the chosen cars are still saved (e.g. unsaved from the result screen). */
    data object TooFew : CompareUiState
}

/**
 * Compare screen state (spec 3 6.1): follows the chosen saved rows and re-evaluates each one's
 * rawJson, so columns show current reports. A failed evaluation becomes a saved-summary column.
 *
 * Plain class with the same lifetime rules as `ResultController`: created with `remember(route)`
 * and a composition scope. [scope] must be main-confined — the memo is touched without locking.
 */
class CompareController(
    carIds: List<String>,
    private val observe: (List<String>) -> Flow<List<SavedCarEntity>>,
    private val evaluator: ReportEvaluator,
    private val scope: CoroutineScope,
) {
    private val ids = carIds.take(MAX_CARS)

    private val _state = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    /** carId → (updatedAt, report or null on failure): unchanged rows are not evaluated again. */
    private var memo: Map<String, Pair<Long, EvaluationReport?>> = emptyMap()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            observe(ids)
                // The DAO returns rows in table order; columns follow the user's choice.
                .map { rows -> ids.mapNotNull { id -> rows.firstOrNull { it.carId == id } } }
                .distinctUntilChanged()
                .collectLatest { rows ->
                    if (rows.size < MIN_CARS) {
                        _state.value = CompareUiState.TooFew
                        return@collectLatest
                    }
                    val inputs = coroutineScope {
                        rows.map { row -> async { CompareInput(row, reportFor(row)) } }.awaitAll()
                    }
                    _state.value = CompareUiState.Ready(buildCompareTable(inputs))
                }
        }
    }

    private suspend fun reportFor(row: SavedCarEntity): EvaluationReport? {
        memo[row.carId]?.let { (updatedAt, report) -> if (updatedAt == row.updatedAt) return report }
        val report = evaluate(row.rawJson)
        memo = memo + (row.carId to (row.updatedAt to report))
        return report
    }

    private suspend fun evaluate(rawJson: String): EvaluationReport? =
        try {
            (evaluator.evaluate(rawJson) as? EngineResult.Success)?.report
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    companion object {
        const val MIN_CARS = 2
        const val MAX_CARS = 3
    }
}
