package com.aldef.system.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Penyimpanan preferensi sederhana. PIN brankas tidak pernah disimpan apa
 * adanya — hanya salt acak dan hash SHA-256 dari (salt + PIN), supaya isi
 * berkas preferensi tidak membocorkan PIN.
 */
class AppPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Sudah pernah mendaftarkan biometrik untuk login cepat. */
    var biometricEnrolled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /** Nama yang disapa di dasbor. */
    var displayName: String
        get() = prefs.getString(KEY_NAME, "Aldef") ?: "Aldef"
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    /** true = km/jam, false = mil/jam. */
    var speedInKmh: Boolean
        get() = prefs.getBoolean(KEY_SPEED_UNIT, true)
        set(value) = prefs.edit().putBoolean(KEY_SPEED_UNIT, value).apply()

    /** Rekor kecepatan tertinggi yang pernah tercatat (km/jam). */
    var topSpeedKmh: Float
        get() = prefs.getFloat(KEY_TOP_SPEED, 0f)
        set(value) = prefs.edit().putFloat(KEY_TOP_SPEED, value).apply()

    // ---------- PIN brankas ----------

    val vaultPinIsDefault: Boolean
        get() = prefs.getString(KEY_PIN_HASH, null) == null

    /**
     * PIN bawaan sebelum pengguna menggantinya. Sengaja tidak ditampilkan di
     * UI mana pun supaya pintu masuk brankas tetap tersembunyi.
     */
    fun defaultPin(): String = DEFAULT_PIN

    fun checkVaultPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
            ?: return pin == DEFAULT_PIN
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return constantTimeEquals(storedHash, hash(pin, Base64.decode(salt, Base64.NO_WRAP)))
    }

    fun setVaultPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(md.digest(), Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private companion object {
        const val FILE = "aldef_prefs"
        const val KEY_BIOMETRIC = "biometric_enrolled"
        const val KEY_NAME = "display_name"
        const val KEY_SPEED_UNIT = "speed_kmh"
        const val KEY_TOP_SPEED = "top_speed"
        const val KEY_PIN_HASH = "vault_pin_hash"
        const val KEY_PIN_SALT = "vault_pin_salt"
        const val DEFAULT_PIN = "1974"
    }
}
