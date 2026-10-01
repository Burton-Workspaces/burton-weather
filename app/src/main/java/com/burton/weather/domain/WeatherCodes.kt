package com.burton.weather.domain

object WeatherCodes {
    fun label(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45 -> "Fog"
        48 -> "Rime fog"
        51 -> "Light drizzle"
        53 -> "Drizzle"
        55 -> "Heavy drizzle"
        56 -> "Freezing drizzle"
        57 -> "Heavy freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66 -> "Freezing rain"
        67 -> "Heavy freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Light showers"
        81 -> "Showers"
        82 -> "Heavy showers"
        85 -> "Snow showers"
        86 -> "Heavy snow showers"
        95 -> "Thunderstorm"
        96 -> "Thunderstorm with hail"
        99 -> "Severe thunderstorm"
        else -> "Unknown"
    }

    fun isPrecip(code: Int): Boolean = code in 51..99

    fun isSnow(code: Int): Boolean = code in 71..77 || code in 85..86

    fun isStorm(code: Int): Boolean = code >= 95
}
