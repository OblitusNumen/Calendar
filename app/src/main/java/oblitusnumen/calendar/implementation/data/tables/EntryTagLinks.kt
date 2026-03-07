package oblitusnumen.calendar.implementation.data.tables

import android.provider.BaseColumns
import oblitusnumen.calendar.implementation.data.DbManager

class EntryTagLinks : BaseColumns {
    companion object {
        const val TABLE_NAME: String = "EntryTagLinks"

        const val COLUMN_NAME_ENTRY_ID: String = "entryId"
        const val COLUMN_NAME_TAG_ID: String = "tagId"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_ENTRY_ID\"  INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_TAG_ID\"    INTEGER NOT NULL,\n" +
                "    PRIMARY KEY (\"$COLUMN_NAME_ENTRY_ID\", \"$COLUMN_NAME_TAG_ID\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_ENTRY_ID\") REFERENCES \"${Entry.TABLE_NAME}\" (\"${Entry.COLUMN_NAME_ID}\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_TAG_ID\") REFERENCES \"${Tag.TABLE_NAME}\" (\"${Tag.COLUMN_NAME_ID}\")\n" +
                ");"

        fun create(dbManager: DbManager, entryId: Int, tagId: Int) {
            dbManager.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO $TABLE_NAME ($COLUMN_NAME_ENTRY_ID, $COLUMN_NAME_TAG_ID) VALUES (?, ?)",
                arrayOf(entryId.toString(), tagId.toString())
            )
        }

        fun delete(dbManager: DbManager, entryId: Int, tagId: Int) {
            dbManager.writableDatabase.execSQL(
                "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ENTRY_ID = ? AND $COLUMN_NAME_TAG_ID = ?",
                arrayOf(entryId.toString(), tagId.toString())
            )
        }
    }
}