package oblitusnumen.calendar.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import java.util.concurrent.atomic.AtomicInteger

@Immutable
data class EntryEditState(
    val uiIdGenerator: UiIdGenerator,
    private var _entryId: Int?,
    private var _optionsId: Int?,
    val isTask: Boolean,
    val name: TextFieldValue,
    val color: Color,
    val contents: TextFieldValue,  // FIXME: this should be List<Content>)
    val tags: List<Tag>,
    val dateStates: List<DateState>,
    val notificationStates: List<NotificationState>,
) {
    val entryId
        get() = _entryId
    val optionsId
        get() = _optionsId

    fun commit(dbManager: DbManager) {
        // TODO:
    }

    companion object {
        fun initial(dbManager: DbManager, entryId: Int?, isTask: Boolean?): EntryEditState {
            val entry = entryId?.let { ViewEntryWithOptions.byId(dbManager, entryId)!! }
            val uiIdGenerator = UiIdGenerator()

            return EntryEditState(
                uiIdGenerator,
                entryId,
                entry?.defaultOptionsId,
                entry?.isTask ?: isTask!!,
                TextFieldValue(entry?.name ?: ""),
                entry?.color ?: dbManager.defaultEntryColor,
                TextFieldValue("entry.getContents()"),
                entryId?.let { Tag.forEntry(dbManager, entryId).sortedBy { it.name } } ?: emptyList(),
                entry?.getDates(dbManager)?.map { it.toUiState(uiIdGenerator) }?.sorted()
                    ?: emptyList(),
                run {
                    if (entry == null || entry.isNotCreated())
                        dbManager.defaultNotifications.map {
                            NotificationState(
                                uiIdGenerator.next(),
                                null,
                                it.first,
                                it.second
                            )
                        }
                    else
                        entry.getNotifications(dbManager).map { it.toUiState(uiIdGenerator) }.sorted()
                })
        }
    }

    class UiIdGenerator {
        private val counter = AtomicInteger(0)

        fun next(): String = "element_${counter.incrementAndGet()}"
    }
}