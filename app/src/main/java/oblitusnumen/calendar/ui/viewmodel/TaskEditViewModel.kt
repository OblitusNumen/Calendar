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

class TaskEditViewModel(initialState: TaskEditState) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<TaskEditState> = _state.asStateFlow()

    fun setName(name: TextFieldValue): Unit = _state.update { it.copy(name = name) }

    fun setColor(color: Color): Unit = _state.update { it.copy(color = color) }

    fun setContents(contents: TextFieldValue): Unit = _state.update { it.copy(contents = contents) }

    fun setTags(tags: List<Tag>): Unit = _state.update { it.copy(tags = tags.sortedBy { it.name }) }

    fun commitToDb(dbManager: DbManager) {
        viewModelScope.launch {
            val snapshot = _state.value
            snapshot.commit(dbManager)
        }
    }
}