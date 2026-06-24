package com.car.autoverdict.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cars")
data class SavedCarEntity(
    @PrimaryKey val carId: String,
    val url: String,
    val title: String,
    val year: Int?,
    val mileageKm: Int?,
    val priceWon: Long?,
    val fuelType: String?,
    val score: Int,
    val verdict: String,
    val dangerCount: Int,
    val cautionCount: Int,
    val passCount: Int,
    val unknownCount: Int,
    val rawJson: String,
    val savedAt: Long,
    val updatedAt: Long,
)
