package com.car.autoverdict.result

import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.report.EvaluationReport

/** Collector progress, shown as the first two loading steps (spec 5.1). */
enum class CollectStage { OpeningPage, FetchingHistory }

/** Why collection failed (spec 5.1 error table). */
enum class CollectErrorKind { Network, Timeout, PageUnreadable }

sealed interface CollectOutcome {
    data class Success(val rawJson: String) : CollectOutcome
    data class Failed(val kind: CollectErrorKind) : CollectOutcome

    /** Listing sold or removed. */
    data object Deleted : CollectOutcome
}

/** Data access for the result screen. Real impl: [DefaultResultRepository]; faked in tests. */
interface ResultRepository {
    suspend fun savedCar(carId: String): SavedCarEntity?

    /** `rawInputJson` of an unexpired cache row, or null. */
    suspend fun cachedRaw(carId: String): String?

    /** Runs the Encar collector. Cancelling the caller stops and destroys the collector. */
    suspend fun collect(url: String, carId: String, onStage: (CollectStage) -> Unit): CollectOutcome

    /** Writes the 24h cache (title/score/verdict/rawInputJson). Callers skip reports with data gaps. */
    suspend fun writeCache(report: EvaluationReport, rawJson: String)

    /**
     * Upserts the saved row from [report]. [savedAt] keeps the original save time when
     * refreshing an already-saved car; null means "now".
     */
    suspend fun save(report: EvaluationReport, rawJson: String, savedAt: Long? = null)

    suspend fun delete(carId: String)
}

enum class LoadingStage { OpeningPage, FetchingHistory, Evaluating }

enum class ResultErrorKind { Network, Timeout, PageUnreadable, EngineFailed, InvalidUrl }

/** One banner at most; declaration order is priority order (spec 5.1). */
enum class ResultBanner { SoldOut, UpdateAvailable, LoginRequired, PartialData }

sealed interface ResultUiState {
    data class Loading(val stage: LoadingStage) : ResultUiState

    data class Ready(
        val report: EvaluationReport,
        val saved: Boolean,
        val banner: ResultBanner?,
        /** True while a save/delete is in flight — the save button ignores taps. */
        val saving: Boolean = false,
    ) : ResultUiState

    data class Error(val kind: ResultErrorKind) : ResultUiState

    /** Deleted listing that the user has not saved. */
    data object Deleted : ResultUiState
}

/** One-off UI effects (toasts). */
enum class ResultEvent { Saved, Unsaved, SaveFailed }
