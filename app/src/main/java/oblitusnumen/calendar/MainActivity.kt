package oblitusnumen.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.LocaleHelper
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver
import oblitusnumen.calendar.implementation.rmRecursively
import oblitusnumen.calendar.implementation.setLogFile
import oblitusnumen.calendar.implementation.unzipFile
import oblitusnumen.calendar.ui.navigation.NavigationGraph
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) { //fixme ask for required permissions somewhere
        super.onCreate(savedInstanceState)

        log(this)

        // restore backup
        val appDataDir = filesDir.parentFile!!
        val stagedZip = File(appDataDir, "restore_staged.zip")
        if (stagedZip.exists()) {
            appDataDir.listFiles()!!.forEach { file -> if (file != stagedZip) rmRecursively(file) }
            try {
                unzipFile(stagedZip, appDataDir)
            } catch (e: Throwable) {
                log("Error while restoring ${e.message}")
            }
            stagedZip.delete()
            Toast.makeText(this, getString(R.string.toast_restore_finished), Toast.LENGTH_LONG).show()
            // optional: kill and restart app to reload clean state
//            android.os.Process.killProcess(android.os.Process.myPid())
        }

        setLogFile(this)
        enableEdgeToEdge()
        val startingEntryId: Int? =
            if (intent.hasExtra(NotificationBroadcastReceiver.INTENT_EXTRA_ENTRY_ID))
                intent.getIntExtra(NotificationBroadcastReceiver.INTENT_EXTRA_ENTRY_ID, -1)
            else
                null
        val openPlannerToday: Boolean =
            intent.getBooleanExtra(NotificationBroadcastReceiver.INTENT_EXTRA_PLANNER_TODAY, false)
        //if (startingEntryId != null)
        //(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(/* fixme find id somehow */)
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    log("isGranted")
                } else {
                    log("not granted")
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can use the API that requires the permission.
                    log("already granted")
                }
                // TODO:
                /*ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS) -> {
                    // In an educational UI, explain to the user why your app requires this
                    // permission for a specific feature to behave as expected, and what
                    // features are disabled if it's declined. In this UI, include a
                    // "cancel" or "no, thanks" button that lets the user continue
                    // using your app without granting the permission.
                    //showInContextUI(...)
                    log("shouldShowRequestPermissionRationale")
                }*/
                else -> {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    log("requestPermissionLauncher.launch")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        NotificationBroadcastReceiver.createNotificationChannels(this)
        NotificationBroadcastReceiver().onReceive(this, null)

        setContent {
            CalendarTheme {
                NavigationGraph(
                    rememberNavController(),
                    DbManager(this@MainActivity),
                    startingEntryId,
                    openPlannerToday
                )
            }
        }
    }
}
