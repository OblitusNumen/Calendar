package oblitusnumen.calendar.implementation.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import oblitusnumen.calendar.implementation.zonedDateTime
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime

class DbManager(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DATABASE_VERSION) {
    val filesDir: File = context.filesDir

    init {
        if (!filesDir.exists() && !filesDir.mkdirs())
            throw RuntimeException("could not create directory for data: $filesDir")
    }

    fun getDates(start: ZonedDateTime, end: ZonedDateTime): List<Date> {
        return getDates(start.toEpochSecond(), end.toEpochSecond())
    }

    fun getDates(start: LocalDate, end: LocalDate): List<Date> {
        return getDates(zonedDateTime(start), zonedDateTime(end))
    }

    fun createEntry(): Entry {
        return Entry(this)
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES)
        sqLiteDatabase.execSQL(SQL_CREATE_TAGS)
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRY_TAG_LINKS)
        sqLiteDatabase.execSQL(SQL_CREATE_DATES)
        sqLiteDatabase.execSQL(SQL_CREATE_NOTIFICATIONS)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException()
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException()
    }

    fun getDates(start: Long, end: Long): List<Date> {
        require(end >= start) { "end must be more than start, got start=$start, end=$end" }
        readableDatabase.rawQuery(
            "SELECT * FROM ${Date.TABLE_NAME} WHERE " +
                    "${Date.COLUMN_NAME_TIME_START} < ? AND ${Date.COLUMN_NAME_TIME_ENDS} >= ?",
            arrayOf(end.toString(), start.toString())
        ).use { cursor ->
            return Date.cursorToList(this, cursor)
        }
    }

    fun getAllTags(): List<Tag> {
        readableDatabase.rawQuery("SELECT * FROM ${Tag.TABLE_NAME}", arrayOf()).use { cursor ->
            return Tag.cursorToList(this, cursor)
        }
    }

    fun getEntries(): List<Entry> {
        readableDatabase.rawQuery("SELECT * FROM ${Entry.TABLE_NAME}", arrayOf()).use { cursor ->
            return Entry.cursorToList(this, cursor)
        }
    }

    companion object {
        const val DATABASE_VERSION: Int = 1
        const val DB_NAME: String = "entries.db"
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS ${Entry.TABLE_NAME} (" +
                    "${Entry.COLUMN_NAME_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${Entry.COLUMN_NAME_NAME} TEXT NOT NULL);"
        private const val SQL_CREATE_TAGS =
            "CREATE TABLE IF NOT EXISTS ${Tag.TABLE_NAME} (" +
                    "${Tag.COLUMN_NAME_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${Tag.COLUMN_NAME_NAME} TEXT NOT NULL UNIQUE," +
                    "${Tag.COLUMN_NAME_COLOR} INTEGER NOT NULL);"
        private const val SQL_CREATE_ENTRY_TAG_LINKS =
            "CREATE TABLE IF NOT EXISTS ${EntryTagLinks.TABLE_NAME} (" +
                    "${EntryTagLinks.COLUMN_NAME_ENTRY_ID} INTEGER NOT NULL," +
                    "${EntryTagLinks.COLUMN_NAME_TAG_ID} INTEGER NOT NULL);"
        private const val SQL_CREATE_DATES =
            "CREATE TABLE IF NOT EXISTS ${Date.TABLE_NAME} (" +
                    "${Date.COLUMN_NAME_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${Date.COLUMN_NAME_ENTRY_ID} INTEGER NOT NULL," +
                    "${Date.COLUMN_NAME_DESC} TEXT NOT NULL," +
                    "${Date.COLUMN_NAME_TIME_START} BIGINT NOT NULL," +
                    "${Date.COLUMN_NAME_DURATION} BIGINT NOT NULL," +
                    "${Date.COLUMN_NAME_TIME_ENDS} BIGINT NOT NULL," +
                    "${Date.COLUMN_NAME_TIMES_REPEATS} INTEGER NOT NULL," +
                    "${Date.COLUMN_NAME_PERIOD} TEXT NOT NULL," +
                    "${Date.COLUMN_NAME_TIME_ZONE} TEXT NOT NULL," +
                    "${Date.COLUMN_NAME_REMOVED} TEXT NOT NULL);"
        private const val SQL_CREATE_NOTIFICATIONS =
            "CREATE TABLE IF NOT EXISTS ${Notification.TABLE_NAME} (" +
                    "${Notification.COLUMN_NAME_ENTRY_ID} INTEGER NOT NULL," +
                    "${Notification.COLUMN_NAME_TIME_OFFSET} BIGINT NOT NULL," +
                    "${Notification.COLUMN_NAME_SOUND} INT NOT NULL);"
    }
}