package com.aldef.system.applock

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent

/**
 * Pembungkus tipis atas [DevicePolicyManager].
 *
 * Semua operasi "keras" (menyembunyikan aplikasi, memblokir uninstall) hanya
 * berhasil bila aplikasi berstatus **Device Owner**. Setiap metode memeriksa
 * status itu lebih dulu dan melaporkan kegagalan secara jujur lewat nilai
 * balik, bukan diam-diam.
 */
class DevicePolicyController(context: Context) {

    private val appContext = context.applicationContext
    private val dpm =
        appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = AldefDeviceAdminReceiver.component(appContext)

    fun isAdminActive(): Boolean = dpm.isAdminActive(admin)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(appContext.packageName)

    /** Intent untuk meminta pengguna mengaktifkan Device Admin. */
    fun requestAdminIntent(reason: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, reason)
        }

    fun removeAdmin() {
        if (isAdminActive()) runCatching { dpm.removeActiveAdmin(admin) }
    }

    /**
     * Menyembunyikan/menampilkan aplikasi dari peluncur & pencarian.
     * @return true bila status berhasil diterapkan.
     */
    fun setHidden(pkg: String, hidden: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching { dpm.setApplicationHidden(admin, pkg, hidden) }.getOrDefault(false)
    }

    fun isHiddenByPolicy(pkg: String): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching { dpm.isApplicationHidden(admin, pkg) }.getOrDefault(false)
    }

    /** Memblokir/mengizinkan uninstall sebuah paket. */
    fun setUninstallBlocked(pkg: String, blocked: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching {
            dpm.setUninstallBlocked(admin, pkg, blocked)
            true
        }.getOrDefault(false)
    }

    fun isUninstallBlockedByPolicy(pkg: String): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching { dpm.isUninstallBlocked(admin, pkg) }.getOrDefault(false)
    }

    /** Perintah ADB untuk menjadikan aplikasi ini Device Owner. */
    fun deviceOwnerAdbCommand(): String =
        "adb shell dpm set-device-owner ${appContext.packageName}/.applock.AldefDeviceAdminReceiver"
}
