package oblitusnumen.calendar.implementation.notifications

import java.util.*

class PendingNotification(val dateId: Int, val offset: Long, val eventDateTime: Long) : Comparable<PendingNotification> {
    fun dateHash(): Int {
        return Objects.hash(dateId, eventDateTime)
    }

    fun getNotificationTime(): Long {
        return eventDateTime - offset
    }

    override fun compareTo(other: PendingNotification): Int {
        val diff = getNotificationTime() - other.getNotificationTime()
        return if (diff < 0) -1 else if (diff == 0L) 0 else 1
    }
}