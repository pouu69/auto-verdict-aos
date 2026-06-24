package com.car.autoverdict.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.ui.theme.Background
import com.car.autoverdict.ui.theme.Border
import com.car.autoverdict.ui.theme.Danger
import com.car.autoverdict.ui.theme.DangerBg
import com.car.autoverdict.ui.theme.Primary
import com.car.autoverdict.ui.theme.Success
import com.car.autoverdict.ui.theme.SuccessBg
import com.car.autoverdict.ui.theme.TextPrimary
import com.car.autoverdict.ui.theme.TextSecondary
import com.car.autoverdict.ui.theme.Warning
import com.car.autoverdict.ui.theme.WarningBg
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SortMode(val label: String) {
    RECENT("최근순"),
    SCORE_DESC("점수 높은순"),
    SCORE_ASC("점수 낮은순"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedListScreen(
    modifier: Modifier = Modifier,
    onCarClick: (String) -> Unit,
    onCompare: (List<String>) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoVerdictApp
    val savedFlow = remember { app.database.savedCarDao().getAllFlow() }
    val savedCars by savedFlow.collectAsState(initial = emptyList())

    var compareMode by remember { mutableStateOf(false) }
    var selectedCarIds by remember { mutableStateOf(emptySet<String>()) }
    var sortMode by remember { mutableStateOf(SortMode.RECENT) }

    val sortedCars = remember(savedCars, sortMode) {
        when (sortMode) {
            SortMode.RECENT -> savedCars
            SortMode.SCORE_DESC -> savedCars.sortedByDescending { it.score }
            SortMode.SCORE_ASC -> savedCars.sortedBy { it.score }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "저장된 매물",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = {
                    if (savedCars.size >= 2) {
                        IconButton(onClick = {
                            compareMode = !compareMode
                            if (!compareMode) selectedCarIds = emptySet()
                        }) {
                            Icon(
                                imageVector = if (compareMode) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.CompareArrows,
                                contentDescription = if (compareMode) "비교 취소" else "비교 모드",
                                tint = if (compareMode) Danger else Primary,
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary,
                ),
            )

            // Compare mode banner
            AnimatedVisibility(
                visible = compareMode,
                enter = slideInVertically(tween(220)) + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) + fadeOut(tween(180)),
            ) {
                CompareModeBanner(selectedCount = selectedCarIds.size)
            }

            if (savedCars.isEmpty()) {
                SavedEmptyState()
                return@Column
            }

            // Sort chips
            if (!compareMode) {
                SortChipRow(
                    current = sortMode,
                    onSelect = { sortMode = it },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = if (compareMode) 12.dp else 4.dp,
                    bottom = if (compareMode && selectedCarIds.size == 2) 96.dp else 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sortedCars, key = { it.carId }) { car ->
                    val isSelected = car.carId in selectedCarIds
                    SavedCarRow(
                        car = car,
                        compareMode = compareMode,
                        isSelected = isSelected,
                        onClick = {
                            if (compareMode) {
                                selectedCarIds = if (isSelected) {
                                    selectedCarIds - car.carId
                                } else if (selectedCarIds.size < 2) {
                                    selectedCarIds + car.carId
                                } else {
                                    selectedCarIds
                                }
                            } else {
                                onCarClick(car.url)
                            }
                        },
                    )
                }
            }
        }

        // FAB for compare action
        AnimatedVisibility(
            visible = compareMode && selectedCarIds.size == 2,
            enter = slideInVertically(tween(220), initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180), targetOffsetY = { it }) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    onCompare(selectedCarIds.toList())
                    compareMode = false
                    selectedCarIds = emptySet()
                },
                containerColor = Primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.CompareArrows,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "2대 비교하기",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun CompareModeBanner(selectedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Primary.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.CompareArrows,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "비교할 매물 2대를 선택하세요",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Primary,
            )
            Text(
                text = "$selectedCount / 2 선택됨",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Primary)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "$selectedCount/2",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChipRow(
    current: SortMode,
    onSelect: (SortMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortMode.entries.forEach { mode ->
            FilterChip(
                selected = current == mode,
                onClick = { onSelect(mode) },
                label = {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = Primary,
                    labelColor = TextSecondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = current == mode,
                    borderColor = Border,
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                ),
            )
        }
    }
}

@Composable
private fun SavedCarRow(
    car: SavedCarEntity,
    compareMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (compareMode && isSelected) {
                    Modifier.border(2.dp, Primary, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, Border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                },
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Leading: Score badge or selection checkbox
            if (compareMode) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Primary else Background,
                        )
                        .then(
                            if (!isSelected) {
                                Modifier.border(1.5.dp, Border, RoundedCornerShape(12.dp))
                            } else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "선택됨",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text(
                            text = "${car.score}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scoreColorList(car.score),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scoreColorList(car.score)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${car.score}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = car.title.ifBlank { "매물 #${car.carId}" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val details = buildList {
                    car.year?.let { add("${it}년") }
                    car.mileageKm?.let { add("${numberFormat.format(it)}km") }
                    car.priceWon?.let { add("${numberFormat.format(it / 10000)}만원") }
                }
                if (details.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (car.dangerCount > 0) {
                        SeverityChip("위험 ${car.dangerCount}", Danger, DangerBg)
                    }
                    if (car.cautionCount > 0) {
                        SeverityChip("주의 ${car.cautionCount}", Warning, WarningBg)
                    }
                    if (car.passCount > 0) {
                        SeverityChip("양호 ${car.passCount}", Success, SuccessBg)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = dateFormat.format(Date(car.savedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityChip(
    label: String,
    textColor: Color,
    bgColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
        )
    }
}

@Composable
private fun SavedEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "저장된 매물이 없습니다",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "분석 결과 화면에서 저장하면\n여기에 모아 비교할 수 있어요",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
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

private fun scoreColorList(score: Int): Color {
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
