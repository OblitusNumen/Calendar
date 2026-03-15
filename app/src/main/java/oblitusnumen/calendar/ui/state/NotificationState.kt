package oblitusnumen.calendar.ui.state

import androidx.compose.runtime.Immutable
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Notification

@Immutable
data class NotificationState(
    val uiId: String,
    private var _eventOptionsId: Int?,
    val offset: Period,
    val sound: Boolean
) :
    Comparable<NotificationState> {
    val eventOptions: Int?
        get() = _eventOptionsId

    fun toDbEntity() = Notification(_eventOptionsId, offset, sound)

    override fun compareTo(other: NotificationState): Int = offset.compareTo(other.offset)
}