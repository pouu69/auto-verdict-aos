package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.car.autoverdict.ui.components.AvCollapsibleBody
import com.car.autoverdict.ui.components.AvSectionHeader

/**
 * A titled report section: header, then [body]. With [onToggleExpanded] the title folds the
 * body away; without it the section always shows (previews, tests).
 */
@Composable
internal fun ResultSection(
    title: String,
    expanded: Boolean,
    onToggleExpanded: (() -> Unit)?,
    modifier: Modifier = Modifier,
    count: Int? = null,
    body: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AvSectionHeader(title = title, count = count, expanded = expanded, onToggle = onToggleExpanded)
        AvCollapsibleBody(expanded = expanded || onToggleExpanded == null) { body() }
    }
}
