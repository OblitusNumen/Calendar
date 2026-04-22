package oblitusnumen.calendar.ui.element

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DateOccurrence
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.Period.Hour
import oblitusnumen.calendar.implementation.data.Period.Minute
import oblitusnumen.calendar.implementation.data.Period.Once
import oblitusnumen.calendar.ui.formatDateTime
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.implementation.data.views.ViewEntryWithOptions
import oblitusnumen.calendar.implementation.data.views.ViewTaskWithOptions
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
import java.util.TimeZone

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
                        contentDescription = stringResource(R.string.cd_filter),
                        Modifier.size(40.dp),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
                    )
                } else {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.cd_filter),
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
                Text(stringResource(R.string.common_cancel))
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
                Text(stringResource(R.string.common_ok))
            }
        },
        text = {
            Column {
                OffsetSelector(OffsetType(Once()), initialCount, { selectedOffsetType = it }, { offsetCount = it })

                Row {
                    Text(stringResource(R.string.shared_silent))
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
            label = { Text(stringResource(R.string.shared_count_label)) }
        )

        val context = LocalContext.current
        materialSpinner(
            stringResource(R.string.shared_type_label), PeriodType.getAll(),
            onSelectPeriodType,
            selectedPeriodType,
            Modifier.padding(horizontal = 8.dp).width(150.dp),
            labelFor = { it.displayName(context) }
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
            label = { Text(stringResource(R.string.shared_count_label)) }
        )

        val context = LocalContext.current
        materialSpinner(
            stringResource(R.string.shared_type_label), OffsetType.getAll(),
            onSelectOffsetType,
            selectedOffsetType,
            Modifier.padding(horizontal = 8.dp).width(150.dp),
            labelFor = { it.displayName(context) }
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

@Composable
fun IntTextField(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.width(100.dp).padding(horizontal = 8.dp),
    label: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    maxDigits: Int = 3
) {
    var textState by remember { mutableStateOf(TextFieldValue(value?.let { "$value" } ?: "")) }

    OutlinedTextField(
        enabled = enabled,
        modifier = modifier,
        value = textState,
        onValueChange = {
            val text = it.text
            try {
                if (text.toInt() >= 0 && text.length <= maxDigits) {
                    textState = it
                    onValueChange(text.toInt())
                }
            } catch (_: NumberFormatException) {
                if (text.isEmpty()) {
                    textState = it
                    onValueChange(null)
                }
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        label = label,
        trailingIcon = trailingIcon,
        maxLines = 1
    )
}

/**
 * A reusable Jetpack Compose element for selecting a time zone.
 *
 * FINAL FIX: The TextField was consuming all click events internally
 * (this is normal behavior of OutlinedTextField even when readOnly = true).
 * Previous Box + clickable approaches only worked on the edges because
 * pointer events are consumed by the child before they reach the parent.
 *
 * Official Material 3 solution (used by DatePicker, TimePicker, etc.):
 * → Use ExposedDropdownMenuBox + .menuAnchor(MenuAnchorType.PrimaryNotEditable)
 * This gives the TextField perfect native click handling, ripple on the
 * entire surface, correct semantics, and no gesture conflicts.
 *
 * The dialog is still shown separately — the ExposedDropdownMenuBox is
 * ONLY used as the trigger (no actual dropdown menu is rendered).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeZoneSelector(
    selectedTimeZoneId: String,
    onTimeZoneSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit = { Text(stringResource(R.string.timezone_label)) }
) {
    val selectedTimeZone = remember(selectedTimeZoneId) {
        TimeZone.getTimeZone(selectedTimeZoneId.ifBlank { TimeZone.getDefault().id })
    }

    val displayText = remember(selectedTimeZone) {
        val offset = selectedTimeZone.rawOffset
        val hours = offset / (1000 * 60 * 60)
        val minutes = (offset % (1000 * 60 * 60)) / (1000 * 60)
        val offsetStr = if (minutes == 0) {
            "GMT%+d".format(hours)
        } else {
            "GMT%+d:%02d".format(hours, minutes)
        }
        "${selectedTimeZone.id} ($offsetStr)"
    }

    var showDialog by remember { mutableStateOf(false) }

    // ─────────────────────────────────────────────────────────────
    // This is the key component that makes the whole field clickable
    // ─────────────────────────────────────────────────────────────
    ExposedDropdownMenuBox(
        expanded = showDialog,
        onExpandedChange = { showDialog = it },   // handles click perfectly
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {}, // read-only
            readOnly = true,
            label = label,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDialog)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)   // ← THIS IS THE MAGIC
        )
    }

    if (showDialog) {
        TimeZoneSelectionDialog(
            selectedTimeZoneId = selectedTimeZoneId,
            onTimeZoneSelected = onTimeZoneSelected,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Internal dialog – unchanged (search + lazy list works perfectly).
 */
@Composable
private fun TimeZoneSelectionDialog(
    selectedTimeZoneId: String,
    onTimeZoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allTimeZones = remember {
        TimeZone.getAvailableIDs()
            .map { TimeZone.getTimeZone(it) }
            .sortedBy { it.id }
    }

    val filteredTimeZones = remember(searchQuery, allTimeZones) {
        if (searchQuery.isBlank()) {
            allTimeZones
        } else {
            allTimeZones.filter { it.id.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.timezone_select_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.timezone_search_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(
                        items = filteredTimeZones,
                        key = { it.id }
                    ) { timeZone ->
                        val isSelected = timeZone.id == selectedTimeZoneId

                        val offsetMillis = timeZone.rawOffset
                        val hours = offsetMillis / (1000 * 60 * 60)
                        val minutes = (offsetMillis % (1000 * 60 * 60)) / (1000 * 60)
                        val offsetStr = if (minutes == 0) {
                            "GMT%+d".format(hours)
                        } else {
                            "GMT%+d:%02d".format(hours, minutes)
                        }
                        val displayText = "${timeZone.id} ($offsetStr)"

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = displayText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    onTimeZoneSelected(timeZone.id)
                                    onDismiss()
                                }
                        )
                    }

                    if (filteredTimeZones.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.timezone_none_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
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
    val context = LocalContext.current
    var nextDateText by remember {
        val nextDateText =
            when (val nextDate = entryView.nextDate) {
                null -> ""
                -1L -> context.getString(R.string.shared_ended)
                else -> formatDateTime(context, nextDate, defaultZoneId())
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
                text = run {
                    val start = occurrence.occurrence.format(DateTimeFormatter.ofPattern("HH:mm"))
                    when (val dur = occurrence.date.duration) {
                        is Minute, is Hour -> {
                            val end = dur.addTo(occurrence.occurrenceZoned, 1)
                            "$start–${end.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                        }
                        else -> start
                    }
                },
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
        Icon(Icons.Filled.Add, stringResource(R.string.cd_add_event))
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
            Icons.Filled.FilterList,
            contentDescription = stringResource(R.string.cd_filter),
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
fun Link(task: ViewTaskWithOptions, isValid: Boolean, onRemove: () -> Unit) {
    Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
        val color = remember { task.color }
        Box(
            Modifier.padding(8.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
                .size(24.dp).align(Alignment.CenterVertically)
        )

        Text(
            task.displayName,
            Modifier.align(Alignment.CenterVertically).padding(4.dp).weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = if (isValid) MaterialTheme.colorScheme.onBackground else Color.Red,
        )

        if (task.isDone) {
            Icon(
                Icons.Filled.Done,
                contentDescription = stringResource(R.string.cd_done),
                Modifier.align(Alignment.CenterVertically),
                tint = Color.Green
            )
        } else {
            Text(
                text = "${
                    task.timeRemaining
                    // FIXME:
//                    task.countPredecessorsTimeEstimate(
//                        allTasks.associateBy { it.taskId!! },
//                        predecessorLinks
//                    )
                }",
                modifier = Modifier.padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.cd_remove))
        }
    }
}

@Composable
fun ChooseTasksDialog(
    dbManager: DbManager,
    title: String,
    tasksToChoose: List<Int>,
    onClose: () -> Unit,
    onChoose: (Set<Int>) -> Unit
) {
    var chosenTasks by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        title = { Text(title) },
        onDismissRequest = onClose,
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.common_cancel)) } },
        confirmButton = { TextButton(onClick = { onChoose(chosenTasks) }) { Text(stringResource(R.string.common_ok)) } },
        text = {
            LazyColumn {
                items(tasksToChoose, key = { "choose$it" }) { task ->
                    SelectableTask(ViewTaskWithOptions.byId(dbManager, task)!!, chosenTasks.contains(task)) {
                        if (chosenTasks.contains(task)) {
                            chosenTasks -= task
                        } else {
                            chosenTasks += task
                        }
                    }
                }
            }
        })
}

@Composable
fun SelectableTask(task: ViewTaskWithOptions, checked: Boolean, onClick: () -> Unit) {
    Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp).clickable(onClick = onClick)) {
        if (checked) {
            Checkbox(checked = true, onCheckedChange = {}, modifier = Modifier.align(Alignment.CenterVertically))
        }

        val color = remember { task.color }
        Box(
            Modifier.padding(8.dp).background(color, CircleShape).border(0.dp, color, CircleShape)
                .size(24.dp).align(Alignment.CenterVertically)
        )

        Text(
            task.displayName,
            Modifier.align(Alignment.CenterVertically).padding(4.dp).weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )

        if (task.isDone) {
            Icon(
                Icons.Filled.Done,
                contentDescription = stringResource(R.string.cd_done),
                Modifier.align(Alignment.CenterVertically),
                tint = Color.Green
            )
        } else {
            Text(
                text = "${
                    task.timeRemaining
                    // FIXME:
//                    task.countPredecessorsTimeEstimate(
//                        allTasks.associateBy { it.taskId!! },
//                        predecessorLinks
//                    )
                }",
                modifier = Modifier.padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
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
            Icon(Icons.Filled.Add, stringResource(R.string.cd_add_event))
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
                Text(stringResource(R.string.common_cancel))
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
                Text(stringResource(R.string.common_ok))
            }
        }, text = {
            Column {
                Text(stringResource(R.string.shared_schedule_event, entry.displayName))
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
                icon = { Icon(topLevelRoute.icon, contentDescription = stringResource(topLevelRoute.nameRes)) },
                label = { Text(stringResource(topLevelRoute.nameRes)) },
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