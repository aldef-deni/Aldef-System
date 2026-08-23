package com.aldef.system.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aldef.system.data.Holidays
import com.aldef.system.notify.HolidayReminder
import com.aldef.system.ui.components.AldefTopBar
import com.aldef.system.ui.components.AuroraBackground
import com.aldef.system.ui.components.CircleIconButton
import com.aldef.system.ui.components.GlassCard
import com.aldef.system.ui.components.GradientDivider
import com.aldef.system.ui.components.GradientText
import com.aldef.system.ui.theme.BrandSweep
import com.aldef.system.ui.theme.CoolSweep
import com.aldef.system.ui.theme.Hairline
import com.aldef.system.ui.theme.InkDeep
import com.aldef.system.ui.theme.NeonBlue
import com.aldef.system.ui.theme.NeonCyan
import com.aldef.system.ui.theme.NeonRed
import com.aldef.system.ui.theme.NumericFont
import com.aldef.system.ui.theme.Surface1
import com.aldef.system.ui.theme.TextMuted
import com.aldef.system.ui.theme.TextPrimary
import com.aldef.system.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

private val ID_LOCALE = Locale("in", "ID")
private const val START_YEAR = 2020
private const val END_YEAR = 2035
private val WEEKDAYS = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

private fun monthIndex(ym: YearMonth): Int = (ym.year - START_YEAR) * 12 + (ym.monthValue - 1)
private fun monthAt(index: Int): YearMonth =
    YearMonth.of(START_YEAR + index / 12, index % 12 + 1)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarScreen(navController: NavController) {
    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.from(today) }
    val totalMonths = (END_YEAR - START_YEAR + 1) * 12

    val pagerState = rememberPagerState(
        initialPage = monthIndex(currentMonth),
        pageCount = { totalMonths }
    )
    val scope = rememberCoroutineScope()

    // Membuka kalender sekaligus meminta izin notifikasi (Android 13+) supaya
    // pengingat H-1 libur bisa muncul.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notif = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notif.status.isGranted) notif.launchPermissionRequest()
        }
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) { HolidayReminder.schedule(context) }

    val visibleMonth = monthAt(pagerState.currentPage)

    AuroraBackground(modifier = Modifier.fillMaxSize(), tint = CoolSweep) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AldefTopBar(
                title = "Kalender",
                subtitle = "LIBUR NASIONAL INDONESIA",
                onBack = { navController.popBackStack() },
                actions = {
                    CircleIconButton(
                        icon = Icons.Rounded.Today,
                        contentDescription = "Ke bulan ini",
                        active = pagerState.currentPage == monthIndex(currentMonth),
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(monthIndex(currentMonth)) }
                        }
                    )
                }
            )

            // Tahun besar sebagai penanda konteks carousel.
            GradientText(
                text = visibleMonth.year.toString(),
                colors = CoolSweep,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 30.dp),
                pageSpacing = 14.dp
            ) { page ->
                // Efek carousel: halaman tengah penuh, tetangganya mengecil & pudar.
                val offset = ((pagerState.currentPage - page) +
                    pagerState.currentPageOffsetFraction).absoluteValue
                val scale = lerp(0.86f, 1f, (1f - offset).coerceIn(0f, 1f))
                val alpha = lerp(0.4f, 1f, (1f - offset).coerceIn(0f, 1f))

                MonthPage(
                    month = monthAt(page),
                    today = today,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Petunjuk arah geser.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { i ->
                    val active = i == 2
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (active) NeonCyan else TextMuted.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthPage(month: YearMonth, today: LocalDate, modifier: Modifier = Modifier) {
    val holidays = remember(month) {
        Holidays.holidaysForYear(month.year).filter { YearMonth.from(it.date) == month }
    }
    val isCurrentMonth = YearMonth.from(today) == month

    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        borderTint = if (isCurrentMonth) {
            listOf(NeonCyan.copy(alpha = 0.5f), NeonBlue.copy(alpha = 0.3f))
        } else {
            listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.03f))
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = month.month
                    .getDisplayName(TextStyle.FULL, ID_LOCALE)
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = month.year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(16.dp))

            // Baris nama hari.
            Row(Modifier.fillMaxWidth()) {
                WEEKDAYS.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (index == 0) NeonRed else TextMuted
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Kisi tanggal. Kolom pertama = Minggu (dayOfWeek 7 -> 0).
            val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
            val daysInMonth = month.lengthOfMonth()
            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOffset + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (dayNumber in 1..daysInMonth) {
                                DayCell(
                                    date = month.atDay(dayNumber),
                                    isToday = month.atDay(dayNumber) == today
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            GradientDivider(colors = CoolSweep)
            Spacer(Modifier.height(14.dp))

            Text(
                text = "TANGGAL MERAH BULAN INI",
                color = TextMuted,
                fontSize = 9.sp,
                letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(10.dp))

            if (holidays.isEmpty()) {
                Text(
                    text = "Tidak ada libur nasional bulan ini",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            } else {
                holidays.forEach { holiday ->
                    HolidayRow(
                        day = holiday.date.dayOfMonth,
                        weekday = holiday.date.dayOfWeek
                            .getDisplayName(TextStyle.SHORT, ID_LOCALE),
                        name = holiday.name,
                        highlight = holiday.date == today
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean) {
    val isRed = Holidays.isRedDate(date)
    val hasHoliday = remember(date) { Holidays.holidaysOn(date).isNotEmpty() }

    Box(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .aspectRatio(1f)
            .then(
                if (isToday) {
                    Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(BrandSweep))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                fontFamily = NumericFont,
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isToday -> InkDeep
                    isRed -> NeonRed
                    else -> TextPrimary
                }
            )
            // Titik penanda hari libur (bukan sekadar Minggu).
            if (hasHoliday && !isToday) {
                Box(
                    Modifier
                        .padding(top = 1.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(NeonRed)
                )
            }
        }
    }
}

@Composable
private fun HolidayRow(day: Int, weekday: String, name: String, highlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (highlight) {
                        Brush.linearGradient(listOf(NeonRed, NeonRed.copy(alpha = 0.7f)))
                    } else {
                        Brush.linearGradient(
                            listOf(NeonRed.copy(alpha = 0.16f), Surface1.copy(alpha = 0.6f))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                fontFamily = NumericFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) InkDeep else NeonRed
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = weekday,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        if (highlight) {
            Icon(
                imageVector = Icons.Rounded.Circle,
                contentDescription = "Hari ini",
                tint = NeonRed,
                modifier = Modifier.size(8.dp)
            )
        }
    }
}

/** Interpolasi linier sederhana. */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
