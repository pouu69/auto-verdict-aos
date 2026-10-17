package com.car.autoverdict.ui.screen

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.theme.AvTheme
import java.text.NumberFormat
import java.util.Locale

private val LABEL_COL_WIDTH = 72.dp
private val SIDE_PADDING = 16.dp
private val COLUMN_GAP = 10.dp

/** OpenType tabular figures so numbers line up across columns (Pretendard supports tnum). */
private const val TNUM = "tnum"

@Composable
fun CompareScreen(
    carIds: List<String>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoVerdictApp
    var cars by remember { mutableStateOf<List<SavedCarEntity>>(emptyList()) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val colors = AvTheme.colors

    LaunchedEffect(carIds) {
        val loaded = app.database.savedCarDao().getByCarIds(carIds)
        cars = carIds.mapNotNull { id -> loaded.firstOrNull { it.carId == id } }.take(2)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceMuted)
            .navigationBarsPadding(),
    ) {
        AvTopBar(
            onBack = onBack,
            title = "비교 분석",
            subtitle = if (cars.isNotEmpty()) "2대 매물을 한눈에" else null,
        )

        if (cars.isEmpty()) {
            EmptyLoadingState()
            return@Column
        }

        val winners = remember(cars) { computeWinners(cars) }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            HeroStrip(cars = cars, bestScoreId = winners.bestScoreId)

            CompareSection(title = "기본 정보") {
                MetricRow(label = "연식") {
                    cars.forEach { car ->
                        ValueCell(
                            modifier = Modifier.weight(1f),
                            text = car.year?.let { "${it}년" } ?: "—",
                            isBest = car.carId == winners.bestYearId,
                            tabular = true,
                        )
                    }
                }
                MetricRow(label = "주행") {
                    cars.forEach { car ->
                        ValueCell(
                            modifier = Modifier.weight(1f),
                            text = car.mileageKm?.let { "${numberFormat.format(it)}km" } ?: "—",
                            isBest = car.carId == winners.bestMileageId,
                            tabular = true,
                        )
                    }
                }
                MetricRow(label = "가격") {
                    cars.forEach { car ->
                        ValueCell(
                            modifier = Modifier.weight(1f),
                            text = car.priceWon?.let { "${numberFormat.format(it / 10000)}만원" } ?: "—",
                            isBest = car.carId == winners.bestPriceId,
                            tabular = true,
                            emphasize = true,
                        )
                    }
                }
                MetricRow(label = "연료", last = true) {
                    cars.forEach { car ->
                        ValueCell(
                            modifier = Modifier.weight(1f),
                            text = car.fuelType ?: "—",
                            isBest = false,
                        )
                    }
                }
            }

            CompareSection(title = "종합 평가") {
                MetricRow(label = "점수", tall = true) {
                    cars.forEach { car ->
                        ScoreCell(
                            modifier = Modifier.weight(1f),
                            score = car.score,
                            isBest = car.carId == winners.bestScoreId,
                        )
                    }
                }
                MetricRow(label = "판정", last = true) {
                    cars.forEach { car ->
                        VerdictCell(
                            modifier = Modifier.weight(1f),
                            verdict = car.verdict,
                            score = car.score,
                        )
                    }
                }
            }

            CompareSection(title = "진단 결과") {
                MetricRow(label = "분포", tall = true) {
                    cars.forEach { car ->
                        DiagnosisStackBar(
                            modifier = Modifier.weight(1f),
                            danger = car.dangerCount,
                            caution = car.cautionCount,
                            pass = car.passCount,
                            unknown = car.unknownCount,
                        )
                    }
                }
                MetricRow(label = "위험") {
                    cars.forEach { car ->
                        CountCell(
                            modifier = Modifier.weight(1f),
                            count = car.dangerCount,
                            textColor = colors.dangerText,
                            bgColor = colors.dangerBg,
                            isBest = car.carId == winners.bestDangerId,
                        )
                    }
                }
                MetricRow(label = "주의") {
                    cars.forEach { car ->
                        CountCell(
                            modifier = Modifier.weight(1f),
                            count = car.cautionCount,
                            textColor = colors.warningText,
                            bgColor = colors.warningBg,
                            isBest = car.carId == winners.bestCautionId,
                        )
                    }
                }
                MetricRow(label = "양호") {
                    cars.forEach { car ->
                        CountCell(
                            modifier = Modifier.weight(1f),
                            count = car.passCount,
                            textColor = colors.successText,
                            bgColor = colors.successBg,
                            isBest = car.carId == winners.bestPassId,
                        )
                    }
                }
                MetricRow(label = "미확인", last = true) {
                    cars.forEach { car ->
                        CountCell(
                            modifier = Modifier.weight(1f),
                            count = car.unknownCount,
                            textColor = colors.textSecondary,
                            bgColor = colors.border,
                            isBest = false,
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EmptyLoadingState() {
    val colors = AvTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, colors.border, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "01",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TNUM,
                ),
                color = colors.textSecondary.copy(alpha = 0.5f),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "매물 정보를 불러오는 중",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "잠시만 기다려 주세요",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun HeroStrip(
    cars: List<SavedCarEntity>,
    bestScoreId: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 12.dp, end = SIDE_PADDING),
        horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP),
        verticalAlignment = Alignment.Top,
    ) {
        Spacer(Modifier.width(LABEL_COL_WIDTH - COLUMN_GAP))
        cars.forEachIndexed { index, car ->
            HeroCarCard(
                modifier = Modifier.weight(1f),
                index = index + 1,
                car = car,
                isBest = car.carId == bestScoreId,
            )
        }
    }
}

@Composable
private fun HeroCarCard(
    modifier: Modifier,
    index: Int,
    car: SavedCarEntity,
    isBest: Boolean,
) {
    val colors = AvTheme.colors
    val accent = scoreColor(car.score)
    val animatedFill by animateFloatAsState(
        targetValue = car.score.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label = "heroBar-${car.carId}",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isBest) 1.5.dp else 1.dp,
                color = if (isBest) accent else colors.border,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "0$index".takeLast(2),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontFeatureSettings = TNUM,
                ),
                color = colors.textSecondary,
            )
            Spacer(Modifier.weight(1f))
            if (isBest) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "TOP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        ),
                        color = accent,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = car.title.ifBlank { "매물 #${car.carId}" },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
            ),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${car.score}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontFeatureSettings = TNUM,
                ),
                color = accent,
                maxLines = 1,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "/100",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFeatureSettings = TNUM,
                ),
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFill)
                    .fillMaxHeight()
                    .background(accent),
            )
        }
    }
}

@Composable
private fun CompareSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AvTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                ),
                color = colors.textPrimary,
            )
        }
        content()
    }
}

@Composable
private fun MetricRow(
    label: String,
    tall: Boolean = false,
    last: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AvTheme.colors
    val rowHeight: Dp = if (tall) 76.dp else 52.dp
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .padding(end = SIDE_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP),
        ) {
            Box(
                modifier = Modifier
                    .width(LABEL_COL_WIDTH - COLUMN_GAP)
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.3.sp,
                    ),
                    color = colors.textSecondary,
                )
            }
            content()
        }
        if (!last) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = colors.border.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ValueCell(
    modifier: Modifier,
    text: String,
    isBest: Boolean,
    tabular: Boolean = false,
    emphasize: Boolean = false,
) {
    val colors = AvTheme.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isBest) Modifier.background(colors.accentWeak) else Modifier,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isBest) {
                Text(
                    text = "▲",
                    fontSize = 11.sp,
                    color = colors.accentText,
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = if (tabular) TNUM else null,
                    fontWeight = when {
                        isBest -> FontWeight.SemiBold
                        emphasize -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    },
                ),
                color = if (isBest) colors.accentText else colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ScoreCell(
    modifier: Modifier,
    score: Int,
    isBest: Boolean,
) {
    val colors = AvTheme.colors
    val accent = scoreColor(score)
    val animatedFill by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label = "scoreCell-$score",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isBest) {
                Text(
                    text = "▲",
                    fontSize = 11.sp,
                    color = accent,
                    modifier = Modifier.padding(end = 4.dp, bottom = 5.dp),
                )
            }
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TNUM,
                ),
                color = accent,
                maxLines = 1,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "점",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFill)
                    .fillMaxHeight()
                    .background(accent),
            )
        }
    }
}

@Composable
private fun VerdictCell(
    modifier: Modifier,
    verdict: String,
    score: Int,
) {
    val colors = AvTheme.colors
    val (bg, fg) = when {
        score >= 78 -> colors.successBg to colors.successText
        score >= 50 -> colors.warningBg to colors.warningText
        else -> colors.dangerBg to colors.dangerText
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = verdict.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CountCell(
    modifier: Modifier,
    count: Int,
    textColor: Color,
    bgColor: Color,
    isBest: Boolean,
) {
    val colors = AvTheme.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isBest) {
                Text(
                    text = "▲",
                    fontSize = 11.sp,
                    color = colors.successText,
                )
                Spacer(Modifier.width(4.dp))
            }
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = TNUM,
                        ),
                        color = textColor,
                    )
                }
            } else {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = TNUM,
                    ),
                    color = colors.textSecondary.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun DiagnosisStackBar(
    modifier: Modifier,
    danger: Int,
    caution: Int,
    pass: Int,
    unknown: Int,
) {
    val colors = AvTheme.colors
    val total = (danger + caution + pass + unknown).coerceAtLeast(1)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.border),
        ) {
            if (danger > 0) {
                Box(
                    modifier = Modifier
                        .weight(danger.toFloat())
                        .fillMaxHeight()
                        .background(colors.dangerFill),
                )
            }
            if (caution > 0) {
                Box(
                    modifier = Modifier
                        .weight(caution.toFloat())
                        .fillMaxHeight()
                        .background(colors.warningFill),
                )
            }
            if (pass > 0) {
                Box(
                    modifier = Modifier
                        .weight(pass.toFloat())
                        .fillMaxHeight()
                        .background(colors.successFill),
                )
            }
            if (unknown > 0) {
                Box(
                    modifier = Modifier
                        .weight(unknown.toFloat())
                        .fillMaxHeight()
                        .background(colors.border),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "총 ${total}건 점검",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.2.sp,
                fontFeatureSettings = TNUM,
            ),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private data class WinnerIds(
    val bestScoreId: String?,
    val bestYearId: String?,
    val bestMileageId: String?,
    val bestPriceId: String?,
    val bestDangerId: String?,
    val bestCautionId: String?,
    val bestPassId: String?,
)

private fun computeWinners(cars: List<SavedCarEntity>): WinnerIds {
    return WinnerIds(
        bestScoreId = uniqueWinner(cars, higherIsBetter = true) { it.score.toDouble() },
        bestYearId = uniqueWinner(cars, higherIsBetter = true) { it.year?.toDouble() },
        bestMileageId = uniqueWinner(cars, higherIsBetter = false) { it.mileageKm?.toDouble() },
        bestPriceId = uniqueWinner(cars, higherIsBetter = false) { it.priceWon?.toDouble() },
        bestDangerId = uniqueWinner(cars, higherIsBetter = false) { it.dangerCount.toDouble() },
        bestCautionId = uniqueWinner(cars, higherIsBetter = false) { it.cautionCount.toDouble() },
        bestPassId = uniqueWinner(cars, higherIsBetter = true) { it.passCount.toDouble() },
    )
}

private inline fun uniqueWinner(
    cars: List<SavedCarEntity>,
    higherIsBetter: Boolean,
    selector: (SavedCarEntity) -> Double?,
): String? {
    if (cars.size < 2) return null
    val pairs = cars.mapNotNull { car -> selector(car)?.let { car.carId to it } }
    if (pairs.size < 2) return null
    val target = if (higherIsBetter) pairs.maxOf { it.second } else pairs.minOf { it.second }
    val tops = pairs.filter { it.second == target }
    return if (tops.size == 1) tops.first().first else null
}

private val COLOR_STOPS = listOf(
    0 to Triple(198, 40, 40),
    30 to Triple(230, 81, 0),
    50 to Triple(245, 127, 23),
    65 to Triple(158, 157, 36),
    78 to Triple(46, 125, 50),
    88 to Triple(0, 137, 123),
    100 to Triple(21, 101, 192),
)

private fun scoreColor(score: Int): Color {
    val s = score.coerceIn(0, 100)
    var lo = COLOR_STOPS.first()
    var hi = COLOR_STOPS.last()
    for (i in 0 until COLOR_STOPS.size - 1) {
        if (s >= COLOR_STOPS[i].first && s <= COLOR_STOPS[i + 1].first) {
            lo = COLOR_STOPS[i]
            hi = COLOR_STOPS[i + 1]
            break
        }
    }
    val t = if (hi.first == lo.first) 0f else (s - lo.first).toFloat() / (hi.first - lo.first)
    val r = (lo.second.first + (hi.second.first - lo.second.first) * t).toInt()
    val g = (lo.second.second + (hi.second.second - lo.second.second) * t).toInt()
    val b = (lo.second.third + (hi.second.third - lo.second.third) * t).toInt()
    return Color(r, g, b)
}
