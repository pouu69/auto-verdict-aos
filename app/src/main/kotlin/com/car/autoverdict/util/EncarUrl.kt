package com.car.autoverdict.util

object EncarUrl {
    private val CAR_DETAIL_RE = Regex("""/cars/detail/(\d+)""")
    private const val BASE_URL = "https://fem.encar.com"

    fun extractCarId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return CAR_DETAIL_RE.find(url)?.groupValues?.get(1)
    }

    fun isEncarDetail(url: String?): Boolean = extractCarId(url) != null

    fun buildDetailUrl(carId: String): String = "$BASE_URL/cars/detail/$carId"
}
