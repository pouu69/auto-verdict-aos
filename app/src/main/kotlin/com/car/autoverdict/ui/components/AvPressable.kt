package com.car.autoverdict.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvPalette
import kotlinx.coroutines.launch

/**
 * Toss-style press feedback: shrinks to 96% for 100ms instead of a ripple, and draws
 * an accent focus ring for keyboard/D-pad focus. Merges descendant semantics like any clickable.
 */
fun Modifier.avPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClickLabel: String? = null,
): Modifier = clickable(
    interactionSource = null,
    indication = AvPressIndication,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick,
)

private object AvPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        AvPressIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = System.identityHashCode(this)
}

private class AvPressIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    private val scale = Animatable(1f)
    private var focused = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> launch { scale.animateTo(PRESSED_SCALE, tween(PRESS_MILLIS)) }
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        launch { scale.animateTo(1f, tween(PRESS_MILLIS)) }
                    is FocusInteraction.Focus -> { focused = true; invalidateDraw() }
                    is FocusInteraction.Unfocus -> { focused = false; invalidateDraw() }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // Reading Animatable.value is a snapshot read, so draw re-runs on every frame of the animation.
        scale(scale.value) { this@draw.drawContent() }
        if (focused) {
            drawRoundRect(
                color = AvPalette.Blue500,
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }

    private companion object {
        const val PRESSED_SCALE = 0.96f
        const val PRESS_MILLIS = 100
    }
}
