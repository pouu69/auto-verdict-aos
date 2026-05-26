package com.daksin.autoverdict.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val carId: String,
    val url: String,
    val title: String,
    val score: Int,
    val verdict: String,
    val resultJson: String,
    val rawInputJson: String,
    val cachedAt: Long,
    val expiresAt: Long,
)
