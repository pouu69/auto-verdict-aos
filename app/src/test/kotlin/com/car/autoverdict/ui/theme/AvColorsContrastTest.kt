package com.car.autoverdict.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Locks the "통과" rows of the spec's contrast table (4.2) at WCAG AA 4.5:1.
 * Assertions reference tokens, not hex, so retuning a token re-validates here.
 */
class AvColorsContrastTest {

    private val c = LightAvColors

    private fun channel(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAA(fg: Color, bg: Color, name: String) {
        val r = ratio(fg, bg)
        assertTrue("$name contrast was $r", r >= 4.5)
    }

    @Test fun `status text meets AA on its status background`() {
        assertAA(c.dangerText, c.dangerBg, "danger")
        assertAA(c.warningText, c.warningBg, "warning")
        assertAA(c.successText, c.successBg, "success")
        assertAA(c.neutralText, c.neutralBg, "neutral")
    }

    @Test fun `secondary text meets AA on page and muted surface`() {
        assertAA(c.textSecondary, c.background, "textSecondary/background")
        assertAA(c.textSecondary, c.surfaceMuted, "textSecondary/surfaceMuted")
    }

    @Test fun `tertiary text meets AA on page`() {
        assertAA(c.textTertiary, c.background, "textTertiary/background")
    }

    @Test fun `accent text meets AA on page, muted surface and weak accent`() {
        assertAA(c.accentText, c.background, "accentText/background")
        assertAA(c.accentText, c.surfaceMuted, "accentText/surfaceMuted")
        assertAA(c.accentText, c.accentWeak, "accentText/accentWeak")
    }

    @Test fun `toast action meets AA on toast`() {
        assertAA(c.toastAction, c.toastBg, "toastAction/toastBg")
    }
}
