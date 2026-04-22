package oblitusnumen.calendar.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.Period
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val QUARTERS_PER_HOUR = 4
const val MINUTES_PER_QUARTER = 15

fun formatDateTime(context: Context, epochSecond: Long, zoneId: ZoneId): String {
    val zdt = Instant.ofEpochSecond(epochSecond).atZone(zoneId)
    val today = LocalDate.now(zoneId)
    val date = zdt.toLocalDate()
    val time = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when {
        date == today -> "${context.getString(R.string.agenda_today)} $time"
        date == today.plusDays(1) -> "${context.getString(R.string.agenda_tomorrow)} $time"
        date == today.minusDays(1) -> "${context.getString(R.string.agenda_yesterday)} $time"
        date.year == today.year -> zdt.format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
        else -> zdt.format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"))
    }
}

fun formatTime(context: Context, quarterHours: Int): String {
    val hours = quarterHours / QUARTERS_PER_HOUR
    val minutes = (quarterHours % QUARTERS_PER_HOUR) * MINUTES_PER_QUARTER
    val h = context.getString(R.string.edit_task_unit_hour)
    val m = context.getString(R.string.edit_task_unit_minute)
    return if (minutes == 0) "${hours}$h" else "${hours}$h ${minutes}$m"
}

fun Period.displayCount(context: Context): String {
    val c = count.toInt()
    return when (this) {
        is Period.Once -> ""
        is Period.Minute -> context.resources.getQuantityString(R.plurals.period_minute_count, c, c)
        is Period.Hour -> context.resources.getQuantityString(R.plurals.period_hour_count, c, c)
        is Period.Day -> context.resources.getQuantityString(R.plurals.period_day_count, c, c)
        is Period.Week, is Period.Weekday ->
            context.resources.getQuantityString(R.plurals.period_week_count, c, c)
        is Period.Month -> context.resources.getQuantityString(R.plurals.period_month_count, c, c)
        is Period.Year -> context.resources.getQuantityString(R.plurals.period_year_count, c, c)
    }
}

fun Period.displayOffsetBefore(context: Context): String =
    if (this is Period.Once) context.getString(R.string.period_at_event_time)
    else context.getString(R.string.period_offset_before, displayCount(context))

fun Period.displayUnitName(context: Context): String = when (this) {
    is Period.Once -> context.getString(R.string.period_unit_once)
    is Period.Minute -> context.getString(R.string.period_unit_minute)
    is Period.Hour -> context.getString(R.string.period_unit_hour)
    is Period.Day -> context.getString(R.string.period_unit_day)
    is Period.Week, is Period.Weekday -> context.getString(R.string.period_unit_week)
    is Period.Month -> context.getString(R.string.period_unit_month)
    is Period.Year -> context.getString(R.string.period_unit_year)
}

fun Period.displayOffsetUnitName(context: Context): String =
    if (this is Period.Once) context.getString(R.string.period_unit_at_time)
    else displayUnitName(context)

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
