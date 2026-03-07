package oblitusnumen.calendar.implementation.notifications

import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Notification
import java.util.*

class PendingNotification(
    val date: Date,
    val notification: Notification,
    private val notificationDateTime: Long,
    val eventDateTime: Long
) : Comparable<PendingNotification> {
    fun dateHash(): Int {
        return Objects.hash(date.id, eventDateTime)
    }

    override fun compareTo(other: PendingNotification): Int {
        val diff = notificationDateTime - other.notificationDateTime
        return if (diff < 0) -1 else if (diff == 0L) 0 else 1
    }
}