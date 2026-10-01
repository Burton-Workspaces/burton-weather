package com.burton.weather.domain

data class SavedCity(
    val id: String,
    val name: String,
    val admin1: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
) {
    val placeLine: String
        get() = listOf(admin1, country).filter { it.isNotBlank() }.joinToString(", ")
}

data class CityHit(
    val id: String,
    val name: String,
    val admin1: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val population: Int,
) {
    fun toSavedCity() = SavedCity(
        id = id,
        name = name,
        admin1 = admin1,
        country = country,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
    )

    val placeLine: String
        get() = listOf(admin1, country).filter { it.isNotBlank() }.joinToString(", ")
}

data class CurrentWeather(
    val time: String,
    val temperatureC: Double,
    val apparentC: Double,
    val humidity: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val precipitationMm: Double,
    val rainMm: Double,
    val snowfallCm: Double,
    val cloudCover: Int,
    val pressureHpa: Double,
    val windKmh: Double,
    val windGustKmh: Double,
    val windDirection: Int,
    val visibilityM: Double?,
    val uvIndex: Double?,
    val dewPointC: Double?,
)

data class HourlyPoint(
    val time: String,
    val temperatureC: Double,
    val apparentC: Double,
    val humidity: Int,
    val weatherCode: Int,
    val precipitationMm: Double,
    val precipChance: Int,
    val windKmh: Double,
    val cloudCover: Int,
    val uvIndex: Double?,
    val visibilityM: Double?,
    val dewPointC: Double?,
)

data class DailyPoint(
    val date: String,
    val weatherCode: Int,
    val highC: Double,
    val lowC: Double,
    val apparentHighC: Double,
    val apparentLowC: Double,
    val sunrise: String,
    val sunset: String,
    val uvIndexMax: Double,
    val precipMm: Double,
    val precipChance: Int,
    val windMaxKmh: Double,
    val windGustMaxKmh: Double,
    val windDirection: Int,
)

data class AirQuality(
    val usAqi: Int?,
    val europeanAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val ozone: Double?,
    val nitrogenDioxide: Double?,
    val sulphurDioxide: Double?,
    val carbonMonoxide: Double?,
    val grassPollen: Double?,
)

data class WeatherBundle(
    val cityId: String,
    val current: CurrentWeather,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
    val air: AirQuality?,
    val fetchedAtMs: Long,
)

data class RadarFrame(
    val timeUnix: Long,
    val path: String,
)

data class RadarCatalog(
    val host: String,
    val frames: List<RadarFrame>,
    val generatedUnix: Long,
)

data class Units(
    val temperature: TempUnit = TempUnit.F,
    val wind: WindUnit = WindUnit.Kmh,
)

enum class TempUnit { C, F }
enum class WindUnit { Kmh, Mph }

data class WeatherSnapshot(
    val ready: Boolean = false,
    val cities: List<SavedCity> = emptyList(),
    val weather: Map<String, WeatherBundle> = emptyMap(),
    val refreshing: Set<String> = emptySet(),
    val units: Units = Units(),
    val error: String? = null,
) {
    fun city(id: String): SavedCity? = cities.firstOrNull { it.id == id }
    fun bundle(id: String): WeatherBundle? = weather[id]
}

fun cityId(latitude: Double, longitude: Double, remoteId: Long?): String {
    if (remoteId != null && remoteId != 0L) return "om-$remoteId"
    return "geo-${"%.4f".format(java.util.Locale.US, latitude)}_${"%.4f".format(java.util.Locale.US, longitude)}"
}

fun Double.toFahrenheit(): Double = this * 9.0 / 5.0 + 32.0

fun Double.kmhToMph(): Double = this * 0.621371

fun formatTemperature(celsius: Double, unit: TempUnit): String {
    val value = if (unit == TempUnit.F) celsius.toFahrenheit() else celsius
    return "${value.toInt()}°"
}

fun formatTemperatureExact(celsius: Double, unit: TempUnit): String {
    val value = if (unit == TempUnit.F) celsius.toFahrenheit() else celsius
    return "${"%.1f".format(value)}°${if (unit == TempUnit.F) "F" else "C"}"
}

fun formatWind(kmh: Double, unit: WindUnit): String {
    return if (unit == WindUnit.Mph) {
        "${kmh.kmhToMph().toInt()} mph"
    } else {
        "${kmh.toInt()} km/h"
    }
}

fun compassLabel(degrees: Int): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees % 360) / 45.0).toInt() % dirs.size
    return dirs[index]
}

fun formatVisibility(meters: Double?): String {
    if (meters == null) return "—"
    return if (meters >= 1000) "${"%.1f".format(meters / 1000.0)} km" else "${meters.toInt()} m"
}

fun usAqiLabel(aqi: Int?): String {
    if (aqi == null) return "—"
    return when {
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Unhealthy for sensitive groups"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very unhealthy"
        else -> "Hazardous"
    }
}
