package com.aldef.system.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.widget.Toast
import com.google.gson.JsonObject

/**
 * AldefActionExecutor: Jembatan eksekusi Function Call dari Google Gemini ke Android System APIs.
 */
class AldefActionExecutor(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isFlashlightOn = false

    fun execute(toolName: String, args: JsonObject): ActionResult {
        return when (toolName) {
            "open_app" -> {
                val appName = args.get("appName")?.asString ?: "app"
                val result = launchApp(appName)
                ActionResult(true, result)
            }
            "toggle_system" -> {
                val feature = args.get("feature")?.asString ?: ""
                val state = args.get("state")?.asString ?: "toggle"
                val result = handleSystemToggle(feature, state)
                ActionResult(true, result)
            }
            "set_volume" -> {
                val level = args.get("level")?.asInt ?: 50
                val result = adjustVolume(level)
                ActionResult(true, result)
            }
            "set_alarm" -> {
                val time = args.get("time")?.asString ?: "07:00"
                val label = args.get("label")?.asString ?: "Alarm Aldef System"
                val result = createAlarm(time, label)
                ActionResult(true, result)
            }
            "send_whatsapp" -> {
                val recipient = args.get("recipient")?.asString ?: ""
                val message = args.get("message")?.asString ?: ""
                val result = sendWhatsAppMessage(recipient, message)
                ActionResult(true, result)
            }
            "search_youtube" -> {
                val query = args.get("query")?.asString ?: ""
                val result = searchYouTube(query)
                ActionResult(true, result)
            }
            "create_note" -> {
                val title = args.get("title")?.asString ?: "Catatan Baru"
                val content = args.get("content")?.asString ?: ""
                val result = createNote(title, content)
                ActionResult(true, result)
            }
            else -> ActionResult(false, "Aksi $toolName tidak dikenal.")
        }
    }

    private fun launchApp(appName: String): String {
        val targetPackage = when (appName.lowercase()) {
            "whatsapp", "wa" -> "com.whatsapp"
            "youtube", "yt" -> "com.google.android.youtube"
            "settings", "pengaturan", "setelan" -> "com.android.settings"
            "camera", "kamera" -> "android.media.action.IMAGE_CAPTURE"
            "spotify" -> "com.spotify.music"
            "maps", "google maps" -> "com.google.android.apps.maps"
            else -> null
        }

        return try {
            if (targetPackage != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    "Membuka aplikasi $appName..."
                } else {
                    "Aplikasi $appName tidak ditemukan di perangkat."
                }
            } else {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Membuka $appName"
            }
        } catch (e: Exception) {
            "Gagal membuka $appName: ${e.message}"
        }
    }

    private fun handleSystemToggle(feature: String, state: String): String {
        return when (feature.lowercase()) {
            "flashlight", "senter" -> {
                try {
                    val cameraId = cameraManager.cameraIdList[0]
                    isFlashlightOn = when (state.lowercase()) {
                        "on", "true", "hidup", "nyala" -> true
                        "off", "false", "mati" -> false
                        else -> !isFlashlightOn
                    }
                    cameraManager.setTorchMode(cameraId, isFlashlightOn)
                    if (isFlashlightOn) "Lampu senter berhasil dinyalakan." else "Lampu senter berhasil dimatikan."
                } catch (e: Exception) {
                    "Gagal mengontrol senter: ${e.message}"
                }
            }
            else -> "Fitur $feature belum didukung untuk kontrol langsung."
        }
    }

    private fun adjustVolume(levelPercent: Int): String {
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (levelPercent.coerceIn(0, 100) * maxVolume) / 100
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
            "Volume media disetel ke $levelPercent%."
        } catch (e: Exception) {
            "Gagal mengatur volume: ${e.message}"
        }
    }

    private fun createAlarm(timeStr: String, label: String): String {
        return try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = parts.getOrNull(1)?.toInt() ?: 0

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Alarm berhasil dipasang untuk pukul $timeStr ($label)."
        } catch (e: Exception) {
            "Gagal memasang alarm: ${e.message}"
        }
    }

    private fun sendWhatsAppMessage(recipient: String, message: String): String {
        return try {
            val url = "https://api.whatsapp.com/send?text=" + Uri.encode(message)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Membuka WhatsApp untuk mengirim pesan ke $recipient."
        } catch (e: Exception) {
            "WhatsApp tidak terpasang atau gagal dibuka."
        }
    }

    private fun searchYouTube(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Mencari $query di YouTube..."
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "Membuka YouTube di browser..."
        }
    }

    private fun createNote(title: String, content: String): String {
        return try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
            "Menyimpan catatan: $title"
        } catch (e: Exception) {
            "Gagal membuat catatan: ${e.message}"
        }
    }
}

data class ActionResult(val success: Boolean = true, val message: String)