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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period.Once
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Notification
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.TagChip
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.format.DateTimeFormatter

@Composable
fun DetailsEntryScreen(dbManager: DbManager, entryId: Int, editEntry: () -> Unit, backPress: () -> Unit) {
    val entry = remember { ViewEntryWithOptions.byId(dbManager, entryId) }!! // FIXME: replace with View

    val entryName = remember { entry.displayName }
    val tags: List<Tag> = remember { entry.getTags(dbManager).sortedBy { it.name } }
    val dates: List<Date> = remember {
        entry.getDates(dbManager).sortedBy { it.epochSecondChainStart }
    }
    val notifications: List<Notification> = remember {
        entry.getNotifications(dbManager).sortedBy { it.offset.secondsApproximation() }
    }
    val contents = remember { entry.getContents(dbManager) } // FIXME: this should be List<Content>

    Scaffold(topBar = { DetailsEntryTopBar(dbManager, entry, entryName, editEntry, backPress) }) { paddingValues ->
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
            Spacer(Modifier.height(paddingValues.calculateTopPadding()))

            // name and color
            val color = remember { entry.color }
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
                            TagChip(tag.name, tag.colorOrDefault(dbManager))
                    }
                }
            }

            // dates
            if (dates.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                for (date in dates)
                    Date(date)
            }

            // notifications
            if (notifications.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                for (notification in notifications)
                    Notification(notification)
            }

            Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
fun Notification(notification: Notification) {
    Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp)) {
        // sound
        Icon(
            if (notification.sound) Icons.Filled.Notifications else Icons.Outlined.Notifications, null,
            Modifier.align(Alignment.CenterVertically).padding(8.dp)
        )

        // offset
        Text(
            modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                .weight(1f),
            text = "${notification.offset.count} ${notification.offset.name} before",// FIXME: text
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun Date(date: Date) {
    val textStart = (if (date.isPeriodic) "from " else "") +
            date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))

    val textDuration: String = if (!date.hasDuration)
        "no duration"
    else
        "for ${date.duration.count} ${date.duration.name}"

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

            // time
            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                    .weight(1f),
                text = textStart,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // duration
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).padding(horizontal = 40.dp)) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                    .weight(1f),
                text = textDuration,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // periodic
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).padding(horizontal = 40.dp)) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                    .weight(1f),
                text = textPeriod,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // exceptions
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
