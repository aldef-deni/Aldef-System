package com.aldef.system.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.data.AppPrefs
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.CircleIconButton
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.KeepScreenOn
import com.aldef.system.ui.components.NeonButton
import com.aldef.system.ui.components.StatTile
import com.aldef.system.ui.theme.NeonAmber
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import com.aldef.system.ui.theme.WarmSweep
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_KMH = 200f
private const val KMH_TO_MPH = 0.621371f

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeedometerScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    KeepScreenOn()

    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var speedKmh by remember { mutableFloatStateOf(0f) }
    var maxKmh by remember { mutableFloatStateOf(0f) }
    var distanceMeters by remember { mutableFloatStateOf(0f) }
    var altitude by remember { mutableFloatStateOf(0f) }
    var accuracyMeters by remember { mutableFloatStateOf(0f) }
    var bearing by remember { mutableFloatStateOf(0f) }
    var samples by remember { mutableIntStateOf(0) }
    var speedSum by remember { mutableFloatStateOf(0f) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var hasFix by remember { mutableStateOf(false) }
    var useKmh by remember { mutableStateOf(prefs.speedInKmh) }

    DisposableEffect(permission.status.isGranted) {
        if (!permission.status.isGranted) return@DisposableEffect onDispose { }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                hasFix = true

                // Kecepatan dari GPS lebih stabil daripada menurunkannya dari
                // selisih posisi; jarak tetap dijumlahkan dari perpindahan.
                val current = if (location.hasSpeed()) location.speed * 3.6f else 0f
                speedKmh = current
                if (current > maxKmh) maxKmh = current
                if (current > 1f) {
                    speedSum += current
                    samples++
                }

                lastLocation?.let { previous ->
                    val step = previous.distanceTo(location)
                    // Lompatan di bawah akurasi sensor hampir selalu derau.
                    if (step > location.accuracy.coerceAtLeast(3f)) {
                        distanceMeters += step
                    }
                }
                lastLocation = location
                altitude = location.altitude.toFloat()
                accuracyMeters = location.accuracy
                if (location.hasBearing()) bearing = location.bearing
            }
        }

        client.requestLocationUpdates(request, callback, context.mainLooper)
        onDispose {
            client.removeLocationUpdates(callback)
            if (maxKmh > prefs.topSpeedKmh) prefs.topSpeedKmh = maxKmh
        }
    }

    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh.coerceIn(0f, MAX_KMH),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "speed"
    )

    fun convert(kmh: Float) = if (useKmh) kmh else kmh * KMH_TO_MPH
    val unitLabel = if (useKmh) "km/j" else "mph"

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = WarmSweep) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AldefTopBar(
                title = "Speedometer",
                subtitle = if (hasFix) "SINYAL GPS TERKUNCI" else "MENUNGGU SINYAL GPS",
                onBack = { navController.popBackStack() },
                actions = {
                    CircleIconButton(
                        icon = Icons.Rounded.SwapHoriz,
                        contentDescription = "Ganti satuan",
                        onClick = {
                            useKmh = !useKmh
                            prefs.speedInKmh = useKmh
                        }
                    )
                }
            )

            if (!permission.status.isGranted) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(Modifier.padding(28.dp)) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Izin lokasi dibutuhkan",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Kecepatan dibaca langsung dari GPS perangkat. Tidak ada lokasi yang disimpan atau dikirim ke mana pun.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            NeonButton(
                                text = "IZINKAN LOKASI",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { permission.launchPermissionRequest() }
                            )
                        }
                    }
                }
                return@Column
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1.05f),
                contentAlignment = Alignment.Center
            ) {
                SpeedGauge(speedKmh = animatedSpeed, useKmh = useKmh)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = convert(speedKmh).roundToInt().toString(),
                        fontFamily = NumericFont,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = unitLabel.uppercase(),
                        color = NeonOrange,
                        fontSize = 13.sp,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (hasFix) NeonGreen else NeonRed)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (hasFix) "±${accuracyMeters.roundToInt()} m" else "mencari…",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Tertinggi",
                        value = convert(maxKmh).roundToInt().toString(),
                        unit = unitLabel,
                        accent = NeonMagenta,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Rata-rata",
                        value = if (samples > 0) {
                            convert(speedSum / samples).roundToInt().toString()
                        } else {
                            "0"
                        },
                        unit = unitLabel,
                        accent = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Jarak",
                        value = if (distanceMeters >= 1000f) {
                            "%.2f".format(distanceMeters / 1000f)
                        } else {
                            distanceMeters.roundToInt().toString()
                        },
                        unit = if (distanceMeters >= 1000f) "km" else "m",
                        accent = NeonViolet,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Ketinggian",
                        value = altitude.roundToInt().toString(),
                        unit = "mdpl",
                        accent = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Arah",
                        value = bearing.roundToInt().toString(),
                        unit = "°",
                        accent = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = {
                        speedKmh = 0f
                        maxKmh = 0f
                        distanceMeters = 0f
                        speedSum = 0f
                        samples = 0
                        lastLocation = null
                    }
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "ATUR ULANG PERJALANAN",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp
                        )
                    }
                }

                Text(
                    text = "Rekor tertinggi tersimpan: ${convert(prefs.topSpeedKmh).roundToInt()} $unitLabel",
                    modifier = Modifier.fillMaxWidth(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

/** Busur 240° dengan zona warna: hijau, kuning, lalu merah di ujung. */
@Composable
private fun SpeedGauge(speedKmh: Float, useKmh: Boolean) {
    val density = LocalDensity.current
    val labelPx = with(density) { 10.sp.toPx() }

    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val startAngle = 150f
        val sweepAngle = 240f
        val strokeWidth = radius * 0.09f
        val arcSize = Size(radius * 1.7f, radius * 1.7f)
        val topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f)

        // Alur latar.
        drawArc(
            color = Surface2.copy(alpha = 0.85f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Isian sesuai kecepatan.
        val fraction = (speedKmh / MAX_KMH).coerceIn(0f, 1f)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(NeonGreen, NeonCyan, NeonAmber, NeonOrange, NeonRed, NeonGreen),
                center = center
            ),
            startAngle = startAngle,
            sweepAngle = sweepAngle * fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Skala angka tiap 20 km/j.
        val step = 20
        val maxTick = MAX_KMH.toInt()
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = labelPx
                color = TextMuted.toArgb()
            }
            var tick = 0
            while (tick <= maxTick) {
                val angle = Math.toRadians((startAngle + sweepAngle * tick / maxTick).toDouble())
                val tickRadius = radius * 0.66f
                val x = center.x + (cos(angle) * tickRadius).toFloat()
                val y = center.y + (sin(angle) * tickRadius).toFloat() + labelPx / 3f
                val display = if (useKmh) tick else (tick * KMH_TO_MPH).roundToInt()
                drawText(display.toString(), x, y, paint)
                tick += step
            }
        }

        // Garis skala kecil.
        var minorTick = 0
        while (minorTick <= maxTick) {
            val angle = Math.toRadians((startAngle + sweepAngle * minorTick / maxTick).toDouble())
            val isMajor = minorTick % step == 0
            val outer = radius * 0.79f
            val length = if (isMajor) radius * 0.06f else radius * 0.03f
            drawLine(
                color = if (isMajor) TextSecondary.copy(alpha = 0.8f) else TextMuted.copy(alpha = 0.4f),
                start = center + Offset((cos(angle) * outer).toFloat(), (sin(angle) * outer).toFloat()),
                end = center + Offset(
                    (cos(angle) * (outer - length)).toFloat(),
                    (sin(angle) * (outer - length)).toFloat()
                ),
                strokeWidth = if (isMajor) 2.5f else 1.2f
            )
            minorTick += 5
        }

        // Jarum.
        val needleAngle = Math.toRadians((startAngle + sweepAngle * fraction).toDouble())
        val needleLength = radius * 0.56f
        val tip = center + Offset(
            (cos(needleAngle) * needleLength).toFloat(),
            (sin(needleAngle) * needleLength).toFloat()
        )
        val perpendicular = needleAngle + Math.PI / 2
        val halfWidth = radius * 0.028f
        val needle = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(
                center.x + (cos(perpendicular) * halfWidth).toFloat(),
                center.y + (sin(perpendicular) * halfWidth).toFloat()
            )
            lineTo(
                center.x - (cos(perpendicular) * halfWidth).toFloat(),
                center.y - (sin(perpendicular) * halfWidth).toFloat()
            )
            close()
        }
        drawPath(needle, brush = Brush.linearGradient(listOf(NeonOrange, NeonRed)))
        drawCircle(color = NeonOrange, radius = radius * 0.045f, center = center)
        drawCircle(color = Color.Black, radius = radius * 0.022f, center = center)
    }
}
