package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt

class Tag private constructor(private val dbManager: DbManager, var name: String, id: Int? = null, var color: Color? = null) : BaseColumns {
    var id: Int? = id
        private set

    fun create() { //todo may fail on UNIQUE violation
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        id = dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
    }

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_NAME, name)
        contentValues.put(COLUMN_NAME_COLOR, color.toInt())
        return contentValues
    }

    fun update() { //todo value update may fail
        dbManager.writableDatabase.update(TABLE_NAME, getContentValues(), "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
    }

    fun deleteCascade() { // TODO: use transaction
        dbManager.writableDatabase.delete(EntryTagLinks.TABLE_NAME, "${EntryTagLinks.COLUMN_NAME_TAG_ID} = ?", arrayOf(id.toString()))
        dbManager.writableDatabase.delete(TABLE_NAME, "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
        id = null
    }

    fun createIfNotExists() {
        if (id == null) create()
    }

    fun set(name: String, color: Color) {
        this.name = name
        this.color = color
        update()
    }

    companion object {
        const val TABLE_NAME: String = "tags"
        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_NAME: String = "name"
        const val COLUMN_NAME_COLOR: String = "color"

        fun new(dbManager: DbManager, name: String): Tag {
            return Tag(dbManager, name)
        }

        fun cursorToList(
            dbManager: DbManager,
            cursor: Cursor
        ): MutableList<Tag> {
            val tags: MutableList<Tag> = ArrayList()
            val idxId = cursor.getColumnIndex(COLUMN_NAME_ID)
            val idxName = cursor.getColumnIndex(COLUMN_NAME_NAME)
            val idxColor = cursor.getColumnIndex(COLUMN_NAME_COLOR)
            while (cursor.moveToNext())
                tags.add(
                    Tag(
                        dbManager, cursor.getString(idxName),
                        cursor.getInt(idxId),
                        cursor.getInt(idxColor).toColor()
                    )
                )
            return tags
        }
    }
}
