package com.burton.weather.data.radar

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object CartoBasemap {
    fun isCartoHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return host == "basemaps.cartocdn.com" || host.endsWith(".basemaps.cartocdn.com")
    }

    fun intercept(context: Context, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url ?: return null
        if (!request.method.equals("GET", ignoreCase = true) || !isCartoHost(url.host)) return null
        val connection = (URL(url.toString()).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            request.requestHeaders.forEach { (name, value) ->
                if (!name.equals("Cookie", ignoreCase = true)) setRequestProperty(name, value)
            }
            setRequestProperty("X-Android-Package", context.packageName)
            setRequestProperty("X-Android-Cert", signingSha1(context))
        }
        return try {
            val code = connection.responseCode
            val stream = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?: ByteArrayInputStream(ByteArray(0))
            val mime = connection.contentType?.substringBefore(';')?.trim().orEmpty().ifBlank { "image/png" }
            val headers = buildMap {
                connection.headerFields.forEach { (name, values) ->
                    if (!name.isNullOrBlank() && !values.isNullOrEmpty()) put(name, values.joinToString(","))
                }
            }
            WebResourceResponse(
                mime,
                connection.contentEncoding,
                code,
                connection.responseMessage?.ifBlank { null } ?: if (code in 200..299) "OK" else "Error",
                headers,
                stream,
            )
        } catch (_: Exception) {
            connection.disconnect()
            null
        }
    }

    fun signingSha1(context: Context): String {
        cachedSha1?.let { return it }
        val digest = MessageDigest.getInstance("SHA-1").digest(signingCert(context))
        val hex = digest.joinToString("") { byte -> "%02X".format(byte) }
        cachedSha1 = hex
        return hex
    }

    @Volatile
    private var cachedSha1: String? = null

    private fun signingCert(context: Context): ByteArray {
        val pm = context.packageManager
        val name = context.packageName
        return if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo
                ?.apkContentsSigners
                ?.firstOrNull()
                ?.toByteArray()
                ?: error("missing signing cert")
        } else {
            @Suppress("DEPRECATION")
            val signatures = pm.getPackageInfo(name, PackageManager.GET_SIGNATURES).signatures
            signatures?.firstOrNull()?.toByteArray() ?: error("missing signing cert")
        }
    }
}
