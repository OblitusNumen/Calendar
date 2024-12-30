package oblitusnumen.calendar.implementation.data

import oblitusnumen.calendar.implementation.toWeekNumber
import java.time.LocalDate
import java.time.ZonedDateTime

class Period {
    val modifier: Char
    private val data: Long

    constructor() {
        modifier = ONCE
        data = 1
    }

    @JvmOverloads
    constructor(modifier: Char, count: Long, weekdayDays: Long = 0) {
        verify(modifier)
        this.modifier = modifier
        if (count <= 0 || count > MAX_PERIOD_COUNT)
            throw IllegalArgumentException("Invalid period")
        if (modifier == WEEKDAY && weekdayDays == 0L)
            throw IllegalArgumentException("Weekdays cannot be empty")
        this.data = count or ((weekdayDays and WD_ALL) shl 32)
    }

    constructor(period: String) {
        verify(period[0])
        this.modifier = period[0]
        data = period.substring(1).toLong()
    }

    override fun toString(): String {
        return "$modifier$data"
    }

    fun getTime(start: ZonedDateTime, idx: Long): ZonedDateTime {
        return when (modifier) {
            ONCE -> start
            MINUTE -> start.plusMinutes(idx * data)
            HOUR -> start.plusHours(idx * data)
            DAY -> start.plusDays(idx * data)
            WEEKDAY -> start.plusDays(idx)
            WEEK -> start.plusWeeks(idx * data)
            MONTH -> start.plusMonths(idx * data)
            YEAR -> start.plusYears(idx * data)
            else -> start
        }
    }

    fun verifyWeekday(start: LocalDate, test: LocalDate): Boolean {
        if (modifier != WEEKDAY)
            throw IllegalStateException("Not a weekday")
        if (!testWeekdayIdx(test.dayOfWeek.value))
            return false
        val firstWeekIdx = start.toWeekNumber()
        val weekIdx = test.toWeekNumber()
        return (weekIdx - firstWeekIdx) % getCount() == 0L
    }

    private fun verify(modifier: Char) {
        if (modifier != ONCE &&
            modifier != MINUTE &&
            modifier != HOUR &&
            modifier != DAY &&
            modifier != WEEK &&
            modifier != MONTH &&
            modifier != YEAR &&
            modifier != WEEKDAY
        )
            throw IllegalArgumentException("Bad modifier $modifier")
    }

    fun secondsApproximation(): Long {
        return when (modifier) {
            ONCE -> 0
            MINUTE -> 60 * data
            HOUR -> 3600 * data
            DAY -> 86400 * data
            WEEKDAY -> 86400
            WEEK -> 86400 * 7 * data
            MONTH -> 86400 * 30 * data
            YEAR -> 86400 * 365 * data
            else -> 0
        }
    }

    fun getCount(): Long {
        return data and 0xFFFFFFFFL
    }

    fun getWeekdays(): Long {
        if (modifier != WEEKDAY)
            throw IllegalArgumentException("Not a weekday")
        return (data shr 32) and WD_ALL
    }

    fun testWeekday(day: Long): Boolean {
        if (modifier != WEEKDAY)
            throw IllegalArgumentException("Not a weekday")
        return (getWeekdays() and day) != 0L
    }

    fun testWeekdayIdx(dayIdx: Int): Boolean {
        if (modifier != WEEKDAY)
            throw IllegalArgumentException("Not a weekday")
        return (getWeekdays() and dayOfWeekIndexToEnum(dayIdx)) != 0L
    }

    fun getEventsPerWeek() = getWeekdays().countOneBits()

    @Suppress("unused")
    companion object {
        const val ONCE = 'O'
        const val MINUTE = 'm'
        const val HOUR = 'H'
        const val DAY = 'D'
        const val WEEK = 'W'
        const val MONTH = 'M'
        const val YEAR = 'Y'
        const val WEEKDAY = 'w'
        const val WD_NONE: Long = 0
        const val WD_MON: Long = 1
        const val WD_TUE: Long = 2
        const val WD_WED: Long = 4
        const val WD_THU: Long = 8
        const val WD_FRI: Long = 16
        const val WD_SAT: Long = 32
        const val WD_SUN: Long = 64
        const val WD_ALL: Long = 127
        const val MAX_PERIOD_COUNT = 4294967296L // 2^32, a little more than END_ENDLESS_EXPECT / 1day

        fun dayOfWeekIndexToEnum(idx: Int): Long = (1L shl (idx - 1))
    }
}
