package com.burton.weather.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.burton.weather.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WeatherRepository,
) : ViewModel() {
    val cityId: String = savedStateHandle.get<String>("cityId").orEmpty()
    val state = repository.state

    fun refresh() = repository.refreshCity(cityId)

    fun remove() = repository.removeCity(cityId)
}
