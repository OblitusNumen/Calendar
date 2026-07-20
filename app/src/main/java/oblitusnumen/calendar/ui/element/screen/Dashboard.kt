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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.tables.TaskLog
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.now
import oblitusnumen.calendar.implementation.planTasks
import oblitusnumen.calendar.implementation.zonedDateTime
import oblitusnumen.calendar.ui.element.MainDrawer
import oblitusnumen.calendar.ui.element.PlanDistributionDialog
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
    openEntriesScreen: () -> Unit,
    openTagsScreen: () -> Unit,
    openTaskLogs: () -> Unit,
    openSettings: () -> Unit,
    openEntryDetails: (Int) -> Unit,
    openTaskDetails: (Int) -> Unit,
    openAgenda: (Int, Int, Int?) -> Unit,
    openCalendar: () -> Unit,
    openPlanner: (PlannerTab) -> Unit,
    openYearView: () -> Unit,
) {
    val today = LocalDate.now()
    val now = now()

    // for updating
    var refreshKey by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val todayOccurrences = remember(refreshKey) { ViewDateWithOptions.occurrencesForDay(dbManager, today) }
    val allTasks = remember(refreshKey) { ViewTaskWithOptions.all(dbManager) }
    val links = remember(refreshKey) { TaskLink.all(dbManager) }

    val activeTasks = remember(allTasks) { allTasks.filter { !it.isDone } }
    val overdueTasks = remember(allTasks, now) { allTasks.filter { it.isOverdue(now) } }

    val todayStart = remember { today.atStartOfDay(defaultZoneId()).toEpochSecond() }
    val todayConsumed: Map<Int, Int> = remember(refreshKey) {
        TaskLog.forDay(dbManager, todayStart).associate { it.taskId to it.timeConsumed }
    }
    val planned = remember(allTasks, links, todayConsumed, now) {
        val plannedTasks: Array<Task> = allTasks.filter { !it.isDone && it.deadlineTimestamp >= now }.toTypedArray()
        if (plannedTasks.isEmpty() && todayConsumed.isEmpty()) emptyMap()
        else planTasks(plannedTasks, links, now, todayConsumed)
    }
    val todayTasks = remember(allTasks, planned, todayConsumed) {
        allTasks.filter { task ->
            (planned[task.taskId!!]?.getOrNull(0) ?: 0) > 0 ||
                    (todayConsumed[task.taskId!!] ?: 0) > 0
        }
    }
    val todayWorkQuarters = remember(planned, todayConsumed) {
        planned.entries.sumOf { (id, dist) ->
            (if (dist.isNotEmpty()) dist[0] else 0) + (todayConsumed[id] ?: 0)
        }
    }
    val weekWorkQuarters = remember(planned, todayConsumed) {
        planned.values.sumOf { dist ->
            var sum = 0
            for (i in 0 until minOf(7, dist.size)) sum += dist[i]
            sum
        } + todayConsumed.values.sum()
    }

    val weekStart = remember { today.with(DayOfWeek.MONDAY).let { if (it.isAfter(today)) it.minusWeeks(1) else it } }
    val weekDates = remember(refreshKey) {
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
    val monthDates = remember(refreshKey) {
        ViewDateWithOptions.all(
            dbManager,
            zonedDateTime(gridStart).toEpochSecond(),
            zonedDateTime(gridEnd.plusDays(1)).toEpochSecond()
        )
    }

    val evtHeight = getEvtInDayExpectedHeight()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = { coroutineScope.launch { drawerState.close() } }
    val openDrawer: () -> Unit = { coroutineScope.launch { drawerState.open() } }

    var showPlanDist by remember { mutableStateOf(false) }
    if (showPlanDist)
        PlanDistributionDialog(planned, today) { showPlanDist = false }

    ModalNavigationDrawer(
        drawerContent = {
            MainDrawer(
                stringResource(R.string.dashboard_title),
                closeDrawer,
                openYearView,
                openEntriesScreen,
                openTagsScreen,
                openTaskLogs,
                openSettings
            )
        },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = { DashboardTopBar(openDrawer, onShowPlanDist = { showPlanDist = true }) },
            bottomBar = navBar,
            floatingActionButton = {
                FloatingActionButton(onClick = newEntry) {
                    Icon(Icons.Filled.Add, stringResource(R.string.cd_add_entry))
                }
            }
        ) { paddingValues ->
            LazyColumn(contentPadding = paddingValues, modifier = Modifier.fillMaxSize()) {

                item {
                    DashboardGreeting(today)
                }

                item {
                    DashboardStats(
                        todayWorkQuarters = todayWorkQuarters,
                        activeCount = activeTasks.size,
                        weekWorkQuarters = weekWorkQuarters,
                        overdueCount = overdueTasks.size,
                        onTodayWorkClick = { openPlanner(PlannerTab.TODAY) },
                        onTasksClick = { openPlanner(PlannerTab.CURRENT) },
                        onWeekWorkClick = { openPlanner(PlannerTab.CURRENT) },
                        onOverdueClick = { openPlanner(PlannerTab.OVERDUE) },
                    )
                }

                item {
                    DashboardSectionHeader(
                        stringResource(R.string.dashboard_today_events),
                        Icons.Filled.Event
                    ) { openThatDayInfo(today) }
                }

                if (todayOccurrences.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.dashboard_no_events_today),
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
                    DashboardSectionHeader(
                        stringResource(R.string.dashboard_today_tasks),
                        Icons.Filled.CheckBox
                    ) { openPlanner(PlannerTab.TODAY) }
                }

                if (todayTasks.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.dashboard_no_tasks_today),
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(todayTasks, key = { it.taskId!! }) { task ->
                        val todayQuarters = (planned[task.taskId!!]?.let { if (it.isNotEmpty()) it[0] else 0 } ?: 0) +
                                (todayConsumed[task.taskId!!] ?: 0)
                        DashboardTaskRow(task, todayQuarters, now) { openTaskDetails(task.taskId!!) }
                    }
                }

                item {
                    DashboardSectionHeader(
                        stringResource(R.string.dashboard_this_week),
                        Icons.Filled.DateRange
                    ) {
                        val weekStart = today.plusDays(-(today.dayOfWeek.value - 1).toLong())
                        openAgenda(weekStart.year, weekStart.monthValue, weekStart.dayOfMonth) // FIXME: should be week view
                    }
                }

                item {
                    DashboardWeekRow(dbManager, weekStart, weekDates, today, evtHeight, openThatDayInfo)
                }

                item {
                    DashboardSectionHeader(stringResource(R.string.dashboard_this_month), Icons.Filled.CalendarMonth, openCalendar)
                }

                item {
                    DashboardMonthGrid(gridStart, monthDates, today, today.monthValue, evtHeight, openThatDayInfo, )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardGreeting(today: LocalDate) {
    val hour = ZonedDateTime.now(defaultZoneId()).hour
    val greeting = when {
        hour < 12 -> stringResource(R.string.dashboard_good_morning)
        hour < 17 -> stringResource(R.string.dashboard_good_afternoon)
        else -> stringResource(R.string.dashboard_good_evening)
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
    todayWorkQuarters: Int,
    activeCount: Int,
    weekWorkQuarters: Int,
    overdueCount: Int,
    onTodayWorkClick: () -> Unit,
    onTasksClick: () -> Unit,
    onWeekWorkClick: () -> Unit,
    onOverdueClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardStatCard(
                Modifier.weight(1f),
                stringResource(R.string.dashboard_stat_hours_today),
                formatTime(context, todayWorkQuarters),
                Icons.Filled.Schedule,
                onClick = onTodayWorkClick
            )
            DashboardStatCard(
                Modifier.weight(1f), stringResource(R.string.dashboard_stat_active_tasks), activeCount.toString(),
                Icons.Filled.CheckBox, onClick = onTasksClick
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardStatCard(
                Modifier.weight(1f),
                stringResource(R.string.dashboard_stat_hours_week),
                formatTime(context, weekWorkQuarters),
                Icons.Filled.DateRange,
                onClick = onWeekWorkClick
            )
            DashboardStatCard(
                Modifier.weight(1f), stringResource(R.string.dashboard_stat_overdue), overdueCount.toString(),
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
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun DashboardTaskRow(task: ViewTaskWithOptions, todayQuarters: Int, nowEpochSecond: Long, onClick: () -> Unit) {
    val context = LocalContext.current
    val isOverdue = task.isOverdue(nowEpochSecond)
    val timeLeft = formatTime(context, todayQuarters)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(openDrawer: () -> Unit, onShowPlanDist: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null
                )
            }
        },
        title = { Text(stringResource(R.string.dashboard_title)) },
        actions = {
            IconButton(onClick = onShowPlanDist) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.cd_plan_dist)
                )
            }
        },
    )
}
