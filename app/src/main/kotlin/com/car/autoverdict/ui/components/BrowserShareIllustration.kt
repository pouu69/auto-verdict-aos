package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.Border
import com.car.autoverdict.ui.theme.Primary
import com.car.autoverdict.ui.theme.TextPrimary
import com.car.autoverdict.ui.theme.TextSecondary

/**
 * Code-drawn mock of a mobile browser's address bar + overflow menu, highlighting
 * the "공유 (Share)" item. Teaches users where the browser share entry point is —
 * the step most people get stuck on — before they reach the system share sheet
 * shown by [ShareSheetIllustration]. Browser-agnostic, with a caption noting that
 * the exact location differs per browser (Chrome top-right ⋮ vs Samsung Internet
 * bottom bar).
 */
@Composable
fun BrowserShareIllustration(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, Border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        // Address bar with the overflow (⋮) button highlighted.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Border.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "fem.encar.com/cars/detail/...",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary)
                    .border(3.dp, Primary.copy(alpha = 0.22f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "메뉴를 열면 ↓",
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.height(6.dp))

        // Overflow menu with the "공유" row highlighted.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(0.5.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        ) {
            BrowserMenuRow(icon = Icons.Outlined.StarOutline, label = "즐겨찾기", highlighted = false)
            BrowserMenuRow(icon = Icons.Outlined.IosShare, label = "공유", highlighted = true)
            BrowserMenuRow(icon = Icons.Outlined.Print, label = "인쇄", highlighted = false)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "크롬은 우측 상단 ⋮ · 삼성 인터넷은 하단 메뉴에 있어요",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BrowserMenuRow(icon: ImageVector, label: String, highlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) Primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) Primary else TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (highlighted) Primary else TextPrimary.copy(alpha = 0.6f),
        )
        if (highlighted) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "← 이것 탭!",
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
            )
        }
    }
}
