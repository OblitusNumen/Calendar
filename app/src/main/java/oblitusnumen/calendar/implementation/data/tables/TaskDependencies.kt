package oblitusnumen.calendar.implementation.data.tables

import android.provider.BaseColumns
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.data.DbManager

class TaskDependencies : BaseColumns {
    companion object {
        const val TABLE_NAME: String = "TaskLinks"

        const val COLUMN_NAME_PARENT_TASK_ID: String = "parentTaskId"
        const val COLUMN_NAME_CHILD_TASK_ID: String = "childTaskId"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_PARENT_TASK_ID\"    INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_CHILD_TASK_ID\"     INTEGER NOT NULL,\n" +
                "    PRIMARY KEY (\"$COLUMN_NAME_PARENT_TASK_ID\", \"$COLUMN_NAME_CHILD_TASK_ID\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_PARENT_TASK_ID\") REFERENCES \"${Task.TABLE_NAME}\" (\"${Task.COLUMN_NAME_ENTRY_ID}\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_CHILD_TASK_ID\") REFERENCES \"${Task.TABLE_NAME}\" (\"${Task.COLUMN_NAME_ENTRY_ID}\")\n" +
                ");"

        fun create(dbManager: DbManager, parentTaskId: Int, childTaskId: Int) {
            dbManager.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO $TABLE_NAME ($COLUMN_NAME_PARENT_TASK_ID, $COLUMN_NAME_CHILD_TASK_ID) VALUES (?, ?)",
                arrayOf(parentTaskId.toString(), childTaskId.toString())
            )
        }

        fun delete(dbManager: DbManager, parentTaskId: Int, childTaskId: Int) {
            dbManager.writableDatabase.delete(
                TABLE_NAME,
                "$COLUMN_NAME_PARENT_TASK_ID = ? AND $COLUMN_NAME_CHILD_TASK_ID = ?",
                arrayOf(parentTaskId.toString(), childTaskId.toString())
            )
        }

        fun deleteAll(dbManager: DbManager, taskId: Int) {
            dbManager.writableDatabase.transaction {
                dbManager.writableDatabase.delete(
                    TABLE_NAME,
                    "$COLUMN_NAME_PARENT_TASK_ID = ? OR $COLUMN_NAME_CHILD_TASK_ID = ?",
                    arrayOf(taskId.toString(), taskId.toString())
                )
            }
        }
    }
}