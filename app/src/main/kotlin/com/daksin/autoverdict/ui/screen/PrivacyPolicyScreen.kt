package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContactMail
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.PublicOff
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daksin.autoverdict.ui.theme.Background
import com.daksin.autoverdict.ui.theme.Border
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextPrimary
import com.daksin.autoverdict.ui.theme.TextSecondary

private data class PolicyEntry(
    val number: Int,
    val icon: ImageVector,
    val title: String,
    val content: String,
)

private val policyEntries = listOf(
    PolicyEntry(
        1, Icons.Outlined.Info, "수집하는 정보",
        "AutoVerdict는 사용자가 입력한 엔카 매물 URL과 해당 매물의 공개 정보(차량 상태, 이력, 가격 등)를 처리합니다. 모든 데이터는 사용자의 기기에만 저장되며, 외부 서버로 전송되지 않습니다.",
    ),
    PolicyEntry(
        2, Icons.Outlined.PrivacyTip, "정보 이용 목적",
        "수집된 정보는 차량 평가 분석 결과를 생성하고, 사용자가 저장한 매물 목록을 관리하는 데에만 사용됩니다.",
    ),
    PolicyEntry(
        3, Icons.Outlined.Storage, "데이터 저장",
        "• 분석 캐시: 최근 분석한 매물 데이터를 24시간 동안 기기 내부에 캐시합니다.\n• 저장 목록: 사용자가 명시적으로 저장한 매물 정보를 기기 내부 데이터베이스에 보관합니다.\n• 캐시 데이터는 설정 화면에서 수동으로 삭제할 수 있습니다.",
    ),
    PolicyEntry(
        4, Icons.Outlined.Lock, "네트워크 통신",
        "앱은 엔카(encar.com) 웹사이트에서 공개된 매물 정보를 조회합니다. 이 과정에서 사용자의 개인정보는 전송되지 않습니다.",
    ),
    PolicyEntry(
        5, Icons.Outlined.PhoneAndroid, "권한 사용",
        "• 인터넷 권한: 엔카 매물 정보를 조회하기 위해 사용합니다.\n• 광고 권한: 배너/전면 광고 표시를 위해 사용합니다.",
    ),
    PolicyEntry(
        6, Icons.Outlined.PublicOff, "제3자 제공",
        "AutoVerdict는 수집한 데이터를 제3자에게 제공하지 않습니다.",
    ),
    PolicyEntry(
        7, Icons.Outlined.ContactMail, "문의",
        "개인정보 처리에 관한 문의는 pouu69@gmail.com 으로 연락해주세요.",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        LargeTopAppBar(
            title = {
                Text(
                    text = "개인정보 처리방침",
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "뒤로",
                        tint = TextPrimary,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = Background,
                scrolledContainerColor = Background,
                titleContentColor = TextPrimary,
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        ) {
            item { PolicyHeroBlock() }

            items(policyEntries) { entry ->
                Spacer(Modifier.height(12.dp))
                PolicyCard(entry)
            }

            item {
                Spacer(Modifier.height(20.dp))
                PolicyFooter()
            }
        }
    }
}

@Composable
private fun PolicyHeroBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Primary.copy(alpha = 0.06f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Policy,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "내 기기에만 저장됩니다",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Primary,
            )
            Text(
                text = "외부 서버로 데이터를 전송하지 않아요",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PolicyCard(entry: PolicyEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "0${entry.number}".takeLast(2),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    ),
                    color = TextSecondary,
                )
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = entry.content,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = TextSecondary,
        )
    }
}

@Composable
private fun PolicyFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Border),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "최종 업데이트",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "2026년 5월 27일",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary,
        )
    }
}
