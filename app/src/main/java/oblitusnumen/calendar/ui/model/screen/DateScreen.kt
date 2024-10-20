package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.implementation.data.CalendarDate
import oblitusnumen.calendar.implementation.data.CalendarDates
import oblitusnumen.calendar.ui.model.Functional
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.TopBarModifier
import java.time.LocalDate
import java.time.LocalDateTime

class DateScreen(var date: LocalDate, private var dates: List<CalendarDate>) : Screen, Functional, TopBarModifier {
    // TODO:
    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column(Modifier.verticalScroll(ScrollState(0))) {// TODO: update state 
            Text("Date $date")
            for (date in dates) {
                val entry = calendarViewModel.dataManager.getEntry(date.entry)
                Box(
                    Modifier.fillMaxWidth().border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                        .clickable(onClick = {
                            calendarViewModel.open(EntryEdit(entry))
                        })
                ) {
                    Column {
                        Text(date.desc)
                        Text(entry.name)
                        Text(date.date.toString())
                    }
                }
            }
        }
    }
    // TODO: fix orientation

    @Composable
    override fun functionButton(calendarViewModel: MainActivity.CalendarViewModel) {
        Button(onClick = {
            val now = LocalDateTime.now()
            val entry = calendarViewModel.dataManager.createEntry()
            entry.name = "huh"
            val eDates = entry.calendarDates
            eDates.add(CalendarDate(
                (calendarViewModel.screen as DateScreen).date.atStartOfDay()
                    .withHour(now.hour).withMinute(now.minute).withSecond(now.second),
                entry.uid
            ))
            entry.update(eDates)
            calendarViewModel.open(EntryEdit(entry))
        }) {
            Text("十")
//            Text("+")
        }
    }

    @Composable
    override fun topBar(calendarViewModel: MainActivity.CalendarViewModel) {
        BackButton(calendarViewModel)
    }
}