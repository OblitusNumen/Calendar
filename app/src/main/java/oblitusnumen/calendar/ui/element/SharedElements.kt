package oblitusnumen.calendar.ui.element

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.Period.Once
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.getZonedFromEpochSeconds
import oblitusnumen.calendar.ui.PositionStatus
import oblitusnumen.calendar.ui.element.screen.ExcludeOccurrenceDialog
import oblitusnumen.calendar.ui.element.screen.OffsetType
import oblitusnumen.calendar.ui.element.screen.PeriodType
import oblitusnumen.calendar.ui.element.screen.TagFilterMenu
import oblitusnumen.calendar.ui.navigation.NavRoutes
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTopBar(
    titleText: String,
    onDone: () -> Unit,
    backPress: () -> Unit
) {// TODO: confirm
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = { BackPressButton(backPress) },
        title = { Text(titleText, maxLines = 1) },
        actions = { EditDoneButton(onDone) },
    )
}

@Composable
fun NotificationAddMenu(onConfirm: (Period, Boolean) -> Unit, onDismiss: () -> Unit) {
    var silent by remember { mutableStateOf(false) }
    val initialCount = 1L
    var offsetCount: Long by remember { mutableStateOf(initialCount) }
    var selectedOffsetType by remember { mutableStateOf(OffsetType(Once())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val count = if (offsetCount < 0)
                    if (selectedOffsetType.period is Once)
                        0
                    else {// TODO:
//                    showErrorToast("Could not parse count: '$periodCountText'")
                        return@TextButton
                    }
                else
                    offsetCount

                onConfirm(
                    if (count == 0L || selectedOffsetType.period is Once)
                        Once()
                    else {
                        selectedOffsetType.period.updateCount(count)
                    },
                    !silent
                )
            }) {
                Text("OK")
            }
        },
        text = {
            Column {
                OffsetSelector(OffsetType(Once()), initialCount, { selectedOffsetType = it }, { offsetCount = it })

                Row {
                    Text("silent")
                    Switch(silent, onCheckedChange = { silent = it })
                }
            }
        }
    )
}

@Composable
fun PeriodSelector(
    initialPeriodType: PeriodType,
    initialCount: Long?,
    onSelectPeriodType: (PeriodType) -> Unit,
    onSetCount: (Long) -> Unit
) {
    var offsetCount: TextFieldValue by remember { mutableStateOf(TextFieldValue(initialCount?.let { "$it" } ?: "")) }
    var selectedPeriodType by remember { mutableStateOf(initialPeriodType) }

    val onSetCount = { newCount: TextFieldValue ->
        offsetCount = newCount
        val offsetCountText = newCount.text
        onSetCount(if (offsetCountText.isEmpty() || offsetCountText.toLong() < 0L) -1 else offsetCountText.toLong())
    }
    val onSelectPeriodType = { newPeriodType: PeriodType ->
        selectedPeriodType = newPeriodType
        onSelectPeriodType(newPeriodType)
    }

    Row {
        //offset text field
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(// FIXME: ui paddings
            enabled = selectedPeriodType.period !is Once,
            modifier = Modifier.width(100.dp).padding(horizontal = 8.dp)
                .focusRequester(focusRequester),
            value = offsetCount, onValueChange = {
                val text = it.text
                try {
                    if (text.toLong() >= 0 && text.length <= 3)
                        onSetCount(it)
                } catch (_: NumberFormatException) {
                    if (text.isEmpty())
                        onSetCount(it)
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            label = { Text("Count") }
        )

        materialSpinner(
            "Type", PeriodType.getAll(),
            onSelectPeriodType,
            selectedPeriodType,
            Modifier.padding(horizontal = 8.dp).width(150.dp)
        )

        LaunchedEffect(selectedPeriodType) {
            if (selectedPeriodType.period !is Once) {
                // Place cursor at the end of current text
                offsetCount = offsetCount.copy(selection = TextRange(offsetCount.text.length))
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun OffsetSelector(
    initialOffsetType: OffsetType,
    initialCount: Long?,
    onSelectOffsetType: (OffsetType) -> Unit,
    onSetCount: (Long) -> Unit
) {
    var offsetCount: TextFieldValue by remember { mutableStateOf(TextFieldValue(initialCount?.let { "$it" } ?: "")) }
    var selectedOffsetType by remember { mutableStateOf(initialOffsetType) }

    val onSetCount = { newCount: TextFieldValue ->
        offsetCount = newCount
        val offsetCountText = newCount.text
        onSetCount(if (offsetCountText.isEmpty() || offsetCountText.toLong() < 0L) -1 else offsetCountText.toLong())
    }
    val onSelectOffsetType = { newOffsetType: OffsetType ->
        selectedOffsetType = newOffsetType
        onSelectOffsetType(newOffsetType)
    }

    Row {
        //offset text field
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(// FIXME: ui paddings
            enabled = selectedOffsetType.period !is Once,
            modifier = Modifier.width(100.dp).padding(horizontal = 8.dp)
                .focusRequester(focusRequester),
            value = offsetCount, onValueChange = {
                val text = it.text
                try {
                    if (text.toLong() >= 0 && text.length <= 3)
                        onSetCount(it)
                } catch (_: NumberFormatException) {
                    if (text.isEmpty())
                        onSetCount(it)
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            label = { Text("Count") }
        )

        materialSpinner(
            "Type", OffsetType.getAll(),
            onSelectOffsetType,
            selectedOffsetType,
            Modifier.padding(horizontal = 8.dp).width(150.dp)
        )

        LaunchedEffect(selectedOffsetType) {
            if (selectedOffsetType.period !is Once) {
                // Place cursor at the end of current text
                offsetCount = offsetCount.copy(selection = TextRange(offsetCount.text.length))
                focusRequester.requestFocus()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SelectableEntry(
    dbManager: DbManager,
    entryView: ViewEntryWithOptions,
    selected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) { //fixme Entry can not be created without DB
    var nextDateText by remember {
        val nextDateText =
            when (val nextDate = entryView.nextDate) {
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
                Box(
                    Modifier.padding(end = 8.dp).size(24.dp).background(entryView.color, CircleShape)
                        .border(0.dp, entryView.color, CircleShape).align(Alignment.CenterVertically)
                )
                Text(
                    modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp).align(Alignment.CenterVertically),
                    text = entryView.displayName,
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

            EntryDescriptionAndTags(dbManager, entryView.getContents(dbManager), entryView.getTags(dbManager))
        }
    }
}

@Composable
fun Entry(dbManager: DbManager, occurrence: DateOccurrence, openEntryInfo: () -> Unit) { //todo maybe show desc too?
    var hack by remember { mutableStateOf(false) }
    var excludeDateShown by remember(hack) { mutableStateOf(false) }
    val dateMeta = occurrence.date

    Column(
        Modifier.padding(2.dp).fillMaxWidth().defaultMinSize(minHeight = 64.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ).combinedClickable(onLongClick = { excludeDateShown = true }, onClick = openEntryInfo)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
            Box(
                Modifier.padding(end = 8.dp).size(24.dp).background(dateMeta.color, CircleShape)
                    .border(0.dp, dateMeta.color, CircleShape)
                    .align(Alignment.CenterVertically)
            )

            Text(
                modifier = Modifier.weight(1.0f).padding(horizontal = 8.dp).align(Alignment.CenterVertically),
                text = dateMeta.displayName,
                style = MaterialTheme.typography.headlineSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = occurrence.occurrence
                    .format(DateTimeFormatter.ofPattern("HH:mm")), //fixme should show end time
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        EntryDescriptionAndTags(
            dbManager,
            dateMeta.getContents(dbManager),
            Tag.forEntry(dbManager, dateMeta.entryId!!)
        )

        if (excludeDateShown)
            ExcludeOccurrenceDialog(occurrence.occurrence, dateMeta.displayName, {
                dateMeta.addExceptions(occurrence.occurrenceZoned.toLocalDate())
                dateMeta.update(dbManager)
                dbManager.tryScheduleNotification()

                hack = true
                excludeDateShown = false
            }) { excludeDateShown = false }
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
        ColorPicker(color, allowCustomColor) {
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
                    RemovableTagChip(tag.name, tag.colorOrDefault(dbManager), {
                        tagsFilterUpdate(tagsFilter - tag)
                    }) { isFilterOpen = true }
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
fun Tag(text: String, color: Color) {
    Text(
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.5.dp).background(
            color, shape = RoundedCornerShape(10.dp)
        ).padding(vertical = 1.dp, horizontal = 6.dp),
        text = text,
        maxLines = 1,
        style = MaterialTheme.typography.bodyMedium,
        color = bgColorToTextColor(color)
    )
}

@Composable
fun TagChip(name: String, color: Color) {
    InputChip(
        false,
        {},
        {
            Text(
                name, style = MaterialTheme.typography.bodyLarge,
                color = bgColorToTextColor(color)
            )
        },
        modifier = Modifier.padding(horizontal = 4.dp),
        colors = InputChipDefaults.inputChipColors(containerColor = color),
    )
}

@Composable
fun RemovableTagChip(name: String, color: Color, onRemove: () -> Unit, onClick: () -> Unit = onRemove) {
    InputChip(
        false,
        onClick,
        {
            Text(
                name, style = MaterialTheme.typography.bodyMedium,
                color = bgColorToTextColor(color)
            )
        },
        modifier = Modifier.padding(4.dp).height(30.dp),
        trailingIcon = {
            IconButton(onClick = onRemove, Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Clear, null,
                    tint = bgColorToTextColor(color)
                )
            }
        },
        colors = InputChipDefaults.inputChipColors(containerColor = color),
    )
}

@Composable
fun SelectableTagChip(name: String, color: Color, selected: Boolean, onSelect: (Boolean) -> Unit) {
    var selected by remember(name) { mutableStateOf(selected) }

    InputChip(
        selected,
        {
            selected = !selected
            onSelect(selected)
        },
        {
            Text(
                name, style = MaterialTheme.typography.bodyLarge,
                color = bgColorToTextColor(color)
            )
        },
        modifier = Modifier.padding(horizontal = 4.dp),
        trailingIcon = {
            if (selected) Icon(
                Icons.Filled.Done, null,
                tint = bgColorToTextColor(color)
            )
        },
        colors = InputChipDefaults.inputChipColors(containerColor = color, selectedContainerColor = color),
    )
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
fun ScheduleDialog(dbManager: DbManager, entry: ViewEntryWithOptions, onClose: () -> Unit, onSchedule: () -> Unit) {
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
                        it.atZone(defaultZoneId()),
                        Once(),
                        1,
                        Once(),
                        entry,
                    ).create(dbManager)
                    dbManager.tryScheduleNotification()
                    onSchedule()
                })
            }) {
                Text("OK")
            }
        }, text = {
            Column {
                Text("Schedule ${entry.displayName} event?")
            }
        })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryDescriptionAndTags(dbManager: DbManager, contents: String, tags: List<Tag>) {
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
                Tag(tag.name, tag.colorOrDefault(dbManager))
            }
        }
    }
}

@Composable
fun BottomBar(navController: NavController) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        NavRoutes.getTopLevelRoutes().forEach { topLevelRoute ->
            NavigationBarItem(
                icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name) },
                label = { Text(topLevelRoute.name) },
                selected = currentDestination?.route == topLevelRoute.route.route ||
                        (topLevelRoute.route.route == NavRoutes.Calendar.route && currentDestination?.route == NavRoutes.ThatDayDetails.route),
                onClick = {
                    navController.navigate(topLevelRoute.route.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            if (currentDestination?.route != NavRoutes.ThatDayDetails.route ||
                                topLevelRoute.route.route != NavRoutes.Calendar.route
                            )
                                saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun TagChipPreview() {
    TagChip("tag 0", Color.Green)
}

@Preview
@Composable
fun TagPreview() {
    Tag("tag 1", Color.Red)
}

@Preview
@Composable
fun RemovableTagPreview() {
    RemovableTagChip("tag 2", Color.Yellow, {}) {}
}

@Preview
@Composable
fun SelectableTagPreview() {
    var chosen by remember { mutableStateOf(true) }
    SelectableTagChip("tag 3", Color.Blue, chosen, { chosen = !chosen })
}