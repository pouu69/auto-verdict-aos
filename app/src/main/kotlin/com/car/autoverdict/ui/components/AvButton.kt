package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme

enum class AvButtonStyle { Primary, Weak, Secondary, Danger }

enum class AvButtonSize(val minHeight: Dp, val horizontalPadding: Dp) {
    Large(56.dp, 20.dp),
    Medium(48.dp, 18.dp),
    Small(36.dp, 14.dp),
}

internal fun isAllowedButtonCombination(style: AvButtonStyle, size: AvButtonSize): Boolean =
    !(style == AvButtonStyle.Primary && size == AvButtonSize.Small)

/**
 * Primary is Large/Medium only: white on accent is the spec's one allowed
 * sub-AA pair (3.71:1) and is limited to SemiBold 15sp+ labels.
 * Danger uses the tinted pair (dangerBg + dangerText), never white on dangerFill.
 */
@Composable
fun AvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AvButtonStyle = AvButtonStyle.Primary,
    size: AvButtonSize = AvButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    require(isAllowedButtonCombination(style, size)) { "Primary buttons are Large or Medium only" }
    val colors = AvTheme.colors
    val shapes = AvTheme.shapes
    val typography = AvTheme.typography
    val (container, content) = when {
        !enabled -> colors.surfaceMuted to colors.textDisabled
        style == AvButtonStyle.Primary -> colors.accent to colors.onAccent
        style == AvButtonStyle.Weak -> colors.accentWeak to colors.accentText
        style == AvButtonStyle.Secondary -> colors.surfaceMuted to colors.textSecondary
        else -> colors.dangerBg to colors.dangerText
    }
    val shape = when (size) {
        AvButtonSize.Large -> shapes.buttonL
        AvButtonSize.Medium -> shapes.buttonM
        AvButtonSize.Small -> shapes.buttonS
    }
    val textStyle = when (size) {
        AvButtonSize.Large -> typography.buttonL
        AvButtonSize.Medium -> typography.buttonM
        AvButtonSize.Small -> typography.buttonS
    }
    Box(
        modifier = modifier
            // Small (36dp) still gets a 48dp touch target (spec 6).
            .minimumInteractiveComponentSize()
            .avPressable(onClick = onClick, enabled = enabled && !loading)
            .heightIn(min = size.minHeight)
            .clip(shape)
            .background(container)
            .padding(horizontal = size.horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = content,
            maxLines = 1,
            modifier = Modifier.alpha(if (loading) 0f else 1f),
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = content,
                strokeWidth = 2.dp,
            )
        }
    }
}
