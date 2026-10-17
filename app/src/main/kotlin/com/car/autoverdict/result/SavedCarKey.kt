package com.car.autoverdict.result

import com.car.autoverdict.collector.ListingAvailability
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * The `saved_cars.carId` a car is saved under, so one car has one saved row.
 *
 * Encar can serve one vehicle under two ids: its original vehicle id and a dummy-ad id (see
 * [ListingAvailability.isGone]). Rule:
 *
 * - **New saves always use the original vehicle id.** For a dummy-ad payload (`cars.base.vehicleId`
 *   differs from `queryCarId`, with `manage.dummy` and `manage.dummyVehicleId == queryCarId`)
 *   that is `base.vehicleId`; for any other payload it is the payload's `carId` (the URL id).
 * - **Legacy rows may use a dummy-ad id** (saved before this rule). They are never migrated or
 *   deleted automatically; an existing row keeps being refreshed under its own id.
 *
 * So two saved rows are the same car when [of] returns the same key for their `rawJson`
 * (legacy rows hold the full collector payload too). The cache stays keyed by the URL id.
 */
object SavedCarKey {

    /** Key for the collector payload [rawJson]; [fallback] when it is not a readable payload. */
    fun of(rawJson: String, fallback: String): String {
        val payload = try {
            Json.parseToJsonElement(rawJson) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: return fallback
        val requested = idOf(payload["carId"]) ?: fallback
        val base = payload.obj("preloadedState")?.obj("cars")?.obj("base") ?: return requested
        val manage = base.obj("manage")
        return ListingAvailability.originalOfDummyAd(
            vehicleId = idOf(base["vehicleId"]),
            queryCarId = idOf(base["queryCarId"]),
            dummy = (manage?.get("dummy") as? JsonPrimitive)?.booleanOrNull ?: false,
            dummyVehicleId = idOf(manage?.get("dummyVehicleId")),
        ) ?: requested
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    // Same normalization as ListingAvailability: numbers and numeric strings compare by value.
    private fun idOf(element: JsonElement?): String? {
        val p = element as? JsonPrimitive ?: return null
        if (p.isString) return p.content.trim().takeIf { it.isNotEmpty() }
        return (p.longOrNull ?: p.doubleOrNull?.toLong())?.toString()
    }
}
