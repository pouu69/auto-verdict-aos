package com.car.autoverdict.ui.theme

import com.car.autoverdict.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AvToneTest {

    @Test fun `fromVerdict maps the four engine verdicts`() {
        assertEquals(AvTone.Danger, AvTone.fromVerdict("NEVER"))
        assertEquals(AvTone.Warning, AvTone.fromVerdict("CAUTION"))
        assertEquals(AvTone.Success, AvTone.fromVerdict("OK"))
        assertEquals(AvTone.Neutral, AvTone.fromVerdict("UNKNOWN"))
    }

    @Test fun `fromVerdict falls back to Neutral for null and unknown values`() {
        assertEquals(AvTone.Neutral, AvTone.fromVerdict(null))
        assertEquals(AvTone.Neutral, AvTone.fromVerdict("whatever"))
    }

    @Test fun `tone resolves to matching token triple`() {
        val c = LightAvColors
        assertEquals(ToneColors(c.dangerFill, c.dangerText, c.dangerBg), c.tone(AvTone.Danger))
        assertEquals(ToneColors(c.accent, c.accentText, c.accentWeak), c.tone(AvTone.Accent))
        assertEquals(ToneColors(c.neutralFill, c.neutralText, c.neutralBg), c.tone(AvTone.Neutral))
    }

    @Test fun `verdictLabelRes picks the label string per verdict`() {
        assertEquals(R.string.verdict_never, verdictLabelRes("NEVER"))
        assertEquals(R.string.verdict_caution, verdictLabelRes("CAUTION"))
        assertEquals(R.string.verdict_ok, verdictLabelRes("OK"))
        assertEquals(R.string.verdict_unknown, verdictLabelRes(null))
    }
}
