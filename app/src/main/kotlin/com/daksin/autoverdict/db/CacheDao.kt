package com.daksin.autoverdict.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache WHERE expiresAt > :now ORDER BY cachedAt DESC")
    fun getRecentFlow(now: Long = System.currentTimeMillis()): Flow<List<CacheEntity>>

    @Query("SELECT * FROM cache WHERE carId = :carId AND expiresAt > :now")
    suspend fun getValid(carId: String, now: Long = System.currentTimeMillis()): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: CacheEntity)

    @Query("DELETE FROM cache WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cache")
    suspend fun clearAll()
}
