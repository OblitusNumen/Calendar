package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.LIST_LEN
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Tag
import oblitusnumen.calendar.implementation.data.ViewDateWithOptions
import oblitusnumen.calendar.ui.model.tab.EntriesTab.Companion.drawDescriptionAndTags
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        floatingActionButton = { DateFunctionButton({ openEditNewEntry(pagerDay) }) }
    ) { paddingValues ->
        val pagerState = rememberPagerState(initialPage = LIST_LEN / 2, pageCount = { LIST_LEN })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { index ->
            val day = day.plusDays((index - LIST_LEN / 2).toLong())
            val dates = remember { ViewDateWithOptions.occurrencesIntersectingDay(dbManager, day) }
            LazyColumn {
                item {
                    Spacer(Modifier.height(paddingValues.calculateTopPadding()))
                }
                items(dates) {
                    DrawEntry(dbManager, it) { openEntryInfoByDateOccurrence(it) }
                }
                item {
                    Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
                }
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
fun DrawEntry(dbManager: DbManager, occurrence: DateOccurrence, openEditEntry: () -> Unit) { //todo maybe show desc too?
    var hack by remember { mutableStateOf(false) }
    var excludeDateShown by remember(hack) { mutableStateOf(false) }
    val dateMeta = occurrence.date
    val occurrence = occurrence.occurrence

    Column(
        Modifier.padding(2.dp).fillMaxWidth().defaultMinSize(minHeight = 64.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ).combinedClickable(onLongClick = { excludeDateShown = true }, onClick = openEditEntry)
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
                text = occurrence
                    .format(DateTimeFormatter.ofPattern("HH:mm")), //fixme should show end time
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        drawDescriptionAndTags(dbManager, dateMeta.getContents(dbManager), Tag.forEntry(dbManager, dateMeta.entryId))

        if (excludeDateShown)
            ExcludeOccurrence(occurrence, dateMeta.displayName, {

//                    date.addExceptions(day)
//                    date.update()
//                    dbManager.tryScheduleNotification()
//                    onClose()
//                    loadDates()
                // TODO:
                hack = true
            }) { excludeDateShown = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTopBar(day: LocalDate, openAgenda: () -> Unit, backPress: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Row {
                Text("Date $day", Modifier.weight(1f).align(Alignment.CenterVertically), maxLines = 1)

                IconButton(openAgenda, Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.Filled.KeyboardArrowRight, "open month agenda")
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = backPress) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Localized description"
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun DateFunctionButton(openEditNewEntry: () -> Unit) {
    FloatingActionButton(onClick = openEditNewEntry) {
        Icon(Icons.Filled.Add, "add event")
    }
}

@Composable
fun ExcludeOccurrence(occurrence: LocalDateTime, name: String, doExclude: () -> Unit, onClose: () -> Unit) {
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
