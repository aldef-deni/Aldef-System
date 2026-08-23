package com.aldef.system.aldefai.action

import android.content.Context
import android.location.Geocoder
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Menyusun kalimat lokasi saat ini untuk diucapkan ALDEF AI.
 *
 * Memakai [Geocoder] bawaan Android untuk mengubah koordinat GPS menjadi nama
 * tempat; bila gagal, menyebut koordinatnya. Tanpa API key.
 */
object LocationSpeaker {

    suspend fun describe(context: Context): String {
        if (!AiLocation.hasPermission(context)) {
            return "Izin lokasi belum diberikan, jadi saya belum bisa menyebutkan lokasi Anda."
        }
        val loc = AiLocation.last(context)
            ?: return "Maaf, lokasi belum tersedia saat ini. Pastikan GPS aktif."
        return withContext(Dispatchers.IO) {
            runCatching { geocode(context, loc) }.getOrElse { coordinates(loc) }
        }
    }

    private fun geocode(context: Context, loc: Location): String {
        @Suppress("DEPRECATION")
        val results = Geocoder(context, Locale("in", "ID"))
            .getFromLocation(loc.latitude, loc.longitude, 1)
        val address = results?.firstOrNull() ?: return coordinates(loc)
        val place = listOfNotNull(
            address.subLocality ?: address.locality,
            address.subAdminArea,
            address.adminArea
        ).distinct().take(2).joinToString(", ")
        return if (place.isBlank()) coordinates(loc) else "Anda berada di sekitar $place."
    }

    private fun coordinates(loc: Location): String =
        "Anda berada di koordinat %.4f, %.4f.".format(loc.latitude, loc.longitude)
}
