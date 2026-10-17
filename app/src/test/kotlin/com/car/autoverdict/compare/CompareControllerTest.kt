package com.car.autoverdict.compare

import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.engine.EngineResult
import com.car.autoverdict.engine.ReportEvaluator
import com.car.autoverdict.result.sampleReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompareControllerTest {

    private val base = sampleReport("sample-caution.json")

    /** Fake repository: the rows table, in DAO (not input) order. */
    private val rows = MutableStateFlow<List<SavedCarEntity>>(emptyList())
    private var observedIds: List<String>? = null

    /** Fake engine: succeeds unless the rawJson is in [failing]; records every request. */
    private val failing = mutableSetOf<String>()
    private val evaluated = mutableListOf<String>()
    private val evaluator = ReportEvaluator { raw ->
        evaluated += raw
        if (raw in failing) EngineResult.Failed("test") else EngineResult.Success(base.copy(carId = raw.removePrefix("raw-")))
    }

    private fun row(carId: String, updatedAt: Long = 0) = SavedCarEntity(
        carId = carId, url = "https://fem.encar.com/cars/detail/$carId", title = "2015 BMW 5시리즈",
        year = 2015, mileageKm = 98_000, priceWon = 12_900_000, fuelType = "디젤",
        score = 70, verdict = "CAUTION", dangerCount = 0, cautionCount = 1,
        passCount = 5, unknownCount = 0, rawJson = "raw-$carId", savedAt = 0, updatedAt = updatedAt,
    )

    private fun TestScope.started(vararg ids: String): CompareController =
        CompareController(
            ids.toList(),
            observe = { observedIds = it; rows },
            evaluator = evaluator,
            scope = backgroundScope,
        ).also {
            it.start()
            runCurrent()
        }

    private fun CompareController.table(): CompareTable = (state.value as CompareUiState.Ready).table

    @Test
    fun `evaluates each row and builds the table in the chosen order`() = runTest {
        rows.value = listOf(row("2"), row("1"))
        val c = started("1", "2")

        assertEquals(listOf("1", "2"), c.table().headers.map { it.carId })
        assertEquals(listOf(false, false), c.table().headers.map { it.fallback })
        assertEquals(setOf("raw-1", "raw-2"), evaluated.toSet())

        // Only the row whose updatedAt changed is evaluated again.
        evaluated.clear()
        rows.value = listOf(row("2", updatedAt = 5), row("1"))
        runCurrent()
        assertEquals(listOf("raw-2"), evaluated)
    }

    @Test
    fun `starts loading and compares at most three cars`() = runTest {
        val c = CompareController(listOf("1", "2", "3", "4"), { observedIds = it; rows }, evaluator, backgroundScope)
        assertEquals(CompareUiState.Loading, c.state.value)
        rows.value = listOf(row("1"), row("2"), row("3"), row("4"))
        c.start()
        runCurrent()

        assertEquals(listOf("1", "2", "3"), observedIds)
        assertEquals(3, c.table().headers.size)
    }

    @Test
    fun `an engine failure becomes a saved-summary column`() = runTest {
        failing += "raw-2"
        rows.value = listOf(row("1"), row("2"))
        val c = started("1", "2")

        assertEquals(listOf(false, true), c.table().headers.map { it.fallback })
    }

    @Test
    fun `fewer than two remaining rows is TooFew`() = runTest {
        rows.value = listOf(row("1"), row("2"))
        val c = started("1", "2")
        assertEquals(2, c.table().headers.size)

        // Unsaved from the result screen, then back here.
        rows.value = listOf(row("1"))
        runCurrent()
        assertEquals(CompareUiState.TooFew, c.state.value)
    }
}
