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
import oblitusnumen.calendar.implementation.data.tables.*
import oblitusnumen.calendar.implementation.data.tables.Date
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
            val dates = dateCache.computeIfAbsent(notification.eventOptionsId!!) {
                Date.byEntryId(
                    this,
                    notification.eventOptionsId!!
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
            val dates = dateCache.computeIfAbsent(notification.eventOptionsId!!) {
                Date.byEntryId(
                    this,
                    notification.eventOptionsId!!
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
        sqLiteDatabase.execSQL(Entry.SQL_CREATE)
        sqLiteDatabase.execSQL(Tag.SQL_CREATE)
        sqLiteDatabase.execSQL(EntryTagLinks.SQL_CREATE)
        sqLiteDatabase.execSQL(EventOptions.SQL_CREATE)
        sqLiteDatabase.execSQL(Date.SQL_CREATE)
        sqLiteDatabase.execSQL(Notification.SQL_CREATE)
        sqLiteDatabase.execSQL(Task.SQL_CREATE)
        sqLiteDatabase.execSQL(TaskDependencies.SQL_CREATE)
        sqLiteDatabase.execSQL(TaskLog.SQL_CREATE)
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
                    "${Date.COLUMN_NAME_EPOCH_SECOND_CHAIN_START} < ? AND ${Date.COLUMN_NAME_EPOCH_SECOND_CHAIN_END} >= ?",
            arrayOf(end.toString(), start.toString())
        ).use { cursor ->
            return Date.cursorToList(cursor)
        }
    }

    private fun getAllNotifications(): List<Notification> {
//        readableDatabase.rawQuery("SELECT * FROM ${Notification.TABLE_NAME}", arrayOf()).use { cursor ->
//            return Notification.cursorToList(this, cursor)
//        }
        return listOf()
    }

    fun fillDB() {
        val epochSecond = 1764432677
        repeat(500) {
            var contentValues = ContentValues()
            contentValues.put("id", it)
            contentValues.put("defaultOptionsId", it)
            contentValues.put("isTask", 0)
            writableDatabase.insert("Entries", null, contentValues)

            contentValues = ContentValues()
            contentValues.put("id", it)
            contentValues.put("entryId", it)
            contentValues.put("eventOptionsId", it)
            contentValues.put("epochSecondChainStart", epochSecond + 86000 * it)
            contentValues.put("duration", Period.Hour(3).toString())
            contentValues.put("epochSecondChainEnd", epochSecond + 86000 * it + 86400 * 7 * 100)
            contentValues.put("timesRepeat", 101)
            contentValues.put("period", Period.Day(7).toString())
            contentValues.put("timeZoneId", ZoneId.systemDefault().toString())
            contentValues.put("occurrenceExceptions", ExceptionRules().toString())
            writableDatabase.insert("Dates", null, contentValues)

            contentValues = ContentValues()
            contentValues.put("id", it)
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
        const val DB_NAME: String = "entries.db"

        private const val SHARED_PREFERENCES_NAME: String = "calendar_preferences"
        private const val DEFAULT_ENTRY_COLOR_PREF_NAME: String = "default_entry_color"
        private const val DEFAULT_TAG_COLOR_PREF_NAME: String = "default_tag_color"
        private const val DEFAULT_NOTIFICATIONS_PREF_NAME: String = "default_notifications"

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