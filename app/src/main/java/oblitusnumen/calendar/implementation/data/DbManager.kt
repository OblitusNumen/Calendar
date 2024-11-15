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
        val dates: MutableList<Date> = ArrayList()
        readableDatabase.rawQuery(
            "SELECT * FROM ${Date.TABLE_NAME} WHERE " +
                    "${Date.COLUMN_NAME_TIME_START} < ? AND ${Date.COLUMN_NAME_TIME_ENDS} >= ?",
            arrayOf(end.toString(), start.toString())
        ).use { cursor ->
            if (cursor != null) {
                val idIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_ID)
                val entryIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_ENTRY_ID)
                val descIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_DESC)
                val timeStartIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_TIME_START)
                val durationIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_DURATION)
                val timeEndsIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_TIME_ENDS)
                val timesRepeatsIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_TIMES_REPEATS)
                val periodIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_PERIOD)
                val timeZoneIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_TIME_ZONE)
                val removedIdx: Int = cursor.getColumnIndex(Date.COLUMN_NAME_REMOVED)
                while (cursor.moveToNext())
                    dates.add(
                        Date(
                            this,
                            cursor.getInt(idIdx),
                            cursor.getInt(entryIdx),
                            cursor.getString(descIdx),
                            cursor.getLong(timeStartIdx),
                            cursor.getLong(durationIdx),
                            cursor.getLong(timeEndsIdx),
                            cursor.getInt(timesRepeatsIdx),
                            cursor.getString(periodIdx),
                            cursor.getString(timeZoneIdx),
                            cursor.getString(removedIdx)
                        )
                    )
            }
        }
        return dates
    }

    fun getAllTags(): List<Tag> {
        val tags: MutableList<Tag> = ArrayList()
        readableDatabase.rawQuery("SELECT * FROM ${Tag.TABLE_NAME}", arrayOf()).use { cursor ->
            if (cursor != null) {
                val idxId = cursor.getInt(cursor.getColumnIndex(Tag.COLUMN_NAME_ID))
                val idxName = cursor.getInt(cursor.getColumnIndex(Tag.COLUMN_NAME_NAME))
                val idxColor = cursor.getInt(cursor.getColumnIndex(Tag.COLUMN_NAME_COLOR))
                while (cursor.moveToNext())
                    tags.add(
                        Tag(
                            this, cursor.getInt(idxId),
                            cursor.getString(idxName),
                            cursor.getInt(idxColor)
                        )
                    )
            }
        }
        return tags
    }

    fun getEntries(): List<Entry> {
        val entries: MutableList<Entry> = ArrayList()
        readableDatabase.rawQuery("SELECT * FROM ${Entry.TABLE_NAME}", arrayOf()).use { cursor ->
            if (cursor != null) {
                val idxId = cursor.getColumnIndex(Entry.COLUMN_NAME_ID)
                val idxName = cursor.getColumnIndex(Entry.COLUMN_NAME_NAME)
                while (cursor.moveToNext())
                    entries.add(Entry(this, cursor.getInt(idxId), cursor.getString(idxName)))
            }
        }
        return entries
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
                    "${Notification.COLUMN_NAME_TIME_OFFSET} BIGINT NOT NULL);"
    }
}