package oblitusnumen.calendar.ui.model.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoutes(private val path: String, val route: String = path) {
    data object Calendar : NavRoutes("calendar")
    data object Tags : NavRoutes("tags")
    data object Entries : NavRoutes("entries")
    data object ThatDayDetails : NavRoutes("thatDayDetails", route = "thatDayDetails/{date}") {
        val date = "date"
    }

    data object EventDetails : NavRoutes("eventDetails", route = "eventDetails/{event}") {
        val event = "event"
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
            TopLevelRoute("Tags", Tags, Icons.Outlined.Search)
        )

        fun isTopLevel(route: String?): Boolean {
            for (r in getTopLevelRoutes())
                if (r.route.route == route) return true
            return false
        }
    }
}

data class TopLevelRoute(val name: String, val route: NavRoutes, val icon: ImageVector)
