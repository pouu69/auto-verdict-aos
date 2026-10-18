package com.car.autoverdict.saved

import com.car.autoverdict.db.SavedCarEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedIdentityTest {

    @Test
    fun `regular listings have vehicleId equal to carId`() {
        for (carId in listOf("39339524", "41623744", "41878112", "42264483")) {
            val raw = requireNotNull(javaClass.classLoader?.getResource("raw/encar-$carId.json")).readText()
            assertEquals(carId, vehicleIdOf(raw))
        }
    }

    @Test
    fun `unusable vehicleId falls back to carId`() {
        assertEquals("123", vehicleIdOf(raw("\"123\"")))
        assertNull(vehicleIdOf(raw("0")))
        assertNull(vehicleIdOf(raw("null")))
        assertNull(vehicleIdOf("""{"preloadedState":null}"""))
        assertNull(vehicleIdOf("{broken"))
        assertEquals("555", savedIdentity(row("555", rawJson = "{broken")))
    }

    @Test
    fun `dummy ad and original rows group into one item led by the latest evaluation`() {
        val original = row("100", vehicleId = "100", updatedAt = 1_000)
        val dummyAd = row("900", vehicleId = "100", updatedAt = 2_000)
        val other = row("200", vehicleId = "200", updatedAt = 1_500)

        val items = groupSavedCars(listOf(original, other, dummyAd))

        assertEquals(listOf("100", "200"), items.map { it.identity })
        assertEquals("900", items[0].representative.carId)
        assertEquals(listOf("900", "100"), items[0].carIds)
    }

    @Test
    fun `representative ties prefer carId equal to identity, then the first carId`() {
        val tiedWithOriginal = listOf(row("900", vehicleId = "100"), row("100", vehicleId = "100"))
        assertEquals("100", groupSavedCars(tiedWithOriginal).single().representative.carId)

        val tiedDummies = listOf(row("902", vehicleId = "100"), row("901", vehicleId = "100"))
        assertEquals("901", groupSavedCars(tiedDummies).single().representative.carId)
    }

    private fun raw(vehicleId: String) = """{"preloadedState":{"cars":{"base":{"vehicleId":$vehicleId}}}}"""

    private fun row(
        carId: String,
        vehicleId: String? = null,
        updatedAt: Long = 1_000,
        rawJson: String = if (vehicleId != null) raw(vehicleId) else "{}",
    ) = SavedCarEntity(
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
        rawJson = rawJson,
        savedAt = updatedAt,
        updatedAt = updatedAt,
    )
}
