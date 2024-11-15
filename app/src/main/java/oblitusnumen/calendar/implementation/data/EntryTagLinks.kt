package oblitusnumen.calendar.implementation.data

import android.provider.BaseColumns

class EntryTagLinks internal constructor(
    private val dbManager: DbManager,
    private val entryId: Int,
    private val tagId: Int) : BaseColumns {
    fun create() {
        dbManager.writableDatabase.execSQL(
            "INSERT OR IGNORE INTO $TABLE_NAME ($COLUMN_NAME_ENTRY_ID, $COLUMN_NAME_TAG_ID) VALUES (?, ?)",
            arrayOf(entryId.toString(), tagId.toString())
        )
    }

    fun delete() {
        dbManager.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ENTRY_ID = ? AND $COLUMN_NAME_TAG_ID = ?",
            arrayOf(entryId.toString(), tagId.toString())
        )
    }

    companion object {
        const val TABLE_NAME: String = "entryTagLinks"
        const val COLUMN_NAME_ENTRY_ID: String = "entryId"
        const val COLUMN_NAME_TAG_ID: String = "tagId"
    }
}