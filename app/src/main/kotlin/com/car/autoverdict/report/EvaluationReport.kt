package com.car.autoverdict.report

import com.car.autoverdict.ui.theme.AvTone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Kotlin mirror of webview-bundle/src/report/types.ts (spec 4). The engine builds it;
 * the result screen only renders it. Change both sides together.
 */
@Serializable
data class EvaluationReport(
    val schemaVersion: Int,
    val carId: String,
    val url: String,
    val car: ReportCar,
    /** NEVER | CAUTION | OK | UNKNOWN — engine value, unchanged. */
    val verdict: String,
    val score: Int,
    val headline: String,
    val counts: ReportCounts,
    val dataGaps: List<DataGap>,
    val facts: List<Fact>,
    val repairs: Repairs?,
    val issues: List<RuleItem>,
    val rules: List<RuleItem>,
    val excluded: List<ExcludedItem>,
    val timeline: List<TimelineEntry>,
    val shareText: ShareText,
)

@Serializable
data class ReportCar(
    val title: String,
    val year: Int?,
    val mileageKm: Int?,
    val priceWon: Long?,
    val fuel: String?,
    val summary: String,
)

@Serializable
data class ReportCounts(
    /** killer */
    val danger: Int,
    /** fail (비공개) */
    val hidden: Int,
    val warning: Int,
    val pass: Int,
    val unknown: Int,
    val excluded: Int,
)

@Serializable
enum class Tone {
    @SerialName("danger") Danger,
    @SerialName("warning") Warning,
    @SerialName("success") Success,
    @SerialName("neutral") Neutral,
    ;

    fun toAvTone(): AvTone = when (this) {
        Danger -> AvTone.Danger
        Warning -> AvTone.Warning
        Success -> AvTone.Success
        Neutral -> AvTone.Neutral
    }
}

@Serializable
enum class GapSource {
    @SerialName("page") Page,
    @SerialName("record") Record,
    @SerialName("diagnosis") Diagnosis,
    @SerialName("inspection") Inspection,
}

@Serializable
enum class GapReason {
    @SerialName("failed") Failed,
    @SerialName("loginRequired") LoginRequired,
}

@Serializable
data class DataGap(val source: GapSource, val reason: GapReason)

@Serializable
data class Fact(val id: String, val label: String, val value: String, val tone: Tone)

@Serializable
data class RepairPanel(val name: String, val status: String, val rank: String?, val tone: Tone)

@Serializable
data class SystemItem(val group: String, val name: String, val status: String, val tone: Tone)

@Serializable
data class SystemSummary(val flagged: List<SystemItem>, val okCount: Int)

@Serializable
data class Repairs(val panels: List<RepairPanel>, val systems: SystemSummary)

@Serializable
data class RuleItem(
    val ruleId: String,
    val category: String,
    val title: String,
    val tone: Tone,
    /** 위험 | 비공개 | 주의 | 통과 | 미확인 */
    val label: String,
    val message: String,
    val why: String?,
    val sellerQuestion: String?,
)

@Serializable
data class ExcludedItem(val ruleId: String, val title: String, val reason: String)

@Serializable
data class TimelineEntry(val date: String, val label: String, val detail: String?, val tone: Tone)

@Serializable
data class ShareText(val summary: String, val sellerQuestions: String?)

/** Parses engine output. Unknown fields are ignored; any other mismatch is an engine error. */
object ReportParser {
    const val SCHEMA_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    sealed interface Result {
        data class Ok(val report: EvaluationReport) : Result
        data class Invalid(val reason: String) : Result
    }

    fun parse(text: String): Result {
        val report = try {
            json.decodeFromString(EvaluationReport.serializer(), text)
        } catch (e: Exception) {
            return Result.Invalid("contract: ${e.message?.take(200)}")
        }
        if (report.schemaVersion != SCHEMA_VERSION) {
            return Result.Invalid("schemaVersion ${report.schemaVersion}")
        }
        return Result.Ok(report)
    }
}
