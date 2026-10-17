package com.car.autoverdict.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvPalette
import kotlinx.coroutines.launch

/**
 * Toss-style press feedback: shrinks to 96% for 100ms instead of a ripple, and draws
 * an accent focus ring for keyboard/D-pad focus. Merges descendant semantics like any clickable.
 *
 * The ring sits outside the element with a gap, so it stays visible on accent-filled
 * surfaces. It follows [focusShape] (the element's visual shape); null means a 12dp rounded rect.
 * Parents that clip (scroll containers) can cut the ring at their edges.
 */
fun Modifier.avPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    focusShape: Shape? = null,
): Modifier = clickable(
    interactionSource = null,
    indication = AvPressIndication(focusShape),
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick,
)

/** Data class so equal shapes compare equal and clickable reuses the node across recompositions. */
internal data class AvPressIndication(val focusShape: Shape?) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        AvPressIndicationNode(interactionSource, focusShape ?: DEFAULT_FOCUS_SHAPE)

    private companion object {
        val DEFAULT_FOCUS_SHAPE = RoundedCornerShape(12.dp)
    }
}

private class AvPressIndicationNode(
    private val interactionSource: InteractionSource,
    private val focusShape: Shape,
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
        if (focused) drawFocusRing()
    }

    /** Stroke centred [RING_GAP] + half its width outside the bounds, so the clear gap is exactly [RING_GAP]. */
    private fun ContentDrawScope.drawFocusRing() {
        val strokeWidth = RING_WIDTH.toPx()
        val offset = RING_GAP.toPx() + strokeWidth / 2
        val outline = ringOutline(offset)
        translate(-offset, -offset) {
            drawOutline(outline, color = AvPalette.Blue500, style = Stroke(width = strokeWidth))
        }
    }

    // Outline cache: only rebuilt when the inputs to createOutline change, not on every draw frame.
    private var cachedOutline: Outline? = null
    private var cachedSize = Size.Unspecified
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedDensity = 0f
    private var cachedFontScale = 0f

    private fun ContentDrawScope.ringOutline(offset: Float): Outline {
        cachedOutline?.let { outline ->
            if (size == cachedSize && layoutDirection == cachedLayoutDirection &&
                density == cachedDensity && fontScale == cachedFontScale
            ) return outline
        }
        val ringSize = Size(size.width + offset * 2, size.height + offset * 2)
        return focusShape.grownBy(offset).createOutline(ringSize, layoutDirection, this).also {
            cachedOutline = it
            cachedSize = size
            cachedLayoutDirection = layoutDirection
            cachedDensity = density
            cachedFontScale = fontScale
        }
    }

    private companion object {
        const val PRESSED_SCALE = 0.96f
        const val PRESS_MILLIS = 100
        val RING_GAP = 3.dp
        val RING_WIDTH = 2.dp
    }
}

/**
 * Grows each corner radius by [offsetPx] so the ring stays concentric with the element
 * (works for dp and percent corners alike). Non-corner shapes are used as-is.
 */
private fun Shape.grownBy(offsetPx: Float): Shape {
    if (this !is CornerBasedShape) return this
    fun CornerSize.grown() = object : CornerSize {
        override fun toPx(shapeSize: Size, density: Density): Float {
            val inner = Size(shapeSize.width - offsetPx * 2, shapeSize.height - offsetPx * 2)
            return this@grown.toPx(inner, density) + offsetPx
        }
    }
    return copy(
        topStart = topStart.grown(),
        topEnd = topEnd.grown(),
        bottomEnd = bottomEnd.grown(),
        bottomStart = bottomStart.grown(),
    )
}
