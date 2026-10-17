package com.car.autoverdict.collector

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * `cars.base` shapes taken from live fem.encar.com pages (2026-10-17), reduced to the fields
 * [ListingAvailability] reads. Plate, VIN, contact and photos are dropped.
 */
@RunWith(RobolectricTestRunner::class)
class ListingAvailabilityTest {

    private fun base(
        vehicleId: Any?,
        queryCarId: Any?,
        dummy: Boolean,
        dummyVehicleId: Any?,
        reRegistered: Boolean,
        status: String,
    ) = JSONObject(
        """
        {
          "manage": {
            "dummy": $dummy,
            "dummyVehicleId": ${json(dummyVehicleId)},
            "reRegistered": $reRegistered
          },
          "advertisement": { "status": "$status" },
          "vehicleId": ${json(vehicleId)},
          "queryCarId": ${json(queryCarId)}
        }
        """.trimIndent(),
    )

    private fun json(v: Any?) = when (v) {
        null -> "null"
        is String -> "\"$v\""
        else -> v.toString()
    }

    // --- reported listings: dummy-ad URLs of live cars (were shown as Deleted) ---

    @Test
    fun dummyAdOfLiveCar_42753994_isNotGone() {
        // Same plate/VIN/price as /cars/detail/42751994.
        assertFalse(
            ListingAvailability.isGone(
                base(42751994, 42753994, dummy = true, dummyVehicleId = 42753994, reRegistered = false, status = "ADVERTISE"),
            ),
        )
    }

    @Test
    fun reRegisteredDummyAdOfLiveCar_41822105_isNotGone() {
        // Same plate/VIN/price as /cars/detail/41821577.
        assertFalse(
            ListingAvailability.isGone(
                base(41821577, 41822105, dummy = true, dummyVehicleId = 41822105, reRegistered = true, status = "ADVERTISE"),
            ),
        )
    }

    @Test
    fun dummyAdIds_matchAcrossNumberAndString() {
        assertFalse(
            ListingAvailability.isGone(
                base(42751994, "42753994", dummy = true, dummyVehicleId = "42753994", reRegistered = false, status = "ADVERTISE"),
            ),
        )
    }

    // --- known-good listings ---

    @Test
    fun liveListing_41878112_isNotGone() {
        assertFalse(
            ListingAvailability.isGone(
                base(41878112, 41878112, dummy = false, dummyVehicleId = null, reRegistered = true, status = "ADVERTISE"),
            ),
        )
    }

    @Test
    fun liveListing_39339524_isNotGone() {
        assertFalse(
            ListingAvailability.isGone(
                base(39339524, 39339524, dummy = false, dummyVehicleId = null, reRegistered = true, status = "ADVERTISE"),
            ),
        )
    }

    @Test
    fun originalOfDummyAd_42751994_isNotGone() {
        // The original vehicle names its dummy ad in dummyVehicleId but is not itself a dummy.
        assertFalse(
            ListingAvailability.isGone(
                base(42751994, 42751994, dummy = false, dummyVehicleId = 42753994, reRegistered = false, status = "ADVERTISE"),
            ),
        )
    }

    @Test
    fun missingManageAndAdvertisement_isNotGone() {
        assertFalse(ListingAvailability.isGone(JSONObject("""{"vehicleId":123,"vehicleNo":"x"}""")))
    }

    // --- sold listings ---

    @Test
    fun soldListing_40000002_isGone() {
        assertTrue(
            ListingAvailability.isGone(
                base(40000002, 40000002, dummy = false, dummyVehicleId = null, reRegistered = false, status = "SOLD"),
            ),
        )
    }

    @Test
    fun soldDummyAd_42753993_isGone() {
        assertTrue(
            ListingAvailability.isGone(
                base(42753654, 42753993, dummy = true, dummyVehicleId = 42753993, reRegistered = false, status = "SOLD"),
            ),
        )
    }

    // --- page shows a car other than the one requested ---

    @Test
    fun otherCarWithoutDummyLink_isGone() {
        // The substitute shape described in 6776ad5: different vehicleId, dummy=true, but no
        // dummyVehicleId tying it to the requested id.
        assertTrue(
            ListingAvailability.isGone(
                base(41000001, 42000002, dummy = true, dummyVehicleId = null, reRegistered = false, status = "ADVERTISE"),
            ),
        )
    }

    @Test
    fun dummyLinkToAnotherId_isGone() {
        assertTrue(
            ListingAvailability.isGone(
                base(41000001, 42000002, dummy = true, dummyVehicleId = 42000003, reRegistered = false, status = "ADVERTISE"),
            ),
        )
    }

    @Test
    fun idMismatchWithoutDummyFlag_isGone() {
        assertTrue(
            ListingAvailability.isGone(
                base(41000001, 42000002, dummy = false, dummyVehicleId = 42000002, reRegistered = false, status = "ADVERTISE"),
            ),
        )
    }
}
