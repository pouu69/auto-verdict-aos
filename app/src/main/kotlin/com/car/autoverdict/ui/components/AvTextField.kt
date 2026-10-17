package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Filled single-line field. Placeholder uses textTertiary on surfaceMuted — the one
 * allowed exception in the spec's contrast table (4.2).
 *
 * With [actionLabel] and [onAction] an empty field shows a Weak S button on the right
 * (e.g. paste); once there is text, the clear icon takes that spot as before.
 */
@Composable
fun AvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    errorText: String? = null,
    onClear: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = typography.body1.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (errorText != null) error(errorText) },
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(AvTheme.shapes.input)
                        .background(colors.surfaceMuted)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.weight(1f).padding(vertical = 14.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = typography.body1,
                                color = colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    if (actionLabel != null && onAction != null && value.isEmpty()) {
                        AvButton(
                            text = actionLabel,
                            onClick = onAction,
                            style = AvButtonStyle.Weak,
                            size = AvButtonSize.Small,
                        )
                    }
                    if (onClear != null && value.isNotEmpty()) {
                        AvIconButton(
                            icon = Icons.Rounded.Cancel,
                            contentDescription = stringResource(R.string.cd_clear),
                            onClick = onClear,
                            tint = colors.textTertiary,
                        )
                    }
                }
            },
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = typography.caption1,
                color = colors.dangerText,
                modifier = Modifier
                    .padding(start = 4.dp, top = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
