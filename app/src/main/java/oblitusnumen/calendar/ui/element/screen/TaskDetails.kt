package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.now
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.TagChip
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun formatTime(quarterHours: Int): String {
    val hours = quarterHours / 4
    val minutes = (quarterHours % 4) * 15
    return if (minutes == 0) "${hours}h" else "${hours}h ${minutes}m"
}

private fun formatDateTime(epochSecond: Long, zoneId: ZoneId): String =
    Instant.ofEpochSecond(epochSecond).atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))

@Composable
fun TaskDetailsScreen(dbManager: DbManager, taskId: Int, editTask: () -> Unit, backPress: () -> Unit) {
    val task = remember { ViewTaskWithOptions.byId(dbManager, taskId) }!! // FIXME: replace with View
    val now = remember { now() }

    val taskName = remember { task.displayName }
    val tags: List<Tag> = remember { Tag.forEntry(dbManager, task.entryId!!).sortedBy { it.name } }
    val contents = remember { task.getContents(dbManager) }

    val allTasks: Map<Int, ViewTaskWithOptions> = remember {
        ViewTaskWithOptions.all(dbManager).associateBy { it.taskId!! }
    }
    val links = remember { TaskLink.all(dbManager) }
    val predecessorLinks: Map<Int, List<Int>> = remember {
        val map: MutableMap<Int, MutableList<Int>> =
            allTasks.keys.associateWith { mutableListOf<Int>() }.toMutableMap()
        links.forEach { link ->
            map.getOrPut(link.successor) { mutableListOf() }.add(link.predecessor)
            map.getOrPut(link.predecessor) { mutableListOf() }
        }
        map
    }

    Scaffold(topBar = { DetailsTaskTopBar(dbManager, task, taskName, editTask, backPress) }) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            // name and color
            item {
                val color = remember { task.color }
                SelectionContainer {
                    Row {
                        Box(
                            Modifier.padding(8.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
                                .size(24.dp).align(Alignment.CenterVertically)
                        )
                        Text(
                            taskName,
                            Modifier.align(Alignment.CenterVertically).padding(4.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            // description
            item {
                if (contents.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

                    Text("Description", modifier = Modifier.padding(12.dp))

                    SelectionContainer {
                        Text(
                            contents,
                            modifier = Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth()
                                .padding(horizontal = 12.dp).padding(bottom = 12.dp)
                        )
                    }
                }
            }

            // tags
            item {
                if (tags.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

                    Row {
                        Icon(Icons.Filled.Star, "Tags", Modifier.padding(8.dp))

                        FlowRow(Modifier.fillMaxWidth().padding(end = 16.dp)) {
                            for (tag in tags)
                                TagChip(tag.name, tag.colorOrDefault(dbManager))
                        }
                    }
                }
            }

            // timing
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

                val startConstraint = task.startConstraintTimestamp
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    if (startConstraint == null || startConstraint <= now)
                        Text("Available now", style = MaterialTheme.typography.bodyLarge)
                    else
                        Text(
                            "Available from ${formatDateTime(startConstraint, task.timeZoneId)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                }

                val overdue = task.isOverdue(now)
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Flag, contentDescription = null, Modifier.size(20.dp),
                        tint = if (overdue) Color.Red else LocalContentColor.current
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Deadline: ${formatDateTime(task.deadlineTimestamp, task.timeZoneId)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (overdue) Color.Red else Color.Unspecified
                    )
                }

                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(task.timeZoneId.toString(), style = MaterialTheme.typography.bodyLarge)
                }
            }

            // time stats and progress
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

                if (task.isDone) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Done, contentDescription = null, tint = Color.Green)
                        Spacer(Modifier.width(8.dp))
                        Text("Done", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    val predTime = remember { task.countPredecessorsTimeEstimate(allTasks, predecessorLinks) }

                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Time elapsed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(formatTime(task.timeConsumed), style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Time remaining",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(formatTime(task.timeRemaining), style = MaterialTheme.typography.bodyLarge)
                        }
                        if (predTime > 0) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Pred. work",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(formatTime(predTime), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    task.progress?.let { progress ->
                        LinearProgressIndicator(
                            { progress },
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // predecessors tree
            item {// FIXME: make every node different element
                val predecessors = remember { task.predecessors(dbManager) }

                if (predecessors.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                    Text("Dependencies", modifier = Modifier.padding(12.dp))

                    predecessors.forEach { predecessor ->
                        PredecessorTreeNode(dbManager, predecessor, 0, allTasks, predecessorLinks)
                    }
                }
            }

            // successors tree
            item {// FIXME: make every node different element
                val successors = remember { TaskLink.successors(dbManager, taskId) }

                if (successors.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                    Text("Successors", modifier = Modifier.padding(12.dp))
                    successors.forEach { successor ->
                        SuccessorTreeNode(dbManager, successor, 0)
                    }
                }
            }
        }
    }
}

@Composable
fun PredecessorTreeNode(
    dbManager: DbManager,
    taskId: Int,
    level: Int,
    allTasks: Map<Int, ViewTaskWithOptions>,
    predecessorLinks: Map<Int, List<Int>>
) {
    val task = remember { allTasks[taskId] ?: ViewTaskWithOptions.byId(dbManager, taskId)!! }
    var expanded by remember { mutableStateOf(false) }
    val ownPredecessors = remember { predecessorLinks[taskId] ?: emptyList() }

    Row(Modifier.padding(start = (16 * level).dp, end = 4.dp).padding(vertical = 2.dp).clickable {
        expanded = !expanded
    }) {
        if (ownPredecessors.isNotEmpty())
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                Modifier.size(24.dp).align(Alignment.CenterVertically)
            )
        else
            Spacer(Modifier.size(24.dp))

        val color = remember { task.color }
        Box(
            Modifier.padding(8.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
                .size(24.dp).align(Alignment.CenterVertically)
        )

        Text(
            task.displayName,
            Modifier.align(Alignment.CenterVertically).padding(4.dp).weight(1f),
            style = MaterialTheme.typography.titleLarge
        )

        if (task.isDone) {
            Icon(
                Icons.Filled.Done, contentDescription = null,
                Modifier.align(Alignment.CenterVertically), tint = Color.Green
            )
        } else {
            val predTime = remember { task.countPredecessorsTimeEstimate(allTasks, predecessorLinks) }
            if (predTime > 0)
                Text(
                    "+${formatTime(predTime)}",
                    Modifier.padding(horizontal = 4.dp).align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            Text(
                formatTime(task.timeRemaining),
                Modifier.padding(horizontal = 8.dp).align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }

    if (expanded) {
        ownPredecessors.forEach { predecessor ->
            PredecessorTreeNode(dbManager, predecessor, level + 1, allTasks, predecessorLinks)
        }
    }
}

@Composable
fun SuccessorTreeNode(dbManager: DbManager, taskId: Int, level: Int) {
    val task = remember { ViewTaskWithOptions.byId(dbManager, taskId) }!!
    var expanded by remember { mutableStateOf(false) }
    var successors by remember { mutableStateOf<List<Int>?>(null) }

    Row(Modifier.padding(start = (16 * level).dp, end = 4.dp).padding(vertical = 2.dp).clickable {
        expanded = !expanded
        if (successors == null)
            successors = TaskLink.successors(dbManager, taskId)
    }) {
        if (successors == null || successors!!.isNotEmpty())
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                Modifier.size(24.dp).align(Alignment.CenterVertically)
            )
        else
            Spacer(Modifier.size(24.dp))

        val color = remember { task.color }
        Box(
            Modifier.padding(8.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
                .size(24.dp).align(Alignment.CenterVertically)
        )

        Text(
            task.displayName,
            Modifier.align(Alignment.CenterVertically).padding(4.dp).weight(1f),
            style = MaterialTheme.typography.titleLarge
        )

        if (task.isDone)
            Icon(
                Icons.Filled.Done, contentDescription = null,
                Modifier.align(Alignment.CenterVertically), tint = Color.Green
            )
        else
            Text(
                formatTime(task.timeRemaining),
                Modifier.padding(horizontal = 8.dp).align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
    }

    if (expanded) {
        successors?.forEach { successor ->
            SuccessorTreeNode(dbManager, successor, level + 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsTaskTopBar(
    dbManager: DbManager,
    task: Task,
    taskName: String,
    editTask: () -> Unit,
    backPress: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        navigationIcon = { BackPressButton(backPress) },
        title = { Text(taskName, maxLines = 1) },
        actions = {
            IconButton(onClick = editTask) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(onClick = {
                task.deleteCascade(dbManager) // FIXME: catch exception
                backPress()
            }) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
            }
        },
    )
}
