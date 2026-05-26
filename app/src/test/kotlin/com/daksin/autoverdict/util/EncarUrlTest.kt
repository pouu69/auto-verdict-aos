package com.daksin.autoverdict.util

import org.junit.Assert.*
import org.junit.Test

class EncarUrlTest {
    @Test fun `extractCarId from standard URL`() = assertEquals("41623743", EncarUrl.extractCarId("https://fem.encar.com/cars/detail/41623743"))
    @Test fun `extractCarId from URL with query params`() = assertEquals("41623743", EncarUrl.extractCarId("https://fem.encar.com/cars/detail/41623743?utm_source=app"))
    @Test fun `extractCarId returns null for non-encar URL`() = assertNull(EncarUrl.extractCarId("https://google.com/search"))
    @Test fun `extractCarId returns null for null input`() = assertNull(EncarUrl.extractCarId(null))
    @Test fun `extractCarId returns null for empty string`() = assertNull(EncarUrl.extractCarId(""))
    @Test fun `isEncarDetail returns true for valid URL`() = assertTrue(EncarUrl.isEncarDetail("https://fem.encar.com/cars/detail/41623743"))
    @Test fun `isEncarDetail returns false for non-detail URL`() = assertFalse(EncarUrl.isEncarDetail("https://fem.encar.com/cars/list"))
    @Test fun `buildDetailUrl creates correct URL`() = assertEquals("https://fem.encar.com/cars/detail/41623743", EncarUrl.buildDetailUrl("41623743"))
}
