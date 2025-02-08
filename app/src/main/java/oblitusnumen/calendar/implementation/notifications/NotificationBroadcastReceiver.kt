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
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.setLogFile
import java.time.format.DateTimeFormatter

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent?) {
        setLogFile(c)
        log("NotificationBroadcastReceiver RECEIVED INTENT: ${intent?.action}")
        DbManager(c).use { dbManager ->
            val now = System.currentTimeMillis() / 1000 + 10// fixing possible early invocation
            val sharedPreferences: SharedPreferences = DbManager.getSharedPrefs(c)
            val pendingNotifications = dbManager.getPendingNotificationsInRange(
                sharedPreferences.getLong(
                    LAST_NOTIFICATION_TIME_PREFERENCE_NAME,
                    now
                ), now
            )
            val manager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (pendingNotification in pendingNotifications) {
                log("NotificationBroadcastReceiver SENDING_NOTIFICATION " + pendingNotification.notification.entryId + ":" + pendingNotification.notification.offset)
                val notification = NotificationCompat.Builder(
                    c,
                    if (pendingNotification.notification.sound) NORMAL_CHANNEL_ID else SILENT_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(pendingNotification.date.getDesc())
                    .setContentText(// FIXME: format, missed events
                        "Upcoming event in ${pendingNotification.notification.offset.count} ${pendingNotification.notification.offset.javaClass.simpleName}\n(at ${
                            getZonedFromEpochSeconds(
                                pendingNotification.eventDateTime
                            ).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                        })"
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
        const val LAST_NOTIFICATION_TIME_PREFERENCE_NAME = "last_notification_time"

        fun scheduleNotification(c: Context, triggerAtMillis: Long, lastNotificationTimeSeconds: Long) {
            DbManager.getSharedPrefs(c).edit()
                .putLong(LAST_NOTIFICATION_TIME_PREFERENCE_NAME, lastNotificationTimeSeconds).apply()
            log(
                "NotificationBroadcastReceiver RESCHEDULING_NOTIFICATION AT ${triggerAtMillis / 1000} " +
                        "T=${System.currentTimeMillis() / 1000} " +
                        "D=${(triggerAtMillis - System.currentTimeMillis()) / 1000}"
            )
            val intent = Intent(c, NotificationBroadcastReceiver::class.java)
            intent.action = "scheduleNotification"
            val pendingIntent = PendingIntent.getBroadcast(
                c,
                12345,
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
                    "Normal",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    SILENT_CHANNEL_ID,
                    "Silent",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }
}