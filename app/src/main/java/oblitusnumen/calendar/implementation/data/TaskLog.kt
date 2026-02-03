package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import java.time.ZoneId

class TaskLog : BaseColumns {
    private val dbManager: DbManager?
    var id: Int? = null
        private set
    val taskId: Int
    var startOfDayTimestamp: Long
    var timeZoneId: ZoneId
    var timeConsumed: Int
//    private var taskCache: Task? = null
//    val task: Task
//        get() {
//            if (taskCache == null) taskCache = dbManager!!.getTaskById(taskId)!!
//            return taskCache!!
//        }

    internal constructor(
        dbManager: DbManager,
        id: Int,
        taskId: Int,
        startOfDayTimestamp: Long,
        zoneId: String,
        timeConsumed: Int,
    ) {
        this.dbManager = dbManager
        this.id = id
        this.taskId = taskId
        this.startOfDayTimestamp = startOfDayTimestamp
        this.timeZoneId = ZoneId.of(zoneId)
        this.timeConsumed = timeConsumed
    }

    fun create() {
        if (isEmpty)
            return
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        id = dbManager!!.writableDatabase.insertWithOnConflict(
            TABLE_NAME,
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_REPLACE
        ).toInt()
    }

    fun update() {
        if (isEmpty) {
            delete()
            return
        }
        dbManager!!.writableDatabase.update(
            TABLE_NAME,
            getContentValues(),
            "$COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun delete() {
        dbManager!!.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
        id = null
    }

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_TASK_ID, taskId)
        contentValues.put(COLUMN_NAME_START_OF_DAY_TIMESTAMP, startOfDayTimestamp)
        contentValues.put(COLUMN_NAME_TIME_ZONE_ID, timeZoneId.toString())
        contentValues.put(COLUMN_NAME_TIME_CONSUMED, timeConsumed)
        return contentValues
    }

    val isEmpty: Boolean
        get() = timeConsumed <= 0

    companion object {
        const val TABLE_NAME: String = "taskLog"
        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_TASK_ID: String = "taskId"
        const val COLUMN_NAME_START_OF_DAY_TIMESTAMP: String = "startOfDayTimestamp"
        const val COLUMN_NAME_TIME_ZONE_ID: String = "timeZoneId"
        const val COLUMN_NAME_TIME_CONSUMED: String = "timeConsumed"

        fun cursorToList(
            dbManager: DbManager,
            cursor: Cursor
        ): MutableList<TaskLog> {
            val taskLogs: MutableList<TaskLog> = ArrayList()
            val idIdx: Int = cursor.getColumnIndex(COLUMN_NAME_ID)
            val taskIdIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TASK_ID)
            val startOfDayTimestampIdx: Int = cursor.getColumnIndex(COLUMN_NAME_START_OF_DAY_TIMESTAMP)
            val timeZoneIdIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE_ID)
            val timeConsumedIdx: Int = cursor.getColumnIndex(COLUMN_NAME_TIME_CONSUMED)
            while (cursor.moveToNext())
                taskLogs.add(
                    TaskLog(
                        dbManager,
                        cursor.getInt(idIdx),
                        cursor.getInt(taskIdIdx),
                        cursor.getLong(startOfDayTimestampIdx),
                        cursor.getString(timeZoneIdIdx),
                        cursor.getInt(timeConsumedIdx),
                    )
                )
            return taskLogs
        }

        fun deleteAll(dbManager: DbManager, taskId: Int) {
            dbManager.writableDatabase.execSQL(
                "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_TASK_ID = ?",
                arrayOf(taskId.toString())
            )
        }
    }
}