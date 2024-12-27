package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import oblitusnumen.calendar.implementation.rmRecursively
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class Entry : BaseColumns {
    private val dbManager: DbManager
    var id: Int? = null
        private set
    var name: String = ""

    /**
     * we initialize `Entry` here and only here
     *
     * @param dbManager
     */
    internal constructor(dbManager: DbManager) { //fixme create entry != save to db
        this.dbManager = dbManager
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        contentValues.put(COLUMN_NAME_NAME, name)
        id = dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()
        if (!dir.mkdirs()) throw RuntimeException("could not create directory for entry $id, filename: $dir")
        try {
            if (!contentsFile.createNewFile()) throw IOException()
        } catch (e: IOException) {
            throw RuntimeException("could not create directory for entry $id, filename: $dir", e)
        }
    }

    private constructor(dbManager: DbManager, id: Int, name: String) {
        this.dbManager = dbManager
        this.id = id
        this.name = name
    }

    fun getContents(): String { //todo contents should not be a just a string
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
        dbManager.readableDatabase.rawQuery("SELECT * FROM ${Notification.TABLE_NAME} " +
                "WHERE ${Notification.COLUMN_NAME_ENTRY_ID} = ?", arrayOf(id.toString())).use { cursor ->
            return Notification.cursorToList(dbManager, cursor)
        }
    }

    fun getDates(): List<Date> {
        return Date.getAllByEntryId(dbManager, id!!)
    }

    private val dir: File
        get() = File(dbManager.filesDir, id.toString())

    private val contentsFile: File
        get() = File(dir, CONTENTS_FILENAME)

    fun deleteCascade() { // FIXME: Use transactions
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
        rmRecursively(dir)
        dbManager.writableDatabase.execSQL(
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
        id = null
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

    fun set(name: String, tags: Iterable<Tag>, dates: Iterable<Date>, notifications: Iterable<Notification>, contents: String) { // todo should be transaction
        //setting contents
        try {
            FileOutputStream(contentsFile).use { fos ->
                fos.write(contents.toByteArray())
            }
        } catch (e: IOException) {
            throw RuntimeException("could not save contents file for entry $id", e)
        }
        this.name = name
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
            if (datesOld.containsKey(d.id)) d.update() else d.create()
        }

        //setting notifications
        val notificationsNew = notifications.map { it.offset.toString() }.toSet()
        val notificationsOld =
            getNotifications().groupingBy { it.offset.toString() }.reduce { _, accumulator, _ -> accumulator }
        for (n in notificationsOld.values) {
            if (!notificationsNew.contains(n.offset.toString())) n.delete()
        }
        for (n in notifications) {
            if (notificationsOld.containsKey(n.offset.toString())) n.update() else n.create()
        }

        dbManager.tryScheduleNotification()
    }

    private fun update() {
        dbManager.writableDatabase.execSQL(
            "UPDATE $TABLE_NAME SET $COLUMN_NAME_NAME = ? WHERE $COLUMN_NAME_ID = ?", arrayOf(name, id.toString())
        )
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
        const val COLUMN_NAME_NAME: String = "name"
        const val CONTENTS_FILENAME: String = "contents.md"

        fun cursorToList(
            dbManager: DbManager,
            cursor: Cursor
        ): MutableList<Entry> {
            val entries: MutableList<Entry> = ArrayList()
            val idxId = cursor.getColumnIndex(COLUMN_NAME_ID)
            val idxName = cursor.getColumnIndex(COLUMN_NAME_NAME)
            while (cursor.moveToNext())
                entries.add(Entry(dbManager, cursor.getInt(idxId), cursor.getString(idxName)))
            return entries
        }
    }
}