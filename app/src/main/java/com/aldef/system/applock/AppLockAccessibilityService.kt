package com.aldef.system.applock

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Mendeteksi aplikasi terkunci yang dibuka lalu menutupinya dengan layar
 * palsu "sistem gagal".
 *
 * Layar kunci digambar sebagai *overlay* window (TYPE_APPLICATION_OVERLAY),
 * bukan Activity — cara ini kebal terhadap pembatasan "memulai Activity dari
 * latar belakang" dan lebih sulit dilewati pengguna. Overlay butuh izin
 * "Tampilkan di atas aplikasi lain".
 */
class AppLockAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlay: View? = null
    private var overlayForPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLockState.init(this)
        AppLockState.serviceConnected = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Jangan pernah mengunci diri sendiri atau UI sistem — kalau tidak,
        // layar kunci bisa menutupi brankasnya sendiri.
        if (pkg == packageName) {
            removeOverlay()
            return
        }
        if (pkg == "com.android.systemui") return

        val shouldLock = AppLockState.isLocked(pkg) && !AppLockState.isTemporarilyAllowed(pkg)
        if (shouldLock) {
            showOverlay(pkg)
        } else if (overlay != null && pkg != overlayForPackage) {
            // Berpindah ke aplikasi lain yang tidak terkunci -> lepas overlay.
            removeOverlay()
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AppLockState.serviceConnected = false
        removeOverlay()
        return super.onUnbind(intent)
    }

    private fun showOverlay(pkg: String) {
        if (overlay != null && overlayForPackage == pkg) return
        removeOverlay()

        val wm = windowManager ?: return
        val view = buildLockView()
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

    /** Membangun tampilan layar kunci sepenuhnya lewat kode (tanpa XML). */
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
                colors = intArrayOf(
                    Color.parseColor("#FF7A18"),
                    Color.parseColor("#FF4D6D")
                )
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
            }
            setPadding(dp(48f), dp(14f), dp(48f), dp(14f))
            setOnClickListener {
                removeOverlay()
                // Tendang pengguna keluar dari aplikasi terkunci.
                performGlobalAction(GLOBAL_ACTION_HOME)
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
}
