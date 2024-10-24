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
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.implementation.Utils.toLocalDateTime
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.data.Tag
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.TopBarModifier
import java.time.LocalDateTime

class EntryEdit(val calendarViewModel: MainActivity.CalendarViewModel, val entry: Entry) : Screen, TopBarModifier {
    var newName: MutableState<TextFieldValue>? = null
    var tags: MutableList<Tag>
    var dates: MutableList<Date>
    var contents: String   // FIXME: this should be List<Content>
    
    // TODO:
    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column(Modifier.verticalScroll(ScrollState(0))) {
            val entry = (calendarViewModel.screen as EntryEdit).entry
            if (newName == null) newName = remember { mutableStateOf(TextFieldValue(entry.name)) }
            TextField(value = newName!!.value, onValueChange = { t ->
                newName!!.value = t
            }, placeholder = { Text("name") })
            Dates()
            Tags()
            Contents()
        }
    }

    @Composable
    fun Contents() {
        Box(Modifier.fillMaxWidth().wrapContentHeight().border(2.dp, MaterialTheme.colorScheme.primary)) {
            var textFieldValue by remember { mutableStateOf(TextFieldValue(contents)) }
            TextField(textFieldValue, onValueChange = { value ->
                contents = value.text
                textFieldValue = value
            })
        }
    }

    @Composable
    fun Dates() {
        Text("dates")
            var flag by remember {mutableStateOf(false)}
            flag
        for (date in dates) {
            Row {
                Button(onClick = {
                    dates.remove(date)
                    flag = !flag
                }) {
                    Text("一")
                }
                Text(toLocalDateTime(date.start).toString())
                val desk = remember { mutableStateOf(TextFieldValue(date.desc)) }
                TextField(desk.value, onValueChange = { value ->
                    date.desc = value.text
                    desk.value = value
                })
            }
        }
        Button(onClick = {
            // TODO:
            dates.add(Date(calendarViewModel.dbManager, LocalDateTime.now(), entry))
                        flag = !flag
        }) {
            Text("十")
        }
    }

    @Composable
    fun Tags() {
        val addingTag = remember{
            mutableStateOf(false)
        }
        if (addingTag.value) {
            Text("addTag")
            val allTags = calendarViewModel.dbManager.tags
            val textFieldValue = remember { mutableStateOf(TextFieldValue("")) }
            val tag = Tag(calendarViewModel.dbManager, textFieldValue.value.text)
            val addTag = { tag: Tag ->
                tags.add(tag)
                addingTag.value = false
            }
            TextField(value = textFieldValue.value, onValueChange = { value ->
                if (value.text.contains("\n")) {// FIXME:
                    addTag.invoke(tag)
                }
                textFieldValue.value = value
            })
            val filteredTags = allTags.filter { t ->
                t.name.contains(textFieldValue.value.text, false) && !tags.contains(t)
            }.plus(tag)// FIXME: adding existing tag
            for (tag in filteredTags) {
                Text(tag.name, Modifier.clickable(onClick = {
                    tags.add(tag)
                    addingTag.value = false
                }))
            }
        } else {
            Text("tags")
            var flag by remember {mutableStateOf(false)}
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
    override fun topBar(calendarViewModel: MainActivity.CalendarViewModel) {// TODO: confirm
        Row {
            BackButton(calendarViewModel)
            Button(onClick = {
                entry.set(newName!!.value.text, tags, dates, contents)
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("save")
            }
            Button(onClick = {
                entry.delete()
                calendarViewModel.back()
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("delete")
            }
        }
    }

    init {
        this.tags = entry.tags
        tags.sortBy { it.name }
        this.dates = entry.dates
        dates.sortBy { it.start }
        this.contents = entry.contents
    }
}