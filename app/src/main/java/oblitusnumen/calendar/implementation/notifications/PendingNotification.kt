package oblitusnumen.calendar.implementation.notifications

import oblitusnumen.calendar.implementation.data.views.ViewNotificationDateWithOptions
import java.util.*

class PendingNotification(
    val notification: ViewNotificationDateWithOptions,
    private val notificationDateTime: Long,
    val eventDateTime: Long
) : Comparable<PendingNotification> {
    val id
        get() = notification.id!!

    fun dateHash(): Int {
        return Objects.hash(id, eventDateTime)
    }

    override fun compareTo(other: PendingNotification): Int {
        val diff = notificationDateTime - other.notificationDateTime
        return if (diff < 0) -1 else if (diff == 0L) 0 else 1
    }
}