package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import oblitusnumen.calendar.implementation.data.DbManager
import java.time.LocalDate

@Composable
fun TaskDetailsScreen(
    dbManager: DbManager,
    navBar: @Composable () -> Unit,
    newEntry: () -> Unit,
    openThatDayInfo: (LocalDate) -> Unit,
    openMonthAgenda: (Int, Int) -> Unit,
    openEntriesScreen: () -> Unit,
    openTagsScreen: () -> Unit,
    openSettings: () -> Unit,
) {
    Scaffold { paddingValues ->
        LazyColumn {
            item {
                Spacer(Modifier.height(paddingValues.calculateTopPadding()))
            }

            item {
                Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}
