package oblitusnumen.calendar.ui.model.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import java.time.LocalDate

sealed class NavRoutes(private val path: String, val route: String = path) {
    data object Calendar : NavRoutes("calendar")
    data object Tags : NavRoutes("tags")
    data object Entries : NavRoutes("entries")
    data object ThatDayDetails : NavRoutes("thatDayDetails", route = "thatDayDetails/{date}") {
        val date = "date"

        fun navHere(navController: NavController, date: LocalDate) {
            navController.navigate(withArgs(date.toEpochDay().toString()))
        }

        fun getArgs(navBackStackEntry: NavBackStackEntry): LocalDate? {
            val thatDayText = navBackStackEntry.arguments?.getString(date)
            return if (thatDayText == null) null else LocalDate.ofEpochDay(thatDayText.toLong())
        }
    }

    data object EntryDetails : NavRoutes("entryDetails", route = "entryDetails/{entry}") {
        val entry = "entry"

        fun navHere(navController: NavController, entryId: Int) {
            navController.navigate(withArgs(entryId.toString()))
        }

        fun getArgs(navBackStackEntry: NavBackStackEntry): Int? {
            val entry = navBackStackEntry.arguments?.getString(NavRoutes.EntryDetails.entry)
            return entry?.toInt()
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
            TopLevelRoute("Calendar", Calendar, Icons.Outlined.Call),
            TopLevelRoute("Tags", Tags, Icons.Outlined.Search),
            TopLevelRoute("Entries", Entries, Icons.Outlined.ThumbUp)
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

data class TopLevelRoute(val name: String, val route: NavRoutes, val icon: ImageVector)
