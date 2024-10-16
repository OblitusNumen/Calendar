package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.ui.model.Tab

class TagsTab : Tab {
    // TODO:
    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column(Modifier.verticalScroll(ScrollState(0))) {
            for (tag in calendarViewModel.dataManager.tags) {
                Box(
                    Modifier.height(200.dp)
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                ) {
                    Text(tag.name)
                }
            }
        }
    }
}