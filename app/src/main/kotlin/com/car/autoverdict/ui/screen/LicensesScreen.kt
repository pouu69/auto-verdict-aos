package com.car.autoverdict.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.R
import com.car.autoverdict.licenses.OssCatalog
import com.car.autoverdict.licenses.OssLicenseSection
import com.car.autoverdict.ui.components.AvCollapsibleBody
import com.car.autoverdict.ui.components.AvEmptyState
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.CancellationException

/**
 * Open-source notices grouped by license (spec 7.4). License and library names are proper nouns
 * and stay in English on this screen. Rows open [LicenseDetailScreen] via [onOpenDetail].
 */
@Composable
fun LicensesScreen(onBack: () -> Unit, onOpenDetail: (String) -> Unit) {
    val state by rememberOssCatalogState()
    Column(modifier = Modifier.fillMaxSize().background(AvTheme.colors.background)) {
        AvTopBar(onBack = onBack, title = stringResource(R.string.licenses_title))
        when (val current = state) {
            // Parsing takes tens of ms; a blank frame reads better than a flashing spinner.
            OssCatalogState.Loading -> Unit
            OssCatalogState.Failed -> LicensesLoadFailed()
            is OssCatalogState.Ready -> LicensesList(current.catalog, onOpenDetail, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LicensesList(catalog: OssCatalog, onOpenDetail: (String) -> Unit, modifier: Modifier = Modifier) {
    // 71 rows: folded by default so the app's own dependencies stay in view.
    var googleExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = stringResource(R.string.licenses_intro),
                style = AvTheme.typography.body2,
                color = AvTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = AvTheme.spacing.screenHorizontal, vertical = AvTheme.spacing.xl),
            )
        }
        catalog.sections.forEach { section ->
            item(key = "header:${section.license.id}") {
                AvSectionHeader(title = sectionTitle(section), count = section.libraries.size)
            }
            items(section.libraries, key = { "lib:${it.id}" }) { library ->
                val trailing = if (OssCatalog.isFont(library)) {
                    stringResource(R.string.licenses_font)
                } else {
                    stringResource(R.string.licenses_module_count, library.artifacts.size)
                }
                AvListRow(
                    title = library.name,
                    onClick = { onOpenDetail(OssCatalog.libraryDetailId(library)) },
                    trailing = { ValueWithChevron(trailing) },
                )
            }
            item(key = "divider:${section.license.id}") { AvSectionDivider() }
        }
        if (catalog.googleComponents.isNotEmpty()) {
            item(key = "google-bundled") {
                Column {
                    AvSectionHeader(
                        title = stringResource(R.string.licenses_section_google_bundled),
                        count = catalog.googleComponents.size,
                        expanded = googleExpanded,
                        onToggle = { googleExpanded = !googleExpanded },
                    )
                    AvCollapsibleBody(expanded = googleExpanded) {
                        Column {
                            catalog.googleComponents.forEach { component ->
                                AvListRow(
                                    title = component.name,
                                    onClick = { onOpenDetail(OssCatalog.googleDetailId(component)) },
                                    trailing = { Chevron() },
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun sectionTitle(section: OssLicenseSection): String =
    if (section.isGoogleTerms) stringResource(R.string.licenses_section_google_terms) else section.license.name

/** Only reachable when a bundled resource is broken, so there is nothing to retry. */
@Composable
internal fun LicensesLoadFailed() {
    AvEmptyState(icon = Icons.Rounded.ErrorOutline, title = stringResource(R.string.licenses_load_failed))
}

@Composable
private fun ValueWithChevron(value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AvTheme.spacing.xs),
    ) {
        Text(text = value, style = AvTheme.typography.body2, color = AvTheme.colors.textTertiary)
        Chevron()
    }
}

/** Decorative: the row itself is the button. */
@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = AvTheme.colors.textTertiary,
        modifier = Modifier.size(20.dp),
    )
}

internal sealed interface OssCatalogState {
    data object Loading : OssCatalogState
    data class Ready(val catalog: OssCatalog) : OssCatalogState
    data object Failed : OssCatalogState
}

/** Shared by the list and detail screens; the app-scoped repository parses the JSON only once. */
@Composable
internal fun rememberOssCatalogState(): State<OssCatalogState> {
    val context = LocalContext.current
    val repository = remember(context) { (context.applicationContext as AutoVerdictApp).ossNoticeRepository }
    return produceState<OssCatalogState>(OssCatalogState.Loading, repository) {
        value = try {
            OssCatalogState.Ready(repository.load())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("LicensesScreen", "failed to load OSS notices", e)
            OssCatalogState.Failed
        }
    }
}
