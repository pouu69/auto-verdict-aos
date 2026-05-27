package com.daksin.autoverdict.ui.screen

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.collector.CollectorWebView
import com.daksin.autoverdict.db.CacheEntity
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
import com.daksin.autoverdict.util.EncarUrl
import com.daksin.autoverdict.webview.EvalWebView
import com.daksin.autoverdict.webview.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResultScreen(
    url: String,
    onBack: () -> Unit,
    isAlreadySaved: Boolean = false,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoVerdictApp
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("매물 데이터 수집 중...") }

    val carId = remember { EncarUrl.extractCarId(url) }

    val evalWebView = remember {
        EvalWebView(context).also { eval ->
            val bridge = NativeBridge(
                database = app.database,
                scope = scope,
                onClose = onBack,
                appContext = context.applicationContext,
            )
            eval.addBridge(bridge)
            eval.loadEvalUi()
        }
    }

    val collector = remember { CollectorWebView(context) }

    DisposableEffect(Unit) {
        onDispose {
            evalWebView.destroy()
            collector.destroy()
        }
    }

    LaunchedEffect(url, carId) {
        if (carId == null) {
            isLoading = false
            statusText = "올바른 엔카 URL이 아닙니다"
            return@LaunchedEffect
        }

        val cached = withContext(Dispatchers.IO) {
            app.database.cacheDao().getValid(carId)
        }

        if (cached != null) {
            statusText = "캐시 데이터 로딩..."
            evalWebView.sendData(cached.rawInputJson)
            if (isAlreadySaved) evalWebView.setAlreadySaved(true)
            isLoading = false
            return@LaunchedEffect
        }

        statusText = "엔카 페이지 분석 중..."
        collector.collect(url, carId) { result ->
            scope.launch(Dispatchers.Main) {
                when (result) {
                    is CollectorWebView.Result.Success -> {
                        evalWebView.sendData(result.json)
                        isLoading = false
                        cacheResult(app, carId, url, result.json)
                    }
                    is CollectorWebView.Result.Error -> {
                        evalWebView.sendError(result.message)
                        isLoading = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "닫기",
                )
            }
            Text(
                text = "분석 결과",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary.copy(alpha = 0.1f))
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "엔카에서 보기",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = {
                    evalWebView.webView.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "잠시만 기다려주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

private fun cacheResult(app: AutoVerdictApp, carId: String, url: String, rawInputJson: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val now = System.currentTimeMillis()
        app.database.cacheDao().upsert(
            CacheEntity(
                carId = carId,
                url = url,
                title = "",
                score = 0,
                verdict = "",
                resultJson = "",
                rawInputJson = rawInputJson,
                cachedAt = now,
                expiresAt = now + 24 * 60 * 60 * 1000L,
            ),
        )
    }
}
