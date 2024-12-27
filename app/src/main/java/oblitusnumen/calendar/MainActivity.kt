package oblitusnumen.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import oblitusnumen.calendar.implementation.notifications.NotificationBroadcastReceiver
import oblitusnumen.calendar.ui.model.navigation.NavRoutes
import oblitusnumen.calendar.ui.model.screen.DateScreen
import oblitusnumen.calendar.ui.model.screen.EntryEdit
import oblitusnumen.calendar.ui.model.tab.CalendarTab
import oblitusnumen.calendar.ui.model.tab.EntriesTab
import oblitusnumen.calendar.ui.model.tab.TagsTab
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var calendarViewModel: CalendarViewModel? = null

    companion object {
        val PADDING: Dp = 5.dp
    }

    override fun onCreate(savedInstanceState: Bundle?) { //fixme ask for required permissions somewhere
        super.onCreate(savedInstanceState)
        NotificationBroadcastReceiver.createNotificationChannels(this)
        NotificationBroadcastReceiver.scheduleNotification(this, System.currentTimeMillis())
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                Intent().also { intent ->
                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    context.startActivity(intent)
                }
            }
        }*/
        setContent {
            CalendarTheme {
                calendarViewModel = viewModel { CalendarViewModel(DbManager(this@MainActivity)) }
                calendarViewModel!!.navController = rememberNavController()
                navGraph(calendarViewModel!!.navController!!, calendarViewModel!!.dbManager)
            }
        }
    }

    @Composable
    fun navGraph(navController: NavHostController, dbManager: DbManager) {
        NavHost(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination = NavRoutes.Calendar.route
        ) {
            composable(route = NavRoutes.Calendar.route) {
                val calendarTab = viewModel {
                    CalendarTab(dbManager, { date ->
                        navController.navigate(NavRoutes.ThatDayDetails.withArgs(date.toEpochDay().toString()))
                    }, { navController.navigate(NavRoutes.EntryDetails.withArgs("-1")) })
                }
                Scaffold(
                    topBar = { calendarTab.topBar() },
                    bottomBar = { drawBottomBar(navController) },
                    floatingActionButton = { calendarTab.functionButton() }) {
                    calendarTab.compose(
                        Modifier.absolutePadding(//seems like a hack
                            PADDING, 50.dp,
                            PADDING, 50.dp
                        )
                    )
                }
            }

            composable(route = NavRoutes.ThatDayDetails.route) { navBackStackEntry ->
                val thatDayText = navBackStackEntry.arguments?.getString(NavRoutes.ThatDayDetails.date)
                val thatDay = LocalDate.ofEpochDay(thatDayText?.toLong() ?: 0)
                val dateScreen = viewModel {
                    DateScreen(
                        thatDay,
                        dbManager,
                        { navController.navigate(NavRoutes.EntryDetails.withArgs(it.toString())) },
                        { navController.navigateUp() })
                }
                Scaffold(
                    topBar = { dateScreen.topBar() },
                    floatingActionButton = { dateScreen.functionButton() }) {
                    dateScreen.compose(
                        Modifier.absolutePadding(//seems like a hack
                            PADDING, 50.dp,
                            PADDING, 50.dp
                        )
                    )
                }
            }

            composable(route = NavRoutes.EntryDetails.route) { navBackStackEntry ->
                val entry = navBackStackEntry.arguments?.getString(NavRoutes.EntryDetails.entry)
                val entryId = entry?.toInt() ?: -1
                val entryEdit = viewModel {
                    EntryEdit(
                        dbManager,
                        if (entryId < 0) dbManager.createEntry() else dbManager.getEntryById(entryId)!!
                    ) { navController.navigateUp() }
                }
                Scaffold(topBar = { entryEdit.topBar() }) {
                    entryEdit.compose(
                        Modifier.absolutePadding(//seems like a hack
                            PADDING, 50.dp,
                            PADDING, 50.dp
                        )
                    )
                }
            }

            composable(route = NavRoutes.Entries.route) {
                val entriesTab = viewModel {
                    EntriesTab(dbManager) {
                        navController.navigate(NavRoutes.EntryDetails.withArgs(it.toString()))
                    }
                }
                Scaffold(topBar = { entriesTab.topBar() },
                    bottomBar = { drawBottomBar(navController) }) {
                    entriesTab.compose(
                        Modifier.absolutePadding(//seems like a hack
                            PADDING, 50.dp,
                            PADDING, 50.dp
                        )
                    )
                }
            }

            composable(route = NavRoutes.Tags.route) {
                val tagsTab = viewModel { TagsTab(dbManager) { /*fixme transition to editTag*/ } }
                Scaffold(
                    bottomBar = { drawBottomBar(navController) },
                    floatingActionButton = { tagsTab.functionButton() }) {
                    tagsTab.compose()
                }
            }
        }
    }

    @Composable
    fun drawBottomBar(navController: NavController) {
        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavRoutes.getTopLevelRoutes().forEach { topLevelRoute ->
                NavigationBarItem(
                    icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name) },
                    label = { Text(topLevelRoute.name) },
                    selected = currentDestination?.route == topLevelRoute.route.route,
                    onClick = {
                        navController.navigate(topLevelRoute.route.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
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

    class CalendarViewModel(val dbManager: DbManager) : ViewModel() {
        var navController: NavHostController? = null
    }
}

@Composable
fun getWidthPartIncludePadding(divisor: Float): Dp {
    return LocalConfiguration.current.screenWidthDp.dp.minus(MainActivity.PADDING.times(2)).div(divisor)
}

@Composable
fun BackButton(backPress: () -> Unit) {
    Button(onClick = backPress) {
        Text("く")
    }
}
