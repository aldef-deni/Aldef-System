package com.aldef.system

import android.app.Application
import com.aldef.system.applock.AppLockState
import com.aldef.system.security.VaultRepository
import com.aldef.system.security.VaultSession

class AldefApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Daftar kunci/sembunyi aplikasi dimuat lebih awal supaya layanan
        // Accessibility langsung punya datanya begitu tersambung.
        AppLockState.init(this)
        // Sisa berkas brankas yang sempat didekripsi pada sesi sebelumnya
        // (misalnya aplikasi ditutup paksa) dibersihkan saat proses baru mulai.
        VaultSession.lock()
        runCatching { VaultRepository(this).clearCache() }
    }
}
