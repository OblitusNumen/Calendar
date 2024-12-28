package oblitusnumen.calendar.implementation.data

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver.Companion.scheduleNotification
import oblitusnumen.calendar.implementation.notifications.PendingNotification
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt
import java.io.File
import java.util.*

class DbManager(private val context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DATABASE_VERSION) {
    val filesDir: File = context.filesDir
    var defaultTagColor: Color = getSharedPrefs(context).getInt(DEFAULT_COLOR_PREF_NAME, -1).toColor() ?: Color.Gray
        set(color) {
            getSharedPrefs(context).edit().putInt(DEFAULT_COLOR_PREF_NAME, color.toInt()).apply()
            field = color
        }

    init {
        if (!filesDir.exists() && !filesDir.mkdirs())
            throw RuntimeException("could not create directory for data: $filesDir")
        val entries: List<Entry>
        readableDatabase.rawQuery(
            "SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.COLUMN_NAME_STATE} != ?",
            arrayOf(Entry.STATE_NORMAL.toString())
        ).use { cursor ->
            entries = Entry.cursorToList(this, cursor)
        }
        for (entry in entries) {
            entry.fixup()
        }
    }

    fun tryScheduleNotification(now: Long = System.currentTimeMillis() / 1000) {
        val nextNotificationTime = getNextNotificationTime(now)
        if (nextNotificationTime != null) scheduleNotification(context, nextNotificationTime * 1000)
    }

    private fun getNextNotificationTime(timeStamp: Long): Long? {
        var notificationTime: Long? = null
        val dateCache: MutableMap<Int, List<Date>> = mutableMapOf()
        for (notification in getAllNotifications()) {
            val fromO = notification.offset.getTime(getZonedFromEpochSeconds(timeStamp), 1).toEpochSecond() //15us
            val dates = dateCache.computeIfAbsent(notification.entryId!!) {
                Date.getAllByEntryId(
                    this,
                    notification.entryId!!
                )
            }
            for (date in dates) {
                val nextTime = date.getNext(fromO)
                if (nextTime != null) {
                    val nnt =
                        notification.offset.getTime(nextTime.withZoneSameInstant(defaultZoneId()), -1).toEpochSecond()
                    if (notificationTime == null || notificationTime > nnt) notificationTime = nnt
                }
            }
        }
        return notificationTime
    }

    fun getPendingNotificationsInRange(from: Long, to: Long): List<PendingNotification> {
        val notifications: MutableList<PendingNotification> = ArrayList()
        val dateCache: MutableMap<Int, List<Date>> = mutableMapOf()
        for (notification in getAllNotifications()) {
            val fromO = notification.offset.getTime(getZonedFromEpochSeconds(from), 1).toEpochSecond()
            val toO = notification.offset.getTime(getZonedFromEpochSeconds(to), 1).toEpochSecond()
            val dates = dateCache.computeIfAbsent(notification.entryId!!) {
                Date.getAllByEntryId(
                    this,
                    notification.entryId!!
                )
            }
            for (date in dates) {
                for (dateTime in date.getAllInRange(fromO, toO)) {
                    notifications.add(
                        PendingNotification(
                            date,
                            notification,
                            notification.offset.getTime(dateTime.withZoneSameInstant(defaultZoneId()), -1)
                                .toEpochSecond(),
                            dateTime.toEpochSecond()
                        )
                    )
                }
            }
        }
        dedupeAndSort(notifications)
        return notifications
    }

    private fun dedupeAndSort(notifications: MutableList<PendingNotification>) {
        Objects.hash()
        val dedupeMap: MutableMap<Int, PendingNotification> = HashMap<Int, PendingNotification>()
        for (notification in notifications) {
            val get = dedupeMap[notification.dateHash()]
            if (get == null || get < notification) dedupeMap[notification.dateHash()] = notification
        }
        notifications.clear()
        notifications.addAll(dedupeMap.values)
        notifications.sort()
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

    fun getAllTagsWithEntryCount(): Map<Tag, Int> {
        readableDatabase.rawQuery(
            "SELECT t.*, count(l.${EntryTagLinks.COLUMN_NAME_ENTRY_ID}) as \"entryCount\" " +
                    "FROM ${Tag.TABLE_NAME} as t " +
                    "LEFT JOIN ${EntryTagLinks.TABLE_NAME} as l " +
                    "ON l.${EntryTagLinks.COLUMN_NAME_TAG_ID} = t.${Tag.COLUMN_NAME_ID} " +
                    "GROUP BY t.${Tag.COLUMN_NAME_ID}", arrayOf()
        ).use { cursor ->
            val result = HashMap<Tag, Int>()
            val tags = Tag.cursorToList(this, cursor)
            val entryCountIdx = cursor.getColumnIndex("entryCount")
            cursor.moveToFirst()
            for (t in tags) {
                result[t] = cursor.getInt(entryCountIdx)
                cursor.moveToNext()
            }
            return result
        }
    }

    private fun getAllNotifications(): List<Notification> {
        readableDatabase.rawQuery("SELECT * FROM ${Notification.TABLE_NAME}", arrayOf()).use { cursor ->
            return Notification.cursorToList(this, cursor)
        }
    }

    fun getEntryById(id: Int): Entry? {
        readableDatabase.rawQuery(
            "SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.COLUMN_NAME_ID} = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            val entries = Entry.cursorToList(this, cursor)
            return if (entries.isEmpty()) null else entries[0]
        }
    }

    fun getEntries(): List<Entry> {
        readableDatabase.rawQuery("SELECT * FROM ${Entry.TABLE_NAME}", arrayOf()).use { cursor ->
            return Entry.cursorToList(this, cursor)
        }
    }

    companion object {
        const val DATABASE_VERSION: Int = 1
        private const val SHARED_PREFERENCES_NAME: String = "calendar_preferences"
        private const val DEFAULT_COLOR_PREF_NAME: String = "default_tag_color"
        const val DB_NAME: String = "entries.db"
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS ${Entry.TABLE_NAME} (" +
                    "${Entry.COLUMN_NAME_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${Entry.COLUMN_NAME_NAME} TEXT NOT NULL," +
                    "${Entry.COLUMN_NAME_STATE} INTEGER NOT NULL);"
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
                    "${Notification.COLUMN_NAME_TIME_OFFSET} VARCHAR(18) NOT NULL," +
                    "${Notification.COLUMN_NAME_SOUND} INT NOT NULL);"

        fun getSharedPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }
}