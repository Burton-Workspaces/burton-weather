package com.burton.weather.ui.cities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.weather.domain.SavedCity
import com.burton.weather.domain.WeatherBundle
import com.burton.weather.domain.WeatherCodes
import com.burton.weather.domain.formatTemperature
import com.burton.weather.ui.components.RoomsSkeleton
import com.burton.weather.ui.components.weatherGlyph
import com.burton.weather.ui.search.AddCityModal
import com.burton.weather.ui.settings.SettingsModal
import com.burton.weather.ui.theme.BurtonCharcoal
import com.burton.weather.ui.theme.BurtonIvory
import com.burton.weather.ui.theme.BurtonMute
import com.burton.weather.ui.theme.BurtonSand

@Composable
fun CitiesScreen(
    onOpenCity: (String) -> Unit,
    onNeedLocation: (onGranted: () -> Unit) -> Unit,
    viewModel: CitiesViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cities",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add city", tint = BurtonIvory)
            }
            IconButton(onClick = viewModel::refreshAll, enabled = snapshot.cities.isNotEmpty()) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh weather", tint = BurtonIvory)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = BurtonIvory)
            }
        }
        Text(
            text = when {
                !snapshot.ready -> "Restoring saved cities"
                snapshot.cities.isEmpty() -> "Add a city to start a forecast"
                snapshot.refreshing.isNotEmpty() -> "Updating ${snapshot.refreshing.size} cities"
                else -> "${snapshot.cities.size} cities"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        snapshot.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonSand)
        }
        Spacer(Modifier.height(16.dp))
        when {
            !snapshot.ready -> RoomsSkeleton()
            snapshot.cities.isEmpty() -> Text(
                "Nothing saved yet. Search for a city, or use this phone’s location.",
                color = BurtonMute,
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(snapshot.cities, key = { it.id }) { city ->
                        CityCard(
                            city = city,
                            bundle = snapshot.bundle(city.id),
                            refreshing = city.id in snapshot.refreshing,
                            temperatureUnit = snapshot.units.temperature,
                            onClick = { onOpenCity(city.id) },
                            onRemove = { viewModel.removeCity(city.id) },
                        )
                    }
                }
            }
        }
    }
    if (showSettings) {
        SettingsModal(onDismiss = { showSettings = false })
    }
    if (showAdd) {
        AddCityModal(
            onDismiss = { showAdd = false },
            onNeedLocation = onNeedLocation,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun CityCard(
    city: SavedCity,
    bundle: WeatherBundle?,
    refreshing: Boolean,
    temperatureUnit: com.burton.weather.domain.TempUnit,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val current = bundle?.current
    val today = bundle?.daily?.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (current != null) {
            Icon(
                weatherGlyph(current.weatherCode),
                contentDescription = WeatherCodes.label(current.weatherCode),
                tint = BurtonSand,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                city.name,
                style = MaterialTheme.typography.titleLarge,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOfNotNull(
                    city.placeLine.ifBlank { null },
                    when {
                        refreshing -> "Updating"
                        current != null -> WeatherCodes.label(current.weatherCode)
                        else -> "Waiting for forecast"
                    },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (today != null) {
                Text(
                    "H ${formatTemperature(today.highC, temperatureUnit)}  L ${formatTemperature(today.lowC, temperatureUnit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonMute,
                )
            }
        }
        if (current != null) {
            Text(
                formatTemperature(current.temperatureC, temperatureUnit),
                style = MaterialTheme.typography.headlineMedium,
                color = BurtonIvory,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove ${city.name}", tint = BurtonMute)
        }
    }
}
