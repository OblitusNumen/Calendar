package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.TaskLog
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.ui.MINUTES_PER_QUARTER
import oblitusnumen.calendar.ui.QUARTERS_PER_HOUR
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.IntTextField
import oblitusnumen.calendar.ui.element.MinuteDropdown
import oblitusnumen.calendar.ui.element.SectionDivider
import oblitusnumen.calendar.ui.element.SectionHeader
import oblitusnumen.calendar.ui.formatTime
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLogsScreen(dbManager: DbManager, backPress: () -> Unit) {
    val logs: SnapshotStateList<TaskLog> = remember {
        mutableStateListOf<TaskLog>().also { it.addAll(TaskLog.all(dbManager)) }
    }
    var tasks: List<ViewTaskWithOptions> by remember {
        mutableStateOf(ViewTaskWithOptions.all(dbManager))
    }
    val tasksById: Map<Int, ViewTaskWithOptions> = remember(tasks) {
        tasks.associateBy { it.taskId!! }
    }
    var editing: TaskLog? by remember { mutableStateOf(null) }
    var showStats by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    fun syncTaskTotals(taskId: Int, delta: Int) {
        if (delta == 0) return
        val task = Task.byId(dbManager, taskId) ?: return
        Task.updateTimeValues(
            dbManager,
            taskId,
            maxOf(0, task.timeConsumed + delta),
            maxOf(0, task.timeRemaining - delta),
        )
    }

    fun refreshTasks() {
        tasks = ViewTaskWithOptions.all(dbManager)
    }

    fun commitEdit(log: TaskLog, newQuarters: Int) {
        val delta = newQuarters - log.timeConsumed
        if (delta == 0) return
        syncTaskTotals(log.taskId, delta)
        log.timeConsumed = newQuarters
        log.update()
        if (log.timeConsumed <= 0)
            logs.remove(log)
        refreshTasks()
    }

    fun deleteLog(log: TaskLog) {
        syncTaskTotals(log.taskId, -log.timeConsumed)
        log.delete()
        logs.remove(log)
        refreshTasks()
    }

    fun addLog(taskId: Int, addedQuarters: Int) {
        if (addedQuarters <= 0) return
        val zone = defaultZoneId()
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toEpochSecond()
        val existingIdx = logs.indexOfFirst { it.taskId == taskId && it.startOfDayTimestamp == todayStart }
        val log = if (existingIdx >= 0) logs[existingIdx]
        else TaskLog.upsert(dbManager, taskId, todayStart, zone)
        log.timeConsumed += addedQuarters
        log.update()
        syncTaskTotals(taskId, addedQuarters)
        if (existingIdx < 0) {
            logs.add(0, log)
        }
        refreshTasks()
    }

    Scaffold(
        topBar = { TaskLogsTopBar(backPress, onShowStats = { showStats = true }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_log))
            }
        },
    ) { paddingValues ->
        if (logs.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.task_logs_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val grouped = logs.groupBy { it.startOfDayTimestamp }
            LazyColumn(contentPadding = paddingValues) {
                grouped.forEach { (startOfDay, dayLogs) ->
                    val date = Instant.ofEpochSecond(startOfDay)
                        .atZone(dayLogs.first().timeZoneId).toLocalDate()
                    item(key = "header-$startOfDay") {
                        SectionHeader(date.format(dateFormatter), emphasised = true)
                    }
                    items(dayLogs, key = { it.id ?: -it.hashCode() }) { log ->
                        TaskLogRow(
                            log = log,
                            task = tasksById[log.taskId],
                            onEdit = { editing = log },
                            onDelete = { deleteLog(log) }
                        )
                    }
                    item(key = "divider-$startOfDay") { SectionDivider(tight = true) }
                }
            }
        }
    }

    val current = editing
    if (current != null) {
        EditLogDialog(
            initialQuarters = current.timeConsumed,
            onDismiss = { editing = null },
            onSave = { newQuarters ->
                commitEdit(current, newQuarters)
                editing = null
            }
        )
    }

    if (showStats) {
        TaskCompletionStatsDialog(
            tasks = tasks,
            logs = logs,
            onDismiss = { showStats = false },
        )
    }

    if (showAdd) {
        AddLogDialog(
            tasks = tasks.filter { !it.isDone },
            onDismiss = { showAdd = false },
            onSave = { taskId, quarters ->
                addLog(taskId, quarters)
                showAdd = false
            },
        )
    }
}

@Composable
private fun TaskLogRow(
    log: TaskLog,
    task: ViewTaskWithOptions?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task != null) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(task.color)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = task?.displayName ?: stringResource(R.string.task_logs_unknown_task),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = formatTime(context, log.timeConsumed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_log))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_log))
        }
    }
}

@Composable
private fun EditLogDialog(
    initialQuarters: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var draft by remember { mutableStateOf(initialQuarters) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.task_logs_edit_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.task_logs_consumed_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                QuartersInput(draft = draft, onDraftChange = { draft = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(stringResource(R.string.task_logs_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.task_logs_cancel))
            }
        }
    )
}

@Composable
private fun QuartersInput(draft: Int, onDraftChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IntTextField(
            value = draft / QUARTERS_PER_HOUR,
            onValueChange = { v ->
                v?.let { h -> onDraftChange(h * QUARTERS_PER_HOUR + draft % QUARTERS_PER_HOUR) }
            },
            modifier = Modifier.width(110.dp).padding(4.dp),
            trailingIcon = {
                Text(
                    stringResource(R.string.edit_task_unit_hour),
                    Modifier.padding(horizontal = 4.dp)
                )
            },
            maxDigits = 5
        )
        MinuteDropdown(
            selectedMinutes = (draft % QUARTERS_PER_HOUR) * MINUTES_PER_QUARTER,
            onMinuteSelected = { m ->
                onDraftChange(draft / QUARTERS_PER_HOUR * QUARTERS_PER_HOUR + m / MINUTES_PER_QUARTER)
            },
            modifier = Modifier.width(100.dp).padding(4.dp),
        )
    }
}

@Composable
private fun TaskCompletionStatsDialog(
    tasks: List<ViewTaskWithOptions>,
    logs: List<TaskLog>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val totalQuarters = logs.sumOf { it.timeConsumed }
    val daysCount = logs.map { it.startOfDayTimestamp }.distinct().size
    val taskTotals = logs.groupBy { it.taskId }.mapValues { entry -> entry.value.sumOf { it.timeConsumed } }
    val tasksWithLogs = tasks.filter { taskTotals.containsKey(it.taskId) }
        .sortedByDescending { taskTotals[it.taskId] ?: 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.task_logs_stats_title)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.task_logs_cancel))
            }
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.task_logs_stats_total_time, formatTime(context, totalQuarters)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.task_logs_stats_days, daysCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                if (tasksWithLogs.isEmpty()) {
                    Text(
                        stringResource(R.string.task_logs_stats_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(tasksWithLogs, key = { it.taskId!! }) { task ->
                            StatsTaskRow(
                                task = task,
                                loggedQuarters = taskTotals[task.taskId] ?: 0,
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun StatsTaskRow(task: ViewTaskWithOptions, loggedQuarters: Int) {
    val context = LocalContext.current
    val total = task.timeConsumed + task.timeRemaining
    val progress = task.progress ?: if (task.isDone) 1f else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(task.color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = task.displayName,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.task_logs_stats_progress,
                    formatTime(context, task.timeConsumed),
                    formatTime(context, total),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        if (loggedQuarters != task.timeConsumed) {
            Text(
                text = formatTime(context, loggedQuarters),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun AddLogDialog(
    tasks: List<ViewTaskWithOptions>,
    onDismiss: () -> Unit,
    onSave: (taskId: Int, quarters: Int) -> Unit,
) {
    var selectedTaskId: Int? by remember { mutableStateOf(tasks.firstOrNull()?.taskId) }
    var draft by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.task_logs_add_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.task_logs_pick_task),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tasks.isEmpty()) {
                    Text(
                        stringResource(R.string.task_logs_no_tasks),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(tasks, key = { it.taskId!! }) { task ->
                            TaskPickerRow(
                                task = task,
                                selected = task.taskId == selectedTaskId,
                                onSelect = { selectedTaskId = task.taskId },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.task_logs_consumed_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                QuartersInput(draft = draft, onDraftChange = { draft = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedTaskId != null && draft > 0,
                onClick = { selectedTaskId?.let { onSave(it, draft) } },
            ) {
                Text(stringResource(R.string.task_logs_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.task_logs_cancel))
            }
        }
    )
}

@Composable
private fun TaskPickerRow(task: ViewTaskWithOptions, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(task.color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = task.displayName,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskLogsTopBar(backPress: () -> Unit, onShowStats: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        navigationIcon = { BackPressButton(backPress) },
        title = { Text(stringResource(R.string.task_logs_title), maxLines = 1) },
        actions = {
            IconButton(onClick = onShowStats) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = stringResource(R.string.cd_task_stats),
                )
            }
        },
    )
}
