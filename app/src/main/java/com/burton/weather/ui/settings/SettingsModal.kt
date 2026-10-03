package com.burton.weather.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.weather.BuildConfig
import com.burton.weather.report.BurtonIssues
import com.burton.weather.data.repository.WeatherRepository
import com.burton.weather.domain.TempUnit
import com.burton.weather.domain.WindUnit
import com.burton.weather.ui.components.FullScreenModal
import com.burton.weather.ui.theme.BurtonCharcoal
import com.burton.weather.ui.theme.BurtonIvory
import com.burton.weather.ui.theme.BurtonMute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WeatherRepository,
) : ViewModel() {
    val state = repository.state
    fun cycleTemperature() = repository.cycleTemperature()
    fun cycleWind() = repository.cycleWind()
}

@Composable
fun SettingsModal(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    FullScreenModal(
        onDismiss = onDismiss,
        title = "Settings",
    ) {
        Spacer(Modifier.height(20.dp))
        SettingsRow(
            title = "Cities",
            subtitle = if (snapshot.cities.isEmpty()) "No saved cities" else "${snapshot.cities.size} saved",
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            title = "Temperature",
            subtitle = "Tap to switch °C and °F",
            trailing = if (snapshot.units.temperature == TempUnit.F) "°F" else "°C",
            onClick = viewModel::cycleTemperature,
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            title = "Wind",
            subtitle = "Tap to switch km/h and mph",
            trailing = if (snapshot.units.wind == WindUnit.Mph) "mph" else "km/h",
            onClick = viewModel::cycleWind,
        )
        Spacer(Modifier.height(10.dp))
        val context = LocalContext.current
        SettingsRow(
            title = "Burton Weather",
            subtitle = "About",
            trailing = BuildConfig.VERSION_NAME,
            onLongClick = { BurtonIssues.openNewIssue(context) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .then(
                when {
                    onClick != null && onLongClick != null -> {
                        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    }
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    onLongClick != null -> Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
                    else -> Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyLarge, color = BurtonMute)
        }
    }
}
