package com.burton.weather

import com.burton.weather.data.geocode.Geocoder
import com.burton.weather.data.parse.TinyJson
import com.burton.weather.data.radar.CartoBasemap
import com.burton.weather.data.radar.RainViewer
import com.burton.weather.data.repository.CityCodec
import com.burton.weather.data.repository.StoredState
import com.burton.weather.data.weather.OpenMeteo
import com.burton.weather.domain.SavedCity
import com.burton.weather.domain.TempUnit
import com.burton.weather.domain.Units
import com.burton.weather.domain.WeatherCodes
import com.burton.weather.domain.WindUnit
import com.burton.weather.domain.cityId
import com.burton.weather.domain.compassLabel
import com.burton.weather.domain.formatTemperature
import com.burton.weather.domain.formatWind
import com.burton.weather.domain.toFahrenheit
import com.burton.weather.domain.usAqiLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TinyJsonTest {
    @Test
    fun roundTripObject() {
        val json = TinyJson.stringify(mapOf("name" to "Paris", "n" to 12, "ok" to true))
        val parsed = TinyJson.parseObject(json)
        assertEquals("Paris", parsed["name"])
        assertEquals(12L, parsed["n"])
        assertEquals(true, parsed["ok"])
    }
}

class WeatherCodesTest {
    @Test
    fun labelsKnownCodes() {
        assertEquals("Clear", WeatherCodes.label(0))
        assertEquals("Thunderstorm", WeatherCodes.label(95))
        assertTrue(WeatherCodes.isSnow(73))
        assertTrue(WeatherCodes.isPrecip(61))
    }
}

class UnitsTest {
    @Test
    fun converts() {
        assertEquals(32.0, 0.0.toFahrenheit(), 0.01)
        assertEquals("32°", formatTemperature(0.0, TempUnit.F))
        assertEquals("10 km/h", formatWind(10.0, WindUnit.Kmh))
        assertEquals("N", compassLabel(0))
        assertEquals("Good", usAqiLabel(12))
    }

    @Test
    fun cityIdsPreferOpenMeteo() {
        assertEquals("om-12", cityId(1.0, 2.0, 12))
        assertTrue(cityId(48.8566, 2.3522, null).startsWith("geo-"))
    }
}

class GeocoderTest {
    @Test
    fun parseHits() {
        val json = """
            {"results":[
              {"id":2988507,"name":"Paris","latitude":48.85341,"longitude":2.3488,"country":"France","admin1":"Île-de-France","timezone":"Europe/Paris","population":2138551},
              {"name":"","latitude":0,"longitude":0}
            ]}
        """.trimIndent()
        val hits = Geocoder.parse(json)
        assertEquals(1, hits.size)
        assertEquals("Paris", hits[0].name)
        assertEquals("om-2988507", hits[0].id)
        assertEquals("Île-de-France, France", hits[0].placeLine)
    }
}

class OpenMeteoTest {
    @Test
    fun parseForecastAndAir() {
        val forecast = """
            {
              "current": {
                "time":"2026-10-01T12:00",
                "temperature_2m":12.4,
                "apparent_temperature":11.0,
                "relative_humidity_2m":64,
                "is_day":1,
                "precipitation":0.2,
                "rain":0.2,
                "snowfall":0,
                "weather_code":61,
                "cloud_cover":80,
                "pressure_msl":1012.5,
                "wind_speed_10m":14.2,
                "wind_direction_10m":220,
                "wind_gusts_10m":28.0
              },
              "hourly": {
                "time":["2026-10-01T12:00"],
                "temperature_2m":[12.4],
                "apparent_temperature":[11.0],
                "relative_humidity_2m":[64],
                "weather_code":[61],
                "precipitation":[0.2],
                "precipitation_probability":[40],
                "wind_speed_10m":[14.2],
                "cloud_cover":[80],
                "uv_index":[3.2],
                "visibility":[12000],
                "dew_point_2m":[6.1]
              },
              "daily": {
                "time":["2026-10-01"],
                "weather_code":[61],
                "temperature_2m_max":[15.0],
                "temperature_2m_min":[8.0],
                "apparent_temperature_max":[14.0],
                "apparent_temperature_min":[7.0],
                "sunrise":["2026-10-01T07:12"],
                "sunset":["2026-10-01T18:44"],
                "uv_index_max":[4.1],
                "precipitation_sum":[2.4],
                "precipitation_probability_max":[70],
                "wind_speed_10m_max":[22.0],
                "wind_gusts_10m_max":[36.0],
                "wind_direction_10m_dominant":[210]
              }
            }
        """.trimIndent()
        val air = """
            {"current":{"us_aqi":41,"european_aqi":22,"pm2_5":8.1,"pm10":12.0,"ozone":40,"nitrogen_dioxide":9,"sulphur_dioxide":1,"carbon_monoxide":140}}
        """.trimIndent()
        val bundle = OpenMeteo.parseForecast("om-1", forecast, air, 1L)
        assertEquals(12.4, bundle.current.temperatureC, 0.01)
        assertEquals(61, bundle.current.weatherCode)
        assertEquals(3.2, bundle.current.uvIndex!!, 0.01)
        assertEquals(1, bundle.hourly.size)
        assertEquals(15.0, bundle.daily[0].highC, 0.01)
        assertEquals(41, bundle.air?.usAqi)
    }
}

class CartoBasemapTest {
    @Test
    fun matchesCartoHosts() {
        assertTrue(CartoBasemap.isCartoHost("a.basemaps.cartocdn.com"))
        assertTrue(CartoBasemap.isCartoHost("basemaps.cartocdn.com"))
        assertTrue(!CartoBasemap.isCartoHost("tilecache.rainviewer.com"))
    }
}

class RainViewerTest {
    @Test
    fun parseCatalog() {
        val json = """
            {
              "version":"2.0.0",
              "generated":1710000000,
              "host":"https://tilecache.rainviewer.com",
              "radar":{
                "past":[{"time":1710000000,"path":"/v2/radar/1710000000"}],
                "nowcast":[{"time":1710000600,"path":"/v2/radar/1710000600"}]
              }
            }
        """.trimIndent()
        val catalog = RainViewer.parse(json)
        assertEquals(2, catalog.frames.size)
        assertEquals(
            "https://tilecache.rainviewer.com/v2/radar/1710000000/256/{z}/{x}/{y}/2/1_1.png",
            RainViewer.tileTemplate(catalog.host, catalog.frames[0].path),
        )
    }
}

class CityCodecTest {
    @Test
    fun roundTrip() {
        val state = StoredState(
            cities = listOf(
                SavedCity("om-1", "Paris", "Île-de-France", "France", 48.85, 2.35, "Europe/Paris"),
            ),
            units = Units(TempUnit.F, WindUnit.Mph),
        )
        val decoded = CityCodec.decode(CityCodec.encode(state))
        assertEquals(1, decoded.cities.size)
        assertEquals("Paris", decoded.cities[0].name)
        assertEquals(TempUnit.F, decoded.units.temperature)
        assertEquals(WindUnit.Mph, decoded.units.wind)
    }

    @Test
    fun emptyJson() {
        val decoded = CityCodec.decode("")
        assertTrue(decoded.cities.isEmpty())
        assertEquals(TempUnit.F, decoded.units.temperature)
    }

    @Test
    fun missingUnitsDefaultToFahrenheit() {
        val decoded = CityCodec.decode("""{"cities":[]}""")
        assertEquals(TempUnit.F, decoded.units.temperature)
        assertEquals(WindUnit.Kmh, decoded.units.wind)
    }

    @Test
    fun explicitCelsiusIsPreserved() {
        val decoded = CityCodec.decode("""{"cities":[],"units":{"fahrenheit":false,"mph":false}}""")
        assertEquals(TempUnit.C, decoded.units.temperature)
        assertEquals(WindUnit.Kmh, decoded.units.wind)
    }
}
