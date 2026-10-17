package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvPalette
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Modal sheet with a 24dp top radius, drag handle and drag-to-dismiss.
 *
 * To close from inside the sheet (e.g. a button), animate it out first:
 * `scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }`.
 * Content is not scrollable by default; add `verticalScroll` for long content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AvTheme.colors
    val handleDescription = stringResource(R.string.cd_sheet_handle)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AvTheme.shapes.sheetTop,
        containerColor = colors.background,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(AvTheme.shapes.chip)
                    .background(AvPalette.Grey300)
                    .semantics { contentDescription = handleDescription },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = AvTheme.typography.title3,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

/** Confirm dialog. `destructive` turns the confirm button into the Danger style. */
@Composable
fun AvDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = AvTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AvTheme.shapes.dialog,
        containerColor = colors.background,
        title = { Text(text = title, style = AvTheme.typography.title3, color = colors.textPrimary) },
        text = { Text(text = message, style = AvTheme.typography.body2, color = colors.textSecondary) },
        confirmButton = {
            AvButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (destructive) AvButtonStyle.Danger else AvButtonStyle.Primary,
                size = AvButtonSize.Medium,
            )
        },
        dismissButton = {
            AvButton(text = dismissLabel, onClick = onDismiss, style = AvButtonStyle.Secondary, size = AvButtonSize.Medium)
        },
    )
}
