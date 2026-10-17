package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.tone

/** Decorative illustration: a rounded icon inside a tone-tinted circle (replaces Tossface; spec 6). */
@Composable
fun AvIllustrationIcon(icon: ImageVector, tone: AvTone, size: Dp, modifier: Modifier = Modifier) {
    val toneColors = AvTheme.colors.tone(tone)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(toneColors.bg)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = toneColors.fill, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Centered empty/error state. The optional secondary action sits beside the main one as a
 * quieter Secondary button (e.g. "다시 시도" · "엔카에서 보기").
 */
@Composable
fun AvEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    tone: AvTone = AvTone.Neutral,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AvIllustrationIcon(icon = icon, tone = tone, size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = typography.title3,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (description != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = description, style = typography.body2, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
        val hasAction = actionLabel != null && onAction != null
        val hasSecondary = secondaryActionLabel != null && onSecondaryAction != null
        if (hasAction || hasSecondary) {
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (actionLabel != null && onAction != null) {
                    AvButton(text = actionLabel, onClick = onAction, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    AvButton(
                        text = secondaryActionLabel,
                        onClick = onSecondaryAction,
                        style = AvButtonStyle.Secondary,
                        size = AvButtonSize.Medium,
                    )
                }
            }
        }
    }
}
