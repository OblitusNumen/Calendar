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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver
import oblitusnumen.calendar.implementation.rmRecursively
import oblitusnumen.calendar.implementation.setLogFile
import oblitusnumen.calendar.implementation.unzipFile
import oblitusnumen.calendar.ui.model.navigation.NavRoutes
import oblitusnumen.calendar.ui.model.screen.DateScreen
import oblitusnumen.calendar.ui.model.screen.EntryDetails
import oblitusnumen.calendar.ui.model.screen.EntryEdit
import oblitusnumen.calendar.ui.model.screen.SettingsScreen
import oblitusnumen.calendar.ui.model.tab.CalendarTab
import oblitusnumen.calendar.ui.model.tab.EntriesTab
import oblitusnumen.calendar.ui.model.tab.TagsTab
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
                calendarViewModel = viewModel { CalendarViewModel(DbManager(this@MainActivity)) }
                navGraph(
                    rememberNavController(),
                    calendarViewModel!!.dbManager,
                    startingEntryId
                )
            }
        }
    }

    @Composable
    fun navGraph(navController: NavHostController, dbManager: DbManager, startingEntryId: Int? = null) {
        NavHost(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination =
                if (startingEntryId == null)
                    NavRoutes.Calendar.route
                else
                    NavRoutes.EntryDetails.withArgs(startingEntryId.toString())
        ) {
            composable(route = NavRoutes.Calendar.route) {
                val calendarTab = viewModel { CalendarTab(dbManager) }
                Scaffold(
                    topBar = { calendarTab.topBar { NavRoutes.Settings.navHere(navController) } },
                    bottomBar = { drawBottomBar(navController) },
                    floatingActionButton = {
                        calendarTab.functionButton { NavRoutes.EntryEdit.navHere(navController, null) }
                    }) {
                    calendarTab.compose(
                        { NavRoutes.ThatDayDetails.navHere(navController, it) },
                        Modifier.padding(//seems like a hack
                            horizontal = PADDING
                        )
                    )
                }
            }

            composable(route = NavRoutes.ThatDayDetails.route) { navBackStackEntry ->
                val thatDay = NavRoutes.ThatDayDetails.getArgs(navBackStackEntry) ?: LocalDate.now()
                val dateScreen = viewModel { DateScreen(thatDay, dbManager) }
                Scaffold(
                    topBar = { dateScreen.topBar { NavRoutes.backPress(navController) } },
                    bottomBar = { drawBottomBar(navController) },
                    floatingActionButton = {
                        dateScreen.functionButton { NavRoutes.EntryEdit.navHere(navController, null, thatDay) }
                    }) {
                    dateScreen.compose(
                        { NavRoutes.EntryDetails.navHere(navController, it) },
                        Modifier.padding(//seems like a hack
                            horizontal = PADDING
                        )
                    )
                }
            }

            composable(route = NavRoutes.EntryEdit.route) { navBackStackEntry ->
                val entryId = NavRoutes.EntryEdit.getArgEntryId(navBackStackEntry)
                var fromDay = NavRoutes.EntryEdit.getArgDate4new(navBackStackEntry)
                val entryEdit = viewModel {
                    var entry = dbManager.getEntryById(entryId ?: -1)
                    if (entry == null) {
                        entry = Entry.new(dbManager)
                    } else
                        fromDay = null
                    EntryEdit(dbManager, entry, fromDay)
                }
                Scaffold(topBar = { entryEdit.topBar { NavRoutes.backPress(navController) } }) {
                    entryEdit.compose(
                        Modifier.padding(//seems like a hack
                            horizontal = PADDING
                        )
                    )
                }
            }

            composable(route = NavRoutes.EntryDetails.route) { navBackStackEntry ->
                val entryId = NavRoutes.EntryDetails.getArgs(navBackStackEntry)
                if (entryId == null) {
                    NavRoutes.backPress(navController)
                    return@composable
                }
                val entryDetails = viewModel { EntryDetails(dbManager, entryId) }
                Scaffold(topBar = {
                    entryDetails.topBar(
                        { NavRoutes.backPress(navController) },
                        { NavRoutes.EntryEdit.navHere(navController, it) })
                }) {
                    entryDetails.compose(
                        { NavRoutes.backPress(navController) },
                        Modifier.padding(//seems like a hack
                            horizontal = PADDING
                        )
                    )
                }
            }

            composable(route = NavRoutes.Entries.route) {
                val entriesTab = viewModel { EntriesTab(dbManager) }
                Scaffold(
                    topBar = { entriesTab.topBar { NavRoutes.Settings.navHere(navController) } },
                    bottomBar = { drawBottomBar(navController) }) {
                    entriesTab.compose(
                        { NavRoutes.EntryDetails.navHere(navController, it) },
                        Modifier.padding(//seems like a hack
                            horizontal = PADDING
                        )
                    )
                }
            }

            composable(route = NavRoutes.Tags.route) {
                val tagsTab = viewModel { TagsTab(dbManager) { /*fixme transition to editTag*/ } }
                Scaffold(
                    topBar = { tagsTab.topBar { NavRoutes.Settings.navHere(navController) } },
                    bottomBar = { drawBottomBar(navController) },
                    floatingActionButton = { tagsTab.functionButton() }) {
                    tagsTab.compose()
                }
            }

            composable(route = NavRoutes.Settings.route) {
                val settingsScreen = viewModel { SettingsScreen(dbManager) }
                Scaffold(
                    topBar = { settingsScreen.topBar { NavRoutes.backPress(navController) } }) { innerPadding ->
                    settingsScreen.compose(
                        Modifier.padding(//seems like a hack
                            horizontal = PADDING
                        ), innerPadding
                    )
                }
            }
        }
    }

    @Composable
    fun drawBottomBar(navController: NavController) {
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
