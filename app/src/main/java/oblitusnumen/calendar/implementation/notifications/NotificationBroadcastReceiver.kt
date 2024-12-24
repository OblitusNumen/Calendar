package oblitusnumen.calendar.implementation.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import oblitusnumen.calendar.R
import java.time.ZonedDateTime
import kotlin.random.Random

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Build the notification using NotificationCompat.Builder
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(intent.getStringExtra(titleExtra)) // Set title from intent
            .setContentText(intent.getStringExtra(messageExtra)) // Set content text from intent
            .build()

        // Get the NotificationManager service
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Show the notification using the manager
        manager.notify(notificationID + Random.nextInt(100000), notification)
        scheduleNotification(context, "my awesome notif",
        "msg?? $i", ZonedDateTime.now().plusMinutes(30).toEpochSecond() * 1000)
        i++
    }

    companion object{
        const val notificationID = 121
        const val channelID = "channel1"
        const val titleExtra = "titleExtra"
        const val messageExtra = "messageExtra"
        @JvmStatic
        var i = 0
        fun scheduleNotification(c: Context, title: String, message: String, triggerAtMillis: Long) {
            // Create an intent for the Notification BroadcastReceiver
            val intent = Intent(c, NotificationBroadcastReceiver::class.java)

            // Add title and message as extras to the intent
            intent.putExtra(titleExtra, title)
            intent.putExtra(messageExtra, message)

            // Create a PendingIntent for the broadcast
            val pendingIntent = PendingIntent.getBroadcast(
                c,
                notificationID,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Get the AlarmManager service
            val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        fun createNotificationChannels(c: Context) {
            val name = "getString(R.string.channel_name)"
            val descriptionText = "getString(R.string.channel_description)"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }
}