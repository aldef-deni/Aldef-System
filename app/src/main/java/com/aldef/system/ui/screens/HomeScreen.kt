package com.aldef.system.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.R
import com.aldef.system.data.AppPrefs
import com.aldef.system.data.Screen
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.CircleIconButton
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.GradientDivider
import com.aldef.system.ui.components.rememberIsOnline
import com.aldef.system.ui.theme.BrandSweep
import com.aldef.system.ui.theme.NeonAmber
import com.aldef.system.ui.theme.NeonBlue
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Feature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: List<Color>,
    val route: String
)

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val isOnline = rememberIsOnline()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val locale = remember { Locale("in", "ID") }
    val timeFormat = remember { SimpleDateFormat("HH:mm", locale) }
    val secondFormat = remember { SimpleDateFormat("ss", locale) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", locale) }
    val date = Date(now)

    val greeting = remember(now / 60_000) {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..18 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }

    val features = remember {
        listOf(
            Feature(
                "QRIS",
                "Scan & baca QR",
                Icons.Rounded.QrCodeScanner,
                listOf(NeonOrange, NeonAmber),
                Screen.Qris.route
            ),
            Feature(
                "Kompas",
                "Arah mata angin",
                Icons.Rounded.Explore,
                listOf(NeonCyan, NeonGreen),
                Screen.Compass.route
            ),
            Feature(
                "Kalkulator",
                "Perhitungan Angka",
                Icons.Rounded.Calculate,
                listOf(NeonViolet, NeonMagenta),
                Screen.Calculator.route
            ),
            Feature(
                "Speedometer",
                "Kecepatan GPS",
                Icons.Rounded.Speed,
                listOf(NeonMagenta, NeonOrange),
                Screen.Speedometer.route
            ),
            Feature(
                "Kalender",
                "Libur nasional",
                Icons.Rounded.CalendarMonth,
                listOf(NeonBlue, NeonCyan),
                Screen.Calendar.route
            ),
            Feature(
                "ALDEF AI",
                "Kontrol suara",
                Icons.Rounded.AutoAwesome,
                listOf(NeonViolet, NeonCyan),
                Screen.AldefAi.route
            )
        )
    }

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.aldef_logo_landscape),
                    contentDescription = stringResource(R.string.logo_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(132.dp)
                )
                Spacer(Modifier.weight(1f))
                CircleIconButton(
                    icon = Icons.Rounded.Logout,
                    contentDescription = "Keluar",
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = greeting + ",",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = prefs.displayName,
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )

            Spacer(Modifier.height(20.dp))

            ClockCard(
                time = timeFormat.format(date),
                seconds = secondFormat.format(date),
                date = dateFormat.format(date).replaceFirstChar { it.uppercase() }
            )

            Spacer(Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                features.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        pair.forEach { feature ->
                            FeatureCard(
                                feature = feature,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.96f),
                                onClick = { navController.navigate(feature.route) }
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            GradientDivider()

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALDEF SYSTEM",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (isOnline) NeonGreen else NeonRed
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (isOnline) "SISTEM ONLINE" else "SISTEM OFFLINE",
                        color = statusColor,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ClockCard(time: String, seconds: String, date: String) {
    val shimmer = rememberInfiniteTransition(label = "clockShimmer")
    val phase by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "clockPhase"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        borderTint = listOf(
            NeonOrange.copy(alpha = 0.30f * (1f - phase) + 0.08f),
            NeonViolet.copy(alpha = 0.22f),
            NeonCyan.copy(alpha = 0.30f * phase + 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = time,
                        fontFamily = NumericFont,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = seconds,
                        fontFamily = NumericFont,
                        fontSize = 15.sp,
                        color = NeonOrange,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(text = date, color = TextSecondary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(BrandSweep.map { it.copy(alpha = 0.22f) })),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        borderTint = feature.accent.map { it.copy(alpha = 0.30f) },
        fill = listOf(
            feature.accent.first().copy(alpha = 0.10f),
            Surface1.copy(alpha = 0.95f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(feature.accent.map { it.copy(alpha = 0.22f) })),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = feature.accent.first(),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .width(34.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Brush.horizontalGradient(feature.accent))
                )
            }
        }
    }
}
