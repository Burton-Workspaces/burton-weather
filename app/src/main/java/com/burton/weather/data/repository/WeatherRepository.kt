package com.burton.weather.data.repository

import com.burton.weather.data.geocode.Geocoder
import com.burton.weather.data.location.DeviceLocation
import com.burton.weather.data.radar.RainViewer
import com.burton.weather.data.weather.OpenMeteo
import com.burton.weather.domain.CityHit
import com.burton.weather.domain.RadarCatalog
import com.burton.weather.domain.SavedCity
import com.burton.weather.domain.TempUnit
import com.burton.weather.domain.WeatherSnapshot
import com.burton.weather.domain.WindUnit
import com.burton.weather.domain.cityId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val prefs: LocalPrefs,
    private val openMeteo: OpenMeteo,
    private val geocoder: Geocoder,
    private val rainViewer: RainViewer,
    private val deviceLocation: DeviceLocation,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val persistLock = Mutex()
    private val _state = MutableStateFlow(WeatherSnapshot())
    val state: StateFlow<WeatherSnapshot> = _state.asStateFlow()

    init {
        scope.launch { start() }
    }

    private suspend fun start() {
        val stored = runCatching { prefs.load() }.getOrDefault(StoredState())
        _state.update {
            it.copy(ready = true, cities = stored.cities, units = stored.units)
        }
        refreshAll()
    }

    suspend fun searchCities(query: String): List<CityHit> = geocoder.search(query)

    fun addCity(hit: CityHit) {
        scope.launch {
            val city = hit.toSavedCity()
            _state.update { snap ->
                if (snap.cities.any { it.id == city.id }) snap
                else snap.copy(cities = snap.cities + city, error = null)
            }
            persist()
            refreshCity(city.id)
        }
    }

    fun addDeviceLocation() {
        scope.launch {
            val location = deviceLocation.lastKnown()
            if (location == null) {
                _state.update { it.copy(error = "Location is not available yet") }
                return@launch
            }
            val hit = CityHit(
                id = cityId(location.latitude, location.longitude, null),
                name = "Current location",
                admin1 = "",
                country = "",
                latitude = location.latitude,
                longitude = location.longitude,
                timezone = "",
                population = 0,
            )
            addCity(hit)
        }
    }

    fun hasLocationPermission(): Boolean = deviceLocation.hasPermission()

    fun removeCity(cityId: String) {
        scope.launch {
            _state.update { snap ->
                snap.copy(
                    cities = snap.cities.filterNot { it.id == cityId },
                    weather = snap.weather - cityId,
                    refreshing = snap.refreshing - cityId,
                )
            }
            persist()
        }
    }

    fun refreshAll() {
        val ids = _state.value.cities.map { it.id }
        ids.forEach { refreshCity(it) }
    }

    fun refreshCity(cityId: String) {
        val city = _state.value.city(cityId) ?: return
        scope.launch {
            _state.update { it.copy(refreshing = it.refreshing + cityId, error = null) }
            runCatching { openMeteo.load(city.id, city.latitude, city.longitude) }
                .onSuccess { bundle ->
                    _state.update { snap ->
                        snap.copy(
                            weather = snap.weather + (cityId to bundle),
                            refreshing = snap.refreshing - cityId,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { snap ->
                        snap.copy(
                            refreshing = snap.refreshing - cityId,
                            error = err.message ?: "Weather refresh failed",
                        )
                    }
                }
        }
    }

    fun cycleTemperature() {
        scope.launch {
            _state.update { snap ->
                val next = if (snap.units.temperature == TempUnit.C) TempUnit.F else TempUnit.C
                snap.copy(units = snap.units.copy(temperature = next))
            }
            persist()
        }
    }

    fun cycleWind() {
        scope.launch {
            _state.update { snap ->
                val next = if (snap.units.wind == WindUnit.Kmh) WindUnit.Mph else WindUnit.Kmh
                snap.copy(units = snap.units.copy(wind = next))
            }
            persist()
        }
    }

    suspend fun radarCatalog(): RadarCatalog = rainViewer.catalog()

    private suspend fun persist() {
        persistLock.withLock {
            val snap = _state.value
            prefs.save(StoredState(cities = snap.cities, units = snap.units))
        }
    }
}
