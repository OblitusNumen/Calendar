package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import oblitusnumen.calendar.ui.element.DrawEntrySelectable
import oblitusnumen.calendar.ui.element.NewEntryFunctionButton
import oblitusnumen.calendar.ui.element.ScheduleDialog
import oblitusnumen.calendar.ui.element.SearchTopBar

@Composable
fun EntriesScreen(
    dbManager: DbManager,
    navBar: @Composable () -> Unit,
    openEditNewEntry: () -> Unit,
    openEntryDetails: (Int) -> Unit,
    backPress: () -> Unit
) {
    val searchQuery = rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = { SearchTopBar(searchQuery, backPress) },
        bottomBar = navBar,
        floatingActionButton = { NewEntryFunctionButton(openEditNewEntry) }
    ) { paddingValues ->
        var scheduleCounter by remember { mutableIntStateOf(0) }
        val allEntries = remember(scheduleCounter) { ViewEntryWithOptions.all(dbManager).sortedBy { it.nextDate } }
        val entries = remember(allEntries, searchQuery.value) {
            allEntries.filter {
                it.getOptions(dbManager).name.contains(
                    searchQuery.value,
                    true
                )
            }
        }

        var scheduleDialogEntry: ViewEntryWithOptions? by remember { mutableStateOf(null) }
        if (scheduleDialogEntry != null)
            ScheduleDialog(dbManager, scheduleDialogEntry!!, { scheduleDialogEntry = null }) {
                scheduleCounter++
                scheduleDialogEntry = null
            }

        LazyColumn {
            item {
                Spacer(Modifier.height(paddingValues.calculateTopPadding()))
            }
            items(entries) { entry ->
                val id = entry.id!!

                DrawEntrySelectable(
                    entry, false,// TODO:
                    { scheduleDialogEntry = entry },
                    { openEntryDetails(id) }
                )
            }
            item {
                Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}
