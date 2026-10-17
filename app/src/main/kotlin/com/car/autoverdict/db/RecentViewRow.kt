package com.car.autoverdict.db

/**
 * Projection of [CacheEntity] for the home "recent views" list. Not an entity, so adding it
 * leaves the Room schema untouched; it skips `resultJson`/`rawInputJson`, which can be
 * hundreds of KB per row.
 */
data class RecentViewRow(
    val carId: String,
    val url: String,
    val title: String,
    val score: Int,
    val verdict: String,
    val cachedAt: Long,
)

/** One cache row's payload, fetched only to resolve its vehicle identity for the recent list. */
data class RecentIdentitySource(
    val carId: String,
    val rawInputJson: String,
)
