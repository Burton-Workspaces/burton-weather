package com.burton.weather.ui.cities

import androidx.lifecycle.ViewModel
import com.burton.weather.data.repository.WeatherRepository
import com.burton.weather.domain.CityHit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CitiesViewModel @Inject constructor(
    private val repository: WeatherRepository,
) : ViewModel() {
    val state = repository.state

    fun refreshAll() = repository.refreshAll()

    fun removeCity(cityId: String) = repository.removeCity(cityId)

    fun addCity(hit: CityHit) = repository.addCity(hit)

    fun addDeviceLocation() = repository.addDeviceLocation()

    fun hasLocationPermission() = repository.hasLocationPermission()

    suspend fun search(query: String) = repository.searchCities(query)
}
