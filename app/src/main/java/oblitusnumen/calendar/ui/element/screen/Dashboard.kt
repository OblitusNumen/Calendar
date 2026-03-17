package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import oblitusnumen.calendar.implementation.data.DbManager
import java.time.LocalDate

@Composable
fun DashboardScreen(
    dbManager: DbManager,
    navBar: @Composable () -> Unit,
    newEntry: () -> Unit,
    openThatDayInfo: (LocalDate) -> Unit,
    openMonthAgenda: (Int, Int) -> Unit,
    openEntriesScreen: () -> Unit,
    openTagsScreen: () -> Unit,
    openSettings: () -> Unit,
) {
    Scaffold(
        topBar = {},
        bottomBar = navBar,
        floatingActionButton = {}
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
        }
    }
}