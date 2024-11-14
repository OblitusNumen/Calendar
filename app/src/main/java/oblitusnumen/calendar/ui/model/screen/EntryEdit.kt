package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.data.Tag
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class EntryEdit(private val dbManager: DbManager,
                private val entry: Entry,
                private val backPress: () -> Unit) : ViewModel() {
    private var entryName by mutableStateOf(TextFieldValue(entry.name))
    private var tags: List<Tag> by mutableStateOf(entry.tags.sortedBy { it.name })
    private var dates: List<Date> by mutableStateOf(entry.dates.sortedBy { it.start })
    private var contents by mutableStateOf(TextFieldValue(entry.contents))  // FIXME: this should be List<Content>

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun compose(modifier: Modifier = Modifier) {
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
            TextField(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                value = entryName, onValueChange = {
                    if (!it.text.contains('\n'))
                        entryName = it
                },
                textStyle = MaterialTheme.typography.titleLarge,
                placeholder = { Text("Enter event name...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                for (tag in tags)
                    drawTag(tag)
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                tags = tags + Tag.getOrNew(dbManager, "genTag" + (Math.random() * 100000).roundToInt())
                // fixme show screen/menu
            }) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(horizontal = 44.dp, vertical = 4.dp),
                    text = "Add tag...",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            for (date in dates)
                drawDate(date)
            Box(Modifier.fillMaxWidth()/*.padding(top = 8.dp)*/.clickable {
                dates += Date(
                    dbManager,
                    entry,
                    "",
                    ZonedDateTime.now(),
                    0,
                    1,
                    Date.Period(Date.Period.Modifier.WEEK, 1)//fixme proper date adding
                )
            }) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(horizontal = 44.dp, vertical = 4.dp),
                    text = "Add date...",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            /*HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            repeat(3) { todo no notification support yet
                Row(modifier = Modifier.clickable { }) {
                    Icon(
                        Icons.Outlined.Notifications, null,
                        Modifier.align(Alignment.CenterVertically).padding(8.dp)
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                            .weight(1f),
                        text = "30 min before",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {},
                        content = { Icon(Icons.Filled.Clear, contentDescription = null) })
                }
            }
            Box(Modifier.fillMaxWidth()/*.padding(top = 8.dp)*/.clickable { }) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(horizontal = 44.dp, vertical = 4.dp),
                    text = "Add notification",
                    style = MaterialTheme.typography.titleLarge
                )
            }*/
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            TextField(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
                value = contents, onValueChange = {
                    contents = it
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text("Enter description...") },
                minLines = 5
            )
        }
    }

    @Composable
    fun drawDate(date: Date) {
        val text =
            date.getZoneDateTime(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                    " " + date.period + " x" + date.timesRepeat //todo much better algorithm needed
        Row(modifier = Modifier.clickable { }) {
            Icon(
                Icons.Outlined.Call, "",
                Modifier.align(Alignment.CenterVertically).padding(8.dp)
            )
            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                    .weight(1f),
                text = text,
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = { dates -= date },
                content = { Icon(Icons.Filled.Clear, contentDescription = null) })
        }
    }

    @Composable
    fun drawTag(tag: Tag) {
        val bgColor = Color.Transparent //fixme tag color
        InputChip(
            false,
            { tags = tags - tag },
            {
                Text(
                    tag.name, style = MaterialTheme.typography.bodyLarge,
                    color = bgColorToTextColor(bgColor)
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            trailingIcon = {
                Icon(
                    Icons.Filled.Clear, null,
                    tint = bgColorToTextColor(bgColor)
                )
            },
            colors = InputChipDefaults.inputChipColors(containerColor = bgColor),
        )
    }

    @Composable
    fun topBar() {// TODO: confirm
        Row {
            BackButton(backPress)
            Button(onClick = {
                entry.set(entryName.text, tags, dates, contents.text)
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
}