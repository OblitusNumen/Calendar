package oblitusnumen.calendar.implementation.data

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import oblitusnumen.calendar.implementation.rmRecursively
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class Entry : BaseColumns {
    private val dbManager: DbManager
    var id: Int = -1
        private set
    var name: String = ""

    internal constructor(dbManager: DbManager, cursor: Cursor) : this(
        dbManager, cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME))
    )

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

    fun getDates(): List<Date> {
        val dates: MutableList<Date> = ArrayList()
        dbManager.readableDatabase.rawQuery(
            "SELECT * FROM ${Date.TABLE_NAME} WHERE ${Date.COLUMN_NAME_ENTRY_ID} = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor != null)
                while (cursor.moveToNext())
                    dates.add(Date(dbManager, cursor))
        }
        return dates
    }

    private val dir: File
        get() = File(dbManager.filesDir, id.toString())

    private val contentsFile: File
        get() = File(dir, CONTENTS_FILENAME)

    fun delete() { // FIXME: check refs or delete cascade. Use transactions
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
    }

    fun getTags(): List<Tag> {
        val tags: MutableList<Tag> = ArrayList()
        dbManager.readableDatabase.rawQuery(
            "SELECT ${Tag.TABLE_NAME}.* FROM ${Tag.TABLE_NAME} JOIN ${EntryTagLinks.TABLE_NAME} " +
                    "ON ${Tag.TABLE_NAME}.${Tag.COLUMN_NAME_ID} = ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_TAG_ID} " +
                    "WHERE ${EntryTagLinks.TABLE_NAME}.${EntryTagLinks.COLUMN_NAME_ENTRY_ID} = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor != null)
                while (cursor.moveToNext())
                    tags.add(Tag(dbManager, cursor))
        }
        return tags
    }

    fun set(name: String, tags: List<Tag>, dates: List<Date>, contents: String) {
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
        val tags1 = LinkedList(tags)
        val tagsOld = getTags().stream().collect(Collectors.toMap(Tag::id) { it })
        tags1.removeIf {
            if (it.exists())
                return@removeIf false
            it.create()
            addTag(it.id)
            return@removeIf true
        }
        for (tag in tags1) {
            if (tagsOld.containsKey(tag.id)) {
                tagsOld.remove(tag.id)
            } else {
                addTag(tag.id)
            }
        }
        for (t in tagsOld.keys) {
            rmTag(t)
        }

        //setting dates
        val dates1 = LinkedList(dates)
        val datesOld = this.getDates().stream().collect(
            Collectors.toMap({ it.id }, { it })
        )
        dates1.removeIf {
            if (it.exists())
                return@removeIf false
            it.createOrUpdate()
            return@removeIf true
        }
        for (date in dates1) { //todo incorrect date modification behavior? cannot check yet (UI does not exist)
            if (datesOld.containsKey(date.id)) {
                datesOld.remove(date.id)
            } else {
                date.createOrUpdate()
            }
        }
        for (d in datesOld.values) {
            d.delete()
        }

        // TODO: 10/26/24 set notifications
    }

    private fun update() {
        dbManager.writableDatabase.execSQL(
            "UPDATE $TABLE_NAME SET $COLUMN_NAME_NAME = ? WHERE $COLUMN_NAME_ID = ?", arrayOf(name, id.toString())
        )
    }

    private fun addTag(tagId: Int) {
        EntryTagLinks(dbManager, id, tagId).create()
    }

    private fun rmTag(tagId: Int) {
        EntryTagLinks(dbManager, id, tagId).delete()
    }

    companion object {
        //TODO not every entry requires 'contents'. create dir only when needed
        const val TABLE_NAME: String = "entries"
        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_NAME: String = "name"
        const val CONTENTS_FILENAME: String = "contents.md"
    }
}