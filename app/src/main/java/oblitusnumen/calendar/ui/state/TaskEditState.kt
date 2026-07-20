package oblitusnumen.calendar.ui.state

import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import oblitusnumen.calendar.implementation.checkRecursiveLinks
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.*
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.initLinks
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
    val timeRemaining: Int = 1, // FIXME: non-zero; might be better if configured in settings
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
    val entry
        get() = Entry(_taskId, _optionsId, isTask = true)
    val task
        get() = Task(_taskId, startConstraintTimestamp, deadlineTimestamp, timeZoneId, timeConsumed, timeRemaining)

    val hasStartConstraint: Boolean
        get() = startConstraintTimestamp != null

    fun validate(dbManager: DbManager): TaskEditValidationError? {
        if (startConstraintTimestamp != null && startConstraintTimestamp >= deadlineTimestamp)
            return TaskEditValidationError.StartAfterDeadline

        val dbTasks = Task.all(dbManager)
        val idToIndex = HashMap<Int, Int>(dbTasks.size + 1)
        dbTasks.forEachIndexed { idx, t -> idToIndex[t.entryId!!] = idx }
        val editedIndex = _taskId?.let { idToIndex[it] } ?: dbTasks.size
        val n = if (_taskId == null) dbTasks.size + 1 else dbTasks.size

        val predAdj: Array<MutableList<Int>> = Array(n) { mutableListOf() }
        val succAdj: Array<MutableList<Int>> = Array(n) { mutableListOf() }

        for (link in TaskLink.all(dbManager)) {
            val p = idToIndex[link.predecessor] ?: continue
            val s = idToIndex[link.successor] ?: continue
            if (p == editedIndex || s == editedIndex) continue
            predAdj[s].add(p)
            succAdj[p].add(s)
        }
        for (predId in predecessors) {
            val p = idToIndex[predId] ?: continue
            predAdj[editedIndex].add(p)
            succAdj[p].add(editedIndex)
        }
        for (sucId in successors) {
            val s = idToIndex[sucId] ?: continue
            succAdj[editedIndex].add(s)
            predAdj[s].add(editedIndex)
        }

        val predClosure: Array<MutableSet<Int>>
        try {
            predClosure = initLinks(n, predAdj)
            checkRecursiveLinks(predClosure)
        } catch (_: IllegalArgumentException) {
            return TaskEditValidationError.RecursiveLinks
        }
        val succClosure = initLinks(n, succAdj)

        val starts = LongArray(n) { idx ->
            if (idx == editedIndex) startConstraintTimestamp ?: Long.MIN_VALUE
            else dbTasks[idx].startConstraintTimestamp ?: Long.MIN_VALUE
        }
        val ends = LongArray(n) { idx ->
            if (idx == editedIndex) deadlineTimestamp
            else dbTasks[idx].deadlineTimestamp
        }
        for (i in 0 until n) {
            predClosure[i].forEach { p -> if (starts[p] > starts[i]) starts[i] = starts[p] }
            succClosure[i].forEach { s -> if (ends[s] < ends[i]) ends[i] = ends[s] }
        }
        if ((0 until n).any { starts[it] > ends[it] })
            return TaskEditValidationError.IllegalLinks

        return null
    }

    fun commit(dbManager: DbManager) {
        val options = options

        val updateTransaction: SQLiteDatabase.() -> Unit = {
            val entry = Entry(_taskId, _optionsId, isTask = true)
            val isNew = entry.isNotCreated()

            if (isNew) {
                entry.create(dbManager)
                _taskId = entry.id
            } else {
                entry.update(dbManager)
            }

            val task = task

            if (isNew)
                task.create(dbManager)
            else
                task.update(dbManager)

            EntryTagLinks.updateTags(dbManager, tags, _taskId!!)

            val predsNew = predecessors.toSet()
            val predsOld = TaskLink.predecessors(dbManager, _taskId!!).toSet()
            for (predId in predsOld - predsNew) TaskLink.delete(dbManager, predId, _taskId!!)
            for (predId in predsNew - predsOld) TaskLink.create(dbManager, predId, _taskId!!)

            val sucsNew = successors.toSet()
            val sucsOld = TaskLink.successors(dbManager, _taskId!!).toSet()
            for (sucId in sucsOld - sucsNew) TaskLink.delete(dbManager, _taskId!!, sucId)
            for (sucId in sucsNew - sucsOld) TaskLink.create(dbManager, _taskId!!, sucId)
        }

        if (options.isNotCreated()) {
            options.createWithTransaction(dbManager) { optionsId ->
                _optionsId = optionsId
                updateTransaction()
            }
        } else {
            options.updateWithTransaction(dbManager, updateTransaction)
        }

        dbManager.tryScheduleNotification()
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
                task?.timeRemaining ?: 1, // FIXME: non-zero; might be better if configured in settings
                TextFieldValue(task?.getContents(dbManager) ?: ""),
                taskId?.let { Tag.forEntry(dbManager, taskId).sortedBy { it.name } } ?: emptyList(),
                predecessors,
                successors)
        }
    }
}

sealed class TaskEditValidationError {
    object StartAfterDeadline : TaskEditValidationError()
    object RecursiveLinks : TaskEditValidationError()
    object IllegalLinks : TaskEditValidationError()
}