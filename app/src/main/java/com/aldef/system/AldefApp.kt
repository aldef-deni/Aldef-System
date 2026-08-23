package com.aldef.system

import android.app.Application
import com.aldef.system.applock.AppLockState
import com.aldef.system.aldefai.service.AldefAiService
import com.aldef.system.applock.service.AppLockService
import com.aldef.system.notify.HolidayReminder
import com.aldef.system.security.VaultRepository
import com.aldef.system.security.VaultSession

class AldefApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Daftar kunci/sembunyi aplikasi dimuat lebih awal supaya layanan
        // layanan proteksi langsung punya datanya begitu berjalan.
        AppLockState.init(this)
        // Lanjutkan proteksi aplikasi kalau ada yang terkunci dan izinnya masih
        // lengkap (mis. setelah proses aplikasi dimulai ulang).
        runCatching { AppLockService.sync(this) }
        // Nyalakan strip pemicu ALDEF AI bila diaktifkan pengguna.
        runCatching { AldefAiService.sync(this) }
        // Jadwalkan pengingat H-1 libur nasional.
        runCatching { HolidayReminder.schedule(this) }
        // Sisa berkas brankas yang sempat didekripsi pada sesi sebelumnya
        // (misalnya aplikasi ditutup paksa) dibersihkan saat proses baru mulai.
        VaultSession.lock()
        runCatching { VaultRepository(this).clearCache() }
    }
}
