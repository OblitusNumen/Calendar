package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns

class Tag private constructor(private val dbManager: DbManager, var name: String, id: Int = -1, var color: Int = -1) : BaseColumns {
    var id: Int = id
        private set

    fun create() { //fixme may fail on UNIQUE violation
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        contentValues.put(COLUMN_NAME_NAME, name)
        contentValues.put(COLUMN_NAME_COLOR, color)
        id = dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
    }

    fun exists(): Boolean {
        return id != -1
    }

    fun delete(cascade: Boolean = false) { //fixme full checks. ask for cascade. set id to -1
        throw UnsupportedOperationException("Not yet implemented")
    }

    fun update() { //fixme value update may fail
        throw UnsupportedOperationException("Not yet implemented")
    }

    companion object {
        const val TABLE_NAME: String = "tags"
        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_NAME: String = "name"
        const val COLUMN_NAME_COLOR: String = "color"

        fun getOrNew(dbManager: DbManager, name: String): Tag {
            dbManager.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE ${Entry.COLUMN_NAME_NAME} = ?",
                arrayOf(name)
            ).use { cursor ->
                return if (cursor.moveToFirst()) {
                    Tag(
                        dbManager, cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_COLOR))
                    )
                } else Tag(dbManager, name)
            }
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
                        cursor.getInt(idxColor)
                    )
                )
            return tags
        }
    }
}
