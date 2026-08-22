package com.aldef.system.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.aldef.system.data.Screen
import com.aldef.system.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PremiumGold, PremiumGoldDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "A",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Aldef System",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumGold
                            )
                            Text(
                                "Premium Tools",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Login.route) }) {
                        Icon(
                            Icons.Outlined.Logout,
                            contentDescription = "Logout",
                            tint = TextGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Welcome Card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    PremiumGold.copy(alpha = 0.15f),
                                    DarkCard
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .border(
                            1.dp,
                            PremiumGold.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Selamat Datang 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih tool yang ingin Anda gunakan",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TOOLS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumGold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feature Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(
                        tween(500, delayMillis = 200),
                        initialOffsetY = { it / 2 }
                    )
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "QRIS Scanner",
                        subtitle = "Scan kode QR",
                        icon = Icons.Filled.QrCodeScanner,
                        gradientColors = listOf(PremiumGreen, Color(0xFF059669)),
                        onClick = { navController.navigate(Screen.QRIS.route) }
                    )
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 350)) + slideInVertically(
                        tween(500, delayMillis = 350),
                        initialOffsetY = { it / 2 }
                    )
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "Kompas",
                        subtitle = "Arah & orientasi",
                        icon = Icons.Filled.Explore,
                        gradientColors = listOf(PremiumBlue, PremiumPurple),
                        onClick = { navController.navigate(Screen.Compass.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 500)) + slideInVertically(
                        tween(500, delayMillis = 500),
                        initialOffsetY = { it / 2 }
                    )
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "Kalkulator",
                        subtitle = "Hitung & simpan",
                        icon = Icons.Filled.Calculate,
                        gradientColors = listOf(PremiumPurple, PremiumPurpleLight),
                        onClick = { navController.navigate(Screen.Calculator.route) }
                    )
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 650)) + slideInVertically(
                        tween(500, delayMillis = 650),
                        initialOffsetY = { it / 2 }
                    )
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "Speedometer",
                        subtitle = "Kecepatan GPS",
                        icon = Icons.Filled.Speed,
                        gradientColors = listOf(PremiumRed, PremiumOrange),
                        onClick = { navController.navigate(Screen.Speedometer.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hidden Files button
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 800)) + slideInVertically(
                    tween(500, delayMillis = 800),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    DarkCardLight,
                                    DarkCard
                                )
                            )
                        )
                        .border(
                            1.dp,
                            PremiumGold.copy(alpha = 0.15f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { navController.navigate(Screen.HiddenFiles.route) }
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            PremiumGold.copy(alpha = 0.2f),
                                            PremiumGold.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "File Tersembunyi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Text(
                                text = "Kunci & sembunyikan file",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version info
            Text(
                text = "Aldef System v1.0.0",
                fontSize = 11.sp,
                color = TextGrayDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = gradientColors.map { it.copy(alpha = 0.15f) } + listOf(DarkCard)
                )
            )
            .border(
                1.dp,
                gradientColors.first().copy(alpha = 0.2f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = gradientColors.map { it.copy(alpha = 0.3f) }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = gradientColors.first(),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        }
    }
}
