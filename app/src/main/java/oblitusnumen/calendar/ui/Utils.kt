package oblitusnumen.calendar.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val QUARTERS_PER_HOUR = 4
const val MINUTES_PER_QUARTER = 15

fun formatDateTime(epochSecond: Long, zoneId: ZoneId): String {
    val zdt = Instant.ofEpochSecond(epochSecond).atZone(zoneId)
    val today = LocalDate.now(zoneId)
    val date = zdt.toLocalDate()
    val time = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when {
        date == today -> "Today $time"
        date == today.plusDays(1) -> "Tomorrow $time"
        date == today.minusDays(1) -> "Yesterday $time"
        date.year == today.year -> zdt.format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
        else -> zdt.format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"))
    }
}

fun formatTime(quarterHours: Int): String {
    val hours = quarterHours / QUARTERS_PER_HOUR
    val minutes = (quarterHours % QUARTERS_PER_HOUR) * MINUTES_PER_QUARTER
    return if (minutes == 0) "${hours}h" else "${hours}h ${minutes}m"
}

@Composable
fun measureTextLine(style: TextStyle, text: String = "0"): Dp {
    val textMeasurer = rememberTextMeasurer()
    val linePx = remember(textMeasurer, style) {
        textMeasurer.measure(text, style).size.height
    }
    return with(LocalDensity.current) { linePx.toDp() }
}

@Composable
fun dpByDpForPixelPerfect(dp: Float): Dp {
    val dpPerPx = with(LocalDensity.current) { 1.toDp() }
    if (dpPerPx > 1.dp)
        return dpPerPx * dp
    val pxPerDp = (1.dp / dpPerPx).toInt()
    if (1.dp - dpPerPx * pxPerDp < dpPerPx * (pxPerDp + 1) - 1.dp)
        return dpPerPx * pxPerDp * dp
    return dpPerPx * (pxPerDp + 1) * dp
}

@Composable
fun PaddingValues.horizontal(): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
        return this@horizontal.calculateLeftPadding(layoutDirection)
    }

    override fun calculateTopPadding(): Dp {
        return 0.dp
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
        return this@horizontal.calculateRightPadding(layoutDirection)
    }

    override fun calculateBottomPadding(): Dp {
        return 0.dp
    }
}

enum class PositionStatus {
    Visible, Above, Below
}
