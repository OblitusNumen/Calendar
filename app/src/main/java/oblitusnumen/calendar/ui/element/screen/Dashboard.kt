package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.now
import oblitusnumen.calendar.implementation.planTasks
import oblitusnumen.calendar.implementation.zonedDateTime
import oblitusnumen.calendar.ui.QUARTERS_PER_HOUR
import oblitusnumen.calendar.ui.formatTime
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dbManager: DbManager,
    navBar: @Composable () -> Unit,
    newEntry: () -> Unit,
    openThatDayInfo: (LocalDate) -> Unit,
    openMonthAgenda: (Int, Int) -> Unit,
    openEntriesScreen: () -> Unit,
    openTagsScreen: () -> Unit,
    openSettings: () -> Unit,
    openEntryDetails: (Int) -> Unit,
    openTaskDetails: (Int) -> Unit,
    openPlanner: () -> Unit,
) {
    val today = LocalDate.now()
    val now = now()

    val todayOccurrences = remember { ViewDateWithOptions.occurrencesForDay(dbManager, today) }
    val allTasks = remember { ViewTaskWithOptions.all(dbManager) }
    val links = remember { TaskLink.all(dbManager) }

    val activeTasks = remember { allTasks.filter { !it.isDone } }
    val overdueTasks = remember { allTasks.filter { it.isOverdue(now) } }
    val workLeftQuarters = remember { activeTasks.sumOf { it.timeRemaining } }

    val todayTasks = remember {
        val plannedTasks: Array<Task> = allTasks.filter { !it.isDone && it.deadlineTimestamp >= now }.toTypedArray()
        if (plannedTasks.isEmpty()) {
            emptyList()
        } else {
            val planned = planTasks(plannedTasks, links, now)
            allTasks.filter { planned[it.taskId!!]?.let { dist -> dist[0] > 0 } ?: false }
        }
    }

    val weekStart = remember { today.with(DayOfWeek.MONDAY).let { if (it.isAfter(today)) it.minusWeeks(1) else it } }
    val weekDates = remember {
        ViewDateWithOptions.all(
            dbManager,
            zonedDateTime(weekStart).toEpochSecond(),
            zonedDateTime(weekStart.plusWeeks(1)).toEpochSecond()
        )
    }

    val monthStart = remember { today.withDayOfMonth(1) }
    val monthEnd = remember { today.withDayOfMonth(today.month.length(today.isLeapYear)) }
    val gridStart = remember {
        monthStart.with(DayOfWeek.MONDAY).let { if (it.isAfter(monthStart)) it.minusWeeks(1) else it }
    }
    val gridEnd = remember {
        monthEnd.with(DayOfWeek.SUNDAY).let { if (it.isBefore(monthEnd)) it.plusWeeks(1) else it }
    }
    val monthDates = remember {
        ViewDateWithOptions.all(
            dbManager,
            zonedDateTime(gridStart).toEpochSecond(),
            zonedDateTime(gridEnd.plusDays(1)).toEpochSecond()
        )
    }

    val evtHeight = getEvtInDayExpectedHeight()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = topBarColors(),
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = openSettings) {
                        Icon(Icons.Filled.Settings, null)
                    }
                }
            )
        },
        bottomBar = navBar,
        floatingActionButton = {
            FloatingActionButton(onClick = newEntry) {
                Icon(Icons.Filled.Add, "add entry")
            }
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues, modifier = Modifier.fillMaxSize()) {

            item {
                DashboardGreeting(today)
            }

            item {
                DashboardStats(
                    todayCount = todayOccurrences.size,
                    activeCount = activeTasks.size,
                    workLeftQuarters = workLeftQuarters,
                    overdueCount = overdueTasks.size,
                    onEventsClick = { openThatDayInfo(today) },
                    onTasksClick = openPlanner,
                    onWorkClick = openPlanner,
                    onOverdueClick = openPlanner,
                )
            }

            item {
                DashboardSectionHeader("Today's events", Icons.Filled.Event) { openThatDayInfo(today) }
            }

            if (todayOccurrences.isEmpty()) {
                item {
                    Text(
                        "No events today",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(todayOccurrences) { occurrence ->
                    DashboardEventRow(occurrence) { occurrence.date.entryId?.let { openEntryDetails(it) } }
                }
            }

            item {
                DashboardSectionHeader("Today's tasks", Icons.Filled.CheckBox) { openPlanner() }
            }

            if (todayTasks.isEmpty()) {
                item {
                    Text(
                        "No tasks scheduled for today",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(todayTasks, key = { it.taskId!! }) { task ->
                    DashboardTaskRow(task, now) { openTaskDetails(task.taskId!!) }
                }
            }

            item {
                DashboardSectionHeader("This week", Icons.Filled.DateRange) { openThatDayInfo(today) }
            }

            item {
                DashboardWeekRow(dbManager, weekStart, weekDates, today, evtHeight, openThatDayInfo)
            }

            item {
                DashboardSectionHeader("This month", Icons.Filled.CalendarMonth) {
                    openMonthAgenda(today.year, today.monthValue)
                }
            }

            item {
                DashboardMonthGrid(gridStart, monthDates, today, today.monthValue, evtHeight, openThatDayInfo)
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DashboardGreeting(today: LocalDate) {
    val hour = ZonedDateTime.now(defaultZoneId()).hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(greeting, style = MaterialTheme.typography.headlineMedium)
        Text(
            dateStr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DashboardStats(
    todayCount: Int,
    activeCount: Int,
    workLeftQuarters: Int,
    overdueCount: Int,
    onEventsClick: () -> Unit,
    onTasksClick: () -> Unit,
    onWorkClick: () -> Unit,
    onOverdueClick: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardStatCard(
                Modifier.weight(1f), "Events today", todayCount.toString(),
                Icons.Filled.Event, onClick = onEventsClick
            )
            DashboardStatCard(
                Modifier.weight(1f), "Active tasks", activeCount.toString(),
                Icons.Filled.CheckBox, onClick = onTasksClick
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardStatCard(
                Modifier.weight(1f), "Work left", formatTime(workLeftQuarters),
                Icons.Filled.Schedule, onClick = onWorkClick
            )
            DashboardStatCard(
                Modifier.weight(1f), "Overdue", overdueCount.toString(),
                Icons.Filled.Warning,
                tint = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                onClick = onOverdueClick
            )
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = tint)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashboardSectionHeader(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun DashboardEventRow(occurrence: DateOccurrence, onClick: () -> Unit) {
    val timeStr = occurrence.occurrence.format(DateTimeFormatter.ofPattern("HH:mm"))
    val color = occurrence.date.color

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(10.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            timeStr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Text(
            occurrence.date.displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DashboardTaskRow(task: ViewTaskWithOptions, nowEpochSecond: Long, onClick: () -> Unit) {
    val isOverdue = task.isOverdue(nowEpochSecond)
    val timeLeft = formatTime(task.timeRemaining)

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(10.dp).background(task.color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            task.displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            timeLeft,
            style = MaterialTheme.typography.bodySmall,
            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        task.progress?.let { p ->
            Spacer(Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = { p },
                modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun DashboardWeekRow(
    dbManager: DbManager,
    weekStart: LocalDate,
    weekDates: List<ViewDateWithOptions>,
    today: LocalDate,
    evtHeight: androidx.compose.ui.unit.Dp,
    openThatDayInfo: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        for (i in 0..6) {
            val day = weekStart.plusDays(i.toLong())
            DisplayDay(
                Modifier.weight(1f / 7),
                evtHeight,
                today,
                2,
                day,
                weekDates,
                true,
                openThatDayInfo
            )
        }
    }
}

@Composable
private fun DashboardMonthGrid(
    gridStart: LocalDate,
    monthDates: List<ViewDateWithOptions>,
    today: LocalDate,
    monthValue: Int,
    evtHeight: androidx.compose.ui.unit.Dp,
    openThatDayInfo: (LocalDate) -> Unit,
) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        var weekStart = gridStart
        while (!weekStart.isAfter(today.withDayOfMonth(today.month.length(today.isLeapYear)).with(DayOfWeek.SUNDAY))) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0..6) {
                    val day = weekStart.plusDays(i.toLong())
                    DisplayDay(
                        Modifier.weight(1f / 7),
                        evtHeight,
                        today,
                        1,
                        day,
                        monthDates,
                        day.monthValue == monthValue,
                        openThatDayInfo
                    )
                }
            }
            weekStart = weekStart.plusWeeks(1)
        }
    }
}
