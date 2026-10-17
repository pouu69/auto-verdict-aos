package com.car.autoverdict.result.sections

import android.os.Build
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
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
import kotlin.math.roundToInt

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

/** From this font scale on, a row's two columns get too narrow and stack instead. */
internal const val DETAIL_ROW_STACK_FONT_SCALE = 1.5f

/** The label column's share of a side-by-side row while the value needs the rest. */
private const val DETAIL_ROW_LABEL_MAX_FRACTION = 0.55f

/**
 * Korean breaks between any two syllables by default, splitting "400만원" or "1월" across
 * lines. Phrase breaking (API 33+) wraps at spaces instead; older platforms ignore it, so
 * [detailRowText] joins the words itself there. The platform only applies phrase breaking to
 * Korean or Japanese text, so the rows say they are Korean whatever the device locale.
 */
private val DetailRowLineBreak = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Strict,
    wordBreak = LineBreak.WordBreak.Phrase,
)

private val KoreanText = LocaleList("ko-KR")

private const val WORD_JOINER = '\u2060'

/**
 * [text] with a word joiner between every two characters of a word, so the line breaker only
 * wraps at whitespace. Words wider than the line still break, at any character. Display only:
 * the joiners are invisible and screen readers skip them.
 */
internal fun keepWordsTogether(text: String): String = buildString(text.length * 2) {
    text.forEachIndexed { i, c ->
        if (i > 0 && !c.isWhitespace() && !text[i - 1].isWhitespace()) append(WORD_JOINER)
        append(c)
    }
}

private fun detailRowText(text: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) text else keepWordsTogether(text)

/**
 * Label left, value right. The label keeps its own width up to [DETAIL_ROW_LABEL_MAX_FRACTION]
 * of the row (more when the value is short) and the value takes the rest, right-aligned. Both
 * wrap between words, so long values grow the row downwards instead of truncating; at
 * [DETAIL_ROW_STACK_FONT_SCALE] and above the label sits over the value.
 * Screen readers hear "label value" as one item either way.
 */
@Composable
private fun DetailRowContent(row: DetailRow) {
    val colors = AvTheme.colors
    val style = AvTheme.typography.body2.copy(lineBreak = DetailRowLineBreak, localeList = KoreanText)
    val valueColor = row.tone?.let { colors.tone(it.toAvTone()).text } ?: colors.textPrimary
    val label = detailRowText(row.label)
    val value = detailRowText(row.value)
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
        .semantics(mergeDescendants = true) {}
    if (LocalDensity.current.fontScale >= DETAIL_ROW_STACK_FONT_SCALE) {
        Column(modifier = modifier) {
            Text(text = label, style = style, color = colors.textSecondary)
            Text(text = value, style = style, color = valueColor)
        }
        return
    }
    Layout(
        modifier = modifier,
        content = {
            Text(text = label, style = style, color = colors.textSecondary)
            Text(text = value, style = style, color = valueColor, textAlign = TextAlign.End)
        },
    ) { measurables, constraints ->
        val (labelMeasurable, valueMeasurable) = measurables
        val gap = 12.dp.roundToPx()
        val width = constraints.maxWidth
        val available = (width - gap).coerceAtLeast(0)
        // A short value leaves the label more than its share, so "3개월" never squeezes a period.
        val labelCap = maxOf(
            (available * DETAIL_ROW_LABEL_MAX_FRACTION).roundToInt(),
            available - valueMeasurable.maxIntrinsicWidth(constraints.maxHeight),
        ).coerceIn(0, available)
        val labelWidth = labelMeasurable.maxIntrinsicWidth(constraints.maxHeight).coerceAtMost(labelCap)
        val labelPlaceable = labelMeasurable.measure(Constraints.fixedWidth(labelWidth))
        val valueWidth = (available - labelWidth).coerceAtLeast(0)
        val valuePlaceable = valueMeasurable.measure(Constraints.fixedWidth(valueWidth))
        val height = maxOf(labelPlaceable.height, valuePlaceable.height)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            labelPlaceable.place(0, 0)
            valuePlaceable.place(width - valueWidth, 0)
        }
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
