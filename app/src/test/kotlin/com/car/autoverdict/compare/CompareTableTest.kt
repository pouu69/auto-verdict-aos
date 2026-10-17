package com.car.autoverdict.compare

import androidx.annotation.StringRes
import com.car.autoverdict.R
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.Fact
import com.car.autoverdict.report.ReportCar
import com.car.autoverdict.report.ReportCounts
import com.car.autoverdict.report.RuleItem
import com.car.autoverdict.report.ShareText
import com.car.autoverdict.report.Tone
import com.car.autoverdict.ui.theme.AvTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareTableTest {

    private fun row(
        carId: String,
        verdict: String = "CAUTION",
        score: Int = 70,
        year: Int? = 2015,
        mileageKm: Int? = 98_000,
        priceWon: Long? = 12_900_000,
        danger: Int = 0,
        caution: Int = 0,
        pass: Int = 0,
        unknown: Int = 0,
    ) = SavedCarEntity(
        carId = carId, url = "https://fem.encar.com/cars/detail/$carId", title = "2015 BMW 5시리즈",
        year = year, mileageKm = mileageKm, priceWon = priceWon, fuelType = "디젤",
        score = score, verdict = verdict, dangerCount = danger, cautionCount = caution,
        passCount = pass, unknownCount = unknown, rawJson = "{}", savedAt = 0, updatedAt = 0,
    )

    private fun report(
        carId: String,
        score: Int = 70,
        year: Int? = 2015,
        mileageKm: Int? = 98_000,
        priceWon: Long? = 12_900_000,
        counts: ReportCounts = ReportCounts(danger = 0, hidden = 0, warning = 0, pass = 9, unknown = 0, excluded = 0),
        issues: List<RuleItem> = emptyList(),
        name: String? = "BMW 5시리즈 520d",
    ) = EvaluationReport(
        schemaVersion = 1, carId = carId, url = "https://fem.encar.com/cars/detail/$carId",
        car = ReportCar("2015 BMW 5시리즈", year, mileageKm, priceWon, "디젤", "", name),
        verdict = "CAUTION", score = score, headline = "", counts = counts, dataGaps = emptyList(),
        facts = listOf(Fact("accidents", "사고", "없음", Tone.Neutral), Fact("owners", "소유자 변경", "3회", Tone.Warning)),
        repairs = null, issues = issues, rules = emptyList(), excluded = emptyList(), timeline = emptyList(),
        shareText = ShareText("", null),
    )

    private fun evaluated(report: EvaluationReport) = CompareInput(row(report.carId), report)

    private fun issue(title: String) = RuleItem(
        ruleId = title, category = "", title = title, tone = Tone.Warning, label = "주의",
        message = "", why = null, sellerQuestion = null,
    )

    private fun CompareTable.row(@StringRes label: Int): CompareRow =
        sections.flatMap { it.rows }.first { it.label == CompareLabel.Res(label) }

    private fun CompareRow.bestIndexes() = cells.indices.filter { cells[it].best }

    @Test fun `marks the unique best score, year, mileage and fewest problems`() {
        val table = buildCompareTable(
            listOf(
                evaluated(report("1", score = 72, year = 2015, mileageKm = 98_000, counts = ReportCounts(1, 0, 2, 7, 1, 0))),
                evaluated(report("2", score = 86, year = 2021, mileageKm = 40_000, counts = ReportCounts(0, 0, 1, 9, 0, 0))),
            ),
        )

        assertEquals(listOf(false, true), table.headers.map { it.scoreBest })
        assertEquals(listOf(1), table.row(R.string.compare_row_year).bestIndexes())
        assertEquals(CompareValue.Text("2021년"), table.row(R.string.compare_row_year).cells[1].value)
        assertEquals(listOf(1), table.row(R.string.compare_row_mileage).bestIndexes())
        assertEquals(CompareValue.Text("4만km"), table.row(R.string.compare_row_mileage).cells[1].value)
        assertEquals(listOf(1), table.row(R.string.compare_row_danger).bestIndexes())
        assertEquals(listOf(1), table.row(R.string.compare_row_warning).bestIndexes())
        assertEquals(listOf(1), table.row(R.string.compare_row_unknown).bestIndexes())
        assertEquals(R.string.compare_best_recent, table.row(R.string.compare_row_year).bestState)
        assertEquals(R.string.compare_best_low, table.row(R.string.compare_row_mileage).bestState)
        assertEquals("BMW 5시리즈 520d", table.headers[0].name)
    }

    @Test fun `ties and missing values mark nothing`() {
        val table = buildCompareTable(
            listOf(
                evaluated(report("1", score = 80, year = 2020, mileageKm = null)),
                evaluated(report("2", score = 80, year = 2020, mileageKm = 50_000)),
                evaluated(report("3", score = 60, year = 2018, mileageKm = null)),
            ),
        )

        assertTrue(table.headers.none { it.scoreBest })
        assertTrue(table.row(R.string.compare_row_year).bestIndexes().isEmpty())
        // Only one car has a mileage, so there is nothing to compare against.
        assertTrue(table.row(R.string.compare_row_mileage).bestIndexes().isEmpty())
        assertEquals(CompareValue.Missing, table.row(R.string.compare_row_mileage).cells[0].value)
        // All zero: a tie, not a win.
        assertTrue(table.row(R.string.compare_row_danger).bestIndexes().isEmpty())
    }

    @Test fun `price and pass never mark a better value`() {
        val table = buildCompareTable(
            listOf(
                evaluated(report("1", priceWon = 12_905_000, counts = ReportCounts(0, 0, 0, 12, 0, 0))),
                evaluated(report("2", priceWon = 18_500_000, counts = ReportCounts(0, 0, 0, 5, 0, 0))),
            ),
        )

        val price = table.row(R.string.compare_row_price)
        assertEquals(listOf(CompareValue.Text("1,290만원"), CompareValue.Text("1,850만원")), price.cells.map { it.value })
        assertTrue(price.bestIndexes().isEmpty())
        assertNull(price.bestState)
        assertTrue(table.row(R.string.compare_row_pass).bestIndexes().isEmpty())
        // Every car excluded nothing, so the row is hidden.
        assertFalse(table.sections.flatMap { it.rows }.any { it.label == CompareLabel.Res(R.string.compare_row_excluded) })
    }

    @Test fun `fallback column uses the saved summary and sits out unsplittable counts`() {
        val table = buildCompareTable(
            listOf(
                evaluated(report("1", score = 60, counts = ReportCounts(0, 1, 0, 9, 0, 2))),
                CompareInput(row("2", verdict = "NEVER", score = 30, year = 2012, danger = 2, caution = 1), report = null),
                evaluated(report("3", score = 50, counts = ReportCounts(1, 0, 0, 9, 0, 0))),
            ),
        )

        val header = table.headers[1]
        assertTrue(header.fallback)
        assertEquals("BMW 5시리즈", header.name)
        assertEquals(30, header.score)
        assertEquals(AvTone.Danger, header.verdictTone)
        assertEquals(R.string.verdict_never, header.verdictLabel)
        assertEquals(listOf(true, false, false), table.headers.map { it.scoreBest })
        assertEquals(CompareValue.Text("2012년"), table.row(R.string.compare_row_year).cells[1].value)

        // Danger 2 on a NEVER row cannot be split: one merged badge, excluded from the comparison.
        val danger = table.row(R.string.compare_row_danger)
        assertEquals(CompareValue.Count(2, AvTone.Danger, R.string.saved_count_danger_or_hidden), danger.cells[1].value)
        assertEquals(listOf(0), danger.bestIndexes())
        val hidden = table.row(R.string.compare_row_hidden)
        assertEquals(CompareValue.Missing, hidden.cells[1].value)
        assertEquals(listOf(2), hidden.bestIndexes())

        // Excluded, facts and issues are unknown for the fallback column.
        assertEquals(CompareValue.Missing, table.row(R.string.compare_row_excluded).cells[1].value)
        val facts = table.sections.first { it.title == R.string.compare_section_facts }
        assertEquals(listOf(CompareLabel.Text("사고"), CompareLabel.Text("소유자 변경")), facts.rows.map { it.label })
        assertEquals(CompareValue.Missing, facts.rows[0].cells[1].value)
        assertEquals(CompareValue.Text("3회", AvTone.Warning), facts.rows[1].cells[0].value)
        val issues = table.sections.first { it.title == R.string.compare_section_issues }.rows.single()
        assertEquals(CompareValue.Missing, issues.cells[1].value)
        assertEquals(CompareValue.Issues(emptyList(), more = 0), issues.cells[0].value)
    }

    @Test fun `issues show the first three and count the rest`() {
        val table = buildCompareTable(
            listOf(
                evaluated(report("1", issues = (1..5).map { issue("항목$it") })),
                evaluated(report("2")),
            ),
        )

        val value = table.sections.first { it.title == R.string.compare_section_issues }.rows.single().cells[0].value
        assertEquals(
            CompareValue.Issues((1..3).map { IssueLine("주의", "항목$it", AvTone.Warning) }, more = 2),
            value,
        )
    }
}
