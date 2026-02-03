package oblitusnumen.calendar.implementation.data

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import oblitusnumen.calendar.implementation.*
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver.Companion.LAST_NOTIFICATION_TIME_PREFERENCE_NAME
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver.Companion.scheduleNotification
import oblitusnumen.calendar.implementation.notifications.PendingNotification
import java.io.File
import java.time.ZoneId
import java.util.*
import kotlin.random.Random

class DbManager(val context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DATABASE_VERSION) {
    var defaultNotifications: List<Pair<Period, Boolean>> =
        getSharedPrefs(context).getString(DEFAULT_NOTIFICATIONS_PREF_NAME, null)?.split(",")?.map {
            val notification = it.split("_")
            Period.decode(notification[0]) to (notification[1] != "0")
        } ?: listOf(Period.Once() to true, Period.Minute(30) to true)
        set(notifications) {
            getSharedPrefs(context).edit {
                putString(
                    DEFAULT_NOTIFICATIONS_PREF_NAME,
                    notifications.joinToString(",") { it.first.toString() + "_" + it.second })
            }
            field = notifications
        }
    val filesDir: File = context.filesDir
    val contentResolver: ContentResolver = context.contentResolver
    var defaultEntryColor: Color =
        getSharedPrefs(context).getInt(DEFAULT_ENTRY_COLOR_PREF_NAME, -1).toColor() ?: Color.Gray
        set(color) {
            getSharedPrefs(context).edit { putInt(DEFAULT_ENTRY_COLOR_PREF_NAME, color.toInt()) }
            field = color
        }
    var defaultTagColor: Color = getSharedPrefs(context).getInt(DEFAULT_TAG_COLOR_PREF_NAME, -1).toColor() ?: Color.Gray
        set(color) {
            getSharedPrefs(context).edit { putInt(DEFAULT_TAG_COLOR_PREF_NAME, color.toInt()) }
            field = color
        }

    init {
        log(this)
        if (!filesDir.exists() && !filesDir.mkdirs())
            throw RuntimeException("could not create directory for data: $filesDir")
        val entries: List<Entry>
//        readableDatabase.rawQuery(
//            "SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.COLUMN_NAME_STATE} != ?",
//            arrayOf(Entry.STATE_NORMAL.toString())
//        ).use { cursor ->
//            entries = Entry.cursorToList(this, cursor)
//        }
//        for (entry in entries) {
//            entry.fixup()
//        }
        // FIXME:
    }

    fun finishApp() {
        (context as? Activity)?.finishAffinity()
    }

    fun tryScheduleNotification(now: Long = System.currentTimeMillis() / 1000) {
        val nextNotificationTime = getNextNotificationTime(now)
        getSharedPrefs(context).edit { putLong(LAST_NOTIFICATION_TIME_PREFERENCE_NAME, now) }
        if (nextNotificationTime != null)
            scheduleNotification(context, nextNotificationTime * 1000)
    }

    private fun getNextNotificationTime(timeStamp: Long): Long? {
        var notificationTime: Long? = null
        val dateCache: MutableMap<Int, List<Date>> = mutableMapOf()
        for (notification in getAllNotifications()) {
            val fromO = notification.offset.addTo(getZonedFromEpochSeconds(timeStamp), 1).toEpochSecond()
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
                        notification.offset.addTo(nextTime.withZoneSameInstant(defaultZoneId()), -1).toEpochSecond()
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
            val fromO = notification.offset.addTo(getZonedFromEpochSeconds(from), 1).toEpochSecond()
            val toO = notification.offset.addTo(getZonedFromEpochSeconds(to), 1).toEpochSecond()
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
                            notification.offset.addTo(dateTime.withZoneSameInstant(defaultZoneId()), -1)
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
        sqLiteDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS \"Entries\"\n" +
                    "(\n" +
                    "    \"id\"                INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    \"defaultOptionsId\"  INTEGER NOT NULL UNIQUE,\n" +
                    "    \"isTask\"            INTEGER NOT NULL,\n" +
                    "    FOREIGN KEY (\"defaultOptionsId\") REFERENCES \"EventOptions\" (\"id\")\n" +
                    ");"
        )
        sqLiteDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS \"EventOptions\"\n" +
                    "(\n" +
                    "    \"id\"          INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    \"entryId\"     INTEGER NOT NULL,\n" +
                    "    \"state\"       INTEGER NOT NULL,\n" +
                    "    \"name\"        TEXT    NOT NULL,\n" +
                    "    \"color\"       INTEGER NOT NULL,\n" +
                    "    FOREIGN KEY (\"entryId\") REFERENCES \"Entries\" (\"id\")\n" +
                    ");"
        )
        sqLiteDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS \"Dates\"\n" +
                    "(\n" +
                    "    \"id\"                    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    \"entryId\"               INTEGER NOT NULL,\n" +
                    "    \"eventOptionsId\"        INTEGER NOT NULL,\n" +
                    "    \"epochSecondChainStart\" BIGINT  NOT NULL,\n" +
                    "    \"duration\"              TEXT    NOT NULL,\n" +
                    "    \"epochSecondChainEnd\"   BIGINT  NOT NULL,\n" +
                    "    \"timesRepeat\"           INTEGER NOT NULL,\n" +
                    "    \"period\"                TEXT    NOT NULL,\n" +
                    "    \"timeZone\"              TEXT    NOT NULL,\n" +
                    "    \"exceptionRules\"        TEXT    NOT NULL,\n" +
                    "    FOREIGN KEY (\"entryId\") REFERENCES \"Entries\" (\"id\"),\n" +
                    "    FOREIGN KEY (\"eventOptionsId\") REFERENCES \"eventOptions\" (\"id\")\n" +
                    ");"
        )
        sqLiteDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS \"Tags\"\n" +
                    "(\n" +
                    "    \"id\"    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    \"name\"  TEXT    NOT NULL UNIQUE,\n" +
                    "    \"color\" INTEGER NOT NULL\n" +
                    ");"
        )
        sqLiteDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS \"EntryTagLinks\"\n" +
                    "(\n" +
                    "    \"entryId\" INTEGER NOT NULL,\n" +
                    "    \"tagId\"   INTEGER NOT NULL,\n" +
                    "    PRIMARY KEY (\"entryId\", \"tagId\"),\n" +
                    "    FOREIGN KEY (\"entryId\") REFERENCES \"Entries\" (\"id\"),\n" +
                    "    FOREIGN KEY (\"tagId\") REFERENCES \"Tags\" (\"id\")\n" +
                    ");"
        )

//        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES)
//        sqLiteDatabase.execSQL(SQL_CREATE_TAGS)
//        sqLiteDatabase.execSQL(SQL_CREATE_ENTRY_TAG_LINKS)
//        sqLiteDatabase.execSQL(SQL_CREATE_DATES)
//        sqLiteDatabase.execSQL(SQL_CREATE_NOTIFICATIONS)
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
                    "${Date.COLUMN_NAME_EPOCH_SECOND_CHAIN_START} < ? AND ${Date.COLUMN_NAME_EPOCH_SECOND_CHAIN_ENDS} >= ?",
            arrayOf(end.toString(), start.toString())
        ).use { cursor ->
            return Date.cursorToList(this, cursor)
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
            val tags = Tag.cursorToList(cursor)
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
//        readableDatabase.rawQuery("SELECT * FROM ${Notification.TABLE_NAME}", arrayOf()).use { cursor ->
//            return Notification.cursorToList(this, cursor)
//        }
        return listOf()
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

    fun fillDB() {
        val epochSecond = 1764432677
        repeat(500) {
            var contentValues = ContentValues()
            contentValues.put("id", it)
            contentValues.put("entryId", it)
            contentValues.put("eventOptionsId", it)
            contentValues.put("epochSecondChainStart", epochSecond + 86000 * it)
            contentValues.put("duration", Period.Hour(3).toString())
            contentValues.put("epochSecondChainEnd", epochSecond + 86000 * it + 86400 * 7 * 100)
            contentValues.put("timesRepeat", 101)
            contentValues.put("period", Period.Day(7).toString())
            contentValues.put("timeZone", ZoneId.systemDefault().toString())
            contentValues.put("exceptionRules", ExceptionRules().toString())
            writableDatabase.insert("Dates", null, contentValues)

            contentValues = ContentValues()
            contentValues.put("id", it)
            contentValues.put("entryId", it)
            contentValues.put("state", 0)
            contentValues.put("name", "" + it)
            contentValues.put("color", Color.Green.toInt())
            writableDatabase.insert("EventOptions", null, contentValues)
        }
        repeat(10) { tagId ->
            var contentValues = ContentValues()
            contentValues.put("id", tagId)
            contentValues.put("name", "tag $tagId")
            contentValues.put("color", presetColors[tagId].toInt())
            writableDatabase.insert("Tags", null, contentValues)

            val entries: MutableSet<Int> = mutableSetOf(0)
            while (entries.size < 70) {
                entries.add(Random.nextInt(500))
            }

            entries.forEach {
                contentValues = ContentValues()
                contentValues.put("tagId", tagId)
                contentValues.put("entryId", it)
                writableDatabase.insert("EntryTagLinks", null, contentValues)
            }
        }
    }

    companion object {
        const val DATABASE_VERSION: Int = 1
        private const val SHARED_PREFERENCES_NAME: String = "calendar_preferences"
        private const val DEFAULT_ENTRY_COLOR_PREF_NAME: String = "default_entry_color"
        private const val DEFAULT_TAG_COLOR_PREF_NAME: String = "default_tag_color"
        private const val DEFAULT_NOTIFICATIONS_PREF_NAME: String = "default_notifications"
        const val DB_NAME: String = "entries.db"
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS ${Entry.TABLE_NAME} (" +
                    "${Entry.COLUMN_NAME_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${Entry.COLUMN_NAME_STATE} INTEGER NOT NULL," +
                    "${Entry.COLUMN_NAME_NAME} TEXT NOT NULL," +
                    "${Entry.COLUMN_NAME_EXCLUDE_FROM_VIEW} INTEGER NOT NULL," +
                    "${Entry.COLUMN_NAME_COLOR} INTEGER NOT NULL);"
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
                    "${Date.COLUMN_NAME_EPOCH_SECOND_CHAIN_START} BIGINT NOT NULL," +
                    "${Date.COLUMN_NAME_DURATION} BIGINT NOT NULL," +
                    "${Date.COLUMN_NAME_EPOCH_SECOND_CHAIN_ENDS} BIGINT NOT NULL," +
                    "${Date.COLUMN_NAME_TIMES_REPEATS} INTEGER NOT NULL," +
                    "${Date.COLUMN_NAME_PERIOD} TEXT NOT NULL," +
                    "${Date.COLUMN_NAME_TIME_ZONE_ID} TEXT NOT NULL," +
                    "${Date.COLUMN_NAME_REMOVED} TEXT NOT NULL);"
        private const val SQL_CREATE_NOTIFICATIONS =
            "CREATE TABLE IF NOT EXISTS ${Notification.TABLE_NAME} (" +
                    "${Notification.COLUMN_NAME_ENTRY_ID} INTEGER NOT NULL," +
                    "${Notification.COLUMN_NAME_TIME_OFFSET} VARCHAR(18) NOT NULL," +
                    "${Notification.COLUMN_NAME_HAS_SOUND} INT NOT NULL);"
        private const val SATURATION = .5f
        private const val VALUE = .8f
        val presetColors: List<Color> = listOf(
            Color.hsv(0f, SATURATION, this.VALUE),
            Color.hsv(30f, SATURATION, this.VALUE),
            Color.hsv(60f, SATURATION, this.VALUE),
            Color.hsv(90f, SATURATION, this.VALUE),
            Color.hsv(120f, SATURATION, this.VALUE),
            Color.hsv(150f, SATURATION, this.VALUE),
            Color.hsv(180f, SATURATION, this.VALUE),
            Color.hsv(210f, SATURATION, this.VALUE),
            Color.hsv(240f, SATURATION, this.VALUE),
            Color.hsv(270f, SATURATION, this.VALUE),
            Color.hsv(300f, SATURATION, this.VALUE),
            Color.hsv(330f, SATURATION, this.VALUE),
            Color.Gray
        )

        fun getSharedPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }
}