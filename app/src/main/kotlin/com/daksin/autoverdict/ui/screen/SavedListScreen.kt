package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.db.SavedCarEntity
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
fun SavedListScreen(onCarClick: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoVerdictApp
    val savedFlow = remember { app.database.savedCarDao().getAllFlow() }
    val savedCars by savedFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "저장된 매물",
            style = MaterialTheme.typography.titleLarge,
        )
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(savedCars, key = { it.carId }) { car ->
                    SavedCarCard(car = car, onClick = { onCarClick(car.url) })
                }
            }
        }
    }
}

@Composable
private fun SavedCarCard(car: SavedCarEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary),
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
