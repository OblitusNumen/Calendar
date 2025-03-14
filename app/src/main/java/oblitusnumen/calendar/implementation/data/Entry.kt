package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.compose.ui.graphics.Color
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.rmRecursively
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class Entry private constructor(
    private val dbManager: DbManager,
    id: Int? = null,
    private var state: Int = STATE_NEW,
    var name: String = "",
    var excludeFromCalendarView: Boolean = false,
    var color: Color? = null
) : BaseColumns {
    var id: Int? = id
        private set

    fun getColorOrDefault(): Color = color ?: dbManager.defaultEntryColor

    fun isNotCreated() = id == null

    private fun create() {
        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        id = dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
        if (!dir.mkdirs() || !contentsFile.createNewFile()) {
            this.id = null
            try {
                if (dir.exists()) rmRecursively(dir)
                dbManager.writableDatabase.execSQL(
                    "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
                    arrayOf(id.toString())
                )
            } catch (_: Exception) {
            }
            throw IOException("could not setup directory for entry $id, filename: $dir")
        }
    }

    private fun tryUpdateContents(contents: String) {// FIXME: atomic saves
        try {
            FileOutputStream(contentsFile).use { fos ->
                fos.write(contents.toByteArray())
                fos.flush()
                fos.fd.sync()
            }
        } catch (e: IOException) {
            throw IOException("could not save contents for entry $id", e)
        }
    }

    private fun update() {
        dbManager.writableDatabase.update(TABLE_NAME, getContentValues(), "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
    }

    @Throws(IOException::class)
    fun deleteCascade() {
        if (isNotCreated()) return
        dbManager.writableDatabase.transaction {
            state = STATE_DELETED
            update()
            dbManager.writableDatabase.execSQL(
                "DELETE FROM ${EntryTagLinks.TABLE_NAME} WHERE ${EntryTagLinks.COLUMN_NAME_ENTRY_ID} = ?",
                arrayOf(id.toString())
            )
            dbManager.writableDatabase.execSQL(
                "DELETE FROM ${Notification.TABLE_NAME} WHERE ${Notification.COLUMN_NAME_ENTRY_ID} = ?",
                arrayOf(id.toString())
            )
            dbManager.writableDatabase.execSQL(
                "DELETE FROM ${Date.TABLE_NAME} WHERE ${Date.COLUMN_NAME_ENTRY_ID} = ?",
                arrayOf(id.toString())
            )
        }
        delete()
        id = null
        dbManager.tryScheduleNotification()
    }

    private fun delete() {
        if (dir.exists()) rmRecursively(dir)
        dbManager.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun fixup() {
        when (state) {// FIXME: maybe differ new and deleted
            STATE_NEW, STATE_DELETED -> {
                delete()
            }

            STATE_UPDATING -> {
                // FIXME: repair file system
                state = STATE_NORMAL
                update()
            }

            else -> throw IllegalStateException("unknown state $state")
        }
    }

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_STATE, state)
        contentValues.put(COLUMN_NAME_NAME, name)
        contentValues.put(COLUMN_NAME_EXCLUDE_VIEW, if (excludeFromCalendarView) 1 else 0)
        contentValues.put(COLUMN_NAME_COLOR, color.toInt())
        return contentValues
    }

    private val dir: File
        get() = File(dbManager.filesDir, id.toString())

    private val contentsFile: File
        get() = File(dir, CONTENTS_FILENAME)

    fun getContents(): String { //todo contents should not be a just a string
        if (!contentsFile.exists()) return ""
        try {
            FileInputStream(contentsFile).use { fis ->
                val buffer = ByteArray(4096) // Buffer to hold file data
                val content = StringBuilder()
                var bytesRead: Int
                while ((fis.read(buffer).also { bytesRead = it }) != -1) {
                    content.append(String(buffer, 0, bytesRead)) // Convert bytes to string
                }
                return content.toString() // Return the complete content as a string
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun getNotifications(): List<Notification> {
        dbManager.readableDatabase.rawQuery(
            "SELECT * FROM ${Notification.TABLE_NAME} " +
                    "WHERE ${Notification.COLUMN_NAME_ENTRY_ID} = ?", arrayOf(id.toString())
        ).use { cursor ->
            return Notification.cursorToList(dbManager, cursor)
        }
    }

    fun getDates(): List<Date> {
        if (isNotCreated()) return emptyList()
        return Date.getAllByEntryId(dbManager, id!!)
    }

    fun getTags(): List<Tag> {
        dbManager.readableDatabase.rawQuery(
            "SELECT ${Tag.TABLE_NAME}.* FROM ${Tag.TABLE_NAME} JOIN ${EntryTagLinks.TABLE_NAME} " +
                    "ON ${Tag.TABLE_NAME}.${Tag.COLUMN_NAME_ID} = ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_TAG_ID} " +
                    "WHERE ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_ENTRY_ID} = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            return Tag.cursorToList(dbManager, cursor)
        }
    }

    @Throws(IOException::class)
    fun set(
        name: String,
        excludeFromCalendarView: Boolean,
        color: Color?,
        tags: Iterable<Tag>,
        dates: Iterable<Date>,
        notifications: Iterable<Notification>,
        contents: String
    ) {
        if (isNotCreated()) {
            create()
        } else {
            state = STATE_UPDATING
            update()
        }
        tryUpdateContents(contents)
        dbManager.writableDatabase.transaction {
            //setting entry
            this@Entry.state = STATE_NORMAL
            this@Entry.name = name
            this@Entry.excludeFromCalendarView = excludeFromCalendarView
            this@Entry.color = color
            update()

            //setting tags
            val tagsNew = tags.map { it.id }.toSet()
            val tagsOld = getTags().groupingBy { it.id }.reduce { _, accumulator, _ -> accumulator }
            for (tId in tagsOld.keys) {
                if (!tagsNew.contains(tId)) rmTag(tId!!)
            }
            for (t in tags) {
                if (!tagsOld.containsKey(t.id)) {
                    t.createIfNotExists()
                    addTag(t.id!!)
                }
            }

            //setting dates
            val datesNew = dates.map { it.id }.toSet()
            val datesOld = getDates().groupingBy { it.id }.reduce { _, accumulator, _ -> accumulator }
            for (d in datesOld.values) {
                if (!datesNew.contains(d.id)) d.delete()
            }
            for (d in dates) {
                if (datesOld.containsKey(d.id)) d.update() else {
                    d.entryId = id
                    d.create()
                }
            }

            //setting notifications
            val notificationsNew = notifications.map { it.offset.toString() }.toSet()
            val notificationsOld =
                getNotifications().groupingBy { it.offset.toString() }.reduce { _, accumulator, _ -> accumulator }
            for (n in notificationsOld.values) {
                if (!notificationsNew.contains(n.offset.toString())) n.delete()
            }
            for (n in notifications) {
                if (notificationsOld.containsKey(n.offset.toString())) n.update() else {
                    n.entryId = id
                    n.create()
                }
            }
        }
        dbManager.tryScheduleNotification()
    }

    private fun addTag(tagId: Int) {
        EntryTagLinks.create(dbManager, id!!, tagId)
    }

    private fun rmTag(tagId: Int) {
        EntryTagLinks.delete(dbManager, id!!, tagId)
    }

    companion object {
        const val TABLE_NAME: String = "entries"
        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_STATE: String = "state"
        const val COLUMN_NAME_NAME: String = "name"
        const val COLUMN_NAME_EXCLUDE_VIEW: String = "excludeView"
        const val COLUMN_NAME_COLOR: String = "color"
        const val STATE_NORMAL: Int = 0
        const val STATE_NEW: Int = 1
        const val STATE_UPDATING: Int = 2
        const val STATE_DELETED: Int = 3
        const val CONTENTS_FILENAME: String = "contents.md"

        fun new(dbManager: DbManager): Entry {
            return Entry(dbManager)
        }

        fun cursorToList(
            dbManager: DbManager,
            cursor: Cursor
        ): MutableList<Entry> {
            val entries: MutableList<Entry> = ArrayList()
            val idxId = cursor.getColumnIndex(COLUMN_NAME_ID)
            val idxState = cursor.getColumnIndex(COLUMN_NAME_STATE)
            val idxName = cursor.getColumnIndex(COLUMN_NAME_NAME)
            val idxExcludeCalendarView = cursor.getColumnIndex(COLUMN_NAME_EXCLUDE_VIEW)
            val idxColor = cursor.getColumnIndex(COLUMN_NAME_COLOR)
            while (cursor.moveToNext())
                entries.add(
                    Entry(
                        dbManager,
                        cursor.getInt(idxId),
                        cursor.getInt(idxState),
                        cursor.getString(idxName),
                        cursor.getInt(idxExcludeCalendarView) != 0,
                        cursor.getInt(idxColor).toColor()
                    )
                )
            return entries
        }
    }
}