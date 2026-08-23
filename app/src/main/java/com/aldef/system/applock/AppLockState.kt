package com.aldef.system.applock

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.text.TextUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * Sumber kebenaran untuk daftar aplikasi yang dikunci / disembunyikan /
 * diproteksi dari uninstall.
 *
 * Objek ini dipakai bersama oleh UI (di dalam brankas) dan oleh
 * [AppLockAccessibilityService] yang berjalan di proses yang sama, jadi
 * perubahannya harus langsung terlihat oleh keduanya. Statusnya bertahan di
 * SharedPreferences; izin sementara ("buka dari brankas") hanya hidup di
 * memori.
 */
object AppLockState {

    private const val FILE = "aldef_applock"
    private const val KEY_LOCKED = "locked_pkgs"
    private const val KEY_HIDDEN = "hidden_pkgs"
    private const val KEY_BLOCKED = "blocked_pkgs"

    private lateinit var prefs: SharedPreferences
    private val locked = mutableSetOf<String>()
    private val hidden = mutableSetOf<String>()
    private val blocked = mutableSetOf<String>()

    // Jendela izin sementara: paket -> waktu kedaluwarsa (epoch ms).
    private val grantUntil = ConcurrentHashMap<String, Long>()

    @Volatile
    private var initialized = false

    /** true selama layanan Accessibility aktif tersambung. */
    @Volatile
    var serviceConnected = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        locked += prefs.getStringSet(KEY_LOCKED, emptySet()).orEmpty()
        hidden += prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()
        blocked += prefs.getStringSet(KEY_BLOCKED, emptySet()).orEmpty()
        initialized = true
    }

    @Synchronized fun lockedPackages(): Set<String> = locked.toSet()
    @Synchronized fun hiddenPackages(): Set<String> = hidden.toSet()
    @Synchronized fun blockedPackages(): Set<String> = blocked.toSet()

    @Synchronized fun isLocked(pkg: String): Boolean = locked.contains(pkg)
    @Synchronized fun isHidden(pkg: String): Boolean = hidden.contains(pkg)
    @Synchronized fun isUninstallBlocked(pkg: String): Boolean = blocked.contains(pkg)

    @Synchronized
    fun setLocked(pkg: String, value: Boolean) {
        if (value) locked += pkg else locked -= pkg
        persist(KEY_LOCKED, locked)
    }

    @Synchronized
    fun setHidden(pkg: String, value: Boolean) {
        if (value) hidden += pkg else hidden -= pkg
        persist(KEY_HIDDEN, hidden)
    }

    @Synchronized
    fun setUninstallBlocked(pkg: String, value: Boolean) {
        if (value) blocked += pkg else blocked -= pkg
        persist(KEY_BLOCKED, blocked)
    }

    private fun persist(key: String, set: Set<String>) {
        // Salinan baru: SharedPreferences tidak boleh diberi Set yang sama
        // yang kita mutasi terus.
        prefs.edit().putStringSet(key, HashSet(set)).apply()
    }

    /** Izinkan [pkg] dibuka tanpa layar kunci selama [durationMs]. */
    fun allowTemporarily(pkg: String, durationMs: Long = 60_000L) {
        grantUntil[pkg] = System.currentTimeMillis() + durationMs
    }

    fun isTemporarilyAllowed(pkg: String): Boolean {
        val until = grantUntil[pkg] ?: return false
        if (System.currentTimeMillis() > until) {
            grantUntil.remove(pkg)
            return false
        }
        return true
    }

    fun clearGrace(pkg: String) {
        grantUntil.remove(pkg)
    }

    /**
     * Membaca daftar layanan Accessibility yang diaktifkan pengguna di
     * Pengaturan, lalu memeriksa apakah layanan kita ada di dalamnya. Lebih
     * andal daripada hanya mengandalkan [serviceConnected] setelah proses
     * baru dimulai.
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = context.packageName + "/" + AppLockAccessibilityService::class.java.name
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
