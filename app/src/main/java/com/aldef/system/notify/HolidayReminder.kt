package com.aldef.system.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aldef.system.MainActivity
import com.aldef.system.R
import com.aldef.system.data.Holidays
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Mengingatkan sehari sebelum libur nasional.
 *
 * Sebuah alarm harian (pukul 19.00, inexact — jadi tidak perlu izin exact-alarm)
 * memeriksa apakah **besok** tanggal merah nasional; kalau ya, memunculkan
 * notifikasi. Alarm dijadwalkan ulang setiap kali aplikasi dibuka dan setelah
 * perangkat menyala kembali.
 */
object HolidayReminder {

    private const val CHANNEL_ID = "aldef_holiday"
    private const val NOTIF_ID = 7301
    private const val REQUEST_CODE = 7302
    private const val REMINDER_HOUR = 19

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var next = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(REMINDER_HOUR, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(zone).toInstant().toEpochMilli()

        // Inexact + berulang harian: cukup untuk pengingat, tanpa izin khusus.
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            alarmIntent(context)
        )
    }

    /** Dipanggil saat alarm berbunyi: cek besok, munculkan notifikasi bila libur. */
    fun notifyIfTomorrowHoliday(context: Context) {
        val tomorrow = LocalDate.now(ZoneId.systemDefault()).plusDays(1)
        val holidays = Holidays.holidaysOn(tomorrow)
        if (holidays.isEmpty()) return

        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("in", "ID"))
        val names = holidays.joinToString(" · ") { it.name }
        val title = "Besok Libur Nasional 🎉"
        val body = "$names\n${tomorrow.format(formatter)}"

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Libur Nasional",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Muncul sehari sebelum tanggal merah nasional" }
            manager.createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(names)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID, notification) }
    }

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, HolidayAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
}

/** Menerima alarm harian lalu meneruskannya ke [HolidayReminder]. */
class HolidayAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        HolidayReminder.notifyIfTomorrowHoliday(context)
    }
}
