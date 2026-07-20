package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.LIST_CENTER
import oblitusnumen.calendar.implementation.LIST_LEN
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.ui.PositionStatus
import oblitusnumen.calendar.ui.element.*
import oblitusnumen.calendar.ui.horizontal
import oblitusnumen.calendar.ui.measureTextLine
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

@Composable
fun AgendaScreen(
    dbManager: DbManager,
    monthDate: MonthDate,
    tagsFilter: MutableState<List<Tag>>,
    navBar: @Composable () -> Unit,
    newEntry: () -> Unit,//TODO current day input
    openThatDayInfo: (LocalDate) -> Unit,
    openEntryInfoByDateOccurrence: (DateOccurrence) -> Unit,
    backPress: () -> Unit,
) {
    val now = LocalDate.now()

    var monthDisplayShown by rememberSaveable { mutableStateOf(false) }

    var contentScrollOffset: Int? = null
    var calendarLazyListState: LazyListState? = null

    val scrollTo: suspend (LocalDate) -> Unit = {
        calendarLazyListState!!.scrollToItem(
            getNowDayItemIndexExact(it),
            -contentScrollOffset!!
        )
        monthDisplayShown = false
    }

    var todayPosition by remember { mutableStateOf(PositionStatus.Visible) }

    var tagsFilter by remember { tagsFilter }

    Scaffold(
        topBar = { AgendaTopBar(dbManager, tagsFilter, { tagsFilter = it }, backPress, scrollTo) },
        bottomBar = navBar,
        floatingActionButton = {
            ActionButtonWithScroll(
                newEntry,
                scrollTo,
                todayPosition
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            contentScrollOffset =
                with(LocalDensity.current) { paddingValues.calculateTopPadding().toPx().toInt() }
            val contentScrollOffsetDown: Int =
                with(LocalDensity.current) { paddingValues.calculateBottomPadding().toPx().toInt() }

            val initialMonthDate = LocalDate.of(monthDate.year, monthDate.month, monthDate.dayOfMonth ?: 1)
            calendarLazyListState =
                rememberLazyListState(getNowDayItemIndexExact(initialMonthDate), -contentScrollOffset)

            LazyColumn(
                state = calendarLazyListState,
                modifier = Modifier.padding(paddingValues.horizontal()).fillMaxWidth(),
            ) {
                items(LIST_LEN) {
                    val offset = it - LIST_LEN / 2 // FIXME: extract functions
                    val dayOfMonth = if (offset < 0) 31 + offset % 32 else offset % 32
                    val monthIdx = if (offset < 0) offset / 32 - 1 else offset / 32
                    val monthDay = LIST_CENTER.plusMonths(monthIdx.toLong())
                    if (dayOfMonth == 0 || monthDay.lengthOfMonth() < dayOfMonth)
                        return@items
                    val day = monthDay.withDayOfMonth(dayOfMonth)
                    DisplayDayAgenda(
                        dbManager,
                        tagsFilter,
                        day,
                        now,
                        monthDate.dayOfMonth != null && day == initialMonthDate || day == now,
                        { openThatDayInfo(day) },
                        openEntryInfoByDateOccurrence
                    )
                }
            }

            LaunchedEffect(calendarLazyListState, getNowDayItemIndexExact(now)) {
                snapshotFlow { calendarLazyListState.layoutInfo }
                    .collect { layoutInfo ->
                        val todayItemChangeViewportOffset =
                            (layoutInfo.viewportEndOffset + contentScrollOffset - contentScrollOffsetDown) / 2
                        val targetItemIdx = getNowDayItemIndexExact(now)
                        val visibleItemsInfo = layoutInfo.visibleItemsInfo
                        val newPositionStatus = if (visibleItemsInfo.isEmpty())
                            PositionStatus.Visible
                        else {
                            val firstVisibleIdx = visibleItemsInfo.first().index
                            val lastVisibleIdx = visibleItemsInfo.last().index
                            if (targetItemIdx < firstVisibleIdx)
                                PositionStatus.Above
                            else {
                                if (lastVisibleIdx < targetItemIdx)
                                    PositionStatus.Below
                                else {
                                    val targetItemInfoIndex = targetItemIdx - firstVisibleIdx
                                    val targetItemInfo: LazyListItemInfo =
                                        visibleItemsInfo.elementAt(targetItemInfoIndex)
                                    val targetItemOffset = targetItemInfo.offset
                                    val targetItemSize = targetItemInfo.size
                                    if (targetItemOffset > todayItemChangeViewportOffset)
                                        PositionStatus.Below
                                    else if (todayItemChangeViewportOffset < targetItemOffset + targetItemSize)
                                        PositionStatus.Visible
                                    else
                                        PositionStatus.Above
                                }
                            }
                        }
                        if (newPositionStatus != todayPosition)
                            todayPosition = newPositionStatus
                    }
            }

            var currentMonth by rememberSaveable { mutableStateOf(initialMonthDate) }

            if (monthDisplayShown) {
                var currentDay by rememberSaveable { mutableStateOf(initialMonthDate) }
                val pagerState = rememberPagerState(
                    initialPage = (LIST_LEN / 2 + ChronoUnit.MONTHS.between(
                        LIST_CENTER,
                        currentMonth
                    )).toInt(), pageCount = { LIST_LEN })
                var pagerMonth by rememberSaveable { mutableStateOf(initialMonthDate) }
                val coroutineScope = rememberCoroutineScope()

                HorizontalPager(pagerState, verticalAlignment = Alignment.Top) {
                    val currentMonth = LIST_CENTER.plusMonths((it - LIST_LEN / 2).toLong())

                    Column(
                        Modifier.padding(top = paddingValues.calculateTopPadding() + 4.dp).padding(horizontal = 16.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                color = MaterialTheme.colorScheme.background.copy(alpha = .5f),
                                RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(false) { }
                    ) {
                        Box(Modifier.fillMaxWidth().clickable { monthDisplayShown = false }) {
                            IconButton(
                                {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pagerState.currentPage - 1)
                                    }
                                }, Modifier.align(Alignment.CenterStart).size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, null)
                            }

                            Text(
                                stringArrayResource(R.array.monthNames)[currentMonth.month.value - 1] + " " + currentMonth.year,
                                modifier = Modifier.align(Alignment.Center).padding(vertical = 4.dp),
                                style = MaterialTheme.typography.titleLarge
                            )

                            IconButton(
                                {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pagerState.currentPage + 1)
                                    }
                                }, Modifier.align(Alignment.CenterEnd).size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, null)
                            }
                        }

                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { dayOfWeek ->
                                Box(Modifier.weight(1f / 7).padding(2.dp).align(Alignment.CenterVertically)) {
                                    Box(
                                        Modifier
                                            .size(measureTextLine(LocalTextStyle.current) + 2.dp)
                                            .align(Alignment.Center)
                                    ) {
                                        Text(
                                            stringArrayResource(R.array.weekdayNames)[dayOfWeek][0].toString(),
                                            Modifier.align(Alignment.Center),
                                            MaterialTheme.colorScheme.onBackground.copy(.5f)
                                        )
                                    }
                                }
                            }
                        }

                        val currentMonthValue = remember(currentMonth) { currentMonth.monthValue }
                        repeat(6) { weekIndex ->
                            val weekStart =
                                remember(currentMonth) { currentMonth.plusDays(7 * (weekIndex) - (currentMonth.dayOfWeek.value - 1).toLong()) }
                            if (weekStart.monthValue != currentMonthValue && weekStart.plusDays(6).monthValue != currentMonthValue)
                                return@repeat
                            val coroutineScope = rememberCoroutineScope()

                            Row(Modifier.fillMaxWidth()) {
                                repeat(7) { dayOfWeek ->
                                    val day = weekStart.plusDays(dayOfWeek.toLong())
                                    val isToday = day == now
                                    val isCurrentDay = day == currentDay
                                    val isThisMonth = day.monthValue == currentMonthValue
                                    Box(Modifier.weight(1f / 7).padding(2.dp).align(Alignment.CenterVertically)) {
                                        Box(
                                            Modifier
                                                .size(measureTextLine(LocalTextStyle.current) + 2.dp)
                                                .align(Alignment.Center)
                                                .clip(RoundedCornerShape(100))
                                                .border(
                                                    1.dp,
                                                    if (isCurrentDay) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(100)
                                                )
                                                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable {
                                                    coroutineScope.launch {
                                                        scrollTo(day)
                                                    }
                                                }
                                        ) {
                                            Text(
                                                "${day.dayOfMonth}",
                                                Modifier.align(Alignment.Center),
                                                if (isToday)
                                                    MaterialTheme.colorScheme.background
                                                else if (isThisMonth)
                                                    MaterialTheme.colorScheme.onBackground
                                                else
                                                    MaterialTheme.colorScheme.onBackground.copy(.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(IntrinsicSize.Max))
                    }
                }

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.layoutInfo }
                        .collect { layoutInfo ->
                            val visiblePagesInfo = layoutInfo.visiblePagesInfo
                            if (visiblePagesInfo.isEmpty())
                                return@collect
                            val day = LIST_CENTER.plusMonths((pagerState.currentPage - LIST_LEN / 2).toLong())
                            if (pagerMonth != day)
                                pagerMonth = day
                        }
                }

                LaunchedEffect(calendarLazyListState, getNowDayItemIndexExact(now)) {
                    snapshotFlow { calendarLazyListState.layoutInfo }
                        .collect { layoutInfo ->
                            val visibleItemsInfo = layoutInfo.visibleItemsInfo

                            val monthChangeViewportOffset =
                                (layoutInfo.viewportEndOffset + 2 * contentScrollOffset - contentScrollOffsetDown) / 3

                            for (info in visibleItemsInfo) {
                                if (info.offset >= monthChangeViewportOffset || info.offset + info.size > monthChangeViewportOffset) {
                                    val newCurrentMonth = getCurrentMonthFromDayItemIndex(info.index)
                                    if (newCurrentMonth != currentMonth) {
                                        if (pagerMonth == currentMonth) {
                                            pagerMonth = newCurrentMonth
                                            pagerState.scrollToPage(
                                                (LIST_LEN / 2 + ChronoUnit.MONTHS.between(
                                                    LIST_CENTER,
                                                    newCurrentMonth
                                                )).toInt()
                                            )
                                        }
                                        currentMonth = newCurrentMonth
                                    }
                                    val offset = info.index - LIST_LEN / 2 // FIXME: extract functions
                                    val dayOfMonth = min(
                                        newCurrentMonth.lengthOfMonth(),
                                        max(1, if (offset < 0) 31 + offset % 32 else offset % 32)
                                    )
                                    val newCurrentDay = newCurrentMonth.withDayOfMonth(dayOfMonth)
                                    if (newCurrentDay != currentDay) {
                                        currentDay = newCurrentDay
                                    }
                                    break
                                }
                            }
                        }
                }
            } else {
                Box(
                    Modifier
                        .padding(top = paddingValues.calculateTopPadding() + 4.dp).align(Alignment.TopCenter)
                        .background(
                            color = MaterialTheme.colorScheme.background.copy(alpha = .5f),
                            RoundedCornerShape(100)
                        )
                        .clip(RoundedCornerShape(100))
                        .clickable {
                            monthDisplayShown = true
                        },
                ) {
                    Text(
                        stringArrayResource(R.array.monthNames)[currentMonth.month.value - 1] + " " + currentMonth.year,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                LaunchedEffect(calendarLazyListState, getNowDayItemIndexExact(now)) {
                    snapshotFlow { calendarLazyListState.layoutInfo }
                        .collect { layoutInfo ->
                            val visibleItemsInfo = layoutInfo.visibleItemsInfo

                            val monthChangeViewportOffset =
                                (layoutInfo.viewportEndOffset + 2 * contentScrollOffset - contentScrollOffsetDown) / 3

                            for (info in visibleItemsInfo) {
                                if (info.offset >= monthChangeViewportOffset || info.offset + info.size > monthChangeViewportOffset) {
                                    val newCurrentMonth = getCurrentMonthFromDayItemIndex(info.index)
                                    if (newCurrentMonth != currentMonth) {
                                        currentMonth = newCurrentMonth
                                    }
                                    break
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun DisplayDayAgenda(
    dbManager: DbManager,
    tagsFilter: List<Tag>,
    day: LocalDate,
    now: LocalDate,
    drawUnconditionally: Boolean = false,
    openDayInfo: () -> Unit,
    openEntryInfoByDateOccurrence: (DateOccurrence) -> Unit
) {
    val dates =
        remember(day, tagsFilter) { ViewDateWithOptions.occurrencesForDay(dbManager, day, tagsFilter.map { it.id!! }) }
//    if (dates.isEmpty() && !drawUnconditionally)
//        return
    // FIXME: ------------------------------------------------------------------------------------------------------- 
    Column {
        AgendaDayHeader(day, now, openDayInfo)

        if (dates.isEmpty()) {
            InfoRow(
                icon = null,
                text = stringResource(R.string.agenda_no_events),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            for (occurrence in dates) {
                Entry(dbManager, occurrence) { openEntryInfoByDateOccurrence(occurrence) }
            }
        }

        SectionDivider()
    }
}

@Composable
private fun AgendaDayHeader(day: LocalDate, now: LocalDate, onClick: () -> Unit) {
    val isToday = day == now
    val bgColor = if (isToday) MaterialTheme.colorScheme.onSurface.copy(alpha = .5f) else MaterialTheme.colorScheme.surface

    val label: String? = when (day) {
        now -> stringResource(R.string.agenda_today)
        now.plusDays(1) -> stringResource(R.string.agenda_tomorrow)
        now.minusDays(1) -> stringResource(R.string.agenda_yesterday)
        else -> null
    }

    val dateStr = if (day.year == now.year)
        day.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    else
        day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

    Row(
        Modifier.fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) bgColorToTextColor(bgColor) else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                dateStr,
                style = MaterialTheme.typography.titleSmall,
                color = if (isToday) bgColorToTextColor(bgColor) else MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            Modifier.size(16.dp),
            tint = if (isToday) bgColorToTextColor(bgColor).copy(alpha = .6f)
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaTopBar(
    dbManager: DbManager,
    tagsFilter: List<Tag>,
    tagsFilterUpdate: (List<Tag>) -> Unit,
    backPress: () -> Unit,
    scrollTo: suspend (LocalDate) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val dateTimePicker = remember {
        DateTimePicker()
    }
    val coroutineScope = rememberCoroutineScope()

    dateTimePicker.tryCompose()

    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = { BackPressButton(backPress) },
        title = { TopBarTagFilterTitle(dbManager, tagsFilter, tagsFilterUpdate) },
        actions = {
            IconButton(onClick = {
                dateTimePicker.datePick({}, {
                    coroutineScope.launch {
                        scrollTo(it)
                    }
                })
            }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null
                )
            }
        },
    )
}

fun getNowMonthItemDayIndex(now: LocalDate) =
    (LIST_LEN / 2 + ChronoUnit.MONTHS.between(LIST_CENTER, now) * 32).toInt()

fun getNowDayItemIndexExact(now: LocalDate) =
    getNowMonthItemDayIndex(now) + now.dayOfMonth

fun getCurrentMonthFromDayItemIndex(index: Int): LocalDate {
    val effectiveIndex = index - LIST_LEN / 2
    return LIST_CENTER.plusMonths(((if (effectiveIndex < 0) effectiveIndex - 32 else effectiveIndex) / 32).toLong())
}

data class MonthDate(val year: Int, val month: Int, val dayOfMonth: Int?)
