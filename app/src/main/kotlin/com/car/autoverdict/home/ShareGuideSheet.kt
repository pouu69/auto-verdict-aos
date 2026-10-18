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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvBottomSheet
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.BrowserMenuButtonIllustration
import com.car.autoverdict.ui.components.BrowserShareMenuIllustration
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.ShareSheetIllustration
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.util.EncarUrl
import kotlinx.coroutines.launch

/**
 * How to send a listing here from Encar's mobile web (spec 3.3): the browser's overflow menu,
 * its share item, then AutoVerdict in the system share sheet, each step drawn like that screen.
 * Only the browser flow is taught; it is the one that hands over a listing link we can read.
 * The button animates the sheet out first, then calls [onDismiss] and [onOpenEncar] once
 * (repeat taps while it closes are ignored).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareGuideSheet(onDismiss: () -> Unit, onOpenEncar: () -> Unit) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var leaving by remember { mutableStateOf(false) }
    AvBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.guide_title), sheetState = sheetState) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.guide_intro),
                style = typography.body2,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            GuideStep(number = 1, text = menuStepText(), inlineContent = menuIconInlineContent()) {
                BrowserMenuButtonIllustration()
            }
            GuideStep(number = 2, text = AnnotatedString(stringResource(R.string.guide_step_share))) {
                BrowserShareMenuIllustration()
            }
            GuideStep(number = 3, text = AnnotatedString(stringResource(R.string.guide_step_pick_app))) {
                ShareSheetIllustration()
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.guide_note_samsung),
                style = typography.caption1,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.guide_tip_more),
                style = typography.caption1,
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
 * Opens Encar's mobile web in a browser rather than the Encar app, since the guide teaches the
 * browser share flow. Shows a toast when nothing can open it.
 */
@Composable
fun rememberOpenEncar(): () -> Unit {
    val context = LocalContext.current
    val toast = LocalAvToast.current
    return remember(context, toast) {
        {
            if (!context.openEncarInBrowser()) toast.show(context.getString(R.string.toast_open_encar_failed))
        }
    }
}

/**
 * Tries a browser-only intent first, then a plain VIEW. False when neither resolves.
 *
 * The selector (MAIN + APP_BROWSER) picks the activity, so only browsers qualify and the Encar
 * app's link handling is skipped; the chosen browser then receives the VIEW intent with the URL.
 * This is the selector half of `Intent.makeMainSelectorActivity`: that helper itself returns a
 * MAIN + LAUNCHER intent (any launchable app) wrapping this selector, so it can't be the selector.
 */
internal fun Context.openEncarInBrowser(): Boolean {
    val uri = Uri.parse(EncarUrl.HOME_URL)
    val inBrowser = Intent(Intent.ACTION_VIEW, uri).apply {
        selector = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
    }
    return tryStart(inBrowser) || tryStart(Intent(Intent.ACTION_VIEW, uri))
}

private fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    Log.w("ShareGuideSheet", "no activity for $intent", e)
    false
}

/** Step 1 text with the overflow menu icon drawn inline where the string has `%1$s`. */
@Composable
private fun menuStepText(): AnnotatedString {
    val template = stringResource(R.string.guide_step_menu)
    // The icon's spoken name stands in for it, so screen readers still get a full sentence.
    val iconName = stringResource(R.string.guide_menu_icon_a11y)
    val parts = template.split(MENU_ICON_ARG, limit = 2)
    return buildAnnotatedString {
        append(parts[0])
        if (parts.size == 2) {
            appendInlineContent(MENU_ICON_ID, iconName)
            append(parts[1])
        }
    }
}

@Composable
private fun menuIconInlineContent(): Map<String, InlineTextContent> {
    val colors = AvTheme.colors
    return mapOf(
        MENU_ICON_ID to InlineTextContent(
            // em-sized so the icon follows the user's font scale along with the text.
            Placeholder(width = 1.3.em, height = 1.3.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colors.accentWeak),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.fillMaxSize(0.85f),
                )
            }
        },
    )
}

/** Numbered step; the optional [content] (an illustration) sits under the text, aligned with it. */
@Composable
private fun GuideStep(
    number: Int,
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    content: (@Composable () -> Unit)? = null,
) {
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
            Text(
                text = text,
                inlineContent = inlineContent,
                style = typography.body2,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        if (content != null) {
            Box(modifier = Modifier.padding(start = STEP_BADGE + STEP_GAP, top = 8.dp)) { content() }
        }
    }
}

private const val MENU_ICON_ARG = "%1\$s"
private const val MENU_ICON_ID = "menuIcon"
private val STEP_BADGE = 24.dp
private val STEP_GAP = 12.dp
