package oblitusnumen.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import oblitusnumen.calendar.implementation.*
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver
import oblitusnumen.calendar.ui.element.screen.*
import oblitusnumen.calendar.ui.navigation.NavRoutes
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.io.File
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var calendarViewModel: CalendarViewModel? = null

    companion object {
        val PADDING: Dp = 5.dp
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
            Toast.makeText(this, "Restore finished", Toast.LENGTH_LONG).show()
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
                NavGraph(
                    rememberNavController(),
                    DbManager(this@MainActivity),
                    startingEntryId
                )
            }
        }
    }

    @Composable
    fun NavGraph(navController: NavHostController, dbManager: DbManager, startingEntryId: Int? = null) {
        val tagFilterSaver: Saver<MutableState<List<Tag>>, Bundle> = Saver(
            save = { tagFilter ->
                Bundle().apply {
                    val tagFilter = tagFilter.value
                    putInt("tagFilter", tagFilter.size)
                    var idx = 0
                    tagFilter.forEach { tag ->
                        putInt("tagFilter$idx.0", tag.id!!)
                        putString("tagFilter$idx.1", tag.name)
                        putInt("tagFilter$idx.2", tag.color.toInt())
                        idx++
                    }
                }
            },
            restore = { bundle ->
                val tagFilter = mutableListOf<Tag>()
                repeat(bundle.getInt("tagFilter", 0)) { idx ->
                    tagFilter.add(
                        Tag(
                            bundle.getString("tagFilter$idx.1")!!,
                            bundle.getInt("tagFilter$idx.0"),
                            bundle.getInt("tagFilter$idx.2").toColor()
                        )
                    )
                }
                mutableStateOf(tagFilter)
            }
        )
        val tagsFilter = rememberSaveable(saver = tagFilterSaver) { mutableStateOf(listOf()) }
        // FIXME: check the saveable

        NavHost(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination =
                if (startingEntryId == null)
                    NavRoutes.Calendar.route
                // FIXME:
//                    NavRoutes.Dashboard.route
                else
                    NavRoutes.EntryDetails.withArgs(startingEntryId.toString())
        ) {
            composable(route = NavRoutes.Dashboard.route) {
                DashboardScreen(
                    dbManager,
                    { DrawBottomBar(navController) },
                    { NavRoutes.EntryEdit.navHere(navController, null) },
                    { NavRoutes.ThatDayDetails.navHere(navController, it) },
                    { year, monthValue ->
                        NavRoutes.Agenda.navHere(navController, year, monthValue, null)
                        log("year: $year, monthValue: $monthValue")
                    },
                    { NavRoutes.Entries.navHere(navController) },
                    { NavRoutes.Tags.navHere(navController) },
                    { NavRoutes.Settings.navHere(navController) },
                )
            }

            composable(route = NavRoutes.Calendar.route) {
                CalendarScreen(
                    dbManager,
                    tagsFilter,
                    { DrawBottomBar(navController) },
//                    {},// FIXME:
//                    {dbManager.fillDB()},
                    { NavRoutes.EntryEdit.navHere(navController, null) },
                    { NavRoutes.ThatDayDetails.navHere(navController, it) },
                    { year, monthValue ->
                        NavRoutes.Agenda.navHere(navController, year, monthValue, null)
                        log("year: $year, monthValue: $monthValue")
                    },
                    { NavRoutes.Entries.navHere(navController) },
                    { NavRoutes.Tags.navHere(navController) },
                    { NavRoutes.Settings.navHere(navController) },
                )
            }

            composable(route = NavRoutes.Planner.route) {
                PlannerScreen(
                    dbManager,
                    tagsFilter,
                    { DrawBottomBar(navController) },
                    { NavRoutes.EntryEdit.navHere(navController, null) },
                    { NavRoutes.ThatDayDetails.navHere(navController, it) },
                    { year, monthValue ->
                        NavRoutes.Agenda.navHere(navController, year, monthValue, null)
                        log("year: $year, monthValue: $monthValue")
                    },
                    { NavRoutes.Entries.navHere(navController) },
                    { NavRoutes.Tags.navHere(navController) },
                    { NavRoutes.Settings.navHere(navController) },
                )
            }

            composable(route = NavRoutes.TaskDetails.route) {
                TaskDetailsScreen(
                    dbManager,
                    { DrawBottomBar(navController) },
                    { NavRoutes.EntryEdit.navHere(navController, null) },
                    { NavRoutes.ThatDayDetails.navHere(navController, it) },
                    { year, monthValue ->
                        NavRoutes.Agenda.navHere(navController, year, monthValue, null)
                        log("year: $year, monthValue: $monthValue")
                    },
                    { NavRoutes.Entries.navHere(navController) },
                    { NavRoutes.Tags.navHere(navController) },
                    { NavRoutes.Settings.navHere(navController) },
                )
            }

            composable(route = NavRoutes.Agenda.route) { navBackStackEntry -> // FIXME: resolve recurring stack
                val month = NavRoutes.Agenda.getArgs(navBackStackEntry)

                AgendaScreen(
                    dbManager,
                    month,
                    tagsFilter,
                    { DrawBottomBar(navController) },
                    {},// FIXME:
                    { NavRoutes.ThatDayDetails.navHere(navController, it) },
                    { NavRoutes.EntryDetails.navHere(navController, it.date.entryId) },
                    { NavRoutes.backPress(navController) }
                )
            }

            composable(route = NavRoutes.ThatDayDetails.route) { navBackStackEntry ->
                val thatDay = NavRoutes.ThatDayDetails.getArgs(navBackStackEntry) ?: LocalDate.now()

                DateScreen(
                    dbManager,
                    thatDay,
                    { year, monthValue, dayOfMonth ->
                        NavRoutes.Agenda.navHere(navController, year, monthValue, dayOfMonth)
                    },
                    { NavRoutes.EntryDetails.navHere(navController, it.date.entryId) },// FIXME:
                    { NavRoutes.EntryEdit.navHere(navController, null, thatDay) },
                    { NavRoutes.backPress(navController) }
                )
            }

            composable(route = NavRoutes.EntryEdit.route) { navBackStackEntry ->
                val entryId = NavRoutes.EntryEdit.getArgEntryId(navBackStackEntry)
                val fromDay = if (Entry.exists(dbManager, entryId ?: -1))
                    null
                else
                    NavRoutes.EntryEdit.getArgDate4new(navBackStackEntry)

                EditEntryScreen(dbManager, entryId, fromDay, { NavRoutes.backPress(navController) })
            }

            composable(route = NavRoutes.EntryDetails.route) { navBackStackEntry ->
                val entryId = NavRoutes.EntryDetails.getArgs(navBackStackEntry)
                if (entryId == null || !Entry.exists(dbManager, entryId)) {
                    NavRoutes.backPress(navController)
                    return@composable
                }

                DetailsEntryScreen(
                    dbManager,
                    entryId,
                    { NavRoutes.EntryEdit.navHere(navController, entryId) },
                    { NavRoutes.backPress(navController) }
                )
            }

            composable(route = NavRoutes.Entries.route) {
                EntriesScreen(
                    dbManager,
                    { DrawBottomBar(navController) },
                    { NavRoutes.EntryEdit.navHere(navController, null) },
                    { NavRoutes.EntryDetails.navHere(navController, it) },
                    { NavRoutes.backPress(navController) }
                )
            }

            composable(route = NavRoutes.Tags.route) {
                TagsScreen(
                    dbManager,
                    { DrawBottomBar(navController) },
                    { NavRoutes.TagEdit.navHere(navController, it) },
                    { NavRoutes.backPress(navController) }
                )
            }

            composable(route = NavRoutes.TagEdit.route) { navBackStackEntry ->
                val tagId = NavRoutes.TagEdit.getArgTagId(navBackStackEntry) ?: -1
                if (tagId < 0)
                    throw RuntimeException("cannot edit non-existing tag")

                TagEditScreen(
                    dbManager,
                    tagId,
                    { NavRoutes.EntryDetails.navHere(navController, it) },// FIXME:
//                    { NavRoutes.EntryEdit.navHere(navController, null) },
                    { NavRoutes.backPress(navController) }
                )
            }

            composable(route = NavRoutes.Settings.route) {
                SettingsScreen(dbManager, { NavRoutes.backPress(navController) })
            }
        }
    }

    @Composable
    fun DrawBottomBar(navController: NavController) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavRoutes.getTopLevelRoutes().forEach { topLevelRoute ->
                NavigationBarItem(
                    icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name) },
                    label = { Text(topLevelRoute.name) },
                    selected = currentDestination?.route == topLevelRoute.route.route ||
                            (topLevelRoute.route.route == NavRoutes.Calendar.route && currentDestination?.route == NavRoutes.ThatDayDetails.route),
                    onClick = {
                        navController.navigate(topLevelRoute.route.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                if (currentDestination?.route != NavRoutes.ThatDayDetails.route ||
                                    topLevelRoute.route.route != NavRoutes.Calendar.route
                                )
                                    saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        calendarViewModel?.dbManager?.close()
        super.onDestroy()
    }

    class CalendarViewModel(val dbManager: DbManager) : ViewModel()
}
