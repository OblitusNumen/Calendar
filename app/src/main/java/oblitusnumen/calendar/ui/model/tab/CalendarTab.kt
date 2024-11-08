package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import oblitusnumen.calendar.MainActivity.Companion.LIST_CENTER
import oblitusnumen.calendar.R
import oblitusnumen.calendar.getWidthPartIncludePadding
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.measureTextLine
import oblitusnumen.calendar.implementation.zonedDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CalendarTab(
    private val dbManager: DbManager,
    private val toThatDayInfo: (LocalDate, List<Date>) -> Unit,
    private val newEntry: (Entry) -> Unit
) : ViewModel() {
    private var calendarLazyListState: LazyListState? = null

    @Composable
    fun compose(modifier: Modifier = Modifier) {
        Column(modifier) {
            Row {
                repeat(7) {
                    Text(
                        stringArrayResource(R.array.weekdayNames)[it],
                        modifier = Modifier.width(getWidthPartIncludePadding(7f)).height(25.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            val now = LocalDate.now()
            if (calendarLazyListState == null) calendarLazyListState = rememberLazyListState(getNowItemIndex(now))
            LazyColumn(
                state = calendarLazyListState!!
            ) {
                items(Int.MAX_VALUE, itemContent = {
                    val offset = it - Int.MAX_VALUE / 2
                    val monthItemIndex = if (offset < 0) 6 + offset % 7 else offset % 7
                    val monthIdx = if (offset < 0) offset / 7 - 1 else offset / 7
                    val mon = LIST_CENTER.plusMonths(monthIdx.toLong()).withDayOfMonth(1)
                    if (monthItemIndex == 0) {
                        Text(stringArrayResource(R.array.monthNames)[mon.month.value - 1] + " " + mon.year)
                    } else {
                        displayWeek(
                            mon.monthValue,
                            mon.plusDays(7 * (monthItemIndex - 1) - (mon.dayOfWeek.value - 1).toLong())
                        )
                    }
                })
            }
        }
    }

    private fun getNowItemIndex(now: LocalDate) =
        (Int.MAX_VALUE / 2 + ChronoUnit.MONTHS.between(LIST_CENTER, now) * 7).toInt()

    @Composable
    fun displayWeek(monthValue: Int, date0: LocalDate) {
        var date = date0
        val blockW = getWidthPartIncludePadding(7f)
        Row {
            while (date.month.value % 12 + 1 == monthValue) {
                Spacer(Modifier.width(blockW))
                date = date.plusDays(1)
            }
            val dates =
                dbManager.getDates(
                    date,
                    date.plusWeeks(1)
                )
            while (date.month.value == monthValue) {
                displayDay(blockW, 3, date, dates)
                if (date.dayOfWeek.value == 7) break
                date = date.plusDays(1)
            }
        }
    }

    @Composable
    fun displayDay(
        blockW: Dp,
        maxElements: Int,
        then: LocalDate,
        dates: List<Date>
    ) {
        val now = LocalDate.now()
        val begin = zonedDateTime(then)
        val eventDates = dates.filter { date -> date.forDay(begin) != null }.sortedBy { it.forDay(begin) }

        val evtHeight = measureTextLine(MaterialTheme.typography.bodySmall) + 4.dp
        val today = (now.year == then.year && now.month == then.month && then.dayOfMonth == now.dayOfMonth)
        val bgColor = if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
        val evtOverflow = eventDates.count() > maxElements
        val spacerHeight = if (evtOverflow) 0.dp else evtHeight * (maxElements - eventDates.count())
        Column(
            Modifier.padding(2.dp).width(blockW - 4.dp)
                .background(
                    bgColor,
                    shape = RoundedCornerShape(10.dp)
                ).clickable(onClick = { toThatDayInfo(then, eventDates) })
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp),
                text = then.dayOfMonth.toString(),
                color = bgColorToTextColor(bgColor)
            )
            repeat(if (evtOverflow) maxElements - 1 else eventDates.count()) {
                drawEvtInDay(Color.Green, eventDates[it].desc) //fixme get color from Date
            }
            if (evtOverflow)
                drawEvtInDay(Color.Red, "+" + (eventDates.count() - maxElements + 1))
            Spacer(Modifier.height(1.dp + spacerHeight))
        }
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
            color = bgColorToTextColor(bgColor)
        )
    }

    @Composable
    fun functionButton() {
        FloatingActionButton(onClick = { newEntry(dbManager.createEntry()) }) {
            Icon(Icons.Filled.Add, "add event")
        }
    }

    // TODO:
    @Composable
    fun topBar() {
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