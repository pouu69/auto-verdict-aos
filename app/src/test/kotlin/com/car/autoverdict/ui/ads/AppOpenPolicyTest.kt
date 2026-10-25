package com.car.autoverdict.ui.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenPolicyTest {
    private val now = 1_800_000_000_000L
    private val today = "2026-10-25"

    private fun allowed(
        completed: Boolean = true,
        last: Long = 0,
        day: String? = null,
        count: Int = 0,
    ) = AppOpenPolicy.canShow(completed, last, day, count, now, today)

    @Test fun `first analysis must complete before any app open ad`() {
        assertFalse(allowed(completed = false))
        assertTrue(allowed())
    }

    @Test fun `ten minute cooldown applies across day boundaries`() {
        assertFalse(allowed(last = now - AppOpenPolicy.MIN_INTERVAL_MS + 1, day = "2026-10-24"))
        assertTrue(allowed(last = now - AppOpenPolicy.MIN_INTERVAL_MS))
    }

    @Test fun `two impressions exhaust the local day`() {
        assertTrue(allowed(day = today, count = 1))
        assertFalse(allowed(day = today, count = 2))
        assertFalse(allowed(day = today, count = 3))
    }

    @Test fun `a new local day resets the daily count`() {
        assertTrue(allowed(last = now - AppOpenPolicy.MIN_INTERVAL_MS, day = "2026-10-24", count = 2))
    }

    @Test fun `moving clock backwards does not bypass cooldown`() {
        assertFalse(allowed(last = now + 1_000))
    }

    @Test fun `four hour old and backwards clock ads are stale`() {
        assertTrue(AppOpenPolicy.isAdFresh(100, 100))
        assertTrue(AppOpenPolicy.isAdFresh(100, 100 + AppOpenPolicy.AD_MAX_AGE_MS - 1))
        assertFalse(AppOpenPolicy.isAdFresh(100, 100 + AppOpenPolicy.AD_MAX_AGE_MS))
        assertFalse(AppOpenPolicy.isAdFresh(100, 99))
    }

    @Test fun `a share can only trigger one initial load decision`() {
        val entry = AppOpenOpportunity("car-a", 100)
        assertTrue(entry.consume("car-a", 101))
        assertFalse(entry.consume("car-a", 102))
    }

    @Test fun `opportunity cannot move to a different car`() {
        val entry = AppOpenOpportunity("car-a", 100)
        assertFalse(entry.consume("car-b", 101))
        assertFalse(entry.consume("car-a", 102))
    }

    @Test fun `delayed opportunity expires instead of showing a late ad`() {
        val entry = AppOpenOpportunity("car-a", 100)
        assertFalse(entry.consume("car-a", 100 + AppOpenPolicy.ENTRY_MAX_AGE_MS))
    }

    @Test fun `opportunity also rejects backwards elapsed time`() {
        assertFalse(AppOpenOpportunity("car-a", 100).consume("car-a", 99))
    }
}
