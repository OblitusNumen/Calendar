package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.ui.model.Tab
import oblitusnumen.calendar.ui.model.screen.EntryEdit

class EntriesTab : Tab {
    // TODO:
    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column(Modifier.verticalScroll(ScrollState(0)).fillMaxWidth()) {// TODO: update state
            Text("Entries")
            for (entry in calendarViewModel.dbManager.getEntries()) {
                Box(
                    Modifier.height(50.dp).fillMaxWidth()
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                        .clickable(onClick = {
                            calendarViewModel.open(EntryEdit(calendarViewModel, entry))
                        })
                ) {
                    Text(entry.name)
                }
            }
        }
    }
}