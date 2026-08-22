package com.aldef.system.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.R
import com.aldef.system.data.Screen
import com.aldef.system.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    var navigateToLogin by remember { mutableStateOf(false) }

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(800), label = "alpha"
    )

    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        navigateToLogin = true
    }

    LaunchedEffect(navigateToLogin) {
        if (navigateToLogin) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        DarkCard,
                        DarkBackground,
                        Color.Black
                    ),
                    radius = 800f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // Logo placeholder with glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(glowAlpha * 0.3f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PremiumGold.copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                radius = 200f
                            )
                        )
                )

                // App name as logo
                Text(
                    text = "ALDEF",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = PremiumGold,
                    letterSpacing = 8.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "S Y S T E M",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = TextGray,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = PremiumGold,
                strokeWidth = 2.dp
            )
        }

        // Version text
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = TextGrayDark,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
