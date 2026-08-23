package com.aldef.system.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.aldef.system.applock.AppLockState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystemApp: Boolean
)

/** Membaca daftar aplikasi terpasang yang punya peluncur, plus ikonnya. */
class InstalledAppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val pm = appContext.packageManager
    private val ownPackage = appContext.packageName

    suspend fun loadApps(iconSizePx: Int = 128): List<InstalledApp> = withContext(Dispatchers.IO) {
        val result = linkedMapOf<String, InstalledApp>()

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0).forEach { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == ownPackage || result.containsKey(pkg)) return@forEach
            val appInfo = info.activityInfo.applicationInfo
            result[pkg] = InstalledApp(
                packageName = pkg,
                label = info.loadLabel(pm).toString(),
                icon = runCatching { info.loadIcon(pm).toImageBitmap(iconSizePx) }.getOrNull(),
                isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }

        // Aplikasi yang disembunyikan lewat Device Owner tidak lagi muncul di
        // daftar peluncur, jadi ditambahkan manual supaya masih bisa dikelola
        // (ditampilkan kembali) dari brankas.
        AppLockState.hiddenPackages().forEach { pkg ->
            if (pkg == ownPackage || result.containsKey(pkg)) return@forEach
            runCatching {
                @Suppress("DEPRECATION")
                val appInfo = pm.getApplicationInfo(
                    pkg,
                    PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS
                )
                result[pkg] = InstalledApp(
                    packageName = pkg,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = runCatching { pm.getApplicationIcon(appInfo).toImageBitmap(iconSizePx) }.getOrNull(),
                    isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }.onFailure {
                result[pkg] = InstalledApp(pkg, pkg, null, false)
            }
        }

        result.values.sortedBy { it.label.lowercase() }
    }

    fun launchIntentFor(pkg: String): Intent? = pm.getLaunchIntentForPackage(pkg)
}

private fun Drawable.toImageBitmap(size: Int): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return Bitmap.createScaledBitmap(bitmap, size, size, true).asImageBitmap()
    }
    val bitmap = Bitmap.createBitmap(
        size.coerceAtLeast(1),
        size.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}
