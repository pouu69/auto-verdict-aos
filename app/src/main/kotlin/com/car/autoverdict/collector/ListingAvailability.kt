package com.car.autoverdict.collector

import org.json.JSONObject

/**
 * Decides from `__PRELOADED_STATE__.cars.base` whether an Encar listing is gone (sold or
 * replaced) and must not be analyzed.
 *
 * - `advertisement.status == "SOLD"`: Encar keeps serving a sold listing with its own data.
 * - Dummy ad: a second ad id for an existing vehicle (`advertisementType` AD_DUMMY). Opening it
 *   serves the original vehicle's base with `vehicleId` = original id, `queryCarId` = the
 *   requested id and `manage.dummy=true, dummyVehicleId=<requested id>`. It is the same car
 *   (same plate/VIN), so it is live unless sold.
 * - Any other `vehicleId != queryCarId` means the page shows a car we did not ask for; treat it
 *   as gone rather than analyze the wrong car.
 */
internal object ListingAvailability {

    fun isGone(base: JSONObject): Boolean {
        if (base.optJSONObject("advertisement")?.optString("status") == SOLD) return true

        val vehicleId = idOf(base, "vehicleId")
        val queryCarId = idOf(base, "queryCarId")
        if (vehicleId == null || queryCarId == null || vehicleId == queryCarId) return false

        val manage = base.optJSONObject("manage")
        val isDummyAliasOfQuery = manage != null &&
            manage.optBoolean("dummy", false) &&
            idOf(manage, "dummyVehicleId") == queryCarId
        return !isDummyAliasOfQuery
    }

    // Encar sends ids as numbers, but compare by value so "123" and 123 match.
    private fun idOf(obj: JSONObject, key: String): String? =
        when (val raw = if (obj.isNull(key)) null else obj.opt(key)) {
            is Number -> raw.toLong().toString()
            is String -> raw.trim().takeIf { it.isNotEmpty() }
            else -> null
        }

    private const val SOLD = "SOLD"
}
