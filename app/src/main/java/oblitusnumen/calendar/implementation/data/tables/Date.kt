package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.ExceptionRules
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.multFrac
import oblitusnumen.calendar.implementation.toEpochDays
import oblitusnumen.calendar.implementation.toWeekNumber
import org.jetbrains.annotations.TestOnly
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.min

open class Date : BaseColumns {
    var id: Int? = null
        private set
    var entryId: Int?
        private set
    var eventOptionsId: Int?
        private set
    var epochSecondChainStart: Long
        private set
    var duration: Period
        private set
    protected var epochSecondChainEnd: Long = 0
        private set
    protected var timesRepeat: Long = 1
        private set
    var period: Period
        private set
    var timeZoneId: ZoneId
        private set
    var exceptionRules: ExceptionRules
        private set
    private var entryCache: Entry? = null
    fun getEntry(dbManager: DbManager): Entry {
        if (entryCache == null) entryCache = Entry.byId(dbManager, entryId!!)!!
        return entryCache!!
    }

    constructor(
        entry: Entry,
        time: ZonedDateTime,
        duration: Period,
        timesRepeat: Long,
        period: Period
    ) {
        this.entryId = entry.id
        this.eventOptionsId = entry.defaultOptionsId
        this.epochSecondChainStart = time.toEpochSecond()
        this.duration = duration
        this.period = period
        this.timeZoneId = time.zone
        this.exceptionRules = ExceptionRules()
        setTimesRepeat(timesRepeat)
    }

    constructor(
        id: Int,
        entryId: Int,
        eventOptionsId: Int,
        epochSecondChainStart: Long,
        duration: Period,
        epochSecondChainEnd: Long,
        timesRepeat: Long,
        period: Period,
        timeZoneId: ZoneId,
        exceptionRules: ExceptionRules,
    ) {
        this.id = id
        this.entryId = entryId
        this.eventOptionsId = eventOptionsId
        this.epochSecondChainStart = epochSecondChainStart
        this.duration = duration
        this.epochSecondChainEnd = epochSecondChainEnd
        this.timesRepeat = timesRepeat
        this.period = period
        this.timeZoneId = timeZoneId
        this.exceptionRules = exceptionRules
    }

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId)
        contentValues.put(COLUMN_NAME_EVENT_OPTIONS_ID, eventOptionsId)
        contentValues.put(COLUMN_NAME_EPOCH_SECOND_CHAIN_START, epochSecondChainStart)
        contentValues.put(COLUMN_NAME_DURATION, duration.toString())
        contentValues.put(COLUMN_NAME_EPOCH_SECOND_CHAIN_END, epochSecondChainEnd)
        contentValues.put(COLUMN_NAME_TIMES_REPEATS, timesRepeat)
        contentValues.put(COLUMN_NAME_PERIOD, period.toString())
        contentValues.put(COLUMN_NAME_TIME_ZONE_ID, timeZoneId.toString())
        contentValues.put(COLUMN_NAME_OCCURRENCE_EXCEPTIONS, exceptionRules.toString())
        return contentValues
    }

    //since function is private isCreated check not needed
    fun create(dbManager: DbManager) {
        fixDateRange()
        fixExceptionList()
        if (isEmpty)
            return
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        id = dbManager.writableDatabase.insertWithOnConflict(
            TABLE_NAME,
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_REPLACE
        ).toInt()
    }

    fun update(dbManager: DbManager) {
        fixDateRange()
        fixExceptionList()
        if (isEmpty) {
            delete(dbManager)
            return
        }
        dbManager.writableDatabase.update(
            TABLE_NAME,
            getContentValues(),
            "$COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun delete(dbManager: DbManager) {
        dbManager.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
        id = null
    }

    val isPeriodic: Boolean
        get() = period !is Period.Once

    val isEndless: Boolean
        get() = epochSecondChainEnd > END_ENDLESS_THRESHOLD

    val isEmpty: Boolean
        get() = timesRepeat <= 0

    private fun getZoneDateTime(idx: Long): ZonedDateTime {
        return period.addTo(Instant.ofEpochSecond(epochSecondChainStart).atZone(timeZoneId), idx)
    }

    fun getFirstZoneDateTime(): ZonedDateTime {
        return getZoneDateTime(0)
    }

    fun getLastZoneDateTime(): ZonedDateTime {
        if (isPeriodic)
            return getZoneDateTime(timesRepeat - 1)
        return getZoneDateTime(0)
    }

    fun getNext(start: Long): ZonedDateTime? {
        var date: ZonedDateTime? = null
        var curStart = start
        while (date == null) {
            val nextClosestRaw = getNextClosestRaw(curStart) ?: break
            val epochSecond = nextClosestRaw.toEpochSecond()
            if (exceptionRules.getRangeForDate(nextClosestRaw.toEpochDays()) == null) date = nextClosestRaw
            curStart = epochSecond + 1
        }
        return date
    }

    fun getAllInRange(start: Long, end: Long): List<ZonedDateTime> {
        val dates: MutableList<ZonedDateTime> = ArrayList()
        var curStart = start
        while (true) {
            val nextClosestRaw = getNextClosestRaw(curStart) ?: break
            val epochSecond = nextClosestRaw.toEpochSecond()
            if (epochSecond >= end) break
            if (exceptionRules.getRangeForDate(nextClosestRaw.toEpochDays()) == null) dates.add(nextClosestRaw)
            curStart = epochSecond + 1
        }
        return dates
    }

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

    private fun checkZonedDateTime(zonedDateTime: ZonedDateTime): Boolean =
        !(exceptionRules.containsDate(zonedDateTime.toEpochDays()) ||
                (period is Period.Weekday && !(period as Period.Weekday).verifyWeekday(// FIXME: works wacky for cases when more than one event in range
                    getFirstZoneDateTime().toLocalDate(),
                    zonedDateTime.toLocalDate()
                )))// FIXME: works wacky for cases when more than one event in range

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

    /**
     * @return null if event does not happen during next day from `startOfDay` or time which event takes place at
     */
    fun forDay(startOfDay: ZonedDateTime): ZonedDateTime? {
        return anyInRange(startOfDay.toEpochSecond(), startOfDay.plusDays(1).toEpochSecond())
    }

    // TODO: extract static methods
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

    fun makeEndless() {
        if (period is Period.Once)
            throw IllegalStateException("event without period cannot be endless")
        setTimesRepeat((END_ENDLESS_EXPECT - epochSecondChainStart) / period.secondsApproximation())
    }

    private fun verifyParams(
        start: Long = this.epochSecondChainStart,
        timesRepeat: Long = this.timesRepeat,
        period: Period = this.period
    ) {
        if (timesRepeat < 0)
            throw IllegalArgumentException("timesRepeat must not be negative")
        try {
            if (timesRepeat > Period.MAX_PERIOD_COUNT ||
                period.secondsApproximation() > END_ENDLESS_THRESHOLD ||
                start + (timesRepeat - 1) * period.secondsApproximation() > END_ENDLESS_EXPECT
            )
                throw IllegalArgumentException("end overflow")
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("end overflow")
        }
    }

    fun getTimesRepeatUI(): Long {
        val period = this.period
        if (period is Period.Weekday) {
            val firstDay = getFirstZoneDateTime().toLocalDate()
            val wholePeriods = timesRepeat / (period.count * 7)
            val eventsPerWeek = period.eventCountPerWeek()
            var timesRepeatCumulative = wholePeriods * eventsPerWeek
            var leftoverDays = (timesRepeat % (period.count * 7)).toInt()
            if (leftoverDays == 0) {
                return timesRepeatCumulative
            } else {
                var then = firstDay.plusDays(period.count * 7)
                while (true) {
                    if (period.verifyWeekday(firstDay, then)) {
                        timesRepeatCumulative++
                    }
                    leftoverDays--
                    if (leftoverDays == 0) break
                    then = then.plusDays(1)
                }
                return timesRepeatCumulative
            }
        } else
            return timesRepeat
    }

    fun setTimesRepeatUI(timesRepeat: Long) {
        val period = this.period
        if (period is Period.Weekday) {
            val firstDay = getFirstZoneDateTime().toLocalDate()
            val eventsPerWeek = period.eventCountPerWeek()
            val eventsInFirstWeek =
                (period.daysMask shr (firstDay.dayOfWeek.value - 1)).countOneBits()
            var wholeWeeksCount = timesRepeat / eventsPerWeek * period.count
            var leftoverEvents = (timesRepeat % eventsPerWeek).toInt()
            if (eventsPerWeek == eventsInFirstWeek && leftoverEvents == 0)
                setTimesRepeat((wholeWeeksCount - period.count + 1) * 7 - firstDay.dayOfWeek.value + 1)
            else {
                if (leftoverEvents == 0) {
                    wholeWeeksCount -= period.count
                    leftoverEvents = eventsPerWeek
                }
                var then = firstDay.plusDays(wholeWeeksCount * 7)
                while (true) {
                    while (!period.verifyWeekday(firstDay, then)) then = then.plusDays(1)
                    if (--leftoverEvents == 0) break
                    then = then.plusDays(1)
                }
                setRange(startOfDayEnd = then.atStartOfDay(timeZoneId))
            }
        } else {
            setTimesRepeat(timesRepeat)
        }
    }

    private fun setTimesRepeat(timesRepeat: Long) {
        verifyParams(timesRepeat = timesRepeat)
        this.timesRepeat = timesRepeat
        this.epochSecondChainEnd = getZoneDateTime(timesRepeat - 1).toEpochSecond()
    }

    fun setPeriod(period: Period) {
        if (isEndless) {
            this.period = period
            makeEndless()
            return
        }
        verifyParams(period = period)
        this.period = period
        this.epochSecondChainEnd = getZoneDateTime(timesRepeat - 1).toEpochSecond()
    }

    fun addExceptions(dateStart: LocalDate, dateEnd: LocalDate = dateStart) {
        exceptionRules.addDates(dateStart.toEpochDay(), dateEnd.toEpochDay())
    }

    fun removeExceptions(dateStart: LocalDate, dateEnd: LocalDate = dateStart) {
        exceptionRules.removeDates(dateStart.toEpochDay(), dateEnd.toEpochDay())
    }

    fun setRange(startOfDayStart: ZonedDateTime? = null, startOfDayEnd: ZonedDateTime? = null) {
        if (startOfDayStart != null) {
            timeZoneId = startOfDayStart.zone
            val start = startOfDayStart.toEpochSecond()
            if (isEndless) {
                this.epochSecondChainStart = start
                makeEndless()
            } else {
                verifyParams(start = start)
                val end = getLastZoneDateTime()
                this.epochSecondChainStart = start
                if (period is Period.Once)
                    setTimesRepeat(1)
                else
                    setRange(startOfDayEnd = end)
            }
        }
        if (startOfDayEnd != null) {
            val startOfDayAfterEnd = startOfDayEnd.plusDays(1)
            val end = startOfDayAfterEnd.toEpochSecond() - 1
            var expectedCount = (end - epochSecondChainStart) / period.secondsApproximation() + 1
            verifyParams(timesRepeat = expectedCount)
            while (getZoneDateTime(expectedCount - 1) < startOfDayAfterEnd)
                expectedCount += (end - getZoneDateTime(expectedCount - 1).toEpochSecond()) / period.secondsApproximation() + 1
            while (getZoneDateTime(expectedCount - 1) >= startOfDayAfterEnd)
                expectedCount--
            if (expectedCount < 0)
                expectedCount = 0
            setTimesRepeat(expectedCount)
        }
    }

    @TestOnly
    fun getTimesRepeatForTesting() = timesRepeat

    @TestOnly
    fun getNextClosestForTesting(fromEpochSecond: Long) = getNextClosestRaw(fromEpochSecond)

    @TestOnly
    @Deprecated("For use only in bad testing data")
    fun fixEndForTesting(): Date {
        setTimesRepeat(timesRepeat)
        return this
    }

    @TestOnly
    fun getZDTForTesting(i: Long): ZonedDateTime {
        return getZoneDateTime(i)
    }

    fun fixExceptionList() { //fixme check period intersections
        if (timesRepeat <= 0)
            exceptionRules = ExceptionRules()
        exceptionRules.trimToFitRange(
            getZoneDateTime(0).toEpochDays(),
            getLastZoneDateTime().toEpochDays()
        )
    }

    private fun getNextClosestRaw(fromEpochSecond: Long): ZonedDateTime? {
        if (fromEpochSecond > epochSecondChainEnd || timesRepeat <= 0)
            return null
        if (fromEpochSecond <= epochSecondChainStart)
            return getFirstZoneDateTime()
        if (period is Period.Weekday) {
            val period = this.period as Period.Weekday
            val firstLocal = getFirstZoneDateTime().toLocalDate()
            val weekNumberFirst = firstLocal.toWeekNumber()
            val start = Instant.ofEpochSecond(fromEpochSecond).atZone(timeZoneId)
            val startDay = start.toLocalDate()
            var then = startDay
            val weekNumberStart = then.toWeekNumber()
            val weeksDiff = weekNumberStart - weekNumberFirst
            if (weeksDiff % period.count == 0L) {//chosen start is in valid week
                if (period.verifyWeekday(firstLocal, then)) {//check that day
                    val thatDay = getZoneDateTime(then.toEpochDay() - firstLocal.toEpochDay())
                    if (fromEpochSecond <= thatDay.toEpochSecond()) {
                        return thatDay
                    }
                }
                //check until end of that week
                while (true) {
                    then = then.plusDays(1)
                    if (then.toWeekNumber() != weekNumberStart) break
                    if (period.verifyWeekday(firstLocal, then)) {
                        val validEvent = getZoneDateTime(then.toEpochDay() - firstLocal.toEpochDay())
                        return if (validEvent.toEpochSecond() > epochSecondChainEnd) null else validEvent
                    }
                }
            }
            //set to next valid week
            then = firstLocal.plusWeeks((weeksDiff / period.count + 1) * period.count)
            //set then to start of week
            then = then.minusDays(then.dayOfWeek.value - 1L)
            //check until end of that week
            while (true) {
                if (period.verifyWeekday(firstLocal, then)) {
                    val validEvent = getZoneDateTime(then.toEpochDay() - firstLocal.toEpochDay())
                    return if (validEvent.toEpochSecond() > epochSecondChainEnd) null else validEvent
                }
                then = then.plusDays(1)
            }
        }
        val period = (epochSecondChainEnd - epochSecondChainStart) / (timesRepeat - 1)
        val idx = min((timesRepeat - 1), ((fromEpochSecond - epochSecondChainStart) / period))
        val zoneDateTime = getZoneDateTime(idx)
        val evt = zoneDateTime.toEpochSecond()
        if (evt < fromEpochSecond) {
            val zoneDateTimeP1 = getZoneDateTime(idx + 1)
            val evtNext = zoneDateTimeP1.toEpochSecond()
            return if (evtNext >= fromEpochSecond && idx < timesRepeat) zoneDateTimeP1 else null
        }
        if (idx == 0L) return getFirstZoneDateTime()
        val zoneDateTimeM1 = getZoneDateTime(idx - 1)
        val evtPrev = zoneDateTimeM1.toEpochSecond()
        return if (evtPrev < fromEpochSecond) zoneDateTime else zoneDateTimeM1
    }

    private fun getNextClosestIdxRaw(fromEpochSecond: Long): Long {
        if (fromEpochSecond > epochSecondChainEnd || timesRepeat <= 0)
            return -1
        if (fromEpochSecond <= epochSecondChainStart)
            return 0
        val period = (epochSecondChainEnd - epochSecondChainStart) / (timesRepeat - 1)
        val idx = min((timesRepeat - 1), ((fromEpochSecond - epochSecondChainStart) / period))
        val evt = getZoneDateTime(idx).toEpochSecond()
        if (evt < fromEpochSecond) {
            val evtNext = getZoneDateTime(idx + 1).toEpochSecond()
            return if (evtNext >= fromEpochSecond && idx < timesRepeat) idx + 1 else -1
        }
        if (idx == 0L) return 0
        val evtPrev = getZoneDateTime(idx - 1).toEpochSecond()
        return if (evtPrev < fromEpochSecond) idx else idx - 1
    }

    fun fixDateRange() {
        val epoch0 = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, timeZoneId)
        if (exceptionRules.containsDate(getLastZoneDateTime().toEpochDays())) {
            val endExceptionRange = exceptionRules.getRangeForDate(getLastZoneDateTime().toEpochDays())!!
            val newEnd = epoch0.plusDays(endExceptionRange.start - 1)
            if (newEnd.toEpochSecond() < epochSecondChainStart) {
                timesRepeat = 0
                epochSecondChainEnd = epochSecondChainStart
            } else {
                setRange(startOfDayEnd = newEnd)
            }
        }
        if (exceptionRules.containsDate(getZoneDateTime(0).toEpochDays())) {
            val startExceptionRange = exceptionRules.getRangeForDate(getZoneDateTime(0).toEpochDays())!!
            if (timesRepeat != 0L) {
                val newStartIdx = getNextClosestIdxRaw(epoch0.plusDays(startExceptionRange.end + 1).toEpochSecond())
                if (newStartIdx != -1L)
                    setRange(startOfDayStart = getZoneDateTime(newStartIdx))
                else
                    setRange(startOfDayStart = getLastZoneDateTime())
            }
        }
    }

    companion object {
        const val TABLE_NAME: String = "Dates"

        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_ENTRY_ID: String = "entryId"
        const val COLUMN_NAME_EVENT_OPTIONS_ID: String = "eventOptionsId"
        const val COLUMN_NAME_EPOCH_SECOND_CHAIN_START: String = "epochSecondChainStart"
        const val COLUMN_NAME_DURATION: String = "duration"
        const val COLUMN_NAME_EPOCH_SECOND_CHAIN_END: String = "epochSecondChainEnd"
        const val COLUMN_NAME_TIMES_REPEATS: String = "timesRepeat"
        const val COLUMN_NAME_PERIOD: String = "period"
        const val COLUMN_NAME_TIME_ZONE_ID: String = "timeZoneId"
        const val COLUMN_NAME_OCCURRENCE_EXCEPTIONS: String = "occurrenceExceptions"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"$TABLE_NAME\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_ID\"                        INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    \"$COLUMN_NAME_ENTRY_ID\"                  INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_EVENT_OPTIONS_ID\"          INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_EPOCH_SECOND_CHAIN_START\"  BIGINT  NOT NULL,\n" +
                "    \"$COLUMN_NAME_DURATION\"                  TEXT    NOT NULL,\n" +
                "    \"$COLUMN_NAME_EPOCH_SECOND_CHAIN_END\"    BIGINT  NOT NULL,\n" +
                "    \"$COLUMN_NAME_TIMES_REPEATS\"             INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_PERIOD\"                    TEXT    NOT NULL,\n" +
                "    \"$COLUMN_NAME_TIME_ZONE_ID\"              TEXT    NOT NULL,\n" +
                "    \"$COLUMN_NAME_OCCURRENCE_EXCEPTIONS\"     TEXT    NOT NULL,\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_ENTRY_ID\") REFERENCES \"${Entry.TABLE_NAME}\" (\"${Entry.COLUMN_NAME_ID}\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_EVENT_OPTIONS_ID\") REFERENCES \"${EventOptions.TABLE_NAME}\" (\"${EventOptions.COLUMN_NAME_ID}\")\n" +
                ");"

        const val END_ENDLESS_EXPECT: Long = 281474976710656L //2^48
        const val END_ENDLESS_THRESHOLD: Long = 140737488355328L //2^47

        fun cursorToList(cursor: Cursor): MutableList<Date> {
            val dates: MutableList<Date> = ArrayList()

            val idIdx: Int = cursor.getColumnIndex(COLUMN_NAME_ID)
            val entryIdx: Int = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
            val eventOptionsIdx: Int = cursor.getColumnIndex(COLUMN_NAME_EVENT_OPTIONS_ID)
            val timeStartIdx: Int = cursor.getColumnIndex(COLUMN_NAME_EPOCH_SECOND_CHAIN_START)
            val durationIdx: Int = cursor.getColumnIndex(COLUMN_NAME_DURATION)
            val timeEndsIdx: Int = cursor.getColumnIndex(COLUMN_NAME_EPOCH_SECOND_CHAIN_END)
            val timesRepeatsIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIMES_REPEATS)
            val periodIdx: Int = cursor.getColumnIndex(COLUMN_NAME_PERIOD)
            val timeZoneIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE_ID)
            val removedIdx: Int = cursor.getColumnIndex(COLUMN_NAME_OCCURRENCE_EXCEPTIONS)

            while (cursor.moveToNext())
                dates.add(
                    Date(
                        cursor.getInt(idIdx),
                        cursor.getInt(entryIdx),
                        cursor.getInt(eventOptionsIdx),
                        cursor.getLong(timeStartIdx),
                        Period.decode(cursor.getString(durationIdx)),
                        cursor.getLong(timeEndsIdx),
                        cursor.getLong(timesRepeatsIdx),
                        Period.decode(cursor.getString(periodIdx)),
                        ZoneId.of(cursor.getString(timeZoneIdx)),
                        ExceptionRules(cursor.getString(removedIdx))
                    )
                )
            return dates
        }

        fun byEntryId(dbManager: DbManager, entryId: Int): List<Date> {
            dbManager.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME_ENTRY_ID = ?",
                arrayOf(entryId.toString())
            ).use { cursor ->
                return cursorToList(cursor)
            }
        }

        @TestOnly
        fun newInstance(
            id: Int,
            entryId: Int,
            eventOptionsId: Int,
            epochSecondChainStart: Long,
            duration: Period,
            epochSecondChainEnd: Long,
            timesRepeat: Long,
            period: Period,
            timeZoneId: ZoneId,
            exceptionRules: String,
        ): Date {
            return Date(
                id,
                entryId,
                eventOptionsId,
                epochSecondChainStart,
                duration,
                epochSecondChainEnd,
                timesRepeat,
                period,
                timeZoneId,
                ExceptionRules(exceptionRules)
            )
        }
    }
}