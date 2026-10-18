package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvPalette
import com.car.autoverdict.ui.theme.AvTheme
import kotlin.math.roundToInt

/**
 * Modal sheet with a 24dp top radius and a drag handle. It closes by dragging its non-scrolling
 * top (drag handle, title), tapping the scrim or pressing back.
 *
 * To close from inside the sheet (e.g. a button), animate it out first:
 * `scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }`.
 * Content is not scrollable by default; add `verticalScroll` for long content. Scrolling that
 * content never moves or closes the sheet: what is left of a drag or fling once the content
 * hits its top stays in the content instead of reaching the sheet (see [KeepScrollInContent]).
 *
 * The sheet never reaches under the status bar: at its tallest it stops at the bottom of the
 * status bar, so the drag handle stays clear of the clock and icons.
 *
 * [maxHeightFraction] (0..1) caps the whole sheet (drag handle and navigation-bar inset
 * included) at that share of the screen height; shorter content still wraps. The capped content
 * must scroll by itself. Null keeps the default, where the sheet grows up to the status bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    maxHeightFraction: Float? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    require(maxHeightFraction == null || maxHeightFraction in 0f..1f) {
        "maxHeightFraction must be within 0..1, was $maxHeightFraction"
    }
    val colors = AvTheme.colors
    val handleDescription = stringResource(R.string.cd_sheet_handle)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // The sheet's dialog is edge to edge and Material only pads the bottom inset, so a tall
        // sheet would otherwise slide under the status bar. Padding outside the sheet lowers
        // its ceiling without adding space above the drag handle.
        modifier = Modifier.statusBarsPadding(),
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
        // Read here, inside the sheet's own dialog window, rather than in the caller's window.
        val statusBars = WindowInsets.statusBars
        val cap = if (maxHeightFraction == null) {
            Modifier
        } else {
            Modifier.capSheetHeight(maxHeightFraction, screenHeightDp, statusBars)
        }
        Column(
            modifier = cap
                .nestedScroll(KeepScrollInContent)
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

/**
 * Swallows whatever scroll and fling the sheet content's scrollables leave over, so none of it
 * reaches [ModalBottomSheet]'s own nested-scroll connection. That connection would otherwise
 * turn a downward drag or fling past the content's top into a dismiss drag. Pre-scroll is left
 * alone, so the sheet still gets first say there (it only acts while it is off its anchor).
 * Drags on the drag handle and title do not go through nested scroll and still move the sheet.
 */
private object KeepScrollInContent : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

/**
 * Caps this sheet body so the whole sheet stays within [fraction] of the screen height.
 * The body may grow to the full screen minus the [topInsets] the sheet is kept below and what
 * the sheet adds around it (bottom inset, drag handle); the latter is taken off the target
 * height too.
 *
 * The screen height is `Configuration.screenHeightDp`, which includes the system bars from
 * API 35 but leaves them out up to API 34, where the cap lands slightly off half the window.
 * Robolectric has no system bars of its own (zero insets unless a test dispatches some) and its
 * screen height always equals the window, so the tests do not cover that deviation.
 */
private fun Modifier.capSheetHeight(
    fraction: Float,
    screenHeightDp: Int,
    topInsets: WindowInsets,
): Modifier = layout { measurable, constraints ->
    val screenHeight = screenHeightDp.dp.toPx()
    val maxHeight = if (constraints.hasBoundedHeight) {
        val chrome = (screenHeight - topInsets.getTop(this) - constraints.maxHeight).coerceAtLeast(0f)
        (screenHeight * fraction - chrome).roundToInt().coerceIn(0, constraints.maxHeight)
    } else {
        (screenHeight * fraction).roundToInt()
    }
    val placeable = measurable.measure(
        constraints.copy(minHeight = constraints.minHeight.coerceAtMost(maxHeight), maxHeight = maxHeight),
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
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
