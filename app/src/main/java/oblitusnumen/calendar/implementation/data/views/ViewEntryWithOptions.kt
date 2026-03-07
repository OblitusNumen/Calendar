package oblitusnumen.calendar.implementation.data.views

import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.EventOptions
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.toColor

class ViewEntryWithOptions(
    val entryId: Int,
    val isTask: Boolean,
    val name: String,
    val color: Color
) {
    val displayName: String = name.ifEmpty { "[No title]" } // FIXME:

    companion object {
        fun getAll(dbManager: DbManager): MutableList<ViewEntryWithOptions> {
            dbManager.readableDatabase.rawQuery(
                "SELECT ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_ID} as entryId, " +
                        "${Entry.COLUMN_NAME_IS_TASK} as isTask, ${EventOptions.COLUMN_NAME_NAME} as name, " +
                        "${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from ${Entry.TABLE_NAME} " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}=${Entry.COLUMN_NAME_DEFAULT_OPTIONS_ID}",
                arrayOf()
            ).use { cursor ->
                val views: MutableList<ViewEntryWithOptions> = ArrayList()
                val idxEntryId = cursor.getColumnIndex("entryId")
                val idxIsTask = cursor.getColumnIndex("isTask")
                val idxName = cursor.getColumnIndex("name")
                val idxColor = cursor.getColumnIndex("color")
                while (cursor.moveToNext())
                    views.add(
                        ViewEntryWithOptions(
                            cursor.getInt(idxEntryId),
                            cursor.getInt(idxIsTask) != 0,
                            cursor.getString(idxName),
                            cursor.getInt(idxColor).toColor() ?: dbManager.defaultEntryColor
                        )
                    )
                log(views)
                return views
            }
        }
    }
}
