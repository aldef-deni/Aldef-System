package com.aldef.system.aldefai.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Host untuk panel ALDEF AI berbasis Compose yang digambar sebagai overlay
 * WindowManager (bukan Activity).
 *
 * ComposeView di luar Activity butuh pemilik Lifecycle/ViewModelStore/SavedState
 * sendiri — kelas ini menyediakannya. Jendelanya *focusable* supaya tombol Back
 * ditangkap dan menutup panel lebih dulu, tanpa menutup aplikasi di belakang.
 */
class AldefAiPanel(context: Context) :
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // Bungkus tema aplikasi agar Compose memakai warna yang benar.
    private val themed = android.view.ContextThemeWrapper(
        context.applicationContext,
        context.applicationInfo.theme
    )
    private val windowManager =
        context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var composeView: ComposeView? = null

    /** Sinyal yang membuat konten menjalankan animasi keluar sebelum dilepas. */
    private val dismissSignal = mutableStateOf(false)

    var isShowing = false
        private set

    init {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun show(onLeft: Boolean, onClosed: () -> Unit) {
        if (isShowing) return
        dismissSignal.value = false

        val view = ComposeView(themed).apply {
            setViewTreeLifecycleOwner(this@AldefAiPanel)
            setViewTreeViewModelStoreOwner(this@AldefAiPanel)
            setViewTreeSavedStateRegistryOwner(this@AldefAiPanel)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusableInTouchMode = true
            setContent {
                AldefAiPanelContent(
                    onLeft = onLeft,
                    dismissSignal = dismissSignal.value,
                    onFullyDismissed = { removeNow(onClosed) }
                )
            }
            // Tombol Back → tutup panel lebih dulu (animasi keluar).
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    requestClose()
                    true
                } else {
                    false
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        runCatching {
            windowManager.addView(view, params)
            composeView = view
            view.requestFocus()
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            isShowing = true
        }
    }

    /** Meminta konten menjalankan animasi keluar; pelepasan terjadi setelahnya. */
    fun requestClose() {
        if (isShowing) dismissSignal.value = true
    }

    private fun removeNow(onClosed: () -> Unit) {
        if (!isShowing) return
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        composeView?.let { runCatching { windowManager.removeView(it) } }
        composeView = null
        isShowing = false
        onClosed()
    }

    fun destroy() {
        composeView?.let { runCatching { windowManager.removeView(it) } }
        composeView = null
        isShowing = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
}
