package com.aldef.system.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.aldef.system.ui.theme.*
import kotlin.math.*

@Composable
fun CalculatorScreen(navController: NavController) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var showHistory by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }

    fun calculateResult(expr: String): String {
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/").replace("−", "-")
            val result = evaluateExpression(sanitized)
            if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                String.format("%.10g", result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Kalkulator", fontWeight = FontWeight.Bold, color = PremiumGold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = PremiumGold)
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            if (showHistory) Icons.Filled.Calculate else Icons.Filled.History,
                            contentDescription = "History",
                            tint = PremiumGold
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (showHistory) {
                // History panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Riwayat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (history.isEmpty()) {
                                Text(
                                    "Belum ada riwayat",
                                    color = TextGray,
                                    fontSize = 13.sp
                                )
                            }
                            history.reversed().forEach { item ->
                                Text(
                                    text = item,
                                    fontSize = 13.sp,
                                    color = TextGrayLight,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkCard)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = expression.ifEmpty { " " },
                        fontSize = 24.sp,
                        color = TextGray,
                        maxLines = 2,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextWhite,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button grid
            val buttons = listOf(
                listOf("C", "()", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("⌫", "0", ".", "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { label ->
                        CalcButton(
                            label = label,
                            modifier = Modifier.weight(1f),
                            isOperator = label in listOf("÷", "×", "−", "+", "="),
                            isSpecial = label in listOf("C", "()", "%", "⌫"),
                            onClick = {
                                when (label) {
                                    "C" -> {
                                        expression = ""
                                        result = "0"
                                    }
                                    "⌫" -> {
                                        if (expression.isNotEmpty()) {
                                            expression = expression.dropLast(1)
                                        }
                                    }
                                    "=" -> {
                                        if (expression.isNotEmpty()) {
                                            val res = calculateResult(expression)
                                            history.add("$expression = $res")
                                            result = res
                                            expression = res
                                        }
                                    }
                                    else -> {
                                        expression += label
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CalcButton(
    label: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    val scaleAnim = remember { mutableStateOf(1f) }
    val scale by animateFloatAsState(
        targetValue = scaleAnim.value,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    LaunchedEffect(scaleAnim.value) {
        if (scaleAnim.value < 1f) {
            kotlinx.coroutines.delay(100)
            scaleAnim.value = 1f
        }
    }

    val backgroundColor = when {
        label == "=" -> Brush.linearGradient(listOf(PremiumGold, PremiumGoldDark))
        isOperator -> Brush.linearGradient(listOf(PremiumPurple, PremiumPurpleLight.copy(alpha = 0.7f)))
        isSpecial -> Brush.linearGradient(listOf(DarkCardLight, DarkCard))
        else -> Brush.linearGradient(listOf(DarkCardLight, DarkCard))
    }

    val textColor = when {
        label == "=" -> DarkBackground
        isOperator -> PremiumPurpleLight
        isSpecial -> PremiumGold
        else -> TextWhite
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable {
                scaleAnim.value = 0.9f
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (isOperator) 22.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// Simple expression evaluator
fun evaluateExpression(expression: String): Double {
    val tokens = tokenize(expression)
    var result = tokens[0] as Double
    var i = 1

    while (i < tokens.size) {
        val operator = tokens[i] as String
        val operand = tokens[i + 1] as Double
        when (operator) {
            "+" -> result += operand
            "-" -> result -= operand
            "*" -> result *= operand
            "/" -> result /= operand
            "%" -> result %= operand
        }
        i += 2
    }
    return result
}

fun tokenize(expression: String): List<Any> {
    val tokens = mutableListOf<Any>()
    var current = StringBuilder()
    var i = 0

    while (i < expression.length) {
        val c = expression[i]
        if (c.isDigit() || c == '.') {
            current.append(c)
        } else if (c in listOf('+', '-', '*', '/', '%')) {
            if (current.isNotEmpty()) {
                tokens.add(current.toString().toDouble())
                current = StringBuilder()
            }
            if (tokens.isEmpty() && c == '-') {
                current.append(c)
            } else {
                tokens.add(c.toString())
            }
        }
        i++
    }
    if (current.isNotEmpty()) {
        tokens.add(current.toString().toDouble())
    }
    return tokens
}
