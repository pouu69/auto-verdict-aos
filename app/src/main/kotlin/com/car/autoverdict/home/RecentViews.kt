package com.car.autoverdict.home

import android.content.res.Resources
import com.car.autoverdict.R
import com.car.autoverdict.db.CacheDao
import com.car.autoverdict.db.RecentViewRow
import com.car.autoverdict.result.SavedCarKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Cars shown at most, after collapsing one car's rows into one. */
const val RECENT_ITEM_LIMIT = 20

/** Rows scanned to find [RECENT_ITEM_LIMIT] cars; 24h retention means this is never reached in practice. */
const val RECENT_SCAN_LIMIT = 200

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
 * Recent items re-queried on every tick, one per car (newest row wins). Kept as one chained flow
 * so a single `collectAsStateWithLifecycle(initialValue = null)` never falls back to null between
 * ticks, which would make the list flicker every minute. Identities are resolved on [dispatcher]
 * with a memo per collection, so each row's payload is parsed once rather than every tick.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun CacheDao.recentViewItems(
    ticks: Flow<Long> = nowTicker(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Flow<List<RecentViewItem>> = flow {
    val resolver = RecentIdentityResolver(this@recentViewItems)
    emitAll(
        ticks.flatMapLatest { now ->
            recentViewsFlow(now, RECENT_SCAN_LIMIT).map { rows ->
                resolver.newestPerCar(rows, RECENT_ITEM_LIMIT).map { it.toItem(now) }
            }
        },
    )
}.flowOn(dispatcher)

/**
 * Collapses recent rows to one per vehicle identity, the rule the saved list uses
 * ([SavedCarKey]): Encar can serve one car under a dummy-ad id and its original id, and the cache
 * is keyed by the URL id. Not thread-safe; calls must be sequential (one collection).
 */
private class RecentIdentityResolver(private val dao: CacheDao) {
    /** (carId, cachedAt) → identity; a re-cached row has a new cachedAt and is parsed again. */
    private var memo: Map<Pair<String, Long>, String> = emptyMap()

    /** The newest row of each car in [rows] (newest first), up to [limit] cars. */
    suspend fun newestPerCar(rows: List<RecentViewRow>, limit: Int): List<RecentViewRow> {
        val known = HashMap(memo)
        val seen = HashSet<String>()
        val result = ArrayList<RecentViewRow>(limit)
        // Payloads can be hundreds of KB, so they are fetched a few rows at a time and only as far as needed.
        for (chunk in rows.chunked(PAYLOAD_CHUNK)) {
            if (result.size >= limit) break
            val missing = chunk.filter { it.key !in known }
            if (missing.isNotEmpty()) {
                val payloads = dao.recentIdentitySources(missing.map { it.carId }).associateBy { it.carId }
                for (row in missing) {
                    known[row.key] = payloads[row.carId]?.let { SavedCarKey.of(it.rawInputJson, row.carId) } ?: row.carId
                }
            }
            for (row in chunk) {
                if (result.size >= limit) break
                if (seen.add(known.getValue(row.key))) result += row
            }
        }
        val current = rows.mapTo(HashSet()) { it.key }
        memo = known.filterKeys { it in current }
        return result
    }

    private val RecentViewRow.key get() = carId to cachedAt
}

private const val PAYLOAD_CHUNK = 5

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
