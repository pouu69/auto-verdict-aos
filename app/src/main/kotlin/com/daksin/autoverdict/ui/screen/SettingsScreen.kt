package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.BuildConfig
import com.daksin.autoverdict.ui.theme.Danger
import com.daksin.autoverdict.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onPrivacyPolicy: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheCount by remember { mutableStateOf(0) }
    var showCacheClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val app = context.applicationContext as AutoVerdictApp
        cacheCount = app.database.cacheDao().count()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "설정",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Cache management
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("캐시 관리", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("캐시 항목", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${cacheCount}개", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("캐시 유효기간", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("24시간", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (showCacheClearConfirm) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${cacheCount}개 항목을 삭제하시겠습니까?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showCacheClearConfirm = false }) {
                                Text("취소", color = TextSecondary)
                            }
                            TextButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val app = context.applicationContext as AutoVerdictApp
                                        app.database.cacheDao().clearAll()
                                        cacheCount = 0
                                        showCacheClearConfirm = false
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "캐시가 삭제되었습니다",
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                },
                            ) {
                                Text("삭제", color = Danger)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = { showCacheClearConfirm = true },
                            enabled = cacheCount > 0,
                        ) {
                            Text(
                                "캐시 삭제",
                                color = if (cacheCount > 0) Danger else TextSecondary,
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Privacy policy
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPrivacyPolicy)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "개인정보 처리방침",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    ">",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "AutoVerdict v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        content()
    }
}
