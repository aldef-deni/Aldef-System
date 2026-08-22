package com.aldef.system.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldef.system.ui.theme.BrandSweep
import com.aldef.system.ui.theme.Hairline
import com.aldef.system.ui.theme.Ink
import com.aldef.system.ui.theme.InkDeep
import com.aldef.system.ui.theme.NeonAmber
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonOrange
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.Surface2
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import com.aldef.system.ui.theme.WarmSweep
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Latar "aurora": beberapa gumpalan cahaya yang bergerak pelan pada orbit
 * berbeda. Dipakai di semua layar supaya aplikasi terasa satu kesatuan tanpa
 * gambar latar statis yang membengkakkan ukuran APK.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    tint: List<Color> = BrandSweep,
    intensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "phase"
    )

    Box(modifier = modifier.background(Ink)) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val blobs = listOf(
                Triple(Offset(w * (0.20f + 0.10f * cos(phase)), h * (0.14f + 0.06f * sin(phase))), w * 0.85f, 0),
                Triple(Offset(w * (0.88f + 0.08f * sin(phase * 0.8f)), h * (0.30f + 0.08f * cos(phase * 1.1f))), w * 0.75f, 1),
                Triple(Offset(w * (0.15f + 0.09f * cos(phase * 1.3f)), h * (0.82f + 0.05f * sin(phase * 0.7f))), w * 0.80f, 2),
                Triple(Offset(w * (0.80f + 0.07f * sin(phase * 1.6f)), h * (0.92f + 0.04f * cos(phase))), w * 0.65f, 3)
            )
            blobs.forEach { (center, radius, index) ->
                val color = tint[index % tint.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.16f * intensity), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
            // Vignette supaya konten di tengah tetap kontras.
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, InkDeep.copy(alpha = 0.85f)),
                    center = Offset(w / 2f, h / 2f),
                    radius = max(w, h) * 0.75f
                )
            )
        }
        content()
    }
}

/** Kartu kaca: permukaan semi transparan dengan garis tepi gradien setipis rambut. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderTint: List<Color> = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f)),
    fill: List<Color> = listOf(Surface2.copy(alpha = 0.92f), Surface1.copy(alpha = 0.94f)),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val base = modifier
        .clip(shape)
        .background(Brush.linearGradient(fill))
        .border(BorderStroke(1.dp, Brush.linearGradient(borderTint)), shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = content
    )
}

/** Teks dengan isian gradien merek. */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = BrandSweep,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Black,
    letterSpacing: TextUnit = 0.sp,
    textAlign: TextAlign = TextAlign.Unspecified
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = Brush.linearGradient(colors),
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            textAlign = textAlign
        )
    )
}

/** Bar atas seragam: tombol kembali bulat, judul, dan aksi opsional. */
@Composable
fun AldefTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CircleIconButton(
                icon = Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Kembali",
                onClick = onBack
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }
        }
        actions()
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    tint: Color = TextPrimary,
    active: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) NeonOrange.copy(alpha = 0.18f) else Surface2.copy(alpha = 0.9f))
            .border(1.dp, if (active) NeonOrange.copy(alpha = 0.6f) else Hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) NeonAmber else tint,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

/** Tombol utama dengan isian gradien dan sedikit "napas" saat ditekan. */
@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = WarmSweep,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) Brush.linearGradient(colors)
                else Brush.linearGradient(listOf(Surface2, Surface1))
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Icon(
                    imageVector = leading,
                    contentDescription = null,
                    tint = if (enabled) InkDeep else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = if (enabled) InkDeep else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/** Tombol sekunder bergaya "outline" dengan tepi bergradien. */
@Composable
fun OutlineNeonButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = BrandSweep,
    leading: ImageVector? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressOutline"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, Brush.linearGradient(colors)), RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Icon(
                    imageVector = leading,
                    contentDescription = null,
                    tint = colors.first(),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        letterSpacing = 2.sp
    )
}

/** Kotak metrik kecil: label di atas, angka besar di bawah. */
@Composable
fun StatTile(
    label: String,
    value: String,
    unit: String? = null,
    accent: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = NumericFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (unit != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

/** Garis pemisah tipis bergradien. */
@Composable
fun GradientDivider(modifier: Modifier = Modifier, colors: List<Color> = BrandSweep) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent) + colors.map { it.copy(alpha = 0.5f) } + listOf(Color.Transparent)
                )
            )
    )
}
