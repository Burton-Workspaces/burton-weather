package com.burton.weather.data.weather

import com.burton.weather.data.parse.TinyJson
import com.burton.weather.data.parse.TinyJson.bool
import com.burton.weather.data.parse.TinyJson.dbl
import com.burton.weather.data.parse.TinyJson.dblOrNull
import com.burton.weather.data.parse.TinyJson.int
import com.burton.weather.data.parse.TinyJson.intOrNull
import com.burton.weather.data.parse.TinyJson.numList
import com.burton.weather.data.parse.TinyJson.obj
import com.burton.weather.data.parse.TinyJson.str
import com.burton.weather.data.parse.TinyJson.strList
import com.burton.weather.domain.AirQuality
import com.burton.weather.domain.CurrentWeather
import com.burton.weather.domain.DailyPoint
import com.burton.weather.domain.HourlyPoint
import com.burton.weather.domain.WeatherBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteo @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun load(cityId: String, latitude: Double, longitude: Double): WeatherBundle =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val forecastJob = async { get(forecastUrl(latitude, longitude)) }
                val airJob = async { runCatching { get(airUrl(latitude, longitude)) }.getOrNull() }
                val forecastJson = forecastJob.await()
                val airJson = airJob.await()
                parseForecast(cityId, forecastJson, airJson, System.currentTimeMillis())
            }
        }

    private fun get(url: String): String {
        return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Weather request failed (${response.code})")
            response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val CURRENT =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m"
        private const val HOURLY =
            "temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m,relative_humidity_2m,uv_index,visibility,cloud_cover,dew_point_2m"
        private const val DAILY =
            "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant"
        private const val AIR =
            "us_aqi,european_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,grass_pollen"

        fun forecastUrl(latitude: Double, longitude: Double): String =
            "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
                .addQueryParameter("latitude", latitude.toString())
                .addQueryParameter("longitude", longitude.toString())
                .addQueryParameter("current", CURRENT)
                .addQueryParameter("hourly", HOURLY)
                .addQueryParameter("daily", DAILY)
                .addQueryParameter("timezone", "auto")
                .addQueryParameter("forecast_days", "14")
                .build()
                .toString()

        fun airUrl(latitude: Double, longitude: Double): String =
            "https://air-quality-api.open-meteo.com/v1/air-quality".toHttpUrl().newBuilder()
                .addQueryParameter("latitude", latitude.toString())
                .addQueryParameter("longitude", longitude.toString())
                .addQueryParameter("current", AIR)
                .build()
                .toString()

        fun parseForecast(
            cityId: String,
            forecastJson: String,
            airJson: String?,
            fetchedAtMs: Long,
        ): WeatherBundle {
            val root = TinyJson.parseObject(forecastJson)
            val current = root.obj("current")
            val hourly = root.obj("hourly")
            val daily = root.obj("daily")
            val hours = parseHourly(hourly)
            val nowHour = hours.firstOrNull()
            return WeatherBundle(
                cityId = cityId,
                current = CurrentWeather(
                    time = current.str("time"),
                    temperatureC = current.dbl("temperature_2m"),
                    apparentC = current.dbl("apparent_temperature"),
                    humidity = current.int("relative_humidity_2m"),
                    weatherCode = current.int("weather_code"),
                    isDay = current.bool("is_day") || current.int("is_day") == 1,
                    precipitationMm = current.dbl("precipitation"),
                    rainMm = current.dbl("rain"),
                    snowfallCm = current.dbl("snowfall"),
                    cloudCover = current.int("cloud_cover"),
                    pressureHpa = current.dbl("pressure_msl").takeIf { it != 0.0 }
                        ?: current.dbl("surface_pressure"),
                    windKmh = current.dbl("wind_speed_10m"),
                    windGustKmh = current.dbl("wind_gusts_10m"),
                    windDirection = current.int("wind_direction_10m"),
                    visibilityM = nowHour?.visibilityM,
                    uvIndex = nowHour?.uvIndex,
                    dewPointC = nowHour?.dewPointC,
                ),
                hourly = hours,
                daily = parseDaily(daily),
                air = airJson?.let { parseAir(it) },
                fetchedAtMs = fetchedAtMs,
            )
        }

        fun parseAir(json: String): AirQuality {
            val current = TinyJson.parseObject(json).obj("current")
            return AirQuality(
                usAqi = current.intOrNull("us_aqi"),
                europeanAqi = current.intOrNull("european_aqi"),
                pm25 = current.dblOrNull("pm2_5"),
                pm10 = current.dblOrNull("pm10"),
                ozone = current.dblOrNull("ozone"),
                nitrogenDioxide = current.dblOrNull("nitrogen_dioxide"),
                sulphurDioxide = current.dblOrNull("sulphur_dioxide"),
                carbonMonoxide = current.dblOrNull("carbon_monoxide"),
                grassPollen = current.dblOrNull("grass_pollen"),
            )
        }

        private fun parseHourly(hourly: Map<String, Any?>): List<HourlyPoint> {
            val times = hourly.strList("time")
            val temps = hourly.numList("temperature_2m")
            val apparent = hourly.numList("apparent_temperature")
            val humidity = hourly.numList("relative_humidity_2m")
            val codes = hourly.numList("weather_code")
            val precip = hourly.numList("precipitation")
            val chance = hourly.numList("precipitation_probability")
            val wind = hourly.numList("wind_speed_10m")
            val cloud = hourly.numList("cloud_cover")
            val uv = hourly.numList("uv_index")
            val vis = hourly.numList("visibility")
            val dew = hourly.numList("dew_point_2m")
            return times.indices.map { i ->
                HourlyPoint(
                    time = times[i],
                    temperatureC = temps.getOrNull(i) ?: 0.0,
                    apparentC = apparent.getOrNull(i) ?: temps.getOrNull(i) ?: 0.0,
                    humidity = humidity.getOrNull(i)?.toInt() ?: 0,
                    weatherCode = codes.getOrNull(i)?.toInt() ?: 0,
                    precipitationMm = precip.getOrNull(i) ?: 0.0,
                    precipChance = chance.getOrNull(i)?.toInt() ?: 0,
                    windKmh = wind.getOrNull(i) ?: 0.0,
                    cloudCover = cloud.getOrNull(i)?.toInt() ?: 0,
                    uvIndex = uv.getOrNull(i),
                    visibilityM = vis.getOrNull(i),
                    dewPointC = dew.getOrNull(i),
                )
            }
        }

        private fun parseDaily(daily: Map<String, Any?>): List<DailyPoint> {
            val dates = daily.strList("time")
            val codes = daily.numList("weather_code")
            val high = daily.numList("temperature_2m_max")
            val low = daily.numList("temperature_2m_min")
            val appHigh = daily.numList("apparent_temperature_max")
            val appLow = daily.numList("apparent_temperature_min")
            val sunrise = daily.strList("sunrise")
            val sunset = daily.strList("sunset")
            val uv = daily.numList("uv_index_max")
            val precip = daily.numList("precipitation_sum")
            val chance = daily.numList("precipitation_probability_max")
            val wind = daily.numList("wind_speed_10m_max")
            val gust = daily.numList("wind_gusts_10m_max")
            val dir = daily.numList("wind_direction_10m_dominant")
            return dates.indices.map { i ->
                DailyPoint(
                    date = dates[i],
                    weatherCode = codes.getOrNull(i)?.toInt() ?: 0,
                    highC = high.getOrNull(i) ?: 0.0,
                    lowC = low.getOrNull(i) ?: 0.0,
                    apparentHighC = appHigh.getOrNull(i) ?: high.getOrNull(i) ?: 0.0,
                    apparentLowC = appLow.getOrNull(i) ?: low.getOrNull(i) ?: 0.0,
                    sunrise = sunrise.getOrElse(i) { "" },
                    sunset = sunset.getOrElse(i) { "" },
                    uvIndexMax = uv.getOrNull(i) ?: 0.0,
                    precipMm = precip.getOrNull(i) ?: 0.0,
                    precipChance = chance.getOrNull(i)?.toInt() ?: 0,
                    windMaxKmh = wind.getOrNull(i) ?: 0.0,
                    windGustMaxKmh = gust.getOrNull(i) ?: 0.0,
                    windDirection = dir.getOrNull(i)?.toInt() ?: 0,
                )
            }
        }
    }
}
