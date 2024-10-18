package oblitusnumen.calendar.ui.model.screen

import android.widget.CalendarView
import android.widget.PopupWindow
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.implementation.Utils
import oblitusnumen.calendar.implementation.data.CalendarDate
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.data.Tag
import oblitusnumen.calendar.implementation.data.content.Content
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.TopBarModifier
import java.time.LocalDateTime

class EntryEdit(val entry: Entry) : Screen, TopBarModifier {
    var newName: MutableState<TextFieldValue>? = null
    var tags: HashSet<Tag> = entry.tags
    var calendarDates: HashSet<CalendarDate> = entry.calendarDates
    var contents: List<Content> = entry.contents

    // TODO:
    @Composable
    override fun compose(calendarViewModel: MainActivity.CalendarViewModel) {
        Column(Modifier.verticalScroll(ScrollState(0))) {
            val entry = (calendarViewModel.screen as EntryEdit).entry
            if (newName == null) newName = remember { mutableStateOf(TextFieldValue(entry.name)) }
            TextField(value = newName!!.value, onValueChange = { t ->
                newName!!.value = t
            }, placeholder = { Text("name") })
            Column {
                for (date in calendarDates) {
                    Row {
                        Text(date.date.toString())
                        val desk = remember { mutableStateOf(TextFieldValue(date.desc)) }
                        TextField(desk.value, onValueChange = { value ->
                            date.desc = value.text
                            desk.value = value
                        })
                        Button(onClick = {
                            calendarDates.remove(date)
                        }) {
                            Text("一")
                        }
                    }
                }
                Button(onClick = {
                    // TODO:
                    calendarDates.add(CalendarDate(LocalDateTime.now(), entry))
                }) {
                    Text("十")
                }
                val addingTag = remember{
                    mutableStateOf(false)
                }
                DisplayTags(addingTag, calendarViewModel)
            }
        }
    }

    @Composable
    fun DisplayTags(addingTag: MutableState<Boolean>, calendarViewModel: MainActivity.CalendarViewModel) {
        if (addingTag.value) {
            Text("addTag")
            val allTags = calendarViewModel.dataManager.tags
            val textFieldValue = remember { mutableStateOf(TextFieldValue("")) }
            TextField(value = textFieldValue.value, onValueChange = { value ->
                textFieldValue.value = value
            })
            val tag = Tag(calendarViewModel.dataManager)
            tag.name = textFieldValue.value.text
            val filteredTags = allTags.filter { t ->
                t.name.contains(textFieldValue.value.text, false) && !tags.contains(t)
            }.plus(tag)
            for (tag in filteredTags) {
                Text(tag.name, Modifier.clickable(onClick = {
                    tags.add(tag)
                    addingTag.value = false
                }))
            }
        } else {
            Text("tags")
            val tagsCache = mutableStateOf(tags)
            Utils.log("ууууууууууууууу")
            for (tag in tagsCache.value) {
                Row {
                    Text(tag.name)
                    Button(onClick = {
                        tags.remove(tag)
                        tagsCache.value = HashSet(tags)
                        Utils.log("hhhhhhhhhhhhhhhhhhh")
                    }) {
                        Text("一")
                    }
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