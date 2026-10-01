package com.burton.weather.ui.radar

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.weather.data.parse.TinyJson
import com.burton.weather.domain.SavedCity
import com.burton.weather.ui.theme.BurtonBlack
import com.burton.weather.ui.theme.BurtonCharcoal
import com.burton.weather.ui.theme.BurtonElevated
import com.burton.weather.ui.theme.BurtonIvory
import com.burton.weather.ui.theme.BurtonMute
import com.burton.weather.ui.theme.BurtonSand
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RadarScreen(
    viewModel: RadarViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val focus = snapshot.cities.firstOrNull { it.id == ui.focusCityId } ?: snapshot.cities.firstOrNull()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Radar",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::togglePlay, enabled = ui.catalog != null) {
                Icon(
                    if (ui.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (ui.playing) "Pause" else "Play",
                    tint = BurtonIvory,
                )
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh radar", tint = BurtonIvory)
            }
        }
        Text(
            text = ui.frame?.let { formatRadarTime(it.timeUnix) } ?: ui.error ?: "Loading RainViewer frames",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (snapshot.cities.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                snapshot.cities.forEach { city ->
                    val selected = city.id == focus?.id
                    Text(
                        city.name,
                        color = if (selected) BurtonIvory else BurtonMute,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(
                                if (selected) BurtonElevated else BurtonCharcoal,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { viewModel.focus(city) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .background(BurtonBlack, RoundedCornerShape(20.dp)),
        ) {
            RadarMap(
                templates = ui.templates,
                frameIndex = ui.frameIndex,
                city = focus,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RadarMap(
    templates: List<String>,
    frameIndex: Int,
    city: SavedCity?,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.loadsImagesAutomatically = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = true
                isNestedScrollingEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageReady = true
                        view?.evaluateJavascript("Radar && Radar.resize && Radar.resize()", null)
                    }
                }
                loadUrl("file:///android_asset/radar/index.html")
                onResume()
                resumeTimers()
                webView = this
            }
        },
        update = { view ->
            view.evaluateJavascript("Radar && Radar.resize && Radar.resize()", null)
        },
        onRelease = { view ->
            view.stopLoading()
            view.onPause()
            view.destroy()
            webView = null
            pageReady = false
        },
    )
    LaunchedEffect(pageReady, templates) {
        val view = webView ?: return@LaunchedEffect
        if (!pageReady || templates.isEmpty()) return@LaunchedEffect
        val json = TinyJson.stringify(templates)
        view.evaluateJavascript("Radar.setFrames($json)", null)
    }
    LaunchedEffect(pageReady, frameIndex) {
        val view = webView ?: return@LaunchedEffect
        if (!pageReady) return@LaunchedEffect
        view.evaluateJavascript("Radar.setIndex($frameIndex)", null)
    }
    LaunchedEffect(pageReady, city?.id) {
        val view = webView ?: return@LaunchedEffect
        val target = city ?: return@LaunchedEffect
        if (!pageReady) return@LaunchedEffect
        view.evaluateJavascript("Radar.center(${target.latitude},${target.longitude},7)", null)
    }
}

private val radarClock: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

private fun formatRadarTime(unix: Long): String {
    return Instant.ofEpochSecond(unix).atZone(ZoneId.systemDefault()).format(radarClock)
}
