package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.compose.ui.graphics.Color
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt

class Tag(
    var name: String = "",
    id: Int? = null,
    var color: Color? = null
) : BaseColumns {
    var id: Int? = id
        private set

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_NAME, name)
        contentValues.put(COLUMN_NAME_COLOR, color.toInt())
        return contentValues
    }

    fun colorOrDefault(dbManager: DbManager): Color = color ?: dbManager.defaultTagColor

    private fun create(dbManager: DbManager) { //todo may fail on UNIQUE violation
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        id = dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
    }

    fun update(dbManager: DbManager) { //todo value update may fail
        log("update")
        dbManager.writableDatabase.update(TABLE_NAME, getContentValues(), "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
    }

    fun deleteCascade(dbManager: DbManager) {
        dbManager.writableDatabase.transaction {
            dbManager.writableDatabase.delete(
                EntryTagLinks.TABLE_NAME,
                "${EntryTagLinks.COLUMN_NAME_TAG_ID} = ?",
                arrayOf(id.toString())
            )
            delete(dbManager)
        }
        id = null
    }

    private fun delete(dbManager: DbManager) {
        dbManager.writableDatabase.delete(TABLE_NAME, "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
    }

    fun createIfNotExists(dbManager: DbManager) {
        if (id == null) create(dbManager)
    }

    fun set(dbManager: DbManager, name: String, color: Color) {
        this.name = name
        this.color = color
        update(dbManager)
    }

    companion object {
        const val TABLE_NAME: String = "Tags"

        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_NAME: String = "name"
        const val COLUMN_NAME_COLOR: String = "color"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_ID\"    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    \"$COLUMN_NAME_NAME\"  TEXT    NOT NULL UNIQUE,\n" +
                "    \"$COLUMN_NAME_COLOR\" INTEGER NOT NULL\n" +
                ");"

        fun cursorToList(cursor: Cursor): MutableList<Tag> {
            val tags: MutableList<Tag> = ArrayList()

            val idxId = cursor.getColumnIndex(COLUMN_NAME_ID)
            val idxName = cursor.getColumnIndex(COLUMN_NAME_NAME)
            val idxColor = cursor.getColumnIndex(COLUMN_NAME_COLOR)

            while (cursor.moveToNext())
                tags.add(
                    Tag(
                        cursor.getString(idxName),
                        cursor.getInt(idxId),
                        cursor.getInt(idxColor).toColor()
                    )
                )
            return tags
        }

        fun all(dbManager: DbManager): List<Tag> {
            dbManager.readableDatabase.rawQuery("SELECT * FROM $TABLE_NAME", arrayOf()).use { cursor ->
                return cursorToList(cursor)
            }
        }

        fun allWithEntryCount(dbManager: DbManager): Map<Tag, Int> {
            dbManager.readableDatabase.rawQuery(
                "SELECT t.*, count(l.${EntryTagLinks.COLUMN_NAME_ENTRY_ID}) as \"entryCount\" " +
                        "FROM $TABLE_NAME as t " +
                        "LEFT JOIN ${EntryTagLinks.TABLE_NAME} as l " +
                        "ON l.${EntryTagLinks.COLUMN_NAME_TAG_ID} = t.$COLUMN_NAME_ID " +
                        "GROUP BY t.$COLUMN_NAME_ID", arrayOf()
            ).use { cursor ->
                val result = HashMap<Tag, Int>()
                val tags = cursorToList(cursor)
                val entryCountIdx = cursor.getColumnIndex("entryCount")
                cursor.moveToFirst()
                for (t in tags) {
                    result[t] = cursor.getInt(entryCountIdx)
                    cursor.moveToNext()
                }
                return result
            }
        }

        fun forEntry(dbManager: DbManager, entryId: Int): List<Tag> {
            dbManager.readableDatabase.rawQuery(
                "SELECT ${TABLE_NAME}.* FROM ${TABLE_NAME} JOIN ${EntryTagLinks.TABLE_NAME} " +
                        "ON ${TABLE_NAME}.${COLUMN_NAME_ID} = ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_TAG_ID} " +
                        "WHERE ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_ENTRY_ID} = ?",
                arrayOf(entryId.toString())
            ).use { cursor ->
                return cursorToList(cursor)
            }
        }

        fun byId(dbManager: DbManager, id: Int): Tag? {
            dbManager.readableDatabase.rawQuery(
                "SELECT * FROM ${TABLE_NAME} WHERE ${COLUMN_NAME_ID} = ?",
                arrayOf(id.toString())
            ).use { cursor ->
                val tags = cursorToList(cursor)
                return if (tags.isEmpty()) null else tags[0]
            }
        }
    }
}
