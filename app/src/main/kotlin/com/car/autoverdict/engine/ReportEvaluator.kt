package com.car.autoverdict.engine

import com.car.autoverdict.report.EvaluationReport

/** Result of one evaluation in the headless rule engine (spec 3.3). */
sealed interface EngineResult {
    data class Success(val report: EvaluationReport) : EngineResult

    /** Init/request timeout, renderer gone, JS error or contract mismatch. [reason] is for logs only. */
    data class Failed(val reason: String) : EngineResult
}

/** Turns a collector payload (raw JSON) into a report. Implemented by [RuleEngine]; faked in tests. */
fun interface ReportEvaluator {
    suspend fun evaluate(rawJson: String): EngineResult
}
