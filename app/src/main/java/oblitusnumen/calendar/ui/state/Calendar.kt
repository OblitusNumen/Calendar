package oblitusnumen.calendar.ui.state

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.StringLocale
import java.time.LocalDate

class Calendar

@Composable
fun CalendarTab(calendarViewModel: MainActivity.CalendarViewModel) {
    val now = LocalDate.now()
    LazyColumn(
        state = rememberLazyListState(calendarViewModel.listState.value),
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
        Log.v("calendar", "day" + then.dayOfWeek.value)
        var dayOfWeek = (then.dayOfWeek.value + 6) % 7 + 1
        var dayOfMonth = -dayOfWeek + 2
        Text(StringLocale.monthName[then.month.value - 1] + ", " + then.year)
        val monthLen = then.month.length(then.isLeapYear)
        Log.v("calendar", "mLen:" + monthLen + "m:" + StringLocale.monthName[then.month.value - 1] + ", " + then.year)
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
                    DisplayDay(blockW, blockH, then, dayOfMonth, calendarViewModel)

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
    dayOfMonth: Int,
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

    if (now.year == then.year && now.month == then.month && dayOfMonth == now.dayOfMonth) {
        modifier = modifier.background(MaterialTheme.colorScheme.primary)
        col = MaterialTheme.colorScheme.background
    } else {
        col = MaterialTheme.colorScheme.primary
    }
    Box(modifier.clickable(onClick = {
        Log.v("calendar", "clicked ${then.year}.${then.month}.$dayOfMonth")
        calendarViewModel.date = then.withDayOfMonth(dayOfMonth)
        calendarViewModel.setState0(MainActivity.Screen.DATE)
    })) {
        Text("" + dayOfMonth, Modifier, col)
    }
}
