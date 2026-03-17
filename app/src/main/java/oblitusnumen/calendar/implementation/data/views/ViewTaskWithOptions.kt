package oblitusnumen.calendar.implementation.data.views

import android.database.Cursor
import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.EventOptions
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.toColor
import java.time.ZoneId

class ViewTaskWithOptions private constructor(
    id: Int,
    val optionsId: Int,
    val name: String,
    val color: Color,
    startConstraintTimestamp: Long,
    deadlineTimestamp: Long,
    timeZoneId: ZoneId,
    timeConsumed: Int,
    timeRemaining: Int
) : Task(id, startConstraintTimestamp, deadlineTimestamp, timeZoneId, timeConsumed, timeRemaining) {
    val displayName: String = name.ifEmpty { "[No title]" } // FIXME:

    fun getContents(dbManager: DbManager): String =
        EventOptions.contentsById(dbManager, optionsId)

    companion object {
        fun cursorToList(dbManager: DbManager, cursor: Cursor): List<ViewTaskWithOptions> {
            val views: MutableList<ViewTaskWithOptions> = ArrayList()

            val idxId = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
            val optionsIdx = cursor.getColumnIndex("optionsId")
            val idxName = cursor.getColumnIndex("name")
            val idxColor = cursor.getColumnIndex("color")
            val idxStart = cursor.getColumnIndex(COLUMN_NAME_START_CONSTRAINT_TIMESTAMP)
            val idxEnd = cursor.getColumnIndex(COLUMN_NAME_DEADLINE_TIMESTAMP)
            val idxZoneId = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE_ID)
            val idxTimeConsumed = cursor.getColumnIndex(COLUMN_NAME_TIME_CONSUMED)
            val idxTimeRemaining = cursor.getColumnIndex(COLUMN_NAME_TIME_REMAINING)

            while (cursor.moveToNext())
                views.add(
                    ViewTaskWithOptions(
                        cursor.getInt(idxId),
                        cursor.getInt(optionsIdx),
                        cursor.getString(idxName),
                        cursor.getInt(idxColor).toColor() ?: dbManager.defaultTaskColor,
                        cursor.getLong(idxStart),
                        cursor.getLong(idxEnd),
                        ZoneId.of(cursor.getString(idxZoneId)),
                        cursor.getInt(idxTimeConsumed),
                        cursor.getInt(idxTimeRemaining)
                    )
                )

            return views
        }

        fun byId(dbManager: DbManager, id: Int): ViewTaskWithOptions? {
            dbManager.readableDatabase.rawQuery(
                "select $TABLE_NAME.*, ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID} as optionsId, " +
                        "${EventOptions.COLUMN_NAME_NAME} as name, ${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from $TABLE_NAME " +
                        "join ${Entry.TABLE_NAME} " +
                        "on $TABLE_NAME.$COLUMN_NAME_ENTRY_ID = ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_ID} " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_DEFAULT_OPTIONS_ID} = " +
                        "${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}" +
                        " where $TABLE_NAME.$COLUMN_NAME_ENTRY_ID = ?",
                arrayOf(id.toString()),
            ).use { cursor ->
                return cursorToList(dbManager, cursor).let { if (it.isEmpty()) null else it.first() }
            }
        }

        fun all(dbManager: DbManager): List<ViewTaskWithOptions> {
            dbManager.readableDatabase.rawQuery(
                "select $TABLE_NAME.*, ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID} as defaultOptionsId, " +
                        "${EventOptions.COLUMN_NAME_NAME} as name, ${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from $TABLE_NAME " +
                        "join ${Entry.TABLE_NAME} " +
                        "on $TABLE_NAME.$COLUMN_NAME_ENTRY_ID = ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_ID} " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_DEFAULT_OPTIONS_ID} = " +
                        "${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}",
                arrayOf()
            ).use { cursor ->
                return cursorToList(dbManager, cursor)
            }
        }
    }
}
