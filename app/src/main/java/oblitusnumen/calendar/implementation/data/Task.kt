package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.database.sqlite.transaction
import java.time.ZoneId

class Task(
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
//    private var entryCache: Entry? = null
//    val entry: Entry
//        get() {
//            if (entryCache == null) entryCache = dbManager.getEntryById(entryId!!)!!
//            return entryCache!!
//        }

    private fun create(dbManager: DbManager) { //todo may fail on UNIQUE violation?
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ENTRY_ID, null as Int?)
        dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues)
    }

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
        const val TABLE_NAME: String = "tasks"
        const val COLUMN_NAME_ENTRY_ID: String = "entryId"
        const val COLUMN_NAME_START_CONSTRAINT_TIMESTAMP: String = "startConstraintTimestamp"
        const val COLUMN_NAME_DEADLINE_TIMESTAMP: String = "deadlineTimestamp"
        const val COLUMN_NAME_TIME_ZONE_ID: String = "timeZoneId"
        const val COLUMN_NAME_TIME_CONSUMED: String = "timeConsumed"
        const val COLUMN_NAME_TIME_REMAINING: String = "timeRemaining"

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
                        cursor.getLong(idxStart),
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
                TaskDependencies.deleteAll(dbManager, entryId)
                execSQL("DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ENTRY_ID = ?", arrayOf(entryId.toString()))
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
    }
}