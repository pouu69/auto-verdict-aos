package com.car.autoverdict.ui.screen

import android.content.Context
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.theme.AvTheme

/** Notices for assets added in sub-project 0 (spec 5). Full OSS list is sub-project 4. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val oflText = remember { readRaw(context, R.raw.license_pretendard_ofl) }
    val apacheText = remember { readRaw(context, R.raw.license_apache_2_0) }

    Column(modifier = Modifier.fillMaxSize().background(AvTheme.colors.background)) {
        AvTopBar(onBack = onBack, title = stringResource(R.string.licenses_title))
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { LicenseSection(title = "Pretendard", notice = stringResource(R.string.license_pretendard_notice), fullText = oflText) }
            item { AvSectionDivider() }
            item { LicenseSection(title = "Material Symbols", notice = stringResource(R.string.license_material_notice), fullText = apacheText) }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun LicenseSection(
    title: String,
    notice: String,
    fullText: String,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text(
            text = title,
            style = typography.body1,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(text = notice, style = typography.body2, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        AvButton(
            text = stringResource(if (expanded) R.string.license_hide_full else R.string.license_show_full),
            onClick = { expanded = !expanded },
            style = AvButtonStyle.Secondary,
            size = AvButtonSize.Small,
        )
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Text(text = fullText, style = typography.caption1, color = colors.textSecondary)
        }
    }
}

private fun readRaw(context: Context, resId: Int): String =
    context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
