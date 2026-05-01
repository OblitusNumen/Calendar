package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Task
import oblitusnumen.calendar.implementation.data.tables.TaskLink
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
import oblitusnumen.calendar.implementation.now
import oblitusnumen.calendar.ui.MINUTES_PER_QUARTER
import oblitusnumen.calendar.ui.QUARTERS_PER_HOUR
import oblitusnumen.calendar.ui.element.*
import oblitusnumen.calendar.ui.state.TaskEditValidationError
import oblitusnumen.calendar.ui.viewmodel.TaskEditViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EditTaskScreen(
    dbManager: DbManager,
    viewModel: TaskEditViewModel = viewModel(),
    backPress: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // TODO: show/hide options mb same for editEntry

    Scaffold(topBar = {
        EditTopBar(
            stringResource(R.string.edit_task_title),
            {
                viewModel.commitToDb(
                    dbManager,
                    onError = { err ->
                        val msgRes = when (err) {
                            TaskEditValidationError.StartAfterDeadline -> R.string.edit_task_error_start_after_deadline
                            TaskEditValidationError.RecursiveLinks -> R.string.edit_task_error_recursive_links
                            TaskEditValidationError.IllegalLinks -> R.string.edit_task_error_illegal_links
                        }
                        Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_LONG).show()
                    },
                    onSuccess = backPress,
                )
            },
            backPress
        )
    }) { paddingValues ->
        // tag choose
        var tagChoose by remember { mutableStateOf(false) }
        if (tagChoose)
            TagChooseMenu(dbManager, state.tags, { tagChoose = false }, { viewModel.setTags(it) })

        LazyColumn(contentPadding = paddingValues) {
            // name and color
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    value = state.name, onValueChange = {
                        if (!it.text.contains('\n'))
                            viewModel.setName(it)
                    },
                    textStyle = MaterialTheme.typography.titleLarge,
                    label = { Text(stringResource(R.string.edit_task_name_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    trailingIcon = {
                        ColorSelectButton(state.color, true) {
                            viewModel.setColor(it)
                        }
                    }
                )
            }

            // description
            item {
                OutlinedTextField(
                    modifier = Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    value = state.contents, onValueChange = {
                        viewModel.setContents(it)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    label = { Text(stringResource(R.string.edit_task_description_hint)) },
                    minLines = 5
                )
            }

            // draw tags
            item {
                Row {
                    Icon(Icons.Filled.Star, stringResource(R.string.cd_tags), Modifier.padding(8.dp))

                    FlowRow(
                        Modifier.fillMaxWidth().padding(end = 16.dp)
                    ) {
                        for (tag in state.tags)
                            RemovableTagChip(
                                tag.name,
                                tag.colorOrDefault(dbManager),
                                { viewModel.setTags(state.tags - tag) }
                            )
                    }
                }
            }

            // choose tags
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clickable { tagChoose = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.edit_task_choose_tags), style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }

            //release day
            item {
                Column {
                    Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp)) {
                        RadioButton(!state.hasStartConstraint, { viewModel.setStartConstraint(null) })

                        Text(stringResource(R.string.edit_task_available_now), Modifier.align(Alignment.CenterVertically).clickable {
                            viewModel.setStartConstraint(null)
                        })
                    }

                    Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp)) {
                        var localDate by remember {
                            mutableStateOf(
                                Instant.ofEpochSecond(state.startConstraintTimestamp ?: now()).atZone(state.timeZoneId)
                                    .toLocalDate()
                            )
                        }
                        var localTime by remember {
                            mutableStateOf(
                                Instant.ofEpochSecond(state.startConstraintTimestamp ?: now()).atZone(state.timeZoneId)
                                    .toLocalTime()
                            )
                        }

                        val dateTimePicker = remember { DateTimePicker() }
                        dateTimePicker.tryCompose()

                        RadioButton(
                            state.hasStartConstraint,
                            {
                                viewModel.setStartConstraint(
                                    localDate.atTime(localTime).atZone(state.timeZoneId).toEpochSecond()
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically).padding(vertical = 8.dp)
                                .weight(1f).clickable {
                                    viewModel.setStartConstraint(
                                        localDate.atTime(localTime).atZone(state.timeZoneId).toEpochSecond()
                                    )
                                },
                            text = stringResource(R.string.edit_task_available_after),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1
                        )

                        // pick date
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                                .weight(1f).clickable {
                                    dateTimePicker.datePick({}, {
                                        localDate = it
                                        viewModel.setStartConstraint(
                                            ZonedDateTime.of(
                                                localDate.atTime(localTime),
                                                state.timeZoneId
                                            ).toEpochSecond()
                                        )
                                    }, localDate)
                                },
                            text = localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy ")),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1
                        )

                        // pick time
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                                .clickable {
                                    dateTimePicker.timePick({}, {
                                        localTime = it
                                        viewModel.setStartConstraint(
                                            ZonedDateTime.of(
                                                localDate.atTime(localTime),
                                                state.timeZoneId
                                            ).toEpochSecond()
                                        )
                                    }, localTime)
                                },
                            text = localTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }

            //deadline
            item {
                Row {
                    val dateTimePicker = remember { DateTimePicker() }
                    dateTimePicker.tryCompose()

                    var localDate by remember {
                        mutableStateOf(
                            Instant.ofEpochSecond(state.deadlineTimestamp).atZone(state.timeZoneId)
                                .toLocalDate()
                        )
                    }
                    var localTime by remember {
                        mutableStateOf(
                            Instant.ofEpochSecond(state.deadlineTimestamp).atZone(state.timeZoneId)
                                .toLocalTime()
                        )
                    }

                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(vertical = 8.dp)
                            .padding(start = 40.dp),
                        text = stringResource(R.string.edit_task_deadline_at),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )

                    // pick date
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                            .padding(start = 24.dp)
                            .weight(1f).clickable {
                                dateTimePicker.datePick({}, {
                                    localDate = it
                                    viewModel.setDeadline(
                                        ZonedDateTime.of(
                                            localDate.atTime(localTime),
                                            state.timeZoneId
                                        ).toEpochSecond()
                                    )
                                }, localDate)
                            },
                        text = localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy ")),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )

                    // pick time
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                            .clickable {
                                dateTimePicker.timePick({}, {
                                    localTime = it
                                    viewModel.setDeadline(
                                        ZonedDateTime.of(
                                            localDate.atTime(localTime),
                                            state.timeZoneId
                                        ).toEpochSecond()
                                    )
                                }, localTime)
                            },
                        text = localTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }

            //time zone
            item {
                TimeZoneSelector(state.timeZoneId.toString(), { viewModel.setTimeZone(ZoneId.of(it)) })
            }

            // time consumed
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.edit_task_time_elapsed), Modifier.padding(horizontal = 8.dp).weight(1f))

                    IntTextField(
                        state.timeConsumed / QUARTERS_PER_HOUR,
                        { it?.let { h -> viewModel.setTimeConsumed(h * QUARTERS_PER_HOUR + state.timeConsumed % QUARTERS_PER_HOUR) } },
                        modifier = Modifier.width(96.dp).padding(4.dp),
                        trailingIcon = { Text(stringResource(R.string.edit_task_unit_hour), Modifier.padding(horizontal = 4.dp)) },
                        maxDigits = 5)

                    IntTextField(
                        (state.timeConsumed % QUARTERS_PER_HOUR) * MINUTES_PER_QUARTER,
                        { it?.let { m -> viewModel.setTimeConsumed(state.timeConsumed / QUARTERS_PER_HOUR * QUARTERS_PER_HOUR + m / MINUTES_PER_QUARTER) } },
                        modifier = Modifier.width(88.dp).padding(4.dp),
                        trailingIcon = { Text(stringResource(R.string.edit_task_unit_minute), Modifier.padding(horizontal = 4.dp)) },
                        maxDigits = 2)
                }
            }

            // time remaining
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.edit_task_time_remaining), Modifier.padding(horizontal = 8.dp).weight(1f))

                    IntTextField(
                        state.timeRemaining / QUARTERS_PER_HOUR,
                        { it?.let { h -> viewModel.setTimeRemaining(h * QUARTERS_PER_HOUR + state.timeRemaining % QUARTERS_PER_HOUR) } },
                        modifier = Modifier.width(96.dp).padding(4.dp),
                        trailingIcon = { Text(stringResource(R.string.edit_task_unit_hour), Modifier.padding(horizontal = 4.dp)) },
                        maxDigits = 5)

                    IntTextField(
                        (state.timeRemaining % QUARTERS_PER_HOUR) * MINUTES_PER_QUARTER,
                        { it?.let { m -> viewModel.setTimeRemaining(state.timeRemaining / QUARTERS_PER_HOUR * QUARTERS_PER_HOUR + m / MINUTES_PER_QUARTER) } },
                        modifier = Modifier.width(88.dp).padding(4.dp),
                        trailingIcon = { Text(stringResource(R.string.edit_task_unit_minute), Modifier.padding(horizontal = 4.dp)) },
                        maxDigits = 2)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Text(
                    stringResource(R.string.edit_task_predecessors),
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // predecessors
            items(state.predecessors, key = { "pre$it" }) { predecessor ->
                val task = remember { ViewTaskWithOptions.byId(dbManager, predecessor) }!!

                Link(task, TaskLink.checkPredecessor(dbManager, predecessor, state.successors)) {
                    viewModel.setPredecessors(state.predecessors - predecessor)
                }
            }

            item {
                var chooseDialogVisible by remember { mutableStateOf(false) }

                TextButton(onClick = { chooseDialogVisible = true }) {
                    Text(stringResource(R.string.edit_task_add_predecessor))
                }

                if (chooseDialogVisible) {
                    ChooseTasksDialog(
                        dbManager,
                        stringResource(R.string.edit_task_add_predecessors),
                        Task.allIds(dbManager) - state.predecessors - state.successors,
                        { chooseDialogVisible = false },
                        {
                            viewModel.setPredecessors(state.predecessors + it)
                            chooseDialogVisible = false
                        })
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Text(
                    stringResource(R.string.edit_task_successors),
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // successors
            items(state.successors, key = { "suc$it" }) { successor ->
                val task = remember { ViewTaskWithOptions.byId(dbManager, successor) }!!

                Link(task, TaskLink.checkSuccessor(dbManager, successor, state.predecessors)) {
                    viewModel.setSuccessors(state.successors - successor)
                }
            }

            item {
                var chooseDialogVisible by remember { mutableStateOf(false) }

                TextButton(onClick = { chooseDialogVisible = true }) {
                    Text(stringResource(R.string.edit_task_add_successor))
                }

                if (chooseDialogVisible) {
                    ChooseTasksDialog(
                        dbManager,
                        stringResource(R.string.edit_task_add_successors),
                        Task.allIds(dbManager) - state.successors - state.predecessors,
                        { chooseDialogVisible = false },
                        {
                            viewModel.setSuccessors(state.successors + it)
                            chooseDialogVisible = false
                        })
                }
            }
        }
    }
}