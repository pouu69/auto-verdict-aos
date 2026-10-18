package com.car.autoverdict.saved

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.car.autoverdict.R
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.verdictLabelRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Display values of one saved-list row (spec 3 4.2), read from the group's representative row
 * only. Pure: labels stay string resources and the UI resolves them.
 */
@Immutable
data class SavedCarDisplay(
    /** Title without the leading model year; null means "no name" ([R.string.saved_no_name]). */
    val name: String?,
    /** "2015년 · 9.8만km · 디젤 · 1,290만원"; empty when every part is missing. */
    val summary: String,
    val verdictTone: AvTone,
    @StringRes val verdictLabel: Int,
    val score: Int,
    val badges: List<SavedCountBadge>,
    /** "10월 18일" this year, "2025년 10월 18일" otherwise; wrap with [R.string.saved_row_saved_on]. */
    val savedOn: String,
)

/** One count badge; [label] is a `%1$d` string resource taking [count]. */
@Immutable
data class SavedCountBadge(@StringRes val label: Int, val count: Int, val tone: AvTone)

/** List order options (spec 3 4.4); [key] is what [com.car.autoverdict.preferences.AppPreferences] stores. */
enum class SavedSort(val key: String, @StringRes val label: Int) {
    Recent("recent", R.string.saved_sort_recent),
    Score("score", R.string.saved_sort_score),
    Price("price", R.string.saved_sort_price);

    companion object {
        val Default = Recent

        /** Unknown or missing keys fall back to [Default]. */
        fun fromKey(key: String?): SavedSort = entries.firstOrNull { it.key == key } ?: Default
    }
}

fun SavedCarEntity.toDisplay(
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): SavedCarDisplay = SavedCarDisplay(
    name = savedDisplayName(title),
    summary = savedSummary(year, mileageKm, fuelType, priceWon),
    verdictTone = AvTone.fromVerdict(verdict),
    verdictLabel = verdictLabelRes(verdict),
    score = score.coerceIn(0, 100),
    badges = savedCountBadges(this),
    savedOn = savedDateText(savedAt, nowMillis, zone),
)

/**
 * Spec 3 4.3. A saved row keeps only `dangerCount = danger + hidden`. The core verdict is NEVER
 * whenever a killer (danger) rule fires (`evaluate` in core `rules/index.ts`), so a non-NEVER row's
 * count is all hidden; a NEVER row with one is the killer; two or more cannot be split.
 * Order and tones follow the result screen's CountChips; zero counts are left out.
 */
fun savedCountBadges(row: SavedCarEntity): List<SavedCountBadge> {
    val danger = row.dangerCount
    val dangerBadge = when {
        row.verdict != VERDICT_NEVER -> SavedCountBadge(R.string.result_count_hidden, danger, AvTone.Danger)
        danger == 1 -> SavedCountBadge(R.string.result_count_danger, danger, AvTone.Danger)
        else -> SavedCountBadge(R.string.saved_count_danger_or_hidden, danger, AvTone.Danger)
    }
    return listOf(
        dangerBadge,
        SavedCountBadge(R.string.result_count_warning, row.cautionCount, AvTone.Warning),
        SavedCountBadge(R.string.result_count_pass, row.passCount, AvTone.Success),
        SavedCountBadge(R.string.result_count_unknown, row.unknownCount, AvTone.Neutral),
    ).filter { it.count > 0 }
}

/** "2015 BMW 5시리즈" -> "BMW 5시리즈"; blank or the engine's no-car title -> null. */
fun savedDisplayName(title: String): String? {
    val name = title.trim().replace(LEADING_YEAR, "").trim()
    return name.takeIf { it.isNotEmpty() && it != NO_CAR_TITLE }
}

/** Same format as the report's `car.summary`; missing parts are dropped. */
fun savedSummary(year: Int?, mileageKm: Int?, fuel: String?, priceWon: Long?): String = listOfNotNull(
    year?.let(::formatYear),
    mileageKm?.takeIf { it >= 0 }?.let(::formatMileage),
    fuel?.trim()?.takeIf { it.isNotEmpty() },
    priceWon?.let(::formatPrice),
).joinToString(SUMMARY_SEPARATOR)

fun formatYear(year: Int): String = "${year}년"

/** 9500 -> "9,500km", 98000 -> "9.8만km", 120000 -> "12만km" (report `formatMileage`). */
fun formatMileage(km: Int): String {
    if (km < 10_000) return "${groupDigits(km.toLong())}km"
    val tenths = Math.round(km / 1_000.0)
    return if (tenths % 10 == 0L) "${tenths / 10}만km" else "${tenths / 10}.${tenths % 10}만km"
}

/** Whole 만원, remainder dropped: 12_905_000 -> "1,290만원"; null below 1만원. */
fun formatPrice(priceWon: Long): String? {
    val man = priceWon / 10_000
    return if (man > 0) "${groupDigits(man)}만원" else null
}

/** "10월 18일" in the current year, "2025년 10월 18일" otherwise. */
fun savedDateText(savedAtMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val saved = Instant.ofEpochMilli(savedAtMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    return (if (saved.year == now.year) THIS_YEAR_DATE else OTHER_YEAR_DATE).format(saved)
}

/**
 * Spec 3 4.4. Generic over the list item so the screen can sort grouped items by their
 * representative row. Ties (and missing prices, which go last) fall back to newest saved first.
 */
fun <T> List<T>.sortedForSaved(sort: SavedSort, row: (T) -> SavedCarEntity): List<T> {
    val recent = compareByDescending<T> { row(it).savedAt }
    val comparator = when (sort) {
        SavedSort.Recent -> recent
        SavedSort.Score -> compareByDescending<T> { row(it).score }.then(recent)
        SavedSort.Price -> compareBy<T, Long?>(nullsLast()) { row(it).priceWon?.takeIf { won -> won > 0 } }
            .then(recent)
    }
    return sortedWith(comparator)
}

private fun groupDigits(n: Long): String = String.format(Locale.US, "%,d", n)

private const val VERDICT_NEVER = "NEVER"
private const val NO_CAR_TITLE = "차량 정보 없음"
private const val SUMMARY_SEPARATOR = " · "
private val LEADING_YEAR = Regex("""^\d{4}(\s+|$)""")
private val THIS_YEAR_DATE = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)
private val OTHER_YEAR_DATE = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
