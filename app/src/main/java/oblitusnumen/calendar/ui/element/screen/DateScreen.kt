package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.LIST_LEN
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.ui.dpByDpForPixelPerfect
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.NewEntryFunctionButton
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

@Composable
fun DateScreen(
    dbManager: DbManager,
    day: LocalDate,
    openAgenda: (Int, Int, Int?) -> Unit,
    openEntryInfoByDateOccurrence: (DateOccurrence) -> Unit,
    openEditNewEntry: (LocalDate) -> Unit,
    onBackPress: () -> Unit
) {
    var pagerDay by remember { mutableStateOf(day) }

    Scaffold(
        topBar = {
            DateTopBar(
                pagerDay,
                { openAgenda(pagerDay.year, pagerDay.monthValue, pagerDay.dayOfMonth) },
                onBackPress
            )
        },
        floatingActionButton = { NewEntryFunctionButton { openEditNewEntry(pagerDay) } }
    ) { paddingValues ->
        val pagerState = rememberPagerState(initialPage = LIST_LEN / 2, pageCount = { LIST_LEN })
        val scrollState = rememberScrollState()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { index ->
            val day = day.plusDays((index - LIST_LEN / 2).toLong())

            Column(Modifier.verticalScroll(scrollState)) {
                Spacer(Modifier.height(paddingValues.calculateTopPadding()))

                Box {
                    repeat(24) {
                        HorizontalDivider(Modifier.padding(top = minutesToDp(it * 60)).height(1.dp))
                    }
                    Row {
                        Box(Modifier.height(minutesToDp(1440)).padding(end = 2.dp)) {
                            repeat(24) {
                                Text(
                                    "$it:00",
                                    Modifier.padding(top = minutesToDp(it * 60) + 1.dp).align(Alignment.TopEnd)
                                )
                            }
                        }
                        Box {
                            Day(dbManager, day, openEntryInfoByDateOccurrence)
                        }
                    }
                }

                Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }

        LaunchedEffect(pagerState, day) {
            snapshotFlow { pagerState.layoutInfo }
                .collect { layoutInfo ->
                    val visiblePagesInfo = layoutInfo.visiblePagesInfo
                    if (visiblePagesInfo.isEmpty())
                        return@collect
                    val day = day.plusDays((pagerState.currentPage - LIST_LEN / 2).toLong())
                    if (pagerDay != day)
                        pagerDay = day
                }
        }
    }
}

@Composable
fun Day(
    dbManager: DbManager,
    day: LocalDate,
    openEntryInfoByDateOccurrence: (DateOccurrence) -> Unit
) {
    val columns: List<List<DateOccurrence>> = remember {
        val dates = ViewDateWithOptions.occurrencesIntersectingDay(dbManager, day).sortedBy { it.occurrence }
        val columns = mutableListOf<MutableList<DateOccurrence>>()

        for (date in dates) {
            var assigned = false

            for (column in columns) {
                // end of last + spacer + duration normalization < start of pending
                if (column.last().endEpochSecond() +
                    MIN_SPACE_BETWEEN_OCCURRENCES_SECONDS +
                    (MIN_OCCURRENCE_SIZE_MINUTES * 60 - column.last().date.duration.secondsApproximation()).let {
                        if (it > 0) it else 0
                    } < date.startEpochSecond()
                ) {
                    column.add(date)
                    assigned = true
                    break
                }
            }

            if (!assigned)
                columns.add(mutableListOf(date))
        }
        columns
    }

    val startOfDay = day.atStartOfDay()
    val endOfDay = startOfDay.plusDays(1)
    val endOfDayMinutesOffset = Duration.between(startOfDay, endOfDay).toMinutes().toInt()

    Row {
        for (column in columns) {
            Column(Modifier.weight(1f / columns.size)) {
                var endOfLast = startOfDay
                var endOfLastMinutesOffset = 0

                for (occurrence in column) {
                    val start = occurrence.occurrence.withSecond(0)
                    val end = getZonedFromEpochSeconds(occurrence.endEpochSecond()).toLocalDateTime().withSecond(0)
                    val endMinutesOffset = Duration.between(startOfDay, end).toMinutes().toInt()

                    if (start > endOfLast) {
                        val startMinutesOffset = Duration.between(startOfDay, start).toMinutes().toInt()

                        Spacer(Modifier.height(minutesToDp(startMinutesOffset - endOfLastMinutesOffset)))

                        endOfLastMinutesOffset = startMinutesOffset
                    }

                    val size = min(endMinutesOffset, endOfDayMinutesOffset) - endOfLastMinutesOffset
                    endOfLastMinutesOffset = endMinutesOffset
                    endOfLast = end

                    EntryBox(
                        occurrence,
                        size,
                        {
                            val date = occurrence.date
                            date.addExceptions(occurrence.occurrenceZoned.toLocalDate())
                            date.update(dbManager)
                            dbManager.tryScheduleNotification()
                        }
                    ) {
                        openEntryInfoByDateOccurrence(occurrence)
                    }
                }
            }
        }
    }
}

@Composable
fun EntryBox(
    occurrence: DateOccurrence,
    minutesSize: Int,
    doExclude: () -> Unit,
    openEntryInfo: () -> Unit
) {
    var excludeDateShown by remember { mutableStateOf(false) }

    Box(
        Modifier.height(minutesToDp(max(minutesSize, MIN_OCCURRENCE_SIZE_MINUTES))).fillMaxWidth()
            .combinedClickable(onLongClick = { excludeDateShown = true }, onClick = openEntryInfo).let {
                if (minutesSize != 0)
//                    it.padding(horizontal = 1.dp).border(1.dp, occurrence.date.color)
                    it
                        .padding(horizontal = 1.dp)
//                        .border(1.dp, MaterialTheme.colorScheme.primary)
                        .background(occurrence.date.color)
                else
                    it
            }
    ) {
        if (minutesSize == 0)
            Box(
                modifier = Modifier.height(2.dp).fillMaxWidth().padding(top = minutesToDp(minutesSize))
                    .background(occurrence.date.color)
            )

        Text(
            modifier = Modifier.padding(2.dp).align(Alignment.TopStart),
            color = if (minutesSize == 0) MaterialTheme.colorScheme.onBackground else bgColorToTextColor(occurrence.date.color),
            text = occurrence.date.displayName,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (excludeDateShown)
        ExcludeOccurrenceDialog(occurrence.occurrence, occurrence.date.displayName, {
            doExclude()
            excludeDateShown = false
        }) { excludeDateShown = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTopBar(day: LocalDate, openAgenda: () -> Unit, backPress: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = { BackPressButton(backPress) },
        title = {
            Row {
                Text("Date $day", Modifier.weight(1f).align(Alignment.CenterVertically), maxLines = 1)

                IconButton(openAgenda, Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "open month agenda")
                }
            }
        },
    )
}

@Composable
fun ExcludeOccurrenceDialog(occurrence: LocalDateTime, name: String, doExclude: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(doExclude) {
                Text("OK")
            }
        },
        text = {
            Column {
                Text("Exclude $occurrence from $name")
            }
        }
    )
}

@Composable
fun minutesToDp(minutes: Int) = dpByDpForPixelPerfect(minutes.toFloat())

const val MIN_OCCURRENCE_SIZE_MINUTES = 30

const val MIN_SPACE_BETWEEN_OCCURRENCES_SECONDS = 200