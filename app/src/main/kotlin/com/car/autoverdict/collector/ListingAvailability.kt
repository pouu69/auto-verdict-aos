package com.car.autoverdict.collector

import org.json.JSONObject

/**
 * Decides from `__PRELOADED_STATE__` whether an Encar listing is gone (sold, replaced or no
 * longer existing — see [isMissing]) and must not be analyzed. [isGone] reads `cars.base`:
 *
 * - `advertisement.status == "SOLD"`: Encar keeps serving a sold listing with its own data.
 * - Dummy ad: a second ad id for an existing vehicle (`advertisementType` AD_DUMMY). Opening it
 *   serves the original vehicle's base with `vehicleId` = original id, `queryCarId` = the
 *   requested id and `manage.dummy=true, dummyVehicleId=<requested id>`. It is the same car
 *   (same plate/VIN), so it is live unless sold.
 * - Any other `vehicleId != queryCarId` means the page shows a car we did not ask for; treat it
 *   as gone rather than analyze the wrong car. Defensive only: no real page with this shape has
 *   been observed (every mismatch sampled was a dummy ad of the requested id). It guards the
 *   "substitute car" case assumed by 6776ad5.
 */
internal object ListingAvailability {

    fun isGone(base: JSONObject): Boolean {
        if (base.optJSONObject("advertisement")?.optString("status") == SOLD) return true

        val vehicleId = idOf(base, "vehicleId")
        val queryCarId = idOf(base, "queryCarId")
        if (vehicleId == null || queryCarId == null || vehicleId == queryCarId) return false

        // Defensive branch (never observed live): a different car without a dummy link.
        val manage = base.optJSONObject("manage")
        val isDummyAliasOfQuery = manage != null &&
            manage.optBoolean("dummy", false) &&
            idOf(manage, "dummyVehicleId") == queryCarId
        return !isDummyAliasOfQuery
    }

    /**
     * True when the listing no longer exists. Encar still answers HTTP 200 with a state, but its
     * base API (`/v1/readside/vehicle/{id}`) 404s, leaving `cars.base` empty and
     * `pender.failure["cars/GET_BASE"] == true` (seen on 30000000, 35000123, 39000001).
     * An empty base without that flag is not proof of removal and stays a read failure.
     */
    fun isMissing(state: JSONObject): Boolean {
        val base = state.optJSONObject("cars")?.optJSONObject("base")
        if (base != null && base.length() > 0) return false
        val failure = state.optJSONObject("pender")?.optJSONObject("failure") ?: return false
        return failure.optBoolean(GET_BASE, false)
    }

    // Encar sends ids as numbers, but compare by value so "123" and 123 match.
    private fun idOf(obj: JSONObject, key: String): String? =
        when (val raw = if (obj.isNull(key)) null else obj.opt(key)) {
            is Number -> raw.toLong().toString()
            is String -> raw.trim().takeIf { it.isNotEmpty() }
            else -> null
        }

    private const val SOLD = "SOLD"
    private const val GET_BASE = "cars/GET_BASE"
}
