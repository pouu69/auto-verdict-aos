package com.car.autoverdict.saved

import com.car.autoverdict.db.SavedCarDao
import com.car.autoverdict.db.SavedCarEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

/**
 * App-scoped access to saved cars, grouped by vehicle identity (spec 3.4–3.5). The DB is never
 * merged or cleaned here; rows are only removed when the user deletes an item.
 */
class SavedCarsRepository(
    private val dao: SavedCarDao,
    private val scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /** carId → (updatedAt, identity); replaced wholesale per emission so deleted rows drop out. */
    @Volatile
    private var identityMemo: Map<String, Pair<Long, String>> = emptyMap()

    /**
     * Grouped saved cars, in the DAO's savedAt-descending order of each group's newest-saved row.
     * Shared so the tab count and the list read the table (and parse rawJson) once between them.
     */
    val items: Flow<List<SavedCarItem>> = dao.getAllFlow()
        .map { rows ->
            val identities = memoizedIdentities(rows)
            groupSavedCars(rows) { identities.getValue(it.carId) }
        }
        .flowOn(dispatcher)
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    /** Number of items (not rows) for the tab bar. */
    val itemCount: Flow<Int> = items.map { it.size }.distinctUntilChanged()

    /** Deletes every row of [item] and returns them as stored, for [restore]. */
    suspend fun delete(item: SavedCarItem): List<SavedCarEntity> = dao.deleteAndReturn(item.carIds)

    /** Writes deleted rows back unchanged (savedAt/updatedAt included), so they return to the same place. */
    suspend fun restore(rows: List<SavedCarEntity>) = dao.upsertAll(rows)

    /**
     * [restore] in the app scope, for an undo toast: the toast outlives the list screen, so the
     * write must not be cancelled with it (spec 4.6). [onFailure] runs off the main thread.
     */
    fun restoreDetached(rows: List<SavedCarEntity>, onFailure: () -> Unit) {
        scope.launch {
            try {
                restore(rows)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                // SQLite and closed-database errors are runtime exceptions.
                onFailure()
            }
        }
    }

    /** The given rows as they change, for the compare screen. Missing ids are simply absent. */
    fun observe(carIds: List<String>): Flow<List<SavedCarEntity>> = dao.getByCarIdsFlow(carIds)

    /** carId → identity, parsing rawJson only for rows that are new or re-evaluated since the last emission. */
    private fun memoizedIdentities(rows: List<SavedCarEntity>): Map<String, String> {
        val previous = identityMemo
        val next = HashMap<String, Pair<Long, String>>(rows.size)
        for (row in rows) {
            val cached = previous[row.carId]?.takeIf { it.first == row.updatedAt }?.second
            next[row.carId] = row.updatedAt to (cached ?: savedIdentity(row))
        }
        identityMemo = next
        return next.mapValues { it.value.second }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
