package com.aldef.system.security

/**
 * Menyimpan PIN brankas selama satu sesi buka, hanya di memori.
 *
 * Kalkulator sudah memverifikasi PIN sebelum berpindah layar, jadi tanpa ini
 * brankas harus menanyakan PIN yang sama dua kali. Isinya dibuang saat brankas
 * dikunci atau proses aplikasi berakhir.
 */
object VaultSession {
    @Volatile
    var pin: String? = null
        private set

    fun unlock(pin: String) {
        this.pin = pin
    }

    fun lock() {
        pin = null
    }
}
