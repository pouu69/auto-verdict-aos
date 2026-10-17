package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.DetailBlock
import com.car.autoverdict.report.DetailRow
import com.car.autoverdict.report.DetailSheet
import com.car.autoverdict.report.RuleItem
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.tone

/**
 * Spec 4.1 detail sheet body: title (with the rule's badge for rules), the result sentence,
 * why it matters, every source-data block, then the seller question with a copy button.
 * Scrolls by itself, so long inspections fit inside the bottom sheet.
 */
@Composable
fun DetailSheetContent(
    sheet: DetailSheet,
    modifier: Modifier = Modifier,
    rule: RuleItem? = null,
    onCopyQuestion: ((String) -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (rule != null) {
                AvBadge(text = rule.label, tone = rule.tone.toAvTone())
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = sheet.title,
                style = typography.title3,
                color = colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
        }
        // Rules lead with their result sentence; a subtitle that only repeats it is dropped.
        val message = rule?.message
        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = message, style = typography.body1, color = colors.textPrimary)
        }
        if (sheet.subtitle != null && sheet.subtitle != message) {
            Spacer(Modifier.height(if (message != null) 4.dp else 8.dp))
            Text(text = sheet.subtitle, style = typography.body2, color = colors.textSecondary)
        }
        if (rule?.why != null) {
            Spacer(Modifier.height(20.dp))
            BlockTitle(stringResource(R.string.result_issue_why))
            Spacer(Modifier.height(4.dp))
            Text(text = rule.why, style = typography.body2, color = colors.textSecondary)
        }
        sheet.blocks.forEach { block ->
            Spacer(Modifier.height(20.dp))
            DetailBlockContent(block)
        }
        val question = rule?.sellerQuestion
        if (question != null && onCopyQuestion != null) {
            Spacer(Modifier.height(20.dp))
            BlockTitle(stringResource(R.string.result_detail_seller_question))
            Spacer(Modifier.height(4.dp))
            Text(text = question, style = typography.body2, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            AvButton(
                text = stringResource(R.string.result_detail_copy_question),
                onClick = { onCopyQuestion(question) },
                style = AvButtonStyle.Secondary,
                size = AvButtonSize.Small,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BlockTitle(text: String) {
    Text(
        text = text,
        style = AvTheme.typography.body2.copy(fontWeight = FontWeight.SemiBold),
        color = AvTheme.colors.textPrimary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun DetailBlockContent(block: DetailBlock) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (block.title != null) {
            BlockTitle(block.title)
            Spacer(Modifier.height(4.dp))
        }
        block.rows.forEach { DetailRowContent(it) }
        if (block.note != null) {
            Text(
                text = block.note,
                style = AvTheme.typography.caption1,
                color = AvTheme.colors.textTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Label left, value right. Both columns wrap, so long values and large fonts grow the row
 * downwards instead of truncating; screen readers hear "label value" as one item.
 */
@Composable
private fun DetailRowContent(row: DetailRow) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = row.label,
            style = typography.body2,
            color = colors.textSecondary,
            modifier = Modifier.weight(0.42f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = row.value,
            style = typography.body2,
            color = row.tone?.let { colors.tone(it.toAvTone()).text } ?: colors.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}

/** Trailing `>` on rows that open a detail sheet. */
@Composable
internal fun DetailChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = AvTheme.colors.textTertiary,
        modifier = modifier.size(20.dp),
    )
}

/** TalkBack verb for rows that open a detail sheet ("자세히 보기"). */
@Composable
internal fun detailClickLabel(): String = stringResource(R.string.result_detail_click_label)
