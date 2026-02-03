package oblitusnumen.calendar.implementation.data

import android.provider.BaseColumns

class TaskDependencies : BaseColumns {
    companion object {
        const val TABLE_NAME: String = "taskLinks"
        const val COLUMN_NAME_PARENT_TASK_ID: String = "parentTaskId"
        const val COLUMN_NAME_CHILD_TASK_ID: String = "childTaskId"

        fun create(dbManager: DbManager, parentTaskId: Int, childTaskId: Int) {
            dbManager.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO $TABLE_NAME ($COLUMN_NAME_PARENT_TASK_ID, $COLUMN_NAME_CHILD_TASK_ID) VALUES (?, ?)",
                arrayOf(parentTaskId.toString(), childTaskId.toString())
            )
        }

        fun delete(dbManager: DbManager, parentTaskId: Int, childTaskId: Int) {
            dbManager.writableDatabase.execSQL(
                "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_PARENT_TASK_ID = ? AND $COLUMN_NAME_CHILD_TASK_ID = ?",
                arrayOf(parentTaskId.toString(), childTaskId.toString())
            )
        }

        fun deleteAll(dbManager: DbManager, taskId: Int) {
            dbManager.writableDatabase.execSQL(
                "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_PARENT_TASK_ID = ? OR $COLUMN_NAME_CHILD_TASK_ID = ?",
                arrayOf(taskId.toString(), taskId.toString())
            )
        }
    }
}