package oblitusnumen.calendar.implementation.notifications

import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import java.util.Objects

class PendingDeadlineNotification(
    val task: ViewTaskWithOptions,
    val offset: Period,
    val sound: Boolean,
    val notificationDateTime: Long,
) {
    val deadline: Long
        get() = task.deadlineTimestamp

    fun dateHash(): Int = Objects.hash("deadline", task.taskId, offset.toString(), deadline)
}
