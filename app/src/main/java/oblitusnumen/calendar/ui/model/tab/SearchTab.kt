package oblitusnumen.calendar.ui.model.tab

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.ui.model.Tab

class SearchTab : Tab {
    // TODO:
    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Text("find")
    }
}