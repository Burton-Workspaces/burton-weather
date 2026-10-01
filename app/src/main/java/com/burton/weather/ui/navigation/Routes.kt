package com.burton.weather.ui.navigation

import android.net.Uri

object Routes {
    const val CITIES = "cities"
    const val RADAR = "radar?cityId={cityId}"
    const val CITY = "city/{cityId}"
    const val CITY_RADAR = "city/{cityId}/radar"

    fun radar(cityId: String? = null): String =
        "radar?cityId=${Uri.encode(cityId.orEmpty())}"

    fun city(cityId: String): String = "city/${Uri.encode(cityId)}"

    fun cityRadar(cityId: String): String = "city/${Uri.encode(cityId)}/radar"
}
