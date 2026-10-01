package com.burton.weather.data.radar

import com.burton.weather.data.parse.TinyJson
import com.burton.weather.data.parse.TinyJson.long
import com.burton.weather.data.parse.TinyJson.obj
import com.burton.weather.data.parse.TinyJson.objList
import com.burton.weather.data.parse.TinyJson.str
import com.burton.weather.domain.RadarCatalog
import com.burton.weather.domain.RadarFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RainViewer @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun catalog(): RadarCatalog = withContext(Dispatchers.IO) {
        val body = client.newCall(
            Request.Builder().url("https://api.rainviewer.com/public/weather-maps.json").build(),
        ).execute().use { response ->
            if (!response.isSuccessful) error("Radar catalog failed (${response.code})")
            response.body?.string().orEmpty()
        }
        parse(body)
    }

    companion object {
        fun parse(json: String): RadarCatalog {
            val root = TinyJson.parseObject(json)
            val host = root.str("host").ifBlank { "https://tilecache.rainviewer.com" }
            val radar = root.obj("radar")
            val frames = (radar.objList("past") + radar.objList("nowcast")).mapNotNull { row ->
                val path = row.str("path")
                val time = row.long("time")
                if (path.isBlank() || time == 0L) null else RadarFrame(timeUnix = time, path = path)
            }
            return RadarCatalog(
                host = host.trimEnd('/'),
                frames = frames,
                generatedUnix = root.long("generated"),
            )
        }

        fun tileTemplate(host: String, path: String): String =
            "${host.trimEnd('/')}${if (path.startsWith("/")) path else "/$path"}/256/{z}/{x}/{y}/2/1_1.png"
    }
}
