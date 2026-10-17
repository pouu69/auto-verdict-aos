package com.car.autoverdict.ui.screen

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.collector.CollectorWebView
import com.car.autoverdict.db.CacheEntity
import com.car.autoverdict.ui.AdBanner
import com.car.autoverdict.ui.AdMediumRectangle
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.util.EncarUrl
import com.car.autoverdict.webview.EvalWebView
import com.car.autoverdict.webview.NativeBridge
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
    // The bridge is created once inside remember{}; read onBack through updated state so
    // "close" always pops the current back stack (spec 7.2).
    val currentOnBack by rememberUpdatedState(onBack)
    val colors = AvTheme.colors

    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("매물 데이터 수집 중") }

    val carId = remember { EncarUrl.extractCarId(url) }

    val evalWebView = remember {
        EvalWebView(context).also { eval ->
            val bridge = NativeBridge(
                database = app.database,
                scope = scope,
                onClose = { currentOnBack() },
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
            statusText = "캐시 데이터 로딩"
            evalWebView.sendData(cached.rawInputJson)
            if (isAlreadySaved) evalWebView.setAlreadySaved(true)
            isLoading = false
            return@LaunchedEffect
        }

        statusText = "엔카 페이지 분석 중"
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
                    is CollectorWebView.Result.Deleted -> {
                        evalWebView.sendDeleted(carId, isAlreadySaved)
                        isLoading = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceMuted)
            .navigationBarsPadding(),
    ) {
        AvTopBar(
            onBack = onBack,
            title = "분석 결과",
            subtitle = if (isAlreadySaved) "저장된 매물" else null,
            actions = {
                OpenInEncarChip(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                )
            },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
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
                LoadingOverlay(statusText = statusText)
            }
        }

        // 로딩 중에는 분석중 화면의 중형 광고(MREC)만 노출하고 하단 배너는 숨겨
        // 광고가 한 화면에 중복으로 쌓이지 않도록 한다.
        if (!isLoading) {
            AdBanner(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OpenInEncarChip(onClick: () -> Unit) {
    val colors = AvTheme.colors
    // minimumInteractiveComponentSize is outermost: it keeps the 48dp touch target (centring the
    // pill inside it) while avPressable sizes to the pill, so the focus ring follows the pill.
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .avPressable(onClick = onClick, focusShape = EncarChipShape)
            .clip(EncarChipShape)
            .background(colors.accentWeak)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "엔카에서 보기",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accentText,
        )
    }
}

private val EncarChipShape = RoundedCornerShape(999.dp)

@Composable
private fun LoadingOverlay(statusText: String) {
    val colors = AvTheme.colors
    val infinite = rememberInfiniteTransition(label = "loadingPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.accentWeak),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                color = colors.accent,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "잠시만 기다려주세요",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.border),
            ) {
                val progress by infinite.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = LinearEasing),
                    ),
                    label = "progressSlide",
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(64.dp)
                        .graphicsLayer { translationX = progress * 96.dp.toPx() }
                        .background(colors.accent),
                )
            }

            Spacer(Modifier.height(32.dp))
            // 분석을 기다리는 동안 노출되는 중형(300x250) 광고.
            AdMediumRectangle()
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
