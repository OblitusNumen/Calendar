package oblitusnumen.calendar.implementation.data

import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class DateOccurrence(
    val occurrence: LocalDateTime,
    val occurrenceZoned: ZonedDateTime,
    val date: ViewDateWithOptions
) {
    fun startEpochSecond() = occurrenceZoned.toEpochSecond()
    fun endEpochSecond() = date.duration.addTo(occurrenceZoned, 1).toEpochSecond()
}