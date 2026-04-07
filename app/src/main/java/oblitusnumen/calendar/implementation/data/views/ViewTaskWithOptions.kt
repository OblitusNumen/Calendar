package oblitusnumen.calendar.implementation.data.views

import android.database.Cursor
import androidx.compose.ui.graphics.Color
import androidx.core.database.getLongOrNull
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.EventOptions
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.readAll1
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.toColor
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

class ViewTaskWithOptions(
    id: Int,
    val optionsId: Int,
    val name: String,
    val color: Color,
    startConstraintTimestamp: Long,
    deadlineTimestamp: Long,
    timeZoneId: ZoneId,
    timeConsumed: Int,
    timeRemaining: Int
) : Task(id, startConstraintTimestamp, deadlineTimestamp, timeZoneId, timeConsumed, timeRemaining) {

    // FIXME:
//    val startLimit
//        get() = (/*max(*/startConstraintTimestamp!!/* - ZonedDateTime.now().toEpochSecond(), 0)*/ + 86399) / 86400
//    val endLimit
//        get() = max((deadlineTimestamp + 86399) / 86400, startLimit)
    val displayName: String = name.ifEmpty { "[No title]" } // FIXME:

    var realStartConstraints: Long? = startConstraintTimestamp
        private set
    var realDeadline: Long = deadlineTimestamp
        private set

    fun getContents(dbManager: DbManager): String =
        EventOptions.contentsById(dbManager, optionsId)

    fun setRealConstraints(
        allTasks: Map<Int, ViewTaskWithOptions>,
        successors: MutableSet<Int>,
        predecessors: MutableSet<Int>
    ) {
        successors.forEach {
            if (allTasks[it]!!.deadlineTimestamp < realDeadline)
                realDeadline = allTasks[it]!!.deadlineTimestamp
        }
        predecessors.forEach {
            if (realStartConstraints == null)
                realStartConstraints = allTasks[it]!!.startConstraintTimestamp
            else if (allTasks[it]!!.startConstraintTimestamp != null &&
                allTasks[it]!!.startConstraintTimestamp!! > realStartConstraints!!
            )
                realStartConstraints = allTasks[it]!!.realStartConstraints
        }
        // FIXME:
//        if (realStartConstraints != null && realStartConstraints!! > realDeadline)
//            throw IllegalArgumentException("Illegal links detected")
    }

    fun countPredecessorsTimeEstimate(allTasks: Map<Int, ViewTaskWithOptions>, predecessorLinks: Map<Int, List<Int>>): Int {
        var sum = 0
        predecessorLinks[taskId!!]!!.forEach { tId ->
            val task = allTasks[tId]!!
            sum += task.countPredecessorsTimeEstimate(allTasks, predecessorLinks)
            sum += task.timeRemaining
        }
        return sum
    }

    companion object {
        fun cursorToList(dbManager: DbManager, cursor: Cursor): List<ViewTaskWithOptions> {
            val views: MutableList<ViewTaskWithOptions> = ArrayList()

            val idxId = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
            val optionsIdx = cursor.getColumnIndex("optionsId")
            val idxName = cursor.getColumnIndex("name")
            val idxColor = cursor.getColumnIndex("color")
            val idxStart = cursor.getColumnIndex(COLUMN_NAME_START_CONSTRAINT_TIMESTAMP)
            val idxEnd = cursor.getColumnIndex(COLUMN_NAME_DEADLINE_TIMESTAMP)
            val idxZoneId = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE_ID)
            val idxTimeConsumed = cursor.getColumnIndex(COLUMN_NAME_TIME_CONSUMED)
            val idxTimeRemaining = cursor.getColumnIndex(COLUMN_NAME_TIME_REMAINING)

            while (cursor.moveToNext())
                views.add(
                    ViewTaskWithOptions(
                        cursor.getInt(idxId),
                        cursor.getInt(optionsIdx),
                        cursor.getString(idxName),
                        cursor.getInt(idxColor).toColor() ?: dbManager.defaultTaskColor,
                        // FIXME: if null?
                        cursor.getLongOrNull(idxStart) ?: ZonedDateTime.now().toEpochSecond(),
                        cursor.getLong(idxEnd),
                        ZoneId.of(cursor.getString(idxZoneId)),
                        cursor.getInt(idxTimeConsumed),
                        cursor.getInt(idxTimeRemaining)
                    )
                )

            return views
        }

        fun byId(dbManager: DbManager, id: Int): ViewTaskWithOptions? {
            // FIXME: this is mock
            return all(dbManager).find { it.taskId == id }

            dbManager.readableDatabase.rawQuery(
                "select $TABLE_NAME.*, ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID} as optionsId, " +
                        "${EventOptions.COLUMN_NAME_NAME} as name, ${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from $TABLE_NAME " +
                        "join ${Entry.TABLE_NAME} " +
                        "on $TABLE_NAME.$COLUMN_NAME_ENTRY_ID = ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_ID} " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_DEFAULT_OPTIONS_ID} = " +
                        "${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}" +
                        " where $TABLE_NAME.$COLUMN_NAME_ENTRY_ID = ?",
                arrayOf(id.toString()),
            ).use { cursor ->
                return cursorToList(dbManager, cursor).let { if (it.isEmpty()) null else it.first() }
            }
        }

        fun all(dbManager: DbManager): List<ViewTaskWithOptions> {
            // FIXME: this is mock
//            val now = ZonedDateTime.now().toEpochSecond()
//            val result: MutableList<ViewTaskWithOptions> = mutableListOf()
//            val random = Random(2L)
//            repeat(640) {
//                val start = now + (random.nextInt(2000) - 1000) * 3600
//                val end = start + (random.nextInt(2000)) * 3600
//                result.add(
//                    ViewTaskWithOptions(
//                        it,
//                        it,
//                        "task $it",
//                        Color.Red,
//                        start,
//                        end,
//                        defaultZoneId(),
//                        random.nextInt(100),
//                        random.nextInt(100)
//                    )
//                )
//            }
//            return result

//            val tasks: MutableList<ViewTaskWithOptions> = mutableListOf()
//            val random = Random(100L)
//            val count = 700
//            val now = ZonedDateTime.now().toEpochSecond()
//            repeat(count) {
//                val start = (random.nextInt(100)) * 1 - 50
//                val end = start + (random.nextInt(100)) * 1
//                tasks.add(
//                    ViewTaskWithOptions(
//                        it,
//                        it,
//                        "task $it",
//                        Color.Red,
//                        now + start.toLong() * 86400,
//                        now + end.toLong() * 86400,
//                        defaultZoneId(),
//                        Random.nextInt(100),
//                        (random.nextInt(100) - 50).let { if (it < 0) 0 else it },
//                    )
//                )
//            }
//            return tasks



            val (tasks, links) = readAll1()

            val now = ZonedDateTime.now().toEpochSecond()
            val random = Random(100L)
            return tasks.map {
                ViewTaskWithOptions(
                    it.index,
                    it.index,
                    "task ${it.index}",
                    Color.Red,
                    now + it.startLimit * 86400,
                    now + it.endLimit * 86400,
                    defaultZoneId(),
                    random.nextInt(100),
                    (random.nextInt(100) - 50).let { if (it < 0) 0 else it },
                )
            }

            dbManager.readableDatabase.rawQuery(
                "select $TABLE_NAME.*, ${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID} as defaultOptionsId, " +
                        "${EventOptions.COLUMN_NAME_NAME} as name, ${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from $TABLE_NAME " +
                        "join ${Entry.TABLE_NAME} " +
                        "on $TABLE_NAME.$COLUMN_NAME_ENTRY_ID = ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_ID} " +
                        "join ${EventOptions.TABLE_NAME} " +
                        "on ${Entry.TABLE_NAME}.${Entry.COLUMN_NAME_DEFAULT_OPTIONS_ID} = " +
                        "${EventOptions.TABLE_NAME}.${EventOptions.COLUMN_NAME_ID}",
                arrayOf()
            ).use { cursor ->
                return cursorToList(dbManager, cursor)
            }
        }
    }
}
