package com.car.autoverdict.ui.ads

/** App product limits, deliberately identical for test ads and production ads. */
internal object AppOpenPolicy {
    const val MIN_INTERVAL_MS = 10 * 60 * 1_000L
    const val MAX_PER_DAY = 2
    const val AD_MAX_AGE_MS = 4 * 60 * 60 * 1_000L
    const val ENTRY_MAX_AGE_MS = 10_000L

    fun canShow(
        completedAnalysis: Boolean,
        lastShownAt: Long,
        shownDay: String?,
        shownCount: Int,
        now: Long,
        today: String,
    ): Boolean = completedAnalysis &&
        (lastShownAt == 0L || now - lastShownAt >= MIN_INTERVAL_MS) &&
        (shownDay != today || shownCount < MAX_PER_DAY)

    fun isAdFresh(loadedAt: Long, now: Long): Boolean =
        now - loadedAt in 0 until AD_MAX_AGE_MS
}

/** A share is a one-shot opportunity, never an instruction to show an ad after it loads. */
internal class AppOpenOpportunity(private val url: String, private val createdAt: Long) {
    private var consumed = false

    fun matches(forUrl: String): Boolean = url == forUrl

    fun consume(forUrl: String, now: Long): Boolean {
        if (consumed) return false
        consumed = true
        return forUrl == url && now - createdAt in 0 until AppOpenPolicy.ENTRY_MAX_AGE_MS
    }
}
