package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.MainActivity.Companion.LIST_CENTER
import oblitusnumen.calendar.R
import oblitusnumen.calendar.getWidthPartIncludePadding
import oblitusnumen.calendar.implementation.Utils.zonedDateTime
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.ui.model.Functional
import oblitusnumen.calendar.ui.model.Tab
import oblitusnumen.calendar.ui.model.TopBarModifier
import oblitusnumen.calendar.ui.model.screen.DateScreen
import oblitusnumen.calendar.ui.model.screen.EntryEdit
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CalendarTab : Tab, Functional, TopBarModifier {
    private var calendarLazyListState: LazyListState? = null

    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column {
            Row {
                var i = 1
                while (i <= 7) {
                    Box(
                        Modifier.width(getWidthPartIncludePadding(7f)).height(25.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringArrayResource(R.array.weekdayNames)[i - 1], Modifier.align(Alignment.Center))
                    }
                    i++
                }
            }
            val now = LocalDate.now()
            if (calendarLazyListState == null) calendarLazyListState = rememberLazyListState(getNowItemIndex(now))
            LazyColumn(
                state = calendarLazyListState!!,
                modifier = Modifier
            ) {
                items(Int.MAX_VALUE, itemContent = {
                    val offset = it - Int.MAX_VALUE / 2
                    val monthItemIndex = if (offset < 0) 6 + offset % 7 else offset % 7
                    val monthIdx = if (offset < 0) offset / 7 - 1 else offset / 7
                    val mon = LIST_CENTER.plusMonths(monthIdx.toLong()).withDayOfMonth(1)
                    if (monthItemIndex == 0) {
                        Text(stringArrayResource(R.array.monthNames)[mon.month.value - 1] + " " + mon.year)
                    } else {
                        DisplayWeek(
                            mon.monthValue,
                            mon.plusDays(7 * (monthItemIndex - 1) - (mon.dayOfWeek.value - 1).toLong()),
                            calendarViewModel
                        )
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
        Row {
            while (date.month.value % 12 + 1 == monthValue) {
                Spacer(Modifier.width(blockW))
                date = date.plusDays(1)
            }
            val dates =
                calendarViewModel.dbManager.getDates(
                    date,
                    date.plusWeeks(1)
                )
            while (date.month.value == monthValue) {
                DisplayDay(blockW, 3, date, calendarViewModel, dates)
                if (date.dayOfWeek.value == 7) break
                date = date.plusDays(1)
            }
        }
    }

    @Composable
    fun DisplayDay(
        blockW: Dp,
        maxElements: Int,
        then: LocalDate,
        calendarViewModel: MainActivity.CalendarViewModel,
        dates: List<Date>
    ) {
        val now = LocalDate.now()
        val begin = zonedDateTime(then)
        val eventDates = dates.filter { date -> date.forDay(begin) != null }.sortedBy { it.forDay(begin) }

        val evtHeight = measureTextLine(MaterialTheme.typography.bodySmall) + 4.dp
        val today = (now.year == then.year && now.month == then.month && then.dayOfMonth == now.dayOfMonth)
        val evtOverflow = eventDates.count() > maxElements
        val spacerHeight = if (evtOverflow) 0.dp else evtHeight * (maxElements - eventDates.count())
        Column(
            Modifier.padding(2.dp).width(blockW - 4.dp)
                .background(
                    if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ).clickable(onClick = {
                    calendarViewModel.open(DateScreen(then, eventDates))
                })
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp),
                text = then.dayOfMonth.toString(),
            )
            repeat(if (evtOverflow) maxElements - 1 else eventDates.count()) {
                drawEvtInDay(Color.Green, eventDates[it].desc) //fixme get color from Date
            }
            if (evtOverflow)
                drawEvtInDay(Color.Red, "+" + (eventDates.count() - maxElements + 1))
            Spacer(Modifier.height(1.dp + spacerHeight))
        }
    }

    private fun colorToLuminance(color: Color): Double {
        return 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    }

    @Composable
    private fun measureTextLine(style: TextStyle): Dp {
        val textMeasurer = rememberTextMeasurer()
        val linePx = remember(textMeasurer, style) {
            textMeasurer.measure("0", style).size.height
        }
        return with(LocalDensity.current) { linePx.toDp() }
    }

    @Composable
    private fun drawEvtInDay(bgColor: Color, text: String) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp)
                .background(
                    bgColor,
                    shape = RoundedCornerShape(10.dp)
                ).padding(vertical = 1.dp, horizontal = 4.dp),
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
            color = if (colorToLuminance(bgColor) > .5) Color.Black else Color.White
        )
    }

    @Composable
    override fun functionButton(calendarViewModel: MainActivity.CalendarViewModel) {
        Button(onClick = {
            val entry = calendarViewModel.dbManager.createEntry()
            calendarViewModel.open(EntryEdit(calendarViewModel, entry))
        }) {
            Text("十")
//            Text("+")
        }
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