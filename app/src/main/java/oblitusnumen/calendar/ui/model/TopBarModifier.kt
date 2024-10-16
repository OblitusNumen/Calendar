package oblitusnumen.calendar.ui.model

import androidx.compose.runtime.Composable
import oblitusnumen.calendar.MainActivity

interface TopBarModifier {
    @Composable
    fun topBar(calendarViewModel: MainActivity.CalendarViewModel)
}
