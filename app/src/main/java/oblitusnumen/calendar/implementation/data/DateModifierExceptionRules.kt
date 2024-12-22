package oblitusnumen.calendar.implementation.data

import kotlin.math.max
import kotlin.math.min

class ExceptionRules internal constructor(serialized: String = "") { // all operations include both start and end
    private val exceptions: MutableList<ExceptionRange> =
        mutableListOf() //there are not too many exceptions so loop in add/delete is fine

    init {
        if (serialized.isNotEmpty())
            for (s in serialized.split(',')) {
                val split = s.split('_')
                if (split.isEmpty() || split.size > 2)
                    throw IllegalArgumentException("Bad serialized string")
                exceptions.add(
                    ExceptionRange(
                        split[0].toLong(),
                        if (split.size == 2) split[1].toLong() else split[0].toLong()
                    )
                )
            }
    }

    fun addDate(day: Long) {
        addDates(day, day)
    }

    fun addDates(start: Long, end: Long) {
        if (start > end)
            throw IllegalArgumentException("start > end")
        var startIndex = -1
        do {
            startIndex++
        } while (startIndex < exceptions.size && exceptions[startIndex].end < start - 1) //off by one?
        if (startIndex == exceptions.size) {
            exceptions.add(ExceptionRange(start, end))
            return
        }
        if (exceptions[startIndex].start > end + 1) {
            exceptions.add(startIndex, ExceptionRange(start, end))
            return
        }
        val newStart = min(exceptions[startIndex].start, start)
        var newEnd = max(exceptions[startIndex].end, end)
        while (startIndex + 1 < exceptions.size && newEnd + 1 >= exceptions[startIndex + 1].start) {
            newEnd = max(exceptions[startIndex + 1].end, newEnd)
            exceptions.removeAt(startIndex + 1)
        }
        exceptions[startIndex] = ExceptionRange(newStart, newEnd)
    }

    fun removeDate(day: Long) {
        removeDates(day, day)
    }

    fun removeDates(start: Long, end: Long) {
        if (start > end)
            throw IllegalArgumentException("start > end")
        var startIndex = -1
        do {
            startIndex++
        } while (startIndex < exceptions.size && exceptions[startIndex].end < start)
        if (startIndex == exceptions.size)
            return
        if (exceptions[startIndex].start < start && exceptions[startIndex].end > end) {
            val prevEnd = exceptions[startIndex].end
            exceptions[startIndex] = ExceptionRange(exceptions[startIndex].start, start - 1)
            exceptions.add(startIndex + 1, ExceptionRange(end + 1, prevEnd))
        }
        while (startIndex < exceptions.size && exceptions[startIndex].start <= end) {
            if (exceptions[startIndex].start >= start && exceptions[startIndex].end <= end) {
                exceptions.removeAt(startIndex)
                continue
            }
            if (exceptions[startIndex].start < start) {
                exceptions[startIndex] = ExceptionRange(exceptions[startIndex].start, start - 1)
                startIndex++
                continue
            }
            exceptions[startIndex] = ExceptionRange(end + 1, exceptions[startIndex].end)
            break
        }
    }

    fun containsDate(day: Long): Boolean {
        /*if (exceptions.isEmpty())
            return false*/
        //val index = findIndex(day) //for faster work
        //return exceptions[index].start <= day && exceptions[index].end >= day
        return getRangeForDate(day) != null
    }

    fun trimToFitRange(start: Long, end: Long) {
        removeDates(Long.MIN_VALUE, start - 1)
        removeDates(end + 1, Long.MAX_VALUE)
    }

    fun getRangeForDate(day: Long): ExceptionRange? {
        for (e in exceptions) {
            if (e.end >= day && e.start <= day)
                return e
            if (e.start > day)
                return null
        }
        return null
    }

    override fun toString(): String {
        val result = StringBuilder()
        for (exception in exceptions) {
            if (result.isNotEmpty())
                result.append(',')
            result.append(exception.start)
            if (exception.start != exception.end)
                result.append('_').append(exception.end)
        }
        return result.toString()
    }

    fun exceptionDatesCount(): Long {
        var count = 0L
        for (exception in exceptions)
            count += (exception.end - exception.start + 1)
        return count
    }

    fun listAll(): List<Long> {
        val list = mutableListOf<Long>()
        for (e in exceptions)
            for (i in e.start..e.end)
                list.add(i)
        return list
    }

    /*private fun findIndex(day: Long): Int { //todo bad, but is it actually needed?
        var begin = 0
        var end = exceptions.size
        while (true) { //search algorithm
            val center = (begin + end) / 2
            if (exceptions[center].start >= day) {
                if (center == end) return center
                end = center
            } else {
                if (begin == center) return end
                begin = center
            }
        }
    }*/

    data class ExceptionRange(val start: Long, val end: Long)
}
