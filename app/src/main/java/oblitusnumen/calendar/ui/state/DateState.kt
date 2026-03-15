package oblitusnumen.calendar.ui.state

import androidx.compose.runtime.Immutable
import oblitusnumen.calendar.implementation.data.ExceptionRules
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Date
import java.time.ZoneId

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
}