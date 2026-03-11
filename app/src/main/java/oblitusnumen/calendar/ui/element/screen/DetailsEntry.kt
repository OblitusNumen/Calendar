package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Notification
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.format.DateTimeFormatter

@Composable
fun DetailsEntryScreen(dbManager: DbManager, entryId: Int, editEntry: () -> Unit, backPress: () -> Unit) {
    val entry = remember { Entry.byId(dbManager, entryId) }!! // FIXME: replace with View

    var entryName by remember { mutableStateOf("entry.name".ifEmpty { "[No title]" }) }
    var tags: List<Tag> by remember { mutableStateOf(entry.getTags(dbManager).sortedBy { it.name }) }
    var dates: List<Date> by remember {
        mutableStateOf(
            entry.getDates(dbManager).sortedBy { it.epochSecondChainStart })
    }
    var notifications: List<Notification> by remember {
        mutableStateOf(
            entry.getNotifications(dbManager).sortedBy { it.offset.secondsApproximation() })
    }
    var contents by remember { mutableStateOf("entry.getContents()") } // FIXME: this should be List<Content>

    Scaffold(topBar = { DetailsEntryTopBar(dbManager, entry, entryName, editEntry, backPress) }) { paddingValues ->
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
            Spacer(Modifier.height(paddingValues.calculateTopPadding()))

            // name and color
            val color by remember { mutableStateOf(Color.Red/*entry.getColorOrDefault()*/) }
            SelectionContainer {
                Row {
                    Box(
                        Modifier.padding(8.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
                            .size(24.dp).align(Alignment.CenterVertically)
                    )
                    Text(
                        entryName,
                        Modifier.align(Alignment.CenterVertically).padding(4.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // description
            if (contents.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Text("Description", modifier = Modifier.padding(12.dp))
                SelectionContainer {
                    Text(
                        contents,
                        modifier = Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    )
                }
            }

            // tags
            if (tags.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Row {
                    Icon(Icons.Filled.Star, "Tags", Modifier.padding(8.dp))
                    FlowRow(
                        Modifier.fillMaxWidth().padding(end = 16.dp)
                    ) {
                        for (tag in tags)
                            DrawTag(dbManager, tag)
                    }
                }
            }

            // dates
            if (dates.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                for (date in dates)
                    DrawDate(date)
            }
            // notifications
            if (notifications.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                for (notification in notifications)
                    DrawNotification(notification)
            }

            Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
fun DrawNotification(notification: Notification) {
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

@Composable
fun DrawDate(date: Date) {
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
fun DrawTag(dbManager: DbManager, tag: Tag) {
    val bgColor = tag.colorOrDefault(dbManager)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsEntryTopBar(
    dbManager: DbManager,
    entry: Entry,
    entryName: String,
    editEntry: () -> Unit,
    backPress: () -> Unit
) {// TODO: confirm
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = { BackPressButton(backPress) },
        title = { Text(entryName, maxLines = 1) },
        actions = {
            IconButton(onClick = {
                editEntry()
            }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null
                )
            }
            IconButton(onClick = {
                entry.deleteCascade(dbManager)// FIXME: catch exception
                backPress()
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null
                )
            }
        },
    )
}
