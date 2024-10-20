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
import oblitusnumen.calendar.implementation.data.CalendarDate
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.data.Tag
import oblitusnumen.calendar.implementation.data.content.Content
import oblitusnumen.calendar.implementation.data.content.TextContent
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.TopBarModifier
import java.time.LocalDateTime

class EntryEdit(val entry: Entry) : Screen, TopBarModifier {
    val dates = entry.calendarDates

    // TODO:
    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column(Modifier.verticalScroll(ScrollState(0))) {
            var newName by remember { mutableStateOf(TextFieldValue(entry.name)) }
            TextField(value = newName, onValueChange = { t ->
                newName = t
            }, placeholder = { Text("name") })
            Column {
                Dates()
                Tags(calendarViewModel)
                Contents(calendarViewModel)
            }
        }
    }

    @Composable
    fun Contents(calendarViewModel: MainActivity.CalendarViewModel) {
        Text("contents")
            var flag = remember {mutableStateOf(false)}
            flag.value
        for ((i, content) in entry.contents.withIndex()) {
            when (content) {
                is TextContent -> {
                    TextContent(calendarViewModel, flag, content, i)
                }
            }
        }
        Button(onClick = {
            // TODO:
            contents.add(TextContent())
                        flag.value = !flag.value
        }) {
            Text("十")
        }
    }

    @Composable
    fun TextContent(calendarViewModel: MainActivity.CalendarViewModel, flag: MutableState<Boolean>, textContent: TextContent, i: Int) {
        var editing by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth().wrapContentHeight().clickable(onClick = {
            editing = true
        }).border(2.dp, MaterialTheme.colorScheme.primary)) {
            if (editing) {
                Row {
                    Button(onClick = {
                        contents.remove(textContent)
                        flag.value = !flag.value
                    }) {
                        Text("一")
                    }
                    Button(onClick = {
                        editing = false
                    }) { Text("✓") }
                    var textFieldValue by remember { mutableStateOf(TextFieldValue(textContent.text)) }
                    TextField(textFieldValue, onValueChange = { value ->
                        textContent.text = value.text
                        textFieldValue = value
                    })
                }
            } else {
                Text(textContent.text)
            }
        }
    }

    @Composable
    fun Dates() {
        Text("dates")
            var flag by remember {mutableStateOf(false)}
            flag
        for (date in calendarDates) {
            Row {
                Button(onClick = {
                    calendarDates.remove(date)
                    flag = !flag
                }) {
                    Text("一")
                }
                Text(date.date.toString())
                val desk = remember { mutableStateOf(TextFieldValue(date.desc)) }
                TextField(desk.value, onValueChange = { value ->
                    date.desc = value.text
                    desk.value = value
                })
            }
        }
        Button(onClick = {
            // TODO:
            calendarDates.add(CalendarDate(LocalDateTime.now(), entry))
                        flag = !flag
        }) {
            Text("十")
        }
    }

    @Composable
    fun Tags(calendarViewModel: MainActivity.CalendarViewModel) {
        val addingTag = remember{
            mutableStateOf(false)
        }
        if (addingTag.value) {
            Text("addTag")
            val allTags = calendarViewModel.dataManager.tags
            val textFieldValue = remember { mutableStateOf(TextFieldValue("")) }
            val tag = calendarViewModel.dataManager.getTag(textFieldValue.value.text)
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
                entry.set(newName!!.value.text, tags, calendarDates, contents)
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("save")
            }
            Button(onClick = {
                entry.remove()
                calendarViewModel.back()
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("delete")
            }
        }
    }
}