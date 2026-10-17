package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.theme.AvTheme

/** Two-tab bar. Unselected tabs use textTertiary (active, not disabled). Owns nav-bar inset. */
@Composable
fun AvTabBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    savedCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AvTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.background)) {
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .heightIn(min = 56.dp)
                .selectableGroup(),
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) colors.textPrimary else colors.textTertiary
                val label = when (tab) {
                    MainTab.Home -> stringResource(R.string.tab_home)
                    MainTab.Saved -> if (savedCount > 0) {
                        stringResource(R.string.tab_saved_count, savedCount)
                    } else {
                        stringResource(R.string.tab_saved)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = isSelected,
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (tab == MainTab.Home) Icons.Rounded.Home else Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(text = label, style = AvTheme.typography.caption2, color = tint)
                }
            }
        }
    }
}
