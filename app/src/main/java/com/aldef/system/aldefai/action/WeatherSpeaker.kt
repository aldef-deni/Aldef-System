package com.aldef.system.aldefai.action

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Menyusun kalimat cuaca untuk diucapkan ALDEF AI.
 *
 * Sumber data: **Open-Meteo** — layanan cuaca publik gratis, **tanpa API key**
 * dan bukan server milik kita. Butuh internet + izin lokasi (GPS perangkat).
 * Ini satu-satunya cara menyebutkan cuaca nyata tanpa membuka aplikasi lain.
 */
object WeatherSpeaker {

    suspend fun describe(context: Context): String {
        if (!AiLocation.hasPermission(context)) {
            return "Izin lokasi belum diberikan, jadi saya belum bisa menyebutkan cuaca."
        }
        val location = AiLocation.last(context)
            ?: return "Maaf, lokasi belum tersedia, jadi saya belum bisa menyebutkan cuaca."
        return runCatching {
            withContext(Dispatchers.IO) { fetch(location.latitude, location.longitude) }
        }.getOrElse {
            "Maaf, saya tidak bisa mengambil data cuaca sekarang. Pastikan internet aktif."
        }
    }

    private fun fetch(lat: Double, lon: Double): String {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "GET"
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val current = JSONObject(body).getJSONObject("current")
        val temp = current.getDouble("temperature_2m").roundToInt()
        val code = current.getInt("weather_code")
        return "Cuaca saat ini sekitar $temp derajat Celsius, ${condition(code)}."
    }

    /** Kode cuaca WMO → deskripsi Bahasa Indonesia. */
    private fun condition(code: Int): String = when (code) {
        0 -> "cerah"
        1, 2 -> "cerah berawan"
        3 -> "berawan"
        45, 48 -> "berkabut"
        51, 53, 55, 56, 57 -> "gerimis"
        61, 63, 65, 66, 67 -> "hujan"
        71, 73, 75, 77 -> "bersalju"
        80, 81, 82 -> "hujan lokal"
        85, 86 -> "hujan salju"
        95 -> "badai petir"
        96, 99 -> "badai petir disertai hujan es"
        else -> "kondisi tidak diketahui"
    }
}
