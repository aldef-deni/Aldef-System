package com.aldef.system.ui.theme

import androidx.compose.ui.graphics.Color

// Palet diambil dari logo Aldef Tech: neon jingga -> magenta -> violet -> cyan
// di atas latar hitam pekat.

val Ink = Color(0xFF05060A)
val InkDeep = Color(0xFF020306)
val Surface1 = Color(0xFF0B0D14)
val Surface2 = Color(0xFF11141D)
val Surface3 = Color(0xFF181C28)
val Hairline = Color(0xFF232838)

val NeonOrange = Color(0xFFFF7A18)
val NeonAmber = Color(0xFFFFB020)
val NeonMagenta = Color(0xFFC724FF)
val NeonViolet = Color(0xFF7B2BFF)
val NeonCyan = Color(0xFF22D3EE)
val NeonBlue = Color(0xFF2E7DFF)
val NeonGreen = Color(0xFF16E0A3)
val NeonRed = Color(0xFFFF4D6D)

val TextPrimary = Color(0xFFF3F5FA)
val TextSecondary = Color(0xFF9AA3B8)
val TextMuted = Color(0xFF5C6478)

/** Gradien tanda tangan merek — dipakai untuk judul, tombol utama, dan garis aksen. */
val BrandSweep = listOf(NeonOrange, NeonMagenta, NeonViolet, NeonCyan)
val WarmSweep = listOf(NeonAmber, NeonOrange, NeonMagenta)
val CoolSweep = listOf(NeonCyan, NeonBlue, NeonViolet)
