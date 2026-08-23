package com.aldef.system.aldefai.overlay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aldef.system.aldefai.core.AldefAiPrefs
import com.aldef.system.aldefai.intent.ALDEFAIIntent
import com.aldef.system.aldefai.intent.IntentClassifiers
import com.aldef.system.aldefai.action.AndroidActionExecutor
import com.aldef.system.aldefai.tts.AldefTtsHolder
import com.aldef.system.aldefai.ui.VoicePermissionActivity
import com.aldef.system.aldefai.voice.ALDEFAIVoiceState
import com.aldef.system.aldefai.voice.ALDEFRecognitionListener
import com.aldef.system.aldefai.voice.AndroidSpeechRecognizer
import com.aldef.system.ui.components.GradientText
import com.aldef.system.ui.theme.InkDeep
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AiSweep = listOf(NeonViolet, NeonMagenta, NeonCyan)

// Jeda otomatis setelah sekian siklus tanpa suara, agar mic tak nyala selamanya.
private const val MAX_EMPTY_CYCLES = 3

/**
 * Isi panel ALDEF AI: kaca gelap yang meluncur dari tepi, mikrofon ALDEFTECH
 * custom, dan pengenalan suara nyata (Phase 5).
 *
 * Yang belum: pemetaan ke aksi & balasan suara (Phase 6–8). Teks yang dikenali
 * ditampilkan, belum dijalankan sebagai perintah.
 */
@Composable
fun AldefAiPanelContent(
    onLeft: Boolean,
    dismissSignal: Boolean,
    onFullyDismissed: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AldefAiPrefs(context) }
    val animationsOn = remember { prefs.aiAnimation }
    val showTranscript = remember { prefs.showTranscript }
    val continuous = remember { prefs.continuousListening }

    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    // ---- Status suara ----
    var voiceState by remember { mutableStateOf<ALDEFAIVoiceState>(ALDEFAIVoiceState.Idle) }
    var partial by remember { mutableStateOf("") }
    var finalText by remember { mutableStateOf("") }
    var rms by remember { mutableFloatStateOf(0f) }
    var needPermission by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf<ALDEFAIIntent?>(null) }
    var autoPaused by remember { mutableStateOf(false) }
    var emptyStreak by remember { mutableIntStateOf(0) }
    // Local-first: AI on-device (bila kelak ada) + fallback mesin aturan.
    val engine = remember { IntentClassifiers.default() }

    val recognizer = remember {
        AndroidSpeechRecognizer(
            context = context.applicationContext,
            languageTag = prefs.language,
            listener = object : ALDEFRecognitionListener {
                override fun onState(state: ALDEFAIVoiceState) { voiceState = state }
                override fun onPartial(text: String) { partial = text }
                override fun onFinal(text: String) { finalText = text; partial = ""; emptyStreak = 0 }
                override fun onRms(level: Float) { rms = level }
            }
        )
    }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    val executor = remember { AndroidActionExecutor(context.applicationContext) }
    var confirmIntent by remember { mutableStateOf<ALDEFAIIntent?>(null) }
    // Hangatkan TTS lebih awal supaya balasan pertama tidak terlewat.
    LaunchedEffect(Unit) { AldefTtsHolder.ensure(context.applicationContext) }

    fun micGranted(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun requestMic() {
        needPermission = true
        runCatching {
            context.startActivity(
                Intent(context, VoicePermissionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun startCycle() {
        if (!micGranted()) { requestMic(); return }
        needPermission = false
        AldefTtsHolder.stop()
        finalText = ""
        partial = ""
        detected = null
        confirmIntent = null
        rms = 0f
        recognizer.startListening()
    }

    fun onMic() {
        when (voiceState) {
            is ALDEFAIVoiceState.Listening -> {
                recognizer.stopListening()
                autoPaused = true
            }
            is ALDEFAIVoiceState.Processing -> Unit
            else -> {
                autoPaused = false
                emptyStreak = 0
                startCycle()
            }
        }
    }

    LaunchedEffect(Unit) {
        shown = true
        if (continuous && micGranted()) {
            delay(350)
            if (!dismissed) startCycle()
        }
    }

    fun close() {
        if (dismissed) return
        dismissed = true
        recognizer.cancel()
        AldefTtsHolder.clearListener()
        scope.launch {
            shown = false
            delay(240)
            onFullyDismissed()
        }
    }

    LaunchedEffect(dismissSignal) { if (dismissSignal) close() }

    fun say(text: String) {
        if (!prefs.voiceResponse) return
        AldefTtsHolder.speak(context.applicationContext, text, prefs.speechRate) { speaking ->
            voiceState = if (speaking) ALDEFAIVoiceState.Speaking
            else if (voiceState is ALDEFAIVoiceState.Speaking) ALDEFAIVoiceState.Idle
            else voiceState
        }
    }

    fun runAction(intent: ALDEFAIIntent) {
        scope.launch {
            val result = executor.execute(intent)
            say(result.speak)
            if (result.closePanel) {
                delay(600)
                close()
            }
        }
    }

    fun handleIntent(intent: ALDEFAIIntent) {
        when {
            intent is ALDEFAIIntent.Unknown -> say("Maaf, saya belum mengerti perintah itu.")
            intent.requiresConfirmation -> confirmIntent = intent
            else -> runAction(intent)
        }
    }

    // Perintah dikenali → dipetakan ke maksud → dijalankan lewat whitelist.
    LaunchedEffect(finalText) {
        if (finalText.isNotBlank()) {
            val recognized = engine.classify(finalText)
            detected = recognized
            handleIntent(recognized)
        }
    }

    // Continuous Listening: usai satu siklus, dengarkan lagi otomatis selama
    // panel terbuka. Jeda aman setelah beberapa siklus tanpa suara.
    LaunchedEffect(voiceState, autoPaused) {
        if (!continuous || dismissed || autoPaused) return@LaunchedEffect
        when (voiceState) {
            is ALDEFAIVoiceState.Error -> {
                emptyStreak++
                delay(1300)
                if (!dismissed && voiceState is ALDEFAIVoiceState.Error) {
                    voiceState = ALDEFAIVoiceState.Idle
                }
            }
            is ALDEFAIVoiceState.Idle -> {
                if (!micGranted() || confirmIntent != null) return@LaunchedEffect
                if (emptyStreak >= MAX_EMPTY_CYCLES) return@LaunchedEffect
                delay(650)
                if (!dismissed && !autoPaused && confirmIntent == null &&
                    voiceState is ALDEFAIVoiceState.Idle
                ) {
                    startCycle()
                }
            }
            else -> Unit
        }
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (shown) 0.62f else 0f,
        animationSpec = tween(240),
        label = "scrim"
    )

    val listening = voiceState is ALDEFAIVoiceState.Listening
    val processing = voiceState is ALDEFAIVoiceState.Processing
    val speaking = voiceState is ALDEFAIVoiceState.Speaking
    val error = voiceState as? ALDEFAIVoiceState.Error

    val statusPrimary = when {
        error != null -> error.message
        listening -> "Mendengarkan…"
        processing -> "Memproses…"
        speaking -> "Menjawab…"
        needPermission -> "Izin mikrofon diperlukan"
        continuous && (autoPaused || emptyStreak >= MAX_EMPTY_CYCLES) -> "Dijeda · ketuk untuk lanjut"
        finalText.isNotBlank() -> "Terdengar"
        else -> "Ketuk mikrofon untuk mulai"
    }
    val statusIsError = error != null || needPermission
    val transcriptText = when {
        partial.isNotBlank() -> partial
        finalText.isNotBlank() -> finalText
        needPermission -> "Ketuk mikrofon lagi setelah mengizinkan"
        else -> "Ucapkan perintah Anda"
    }
    val commandLabel = detected?.let { d ->
        if (d is ALDEFAIIntent.Unknown) "Perintah belum dikenali"
        else "→ ${d.label} · ${(d.confidence * 100).roundToInt()}%"
    }
    val commandKnown = detected != null && detected !is ALDEFAIIntent.Unknown

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val panelWidthDp = maxWidth * 0.86f
        val panelHeightDp = maxHeight * 0.72f
        val edgePx = with(density) { panelWidthDp.toPx() } * if (onLeft) -1f else 1f

        val slidePx by animateFloatAsState(
            targetValue = if (shown) 0f else edgePx,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
            label = "slide"
        )

        val dragX = remember { Animatable(0f) }
        val maxDrag = with(density) { panelWidthDp.toPx() }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { close() } }
        )

        Box(
            modifier = Modifier
                .align(if (onLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .offset { IntOffset((slidePx + dragX.value).roundToInt(), 0) }
                .width(panelWidthDp)
                .height(panelHeightDp)
                .pointerInput(onLeft) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, delta ->
                            change.consume()
                            val next = (dragX.value + delta).let {
                                if (onLeft) it.coerceIn(-maxDrag, 0f) else it.coerceIn(0f, maxDrag)
                            }
                            scope.launch { dragX.snapTo(next) }
                        },
                        onDragEnd = {
                            val past = if (onLeft) -dragX.value > maxDrag * 0.32f
                            else dragX.value > maxDrag * 0.32f
                            if (past) close() else scope.launch { dragX.animateTo(0f, tween(200)) }
                        },
                        onDragCancel = { scope.launch { dragX.animateTo(0f, tween(200)) } }
                    )
                }
        ) {
            PanelCard(
                onLeft = onLeft,
                listening = listening,
                processing = processing,
                speaking = speaking,
                animationsOn = animationsOn,
                rms = rms,
                statusPrimary = statusPrimary,
                statusIsError = statusIsError,
                transcriptText = transcriptText,
                commandLabel = commandLabel,
                commandKnown = commandKnown,
                showTranscript = showTranscript,
                onMicTap = { onMic() },
                onClose = { close() }
            )
        }

        if (confirmIntent != null) {
            ConfirmDialog(
                prompt = confirmIntent?.label ?: "",
                onCancel = {
                    confirmIntent = null
                    say("Dibatalkan.")
                },
                onConfirm = {
                    val target = confirmIntent
                    confirmIntent = null
                    if (target != null) runAction(target)
                }
            )
        }
    }
}

@Composable
private fun PanelCard(
    onLeft: Boolean,
    listening: Boolean,
    processing: Boolean,
    speaking: Boolean,
    animationsOn: Boolean,
    rms: Float,
    statusPrimary: String,
    statusIsError: Boolean,
    transcriptText: String,
    commandLabel: String?,
    commandKnown: Boolean,
    showTranscript: Boolean,
    onMicTap: () -> Unit,
    onClose: () -> Unit
) {
    val shape = if (onLeft) {
        RoundedCornerShape(topStart = 8.dp, topEnd = 34.dp, bottomEnd = 34.dp, bottomStart = 8.dp)
    } else {
        RoundedCornerShape(topStart = 34.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 34.dp)
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Surface2.copy(alpha = 0.97f), Surface1.copy(alpha = 0.98f))
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(NeonViolet.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.35f))
                    )
                ),
                shape
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    GradientText(
                        text = "ALDEF AI",
                        colors = AiSweep,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "ALDEFTECH · Voice Assistant",
                        color = TextMuted,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    Modifier
                        .size(width = 34.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TextMuted.copy(alpha = 0.5f))
                        .pointerInput(Unit) { detectTapGestures { onClose() } }
                )
            }

            Spacer(Modifier.weight(1f))

            AldeftechMic(
                listening = listening,
                processing = processing,
                speaking = speaking,
                animationsOn = animationsOn,
                rms = rms,
                onTap = onMicTap
            )

            Spacer(Modifier.height(26.dp))

            Text(
                text = statusPrimary,
                color = when {
                    statusIsError -> NeonRed
                    listening || processing -> NeonCyan
                    speaking -> NeonViolet
                    else -> TextSecondary
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // Maksud perintah yang terdeteksi (Phase 7) — belum dijalankan.
            if (commandLabel != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (commandKnown) NeonCyan.copy(alpha = 0.16f)
                            else NeonRed.copy(alpha = 0.14f)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = commandLabel,
                        color = if (commandKnown) NeonCyan else NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (showTranscript) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface1.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transcriptText,
                        color = if (transcriptText.length > 24 || listening) TextPrimary else TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Mikrofon ALDEFTECH — Canvas ringan: kapsul bergradien dengan tanda "^",
 * dudukan, dan cincin pulse yang bereaksi ke tingkat suara saat mendengarkan.
 */
@Composable
private fun AldeftechMic(
    listening: Boolean,
    processing: Boolean,
    speaking: Boolean,
    animationsOn: Boolean,
    rms: Float,
    onTap: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "pulse"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "spin"
    )
    val scale = if (animationsOn && !processing) breathe else 1f
    val accent = when {
        listening -> NeonCyan
        speaking -> NeonViolet
        else -> NeonViolet
    }

    Box(
        modifier = Modifier
            .size(168.dp)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f

            // Cincin pulse bereaksi ke suara.
            if (listening && animationsOn) {
                val boost = 0.08f * rms
                for (i in 0 until 3) {
                    val phase = (pulse + i / 3f) % 1f
                    drawCircle(
                        color = NeonCyan.copy(alpha = (1f - phase) * 0.35f),
                        radius = r * (0.55f + phase * 0.45f + boost),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Gelombang "speaking": equalizer batang mengikuti TTS.
            if (speaking && animationsOn) {
                val bars = 5
                val gap = r * 0.14f
                val baseX = cx - gap * (bars - 1) / 2f
                for (i in 0 until bars) {
                    val h = r * (0.18f + 0.22f * (0.5f + 0.5f *
                        kotlin.math.sin((spin / 40f) + i * 0.9f)))
                    drawLine(
                        color = NeonViolet,
                        start = Offset(baseX + i * gap, cy + r * 0.78f - h),
                        end = Offset(baseX + i * gap, cy + r * 0.78f + h),
                        strokeWidth = r * 0.06f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Busur "processing" berputar.
            if (processing && animationsOn) {
                drawArc(
                    color = NeonCyan,
                    startAngle = spin,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.9f, cy - r * 0.9f),
                    size = Size(r * 1.8f, r * 1.8f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            val discR = r * 0.62f * scale
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(Surface2, Surface1),
                    start = Offset(cx - discR, cy - discR),
                    end = Offset(cx + discR, cy + discR)
                ),
                radius = discR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = accent.copy(alpha = 0.6f),
                radius = discR,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )

            val micW = discR * 0.5f
            val micH = discR * 0.95f
            val micTop = cy - micH * 0.55f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(NeonViolet, NeonMagenta, NeonCyan),
                    startY = micTop,
                    endY = micTop + micH
                ),
                topLeft = Offset(cx - micW / 2f, micTop),
                size = Size(micW, micH),
                cornerRadius = CornerRadius(micW / 2f, micW / 2f)
            )

            // Tanda "^" ALDEFTECH.
            val chevW = micW * 0.5f
            val chevY = micTop + micH * 0.42f
            val chevH = micH * 0.16f
            drawLine(
                color = InkDeep,
                start = Offset(cx - chevW / 2f, chevY + chevH),
                end = Offset(cx, chevY),
                strokeWidth = micW * 0.12f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = InkDeep,
                start = Offset(cx, chevY),
                end = Offset(cx + chevW / 2f, chevY + chevH),
                strokeWidth = micW * 0.12f,
                cap = StrokeCap.Round
            )

            // Dudukan.
            val standR = micW * 0.95f
            val standTop = micTop + micH * 0.55f
            drawArc(
                color = accent,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - standR, standTop - standR + micH * 0.3f),
                size = Size(standR * 2f, standR * 2f),
                style = Stroke(width = micW * 0.1f, cap = StrokeCap.Round)
            )
            val stemTop = standTop + micH * 0.3f + standR * 0.05f
            drawLine(
                color = accent,
                start = Offset(cx, stemTop),
                end = Offset(cx, stemTop + micH * 0.18f),
                strokeWidth = micW * 0.1f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = accent,
                start = Offset(cx - micW * 0.4f, stemTop + micH * 0.18f),
                end = Offset(cx + micW * 0.4f, stemTop + micH * 0.18f),
                strokeWidth = micW * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Dialog konfirmasi untuk aksi yang menandai [ALDEFAIIntent.requiresConfirmation]. */
@Composable
private fun ConfirmDialog(prompt: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onCancel() } },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .padding(30.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Surface2, Surface1)))
                .border(
                    BorderStroke(1.dp, Brush.linearGradient(listOf(NeonViolet, NeonCyan))),
                    RoundedCornerShape(24.dp)
                )
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GradientText(
                    text = "ALDEF AI",
                    colors = AiSweep,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(14.dp))
                Text("Apakah Anda yakin?", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (prompt.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(prompt, color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth()) {
                    DialogButton("BATAL", filled = false, modifier = Modifier.weight(1f), onClick = onCancel)
                    Spacer(Modifier.width(12.dp))
                    DialogButton("KONFIRMASI", filled = true, modifier = Modifier.weight(1f), onClick = onConfirm)
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (filled) Brush.linearGradient(listOf(NeonViolet, NeonCyan))
                else Brush.linearGradient(listOf(Surface1, Surface1))
            )
            .border(
                BorderStroke(1.dp, if (filled) Color.Transparent else NeonRed.copy(alpha = 0.4f)),
                RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled) InkDeep else NeonRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}
