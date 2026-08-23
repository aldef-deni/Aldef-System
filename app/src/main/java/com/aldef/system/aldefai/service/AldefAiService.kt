package com.aldef.system.aldefai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.aldef.system.MainActivity
import com.aldef.system.R
import com.aldef.system.aldefai.core.AldefAiPrefs
import com.aldef.system.aldefai.overlay.AldefAiPanel
import com.aldef.system.aldefai.tts.AldefTtsHolder
import kotlin.math.abs

/**
 * Layanan latar depan ALDEF AI.
 *
 * Menampilkan strip pemicu tipis di tepi layar (kiri/kanan sesuai setelan) lewat
 * overlay WindowManager — cara resmi yang sama seperti [com.aldef.system.applock.service.AppLockService],
 * bukan Accessibility. Strip hanya hadir saat ALDEF AI + Edge Swipe aktif dan
 * izin overlay diberikan.
 *
 * Phase 3: strip + reaksi sentuh (glow + haptic) + pil konfirmasi singkat.
 * Panel penuh (mic ALDEFTECH, animasi, suara) menyusul di fase berikutnya.
 */
class AldefAiService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: AldefAiPrefs

    private var strip: View? = null
    private var panel: AldefAiPanel? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = AldefAiPrefs(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        // Siapkan TTS agar balasan suara pertama tidak terlewat.
        runCatching { AldefTtsHolder.ensure(this) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshStrip()
        return START_STICKY
    }

    override fun onDestroy() {
        removeStrip()
        panel?.destroy()
        panel = null
        super.onDestroy()
    }

    private fun dp(value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    ).toInt()

    // ------------------------------------------------------------- Edge strip

    private fun refreshStrip() {
        removeStrip()
        if (!prefs.enabled || !prefs.edgeSwipe) return
        if (!Settings.canDrawOverlays(this)) {
            // Tanpa izin overlay strip tak bisa muncul; hentikan diri dgn tenang.
            stopSelf()
            return
        }

        val onLeft = prefs.edgeLeft
        val view = EdgeStripView(this, onLeft)
        val params = WindowManager.LayoutParams(
            dp(22f),
            dp(150f),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (onLeft) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
        }

        var downX = 0f
        var downY = 0f
        var moved = false
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    (v as EdgeStripView).active = true
                    v.invalidate()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawX - downX) > dp(6f) || abs(event.rawY - downY) > dp(6f)) {
                        moved = true
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as EdgeStripView).active = false
                    v.invalidate()
                    val dx = event.rawX - downX
                    // Arah membuka: dari kiri geser kanan; dari kanan geser kiri.
                    val opened = if (onLeft) dx > dp(36f) else dx < -dp(36f)
                    val tap = !moved
                    if (event.action == MotionEvent.ACTION_UP && (opened || tap)) activate()
                    true
                }

                else -> false
            }
        }

        runCatching {
            windowManager.addView(view, params)
            strip = view
        }
    }

    private fun removeStrip() {
        strip?.let { runCatching { windowManager.removeView(it) } }
        strip = null
    }

    /** Dipicu saat strip di-swipe/ketuk: buka panel premium ALDEF AI. */
    private fun activate() {
        val current = panel
        if (current != null && current.isShowing) return
        if (prefs.haptic) lightHaptic()
        val host = current ?: AldefAiPanel(this).also { panel = it }
        host.show(onLeft = prefs.edgeLeft, onClosed = { })
    }

    // -------------------------------------------------------------- Utilitas

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

    private fun lightHaptic() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(18, 90))
        }
    }

    private fun buildNotification(): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ALDEF AI",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            manager.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ALDEF AI")
            .setContentText("Voice Assistant Ready")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(open)
            .build()
    }

    /** Strip tepi: batang membulat bergradien dengan sedikit glow. */
    private class EdgeStripView(context: Context, private val onLeft: Boolean) : View(context) {
        var active = false

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val barW = w * 0.34f
            val left = if (onLeft) 0f else w - barW
            val inset = h * 0.14f
            rect.set(left, inset, left + barW, h - inset)

            paint.shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                intArrayOf(
                    Color.parseColor("#7B2BFF"),
                    Color.parseColor("#C724FF"),
                    Color.parseColor("#22D3EE")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            paint.alpha = if (active) 255 else 150

            if (active) {
                glowPaint.color = Color.parseColor("#5522D3EE")
                canvas.drawRoundRect(
                    RectF(rect.left - barW, rect.top, rect.right + barW, rect.bottom),
                    barW, barW, glowPaint
                )
            }

            val radius = barW / 2f
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    companion object {
        private const val CHANNEL_ID = "aldef_ai"
        private const val NOTIF_ID = 4301

        /**
         * Menyalakan/mematikan layanan sesuai setelan. Aman dipanggil berulang.
         */
        fun sync(context: Context) {
            val prefs = AldefAiPrefs(context)
            val needed = prefs.enabled && prefs.edgeSwipe && Settings.canDrawOverlays(context)
            val intent = Intent(context, AldefAiService::class.java)
            if (needed) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
            } else {
                runCatching { context.stopService(intent) }
            }
        }
    }
}
