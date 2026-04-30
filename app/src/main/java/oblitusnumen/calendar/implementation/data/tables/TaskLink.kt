package oblitusnumen.calendar.implementation.data.tables

import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.data.DbManager

data class TaskLink(val predecessor: Int, val successor: Int) : BaseColumns {
    companion object {
        const val TABLE_NAME: String = "TaskLinks"

        const val COLUMN_NAME_PREDECESSOR_ID: String = "predecessorId"
        const val COLUMN_NAME_SUCCESSOR_ID: String = "successorId"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_PREDECESSOR_ID\"    INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_SUCCESSOR_ID\"     INTEGER NOT NULL,\n" +
                "    PRIMARY KEY (\"$COLUMN_NAME_PREDECESSOR_ID\", \"$COLUMN_NAME_SUCCESSOR_ID\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_PREDECESSOR_ID\") REFERENCES \"${Task.TABLE_NAME}\" (\"${Task.COLUMN_NAME_ENTRY_ID}\"),\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_SUCCESSOR_ID\") REFERENCES \"${Task.TABLE_NAME}\" (\"${Task.COLUMN_NAME_ENTRY_ID}\")\n" +
                ");"

        fun cursorToList(cursor: Cursor): List<TaskLink> {
            val taskLinks: MutableList<TaskLink> = ArrayList()

            val predecessorIdx = cursor.getColumnIndex(COLUMN_NAME_PREDECESSOR_ID)
            val successorIdx = cursor.getColumnIndex(COLUMN_NAME_SUCCESSOR_ID)

            while (cursor.moveToNext())
                taskLinks.add(
                    TaskLink(
                        cursor.getInt(predecessorIdx),
                        cursor.getInt(successorIdx)
                    )
                )

            return taskLinks
        }

        fun create(dbManager: DbManager, predecessorId: Int, successorId: Int) {
            dbManager.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO $TABLE_NAME ($COLUMN_NAME_PREDECESSOR_ID, $COLUMN_NAME_SUCCESSOR_ID) VALUES (?, ?)",
                arrayOf(predecessorId.toString(), successorId.toString())
            )
        }

        fun delete(dbManager: DbManager, predecessorId: Int, successorId: Int) {
            dbManager.writableDatabase.delete(
                TABLE_NAME,
                "$COLUMN_NAME_PREDECESSOR_ID = ? AND $COLUMN_NAME_SUCCESSOR_ID = ?",
                arrayOf(predecessorId.toString(), successorId.toString())
            )
        }

        fun deleteAll(dbManager: DbManager, taskId: Int) {
            dbManager.writableDatabase.transaction {
                dbManager.writableDatabase.delete(
                    TABLE_NAME,
                    "$COLUMN_NAME_PREDECESSOR_ID = ? OR $COLUMN_NAME_SUCCESSOR_ID = ?",
                    arrayOf(taskId.toString(), taskId.toString())
                )
            }
        }

        fun all(dbManager: DbManager): List<TaskLink> {
            dbManager.readableDatabase.rawQuery("SELECT * FROM $TABLE_NAME", arrayOf()).use { cursor ->
                return cursorToList(cursor)
            }
        }

        fun predecessors(dbManager: DbManager, id: Int): List<Int> {
            dbManager.readableDatabase.rawQuery(
                "SELECT DISTINCT $COLUMN_NAME_PREDECESSOR_ID FROM $TABLE_NAME WHERE $COLUMN_NAME_SUCCESSOR_ID = ?",
                arrayOf(id.toString())
            ).use { cursor ->
                val predecessors: MutableList<Int> = ArrayList()

                val predecessorIdx = cursor.getColumnIndex(COLUMN_NAME_PREDECESSOR_ID)

                while (cursor.moveToNext())
                    predecessors.add(cursor.getInt(predecessorIdx))

                return predecessors
            }
        }

        fun successors(dbManager: DbManager, id: Int): List<Int> {
            dbManager.readableDatabase.rawQuery(
                "SELECT DISTINCT $COLUMN_NAME_SUCCESSOR_ID FROM $TABLE_NAME WHERE $COLUMN_NAME_PREDECESSOR_ID = ?",
                arrayOf(id.toString())
            ).use { cursor ->
                val successors: MutableList<Int> = ArrayList()

                val successorIdx = cursor.getColumnIndex(COLUMN_NAME_SUCCESSOR_ID)

                while (cursor.moveToNext())
                    successors.add(cursor.getInt(successorIdx))

                return successors
            }
        }

        fun checkPredecessor(dbManager: DbManager, id: Int, successors: List<Int>): Boolean {
            for (successor in successors) {
                if (id == successor)
                    return false
                if (!checkPredecessor(dbManager, id, successors(dbManager, successor)))
                    return false
            }
            return true
        }

        fun checkSuccessor(dbManager: DbManager, id: Int, predecessors: List<Int>): Boolean {
            for (predecessor in predecessors) {
                if (!checkPredecessor(dbManager, predecessor, successors(dbManager, id)))
                    return false
            }
            return true
        }
    }
}
