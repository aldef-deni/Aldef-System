package com.aldef.system.ui.screens

import androidx.activity.compose.rememberComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.aldef.system.biometric.BiometricHelper
import com.aldef.system.data.Screen
import com.aldef.system.data.UserCredentials
import com.aldef.system.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = rememberComponentActivity()
    val biometricHelper = remember { BiometricHelper(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var biometricRegistered by remember { mutableStateOf(false) }

    val shimmerAnim = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerAnim.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerOffset"
    )

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )

    fun handleLogin() {
        if (username == UserCredentials.USERNAME && password == UserCredentials.PASSWORD) {
            isLoading = true
            errorMessage = ""
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        } else {
            errorMessage = "Username atau password salah"
        }
    }

    fun handleBiometric() {
        if (!biometricHelper.isBiometricAvailable()) {
            errorMessage = "Biometrik tidak tersedia di perangkat ini"
            return
        }

        if (!biometricRegistered) {
            // First time: register biometric
            biometricHelper.authenticate(
                activity = activity,
                title = "Daftarkan Biometrik",
                subtitle = "Atur sidik jari atau wajah untuk login cepat",
                onSuccess = {
                    biometricRegistered = true
                    isLoading = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onError = { error -> errorMessage = error },
                onFailed = { errorMessage = "Pendaftaran biometrik gagal" }
            )
        } else {
            // Subsequent: use biometric to login
            biometricHelper.authenticate(
                activity = activity,
                title = "Login dengan Biometrik",
                subtitle = "Gunakan sidik jari atau wajah",
                onSuccess = {
                    isLoading = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onError = { error -> errorMessage = error },
                onFailed = { errorMessage = "Autentikasi biometrik gagal" }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkCard,
                        DarkBackground
                    )
                )
            )
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PremiumGold.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width * 0.6f,
                        center = Offset(size.width * 0.8f, size.height * 0.2f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PremiumBlue.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = size.width * 0.4f
                        ),
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 0.2f, size.height * 0.8f)
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // App Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PremiumGold, PremiumGoldDark)
                        )
                    )
                    .scale(pulseScale)
            ) {
                Text(
                    text = "A",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ALDEF SYSTEM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = PremiumGold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Masuk ke sistem Anda",
                fontSize = 14.sp,
                color = TextGray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Username Field
            PremiumTextField(
                value = username,
                onValueChange = { username = it; errorMessage = "" },
                label = "Username",
                icon = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            PremiumTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                label = "Password",
                icon = Icons.Outlined.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Error message
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = PremiumRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Button
            Button(
                onClick = { handleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumGold,
                    contentColor = DarkBackground
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "MASUK",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider with "atau"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = TextGrayDark.copy(alpha = 0.5f)
                )
                Text(
                    "  atau  ",
                    color = TextGray,
                    fontSize = 12.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = TextGrayDark.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Biometric Button
            OutlinedButton(
                onClick = { handleBiometric() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PremiumGold
                )
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (biometricRegistered) "LOGIN DENGAN BIOMETRIK" else "DAFTARKAN BIOMETRIK",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "© 2026 Aldef System",
                fontSize = 12.sp,
                color = TextGrayDark
            )
        }
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = PremiumGold.copy(alpha = 0.7f)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onTogglePassword?.invoke() }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = TextGray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PremiumGold,
            unfocusedBorderColor = TextGrayDark.copy(alpha = 0.3f),
            focusedContainerColor = DarkCard.copy(alpha = 0.5f),
            unfocusedContainerColor = DarkCard.copy(alpha = 0.3f),
            cursorColor = PremiumGold,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        )
    )
}
