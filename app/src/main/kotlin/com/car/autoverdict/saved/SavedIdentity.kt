package com.car.autoverdict.saved

import com.car.autoverdict.db.SavedCarEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

/**
 * One saved car as the screens see it: every row sharing a [identity] collapsed into one (spec 3.4).
 * All displayed values come from [representative]; deleting the item deletes every row in [carIds].
 */
data class SavedCarItem(
    val identity: String,
    val representative: SavedCarEntity,
    /** Every grouped row's carId, representative first. */
    val carIds: List<String>,
)

/**
 * The listing's original vehicle id from a collector payload (`preloadedState.cars.base.vehicleId`),
 * or null when missing, non-positive or unreadable (spec 3.2). A dummy-ad row points at the original
 * car's id here, so it groups with a row saved from the original URL.
 */
fun vehicleIdOf(rawJson: String): String? {
    val payload = try {
        payloadJson.decodeFromString(RawPayload.serializer(), rawJson)
    } catch (e: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException: malformed JSON or unexpected shape.
        return null
    }
    val vehicleId = payload.preloadedState?.cars?.base?.vehicleId ?: return null
    // Accepts both a JSON number and a numeric string.
    return vehicleId.content.toLongOrNull()?.takeIf { it > 0 }?.toString()
}

/**
 * Grouping key of a saved row: its vehicle id, falling back to carId for old or damaged rows.
 *
 * Same rule as `result/SavedCarKey` on the save path (dummy ad → base.vehicleId, otherwise the
 * listing's own id, which equals vehicleId for regular listings); to be unified into
 * `SavedCarKey.of` after merge.
 */
fun savedIdentity(row: SavedCarEntity): String = vehicleIdOf(row.rawJson) ?: row.carId

/**
 * Collapses rows with the same identity into items, in order of each group's first row in [rows].
 * The representative is the latest [SavedCarEntity.updatedAt]; ties prefer carId == identity,
 * then the lexicographically first carId (spec 3.4).
 */
fun groupSavedCars(
    rows: List<SavedCarEntity>,
    identityOf: (SavedCarEntity) -> String = ::savedIdentity,
): List<SavedCarItem> =
    rows.groupBy(identityOf).map { (identity, group) ->
        val representative = group.minWith(
            compareByDescending<SavedCarEntity> { it.updatedAt }
                .thenBy { if (it.carId == identity) 0 else 1 }
                .thenBy { it.carId },
        )
        SavedCarItem(
            identity = identity,
            representative = representative,
            carIds = listOf(representative.carId) + group.map { it.carId }.filter { it != representative.carId },
        )
    }

private val payloadJson = Json { ignoreUnknownKeys = true }

// Skeleton of the collector payload: decoding into it skips every other field without building a tree.
@Serializable
private class RawPayload(val preloadedState: PreloadedState? = null)

@Serializable
private class PreloadedState(val cars: Cars? = null)

@Serializable
private class Cars(val base: Base? = null)

@Serializable
private class Base(val vehicleId: JsonPrimitive? = null)
