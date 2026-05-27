package com.daksin.autoverdict.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.daksin.autoverdict.ui.theme.TextSecondary

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
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
                text = "개인정보 처리방침",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            PolicySection(
                title = "1. 수집하는 정보",
                content = "AutoVerdict는 사용자가 입력한 엔카 매물 URL과 해당 매물의 공개 정보(차량 상태, 이력, 가격 등)를 처리합니다. 모든 데이터는 사용자의 기기에만 저장되며, 외부 서버로 전송되지 않습니다.",
            )
            PolicySection(
                title = "2. 정보 이용 목적",
                content = "수집된 정보는 차량 평가 분석 결과를 생성하고, 사용자가 저장한 매물 목록을 관리하는 데에만 사용됩니다.",
            )
            PolicySection(
                title = "3. 데이터 저장",
                content = "• 분석 캐시: 최근 분석한 매물 데이터를 24시간 동안 기기 내부에 캐시합니다.\n• 저장 목록: 사용자가 명시적으로 저장한 매물 정보를 기기 내부 데이터베이스에 보관합니다.\n• 캐시 데이터는 설정 화면에서 수동으로 삭제할 수 있습니다.",
            )
            PolicySection(
                title = "4. 네트워크 통신",
                content = "앱은 엔카(encar.com) 웹사이트에서 공개된 매물 정보를 조회합니다. 이 과정에서 사용자의 개인정보는 전송되지 않습니다.",
            )
            PolicySection(
                title = "5. 권한 사용",
                content = "• 오버레이 권한: 다른 앱 위에 분석 결과를 표시하기 위해 사용합니다.\n• 알림 권한: 플로팅 서비스 실행 상태를 알리기 위해 사용합니다.\n• 인터넷 권한: 엔카 매물 정보를 조회하기 위해 사용합니다.",
            )
            PolicySection(
                title = "6. 제3자 제공",
                content = "AutoVerdict는 수집한 데이터를 제3자에게 제공하지 않습니다.",
            )
            PolicySection(
                title = "7. 문의",
                content = "개인정보 처리에 관한 문의는 pouu69@gmail.com 으로 연락해주세요.",
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "최종 업데이트: 2026년 5월 27일",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
    )
}
