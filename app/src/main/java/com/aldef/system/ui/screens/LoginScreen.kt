package com.aldef.system.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.R
import com.aldef.system.data.AppPrefs
import com.aldef.system.data.Screen
import com.aldef.system.data.UserCredentials
import com.aldef.system.security.BiometricHelper
import com.aldef.system.security.BiometricStatus
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.GradientDivider
import com.aldef.system.ui.components.NeonButton
import com.aldef.system.ui.components.rememberFragmentActivity
import com.aldef.system.ui.theme.BrandSweep
import com.aldef.system.ui.theme.Hairline
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Gerbang aplikasi: nama pengguna + kata sandi, dengan biometrik sebagai
 * jalur cepat. Klik pertama pada tombol biometrik menautkan sidik jari
 * perangkat ke aplikasi; setelah tertaut, tombol yang sama langsung membuka
 * akses tanpa mengetik.
 */
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = rememberFragmentActivity()
    val prefs = remember { AppPrefs(context) }
    val biometric = remember { BiometricHelper(context) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // teks ke apakah-error
    var enrolled by remember { mutableStateOf(prefs.biometricEnrolled) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var biometricStatus by remember { mutableStateOf(biometric.status()) }

    // Getaran halus saat kredensial ditolak.
    val shake = remember { Animatable(0f) }

    val glow = rememberInfiniteTransition(label = "loginGlow")
    val glowPhase by glow.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "glowPhase"
    )

    fun fail(text: String) {
        message = text to true
        scope.launch {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 320
                    0f at 0
                    -14f at 60
                    12f at 120
                    -8f at 180
                    5f at 240
                    0f at 320
                }
            )
        }
    }

    fun enter() {
        keyboard?.hide()
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Login.route) { inclusive = true }
        }
    }

    fun submit() {
        if (UserCredentials.matches(username, password)) {
            message = null
            enter()
        } else {
            fail(
                when {
                    username.isBlank() || password.isBlank() -> "Nama pengguna dan kata sandi wajib diisi"
                    else -> "Nama pengguna atau kata sandi salah"
                }
            )
        }
    }

    fun runBiometric() {
        val host = activity
        if (host == null) {
            fail("Dialog biometrik tidak bisa ditampilkan di layar ini")
            return
        }
        when (biometricStatus) {
            BiometricStatus.NOT_ENROLLED -> {
                message = "Belum ada sidik jari di perangkat. Daftarkan dulu lewat Pengaturan." to true
                // Android 11+ punya layar pendaftaran langsung; di bawah itu
                // hanya tersedia layar pengaturan keamanan umum.
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                } else {
                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                }
                runCatching { context.startActivity(intent) }
                return
            }

            BiometricStatus.NO_HARDWARE -> {
                fail("Perangkat ini tidak punya sensor biometrik")
                return
            }

            BiometricStatus.UNAVAILABLE -> {
                fail("Sensor biometrik sedang tidak tersedia")
                return
            }

            BiometricStatus.READY -> Unit
        }

        val firstTime = !enrolled
        biometric.authenticate(
            activity = host,
            title = if (firstTime) "Daftarkan Biometrik" else "Login Aldef System",
            subtitle = if (firstTime) {
                "Tempelkan sidik jari untuk menautkannya ke aplikasi"
            } else {
                "Tempelkan sidik jari untuk membuka akses"
            },
            onSuccess = {
                if (firstTime) {
                    prefs.biometricEnrolled = true
                    enrolled = true
                }
                message = null
                enter()
            },
            onError = { error -> if (error.isNotEmpty()) fail(error) },
            onFailed = { fail("Sidik jari tidak dikenali") }
        )
    }

    // Status sensor bisa berubah kalau pengguna baru mendaftarkan sidik jari
    // di Pengaturan lalu kembali ke sini.
    LaunchedEffect(Unit) { biometricStatus = biometric.status() }

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = BrandSweep) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            Image(
                painter = painterResource(R.drawable.aldef_logo_landscape),
                contentDescription = stringResource(R.string.logo_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .graphicsLayer { alpha = 0.96f }
            )

            Spacer(Modifier.height(34.dp))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value },
                shape = RoundedCornerShape(28.dp),
                borderTint = listOf(
                    NeonOrange.copy(alpha = 0.35f + 0.25f * glowPhase),
                    NeonViolet.copy(alpha = 0.30f),
                    NeonCyan.copy(alpha = 0.35f + 0.25f * (1f - glowPhase))
                )
            ) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 26.dp)) {
                    PremiumField(
                        value = username,
                        onValueChange = { username = it; message = null },
                        label = "Nama Pengguna",
                        leadingIcon = Icons.Rounded.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    PremiumField(
                        value = password,
                        onValueChange = { password = it; message = null },
                        label = "Kata Sandi",
                        leadingIcon = Icons.Rounded.Lock,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailing = {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = if (passwordVisible) "Sembunyikan" else "Tampilkan",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { passwordVisible = !passwordVisible }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = message != null,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(150))
                    ) {
                        val (text, isError) = message ?: ("" to true)
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (isError) NeonRed else NeonGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = text,
                                color = if (isError) NeonRed else NeonGreen,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    NeonButton(
                        text = "MASUK",
                        modifier = Modifier.fillMaxWidth(),
                        leading = Icons.Rounded.Login,
                        onClick = { submit() }
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientDivider(Modifier.weight(1f))
                        Text(
                            text = "  ATAU  ",
                            color = TextMuted,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                        GradientDivider(Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(20.dp))

                    BiometricButton(
                        enrolled = enrolled,
                        available = biometricStatus.isUsable,
                        pulse = glowPhase,
                        onClick = { runBiometric() }
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (enrolled) {
                            "Sidik jari sudah tertaut ke akun ini"
                        } else {
                            "Ketuk sekali untuk menautkan sidik jari Anda"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = TextMuted,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "ALDEF SYSTEM © $currentYear",
                color = TextMuted.copy(alpha = 0.7f),
                fontSize = 9.sp,
                letterSpacing = 1.8.sp
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Tombol biometrik: cincin berdenyut mengelilingi ikon sidik jari. */
@Composable
private fun BiometricButton(
    enrolled: Boolean,
    available: Boolean,
    pulse: Float,
    onClick: () -> Unit
) {
    val ringAlpha = if (available) 0.35f + 0.35f * kotlin.math.sin(pulse * 2f * Math.PI.toFloat()).let { (it + 1f) / 2f } else 0.15f
    val accent = if (enrolled) NeonCyan else NeonOrange

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.20f), Surface1.copy(alpha = 0.9f))
                    )
                )
                .border(1.5.dp, accent.copy(alpha = ringAlpha), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = if (enrolled) "Masuk dengan biometrik" else "Daftarkan biometrik",
                tint = if (available) accent else TextMuted,
                modifier = Modifier
                    .size(30.dp)
                    .scale(if (available) 1f + 0.05f * pulse else 1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (enrolled) "MASUK DENGAN BIOMETRIK" else "DAFTARKAN BIOMETRIK",
            color = if (available) TextPrimary else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp
        )
    }
}

/** Kolom isian bergaya seragam untuk seluruh aplikasi. */
@Composable
private fun PremiumField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(19.dp))
        },
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface2.copy(alpha = 0.6f),
            unfocusedContainerColor = Surface1.copy(alpha = 0.5f),
            focusedBorderColor = NeonOrange.copy(alpha = 0.75f),
            unfocusedBorderColor = Hairline,
            focusedLabelColor = NeonOrange,
            unfocusedLabelColor = TextMuted,
            focusedLeadingIconColor = NeonOrange,
            unfocusedLeadingIconColor = TextMuted,
            cursorColor = NeonOrange,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                handleColor = NeonOrange,
                backgroundColor = NeonOrange.copy(alpha = 0.3f)
            )
        )
    )
}
