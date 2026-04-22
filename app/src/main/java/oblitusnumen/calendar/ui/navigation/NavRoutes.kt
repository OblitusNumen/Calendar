package oblitusnumen.calendar.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import oblitusnumen.calendar.R
import oblitusnumen.calendar.ui.element.screen.MonthDate
import oblitusnumen.calendar.ui.element.screen.PlannerTab
import java.time.LocalDate

sealed class NavRoutes(private val path: String, val route: String = path) {
    data object Dashboard : NavRoutes("dashboard")
    data object Calendar : NavRoutes("calendar")
    data object Planner : NavRoutes("planner", route = "planner/{tab}") {
        private const val tab = "tab"

        fun navHere(navController: NavController, tab: PlannerTab) {
            navController.navigate(withArgs("${tab.ordinal}"))
        }

        fun getArgTab(navBackStackEntry: NavBackStackEntry): Int? {
            val tabText = navBackStackEntry.arguments?.getString(tab)
            val tab = tabText?.toIntOrNull() ?: return null
            return if (tab < 0) null else tab
        }
    }

    data object Settings : NavRoutes("settings") {
        fun navHere(navController: NavController) {
            navController.navigate(route)
        }
    }

    data object TaskEdit : NavRoutes("taskEdit", route = "taskEdit/{task}") {
        private const val task = "task"

        fun navHere(navController: NavController, taskId: Int?) {
            val tid = taskId?.toString() ?: "-1"
            navController.navigate(withArgs(tid))
        }

        fun getArgTaskId(navBackStackEntry: NavBackStackEntry): Int? {
            val taskText = navBackStackEntry.arguments?.getString(task)
            val taskId = taskText?.toInt()
            return if (taskId == null || taskId < 0) null else taskId
        }
    }

    data object Tags : NavRoutes("tags") {
        fun navHere(navController: NavController) {
            navController.navigate(route)
        }
    }

    data object Entries : NavRoutes("entries") {
        fun navHere(navController: NavController) {
            navController.navigate(route)
        }
    }

    data object Agenda : NavRoutes("agenda", route = "agenda/{year}/{month}/{day}") {
        private const val path = "agenda"
        private const val year = "year"
        private const val month = "month"
        private const val day = "day"

        fun navHere(navController: NavController, year: Int, monthValue: Int, day: Int?) {
            if (navController.previousBackStackEntry?.destination?.route?.startsWith(path) ?: false) {// FIXME:
                navController.popBackStack()
                navController.popBackStack()
            }
            navController.navigate(withArgs(year.toString(), monthValue.toString(), day.toString()))
        }

        fun getArgs(navBackStackEntry: NavBackStackEntry): MonthDate {
            val year = navBackStackEntry.arguments!!.getString(year)
            val monthValue = navBackStackEntry.arguments!!.getString(month)
            val day = navBackStackEntry.arguments!!.getString(day)
            return MonthDate(year!!.toInt(), monthValue!!.toInt(), day?.toIntOrNull())
        }
    }

    data object ThatDayDetails : NavRoutes("thatDayDetails", route = "thatDayDetails/{date}") {
        private const val path = "thatDayDetails"
        private const val date = "date"

        fun navHere(navController: NavController, date: LocalDate) {
            if (navController.previousBackStackEntry?.destination?.route?.startsWith(path) ?: false) {// FIXME:
                navController.popBackStack()
                navController.popBackStack()
            }
            navController.navigate(withArgs(date.toEpochDay().toString()))
        }

        fun getArgs(navBackStackEntry: NavBackStackEntry): LocalDate? {
            val thatDayText = navBackStackEntry.arguments?.getString(date)
            return if (thatDayText == null) null else LocalDate.ofEpochDay(thatDayText.toLong())
        }
    }

    data object EntryEdit : NavRoutes("entryEdit", route = "entryEdit/{entry}/{date4new}") {
        private const val entry = "entry"
        private const val date4new = "date4new"

        fun navHere(navController: NavController, entryId: Int?, date: LocalDate? = null) {
            val eid = entryId?.toString() ?: "-1"
            val epochDay = (date?.toEpochDay() ?: Long.MAX_VALUE).toString()
            navController.navigate(withArgs(eid, epochDay))
        }

        fun getArgEntryId(navBackStackEntry: NavBackStackEntry): Int? {
            val entryText = navBackStackEntry.arguments?.getString(entry)
            val entryId = entryText?.toInt()
            return if (entryId == null || entryId < 0) null else entryId
        }

        fun getArgDate4new(navBackStackEntry: NavBackStackEntry): LocalDate? {
            val d4n = navBackStackEntry.arguments?.getString(date4new)?.toLong()
            return if (d4n != null && d4n != Long.MAX_VALUE) LocalDate.ofEpochDay(d4n) else null
        }
    }

    data object TagEdit : NavRoutes("tagEdit", route = "tagEdit/{tag}") {
        private const val tag = "tag"

        fun navHere(navController: NavController, tagId: Int?) {
            val tagId = tagId?.toString() ?: "-1"
            navController.navigate(withArgs(tagId))
        }

        fun getArgTagId(navBackStackEntry: NavBackStackEntry): Int? {
            val tagText = navBackStackEntry.arguments?.getString(tag)
            val tagId = tagText?.toInt()
            return if (tagId == null || tagId < 0) null else tagId
        }
    }

    data object EntryDetails : NavRoutes("entryDetails", route = "entryDetails/{entry}") {
        private const val entry = "entry"

        fun navHere(navController: NavController, entryId: Int?) {
            navController.navigate(if (entryId != null) withArgs(entryId.toString()) else withArgs("-1"))
        }

        fun getArgs(navBackStackEntry: NavBackStackEntry): Int? {
            val entryText = navBackStackEntry.arguments?.getString(entry)
            val entryId = entryText?.toInt()
            return if (entryId == null || entryId < 0) null else entryId
        }
    }

    data object TaskDetails : NavRoutes("taskDetails", route = "taskDetails/{task}") {
        private const val task = "task"

        fun navHere(navController: NavController, taskId: Int?) {
            navController.navigate(if (taskId != null) withArgs(taskId.toString()) else withArgs("-1"))
        }

        fun getArgs(navBackStackEntry: NavBackStackEntry): Int? {
            val taskText = navBackStackEntry.arguments?.getString(task)
            val taskId = taskText?.toInt()
            return if (taskId == null || taskId < 0) null else taskId
        }
    }

    // build navigation path (for screen navigation)
    fun withArgs(vararg args: String): String {
        return buildString {
            append(path)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }

    // build and setup route format (in navigation graph)
    protected fun withArgsFormat(vararg args: String): String {
        return buildString {
            append(path)
            args.forEach { arg ->
                append("/{$arg}")
            }
        }
    }

    companion object {
        fun getTopLevelRoutes() = listOf(
            TopLevelRoute(R.string.nav_dashboard, Dashboard, Icons.Outlined.Home),
            TopLevelRoute(R.string.nav_calendar, Calendar, Icons.Outlined.DateRange),
            TopLevelRoute(R.string.nav_planner, Planner, Icons.Outlined.CheckCircle)
        )

        fun isTopLevel(route: String?): Boolean {
            for (r in getTopLevelRoutes())
                if (r.route.route == route) return true
            return false
        }

        fun backPress(navController: NavController) {
            navController.navigateUp()
        }
    }
}

data class TopLevelRoute(@StringRes val nameRes: Int, val route: NavRoutes, val icon: ImageVector)
