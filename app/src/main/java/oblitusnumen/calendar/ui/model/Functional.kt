package oblitusnumen.calendar.ui.model

import androidx.compose.runtime.Composable
import oblitusnumen.calendar.MainActivity

interface Functional {
    @Composable
    fun functionButton(calendarViewModel: MainActivity.CalendarViewModel)
}