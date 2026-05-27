package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.db.SavedCarEntity
import com.daksin.autoverdict.ui.theme.Background
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
import java.util.Locale

@Composable
fun CompareScreen(
    carIds: List<String>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoVerdictApp
    var cars by remember { mutableStateOf<List<SavedCarEntity>>(emptyList()) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }

    LaunchedEffect(carIds) {
        cars = app.database.savedCarDao().getByCarIds(carIds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_revert),
                    contentDescription = "뒤로",
                )
            }
            Text(
                text = "비교",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (cars.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "로딩 중...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
            }
        } else {
            val horizontalScroll = rememberScrollState()
            val labelWidth = 80.dp
            val columnWidth = 140.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    // Label column (fixed)
                    Column(
                        modifier = Modifier
                            .width(labelWidth)
                            .fillMaxHeight(),
                    ) {
                        // Header spacer for car title row
                        CompareCell(
                            text = "",
                            modifier = Modifier
                                .width(labelWidth)
                                .height(72.dp),
                        )
                        CompareLabelCell("점수", labelWidth)
                        CompareLabelCell("판정", labelWidth)
                        CompareLabelCell("연식", labelWidth)
                        CompareLabelCell("주행거리", labelWidth)
                        CompareLabelCell("가격", labelWidth)
                        CompareLabelCell("연료", labelWidth)
                        CompareLabelCell("위험", labelWidth)
                        CompareLabelCell("주의", labelWidth)
                        CompareLabelCell("양호", labelWidth)
                        CompareLabelCell("미확인", labelWidth)
                    }

                    // Scrollable car columns
                    Row(
                        modifier = Modifier
                            .horizontalScroll(horizontalScroll)
                            .fillMaxHeight(),
                    ) {
                        cars.forEach { car ->
                            Column(modifier = Modifier.width(columnWidth)) {
                                // Car title header
                                Box(
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .height(72.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = car.title.ifBlank { "매물 #${car.carId}" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                // Score (color-coded)
                                ScoreBadgeCell(car.score, columnWidth)

                                // Verdict
                                CompareCell(
                                    text = car.verdict,
                                    modifier = Modifier.width(columnWidth),
                                )

                                // Year
                                CompareCell(
                                    text = car.year?.let { "${it}년" } ?: "-",
                                    modifier = Modifier.width(columnWidth),
                                )

                                // Mileage
                                CompareCell(
                                    text = car.mileageKm?.let { "${numberFormat.format(it)}km" } ?: "-",
                                    modifier = Modifier.width(columnWidth),
                                )

                                // Price
                                CompareCell(
                                    text = car.priceWon?.let { "${numberFormat.format(it / 10000)}만원" } ?: "-",
                                    modifier = Modifier.width(columnWidth),
                                )

                                // Fuel type
                                CompareCell(
                                    text = car.fuelType ?: "-",
                                    modifier = Modifier.width(columnWidth),
                                )

                                // Danger count
                                CountBadgeCell(car.dangerCount, Danger, DangerBg, columnWidth)

                                // Caution count
                                CountBadgeCell(car.cautionCount, Warning, WarningBg, columnWidth)

                                // Pass count
                                CountBadgeCell(car.passCount, Success, SuccessBg, columnWidth)

                                // Unknown count
                                CompareCell(
                                    text = "${car.unknownCount}",
                                    modifier = Modifier.width(columnWidth),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareLabelCell(
    label: String,
    width: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(44.dp)
            .background(Background)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary,
        )
    }
}

@Composable
private fun CompareCell(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScoreBadgeCell(
    score: Int,
    width: androidx.compose.ui.unit.Dp,
) {
    val (textColor, bgColor) = when {
        score >= 70 -> Success to SuccessBg
        score >= 40 -> Warning to WarningBg
        else -> Danger to DangerBg
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = textColor,
            )
        }
    }
}

@Composable
private fun CountBadgeCell(
    count: Int,
    textColor: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    width: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                )
            }
        } else {
            Text(
                text = "0",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
