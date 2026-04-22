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
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.views.ViewNotificationDateWithOptions
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver.Companion.LAST_NOTIFICATION_TIME_PREFERENCE_NAME
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver.Companion.scheduleMorningNotification
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver.Companion.scheduleNotification
import oblitusnumen.calendar.implementation.notifications.PendingNotification
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
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
    var morningNotificationHour: Int = getSharedPrefs(context).getInt(MORNING_NOTIFICATION_HOUR_PREF, 9)
        set(value) {
            getSharedPrefs(context).edit { putInt(MORNING_NOTIFICATION_HOUR_PREF, value) }
            field = value
            tryScheduleMorningNotification()
        }
    var morningNotificationMinute: Int = getSharedPrefs(context).getInt(MORNING_NOTIFICATION_MINUTE_PREF, 0)
        set(value) {
            getSharedPrefs(context).edit { putInt(MORNING_NOTIFICATION_MINUTE_PREF, value) }
            field = value
            tryScheduleMorningNotification()
        }

    // FIXME:
    val defaultTaskColor: Color
        get() = defaultEntryColor

    init {
        log(this)
        if (!filesDir.exists() && !filesDir.mkdirs())
            throw RuntimeException("could not create directory for data: $filesDir")
        // FIXME: resolve inconsistencies with db and files
//        val entries: List<Entry>
//        readableDatabase.rawQuery(
//            "SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.COLUMN_NAME_STATE} != ?",
//            arrayOf(Entry.STATE_NORMAL.toString())
//        ).use { cursor ->
//            entries = Entry.cursorToList(this, cursor)
//        }
//        for (entry in entries) {
//            entry.fixup()
//        }
        // FIXME: for debug
//        fillTasksDB()
        tryScheduleMorningNotification()
    }

    fun finishApp() {
        (context as? Activity)?.finishAffinity()
    }

    fun tryScheduleMorningNotification(now: Long = System.currentTimeMillis() / 1000) {
        val nowZdt = ZonedDateTime.now(defaultZoneId())
        var next = nowZdt.withHour(morningNotificationHour).withMinute(morningNotificationMinute)
            .withSecond(0).withNano(0)
        if (!next.isAfter(nowZdt))
            next = next.plusDays(1)
        scheduleMorningNotification(context, next.toInstant().toEpochMilli())
    }

    fun tryScheduleNotification(now: Long = System.currentTimeMillis() / 1000) {
        val nextNotificationTime = getNextNotificationTime(now)
        getSharedPrefs(context).edit { putLong(LAST_NOTIFICATION_TIME_PREFERENCE_NAME, now) }
        if (nextNotificationTime != null)
            scheduleNotification(context, nextNotificationTime * 1000)
    }

    private fun getNextNotificationTime(timeStamp: Long): Long? {
        var notificationTime: Long? = null

        for (notification in ViewNotificationDateWithOptions.all(this)) {
            val fromO = notification.offset.addTo(getZonedFromEpochSeconds(timeStamp), 1).toEpochSecond()
            val nnt = notification.nextNotificationTime(fromO)
            if (notificationTime == null || nnt != null && notificationTime > nnt)
                notificationTime = nnt
        }

        return notificationTime
    }

    fun getPendingNotificationsInRange(from: Long, to: Long): List<PendingNotification> {
        val notifications: MutableList<PendingNotification> = ArrayList()

        for (notification in ViewNotificationDateWithOptions.all(this)) {
            val fromO = notification.offset.addTo(getZonedFromEpochSeconds(from), 1).toEpochSecond()
            val toO = notification.offset.addTo(getZonedFromEpochSeconds(to), 1).toEpochSecond()

            for (dateTime in notification.getAllInRange(fromO, toO)) {
                notifications.add(
                    PendingNotification(
                        notification,
                        notification.offset.addTo(dateTime.withZoneSameInstant(defaultZoneId()), -1)
                            .toEpochSecond(),
                        dateTime.toEpochSecond()
                    )
                )
            }
        }

        return dedupeAndSort(notifications)
    }

    private fun dedupeAndSort(notifications: List<PendingNotification>): List<PendingNotification> {
        val dedupeMap: MutableMap<Int, PendingNotification> = HashMap<Int, PendingNotification>()

        for (notification in notifications) {
            val get = dedupeMap[notification.dateHash()]
            if (get == null || get < notification)
                dedupeMap[notification.dateHash()] = notification
        }

        return dedupeMap.values.sorted()
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(Entry.SQL_CREATE)
        sqLiteDatabase.execSQL(Tag.SQL_CREATE)
        sqLiteDatabase.execSQL(EntryTagLinks.SQL_CREATE)
        sqLiteDatabase.execSQL(EventOptions.SQL_CREATE)
        sqLiteDatabase.execSQL(Date.SQL_CREATE)
        sqLiteDatabase.execSQL(Notification.SQL_CREATE)
        sqLiteDatabase.execSQL(Task.SQL_CREATE)
        sqLiteDatabase.execSQL(TaskLink.SQL_CREATE)
        sqLiteDatabase.execSQL(TaskLog.SQL_CREATE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2)
            sqLiteDatabase.execSQL(
                "ALTER TABLE ${TaskLog.TABLE_NAME} ADD COLUMN ${TaskLog.COLUMN_NAME_TIME_PLANNED} INTEGER NOT NULL DEFAULT 0"
            )
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException()
    }

    fun fillTasksDB() {
        val isEmpty = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${Task.TABLE_NAME}", arrayOf()
        ).use { cursor -> cursor.moveToFirst(); cursor.getInt(0) == 0 }
        if (!isEmpty) return

        val (algoTasks, algoLinks) = readAll1()
        val nowEpoch = System.currentTimeMillis() / 1000L
        val random = Random(100L)
        val indexToEntryId = mutableMapOf<Int, Int>()

        algoTasks.forEach { algoTask ->
            val options = EventOptions(
                name = "task ${algoTask.index}",
                color = defaultTaskColor,
            )
            options.createWithTransaction(this) { optionsId ->
                val entryValues = ContentValues().apply {
                    put(Entry.COLUMN_NAME_DEFAULT_OPTIONS_ID, optionsId)
                    put(Entry.COLUMN_NAME_IS_TASK, 1)
                }
                val entryId = insert(Entry.TABLE_NAME, null, entryValues).toInt()

                val taskValues = ContentValues().apply {
                    put(Task.COLUMN_NAME_ENTRY_ID, entryId)
                    put(Task.COLUMN_NAME_START_CONSTRAINT_TIMESTAMP, nowEpoch + algoTask.startLimit * 86400L)
                    put(Task.COLUMN_NAME_DEADLINE_TIMESTAMP, nowEpoch + algoTask.endLimit * 86400L)
                    put(Task.COLUMN_NAME_TIME_ZONE_ID, ZoneId.systemDefault().toString())
                    put(Task.COLUMN_NAME_TIME_CONSUMED, random.nextInt(100))
                    put(Task.COLUMN_NAME_TIME_REMAINING, maxOf(0, random.nextInt(100) - 50))
                }
                insert(Task.TABLE_NAME, null, taskValues)

                indexToEntryId[algoTask.index] = entryId
            }
        }

        algoLinks.forEach { link ->
            val predId = indexToEntryId[link.predecessor] ?: return@forEach
            val sucId = indexToEntryId[link.successor] ?: return@forEach
            TaskLink.create(this, predId, sucId)
        }
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

    fun fillDbFromStrings() {
        val links = ""

        val tags = ""

        val notifications = ""

        val dates = ""

        val options = ""

        val entries = ""

        entries.lines().forEach {
            if (it.isEmpty())
                return@forEach
            val line = it.split("\t")
            log(line)
            val contentValues = ContentValues()
            contentValues.put("id", line[0].toInt())
            contentValues.put("defaultOptionsId", line[1].toInt())
            contentValues.put("isTask", line[2].toInt())
            writableDatabase.insert("Entries", null, contentValues)
        }

        options.lines().forEach {
            if (it.isEmpty())
                return@forEach
            val line = it.split("\t")
            val contentValues = ContentValues()
            contentValues.put("id", line[0].toInt())
            contentValues.put("state", line[1].toInt())
            contentValues.put("name", line[2])
            contentValues.put("color", line[3].toInt())
            writableDatabase.insert("EventOptions", null, contentValues)
        }

        dates.lines().forEach {
            if (it.isEmpty())
                return@forEach
            val line = it.split("\t")
            val contentValues = ContentValues()
            contentValues.put("id", line[0].toInt())
            contentValues.put("entryId", line[1].toInt())
            contentValues.put("eventOptionsId", line[2].toInt())
            contentValues.put("epochSecondChainStart", line[3].toLong())
            contentValues.put("duration", line[4])
            contentValues.put("epochSecondChainEnd", line[5].toLong())
            contentValues.put("timesRepeat", line[6].toInt())
            contentValues.put("period", line[7])
            contentValues.put("timeZoneId", line[8])
            contentValues.put("occurrenceExceptions", line[9])
            writableDatabase.insert("Dates", null, contentValues)
        }

        notifications.lines().forEach {
            if (it.isEmpty())
                return@forEach
            val line = it.split("\t")
            val contentValues = ContentValues()
            contentValues.put("eventOptionsId", line[0].toInt())
            contentValues.put("timeOffset", line[1])
            contentValues.put("hasSound", line[2].toInt())
            writableDatabase.insert("Notifications", null, contentValues)
        }

        tags.lines().forEach {
            if (it.isEmpty())
                return@forEach
            val line = it.split("\t")
            val contentValues = ContentValues()
            contentValues.put("id", line[0].toInt())
            contentValues.put("name", line[1])
            contentValues.put("color", line[2].toInt())
            writableDatabase.insert("Tags", null, contentValues)
        }

        links.lines().forEach {
            if (it.isEmpty())
                return@forEach
            val line = it.split("\t")
            val contentValues = ContentValues()
            contentValues.put("entryId", line[0].toInt())
            contentValues.put("tagId", line[1].toInt())
            writableDatabase.insert("EntryTagLinks", null, contentValues)
        }
    }

    companion object {
        const val DATABASE_VERSION: Int = 2
        const val DB_NAME: String = "entries.db"

        private const val SHARED_PREFERENCES_NAME: String = "calendar_preferences"
        private const val DEFAULT_ENTRY_COLOR_PREF_NAME: String = "default_entry_color"
        private const val DEFAULT_TAG_COLOR_PREF_NAME: String = "default_tag_color"
        private const val DEFAULT_NOTIFICATIONS_PREF_NAME: String = "default_notifications"
        private const val MORNING_NOTIFICATION_HOUR_PREF: String = "morning_notification_hour"
        private const val MORNING_NOTIFICATION_MINUTE_PREF: String = "morning_notification_minute"

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