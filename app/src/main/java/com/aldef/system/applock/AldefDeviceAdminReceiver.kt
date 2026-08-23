package com.aldef.system.applock

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context

/**
 * Receiver admin perangkat.
 *
 * Dua peran:
 *  - Sebagai **Device Admin** biasa: mengaktifkannya membuat Aldef System
 *    sendiri tidak bisa di-uninstall sampai admin dinonaktifkan.
 *  - Sebagai **Device Owner** (dipasang lewat ADB): membuka kemampuan
 *    menyembunyikan aplikasi lain dari peluncur/pencarian dan memblokir
 *    uninstall aplikasi lain.
 */
class AldefDeviceAdminReceiver : DeviceAdminReceiver() {
    companion object {
        fun component(context: Context): ComponentName =
            ComponentName(context.applicationContext, AldefDeviceAdminReceiver::class.java)
    }
}
