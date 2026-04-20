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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.coroutines.launch
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.implementation.now
import oblitusnumen.calendar.implementation.planTasks
import oblitusnumen.calendar.ui.element.EntryDescriptionAndTags
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.ZonedDateTime
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalKoalaPlotApi::class)
@Composable
fun PlannerScreen(
    dbManager: DbManager,
    tagsFilter: MutableState<List<Tag>>,
    navBar: @Composable () -> Unit,
    openEditNewTask: () -> Unit,
    openTaskDetails: (Int) -> Unit,
    openSettings: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.close() } } }
    val openDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.open() } } }

    ModalNavigationDrawer(
        drawerContent = { PlannerDrawer(closeDrawer, openSettings) },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = { PlannerTopBar(openDrawer) },
            bottomBar = navBar,
            floatingActionButton = {
                FloatingActionButton(onClick = openEditNewTask) {
                    Icon(Icons.Filled.Add, "add task")
                }
            }
        ) { paddingValues ->
            Column {
                val startDestination = PlannerTab.CURRENT
                val pagerState = rememberPagerState(startDestination.ordinal, pageCount = { PlannerTab.entries.size })
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
                                    text = destination.name,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                )
                            }
                        )
                    }
                }

                val now = now()
                val links = remember { TaskLink.all(dbManager) }
                val allTasks = (ViewTaskWithOptions.all(dbManager)).sortedBy { it.progress }
                val predecessorLinks: Map<Int, MutableList<Int>> = remember {
                    val predecessorLinks: Map<Int, MutableList<Int>> =
                        allTasks.map { it.taskId!! }.associateWith { mutableListOf() }
                    links.forEach { link ->
                        predecessorLinks[link.successor]!!.add(link.predecessor)
                    }
                    return@remember predecessorLinks
                }
                val successorsLinks: Map<Int, MutableList<Int>> = remember {
                    val successorsLinks: Map<Int, MutableList<Int>> =
                        allTasks.map { it.taskId!! }.associateWith { mutableListOf() }
                    links.forEach { link ->
                        successorsLinks[link.predecessor]!!.add(link.successor)
                    }
                    return@remember successorsLinks
                }

                val planned = remember {
                    // FIXME: wrong filter
                    val plannedTasks: Array<Task> = allTasks.filter { task ->
                        task.deadlineTimestamp >= now && !task.isDone
                    }.toTypedArray()
                    planTasks(plannedTasks, links, now)
                }

                HorizontalPager(pagerState, verticalAlignment = Alignment.Top) { page ->
                    val tabTasks = remember(page) {
                        when (PlannerTab.entries[page]) {
                            PlannerTab.TODAY -> allTasks.filter { task ->
                                planned[task.taskId!!]?.let { taskDistribution -> taskDistribution[0] > 0 } ?: false
                            }

                            PlannerTab.CURRENT -> allTasks.filter { task ->
                                (task.startConstraintTimestamp == null || task.startConstraintTimestamp!! <= now) &&
                                        !task.isDone
                            }

                            PlannerTab.PAST -> allTasks.filter { it.isDone }

                            PlannerTab.OVERDUE -> allTasks.filter { it.isOverdue(now) }

                            PlannerTab.ALL -> allTasks
                        }
                    }

                    LazyColumn(Modifier.fillMaxSize()) {
                        items(tabTasks, key = { it.taskId!! }) { task ->
                            Task(openTaskDetails, task, now, allTasks, predecessorLinks, dbManager)
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
    dbManager: DbManager
) {
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

//                                Text(
//                                    modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp)
//                                        .align(Alignment.CenterVertically),
//                                    text = "${task.timeConsumed}",
//                                    style = MaterialTheme.typography.headlineSmall,
//                                    overflow = TextOverflow.Ellipsis,
//                                    maxLines = 1,
//                                )
//
//                                Text(
//                                    modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp)
//                                        .align(Alignment.CenterVertically),
//                                    text = "${task.timeRemaining}",
//                                    style = MaterialTheme.typography.headlineSmall,
//                                    overflow = TextOverflow.Ellipsis,
//                                    maxLines = 1,
//                                )

            if (task.isDone)
                Icon(Icons.Filled.Done, tint = Color.Green, contentDescription = "done")
            else
                Text(
                    text = "${getZonedFromEpochSeconds(task.deadlineTimestamp).toLocalDate()}",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    color = if (task.isOverdue(now)) Color.Red else Color.Unspecified,
                    style = MaterialTheme.typography.bodyLarge,
                )
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
            Text(
                text = "${
                    task.countPredecessorsTimeEstimate(
                        allTasks.associateBy { it.taskId!! },
                        predecessorLinks
                    )
                }",
                modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            Text(
                text = "${task.timeRemaining}",
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
    }
}

@Composable
fun PlannerDrawer(
    closeDrawer: () -> Unit,
    openSettings: () -> Unit
) {
    ModalDrawerSheet {
        Row(Modifier.padding(16.dp)) {
            Text("Planner", Modifier.align(Alignment.CenterVertically).weight(1f))
            IconButton(onClick = closeDrawer) {
                Icon(Icons.Filled.Close, contentDescription = "close drawer")
            }
        }

        NavigationDrawerItem(
            label = { Text(text = "Settings") },
            selected = false,
            onClick = {
                openSettings()
                closeDrawer()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerTopBar(openDrawer: () -> Unit) {
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
            IconButton(onClick = {
            }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null
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
