package com.car.autoverdict.ui

import com.car.autoverdict.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdIdsTest {
    @Test fun `placements use the inventory for the current build`() {
        val publisher = if (BuildConfig.DEBUG) "3940256099942544" else "5353705048338468"
        listOf(AdIds.banner, AdIds.resultNative, AdIds.appOpen).forEach {
            assertTrue(requireNotNull(it).startsWith("ca-app-pub-$publisher/"))
        }
    }

    @Test fun `debug cannot accidentally select a production unit`() {
        assertEquals("demo", selectAdUnit(true, "demo", "ca-app-pub-5353705048338468/4066384469"))
    }

    @Test fun `release configuration accepts actual publisher units only`() {
        assertEquals("ca-app-pub-5353705048338468/4066384469", selectAdUnit(false, "demo", "ca-app-pub-5353705048338468/4066384469"))
        assertNull(selectAdUnit(false, "demo", ""))
        assertNull(selectAdUnit(false, "demo", "ca-app-pub-3940256099942544/9214589741"))
        assertNull(selectAdUnit(false, "demo", "invalid"))
    }
}
