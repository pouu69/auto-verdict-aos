package com.car.autoverdict.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var savedCarDao: SavedCarDao
    private lateinit var cacheDao: CacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        savedCarDao = db.savedCarDao()
        cacheDao = db.cacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- SavedCarDao ---

    @Test
    fun `upsert and retrieve saved car`() = runTest {
        val car = makeSavedCar("123")
        savedCarDao.upsert(car)

        val result = savedCarDao.getByCarId("123")
        assertNotNull(result)
        assertEquals("123", result?.carId)
        assertEquals(85, result?.score)
    }

    @Test
    fun `upsert replaces existing car`() = runTest {
        savedCarDao.upsert(makeSavedCar("123", score = 70))
        savedCarDao.upsert(makeSavedCar("123", score = 90))

        val result = savedCarDao.getByCarId("123")
        assertEquals(90, result?.score)
        assertEquals(1, savedCarDao.count())
    }

    @Test
    fun `getAllFlow returns cars ordered by savedAt DESC`() = runTest {
        savedCarDao.upsert(makeSavedCar("1", savedAt = 1000))
        savedCarDao.upsert(makeSavedCar("2", savedAt = 3000))
        savedCarDao.upsert(makeSavedCar("3", savedAt = 2000))

        val cars = savedCarDao.getAllFlow().first()
        assertEquals(listOf("2", "3", "1"), cars.map { it.carId })
    }

    @Test
    fun `deleteByCarId removes car`() = runTest {
        savedCarDao.upsert(makeSavedCar("123"))
        savedCarDao.deleteByCarId("123")

        assertNull(savedCarDao.getByCarId("123"))
        assertEquals(0, savedCarDao.count())
    }

    @Test
    fun `getByCarId returns null for nonexistent`() = runTest {
        assertNull(savedCarDao.getByCarId("nonexistent"))
    }

    // --- CacheDao ---

    @Test
    fun `upsert and retrieve valid cache`() = runTest {
        val now = System.currentTimeMillis()
        cacheDao.upsert(makeCache("123", cachedAt = now, expiresAt = now + 86400000))

        val result = cacheDao.getValid("123", now)
        assertNotNull(result)
        assertEquals("123", result?.carId)
    }

    @Test
    fun `getValid returns null for expired cache`() = runTest {
        val now = System.currentTimeMillis()
        cacheDao.upsert(makeCache("123", cachedAt = now - 100000, expiresAt = now - 1))

        assertNull(cacheDao.getValid("123", now))
    }

    @Test
    fun `purgeExpired removes old entries`() = runTest {
        val now = System.currentTimeMillis()
        cacheDao.upsert(makeCache("old", cachedAt = now - 100000, expiresAt = now - 1))
        cacheDao.upsert(makeCache("fresh", cachedAt = now, expiresAt = now + 86400000))

        cacheDao.purgeExpired(now)

        assertNull(cacheDao.getValid("old", now))
        assertNotNull(cacheDao.getValid("fresh", now))
    }

    @Test
    fun `clearAll removes everything`() = runTest {
        val now = System.currentTimeMillis()
        cacheDao.upsert(makeCache("1", cachedAt = now, expiresAt = now + 86400000))
        cacheDao.upsert(makeCache("2", cachedAt = now, expiresAt = now + 86400000))

        cacheDao.clearAll()

        val recent = cacheDao.getRecentFlow(now).first()
        assertEquals(0, recent.size)
    }

    @Test
    fun `getRecentFlow excludes expired`() = runTest {
        val now = System.currentTimeMillis()
        cacheDao.upsert(makeCache("expired", cachedAt = now - 100000, expiresAt = now - 1))
        cacheDao.upsert(makeCache("valid", cachedAt = now, expiresAt = now + 86400000))

        val recent = cacheDao.getRecentFlow(now).first()
        assertEquals(1, recent.size)
        assertEquals("valid", recent[0].carId)
    }

    // --- Helpers ---

    private fun makeSavedCar(
        carId: String,
        score: Int = 85,
        savedAt: Long = System.currentTimeMillis(),
    ) = SavedCarEntity(
        carId = carId,
        url = "https://fem.encar.com/cars/detail/$carId",
        title = "Test Car $carId",
        year = 2022,
        mileageKm = 35000,
        priceWon = 25000000,
        fuelType = "가솔린",
        score = score,
        verdict = "OK",
        dangerCount = 0,
        cautionCount = 1,
        passCount = 8,
        unknownCount = 0,
        rawJson = "{}",
        savedAt = savedAt,
        updatedAt = savedAt,
    )

    private fun makeCache(
        carId: String,
        cachedAt: Long = System.currentTimeMillis(),
        expiresAt: Long = cachedAt + 86400000,
    ) = CacheEntity(
        carId = carId,
        url = "https://fem.encar.com/cars/detail/$carId",
        title = "Test Car",
        score = 80,
        verdict = "OK",
        resultJson = "{}",
        rawInputJson = """{"url":"test","carId":"$carId","preloadedState":null}""",
        cachedAt = cachedAt,
        expiresAt = expiresAt,
    )
}
