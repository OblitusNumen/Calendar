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
            // FIXME: mock
//            val result: MutableSet<TaskLink> = mutableSetOf()
//            val random = Random(2L)
//            repeat(100) {
//                result.add(TaskLink(random.nextInt(500) + 1, random.nextInt(500) + 1))
//            }
//            return result.toList()

//            val links: MutableSet<TaskLink> = mutableSetOf()
//            val tasks: MutableList<oblitusnumen.calendar.implementation.Task> = mutableListOf()
//            val random = Random(100L)
//            val count = 700
//            repeat(count) {
//                val start = (random.nextInt(100)) * 1
//                val end = start + (random.nextInt(100)) * 1
//                tasks.add(
//                    oblitusnumen.calendar.implementation.Task(
//                        it,
//                        random.nextInt(100000),
//                        start,
//                        end,
//                    )
//                )
//            }
//            repeat(60) {
//                val pred = random.nextInt(count)
//                var suc = pred
//                while (suc == pred || tasks[suc].endLimit < tasks[pred].startLimit) suc = random.nextInt(count)
//                links.add(TaskLink(pred, suc))
//            }
//            return links.toMutableList()

            val (tasks, links) = readAll1()
            return links

            dbManager.readableDatabase.rawQuery("SELECT * FROM $TABLE_NAME", arrayOf()).use { cursor ->
                return cursorToList(cursor)
            }
        }

        fun predecessors(dbManager: DbManager, id: Int): List<Int> {
            // FIXME: mock
            return all(dbManager).filter { it.successor == id }.map { it.predecessor }

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
    }
}
