package com.car.autoverdict.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.R
import com.car.autoverdict.db.CacheEntity
import com.car.autoverdict.ui.components.AvIconButton
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.BrowserShareIllustration
import com.car.autoverdict.ui.components.ShareSheetIllustration
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.util.ClipboardUtil
import com.car.autoverdict.util.EncarUrl
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    modifier: Modifier = Modifier,
    lastAnalyzedUrl: String? = null,
    onOpenSettings: () -> Unit = {},
    onAnalyze: (String) -> Unit,
) {
    val colors = AvTheme.colors
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }
    var fromClipboard by remember { mutableStateOf(false) }
    val isValidUrl = EncarUrl.isEncarDetail(urlText)
    val showError = urlText.isNotBlank() && !isValidUrl
    val app = context.applicationContext as AutoVerdictApp
    val recentFlow: Flow<List<CacheEntity>> = remember { app.database.cacheDao().getRecentFlow() }
    val recentItems by recentFlow.collectAsState(initial = emptyList())
    var showGuideSheet by remember { mutableStateOf(false) }

    // Auto-detect an Encar URL on the clipboard whenever the screen comes to the
    // foreground — e.g. the user copied a listing link in the browser, then
    // opened the app. Closes the "paste" path with zero typing.
    //
    // Skip the URL we just analyzed: returning from a result would otherwise
    // re-fill the field with the same link that is still sitting on the
    // clipboard, making the input look like it never reset.
    LifecycleResumeEffect(lastAnalyzedUrl) {
        if (urlText.isBlank()) {
            ClipboardUtil.getEncarUrl(context)
                ?.takeIf { EncarUrl.extractCarId(it) != EncarUrl.extractCarId(lastAnalyzedUrl) }
                ?.let {
                    urlText = it
                    fromClipboard = true
                    Toast.makeText(context, "복사한 엔카 링크를 가져왔어요", Toast.LENGTH_SHORT).show()
                }
        }
        onPauseOrDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceMuted)
            .imePadding(),
    ) {
        AvTopBar(
            onBack = null,
            title = "AutoVerdict",
            actions = {
                AvIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    onClick = onOpenSettings,
                )
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        ) {
            item {
                ShareGuidePromptCard(onClick = { showGuideSheet = true })
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
                    onAnalyze = {
                        // Clear the field as we hand the link off to analysis, so
                        // returning from the result never shows the consumed link —
                        // regardless of whether this screen stays in composition.
                        onAnalyze(urlText)
                        urlText = ""
                        fromClipboard = false
                    },
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

    if (showGuideSheet) {
        ShareGuideBottomSheet(
            onDismiss = { showGuideSheet = false },
            onOpenEncar = {
                showGuideSheet = false
                openEncarHome(context)
            },
        )
    }
}

private fun openEncarHome(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EncarUrl.HOME_URL)))
    }
}

@Composable
private fun ShareGuidePromptCard(onClick: () -> Unit) {
    val colors = AvTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.accentWeak),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.IosShare,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "엔카에서 공유하면 자동 분석",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                Text(
                    text = "탭해서 사용법 보기",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.accentText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareGuideBottomSheet(
    onDismiss: () -> Unit,
    onOpenEncar: () -> Unit,
) {
    val colors = AvTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = "엔카 공유로 분석하는 법",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "엔카 매물을 가장 빠르게 분석하는 방법이에요",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            ShareGuideStep(1, "브라우저로 엔카 매물 페이지 열기")
            Spacer(Modifier.height(16.dp))
            ShareGuideStep(2, "브라우저 메뉴(⋮)에서 '공유' 선택") {
                BrowserShareIllustration()
            }
            Spacer(Modifier.height(16.dp))
            ShareGuideStep(3, "공유 목록에서 AutoVerdict 선택") {
                ShareSheetIllustration()
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onOpenEncar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(999.dp),
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
    val colors = AvTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accent),
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
                color = colors.textPrimary,
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
    val colors = AvTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(colors.border.copy(alpha = 0.6f)),
        )
        Text(
            text = "또는 링크 직접 입력",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(colors.border.copy(alpha = 0.6f)),
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
    val colors = AvTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.border),
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
                        tint = colors.accentText,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "복사한 링크를 가져왔어요",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.accentText,
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
                        tint = if (isError) colors.dangerText else colors.textSecondary,
                    )
                },
                trailingIcon = if (urlText.isNotEmpty()) {
                    {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "입력 지우기",
                                tint = colors.textSecondary,
                            )
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("올바른 엔카 매물 URL을 입력해주세요") }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    cursorColor = colors.accent,
                    errorBorderColor = colors.dangerFill,
                    errorSupportingTextColor = colors.dangerText,
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
                    containerColor = colors.accent,
                    disabledContainerColor = colors.surfaceMuted,
                    disabledContentColor = colors.textTertiary,
                ),
                shape = RoundedCornerShape(999.dp),
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
    val colors = AvTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "최근 조회",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "24시간 이내",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun RecentItemRow(item: CacheEntity, onClick: () -> Unit) {
    val colors = AvTheme.colors
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.accentWeak),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        headlineContent = {
            Text(
                text = item.title.ifBlank { "매물 #${item.carId}" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = dateFormat.format(Date(item.cachedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
