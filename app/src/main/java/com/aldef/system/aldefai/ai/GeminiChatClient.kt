package com.aldef.system.aldefai.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Klien ringan untuk Google Gemini REST API.
 *
 * Digunakan sebagai **fallback cerdas** ketika [IntentEngine] tidak mengenali
 * perintah suara. Jawaban dikembalikan sebagai teks Bahasa Indonesia yang bisa
 * langsung diucapkan lewat TTS.
 *
 * Tidak ada state / history percakapan — setiap panggilan berdiri sendiri
 * (stateless single-turn) agar sederhana dan hemat kuota.
 */
class GeminiChatClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val systemPrompt = """
        Kamu adalah ALDEF AI, asisten suara pribadi di smartphone Android milik pengguna.
        Nama lengkapmu ALDEF AI buatan ALDEFTECH.
        
        Aturan:
        - Jawab dalam Bahasa Indonesia yang natural dan singkat (maksimal 2-3 kalimat).
        - Jawaban akan diucapkan lewat text-to-speech, jadi hindari simbol, kode, tabel,
          atau format yang sulit dibaca suara.
        - Jangan gunakan emoji, markdown, atau bullet points.
        - Jika ditanya siapa kamu, jawab bahwa kamu adalah ALDEF AI, asisten suara pribadi.
        - Bersikap ramah, sopan, dan informatif.
    """.trimIndent()

    /**
     * Kirim [userMessage] ke Gemini dan kembalikan jawaban teks.
     *
     * Fungsi ini suspend dan aman dipanggil dari coroutine scope.
     * Mengembalikan `null` jika gagal (timeout, jaringan, dsb.).
     */
    suspend fun ask(userMessage: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = buildRequestBody(userMessage)
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(response.body?.string() ?: return@withContext null)
            extractText(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildRequestBody(userMessage: String): JSONObject {
        return JSONObject().apply {
            // System instruction
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemPrompt))
                })
            })

            // User message
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userMessage))
                    })
                })
            })

            // Generation config — jawaban pendek untuk TTS
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 256)
                put("temperature", 0.7)
            })
        }
    }

    private fun extractText(json: JSONObject): String? {
        return try {
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Mengecek apakah API key tersedia dan valid (bukan placeholder kosong).
         */
        fun hasValidKey(): Boolean {
            return try {
                val key = com.aldef.system.BuildConfig.GEMINI_API_KEY
                key.isNotBlank() && key != "null"
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Membuat instance baru dari BuildConfig API key.
         * Mengembalikan `null` jika key tidak tersedia.
         */
        fun create(): GeminiChatClient? {
            return if (hasValidKey()) {
                GeminiChatClient(com.aldef.system.BuildConfig.GEMINI_API_KEY)
            } else {
                null
            }
        }
    }
}

