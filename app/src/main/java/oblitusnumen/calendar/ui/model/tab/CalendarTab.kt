package oblitusnumen.calendar.ui.model.tab

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.MainActivity.Companion.LIST_CENTER
import oblitusnumen.calendar.R
import oblitusnumen.calendar.getWidthPartIncludePadding
import oblitusnumen.calendar.implementation.Utils
import oblitusnumen.calendar.implementation.data.CalendarDate
import oblitusnumen.calendar.ui.model.Functional
import oblitusnumen.calendar.ui.model.Tab
import oblitusnumen.calendar.ui.model.TopBarModifier
import oblitusnumen.calendar.ui.model.screen.DateScreen
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CalendarTab : Tab, Functional, TopBarModifier {
    private var calendarLazyListState: LazyListState? = null

    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column {
            Row {
                var i = 1;
                while (i <= 7) {
                    Box(Modifier.width(getWidthPartIncludePadding(7f)).height(25.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary)) {
                        Text(stringArrayResource(R.array.weekdayNames)[i-1], Modifier.align(Alignment.Center))
                    }
                    i++;
                }
            }
            val now = LocalDate.now()
            if (calendarLazyListState == null) calendarLazyListState = rememberLazyListState(getNowItemIndex(now))
            LazyColumn(
                state = calendarLazyListState!!,
                modifier = Modifier
            ) {
                items(Int.MAX_VALUE, itemContent = {
                    Utils.log(it)
                    val monthItemIndex = getMonthItemIndex(it)
                    val mon = LIST_CENTER.plusMonths(((it - Int.MAX_VALUE / 2) / 7).toLong()).withDayOfMonth(1)
                    if (monthItemIndex == 0) {
                        Text(stringArrayResource(R.array.monthNames)[mon.month.value - 1] + " " + mon.year)
                    } else {
                        DisplayWeek(mon.monthValue, mon.plusDays(7*(monthItemIndex-1)-(mon.dayOfWeek.value - 1).toLong()), calendarViewModel)
                    }
                })
            }
        }
    }

    private fun getNowItemIndex(now: LocalDate) =
        (Int.MAX_VALUE / 2 + ChronoUnit.MONTHS.between(LIST_CENTER, now) * 7).toInt()

    @Composable
    fun DisplayWeek(monthValue: Int, date0: LocalDate, calendarViewModel: MainActivity.CalendarViewModel) {
        var date = date0
            val blockW = getWidthPartIncludePadding(7f)
            val blockH = blockW.times(1.5f)
        Row {
            while (date.month.value % 12 + 1 == monthValue) {
                Box(
                    Modifier.size(blockW, blockH)
                ) {
                }
                date = date.plusDays(1)
            }
            while (date.month.value == monthValue) {
                        val dates =
                            ArrayList(
                                calendarViewModel.dataManager.getDates(
                                    date.atStartOfDay(),
                                    date.plusMonths(1).atStartOfDay()
                                ).toList()
                            )
                DisplayDay(blockW, blockH, date, calendarViewModel, dates)
                date = date.plusDays(1)
            }
        }
    }

    private fun getMonthItemIndex(it: Int): Int {
        val offset = it - Int.MAX_VALUE / 2
        return if (offset < 0) 6 + offset % 7 else offset % 7
    }

    @SuppressLint("NewApi")
    @Composable
    fun DisplayDay(
        blockW: Dp,
        blockH: Dp,
        then: LocalDate,
        calendarViewModel: MainActivity.CalendarViewModel,
        dates: ArrayList<CalendarDate>
    ) {
        val now = LocalDate.now()
        var modifier = Modifier.padding(2.dp).size(blockW.minus(4.dp), blockH.minus(4.dp))
            .border(
                BorderStroke(
                    2.dp, MaterialTheme.colorScheme.primary
                ), shape = RoundedCornerShape(10.dp)
            )
        val col: Color

        if (now.year == then.year && now.month == then.month && then.dayOfMonth == now.dayOfMonth) {
            modifier = modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
            col = MaterialTheme.colorScheme.background
        } else {
            col = MaterialTheme.colorScheme.primary
        }
        val begin = then.atStartOfDay()
        val end = then.plusDays(1).atStartOfDay()
        val eventDates =
            ArrayList(dates.stream().filter({ date -> date.date.isAfter(begin) && date.date.isBefore(end) }).toList())
        Box(modifier.clickable(onClick = {
//        Log.v("calendar", "clicked ${then.year}.${then.month}.$dayOfMonth")
            eventDates.sortBy { it.date }
            calendarViewModel.open(DateScreen(then, eventDates))
        })) {
            Column {
                Box(Modifier.fillMaxWidth()) {
                    Text("" + then.dayOfMonth, Modifier.align(Alignment.TopCenter), col)
                }
                for (date in eventDates) {
                    Text(date.desc, Modifier.background(Color(0x989800)))
                }
            }
        }
    }

    @Composable
    override fun functionButton(calendarViewModel: MainActivity.CalendarViewModel) {
//        TODO("Not yet implemented")
    }

    // TODO:
    @Composable
    override fun topBar(calendarViewModel: MainActivity.CalendarViewModel) {
        val coroutineScope = rememberCoroutineScope()
        Button(onClick = {
            coroutineScope.launch {
                calendarLazyListState!!.scrollToItem(getNowItemIndex(LocalDate.now()))
            }
        }) {
            Text("to current date")
        }
    }
}