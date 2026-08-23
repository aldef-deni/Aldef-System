package com.aldef.system.aldefai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Implementasi [ALDEFSpeechRecognizer] di atas [SpeechRecognizer] bawaan
 * Android — API resmi, tanpa server. Di Realme 5 Pro mesin di baliknya adalah
 * layanan pengenalan Google (butuh internet untuk Bahasa Indonesia).
 *
 * Semua metode dipanggil dari main thread (dipakai dari panel Compose overlay).
 */
class AndroidSpeechRecognizer(
    private val context: Context,
    private val languageTag: String,
    private val listener: ALDEFRecognitionListener
) : ALDEFSpeechRecognizer {

    private var recognizer: SpeechRecognizer? = null
    private var cancelled = false

    private val available: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private val internalListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listener.onState(ALDEFAIVoiceState.Listening)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            // RMS kira-kira -2..10 dB → 0..1 untuk animasi.
            listener.onRms(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            listener.onState(ALDEFAIVoiceState.Processing)
        }

        override fun onError(error: Int) {
            if (cancelled && error == SpeechRecognizer.ERROR_CLIENT) {
                listener.onState(ALDEFAIVoiceState.Idle)
                return
            }
            listener.onState(ALDEFAIVoiceState.Error(messageFor(error)))
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) {
                listener.onFinal(text)
            }
            listener.onState(ALDEFAIVoiceState.Idle)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) listener.onPartial(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun startListening() {
        if (!available) {
            listener.onState(
                ALDEFAIVoiceState.Error("Pengenalan suara tidak tersedia di perangkat ini")
            )
            return
        }
        cancelled = false
        // Buat instance baru tiap sesi — SpeechRecognizer rewel bila dipakai ulang.
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(internalListener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        listener.onState(ALDEFAIVoiceState.Listening)
        runCatching { recognizer?.startListening(intent) }
            .onFailure {
                listener.onState(ALDEFAIVoiceState.Error("Tidak bisa memulai mikrofon"))
            }
    }

    override fun stopListening() {
        runCatching { recognizer?.stopListening() }
    }

    override fun cancel() {
        cancelled = true
        runCatching { recognizer?.cancel() }
        listener.onState(ALDEFAIVoiceState.Idle)
    }

    override fun destroy() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tidak ada suara yang dikenali. Coba lagi."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Perlu koneksi internet untuk pengenalan suara"
        SpeechRecognizer.ERROR_AUDIO -> "Terjadi masalah pada mikrofon"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mesin pengenal sedang sibuk, coba lagi"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan"
        SpeechRecognizer.ERROR_SERVER -> "Layanan pengenalan suara bermasalah"
        SpeechRecognizer.ERROR_CLIENT -> "Pengenalan dibatalkan"
        else -> "Terjadi kesalahan pada pengenalan suara"
    }
}
