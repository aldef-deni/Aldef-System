package com.aldef.system.aldefai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Activity transparan sekali-pakai untuk meminta izin RECORD_AUDIO.
 *
 * Panel ALDEF AI adalah overlay (bukan Activity), jadi tidak bisa meminta izin
 * runtime langsung. Tombol di panel menjalankan Activity ini; ia meminta izin
 * lewat dialog resmi Android lalu langsung menutup diri.
 */
class VoicePermissionActivity : ComponentActivity() {

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            finish()
        } else {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
