package com.aldef.system.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.aldef.system.ui.theme.*
import kotlin.math.*

@Composable
fun CompassScreen(navController: NavController) {
    val context = LocalContext.current
    var heading by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                    }
                }

                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    heading = (azimuth + 360) % 360
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val direction = getDirection(heading)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Kompas", fontWeight = FontWeight.Bold, color = PremiumGold)
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
            // Direction text
            Text(
                text = direction,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = PremiumGold,
                letterSpacing = 2.sp
            )
            Text(
                text = "${String.format("%.1f", heading)}°",
                fontSize = 20.sp,
                color = TextGray,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Compass Canvas
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(DarkCard, DarkBackground)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(280.dp)) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Outer ring
                    drawCircle(
                        color = PremiumGold.copy(alpha = 0.2f),
                        radius = radius - 8.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )

                    // Degree marks
                    for (i in 0..359 step 5) {
                        val isMajor = i % 30 == 0
                        val markLength = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                        val markWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                        val markColor = if (isMajor) PremiumGold else TextGrayDark

                        rotate(i.toFloat(), pivot = center) {
                            drawLine(
                                color = markColor,
                                start = Offset(center.x, 12.dp.toPx()),
                                end = Offset(center.x, 12.dp.toPx() + markLength),
                                strokeWidth = markWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Cardinal directions
                    val cardinals = mapOf(
                        0 to ("N" to PremiumRed),
                        90 to ("E" to PremiumGold),
                        180 to ("S" to TextWhite),
                        270 to ("W" to PremiumGold)
                    )

                    // North triangle (always points up, rotates with compass)
                    rotate(-heading, pivot = center) {
                        // North needle
                        drawLine(
                            color = PremiumRed,
                            start = center,
                            end = Offset(center.x, 30.dp.toPx()),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // South needle
                        drawLine(
                            color = PremiumBlue,
                            start = center,
                            end = Offset(center.x, size.height - 30.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Center dot
                        drawCircle(
                            color = PremiumGold,
                            radius = 6.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = DarkBackground,
                            radius = 3.dp.toPx(),
                            center = center
                        )
                    }
                }

                // Fixed cardinal labels
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "N",
                        color = PremiumRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
                    )
                    Text(
                        "S",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                    )
                    Text(
                        "W",
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp)
                    )
                    Text(
                        "E",
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompassInfoItem("Heading", "${String.format("%.1f", heading)}°")
                    CompassInfoItem("Arah", direction)
                    CompassInfoItem("Azimuth", "${String.format("%.0f", heading)}")
                }
            }
        }
    }
}

@Composable
fun CompassInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PremiumGold
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextGray
        )
    }
}

fun getDirection(heading: Float): String {
    return when {
        heading < 22.5f || heading >= 337.5f -> "Utara"
        heading < 67.5f -> "Timur Laut"
        heading < 112.5f -> "Timur"
        heading < 157.5f -> "Tenggara"
        heading < 202.5f -> "Selatan"
        heading < 247.5f -> "Barat Daya"
        heading < 292.5f -> "Barat"
        else -> "Barat Laut"
    }
}
