package com.car.autoverdict.result.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.RuleItem
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.theme.AvTheme

/** Spec 5.2-7: every non-passing rule with its result, why it matters and what to ask. */
@Composable
fun IssuesSection(issues: List<RuleItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        AvSectionHeader(title = stringResource(R.string.result_section_issues), count = issues.size)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AvTheme.spacing.screenHorizontal)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            issues.forEach { IssueCard(it) }
        }
    }
}

@Composable
private fun IssueCard(issue: RuleItem) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AvTheme.shapes.tile)
            .background(colors.surfaceSubtle)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvBadge(text = issue.label, tone = issue.tone.toAvTone())
            Spacer(Modifier.width(8.dp))
            Text(
                text = issue.title,
                style = typography.body1.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(text = issue.message, style = typography.body2, color = colors.textPrimary)
        if (issue.why != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.result_issue_why),
                style = typography.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = issue.why, style = typography.body2, color = colors.textSecondary)
        }
        if (issue.sellerQuestion != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.result_issue_seller_question, issue.sellerQuestion),
                style = typography.body2,
                color = colors.textSecondary,
            )
        }
    }
}
