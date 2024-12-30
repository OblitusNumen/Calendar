package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.ui.model.DateTimePicker
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class EntriesTab(private val dbManager: DbManager) : ViewModel() {

    @Composable
    fun compose(editEntry: (Int) -> Unit, modifier: Modifier) {
        val entries = remember {// TODO: maybe sort by netx date
            dbManager.getEntries().sortedBy { it.name }
        }
        LazyColumn(modifier) {
            items(entries) {
                drawEntry(it, editEntry)
            }
        }
    }

    @Composable
    fun drawTag(text: String, bgColor: Color) {
        Text(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.5.dp)
                .background(
                    bgColor,
                    shape = RoundedCornerShape(10.dp)
                ).padding(vertical = 1.dp, horizontal = 6.dp),
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            color = bgColorToTextColor(bgColor)
        )
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
    @Composable
    fun drawEntry(entry: Entry, editEntry: (Int) -> Unit) {
        val tags = entry.getTags()
        var scheduleDialogShown by remember { mutableStateOf(false) }
        var nextDateText by remember { mutableStateOf(getNextDateText(entry)) }
        Column(
            Modifier.padding(2.dp).fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ).combinedClickable(onLongClick = { scheduleDialogShown = true }, onClick = { editEntry(entry.id!!) })
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
                Text(
                    modifier = Modifier.weight(1.0f).padding(end = 8.dp),
                    text = entry.name,
                    style = MaterialTheme.typography.headlineSmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = nextDateText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }//todo next line "10 events from 2024.01.01 to 2025.01.01"
            if (tags.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 6.5.dp)
                ) {
                    for (tag in tags) {
                        drawTag(tag.name, tag.getColorOrDefault())
                    }
                }
            }
        }
        val dateTimePicker = remember { DateTimePicker() }
        dateTimePicker.tryCompose()
        if (scheduleDialogShown) scheduleDialog(
            entry,
            {
                dateTimePicker.dateTimePick({},
                    {
                        Date(dbManager, entry, "", it.atZone(defaultZoneId()), 0, 1, Period()).create()
                        nextDateText = getNextDateText(entry)
                    })
            }) { scheduleDialogShown = false }
    }

    private fun getNextDateText(entry: Entry): String {
        val now = System.currentTimeMillis() / 1000
        var nextDate: ZonedDateTime? = null
        var hasDates = false
        for (date in entry.getDates()) {
            hasDates = true
            val next = date.getNext(now)
            if (nextDate == null || next != null && next < nextDate) nextDate = next
        }
        val nextDateText = if (nextDate != null)
            nextDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
        else if (hasDates) "Ended" else ""
        return nextDateText
    }

    @Composable
    fun scheduleDialog(entry: Entry, schedule: () -> Unit, onClose: () -> Unit) {
        AlertDialog(
            onDismissRequest = onClose,
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onClose()
                    schedule()
                }) {
                    Text("OK")
                }
            },
            text = {
                Column {
                    Text("Schedule ${entry.name} event?")
                }
            }
        )
    }

    @Composable
    fun topBar() {
        Text("Entries")
    }
}