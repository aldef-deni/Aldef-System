package com.aldef.system.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.aldef.system.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import kotlin.math.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeedometerScreen(navController: NavController) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var speed by remember { mutableStateOf(0f) }
    var maxSpeed by remember { mutableStateOf(0f) }
    var totalDistance by remember { mutableStateOf(0f) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var isTracking by remember { mutableStateOf(false) }

    val animatedSpeed by animateFloatAsState(
        targetValue = speed,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "speed"
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(isTracking) {
        if (!isTracking || !locationPermissionState.status.isGranted) {
            onDispose {}
        } else {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
                .setMinUpdateDistanceMeters(1f)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    val currentSpeed = location.speed * 3.6f // m/s to km/h

                    lastLocation?.let { prev ->
                        val distance = prev.distanceTo(location)
                        totalDistance += distance
                    }

                    speed = currentSpeed
                    if (currentSpeed > maxSpeed) {
                        maxSpeed = currentSpeed
                    }
                    lastLocation = location
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Speedometer", fontWeight = FontWeight.Bold, color = PremiumGold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = PremiumGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Speed Gauge
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(DarkCard, DarkBackground)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(260.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Background arc
                    drawArc(
                        color = DarkCardLight,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Speed arc
                    val speedSweep = (animatedSpeed / 200f).coerceIn(0f, 1f) * 270f
                    val arcColor = when {
                        animatedSpeed < 60 -> PremiumGreen
                        animatedSpeed < 120 -> PremiumGold
                        else -> PremiumRed
                    }

                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(PremiumGreen, PremiumGold, PremiumRed)
                        ),
                        startAngle = 135f,
                        sweepAngle = speedSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Tick marks
                    for (i in 0..20) {
                        val angle = 135f + (i * 13.5f)
                        val rad = Math.toRadians(angle.toDouble())
                        val isMajor = i % 5 == 0
                        val innerR = radius - (if (isMajor) 30.dp else 22.dp).toPx()

                        drawLine(
                            color = if (isMajor) PremiumGold else TextGrayDark,
                            start = Offset(
                                center.x + (innerR * cos(rad)).toFloat(),
                                center.y + (innerR * sin(rad)).toFloat()
                            ),
                            end = Offset(
                                center.x + ((radius - 4.dp.toPx()) * cos(rad)).toFloat(),
                                center.y + ((radius - 4.dp.toPx()) * sin(rad)).toFloat()
                            ),
                            strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Speed labels
                        if (isMajor) {
                            val labelR = radius - 40.dp.toPx()
                            val text = (i * 10).toString()
                        }
                    }

                    // Needle
                    val needleAngle = 135f + (animatedSpeed / 200f).coerceIn(0f, 1f) * 270f
                    val needleRad = Math.toRadians(needleAngle.toDouble())
                    val needleLength = radius - 50.dp.toPx()

                    drawLine(
                        color = PremiumRed,
                        start = center,
                        end = Offset(
                            center.x + (needleLength * cos(needleRad)).toFloat(),
                            center.y + (needleLength * sin(needleRad)).toFloat()
                        ),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Center hub
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PremiumGold, PremiumGoldDark),
                            center = center,
                            radius = 12.dp.toPx()
                        ),
                        radius = 12.dp.toPx(),
                        center = center
                    )
                }

                // Speed text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 30.dp)
                ) {
                    Text(
                        text = String.format("%.0f", animatedSpeed),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            animatedSpeed < 60 -> PremiumGreen
                            animatedSpeed < 120 -> PremiumGold
                            else -> PremiumRed
                        }
                    )
                    Text(
                        text = "KM/H",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray,
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard("Max Speed", "${String.format("%.1f", maxSpeed)} km/h", PremiumRed)
                StatCard("Jarak", "${String.format("%.2f", totalDistance / 1000)} km", PremiumBlue)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start/Stop button
            Button(
                onClick = {
                    isTracking = !isTracking
                    if (!isTracking) {
                        speed = 0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) PremiumRed else PremiumGold,
                    contentColor = DarkBackground
                )
            ) {
                Icon(
                    if (isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isTracking) "HENTIKAN" else "MULAI",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextGray
        )
    }
}
