package com.car.autoverdict.result

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedCarKeyTest {

    private fun payload(carId: String, base: String) =
        """{"carId":"$carId","preloadedState":{"cars":{"base":$base}}}"""

    @Test
    fun `key is the original vehicle id, else the fallback`() {
        // Unreadable payload.
        assertEquals("555", SavedCarKey.of("{broken", fallback = "555"))
        // Regular listing: vehicleId equals the URL id, as a number or a numeric string.
        assertEquals("41878112", SavedCarKey.of(payload("41878112", """{"vehicleId":41878112}"""), "41878112"))
        assertEquals("41878112", SavedCarKey.of(payload("41878112", """{"vehicleId":"41878112"}"""), "41878112"))
        // Dummy ad: the original vehicle id.
        val dummy = """{"vehicleId":42751994,"queryCarId":42753994,"manage":{"dummy":true,"dummyVehicleId":42753994}}"""
        assertEquals("42751994", SavedCarKey.of(payload("42753994", dummy), "42753994"))
        // Dummy flag pointing elsewhere: the collector reports such a page Deleted, so it is never
        // saved; the key still follows vehicleId, like the saved list's grouping.
        val mismatch = """{"vehicleId":41000001,"queryCarId":42000002,"manage":{"dummy":true,"dummyVehicleId":42000003}}"""
        assertEquals("41000001", SavedCarKey.of(payload("42000002", mismatch), "42000002"))
    }
}
