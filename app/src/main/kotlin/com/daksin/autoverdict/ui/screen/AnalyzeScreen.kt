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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.db.CacheEntity
import com.daksin.autoverdict.ui.theme.Border
import com.daksin.autoverdict.ui.theme.Danger
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
import com.daksin.autoverdict.util.ClipboardUtil
import com.daksin.autoverdict.util.EncarUrl
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyzeScreen(modifier: Modifier = Modifier, onAnalyze: (String) -> Unit) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }
    val isValidUrl = EncarUrl.isEncarDetail(urlText)
    val showError = urlText.isNotBlank() && !isValidUrl
    val app = context.applicationContext as AutoVerdictApp
    val recentFlow: Flow<List<CacheEntity>> = remember { app.database.cacheDao().getRecentFlow() }
    val recentItems by recentFlow.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "엔카 매물 분석",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("엔카 매물 URL") },
                placeholder = { Text("https://fem.encar.com/cars/detail/...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = showError,
                supportingText = if (showError) {
                    { Text("올바른 엔카 매물 URL을 입력해주세요", color = Danger) }
                } else {
                    null
                },
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary.copy(alpha = 0.1f))
                    .clickable {
                        val clipUrl = ClipboardUtil.getEncarUrl(context)
                        if (clipUrl != null) urlText = clipUrl
                    }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "붙여넣기",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (isValidUrl) {
                    onAnalyze(urlText)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            enabled = isValidUrl,
        ) {
            Text("분석 시작")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Share guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "브라우저에서 바로 분석하기",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShareGuideStep("1", "엔카 앱이나 브라우저에서 매물 페이지 열기")
                Spacer(modifier = Modifier.height(4.dp))
                ShareGuideStep("2", "공유 버튼 누르기")
                Spacer(modifier = Modifier.height(4.dp))
                ShareGuideStep("3", "AutoVerdict 선택하면 자동 분석")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (recentItems.isNotEmpty()) {
            Text(
                text = "최근 분석",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentItems, key = { it.carId }) { item ->
                    RecentCacheCard(item = item, onClick = { onAnalyze(item.url) })
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "아직 분석한 매물이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "URL을 입력하거나 브라우저에서 공유해보세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = Border,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareGuideStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun RecentCacheCard(item: CacheEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.title.isNotBlank()) item.title else "매물 #${item.carId}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(item.cachedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (item.score > 0) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${item.score}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
