package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Tag
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt

class TagsTab(private val dbManager: DbManager, private val editTag: (Int) -> Unit) : ViewModel() {
    private var newTagEditShown by mutableStateOf(false)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun compose() {
        val tagsWithEntryCount: MutableMap<Tag, Int> = remember { dbManager.getAllTagsWithEntryCount().toMutableMap() }
        val tags: MutableState<List<Tag>> = remember {
            mutableStateOf(tagsWithEntryCount.keys.toList().sortedBy { it.name }
                .sortedByDescending { tagsWithEntryCount[it] })
        }
        val tagNames: MutableSet<String> = remember { tags.value.map { it.name }.toMutableSet() }
        Column(Modifier.verticalScroll(ScrollState(0)).fillMaxWidth()) {
            for (tag in tags.value) {
                var deleteShown by remember { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                        .padding(horizontal = 8.dp).combinedClickable(onClick = {
                            // TODO: move to entries menu
                        }, onLongClick = {
                            deleteShown = true
                        })
                ) {
                    if (deleteShown) deleteTag(tagsWithEntryCount, tags, tagNames, tag) {
                        deleteShown = false
                    }
                    var editShown by remember { mutableStateOf(false) }
                    if (editShown) editTag(tagsWithEntryCount, tags, tagNames, tag) {
                        editShown = false
                    }
                    Icon(
                        Icons.Filled.Star, null,
                        Modifier.align(Alignment.CenterVertically).padding(8.dp),
                        tag.getColorOrDefault()
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
        }
        if (newTagEditShown) editTag(tagsWithEntryCount, tags, tagNames) { newTagEditShown = false }
    }

    @Composable
    fun deleteTag(
        tagsWithEntryCount: MutableMap<Tag, Int>,
        tags: MutableState<List<Tag>>,
        tagNames: MutableSet<String>,
        tag: Tag,
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
                    tagsWithEntryCount.remove(tag)
                    tags.value -= tag
                    tagNames.remove(tag.name)
                    tag.deleteCascade()
                    onClose()
                }) {
                    Text("OK")
                }
            },
            text = {
                Column {
                    Text("Delete tag ${tag.name}?")
                    if (tagsWithEntryCount[tag]!! > 0) Text(
                        "${tagsWithEntryCount[tag]!!} events are associated with this tag.",
                        color = Color.Red
                    )
                }
            }
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Composable
    fun editTag(
        tagsWithEntryCount: MutableMap<Tag, Int>,
        tags: MutableState<List<Tag>>,
        tagNames: MutableSet<String>,
        tag: Tag = Tag.new(dbManager, ""),
        onClose: () -> Unit
    ) {
        var hasError: Boolean by remember { mutableStateOf(tag.name == "") }
        var error = "Enter tag name"
        var name by remember { mutableStateOf(tag.name) }
        var color by remember { mutableStateOf(tag.getColorOrDefault()) }
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
                        tag.set(name, color)
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
                        OutlinedTextField(// FIXME: ui paddings
                            isError = hasError,
                            modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
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
                        Box(
                            Modifier.align(Alignment.CenterVertically).background(color, CircleShape)
                                .border(0.dp, color, CircleShape).size(48.dp).padding(horizontal = 8.dp)
                        )
                    }
                    if (hasError) Text(error, color = Color.Red)//errortext
                    val focusManager = LocalFocusManager.current
                    colorPicker(color) {
                        color = it
                        focusManager.clearFocus()
                    }
                    var value: String by remember(color) { mutableStateOf(String.format("#%06X", color.toInt())) }
                    OutlinedTextField(// FIXME: ui paddings
                        isError = try {
                            value.substring(1).hexToInt()
                            value.length != 7
                        } catch (_: NumberFormatException) {
                            true
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        value = value, onValueChange = {
                            if (it.startsWith("#") && it.length <= 7) {
                                value = it
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            try {
                                if (value.length != 7) return@KeyboardActions
                                color = value.substring(1).hexToInt().toColor()!!
                                focusManager.clearFocus()
                            } catch (_: NumberFormatException) {
                            }
                        }),
                        label = { Text("Tag name") }
                    )
                }
            }
        )
    }

    @Composable
    fun colorPicker(color: Color, onColorChanged: (Color) -> Unit) {// FIXME: initial color is not being set
//        val controller = rememberColorPickerController()
//        HsvColorPicker(
//            modifier = Modifier.fillMaxWidth().height(450.dp).padding(10.dp),
//            controller = controller,
//            onColorChanged = { onColorChanged(it.color) }
//        )
    }

    @Composable
    fun functionButton() {
        FloatingActionButton(onClick = {
            newTagEditShown = true
        }) {
            Icon(Icons.Filled.Add, "add tag")
        }
    }
}