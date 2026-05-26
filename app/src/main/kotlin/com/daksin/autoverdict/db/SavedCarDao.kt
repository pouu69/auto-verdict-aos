package com.daksin.autoverdict.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCarDao {
    @Query("SELECT * FROM saved_cars ORDER BY savedAt DESC")
    fun getAllFlow(): Flow<List<SavedCarEntity>>

    @Query("SELECT * FROM saved_cars WHERE carId = :carId")
    suspend fun getByCarId(carId: String): SavedCarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(car: SavedCarEntity)

    @Query("DELETE FROM saved_cars WHERE carId = :carId")
    suspend fun deleteByCarId(carId: String)

    @Query("SELECT COUNT(*) FROM saved_cars")
    suspend fun count(): Int
}
