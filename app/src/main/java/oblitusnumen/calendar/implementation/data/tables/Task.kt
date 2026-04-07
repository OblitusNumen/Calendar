package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.database.getLongOrNull
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry.Companion.COLUMN_NAME_ID
import java.time.ZoneId

open class Task(
    entryId: Int? = null,
    var startConstraintTimestamp: Long? = null,
    var deadlineTimestamp: Long,
    var timeZoneId: ZoneId,
    var timeConsumed: Int = 0,
    var timeRemaining: Int = 0
) : BaseColumns {
    var entryId: Int? = entryId
        private set
    val taskId: Int?
        get() = entryId
    val progress: Float?
        get() = if (timeRemaining + timeConsumed == 0)
            null
        else
            timeConsumed.toFloat() / (timeRemaining + timeConsumed)
    val isDone: Boolean
        get() = progress?.let { it == 1f } ?: true
//    private var entryCache: Entry? = null
//    val entry: Entry
//        get() {
//            if (entryCache == null) entryCache = dbManager.getEntryById(entryId!!)!!
//            return entryCache!!
//        }

    fun isOverdue(epochSecondNow: Long): Boolean =
        deadlineTimestamp <= epochSecondNow && !isDone

    fun predecessors(dbManager: DbManager) = TaskLink.predecessors(dbManager, taskId!!)

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId)
        contentValues.put(COLUMN_NAME_START_CONSTRAINT_TIMESTAMP, startConstraintTimestamp)
        contentValues.put(COLUMN_NAME_DEADLINE_TIMESTAMP, deadlineTimestamp)
        contentValues.put(COLUMN_NAME_TIME_ZONE_ID, timeZoneId.toString())
        contentValues.put(COLUMN_NAME_TIME_CONSUMED, timeConsumed)
        contentValues.put(COLUMN_NAME_TIME_REMAINING, timeRemaining)
        return contentValues
    }

    private fun create(dbManager: DbManager) { //todo may fail on UNIQUE violation?
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ENTRY_ID, null as Int?)
        dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues)
    }

    private fun update(dbManager: DbManager) { //todo value update may fail?
        dbManager.writableDatabase.update(
            TABLE_NAME,
            getContentValues(),
            "$COLUMN_NAME_ENTRY_ID = ?",
            arrayOf(entryId.toString())
        )
    }

    fun deleteCascade(dbManager: DbManager) {
//        entry.deleteCascade()
    }

    private fun delete(dbManager: DbManager) {
        dbManager.writableDatabase.delete(TABLE_NAME, "$COLUMN_NAME_ENTRY_ID = ?", arrayOf(entryId.toString()))
    }

//    fun createIfNotExists() {
//        if (id == null) create()
//    }
//
//    fun set(name: String, color: Color) {
//        this.name = name
//        this.color = color
//        update()
//    }

    companion object {
        const val TABLE_NAME: String = "Tasks"

        const val COLUMN_NAME_ENTRY_ID: String = "entryId"
        const val COLUMN_NAME_START_CONSTRAINT_TIMESTAMP: String = "startConstraintTimestamp"
        const val COLUMN_NAME_DEADLINE_TIMESTAMP: String = "deadlineTimestamp"
        const val COLUMN_NAME_TIME_ZONE_ID: String = "timeZoneId"
        const val COLUMN_NAME_TIME_CONSUMED: String = "timeConsumed"
        const val COLUMN_NAME_TIME_REMAINING: String = "timeRemaining"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_ENTRY_ID\"                      INTEGER PRIMARY KEY NOT NULL UNIQUE,\n" +
                "    \"$COLUMN_NAME_START_CONSTRAINT_TIMESTAMP\"    BIGINT,\n" +
                "    \"$COLUMN_NAME_DEADLINE_TIMESTAMP\"            BIGINT  NOT NULL,\n" +
                "    \"$COLUMN_NAME_TIME_ZONE_ID\"                  TEXT    NOT NULL,\n" +
                "    \"$COLUMN_NAME_TIME_CONSUMED\"                 INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_TIME_REMAINING\"                INTEGER NOT NULL,\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_ENTRY_ID\") REFERENCES \"${Entry.TABLE_NAME}\" (\"${Entry.COLUMN_NAME_ID}\")\n" +
                ");"

        fun cursorToList(cursor: Cursor): MutableList<Task> {
            val tasks: MutableList<Task> = ArrayList()

            val idxId = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
            val idxStart = cursor.getColumnIndex(COLUMN_NAME_START_CONSTRAINT_TIMESTAMP)
            val idxEnd = cursor.getColumnIndex(COLUMN_NAME_DEADLINE_TIMESTAMP)
            val idxZoneId = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE_ID)
            val idxTimeConsumed = cursor.getColumnIndex(COLUMN_NAME_TIME_CONSUMED)
            val idxTimeRemaining = cursor.getColumnIndex(COLUMN_NAME_TIME_REMAINING)

            while (cursor.moveToNext())
                tasks.add(
                    Task(
                        cursor.getInt(idxId),
                        cursor.getLongOrNull(idxStart),
                        cursor.getLong(idxEnd),
                        ZoneId.of(cursor.getString(idxZoneId)),
                        cursor.getInt(idxTimeConsumed),
                        cursor.getInt(idxTimeRemaining)
                    )
                )
            return tasks
        }

        fun deleteCascade(dbManager: DbManager, entryId: Int) {
            dbManager.writableDatabase.transaction {
                TaskLog.deleteAll(dbManager, entryId)
                TaskLink.deleteAll(dbManager, entryId)
                delete(TABLE_NAME, "$COLUMN_NAME_ENTRY_ID = ?", arrayOf(entryId.toString()))
            }
        }

        fun byId(dbManager: DbManager, id: Int): Task? {
            dbManager.readableDatabase.rawQuery(
                "SELECT * FROM ${TABLE_NAME} WHERE ${COLUMN_NAME_ENTRY_ID} = ?",
                arrayOf(id.toString())
            ).use { cursor ->
                val tasks = cursorToList(cursor)
                return if (tasks.isEmpty()) null else tasks[0]
            }
        }

        fun exists(dbManager: DbManager, id: Int): Boolean {
            // FIXME: mock
            return true

            dbManager.readableDatabase.rawQuery(
                "SELECT count(*) as eCount " +
                        "FROM $TABLE_NAME " +
                        "WHERE $COLUMN_NAME_ID = ?", arrayOf(id.toString())
            ).use { cursor ->
                cursor.moveToFirst()
                return cursor.getInt(cursor.getColumnIndex("eCount")) > 0
            }
        }
    }
}

// FIXME: mock
val _links = """
#0 1


#0 1
#1 2
#2 3


0	1
1 6
3	5
3 7
5 10
10 7
11 13
2 3
8 7
12 6
10 11
"""

val _tasks = """

#100 0 30
#100 10 40
#100 20 50
#100 30 60
#100 0 10
#100 1 15
#100 1 20
#100 30 40


#100000 1 10
#100000 10 15
#1000 0 20

#70000 0 10
#60000 1 15
#50000 1 20
#400000 1 25
#300000 1 30
#200000 1 35
#10000 30 40

#10000 0 50
#10000 0 50
#10000 0 50
#10000 0 50
#10000 0 50
#10000 0 50
#10000 0 50
#100000 40 60

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

5000000	20	200
5000000	0	250
400000	0	6
200000	0	8
600000	10	20
100000	0	30
5000000	0	300
10000000	0	300
300000	20	24
500000	0	70
200000	0	80
300000	0	8
2000000	205	220
400000	0	225

"""

fun readAll1(): Pair<MutableList<oblitusnumen.calendar.implementation.Task>, MutableList<TaskLink>> {
    val tasks: MutableList<oblitusnumen.calendar.implementation.Task> = mutableListOf()
    var index = 0
    _tasks.reader().readLines().forEach { line ->
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val taskParams = line.split(",", "\t", " ").map { it.toInt() }
        tasks.add(
            oblitusnumen.calendar.implementation.Task(
                index++,
                taskParams[0],
                taskParams[1],
                taskParams[2]
            )
        )
    }
    val links: MutableList<TaskLink> = mutableListOf()
    _links.reader().readLines().forEach { line ->
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val linkParams = line.split(",", "\t", " ").map { it.toInt() }
        links.add(TaskLink(linkParams[0], linkParams[1]))
    }
    return Pair(tasks, links)
}
