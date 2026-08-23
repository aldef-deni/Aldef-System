package com.aldef.system.applock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.aldef.system.R
import com.aldef.system.applock.AppLockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Menjaga aplikasi terkunci dengan memantau aplikasi mana yang sedang di depan
 * lewat [UsageStatsManager], lalu menutupinya dengan overlay palsu
 * "Sistem Gagal".
 *
 * Pendekatan ini sengaja **tidak** memakai AccessibilityService: izin Akses
 * Penggunaan jauh lebih sempit (hanya tahu paket mana yang aktif, tidak membaca
 * isi layar) dan tidak memicu blokir keras Google Play Protect. Overlay-nya
 * butuh izin "Tampilkan di atas aplikasi lain".
 */
class AppLockService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchJob: Job? = null

    private var windowManager: WindowManager? = null
    private var overlay: View? = null
    private var overlayForPackage: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLockState.init(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startWatching()
        return START_STICKY
    }

    override fun onDestroy() {
        watchJob?.cancel()
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun startWatching() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            var since = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val front = latestForegroundPackage(since - QUERY_SLACK_MS, now)
                since = now
                if (front != null) reconcile(front)
                delay(POLL_MS)
            }
        }
    }

    /** Menentukan apakah aplikasi yang sedang di depan harus ditutupi. */
    private fun reconcile(pkg: String) {
        val shouldLock = pkg != packageName &&
            pkg != "com.android.systemui" &&
            AppLockState.isLocked(pkg) &&
            !AppLockState.isTemporarilyAllowed(pkg)

        // Overlay hanya boleh disentuh dari thread utama.
        android.os.Handler(mainLooper).post {
            if (shouldLock) showOverlay(pkg) else removeOverlay()
        }
    }

    private fun latestForegroundPackage(from: Long, to: Long): String? {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        return runCatching {
            val events = usage.queryEvents(from, to)
            val event = UsageEvents.Event()
            var last: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    last = event.packageName
                }
            }
            last
        }.getOrNull()
    }

    // ---------------------------------------------------------------- Overlay

    private fun showOverlay(pkg: String) {
        if (overlay != null && overlayForPackage == pkg) return
        removeOverlay()
        val wm = windowManager ?: return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        )
        runCatching {
            val view = buildLockView()
            wm.addView(view, params)
            overlay = view
            overlayForPackage = pkg
        }
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        runCatching { windowManager?.removeView(view) }
        overlay = null
        overlayForPackage = null
    }

    private fun buildLockView(): View {
        fun dp(value: Float): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
        ).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#05060A"))
            isClickable = true
            isFocusable = true
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(40f), dp(40f), dp(40f), dp(40f))
        }
        val icon = TextView(this).apply {
            text = "⚠"
            setTextColor(Color.parseColor("#FF4D6D"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f)
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "Sistem Gagal"
            setTextColor(Color.parseColor("#F3F5FA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            gravity = Gravity.CENTER
            setPadding(0, dp(18f), 0, 0)
        }
        val message = TextView(this).apply {
            text = "Aplikasi tidak dapat dibuka karena berkas sistemnya rusak " +
                "atau tidak lengkap. Coba lagi nanti."
            setTextColor(Color.parseColor("#9AA3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12f), 0, 0)
        }
        val code = TextView(this).apply {
            text = "Kode kesalahan: 0xC0000221"
            setTextColor(Color.parseColor("#5C6478"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, dp(20f), 0, 0)
        }
        val button = Button(this).apply {
            text = "Tutup"
            isAllCaps = false
            setTextColor(Color.parseColor("#05060A"))
            background = GradientDrawable().apply {
                cornerRadius = dp(16f).toFloat()
                colors = intArrayOf(Color.parseColor("#FF7A18"), Color.parseColor("#FF4D6D"))
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
            }
            setPadding(dp(48f), dp(14f), dp(48f), dp(14f))
            setOnClickListener {
                removeOverlay()
                // Tendang pengguna ke layar Home.
                val home = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { startActivity(home) }
            }
        }
        column.addView(icon)
        column.addView(title)
        column.addView(message)
        column.addView(code)
        column.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(30f) }
        )
        root.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        return root
    }

    // ---------------------------------------------------------- Notification

    private fun buildNotification(): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proteksi Aplikasi",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aldef System")
            .setContentText("Proteksi aplikasi aktif")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val POLL_MS = 500L
        private const val QUERY_SLACK_MS = 1_500L
        private const val CHANNEL_ID = "aldef_applock"
        private const val NOTIF_ID = 4201

        /**
         * Menyalakan layanan bila memang perlu (ada aplikasi terkunci + izin
         * lengkap), atau mematikannya bila tidak. Aman dipanggil berkali-kali.
         */
        fun sync(context: Context) {
            AppLockState.init(context)
            val needed = AppLockState.hasLockedApps() &&
                AppLockState.hasUsageAccess(context) &&
                android.provider.Settings.canDrawOverlays(context)
            val intent = Intent(context, AppLockService::class.java)
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
