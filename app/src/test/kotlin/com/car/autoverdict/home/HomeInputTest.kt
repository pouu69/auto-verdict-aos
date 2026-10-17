package com.car.autoverdict.home

import com.car.autoverdict.home.HomeInput.Paste
import com.car.autoverdict.home.HomeInput.Submit
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeInputTest {

    private val canonical = "https://fem.encar.com/cars/detail/41623743"

    @Test
    fun `submit normalizes share text and query params to the canonical URL`() {
        assertEquals(Submit.Go(canonical), HomeInput.submit(canonical))
        assertEquals(
            Submit.Go(canonical),
            HomeInput.submit("[엔카] 2015 BMW 5시리즈 https://fem.encar.com/cars/detail/41623743?utm_source=app 확인"),
        )
    }

    @Test
    fun `submit rejects list pages and other sites, and blank input`() {
        assertEquals(Submit.Invalid, HomeInput.submit("https://fem.encar.com/cars/list"))
        assertEquals(Submit.Invalid, HomeInput.submit("https://google.com/search"))
        assertEquals(Submit.Blank, HomeInput.submit("   "))
    }

    @Test
    fun `paste distinguishes filled, empty and non-Encar clips`() {
        assertEquals(Paste.Filled(canonical), HomeInput.paste("이 매물 어때? $canonical?x=1"))
        assertEquals(Paste.Empty, HomeInput.paste(null))
        assertEquals(Paste.Empty, HomeInput.paste(" \n"))
        assertEquals(Paste.NotEncar, HomeInput.paste("hello"))
    }
}
