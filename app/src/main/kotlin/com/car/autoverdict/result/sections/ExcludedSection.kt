package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.ExcludedItem
import com.car.autoverdict.ui.theme.AvTheme

/** Spec 5.2-10: rules the engine skipped, with the reason. Callers skip it when empty. */
@Composable
fun ExcludedSection(
    items: List<ExcludedItem>,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    ResultSection(
        title = stringResource(R.string.result_section_excluded),
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AvTheme.spacing.screenHorizontal)
                .padding(top = 4.dp, bottom = 20.dp),
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = item.title,
                        style = typography.body2.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                    )
                    Text(text = item.reason, style = typography.body2, color = colors.textSecondary)
                }
            }
        }
    }
}
