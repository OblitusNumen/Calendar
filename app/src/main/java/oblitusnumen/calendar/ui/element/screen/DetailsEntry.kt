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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import oblitusnumen.calendar.implementation.MILLIS_PER_DAY
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Notification
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import oblitusnumen.calendar.ui.displayCount
import oblitusnumen.calendar.ui.displayOffsetBefore
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.TagChip
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.format.DateTimeFormatter

@Composable
fun EntryDetailsScreen(dbManager: DbManager, entryId: Int, editEntry: () -> Unit, viewTask: () -> Unit, backPress: () -> Unit) {
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
            Row(Modifier.fillMaxWidth()) {
                SelectionContainer(Modifier.weight(1f)) {
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
                if (entry.isTask) {
                    IconButton(onClick = viewTask, modifier = Modifier.align(Alignment.CenterVertically)) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    }
                }
            }

            // description
            if (contents.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Text(stringResource(R.string.details_entry_description), modifier = Modifier.padding(12.dp))
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
                    Icon(Icons.Filled.Star, stringResource(R.string.cd_tags), Modifier.padding(8.dp))
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
    val context = LocalContext.current
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
            text = notification.offset.displayOffsetBefore(context),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun Date(date: Date) {
    val context = LocalContext.current
    val firstFormatted = date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
    val textStart = if (date.isPeriodic) stringResource(R.string.edit_entry_period_from, firstFormatted)
        else firstFormatted

    val textDuration: String = if (!date.hasDuration)
        stringResource(R.string.details_entry_no_duration)
    else
        stringResource(R.string.edit_entry_duration_for, date.duration.displayCount(context))

    val textPeriod: String = if (date.isPeriodic) {
        val every = stringResource(R.string.edit_entry_period_every, date.period.displayCount(context))
        if (date.isEndless) every
        else every + " " + stringResource(
            R.string.edit_entry_period_until,
            date.getLastZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        )
    } else PeriodType(date.period).displayName(context)

    Column(Modifier.padding(bottom = 6.dp)) {
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp)) {
            Icon(
                Icons.Outlined.Schedule, "",
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

        // time zone
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).padding(horizontal = 40.dp)) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                    .weight(1f),
                text = date.timeZoneId.toString(),
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
                val textException = stringResource(R.string.edit_entry_period_except, convertMillisToDate(epochDay * MILLIS_PER_DAY))
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
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
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
