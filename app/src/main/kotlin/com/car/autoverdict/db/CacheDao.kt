package com.car.autoverdict.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {
    /** Non-expired rows, newest first, with only the columns the home list needs. */
    @Query(
        "SELECT carId, url, title, score, verdict, cachedAt FROM cache " +
            "WHERE expiresAt > :now ORDER BY cachedAt DESC LIMIT :limit",
    )
    fun recentViewsFlow(now: Long, limit: Int): Flow<List<RecentViewRow>>

    @Query("SELECT * FROM cache WHERE carId = :carId AND expiresAt > :now")
    suspend fun getValid(carId: String, now: Long = System.currentTimeMillis()): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: CacheEntity)

    @Query("DELETE FROM cache WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cache")
    suspend fun count(): Int
}
