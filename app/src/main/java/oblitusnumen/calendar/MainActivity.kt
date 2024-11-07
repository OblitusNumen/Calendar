package oblitusnumen.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.util.Supplier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import oblitusnumen.calendar.MainActivity.CalendarViewModel
import oblitusnumen.calendar.MainActivity.Companion.PADDING
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.Tab
import oblitusnumen.calendar.ui.model.navigation.NavRoutes
import oblitusnumen.calendar.ui.model.tab.CalendarTab
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.time.LocalDate
import java.util.*

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
            navController = navController,
            startDestination = NavRoutes.Calendar.route
        ) {
            composable(route = NavRoutes.Calendar.route) {
                val calendarTab = viewModel {
                    CalendarTab(calendarViewModel.dbManager, {date, evtDates ->
                        calendarViewModel.workaroundArgList = listOf(date, evtDates)
                        navController.navigate(NavRoutes.ThatDayDetails.withArgs("date arg")) //fixme proper args
                    }, {
                        calendarViewModel.workaroundArgList = listOf(it)
                        navController.navigate(NavRoutes.EntryDetails.withArgs("new entry")) //fixme proper args
                    })
                }
                Scaffold(
                    topBar = { calendarTab.topBar() },
                    bottomBar = { tryDrawBottomBar(navController) },
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
                        calendarViewModel.workaroundArgList!![1] as List<oblitusnumen.calendar.implementation.data.Date>,
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

            composable(route = NavRoutes.EntryDetails.route) { navBackStackEntry ->
                val entry = navBackStackEntry.arguments?.getString(NavRoutes.EntryDetails.entry)
                Button({ navController.navigateUp() }) {
                    Text("entry: " + entry!!)
                }
            }

            composable(route = NavRoutes.Tags.route) {
                Scaffold(
                    bottomBar = { tryDrawBottomBar(navController) }) {
                    Button({ navController.navigateUp() }) {
                        Text("hii")
                    }
                }
            }
        }
    }

    @Composable
    fun tryDrawBottomBar(navController: NavController) {
        if (NavRoutes.isTopLevel(navController.currentBackStackEntryAsState().value?.destination?.route)) {
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
    }

    override fun onDestroy() {
        dbManager.close()
        super.onDestroy()
    }

    class CalendarViewModel(val dbManager: DbManager) : ViewModel() {
        // FIXME: add stack for each tab
        var workaroundArgList: List<Any>? = null
        var navController: NavHostController? = null

        fun back(): Boolean {
            return false
        }

        fun open(screen: Screen) {
        }
    }
}

@Composable
fun getWidthPartIncludePadding(divisor: Float): Dp {
    return LocalConfiguration.current.screenWidthDp.dp.minus(PADDING.times(2)).div(divisor)
}

@Composable
fun getHeightPart(divisor: Float): Dp {
    return LocalConfiguration.current.screenHeightDp.dp.div(divisor)
}

@Composable
inline fun <reified T : Screen> getTabBoxModifier(
    classSupplier: Supplier<T>,
    calendarViewModel: CalendarViewModel
): Modifier {
    var modifier = Modifier.width(getWidthPart(3f)).height(50.dp)
    modifier = modifier.clickable(
        onClick = {
            calendarViewModel.changeTab(classSupplier.get())
        }
    )
    if (calendarViewModel.screen::class == T::class) modifier =
        modifier.border(2.dp, MaterialTheme.colorScheme.primary)
    return modifier
}

@Composable
fun BackButton(calendarViewModel: CalendarViewModel) {
    Button(onClick = {
        calendarViewModel.back()
    }) {
        Text("く")
//        Text("←")
    }
}

@Composable
fun BackButton(backPress: () -> Unit) {
    Button(onClick = backPress) {
        Text("く")
//        Text("←")
    }
}
