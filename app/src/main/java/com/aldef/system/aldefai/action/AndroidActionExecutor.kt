package com.aldef.system.aldefai.action

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.AlarmClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.aldef.system.aldefai.intent.ALDEFAIIntent
import com.aldef.system.aldefai.intent.AppTarget
import com.aldef.system.aldefai.intent.DeviceAction
import com.aldef.system.aldefai.intent.FeatureTarget
import com.aldef.system.aldefai.intent.NavTarget
import com.aldef.system.aldefai.intent.VolumeMode
import com.aldef.system.MainActivity

/**
 * Eksekutor aksi berbasis Intent & API resmi Android — tanpa root, tanpa hidden
 * API, tanpa Accessibility.
 *
 * Aplikasi ini memegang izin overlay (SYSTEM_ALERT_WINDOW), yang membuatnya
 * dikecualikan dari pembatasan "background activity start", sehingga membuka
 * aplikasi lain dari panel overlay diperbolehkan secara resmi.
 */
class AndroidActionExecutor(context: Context) : ALDEFAIActionExecutor {

    private val app = context.applicationContext

    override suspend fun execute(intent: ALDEFAIIntent): ALDEFAIActionResult = when (intent) {
        is ALDEFAIIntent.OpenApp -> openApp(intent.app)
        is ALDEFAIIntent.Navigate -> navigate(intent.target)
        is ALDEFAIIntent.Device -> device(intent.action)
        is ALDEFAIIntent.SetVolume -> setVolumePercent(intent.percent, intent.mode)
        is ALDEFAIIntent.Weather -> ALDEFAIActionResult(true, WeatherSpeaker.describe(app), closePanel = false)
        is ALDEFAIIntent.TellTime -> ALDEFAIActionResult(true, tellTime(), closePanel = false)
        is ALDEFAIIntent.TellDate -> ALDEFAIActionResult(true, tellDate(), closePanel = false)
        is ALDEFAIIntent.TellDay -> ALDEFAIActionResult(true, tellDay(), closePanel = false)
        is ALDEFAIIntent.TellLocation -> ALDEFAIActionResult(true, LocationSpeaker.describe(app), closePanel = false)
        is ALDEFAIIntent.SetAlarm -> setAlarm(intent.hour, intent.minute)
        is ALDEFAIIntent.OpenFeature -> openFeature(intent.feature)
        is ALDEFAIIntent.OpenAppByName -> openByName(intent.query)
        is ALDEFAIIntent.Unknown ->
            ALDEFAIActionResult(false, "Maaf, saya belum mengerti perintah itu.", closePanel = false)
    }

    private fun openApp(target: AppTarget): ALDEFAIActionResult = when (target) {
        AppTarget.CAMERA -> {
            val still = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            val generic = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launchFirst(still, generic, ok = "Baik, membuka kamera.", fail = "Saya tidak menemukan aplikasi kamera.")
        }

        AppTarget.GALLERY -> {
            // Utamakan galeri bawaan Realme/ColorOS, bukan Google Photos.
            val builtIn = app.packageManager.getLaunchIntentForPackage("com.coloros.gallery3d")
            val gallery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)
            val view = Intent(Intent.ACTION_VIEW).setType("image/*")
            val candidates = listOfNotNull(builtIn, gallery, view).toTypedArray()
            launchFirst(*candidates, ok = "Baik, membuka galeri.", fail = "Saya tidak menemukan aplikasi galeri.")
        }

        AppTarget.BROWSER -> {
            val browser = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
            val view = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            launchFirst(browser, view, ok = "Baik, membuka browser.", fail = "Saya tidak menemukan browser.")
        }
    }

    private fun navigate(target: NavTarget): ALDEFAIActionResult = when (target) {
        NavTarget.HOME -> {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            launchFirst(home, ok = "Baik, ke layar utama.", fail = "Tidak bisa ke layar utama.")
        }

        NavTarget.SETTINGS -> {
            launchFirst(Intent(Settings.ACTION_SETTINGS), ok = "Baik, membuka pengaturan.", fail = "Tidak bisa membuka pengaturan.")
        }

        // "Kembali" tanpa Accessibility tidak bisa menekan tombol back sistem;
        // di sini dimaknai menutup ALDEF AI (kembali dari asisten).
        NavTarget.BACK -> ALDEFAIActionResult(true, "Baik.", closePanel = true)
    }

    private fun device(action: DeviceAction): ALDEFAIActionResult = when (action) {
        DeviceAction.VOLUME_UP -> {
            adjustVolume(AudioManager.ADJUST_RAISE)
            ALDEFAIActionResult(true, "Baik, volume dinaikkan.")
        }

        DeviceAction.VOLUME_DOWN -> {
            adjustVolume(AudioManager.ADJUST_LOWER)
            ALDEFAIActionResult(true, "Baik, volume diturunkan.")
        }

        // Sejak targetSdk 33+, enable()/disable() Bluetooth jadi no-op walau di
        // Android 11. Solusi resmi: buka pengaturan Bluetooth.
        DeviceAction.BLUETOOTH_ON,
        DeviceAction.BLUETOOTH_OFF ->
            launchFirst(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                ok = "Baik, saya membuka pengaturan Bluetooth.",
                fail = "Tidak bisa membuka pengaturan Bluetooth."
            )
    }

    private fun openFeature(feature: FeatureTarget): ALDEFAIActionResult {
        val intent = Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_ROUTE, feature.route)
        val ok = runCatching { app.startActivity(intent) }.isSuccess
        return if (ok) ALDEFAIActionResult(true, "Baik, membuka ${feature.label}.")
        else ALDEFAIActionResult(false, "Tidak bisa membuka ${feature.label}.", closePanel = false)
    }

    private fun openByName(query: String): ALDEFAIActionResult {
        val entry = InstalledAppResolver.findBest(app, query)
            ?: return ALDEFAIActionResult(false, "Saya tidak menemukan aplikasi $query.", closePanel = false)
        val launch = app.packageManager.getLaunchIntentForPackage(entry.pkg)
            ?: return ALDEFAIActionResult(false, "Tidak bisa membuka ${entry.label}.", closePanel = false)
        val ok = runCatching {
            app.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        return if (ok) ALDEFAIActionResult(true, "Baik, membuka ${entry.label}.")
        else ALDEFAIActionResult(false, "Tidak bisa membuka ${entry.label}.", closePanel = false)
    }

    private fun setAlarm(hour: Int?, minute: Int): ALDEFAIActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (hour != null) {
            intent.putExtra(AlarmClock.EXTRA_HOUR, hour)
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minute)
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, "ALDEF AI")
        }
        if (intent.resolveActivity(app.packageManager) == null) {
            return ALDEFAIActionResult(false, "Saya tidak menemukan aplikasi alarm.", closePanel = false)
        }
        val ok = runCatching { app.startActivity(intent) }.isSuccess
        return when {
            !ok -> ALDEFAIActionResult(false, "Tidak bisa menyetel alarm.", closePanel = false)
            hour != null -> {
                val waktu = if (minute == 0) "$hour tepat" else "$hour lewat $minute menit"
                ALDEFAIActionResult(true, "Baik, alarm disetel pukul $waktu.")
            }
            else -> ALDEFAIActionResult(true, "Baik, saya membuka penyetelan alarm.")
        }
    }

    private fun tellTime(): String {
        val now = Calendar.getInstance()
        val h = now.get(Calendar.HOUR_OF_DAY)
        val m = now.get(Calendar.MINUTE)
        return if (m == 0) "Sekarang pukul $h tepat." else "Sekarang pukul $h lewat $m menit."
    }

    private fun tellDate(): String {
        val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("in", "ID"))
        return "Hari ini ${fmt.format(Date())}."
    }

    private fun tellDay(): String {
        val fmt = SimpleDateFormat("EEEE", Locale("in", "ID"))
        return "Hari ini hari ${fmt.format(Date())}."
    }

    private fun setVolumePercent(percent: Int, mode: VolumeMode): ALDEFAIActionResult {
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ALDEFAIActionResult(false, "Tidak bisa mengatur volume.", closePanel = false)
        val stream = AudioManager.STREAM_MUSIC
        val max = audio.getStreamMaxVolume(stream)
        if (max <= 0) return ALDEFAIActionResult(false, "Tidak bisa mengatur volume.", closePanel = false)
        val current = audio.getStreamVolume(stream)
        val delta = Math.round(max * percent / 100f)
        val target = when (mode) {
            VolumeMode.SET -> delta
            VolumeMode.RAISE -> current + delta
            VolumeMode.LOWER -> current - delta
        }.coerceIn(0, max)

        // Coba set langsung; sebagian ROM (ColorOS) mengabaikan setStreamVolume
        // dari latar. Kalau tak berubah, dorong dengan adjustStreamVolume yang
        // lebih diizinkan, langkah demi langkah sampai target.
        runCatching { audio.setStreamVolume(stream, target, 0) }
        var now = audio.getStreamVolume(stream)
        var guard = 0
        while (now != target && guard <= max + 2) {
            val dir = if (now < target) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            runCatching { audio.adjustStreamVolume(stream, dir, AudioManager.FLAG_SHOW_UI) }
            val next = audio.getStreamVolume(stream)
            if (next == now) break
            now = next
            guard++
        }

        val actual = Math.round(now * 100f / max)
        val verb = when (mode) {
            VolumeMode.SET -> "disetel ke"
            VolumeMode.RAISE -> "dinaikkan ke"
            VolumeMode.LOWER -> "diturunkan ke"
        }
        return ALDEFAIActionResult(true, "Baik, volume $verb $actual persen.")
    }

    private fun adjustVolume(direction: Int) {
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        }
    }

    /** Mencoba tiap intent secara berurutan; sukses pada yang pertama bisa dibuka. */
    private fun launchFirst(
        vararg candidates: Intent,
        ok: String,
        fail: String
    ): ALDEFAIActionResult {
        for (candidate in candidates) {
            if (candidate.resolveActivity(app.packageManager) != null) {
                val started = runCatching {
                    app.startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.isSuccess
                if (started) return ALDEFAIActionResult(true, ok)
            }
        }
        return ALDEFAIActionResult(false, fail, closePanel = false)
    }
}
