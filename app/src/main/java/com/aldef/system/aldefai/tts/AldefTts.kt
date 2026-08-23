package com.aldef.system.aldefai.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Pembungkus [TextToSpeech] Android untuk balasan suara ALDEF AI.
 *
 * Bahasa utama Indonesia; jika suara id-ID tak tersedia di perangkat, jatuh ke
 * bahasa default. Semua callback status dikembalikan di main thread.
 */
class AldefTts(
    context: Context,
    private val onSpeaking: (Boolean) -> Unit
) {

    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var languageOk = false

    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            val result = engine?.setLanguage(Locale("id", "ID"))
            languageOk = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!languageOk) {
                // Cadangan: pakai bahasa default perangkat.
                engine?.setLanguage(Locale.getDefault())
            }
        }
    }

    private val engine: TextToSpeech? get() = tts

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = post { onSpeaking(true) }
            override fun onDone(utteranceId: String?) = post { onSpeaking(false) }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = post { onSpeaking(false) }
            override fun onError(utteranceId: String?, errorCode: Int) = post { onSpeaking(false) }
        })
    }

    /** Mengucapkan [text]; [rate] 0.5–2.0. Aman dipanggil sebelum siap (diabaikan). */
    fun speak(text: String, rate: Float) {
        if (!ready || text.isBlank()) return
        tts.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        runCatching { tts.stop() }
        post { onSpeaking(false) }
    }

    fun shutdown() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }

    private fun post(action: () -> Unit) {
        main.post { action() }
    }

    private companion object {
        const val UTTERANCE_ID = "aldef_ai_tts"
    }
}
