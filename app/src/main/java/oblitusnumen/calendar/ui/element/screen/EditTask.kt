package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.ui.element.ColorSelectButton
import oblitusnumen.calendar.ui.element.EditTopBar
import oblitusnumen.calendar.ui.element.RemovableTagChip
import oblitusnumen.calendar.ui.viewmodel.TaskEditViewModel

@Composable
fun EditTaskScreen(
    dbManager: DbManager,
    viewModel: TaskEditViewModel = viewModel(),
    backPress: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // TODO: show/hide options mb same for editEntry

    Scaffold(topBar = {
        EditTopBar(
            "Edit task",
            {
                backPress()
                viewModel.commitToDb(dbManager)
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
                    label = { Text("Enter event name") },
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
                    label = { Text("Enter description") },
                    minLines = 5
                )
            }

            // draw tags
            item {
                Row {
                    Icon(Icons.Filled.Star, "Tags", Modifier.padding(8.dp))

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
                Box(Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                    tagChoose = true
                }) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterStart)
                            .padding(horizontal = 44.dp, vertical = 16.dp),
                        text = "Choose tags...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }

            // TODO:
        }
    }
}
