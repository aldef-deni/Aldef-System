package com.aldef.system.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.aldef.system.data.QrisParser
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.CircleIconButton
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.GradientDivider
import com.aldef.system.ui.components.NeonButton
import com.aldef.system.ui.components.OutlineNeonButton
import com.aldef.system.ui.theme.NeonAmber
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import com.aldef.system.ui.theme.WarmSweep
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "QrisScanner"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrisScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Hasil dipegang di level layar supaya panel hasil muncul baik dari kamera
    // maupun dari gambar galeri, bahkan ketika izin kamera ditolak.
    var result by remember { mutableStateOf<String?>(null) }
    var decoding by remember { mutableStateOf(false) }
    var galleryError by remember { mutableStateOf<String?>(null) }

    // Pemindai terpisah untuk gambar galeri; kamera memakai miliknya sendiri.
    val galleryScanner = remember {
        BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    DisposableEffect(Unit) { onDispose { galleryScanner.close() } }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        decoding = true
        galleryError = null
        runCatching { InputImage.fromFilePath(context, uri) }
            .onSuccess { image ->
                galleryScanner.process(image)
                    .addOnSuccessListener { codes ->
                        decoding = false
                        val value = codes.firstNotNullOfOrNull { it.rawValue }
                        if (value != null) {
                            result = value
                            vibrate(context)
                        } else {
                            galleryError = "Tidak ada kode QR yang terbaca pada gambar itu"
                        }
                    }
                    .addOnFailureListener {
                        decoding = false
                        galleryError = "Gagal membaca gambar"
                    }
            }
            .onFailure {
                decoding = false
                galleryError = "Gagal membuka gambar"
            }
    }

    fun openGallery() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = WarmSweep) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            AldefTopBar(
                title = "Pemindai QRIS",
                subtitle = "PINDAI ATAU AMBIL DARI GALERI",
                onBack = { navController.popBackStack() },
                actions = {
                    CircleIconButton(
                        icon = Icons.Rounded.Image,
                        contentDescription = "Ambil dari galeri",
                        onClick = { openGallery() }
                    )
                }
            )

            if (cameraPermission.status.isGranted) {
                ScannerSurface(
                    modifier = Modifier.weight(1f),
                    paused = result != null || decoding,
                    onResult = { result = it }
                )
            } else {
                PermissionNotice(
                    modifier = Modifier.weight(1f),
                    permanentlyDenied = !cameraPermission.status.shouldShowRationale,
                    onRequest = { cameraPermission.launchPermissionRequest() },
                    onPickGallery = { openGallery() }
                )
            }
        }

        if (decoding) {
            GlassCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NeonOrange,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Membaca gambar…", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }

        if (galleryError != null) {
            GlassCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp, start = 24.dp, end = 24.dp),
                borderTint = listOf(NeonRed.copy(alpha = 0.5f), Color.Transparent)
            ) {
                Text(
                    text = galleryError ?: "",
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable { galleryError = null },
                    color = NeonRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = result != null,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(320)),
            exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(240)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            result?.let { payload ->
                ResultSheet(
                    payload = payload,
                    onScanAgain = {
                        result = null
                        galleryError = null
                    },
                    onCopy = { copyToClipboard(context, payload) }
                )
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScannerSurface(
    modifier: Modifier = Modifier,
    paused: Boolean,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Dibaca dari dalam analyzer (thread lain); rememberUpdatedState menjaga
    // nilainya selalu yang terbaru tanpa memicu ikatan ulang kamera.
    val isPaused by rememberUpdatedState(paused)
    val emitResult by rememberUpdatedState(onResult)
    var torchOn by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    val media = imageProxy.image
                    if (media == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(
                        media,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    scanner.process(input)
                        .addOnSuccessListener { codes ->
                            // Hasil pertama menang; pemindaian berhenti selama
                            // panel hasil terbuka atau gambar galeri diproses.
                            if (!isPaused) {
                                codes.firstNotNullOfOrNull { it.rawValue }?.let { value ->
                                    emitResult(value)
                                    vibrate(context)
                                }
                            }
                        }
                        .addOnFailureListener { Log.w(TAG, "Gagal membaca frame", it) }
                        .addOnCompleteListener { imageProxy.close() }
                }

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }.onFailure {
                Log.e(TAG, "Kamera gagal dijalankan", it)
                cameraError = it.message ?: "Kamera tidak bisa dijalankan"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(torchOn, camera) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torchOn)
    }

    Box(modifier = modifier.fillMaxSize()) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        ScannerOverlay(active = !paused)

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        ) {
            CircleIconButton(
                icon = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                contentDescription = "Senter",
                active = torchOn,
                onClick = { torchOn = !torchOn }
            )
        }

        if (cameraError != null) {
            GlassCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
            ) {
                Text(
                    text = cameraError ?: "",
                    modifier = Modifier.padding(20.dp),
                    color = NeonRed,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Bingkai pemindai: area gelap di luar kotak, sudut neon, dan garis sapuan. */
@Composable
private fun ScannerOverlay(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "scanline")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "sweepPos"
    )

    Canvas(Modifier.fillMaxSize()) {
        val boxSize = size.minDimension * 0.72f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f - size.height * 0.05f
        val rect = Rect(Offset(left, top), Size(boxSize, boxSize))
        val corner = 28f

        val hole = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect,
                    androidx.compose.ui.geometry.CornerRadius(corner, corner)
                )
            )
        }

        // Redupkan seluruh layar kecuali kotak pemindaian.
        clipPath(hole, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.62f))
        }

        // Sudut-sudut bingkai.
        val armLength = boxSize * 0.16f
        val stroke = Stroke(width = 4f)
        val accent = if (active) NeonOrange else NeonGreen
        listOf(
            Triple(rect.topLeft, Offset(1f, 0f), Offset(0f, 1f)),
            Triple(rect.topRight, Offset(-1f, 0f), Offset(0f, 1f)),
            Triple(rect.bottomLeft, Offset(1f, 0f), Offset(0f, -1f)),
            Triple(rect.bottomRight, Offset(-1f, 0f), Offset(0f, -1f))
        ).forEach { (corner0, hDir, vDir) ->
            drawLine(
                color = accent,
                start = corner0,
                end = corner0 + Offset(hDir.x * armLength, hDir.y * armLength),
                strokeWidth = stroke.width,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = accent,
                start = corner0,
                end = corner0 + Offset(vDir.x * armLength, vDir.y * armLength),
                strokeWidth = stroke.width,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        if (active) {
            val y = rect.top + rect.height * sweep
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NeonAmber.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    startY = y - 60f,
                    endY = y + 60f
                ),
                topLeft = Offset(rect.left, (y - 60f).coerceAtLeast(rect.top)),
                size = Size(rect.width, 120f.coerceAtMost(rect.bottom - y + 60f))
            )
            drawLine(
                color = NeonAmber,
                start = Offset(rect.left, y),
                end = Offset(rect.right, y),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun ResultSheet(payload: String, onScanAgain: () -> Unit, onCopy: () -> Unit) {
    val context = LocalContext.current
    val qris = remember(payload) { QrisParser.parse(payload) }
    val isUrl = remember(payload) {
        payload.startsWith("http://", true) || payload.startsWith("https://", true)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(28.dp),
        borderTint = listOf(NeonOrange.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.25f)),
        fill = listOf(Surface1.copy(alpha = 0.97f), Surface1.copy(alpha = 0.99f))
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (qris != null) "QRIS Terbaca" else "Kode QR Terbaca",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                if (qris != null) {
                    Text(
                        text = if (qris.crcValid) "CRC OK" else "CRC TIDAK COCOK",
                        color = if (qris.crcValid) NeonGreen else NeonRed,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            GradientDivider()
            Spacer(Modifier.height(14.dp))

            if (qris != null) {
                qris.merchantName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }
                listOfNotNull(qris.merchantCity, qris.postalCode).takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                QrisParser.formatAmount(qris.amount, qris.currencyLabel)?.let { amount ->
                    Spacer(Modifier.height(14.dp))
                    Text("NOMINAL", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.6.sp)
                    Text(
                        text = amount,
                        fontFamily = NumericFont,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                }

                Spacer(Modifier.height(16.dp))

                InfoRow("Tipe", if (qris.isDynamic) "Dinamis (nominal terkunci)" else "Statis")
                qris.acquirer?.let { InfoRow("Penyelenggara", it) }
                qris.accounts.firstNotNullOfOrNull { it.merchantPan }?.let { InfoRow("PAN Merchant", it) }
                qris.accounts.firstNotNullOfOrNull { it.merchantId }?.let { InfoRow("ID Merchant", it) }
                qris.merchantCriteria?.let { InfoRow("Kriteria", it) }
                qris.merchantCategoryCode?.let { InfoRow("MCC", it) }
                qris.currencyLabel?.let { InfoRow("Mata Uang", it) }
                qris.countryCode?.let { InfoRow("Negara", it) }
                qris.terminalLabel?.let { InfoRow("Terminal", it) }
                qris.referenceLabel?.let { InfoRow("Referensi", it) }
                qris.billNumber?.let { InfoRow("No. Tagihan", it) }

                Spacer(Modifier.height(14.dp))
                Text("MUATAN MENTAH", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(4.dp))
            } else {
                Text("ISI KODE", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = payload,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontFamily = NumericFont
            )

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(
                    text = "PINDAI LAGI",
                    modifier = Modifier.weight(1f),
                    leading = Icons.Rounded.QrCodeScanner,
                    onClick = onScanAgain
                )
                OutlineNeonButton(
                    text = "SALIN",
                    modifier = Modifier.weight(1f),
                    leading = Icons.Rounded.ContentCopy,
                    onClick = onCopy
                )
            }

            if (isUrl) {
                Spacer(Modifier.height(10.dp))
                OutlineNeonButton(
                    text = "BUKA TAUTAN",
                    modifier = Modifier.fillMaxWidth(),
                    leading = Icons.Rounded.OpenInNew,
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payload)))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(120.dp),
            color = TextMuted,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionNotice(
    modifier: Modifier,
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onPickGallery: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(modifier = Modifier.padding(28.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Izin kamera dibutuhkan",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pemindai QRIS membaca kode langsung dari kamera. Tidak ada gambar yang disimpan atau dikirim ke mana pun. Anda juga bisa mengambil QRIS dari galeri tanpa kamera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                NeonButton(
                    text = if (permanentlyDenied) "BUKA PENGATURAN" else "IZINKAN KAMERA",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (permanentlyDenied) {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                        } else {
                            onRequest()
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                OutlineNeonButton(
                    text = "AMBIL DARI GALERI",
                    modifier = Modifier.fillMaxWidth(),
                    leading = Icons.Rounded.Image,
                    onClick = onPickGallery
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("QRIS", text))
}

private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
}
