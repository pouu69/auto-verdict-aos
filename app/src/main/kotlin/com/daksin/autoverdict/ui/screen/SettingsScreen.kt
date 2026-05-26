package com.daksin.autoverdict.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.floating.FloatingService
import com.daksin.autoverdict.ui.theme.Danger
import com.daksin.autoverdict.ui.theme.DangerBg
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canDrawOverlays = remember { Settings.canDrawOverlays(context) }
    var floatingEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "설정",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Floating toggle
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("플로팅 버튼", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "화면 위에 분석 버튼 표시",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = floatingEnabled,
                    onCheckedChange = { enabled ->
                        floatingEnabled = enabled
                        val intent = Intent(context, FloatingService::class.java)
                        if (enabled) {
                            context.startForegroundService(intent)
                        } else {
                            context.stopService(intent)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Overlay permission
        if (!canDrawOverlays) {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            )
                            context.startActivity(intent)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("오버레이 권한", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "다른 앱 위에 표시 권한이 필요합니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DangerBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("설정", style = MaterialTheme.typography.labelMedium, color = Danger)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Cache clear
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("캐시 삭제", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "분석 결과 캐시를 모두 삭제합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val app = context.applicationContext as AutoVerdictApp
                            app.database.cacheDao().clearAll()
                        }
                    },
                ) {
                    Text("삭제", color = Danger)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Version info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "AutoVerdict v1.0.0",
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
