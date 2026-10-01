package com.burton.weather.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.weather.domain.DailyPoint
import com.burton.weather.domain.HourlyPoint
import com.burton.weather.domain.Units
import com.burton.weather.domain.WeatherBundle
import com.burton.weather.domain.WeatherCodes
import com.burton.weather.domain.compassLabel
import com.burton.weather.domain.formatTemperature
import com.burton.weather.domain.formatTemperatureExact
import com.burton.weather.domain.formatVisibility
import com.burton.weather.domain.formatWind
import com.burton.weather.domain.usAqiLabel
import com.burton.weather.ui.components.RoomsSkeleton
import com.burton.weather.ui.components.weatherGlyph
import com.burton.weather.ui.theme.BurtonCharcoal
import com.burton.weather.ui.theme.BurtonDanger
import com.burton.weather.ui.theme.BurtonIvory
import com.burton.weather.ui.theme.BurtonMute
import com.burton.weather.ui.theme.BurtonSand
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onRadar: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val city = snapshot.city(viewModel.cityId)
    val bundle = snapshot.bundle(viewModel.cityId)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onRadar(viewModel.cityId) }) {
                Icon(Icons.Rounded.Radar, contentDescription = "Radar", tint = BurtonIvory)
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = BurtonIvory)
            }
            IconButton(onClick = {
                viewModel.remove()
                onBack()
            }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Remove city", tint = BurtonDanger)
            }
        }
        if (city == null) {
            Text("This city is no longer saved.", color = BurtonMute)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(city.name, style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
                if (city.placeLine.isNotBlank()) {
                    Text(city.placeLine, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
                }
                Spacer(Modifier.height(16.dp))
                when {
                    bundle == null && viewModel.cityId in snapshot.refreshing -> RoomsSkeleton(count = 2)
                    bundle == null -> Text("Waiting for forecast.", color = BurtonMute)
                    else -> WeatherBody(bundle = bundle, units = snapshot.units, onRadar = { onRadar(city.id) })
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun WeatherBody(
    bundle: WeatherBundle,
    units: Units,
    onRadar: () -> Unit,
) {
    val current = bundle.current
    val today = bundle.daily.firstOrNull()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(weatherGlyph(current.weatherCode), contentDescription = null, tint = BurtonSand)
        Text(
            formatTemperature(current.temperatureC, units.temperature),
            style = MaterialTheme.typography.displayLarge,
            color = BurtonIvory,
        )
    }
    Text(WeatherCodes.label(current.weatherCode), style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
    Text(
        "Feels like ${formatTemperatureExact(current.apparentC, units.temperature)}",
        style = MaterialTheme.typography.bodyLarge,
        color = BurtonMute,
    )
    if (today != null) {
        Text(
            "High ${formatTemperature(today.highC, units.temperature)}  ·  Low ${formatTemperature(today.lowC, units.temperature)}",
            style = MaterialTheme.typography.bodyLarge,
            color = BurtonMute,
        )
    }
    TextButton(onClick = onRadar) {
        Text("Open radar for this city", color = BurtonSand)
    }
    Spacer(Modifier.height(8.dp))
    MetricGrid(
        listOf(
            "Humidity" to "${current.humidity}%",
            "Wind" to "${formatWind(current.windKmh, units.wind)} ${compassLabel(current.windDirection)}",
            "Gusts" to formatWind(current.windGustKmh, units.wind),
            "Pressure" to "${current.pressureHpa.toInt()} hPa",
            "Clouds" to "${current.cloudCover}%",
            "Precip" to "${"%.1f".format(current.precipitationMm)} mm",
            "Dew point" to (current.dewPointC?.let { formatTemperatureExact(it, units.temperature) } ?: "—"),
            "Visibility" to formatVisibility(current.visibilityM),
            "UV" to (current.uvIndex?.let { "%.1f".format(it) } ?: "—"),
            "Rain" to "${"%.1f".format(current.rainMm)} mm",
            "Snow" to "${"%.1f".format(current.snowfallCm)} cm",
            "Sun" to if (today == null) "—" else "${clock(today.sunrise)} – ${clock(today.sunset)}",
        ),
    )
    bundle.air?.let { air ->
        Spacer(Modifier.height(18.dp))
        SectionTitle("Air quality")
        MetricGrid(
            listOfNotNull(
                "US AQI" to (air.usAqi?.let { "$it · ${usAqiLabel(it)}" } ?: "—"),
                air.europeanAqi?.let { "EU AQI" to it.toString() },
                air.pm25?.let { "PM2.5" to "${"%.1f".format(it)} µg/m³" },
                air.pm10?.let { "PM10" to "${"%.1f".format(it)} µg/m³" },
                air.ozone?.let { "Ozone" to "${"%.0f".format(it)} µg/m³" },
                air.nitrogenDioxide?.let { "NO₂" to "${"%.0f".format(it)} µg/m³" },
                air.sulphurDioxide?.let { "SO₂" to "${"%.0f".format(it)} µg/m³" },
                air.carbonMonoxide?.let { "CO" to "${"%.0f".format(it)} µg/m³" },
                air.grassPollen?.let { "Grass pollen" to "${it.toInt()} grains/m³" },
            ),
        )
    }
    Spacer(Modifier.height(18.dp))
    SectionTitle("Next 24 hours")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        bundle.hourly.take(24).forEach { hour ->
            HourChip(hour, units)
        }
    }
    Spacer(Modifier.height(18.dp))
    SectionTitle("14-day outlook")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        bundle.daily.forEach { day ->
            DayRow(day, units)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun MetricGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(BurtonCharcoal, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = BurtonMute)
                        Spacer(Modifier.height(4.dp))
                        Text(value, style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HourChip(hour: HourlyPoint, units: Units) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .background(BurtonCharcoal, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(clock(hour.time), style = MaterialTheme.typography.labelLarge, color = BurtonMute)
        Spacer(Modifier.height(6.dp))
        Icon(weatherGlyph(hour.weatherCode), contentDescription = null, tint = BurtonSand)
        Spacer(Modifier.height(6.dp))
        Text(formatTemperature(hour.temperatureC, units.temperature), color = BurtonIvory, style = MaterialTheme.typography.titleMedium)
        Text("${hour.precipChance}%", color = BurtonMute, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DayRow(day: DailyPoint, units: Units) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            weekday(day.date),
            modifier = Modifier.width(72.dp),
            color = BurtonIvory,
            style = MaterialTheme.typography.titleMedium,
        )
        Icon(weatherGlyph(day.weatherCode), contentDescription = WeatherCodes.label(day.weatherCode), tint = BurtonSand)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                WeatherCodes.label(day.weatherCode),
                color = BurtonIvory,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${day.precipChance}%  ·  ${"%.1f".format(day.precipMm)} mm  ·  UV ${"%.0f".format(day.uvIndexMax)}",
                color = BurtonMute,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "${formatTemperature(day.highC, units.temperature)} / ${formatTemperature(day.lowC, units.temperature)}",
            color = BurtonIvory,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private val clockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)
private val weekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

private fun clock(value: String): String {
    return runCatching {
        val parsed = if (value.length > 10) LocalDateTime.parse(value) else LocalDate.parse(value).atStartOfDay()
        parsed.format(clockFormatter)
    }.getOrDefault(value.takeLast(5))
}

private fun weekday(value: String): String {
    return runCatching { LocalDate.parse(value.take(10)).format(weekdayFormatter) }.getOrDefault(value)
}
