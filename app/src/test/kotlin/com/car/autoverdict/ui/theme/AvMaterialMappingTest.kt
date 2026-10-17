package com.car.autoverdict.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AvMaterialMappingTest {

    private val c = LightAvColors
    private val t = DefaultAvTypography

    @Test fun `color scheme maps spec 4_6 slots`() {
        val s = c.toM3ColorScheme()
        assertEquals(c.accent, s.primary)
        assertEquals(c.onAccent, s.onPrimary)
        assertEquals(c.accentWeak, s.primaryContainer)
        assertEquals(c.accentText, s.onPrimaryContainer)
        assertEquals(c.surfaceMuted, s.secondaryContainer)
        assertEquals(c.background, s.background)
        assertEquals(c.background, s.surface)
        assertEquals(c.surfaceMuted, s.surfaceVariant)
        assertEquals(c.textSecondary, s.onSurfaceVariant)
        assertEquals(c.surfaceSubtle, s.surfaceContainer)
        assertEquals(AvPalette.Grey300, s.outline)
        assertEquals(c.border, s.outlineVariant)
        assertEquals(c.dangerText, s.error)
        assertEquals(c.dangerBg, s.errorContainer)
        assertEquals(c.toastBg, s.inverseSurface)
        assertEquals(c.scrim, s.scrim)
    }

    @Test fun `typography maps every M3 slot to an Av token`() {
        val m = t.toM3Typography()
        assertEquals(t.title1, m.displayLarge)
        assertEquals(t.title1, m.headlineMedium)
        assertEquals(t.title2, m.headlineSmall)
        assertEquals(t.title2, m.titleLarge)
        assertEquals(t.body1, m.titleMedium)
        assertEquals(t.buttonM, m.titleSmall)
        assertEquals(t.body2, m.bodyLarge)
        assertEquals(t.body2, m.bodyMedium)
        assertEquals(t.caption1, m.bodySmall)
        assertEquals(t.buttonL, m.labelLarge)
        assertEquals(t.caption1, m.labelMedium)
        assertEquals(t.caption2, m.labelSmall)
    }
}
