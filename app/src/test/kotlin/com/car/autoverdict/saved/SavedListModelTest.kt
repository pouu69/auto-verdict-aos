package com.car.autoverdict.saved

import com.car.autoverdict.R
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.ui.theme.AvTone
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedListModelTest {

    private val zone = ZoneId.of("Asia/Seoul")

    private fun millis(y: Int, m: Int, d: Int) = LocalDateTime.of(y, m, d, 12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun row(
        carId: String = "1",
        title: String = "2015 BMW 5시리즈",
        verdict: String = "CAUTION",
        score: Int = 72,
        danger: Int = 0,
        caution: Int = 0,
        pass: Int = 0,
        unknown: Int = 0,
        priceWon: Long? = 12_900_000,
        savedAt: Long = 0,
    ) = SavedCarEntity(
        carId = carId, url = "https://fem.encar.com/cars/detail/$carId", title = title,
        year = 2015, mileageKm = 98_000, priceWon = priceWon, fuelType = "디젤",
        score = score, verdict = verdict, dangerCount = danger, cautionCount = caution,
        passCount = pass, unknownCount = unknown, rawJson = "{}", savedAt = savedAt, updatedAt = savedAt,
    )

    private fun labels(row: SavedCarEntity) = savedCountBadges(row).map { it.label to it.count }

    // Spec 3 4.3: dangerCount is danger + hidden; only a NEVER verdict can hold a danger rule.
    @Test fun `danger count is hidden unless the verdict is NEVER`() {
        assertEquals(listOf(R.string.result_count_hidden to 2), labels(row(verdict = "OK", danger = 2)))
        assertEquals(listOf(R.string.result_count_danger to 1), labels(row(verdict = "NEVER", danger = 1)))
        assertEquals(
            listOf(R.string.saved_count_danger_or_hidden to 3),
            labels(row(verdict = "NEVER", danger = 3)),
        )
    }

    @Test fun `badges follow the result order and tones and drop zeros`() {
        val badges = savedCountBadges(row(verdict = "NEVER", danger = 1, caution = 0, pass = 8, unknown = 2))
        assertEquals(
            listOf(
                SavedCountBadge(R.string.result_count_danger, 1, AvTone.Danger),
                SavedCountBadge(R.string.result_count_pass, 8, AvTone.Success),
                SavedCountBadge(R.string.result_count_unknown, 2, AvTone.Neutral),
            ),
            badges,
        )
    }

    @Test fun `name drops the leading model year and treats empty titles as no name`() {
        assertEquals("BMW 5시리즈", savedDisplayName("2015 BMW 5시리즈"))
        assertEquals("BMW 5시리즈", savedDisplayName("BMW 5시리즈"))
        assertNull(savedDisplayName("  "))
        assertNull(savedDisplayName("2015"))
        assertNull(savedDisplayName("차량 정보 없음"))
    }

    @Test fun `summary matches the report format and skips missing parts`() {
        assertEquals("2015년 · 9.8만km · 디젤 · 1,290만원", savedSummary(2015, 98_000, "디젤", 12_905_000))
        assertEquals("9,500km", savedSummary(null, 9_500, null, null))
        assertEquals("12만km · 가솔린", savedSummary(null, 120_000, "가솔린", 5_000))
        assertEquals("", savedSummary(null, null, " ", null))
    }

    @Test fun `saved date omits the year only within the current year`() {
        val now = millis(2026, 10, 18)
        assertEquals("10월 18일", savedDateText(millis(2026, 10, 18), now, zone))
        assertEquals("2025년 10월 8일", savedDateText(millis(2025, 10, 8), now, zone))
    }

    @Test fun `sorts by recent, score, and price with missing prices last`() {
        val a = row(carId = "a", score = 80, priceWon = 20_000_000, savedAt = 1)
        val b = row(carId = "b", score = 90, priceWon = null, savedAt = 2)
        val c = row(carId = "c", score = 80, priceWon = 10_000_000, savedAt = 3)
        val d = row(carId = "d", score = 50, priceWon = 10_000_000, savedAt = 4)
        val rows = listOf(a, b, c, d)
        fun ids(sort: SavedSort) = rows.sortedForSaved(sort) { it }.map { it.carId }

        assertEquals(listOf("d", "c", "b", "a"), ids(SavedSort.Recent))
        assertEquals(listOf("b", "c", "a", "d"), ids(SavedSort.Score))
        assertEquals(listOf("d", "c", "a", "b"), ids(SavedSort.Price))
    }

    @Test fun `unknown sort keys fall back to recent`() {
        assertEquals(SavedSort.Price, SavedSort.fromKey("price"))
        assertEquals(SavedSort.Recent, SavedSort.fromKey("score_asc"))
        assertEquals(SavedSort.Recent, SavedSort.fromKey(null))
    }
}
