package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.db.SavedCarEntity
import com.daksin.autoverdict.ui.theme.Border
import com.daksin.autoverdict.ui.theme.Danger
import com.daksin.autoverdict.ui.theme.DangerBg
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.Success
import com.daksin.autoverdict.ui.theme.SuccessBg
import com.daksin.autoverdict.ui.theme.TextSecondary
import com.daksin.autoverdict.ui.theme.Warning
import com.daksin.autoverdict.ui.theme.WarningBg
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "저장된 매물",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (savedCars.size >= 2) {
                    TextButton(
                        onClick = {
                            compareMode = !compareMode
                            if (!compareMode) {
                                selectedCarIds = emptySet()
                            }
                        },
                    ) {
                        Text(
                            text = if (compareMode) "취소" else "비교하기",
                            color = Primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (savedCars.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "저장된 매물이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = if (compareMode && selectedCarIds.size >= 2) {
                        Modifier.padding(bottom = 64.dp)
                    } else {
                        Modifier
                    },
                ) {
                    items(savedCars, key = { it.carId }) { car ->
                        val isSelected = car.carId in selectedCarIds
                        SavedCarCard(
                            car = car,
                            compareMode = compareMode,
                            isSelected = isSelected,
                            onClick = {
                                if (compareMode) {
                                    selectedCarIds = if (isSelected) {
                                        selectedCarIds - car.carId
                                    } else if (selectedCarIds.size < 4) {
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
        }

        if (compareMode && selectedCarIds.size >= 2) {
            Button(
                onClick = {
                    onCompare(selectedCarIds.toList())
                    compareMode = false
                    selectedCarIds = emptySet()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("비교 (${selectedCarIds.size}대)")
            }
        }
    }
}

@Composable
private fun SavedCarCard(
    car: SavedCarEntity,
    compareMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (compareMode && isSelected) {
                    Modifier.border(2.dp, Primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (compareMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Primary else Border),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scoreColor(car.score)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${car.score}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = car.title.ifBlank { "매물 #${car.carId}" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (car.url.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = car.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                val details = buildList {
                    car.year?.let { add("${it}년") }
                    car.mileageKm?.let { add("${numberFormat.format(it)}km") }
                    car.priceWon?.let { add("${numberFormat.format(it / 10000)}만원") }
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" | "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (car.dangerCount > 0) {
                        SeverityChip("위험 ${car.dangerCount}", Danger, DangerBg)
                    }
                    if (car.cautionCount > 0) {
                        SeverityChip("주의 ${car.cautionCount}", Warning, WarningBg)
                    }
                    if (car.passCount > 0) {
                        SeverityChip("양호 ${car.passCount}", Success, SuccessBg)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(car.savedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SeverityChip(
    label: String,
    textColor: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
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
