package com.car.autoverdict.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.theme.AvTheme

/** One summary section: a heading and a plain paragraph (spec 4). */
private data class PolicySection(@StringRes val title: Int, @StringRes val body: Int)

private val policySections = listOf(
    PolicySection(R.string.privacy_collected_title, R.string.privacy_collected_body),
    PolicySection(R.string.privacy_storage_title, R.string.privacy_storage_body),
    PolicySection(R.string.privacy_delete_title, R.string.privacy_delete_body),
    PolicySection(R.string.privacy_ads_title, R.string.privacy_ads_body),
    PolicySection(R.string.privacy_network_title, R.string.privacy_network_body),
    PolicySection(R.string.privacy_permissions_title, R.string.privacy_permissions_body),
    PolicySection(R.string.privacy_contact_title, R.string.privacy_contact_body),
)

/** In-app summary of the privacy policy; the full text lives on the web (spec 4). */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val toast = LocalAvToast.current
    val url = stringResource(R.string.privacy_full_policy_url)
    val noBrowser = stringResource(R.string.privacy_toast_no_browser)
    PrivacyPolicyContent(
        onBack = onBack,
        onOpenFullPolicy = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                toast.show(noBrowser)
            }
        },
    )
}

/** Stateless body, so tests can render it without a toast host or an activity. */
@Composable
internal fun PrivacyPolicyContent(onBack: () -> Unit, onOpenFullPolicy: () -> Unit) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val spacing = AvTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        AvTopBar(onBack = onBack, title = stringResource(R.string.privacy_title))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.padding(horizontal = spacing.screenHorizontal)) {
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = stringResource(R.string.privacy_headline),
                    style = typography.title2,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    text = stringResource(
                        R.string.privacy_updated_label,
                        stringResource(R.string.privacy_updated_at),
                    ),
                    style = typography.caption1,
                    color = colors.textTertiary,
                )
                policySections.forEach { section ->
                    Spacer(Modifier.height(spacing.xxl))
                    Text(
                        text = stringResource(section.title),
                        style = typography.title3,
                        color = colors.textPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        text = stringResource(section.body),
                        style = typography.body2,
                        color = colors.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(spacing.xxl))
            }
            AvSectionDivider()
            AvListRow(
                title = stringResource(R.string.privacy_full_policy),
                subtitle = stringResource(R.string.privacy_full_policy_subtitle),
                onClick = onOpenFullPolicy,
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}
