package com.aldef.system.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.data.AppPrefs
import com.aldef.system.data.ExpressionEvaluator
import com.aldef.system.data.Screen
import com.aldef.system.security.VaultSession
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.theme.Hairline
import com.aldef.system.ui.theme.InkDeep
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonMagenta
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NeonViolet
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.Surface3
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary

private enum class KeyKind { DIGIT, OPERATOR, FUNCTION, EQUALS }

private data class CalcKey(val label: String, val kind: KeyKind)

private val KEYPAD: List<List<CalcKey>> = listOf(
    listOf(
        CalcKey("AC", KeyKind.FUNCTION),
        CalcKey("( )", KeyKind.FUNCTION),
        CalcKey("%", KeyKind.FUNCTION),
        CalcKey("÷", KeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("7", KeyKind.DIGIT),
        CalcKey("8", KeyKind.DIGIT),
        CalcKey("9", KeyKind.DIGIT),
        CalcKey("×", KeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("4", KeyKind.DIGIT),
        CalcKey("5", KeyKind.DIGIT),
        CalcKey("6", KeyKind.DIGIT),
        CalcKey("−", KeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("1", KeyKind.DIGIT),
        CalcKey("2", KeyKind.DIGIT),
        CalcKey("3", KeyKind.DIGIT),
        CalcKey("+", KeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("0", KeyKind.DIGIT),
        CalcKey(".", KeyKind.DIGIT),
        CalcKey("⌫", KeyKind.FUNCTION),
        CalcKey("=", KeyKind.EQUALS)
    )
)

/**
 * Kalkulator biasa di permukaan — dan pintu ke brankas berkas di baliknya:
 * ketik PIN brankas lalu tekan `=`, layar akan berpindah ke brankas alih-alih
 * menghitung. Tidak ada tombol atau petunjuk apa pun di layar ini yang
 * membocorkan keberadaan brankas.
 */
@Composable
fun CalculatorScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }

    var expression by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<String>() }

    fun refreshPreview(next: String) {
        preview = if (next.isBlank()) {
            ""
        } else {
            when (val result = ExpressionEvaluator.evaluate(next)) {
                is ExpressionEvaluator.Result.Value -> ExpressionEvaluator.format(result.value)
                is ExpressionEvaluator.Result.Error -> ""
            }
        }
    }

    fun append(text: String) {
        error = null
        val next = expression + text
        expression = next
        refreshPreview(next)
    }

    fun toggleParenthesis() {
        val opens = expression.count { it == '(' }
        val closes = expression.count { it == ')' }
        val last = expression.lastOrNull()
        val needsClose = opens > closes && (last?.isDigit() == true || last == ')' || last == '%')
        append(if (needsClose) ")" else "(")
    }

    fun evaluate() {
        // Gerbang tersembunyi: rangkaian angka yang cocok dengan PIN brankas
        // membuka brankas, bukan menghitung.
        if (expression.isNotEmpty() && expression.all { it.isDigit() } && prefs.checkVaultPin(expression)) {
            VaultSession.unlock(expression)
            expression = ""
            preview = ""
            error = null
            navController.navigate(Screen.Vault.route)
            return
        }

        when (val result = ExpressionEvaluator.evaluate(expression)) {
            is ExpressionEvaluator.Result.Value -> {
                val formatted = ExpressionEvaluator.format(result.value)
                history.add(0, "$expression = $formatted")
                if (history.size > 12) history.removeAt(history.lastIndex)
                expression = formatted.replace(",", "")
                preview = ""
                error = null
            }

            is ExpressionEvaluator.Result.Error -> {
                error = result.message
                preview = ""
            }
        }
    }

    fun press(key: CalcKey) {
        when (key.label) {
            "AC" -> {
                expression = ""
                preview = ""
                error = null
                history.clear()
            }

            "⌫" -> {
                val next = expression.dropLast(1)
                expression = next
                error = null
                refreshPreview(next)
            }

            "( )" -> toggleParenthesis()
            "=" -> evaluate()
            else -> append(key.label)
        }
    }

    AuroraBackground(
        modifier = Modifier.fillMaxSize(),
        tint = listOf(NeonViolet, NeonMagenta, NeonCyan, NeonOrange)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AldefTopBar(
                title = "Kalkulator",
                subtitle = "PRESISI DESIMAL PENUH",
                onBack = { navController.popBackStack() }
            )

            // Riwayat singkat, tergulir mendatar supaya tidak makan tinggi layar.
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.forEach { line ->
                        GlassCard(shape = RoundedCornerShape(12.dp)) {
                            Text(
                                text = line,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = NumericFont
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifBlank { "0" },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontFamily = NumericFont,
                    fontSize = if (expression.length > 16) 30.sp else 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = error ?: preview.takeIf { it.isNotBlank() }?.let { "= $it" } ?: "",
                    fontFamily = NumericFont,
                    fontSize = 17.sp,
                    color = if (error != null) NeonRed else TextSecondary
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KEYPAD.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { key ->
                            CalculatorButton(
                                key = key,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.12f),
                                onClick = { press(key) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CalculatorButton(key: CalcKey, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "keyPress"
    )

    val background: Brush = when (key.kind) {
        KeyKind.EQUALS -> Brush.linearGradient(listOf(NeonOrange, NeonMagenta))
        KeyKind.OPERATOR -> Brush.linearGradient(
            listOf(Surface3.copy(alpha = 0.95f), Surface2.copy(alpha = 0.95f))
        )
        KeyKind.FUNCTION -> Brush.linearGradient(
            listOf(Surface2.copy(alpha = 0.9f), Surface1.copy(alpha = 0.9f))
        )
        KeyKind.DIGIT -> Brush.linearGradient(
            listOf(Surface2.copy(alpha = 0.72f), Surface1.copy(alpha = 0.82f))
        )
    }

    val contentColor = when (key.kind) {
        KeyKind.EQUALS -> InkDeep
        KeyKind.OPERATOR -> NeonOrange
        KeyKind.FUNCTION -> NeonCyan
        KeyKind.DIGIT -> TextPrimary
    }

    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (key.kind == KeyKind.EQUALS) Color.Transparent else Hairline,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.label,
            color = contentColor,
            fontSize = if (key.label.length > 2) 16.sp else 24.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (key.kind == KeyKind.DIGIT) NumericFont else MaterialTheme.typography.titleLarge.fontFamily
        )
    }
}
