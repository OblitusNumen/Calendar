package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.EditViewTag
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.ui.element.*
import oblitusnumen.calendar.ui.theme.topBarColors

@Composable
fun TagEditScreen(
    dbManager: DbManager,
    tagId: Int,
    openEntryDetails: (Int) -> Unit,
    backPress: () -> Unit
) {
    var editable by remember { mutableStateOf(Entry.forTag(dbManager, tagId).isEmpty()) }
    val edits = remember(editable) {
        log("EditViewTag init: $editable")
        EditViewTag(dbManager, tagId)
    }
    val selectedEntries: MutableState<Set<Int>> =
        remember(editable, edits.entryAssociations) { mutableStateOf(setOf()) }

    var scheduleCounter by remember { mutableIntStateOf(0) }
    val allEntries = remember(scheduleCounter) { ViewEntryWithOptions.all(dbManager).sortedBy { it.nextDate } }

    var addEntriesDialogShown by remember { mutableStateOf(false) }
    if (addEntriesDialogShown)
        AddEntriesDialog(
            dbManager,
            allEntries.filter { it.id !in edits.entryAssociations },
            { edits.addEntryAssociations(*it.toIntArray()) }
        ) { addEntriesDialogShown = false }

    Scaffold(
        topBar = {
            Column {
                TagEditTopBar(
                    dbManager,
                    edits.tagId,
                    editable,
                    edits.tagName,
                    edits.tagColor,
                    { editable = it },
                    { edits.delete(dbManager) },
                    {
                        edits.commit(dbManager)
                    },
                    backPress
                )

                var selectedEntries by selectedEntries
                if (selectedEntries.isNotEmpty())
                    Row(Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))) {
                        Checkbox(
                            checked = selectedEntries.size == edits.entryAssociations.size,
                            onCheckedChange = {
                                selectedEntries = if (it) edits.entryAssociations else setOf()
                            }
                        )

                        Text("${selectedEntries.size}/${edits.entryAssociations.size} selected", Modifier.weight(1f))

                        DeleteButton { edits.rmEntryAssociations(*selectedEntries.toIntArray()) }
                    }
            }
        },
        floatingActionButton = { if (editable) TagEditActionButton { addEntriesDialogShown = true } }
    ) { paddingValues ->
        val tagEntries = remember(edits.entryAssociations) { allEntries.filter { it.id in edits.entryAssociations } }
        log("allEntries: $allEntries")
        log("tagEntries: $tagEntries")
        log("edits.entryAssociations: ${edits.entryAssociations.size}")
        var selectedEntries by selectedEntries

        var scheduleDialogEntry: ViewEntryWithOptions? by remember { mutableStateOf(null) }
        if (scheduleDialogEntry != null)
            ScheduleDialog(dbManager, scheduleDialogEntry!!, { scheduleDialogEntry = null }) {
                scheduleCounter++
                scheduleDialogEntry = null
            }

        LazyColumn(contentPadding = paddingValues) {
            items(tagEntries.size) { index ->
                val entryView = tagEntries[index]
                val id = entryView.id!!
                val selected = id in selectedEntries

                SelectableEntry(
                    dbManager, entryView, selected,
                    if (editable) {
                        { selectedEntries += id }
                    } else {
                        { scheduleDialogEntry = entryView }
                    },
                    if (editable) {
                        {
                            if (selected)
                                selectedEntries -= id
                            else
                                selectedEntries += id
                        }
                    } else {
                        { openEntryDetails(id) }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditTopBar(
    dbManager: DbManager,
    tagId: Int,
    editable: Boolean,
    name: MutableState<String>,
    color: MutableState<Color?>,
    setEditable: (Boolean) -> Unit,
    deleteTag: () -> Unit,
    editDone: () -> Unit,
    backPress: () -> Unit
) {
    log("EditTopBar")
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var name by name
    var color by color
    val tagNames = remember { Tag.all(dbManager).filter { it.id!! != tagId }.map { it.name }.toSet() }

    var error by remember(editable) { mutableStateOf(TagNameError.NONE) }// FIXME: this should be computed// FIXME: this is not taken into account

    Column {
        CenterAlignedTopAppBar(
            colors = topBarColors(),
            scrollBehavior = scrollBehavior,
            navigationIcon = {
                BackPressButton(
                    if (editable) {
                        { setEditable(false) }
                    } else backPress
                )
            },
            title = {
//                val focusRequester = remember { FocusRequester() }
                Column {
                    TextField(// FIXME: ui paddings
                        isError = error != TagNameError.NONE,
                        modifier = Modifier
//                            .focusRequester(focusRequester)
                            .padding(horizontal = 8.dp)
                            .align(Alignment.CenterHorizontally),
                        value = name,
                        readOnly = !editable,
                        onValueChange = { newVal: String ->
                            error = if (newVal.isEmpty()) {
                                TagNameError.EMPTY_NAME
                            } else if (tagNames.contains(newVal) && name != newVal) {
                                TagNameError.ALREADY_EXISTS
                            } else {
                                TagNameError.NONE
                            }
                            name = newVal
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        placeholder = { Text("Enter tag name") }
                    )
                }
//                LaunchedEffect(Unit) {
//                    focusRequester.requestFocus()
//                }
            },
            actions = {
                Row {
                    ColorSelectButton(color ?: dbManager.defaultTagColor, true, !editable) {
                        color = it
                    }

                    if (editable) {
                        EditDoneButton {
                            editDone()
                            setEditable(false)
                        }
                    } else {
                        IconButton(onClick = {
                            setEditable(true)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null
                            )
                        }

                        DeleteTagButton(name, Entry.countForTag(dbManager, tagId)) {
                            deleteTag()
                            backPress()
                        }
                    }
                }
            },
        )

        if (error != TagNameError.NONE)
            Text(
                when (error) {
                    TagNameError.EMPTY_NAME -> "Enter tag name"
                    TagNameError.ALREADY_EXISTS -> "Tag with that name already exists"
                    else -> throw RuntimeException("Unknown error")
                },
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = .5f)),
                color = Color.Red
            )
    }
}

@Composable
fun DeleteTagButton(
    tagName: String,
    entriesNumber: Int,
    onDelete: () -> Unit
) {
    var deleteDialogShown by remember { mutableStateOf(false) }
    DeleteButton { deleteDialogShown = true }

    if (deleteDialogShown)
        DeleteTag(tagName, entriesNumber, onDelete) { deleteDialogShown = false }
}

@Composable
fun DeleteTag(
    tagName: String,
    entriesNumber: Int,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onClose()
            }) {
                Text("OK")
            }
        },
        text = {
            Column {
                Text("Delete tag ${tagName}?")
                if (entriesNumber > 0)
                    Text(
                        "$entriesNumber events are associated with this tag.",
                        color = Color.Red
                    )
            }
        }
    )
}

@Composable
fun TagEditActionButton(onClick: () -> Unit) {
    FloatingActionButton(onClick) {
        Icon(Icons.Filled.Add, "add events to tag")
    }
}

@Composable
fun AddEntriesDialog(
    dbManager: DbManager,
    allEntries: List<ViewEntryWithOptions>,
    addEntries: (Set<Int>) -> Unit,
    onClose: () -> Unit
) {
    var selectedEntries: Set<Int> by remember { mutableStateOf(setOf()) }

    AlertDialog(
        onDismissRequest = onClose,
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                addEntries(selectedEntries)
                onClose()
            }) {
                Text("OK")
            }
        },
        text = {
            Column {
                Row(Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))) {
                    Checkbox(
                        checked = selectedEntries.size == allEntries.size,
                        onCheckedChange = { value ->
                            selectedEntries = if (value) allEntries.map { it.id!! }.toSet() else setOf()
                        }
                    )

                    Text("${selectedEntries.size}/${allEntries.size} selected", Modifier.weight(1f))


                    IconButton({ selectedEntries = setOf() }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = null
                        )
                    }
                }

                LazyColumn {
                    items(allEntries.size) { index ->
                        val entryView = allEntries[index]
                        val id = entryView.id!!
                        val selected = id in selectedEntries

                        SelectableEntry(
                            dbManager,
                            entryView,
                            selected,
                            { selectedEntries += id },
                            { if (selected) selectedEntries -= id else selectedEntries += id }
                        )
                    }
                }
            }
        }
    )
}

enum class TagNameError {
    NONE,
    EMPTY_NAME,
    ALREADY_EXISTS,
}
