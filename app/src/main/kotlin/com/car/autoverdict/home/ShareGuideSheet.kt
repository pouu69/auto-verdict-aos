package com.car.autoverdict.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvBottomSheet
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.BrowserShareIllustration
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.ShareSheetIllustration
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.util.EncarUrl
import kotlinx.coroutines.launch

/**
 * How to send a listing here from the Encar app or a browser (spec 3.3), with a button that
 * opens Encar. The button animates the sheet out first, then calls [onDismiss] and
 * [onOpenEncar] once (repeat taps while it closes are ignored).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareGuideSheet(onDismiss: () -> Unit, onOpenEncar: () -> Unit) {
    val colors = AvTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var leaving by remember { mutableStateOf(false) }
    AvBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.guide_title), sheetState = sheetState) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            GuideGroupLabel(stringResource(R.string.guide_from_app))
            GuideStep(number = 1, text = stringResource(R.string.guide_app_step_share))
            GuideStep(number = 2, text = stringResource(R.string.guide_step_pick_app))

            Spacer(Modifier.height(16.dp))
            GuideGroupLabel(stringResource(R.string.guide_from_browser))
            GuideStep(number = 1, text = stringResource(R.string.guide_browser_step_share)) {
                BrowserShareIllustration()
            }
            GuideStep(number = 2, text = stringResource(R.string.guide_step_pick_app)) {
                ShareSheetIllustration()
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.guide_tip_more),
                style = AvTheme.typography.caption1,
                color = colors.textTertiary,
            )
            Spacer(Modifier.height(20.dp))
            AvButton(
                text = stringResource(R.string.guide_open_encar),
                onClick = {
                    if (!leaving) {
                        leaving = true
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            onOpenEncar()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Opens Encar's home page; the Encar app takes it when installed and handling the link,
 * otherwise a browser does. Shows a toast when nothing can open it.
 */
@Composable
fun rememberOpenEncar(): () -> Unit {
    val context = LocalContext.current
    val toast = LocalAvToast.current
    return remember(context, toast) {
        {
            if (!context.openEncarHome()) toast.show(context.getString(R.string.toast_open_encar_failed))
        }
    }
}

private fun Context.openEncarHome(): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EncarUrl.HOME_URL)))
    true
} catch (e: ActivityNotFoundException) {
    Log.w("ShareGuideSheet", "no activity to open Encar", e)
    false
}

@Composable
private fun GuideGroupLabel(text: String) {
    Text(
        text = text,
        style = AvTheme.typography.buttonM,
        color = AvTheme.colors.textSecondary,
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

/** Numbered step; the optional [content] (an illustration) sits under the text, aligned with it. */
@Composable
private fun GuideStep(number: Int, text: String, content: (@Composable () -> Unit)? = null) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(STEP_GAP),
        ) {
            Box(
                modifier = Modifier
                    .size(STEP_BADGE)
                    .clip(CircleShape)
                    .background(colors.accentWeak),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = number.toString(), style = typography.caption2, color = colors.accentText)
            }
            Text(text = text, style = typography.body2, color = colors.textPrimary, modifier = Modifier.weight(1f))
        }
        if (content != null) {
            Box(modifier = Modifier.padding(start = STEP_BADGE + STEP_GAP, top = 8.dp)) { content() }
        }
    }
}

private val STEP_BADGE = 24.dp
private val STEP_GAP = 12.dp
