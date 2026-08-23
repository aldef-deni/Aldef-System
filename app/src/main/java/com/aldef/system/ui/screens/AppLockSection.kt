package com.aldef.system.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aldef.system.applock.AppLockState
import com.aldef.system.applock.DevicePolicyController
import com.aldef.system.applock.service.AppLockService
import com.aldef.system.data.InstalledApp
import com.aldef.system.data.InstalledAppsRepository
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.OutlineNeonButton
import com.aldef.system.ui.theme.Hairline
import com.aldef.system.ui.theme.NeonAmber
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary

/**
 * Pengelola kunci/sembunyi aplikasi terpasang, tampil sebagai tab di dalam
 * brankas. Setiap kali layar kembali aktif, status disinkronkan ulang: izin
 * dicek lagi dan aplikasi tersembunyi yang sempat dibuka dikembalikan ke
 * keadaan tersembunyi.
 */
@Composable
fun AppLockSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val controller = remember { DevicePolicyController(context) }
    val repository = remember { InstalledAppsRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val apps = remember { mutableStateListOf<InstalledApp>() }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }

    var usageOn by remember { mutableStateOf(AppLockState.hasUsageAccess(context)) }
    var overlayOn by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var deviceOwner by remember { mutableStateOf(controller.isDeviceOwner()) }
    var adminActive by remember { mutableStateOf(controller.isAdminActive()) }

    // Muat daftar aplikasi (dan muat ulang saat diminta).
    androidx.compose.runtime.LaunchedEffect(refreshKey) {
        loading = true
        val loaded = repository.loadApps()
        apps.clear()
        apps.addAll(loaded)
        loading = false
    }

    // Saat layar kembali aktif: perbarui status izin dan pulihkan sembunyi.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageOn = AppLockState.hasUsageAccess(context)
                overlayOn = Settings.canDrawOverlays(context)
                deviceOwner = controller.isDeviceOwner()
                adminActive = controller.isAdminActive()
                AppLockService.sync(context)
                // Aplikasi tersembunyi yang tadi dibuka dari brankas
                // dikembalikan ke keadaan tersembunyi.
                if (deviceOwner) {
                    AppLockState.hiddenPackages().forEach { pkg ->
                        if (!controller.isHiddenByPolicy(pkg)) controller.setHidden(pkg, true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filtered = remember(apps.toList(), query) {
        if (query.isBlank()) apps.toList()
        else apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        StatusStrip(
            usageOn = usageOn,
            overlayOn = overlayOn,
            deviceOwner = deviceOwner,
            adminActive = adminActive,
            onEnableUsage = {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onEnableOverlay = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
            onToggleAdmin = {
                if (adminActive) {
                    controller.removeAdmin()
                    adminActive = false
                } else {
                    context.startActivity(
                        controller.requestAdminIntent(
                            "Aktifkan agar Aldef System tidak bisa dihapus tanpa izin Anda."
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
            deviceOwnerCommand = controller.deviceOwnerAdbCommand()
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Cari aplikasi…", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Hairline,
                cursorColor = NeonCyan,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLeadingIconColor = NeonCyan,
                unfocusedLeadingIconColor = TextMuted
            )
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat daftar aplikasi…", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        deviceOwner = deviceOwner,
                        onToggleLock = { locked ->
                            AppLockState.setLocked(app.packageName, locked)
                            AppLockService.sync(context)
                            refreshKey++
                        },
                        onToggleHide = { hide ->
                            val ok = controller.setHidden(app.packageName, hide)
                            if (ok) {
                                AppLockState.setHidden(app.packageName, hide)
                                refreshKey++
                            }
                        },
                        onToggleUninstallBlock = { blocked ->
                            val ok = controller.setUninstallBlocked(app.packageName, blocked)
                            if (ok) AppLockState.setUninstallBlocked(app.packageName, blocked)
                        },
                        onOpen = { openFromVault(context, controller, repository, app.packageName) }
                    )
                }
            }
        }
    }
}

/** Membuka aplikasi dari dalam brankas, melewati kunci sesaat. */
private fun openFromVault(
    context: Context,
    controller: DevicePolicyController,
    repository: InstalledAppsRepository,
    pkg: String
) {
    // Kalau tersembunyi lewat Device Owner, tampilkan dulu supaya bisa dibuka;
    // saat kembali ke brankas, disembunyikan lagi.
    if (AppLockState.isHidden(pkg) && controller.isDeviceOwner()) {
        controller.setHidden(pkg, false)
    }
    AppLockState.allowTemporarily(pkg)
    val intent = repository.launchIntentFor(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent != null) runCatching { context.startActivity(intent) }
}

@Composable
private fun StatusStrip(
    usageOn: Boolean,
    overlayOn: Boolean,
    deviceOwner: Boolean,
    adminActive: Boolean,
    onEnableUsage: () -> Unit,
    onEnableOverlay: () -> Unit,
    onToggleAdmin: () -> Unit,
    deviceOwnerCommand: String
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val allReady = usageOn && overlayOn

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        borderTint = listOf(
            (if (allReady) NeonGreen else NeonAmber).copy(alpha = 0.5f),
            Color.Transparent
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (allReady) NeonGreen else NeonAmber)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (allReady) "Kunci aplikasi aktif" else "Perlu izin agar kunci bekerja",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(if (expanded) "Tutup" else "Atur", color = NeonCyan, fontSize = 11.sp)
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    PermissionLine(
                        label = "Izin Akses Penggunaan",
                        granted = usageOn,
                        actionText = "Aktifkan",
                        onAction = onEnableUsage
                    )
                    PermissionLine(
                        label = "Tampilkan di atas aplikasi lain",
                        granted = overlayOn,
                        actionText = "Izinkan",
                        onAction = onEnableOverlay
                    )
                    PermissionLine(
                        label = "Proteksi uninstall Aldef System",
                        granted = adminActive,
                        actionText = if (adminActive) "Matikan" else "Aktifkan",
                        onAction = onToggleAdmin
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (deviceOwner) {
                            "Device Owner aktif — sembunyikan total & blokir uninstall aplikasi lain tersedia."
                        } else {
                            "Sembunyikan total (hilang dari drawer & pencarian) dan blokir uninstall aplikasi lain butuh Device Owner. Aktifkan sekali lewat ADB dari komputer:"
                        },
                        color = if (deviceOwner) NeonGreen else TextMuted,
                        fontSize = 11.sp
                    )
                    if (!deviceOwner) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface2)
                                .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                                .clickable { copyToClipboard(context, deviceOwnerCommand) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = deviceOwnerCommand,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = "Salin",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Perangkat mungkin harus tanpa akun Google dulu (kadang perlu factory reset).",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionLine(
    label: String,
    granted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (granted) NeonGreen else NeonRed)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            text = if (granted) "OK" else actionText,
            color = if (granted) NeonGreen else NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !granted, onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    deviceOwner: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onToggleHide: (Boolean) -> Unit,
    onToggleUninstallBlock: (Boolean) -> Unit,
    onOpen: () -> Unit
) {
    val locked = AppLockState.isLocked(app.packageName)
    val hidden = AppLockState.isHidden(app.packageName)
    val blocked = AppLockState.isUninstallBlocked(app.packageName)
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.icon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Text(
                            text = app.label.take(1).uppercase(),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (locked || hidden || blocked) {
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (locked) StateChip("TERKUNCI", NeonOrange)
                            if (hidden) StateChip("TERSEMBUNYI", NeonViolet)
                            if (blocked) StateChip("TERPROTEKSI", NeonGreen)
                        }
                    } else {
                        Spacer(Modifier.height(3.dp))
                        Text(app.packageName, color = TextMuted, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionToggle(
                            modifier = Modifier.weight(1f),
                            text = if (locked) "BUKA KUNCI" else "KUNCI",
                            icon = if (locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            active = locked,
                            colors = listOf(NeonOrange, NeonMagenta),
                            onClick = { onToggleLock(!locked) }
                        )
                        ActionToggle(
                            modifier = Modifier.weight(1f),
                            text = if (hidden) "TAMPILKAN" else "SEMBUNYIKAN",
                            icon = if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            active = hidden,
                            enabled = deviceOwner,
                            colors = listOf(NeonViolet, NeonCyan),
                            onClick = { onToggleHide(!hidden) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionToggle(
                            modifier = Modifier.weight(1f),
                            text = if (blocked) "IZINKAN HAPUS" else "CEGAH HAPUS",
                            icon = Icons.Rounded.Lock,
                            active = blocked,
                            enabled = deviceOwner,
                            colors = listOf(NeonGreen, NeonCyan),
                            onClick = { onToggleUninstallBlock(!blocked) }
                        )
                        OutlineNeonButton(
                            text = "BUKA",
                            modifier = Modifier.weight(1f),
                            colors = listOf(NeonCyan, NeonViolet),
                            leading = Icons.Rounded.OpenInNew,
                            onClick = onOpen
                        )
                    }
                    if (!deviceOwner) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Sembunyikan & cegah-hapus perlu Device Owner (lihat panel status di atas).",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateChip(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 8.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionToggle(
    modifier: Modifier,
    text: String,
    icon: ImageVector,
    active: Boolean,
    colors: List<Color>,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = if (!enabled) TextMuted else if (active) colors.first() else TextSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active && enabled) {
                    Brush.linearGradient(colors.map { it.copy(alpha = 0.18f) })
                } else {
                    Brush.linearGradient(listOf(Surface1.copy(alpha = 0.7f), Surface1.copy(alpha = 0.7f)))
                }
            )
            .border(
                1.dp,
                if (active && enabled) colors.first().copy(alpha = 0.6f) else Hairline,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("adb", text))
}

