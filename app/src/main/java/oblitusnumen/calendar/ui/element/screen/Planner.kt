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
import oblitusnumen.calendar.ui.element.TopBarTagFilterTitle
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
    openTaskLogs: () -> Unit,
    openSettings: () -> Unit,
    openYearView: () -> Unit,
    initialTab: PlannerTab = PlannerTab.TODAY,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.close() } } }
    val openDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.open() } } }

    val dtPicker = remember { DateTimePicker() }
    dtPicker.tryCompose()

    var tagsFilter by remember { tagsFilter }

    val now = now()
    val links = remember { TaskLink.all(dbManager) }
    val allTasks = remember {
        mutableStateListOf(*ViewTaskWithOptions.all(dbManager).sortedBy { it.progress }.toTypedArray())
    }
    val taskTagIds: Map<Int, Set<Int>> = remember(allTasks.size) {
        allTasks.associate { it.entryId!! to Tag.forEntry(dbManager, it.entryId!!).map { t -> t.id!! }.toSet() }
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
    val todayLogs = remember {
        mutableStateMapOf<Int, TaskLog>().also { map ->
            TaskLog.forDay(dbManager, todayStart).forEach { map[it.taskId] = it }
        }
    }
    val planned by remember {
        derivedStateOf {
            // FIXME: wrong filter
            val plannedTasks: Array<Task> = allTasks.filter { task ->
                task.deadlineTimestamp >= now && !task.isDone
            }.toTypedArray()
            val todayConsumed = todayLogs.mapValues { it.value.timeConsumed }
            planTasks(plannedTasks, links, now, todayConsumed)
        }
    }

    var showPlanDist by remember { mutableStateOf(false) }
    if (showPlanDist)
        PlanDistributionDialog(planned, today) { showPlanDist = false }

    ModalNavigationDrawer(
        drawerContent = { MainDrawer(stringResource(R.string.planner_title), closeDrawer, openYearView, openEntriesScreen, openTagsScreen, openTaskLogs, openSettings) },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = { PlannerTopBar(dbManager, tagsFilter, { tagsFilter = it }, openDrawer, onShowPlanDist = { showPlanDist = true }) },
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

                fun toggleTodayDone(task: ViewTaskWithOptions, markDone: Boolean) {
                    val taskId = task.taskId!!
                    if (markDone) {
                        val dist0 = planned[taskId]?.getOrNull(0) ?: 0
                        if (dist0 <= 0) return
                        val log = todayLogs[taskId] ?: TaskLog.upsert(dbManager, taskId, todayStart, task.timeZoneId)
                        log.timeConsumed += dist0
                        log.update()
                        todayLogs[taskId] = log
                        Task.updateTimeValues(
                            dbManager, taskId,
                            task.timeConsumed + dist0,
                            maxOf(0, task.timeRemaining - dist0)
                        )
                    } else {
                        val existing = todayLogs[taskId] ?: return
                        val undo = existing.timeConsumed
                        if (undo <= 0) return
                        existing.timeConsumed = 0
                        existing.update()
                        if (existing.id == null) todayLogs.remove(taskId) else todayLogs[taskId] = existing
                        Task.updateTimeValues(
                            dbManager, taskId,
                            maxOf(0, task.timeConsumed - undo),
                            task.timeRemaining + undo
                        )
                    }
                    val idx = allTasks.indexOfFirst { it.taskId == taskId }
                    if (idx >= 0)
                        allTasks[idx] = ViewTaskWithOptions.byId(dbManager, taskId)!!
                }

                val selectedTagIds = remember(tagsFilter) { tagsFilter.map { it.id!! }.toSet() }

                HorizontalPager(pagerState, verticalAlignment = Alignment.Top) { page ->
                    val isToday = PlannerTab.entries[page] == PlannerTab.TODAY
                    val tabTasks = when (PlannerTab.entries[page]) {
                        PlannerTab.TODAY -> allTasks.filter { task ->
                            (planned[task.taskId!!]?.getOrNull(0) ?: 0) > 0 ||
                                    (todayLogs[task.taskId!!]?.timeConsumed ?: 0) > 0
                        }

                        PlannerTab.CURRENT -> allTasks.filter { task ->
                            (task.startConstraintTimestamp == null || task.startConstraintTimestamp!! <= now) &&
                                    !task.isDone
                        }

                        PlannerTab.PAST -> allTasks.filter { it.isDone }

                        PlannerTab.OVERDUE -> allTasks.filter { it.isOverdue(now) }

                        PlannerTab.ALL -> allTasks.toList()
                    }

                    val visibleTasks = if (selectedTagIds.isEmpty()) tabTasks
                    else tabTasks.filter { task ->
                        (taskTagIds[task.entryId!!] ?: emptySet()).containsAll(selectedTagIds)
                    }

                    LazyColumn(Modifier.fillMaxSize()) {
                        items(visibleTasks, key = { it.taskId!! }) { task ->
                            val log = if (isToday) todayLogs[task.taskId!!] else null
                            val todayPlannedQuarters =
                                if (isToday) planned[task.taskId!!]?.getOrNull(0) ?: 0 else 0
                            Task(
                                openTaskDetails, task, now, allTasks, predecessorLinks, dbManager,
                                todayLog = log,
                                todayPlannedQuarters = todayPlannedQuarters,
                                onToggleDone = if (isToday) { markDone ->
                                    toggleTodayDone(task, markDone)
                                } else null,
                                onSchedulePortion = if ((isToday && todayPlannedQuarters > 0) || task.timeRemaining > 0) { ->
                                    val durationMinutes =
                                        if (isToday) todayPlannedQuarters * 15
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
    todayPlannedQuarters: Int = 0,
    onToggleDone: ((Boolean) -> Unit)? = null,
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

        val showToggle = onToggleDone != null && (todayPlannedQuarters > 0 || (todayLog?.timeConsumed ?: 0) > 0)
        if (showToggle || onSchedulePortion != null) {
            Row(Modifier.align(Alignment.End)) {
                if (showToggle) {
                    val isDoneToday = todayPlannedQuarters == 0 && (todayLog?.timeConsumed ?: 0) > 0
                    TextButton(onClick = { onToggleDone(!isDoneToday) }) {
                        Text(
                            if (isDoneToday) stringResource(R.string.planner_undo_today)
                            else stringResource(R.string.planner_done_today, formatTime(context, todayPlannedQuarters))
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
fun PlannerTopBar(
    dbManager: DbManager,
    tagsFilter: List<Tag>,
    tagsFilterUpdate: (List<Tag>) -> Unit,
    openDrawer: () -> Unit,
    onShowPlanDist: () -> Unit,
) {
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
        title = { TopBarTagFilterTitle(dbManager, tagsFilter, tagsFilterUpdate) },
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
