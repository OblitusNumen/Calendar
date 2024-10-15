package oblitusnumen.calendar.ui.state

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.R
import oblitusnumen.calendar.ui.model.CalendarTab
import oblitusnumen.calendar.ui.model.DateScreen
import java.time.LocalDate

class Calendar

@Composable
fun CalendarTab(calendarViewModel: MainActivity.CalendarViewModel) {
    val now = LocalDate.now()
    if ((calendarViewModel.screen as CalendarTab).calendarLazyListState == null)
        (calendarViewModel.screen as CalendarTab).calendarLazyListState = rememberLazyListState(Int.MAX_VALUE / 2)
    LazyColumn(
        state = (calendarViewModel.screen as CalendarTab).calendarLazyListState!!,
        modifier = Modifier
    ) {
        items(Int.MAX_VALUE, itemContent = {
            DisplayMonth(now.withDayOfMonth(1).plusMonths((it - Int.MAX_VALUE / 2).toLong()), calendarViewModel)
        })
    }
}

@Composable
fun DisplayMonth(
    then: LocalDate,
    calendarViewModel: MainActivity.CalendarViewModel
) {
    Column {
        val blockW = LocalConfiguration.current.screenWidthDp.dp.div(7)
        val blockH = blockW.times(1.2f)
//        Log.v("calendar", "day" + then.dayOfWeek.value)
        var dayOfWeek = (then.dayOfWeek.value + 6) % 7 + 1
        var dayOfMonth = -dayOfWeek + 2
        Text(stringArrayResource(R.array.monthNames)[then.month.value - 1] + ", " + then.year)
        val monthLen = then.month.length(then.isLeapYear)
//        Log.v("calendar", "mLen:" + monthLen + "m:" + stringArrayResource(R.array.monthNames)[then.month.value - 1] + ", " + then.year)
//        calendarViewModel.dataManager.getDates(then.atStartOfDay(), then.plusMonths(1).atStartOfDay())
        while (dayOfMonth <= monthLen) {
            Row {
                while (dayOfMonth < 1) {
                    Box(
                        Modifier.size(blockW, blockH)
                    ) {
                    }
                    dayOfMonth++
                }
                while (dayOfWeek <= 7) {
                    DisplayDay(blockW, blockH, then.withDayOfMonth(dayOfMonth), calendarViewModel)

                    dayOfMonth++
                    dayOfWeek++
                    if (dayOfMonth > monthLen) break
                }
                dayOfWeek = 1
            }
        }
    }
}

@Composable
fun DisplayDay(
    blockW: Dp,
    blockH: Dp,
    then: LocalDate,
    calendarViewModel: MainActivity.CalendarViewModel
) {
    val now = LocalDate.now()
    var modifier = Modifier.size(blockW, blockH)
        .border(
            BorderStroke(
                2.dp, MaterialTheme.colorScheme.primary
            )
        )
    val col: Color

    if (now.year == then.year && now.month == then.month && then.dayOfMonth == now.dayOfMonth) {
        modifier = modifier.background(MaterialTheme.colorScheme.primary)
        col = MaterialTheme.colorScheme.background
    } else {
        col = MaterialTheme.colorScheme.primary
    }
    val dates =
        ArrayList(calendarViewModel.dataManager.getDates(then.atStartOfDay(), then.plusDays(1).atStartOfDay()).toList())
    Box(modifier.clickable(onClick = {
//        Log.v("calendar", "clicked ${then.year}.${then.month}.$dayOfMonth")
        dates.sortBy { it.date }
        calendarViewModel.open(DateScreen(then, dates))
    })) {
        Column {
            Text("" + then.dayOfMonth, Modifier, col)
            for (date in dates) {
                Text(date.desc, Modifier.background(Color(0x989800)))
            }
        }

    }
}
