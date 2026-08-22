package com.aldef.system.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Penghitung ekspresi aritmetika sederhana.
 *
 * Sengaja memakai [BigDecimal], bukan Double: kalkulator yang menampilkan
 * `0.30000000000000004` untuk `0.1 + 0.2` terasa rusak walaupun secara IEEE-754
 * benar.
 */
object ExpressionEvaluator {

    private val mathContext = MathContext(16, RoundingMode.HALF_UP)

    sealed interface Result {
        data class Value(val value: BigDecimal) : Result
        data class Error(val message: String) : Result
    }

    fun evaluate(expression: String): Result {
        val normalized = expression
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace(",", "")
            .trim()
        if (normalized.isEmpty()) return Result.Error("Kosong")
        return runCatching {
            val parser = Parser(normalized)
            val value = parser.parseExpression()
            parser.expectEnd()
            Result.Value(value.stripTrailingZeros())
        }.getOrElse { error ->
            Result.Error(error.message ?: "Ekspresi tidak valid")
        }
    }

    /** Format angka ala kalkulator: pemisah ribuan, tanpa nol berlebih. */
    fun format(value: BigDecimal): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val plain = value.stripTrailingZeros()
        // Angka yang terlalu besar/kecil lebih terbaca dalam notasi ilmiah.
        if (plain.abs() >= BigDecimal("1E12") ||
            (plain.abs() < BigDecimal("1E-6") && plain.signum() != 0)
        ) {
            return DecimalFormat("0.########E0", symbols).format(plain)
        }
        return DecimalFormat("#,##0.##########", symbols).format(plain)
    }

    /**
     * Parser turun-rekursif.
     *
     * ekspresi := suku (('+' | '-') suku)*
     * suku     := faktor (('*' | '/') faktor)*
     * faktor   := ['-'] primer ['%']
     * primer   := angka | '(' ekspresi ')'
     */
    private class Parser(private val text: String) {
        private var pos = 0

        fun parseExpression(): BigDecimal {
            var left = parseTerm()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> { pos++; left = left.add(parseTerm(), mathContext) }
                    '-' -> { pos++; left = left.subtract(parseTerm(), mathContext) }
                    else -> return left
                }
            }
        }

        private fun parseTerm(): BigDecimal {
            var left = parseFactor()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '*' -> { pos++; left = left.multiply(parseFactor(), mathContext) }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor.signum() == 0) throw ArithmeticException("Tidak bisa dibagi nol")
                        left = left.divide(divisor, mathContext)
                    }
                    else -> return left
                }
            }
        }

        private fun parseFactor(): BigDecimal {
            skipSpaces()
            var sign = BigDecimal.ONE
            while (peek() == '-' || peek() == '+') {
                if (peek() == '-') sign = sign.negate()
                pos++
                skipSpaces()
            }
            var value = parsePrimary().multiply(sign, mathContext)
            skipSpaces()
            // Persen ditulis di belakang angka: 50% -> 0.5
            while (peek() == '%') {
                pos++
                value = value.divide(BigDecimal(100), mathContext)
                skipSpaces()
            }
            return value
        }

        private fun parsePrimary(): BigDecimal {
            skipSpaces()
            if (peek() == '(') {
                pos++
                val inner = parseExpression()
                skipSpaces()
                if (peek() != ')') throw IllegalArgumentException("Kurung tidak ditutup")
                pos++
                return inner
            }
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("Ekspresi tidak lengkap")
            return BigDecimal(text.substring(start, pos))
        }

        fun expectEnd() {
            skipSpaces()
            if (pos != text.length) throw IllegalArgumentException("Ekspresi tidak lengkap")
        }

        private fun peek(): Char? = text.getOrNull(pos)

        private fun skipSpaces() {
            while (pos < text.length && text[pos] == ' ') pos++
        }
    }
}
