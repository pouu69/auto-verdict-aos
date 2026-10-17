package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvPalette
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Owned by AppRoot and handed down via [LocalAvToast] (spec 6). Toasts run in the root [scope],
 * so they survive the caller leaving composition; a new toast replaces the current one.
 */
@Stable
class AvToastState(private val scope: CoroutineScope) {
    internal val host = SnackbarHostState()

    /** [long] keeps the toast up longer, e.g. for an undo action. */
    fun show(
        message: String,
        actionLabel: String? = null,
        long: Boolean = false,
        onAction: (() -> Unit)? = null,
    ) {
        val duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(message = message, actionLabel = actionLabel, duration = duration)
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalAvToast = staticCompositionLocalOf<AvToastState> {
    error("AvToastState not provided — wrap content in AppRoot")
}

@Composable
fun rememberAvToastState(): AvToastState {
    val scope = rememberCoroutineScope()
    return remember(scope) { AvToastState(scope) }
}

@Composable
fun AvToastHost(state: AvToastState, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    SnackbarHost(hostState = state.host, modifier = modifier) { data ->
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(AvTheme.shapes.buttonM)
                .background(colors.toastBg)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.visuals.message,
                style = typography.body2,
                color = AvPalette.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
            )
            val action = data.visuals.actionLabel
            if (action != null) {
                Box(
                    modifier = Modifier
                        .avPressable(onClick = { data.performAction() })
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                ) {
                    Text(text = action, style = typography.buttonM, color = colors.toastAction)
                }
            }
        }
    }
}
