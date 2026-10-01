package com.burton.weather.ui.radar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.weather.data.radar.RainViewer
import com.burton.weather.data.repository.WeatherRepository
import com.burton.weather.domain.RadarCatalog
import com.burton.weather.domain.SavedCity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadarUi(
    val ready: Boolean = false,
    val playing: Boolean = true,
    val frameIndex: Int = 0,
    val catalog: RadarCatalog? = null,
    val focusCityId: String? = null,
    val error: String? = null,
) {
    val frame get() = catalog?.frames?.getOrNull(frameIndex)
    val templates: List<String>
        get() = catalog?.frames.orEmpty().map { RainViewer.tileTemplate(catalog!!.host, it.path) }
}

@HiltViewModel
class RadarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WeatherRepository,
) : ViewModel() {
    val snapshot = repository.state
    private val initialCity = savedStateHandle.get<String>("cityId")?.takeIf { it.isNotBlank() }
    private val _ui = MutableStateFlow(RadarUi(focusCityId = initialCity))
    val ui: StateFlow<RadarUi> = _ui.asStateFlow()
    private var ticker: Job? = null

    init {
        refresh()
        startTicker()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(error = null) }
            runCatching { repository.radarCatalog() }
                .onSuccess { catalog ->
                    _ui.update { ui ->
                        ui.copy(
                            ready = true,
                            catalog = catalog,
                            frameIndex = (catalog.frames.lastIndex).coerceAtLeast(0),
                        )
                    }
                }
                .onFailure { err ->
                    _ui.update { it.copy(ready = true, error = err.message ?: "Radar failed") }
                }
        }
    }

    fun togglePlay() {
        _ui.update { it.copy(playing = !it.playing) }
        startTicker()
    }

    fun setFrame(index: Int) {
        val last = _ui.value.catalog?.frames?.lastIndex ?: return
        _ui.update { it.copy(frameIndex = index.coerceIn(0, last)) }
    }

    fun focus(city: SavedCity) {
        _ui.update { it.copy(focusCityId = city.id) }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(500)
                val ui = _ui.value
                val last = ui.catalog?.frames?.lastIndex ?: continue
                if (!ui.playing || last <= 0) continue
                val next = if (ui.frameIndex >= last) 0 else ui.frameIndex + 1
                _ui.update { it.copy(frameIndex = next) }
            }
        }
    }
}
