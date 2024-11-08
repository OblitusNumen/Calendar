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
import oblitusnumen.calendar.MainActivity.Companion.PADDING
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.ui.model.navigation.NavRoutes
import oblitusnumen.calendar.ui.model.screen.DateScreen
import oblitusnumen.calendar.ui.model.screen.EntryEdit
import oblitusnumen.calendar.ui.model.tab.CalendarTab
import oblitusnumen.calendar.ui.model.tab.EntriesTab
import oblitusnumen.calendar.ui.model.tab.TagsTab
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val dbManager = DbManager(this)

    companion object {
        val PADDING: Dp = 5.dp
        val LIST_CENTER: LocalDate = LocalDate.of(1970, 1, 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbManager.init()
        setContent {
            CalendarTheme {
                val calendarViewModel = viewModel { CalendarViewModel(dbManager) }
                calendarViewModel.navController = rememberNavController()
                navGraph(calendarViewModel.navController!!, calendarViewModel)
            }
        }
    }

    @Composable
    fun navGraph(navController: NavHostController, calendarViewModel: CalendarViewModel) {
        NavHost(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination = NavRoutes.Calendar.route
        ) {
            composable(route = NavRoutes.Calendar.route) {
                val calendarTab = viewModel {
                    CalendarTab(calendarViewModel.dbManager, { date ->
                        calendarViewModel.workaroundArgList = listOf(date)
                        navController.navigate(NavRoutes.ThatDayDetails.withArgs("date arg")) //fixme proper args
                    }, {
                        calendarViewModel.workaroundArgList = listOf(it)
                        navController.navigate(NavRoutes.EntryDetails.withArgs("new entry")) //fixme proper args
                    })
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

            composable(route = NavRoutes.ThatDayDetails.route) { //navBackStackEntry ->
                //val thatDay = navBackStackEntry.arguments?.getString(NavRoutes.ThatDayDetails.date)
                val dateScreen = viewModel {
                    DateScreen(
                        calendarViewModel.workaroundArgList!![0] as LocalDate,
                        dbManager,
                        {
                            calendarViewModel.workaroundArgList = listOf(it)
                            navController.navigate(NavRoutes.EntryDetails.withArgs("new entry")) //fixme proper args
                        },
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

            composable(route = NavRoutes.EntryDetails.route) { //navBackStackEntry ->
                //val entry = navBackStackEntry.arguments?.getString(NavRoutes.EntryDetails.entry)
                val entryEdit = viewModel {
                    EntryEdit(
                        calendarViewModel.dbManager,
                        calendarViewModel.workaroundArgList!![0] as Entry
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
                    EntriesTab(calendarViewModel.dbManager) {
                        calendarViewModel.workaroundArgList = listOf(it)
                        navController.navigate(NavRoutes.EntryDetails.withArgs("new entry")) //fixme proper args
                    }
                }
                Scaffold(
                    bottomBar = { drawBottomBar(navController) }) {
                    entriesTab.compose()
                }
            }

            composable(route = NavRoutes.Tags.route) {
                val tagsTab = viewModel { TagsTab(calendarViewModel.dbManager) }
                Scaffold(
                    bottomBar = { drawBottomBar(navController) }) {
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
        dbManager.close()
        super.onDestroy()
    }

    class CalendarViewModel(val dbManager: DbManager) : ViewModel() {
        var workaroundArgList: List<Any>? = null
        var navController: NavHostController? = null
    }
}

@Composable
fun getWidthPartIncludePadding(divisor: Float): Dp {
    return LocalConfiguration.current.screenWidthDp.dp.minus(PADDING.times(2)).div(divisor)
}

@Composable
fun BackButton(backPress: () -> Unit) {
    Button(onClick = backPress) {
        Text("く")
    }
}
