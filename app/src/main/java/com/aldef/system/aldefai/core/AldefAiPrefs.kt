package com.aldef.system.aldefai.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Penyimpanan pengaturan ALDEF AI (SharedPreferences, sejalan dengan gaya
 * [com.aldef.system.data.AppPrefs] yang sudah ada — tidak menambah database).
 *
 * Semua nilai punya default hemat baterai: fitur mati kecuali diaktifkan, dan
 * Continuous Listening default OFF.
 */
class AldefAiPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Sakelar utama. Selama OFF, tidak ada service/overlay yang berjalan. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_ENABLED, v).apply()

    /** Pemicu tepi layar untuk memanggil panel. */
    var edgeSwipe: Boolean
        get() = prefs.getBoolean(KEY_EDGE, true)
        set(v) = prefs.edit().putBoolean(KEY_EDGE, v).apply()

    /** Sisi tepi pemicu: true = kiri (default), false = kanan. */
    var edgeLeft: Boolean
        get() = prefs.getBoolean(KEY_EDGE_LEFT, true)
        set(v) = prefs.edit().putBoolean(KEY_EDGE_LEFT, v).apply()

    /** Dengar terus-menerus. Default OFF demi baterai. */
    var continuousListening: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS, false)
        set(v) = prefs.edit().putBoolean(KEY_CONTINUOUS, v).apply()

    /** Balasan suara (TTS). */
    var voiceResponse: Boolean
        get() = prefs.getBoolean(KEY_VOICE_RESPONSE, true)
        set(v) = prefs.edit().putBoolean(KEY_VOICE_RESPONSE, v).apply()

    /** Kode bahasa TTS/pengenalan. Untuk sekarang tetap Indonesia. */
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "id-ID") ?: "id-ID"
        set(v) = prefs.edit().putString(KEY_LANGUAGE, v).apply()

    /** Kecepatan bicara TTS, 0.5–2.0. */
    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(v) = prefs.edit().putFloat(KEY_SPEECH_RATE, v.coerceIn(0.5f, 2.0f)).apply()

    var haptic: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(v) = prefs.edit().putBoolean(KEY_HAPTIC, v).apply()

    var soundFeedback: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(v) = prefs.edit().putBoolean(KEY_SOUND, v).apply()

    var showTranscript: Boolean
        get() = prefs.getBoolean(KEY_TRANSCRIPT, true)
        set(v) = prefs.edit().putBoolean(KEY_TRANSCRIPT, v).apply()

    var aiAnimation: Boolean
        get() = prefs.getBoolean(KEY_ANIMATION, true)
        set(v) = prefs.edit().putBoolean(KEY_ANIMATION, v).apply()

    /** Penanda wizard penyiapan sudah dituntaskan (dipakai fase berikutnya). */
    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP, false)
        set(v) = prefs.edit().putBoolean(KEY_SETUP, v).apply()

    private companion object {
        const val FILE = "aldef_ai_prefs"
        const val KEY_ENABLED = "enabled"
        const val KEY_EDGE = "edge_swipe"
        const val KEY_EDGE_LEFT = "edge_left"
        const val KEY_CONTINUOUS = "continuous_listening"
        const val KEY_VOICE_RESPONSE = "voice_response"
        const val KEY_LANGUAGE = "language"
        const val KEY_SPEECH_RATE = "speech_rate"
        const val KEY_HAPTIC = "haptic"
        const val KEY_SOUND = "sound_feedback"
        const val KEY_TRANSCRIPT = "show_transcript"
        const val KEY_ANIMATION = "ai_animation"
        const val KEY_SETUP = "setup_complete"
    }
}
