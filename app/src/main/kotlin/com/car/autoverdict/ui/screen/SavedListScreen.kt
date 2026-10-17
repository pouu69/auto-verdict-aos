package com.car.autoverdict.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.preferences.AppPreferences
import com.car.autoverdict.saved.SavedCarItem
import com.car.autoverdict.saved.SavedCarsRepository
import com.car.autoverdict.saved.SavedSort
import com.car.autoverdict.saved.sortedForSaved
import com.car.autoverdict.saved.toDisplay
import com.car.autoverdict.ui.components.AvBottomCta
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvEmptyState
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.saved.SavedCarRow
import com.car.autoverdict.ui.saved.SavedSortSheet
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Most cars one comparison takes (spec 3 2). */
private const val MAX_COMPARE = 3
private const val MIN_COMPARE = 2

/**
 * Saved tab wiring (spec 3 4): grouped items from the app-scoped [repository], sort kept in
 * [preferences], delete with an undo toast that outlives this screen (spec 4.6).
 */
@Composable
fun SavedListRoute(
    repository: SavedCarsRepository,
    preferences: AppPreferences,
    onOpenResult: (url: String) -> Unit,
    onCompare: (carIds: List<String>) -> Unit,
    onGoHome: () -> Unit,
) {
    // Null until the first emission, so the empty state doesn't flash while loading (spec 4.7).
    val items by repository.items.collectAsState(initial = null)
    var sort by remember { mutableStateOf(preferences.savedSort) }
    val toast = LocalAvToast.current
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.saved_toast_deleted)
    val undoLabel = stringResource(R.string.saved_toast_undo)
    val deleteFailedMessage = stringResource(R.string.saved_toast_delete_failed)
    val restoreFailedMessage = stringResource(R.string.saved_toast_restore_failed)

    SavedListScreen(
        items = items,
        sort = sort,
        onSortChange = {
            sort = it
            preferences.savedSort = it
        },
        onOpenResult = { onOpenResult(it.representative.url) },
        onDelete = { item ->
            scope.launch {
                val rows = try {
                    repository.delete(item)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RuntimeException) {
                    // SQLite and closed-database errors are runtime exceptions.
                    toast.show(deleteFailedMessage)
                    return@launch
                }
                // A repeated delete of the same item finds nothing; keep the first toast's undo.
                if (rows.isEmpty()) return@launch
                toast.show(deletedMessage, actionLabel = undoLabel, long = true) {
                    repository.restoreDetached(rows) { toast.show(restoreFailedMessage) }
                }
            }
        },
        onCompare = onCompare,
        onGoHome = onGoHome,
    )
}

/**
 * Stateless saved list. [items] null means not loaded yet (draws nothing). Selection mode is
 * local UI state: "비교" enters it, the top-bar "취소" or system back leaves it.
 * [onCompare] gets the representatives' carIds in on-screen order.
 */
@Composable
fun SavedListScreen(
    items: List<SavedCarItem>?,
    sort: SavedSort,
    onSortChange: (SavedSort) -> Unit,
    onOpenResult: (SavedCarItem) -> Unit,
    onDelete: (SavedCarItem) -> Unit,
    onCompare: (carIds: List<String>) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val toast = LocalAvToast.current
    // Fixed per composition: the default argument would change on every recomposition.
    val now = remember { nowMillis }
    val maxMessage = stringResource(R.string.saved_select_max)

    var selecting by rememberSaveable { mutableStateOf(false) }
    // Identities, so a re-evaluated representative stays selected.
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var sortSheetOpen by rememberSaveable { mutableStateOf(false) }

    val sorted = remember(items, sort) { items?.sortedForSaved(sort) { it.representative }.orEmpty() }
    val canCompare = sorted.size >= MIN_COMPARE

    // Drop selections deleted elsewhere (e.g. unsaved from the result screen), and leave the mode
    // once there is nothing left to compare.
    LaunchedEffect(sorted) {
        if (items == null) return@LaunchedEffect
        val identities = sorted.mapTo(HashSet()) { it.identity }
        selected = selected.filter { it in identities }
        if (!canCompare) selecting = false
    }

    fun exitSelection() {
        selecting = false
        selected = emptyList()
    }

    // Registered after AppRoot's, so it wins while selecting (spec 4.5).
    BackHandler(enabled = selecting) { exitSelection() }

    Column(modifier = modifier.fillMaxSize()) {
        AvTopBar(
            onBack = null,
            title = stringResource(if (selecting) R.string.saved_select_title else R.string.saved_title),
            actions = {
                when {
                    selecting -> TopBarTextButton(stringResource(R.string.saved_select_cancel), ::exitSelection)
                    canCompare -> TopBarTextButton(stringResource(R.string.saved_action_compare)) { selecting = true }
                }
            },
        )
        when {
            items == null -> Unit
            sorted.isEmpty() -> EmptyBody(onGoHome = onGoHome, modifier = Modifier.weight(1f))
            else -> {
                ListHeader(
                    count = if (selecting) selected.size else sorted.size,
                    selecting = selecting,
                    sort = sort,
                    onOpenSort = { sortSheetOpen = true },
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(sorted, key = { it.identity }) { item ->
                        val display = remember(item.representative) { item.representative.toDisplay(now) }
                        val isSelected = item.identity in selected
                        SavedCarRow(
                            display = display,
                            selecting = selecting,
                            selected = isSelected,
                            onClick = {
                                when {
                                    !selecting -> onOpenResult(item)
                                    isSelected -> selected = selected - item.identity
                                    selected.size >= MAX_COMPARE -> toast.show(maxMessage)
                                    else -> selected = selected + item.identity
                                }
                            },
                            onDelete = { onDelete(item) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (selecting) {
                    val count = selected.size
                    // Tab root: the tab bar below owns the navigation-bar inset (spec 0 6).
                    AvBottomCta(windowInsets = WindowInsets(0)) {
                        AvButton(
                            text = if (count >= MIN_COMPARE) {
                                stringResource(R.string.saved_select_cta, count)
                            } else {
                                stringResource(R.string.saved_select_cta_min)
                            },
                            onClick = {
                                val ids = sorted.filter { it.identity in selected }.map { it.representative.carId }
                                exitSelection()
                                onCompare(ids)
                            },
                            enabled = count >= MIN_COMPARE,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (sortSheetOpen) {
        SavedSortSheet(
            current = sort,
            onSelect = onSortChange,
            onDismiss = { sortSheetOpen = false },
        )
    }
}

/** "5대 · 최근 저장순 ⌄", or "2대 선택" while selecting (spec 4.1, 4.5). */
@Composable
private fun ListHeader(count: Int, selecting: Boolean, sort: SavedSort, onOpenSort: () -> Unit) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(if (selecting) R.string.saved_select_count else R.string.saved_total, count),
            style = typography.caption1,
            color = colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        if (!selecting) {
            val sortLabel = stringResource(sort.label)
            val description = "${stringResource(R.string.saved_sort_title)}, $sortLabel"
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .avPressable(onClick = onOpenSort)
                    .semantics { contentDescription = description }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = sortLabel, style = typography.body2, color = colors.textSecondary)
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Top-bar text action (buttonM, accentText) with a 48dp touch target (spec 4.5, 8). */
@Composable
private fun TopBarTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .avPressable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AvTheme.typography.buttonM, color = AvTheme.colors.accentText)
    }
}

@Composable
private fun EmptyBody(onGoHome: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        AvEmptyState(
            icon = Icons.Rounded.BookmarkBorder,
            title = stringResource(R.string.saved_empty_title),
            description = stringResource(R.string.saved_empty_desc),
            actionLabel = stringResource(R.string.saved_empty_action),
            onAction = onGoHome,
        )
    }
}
