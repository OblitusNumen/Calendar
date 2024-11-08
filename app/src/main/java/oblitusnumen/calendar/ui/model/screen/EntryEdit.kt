package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.data.Tag
import oblitusnumen.calendar.implementation.defaultZoneId
import java.time.ZonedDateTime

class EntryEdit(private val dbManager: DbManager,
                private val entry: Entry,
                private val backPress: () -> Unit) : ViewModel() {
    private var newName: MutableState<TextFieldValue>? = null
    private var tags: MutableList<Tag> = entry.tags
    private var dates: MutableList<Date>
    private var contents: String   // FIXME: this should be List<Content>

    // TODO:
    @Composable
    fun compose(modifier: Modifier = Modifier) {
        Column(modifier.verticalScroll(ScrollState(0))) {
            if (newName == null) newName = remember { mutableStateOf(TextFieldValue(entry.name)) }
            TextField(value = newName!!.value, onValueChange = { t ->
                newName!!.value = t
            }, placeholder = { Text("name") })
            dates()
            tags()
            contents()
        }
    }

    @Composable
    fun contents() {
        Box(Modifier.fillMaxWidth().wrapContentHeight().border(2.dp, MaterialTheme.colorScheme.primary)) {
            var textFieldValue by remember { mutableStateOf(TextFieldValue(contents)) }
            TextField(textFieldValue, modifier = Modifier.fillMaxWidth(), onValueChange = { value ->
                contents = value.text
                textFieldValue = value
            })
        }
    }

    @Composable
    fun dates() {
        Text("dates")
        var flag by remember { mutableStateOf(false) }
        flag
        for (date in dates) {
            Row {
                Button(onClick = {
                    dates.remove(date)
                    flag = !flag
                }) {
                    Text("一")
                }
                Text(date.getZoneDateTime(defaultZoneId(), 0).toString())// FIXME: index might be not 0
                val desk = remember { mutableStateOf(TextFieldValue(date.desc)) }
                TextField(desk.value, onValueChange = { value ->
                    date.desc = value.text
                    desk.value = value
                })
            }
        }
        Button(onClick = {
            // TODO:
            dates.add(
                Date(
                    dbManager,
                    entry,
                    "",
                    ZonedDateTime.now(),
                    0,
                    10,
                    Date.Period(Date.Period.Modifier.WEEK, 1)
                )
            )
            flag = !flag
        }) {
            Text("十")
        }
    }

    @Composable
    fun tags() {
        val addingTag = remember {
            mutableStateOf(false)
        }
        if (addingTag.value) {
            Text("addTag")
            val allTags = dbManager.tags
            val textFieldValue = remember { mutableStateOf(TextFieldValue("")) }
            val newTag = Tag(dbManager, textFieldValue.value.text)
            val addTag = { tag: Tag ->
                tags.add(tag)
                addingTag.value = false
            }
            TextField(value = textFieldValue.value, onValueChange = { value ->
                if (value.text.contains("\n")) {// FIXME:
                    addTag.invoke(newTag)
                }
                textFieldValue.value = value
            })
            val filteredTags = allTags.filter { t ->
                t.name.contains(textFieldValue.value.text, false) && !tags.contains(t)
            }.plus(newTag)// FIXME: adding existing tag
            for (tag in filteredTags) {
                Text(tag.name, Modifier.clickable(onClick = {
                    tags.add(tag)
                    addingTag.value = false
                }))
            }
        } else {
            Text("tags")
            var flag by remember { mutableStateOf(false) }
            flag
            for (tag in tags) {
                Row {
                    Button(onClick = {
                        tags.remove(tag)
                        flag = !flag
                    }) {
                        Text("一")
                    }
                    Text(tag.name)
                }
            }
            Button(onClick = {
                // TODO:
                addingTag.value = true
            }) {
                Text("十")
            }
        }
    }

    @Composable
    fun topBar() {// TODO: confirm
        Row {
            BackButton(backPress)
            Button(onClick = {
                entry.set(newName!!.value.text, tags, dates, contents)
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("save")
            }
            Button(onClick = {
                entry.delete()
                backPress()
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("delete")
            }
        }
    }

    init {
        tags.sortBy { it.name }
        this.dates = entry.dates
        dates.sortBy { it.start }
        this.contents = entry.contents
    }
}