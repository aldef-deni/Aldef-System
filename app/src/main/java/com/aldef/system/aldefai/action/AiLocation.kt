package com.aldef.system.aldefai.action

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Pengambil lokasi terakhir perangkat, dipakai bersama fitur cuaca & lokasi. */
object AiLocation {

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun last(context: Context): Location? {
        if (!hasPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        // 1) Lokasi cache dari fusedLocation.
        await { client.lastLocation }?.let { return it }
        // 2) Kalau cache kosong (belum ada aplikasi minta lokasi), minta fix
        //    baru langsung — inilah kenapa dulu selalu "lokasi belum tersedia"
        //    padahal GPS aktif.
        return await {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        }
    }

    private suspend inline fun await(crossinline task: () -> Task<Location>): Location? =
        suspendCancellableCoroutine { cont ->
            runCatching {
                task()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }.onFailure { cont.resume(null) }
        }
}
