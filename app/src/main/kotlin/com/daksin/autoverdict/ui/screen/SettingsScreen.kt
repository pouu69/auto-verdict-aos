package com.daksin.autoverdict.ui.screen

import android.content.Context
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.floating.FloatingService
import com.daksin.autoverdict.ui.theme.Danger
import com.daksin.autoverdict.ui.theme.DangerBg
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.Success
import com.daksin.autoverdict.ui.theme.SuccessBg
import com.daksin.autoverdict.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var floatingEnabled by remember { mutableStateOf(false) }
    var a11yEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var cacheCount by remember { mutableStateOf(0) }
    var showCacheClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val app = context.applicationContext as AutoVerdictApp
        cacheCount = app.database.cacheDao().count()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(context)
                a11yEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
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

        // Accessibility service status
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!a11yEnabled) {
                            Modifier.clickable {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("URL 자동 감지", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "브라우저에서 엔카 매물을 자동으로 감지합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                if (a11yEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SuccessBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("활성", style = MaterialTheme.typography.labelMedium, color = Success)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DangerBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("설정 필요", style = MaterialTheme.typography.labelMedium, color = Danger)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

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
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
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

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabledServices.contains(
        "${context.packageName}/.accessibility.AutoVerdictAccessibilityService",
    )
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
