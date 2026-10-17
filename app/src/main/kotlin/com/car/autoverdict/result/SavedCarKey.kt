package com.car.autoverdict.result

import com.car.autoverdict.saved.savedIdentity
import com.car.autoverdict.saved.vehicleIdOf

/**
 * The `saved_cars.carId` a car is saved under, so one car has one saved row. Also the saved
 * list's grouping key ([savedIdentity] delegates here), so save and display share one rule.
 *
 * Encar can serve one vehicle under two ids: its original vehicle id and a dummy-ad id. The
 * collector payload's `cars.base.vehicleId` is always the original: for a regular listing it
 * equals the URL id, for a dummy ad it is the original vehicle's id. (A page whose vehicleId
 * names another car without a dummy link is reported Deleted and never saved.) Rule:
 *
 * - **New saves always use the original vehicle id**: `base.vehicleId`, or [of]'s fallback (the
 *   URL id) when the payload has no usable vehicleId.
 * - **Legacy rows may use a dummy-ad id** (saved before this rule). They are never migrated or
 *   deleted automatically; an existing row keeps being refreshed under its own id.
 *
 * The cache stays keyed by the URL id.
 */
object SavedCarKey {

    /** Key for the collector payload [rawJson]; [fallback] when it has no usable vehicleId. */
    fun of(rawJson: String, fallback: String): String = vehicleIdOf(rawJson) ?: fallback
}
