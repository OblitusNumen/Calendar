package oblitusnumen.calendar.implementation.data.views

import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.ExceptionRules
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.EntryTagLinks
import oblitusnumen.calendar.implementation.data.tables.EventOptions
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.zonedDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class ViewDateWithOptions(
    id: Int,
    entryId: Int,
    eventOptionsId: Int,
    epochSecondChainStart: Long,
    duration: Period,// FIXME: fixup in db in date
    epochSecondChainEnd: Long,// FIXME: account for duration in db in date
    timesRepeat: Long,
    period: Period,
    timeZoneId: ZoneId,
    exceptionRules: ExceptionRules,
    val name: String,
    val color: Color
) : Date(
    id,
    entryId,
    eventOptionsId,
    epochSecondChainStart,
    duration,
    epochSecondChainEnd,
    timesRepeat,
    period,
    timeZoneId,
    exceptionRules
) {
    val displayName: String = name.ifEmpty { "[No title]" } // FIXME:

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
        // FIXME: for debug
        fun all(dbManager: DbManager): MutableList<ViewDateWithOptions> =
            all(dbManager, 1764432677 + 50 * 86400, 1764432677 + 150 * 86400)

        fun all(
            dbManager: DbManager,
            start: Long,
            end: Long,
            tagsFilter: List<Int> = listOf()
        ): MutableList<ViewDateWithOptions> {
            dbManager.readableDatabase.rawQuery(
                "SELECT ${TABLE_NAME}.*, " +
                        "${EventOptions.COLUMN_NAME_NAME} as name, ${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from ${TABLE_NAME} " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}=${COLUMN_NAME_EVENT_OPTIONS_ID} " +
                        if (tagsFilter.isEmpty())
                            ""
                        else {
                            "join " +
                                    "(SELECT ${EntryTagLinks.COLUMN_NAME_ENTRY_ID} as eId " +
                                    "FROM ${EntryTagLinks.TABLE_NAME} " +
                                    "WHERE ${EntryTagLinks.COLUMN_NAME_TAG_ID} " +
                                    "IN (${tagsFilter.joinToString(", ") { it.toString() }}) " +
                                    "GROUP BY eId " +
                                    "HAVING COUNT(DISTINCT ${EntryTagLinks.COLUMN_NAME_TAG_ID}) = ${tagsFilter.size}" +
                                    ") " +
                                    "on eId=${COLUMN_NAME_ENTRY_ID} " // FIXME: might as well be needed  ${Date.TABLE_NAME}.${Date.COLUMN_NAME_ENTRY_ID}
                        } +
                        "WHERE ${COLUMN_NAME_EPOCH_SECOND_CHAIN_START} < ? AND ${COLUMN_NAME_EPOCH_SECOND_CHAIN_END} >= ?",
                arrayOf(
                    end.toString(),
                    start.toString(),
                )
            ).use { cursor ->
                val views: MutableList<ViewDateWithOptions> = ArrayList()

                val idxId = cursor.getColumnIndex(COLUMN_NAME_ID)
                val idxEntryId = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
                val idxOptionsId = cursor.getColumnIndex(COLUMN_NAME_EVENT_OPTIONS_ID)
                val idxStart = cursor.getColumnIndex(COLUMN_NAME_EPOCH_SECOND_CHAIN_START)
                val idxDuration = cursor.getColumnIndex(COLUMN_NAME_DURATION)
                val idxEnd = cursor.getColumnIndex(COLUMN_NAME_EPOCH_SECOND_CHAIN_END)
                val idxRepeat = cursor.getColumnIndex(COLUMN_NAME_TIMES_REPEATS)
                val idxPeriod = cursor.getColumnIndex(COLUMN_NAME_PERIOD)
                val idxZone = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE_ID)
                val idxExceptions = cursor.getColumnIndex(COLUMN_NAME_OCCURRENCE_EXCEPTIONS)
                val idxName = cursor.getColumnIndex("name")
                val idxColor = cursor.getColumnIndex("color")

                while (cursor.moveToNext())
                    views.add(
                        ViewDateWithOptions(
                            cursor.getInt(idxId),
                            cursor.getInt(idxEntryId),
                            cursor.getInt(idxOptionsId),
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
            tagsFilter: List<Int> = listOf()
        ): List<DateOccurrence> {
            val begin = zonedDateTime(day)

            val start = begin.toEpochSecond()
            val end = begin.plusDays(1).toEpochSecond()

            val result: SortedSet<DateOccurrence> =
                sortedSetOf(compareBy<DateOccurrence> { it.occurrence }.thenBy { it.date.name }.thenBy { it.date.id })

            all(
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
            tagsFilter: List<Int> = listOf()
        ): List<DateOccurrence> {
            val begin = zonedDateTime(day)

            val start = begin.toEpochSecond()
            val end = begin.plusDays(1).toEpochSecond()

            val result: SortedSet<DateOccurrence> =
                sortedSetOf(compareBy<DateOccurrence> { it.occurrence }.thenBy { it.date.name }.thenBy { it.date.id })

            all(
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