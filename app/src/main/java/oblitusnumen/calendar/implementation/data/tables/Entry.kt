package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.data.DbManager
import java.io.IOException
import java.time.ZonedDateTime

class Entry(
    id: Int? = null,
    defaultOptionsId: Int? = null,
    var isTask: Boolean = false,
) : BaseColumns {
    var id: Int? = id
        private set
    var defaultOptionsId: Int? = defaultOptionsId
        private set

    private var optionsCache: EventOptions? = null

    fun getOptions(dbManager: DbManager): EventOptions {
        if (optionsCache == null)
            optionsCache = EventOptions.byId(dbManager, defaultOptionsId!!)
        return optionsCache!!
    }

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_DEFAULT_OPTIONS_ID, defaultOptionsId)
        contentValues.put(COLUMN_NAME_IS_TASK, if (isTask) 1 else 0)
        return contentValues
    }

    fun isNotCreated() = id == null

    //since function is private isCreated check not needed
    private fun create(dbManager: DbManager) {
//        val contentValues = getContentValues()
//        contentValues.put(COLUMN_NAME_ID, null as Int?)
//        id = dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
//        if (!getDirectory(dbManager).mkdirs() || !getContentsFile(dbManager).createNewFile()) {
//            this.id = null
//            try {
//                if (getDirectory(dbManager).exists()) rmRecursively(getDirectory(dbManager))
//                dbManager.writableDatabase.execSQL(
//                    "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
//                    arrayOf(id.toString())
//                )
//            } catch (_: Exception) {
//            }
//            throw IOException("could not setup directory for entry $id, filename: ${getDirectory(dbManager)}")
//        }
    }

    private fun tryUpdateContents(dbManager: DbManager, contents: String) {// FIXME: atomic saves
//        try {
//            FileOutputStream(getContentsFile(dbManager)).use { fos ->
//                fos.write(contents.toByteArray())
//                fos.flush()
//                fos.fd.sync()
//            }
//        } catch (e: IOException) {
//            throw IOException("could not save contents for entry $id", e)
//        }
    }

    private fun update(dbManager: DbManager) {
        dbManager.writableDatabase.update(TABLE_NAME, getContentValues(), "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
    }

    @Throws(IOException::class)
    fun deleteCascade(dbManager: DbManager) {
        if (isNotCreated()) return
        // FIXME: code stacking
//        dbManager.writableDatabase.transaction {
//            state = STATE_DELETED
//            update(dbManager)
//            dbManager.writableDatabase.execSQL(
//                "DELETE FROM ${EntryTagLinks.TABLE_NAME} WHERE ${EntryTagLinks.COLUMN_NAME_ENTRY_ID} = ?",
//                arrayOf(id.toString())
//            )
//            dbManager.writableDatabase.execSQL(
//                "DELETE FROM ${Notification.TABLE_NAME} WHERE ${Notification.COLUMN_NAME_ENTRY_ID} = ?",
//                arrayOf(id.toString())
//            )
//            dbManager.writableDatabase.execSQL(
//                "DELETE FROM ${Date.TABLE_NAME} WHERE ${Date.COLUMN_NAME_ENTRY_ID} = ?",
//                arrayOf(id.toString())
//            )
//        }
        delete(dbManager)
        id = null
        dbManager.tryScheduleNotification()
    }

    private fun delete(dbManager: DbManager) {
//        if (getDirectory(dbManager).exists()) rmRecursively(getDirectory(dbManager))
        dbManager.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun fixup(dbManager: DbManager) {
//        when (state) {// FIXME: maybe differ new and deleted
//            STATE_NEW, STATE_DELETED -> {
//                delete(dbManager)
//            }
//
//            STATE_UPDATING -> {
//                // FIXME: repair file system
//                state = STATE_NORMAL
//                update(dbManager)
//            }
//
//            else -> throw IllegalStateException("unknown state $state")
//        }
    }

//    fun getDirectory(dbManager: DbManager): File = File(dbManager.filesDir, id.toString())

//    fun getContentsFile(dbManager: DbManager): File = File(getDirectory(dbManager), CONTENTS_FILENAME)

//    fun getContents(dbManager: DbManager): String { //todo contents should not be a just a string
//        if (!getContentsFile(dbManager).exists()) return ""
//        try {
//            FileInputStream(getContentsFile(dbManager)).use { fis ->
//                val buffer = ByteArray(4096) // Buffer to hold file data
//                val content = StringBuilder()
//                var bytesRead: Int
//                while ((fis.read(buffer).also { bytesRead = it }) != -1) {
//                    content.append(String(buffer, 0, bytesRead)) // Convert bytes to string
//                }
//                return content.toString() // Return the complete content as a string
//            }
//        } catch (e: IOException) {
//            throw RuntimeException(e)
//        }
//    }

    fun getNotifications(dbManager: DbManager): List<Notification> = Notification.forEntry(dbManager, id!!)

    fun getDates(dbManager: DbManager): List<Date> {
        if (isNotCreated()) return emptyList()
        return Date.byEntryId(dbManager, id!!)
    }

    fun getTags(dbManager: DbManager): List<Tag> {
        dbManager.readableDatabase.rawQuery(
            "SELECT ${Tag.TABLE_NAME}.* FROM ${Tag.TABLE_NAME} JOIN ${EntryTagLinks.TABLE_NAME} " +
                    "ON ${Tag.TABLE_NAME}.${Tag.COLUMN_NAME_ID} = ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_TAG_ID} " +
                    "WHERE ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_ENTRY_ID} = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            return Tag.cursorToList(cursor)
        }
    }

    @Throws(IOException::class)
    fun set(
        dbManager: DbManager,
        name: String,
        excludeFromCalendarView: Boolean,
        color: Color?,
        tags: Iterable<Tag>,
        dates: Iterable<Date>,
        notifications: Iterable<Notification>,
        contents: String
    ) {
//        if (isNotCreated()) {
//            create(dbManager)
//        } else {
//            state = STATE_UPDATING
//            update(dbManager)
//        }
//        tryUpdateContents(dbManager, contents)
//        dbManager.writableDatabase.transaction {
//            //setting entry
//            this@Entry.state = STATE_NORMAL
//            this@Entry.name = name
//            this@Entry.excludeFromCalendarView = excludeFromCalendarView
//            this@Entry.color = color
//            update(dbManager)
//
//            //setting tags
//            val tagsNew = tags.map { it.id }.toSet()
//            val tagsOld = getTags(dbManager).groupingBy { it.id }.reduce { _, accumulator, _ -> accumulator }
//            for (tId in tagsOld.keys) {
//                if (!tagsNew.contains(tId)) rmTag(dbManager, tId!!)
//            }
//            for (t in tags) {
//                if (!tagsOld.containsKey(t.id)) {
//                    t.createIfNotExists()
//                    addTag(dbManager, t.id!!)
//                }
//            }
//
//            //setting dates
//            val datesNew = dates.map { it.id }.toSet()
//            val datesOld = getDates(dbManager).groupingBy { it.id }.reduce { _, accumulator, _ -> accumulator }
//            for (d in datesOld.values) {
//                if (!datesNew.contains(d.id)) d.delete()
//            }
//            for (d in dates) {
//                if (datesOld.containsKey(d.id)) d.update() else {
//                    d.entryId = id
//                    d.create()
//                }
//            }
//
//            //setting notifications
//            val notificationsNew = notifications.map { it.offset.toString() }.toSet()
//            val notificationsOld =
//                getNotifications(dbManager).groupingBy { it.offset.toString() }.reduce { _, accumulator, _ -> accumulator }
//            for (n in notificationsOld.values) {
//                if (!notificationsNew.contains(n.offset.toString())) n.delete()
//            }
//            for (n in notifications) {
//                if (notificationsOld.containsKey(n.offset.toString())) n.update() else {
//                    n.eventOptionsId = id
//                    n.create()
//                }
//            }
//        }
//        dbManager.tryScheduleNotification()
    }

    private fun addTag(dbManager: DbManager, tagId: Int) {
        EntryTagLinks.create(dbManager, id!!, tagId)
    }

    private fun rmTag(dbManager: DbManager, tagId: Int) {
        EntryTagLinks.delete(dbManager, id!!, tagId)
    }

    /**
     * returns null if no associated dates, -1 if ended, next occurrence epoch second otherwise
     */
    fun nextDate(dbManager: DbManager): Long? {
        val now = System.currentTimeMillis() / 1000
        var nextDate: ZonedDateTime? = null
        var hasDates = false
        for (date in getDates(dbManager)) {
            hasDates = true
            val next = date.getNext(now)
            if (nextDate == null || next != null && next < nextDate) nextDate = next
        }
        return if (hasDates) nextDate?.toEpochSecond() ?: -1 else null
    }

    companion object {
        const val TABLE_NAME: String = "Entries"

        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_DEFAULT_OPTIONS_ID: String = "defaultOptionsId"
        const val COLUMN_NAME_IS_TASK: String = "isTask"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"$TABLE_NAME\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_ID\"                    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    \"$COLUMN_NAME_DEFAULT_OPTIONS_ID\"    INTEGER NOT NULL UNIQUE,\n" +
                "    \"$COLUMN_NAME_IS_TASK\"               INTEGER NOT NULL,\n" +
                "    FOREIGN KEY (\"$COLUMN_NAME_DEFAULT_OPTIONS_ID\") REFERENCES \"${EventOptions.TABLE_NAME}\" (\"${EventOptions.COLUMN_NAME_ID}\")\n" +
                ");"

        fun cursorToList(cursor: Cursor): MutableList<Entry> {
            val entries: MutableList<Entry> = ArrayList()

            val idIdx = cursor.getColumnIndex(COLUMN_NAME_ID)
            val defaultOptionsIdx = cursor.getColumnIndex(COLUMN_NAME_DEFAULT_OPTIONS_ID)
            val isTaskIdx = cursor.getColumnIndex(COLUMN_NAME_IS_TASK)

            while (cursor.moveToNext())
                entries.add(
                    Entry(
                        cursor.getInt(idIdx),
                        cursor.getInt(defaultOptionsIdx),
                        cursor.getInt(isTaskIdx) != 0,
                    )
                )
            return entries
        }

        fun all(dbManager: DbManager): List<Entry> {
            dbManager.readableDatabase.rawQuery("SELECT * FROM $TABLE_NAME", arrayOf()).use { cursor ->
                return cursorToList(cursor)
            }
        }

        fun byId(dbManager: DbManager, id: Int): Entry? {
            dbManager.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
                arrayOf(id.toString())
            ).use { cursor ->
                val entries = cursorToList(cursor)
                return if (entries.isEmpty()) null else entries[0]
            }
        }

        fun forTag(dbManager: DbManager, tagId: Int): List<Int> {
            dbManager.readableDatabase.rawQuery(
                "SELECT ${EntryTagLinks.COLUMN_NAME_ENTRY_ID} as eId " +
                        "from ${EntryTagLinks.TABLE_NAME} " +
                        "where ${EntryTagLinks.COLUMN_NAME_TAG_ID} = ?",
                arrayOf(tagId.toString())
            ).use { cursor ->
                val ids: MutableList<Int> = ArrayList()
                val idxEntryId = cursor.getColumnIndex("eId")
                while (cursor.moveToNext())
                    ids.add(cursor.getInt(idxEntryId))
                return ids
            }
        }

        fun countForTag(dbManager: DbManager, tagId: Int): Int {
            dbManager.readableDatabase.rawQuery(
                "SELECT count(*) as eCount " +
                        "FROM ${EntryTagLinks.TABLE_NAME} " +
                        "WHERE ${EntryTagLinks.COLUMN_NAME_TAG_ID} = ?", arrayOf(tagId.toString())
            ).use { cursor ->
                cursor.moveToFirst()
                return cursor.getInt(cursor.getColumnIndex("eCount"))
            }
        }

        fun exists(dbManager: DbManager, id: Int): Boolean {
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