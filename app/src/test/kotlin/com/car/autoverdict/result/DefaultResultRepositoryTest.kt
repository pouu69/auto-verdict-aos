package com.car.autoverdict.result

import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.collector.Collector
import com.car.autoverdict.collector.CollectorWebView
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.db.CacheEntity
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.ReportParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
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

        val savedAt = repository.save(report, RAW)

        assertEquals(now, savedAt)
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
        val written = repository.save(report.copy(score = 70), "{\"v\":2}", savedAt = firstSavedAt)

        assertEquals(firstSavedAt, written)

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

    // --- collect (fake collector; the WebView itself is covered by CollectorWebViewTest) ---

    private class FakeCollector : Collector {
        var url: String? = null
        var carId: String? = null
        var onStage: ((CollectStage) -> Unit)? = null
        var callback: ((CollectorWebView.Result) -> Unit)? = null
        var destroyCount = 0
        var throwOnCollect: RuntimeException? = null

        override fun collect(
            url: String,
            carId: String,
            onStage: (CollectStage) -> Unit,
            callback: (CollectorWebView.Result) -> Unit,
        ) {
            this.url = url
            this.carId = carId
            this.onStage = onStage
            this.callback = callback
            throwOnCollect?.let { throw it }
        }

        override fun destroy() {
            destroyCount++
        }
    }

    private fun TestScope.collecting(fake: FakeCollector) = DefaultResultRepository(
        context = ApplicationProvider.getApplicationContext(),
        db = db,
        clock = { now },
        collectorFactory = { fake },
        mainDispatcher = StandardTestDispatcher(testScheduler),
    )

    /** Runs the destroy that collect() posts to the main looper. */
    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `collect maps every collector result and destroys the collector once`() = runTest {
        val cases = listOf(
            CollectorWebView.Result.Success("raw") to CollectOutcome.Success("raw"),
            CollectorWebView.Result.Error(CollectErrorKind.Network, "log") to
                CollectOutcome.Failed(CollectErrorKind.Network),
            CollectorWebView.Result.Error(CollectErrorKind.Timeout, "log") to
                CollectOutcome.Failed(CollectErrorKind.Timeout),
            CollectorWebView.Result.Error(CollectErrorKind.PageUnreadable, "log") to
                CollectOutcome.Failed(CollectErrorKind.PageUnreadable),
            CollectorWebView.Result.Deleted to CollectOutcome.Deleted,
        )
        for ((result, expected) in cases) {
            val fake = FakeCollector()
            val outcome = async { collecting(fake).collect(URL, CAR_ID) {} }
            runCurrent()
            assertEquals(URL, fake.url)
            assertEquals(CAR_ID, fake.carId)

            fake.callback!!(result)
            assertEquals(expected, outcome.await())
            idleMain()
            assertEquals(1, fake.destroyCount)
        }
    }

    @Test
    fun `collect forwards progress stages`() = runTest {
        val fake = FakeCollector()
        val stages = mutableListOf<CollectStage>()
        val outcome = async { collecting(fake).collect(URL, CAR_ID) { stages += it } }
        runCurrent()

        fake.onStage!!(CollectStage.OpeningPage)
        fake.onStage!!(CollectStage.FetchingHistory)
        fake.callback!!(CollectorWebView.Result.Deleted)
        outcome.await()

        assertEquals(listOf(CollectStage.OpeningPage, CollectStage.FetchingHistory), stages)
    }

    @Test
    fun `cancelling collect destroys the collector once and drops a late result`() = runTest {
        val fake = FakeCollector()
        val outcome = async { collecting(fake).collect(URL, CAR_ID) {} }
        runCurrent()

        outcome.cancel()
        runCurrent()
        idleMain()
        assertEquals(1, fake.destroyCount)

        fake.callback!!(CollectorWebView.Result.Success("late"))
        idleMain()
        assertEquals(1, fake.destroyCount)
        assertTrue(outcome.isCancelled)
    }

    @Test
    fun `collect destroys the collector and rethrows when collect throws synchronously`() = runTest {
        val fake = FakeCollector().apply { throwOnCollect = IllegalStateException("boom") }

        val result = runCatching { collecting(fake).collect(URL, CAR_ID) {} }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(1, fake.destroyCount)
        idleMain()
        assertEquals(1, fake.destroyCount)
    }

    private fun sample(name: String): EvaluationReport {
        val text = requireNotNull(javaClass.classLoader?.getResource("report/$name")).readText()
        return (ReportParser.parse(text) as ReportParser.Result.Ok).report
    }

    private companion object {
        const val RAW = """{"carId":"41878112"}"""
        const val CAR_ID = "41878112"
        const val URL = "https://fem.encar.com/cars/detail/$CAR_ID"
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
