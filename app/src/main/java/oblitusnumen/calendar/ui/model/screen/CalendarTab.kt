package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.LIST_CENTER
import oblitusnumen.calendar.implementation.LIST_LEN
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.implementation.zonedDateTime
import oblitusnumen.calendar.ui.ActionButtonWithScroll
import oblitusnumen.calendar.ui.PositionStatus
import oblitusnumen.calendar.ui.horizontal
import oblitusnumen.calendar.ui.measureTextLine
import oblitusnumen.calendar.ui.model.DateTimePicker
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTab(
    dbManager: DbManager,
    tagsFilter: MutableState<List<Tag>>,
    navBar: @Composable () -> Unit,
    newEntry: () -> Unit,
    openThatDayInfo: (LocalDate) -> Unit,
    openMonthAgenda: (Int, Int) -> Unit,
    openSettings: () -> Unit,
) {
    val now = LocalDate.now()

    val eventsPerDay = 3 // TODO: maxElements setting
    val evtHeight: Dp = getEvtInDayExpectedHeight()
    val halfWeekOffset = (((eventsPerDay + 1f) * with(LocalDensity.current) {
        evtHeight.toPx().toInt()
    }) / 2).toInt()// TODO: measure day
    var contentScrollOffset: Int? = null
    var contentScrollOffsetDown: Int? = null
    var calendarLazyListState: LazyListState? = null

    val scrollTo: suspend (LocalDate) -> Unit = {// TODO: light up the destination day
        calendarLazyListState!!.scrollToItem(
            getNowWeekItemIndexExact(it),
            halfWeekOffset - (calendarLazyListState!!.layoutInfo.viewportEndOffset + contentScrollOffset!! - contentScrollOffsetDown!!) / 2
        )
    }

    var todayPosition by remember { mutableStateOf(PositionStatus.Visible) }

    var tagsFilter by remember { tagsFilter }

    Scaffold(
        topBar = { CalendarTopBar(dbManager, tagsFilter, { tagsFilter = it }, openSettings, scrollTo) },
        bottomBar = navBar,
        floatingActionButton = {
            ActionButtonWithScroll(
                newEntry,
                scrollTo,
                todayPosition
            )
        }
    ) { paddingValues ->
        contentScrollOffset =
            with(LocalDensity.current) { paddingValues.calculateTopPadding().toPx().toInt() }
        contentScrollOffsetDown =
            with(LocalDensity.current) { paddingValues.calculateBottomPadding().toPx().toInt() }
        calendarLazyListState =
            rememberLazyListState(getNowMonthItemWeekIndex(now), -contentScrollOffset)

        var currentMonth by rememberSaveable { mutableStateOf(now) }

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = calendarLazyListState,
                modifier = Modifier.padding(paddingValues.horizontal()),
            ) {
                items(LIST_LEN) {
                    val offset = it - LIST_LEN / 2 // FIXME: extract functions
                    val monthItemIndex = if (offset < 0) 6 + offset % 7 else offset % 7
                    val monthIdx = if (offset < 0) offset / 7 - 1 else offset / 7
                    val mon = LIST_CENTER.plusMonths(monthIdx.toLong()).withDayOfMonth(1)
                    if (monthItemIndex == 0) {
                        HorizontalDivider(Modifier.padding(4.dp))
                        Box(Modifier.fillMaxWidth()) {
                            Box(
                                Modifier.padding(top = 8.dp, bottom = 4.dp).align(Alignment.Center)
                                    .background(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .1f),
                                        RoundedCornerShape(100)
                                    )
                                    .clip(RoundedCornerShape(100))
                                    .clickable { openMonthAgenda(mon.year, mon.month.value) },
                            ) {
                                Text(
                                    stringArrayResource(R.array.monthNames)[mon.month.value - 1] + " " + mon.year,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }

                            IconButton(
                                { openMonthAgenda(mon.year, mon.month.value) },
                                Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowRight, "open month agenda")
                            }
                        }
                    } else {
                        DisplayWeek(
                            dbManager,
                            tagsFilter,
                            evtHeight,
                            mon.monthValue,
                            mon.plusDays(7 * (monthItemIndex - 1) - (mon.dayOfWeek.value - 1).toLong()),
                            eventsPerDay,
                            openThatDayInfo
                        )
                    }
                }
            }

            Box(
                Modifier.padding(top = paddingValues.calculateTopPadding() + 4.dp).align(Alignment.TopCenter)
                    .background(
                        color = MaterialTheme.colorScheme.background.copy(alpha = .5f),
                        RoundedCornerShape(100)
                    )
                    .clip(RoundedCornerShape(100))
                    .clickable { openMonthAgenda(currentMonth.year, currentMonth.month.value) },
            ) {
                Text(
                    stringArrayResource(R.array.monthNames)[currentMonth.month.value - 1] + " " + currentMonth.year,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            LaunchedEffect(calendarLazyListState, getNowWeekItemIndexExact(now)) {
                snapshotFlow { calendarLazyListState.layoutInfo }
                    .collect { layoutInfo ->
                        val targetItemIdx = getNowWeekItemIndexExact(now)
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
                                    if (contentScrollOffset > targetItemOffset)
                                        PositionStatus.Above
                                    else if (layoutInfo.viewportEndOffset - contentScrollOffsetDown < targetItemOffset + targetItemSize)
                                        PositionStatus.Below
                                    else
                                        PositionStatus.Visible
                                }
                            }
                        }
                        if (newPositionStatus != todayPosition)
                            todayPosition = newPositionStatus

                        for (info in visibleItemsInfo) {
                            if (info.offset >= contentScrollOffset) {
                                val newCurrentMonth = getCurrentMonthFromWeekItemIndex(info.index)
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

@Composable
fun DisplayWeek(
    dbManager: DbManager,
    tagsFilter: List<Tag>,
    cachedEvtExpectedHeight: Dp,
    monthValue: Int,
    startOfWeek: LocalDate,
    eventsPerDay: Int,
    toThatDayInfo: (LocalDate) -> Unit
) {
    var date = startOfWeek
    if (date.monthValue != monthValue && date.plusDays(6).monthValue != monthValue)
        return
    val now = LocalDate.now()

    Row {
        val dates =
            ViewDateWithOptions.getAll(
                dbManager,
                zonedDateTime(date).toEpochSecond(),
                zonedDateTime(date.plusWeeks(1)).toEpochSecond(),
                tagsFilter.map { it.id!! }
            )

        while (true) {
            DisplayDay(
                Modifier.weight(1f / 7),
                cachedEvtExpectedHeight,
                now,
                eventsPerDay,
                date,
                dates,
                date.monthValue == monthValue,
                toThatDayInfo
            )
            if (date.dayOfWeek.value == 7) break
            date = date.plusDays(1)
        }
    }
}

@Composable
fun DisplayDay(
    modifier: Modifier,
    cachedEvtExpectedHeight: Dp,
    now: LocalDate,
    maxElements: Int,
    then: LocalDate,
    dates: List<ViewDateWithOptions>,
    isThatMonth: Boolean,
    toThatDayInfo: (LocalDate) -> Unit
) {
    val begin = zonedDateTime(then)
    val startOfDayCache = begin.toEpochSecond()
    val endOfDayCache = begin.plusDays(1).toEpochSecond()
    val eventDates = remember(dates) {
        dates.filter {
            it.anyInRange(
                startOfDayCache,
                endOfDayCache
            ) != null
        }
            .sortedBy { it.anyInRange(startOfDayCache, endOfDayCache) }
    }

    val today = (now.year == then.year && now.month == then.month && then.dayOfMonth == now.dayOfMonth)
    var bgColor = if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    bgColor = if (isThatMonth) bgColor else bgColor.copy(alpha = .5f)
    val evtOverflow = eventDates.count() > maxElements
    val spacerHeight = if (evtOverflow) 0.dp else cachedEvtExpectedHeight * (maxElements - eventDates.count())

    Column(
        modifier.padding(1.dp)
            .background(
                bgColor,
                shape = RoundedCornerShape(10.dp)
            ).clip(shape = RoundedCornerShape(10.dp))
            .clickable(onClick = { toThatDayInfo(then) })
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .padding(top = 2.dp),
            text = then.dayOfMonth.toString(),
            color = if (isThatMonth) bgColorToTextColor(bgColor) else bgColorToTextColor(bgColor).copy(alpha = .5f),
        )

        repeat(if (evtOverflow) maxElements - 1 else eventDates.count()) {
            DrawEvtInDay(
                eventDates[it].color,
                eventDates[it].displayName,
                isThatMonth
            ) //fixme get color from Date. should cache these vals in Date
        }

        if (evtOverflow)
            Text(
                modifier = Modifier.padding(vertical = 1.dp).align(Alignment.CenterHorizontally),
                text = "+" + (eventDates.count() - maxElements + 1),
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = bgColorToTextColor(bgColor).let {
                    if (isThatMonth)
                        it
                    else
                        it.copy(alpha = .5f)
                }
            )

        Spacer(Modifier.height(1.dp + spacerHeight))
    }
}

@Composable
private fun DrawEvtInDay(bgColor: Color, text: String, isThatMonth: Boolean) {
    val bgColor = if (isThatMonth) bgColor else bgColor.copy(alpha = .5f)
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp)
            .background(
                bgColor,
                shape = RoundedCornerShape(10.dp)
            ).padding(vertical = 1.dp, horizontal = 4.dp),
        text = text,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 10.sp,
        color = bgColorToTextColor(bgColor)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTopBar(
    dbManager: DbManager,
    tagsFilter: List<Tag>,
    tagsFilterUpdate: (List<Tag>) -> Unit,
    openSettings: () -> Unit,
    scrollTo: suspend (LocalDate) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val dateTimePicker = remember {
        DateTimePicker()
    }
    val coroutineScope = rememberCoroutineScope()

    dateTimePicker.tryCompose()

    Column {
        CenterAlignedTopAppBar(
            colors = topBarColors(),
            scrollBehavior = scrollBehavior,
            navigationIcon = {
                IconButton(onClick = openSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null
                    )
                }
            },
            title = {
                var isFilterOpen by remember { mutableStateOf(false) }
                if (isFilterOpen)
                    TagFilterMenu(dbManager, tagsFilter, { isFilterOpen = false }, tagsFilterUpdate)

                Row(
                    Modifier.background(
                        MaterialTheme.colorScheme.background.copy(alpha = .5f),
                        shape = RoundedCornerShape(100)
                    )
                        .clip(RoundedCornerShape(100))
                        .height(40.dp).fillMaxWidth().clickable { isFilterOpen = true }
                ) {
                    LazyRow(Modifier.weight(1f)/*.clip(RoundedCornerShape(100))*/) {
                        for (tag in tagsFilter)
                            item {
                                DrawTag(dbManager, tag, { isFilterOpen = true }) { tagsFilterUpdate(tagsFilter - tag) }
                            }
                    }
                    Icon(
                        Icons.Filled.Face,
                        contentDescription = "filter",
                        Modifier.size(40.dp),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
                    )
                }
            },
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

        Box(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f))
                .clickable(false) {}
        ) {
            Row(
                Modifier.padding(horizontal = MainActivity.PADDING).height(IntrinsicSize.Min)
                    .defaultMinSize(minHeight = 25.dp).fillMaxWidth(),
            ) {
                repeat(7) {
                    if (it != 0)
                        VerticalDivider(Modifier.padding(vertical = 2.dp))
                    Text(
                        stringArrayResource(R.array.weekdayNames)[it],
                        modifier = Modifier.align(Alignment.CenterVertically).weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        HorizontalDivider() // FIXME: use bar shadow instead
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterMenu(dbManager: DbManager, chosenTags: List<Tag>, onClose: () -> Unit, tagAcceptor: (List<Tag>) -> Unit) {
    val allTags = Tag.all(dbManager).groupingBy { it.name }.reduce { _, accumulator, _ -> accumulator }
    val chosenTags: MutableSet<String> = chosenTags.map { it.name }.toMutableSet()
    var searchTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClose()
                tagAcceptor(chosenTags.map {
                    allTags[it]!!
                })
            }) {
                Text("OK")
            }
        },
        text = {
            Column {
                OutlinedTextField(// FIXME: ui paddings
                    modifier = Modifier.padding(horizontal = 8.dp),
                    value = searchTag, onValueChange = {
                        searchTag = it
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    label = { Text("Tag name") }
                )

                FlowRow(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                ) {
                    searchTag // FIXME: yet another filthy hack

                    for (tag in chosenTags) {
                        DrawTag(
                            dbManager,
                            tag,
                            allTags[tag]!!,
                            true
                        ) { if (it) chosenTags += tag else chosenTags -= tag }
                    }

                    for (tag in allTags.values) {
                        if (!chosenTags.contains(tag.name) && tag.name.contains(
                                searchTag,
                                true
                            ) && tag.name != searchTag
                        ) DrawTag(
                            dbManager,
                            tag.name,
                            tag,
                            false
                        ) { if (it) chosenTags += tag.name else chosenTags -= tag.name }
                    }
                }
            }
        }
    )
}

@Composable
fun DrawTag(dbManager: DbManager, name: String, tag: Tag, chosen: Boolean, onChooseToggle: (Boolean) -> Unit) {
    var selected by remember(name) { mutableStateOf(chosen) }
    val bgColor = tag.colorOrDefault(dbManager)

    InputChip(
        selected,
        {
            selected = !selected
            onChooseToggle(selected)
        },
        {
            Text(
                name, style = MaterialTheme.typography.bodyLarge,
                color = bgColorToTextColor(bgColor)
            )
        },
        modifier = Modifier.padding(horizontal = 4.dp),
        trailingIcon = {
            if (selected) Icon(
                Icons.Filled.Done, null,
                tint = bgColorToTextColor(bgColor)
            )
        },
        colors = InputChipDefaults.inputChipColors(containerColor = bgColor, selectedContainerColor = bgColor),
    )
}

@Composable
fun DrawTag(dbManager: DbManager, tag: Tag, openFilter: () -> Unit, rmTag: () -> Unit) {
    val bgColor = tag.colorOrDefault(dbManager)

    InputChip(
        false,
        openFilter,
        {
            Box {
                Text(
                    tag.name, modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodyMedium,
                    color = bgColorToTextColor(bgColor)
                )
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        trailingIcon = {
            IconButton(onClick = rmTag, Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Clear, null,
                    Modifier.size(24.dp),
                    tint = bgColorToTextColor(bgColor)
                )
            }
        },
        colors = InputChipDefaults.inputChipColors(containerColor = bgColor),
    )
}

fun getNowMonthItemWeekIndex(now: LocalDate) =
    (LIST_LEN / 2 + ChronoUnit.MONTHS.between(LIST_CENTER, now) * 7).toInt()

fun getNowWeekItemIndexExact(now: LocalDate) =
    getNowMonthItemWeekIndex(now) + (now.dayOfMonth + now.withDayOfMonth(1).dayOfWeek.value + 5) / 7

fun getCurrentMonthFromWeekItemIndex(index: Int): LocalDate {
    val effectiveIndex = index - LIST_LEN / 2 - 1
    return LIST_CENTER.plusMonths(((if (effectiveIndex < 0) effectiveIndex - 7 else effectiveIndex) / 7).toLong())
}

@Composable
fun getEvtInDayExpectedHeight(): Dp = measureTextLine(MaterialTheme.typography.bodySmall) + 4.dp


@Preview
@Composable
fun DisplayDayPreview() {
    val cachedEvtExpectedHeight: Dp = getEvtInDayExpectedHeight()
    val monthValue = 1
    val date0: LocalDate = LocalDate.of(2026, 1, 26)
    var date = date0
    val now = LocalDate.of(2026, 1, 28)
    val dates = listOf<ViewDateWithOptions>()

    Row {
        while (true) {
            DisplayDay(
                Modifier.weight(1f / 7),
                cachedEvtExpectedHeight,
                now,
                3,
                date,
                dates,
                date.monthValue == monthValue
            ) {}// TODO: maxElements setting
            if (date.dayOfWeek.value == 7) break
            date = date.plusDays(1)
        }
    }
}