package com.aldef.system.aldefai.tts

import android.content.Context

/**
 * Pemegang TTS tingkat-proses.
 *
 * Panel ALDEF AI ditutup begitu sebuah aplikasi dibuka (agar overlay tidak
 * menutupi aplikasi tujuan), jadi TTS tidak boleh ikut mati bersama panel —
 * balasan "Baik, membuka kamera." harus tetap terdengar. Karena itu instance
 * TTS hidup di sini, lintas siklus panel.
 */
object AldefTtsHolder {

    private var engine: AldefTts? = null

    @Volatile
    private var listener: (Boolean) -> Unit = {}

    /** Menyiapkan mesin lebih awal agar hangat saat perintah pertama selesai. */
    fun ensure(context: Context) {
        if (engine == null) {
            engine = AldefTts(context.applicationContext) { speaking -> listener(speaking) }
        }
    }

    fun speak(context: Context, text: String, rate: Float, onSpeaking: (Boolean) -> Unit) {
        ensure(context)
        listener = onSpeaking
        engine?.speak(text, rate)
    }

    fun stop() {
        engine?.stop()
    }

    fun clearListener() {
        listener = {}
    }
}
