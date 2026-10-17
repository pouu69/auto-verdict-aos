package com.car.autoverdict.compare

import androidx.annotation.StringRes
import com.car.autoverdict.R
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.Tone
import com.car.autoverdict.saved.formatMileage
import com.car.autoverdict.saved.formatPrice
import com.car.autoverdict.saved.formatYear
import com.car.autoverdict.saved.savedDisplayName
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.verdictLabelRes

/**
 * One compared car (spec 3 6.1): its saved row plus the re-evaluated report. A null [report]
 * means evaluation failed and the column falls back to the saved summary columns.
 */
data class CompareInput(val row: SavedCarEntity, val report: EvaluationReport?)

/** Everything the compare screen draws below the top bar (spec 3 6.2–6.3). Pure: labels stay resources. */
data class CompareTable(
    val headers: List<CompareHeader>,
    val sections: List<CompareSection>,
)

/** Column head: name line, verdict badge, score and the tap target to the result screen. */
data class CompareHeader(
    val carId: String,
    /** Passed as is to `Route.Result(url, fromSaved = true)`. */
    val url: String,
    /** Null means "no name" ([R.string.saved_no_name]). */
    val name: String?,
    val verdictTone: AvTone,
    @StringRes val verdictLabel: Int,
    val score: Int,
    /** Unique highest score among the columns. */
    val scoreBest: Boolean,
    /** Saved-summary column; the UI adds [R.string.compare_fallback_note] under the name. */
    val fallback: Boolean,
)

data class CompareSection(@StringRes val title: Int, val rows: List<CompareRow>)

/**
 * One row: a label line over one cell per column, in header order. [bestState] is the
 * accessibility state of the marked cell; null when the row never marks a better value.
 */
data class CompareRow(
    /** Null for a row that the section title already names (the issues list). */
    val label: CompareLabel?,
    val cells: List<CompareCell>,
    @StringRes val bestState: Int? = null,
)

sealed interface CompareLabel {
    data class Res(@StringRes val id: Int) : CompareLabel

    /** Engine-provided label (fact rows). */
    data class Text(val text: String) : CompareLabel
}

data class CompareCell(val value: CompareValue, val best: Boolean = false)

sealed interface CompareValue {
    /** "—", read as [R.string.compare_missing_a11y]. */
    data object Missing : CompareValue

    /** Plain value; a null [tone] keeps the default text colour. */
    data class Text(val text: String, val tone: AvTone? = null) : CompareValue

    /**
     * A check-result count: 0 as plain "0", otherwise a [tone] badge. [label] is set only for a
     * fallback column whose danger and hidden counts cannot be split ("위험·비공개 n", spec 4.3).
     */
    data class Count(val count: Int, val tone: AvTone, @StringRes val label: Int? = null) : CompareValue

    /** First issues; [more] feeds [R.string.compare_issues_more]. Empty [lines] means "없어요". */
    data class Issues(val lines: List<IssueLine>, val more: Int) : CompareValue
}

/** "{label} {title}", the label in the tone's text colour (e.g. "주의 소유자 변경"). */
data class IssueLine(val label: String, val title: String, val tone: AvTone)

/** Spec 3 6.3. Better values are marked only when unique among at least two cars with a value. */
fun buildCompareTable(columns: List<CompareInput>): CompareTable {
    val cars = columns.map(::columnValues)
    val scoreBest = uniqueBest(cars.map { it.score }, higherIsBetter = true)
    val headers = columns.mapIndexed { i, input ->
        val car = cars[i]
        CompareHeader(
            carId = input.row.carId,
            url = input.row.url,
            name = car.name,
            verdictTone = AvTone.fromVerdict(car.verdict),
            verdictLabel = verdictLabelRes(car.verdict),
            score = car.score,
            scoreBest = i == scoreBest,
            fallback = input.report == null,
        )
    }
    val sections = listOfNotNull(
        basicSection(cars),
        checksSection(cars),
        factsSection(columns),
        issuesSection(columns),
    )
    return CompareTable(headers, sections)
}

/** Per-column values, from the report when evaluated or the saved columns otherwise. */
private class ColumnValues(
    val name: String?,
    val verdict: String,
    val score: Int,
    val year: Int?,
    val mileageKm: Int?,
    val priceWon: Long?,
    val fuel: String?,
    val checks: Checks,
)

/** Null counts are unknown for this column (fallback): shown as "—" or merged, never compared. */
private class Checks(
    val danger: Int?,
    val hidden: Int?,
    val warning: Int,
    val pass: Int,
    val unknown: Int,
    val excluded: Int?,
    /** Fallback NEVER row with two or more danger/hidden rules: one merged count. */
    val dangerOrHidden: Int? = null,
)

private fun columnValues(input: CompareInput): ColumnValues {
    val report = input.report
    val row = input.row
    if (report != null) {
        val car = report.car
        return ColumnValues(
            name = car.name?.trim()?.takeIf { it.isNotEmpty() } ?: savedDisplayName(car.title),
            verdict = report.verdict,
            score = report.score.coerceIn(0, 100),
            year = car.year,
            mileageKm = car.mileageKm,
            priceWon = car.priceWon,
            fuel = car.fuel,
            checks = report.counts.let {
                Checks(it.danger, it.hidden, it.warning, it.pass, it.unknown, it.excluded)
            },
        )
    }
    return ColumnValues(
        name = savedDisplayName(row.title),
        verdict = row.verdict,
        score = row.score.coerceIn(0, 100),
        year = row.year,
        mileageKm = row.mileageKm,
        priceWon = row.priceWon,
        fuel = row.fuelType,
        checks = savedChecks(row),
    )
}

/**
 * Spec 3 4.3 split of `dangerCount = danger + hidden`: a killer rule always makes the verdict
 * NEVER (core `rules/index.ts` `evaluate`), so a non-NEVER count is all hidden and a NEVER count
 * of one is the killer. Two or more on a NEVER row cannot be split.
 */
private fun savedChecks(row: SavedCarEntity): Checks {
    val n = row.dangerCount
    val (danger, hidden, merged) = when {
        n == 0 -> Triple(0, 0, null)
        row.verdict != VERDICT_NEVER -> Triple(0, n, null)
        n == 1 -> Triple(1, 0, null)
        else -> Triple(null, null, n)
    }
    return Checks(
        danger = danger,
        hidden = hidden,
        warning = row.cautionCount,
        pass = row.passCount,
        unknown = row.unknownCount,
        excluded = null,
        dangerOrHidden = merged,
    )
}

private fun basicSection(cars: List<ColumnValues>): CompareSection {
    val years = cars.map { it.year }
    val mileages = cars.map { car -> car.mileageKm?.takeIf { it >= 0 } }
    return CompareSection(
        R.string.compare_section_basic,
        listOf(
            // Price depends on condition, so it never marks a better value (spec 3 6.3).
            CompareRow(
                CompareLabel.Res(R.string.compare_row_price),
                cars.map { car -> CompareCell(textOrMissing(car.priceWon?.let(::formatPrice))) },
            ),
            markedRow(R.string.compare_row_year, years, higherIsBetter = true, R.string.compare_best_recent) {
                formatYear(it)
            },
            markedRow(R.string.compare_row_mileage, mileages, higherIsBetter = false, R.string.compare_best_low) {
                formatMileage(it)
            },
            CompareRow(
                CompareLabel.Res(R.string.compare_row_fuel),
                cars.map { car -> CompareCell(textOrMissing(car.fuel?.trim())) },
            ),
        ),
    )
}

private fun checksSection(cars: List<ColumnValues>): CompareSection {
    val checks = cars.map { it.checks }
    val dangerCells = countCells(checks.map { it.danger }, AvTone.Danger, markLowest = true).mapIndexed { i, cell ->
        val merged = checks[i].dangerOrHidden ?: return@mapIndexed cell
        CompareCell(CompareValue.Count(merged, AvTone.Danger, R.string.saved_count_danger_or_hidden))
    }
    val rows = listOf(
        CompareRow(CompareLabel.Res(R.string.compare_row_danger), dangerCells, R.string.compare_best_low),
        countRow(R.string.compare_row_hidden, checks.map { it.hidden }, AvTone.Danger, markLowest = true),
        countRow(R.string.compare_row_warning, checks.map { it.warning }, AvTone.Warning, markLowest = true),
        // More passes is not better: each car is evaluated on a different number of rules.
        countRow(R.string.compare_row_pass, checks.map { it.pass }, AvTone.Success, markLowest = false),
        countRow(R.string.compare_row_unknown, checks.map { it.unknown }, AvTone.Neutral, markLowest = true),
    )
    val excluded = checks.map { it.excluded }
    val excludedRow = if (excluded.any { (it ?: 0) > 0 }) {
        countRow(R.string.compare_row_excluded, excluded, AvTone.Neutral, markLowest = false)
    } else {
        null
    }
    return CompareSection(R.string.compare_section_checks, rows + listOfNotNull(excludedRow))
}

/** Report fact slots in display order; rows nobody has are dropped, as is an empty section. */
private fun factsSection(columns: List<CompareInput>): CompareSection? {
    val rows = FACT_IDS.mapNotNull { id ->
        val facts = columns.map { input -> input.report?.facts?.firstOrNull { it.id == id } }
        val label = facts.firstNotNullOfOrNull { it?.label } ?: return@mapNotNull null
        CompareRow(
            CompareLabel.Text(label),
            facts.map { fact ->
                CompareCell(
                    if (fact == null) {
                        CompareValue.Missing
                    } else {
                        CompareValue.Text(fact.value, fact.tone.takeIf { it != Tone.Neutral }?.toAvTone())
                    },
                )
            },
        )
    }
    return rows.takeIf { it.isNotEmpty() }?.let { CompareSection(R.string.compare_section_facts, it) }
}

private fun issuesSection(columns: List<CompareInput>): CompareSection {
    val cells = columns.map { input ->
        val issues = input.report?.issues ?: return@map CompareCell(CompareValue.Missing)
        CompareCell(
            CompareValue.Issues(
                lines = issues.take(MAX_ISSUES).map { IssueLine(it.label, it.title, it.tone.toAvTone()) },
                more = (issues.size - MAX_ISSUES).coerceAtLeast(0),
            ),
        )
    }
    return CompareSection(R.string.compare_section_issues, listOf(CompareRow(label = null, cells = cells)))
}

private fun countRow(@StringRes label: Int, counts: List<Int?>, tone: AvTone, markLowest: Boolean) = CompareRow(
    CompareLabel.Res(label),
    countCells(counts, tone, markLowest),
    if (markLowest) R.string.compare_best_low else null,
)

private fun countCells(counts: List<Int?>, tone: AvTone, markLowest: Boolean): List<CompareCell> {
    val best = if (markLowest) uniqueBest(counts, higherIsBetter = false) else null
    return counts.mapIndexed { i, count ->
        CompareCell(count?.let { CompareValue.Count(it, tone) } ?: CompareValue.Missing, best = i == best)
    }
}

private inline fun markedRow(
    @StringRes label: Int,
    values: List<Int?>,
    higherIsBetter: Boolean,
    @StringRes bestState: Int,
    format: (Int) -> String,
): CompareRow {
    val best = uniqueBest(values, higherIsBetter)
    return CompareRow(
        CompareLabel.Res(label),
        values.mapIndexed { i, value -> CompareCell(textOrMissing(value?.let(format)), best = i == best) },
        bestState,
    )
}

private fun textOrMissing(text: String?): CompareValue =
    text?.takeIf { it.isNotEmpty() }?.let { CompareValue.Text(it) } ?: CompareValue.Missing

/**
 * Index of the single best value, or null when fewer than two columns have a value or the best
 * is tied. Null values never compete.
 */
internal fun uniqueBest(values: List<Int?>, higherIsBetter: Boolean): Int? {
    val present = values.withIndex().filter { it.value != null }
    if (present.size < 2) return null
    val target = present.map { it.value!! }.let { if (higherIsBetter) it.max() else it.min() }
    return present.singleOrNull { it.value == target }?.index
}

private const val VERDICT_NEVER = "NEVER"
private const val MAX_ISSUES = 3
private val FACT_IDS = listOf("accidents", "owners", "usage", "floodLoss")
