package com.aldef.system.aldefai.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.aldef.system.aldefai.core.AldefAiPrefs
import com.aldef.system.aldefai.core.DeviceOptimization
import com.aldef.system.aldefai.service.AldefAiService
import com.aldef.system.aldefai.tts.AldefTtsHolder
import com.aldef.system.aldefai.voice.ALDEFAIVoiceState
import com.aldef.system.aldefai.voice.ALDEFRecognitionListener
import com.aldef.system.aldefai.voice.AndroidSpeechRecognizer
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.GradientText
import com.aldef.system.ui.components.NeonButton
import com.aldef.system.ui.components.OutlineNeonButton
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

private val AiSweep = listOf(NeonViolet, NeonMagenta, NeonCyan)
private const val STEP_COUNT = 5

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AldefAiSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AldefAiPrefs(context) }

    var step by remember { mutableIntStateOf(0) }

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var batteryOk by remember { mutableStateOf(DeviceOptimization.isBatteryUnrestricted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canOverlay = Settings.canDrawOverlays(context)
                batteryOk = DeviceOptimization.isBatteryUnrestricted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = AiSweep) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AldefTopBar(
                title = "Penyiapan ALDEF AI",
                subtitle = "LANGKAH ${step + 1} DARI $STEP_COUNT",
                onBack = { navController.popBackStack() }
            )

            // Indikator langkah.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(STEP_COUNT) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= step) Brush.linearGradient(AiSweep)
                                else Brush.linearGradient(listOf(TextMuted.copy(alpha = 0.3f), TextMuted.copy(alpha = 0.3f)))
                            )
                    )
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = step, animationSpec = tween(300), label = "step") { s ->
                    when (s) {
                        0 -> StepWelcome()
                        1 -> StepPermission(
                            icon = Icons.Rounded.Mic,
                            title = "Izin Mikrofon",
                            body = "ALDEF AI mendengarkan perintah Anda langsung di perangkat. Suara diproses lewat pengenalan Android; tidak ada rekaman yang disimpan.",
                            done = micPermission.status.isGranted,
                            actionText = "Izinkan Mikrofon",
                            onAction = { micPermission.launchPermissionRequest() }
                        )
                        2 -> StepPermission(
                            icon = Icons.Rounded.RadioButtonUnchecked,
                            title = "Izin Tampil di Atas Aplikasi",
                            body = "Strip pemicu dan panel ALDEF AI muncul di atas aplikasi lain. Ini memakai overlay resmi Android.",
                            done = canOverlay,
                            actionText = "Aktifkan Overlay",
                            onAction = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + context.packageName)
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        )
                        3 -> StepBattery(
                            batteryOk = batteryOk,
                            onBattery = { DeviceOptimization.requestBatteryUnrestricted(context) },
                            onAutostart = { DeviceOptimization.openAutostart(context) }
                        )
                        else -> StepTest()
                    }
                }
            }

            // Navigasi bawah.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    OutlineNeonButton(
                        text = "KEMBALI",
                        modifier = Modifier.weight(1f),
                        onClick = { step-- }
                    )
                    Spacer(Modifier.width(12.dp))
                }
                NeonButton(
                    text = if (step < STEP_COUNT - 1) "LANJUT" else "SELESAI",
                    modifier = Modifier.weight(1f),
                    colors = AiSweep,
                    onClick = {
                        if (step < STEP_COUNT - 1) {
                            step++
                        } else {
                            prefs.setupComplete = true
                            runCatching { AldefAiService.sync(context) }
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(AiSweep.map { it.copy(alpha = 0.22f) })),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Mic, null, tint = NeonCyan, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("SELAMAT DATANG DI", color = TextMuted, fontSize = 11.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        GradientText("ALDEF AI", colors = AiSweep, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Asisten suara pribadi Anda.\nLokal, tanpa server, dioptimalkan untuk perangkat Anda.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Panduan singkat ini menyiapkan izin dan pengaturan agar ALDEF AI berjalan andal.",
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepPermission(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    done: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StatusBadge(done)
            Spacer(Modifier.height(18.dp))
            Icon(icon, null, tint = if (done) NeonGreen else NeonCyan, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            if (done) {
                Text("Sudah diizinkan", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                NeonButton(text = actionText.uppercase(), modifier = Modifier.fillMaxWidth(), colors = AiSweep, onClick = onAction)
            }
        }
    }
}

@Composable
private fun StepBattery(batteryOk: Boolean, onBattery: () -> Unit, onAutostart: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.BatteryChargingFull, null, tint = if (batteryOk) NeonGreen else NeonCyan, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Baterai & Autostart", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "ColorOS di Realme cenderung menutup aplikasi latar. Agar strip pemicu ALDEF AI tetap hidup, kecualikan dari optimasi baterai dan izinkan Autostart.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(batteryOk)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (batteryOk) "Optimasi baterai sudah dikecualikan" else "Masih dioptimalkan baterai",
                    color = if (batteryOk) NeonGreen else TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            if (!batteryOk) {
                NeonButton(text = "KECUALIKAN DARI OPTIMASI BATERAI", modifier = Modifier.fillMaxWidth(), colors = AiSweep, onClick = onBattery)
                Spacer(Modifier.height(10.dp))
            }
            OutlineNeonButton(text = "BUKA PENGATURAN AUTOSTART", modifier = Modifier.fillMaxWidth(), onClick = onAutostart)
            Spacer(Modifier.height(8.dp))
            Text(
                "Autostart tidak punya izin resmi Android, jadi aktifkan manual di halaman yang terbuka. Kunci juga ALDEF System di layar Recents.",
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun StepTest() {
    val context = LocalContext.current
    val prefs = remember { AldefAiPrefs(context) }
    var voiceState by remember { mutableStateOf<ALDEFAIVoiceState>(ALDEFAIVoiceState.Idle) }
    var heard by remember { mutableStateOf("") }

    val recognizer = remember {
        AndroidSpeechRecognizer(
            context.applicationContext,
            prefs.language,
            object : ALDEFRecognitionListener {
                override fun onState(state: ALDEFAIVoiceState) { voiceState = state }
                override fun onPartial(text: String) { heard = text }
                override fun onFinal(text: String) { heard = text }
                override fun onRms(level: Float) {}
            }
        )
    }
    DisposableEffect(Unit) {
        AldefTtsHolder.ensure(context.applicationContext)
        onDispose { recognizer.destroy() }
    }

    val listening = voiceState is ALDEFAIVoiceState.Listening
    val error = voiceState as? ALDEFAIVoiceState.Error

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Uji Coba", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Coba ucapkan “Halo ALDEF AI” lalu dengarkan balasannya.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonViolet.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error?.message ?: heard.ifBlank {
                        if (listening) "Mendengarkan…" else "Hasil akan tampil di sini"
                    },
                    color = if (error != null) NeonMagenta else TextPrimary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))
            NeonButton(
                text = if (listening) "MENDENGARKAN…" else "TES MIKROFON",
                modifier = Modifier.fillMaxWidth(),
                colors = AiSweep,
                onClick = {
                    heard = ""
                    recognizer.startListening()
                }
            )
            Spacer(Modifier.height(10.dp))
            OutlineNeonButton(
                text = "TES SUARA",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    AldefTtsHolder.speak(
                        context.applicationContext,
                        "Halo. Saya siap membantu.",
                        prefs.speechRate
                    ) {}
                }
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeUp, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tekan SELESAI untuk mulai memakai ALDEF AI", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatusBadge(done: Boolean) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background((if (done) NeonGreen else TextMuted).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RestartAlt,
            contentDescription = null,
            tint = if (done) NeonGreen else TextMuted,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun StatusDot(ok: Boolean) {
    Box(
        Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (ok) NeonGreen else TextMuted)
    )
}
