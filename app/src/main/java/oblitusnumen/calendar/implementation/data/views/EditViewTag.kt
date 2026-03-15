package oblitusnumen.calendar.implementation.data.views

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Modification
import oblitusnumen.calendar.implementation.data.SetModificationMutableState
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.EntryTagLinks
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.log

// this view is only for created tag
// must be dropped every time external update is invoked
class EditViewTag(dbManager: DbManager, val tagId: Int) {
    val tagName: MutableState<String>
    val tagColor: MutableState<Color?>

    private val entryMods: SetModificationMutableState<Int> =
        SetModificationMutableState(Entry.forTag(dbManager, tagId).toSet())
    val entryAssociations by entryMods.value

    val tag
        get() = Tag(tagName.value, tagId, tagColor.value)

    init {
        val tag = Tag.byId(dbManager, tagId)!!
        tagName = mutableStateOf(tag.name)
        tagColor = mutableStateOf(tag.color)
        log("EditViewTag init: $entryAssociations")
    }

    fun addEntryAssociations(vararg ids: Int) {
        entryMods.add(*ids.toTypedArray())
    }

    fun rmEntryAssociations(vararg ids: Int) {
        entryMods.rm(*ids.toTypedArray())
    }

    fun commit(dbManager: DbManager) {
        dbManager.writableDatabase.transaction {
            tag.update(dbManager)
            entryMods.forEachModification { entry, modification ->
                when (modification) {
                    Modification.ADD -> EntryTagLinks.create(dbManager, entry, tagId)
                    Modification.DELETE -> EntryTagLinks.delete(dbManager, entry, tagId)
                }
            }
        }
        entryMods.commit()
    }

    fun delete(dbManager: DbManager) {
        tag.deleteCascade(dbManager)
    }
}
