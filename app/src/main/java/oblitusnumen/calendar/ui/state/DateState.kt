package oblitusnumen.calendar.ui.state

import androidx.compose.runtime.Immutable
import oblitusnumen.calendar.implementation.data.ExceptionRules
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Date
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Immutable
data class DateState(
    val uiId: String,
    private var _id: Int? = null,
    private val entryId: Int?,
    private var _eventOptionsId: Int?,
    val epochSecondChainStart: Long,
    val duration: Period,
    var epochSecondChainEnd: Long = 0,
    private var _timesRepeat: Long = 1,
    val period: Period,
    val timeZoneId: ZoneId,
    val exceptionRules: ExceptionRules
) : Comparable<DateState> {
    val id: Int?
        get() = _id
    val eventOptionsId: Int?
        get() = _eventOptionsId
    val timesRepeat: Long
        get() = _timesRepeat

    fun toDbEntity() =
        Date(
            _id,
            _eventOptionsId,
            entryId,
            epochSecondChainStart,
            duration,
            epochSecondChainEnd,
            timesRepeat,
            period,
            timeZoneId,
            exceptionRules
        )

    override fun compareTo(other: DateState): Int = epochSecondChainStart.compareTo(other.epochSecondChainStart)

    // FIXME: static
    val isPeriodic: Boolean = toDbEntity().isPeriodic
    val isEndless: Boolean = toDbEntity().isEndless
    fun getFirstZoneDateTime() = toDbEntity().getFirstZoneDateTime()
    fun getLastZoneDateTime() = toDbEntity().getLastZoneDateTime()
    fun getTimesRepeatUI() = toDbEntity().getTimesRepeatUI()
    fun addExceptions(date: LocalDate) =
        copy(exceptionRules = exceptionRules.apply { addDates(date.toEpochDay(), date.toEpochDay()) })

    fun removeExceptions(date: LocalDate): DateState =
        copy(exceptionRules = exceptionRules.apply { removeDates(date.toEpochDay(), date.toEpochDay()) })

    fun setRange(startOfDayStart: ZonedDateTime? = null, startOfDayEnd: ZonedDateTime? = null): DateState =
        toDbEntity().apply { setRange(startOfDayStart, startOfDayEnd) }.toUiState(uiId)

    fun setDuration(duration: Period): DateState =
        copy(duration = duration)

    fun setPeriod(period: Period): DateState =
        toDbEntity().apply { setPeriod(period) }.toUiState(uiId)

    fun makeEndless(): DateState =
        toDbEntity().apply { makeEndless() }.toUiState(uiId)

    fun setTimesRepeatUI(timesRepeat: Long): DateState =
        toDbEntity().apply { setTimesRepeatUI(timesRepeat) }.toUiState(uiId)
}