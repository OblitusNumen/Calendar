package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.compose.runtime.Immutable
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.ui.state.EntryEditState
import oblitusnumen.calendar.ui.state.NotificationState


@Immutable
class Notification : BaseColumns {
    var eventOptionsId: Int?
    var offset: Period
    var sound: Boolean

    constructor(eventOptionsId: Int?, offset: Period, sound: Boolean = true) {
        this.eventOptionsId = eventOptionsId
        this.offset = offset
        this.sound = sound
    }

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

    fun setOptionsId(optionsId: Int) {
        eventOptionsId = optionsId
    }

    fun toUiState(uiIdGenerator: EntryEditState.UiIdGenerator) =
        NotificationState(uiIdGenerator.next(), eventOptionsId, offset, sound)

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

        fun forOptions(dbManager: DbManager, eventOptionsId: Int): List<Notification> {
            dbManager.readableDatabase.rawQuery(
                "SELECT $TABLE_NAME.* " +
                        "FROM $TABLE_NAME " +
                        "WHERE $COLUMN_NAME_EVENT_OPTIONS_ID = ?", arrayOf(eventOptionsId.toString())
            ).use { cursor ->
                return cursorToList(cursor)
            }
        }
    }
}