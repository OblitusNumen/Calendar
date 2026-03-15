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
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.ui.state.DateState
import oblitusnumen.calendar.ui.state.EntryEditState
import oblitusnumen.calendar.ui.state.NotificationState

class EntryEditViewModel(initialState: EntryEditState) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<EntryEditState> = _state.asStateFlow()

    fun setName(name: TextFieldValue): Unit = _state.update { it.copy(name = name) }

    fun setColor(color: Color): Unit = _state.update { it.copy(color = color) }

    fun setContents(contents: TextFieldValue): Unit = _state.update { it.copy(contents = contents) }

    fun setTags(tags: List<Tag>): Unit = _state.update { it.copy(tags = tags.sortedBy { it.name }) }

    fun addDate(date: Date): DateState {
        val dateState = date.toUiState(_state.value.uiIdGenerator)
        _state.update { it.copy(dateStates = (it.dateStates + dateState).sorted()) }
        return dateState
    }

    fun rmDate(id: String): Unit = _state.update { it.copy(dateStates = it.dateStates.filter { it.uiId != id }) }

    fun updateDate(id: String, dateState: DateState) {
        _state.update { it.copy(dateStates = it.dateStates.map { if (it.uiId == id) dateState else it }) }
    }

    fun addNotification(offset: Period, sound: Boolean): Unit =
        _state.update {
            it.copy(
                notificationStates = (it.notificationStates + NotificationState(
                    it.uiIdGenerator.next(),
                    it.optionsId,
                    offset,
                    sound
                )).sorted()
            )
        }

    fun rmNotification(id: String): Unit =
        _state.update { it.copy(notificationStates = it.notificationStates.filter { it.uiId != id }) }

    fun setNotificationSound(id: String, sound: Boolean): Unit =
        _state.update { it.copy(notificationStates = it.notificationStates.map { if (it.uiId == id) it.copy(sound = sound) else it }) }

    fun commitToDb(dbManager: DbManager) {
        viewModelScope.launch {
            val snapshot = _state.value  // ← trivial to grab
            snapshot.commit(dbManager)
        }
    }
}