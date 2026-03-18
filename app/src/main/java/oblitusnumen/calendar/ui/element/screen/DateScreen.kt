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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.LIST_LEN
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.ExceptionRules
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.ui.dpByDpForPixelPerfect
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.NewEntryFunctionButton
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

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
        val vScrollState = rememberScrollState()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { index ->
            val day = day.plusDays((index - LIST_LEN / 2).toLong())

            Column(Modifier.verticalScroll(vScrollState)) {
                Spacer(Modifier.height(paddingValues.calculateTopPadding()))

                Box(Modifier.padding(top = 0.dp)) {
                    repeat(24) {
                        HorizontalDivider(
                            Modifier.padding(
                                top = minutesToDp(it * 60),
                                bottom = androidx.compose.ui.unit.max(30.dp, minutesToDp(MIN_OCCURRENCE_SIZE_MINUTES))
                            ).height(1.dp)
                        )
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

                        Day(
                            day,
                            ViewDateWithOptions.occurrencesIntersectingDay(dbManager, day),
                            openEntryInfoByDateOccurrence
                        ) { occurrence ->
                            val date = occurrence.date
                            date.addExceptions(occurrence.occurrenceZoned.toLocalDate())
                            date.update(dbManager)
                            dbManager.tryScheduleNotification()
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
    day: LocalDate,
    occurrences: List<DateOccurrence>,
    openEntryInfoByDateOccurrence: (DateOccurrence) -> Unit,
    excludeOccurrence: (DateOccurrence) -> Unit
) {
    val columns: List<List<DateOccurrence>> = remember(occurrences) {
        val columns = mutableListOf<MutableList<DateOccurrence>>()

        for (date in occurrences.sortedBy { it.occurrence }) {
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

    Row(Modifier.padding(horizontal = 2.dp)) {
        for (column in columns) {
            Box(Modifier.weight(1f / columns.size).defaultMinSize(minWidth = 30.dp)) {
                for (occurrence in column) {
                    EntryBox(
                        startOfDay,
                        occurrence,
                        {
                            excludeOccurrence(occurrence)
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
    startOfDay: LocalDateTime,
    occurrence: DateOccurrence,
    doExclude: () -> Unit,
    openEntryInfo: () -> Unit
) {
    val start = occurrence.occurrence.withSecond(0)
    val end = getZonedFromEpochSeconds(occurrence.endEpochSecond()).toLocalDateTime().withSecond(0)
    val startMinutesOffset = Duration.between(startOfDay, start).toMinutes().toInt()
    val endMinutesOffset = Duration.between(startOfDay, end).toMinutes().toInt()
    val size = endMinutesOffset - startMinutesOffset
    val hasDuration = occurrence.date.hasDuration

    var excludeDateDialogShown by remember { mutableStateOf(false) }

    if (excludeDateDialogShown)
        ExcludeOccurrenceDialog(occurrence.occurrence, occurrence.date.displayName, {
            doExclude()
            excludeDateDialogShown = false
        }) { excludeDateDialogShown = false }

    Box(
        Modifier.padding(top = minutesToDp(startMinutesOffset))
            // FIXME: either events with small duration will appear bigger than they are
            // or text won't be visible on events with small duration
            .height(
                minutesToDp(/*if (hasDuration) size else MIN_OCCURRENCE_SIZE_MINUTES*/
                    max(size, MIN_OCCURRENCE_SIZE_MINUTES)
                )
            ).fillMaxWidth().combinedClickable(onLongClick = { excludeDateDialogShown = true }, onClick = openEntryInfo)
            .let {
                if (hasDuration)
//                    it.padding(horizontal = 1.dp).border(1.dp, occurrence.date.color)
                    it
                        .padding(horizontal = 1.dp)
//                        .border(1.dp, MaterialTheme.colorScheme.primary)
                        .background(occurrence.date.color)
                else
                    it
            }
    ) {
        if (!hasDuration)
            Box(
                modifier = Modifier.height(2.dp).fillMaxWidth()
                    .background(occurrence.date.color)
            )

        Text(
            modifier = Modifier.padding(2.dp).align(Alignment.TopStart),
            color = if (!hasDuration) MaterialTheme.colorScheme.onBackground else bgColorToTextColor(occurrence.date.color),
            text = occurrence.date.displayName,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

@Preview
@Composable
fun PreviewEntryBox() {
    val day = LocalDate.of(2026, 3, 18)
    val startOfDay = day.atStartOfDay()

    val occurrences: MutableList<DateOccurrence> = mutableListOf()

    repeat(40) {
        val dateTime = startOfDay.plusMinutes(it.toLong() * 20).atZone(defaultZoneId())
        occurrences.add(
            DateOccurrence(
                dateTime.toLocalDateTime(),
                dateTime,
                ViewDateWithOptions(
                    0,
                    0,
                    0,
                    0,
                    if (it == 0) Period.Once() else Period.Minute(2 * it.toLong()),
                    0,
                    1,
                    Period.Once(),
                    dateTime.zone,
                    ExceptionRules(""),
                    "event name",
                    Color.Red
                )
            )
        )
    }

    Box(Modifier.padding(top = 0.dp)) {
        repeat(24) {
            HorizontalDivider(
                Modifier.padding(
                    top = minutesToDp(it * 60),
                    bottom = androidx.compose.ui.unit.max(30.dp, minutesToDp(MIN_OCCURRENCE_SIZE_MINUTES))
                ).height(1.dp)
            )
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

            Day(
                day,
                occurrences,
                {},
                {}
            )
        }
    }
}