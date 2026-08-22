package com.aldef.system

import android.app.Application
import com.aldef.system.security.VaultRepository
import com.aldef.system.security.VaultSession

class AldefApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sisa berkas brankas yang sempat didekripsi pada sesi sebelumnya
        // (misalnya aplikasi ditutup paksa) dibersihkan saat proses baru mulai.
        VaultSession.lock()
        runCatching { VaultRepository(this).clearCache() }
    }
}
