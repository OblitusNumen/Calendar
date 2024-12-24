package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import java.util.ArrayList


class Notification private constructor(
    private val dbManager: DbManager,
    var entryId: Int,
    var offset: Long,
    var sound: Boolean = true
) : BaseColumns {

    fun create() { //fixme may fail on UNIQUE violation// createOrUpdate
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId)
        contentValues.put(COLUMN_NAME_TIME_OFFSET, offset)
        contentValues.put(COLUMN_NAME_SOUND, if (sound) 1 else 0)
        dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
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
                        cursor.getLong(idxOffset),
                        cursor.getInt(idxSound) != 0
                    )
                )
            return notifications
        }
    }
}