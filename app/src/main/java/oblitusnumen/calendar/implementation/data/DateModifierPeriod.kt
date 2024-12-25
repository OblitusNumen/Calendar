package oblitusnumen.calendar.implementation.data

import java.time.ZonedDateTime

class Period {
    val modifier: Char
    val data: Long

    constructor() {
        modifier = ONCE
        data = 0
    }

    constructor(modifier: Char, data: Long) {
        verify(modifier)
        this.modifier = modifier
        if (data < 0)
            throw IllegalArgumentException("period cannot be negative")
        if (modifier != WEEKDAY) {
            if (data == 0L)
                this.data = 1
            else
                this.data = data
        } else
            this.data = data and WD_ALL
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
            WEEK -> start.plusWeeks(idx * data)
            MONTH -> start.plusMonths(idx * data)
            YEAR -> start.plusYears(idx * data)
            WEEKDAY -> start.plusDays(idx) //todo invalid indexes do exist. should check for them
            else -> start
        }
    }

    private fun verify(modifier: Char) {
        if (modifier != ONCE &&
            modifier != MINUTE &&
            modifier != HOUR &&
            modifier != DAY &&
            modifier != WEEK &&
            modifier != MONTH &&
            modifier != YEAR &&
            modifier != WEEKDAY)
            throw IllegalArgumentException("Bad modifier $modifier")
    }

    fun secondsApproximation(): Long {
        return when (modifier) {
            ONCE -> 0
            MINUTE -> 60 * data
            HOUR -> 3600 * data
            DAY, WEEKDAY -> 86400 * data
            WEEK -> 86400 * 7 * data
            MONTH -> 86400 * 30 * data
            YEAR -> 86400 * 365 * data
            else -> 0
        }
    }

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
    }
}
