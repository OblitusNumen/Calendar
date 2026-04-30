package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.coroutines.launch
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.tables.TaskLog
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.ui.formatDateTime
import oblitusnumen.calendar.implementation.now
import oblitusnumen.calendar.implementation.planTasks
import oblitusnumen.calendar.ui.element.DateTimePicker
import oblitusnumen.calendar.ui.element.EntryDescriptionAndTags
import oblitusnumen.calendar.ui.element.MainDrawer
import oblitusnumen.calendar.ui.element.PlanDistributionDialog
import oblitusnumen.calendar.ui.formatTime
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalKoalaPlotApi::class)
@Composable
fun PlannerScreen(
    dbManager: DbManager,
    tagsFilter: MutableState<List<Tag>>,
    navBar: @Composable () -> Unit,
    openEditNewTask: () -> Unit,
    openTaskDetails: (Int) -> Unit,
    openEntriesScreen: () -> Unit,
    openTagsScreen: () -> Unit,
    openSettings: () -> Unit,
    initialTab: PlannerTab = PlannerTab.TODAY,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.close() } } }
    val openDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.open() } } }

    val dtPicker = remember { DateTimePicker() }
    dtPicker.tryCompose()

    val now = now()
    val links = remember { TaskLink.all(dbManager) }
    val allTasks = remember {
        mutableStateListOf(*ViewTaskWithOptions.all(dbManager).sortedBy { it.progress }.toTypedArray())
    }
    val predecessorLinks: Map<Int, MutableList<Int>> = remember {
        val predecessorLinks: Map<Int, MutableList<Int>> =
            allTasks.map { it.taskId!! }.associateWith { mutableListOf() }
        links.forEach { link ->
            predecessorLinks[link.successor]!!.add(link.predecessor)
        }
        return@remember predecessorLinks
    }
    val today = remember { LocalDate.now(defaultZoneId()) }
    val todayStart = remember { today.atStartOfDay(defaultZoneId()).toEpochSecond() }
    val todayLogs = remember { mutableStateMapOf<Int, TaskLog>() }
    val planned = remember {
        // FIXME: wrong filter
        val plannedTasks: Array<Task> = allTasks.filter { task ->
            task.deadlineTimestamp >= now && !task.isDone
        }.toTypedArray()
        val result = planTasks(plannedTasks, links, now)

        // persist today's planned portions in TaskLog
        result.forEach { (taskId, dist) ->
            if (dist[0] > 0) {
                val task = allTasks.firstOrNull { it.taskId == taskId } ?: return@forEach
                val log = TaskLog.upsert(
                    dbManager, taskId, todayStart, task.timeZoneId, dist[0]
                )
                todayLogs[taskId] = log
            }
        }
        result
    }

    var showPlanDist by remember { mutableStateOf(false) }
    if (showPlanDist)
        PlanDistributionDialog(planned, today) { showPlanDist = false }

    ModalNavigationDrawer(
        drawerContent = { MainDrawer(stringResource(R.string.planner_title), closeDrawer, openEntriesScreen, openTagsScreen, openSettings) },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = { PlannerTopBar(openDrawer, onShowPlanDist = { showPlanDist = true }) },
            bottomBar = navBar,
            floatingActionButton = {
                FloatingActionButton(onClick = openEditNewTask) {
                    Icon(Icons.Filled.Add, stringResource(R.string.cd_add_task))
                }
            }
        ) { paddingValues ->
            Column {
                val pagerState = rememberPagerState(initialTab.ordinal, pageCount = { PlannerTab.entries.size })
                val coroutineScope = rememberCoroutineScope()

                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth().padding(top = paddingValues.calculateTopPadding()),
                    edgePadding = 0.dp
                ) {
                    PlannerTab.entries.forEachIndexed { index, destination ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = stringResource(when (destination) {
                                        PlannerTab.TODAY -> R.string.planner_tab_today
                                        PlannerTab.CURRENT -> R.string.planner_tab_current
                                        PlannerTab.PAST -> R.string.planner_tab_past
                                        PlannerTab.OVERDUE -> R.string.planner_tab_overdue
                                        PlannerTab.ALL -> R.string.planner_tab_all
                                    }),
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                )
                            }
                        )
                    }
                }

                fun toggleTodayDone(task: ViewTaskWithOptions, log: TaskLog, markDone: Boolean) {
                    val prev = log.timeConsumed
                    val next = if (markDone) log.timePlanned else 0
                    val delta = next - prev
                    log.timeConsumed = next
                    log.update()
                    Task.updateTimeValues(
                        dbManager, task.taskId!!,
                        task.timeConsumed + delta,
                        maxOf(0, task.timeRemaining - delta)
                    )
                    todayLogs[task.taskId!!] = log
                    val idx = allTasks.indexOfFirst { it.taskId == task.taskId }
                    if (idx >= 0)
                        allTasks[idx] = ViewTaskWithOptions.byId(dbManager, task.taskId!!)!!
                }

                HorizontalPager(pagerState, verticalAlignment = Alignment.Top) { page ->
                    val isToday = PlannerTab.entries[page] == PlannerTab.TODAY
                    val tabTasks = when (PlannerTab.entries[page]) {
                        PlannerTab.TODAY -> allTasks.filter { task ->
                            planned[task.taskId!!]?.let { dist -> dist[0] > 0 } ?: false
                        }

                        PlannerTab.CURRENT -> allTasks.filter { task ->
                            (task.startConstraintTimestamp == null || task.startConstraintTimestamp!! <= now) &&
                                    !task.isDone
                        }

                        PlannerTab.PAST -> allTasks.filter { it.isDone }

                        PlannerTab.OVERDUE -> allTasks.filter { it.isOverdue(now) }

                        PlannerTab.ALL -> allTasks.toList()
                    }

                    LazyColumn(Modifier.fillMaxSize()) {
                        items(tabTasks, key = { it.taskId!! }) { task ->
                            val log = if (isToday) todayLogs[task.taskId!!] else null
                            Task(
                                openTaskDetails, task, now, allTasks, predecessorLinks, dbManager,
                                todayLog = log,
                                onToggleDone = if (isToday) { l, markDone ->
                                    toggleTodayDone(task, l, markDone)
                                } else null,
                                onSchedulePortion = if ((isToday && log != null) || task.timeRemaining > 0) { ->
                                    val durationMinutes =
                                        if (isToday && log != null) log.timePlanned * 15
                                        else task.timeRemaining * 15
                                    dtPicker.dateTimePick(
                                        onCancel = {},
                                        onConfirm = { dateTime ->
                                            Date.scheduleOnce(
                                                dbManager, task.entryId!!,
                                                dateTime.atZone(task.timeZoneId),
                                                durationMinutes
                                            )
                                        }
                                    )
                                } else null
                            )
                            // TODO:
                        }

                        item { Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding())) }
                    }
                }
            }
        }
    }
}

@Composable
fun Task(
    openTaskDetails: (Int) -> Unit,
    task: ViewTaskWithOptions,
    now: Long,
    allTasks: List<ViewTaskWithOptions>,
    predecessorLinks: Map<Int, MutableList<Int>>,
    dbManager: DbManager,
    todayLog: TaskLog? = null,
    onToggleDone: ((TaskLog, Boolean) -> Unit)? = null,
    onSchedulePortion: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    Column(
        Modifier.padding(2.dp).fillMaxWidth().defaultMinSize(minHeight = 64.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ).clickable(onClick = { openTaskDetails(task.taskId!!) })
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
            Box(
                Modifier.padding(end = 8.dp).size(24.dp).background(task.color, CircleShape)
                    .border(0.dp, task.color, CircleShape)
                    .align(Alignment.CenterVertically)
            )

            Text(
                modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically),
                text = task.displayName,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            if (task.isDone)
                Icon(Icons.Filled.Done, tint = Color.Green, contentDescription = stringResource(R.string.cd_done))
            else
                Text(
                    text = formatDateTime(context, task.deadlineTimestamp, task.timeZoneId),
                    modifier = Modifier.align(Alignment.CenterVertically),
                    color = if (task.isOverdue(now)) Color.Red else Color.Unspecified,
                    style = MaterialTheme.typography.bodyLarge,
                )
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
            val predTime = task.countPredecessorsTimeEstimate(
                allTasks.associateBy { it.taskId!! },
                predecessorLinks
            )

            if (predTime > 0)
                Text(
                    text = stringResource(R.string.planner_deps_suffix, formatTime(context, predTime)),
                    modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            else
                Spacer(Modifier.weight(1.0f))

            Text(
                text = formatTime(context, task.timeRemaining),
                modifier = Modifier.align(Alignment.CenterVertically),
                color = if (task.isOverdue(now)) Color.Red else Color.Unspecified,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        EntryDescriptionAndTags(
            dbManager,
            task.getContents(dbManager),
            Tag.forEntry(dbManager, task.entryId!!)
        )

        if (!task.isDone)
            LinearProgressIndicator(
                { task.progress ?: 0f },
                Modifier.fillMaxWidth().padding(4.dp),
            )

        if ((todayLog != null && onToggleDone != null) || onSchedulePortion != null) {
            Row(Modifier.align(Alignment.End)) {
                if (todayLog != null && onToggleDone != null) {
                    val isDoneToday =
                        todayLog.timeConsumed >= todayLog.timePlanned && todayLog.timePlanned > 0
                    TextButton(onClick = { onToggleDone(todayLog, !isDoneToday) }) {
                        Text(
                            if (isDoneToday) stringResource(R.string.planner_undo_today)
                            else stringResource(R.string.planner_done_today, formatTime(context, todayLog.timePlanned))
                        )
                    }
                }
                if (onSchedulePortion != null) {
                    TextButton(onClick = onSchedulePortion) {
                        Text(stringResource(R.string.planner_schedule))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerTopBar(openDrawer: () -> Unit, onShowPlanDist: () -> Unit) {
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
        title = {},
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

enum class PlannerTab {
    TODAY,
    CURRENT,
    PAST,
    OVERDUE,
    ALL
}
