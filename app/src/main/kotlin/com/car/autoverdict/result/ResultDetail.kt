package com.car.autoverdict.result

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.car.autoverdict.report.DetailSheet
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.RuleItem
import com.car.autoverdict.report.TimelineEntry
import com.car.autoverdict.result.sections.DetailSheetContent
import com.car.autoverdict.ui.components.AvBottomSheet

/**
 * Which detail sheet is open, as a saveable string: "rule:R07", "timeline:2015-05|사고 · 내차 피해"
 * or "repairs". Keys name content, not positions, so a refreshed report with entries added or
 * removed reopens the same item (or closes). Strings survive recreation without a Saver.
 */
internal object DetailKey {
    const val REPAIRS = "repairs"
    private const val RULE = "rule:"
    private const val TIMELINE = "timeline:"

    fun rule(ruleId: String) = "$RULE$ruleId"

    /** Dates are yyyy-MM, so the first "|" always ends the date. */
    fun timeline(entry: TimelineEntry) = "$TIMELINE${entry.date}|${entry.label}"

    /** The sheet [key] points at in [report], or null when it no longer has one. */
    fun resolve(report: EvaluationReport, key: String): DetailTarget? = when {
        key == REPAIRS -> report.repairs?.sheet?.let { DetailTarget(it) }
        key.startsWith(RULE) -> {
            val id = key.removePrefix(RULE)
            // Issues carry the "why" and seller question; rules repeat them for non-passing items.
            val rule = report.issues.firstOrNull { it.ruleId == id && it.detail != null }
                ?: report.rules.firstOrNull { it.ruleId == id && it.detail != null }
            rule?.detail?.let { DetailTarget(it, rule) }
        }
        key.startsWith(TIMELINE) -> {
            val id = key.removePrefix(TIMELINE)
            val date = id.substringBefore('|')
            val label = id.substringAfter('|')
            report.timeline.firstOrNull { it.date == date && it.label == label && it.sheet != null }
                ?.sheet?.let { DetailTarget(it) }
        }
        else -> null
    }
}

internal data class DetailTarget(val sheet: DetailSheet, val rule: RuleItem? = null)

/**
 * The open detail sheet, if any (spec 4.1). [openKey] is owned by the caller so it survives
 * recreation; back, the scrim and dragging down all close it through [onDismiss]. A newer
 * report that no longer has the sheet closes it too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultDetailSheet(
    report: EvaluationReport,
    openKey: String?,
    onDismiss: () -> Unit,
    onCopyQuestion: (String) -> Unit,
) {
    if (openKey == null) return
    val target = DetailKey.resolve(report, openKey)
    if (target == null) {
        LaunchedEffect(openKey, report) { onDismiss() }
        return
    }
    AvBottomSheet(onDismiss = onDismiss) {
        DetailSheetContent(sheet = target.sheet, rule = target.rule, onCopyQuestion = onCopyQuestion)
    }
}
