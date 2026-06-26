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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.Border
import com.car.autoverdict.ui.theme.Primary
import com.car.autoverdict.ui.theme.TextPrimary
import com.car.autoverdict.ui.theme.TextSecondary

/**
 * A simplified, code-drawn mock of the Android share sheet that highlights the
 * AutoVerdict tile. Used on the home screen and onboarding to teach the
 * "Encar page → Share → AutoVerdict" flow without shipping a screenshot asset
 * (theme- and density-adaptive, zero binary weight).
 */
@Composable
fun ShareSheetIllustration(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, Border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "공유 시트에서 이렇게 보여요",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            DimmedAppTile(icon = Icons.Outlined.ChatBubbleOutline, label = "메신저")
            AutoVerdictTile()
            DimmedAppTile(icon = Icons.Outlined.Sms, label = "메시지")
        }
    }
}

@Composable
private fun DimmedAppTile(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Border.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun AutoVerdictTile() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Primary)
                .border(3.dp, Primary.copy(alpha = 0.22f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "AV",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "AutoVerdict",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Primary,
        )
        Text(
            text = "← 이것만 탭!",
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
        )
    }
}
