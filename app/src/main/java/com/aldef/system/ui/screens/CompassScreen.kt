package com.aldef.system.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.KeepScreenOn
import com.aldef.system.ui.components.StatTile
import com.aldef.system.ui.theme.CoolSweep
import com.aldef.system.ui.theme.NeonAmber
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonGreen
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Koordinat Ka'bah, dipakai untuk menghitung arah kiblat. */
private const val KAABA_LAT = 21.4224779
private const val KAABA_LON = 39.6234497

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CompassScreen(navController: NavController) {
    val context = LocalContext.current
    KeepScreenOn()

    var rawHeading by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var fieldStrength by remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    var hasSensor by remember { mutableStateOf(true) }
    var declination by remember { mutableFloatStateOf(0f) }
    var qiblaBearing by remember { mutableStateOf<Float?>(null) }
    var locationLabel by remember { mutableStateOf<String?>(null) }

    // Sudut yang ditampilkan diakumulasikan supaya jarum memilih jalur terpendek
    // dan tidak berputar penuh saat melewati 360/0 derajat.
    var continuousHeading by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelerometer == null || magnetometer == null)) {
            hasSensor = false
            return@DisposableEffect onDispose { }
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false

        // Penapis lolos-rendah: pembacaan magnetometer sangat berisik, tanpa
        // pelicinan jarum akan bergetar terus.
        fun smooth(previous: Float, target: Float, factor: Float): Float {
            var delta = target - previous
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            return (previous + delta * factor + 360f) % 360f
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        applyOrientation(orientation) { az, p, r ->
                            rawHeading = smooth(rawHeading, az, 0.18f)
                            pitch = p
                            roll = r
                        }
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeomagnetic = true
                        fieldStrength = kotlin.math.sqrt(
                            event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]
                        )
                        accuracy = event.accuracy
                    }
                }

                // Jalur cadangan hanya dipakai bila sensor rotasi tidak ada.
                if (rotationSensor == null && hasGravity && hasGeomagnetic &&
                    SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                ) {
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    applyOrientation(orientation) { az, p, r ->
                        rawHeading = smooth(rawHeading, az, 0.12f)
                        pitch = p
                        roll = r
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) accuracy = value
            }
        }

        val delay = SensorManager.SENSOR_DELAY_GAME
        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, delay)
            // Magnetometer tetap didengarkan demi indikator kekuatan medan.
            magnetometer?.let { sensorManager.registerListener(listener, it, delay) }
        } else {
            sensorManager.registerListener(listener, accelerometer, delay)
            sensorManager.registerListener(listener, magnetometer, delay)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Akumulasi sudut kontinu untuk animasi jarum.
    LaunchedEffect(rawHeading) {
        var delta = rawHeading - (continuousHeading % 360f + 360f) % 360f
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f
        continuousHeading += delta
    }

    val animatedHeading by animateFloatAsState(
        targetValue = continuousHeading,
        animationSpec = tween(140),
        label = "heading"
    )

    // Lokasi dipakai untuk dua hal: koreksi deklinasi magnetik (utara sejati)
    // dan arah kiblat. Keduanya opsional — kompas tetap jalan tanpanya.
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    LaunchedEffect(locationPermission.status.isGranted) {
        if (!locationPermission.status.isGranted) return@LaunchedEffect
        runCatching {
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) return@addOnSuccessListener
                    declination = GeomagneticField(
                        location.latitude.toFloat(),
                        location.longitude.toFloat(),
                        location.altitude.toFloat(),
                        System.currentTimeMillis()
                    ).declination
                    qiblaBearing = qiblaBearing(location.latitude, location.longitude)
                    locationLabel = "%.4f, %.4f".format(location.latitude, location.longitude)
                }
        }
    }

    val trueHeading = ((rawHeading + declination) % 360f + 360f) % 360f

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = CoolSweep) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AldefTopBar(
                title = "Kompas",
                subtitle = if (declination != 0f) "UTARA SEJATI" else "UTARA MAGNETIS",
                onBack = { navController.popBackStack() }
            )

            if (!hasSensor) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(Modifier.padding(28.dp)) {
                        Text(
                            text = "Perangkat ini tidak punya sensor magnetometer, jadi kompas tidak bisa bekerja.",
                            modifier = Modifier.padding(22.dp),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Column
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(
                    heading = animatedHeading,
                    declination = declination,
                    qiblaBearing = qiblaBearing
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${trueHeading.roundToInt() % 360}°",
                        fontFamily = NumericFont,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = cardinalName(trueHeading),
                        color = NeonCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Kemiringan",
                        value = "${pitch.roundToInt()}",
                        unit = "°",
                        accent = NeonViolet,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Guling",
                        value = "${roll.roundToInt()}",
                        unit = "°",
                        accent = NeonViolet,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Medan",
                        value = fieldStrength.roundToInt().toString(),
                        unit = "µT",
                        accent = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                AccuracyBar(accuracy)

                if (qiblaBearing != null) {
                    QiblaRow(qiblaBearing!!, trueHeading, locationLabel)
                } else if (!locationPermission.status.isGranted) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { locationPermission.launchPermissionRequest() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NeonAmber)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Izinkan lokasi untuk utara sejati & arah kiblat",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Mengubah keluaran getOrientation menjadi derajat yang enak dibaca. */
private inline fun applyOrientation(
    orientation: FloatArray,
    apply: (azimuth: Float, pitch: Float, roll: Float) -> Unit
) {
    val azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
    apply(azimuth, pitch, roll)
}

/** Bearing lingkaran besar dari posisi pengamat ke Ka'bah. */
private fun qiblaBearing(lat: Double, lon: Double): Float {
    val phi = Math.toRadians(lat)
    val kaabaPhi = Math.toRadians(KAABA_LAT)
    val deltaLambda = Math.toRadians(KAABA_LON - lon)
    val y = sin(deltaLambda) * cos(kaabaPhi)
    val x = cos(phi) * sin(kaabaPhi) - sin(phi) * cos(kaabaPhi) * cos(deltaLambda)
    return ((Math.toDegrees(atan2(y, x)).toFloat()) + 360f) % 360f
}

private fun cardinalName(heading: Float): String {
    val names = listOf(
        "UTARA", "TIMUR LAUT", "TIMUR", "TENGGARA",
        "SELATAN", "BARAT DAYA", "BARAT", "BARAT LAUT"
    )
    val index = ((heading + 22.5f) / 45f).toInt() % 8
    return names[index]
}

@Composable
private fun CompassDial(heading: Float, declination: Float, qiblaBearing: Float?) {
    val density = LocalDensity.current
    val labelSizePx = with(density) { 15.sp.toPx() }
    val minorLabelPx = with(density) { 10.sp.toPx() }

    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Cincin luar statis.
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(NeonCyan, NeonViolet, NeonOrange, NeonCyan),
                center
            ),
            radius = radius * 0.97f,
            center = center,
            style = Stroke(width = 2f),
            alpha = 0.55f
        )
        drawCircle(
            color = Surface2.copy(alpha = 0.55f),
            radius = radius * 0.90f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = radius * 0.66f,
            center = center,
            style = Stroke(width = 1f)
        )

        // Piringan berputar melawan arah heading.
        rotate(degrees = -heading, pivot = center) {
            for (deg in 0 until 360 step 5) {
                val isMajor = deg % 45 == 0
                val isMid = deg % 15 == 0
                val tickLength = when {
                    isMajor -> radius * 0.10f
                    isMid -> radius * 0.06f
                    else -> radius * 0.035f
                }
                val angle = Math.toRadians((deg - 90).toDouble())
                val outer = radius * 0.88f
                val start = center + Offset(
                    (cos(angle) * outer).toFloat(),
                    (sin(angle) * outer).toFloat()
                )
                val end = center + Offset(
                    (cos(angle) * (outer - tickLength)).toFloat(),
                    (sin(angle) * (outer - tickLength)).toFloat()
                )
                drawLine(
                    color = when {
                        deg == 0 -> NeonRed
                        isMajor -> NeonCyan.copy(alpha = 0.9f)
                        isMid -> TextSecondary.copy(alpha = 0.6f)
                        else -> TextMuted.copy(alpha = 0.35f)
                    },
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 3f else 1.5f
                )
            }

            // Huruf mata angin.
            val cardinals = listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")
            val diagonals = listOf(45 to "NE", 135 to "SE", 225 to "SW", 315 to "NW")
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
                cardinals.forEach { (deg, label) ->
                    val angle = Math.toRadians((deg - 90).toDouble())
                    val r = radius * 0.70f
                    paint.textSize = labelSizePx
                    paint.color = if (label == "N") NeonRed.toArgb() else TextPrimary.toArgb()
                    val x = center.x + (cos(angle) * r).toFloat()
                    val y = center.y + (sin(angle) * r).toFloat() + labelSizePx / 3f
                    save()
                    rotate(heading, x, y)
                    drawText(label, x, y, paint)
                    restore()
                }
                diagonals.forEach { (deg, label) ->
                    val angle = Math.toRadians((deg - 90).toDouble())
                    val r = radius * 0.70f
                    paint.textSize = minorLabelPx
                    paint.color = TextMuted.toArgb()
                    val x = center.x + (cos(angle) * r).toFloat()
                    val y = center.y + (sin(angle) * r).toFloat() + minorLabelPx / 3f
                    save()
                    rotate(heading, x, y)
                    drawText(label, x, y, paint)
                    restore()
                }
            }

            // Penanda kiblat ikut berputar bersama piringan.
            if (qiblaBearing != null) {
                val angle = Math.toRadians((qiblaBearing - declination - 90).toDouble())
                val r = radius * 0.80f
                val markerCenter = center + Offset(
                    (cos(angle) * r).toFloat(),
                    (sin(angle) * r).toFloat()
                )
                drawCircle(color = NeonGreen, radius = radius * 0.035f, center = markerCenter)
                drawCircle(
                    color = NeonGreen.copy(alpha = 0.3f),
                    radius = radius * 0.07f,
                    center = markerCenter
                )
            }
        }

        // Jarum tetap di atas menunjuk arah hadap perangkat.
        val needle = Path().apply {
            moveTo(center.x, center.y - radius * 0.58f)
            lineTo(center.x - radius * 0.055f, center.y)
            lineTo(center.x + radius * 0.055f, center.y)
            close()
        }
        drawPath(needle, brush = Brush.verticalGradient(listOf(NeonOrange, NeonRed)))
        drawCircle(color = NeonOrange, radius = radius * 0.035f, center = center)
        drawCircle(
            color = Color.Black,
            radius = radius * 0.016f,
            center = center
        )

        // Segitiga penunjuk arah hadap di puncak dial.
        val pointer = Path().apply {
            moveTo(center.x, center.y - radius * 0.97f)
            lineTo(center.x - radius * 0.045f, center.y - radius * 0.88f)
            lineTo(center.x + radius * 0.045f, center.y - radius * 0.88f)
            close()
        }
        drawPath(pointer, color = NeonAmber)
    }
}

@Composable
private fun AccuracyBar(accuracy: Int) {
    val (label, color) = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "AKURASI TINGGI" to NeonGreen
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "AKURASI SEDANG" to NeonCyan
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "AKURASI RENDAH — GOYANG PONSEL MEMBENTUK ANGKA 8" to NeonAmber
        else -> "BELUM TERKALIBRASI — GOYANG PONSEL MEMBENTUK ANGKA 8" to NeonRed
    }
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(text = label, color = color, fontSize = 10.sp, letterSpacing = 1.2.sp)
        }
    }
}

@Composable
private fun QiblaRow(qibla: Float, heading: Float, locationLabel: String?) {
    val diff = ((qibla - heading + 540f) % 360f) - 180f
    val aligned = abs(diff) < 5f
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        borderTint = if (aligned) {
            listOf(NeonGreen.copy(alpha = 0.6f), NeonGreen.copy(alpha = 0.2f))
        } else {
            listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.03f))
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ARAH KIBLAT", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${qibla.roundToInt()}°  " + when {
                        aligned -> "· tepat"
                        diff > 0 -> "· putar kanan ${abs(diff).roundToInt()}°"
                        else -> "· putar kiri ${abs(diff).roundToInt()}°"
                    },
                    color = if (aligned) NeonGreen else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (locationLabel != null) {
                Text(
                    text = locationLabel,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = NumericFont
                )
            }
        }
    }
}
