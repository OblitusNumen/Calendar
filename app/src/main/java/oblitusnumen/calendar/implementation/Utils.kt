package oblitusnumen.calendar.implementation

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.*
import java.text.SimpleDateFormat
import java.time.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

val LIST_CENTER: LocalDate = LocalDate.of(1970, 1, 1)
const val LIST_LEN = 420168000 * 2 // ~5M years, should be enough (no point fixing bugs at >5M)

fun ZonedDateTime.toEpochDays(): Long {
    return toLocalDate().toEpochDay()
}

fun Int.toColor(): Color? = if (this == -1) null else Color(this or 0xFF000000.toInt())

fun Color?.toInt(): Int = if (this == null) -1 else (this.toArgb() and 0xFFFFFF)

fun <T : Comparable<T>> List<T>.sorted() = this.sortedBy { it }

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

fun log(o: Any?) {
    log("calendar", o)
}

var logfile: File? = null
fun setLogFile(c: Context) {
    if (logfile == null)
        logfile = File(c.filesDir, "logfile.log")
}

fun log(tag: String?, o: Any?) {
    Log.v(tag, o.toString())
    // FIXME: for testing
//    if (logfile != null)
//        logfile!!.appendText("[${LocalDateTime.now()}] $tag: $o\n")
}

private fun colorToLuminance(color: Color): Double {
    return 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
}

fun bgColorToTextColor(color: Color): Color {
    return if (colorToLuminance(color) < 0.5) Color.White else Color.Black
}

fun multFrac(x: Long, numer: Long, denominator: Long): Long {
    val quot = (x) / (denominator)
    val rem = (x) % (denominator)
    return (quot * (numer)) + ((rem * (numer)) / (denominator))
}

fun LocalDate.toWeekNumber(): Long {
    val v = toEpochDay() + 3
    return if (v >= 0) (v / 7) else (v / 7 - 1)
}

fun unzipFile(zipFile: File, targetDir: File) {
    ZipInputStream(FileInputStream(zipFile)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val newFile = File(targetDir, entry.name)
            if (entry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile?.mkdirs()
                FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

fun zipDirectoryToStream(sourceDir: File, outputStream: OutputStream) {
    ZipOutputStream(outputStream).use { zos ->
        sourceDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val entryName = file.relativeTo(sourceDir).path
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        zos.flush()
    }
}
