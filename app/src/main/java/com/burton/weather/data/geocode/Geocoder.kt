package com.burton.weather.data.geocode

import com.burton.weather.data.parse.TinyJson
import com.burton.weather.data.parse.TinyJson.dbl
import com.burton.weather.data.parse.TinyJson.int
import com.burton.weather.data.parse.TinyJson.long
import com.burton.weather.data.parse.TinyJson.objList
import com.burton.weather.data.parse.TinyJson.str
import com.burton.weather.domain.CityHit
import com.burton.weather.domain.cityId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Geocoder @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun search(query: String): List<CityHit> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", trimmed)
            .addQueryParameter("count", "10")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()
        val body = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Geocoding failed (${response.code})")
            response.body?.string().orEmpty()
        }
        parse(body)
    }

    companion object {
        fun parse(json: String): List<CityHit> {
            val root = TinyJson.parseObject(json)
            return root.objList("results").mapNotNull { row ->
                val name = row.str("name")
                if (name.isBlank()) return@mapNotNull null
                val latitude = row.dbl("latitude")
                val longitude = row.dbl("longitude")
                CityHit(
                    id = cityId(latitude, longitude, row.long("id").takeIf { it != 0L }),
                    name = name,
                    admin1 = row.str("admin1"),
                    country = row.str("country"),
                    latitude = latitude,
                    longitude = longitude,
                    timezone = row.str("timezone"),
                    population = row.int("population"),
                )
            }
        }
    }
}
