package oblitusnumen.calendar.implementation.data.views

import android.database.Cursor
import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.EventOptions
import oblitusnumen.calendar.implementation.toColor

class ViewEntryWithOptions private constructor(
    id: Int,
    defaultOptionsId: Int,
    isTask: Boolean,
    val name: String,
    val color: Color,
    dbManager: DbManager,
) : Entry(id, defaultOptionsId, isTask) {
    var nextDate: Long?
        private set

    val displayName: String = name.ifEmpty { "[No title]" } // FIXME:

    init {
        nextDate = nextDate(dbManager)
    }

    companion object {
        fun cursorToList(dbManager: DbManager, cursor: Cursor): List<ViewEntryWithOptions> {
            val views: MutableList<ViewEntryWithOptions> = ArrayList()

            val idIdx = cursor.getColumnIndex(COLUMN_NAME_ID)
            val defaultOptionsIdx = cursor.getColumnIndex(COLUMN_NAME_DEFAULT_OPTIONS_ID)
            val isTaskIdx = cursor.getColumnIndex(COLUMN_NAME_IS_TASK)
            val idxName = cursor.getColumnIndex("name")
            val idxColor = cursor.getColumnIndex("color")

            while (cursor.moveToNext())
                views.add(
                    ViewEntryWithOptions(
                        cursor.getInt(idIdx),
                        cursor.getInt(defaultOptionsIdx),
                        cursor.getInt(isTaskIdx) != 0,
                        cursor.getString(idxName),
                        cursor.getInt(idxColor).toColor() ?: dbManager.defaultEntryColor,
                        dbManager
                    )
                )

            return views
        }

        fun byId(dbManager: DbManager, id: Int): ViewEntryWithOptions? {
            dbManager.readableDatabase.rawQuery(
                "SELECT $TABLE_NAME.*, ${EventOptions.COLUMN_NAME_NAME} as name, " +
                        "${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from $TABLE_NAME " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}=$COLUMN_NAME_DEFAULT_OPTIONS_ID " +
                        "where $TABLE_NAME.$COLUMN_NAME_ID = ?",
                arrayOf(id.toString()),
            ).use { cursor ->
                return cursorToList(dbManager, cursor).let { if (it.isEmpty()) null else it.first() }
            }
        }

        fun all(dbManager: DbManager): List<ViewEntryWithOptions> {
            dbManager.readableDatabase.rawQuery(
                "SELECT $TABLE_NAME.*, ${EventOptions.COLUMN_NAME_NAME} as name, " +
                        "${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from $TABLE_NAME " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}=$COLUMN_NAME_DEFAULT_OPTIONS_ID",
                arrayOf()
            ).use { cursor ->
                return cursorToList(dbManager, cursor)
            }
        }
    }
}
