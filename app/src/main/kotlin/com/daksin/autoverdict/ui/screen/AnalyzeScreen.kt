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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.db.CacheEntity
import com.daksin.autoverdict.ui.theme.Border
import com.daksin.autoverdict.ui.theme.Danger
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
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
    val recentPreview = recentItems.take(3)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "AV",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "엔카 매물 분석",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "중고차 종합 평가",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // URL input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("엔카 매물 URL") },
                        placeholder = { Text("https://fem.encar.com/cars/detail/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showError,
                        supportingText = if (showError) {
                            { Text("올바른 엔카 매물 URL을 입력해주세요", color = Danger) }
                        } else {
                            null
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            cursorColor = Primary,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { if (isValidUrl) onAnalyze(urlText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isValidUrl,
                    ) {
                        Text("분석 시작", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share guide
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Primary.copy(alpha = 0.05f),
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tip: 더 빠르게 분석하기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ShareGuideStep("1", "브라우저에서 엔카 매물 페이지 열기")
                    Spacer(modifier = Modifier.height(6.dp))
                    ShareGuideStep("2", "브라우저 메뉴의 공유 버튼 탭")
                    Spacer(modifier = Modifier.height(6.dp))
                    ShareGuideStep("3", "목록에서 AutoVerdict 선택 → 자동 분석")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (recentPreview.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "최근 조회",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "24시간 이내",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                recentPreview.forEach { item ->
                    RecentItem(item = item, onClick = { onAnalyze(item.url) })
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecentItem(item: CacheEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (item.title.isNotBlank()) item.title else "매물 #${item.carId}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dateFormat.format(Date(item.cachedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Text(
            text = ">",
            style = MaterialTheme.typography.bodyMedium,
            color = Border,
        )
    }
}

@Composable
private fun ShareGuideStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

