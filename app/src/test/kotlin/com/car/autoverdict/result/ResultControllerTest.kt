package com.car.autoverdict.result

import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.engine.EngineResult
import com.car.autoverdict.engine.ReportEvaluator
import com.car.autoverdict.report.DataGap
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.GapReason
import com.car.autoverdict.report.GapSource
import com.car.autoverdict.report.ReportParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ResultControllerTest {

    private val caution = sample("sample-caution.json")
    private val refreshed = caution.copy(score = 70, headline = "refreshed")
    private val loginGap = DataGap(GapSource.Record, GapReason.LoginRequired)
    private val failedGap = DataGap(GapSource.Inspection, GapReason.Failed)

    private val repo = FakeRepository()
    private val evaluator = FakeEvaluator()

    private fun TestScope.controller(url: String = URL): ResultController =
        ResultController(url, repo, evaluator, backgroundScope)

    /** Records every state, including the transient ones (Loading steps). */
    private fun TestScope.history(controller: ResultController): List<ResultUiState> {
        val states = mutableListOf<ResultUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.state.toList(states) }
        return states
    }

    private fun TestScope.events(controller: ResultController): List<ResultEvent> {
        val events = mutableListOf<ResultEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.events.toList(events) }
        return events
    }

    private fun ResultController.ready(): ResultUiState.Ready = state.value as ResultUiState.Ready

    // --- entry ---

    @Test
    fun `invalid url is an error without touching the repository`() = runTest {
        val c = controller(url = "https://example.com/nope")
        c.start()
        runCurrent()

        assertEquals(ResultUiState.Error(ResultErrorKind.InvalidUrl), c.state.value)
        assertTrue(repo.reads.isEmpty())
        assertTrue(repo.collects.isEmpty())
    }

    @Test
    fun `start runs only once`() = runTest {
        val c = controller()
        c.start()
        c.start()
        runCurrent()

        assertEquals(1, repo.collects.size)
    }

    // --- cache ---

    @Test
    fun `fresh cache shows Ready without collecting`() = runTest {
        repo.cache = "cache-raw"
        evaluator.results["cache-raw"] = EngineResult.Success(caution)
        val c = controller()
        val states = history(c)

        c.start()
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = false, banner = null), c.state.value)
        assertTrue(states.none { it is ResultUiState.Loading && it.stage != LoadingStage.OpeningPage })
        assertTrue(repo.collects.isEmpty())
        assertTrue(repo.cacheWrites.isEmpty())
    }

    @Test
    fun `cache that fails to evaluate falls back to collecting`() = runTest {
        repo.cache = "cache-raw"
        val c = controller()
        c.start()
        runCurrent()

        assertEquals(1, repo.collects.size)
        assertEquals(ResultUiState.Loading(LoadingStage.OpeningPage), c.state.value)
    }

    // --- collect ---

    @Test
    fun `collect walks the loading steps, then shows Ready and caches`() = runTest {
        val c = controller()
        val states = history(c)
        c.start()
        runCurrent()
        assertEquals(ResultUiState.Loading(LoadingStage.OpeningPage), c.state.value)

        val call = repo.collects.single()
        assertEquals(URL, call.url)
        assertEquals(CAR_ID, call.carId)
        call.onStage(CollectStage.OpeningPage)
        call.onStage(CollectStage.FetchingHistory)
        assertEquals(ResultUiState.Loading(LoadingStage.FetchingHistory), c.state.value)

        evaluator.results["raw"] = EngineResult.Success(caution)
        call.result.complete(CollectOutcome.Success("raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = false, banner = null), c.state.value)
        assertEquals(
            listOf(
                ResultUiState.Loading(LoadingStage.OpeningPage),
                ResultUiState.Loading(LoadingStage.FetchingHistory),
                ResultUiState.Loading(LoadingStage.Evaluating),
                ResultUiState.Ready(caution, saved = false, banner = null),
            ),
            states,
        )
        assertEquals(listOf(caution to "raw"), repo.cacheWrites)
    }

    @Test
    fun `collect failures map to error kinds`() = runTest {
        val expected = mapOf(
            CollectErrorKind.Network to ResultErrorKind.Network,
            CollectErrorKind.Timeout to ResultErrorKind.Timeout,
            CollectErrorKind.PageUnreadable to ResultErrorKind.PageUnreadable,
        )
        for ((kind, errorKind) in expected) {
            val c = controller()
            c.start()
            runCurrent()
            repo.collects.last().result.complete(CollectOutcome.Failed(kind))
            runCurrent()
            assertEquals(ResultUiState.Error(errorKind), c.state.value)
        }
    }

    @Test
    fun `engine failure after collecting is EngineFailed`() = runTest {
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Success("bad-raw"))
        runCurrent()

        assertEquals(ResultUiState.Error(ResultErrorKind.EngineFailed), c.state.value)
        assertTrue(repo.cacheWrites.isEmpty())
    }

    @Test
    fun `deleted listing that is not saved shows Deleted`() = runTest {
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Deleted)
        runCurrent()

        assertEquals(ResultUiState.Deleted, c.state.value)
    }

    @Test
    fun `reports with data gaps are shown with a banner but not cached`() = runTest {
        val cases = listOf(
            listOf(loginGap) to ResultBanner.LoginRequired,
            listOf(failedGap) to ResultBanner.PartialData,
            listOf(failedGap, loginGap) to ResultBanner.LoginRequired,
        )
        for ((gaps, banner) in cases) {
            val report = caution.copy(dataGaps = gaps)
            evaluator.results["gap-raw"] = EngineResult.Success(report)
            val c = controller()
            c.start()
            runCurrent()
            repo.collects.last().result.complete(CollectOutcome.Success("gap-raw"))
            runCurrent()

            assertEquals(ResultUiState.Ready(report, saved = false, banner = banner), c.state.value)
        }
        assertTrue(repo.cacheWrites.isEmpty())
    }

    private fun givenPartialCache(): EvaluationReport {
        val partial = caution.copy(dataGaps = listOf(failedGap))
        repo.cache = "cache-raw"
        evaluator.results["cache-raw"] = EngineResult.Success(partial)
        return partial
    }

    @Test
    fun `retry with a report on screen skips the cache and keeps the report while refreshing`() = runTest {
        val partial = givenPartialCache()
        val c = controller()
        val states = history(c)
        c.start()
        runCurrent()
        assertEquals(ResultBanner.PartialData, c.ready().banner)

        c.retry()
        runCurrent()

        assertEquals(
            ResultUiState.Ready(partial, saved = false, banner = ResultBanner.PartialData, refreshing = true),
            c.state.value,
        )
        assertEquals(1, repo.collects.size)
        evaluator.results["raw"] = EngineResult.Success(caution)
        repo.collects.single().result.complete(CollectOutcome.Success("raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = false, banner = null), c.state.value)
        assertEquals(listOf(caution to "raw"), repo.cacheWrites)
        assertTrue(states.none { it is ResultUiState.Loading && it.stage != LoadingStage.OpeningPage })
        assertTrue(states.dropWhile { it !is ResultUiState.Ready }.all { it is ResultUiState.Ready })
    }

    @Test
    fun `in-place retry that fails keeps the report and reports RetryFailed`() = runTest {
        val partial = givenPartialCache()
        val outcomes = listOf(
            CollectOutcome.Failed(CollectErrorKind.Network),
            CollectOutcome.Success("bad-raw"), // engine failure
            CollectOutcome.Success("more-gaps"),
        )
        evaluator.results["more-gaps"] =
            EngineResult.Success(caution.copy(dataGaps = listOf(failedGap, loginGap)))
        val c = controller()
        val events = events(c)
        c.start()
        runCurrent()

        for (outcome in outcomes) {
            c.retry()
            runCurrent()
            repo.collects.last().result.complete(outcome)
            runCurrent()
            assertEquals(ResultUiState.Ready(partial, saved = false, banner = ResultBanner.PartialData), c.state.value)
        }
        assertEquals(List(3) { ResultEvent.RetryFailed }, events)
        assertTrue(repo.cacheWrites.isEmpty())
    }

    @Test
    fun `in-place retry on an unsaved car that is now deleted shows Deleted`() = runTest {
        givenPartialCache()
        val c = controller()
        c.start()
        runCurrent()

        c.retry()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Deleted)
        runCurrent()

        assertEquals(ResultUiState.Deleted, c.state.value)
    }

    @Test
    fun `retry is ignored while an in-place retry is running`() = runTest {
        givenPartialCache()
        val c = controller()
        c.start()
        runCurrent()

        c.retry()
        runCurrent()
        c.retry()
        runCurrent()

        assertEquals(1, repo.collects.size)
        assertFalse(repo.collects.single().cancelled)
    }

    @Test
    fun `cancelling an in-place retry clears refreshing and keeps the report`() = runTest {
        val partial = givenPartialCache()
        val scopeJob = Job()
        val c = ResultController(URL, repo, evaluator, CoroutineScope(StandardTestDispatcher(testScheduler) + scopeJob))
        c.start()
        runCurrent()

        c.retry()
        runCurrent()
        assertTrue(c.ready().refreshing)

        scopeJob.cancel()
        runCurrent()

        assertTrue(repo.collects.single().cancelled)
        assertEquals(ResultUiState.Ready(partial, saved = false, banner = ResultBanner.PartialData), c.state.value)
    }

    @Test
    fun `in-place retry swaps immediately even after the user scrolled`() = runTest {
        givenPartialCache()
        val c = controller()
        c.start()
        runCurrent()
        c.onUserScrolled()

        c.retry()
        runCurrent()
        evaluator.results["raw"] = EngineResult.Success(caution)
        repo.collects.single().result.complete(CollectOutcome.Success("raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = false, banner = null), c.state.value)
    }

    @Test
    fun `retry cancels the running collect and ignores its late stages`() = runTest {
        val c = controller()
        c.start()
        runCurrent()
        val first = repo.collects.single()

        c.retry()
        runCurrent()
        assertTrue(first.cancelled)
        first.onStage(CollectStage.FetchingHistory)

        assertEquals(ResultUiState.Loading(LoadingStage.OpeningPage), c.state.value)
        assertEquals(2, repo.collects.size)
    }

    @Test
    fun `retry from an error collects again`() = runTest {
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Failed(CollectErrorKind.Network))
        runCurrent()

        c.retry()
        runCurrent()
        evaluator.results["raw"] = EngineResult.Success(caution)
        repo.collects.last().result.complete(CollectOutcome.Success("raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = false, banner = null), c.state.value)
    }

    // --- saved car ---

    private fun givenSavedCar() {
        repo.saved = savedEntity(rawJson = "saved-raw", savedAt = SAVED_AT)
        evaluator.results["saved-raw"] = EngineResult.Success(caution)
    }

    @Test
    fun `saved car shows Ready immediately and refreshes in the background`() = runTest {
        givenSavedCar()
        repo.cache = "cache-raw" // the saved row wins over the cache
        val c = controller()
        val states = history(c)
        c.start()
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null), c.state.value)
        assertEquals(1, repo.collects.size)

        evaluator.results["new-raw"] = EngineResult.Success(refreshed)
        repo.collects.single().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(refreshed, saved = true, banner = null), c.state.value)
        assertEquals(listOf(SaveCall(refreshed, "new-raw", SAVED_AT)), repo.saves)
        assertEquals(listOf(refreshed to "new-raw"), repo.cacheWrites)
        // The background refresh never shows loading steps after Ready.
        val firstReady = states.indexOfFirst { it is ResultUiState.Ready }
        assertTrue(states.drop(firstReady).all { it is ResultUiState.Ready })
    }

    @Test
    fun `refresh after the user scrolled waits behind the UpdateAvailable banner`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        c.onUserScrolled()

        evaluator.results["new-raw"] = EngineResult.Success(refreshed)
        repo.collects.single().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = ResultBanner.UpdateAvailable), c.state.value)
        assertTrue(repo.saves.isEmpty())

        c.applyUpdate()
        runCurrent()

        assertEquals(ResultUiState.Ready(refreshed, saved = true, banner = null), c.state.value)
        assertEquals(listOf(SaveCall(refreshed, "new-raw", SAVED_AT)), repo.saves)
    }

    @Test
    fun `failed refresh keeps the saved result quietly`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Failed(CollectErrorKind.Timeout))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null), c.state.value)
        assertTrue(repo.saves.isEmpty())
    }

    @Test
    fun `refresh that fails to evaluate keeps the saved result`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Success("bad-raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null), c.state.value)
    }

    @Test
    fun `refresh with more data gaps than the saved result is ignored`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        evaluator.results["new-raw"] = EngineResult.Success(refreshed.copy(dataGaps = listOf(loginGap)))
        repo.collects.single().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null), c.state.value)
        assertTrue(repo.saves.isEmpty())
        assertTrue(repo.cacheWrites.isEmpty())
    }

    @Test
    fun `refresh that finds the listing deleted keeps the saved result with SoldOut`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Deleted)
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = ResultBanner.SoldOut), c.state.value)
    }

    @Test
    fun `saved car whose raw fails to evaluate is collected again and re-saved`() = runTest {
        repo.saved = savedEntity(rawJson = "broken", savedAt = SAVED_AT)
        repo.cache = "cache-raw"
        evaluator.results["cache-raw"] = EngineResult.Success(caution)
        val c = controller()
        c.start()
        runCurrent()

        // Plan: a broken saved row goes straight to collecting (skips the cache).
        assertEquals(ResultUiState.Loading(LoadingStage.OpeningPage), c.state.value)
        evaluator.results["new-raw"] = EngineResult.Success(refreshed)
        repo.collects.single().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()

        // Saved status still comes from the DB row.
        assertEquals(ResultUiState.Ready(refreshed, saved = true, banner = null), c.state.value)
        assertEquals(listOf(SaveCall(refreshed, "new-raw", SAVED_AT)), repo.saves)
    }

    @Test
    fun `retry on a saved car that comes back with more gaps keeps the saved result`() = runTest {
        givenSavedCar()
        val c = controller()
        val events = events(c)
        c.start()
        runCurrent()

        c.retry()
        runCurrent()
        evaluator.results["new-raw"] = EngineResult.Success(refreshed.copy(dataGaps = listOf(loginGap)))
        repo.collects.last().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null), c.state.value)
        assertTrue(repo.saves.isEmpty())
        assertEquals(listOf(ResultEvent.RetryFailed), events)
    }

    @Test
    fun `save during an in-place retry keeps refreshing and the result updates the new row`() = runTest {
        repo.cache = "cache-raw"
        evaluator.results["cache-raw"] = EngineResult.Success(caution)
        val c = controller()
        c.start()
        runCurrent()

        c.retry()
        c.save()
        runCurrent()
        assertEquals(
            ResultUiState.Ready(caution, saved = true, banner = null, refreshing = true),
            c.state.value,
        )

        evaluator.results["raw"] = EngineResult.Success(refreshed)
        repo.collects.single().result.complete(CollectOutcome.Success("raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(refreshed, saved = true, banner = null), c.state.value)
        // The second write keeps the savedAt returned by the first one.
        assertEquals(
            listOf(SaveCall(caution, "cache-raw", null), SaveCall(refreshed, "raw", NOW)),
            repo.saves,
        )
    }

    @Test
    fun `save writes the report on screen when the lock is taken, not when tapped`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        c.onUserScrolled()
        evaluator.results["new-raw"] = EngineResult.Success(refreshed)
        repo.collects.single().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()
        c.unsave()
        runCurrent()
        assertEquals(ResultBanner.UpdateAvailable, c.ready().banner)

        c.save()
        c.applyUpdate() // swaps the report before the save coroutine runs
        runCurrent()

        assertEquals(SaveCall(refreshed, "new-raw", null), repo.saves.first())
        assertEquals(ResultUiState.Ready(refreshed, saved = true, banner = null), c.state.value)
    }

    @Test
    fun `retry on a saved car that is now deleted keeps the saved result with SoldOut`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Failed(CollectErrorKind.Network))
        runCurrent()

        c.retry()
        runCurrent()
        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null, refreshing = true), c.state.value)
        repo.collects.last().result.complete(CollectOutcome.Deleted)
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = ResultBanner.SoldOut), c.state.value)
    }

    @Test
    fun `retry on a saved car that fails keeps the saved result`() = runTest {
        givenSavedCar()
        val c = controller()
        val events = events(c)
        c.start()
        runCurrent()

        c.retry()
        runCurrent()
        repo.collects.last().result.complete(CollectOutcome.Failed(CollectErrorKind.Timeout))
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null), c.state.value)
        assertEquals(listOf(ResultEvent.RetryFailed), events)
        // The cancelled background refresh does not count as a failure.
        assertTrue(repo.collects.first().cancelled)
    }

    @Test
    fun `retry on a saved car that succeeds updates the saved row`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()

        c.retry()
        runCurrent()
        evaluator.results["new-raw"] = EngineResult.Success(refreshed)
        repo.collects.last().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(refreshed, saved = true, banner = null), c.state.value)
        assertEquals(listOf(SaveCall(refreshed, "new-raw", SAVED_AT)), repo.saves)
        assertEquals(listOf(refreshed to "new-raw"), repo.cacheWrites)
    }

    // --- save / unsave ---

    @Test
    fun `save stores the raw behind the shown report and flips saved`() = runTest {
        repo.cache = "cache-raw"
        evaluator.results["cache-raw"] = EngineResult.Success(caution)
        val c = controller()
        val events = events(c)
        c.start()
        runCurrent()

        c.save()
        assertTrue(c.ready().saving)
        c.save() // ignored while saving
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = true, banner = null, saving = false), c.state.value)
        assertEquals(listOf(SaveCall(caution, "cache-raw", null)), repo.saves)
        assertEquals(listOf(ResultEvent.Saved), events)
    }

    @Test
    fun `unsave deletes the row and flips saved`() = runTest {
        givenSavedCar()
        val c = controller()
        val events = events(c)
        c.start()
        runCurrent()

        c.unsave()
        c.unsave()
        runCurrent()

        assertFalse(c.ready().saved)
        assertEquals(listOf(CAR_ID), repo.deletes)
        assertEquals(listOf(ResultEvent.Unsaved), events)
    }

    @Test
    fun `save failure emits SaveFailed and keeps the button usable`() = runTest {
        repo.cache = "cache-raw"
        evaluator.results["cache-raw"] = EngineResult.Success(caution)
        repo.failWrites = true
        val c = controller()
        val events = events(c)
        c.start()
        runCurrent()

        c.save()
        runCurrent()

        assertEquals(ResultUiState.Ready(caution, saved = false, banner = null, saving = false), c.state.value)
        assertEquals(listOf(ResultEvent.SaveFailed), events)
    }

    @Test
    fun `refresh after unsave does not write the saved row`() = runTest {
        givenSavedCar()
        val c = controller()
        c.start()
        runCurrent()
        c.unsave()
        runCurrent()

        evaluator.results["new-raw"] = EngineResult.Success(refreshed)
        repo.collects.single().result.complete(CollectOutcome.Success("new-raw"))
        runCurrent()

        assertEquals(ResultUiState.Ready(refreshed, saved = false, banner = null), c.state.value)
        assertTrue(repo.saves.isEmpty())
    }

    @Test
    fun `save after collecting stores the collected raw`() = runTest {
        evaluator.results["raw"] = EngineResult.Success(caution)
        val c = controller()
        c.start()
        runCurrent()
        repo.collects.single().result.complete(CollectOutcome.Success("raw"))
        runCurrent()
        c.save()
        runCurrent()

        assertEquals(listOf(SaveCall(caution, "raw", null)), repo.saves)
        assertTrue(c.ready().saved)
        // savedAt comes back from save(); no re-read of the row.
        assertEquals(1, repo.savedCarReads)
    }

    // --- fakes ---

    private data class SaveCall(val report: EvaluationReport, val rawJson: String, val savedAt: Long?)

    private class CollectCall(val url: String, val carId: String, val onStage: (CollectStage) -> Unit) {
        val result = CompletableDeferred<CollectOutcome>()
        var cancelled = false
    }

    private inner class FakeRepository : ResultRepository {
        var saved: SavedCarEntity? = null
        var cache: String? = null
        var failWrites = false
        val reads = mutableListOf<String>()
        var savedCarReads = 0
        val collects = mutableListOf<CollectCall>()
        val cacheWrites = mutableListOf<Pair<EvaluationReport, String>>()
        val saves = mutableListOf<SaveCall>()
        val deletes = mutableListOf<String>()

        override suspend fun savedCar(carId: String): SavedCarEntity? {
            reads += carId
            savedCarReads++
            return saved
        }

        override suspend fun cachedRaw(carId: String): String? {
            reads += carId
            return cache
        }

        override suspend fun collect(url: String, carId: String, onStage: (CollectStage) -> Unit): CollectOutcome {
            val call = CollectCall(url, carId, onStage)
            collects += call
            try {
                return call.result.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                call.cancelled = true
                throw e
            }
        }

        override suspend fun writeCache(report: EvaluationReport, rawJson: String) {
            cacheWrites += report to rawJson
        }

        override suspend fun save(report: EvaluationReport, rawJson: String, savedAt: Long?): Long {
            if (failWrites) throw IOException("disk full")
            saves += SaveCall(report, rawJson, savedAt)
            val written = savedAt ?: NOW
            saved = savedEntity(rawJson, written)
            return written
        }

        override suspend fun delete(carId: String) {
            if (failWrites) throw IOException("disk full")
            deletes += carId
            saved = null
        }
    }

    private class FakeEvaluator : ReportEvaluator {
        val results = mutableMapOf<String, EngineResult>()
        override suspend fun evaluate(rawJson: String): EngineResult =
            results[rawJson] ?: EngineResult.Failed("unknown raw")
    }

    private companion object {
        const val CAR_ID = "41878112"
        const val URL = "https://fem.encar.com/cars/detail/$CAR_ID"
        const val SAVED_AT = 111L
        const val NOW = 999L

        fun sample(name: String): EvaluationReport {
            val text = requireNotNull(ResultControllerTest::class.java.classLoader?.getResource("report/$name"))
                .readText()
            return (ReportParser.parse(text) as ReportParser.Result.Ok).report
        }

        fun savedEntity(rawJson: String, savedAt: Long) = SavedCarEntity(
            carId = CAR_ID,
            url = URL,
            title = "2015 BMW 3시리즈 (F30)",
            year = 2015,
            mileageKm = 119_529,
            priceWon = 8_900_000L,
            fuelType = "디젤",
            score = 89,
            verdict = "CAUTION",
            dangerCount = 0,
            cautionCount = 2,
            passCount = 9,
            unknownCount = 0,
            rawJson = rawJson,
            savedAt = savedAt,
            updatedAt = savedAt,
        )
    }
}
