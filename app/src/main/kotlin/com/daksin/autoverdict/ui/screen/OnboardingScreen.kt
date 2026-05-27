package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.ui.theme.Border
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: String,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = "AV",
        title = "엔카 매물을 자동으로 분석하세요",
        description = "AutoVerdict는 엔카 중고차 매물의\n사고이력, 정비기록, 진단정보를\n12가지 규칙으로 자동 평가합니다.",
    ),
    OnboardingPage(
        icon = ">>",
        title = "브라우저에서 공유하기",
        description = "브라우저에서 엔카 매물 페이지를 열고\n메뉴의 공유 버튼을 탭한 뒤\n목록에서 AutoVerdict를 선택하세요.",
    ),
    OnboardingPage(
        icon = "100",
        title = "종합 점수로 한눈에 파악",
        description = "위험 요소, 주의 사항, 종합 점수를\n확인하고 마음에 드는 매물은\n저장하여 비교해보세요.",
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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        if (!isLastPage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onComplete) {
                    Text("건너뛰기", color = TextSecondary)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val item = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.icon,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (index == pagerState.currentPage) Primary else Border),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = if (isLastPage) "시작하기" else "다음",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
