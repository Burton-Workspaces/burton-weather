package com.burton.weather.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.burton.weather.domain.WeatherCodes

fun weatherGlyph(code: Int): ImageVector = when {
    WeatherCodes.isStorm(code) -> Icons.Rounded.Thunderstorm
    WeatherCodes.isSnow(code) -> Icons.Rounded.AcUnit
    WeatherCodes.isPrecip(code) && code in 51..57 -> Icons.Rounded.Grain
    WeatherCodes.isPrecip(code) -> Icons.Rounded.WaterDrop
    code == 45 || code == 48 -> Icons.Rounded.Dehaze
    code == 3 -> Icons.Rounded.Cloud
    code == 2 -> Icons.Rounded.WbCloudy
    else -> Icons.Rounded.WbSunny
}
