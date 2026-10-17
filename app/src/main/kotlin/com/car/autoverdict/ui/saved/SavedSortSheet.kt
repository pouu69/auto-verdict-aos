package com.car.autoverdict.ui.saved

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.saved.SavedSort
import com.car.autoverdict.ui.components.AvBottomSheet
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.launch

/** Sort options as radio rows (spec 3 4.4). Picking one hides the sheet, then reports it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSortSheet(
    current: SavedSort,
    onSelect: (SavedSort) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    AvBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.saved_sort_title), sheetState = sheetState) {
        SavedSort.entries.forEach { sort ->
            val isCurrent = sort == current
            AvListRow(
                title = stringResource(sort.label),
                onClick = {
                    onSelect(sort)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    selected = isCurrent
                },
                trailing = if (isCurrent) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = AvTheme.colors.accentText,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
