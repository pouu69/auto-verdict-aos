package com.car.autoverdict.home

import android.content.res.Resources
import com.car.autoverdict.R
import com.car.autoverdict.db.CacheDao
import com.car.autoverdict.db.RecentViewRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Rows fetched from the cache; 24h retention means this is rarely reached. */
const val RECENT_QUERY_LIMIT = 20

/** Rows shown before "{n}대 더 보기" is tapped. */
const val RECENT_COLLAPSED_COUNT = 5

/** How often the list is re-queried while visible, dropping expired rows and aging timestamps. */
const val RECENT_REFRESH_INTERVAL_MS = 60_000L

data class RecentViewItem(
    val carId: String,
    val url: String,
    /** Null for rows without a title; the UI shows "엔카 매물 {carId}". */
    val title: String?,
    /** Null for rows cached before sub-project 1; the UI hides the badge. */
    val verdict: String?,
    /** Null together with [verdict]; a real score can be 0, so it is not the marker. */
    val score: Int?,
    val age: RelativeAge,
)

sealed interface RelativeAge {
    data object JustNow : RelativeAge
    data class Minutes(val n: Int) : RelativeAge
    data class Hours(val n: Int) : RelativeAge
}

fun RecentViewRow.toItem(now: Long): RecentViewItem {
    val legacy = verdict.isBlank()
    return RecentViewItem(
        carId = carId,
        url = url,
        title = title.takeIf { it.isNotBlank() },
        verdict = verdict.takeUnless { legacy },
        score = score.takeUnless { legacy },
        age = relativeAge(cachedAt, now),
    )
}

fun relativeAge(cachedAt: Long, now: Long): RelativeAge {
    // Negative elapsed (clock moved backwards) also reads as "just now".
    val elapsed = now - cachedAt
    if (elapsed < MINUTE_MS) return RelativeAge.JustNow
    if (elapsed < HOUR_MS) return RelativeAge.Minutes((elapsed / MINUTE_MS).toInt())
    // Rows expire after 24h; cap so clock skew never shows "24시간 전".
    return RelativeAge.Hours((elapsed / HOUR_MS).toInt().coerceAtMost(23))
}

/** Splits the list into the visible rows and how many "{n}대 더 보기" would reveal. */
data class RecentVisible(val items: List<RecentViewItem>, val hiddenCount: Int)

fun recentVisible(items: List<RecentViewItem>, expanded: Boolean): RecentVisible =
    if (expanded || items.size <= RECENT_COLLAPSED_COUNT) {
        RecentVisible(items, 0)
    } else {
        RecentVisible(items.take(RECENT_COLLAPSED_COUNT), items.size - RECENT_COLLAPSED_COUNT)
    }

/** Formats [RelativeAge] for display; an interface so callers can be tested without resources. */
fun interface RelativeAgeFormatter {
    fun format(age: RelativeAge): String
}

class ResourcesRelativeAgeFormatter(private val resources: Resources) : RelativeAgeFormatter {
    override fun format(age: RelativeAge): String = when (age) {
        RelativeAge.JustNow -> resources.getString(R.string.time_just_now)
        is RelativeAge.Minutes -> resources.getString(R.string.time_minutes_ago, age.n)
        is RelativeAge.Hours -> resources.getString(R.string.time_hours_ago, age.n)
    }
}

/** Emits the current time immediately and then every [intervalMs] while collected. */
fun nowTicker(
    intervalMs: Long = RECENT_REFRESH_INTERVAL_MS,
    clock: () -> Long = System::currentTimeMillis,
): Flow<Long> = flow {
    while (true) {
        emit(clock())
        delay(intervalMs)
    }
}

/**
 * Recent items re-queried on every tick. Kept as one chained flow so a single
 * `collectAsStateWithLifecycle(initialValue = null)` never falls back to null between ticks,
 * which would make the list flicker every minute.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun CacheDao.recentViewItems(ticks: Flow<Long> = nowTicker()): Flow<List<RecentViewItem>> =
    ticks.flatMapLatest { now ->
        recentViewsFlow(now, RECENT_QUERY_LIMIT).map { rows -> rows.map { it.toItem(now) } }
    }

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
