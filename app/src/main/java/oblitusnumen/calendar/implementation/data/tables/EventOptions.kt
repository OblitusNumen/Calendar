package oblitusnumen.calendar.implementation.data.tables

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.compose.ui.graphics.Color
import androidx.core.database.sqlite.transaction
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.rmRecursively
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class EventOptions : BaseColumns {
    var id: Int?
        private set
    private var state: Int
    val name: String
    val color: Color?
    val contents: String?
    var contentsCache: String? = null
        private set

    constructor(
        id: Int? = null,
        state: Int = STATE_NEW,
        name: String = "",
        color: Color? = null,
        contents: String? = null
    ) {
        this.state = state
        this.name = name
        this.color = color
        this.id = id
        this.contents = contents
    }

    private fun getContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_NAME_STATE, state)
        contentValues.put(COLUMN_NAME_NAME, name)
        contentValues.put(COLUMN_NAME_COLOR, color.toInt())
        return contentValues
    }

    fun isCreated() = id != null

    fun isNotCreated() = id == null

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

    @Throws(IOException::class)
    fun createWithTransaction(dbManager: DbManager, onCreatedTransaction: (id: Int) -> Unit) {
        if (isCreated())
            throw IllegalStateException("alreadyCreated")

        val contentValues = getContentValues()
        contentValues.put(COLUMN_NAME_ID, null as Int?)
        id = dbManager.writableDatabase.insert(TABLE_NAME, null, contentValues).toInt()

        if (!getDirectory(dbManager).mkdirs() || !getContentsFile(dbManager).createNewFile()) {
            this.id = null
            try {
                if (getDirectory(dbManager).exists()) rmRecursively(getDirectory(dbManager))
                dbManager.writableDatabase.execSQL(
                    "DELETE FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
                    arrayOf(id.toString())
                )
            } catch (_: Exception) {
            }
            throw IOException("could not setup directory for entry $id, filename: ${getDirectory(dbManager)}")
        }

        writeContents(getContentsFile(dbManager))

        dbManager.writableDatabase.transaction {
            updateState(dbManager, STATE_NORMAL)

            onCreatedTransaction(id!!)
        }
    }

    fun updateWithTransaction(dbManager: DbManager, updateTransaction: () -> Unit) {
        normalizeFiles(dbManager)

        updateState(dbManager, STATE_UPDATING)

        writeContents(getTempContentsFile(dbManager))

        dbManager.writableDatabase.transaction {
            updateState(dbManager, STATE_NORMAL)

            updateDbRecord(dbManager)
            updateTransaction()
        }

        normalizeFiles(dbManager)
    }

    fun deleteWithTransaction(dbManager: DbManager, deleteTransaction: () -> Unit) {
        dbManager.writableDatabase.transaction {
            deleteTransaction()
            updateState(dbManager, STATE_DELETED)
        }

        if (getDirectory(dbManager).exists())
            getDirectory(dbManager).deleteRecursively()

        delete(dbManager)
    }

    fun getContents(dbManager: DbManager): String {
        if (contents == null && contentsCache == null)
            contentsCache = id?.let { contentsById(dbManager, id!!) } ?: ""
        return contents ?: contentsCache!!
    }

    private fun updateState(dbManager: DbManager, state: Int = this.state) {
        this.state = state
        dbManager.writableDatabase.update(
            TABLE_NAME,
            ContentValues().also { it.put(COLUMN_NAME_STATE, state) },
            "$COLUMN_NAME_ID = ?",
            arrayOf(id.toString())
        )
    }

    private fun updateDbRecord(dbManager: DbManager) {
        dbManager.writableDatabase.update(TABLE_NAME, getContentValues(), "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
    }

    private fun writeContents(file: File) {// FIXME: atomic saves
        if (contents == null) return

        try {
            FileOutputStream(file).use { fos ->
                fos.write(contents.toByteArray())
                fos.flush()
                fos.fd.sync() // FIXME: is this necessary?
            }
        } catch (e: IOException) {
            throw IOException("could not save contents for options $id", e)
        }
    }

    private fun normalizeFiles(dbManager: DbManager) = normalizeFilesById(dbManager, id!!)

    private fun delete(dbManager: DbManager) {
        dbManager.writableDatabase.delete(TABLE_NAME, "$COLUMN_NAME_ID = ?", arrayOf(id.toString()))
        id = null
    }

    private fun getDirectory(dbManager: DbManager) = directoryById(dbManager, id!!)

    private fun getTempContentsFile(dbManager: DbManager) = tempContentsFileById(dbManager, id!!)

    private fun getContentsFile(dbManager: DbManager) = contentsFileById(dbManager, id!!)

    companion object {
        const val TABLE_NAME = "EventOptions"

        const val COLUMN_NAME_ID: String = "id"
        const val COLUMN_NAME_STATE: String = "state"
        const val COLUMN_NAME_NAME: String = "name"
        const val COLUMN_NAME_COLOR: String = "color"

        const val SQL_CREATE: String = "CREATE TABLE IF NOT EXISTS \"${TABLE_NAME}\"\n" +
                "(\n" +
                "    \"$COLUMN_NAME_ID\"        INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    \"$COLUMN_NAME_STATE\"     INTEGER NOT NULL,\n" +
                "    \"$COLUMN_NAME_NAME\"      TEXT    NOT NULL,\n" +
                "    \"$COLUMN_NAME_COLOR\"     INTEGER NOT NULL\n" +
                ");"

        const val STATE_NORMAL: Int = 0
        const val STATE_NEW: Int = 1
        const val STATE_UPDATING: Int = 2
        const val STATE_DELETED: Int = 3
        const val CONTENTS_FILENAME: String = "contents.md"
        const val TEMP_CONTENTS_FILENAME: String = "$CONTENTS_FILENAME.new"

        fun cursorToList(cursor: Cursor): MutableList<EventOptions> {
            val options: MutableList<EventOptions> = ArrayList()

            val idIdx = cursor.getColumnIndex(COLUMN_NAME_ID)
            val stateIdx = cursor.getColumnIndex(COLUMN_NAME_STATE)
            val nameIdx = cursor.getColumnIndex(COLUMN_NAME_NAME)
            val colorIdx = cursor.getColumnIndex(COLUMN_NAME_COLOR)

            while (cursor.moveToNext())
                options.add(
                    EventOptions(
                        cursor.getInt(idIdx),
                        cursor.getInt(stateIdx),
                        cursor.getString(nameIdx),
                        cursor.getInt(colorIdx).toColor()
                    )
                )
            return options
        }

        fun byId(dbManager: DbManager, id: Int): EventOptions? {
            dbManager.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME_ID = ?",
                arrayOf(id.toString())
            ).use { cursor ->
                val entries = cursorToList(cursor)
                return if (entries.isEmpty()) null else entries[0]
            }
        }

        fun directoryById(dbManager: DbManager, id: Int): File = File(dbManager.filesDir, id.toString())

        fun tempContentsFileById(dbManager: DbManager, id: Int): File =
            File(directoryById(dbManager, id), TEMP_CONTENTS_FILENAME)

        fun contentsFileById(dbManager: DbManager, id: Int): File =
            File(directoryById(dbManager, id), CONTENTS_FILENAME)

        private fun normalizeFilesById(dbManager: DbManager, id: Int) {
            val tempFile = tempContentsFileById(dbManager, id)
            val contentsFile = contentsFileById(dbManager, id)

            if (tempFile.exists()) {
                if (contentsFile.exists())
                    contentsFile.delete()
                tempFile.renameTo(contentsFile)
            }
        }

        fun contentsById(dbManager: DbManager, id: Int): String { //todo contents should not be a just a string
            normalizeFilesById(dbManager, id)

            val contentsFile = contentsFileById(dbManager, id)
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
    }
}