package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import oblitusnumen.calendar.implementation.MILLIS_PER_DAY
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Notification
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.zonedDateTime
import oblitusnumen.calendar.ui.displayCount
import oblitusnumen.calendar.ui.displayOffsetBefore
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.InfoRow
import oblitusnumen.calendar.ui.element.SectionDivider
import oblitusnumen.calendar.ui.element.TagChip
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EntryDetailsScreen(dbManager: DbManager, entryId: Int, editEntry: () -> Unit, viewTask: () -> Unit, backPress: () -> Unit) {
    val entry = remember { ViewEntryWithOptions.byId(dbManager, entryId) }!! // FIXME: replace with View

    val entryName = remember { entry.displayName }
    val tags: List<Tag> = remember { entry.getTags(dbManager).sortedBy { it.name } }
    val dates: List<Date> = remember {
        entry.getDates(dbManager).sortedBy { it.epochSecondChainStart }
    }
    val notifications: List<Notification> = remember {
        entry.getNotifications(dbManager).sortedBy { it.offset.secondsApproximation() }
    }
    val contents = remember { entry.getContents(dbManager) } // FIXME: this should be List<Content>

    val nextOccurrence: ZonedDateTime? = remember(dates) {
        val now = ZonedDateTime.now(defaultZoneId()).toEpochSecond()
        dates.mapNotNull { it.getNext(now) }.minByOrNull { it.toEpochSecond() }
    }

    var showOccurrenceCalendar by remember { mutableStateOf(false) }
    if (showOccurrenceCalendar)
        EntryOccurrencesCalendarDialog(dates, entry.color) { showOccurrenceCalendar = false }

    Scaffold(topBar = {
        DetailsEntryTopBar(dbManager, entry, entryName, editEntry, { showOccurrenceCalendar = true }, backPress)
    }) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            // name and color
            item {
                val color = remember { entry.color }
                Row(Modifier.fillMaxWidth()) {
                    SelectionContainer(Modifier.weight(1f)) {
                        Row {
                            Box(
                                Modifier.padding(8.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
                                    .size(24.dp).align(Alignment.CenterVertically)
                            )
                            Text(
                                entryName,
                                Modifier.align(Alignment.CenterVertically).padding(4.dp),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    if (entry.isTask) {
                        IconButton(onClick = viewTask, modifier = Modifier.align(Alignment.CenterVertically)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        }
                    }
                }
            }

            // next occurrence
            if (nextOccurrence != null) {
                item {
                    SectionDivider()
                    val formatted = nextOccurrence.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm"))
                    InfoRow(
                        icon = Icons.Outlined.Schedule,
                        text = stringResource(R.string.details_entry_next_occurrence, formatted)
                    )
                }
            }

            // description
            item {
                if (contents.isNotEmpty()) {
                    SectionDivider()
                    Text(stringResource(R.string.details_entry_description), modifier = Modifier.padding(12.dp))
                    SelectionContainer {
                        Text(
                            contents,
                            modifier = Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }

            // tags
            item {
                if (tags.isNotEmpty()) {
                    SectionDivider()
                    Row {
                        Icon(Icons.Filled.Star, stringResource(R.string.cd_tags), Modifier.padding(8.dp))
                        FlowRow(
                            Modifier.fillMaxWidth().padding(end = 16.dp)
                        ) {
                            for (tag in tags)
                                TagChip(tag.name, tag.colorOrDefault(dbManager))
                        }
                    }
                }
            }

            // dates
            item {
                if (dates.isNotEmpty()) {
                    SectionDivider()
                    dates.forEachIndexed { index, date ->
                        if (index > 0) SectionDivider()
                        Date(date)
                    }
                }
            }

            // notifications
            item {
                if (notifications.isNotEmpty()) {
                    SectionDivider()
                    for (notification in notifications)
                        Notification(notification)
                }
            }
        }
    }
}

@Composable
fun Notification(notification: Notification) {
    val context = LocalContext.current
    InfoRow(
        icon = if (notification.sound) Icons.Filled.Notifications else Icons.Outlined.Notifications,
        text = notification.offset.displayOffsetBefore(context)
    )
}

@Composable
fun Date(date: Date) {
    val context = LocalContext.current
    val firstFormatted = date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
    val textStart = if (date.isPeriodic) stringResource(R.string.edit_entry_period_from, firstFormatted)
        else firstFormatted

    val textDuration: String = if (!date.hasDuration)
        stringResource(R.string.details_entry_no_duration)
    else
        stringResource(R.string.edit_entry_duration_for, date.duration.displayCount(context))

    val textPeriod: String = if (date.isPeriodic) {
        val every = stringResource(R.string.edit_entry_period_every, date.period.displayCount(context))
        if (date.isEndless) every
        else every + " " + stringResource(
            R.string.edit_entry_period_until,
            date.getLastZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        )
    } else PeriodType(date.period).displayName(context)

    Column(Modifier.padding(vertical = 4.dp)) {
        // time
        InfoRow(icon = Icons.Outlined.Schedule, text = textStart)
        // time zone
        InfoRow(icon = Icons.Filled.Language, text = date.timeZoneId.toString())
    }

    SectionDivider(tight = true)

    Column(Modifier.padding(vertical = 4.dp)) {
        // duration
        InfoRow(icon = Icons.Filled.HourglassEmpty, text = textDuration)
        // periodic
        InfoRow(icon = Icons.Filled.Repeat, text = textPeriod)
    }

    // exceptions
    if (date.isPeriodic) {
        val exceptions = date.exceptionRules.listAll()
        if (exceptions.isNotEmpty()) {
            SectionDivider(tight = true)
            Column(Modifier.padding(vertical = 4.dp)) {
                for (epochDay in exceptions) {
                    val textException = stringResource(R.string.edit_entry_period_except, convertMillisToDate(epochDay * MILLIS_PER_DAY))
                    InfoRow(icon = Icons.Filled.EventBusy, text = textException)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsEntryTopBar(
    dbManager: DbManager,
    entry: Entry,
    entryName: String,
    editEntry: () -> Unit,
    onShowOccurrenceCalendar: () -> Unit,
    backPress: () -> Unit
) {// TODO: confirm
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        navigationIcon = { BackPressButton(backPress) },
        title = { Text(entryName, maxLines = 1) },
        actions = {
            IconButton(onClick = onShowOccurrenceCalendar) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = stringResource(R.string.cd_show_calendar)
                )
            }
            IconButton(onClick = {
                editEntry()
            }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null
                )
            }
            IconButton(onClick = {
                entry.deleteCascade(dbManager)// FIXME: catch exception
                backPress()
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null
                )
            }
        },
    )
}

@Composable
fun EntryOccurrencesCalendarDialog(
    dates: List<Date>,
    color: Color,
    onDismiss: () -> Unit,
) {// FIXME: in calendar highlight today with accent; every occurrence should be clickable
    val today = remember { LocalDate.now(defaultZoneId()) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }

    val monthNames = stringArrayResource(R.array.monthNames)
    val weekdayNames = stringArrayResource(R.array.weekdayNames)

    val monthStart = month.atDay(1)
    val monthEnd = month.atEndOfMonth()
    val gridStart = monthStart.with(DayOfWeek.MONDAY)
        .let { if (it.isAfter(monthStart)) it.minusWeeks(1) else it }
    val gridEnd = monthEnd.with(DayOfWeek.SUNDAY)
        .let { if (it.isBefore(monthEnd)) it.plusWeeks(1) else it }

    val occurrences: Set<LocalDate> = remember(month, dates) {
        val rangeStart = zonedDateTime(gridStart).toEpochSecond()
        val rangeEnd = zonedDateTime(gridEnd.plusDays(1)).toEpochSecond()
        dates.flatMap { it.getAllInRange(rangeStart, rangeEnd) }
            .map { it.toLocalDate() }
            .toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) }
        },
        title = { Text(stringResource(R.string.entry_calendar_title)) },
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                    }
                    Text(
                        "${monthNames[month.monthValue - 1]} ${month.year}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    for (i in 0..6) {
                        Text(
                            weekdayNames[i],
                            modifier = Modifier.weight(1f / 7).padding(vertical = 4.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                var weekStart = gridStart
                while (!weekStart.isAfter(gridEnd)) {
                    Row(Modifier.fillMaxWidth()) {
                        for (i in 0..6) {
                            val day = weekStart.plusDays(i.toLong())
                            DayCell(
                                Modifier.weight(1f / 7),
                                day = day,
                                inCurrentMonth = day.monthValue == month.monthValue,
                                isToday = day == today,
                                isOccurrence = day in occurrences,
                                accentColor = color,
                            )
                        }
                    }
                    weekStart = weekStart.plusWeeks(1)
                }
            }
        },
    )
}

@Composable
private fun DayCell(
    modifier: Modifier,
    day: LocalDate,
    inCurrentMonth: Boolean,
    isToday: Boolean,
    isOccurrence: Boolean,
    accentColor: Color,
) {
    val textColor = when {
        isOccurrence -> bgColorToTextColor(accentColor)
        inCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
    }
    Box(
        modifier = modifier.aspectRatio(1f).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        val cellModifier = Modifier.fillMaxSize().clip(CircleShape).let {
            if (isOccurrence) it.background(accentColor) else it
        }.let {
            if (isToday && !isOccurrence) it.border(1.dp, accentColor, CircleShape) else it
        }
        Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
            Text(
                day.dayOfMonth.toString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else null,
                color = textColor,
            )
        }
    }
}
