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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatDateTime(epochSecond: Long, zoneId: ZoneId): String =
    Instant.ofEpochSecond(epochSecond).atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))

fun formatTime(quarterHours: Int): String {
    val hours = quarterHours / 4
    val minutes = (quarterHours % 4) * 15
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
