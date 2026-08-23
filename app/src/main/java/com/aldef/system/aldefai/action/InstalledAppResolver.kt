package com.aldef.system.aldefai.action

import android.content.Context
import android.content.Intent
import com.aldef.system.applock.AppLockState

/**
 * Mencari aplikasi terpasang berdasarkan nama yang diucapkan.
 *
 * Hanya aplikasi yang punya ikon peluncur (bisa dibuka) yang dipertimbangkan,
 * dan aplikasi yang **disembunyikan** lewat brankas (AppLockState) dikecualikan —
 * sesuai permintaan: buka aplikasi apa pun kecuali yang di-hide.
 */
object InstalledAppResolver {

    data class AppEntry(val label: String, val pkg: String)

    fun launchable(context: Context): List<AppEntry> {
        AppLockState.init(context)
        val hidden = AppLockState.hiddenPackages()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { (pkg, _) -> pkg != context.packageName && pkg !in hidden }
            .map { (pkg, label) -> AppEntry(label, pkg) }
            .distinctBy { it.pkg }
            .toList()
    }

    /** Mengembalikan aplikasi paling cocok dengan [query], atau null bila tak yakin. */
    fun findBest(context: Context, query: String): AppEntry? {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return null
        val apps = launchable(context)
        var best: AppEntry? = null
        var bestScore = 0
        for (app in apps) {
            val score = score(app.label.lowercase(), q)
            if (score > bestScore) {
                bestScore = score
                best = app
            }
        }
        return if (bestScore >= 40) best else null
    }

    private fun score(label: String, q: String): Int {
        if (label == q) return 100
        if (label.startsWith(q) || q.startsWith(label)) return 80
        if (label.contains(q) || q.contains(label)) return 60
        val labelWords = label.split(' ')
        val queryWords = q.split(' ')
        val overlap = queryWords.count { qw ->
            qw.length >= 3 && labelWords.any { it.contains(qw) || qw.contains(it) }
        }
        return overlap * 25
    }
}
