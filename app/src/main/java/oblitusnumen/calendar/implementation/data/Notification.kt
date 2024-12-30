package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns


class Notification(
    private val dbManager: DbManager,
    var entryId: Int?,
    var offset: Period,
    var sound: Boolean = true
) : BaseColumns {

    fun create() {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId)
        contentValues.put(COLUMN_NAME_TIME_OFFSET, offset.toString())
        contentValues.put(COLUMN_NAME_SOUND, if (sound) 1 else 0)
        dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
    }

    fun update() {
        dbManager.writableDatabase.execSQL(
            "UPDATE $TABLE_NAME SET $COLUMN_NAME_SOUND = ? WHERE $COLUMN_NAME_ENTRY_ID = ? AND $COLUMN_NAME_TIME_OFFSET = ?",
            arrayOf(if (sound) 1 else 0, entryId.toString(), offset.toString())
        )
    }

    fun delete() {
        dbManager.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ENTRY_ID = ? AND $COLUMN_NAME_TIME_OFFSET = ?",
            arrayOf(entryId.toString(), offset.toString())
        )
    }

    companion object {
        const val TABLE_NAME: String = "notifications"
        const val COLUMN_NAME_ENTRY_ID: String = "entryId"
        const val COLUMN_NAME_TIME_OFFSET: String = "timeOffset"
        const val COLUMN_NAME_SOUND: String = "sound"

        fun cursorToList(
            dbManager: DbManager,
            cursor: Cursor
        ): MutableList<Notification> {
            val notifications: MutableList<Notification> = ArrayList()
            val idxEntryId = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
            val idxOffset = cursor.getColumnIndex(COLUMN_NAME_TIME_OFFSET)
            val idxSound = cursor.getColumnIndex(COLUMN_NAME_SOUND)
            while (cursor.moveToNext())
                notifications.add(
                    Notification(
                        dbManager, cursor.getInt(idxEntryId),
                        Period.decode(cursor.getString(idxOffset)),
                        cursor.getInt(idxSound) != 0
                    )
                )
            return notifications
        }
    }
}