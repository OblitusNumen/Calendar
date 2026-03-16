package oblitusnumen.calendar.ui.state

import android.database.sqlite.SQLiteDatabase
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.*
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
    val entry
        get() = Entry(_entryId, _optionsId, isTask)
    val options
        get() = EventOptions(optionsId, name = name.text, color = color, contents = contents.text)

    fun commit(dbManager: DbManager) {
        // TODO:
        val options = options

        val updateTransaction: SQLiteDatabase.() -> Unit = {
            val entry = entry

            val entryNotCreated = entry.isNotCreated()

            if (entryNotCreated) {
                entry.create(dbManager)
                _entryId = entry.id
            } else {
                entry.update(dbManager)
            }

            //setting tags
            val tagsNew = tags.map { it.id }.toSet()
            val tagsOld = Tag.forEntry(dbManager, entryId!!).map { it.id }
            for (tId in tagsOld) {
                if (!tagsNew.contains(tId))
                    EntryTagLinks.delete(dbManager, entryId!!, tId!!)
            }
            for (t in tags) {
                if (t.id !in tagsOld) {
                    t.createIfNotExists(dbManager)
                    EntryTagLinks.create(dbManager, entryId!!, t.id!!)
                }
            }

            //setting dates
            val datesNew = dateStates.map { it.id }.toSet()
            val datesOld =
                Date.forEntry(dbManager, entryId!!).groupingBy { it.id }.reduce { _, accumulator, _ -> accumulator }
            for (d in datesOld.values) {
                if (!datesNew.contains(d.id))
                    d.deleteCascade(dbManager)
            }
            for (d in dateStates) {
                d.toDbEntity().apply {
                    setEntry(entry)
                    if (datesOld.containsKey(d.id))
                        this.update(dbManager)
                    else
                        this.create(dbManager)
                }
            }

            //setting notifications
            val notificationsNew = notificationStates.map { it.offset.toString() }.toSet()
            val notificationsOld =
                Notification.forOptions(dbManager, optionsId!!).groupingBy { it.offset.toString() }
                    .reduce { _, accumulator, _ -> accumulator }
            for (n in notificationsOld.values) {
                if (!notificationsNew.contains(n.offset.toString()))
                    n.delete(dbManager)
            }
            for (n in notificationStates) {
                n.toDbEntity().apply {
                    setOptionsId(optionsId!!)
                    if (notificationsOld.containsKey(n.offset.toString()))
                        this.update(dbManager)
                    else
                        this.create(dbManager)
                }
            }
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

    fun delete(dbManager: DbManager) {
        entry.deleteCascade(dbManager)
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
                TextFieldValue(entry?.getContents(dbManager) ?: ""),
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