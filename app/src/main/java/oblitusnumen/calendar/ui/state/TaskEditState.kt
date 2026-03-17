package oblitusnumen.calendar.ui.state

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions

data class TaskEditState(
    val uiIdGenerator: UiIdGenerator,
    private var _taskId: Int?,
    private var _optionsId: Int?,
    val name: TextFieldValue,
    val color: Color,
    val contents: TextFieldValue,
    val tags: List<Tag>,
) {
    val taskId
        get() = _taskId
    val entryId
        get() = _taskId
    val optionsId
        get() = _optionsId

    fun commit(dbManager: DbManager) {
        // TODO:
    }

    companion object {
        fun initial(dbManager: DbManager, taskId: Int?): TaskEditState {
            val task = taskId?.let { ViewTaskWithOptions.byId(dbManager, taskId)!! }
            val uiIdGenerator = UiIdGenerator()

            return TaskEditState(
                uiIdGenerator,
                taskId,
                task?.optionsId,
                TextFieldValue(task?.name ?: ""),
                task?.color ?: dbManager.defaultTaskColor,
                TextFieldValue(task?.getContents(dbManager) ?: ""),
                taskId?.let { Tag.forEntry(dbManager, taskId).sortedBy { it.name } } ?: emptyList())
        }
    }
}