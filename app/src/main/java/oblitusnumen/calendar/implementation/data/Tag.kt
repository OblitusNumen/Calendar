package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.provider.BaseColumns

class Tag : BaseColumns {
    private val dbManager: DbManager
    var id: Int = -1
        private set
    var name: String
    var color: Int = -1

    private constructor(dbManager: DbManager, name: String) {
        this.dbManager = dbManager
        this.name = name
    }

    internal constructor(dbManager: DbManager, id: Int, name: String, color: Int) {
        this.dbManager = dbManager
        this.id = id
        this.name = name
        this.color = color
    }

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
                        dbManager, cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_COLOR))
                    )
                } else Tag(dbManager, name)
            }
        }
    }
}
