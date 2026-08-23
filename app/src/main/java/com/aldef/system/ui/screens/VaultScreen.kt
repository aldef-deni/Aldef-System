package com.aldef.system.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.aldef.system.data.AppPrefs
import com.aldef.system.security.VaultEntry
import com.aldef.system.security.VaultRepository
import com.aldef.system.security.VaultSession
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.CircleIconButton
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.NeonButton
import com.aldef.system.ui.components.OutlineNeonButton
import com.aldef.system.ui.theme.CoolSweep
import com.aldef.system.ui.theme.Hairline
import com.aldef.system.ui.theme.NeonAmber
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.SecretKey

/**
 * Brankas berkas. Hanya bisa dicapai lewat kalkulator (ketik PIN lalu `=`),
 * dan isinya tidak pernah tersimpan dalam bentuk terbaca di penyimpanan.
 */
@Composable
fun VaultScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val repository = remember { VaultRepository(context) }
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf(VaultSession.pin) }
    var key by remember { mutableStateOf<SecretKey?>(null) }
    var deriving by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var unreadable by remember { mutableStateOf(0) }
    val entries = remember { mutableStateListOf<VaultEntry>() }

    var pendingDelete by remember { mutableStateOf<VaultEntry?>(null) }
    var pendingExport by remember { mutableStateOf<VaultEntry?>(null) }
    var showPinChange by remember { mutableStateOf(false) }

    fun lockAndLeave() {
        VaultSession.lock()
        repository.clearCache()
        navController.popBackStack()
    }

    // Penurunan kunci PBKDF2 sengaja lambat; jalankan di luar main thread.
    LaunchedEffect(pin) {
        val current = pin ?: return@LaunchedEffect
        deriving = true
        val derived = withContext(Dispatchers.Default) { repository.deriveKey(current) }
        val listing = withContext(Dispatchers.IO) { repository.list(derived) }
        key = derived
        entries.clear()
        entries.addAll(listing.entries)
        unreadable = listing.unreadable
        deriving = false
    }

    fun reload(currentKey: SecretKey) {
        scope.launch {
            val listing = withContext(Dispatchers.IO) { repository.list(currentKey) }
            entries.clear()
            entries.addAll(listing.entries)
            unreadable = listing.unreadable
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val currentKey = key ?: return@rememberLauncherForActivityResult
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Mengunci ${uris.size} berkas…"
            val failures = withContext(Dispatchers.IO) {
                uris.count { uri -> repository.import(currentKey, uri).isFailure }
            }
            busy = null
            status = if (failures == 0) {
                "${uris.size} berkas terkunci di brankas" to false
            } else {
                "$failures dari ${uris.size} berkas gagal dikunci" to true
            }
            reload(currentKey)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { target ->
        val entry = pendingExport
        val currentKey = key
        pendingExport = null
        if (target == null || entry == null || currentKey == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Mengekspor ${entry.name}…"
            val result = withContext(Dispatchers.IO) {
                repository.exportTo(currentKey, entry, target)
            }
            busy = null
            status = if (result.isSuccess) {
                "${entry.name} diekspor" to false
            } else {
                "Gagal mengekspor: ${result.exceptionOrNull()?.message}" to true
            }
        }
    }

    fun openEntry(entry: VaultEntry) {
        val currentKey = key ?: return
        scope.launch {
            busy = "Membuka ${entry.name}…"
            val result = withContext(Dispatchers.IO) { repository.decryptToCache(currentKey, entry) }
            busy = null
            result.onSuccess { file ->
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, entry.mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    status = "Tidak ada aplikasi yang bisa membuka ${entry.name}" to true
                }
            }.onFailure {
                status = "Gagal membuka: ${it.message}" to true
            }
        }
    }

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = CoolSweep, intensity = 0.8f) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AldefTopBar(
                title = "Brankas",
                subtitle = "TERENKRIPSI AES-256",
                onBack = { lockAndLeave() },
                actions = {
                    if (key != null) {
                        CircleIconButton(
                            icon = Icons.Rounded.Shield,
                            contentDescription = "Ganti PIN",
                            onClick = { showPinChange = true }
                        )
                        Spacer(Modifier.width(8.dp))
                        CircleIconButton(
                            icon = Icons.Rounded.Lock,
                            contentDescription = "Kunci",
                            onClick = { lockAndLeave() }
                        )
                    }
                }
            )

            when {
                pin == null -> PinGate(
                    title = "Masukkan PIN Brankas",
                    onSubmit = { candidate ->
                        if (prefs.checkVaultPin(candidate)) {
                            VaultSession.unlock(candidate)
                            pin = candidate
                            true
                        } else {
                            false
                        }
                    }
                )

                deriving || key == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("Membuka brankas…", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                else -> VaultContent(
                    entries = entries,
                    unreadable = unreadable,
                    totalBytes = remember(entries.size) { repository.totalBytes() },
                    pinIsDefault = prefs.vaultPinIsDefault,
                    onAdd = { importLauncher.launch(arrayOf("*/*")) },
                    onOpen = { openEntry(it) },
                    onExport = {
                        pendingExport = it
                        exportLauncher.launch(it.name)
                    },
                    onDelete = { pendingDelete = it },
                    onChangePin = { showPinChange = true }
                )
            }
        }

        AnimatedVisibility(
            visible = busy != null || status != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(18.dp)
                .navigationBarsPadding()
        ) {
            val isError = status?.second == true
            GlassCard(
                shape = RoundedCornerShape(16.dp),
                borderTint = listOf(
                    (if (isError) NeonRed else NeonGreen).copy(alpha = 0.5f),
                    Color.Transparent
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (busy != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = busy ?: status?.first.orEmpty(),
                        color = if (isError) NeonRed else TextPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { status = null }
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Surface1,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Hapus permanen?") },
            text = {
                Text("${entry.name} akan dihapus dari brankas dan tidak bisa dikembalikan.")
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.delete(entry)
                    entries.remove(entry)
                    status = "${entry.name} dihapus" to false
                    pendingDelete = null
                }) {
                    Text("Hapus", color = NeonRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    if (showPinChange) {
        ChangePinDialog(
            currentPinValid = { prefs.checkVaultPin(it) },
            onDismiss = { showPinChange = false },
            onConfirm = { oldPin, newPin ->
                // Kunci lama masih dibutuhkan untuk membaca ulang isi brankas
                // sebelum dienkripsi ulang dengan kunci baru.
                scope.launch {
                    busy = "Mengenkripsi ulang brankas…"
                    val result = withContext(Dispatchers.Default) {
                        val oldKey = repository.deriveKey(oldPin)
                        val newKey = repository.deriveKey(newPin)
                        val existing = repository.list(oldKey).entries
                        val rewritten = mutableListOf<VaultEntry>()

                        // Dua tahap: semua salinan baru ditulis lebih dulu, dan
                        // PIN baru dipasang hanya kalau seluruhnya berhasil.
                        // Kalau ada yang gagal di tengah jalan, salinan baru
                        // dibuang dan brankas kembali ke keadaan semula.
                        runCatching {
                            existing.forEach { entry ->
                                val plain = repository.decryptToCache(oldKey, entry).getOrThrow()
                                rewritten += repository.import(newKey, Uri.fromFile(plain)).getOrThrow()
                                plain.delete()
                            }
                            prefs.setVaultPin(newPin)
                            existing.forEach { repository.delete(it) }
                            repository.clearCache()
                            newKey
                        }.onFailure {
                            rewritten.forEach { repository.delete(it) }
                            repository.clearCache()
                        }
                    }
                    busy = null
                    result.onSuccess { newKey ->
                        VaultSession.unlock(newPin)
                        pin = newPin
                        key = newKey
                        reload(newKey)
                        status = "PIN brankas diperbarui" to false
                    }.onFailure {
                        status = "Gagal mengganti PIN: ${it.message}" to true
                    }
                }
                showPinChange = false
            }
        )
    }
}

@Composable
private fun VaultContent(
    entries: List<VaultEntry>,
    unreadable: Int,
    totalBytes: Long,
    pinIsDefault: Boolean,
    onAdd: () -> Unit,
    onOpen: (VaultEntry) -> Unit,
    onExport: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit,
    onChangePin: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryTile(
                label = "Berkas",
                value = entries.size.toString(),
                accent = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label = "Ukuran",
                value = formatSize(totalBytes),
                accent = NeonViolet,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label = "Sandi",
                value = "AES-256",
                accent = NeonGreen,
                modifier = Modifier.weight(1f)
            )
        }

        if (pinIsDefault) {
            Spacer(Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                borderTint = listOf(NeonAmber.copy(alpha = 0.6f), NeonAmber.copy(alpha = 0.15f)),
                onClick = onChangePin
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NeonAmber)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "PIN masih bawaan — ketuk untuk menggantinya",
                        color = NeonAmber,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (unreadable > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "$unreadable berkas tidak terbaca dengan PIN ini",
                modifier = Modifier.padding(horizontal = 22.dp),
                color = NeonRed,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = NeonCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Brankas kosong",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Berkas yang ditambahkan di sini dienkripsi dan disimpan di ruang privat aplikasi — tidak muncul di galeri maupun pengelola berkas.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    VaultRow(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onExport = { onExport(entry) },
                        onDelete = { onDelete(entry) }
                    )
                }
            }
        }

        NeonButton(
            text = "TAMBAH BERKAS KE BRANKAS",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            colors = listOf(NeonCyan, NeonViolet),
            leading = Icons.Rounded.Add,
            onClick = onAdd
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: String, accent: Color, modifier: Modifier) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(label.uppercase(), color = TextMuted, fontSize = 9.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NumericFont
            )
        }
    }
}

@Composable
private fun VaultRow(
    entry: VaultEntry,
    onOpen: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale("in", "ID")) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    iconAccent(entry.mime).copy(alpha = 0.22f),
                                    Surface2.copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconFor(entry.mime),
                        contentDescription = null,
                        tint = iconAccent(entry.mime),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${formatSize(entry.size)}  ·  ${dateFormat.format(Date(entry.addedAt))}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlineNeonButton(
                        text = "BUKA",
                        modifier = Modifier.weight(1f),
                        colors = listOf(NeonCyan, NeonViolet),
                        leading = Icons.Rounded.OpenInNew,
                        onClick = onOpen
                    )
                    OutlineNeonButton(
                        text = "EKSPOR",
                        modifier = Modifier.weight(1f),
                        colors = listOf(NeonGreen, NeonCyan),
                        leading = Icons.Rounded.DriveFileMove,
                        onClick = onExport
                    )
                    OutlineNeonButton(
                        text = "HAPUS",
                        modifier = Modifier.weight(1f),
                        colors = listOf(NeonRed, NeonMagenta),
                        leading = Icons.Rounded.Delete,
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

/** Papan PIN untuk membuka brankas saat sesi sebelumnya sudah berakhir. */
@Composable
private fun PinGate(title: String, onSubmit: (String) -> Boolean) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (error) NeonRed else NeonCyan,
            modifier = Modifier.size(38.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { index ->
                val filled = index < input.length
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                error -> NeonRed
                                filled -> NeonCyan
                                else -> Surface2
                            }
                        )
                        .border(
                            1.dp,
                            if (filled) Color.Transparent else Hairline,
                            CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { label ->
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (label.isEmpty()) Color.Transparent else Surface1.copy(alpha = 0.8f))
                                .border(
                                    1.dp,
                                    if (label.isEmpty()) Color.Transparent else Hairline,
                                    CircleShape
                                )
                                .clickable(enabled = label.isNotEmpty()) {
                                    error = false
                                    when (label) {
                                        "⌫" -> input = input.dropLast(1)
                                        else -> if (input.length < 6) input += label
                                    }
                                    if (input.length >= 4 && label != "⌫") {
                                        if (onSubmit(input)) return@clickable
                                    }
                                    if (input.length == 6) {
                                        error = true
                                        input = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (label == "⌫") {
                                Icon(
                                    imageVector = Icons.Rounded.Backspace,
                                    contentDescription = "Hapus",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (label.isNotEmpty()) {
                                Text(
                                    text = label,
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontFamily = NumericFont
                                )
                            }
                        }
                    }
                }
            }
        }

        if (error) {
            Spacer(Modifier.height(16.dp))
            Text("PIN salah", color = NeonRed, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChangePinDialog(
    currentPinValid: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (oldPin: String, newPin: String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Ganti PIN Brankas") },
        text = {
            Column {
                Text(
                    text = "Semua berkas akan dienkripsi ulang dengan PIN baru.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(14.dp))
                PinInput("PIN lama", oldPin) { oldPin = it; error = null }
                Spacer(Modifier.height(10.dp))
                PinInput("PIN baru (4–6 digit)", newPin) { newPin = it; error = null }
                Spacer(Modifier.height(10.dp))
                PinInput("Ulangi PIN baru", confirmPin) { confirmPin = it; error = null }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, color = NeonRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    !currentPinValid(oldPin) -> "PIN lama salah"
                    newPin.length !in 4..6 -> "PIN baru harus 4–6 digit"
                    newPin != confirmPin -> "Ulangan PIN tidak cocok"
                    else -> null
                }
                if (error == null) onConfirm(oldPin, newPin)
            }) {
                Text("Simpan", color = NeonOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}

@Composable
private fun PinInput(label: String, value: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }.take(6)) },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
        ),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = Hairline,
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = TextMuted,
            cursorColor = NeonCyan,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun iconFor(mime: String): ImageVector = when {
    mime.startsWith("image/") -> Icons.Rounded.Image
    mime.startsWith("video/") -> Icons.Rounded.VideoFile
    mime.startsWith("audio/") -> Icons.Rounded.AudioFile
    mime == "application/pdf" -> Icons.Rounded.PictureAsPdf
    mime.startsWith("text/") -> Icons.Rounded.Article
    else -> Icons.Rounded.InsertDriveFile
}

private fun iconAccent(mime: String): Color = when {
    mime.startsWith("image/") -> NeonMagenta
    mime.startsWith("video/") -> NeonViolet
    mime.startsWith("audio/") -> NeonCyan
    mime == "application/pdf" -> NeonRed
    mime.startsWith("text/") -> NeonGreen
    else -> NeonAmber
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.US, if (value >= 10) "%.0f %s" else "%.1f %s", value, units[index])
}
