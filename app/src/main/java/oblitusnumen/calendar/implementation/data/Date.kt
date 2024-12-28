package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import org.jetbrains.annotations.TestOnly
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.ArrayList
import kotlin.math.min

class Date : BaseColumns {
    private val dbManager: DbManager?
    var id: Int? = null
        private set
    internal var entryId: Int?
    private var desc: String
    var start: Long
        private set
    private var duration: Long
    private var end: Long = 0
    var timesRepeat: Long = 1
        private set
    var period: Period
        private set
    var zoneId: ZoneId
        private set
    var exceptionRules: ExceptionRules
        private set

    val entry: Entry
        get() = dbManager!!.getEntryById(entryId!!)!!

    internal constructor(
        dbManager: DbManager?,
        id: Int,
        entryId: Int,
        desc: String,
        start: Long,
        duration: Long,
        end: Long,
        timesRepeat: Long,
        period: String,
        zoneId: String?,
        removed: String
    ) {
        this.dbManager = dbManager
        this.id = id
        this.entryId = entryId
        this.desc = desc
        this.start = start
        this.duration = duration
        this.end = end
        this.timesRepeat = timesRepeat
        this.period = Period(period)
        this.zoneId = ZoneId.of(zoneId)
        this.exceptionRules = ExceptionRules(removed)
    }

    constructor(
        dbManager: DbManager,
        entry: Entry,
        desc: String,
        time: ZonedDateTime,
        duration: Long,
        timesRepeat: Long,
        period: Period
    ) {
        this.dbManager = dbManager
        this.entryId = entry.id
        this.desc = desc
        this.start = time.toEpochSecond()
        this.duration = duration
        this.period = period
        this.zoneId = time.zone
        this.exceptionRules = ExceptionRules()
        setTimesRepeat(timesRepeat)
    }

    fun create() {
        fixDateRange()
        fixExceptionList()
        if (isEmpty)
            return
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        id = dbManager!!.writableDatabase.insertWithOnConflict(
            TABLE_NAME,
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_REPLACE
        ).toInt()
    }

    fun update() {
        fixDateRange()
        fixExceptionList()
        if (isEmpty) {
            delete()
            return
        }
        dbManager!!.writableDatabase.update(
            TABLE_NAME,
            getContentValues(),
            "$COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun delete() {
        dbManager!!.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
        id = null
    }

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId)
        contentValues.put(COLUMN_NAME_DESC, desc)
        contentValues.put(COLUMN_NAME_TIME_START, start)
        contentValues.put(COLUMN_NAME_DURATION, duration)
        contentValues.put(COLUMN_NAME_TIME_ENDS, end)
        contentValues.put(COLUMN_NAME_TIMES_REPEATS, timesRepeat)
        contentValues.put(COLUMN_NAME_PERIOD, period.toString())
        contentValues.put(COLUMN_NAME_TIME_ZONE, zoneId.toString())
        contentValues.put(COLUMN_NAME_REMOVED, exceptionRules.toString())
        return contentValues
    }

    val isPeriodic: Boolean
        get() = period.modifier != Period.ONCE

    val isEndless: Boolean
        get() = end > END_ENDLESS_THRESHOLD

    val isEmpty: Boolean
        get() = timesRepeat <= 0

    private fun getZoneDateTime(idx: Long): ZonedDateTime {
        return period.getTime(Instant.ofEpochSecond(start).atZone(zoneId), idx)
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
        val dates: MutableList<ZonedDateTime> = ArrayList<ZonedDateTime>()
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

    @TestOnly
    fun getZonedDateTimeIndex(start: Long, finish: Long): Long { //any(?) in range. still used by tests
        if (finish <= this.start) return -1
        if (this.end == this.start) return if (this.start >= start) 0 else -1
        val period = (this.end - this.start) / timesRepeat
        val idx = min((timesRepeat - 1), ((finish - this.start) / period))
        val time = getZoneDateTime(idx).toEpochSecond()
        if (time in start..<finish) return idx
        if (time >= finish && idx > 1) {
            val timeM1 = getZoneDateTime(idx - 1).toEpochSecond()
            return if (timeM1 >= finish || timeM1 < start) -1
            else idx - 1
        }
        if (time < start && idx < timesRepeat - 1) {
            val timeP1 = getZoneDateTime(idx + 1).toEpochSecond()
            return if (timeP1 < start || timeP1 >= finish) -1
            else idx + 1
        }
        return -1
    }

    private fun getZonedDateTimeInRange(start: Long, finish: Long): ZonedDateTime? { //any(?) in range
        if (finish <= this.start) return null
        if (this.end == this.start) return if (this.start >= start) getZoneDateTime(0) else null
        val period = (this.end - this.start) / timesRepeat
        val idx = min((timesRepeat - 1), ((finish - this.start) / period))
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

    fun anyInRange(start: Long, finish: Long): ZonedDateTime? {
        val zonedDateTime = getZonedDateTimeInRange(start, finish)
        if (zonedDateTime == null || exceptionRules.containsDate(zonedDateTime.toEpochDays())) return null
        return zonedDateTime
    }

    private fun getTime(idx: Long): Long {
        return getZoneDateTime(idx).toEpochSecond()
    }

    fun getDesc(): String {
        return desc.ifEmpty { entry.name }
    }

    fun exists(): Boolean {
        return id != null
    }

    fun setDesc(desc: String) {
        this.desc = desc
    }

    fun makeEndless() {
        if (period.modifier == Period.ONCE)
            throw IllegalStateException("event without period cannot be endless")
        setTimesRepeat((END_ENDLESS_EXPECT - start) / period.secondsApproximation())
    }

    private fun ZonedDateTime.toEpochDays(): Long {
        return toLocalDate().toEpochDay()
    }

    private fun verifyParams(start: Long = this.start, timesRepeat: Long = this.timesRepeat, period: Period = this.period) {
        if (timesRepeat < 0)
            throw IllegalArgumentException("timesRepeat must not be negative")
        try {
            if (timesRepeat > Period.MAX_PERIOD_COUNT ||
                period.secondsApproximation() > END_ENDLESS_THRESHOLD ||
                start + (timesRepeat - 1) * period.secondsApproximation() > END_ENDLESS_EXPECT
            )
                throw IllegalArgumentException("end overflow")
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("end overflow")
        }
    }

    fun setTimesRepeat(timesRepeat: Long) {
        verifyParams(timesRepeat = timesRepeat)
        this.timesRepeat = timesRepeat
        this.end = getTime(timesRepeat - 1)
    }

    fun setPeriod(period: Period) {
        if (isEndless) {
            this.period = period
            makeEndless()
            return
        }
        verifyParams(period = period)
        this.period = period
        this.end = getTime(timesRepeat - 1)
    }

    fun addExceptions(dateStart: LocalDate, dateEnd: LocalDate = dateStart) {
        exceptionRules.addDates(dateStart.toEpochDay(), dateEnd.toEpochDay())
    }

    fun removeExceptions(dateStart: LocalDate, dateEnd: LocalDate = dateStart) {
        exceptionRules.removeDates(dateStart.toEpochDay(), dateEnd.toEpochDay())
    }

    fun setRange(startOfDayStart: ZonedDateTime? = null, startOfDayEnd: ZonedDateTime? = null) {
        if (startOfDayStart != null) {
            zoneId = startOfDayStart.zone
            val start = startOfDayStart.toEpochSecond()
            verifyParams(start = start)
            if (isEndless) {
                this.start = start
                makeEndless()
            } else {
                val end = getLastZoneDateTime()
                this.start = start
                if (period.modifier == Period.ONCE)
                    setTimesRepeat(1)
                else
                    setRange(startOfDayEnd = end)
            }
        }
        if (startOfDayEnd != null) {
            val end = startOfDayEnd.plusDays(1).toEpochSecond() - 1
            var expectedCount = (end - start) / period.secondsApproximation() + 1
            verifyParams(timesRepeat = expectedCount)
            while (getZoneDateTime(expectedCount) < startOfDayEnd.plusDays(1))
                expectedCount += (end - getZoneDateTime(expectedCount).toEpochSecond()) / period.secondsApproximation() + 1
            if (getZoneDateTime(expectedCount - 1) > startOfDayEnd.plusDays(1))
                expectedCount--
            if (expectedCount < 0)
                expectedCount = 0
            setTimesRepeat(expectedCount)
        }
    }

    @TestOnly
    @Deprecated("For use only in bad testing data")
    fun fixEnd(): Date {
        setTimesRepeat(timesRepeat)
        return this
    }

    @TestOnly
    fun getZDT(i: Long): ZonedDateTime {
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

    private fun getNextClosestRaw(time: Long): ZonedDateTime? {
        if (time > end || timesRepeat <= 0)
            return null
        if (time <= start)
            return getFirstZoneDateTime()
        val period = (end - start) / (timesRepeat - 1)
        val idx = min((timesRepeat - 1), ((time - start) / period))
        val zoneDateTime = getZoneDateTime(idx)
        val evt = zoneDateTime.toEpochSecond()
        if (evt < time) {
            val zoneDateTimeP1 = getZoneDateTime(idx + 1)
            val evtNext = zoneDateTimeP1.toEpochSecond()
            return if (evtNext >= time && idx < timesRepeat) zoneDateTimeP1 else null
        }
        if (idx == 0L) return getFirstZoneDateTime()
        val zoneDateTimeM1 = getZoneDateTime(idx - 1)
        val evtPrev = zoneDateTimeM1.toEpochSecond()
        return if (evtPrev < time) zoneDateTime else zoneDateTimeM1
    }

    private fun getNextClosestIdxRaw(time: Long): Long {
        if (time > end || timesRepeat <= 0)
            return -1
        if (time <= start)
            return 0
        val period = (end - start) / (timesRepeat - 1)
        val idx = min((timesRepeat - 1), ((time - start) / period))
        val evt = getZoneDateTime(idx).toEpochSecond()
        if (evt < time) {
            val evtNext = getZoneDateTime(idx + 1).toEpochSecond()
            return if (evtNext >= time && idx < timesRepeat) idx + 1 else -1
        }
        if (idx == 0L) return 0
        val evtPrev = getZoneDateTime(idx - 1).toEpochSecond()
        return if (evtPrev < time) idx else idx - 1
    }

    fun fixDateRange() {
        val epoch0 = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, zoneId)
        if (exceptionRules.containsDate(getLastZoneDateTime().toEpochDays())) {
            val endExceptionRange = exceptionRules.getRangeForDate(getLastZoneDateTime().toEpochDays())!!
            val newEnd = epoch0.plusDays(endExceptionRange.start - 1)
            if (newEnd.toEpochSecond() < start) {
                timesRepeat = 0
                end = start
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
        const val TABLE_NAME: String = "dates"
        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_ENTRY_ID: String = "entryId"
        const val COLUMN_NAME_DESC: String = "description"
        const val COLUMN_NAME_TIME_START: String = "timeStart"
        const val COLUMN_NAME_DURATION: String = "duration"
        const val COLUMN_NAME_TIME_ENDS: String = "timeEnd"
        const val COLUMN_NAME_TIMES_REPEATS: String = "timesRepeat"
        const val COLUMN_NAME_PERIOD: String = "period"
        const val COLUMN_NAME_TIME_ZONE: String = "timeZone"
        const val COLUMN_NAME_REMOVED: String = "exceptionRules"
        const val END_ENDLESS_EXPECT: Long = 281474976710656L //2^48
        const val END_ENDLESS_THRESHOLD: Long = 140737488355328L //2^47

        fun cursorToList(
            dbManager: DbManager,
            cursor: Cursor
        ): MutableList<Date> {
            val dates: MutableList<Date> = ArrayList()
            val idIdx: Int = cursor.getColumnIndex(COLUMN_NAME_ID)
            val entryIdx: Int = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
            val descIdx: Int = cursor.getColumnIndex(COLUMN_NAME_DESC)
            val timeStartIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIME_START)
            val durationIdx: Int = cursor.getColumnIndex(COLUMN_NAME_DURATION)
            val timeEndsIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIME_ENDS)
            val timesRepeatsIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIMES_REPEATS)
            val periodIdx: Int = cursor.getColumnIndex(COLUMN_NAME_PERIOD)
            val timeZoneIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE)
            val removedIdx: Int = cursor.getColumnIndex(COLUMN_NAME_REMOVED)
            while (cursor.moveToNext())
                dates.add(
                    Date(
                        dbManager,
                        cursor.getInt(idIdx),
                        cursor.getInt(entryIdx),
                        cursor.getString(descIdx),
                        cursor.getLong(timeStartIdx),
                        cursor.getLong(durationIdx),
                        cursor.getLong(timeEndsIdx),
                        cursor.getLong(timesRepeatsIdx),
                        cursor.getString(periodIdx),
                        cursor.getString(timeZoneIdx),
                        cursor.getString(removedIdx)
                    )
                )
            return dates
        }

        fun getAllByEntryId(dbManager: DbManager, entryId: Int): List<Date> {
            dbManager.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME_ENTRY_ID = ?",
                arrayOf(entryId.toString())
            ).use { cursor ->
                return cursorToList(dbManager, cursor)
            }
        }
    }
}