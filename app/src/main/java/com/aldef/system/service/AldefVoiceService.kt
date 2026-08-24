package com.aldef.system.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.aldef.system.MainActivity
import com.aldef.system.R
import com.aldef.system.actions.AldefActionExecutor
import com.aldef.system.ai.GeminiAgentClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ForegroundService dengan Persistent Notification untuk menangkap ucapan pengguna
 * dan membalas melalui Android TextToSpeech (Locale id-ID).
 */
class AldefVoiceService : Service(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var geminiClient: GeminiAgentClient
    private lateinit var actionExecutor: AldefActionExecutor
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private val CHANNEL_ID = "aldef_voice_service_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification("Aldef Voice Agent Siap Mendengarkan"))

        // Inisialisasi komponen
        val apiKey = getGeminiApiKey()
        geminiClient = GeminiAgentClient(apiKey)
        actionExecutor = AldefActionExecutor(this)
        textToSpeech = TextToSpeech(this, this)
        initSpeechRecognizer()
    }

    private fun getGeminiApiKey(): String {
        return try {
            val clazz = Class.forName("com.aldef.system.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String
            if (!key.isNullOrBlank()) key else "AIzaSyYourActualGoogleGeminiApiKeyHere"
        } catch (e: Exception) {
            "AIzaSyYourActualGoogleGeminiApiKeyHere"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aldef Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan latar belakang asisten suara Aldef System"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aldef Voice AI Agent")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateNotification("Mendengarkan suara Anda...")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                updateNotification("Memproses ucapan...")
            }
            override fun onError(error: Int) {
                // Restart listen atau update status
                updateNotification("Siaga (Standby) - Klik mic untuk bicara")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val userSpeech = matches[0]
                    processVoiceWithAI(userSpeech)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara ke Aldef System...")
        }
        speechRecognizer.startListening(intent)
    }

    private fun processVoiceWithAI(prompt: String) {
        serviceScope.launch {
            updateNotification("Gemini AI: $prompt")
            val response = geminiClient.processVoiceCommand(prompt)

            // 1. Eksekusi semua tool panggilan fungsi di Android
            for (call in response.functionCalls) {
                actionExecutor.execute(call.name, call.args)
            }

            // 2. Balas suara pengguna via TextToSpeech bahasa Indonesia
            speakReply(response.replyText)
        }
    }

    private fun speakReply(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ALDEF_REPLY_UTTERANCE")
        updateNotification("Aldef: $text")
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildForegroundNotification(text))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Setting bahasa Indonesia untuk Android TTS
            val result = textToSpeech.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.language = Locale.getDefault()
            }
            textToSpeech.setPitch(1.0f)
            textToSpeech.setSpeechRate(1.0f)
        }
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}