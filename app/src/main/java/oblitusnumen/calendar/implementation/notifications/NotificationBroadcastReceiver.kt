package oblitusnumen.calendar.implementation.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.LocaleHelper
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.now
import oblitusnumen.calendar.implementation.planTasks
import oblitusnumen.calendar.implementation.setLogFile
import oblitusnumen.calendar.ui.displayCount
import java.time.format.DateTimeFormatter

class NotificationBroadcastReceiver : BroadcastReceiver() {
    // TODO: show missed events, show events that is in progress
    override fun onReceive(rawContext: Context, intent: Intent?) {
        setLogFile(rawContext)
        log("NotificationBroadcastReceiver RECEIVED INTENT: ${intent?.action}")
        val c = LocaleHelper.wrap(rawContext)
        DbManager(c).use { dbManager ->
            val now = now() + 10// fixing possible early invocation
            val manager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (intent?.action == MORNING_NOTIFICATION_ACTION) {
                val allTasks = ViewTaskWithOptions.all(dbManager)
                val links = TaskLink.all(dbManager)
                val planned = planTasks(
                    allTasks.filter { !it.isDone && it.deadlineTimestamp >= now }.toTypedArray(),
                    links,
                    now
                )
                val todayCount = planned.count { (_, dist) -> dist[0] > 0 }

                val notification = NotificationCompat.Builder(c, MORNING_TASK_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_calendar)
                    .setContentTitle(c.getString(R.string.notification_today_tasks_title, todayCount))
                    .setContentText(c.getString(R.string.notification_tap_to_view))
                    .setAutoCancel(true)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            c,
                            MORNING_NOTIFICATION_REQUEST_CODE,
                            Intent(c, MainActivity::class.java)
                                .putExtra(INTENT_EXTRA_PLANNER_TODAY, true),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                    .build()
                manager.notify(MORNING_NOTIFICATION_REQUEST_CODE, notification)
                dbManager.tryScheduleMorningNotification(now)
                return
            }

            val sharedPreferences: SharedPreferences = DbManager.getSharedPrefs(c)
            val pendingNotifications = dbManager.getPendingNotificationsInRange(
                sharedPreferences.getLong(
                    LAST_NOTIFICATION_TIME_PREFERENCE_NAME,
                    now
                ), now
            )
            for (pendingNotification in pendingNotifications) {
                log("NotificationBroadcastReceiver SENDING_NOTIFICATION " + pendingNotification.notification.eventOptionsId + ":" + pendingNotification.notification.offset)
                val notification = NotificationCompat.Builder(
                    c,
                    if (pendingNotification.notification.sound) NORMAL_CHANNEL_ID else SILENT_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.ic_calendar) // FIXME: this icon must be transparent
                    .setContentTitle("${pendingNotification.notification.name}")
                    .setContentIntent(
                        PendingIntent.getActivity(
                            c,
                            pendingNotification.notification.eventOptionsId!!,// FIXME: mb by entryId
                            Intent(c, MainActivity::class.java).putExtra(
                                INTENT_EXTRA_ENTRY_ID,
                                pendingNotification.notification.eventOptionsId!!
                            ),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                    .setContentText(// FIXME: format, missed events
                        c.getString(
                            R.string.notification_upcoming_event,
                            pendingNotification.notification.offset.displayCount(c),
                            getZonedFromEpochSeconds(pendingNotification.eventDateTime)
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                        )
                    )
                    .build()
                manager.notify(pendingNotification.dateHash(), notification)
            }
            dbManager.tryScheduleNotification(now)
        }
    }

    companion object {
        const val NORMAL_CHANNEL_ID = "normal_channel"
        const val SILENT_CHANNEL_ID = "silent_channel"
        const val MORNING_TASK_CHANNEL_ID = "morning_tasks_channel"
        const val LAST_NOTIFICATION_TIME_PREFERENCE_NAME = "last_notification_time"
        const val INTENT_EXTRA_ENTRY_ID = "entryId"
        const val INTENT_EXTRA_PLANNER_TODAY = "openPlannerToday"
        const val MORNING_NOTIFICATION_ACTION = "morningTaskNotification"
        const val NOTIFICATION_REQUEST_CODE = 12345
        const val MORNING_NOTIFICATION_REQUEST_CODE = 12346

        fun scheduleNotification(c: Context, triggerAtMillis: Long) {
            log(
                "NotificationBroadcastReceiver RESCHEDULING_NOTIFICATION AT ${triggerAtMillis / 1000} " +
                        "T=${System.currentTimeMillis() / 1000} " +
                        "D=${(triggerAtMillis - System.currentTimeMillis()) / 1000}"
            )
            val intent = Intent(c, NotificationBroadcastReceiver::class.java)
            intent.action = "scheduleNotification"
            val pendingIntent = PendingIntent.getBroadcast(
                c,
                NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        fun scheduleMorningNotification(c: Context, triggerAtMillis: Long) {
            log(
                "NotificationBroadcastReceiver SCHEDULING_MORNING_NOTIFICATION AT ${triggerAtMillis / 1000} " +
                        "T=${System.currentTimeMillis() / 1000} " +
                        "D=${(triggerAtMillis - System.currentTimeMillis()) / 1000}"
            )
            val intent = Intent(c, NotificationBroadcastReceiver::class.java)
            intent.action = MORNING_NOTIFICATION_ACTION
            val pendingIntent = PendingIntent.getBroadcast(
                c,
                MORNING_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        fun createNotificationChannels(c: Context) {
            val notificationManager: NotificationManager =
                c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NORMAL_CHANNEL_ID,
                    c.getString(R.string.notification_channel_normal),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    SILENT_CHANNEL_ID,
                    c.getString(R.string.notification_channel_silent),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    MORNING_TASK_CHANNEL_ID,
                    c.getString(R.string.notification_channel_morning),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }
}
