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
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.TagChip
import oblitusnumen.calendar.ui.theme.topBarColors

@Composable
fun TaskDetailsScreen(dbManager: DbManager, taskId: Int, editTask: () -> Unit, backPress: () -> Unit) {
    val task = remember { ViewTaskWithOptions.byId(dbManager, taskId) }!! // FIXME: replace with View

    val taskName = remember { task.displayName }
    val tags: List<Tag> = remember { Tag.forEntry(dbManager, task.entryId!!).sortedBy { it.name } }
    val contents = remember { task.getContents(dbManager) }
    // TODO: show/hide options mb same for editEntry

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
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
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

            // predecessors
            item {// FIXME: make every node different element
                val predecessors = task.predecessors(dbManager)

                if (predecessors.isNotEmpty()) {
                    Text("Dependencies", modifier = Modifier.padding(12.dp))

                    predecessors.forEach { predecessor ->
                        PredecessorTreeNode(dbManager, predecessor, 0)
                    }
                }
                // TODO:
            }
            // TODO:
        }
    }
}

@Composable
fun PredecessorTreeNode(dbManager: DbManager, taskId: Int, level: Int) {
    val task = remember { ViewTaskWithOptions.byId(dbManager, taskId) }!!
    var expanded by remember { mutableStateOf(false) }
    var predecessors by remember { mutableStateOf<List<Int>?>(null) }

    Row(Modifier.padding(start = (16 * level).dp, end = 4.dp).padding(vertical = 2.dp).clickable {
        expanded = !expanded
        if (predecessors == null)
            predecessors = task.predecessors(dbManager)
    }) {
        if (predecessors == null || predecessors!!.isNotEmpty())
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
                Icons.Filled.Done,
                contentDescription = "done",
                Modifier.align(Alignment.CenterVertically),
                tint = Color.Green
            )
        } else {
            Text(
                text = "${
                    task.timeRemaining
                    // FIXME: 
//                    task.countPredecessorsTimeEstimate(
//                        allTasks.associateBy { it.taskId!! },
//                        predecessorLinks
//                    )
                }",
                modifier = Modifier.padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }

    if (expanded) {
        predecessors?.forEach { predecessor ->
            PredecessorTreeNode(dbManager, predecessor, level + 1)
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
) {// TODO: confirm
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        navigationIcon = { BackPressButton(backPress) },
        title = { Text(taskName, maxLines = 1) },
        actions = {
            IconButton(onClick = {
                editTask()
            }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null
                )
            }
            IconButton(onClick = {
                task.deleteCascade(dbManager)// FIXME: catch exception
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

