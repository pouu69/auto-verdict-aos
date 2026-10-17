package com.car.autoverdict.home

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.db.CacheEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecentViewItemsTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val now = 1_000_000_000_000L

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `dummy ad and its original show as one recent item, the newer`() = runTest {
        val dao = db.cacheDao()
        // The dummy-ad payload names the original vehicle id, like the collector's real payload.
        dao.upsert(row("42751994", cachedAt = now - 2_000))
        dao.upsert(row("42753994", cachedAt = now - 1_000))

        val items = dao.recentViewItems(flowOf(now), UnconfinedTestDispatcher(testScheduler)).first()

        assertEquals(listOf("42753994"), items.map { it.carId })
    }

    private fun row(carId: String, cachedAt: Long) = CacheEntity(
        carId = carId,
        url = "https://fem.encar.com/cars/detail/$carId",
        title = "Car $carId",
        score = 80,
        verdict = "OK",
        resultJson = "{}",
        rawInputJson = """{"preloadedState":{"cars":{"base":{"vehicleId":42751994}}}}""",
        cachedAt = cachedAt,
        expiresAt = now + 60_000,
    )
}
