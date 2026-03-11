package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.ui.element.SearchTopBar
import oblitusnumen.calendar.ui.element.colorPicker

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagsScreen(
    dbManager: DbManager,
    navBar: @Composable () -> Unit,
    openEditTag: (id: Int) -> Unit,
    backPress: () -> Unit,
) {
    var newTagEditShown by rememberSaveable { mutableStateOf(false) }
    val searchQuery = rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = { SearchTopBar(searchQuery, backPress) },
        bottomBar = navBar,
        floatingActionButton = { TagsActionButton({ newTagEditShown = true }) }
    ) { innerPadding ->
        val tagsWithEntryCount: MutableMap<Tag, Int> = remember { Tag.allWithEntryCount(dbManager).toMutableMap() }
        val tags: MutableState<List<Tag>> = remember {
            mutableStateOf(tagsWithEntryCount.keys.toList().sortedBy { it.name }
                .sortedByDescending { tagsWithEntryCount[it] })
        }
        val filteredTags =
            remember(tags, searchQuery.value) { tags.value.filter { it.name.contains(searchQuery.value, true) } }
        val tagNames: MutableSet<String> = remember { filteredTags.map { it.name }.toMutableSet() }

        LazyColumn {
            item {
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            }
            items(filteredTags) { tag ->
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                        .padding(horizontal = 8.dp).clickable(onClick = {
                            // TODO: move to entries menu
                            log("open")
                            openEditTag(tag.id!!)
                        })
                ) {
                    var editShown by remember { mutableStateOf(false) }
                    if (editShown) EditTag(dbManager, tagsWithEntryCount, tags, tagNames, tag) {
                        editShown = false
                    }
                    Icon(
                        Icons.Filled.Star, null,
                        Modifier.align(Alignment.CenterVertically).padding(8.dp),
                        tag.colorOrDefault(dbManager)
                    )
                    Text(
                        modifier = Modifier.weight(1.0f).padding(end = 8.dp).align(Alignment.CenterVertically),
                        text = tag.name,
                        style = MaterialTheme.typography.headlineSmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = tagsWithEntryCount[tag].toString() + " event",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    IconButton(modifier = Modifier.size(48.dp).align(Alignment.CenterVertically), onClick = {
                        editShown = true
                    }) {
                        Icon(Icons.Filled.Edit, null)
                    }
                }
            }
            item {
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
        if (newTagEditShown) EditTag(dbManager, tagsWithEntryCount, tags, tagNames) { newTagEditShown = false }
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun EditTag(
    dbManager: DbManager,
    tagsWithEntryCount: MutableMap<Tag, Int>,
    tags: MutableState<List<Tag>>,
    tagNames: MutableSet<String>,
    tag: Tag = Tag(""),
    onClose: () -> Unit
) {
    var hasError: Boolean by remember { mutableStateOf(tag.name == "") }
    var error = "Enter tag name"
    var name by remember { mutableStateOf(tag.name) }
    var color by remember { mutableStateOf(tag.colorOrDefault(dbManager)) }
    AlertDialog(
        onDismissRequest = onClose,
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (hasError) {// TODO:
//                        showErrorToast(error)
                } else {
                    if (tagNames.contains(tag.name))
                        tagNames.remove(tag.name)
                    else {
                        tagsWithEntryCount[tag] = 0
                        tags.value =
                            (tags.value + tag).sortedBy { it.name }.sortedByDescending { tagsWithEntryCount[it] }
                    }
                    tag.createIfNotExists()
                    tag.set(dbManager, name, color)
                    tagNames.add(tag.name)
                    onClose()
                }
            }) {
                Text("OK")
            }
        },
        text = {
            Column {
                Row {
                    val focusRequester = remember { FocusRequester() }
                    var laidOut by remember { mutableStateOf(false) }
                    OutlinedTextField(// FIXME: ui paddings
                        isError = hasError,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f)
                            .onGloballyPositioned { laidOut = true }
                            .focusRequester(focusRequester),
                        value = name, onValueChange = {
                            if (it.isEmpty()) {
                                hasError = true
                                error = "Enter tag name"
                            } else if (tagNames.contains(it) && tag.name != it) {
                                hasError = true
                                error = "Tag with that name already exists"
                            } else {
                                hasError = false
                            }
                            name = it
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        label = { Text("Tag name") }
                    )
                    var colorPickerShown by remember { mutableStateOf(false) }
                    Box(
                        Modifier.align(Alignment.CenterVertically).background(color, CircleShape)
                            .border(0.dp, color, CircleShape).size(48.dp).padding(horizontal = 8.dp)
                            .clickable { colorPickerShown = true }
                    )
                    if (colorPickerShown)
                        colorPicker(color, true) {
                            if (it != null)
                                color = it
                            colorPickerShown = false
                        }
                    LaunchedEffect(laidOut) {
                        focusRequester.requestFocus()
                    }
                }
                if (hasError) Text(error, color = Color.Red)//errortext
            }
        }
    )
}

@Composable
fun TagsActionButton(openNewTagDialog: () -> Unit) {
    FloatingActionButton(openNewTagDialog) {
        Icon(Icons.Filled.Add, "add tag")
    }
}
