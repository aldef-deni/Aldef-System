package com.aldef.system.aldefai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController
import com.aldef.system.aldefai.core.AldefAiPrefs
import com.aldef.system.data.Screen
import com.aldef.system.aldefai.service.AldefAiService
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.GradientText
import com.aldef.system.ui.components.OutlineNeonButton
import com.aldef.system.ui.components.SectionLabel
import com.aldef.system.ui.theme.Hairline
import com.aldef.system.ui.theme.InkDeep
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary

private val AiSweep = listOf(NeonViolet, NeonMagenta, NeonCyan)

@Composable
fun AldefAiSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AldefAiPrefs(context) }

    var enabled by remember { mutableStateOf(prefs.enabled) }
    var edgeSwipe by remember { mutableStateOf(prefs.edgeSwipe) }
    var edgeLeft by remember { mutableStateOf(prefs.edgeLeft) }
    var continuous by remember { mutableStateOf(prefs.continuousListening) }
    var voiceResponse by remember { mutableStateOf(prefs.voiceResponse) }
    var haptic by remember { mutableStateOf(prefs.haptic) }
    var sound by remember { mutableStateOf(prefs.soundFeedback) }
    var transcript by remember { mutableStateOf(prefs.showTranscript) }
    var animation by remember { mutableStateOf(prefs.aiAnimation) }
    var speechRate by remember { mutableFloatStateOf(prefs.speechRate) }

    // Status izin overlay dipantau; disegarkan tiap layar kembali ke depan
    // (mis. setelah pengguna balik dari halaman izin sistem).
    var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canOverlay = Settings.canDrawOverlays(context)
                AldefAiService.sync(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun applyServiceState() = AldefAiService.sync(context)

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = AiSweep) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AldefTopBar(
                title = "ALDEF AI",
                subtitle = "PENGATURAN ASISTEN SUARA",
                onBack = { navController.popBackStack() }
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                HeroCard(
                    enabled = enabled,
                    onToggle = {
                        enabled = it
                        prefs.enabled = it
                        applyServiceState()
                        if (it && !prefs.setupComplete) {
                            navController.navigate(Screen.AldefAiSetup.route)
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))
                OutlineNeonButton(
                    text = "PANDUAN PENYIAPAN",
                    modifier = Modifier.fillMaxWidth(),
                    colors = AiSweep,
                    onClick = { navController.navigate(Screen.AldefAiSetup.route) }
                )

                // Peringatan izin overlay — wajib untuk strip tepi. Muncul hanya
                // saat ALDEF AI aktif tapi izin belum diberikan.
                AnimatedVisibility(visible = enabled && !canOverlay) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        OverlayPermissionCard(
                            onGrant = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + context.packageName)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                SectionLabel("Pemanggilan")
                Spacer(Modifier.height(10.dp))
                SettingGroup {
                    SwitchRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Edge Swipe",
                        subtitle = "Geser dari tepi layar untuk memanggil",
                        checked = edgeSwipe,
                        enabled = enabled,
                        onChange = { edgeSwipe = it; prefs.edgeSwipe = it; applyServiceState() }
                    )
                    RowDivider()
                    SegmentRow(
                        title = "Sisi tepi",
                        subtitle = "Posisi strip pemicu",
                        options = listOf("Kiri", "Kanan"),
                        selectedIndex = if (edgeLeft) 0 else 1,
                        enabled = enabled && edgeSwipe,
                        onSelect = { index ->
                            edgeLeft = index == 0
                            prefs.edgeLeft = edgeLeft
                            applyServiceState()
                        }
                    )
                    RowDivider()
                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Continuous Listening",
                        subtitle = "Dengar terus-menerus (boros baterai)",
                        checked = continuous,
                        enabled = enabled,
                        onChange = { continuous = it; prefs.continuousListening = it }
                    )
                }

                Spacer(Modifier.height(20.dp))

                SectionLabel("Suara")
                Spacer(Modifier.height(10.dp))
                SettingGroup {
                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Balasan Suara",
                        subtitle = "ALDEF AI menjawab dengan TTS",
                        checked = voiceResponse,
                        enabled = enabled,
                        onChange = { voiceResponse = it; prefs.voiceResponse = it }
                    )
                    RowDivider()
                    StaticRow(title = "Bahasa", value = "Indonesia", enabled = enabled)
                    RowDivider()
                    SpeechRateRow(
                        rate = speechRate,
                        enabled = enabled && voiceResponse,
                        onChange = {
                            speechRate = it
                            prefs.speechRate = it
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                SectionLabel("Umpan balik & tampilan")
                Spacer(Modifier.height(10.dp))
                SettingGroup {
                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Haptic",
                        subtitle = "Getaran ringan saat aktif",
                        checked = haptic,
                        enabled = enabled,
                        onChange = { haptic = it; prefs.haptic = it }
                    )
                    RowDivider()
                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Umpan Balik Suara",
                        subtitle = "Nada singkat saat mulai/selesai",
                        checked = sound,
                        enabled = enabled,
                        onChange = { sound = it; prefs.soundFeedback = it }
                    )
                    RowDivider()
                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Tampilkan Transkrip",
                        subtitle = "Perlihatkan teks yang dikenali",
                        checked = transcript,
                        enabled = enabled,
                        onChange = { transcript = it; prefs.showTranscript = it }
                    )
                    RowDivider()
                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Animasi AI",
                        subtitle = "Efek pulse & gelombang (ringan)",
                        checked = animation,
                        enabled = enabled,
                        onChange = { animation = it; prefs.aiAnimation = it }
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "Dioptimalkan untuk Realme 5 Pro · Lokal, tanpa server. " +
                        "Fitur lanjutan (edge panel, suara, aksi) menyusul di pembaruan berikutnya.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun HeroCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        borderTint = if (enabled) {
            listOf(NeonViolet.copy(alpha = 0.55f), NeonCyan.copy(alpha = 0.35f))
        } else {
            listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.03f))
        }
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(AiSweep.map { it.copy(alpha = 0.22f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    GradientText(
                        text = "ALDEF AI",
                        colors = AiSweep,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Full Voice Control · ALDEFTECH",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Aktifkan ALDEF AI",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (enabled) "Aktif" else "Nonaktif",
                        color = if (enabled) NeonCyan else TextMuted,
                        fontSize = 11.sp
                    )
                }
                AiSwitch(checked = enabled, enabled = true, onChange = onToggle)
            }
        }
    }
}

@Composable
private fun OverlayPermissionCard(onGrant: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderTint = listOf(NeonMagenta.copy(alpha = 0.6f), NeonViolet.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Izin \"Tampilkan di atas aplikasi lain\" dibutuhkan",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Strip pemicu tepi ALDEF AI memakai overlay resmi Android. " +
                    "Aktifkan izin ini agar pemicu bisa muncul di Home maupun aplikasi lain.",
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(NeonViolet, NeonCyan)))
                    .clickableNoRipple(onGrant)
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            ) {
                Text(
                    text = "BUKA PENGATURAN IZIN",
                    color = InkDeep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
private fun SettingGroup(content: @Composable () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) NeonCyan else TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
        }
        AiSwitch(checked = checked, enabled = enabled, onChange = onChange)
    }
}

@Composable
private fun StaticRow(title: String, value: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (enabled) TextPrimary else TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, color = NeonCyan, fontSize = 13.sp)
    }
}

@Composable
private fun SpeechRateRow(rate: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Kecepatan Bicara",
                color = if (enabled) TextPrimary else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "%.1fx".format(rate),
                color = if (enabled) NeonCyan else TextMuted,
                fontFamily = NumericFont,
                fontSize = 13.sp
            )
        }
        Slider(
            value = rate,
            onValueChange = onChange,
            valueRange = 0.5f..2.0f,
            steps = 14,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonViolet,
                inactiveTrackColor = Hairline
            )
        )
    }
}

@Composable
private fun SegmentRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Surface1.copy(alpha = 0.7f))
                .padding(3.dp)
        ) {
            options.forEachIndexed { index, label ->
                val active = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (active && enabled) {
                                Brush.linearGradient(listOf(NeonViolet, NeonCyan))
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .then(
                            if (enabled) Modifier.clickableNoRipple { onSelect(index) } else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = when {
                            active && enabled -> InkDeep
                            enabled -> TextSecondary
                            else -> TextMuted
                        },
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSwitch(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = InkDeep,
            checkedTrackColor = NeonCyan,
            checkedBorderColor = NeonCyan,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = Surface1,
            uncheckedBorderColor = Hairline
        )
    )
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 16.dp)
            .height(1.dp)
            .background(Hairline.copy(alpha = 0.5f))
    )
}

/** Klik tanpa efek ripple, agar segmen terasa ringan di Snapdragon 712. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
