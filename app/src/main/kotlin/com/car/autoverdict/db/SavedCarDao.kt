package com.car.autoverdict.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT COUNT(*) FROM saved_cars")
    fun countFlow(): Flow<Int>

    @Query("SELECT * FROM saved_cars WHERE carId IN (:carIds)")
    suspend fun getByCarIds(carIds: List<String>): List<SavedCarEntity>

    @Query("SELECT * FROM saved_cars WHERE carId IN (:carIds)")
    fun getByCarIdsFlow(carIds: List<String>): Flow<List<SavedCarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cars: List<SavedCarEntity>)

    @Query("DELETE FROM saved_cars WHERE carId IN (:carIds)")
    suspend fun deleteByCarIds(carIds: List<String>)

    /** Deletes the rows and returns them as they were, so the caller can undo (spec 4.6). */
    @Transaction
    suspend fun deleteAndReturn(carIds: List<String>): List<SavedCarEntity> {
        val rows = getByCarIds(carIds)
        deleteByCarIds(carIds)
        return rows
    }
}
