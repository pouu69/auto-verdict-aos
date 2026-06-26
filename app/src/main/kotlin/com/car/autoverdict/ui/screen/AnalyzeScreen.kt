package com.car.autoverdict.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.db.CacheEntity
import com.car.autoverdict.ui.components.BrowserShareIllustration
import com.car.autoverdict.ui.components.ShareSheetIllustration
import com.car.autoverdict.ui.theme.Background
import com.car.autoverdict.ui.theme.Border
import com.car.autoverdict.ui.theme.Danger
import com.car.autoverdict.ui.theme.Primary
import com.car.autoverdict.ui.theme.TextPrimary
import com.car.autoverdict.ui.theme.TextSecondary
import com.car.autoverdict.util.ClipboardUtil
import com.car.autoverdict.util.EncarUrl
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(modifier: Modifier = Modifier, onAnalyze: (String) -> Unit) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }
    var fromClipboard by remember { mutableStateOf(false) }
    val isValidUrl = EncarUrl.isEncarDetail(urlText)
    val showError = urlText.isNotBlank() && !isValidUrl
    val app = context.applicationContext as AutoVerdictApp
    val recentFlow: Flow<List<CacheEntity>> = remember { app.database.cacheDao().getRecentFlow() }
    val recentItems by recentFlow.collectAsState(initial = emptyList())

    // Auto-detect an Encar URL on the clipboard whenever the screen comes to the
    // foreground — e.g. the user copied a listing link in the browser, then
    // opened the app. Closes the "paste" path with zero typing.
    LifecycleResumeEffect(Unit) {
        if (urlText.isBlank()) {
            ClipboardUtil.getEncarUrl(context)?.let {
                urlText = it
                fromClipboard = true
            }
        }
        onPauseOrDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "AutoVerdict",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Background,
                titleContentColor = TextPrimary,
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        ) {
            item {
                ShareGuideHero(onOpenEncar = { openEncarHome(context) })
                Spacer(Modifier.height(20.dp))
            }

            item {
                OrDivider()
                Spacer(Modifier.height(14.dp))
            }

            item {
                UrlInputCard(
                    urlText = urlText,
                    onUrlChange = { urlText = it; fromClipboard = false },
                    isError = showError,
                    isValid = isValidUrl,
                    fromClipboard = fromClipboard && isValidUrl,
                    onAnalyze = { onAnalyze(urlText) },
                )
                Spacer(Modifier.height(28.dp))
            }

            if (recentItems.isNotEmpty()) {
                item { RecentSectionHeader() }
                items(recentItems.take(5), key = { it.carId }) { item ->
                    RecentItemRow(item = item, onClick = { onAnalyze(item.url) })
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun openEncarHome(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EncarUrl.HOME_URL)))
    }
}

@Composable
private fun ShareGuideHero(onOpenEncar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.IosShare,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "엔카에서 공유하면 자동 분석",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                    )
                    Text(
                        text = "가장 빠른 사용법이에요",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            ShareGuideStep(1, "브라우저로 엔카 매물 페이지 열기")
            Spacer(Modifier.height(14.dp))
            ShareGuideStep(2, "브라우저 메뉴(⋮)에서 '공유' 선택") {
                BrowserShareIllustration()
            }
            Spacer(Modifier.height(14.dp))
            ShareGuideStep(3, "공유 목록에서 AutoVerdict 선택") {
                ShareSheetIllustration()
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onOpenEncar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "엔카에서 매물 찾기",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ShareGuideStep(
    number: Int,
    text: String,
    illustration: (@Composable () -> Unit)? = null,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
        if (illustration != null) {
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.padding(start = 38.dp)) {
                illustration()
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(Border.copy(alpha = 0.6f)),
        )
        Text(
            text = "또는 링크 직접 입력",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(Border.copy(alpha = 0.6f)),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrlInputCard(
    urlText: String,
    onUrlChange: (String) -> Unit,
    isError: Boolean,
    isValid: Boolean,
    fromClipboard: Boolean,
    onAnalyze: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (fromClipboard) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "복사한 링크를 가져왔어요",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = Primary,
                    )
                }
            }
            OutlinedTextField(
                value = urlText,
                onValueChange = onUrlChange,
                placeholder = {
                    Text(
                        text = "fem.encar.com/cars/detail/...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = if (isError) Danger else TextSecondary,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("올바른 엔카 매물 URL을 입력해주세요") }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    cursorColor = Primary,
                    errorBorderColor = Danger,
                    errorSupportingTextColor = Danger,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = isValid,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "분석 시작",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun RecentSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "최근 조회",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "24시간",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun RecentItemRow(item: CacheEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        headlineContent = {
            Text(
                text = item.title.ifBlank { "매물 #${item.carId}" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = dateFormat.format(Date(item.cachedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
