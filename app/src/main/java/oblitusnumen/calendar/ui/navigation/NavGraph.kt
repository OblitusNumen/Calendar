package oblitusnumen.calendar.ui.navigation

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt
import oblitusnumen.calendar.ui.element.BottomBar
import oblitusnumen.calendar.ui.element.screen.*
import oblitusnumen.calendar.ui.state.EntryEditState
import oblitusnumen.calendar.ui.state.TaskEditState
import oblitusnumen.calendar.ui.viewmodel.EntryEditViewModel
import oblitusnumen.calendar.ui.viewmodel.TaskEditViewModel
import java.time.LocalDate

@Composable
fun NavigationGraph(navController: NavHostController, dbManager: DbManager, startingEntryId: Int? = null) {
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
                { BottomBar(navController) },
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
                { BottomBar(navController) },
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
                { BottomBar(navController) },
                { NavRoutes.TaskEdit.navHere(navController, null) },
                { NavRoutes.TaskDetails.navHere(navController, it) },
                { NavRoutes.Settings.navHere(navController) }
            )
        }

        composable(route = NavRoutes.TaskDetails.route) { navBackStackEntry ->
            val taskId = NavRoutes.TaskDetails.getArgs(navBackStackEntry)
            if (taskId == null || !Task.exists(dbManager, taskId)) {
                NavRoutes.backPress(navController)
                return@composable
            }

            TaskDetailsScreen(dbManager,
                taskId,
                { NavRoutes.TaskEdit.navHere(navController, taskId) },
                { NavRoutes.backPress(navController) }
            )
        }

        composable(route = NavRoutes.Agenda.route) { navBackStackEntry -> // FIXME: resolve recurring stack
            val month = NavRoutes.Agenda.getArgs(navBackStackEntry)

            AgendaScreen(
                dbManager,
                month,
                tagsFilter,
                { BottomBar(navController) },
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

            viewModel { EntryEditViewModel(EntryEditState.initial(dbManager, entryId, false)) }

            EditEntryScreen(dbManager, viewModel(), fromDay) { NavRoutes.backPress(navController) }
        }

        composable(route = NavRoutes.TaskEdit.route) { navBackStackEntry ->
            val taskId = NavRoutes.TaskEdit.getArgTaskId(navBackStackEntry)

            viewModel { TaskEditViewModel(TaskEditState.initial(dbManager, taskId)) }

            EditTaskScreen(dbManager, viewModel()) { NavRoutes.backPress(navController) }
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
                { BottomBar(navController) },
                { NavRoutes.EntryEdit.navHere(navController, null) },
                { NavRoutes.EntryDetails.navHere(navController, it) },
                { NavRoutes.backPress(navController) }
            )
        }

        composable(route = NavRoutes.Tags.route) {
            TagsScreen(
                dbManager,
                { BottomBar(navController) },
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