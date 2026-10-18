package com.car.autoverdict.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.car.autoverdict.R
import com.car.autoverdict.compare.CompareCell
import com.car.autoverdict.compare.CompareController
import com.car.autoverdict.compare.CompareHeader
import com.car.autoverdict.compare.CompareLabel
import com.car.autoverdict.compare.CompareRow
import com.car.autoverdict.compare.CompareSection
import com.car.autoverdict.compare.CompareTable
import com.car.autoverdict.compare.CompareUiState
import com.car.autoverdict.compare.CompareValue
import com.car.autoverdict.engine.ReportEvaluator
import com.car.autoverdict.saved.SavedCarsRepository
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvEmptyState
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.tone
import kotlinx.coroutines.delay

/** Gap between compared columns (spec 3 6.2). */
private val COLUMN_GAP = 12.dp

/** Inner padding of every cell, so a marked cell's tile doesn't shift its text. */
private val CELL_PADDING_H = 8.dp
private val CELL_PADDING_V = 4.dp

/** A load that finishes within this never shows the loading text (spec 3 6.4). */
private const val LOADING_TEXT_DELAY_MS = 300L

/** OpenType tabular figures so numbers line up across columns (Pretendard supports tnum). */
private const val TNUM = "tnum"

/**
 * Wires [CompareScreen] to a [CompareController] for [carIds]. The controller lives as long as
 * this route stays composed; leaving the screen cancels the evaluations.
 */
@Composable
fun CompareRoute(
    carIds: List<String>,
    repository: SavedCarsRepository,
    evaluator: ReportEvaluator,
    onBack: () -> Unit,
    onOpenResult: (url: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(carIds) { CompareController(carIds, repository::observe, evaluator, scope) }
    LaunchedEffect(controller) { controller.start() }
    val state by controller.state.collectAsStateWithLifecycle()
    CompareScreen(state = state, onBack = onBack, onOpenResult = onOpenResult)
}

@Composable
fun CompareScreen(
    state: CompareUiState,
    onBack: () -> Unit,
    onOpenResult: (url: String) -> Unit,
) {
    val colors = AvTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding(),
    ) {
        AvTopBar(
            onBack = onBack,
            title = stringResource(R.string.compare_title),
            subtitle = (state as? CompareUiState.Ready)?.let {
                stringResource(R.string.compare_subtitle, it.table.headers.size)
            },
        )
        when (state) {
            CompareUiState.Loading -> CompareLoading()
            CompareUiState.TooFew -> AvEmptyState(
                icon = Icons.AutoMirrored.Rounded.CompareArrows,
                title = stringResource(R.string.compare_too_few_title),
                description = stringResource(R.string.compare_too_few_desc),
                actionLabel = stringResource(R.string.compare_too_few_action),
                onAction = onBack,
            )
            is CompareUiState.Ready -> CompareTableContent(state.table, onOpenResult)
        }
    }
}

@Composable
private fun CompareLoading() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADING_TEXT_DELAY_MS)
        visible = true
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (visible) {
            Text(
                text = stringResource(R.string.compare_loading),
                style = AvTheme.typography.body2,
                color = AvTheme.colors.textTertiary,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompareTableContent(table: CompareTable, onOpenResult: (String) -> Unit) {
    val colors = AvTheme.colors
    val noName = stringResource(R.string.saved_no_name)
    val names = table.headers.map { it.name ?: noName }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader(key = "names") {
            Column(Modifier.background(colors.background)) {
                ColumnsRow(table.headers.size, Modifier.padding(vertical = 12.dp)) { i ->
                    NameCell(table.headers[i], names[i])
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border),
                )
            }
        }
        item(key = "heads") {
            ColumnsRow(table.headers.size, Modifier.padding(top = 12.dp, bottom = 20.dp)) { i ->
                ColumnHead(table.headers[i], names[i], onOpenResult)
            }
        }
        table.sections.forEach { section -> sectionItems(section, names) }
        item(key = "legend") {
            AvSectionDivider()
            Text(
                text = stringResource(R.string.compare_legend),
                style = AvTheme.typography.caption1,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = AvTheme.spacing.screenHorizontal, vertical = 20.dp),
            )
        }
    }
}

/** One equal-width slot per column, 20dp screen margins and [COLUMN_GAP] between slots. */
@Composable
private fun ColumnsRow(
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AvTheme.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP),
    ) {
        repeat(count) { i ->
            Box(Modifier.weight(1f)) { content(i) }
        }
    }
}

@Composable
private fun NameCell(header: CompareHeader, name: String) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(Modifier.padding(horizontal = CELL_PADDING_H)) {
        Text(
            text = name,
            style = typography.body2.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (header.fallback) {
            Text(
                text = stringResource(R.string.compare_fallback_note),
                style = typography.caption1,
                color = colors.textTertiary,
            )
        }
    }
}

/** Verdict, score, score bar and "결과 보기": one tap target per column (spec 3 6.2). */
@Composable
private fun ColumnHead(header: CompareHeader, name: String, onOpenResult: (String) -> Unit) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val toneColors = colors.tone(header.verdictTone)
    val verdict = stringResource(header.verdictLabel)
    val score = stringResource(R.string.result_score, header.score)
    val description = listOf(
        name,
        "${stringResource(R.string.compare_row_verdict)} $verdict",
        "${stringResource(R.string.compare_row_score)} $score",
    ).joinToString(", ")
    val bestState = if (header.scoreBest) stringResource(R.string.compare_best_high) else null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .avPressable(
                onClick = { onOpenResult(header.url) },
                onClickLabel = stringResource(R.string.compare_open_result),
                focusShape = AvTheme.shapes.tile,
            )
            .clearAndSetSemantics {
                contentDescription = description
                if (bestState != null) stateDescription = bestState
            }
            .padding(horizontal = CELL_PADDING_H, vertical = CELL_PADDING_V),
    ) {
        AvBadge(text = verdict, tone = header.verdictTone)
        Spacer(Modifier.height(6.dp))
        Text(
            text = score,
            style = typography.title2.copy(fontFeatureSettings = TNUM),
            color = toneColors.text,
            maxLines = 1,
            modifier = if (header.scoreBest) {
                Modifier
                    .clip(AvTheme.shapes.tile)
                    .background(colors.accentWeak)
                    .padding(horizontal = 6.dp)
            } else {
                Modifier
            },
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(AvTheme.shapes.chip)
                .background(colors.surfaceMuted),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(header.score.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .clip(AvTheme.shapes.chip)
                    .background(toneColors.fill),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.compare_open_result),
                style = typography.caption1,
                color = colors.accentText,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun LazyListScope.sectionItems(section: CompareSection, names: List<String>) {
    item(key = "section-${section.title}") {
        AvSectionDivider()
        AvSectionHeader(title = stringResource(section.title))
    }
    section.rows.forEachIndexed { index, row ->
        item(key = "row-${section.title}-$index") {
            CompareRowItem(row, section, names, last = index == section.rows.lastIndex)
        }
    }
}

/** Label line across the screen, then one value per column (spec 3 6.2). */
@Composable
private fun CompareRowItem(row: CompareRow, section: CompareSection, names: List<String>, last: Boolean) {
    val colors = AvTheme.colors
    val label = when (val l = row.label) {
        is CompareLabel.Res -> stringResource(l.id)
        is CompareLabel.Text -> l.text
        // The issues row has no label of its own; the section title names it.
        null -> null
    }
    val a11yLabel = label ?: stringResource(section.title)
    val bestState = row.bestState?.let { stringResource(it) }
    Column(Modifier.padding(top = 8.dp, bottom = if (last) 20.dp else 4.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = AvTheme.typography.caption1,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Each value cell reads its own label, so the line itself stays silent.
                modifier = Modifier
                    .padding(horizontal = AvTheme.spacing.screenHorizontal + CELL_PADDING_H)
                    .padding(bottom = 2.dp)
                    .clearAndSetSemantics {},
            )
        }
        ColumnsRow(row.cells.size) { i ->
            ValueCell(row.cells[i], "${names[i]} $a11yLabel", bestState)
        }
    }
}

@Composable
private fun ValueCell(cell: CompareCell, a11yPrefix: String, bestState: String?) {
    val colors = AvTheme.colors
    val spoken = spokenValue(cell.value)
    Box(
        modifier = Modifier
            .then(
                if (cell.best) {
                    Modifier
                        .clip(AvTheme.shapes.tile)
                        .background(colors.accentWeak)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = CELL_PADDING_H, vertical = CELL_PADDING_V)
            // "{car} {row} {value}": a value alone can't tell the columns apart (spec 3 8).
            .clearAndSetSemantics {
                contentDescription = "$a11yPrefix $spoken"
                if (cell.best && bestState != null) stateDescription = bestState
            },
    ) {
        CellValue(cell.value, cell.best)
    }
}

@Composable
private fun CellValue(value: CompareValue, best: Boolean) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val valueStyle = typography.body2.copy(fontFeatureSettings = TNUM)
    val bestStyle = valueStyle.copy(fontWeight = FontWeight.SemiBold, color = colors.accentText)
    when (value) {
        CompareValue.Missing -> PlainValue(stringResource(R.string.compare_missing), valueStyle.copy(color = colors.textTertiary))
        is CompareValue.Text -> PlainValue(
            value.text,
            when {
                best -> bestStyle
                value.tone != null -> valueStyle.copy(color = colors.tone(value.tone).text)
                else -> valueStyle.copy(color = colors.textPrimary)
            },
        )
        is CompareValue.Count -> {
            val text = countText(value)
            when {
                value.count > 0 -> AvBadge(text = text, tone = value.tone)
                best -> PlainValue(text, bestStyle)
                else -> PlainValue(text, valueStyle.copy(color = colors.textTertiary))
            }
        }
        is CompareValue.Issues -> IssuesValue(value)
    }
}

@Composable
private fun PlainValue(text: String, style: TextStyle) {
    Text(text = text, style = style, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun IssuesValue(value: CompareValue.Issues) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    if (value.lines.isEmpty()) {
        PlainValue(stringResource(R.string.compare_issues_none), typography.body2.copy(color = colors.textTertiary))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        value.lines.forEach { line ->
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.tone(line.tone).text, fontWeight = FontWeight.SemiBold)) {
                        append(line.label)
                    }
                    append(" ")
                    append(line.title)
                },
                style = typography.body2,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (value.more > 0) {
            Text(
                text = stringResource(R.string.compare_issues_more, value.more),
                style = typography.caption1,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun countText(value: CompareValue.Count): String =
    value.label?.let { stringResource(it, value.count) } ?: value.count.toString()

/** Full value for screen readers; truncated cells still read everything. */
@Composable
private fun spokenValue(value: CompareValue): String = when (value) {
    CompareValue.Missing -> stringResource(R.string.compare_missing_a11y)
    is CompareValue.Text -> value.text
    is CompareValue.Count -> countText(value)
    is CompareValue.Issues ->
        if (value.lines.isEmpty()) {
            stringResource(R.string.compare_issues_none)
        } else {
            val lines = value.lines.map { "${it.label} ${it.title}" }
            val more = if (value.more > 0) listOf(stringResource(R.string.compare_issues_more, value.more)) else emptyList()
            (lines + more).joinToString(", ")
        }
}
