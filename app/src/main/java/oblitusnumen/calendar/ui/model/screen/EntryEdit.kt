package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.MainActivity
import oblitusnumen.calendar.implementation.data.CalendarDate
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.data.Tag
import oblitusnumen.calendar.implementation.data.content.Content
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.TopBarModifier

class EntryEdit(val entry: Entry) : Screen, TopBarModifier {
    var newName: MutableState<TextFieldValue>? = null
    var tags: Set<Tag> = entry.tags
    var calendarDates: Set<CalendarDate> = entry.calendarDates
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
                    Text(date.date.toString())
                }
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