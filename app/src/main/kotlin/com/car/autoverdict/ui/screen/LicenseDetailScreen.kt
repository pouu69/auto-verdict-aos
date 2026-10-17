package com.car.autoverdict.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.car.autoverdict.R
import com.car.autoverdict.licenses.OssCatalog
import com.car.autoverdict.licenses.OssDetail
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.theme.AvTheme

/**
 * One notice in full (spec 7.4). [id] comes from [OssCatalog.libraryDetailId] or
 * [OssCatalog.googleDetailId]; an id the data no longer has (e.g. restored after an update)
 * shows the load-failed state and the top bar's back.
 */
@Composable
fun LicenseDetailScreen(id: String, onBack: () -> Unit) {
    val state by rememberOssCatalogState()
    val detail = (state as? OssCatalogState.Ready)?.catalog?.detail(id)
    Column(modifier = Modifier.fillMaxSize().background(AvTheme.colors.background)) {
        AvTopBar(onBack = onBack, title = detail?.title ?: stringResource(R.string.licenses_title))
        when {
            state == OssCatalogState.Loading -> Unit
            detail == null -> LicensesLoadFailed()
            else -> LicenseDetailBody(detail, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LicenseDetailBody(detail: OssDetail, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val spacing = AvTheme.spacing
    val context = LocalContext.current
    // Up to ~110KB: one Text per paragraph lets the list render only what is on screen.
    val paragraphs = remember(detail.text) { detail.text?.let(OssCatalog::paragraphs).orEmpty() }
    SelectionContainer(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xl),
                ) {
                    if (detail.licenseName != null) {
                        Text(text = detail.licenseName, style = typography.title3, color = colors.textPrimary)
                    }
                    if (detail.isGoogleBundled) {
                        Text(
                            text = stringResource(R.string.licenses_section_google_bundled),
                            style = typography.caption1,
                            color = colors.textTertiary,
                        )
                    }
                    if (detail.artifacts.isNotEmpty()) {
                        Spacer(Modifier.height(spacing.sm))
                        detail.artifacts.forEach { artifact ->
                            Text(text = artifact, style = typography.caption1, color = colors.textTertiary)
                        }
                    }
                    // Linked-only terms (Google SDK license) have no text to show here.
                    if (detail.url != null && detail.text == null) {
                        Spacer(Modifier.height(spacing.lg))
                        AvButton(
                            text = stringResource(R.string.licenses_open_web),
                            onClick = { context.openInBrowser(detail.url) },
                            style = AvButtonStyle.Weak,
                            size = AvButtonSize.Small,
                        )
                    }
                }
            }
            if (paragraphs.isNotEmpty()) {
                item { AvSectionDivider() }
                itemsIndexed(paragraphs) { index, paragraph ->
                    Text(
                        text = paragraph,
                        style = typography.caption1,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = spacing.screenHorizontal,
                                end = spacing.screenHorizontal,
                                top = if (index == 0) spacing.xl else spacing.md,
                            ),
                    )
                }
                item { Spacer(Modifier.height(spacing.xl)) }
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

private fun Context.openInBrowser(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser installed; nothing sensible to offer instead.
        Log.w("LicenseDetailScreen", "no activity for $url", e)
    }
}
