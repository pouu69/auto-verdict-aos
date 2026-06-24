package com.car.autoverdict.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.autoverdict.ui.theme.Background
import com.car.autoverdict.ui.theme.Border
import com.car.autoverdict.ui.theme.Primary
import com.car.autoverdict.ui.theme.TextPrimary
import com.car.autoverdict.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val tag: String,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.AutoAwesome,
        tag = "AUTO ANALYSIS",
        title = "엔카 매물,\n자동으로 분석해드려요",
        description = "사고 이력, 정비 기록, 진단 정보를\n12가지 규칙으로 한 번에 평가합니다.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.IosShare,
        tag = "QUICK SHARE",
        title = "공유 한 번으로\n바로 분석 시작",
        description = "엔카 페이지에서 공유 버튼을 탭하고\nAutoVerdict를 선택하기만 하면 끝.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.Verified,
        tag = "VERDICT SCORE",
        title = "점수로 보는\n한눈에 매물 등급",
        description = "위험·주의·양호 카운트와 종합 점수로\n좋은 매물을 빠르게 찾고 저장해보세요.",
    ),
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // Skip button (always reserved space to keep layout stable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isLastPage) {
                TextButton(onClick = onComplete) {
                    Text(
                        text = "건너뛰기",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            repeat(pages.size) { index ->
                val isActive = index == pagerState.currentPage
                val width by animateDpAsState(
                    targetValue = if (isActive) 28.dp else 8.dp,
                    animationSpec = tween(280),
                    label = "indicatorWidth",
                )
                Box(
                    modifier = Modifier
                        .size(width = width, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Primary else Border),
                )
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLastPage) {
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = if (isLastPage) "시작하기" else "다음",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            if (!isLastPage) {
                Spacer(Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Hero icon with halo
        Box(
            modifier = Modifier
                .size(176.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Outer halo
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.18f),
                                Primary.copy(alpha = 0.0f),
                            ),
                        ),
                    ),
            )
            // Inner card
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Primary, Primary.copy(alpha = 0.85f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        // Tag chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Primary.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = page.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                ),
                color = Primary,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
