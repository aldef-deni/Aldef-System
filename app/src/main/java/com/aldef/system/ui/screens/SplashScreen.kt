package com.aldef.system.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.R
import com.aldef.system.data.Screen
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.theme.BrandSweep
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private const val SPLASH_TOTAL_MS = 2600L

/**
 * Pembuka aplikasi: logo Aldef Tech masuk dengan pegas, disapu kilau cahaya,
 * dikelilingi cincin energi yang berdenyut, lalu garis progres menutup adegan.
 */
@Composable
fun SplashScreen(navController: NavController) {
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }
    val ringSpread = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    var showTagline by remember { mutableStateOf(false) }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // Kilau yang menyapu logo dari kiri ke kanan, berulang pelan.
    val sweepTransition = rememberInfiniteTransition(label = "sweep")
    val sweep by sweepTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 300, easing = LinearEasing)),
        label = "sweepPos"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "pulseValue"
    )

    LaunchedEffect(Unit) {
        ringSpread.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(700))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
    }
    LaunchedEffect(Unit) {
        delay(650)
        showTagline = true
        delay(250)
        progress.animateTo(1f, tween(1500, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        delay(SPLASH_TOTAL_MS)
        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    AuroraBackground(
        modifier = Modifier.fillMaxSize(),
        tint = BrandSweep,
        intensity = 1.4f
    ) {
        // Cincin energi di belakang logo.
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.44f)
            val base = size.minDimension * 0.30f
            listOf(NeonOrange, NeonMagenta, NeonViolet, NeonCyan).forEachIndexed { i, color ->
                val phase = (pulse + i * 0.25f) % 1f
                val radius = base * (0.75f + phase * 1.35f) * ringSpread.value
                val alpha = (1f - phase) * 0.30f * ringSpread.value
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }
            // Sorotan halus tepat di belakang logo.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonViolet.copy(alpha = 0.22f * ringSpread.value), Color.Transparent),
                    center = center,
                    radius = base * 2.2f
                ),
                radius = base * 2.2f,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.aldef_logo_landscape),
                contentDescription = stringResource(R.string.logo_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(screenWidth * 0.78f)
                    .scale(logoScale.value)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        // Offscreen supaya kilau bisa dikunci ke bentuk logo.
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val bandWidth = size.width * 0.35f
                        val start = size.width * sweep
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                start = Offset(start, 0f),
                                end = Offset(start + bandWidth, size.height)
                            ),
                            blendMode = BlendMode.SrcAtop
                        )
                    }
            )

            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "S Y S T E M",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 7.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "QRIS  ·  KOMPAS  ·  KALKULATOR  ·  SPEEDOMETER",
                        color = TextMuted,
                        fontSize = 9.sp,
                        letterSpacing = 1.6.sp
                    )
                }
            }
        }

        // Garis progres tipis di bawah, bergradien penuh warna merek.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(160.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Surface2)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .height(2.dp)
                        .background(Brush.horizontalGradient(BrandSweep))
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "v1.0.0  ·  PERSONAL EDITION",
                color = TextMuted,
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )
        }
    }
}
