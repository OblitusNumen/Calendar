package oblitusnumen.calendar.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.ui.state.TaskEditState
import oblitusnumen.calendar.ui.state.TaskEditValidationError
import java.time.ZoneId

class TaskEditViewModel(initialState: TaskEditState) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<TaskEditState> = _state.asStateFlow()

    fun setName(name: TextFieldValue): Unit = _state.update { it.copy(name = name) }

    fun setColor(color: Color): Unit = _state.update { it.copy(color = color) }

    fun setContents(contents: TextFieldValue): Unit = _state.update { it.copy(contents = contents) }

    fun setTags(tags: List<Tag>): Unit = _state.update { it.copy(tags = tags.sortedBy { it.name }) }

    fun setTimeZone(timeZoneId: ZoneId): Unit = _state.update { it.withTimeZone(timeZoneId = timeZoneId) }

    fun setStartConstraint(startConstraintTimestamp: Long?): Unit =
        _state.update { it.copy(startConstraintTimestamp = startConstraintTimestamp) }

    fun setDeadline(deadlineTimestamp: Long): Unit = _state.update { it.copy(deadlineTimestamp = deadlineTimestamp) }

    fun setTimeConsumed(timeConsumed: Int): Unit = _state.update { it.copy(timeConsumed = timeConsumed) }

    fun setTimeRemaining(timeRemaining: Int): Unit = _state.update { it.copy(timeRemaining = timeRemaining) }

    fun setPredecessors(predecessors: List<Int>): Unit = _state.update { it.copy(predecessors = predecessors) }

    fun setSuccessors(successors: List<Int>): Unit = _state.update { it.copy(successors = successors) }

    fun commitToDb(
        dbManager: DbManager,
        onError: (TaskEditValidationError) -> Unit,
        onSuccess: () -> Unit,
    ) {
        val snapshot = _state.value
        val err = snapshot.validate(dbManager)
        if (err != null) {
            onError(err)
            return
        }
        viewModelScope.launch {
            snapshot.commit(dbManager)
            onSuccess()
        }
    }
}