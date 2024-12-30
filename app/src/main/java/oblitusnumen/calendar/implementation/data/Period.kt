package oblitusnumen.calendar.implementation.data

import oblitusnumen.calendar.implementation.toWeekNumber
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

sealed class Period private constructor() : Comparable<Period> {
    class Once : Period() {
        override val count: Long = 1
        override fun toString(): String = "O"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start
        override fun secondsApproximation(): Long = 0
        override fun updateCount(count: Long): Period = this
    }

    class Minute(override val count: Long) : Period() {
        init {
            if (count <= 0 || count > MAX_PERIOD_COUNT)
                throw IllegalArgumentException("Invalid period count")
        }
        override fun toString(): String = "m$count"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start.plusMinutes(count * this.count)
        override fun secondsApproximation(): Long = count * 60
        override fun updateCount(count: Long): Period = Minute(count)
    }

    class Hour(override val count: Long) : Period() {
        init {
            if (count <= 0 || count > MAX_PERIOD_COUNT)
                throw IllegalArgumentException("Invalid period count")
        }
        override fun toString(): String = "H$count"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start.plusHours(count * this.count)
        override fun secondsApproximation(): Long = count * 3600
        override fun updateCount(count: Long): Period = Hour(count)
    }

    class Day(override val count: Long) : Period() {
        init {
            if (count <= 0 || count > MAX_PERIOD_COUNT)
                throw IllegalArgumentException("Invalid period count")
        }
        override fun toString(): String = "D$count"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start.plusDays(count * this.count)
        override fun secondsApproximation(): Long = count * 86400
        override fun updateCount(count: Long): Period = Day(count)
    }

    class Week(override val count: Long) : Period() {
        init {
            if (count <= 0 || count > MAX_PERIOD_COUNT)
                throw IllegalArgumentException("Invalid period count")
        }
        override fun toString(): String = "W$count"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start.plusWeeks(count * this.count)
        override fun secondsApproximation(): Long = count * 86400 * 7
        override fun updateCount(count: Long): Period = Week(count)
    }

    class Month(override val count: Long) : Period() {
        init {
            if (count <= 0 || count > MAX_PERIOD_COUNT)
                throw IllegalArgumentException("Invalid period count")
        }
        override fun toString(): String = "M$count"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start.plusMonths(count * this.count)
        override fun secondsApproximation(): Long = count * 86400 * 30
        override fun updateCount(count: Long): Period = Month(count)
    }

    class Year(override val count: Long) : Period() {
        init {
            if (count <= 0 || count > MAX_PERIOD_COUNT)
                throw IllegalArgumentException("Invalid period count")
        }
        override fun toString(): String = "Y$count"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start.plusYears(count * this.count)
        override fun secondsApproximation(): Long = count * 86400 * 365
        override fun updateCount(count: Long): Period = Year(count)
    }

    class Weekday(override val count: Long, val daysMask: Long) : Period() {
        init {
            if (count <= 0 || count > MAX_PERIOD_COUNT || daysMask > WD_ALL)
                throw IllegalArgumentException("Invalid period arguments")
        }
        override fun toString(): String = "w${daysMask.toString().padStart(3, '0')}|$count"
        override fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime = start.plusDays(count) //count is not in periods but days here
        override fun secondsApproximation(): Long = 86400
        override fun updateCount(count: Long): Period = Weekday(count, daysMask)
        fun eventCountPerWeek() = daysMask.countOneBits()
        fun testWeekday(day: Long): Boolean = (daysMask and day) != 0L
        fun testWeekdayIdx(dayIdx: Int): Boolean = (daysMask and dayOfWeekIndexToEnum(dayIdx)) != 0L

        fun verifyWeekday(start: LocalDate, test: LocalDate): Boolean {
            if (!testWeekdayIdx(test.dayOfWeek.value))
                return false
            val firstWeekIdx = start.toWeekNumber()
            val weekIdx = test.toWeekNumber()
            return (weekIdx - firstWeekIdx) % count == 0L
        }

        override fun compareTo(other: Period): Int {
            throw IllegalStateException("Cannot compare weekday")
        }

        companion object {
            const val WD_NONE: Long = 0
            const val WD_MON: Long = 1
            const val WD_TUE: Long = 2
            const val WD_WED: Long = 4
            const val WD_THU: Long = 8
            const val WD_FRI: Long = 16
            const val WD_SAT: Long = 32
            const val WD_SUN: Long = 64
            const val WD_ALL: Long = 127
            fun dayOfWeekIndexToEnum(idx: Int): Long = (1L shl (idx - 1))
        }
    }

    open val count = 1L
    abstract fun addTo(start: ZonedDateTime, count: Long): ZonedDateTime
    abstract fun secondsApproximation(): Long
    abstract fun updateCount(count: Long): Period

    override fun equals(other: Any?): Boolean {
        return other is Period &&
                this.count == other.count &&
                this.secondsApproximation() == other.secondsApproximation() &&
                (other !is Weekday || (this as Weekday).daysMask == other.daysMask)
    }

    override fun hashCode(): Int {
        return Objects.hash(count, secondsApproximation(), if (this is Weekday) daysMask else 0)
    }

    override fun compareTo(other: Period): Int {
        if (other is Weekday)
            throw IllegalArgumentException("Cannot compare to weekday")
        return secondsApproximation().compareTo(other.secondsApproximation())
    }

    companion object{
        const val MAX_PERIOD_COUNT = 4294967296L

        fun decode(string: String): Period {
            return when (string[0]) {
                'O' -> Once()
                'm' -> Minute(string.substring(1).toLong())
                'H' -> Hour(string.substring(1).toLong())
                'D' -> Day(string.substring(1).toLong())
                'W' -> Week(string.substring(1).toLong())
                'M' -> Month(string.substring(1).toLong())
                'Y' -> Year(string.substring(1).toLong())
                'w' -> Weekday(string.substring(5).toLong(), string.substring(1, 4).toLong())
                else-> throw IllegalArgumentException("Invalid period: $string")
            }
        }
    }
}