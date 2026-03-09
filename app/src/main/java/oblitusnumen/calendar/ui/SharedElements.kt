package oblitusnumen.calendar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.ui.model.DateTimePicker
import oblitusnumen.calendar.ui.model.colorPicker
import oblitusnumen.calendar.ui.model.screen.DrawTag
import oblitusnumen.calendar.ui.model.screen.TagFilterMenu
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    searchQuery: MutableState<String>,
    backPress: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = { BackPressButton(backPress) },
        title = {
            Row(
                Modifier.background(
                    MaterialTheme.colorScheme.background.copy(alpha = .5f),
                    shape = RoundedCornerShape(100)
                )
                    .clip(RoundedCornerShape(100))
                    .height(40.dp).fillMaxWidth().clickable { /*fixme mb focus on text field*/ }
            ) {
                TextField(searchQuery.value, { it: String ->
                    searchQuery.value = it.replace("\n", "")
                }, Modifier.weight(1f)/*.clip(RoundedCornerShape(100))*/, maxLines = 1)

                if (searchQuery.value.isBlank()) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "filter",
                        Modifier.size(40.dp),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
                    )
                } else {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "filter",
                        Modifier.size(40.dp).clickable { searchQuery.value = "" },
                        MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DrawEntrySelectable(
    entry: Entry,
    nextDate: Long?,
    selected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) { //fixme Entry can not be created without DB
    var nextDateText by remember {
        val nextDateText =
            when (val nextDate = nextDate) {
                null -> ""
                -1L -> "Ended"
                else -> getZonedFromEpochSeconds(nextDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
            }
        mutableStateOf(nextDateText)
    }

    Row {
        if (selected)
            Checkbox(checked = true, onCheckedChange = {})
        Column(
            Modifier.padding(2.dp).fillMaxWidth().defaultMinSize(minHeight = 64.dp).background(
                MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)
            ).combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
//                    Box(
//                        Modifier.padding(end = 8.dp).size(24.dp).background(entry.getColorOrDefault(), CircleShape)
//                            .border(0.dp, entry.getColorOrDefault(), CircleShape).align(Alignment.CenterVertically)
//                    )
//                    Text(
//                        modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp).align(Alignment.CenterVertically),
//                        text = entry.name.ifEmpty { "[No title]" },
//                        style = MaterialTheme.typography.headlineSmall,
//                        overflow = TextOverflow.Ellipsis,
//                        maxLines = 1,
//                    )
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = nextDateText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }//todo next line "10 events from 2024.01.01 to 2025.01.01"
//                drawDescriptionAndTags(dbManager, entry.getContents(), entry.getTags())
        }
    }
}

@Composable
fun ColorSelectButton(
    color: Color,
    allowCustomColor: Boolean,
    readonly: Boolean = false,
    onColorSelected: (Color) -> Unit
) {
    var colorPickerShown by remember { mutableStateOf(false) }
    Box(
        Modifier.padding(horizontal = 8.dp).background(color, CircleShape)
            .border(0.dp, color, CircleShape).size(48.dp).clip(CircleShape)
            .clickable { if (!readonly) colorPickerShown = true }
    )

    if (colorPickerShown)
        colorPicker(color, allowCustomColor) {
            if (it != null) {
                onColorSelected(it)
            }
            colorPickerShown = false
        }
}

@Composable
fun NewEntryFunctionButton(openEditNewEntry: () -> Unit) {
    FloatingActionButton(onClick = openEditNewEntry) {
        Icon(Icons.Filled.Add, "add event")
    }
}

@Composable
fun BackPressButton(backPress: () -> Unit) {
    IconButton(onClick = backPress) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null
        )
    }
}

@Composable
fun EditDoneButton(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null
        )
    }
}

@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = null
        )
    }
}

@Composable
fun TopBarTagFilterTitle(dbManager: DbManager, tagsFilter: List<Tag>, tagsFilterUpdate: (List<Tag>) -> Unit) {
    var isFilterOpen by remember { mutableStateOf(false) }
    if (isFilterOpen)
        TagFilterMenu(dbManager, tagsFilter, { isFilterOpen = false }, tagsFilterUpdate)

    Row(
        Modifier.background(
            MaterialTheme.colorScheme.background.copy(alpha = .5f),
            shape = RoundedCornerShape(100)
        )
            .clip(RoundedCornerShape(100))
            .height(40.dp).fillMaxWidth().clickable { isFilterOpen = true }
    ) {
        LazyRow(Modifier.weight(1f)/*.clip(RoundedCornerShape(100))*/) {
            for (tag in tagsFilter)
                item {
                    DrawTag(dbManager, tag, { isFilterOpen = true }) { tagsFilterUpdate(tagsFilter - tag) }
                }
        }

        Icon(
            Icons.Filled.Face,
            contentDescription = "filter",
            Modifier.size(40.dp),
            MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
        )
    }
}

@Composable
fun ActionButtonWithScroll(onClick: () -> Unit, scrollTo: suspend (LocalDate) -> Unit, positionStatus: PositionStatus) {
    val coroutineScope = rememberCoroutineScope()
    Column {
        if (positionStatus != PositionStatus.Visible) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        scrollTo(LocalDate.now())
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
                    .background(color = MaterialTheme.colorScheme.background, shape = CircleShape).size(36.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = if (positionStatus == PositionStatus.Above)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }

        FloatingActionButton(onClick) {
            Icon(Icons.Filled.Add, "add event")
        }
    }
}

@Composable
fun ScheduleDialog(dbManager: DbManager, entry: Entry, onClose: () -> Unit, onSchedule: () -> Unit) {
    val dateTimePicker = remember { DateTimePicker() }
    dateTimePicker.tryCompose()

    var dialogShown by remember { mutableStateOf(true) }
    if (dialogShown)
        AlertDialog(onDismissRequest = onClose, dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        }, confirmButton = {
            TextButton(onClick = {
                dialogShown = false
                dateTimePicker.dateTimePick(onClose, {
                    Date(
                        entry,
                        it.atZone(defaultZoneId()),
                        Period.Once(),
                        1,
                        Period.Once()
                    ).create(dbManager)
                    onSchedule()
                })
            }) {
                Text("OK")
            }
        }, text = {
            Column {
                // FIXME:
//                    Text("Schedule ${entry.name.ifEmpty { "[No title]" }} event?")
            }
        })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawEntryDescriptionAndTags(dbManager: DbManager, contents: String, tags: List<Tag>) {
    if (contents.isNotEmpty()) {
        Text(
            text = contents,
            modifier = Modifier.padding(12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (tags.isNotEmpty()) {
        FlowRow(
            Modifier.fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 6.5.dp)
        ) {
            for (tag in tags) {
                DrawTag(tag.name, tag.colorOrDefault(dbManager))
            }
        }
    }
}

@Composable
private fun DrawTag(text: String, bgColor: Color) {
    Text(
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.5.dp).background(
            bgColor, shape = RoundedCornerShape(10.dp)
        ).padding(vertical = 1.dp, horizontal = 6.dp),
        text = text,
        maxLines = 1,
        style = MaterialTheme.typography.bodyMedium,
        color = bgColorToTextColor(bgColor)
    )
}