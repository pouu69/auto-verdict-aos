package com.car.autoverdict.home

import com.car.autoverdict.db.RecentViewRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentViewsTest {

    private val now = 1_000_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute

    private fun row(title: String = "2015 BMW 5시리즈", verdict: String = "OK", score: Int = 82, cachedAt: Long = now) =
        RecentViewRow("123", "https://fem.encar.com/cars/detail/123", title, score, verdict, cachedAt)

    @Test
    fun `maps a current row with title, verdict and score`() {
        val item = row().toItem(now)
        assertEquals("2015 BMW 5시리즈", item.title)
        assertEquals("OK", item.verdict)
        assertEquals(82, item.score)
    }

    @Test
    fun `legacy row hides title, verdict and score`() {
        val item = row(title = "", verdict = "", score = 0).toItem(now)
        assertNull(item.title)
        assertNull(item.verdict)
        assertNull(item.score)
    }

    @Test
    fun `relative age boundaries`() {
        assertEquals(RelativeAge.JustNow, relativeAge(now - 59_999, now))
        assertEquals(RelativeAge.Minutes(1), relativeAge(now - minute, now))
        assertEquals(RelativeAge.Minutes(59), relativeAge(now - 59 * minute - 59_999, now))
        assertEquals(RelativeAge.Hours(1), relativeAge(now - hour, now))
        assertEquals(RelativeAge.Hours(23), relativeAge(now - 23 * hour - 59 * minute, now))
        assertEquals(RelativeAge.Hours(23), relativeAge(now - 30 * hour, now))
        // Clock moved backwards.
        assertEquals(RelativeAge.JustNow, relativeAge(now + hour, now))
    }

    @Test
    fun `collapsed list shows five and counts the rest`() {
        val items = (1..7).map { row().toItem(now).copy(carId = "$it") }
        assertEquals(RecentVisible(items.take(5), 2), recentVisible(items, expanded = false))
        assertEquals(RecentVisible(items, 0), recentVisible(items, expanded = true))
        assertEquals(RecentVisible(items.take(5), 0), recentVisible(items.take(5), expanded = false))
    }
}
