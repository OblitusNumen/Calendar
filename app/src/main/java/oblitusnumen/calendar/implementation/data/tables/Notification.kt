package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period


class Notification(
    var eventOptionsId: Int?,
    var offset: Period,
    var sound: Boolean = true
) : BaseColumns {
    fun create(dbManager: DbManager) {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_EVENT_OPTIONS_ID, eventOptionsId)
        contentValues.put(COLUMN_NAME_TIME_OFFSET, offset.toString())
        contentValues.put(COLUMN_NAME_HAS_SOUND, if (sound) 1 else 0)
        dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
    }

    fun update(dbManager: DbManager) {
        dbManager.writableDatabase.execSQL(
            "UPDATE $TABLE_NAME SET $COLUMN_NAME_HAS_SOUND = ? WHERE $COLUMN_NAME_EVENT_OPTIONS_ID = ? AND $COLUMN_NAME_TIME_OFFSET = ?",
            arrayOf((if (sound) 1 else 0).toString(), eventOptionsId.toString(), offset.toString())
        )
    }

    fun delete(dbManager: DbManager) {
        dbManager.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_EVENT_OPTIONS_ID = ? AND $COLUMN_NAME_TIME_OFFSET = ?",
            arrayOf(eventOptionsId.toString(), offset.toString())
        )
    }

    companion object {
        const val TABLE_NAME: String = "Notifications"

        const val COLUMN_NAME_EVENT_OPTIONS_ID: String = "eventOptionsId"
        const val COLUMN_NAME_TIME_OFFSET: String = "timeOffset"
        const val COLUMN_NAME_HAS_SOUND: String = "hasSound"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_EVENT_OPTIONS_ID\"  INTEGER     NOT NULL,\n" +
                "    \"$COLUMN_NAME_TIME_OFFSET\"       VARCHAR(18) NOT NULL,\n" +
                "    \"$COLUMN_NAME_HAS_SOUND\"         INTEGER     NOT NULL,\n" +
                "    PRIMARY KEY (\"$COLUMN_NAME_EVENT_OPTIONS_ID\", \"$COLUMN_NAME_TIME_OFFSET\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_EVENT_OPTIONS_ID\") REFERENCES \"${EventOptions.TABLE_NAME}\" (\"${EventOptions.COLUMN_NAME_ID}\")\n" +
                ");"

        fun cursorToList(cursor: Cursor): MutableList<Notification> {
            val notifications: MutableList<Notification> = ArrayList()

            val optionsIdx = cursor.getColumnIndex(COLUMN_NAME_EVENT_OPTIONS_ID)
            val offsetIdx = cursor.getColumnIndex(COLUMN_NAME_TIME_OFFSET)
            val soundIdx = cursor.getColumnIndex(COLUMN_NAME_HAS_SOUND)

            while (cursor.moveToNext())
                notifications.add(
                    Notification(
                        cursor.getInt(optionsIdx),
                        Period.decode(cursor.getString(offsetIdx)),
                        cursor.getInt(soundIdx) != 0
                    )
                )
            return notifications
        }

        fun forEntry(dbManager: DbManager, entryID: Int): List<Notification> {
            dbManager.readableDatabase.rawQuery(
                "SELECT $TABLE_NAME.* " +
                        "FROM $TABLE_NAME " +
                        "JOIN ${Date.TABLE_NAME} " +
                        "ON $TABLE_NAME.$COLUMN_NAME_EVENT_OPTIONS_ID = ${Date.TABLE_NAME}.${Date.COLUMN_NAME_EVENT_OPTIONS_ID} " +
                        "WHERE ${Date.COLUMN_NAME_ENTRY_ID} = ?", arrayOf(entryID.toString())
            ).use { cursor ->
                return cursorToList(cursor)
            }
        }
    }
}