package oblitusnumen.calendar.implementation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

fun Int.toColor(): Color? = if (this == -1) null else Color(this or 0xFF000000.toInt())

fun Color?.toInt(): Int = if (this == null) -1 else (this.toArgb() and 0xFFFFFF)

fun rmRecursively(file: File) {
    if (file.isDirectory) {
        val listFiles = file.listFiles() ?: throw IllegalStateException("never occurs")
        for (listFile in listFiles) {
            rmRecursively(listFile)
        }
    }
    if (!file.delete()) throw IOException("could not delete file $file")
}

fun getZonedFromEpochSeconds(epochSeconds: Long): ZonedDateTime {
    return Instant.ofEpochSecond(epochSeconds).atZone(defaultZoneId())
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun zonedDateTime(day: LocalDate): ZonedDateTime {
    return day.atStartOfDay().atZone(defaultZoneId())
}

fun defaultZoneId(): ZoneId {
    return ZoneId.systemDefault()
}

fun log(o: Any) {
    log("calendar", o)
}

fun log(tag: String?, o: Any) {
    Log.v(tag, o.toString())
}

private fun colorToLuminance(color: Color): Double {
    return 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
}

fun bgColorToTextColor(color: Color): Color {
    return if (colorToLuminance(color) < 0.5) Color.White else Color.Black
}

@Composable
fun measureTextLine(style: TextStyle): Dp {
    val textMeasurer = rememberTextMeasurer()
    val linePx = remember(textMeasurer, style) {
        textMeasurer.measure("0", style).size.height
    }
    return with(LocalDensity.current) { linePx.toDp() }
}

fun mult_frac(x: Long, numer: Long, denom: Long): Long {
    val quot = (x) / (denom)
    val rem = (x) % (denom)
    return (quot * (numer)) + ((rem * (numer)) / (denom))
}