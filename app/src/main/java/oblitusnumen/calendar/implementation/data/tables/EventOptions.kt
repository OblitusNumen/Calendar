package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt

class EventOptions private constructor(
    id: Int? = null,
    private val state: Int = STATE_NEW,
    var name: String = "",
    var color: Color? = null
) : BaseColumns {
    var id: Int? = id
        private set

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_STATE, state)
        contentValues.put(COLUMN_NAME_NAME, name)
        contentValues.put(COLUMN_NAME_COLOR, color.toInt())
        return contentValues
    }

    companion object {
        const val TABLE_NAME = "EventOptions"

        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_STATE: String = "state"
        const val COLUMN_NAME_NAME: String = "name"
        const val COLUMN_NAME_COLOR: String = "color"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_ID\"        INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    \"$COLUMN_NAME_STATE\"     INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_NAME\"      TEXT    NOT NULL,\n" +
                "    \"$COLUMN_NAME_COLOR\"     INTEGER NOT NULL\n" +
                ");"

        const val STATE_NORMAL: Int = 0
        const val STATE_NEW: Int = 1
        const val STATE_UPDATING: Int = 2
        const val STATE_DELETED: Int = 3
        const val CONTENTS_FILENAME: String = "contents.md"

        fun cursorToList(cursor: Cursor): MutableList<EventOptions> {
            val options: MutableList<EventOptions> = ArrayList()

            val idIdx = cursor.getColumnIndex(COLUMN_NAME_ID)
            val stateIdx = cursor.getColumnIndex(COLUMN_NAME_STATE)
            val nameIdx = cursor.getColumnIndex(COLUMN_NAME_NAME)
            val colorIdx = cursor.getColumnIndex(COLUMN_NAME_COLOR)

            while (cursor.moveToNext())
                options.add(
                    EventOptions(
                        cursor.getInt(idIdx),
                        cursor.getInt(stateIdx),
                        cursor.getString(nameIdx),
                        cursor.getInt(colorIdx).toColor()
                    )
                )
            return options
        }
    }
}