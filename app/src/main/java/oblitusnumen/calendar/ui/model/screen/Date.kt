package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.LIST_LEN
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.DateOccurrence
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.ui.BackPressButton
import oblitusnumen.calendar.ui.DrawEntryDescriptionAndTags
import oblitusnumen.calendar.ui.NewEntryFunctionButton
import oblitusnumen.calendar.ui.dpByDpForPixelPerfect
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { index ->
            val day = day.plusDays((index - LIST_LEN / 2).toLong())

            Column(Modifier.verticalScroll(rememberScrollState())) {
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
                            DrawDay(dbManager, day, openEntryInfoByDateOccurrence)
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
fun DrawDay(
    dbManager: DbManager,
    day: LocalDate,
    openEntryInfoByDateOccurrence: (DateOccurrence) -> Unit
) {
    // FIXME: debug
    val ids = remember { mutableMapOf<DateOccurrence, Int>() }
    val columns: List<List<DateOccurrence>> = remember {
        val dates = ViewDateWithOptions.occurrencesIntersectingDay(dbManager, day).sortedBy { it.occurrence }
        ids.putAll(dates.associate { it to dates.indexOf(it) })
        val columns = mutableListOf<MutableList<DateOccurrence>>()
        for (date in dates) {
            var assigned = false
            for (column in columns) {
                if (column.last().endEpochSecond() < date.startEpochSecond()) {
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

                    // FIXME:
//                    if (column.last() == occurrence) {
//                        log(Duration.between(start, endOfDay).toMinutes().toInt())
//                        log(occurrence.date.duration)
//                        log(occurrence.date.duration.secondsApproximation() / 60)
//                        log(endOfLast)
//                    }

                    // FIXME:
                    val s = endOfLastMinutesOffset
                    if (start > endOfLast) {
                        val startMinutesOffset = Duration.between(startOfDay, start).toMinutes().toInt()

                        Spacer(Modifier.height(minutesToDp(startMinutesOffset - endOfLastMinutesOffset)))

                        endOfLastMinutesOffset = startMinutesOffset
                    }

                    val size = min(endMinutesOffset, endOfDayMinutesOffset) - endOfLastMinutesOffset
                    // FIXME:
                    val e = endOfLastMinutesOffset
                    val a = {
                        log("DrawEntryBox:${ids[occurrence]}")
                        log("pre:$s")
                        log("start:$e")
                        log("end:${e + size}")
                        log("pre_size:${e - s}")
                        log("size:$size")
                        log("real_end:$endMinutesOffset")
                    }
                    endOfLastMinutesOffset = endMinutesOffset
                    endOfLast = end

                    DrawEntryBox(
                        occurrence,
                        size, ids[occurrence],
                        {
                            val date = occurrence.date.date
                            date.addExceptions(occurrence.occurrenceZoned.toLocalDate())
                            date.update(dbManager)
                        }
                    ) {
                        // FIXME:
                        a()
//                        openEntryInfoByDateOccurrence(occurrence)
                    }
//                        DrawEntry(dbManager, occurrence) { openEntryInfoByDateOccurrence(occurrence) }
                }
            }
        }
    }
}

@Composable
fun DrawEntryBox(
    occurrence: DateOccurrence,
    minutesSize: Int,
    id: Int?,
    doExclude: () -> Unit,
    openEntryInfo: () -> Unit
) {
    var excludeDateShown by remember { mutableStateOf(false) }

    Box(
        Modifier.height(minutesToDp(minutesSize)).fillMaxWidth()
            .combinedClickable(onLongClick = { excludeDateShown = true }, onClick = openEntryInfo)
            .border(1.dp, MaterialTheme.colorScheme.primary).background(occurrence.date.color)
    ) {
        Text("$id")
    }

    if (excludeDateShown)
        ExcludeOccurrenceDialog(occurrence.occurrence, occurrence.date.displayName, {
            doExclude()
            excludeDateShown = false
        }) { excludeDateShown = false }
}

@Composable
fun DrawEntry(dbManager: DbManager, occurrence: DateOccurrence, openEntryInfo: () -> Unit) { //todo maybe show desc too?
    var hack by remember { mutableStateOf(false) }
    var excludeDateShown by remember(hack) { mutableStateOf(false) }
    val dateMeta = occurrence.date

    Column(
        Modifier.padding(2.dp).fillMaxWidth().defaultMinSize(minHeight = 64.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ).combinedClickable(onLongClick = { excludeDateShown = true }, onClick = openEntryInfo)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
            Box(
                Modifier.padding(end = 8.dp).size(24.dp).background(dateMeta.color, CircleShape)
                    .border(0.dp, dateMeta.color, CircleShape)
                    .align(Alignment.CenterVertically)
            )

            Text(
                modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp).align(Alignment.CenterVertically),
                text = dateMeta.displayName,
                style = MaterialTheme.typography.headlineSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = occurrence.occurrence
                    .format(DateTimeFormatter.ofPattern("HH:mm")), //fixme should show end time
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        DrawEntryDescriptionAndTags(
            dbManager,
            dateMeta.getContents(dbManager),
            Tag.forEntry(dbManager, dateMeta.entryId)
        )

        if (excludeDateShown)
            ExcludeOccurrenceDialog(occurrence.occurrence, dateMeta.displayName, {

//                    date.addExceptions(day)
//                    date.update()
//                    dbManager.tryScheduleNotification()
//                    onClose()
//                    loadDates()
                // TODO:
                val date = dateMeta.date
                date.addExceptions(occurrence.occurrenceZoned.toLocalDate())
                date.update(dbManager)
                hack = true
                excludeDateShown = false
            }) { excludeDateShown = false }
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
                    Icon(Icons.Filled.KeyboardArrowRight, "open month agenda")
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
