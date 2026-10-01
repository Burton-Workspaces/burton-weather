package com.burton.weather.data.repository

import com.burton.weather.data.parse.TinyJson
import com.burton.weather.data.parse.TinyJson.bool
import com.burton.weather.data.parse.TinyJson.dbl
import com.burton.weather.data.parse.TinyJson.obj
import com.burton.weather.data.parse.TinyJson.objList
import com.burton.weather.data.parse.TinyJson.str
import com.burton.weather.domain.SavedCity
import com.burton.weather.domain.TempUnit
import com.burton.weather.domain.Units
import com.burton.weather.domain.WindUnit

data class StoredState(
    val cities: List<SavedCity> = emptyList(),
    val units: Units = Units(),
)

object CityCodec {
    fun encode(state: StoredState): String = TinyJson.stringify(
        mapOf(
            "cities" to state.cities.map { city ->
                mapOf(
                    "id" to city.id,
                    "name" to city.name,
                    "admin1" to city.admin1,
                    "country" to city.country,
                    "latitude" to city.latitude,
                    "longitude" to city.longitude,
                    "timezone" to city.timezone,
                )
            },
            "units" to mapOf(
                "fahrenheit" to (state.units.temperature == TempUnit.F),
                "mph" to (state.units.wind == WindUnit.Mph),
            ),
        ),
    )

    fun decode(json: String): StoredState {
        if (json.isBlank()) return StoredState()
        val root = TinyJson.parseObject(json)
        val cities = root.objList("cities").mapNotNull { row ->
            val id = row.str("id")
            val name = row.str("name")
            if (id.isBlank() || name.isBlank()) return@mapNotNull null
            SavedCity(
                id = id,
                name = name,
                admin1 = row.str("admin1"),
                country = row.str("country"),
                latitude = row.dbl("latitude"),
                longitude = row.dbl("longitude"),
                timezone = row.str("timezone"),
            )
        }
        val unitsObj = root.obj("units")
        val units = Units(
            temperature = if (unitsObj.bool("fahrenheit", fallback = true)) TempUnit.F else TempUnit.C,
            wind = if (unitsObj.bool("mph")) WindUnit.Mph else WindUnit.Kmh,
        )
        return StoredState(cities = cities, units = units)
    }
}
