package oblitusnumen.calendar.implementation.data.views

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Modification
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.EntryTagLinks
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.log

// this view is only for created tag
// must be dropped every time external update is invoked
class EditViewTag(dbManager: DbManager, val tagId: Int) {
    val tagName: MutableState<String>
    val tagColor: MutableState<Color?>
    private var cachedAssociations: Set<Int> = Entry.forTag(dbManager, tagId).toSet()
    private val entries: MutableMap<Int, Modification> = mutableMapOf()
    var entryAssociations: Set<Int> by mutableStateOf(cachedAssociations)
        private set
    val tag
        get() = Tag(tagName.value, tagId, tagColor.value)

    init {
        val tag = Tag.byId(dbManager, tagId)!!
        tagName = mutableStateOf(tag.name)
        tagColor = mutableStateOf(tag.color)
        log("EditViewTag init: $entryAssociations")
    }

    private fun bakeIds() {
        entryAssociations = cachedAssociations.toMutableSet().apply {
            for (entry in entries) {
                when (entry.value) {
                    Modification.ADD -> this.add(entry.key)
                    Modification.DELETE -> this.remove(entry.key)
                }
            }
        }
    }

    fun addEntryAssociations(vararg ids: Int) {
        var update = false
        for (id in ids) {
            if (entries[id] == Modification.DELETE) {
                entries.remove(id)
                update = true
            } else if (!cachedAssociations.contains(id)) {
                entries[id] = Modification.ADD
                update = true
            }
        }
        if (update)
            bakeIds()
    }

    fun rmEntryAssociations(vararg ids: Int) {
        var update = false
        for (id in ids) {
            if (entries[id] == Modification.ADD) {
                entries.remove(id)
                update = true
            } else if (cachedAssociations.contains(id)) {
                entries[id] = Modification.DELETE
                update = true
            }
        }
        if (update)
            bakeIds()
    }

    fun commit(dbManager: DbManager) {
        dbManager.writableDatabase.transaction {
            tag.update(dbManager)
            for (entry in entries) {
                when (entry.value) {
                    Modification.ADD -> EntryTagLinks.create(dbManager, entry.key, tagId)
                    Modification.DELETE -> EntryTagLinks.delete(dbManager, entry.key, tagId)
                }
            }
        }
        entries.clear()
        cachedAssociations = entryAssociations
    }

    fun delete(dbManager: DbManager) {
        tag.deleteCascade(dbManager)
    }
}
