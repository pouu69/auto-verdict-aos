package com.car.autoverdict.result

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.db.CacheEntity
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.ReportParser
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultResultRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultResultRepository
    private var now = 1_000_000L

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = DefaultResultRepository(
            context = ApplicationProvider.getApplicationContext(),
            db = db,
            clock = { now },
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- cache ---

    @Test
    fun `writeCache stores report summary and raw input for 24 hours`() = runTest {
        val report = sample("sample-caution.json")

        repository.writeCache(report, RAW)

        assertEquals(RAW, repository.cachedRaw(report.carId))
        val row = db.cacheDao().getValid(report.carId, now)!!
        assertEquals(report.url, row.url)
        assertEquals("2015 BMW 3시리즈 (F30)", row.title)
        assertEquals(89, row.score)
        assertEquals("CAUTION", row.verdict)
        assertEquals("", row.resultJson)
        assertEquals(now, row.cachedAt)
        assertEquals(now + DAY_MS, row.expiresAt)
    }

    @Test
    fun `cachedRaw is null once the cache expires`() = runTest {
        val report = sample("sample-caution.json")
        repository.writeCache(report, RAW)

        now += DAY_MS - 1
        assertEquals(RAW, repository.cachedRaw(report.carId))
        now += 1
        assertNull(repository.cachedRaw(report.carId))
    }

    @Test
    fun `cachedRaw treats an empty raw input as missing`() = runTest {
        db.cacheDao().upsert(
            CacheEntity("1", "u", "", 0, "", "", rawInputJson = "", cachedAt = now, expiresAt = now + DAY_MS),
        )
        assertNull(repository.cachedRaw("1"))
        assertNull(repository.cachedRaw("unknown"))
    }

    // --- saved cars ---

    @Test
    fun `save maps report fields onto the saved row`() = runTest {
        val base = sample("sample-caution.json")
        val report = base.copy(counts = base.counts.copy(danger = 1, hidden = 2, unknown = 3))

        repository.save(report, RAW)

        val row = repository.savedCar(report.carId)!!
        assertEquals("41878112", row.carId)
        assertEquals(report.url, row.url)
        assertEquals("2015 BMW 3시리즈 (F30)", row.title)
        assertEquals(2015, row.year)
        assertEquals(119_529, row.mileageKm)
        assertEquals(8_900_000L, row.priceWon)
        assertEquals("디젤", row.fuelType)
        assertEquals(89, row.score)
        assertEquals("CAUTION", row.verdict)
        assertEquals(3, row.dangerCount) // danger + hidden
        assertEquals(2, row.cautionCount)
        assertEquals(9, row.passCount)
        assertEquals(3, row.unknownCount)
        assertEquals(RAW, row.rawJson)
        assertEquals(now, row.savedAt)
        assertEquals(now, row.updatedAt)
    }

    @Test
    fun `save keeps nullable car fields null`() = runTest {
        val report = sample("sample-minimal.json")

        repository.save(report, RAW)

        val row = repository.savedCar(report.carId)!!
        assertEquals("차량 정보 없음", row.title)
        assertNull(row.year)
        assertNull(row.mileageKm)
        assertNull(row.priceWon)
        assertNull(row.fuelType)
        assertEquals(1, row.unknownCount)
    }

    @Test
    fun `save clamps score and trims long text like the old bridge`() = runTest {
        val base = sample("sample-caution.json")
        val report = base.copy(
            score = 150,
            car = base.car.copy(title = "가".repeat(300), fuel = "f".repeat(80)),
        )

        repository.save(report, RAW)

        val row = repository.savedCar(report.carId)!!
        assertEquals(100, row.score)
        assertEquals(200, row.title.length)
        assertEquals(50, row.fuelType!!.length)
    }

    @Test
    fun `refresh keeps the original savedAt and bumps updatedAt`() = runTest {
        val report = sample("sample-caution.json")
        repository.save(report, RAW)
        val firstSavedAt = now

        now += 5_000
        repository.save(report.copy(score = 70), "{\"v\":2}", savedAt = firstSavedAt)

        val row = repository.savedCar(report.carId)!!
        assertEquals(firstSavedAt, row.savedAt)
        assertEquals(now, row.updatedAt)
        assertEquals(70, row.score)
        assertEquals("{\"v\":2}", row.rawJson)
        assertEquals(1, db.savedCarDao().count())
    }

    @Test
    fun `delete removes the saved row`() = runTest {
        val report = sample("sample-caution.json")
        repository.save(report, RAW)

        repository.delete(report.carId)

        assertNull(repository.savedCar(report.carId))
    }

    private fun sample(name: String): EvaluationReport {
        val text = requireNotNull(javaClass.classLoader?.getResource("report/$name")).readText()
        return (ReportParser.parse(text) as ReportParser.Result.Ok).report
    }

    private companion object {
        const val RAW = """{"carId":"41878112"}"""
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
