package oblitusnumen.calendar.ui.model

import androidx.compose.runtime.Composable
import oblitusnumen.calendar.MainActivity.CalendarViewModel

interface Screen {
    @Composable
    fun compose(calendarViewModel: CalendarViewModel)
}