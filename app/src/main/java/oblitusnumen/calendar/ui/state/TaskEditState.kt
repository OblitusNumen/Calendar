package oblitusnumen.calendar.ui.state

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.EventOptions
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class TaskEditState(
    val uiIdGenerator: UiIdGenerator,
    private var _taskId: Int?,
    private var _optionsId: Int?,
    val name: TextFieldValue,
    val color: Color,
    val startConstraintTimestamp: Long? = null,
    val deadlineTimestamp: Long,
    val timeZoneId: ZoneId,
    val timeConsumed: Int = 0,
    val timeRemaining: Int = 0,
    val contents: TextFieldValue,
    val tags: List<Tag>,
    val predecessors: List<Int>,
    val successors: List<Int>
) {
    val taskId
        get() = _taskId
    val entryId
        get() = _taskId
    val optionsId
        get() = _optionsId
    val options
        get() = EventOptions(optionsId, name = name.text, color = color, contents = contents.text)

    val hasStartConstraint: Boolean
        get() = startConstraintTimestamp != null

    fun commit(dbManager: DbManager) {
        // TODO:
    }

    fun withTimeZone(timeZoneId: ZoneId): TaskEditState {
        val startTimestamp = if (startConstraintTimestamp == null)
            null
        else
            Instant.ofEpochSecond(startConstraintTimestamp).atZone(this.timeZoneId).toLocalDateTime().atZone(timeZoneId)
                .toEpochSecond()
        val deadline =
            Instant.ofEpochSecond(deadlineTimestamp).atZone(this.timeZoneId).toLocalDateTime().atZone(timeZoneId)
                .toEpochSecond()
        return copy(startConstraintTimestamp = startTimestamp, deadlineTimestamp = deadline, timeZoneId = timeZoneId)
    }

    companion object {
        fun initial(dbManager: DbManager, taskId: Int?): TaskEditState {
            val task = taskId?.let { ViewTaskWithOptions.byId(dbManager, taskId)!! }
            val uiIdGenerator = UiIdGenerator()
            val predecessors = taskId?.let { TaskLink.predecessors(dbManager, taskId) } ?: listOf()
            val successors = taskId?.let { TaskLink.successors(dbManager, taskId) } ?: listOf()

            return TaskEditState(
                uiIdGenerator,
                taskId,
                task?.optionsId,
                TextFieldValue(task?.name ?: ""),
                task?.color ?: dbManager.defaultTaskColor,
                task?.startConstraintTimestamp,
                task?.deadlineTimestamp ?: ZonedDateTime.now().toEpochSecond(),
                task?.timeZoneId ?: defaultZoneId(),
                task?.timeConsumed ?: 0,
                task?.timeRemaining ?: 0,
                TextFieldValue(task?.getContents(dbManager) ?: ""),
                taskId?.let { Tag.forEntry(dbManager, taskId).sortedBy { it.name } } ?: emptyList(),
                predecessors,
                successors)
        }
    }
}