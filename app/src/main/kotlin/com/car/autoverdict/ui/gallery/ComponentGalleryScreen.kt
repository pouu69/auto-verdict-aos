package com.car.autoverdict.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvBottomCta
import com.car.autoverdict.ui.components.AvBottomSheet
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvDialog
import com.car.autoverdict.ui.components.AvIllustrationIcon
import com.car.autoverdict.ui.components.AvEmptyState
import com.car.autoverdict.ui.components.AvFactTile
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.components.AvLoadingSteps
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.components.AvTabBar
import com.car.autoverdict.ui.components.AvTextField
import com.car.autoverdict.ui.components.AvToastHost
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.LoadingStep
import com.car.autoverdict.ui.components.StepState
import com.car.autoverdict.ui.components.rememberAvToastState
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone

/** Debug-only visual check of every shared component and state (spec 6). */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ComponentGalleryScreen(onBack: () -> Unit) {
    val colors = AvTheme.colors
    val toast = rememberAvToastState()
    var field by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(MainTab.Home) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AvTopBar(onBack = onBack, title = "컴포넌트 갤러리", subtitle = "debug 전용")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                AvSectionHeader(title = "Button")
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvButton(text = "Primary L", onClick = {})
                    AvButton(text = "Primary M", onClick = {}, size = AvButtonSize.Medium)
                    AvButton(text = "Weak M", onClick = {}, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
                    AvButton(text = "Secondary S", onClick = {}, style = AvButtonStyle.Secondary, size = AvButtonSize.Small)
                    AvButton(text = "Danger M", onClick = {}, style = AvButtonStyle.Danger, size = AvButtonSize.Medium)
                    AvButton(text = "Disabled", onClick = {}, enabled = false, size = AvButtonSize.Medium)
                    AvButton(text = "Loading", onClick = {}, loading = true, size = AvButtonSize.Medium)
                }
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "List row", count = 2, actionLabel = "전체 보기", onAction = {})
                AvListRow(
                    title = "그랜저 IG 2.4",
                    subtitle = "8.2만km · 2,180만원",
                    onClick = {},
                    leading = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = colors.textSecondary) },
                    trailing = { AvBadge(text = "주의", tone = AvTone.Warning) },
                )
                AvListRow(
                    title = "설정 행",
                    onClick = {},
                    trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textTertiary) },
                )
                AvListRow(title = "정적 행 (클릭 없음)", subtitle = "부제는 body2")
                AvSectionDivider()

                AvSectionHeader(title = "Top bar (tab root)")
                AvTopBar(onBack = null, title = "AutoVerdict", subtitle = "탭 루트 변형")
                AvSectionDivider()

                AvSectionHeader(title = "Text field")
                AvTextField(
                    value = field,
                    onValueChange = { field = it },
                    placeholder = "엔카 매물 링크 붙여넣기",
                    leadingIcon = Icons.Rounded.Link,
                    errorText = if (field.isNotEmpty() && !field.contains("encar")) "엔카 매물 링크가 아니에요" else null,
                    onClear = { field = "" },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                AvTextField(
                    value = "https://google.com",
                    onValueChange = {},
                    placeholder = "엔카 매물 링크 붙여넣기",
                    leadingIcon = Icons.Rounded.Link,
                    errorText = "엔카 매물 링크가 아니에요",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                )
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "Badge · Fact tile")
                Row(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvBadge(text = "피하세요", tone = AvTone.Danger)
                    AvBadge(text = "주의", tone = AvTone.Warning)
                    AvBadge(text = "괜찮아요", tone = AvTone.Success)
                    AvBadge(text = "확인 필요", tone = AvTone.Neutral)
                }
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvFactTile(label = "사고", value = "내차 0건", modifier = Modifier.weight(1f))
                    AvFactTile(label = "소유자", value = "3명", tone = AvTone.Warning, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvFactTile(label = "침수·전손", value = "있음", tone = AvTone.Danger, modifier = Modifier.weight(1f))
                    AvFactTile(label = "프레임", value = "무사고", tone = AvTone.Success, modifier = Modifier.weight(1f))
                }
                AvSectionDivider()

                AvSectionHeader(title = "Illustration · Empty state")
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvIllustrationIcon(icon = Icons.Rounded.DirectionsCar, tone = AvTone.Accent, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Shield, tone = AvTone.Success, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Warning, tone = AvTone.Warning, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Block, tone = AvTone.Danger, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Search, tone = AvTone.Neutral, size = 48.dp)
                }
                AvEmptyState(
                    icon = Icons.Rounded.BookmarkBorder,
                    title = "마음에 드는 차를 저장해 보세요",
                    description = "결과 화면에서 저장하면 여기서 비교할 수 있어요",
                    actionLabel = "차 확인하러 가기",
                    onAction = {},
                )
                AvSectionDivider()

                AvSectionHeader(title = "Loading steps")
                AvLoadingSteps(
                    steps = listOf(
                        LoadingStep("매물 정보 가져오기", StepState.Done),
                        LoadingStep("사고·보험 이력 확인 중", StepState.Active),
                        LoadingStep("12가지 기준으로 평가", StepState.Pending),
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "Sheet · Dialog · Toast")
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvButton(text = "시트", onClick = { showSheet = true }, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
                    AvButton(text = "다이얼로그", onClick = { showDialog = true }, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
                    AvButton(
                        text = "토스트",
                        onClick = { toast.show("저장했어요", actionLabel = "보기") },
                        style = AvButtonStyle.Weak,
                        size = AvButtonSize.Medium,
                    )
                }
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "Tab bar")
                AvTabBar(selected = tab, onSelect = { tab = it }, savedCount = 3)
            }
            AvBottomCta {
                AvButton(text = "판매자에게 물어볼 것", onClick = {}, modifier = Modifier.weight(1f))
            }
        }
        AvToastHost(state = toast, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (showSheet) {
        AvBottomSheet(onDismiss = { showSheet = false }, title = "이 판정은 무슨 뜻인가요?") {
            AvListRow(title = "시트 안의 행", subtitle = "드래그해서 닫을 수 있어요")
        }
    }
    if (showDialog) {
        AvDialog(
            title = "캐시를 삭제할까요?",
            message = "저장된 분석 캐시가 모두 삭제돼요.",
            confirmLabel = "삭제",
            onConfirm = { showDialog = false },
            dismissLabel = "취소",
            onDismiss = { showDialog = false },
            destructive = true,
        )
    }
}
