package com.aldef.system.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Menyalakan kembali proteksi aplikasi setelah perangkat dinyalakan ulang. */
class AppLockBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AppLockService.sync(context)
        }
    }
}
