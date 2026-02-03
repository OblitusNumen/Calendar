package oblitusnumen.calendar.implementation.data

import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.*
import java.time.*
import java.util.*
import kotlin.math.min

class ViewDateWithOptions(
    val dateId: Int,
    val entryId: Int,
    val epochSecondChainStart: Long,
    val duration: Period,// FIXME: fixup in db in date
    val epochSecondChainEnd: Long,// FIXME: account for duration in db in date
    val timesRepeat: Long,
    val period: Period,
    val timeZoneId: ZoneId,
    val exceptionRules: ExceptionRules,
    val name: String,
    val color: Color
) {
    val displayName: String = name.ifEmpty { "[No title]" } // FIXME:

    fun anyInRange(start: Long, finish: Long): ZonedDateTime? {// FIXME: measure performance (weekdays especially)
        val zonedDateTime = getZonedDateTimeInRange(start, finish)
        val period = this.period
        if (zonedDateTime == null || exceptionRules.containsDate(zonedDateTime.toEpochDays()) ||
            (period is Period.Weekday && !period.verifyWeekday(// FIXME: works wacky for cases when more than one event in range
                getFirstZoneDateTime().toLocalDate(),
                zonedDateTime.toLocalDate()
            ))
        ) return null
        return zonedDateTime
    }

    private fun checkZonedDateTime(zonedDateTime: ZonedDateTime): Boolean =
        !(exceptionRules.containsDate(zonedDateTime.toEpochDays()) ||
                (period is Period.Weekday && !period.verifyWeekday(// FIXME: works wacky for cases when more than one event in range
                    getFirstZoneDateTime().toLocalDate(),
                    zonedDateTime.toLocalDate()
                )))// FIXME: works wacky for cases when more than one event in range

    fun allIntersectingRange(start: Long, end: Long): Collection<ZonedDateTime> {
        val result = mutableListOf<ZonedDateTime>()

        if (duration is Period.Once) {
            anyInRange(start, end)?.let { result.add(it) }
            return result
        }

        val start = duration.addTo(Instant.ofEpochSecond(start).atZone(timeZoneId), -1).toEpochSecond()
        val anyZonedDateTime = getZonedDateTimeInRange(start, end) ?: return emptyList()

        var zonedDateTime = anyZonedDateTime
        while (start < zonedDateTime.toEpochSecond()) {
            if (checkZonedDateTime(zonedDateTime))
                result.add(zonedDateTime)
            zonedDateTime = period.addTo(zonedDateTime, -1)
        }
        zonedDateTime = period.addTo(anyZonedDateTime, 1)
        while (zonedDateTime.toEpochSecond() < end) {
            if (checkZonedDateTime(zonedDateTime))
                result.add(zonedDateTime)
            zonedDateTime = period.addTo(zonedDateTime, 1)
        }
        return result
    }

    fun getFirstZoneDateTime(): ZonedDateTime {
        return getZoneDateTime(0)
    }

    private fun getZoneDateTime(idx: Long): ZonedDateTime {
        return period.addTo(Instant.ofEpochSecond(epochSecondChainStart).atZone(timeZoneId), idx)
    }

    private fun getZonedDateTimeInRange(start: Long, finish: Long): ZonedDateTime? { //any(?) in range
        if (finish <= this.epochSecondChainStart || timesRepeat == 0L) return null
        if (this.epochSecondChainEnd == this.epochSecondChainStart) return if (this.epochSecondChainStart >= start) getZoneDateTime(
            0
        ) else null
        val periodExpect = (this.epochSecondChainEnd - this.epochSecondChainStart) / (timesRepeat - 1)
        val idxExpect = (finish - this.epochSecondChainStart) / periodExpect
        //mult_frac will overflow with period 1D after a few million years, but who cares about that
        val timeEst = multFrac(
            this.epochSecondChainEnd - this.epochSecondChainStart,
            idxExpect,
            timesRepeat
        ) + this.epochSecondChainStart
        val idxDiff = (timeEst - start) / periodExpect
        val idx = min((timesRepeat - 1), idxExpect - idxDiff)
        val zdtIdx = getZoneDateTime(idx)
        val time = zdtIdx.toEpochSecond()
        if (time in start..<finish) return zdtIdx
        if (time >= finish && idx > 1) {
            val zdtIdxM1 = getZoneDateTime(idx - 1)
            val timeM1 = zdtIdxM1.toEpochSecond()
            return if (timeM1 >= finish || timeM1 < start) null
            else zdtIdxM1
        }
        if (time < start && idx < timesRepeat - 1) {
            val zdtIdxP1 = getZoneDateTime(idx + 1)
            val timeP1 = zdtIdxP1.toEpochSecond()
            return if (timeP1 < start || timeP1 >= finish) null
            else zdtIdxP1
        }
        return null
    }

    fun getContents(dbManager: DbManager): String {
        // TODO:
        return "STUB"
    }

//    private fun getZonedDateTimeInRange(start: Long, finish: Long): ZonedDateTime? { //any(?) in range
//        val index = getIndexInRange(start, finish) ?: return null
//        return getZoneDateTime(index)
//    }
//
//    private fun getIndexInRange(start: Long, finish: Long): Long? { //any(?) in range
//        if (finish <= this.epochSecondChainStart || timesRepeat == 0L)
//            return null
//        if (this.epochSecondChainEnd == this.epochSecondChainStart)
//            return if (this.epochSecondChainStart >= start) 0
//            else null
//        val periodExpect = (this.epochSecondChainEnd - this.epochSecondChainStart) / (timesRepeat - 1)
//        val idxExpect = (finish - this.epochSecondChainStart) / periodExpect
//        //mult_frac will overflow with period 1D after a few million years, but who cares about that
//        val timeEst = multFrac(
//            this.epochSecondChainEnd - this.epochSecondChainStart,
//            idxExpect,
//            timesRepeat
//        ) + this.epochSecondChainStart
//        val idxDiff = (timeEst - start) / periodExpect
//        val idx = min((timesRepeat - 1), idxExpect - idxDiff)
//        val zdtIdx = getZoneDateTime(idx)
//        val time = zdtIdx.toEpochSecond()
//        if (time in start..<finish) return idx
//        if (time >= finish && idx > 1) {
//            val zdtIdxM1 = getZoneDateTime(idx - 1)
//            val timeM1 = zdtIdxM1.toEpochSecond()
//            return if (timeM1 !in start..<finish) null
//            else idx - 1
//        }
//        if (time < start && idx < timesRepeat - 1) {
//            val zdtIdxP1 = getZoneDateTime(idx + 1)
//            val timeP1 = zdtIdxP1.toEpochSecond()
//            return if (timeP1 !in start..<finish) null
//            else idx + 1
//        }
//        return null
//    }

    companion object {
        fun getAll(dbManager: DbManager): MutableList<ViewDateWithOptions> =
            getAll(dbManager, 1764432677 + 50 * 86400, 1764432677 + 150 * 86400)

        fun getAll(
            dbManager: DbManager,
            start: Long,
            end: Long,
            tagsFilter: List<Tag> = listOf()
        ): MutableList<ViewDateWithOptions> {
            dbManager.readableDatabase.rawQuery(
                "SELECT Dates.id as dateId, Dates.entryId as entryId, epochSecondChainStart, duration, epochSecondChainEnd, " +
                        "timesRepeat, period, timeZone, exceptionRules, name, color " +
                        "from Dates " +
                        "join EventOptions on EventOptions.id=eventOptionsId " +
                        if (tagsFilter.isEmpty())
                            ""
                        else {
                            "join " +
                                    "(SELECT entryId as eId " +
                                    "FROM EntryTagLinks " +
                                    "WHERE tagId IN (${tagsFilter.joinToString(", ") { it.id!!.toString() }}) " +
                                    "GROUP BY eId " +
                                    "HAVING COUNT(DISTINCT tagId) = ${tagsFilter.size}" +
                                    ") " +
                                    "on eId=Dates.entryId "
                        } +
                        "WHERE epochSecondChainStart < ? AND epochSecondChainEnd >= ?",
                arrayOf(
                    end.toString(),
                    start.toString(),
                )
            ).use { cursor ->
                val views: MutableList<ViewDateWithOptions> = ArrayList()
                val idxId = cursor.getColumnIndex("dateId")
                val idxEntryId = cursor.getColumnIndex("entryId")
                val idxStart = cursor.getColumnIndex("epochSecondChainStart")
                val idxDuration = cursor.getColumnIndex("duration")
                val idxEnd = cursor.getColumnIndex("epochSecondChainEnd")
                val idxRepeat = cursor.getColumnIndex("timesRepeat")
                val idxPeriod = cursor.getColumnIndex("period")
                val idxZone = cursor.getColumnIndex("timeZone")
                val idxExceptions = cursor.getColumnIndex("exceptionRules")
                val idxName = cursor.getColumnIndex("name")
                val idxColor = cursor.getColumnIndex("color")
                while (cursor.moveToNext())
                    views.add(
                        ViewDateWithOptions(
                            cursor.getInt(idxId),
                            cursor.getInt(idxEntryId),
                            cursor.getLong(idxStart),
                            Period.decode(cursor.getString(idxDuration)),
                            cursor.getLong(idxEnd),
                            cursor.getLong(idxRepeat),
                            Period.decode(cursor.getString(idxPeriod)),
                            ZoneId.of(cursor.getString(idxZone)),
                            ExceptionRules(cursor.getString(idxExceptions)),
                            cursor.getString(idxName),
                            cursor.getInt(idxColor).toColor() ?: dbManager.defaultEntryColor
                        )
                    )
                log(views)
                return views
            }
        }

        fun occurrencesForDay(
            dbManager: DbManager,
            day: LocalDate,
            tagsFilter: List<Tag> = listOf()
        ): List<DateOccurrence> {
            val begin = zonedDateTime(day)
            val start = begin.toEpochSecond()
            val end = begin.plusDays(1).toEpochSecond()
            val result: SortedSet<DateOccurrence> = sortedSetOf(compareBy<DateOccurrence> { it.occurrence })
            getAll(
                dbManager,
                start,
                end,
                tagsFilter
            ).forEach { date ->
                date.anyInRange(start, end)?.let { dateOccurrence ->
                    result.add(
                        DateOccurrence(
                            getZonedFromEpochSeconds(dateOccurrence.toEpochSecond()).toLocalDateTime(),
                            dateOccurrence,
                            date
                        )
                    )
                }
            }
            return result.toList()
        }

        fun occurrencesIntersectingDay(
            dbManager: DbManager,
            day: LocalDate,
            tagsFilter: List<Tag> = listOf()
        ): List<DateOccurrence> {
            val begin = zonedDateTime(day)
            val start = begin.toEpochSecond()
            val end = begin.plusDays(1).toEpochSecond()
            val result: SortedSet<DateOccurrence> = sortedSetOf(compareBy<DateOccurrence> { it.occurrence })
            getAll(
                dbManager,
                start,
                end,
                tagsFilter
            ).forEach { date ->
                date.allIntersectingRange(start, end).forEach { dateOccurrence ->
                    result.add(
                        DateOccurrence(
                            getZonedFromEpochSeconds(dateOccurrence.toEpochSecond()).toLocalDateTime(),
                            dateOccurrence,
                            date
                        )
                    )
                }
            }
            return result.toList()
        }
    }
}

data class DateOccurrence(
    val occurrence: LocalDateTime,
    val occurrenceZoned: ZonedDateTime,
    val date: ViewDateWithOptions
)