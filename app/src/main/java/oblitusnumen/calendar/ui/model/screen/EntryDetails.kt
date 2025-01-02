package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Notification
import oblitusnumen.calendar.implementation.data.Tag
import java.time.format.DateTimeFormatter

class EntryDetails(
    private val dbManager: DbManager,
    private val entryID: Int
) : ViewModel() {
    private var entry = dbManager.getEntryById(entryID)!!
    private var entryName by mutableStateOf(entry.name)
    private var tags: List<Tag> by mutableStateOf(entry.getTags().sortedBy { it.name })
    private var dates: List<Date> by mutableStateOf(entry.getDates().sortedBy { it.start })
    private var notifications: List<Notification> by mutableStateOf(
        entry.getNotifications().sortedBy { it.offset.secondsApproximation() })
    private var contents by mutableStateOf(entry.getContents())  // FIXME: this should be List<Content>

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun compose(backPress: () -> Unit, modifier: Modifier = Modifier) {
        val ok = remember {
            val entryNullable = dbManager.getEntryById(entryID)
            if (entryNullable == null) {
                backPress()
                return@remember false
            }
            entry = entryNullable
            entryName = entry.name
            tags = entry.getTags().sortedBy { it.name }
            dates = entry.getDates().sortedBy { it.start }
            notifications = entry.getNotifications()
            contents = entry.getContents()
            return@remember true
        }
        if (!ok) return
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
            SelectionContainer {
                Text(entryName, modifier = Modifier.fillMaxWidth().padding(12.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Row {
                Icon(Icons.Filled.Star, "Tags", Modifier.padding(8.dp))
                FlowRow(
                    Modifier.fillMaxWidth().padding(end = 16.dp)
                ) {
                    for (tag in tags)
                        drawTag(tag)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            for (date in dates)
                drawDate(date)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            for (notification in notifications) {
                Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp)) {
                    Icon(
                        if (notification.sound) Icons.Filled.Notifications else Icons.Outlined.Notifications, null,
                        Modifier.align(Alignment.CenterVertically).padding(8.dp)
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                            .weight(1f),
                        text = "${notification.offset.count} ${notification.offset.javaClass.simpleName} before",// FIXME: text
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Text("Description", modifier = Modifier.padding(12.dp))
            SelectionContainer {
                Text(contents,
                    modifier = Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }

    @Composable
    fun drawDate(date: Date) {
        val textStart = (if (date.isPeriodic) "from " else "") +
                date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
        val textPeriod: String = if (date.isPeriodic)
            "every " + date.period.count.toString() + " " + PeriodType(date.period).toString() +
                    if (date.isEndless) "" else
                        " until " + date.getLastZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        else
            PeriodType(date.period).toString()
        Column(Modifier.padding(bottom = 6.dp)) {
            Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp)) {
                Icon(
                    Icons.Outlined.Call, "",
                    Modifier.align(Alignment.CenterVertically).padding(8.dp)
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                        .weight(1f),
                    text = textStart,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).padding(horizontal = 40.dp)) {
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                        .weight(1f),
                    text = textPeriod,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (date.isPeriodic) {
                for (epochDay in date.exceptionRules.listAll()) {
                    val textException = "except " + convertMillisToDate(epochDay * 86400000)
                    Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).padding(horizontal = 40.dp)) {
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                                .weight(1f),
                            text = textException,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun drawTag(tag: Tag) {
        val bgColor = tag.getColorOrDefault()
        InputChip(
            false,
            {},
            {
                Text(
                    tag.name, style = MaterialTheme.typography.bodyLarge,
                    color = bgColorToTextColor(bgColor)
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            colors = InputChipDefaults.inputChipColors(containerColor = bgColor),
        )
    }

    @Composable
    fun topBar(backPress: () -> Unit, editEntry: (Int) -> Unit) {// TODO: confirm
        Row {
            BackButton(backPress)
            Button(onClick = {
                editEntry(entryID)
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("edit")
            }
            Button(onClick = {
                entry.deleteCascade()// FIXME: catch exception
                backPress()
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("delete")
            }
        }
    }
}
