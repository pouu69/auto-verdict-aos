package com.car.autoverdict.collector

import org.json.JSONException
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
        return originalOfDummyAd(base) == null
    }

    private fun originalOfDummyAd(base: JSONObject): String? {
        val manage = base.optJSONObject("manage")
        return originalOfDummyAd(
            vehicleId = idOf(base, "vehicleId"),
            queryCarId = idOf(base, "queryCarId"),
            dummy = manage?.optBoolean("dummy", false) ?: false,
            dummyVehicleId = manage?.let { idOf(it, "dummyVehicleId") },
        )
    }

    /**
     * The original vehicle id when a base is a dummy ad served for its `queryCarId`
     * (`vehicleId != queryCarId`, `manage.dummy`, `manage.dummyVehicleId == queryCarId`); null
     * for any other base, including the original listing itself. Ids are compared as normalized
     * strings ("123" for both 123 and "123").
     */
    fun originalOfDummyAd(vehicleId: String?, queryCarId: String?, dummy: Boolean, dummyVehicleId: String?): String? {
        if (vehicleId == null || queryCarId == null || vehicleId == queryCarId) return null
        return vehicleId.takeIf { dummy && dummyVehicleId == queryCarId }
    }

    /**
     * True when the page looks like a listing that no longer exists. Encar still answers HTTP 200
     * with a state, but its base API (`/v1/readside/vehicle/{id}`) failed, leaving `cars.base`
     * empty and `pender.failure["cars/GET_BASE"] == true` (seen on 30000000, 35000123, 39000001).
     * An empty base without that flag is not proof of removal and stays a read failure.
     *
     * The flag is set for any failed fetch (404, 5xx, network), so it only makes the listing a
     * suspect: the collector re-asks the base API and calls it gone only when
     * [isConfirmedMissing] sees a 404.
     */
    fun isMissing(state: JSONObject): Boolean {
        val base = state.optJSONObject("cars")?.optJSONObject("base")
        if (base != null && base.length() > 0) return false
        val failure = state.optJSONObject("pender")?.optJSONObject("failure") ?: return false
        return failure.optBoolean(GET_BASE, false)
    }

    /**
     * Reads the collector's base-API probe result (`{"status":<http status>}` or
     * `{"error":"..."}` for a network error / timeout). Only a 404 confirms the listing is gone;
     * 5xx, other statuses, network errors and unreadable results are transient, so the caller
     * reports a retryable read failure instead.
     */
    fun isConfirmedMissing(probeResult: String?): Boolean {
        if (probeResult == null) return false
        val obj = try {
            JSONObject(probeResult)
        } catch (e: JSONException) {
            return false
        }
        return obj.optInt("status", -1) == HTTP_NOT_FOUND
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
    private const val HTTP_NOT_FOUND = 404
}
