package com.car.autoverdict.saved

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.db.SavedCarEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedCarsRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `twin rows count once, delete removes both and restore brings them back`() = runTest {
        val dao = db.savedCarDao()
        val repository = SavedCarsRepository(dao, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val twins = listOf(row("100", savedAt = 1_000), row("900", savedAt = 2_000))
        dao.upsertAll(twins + row("200", savedAt = 3_000, vehicleId = "200"))

        assertEquals(2, repository.itemCount.first { it == 2 })
        val item = repository.items.first().single { it.identity == "100" }

        val deleted = repository.delete(item)
        assertEquals(setOf("100", "900"), deleted.map { it.carId }.toSet())
        assertEquals(listOf("200"), repository.items.first { it.size == 1 }.map { it.identity })

        repository.restore(deleted)
        assertEquals(twins.toSet(), dao.getByCarIds(listOf("100", "900")).toSet())
    }

    private fun row(carId: String, savedAt: Long, vehicleId: String = "100") = SavedCarEntity(
        carId = carId,
        url = "https://fem.encar.com/cars/detail/$carId",
        title = "Car $carId",
        year = null,
        mileageKm = null,
        priceWon = null,
        fuelType = null,
        score = 80,
        verdict = "OK",
        dangerCount = 0,
        cautionCount = 0,
        passCount = 0,
        unknownCount = 0,
        rawJson = """{"preloadedState":{"cars":{"base":{"vehicleId":$vehicleId}}}}""",
        savedAt = savedAt,
        updatedAt = savedAt,
    )
}
