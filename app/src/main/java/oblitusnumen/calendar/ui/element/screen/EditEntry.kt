package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.Period.*
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.ui.element.*
import oblitusnumen.calendar.ui.state.DateState
import oblitusnumen.calendar.ui.theme.topBarColors
import oblitusnumen.calendar.ui.viewmodel.EntryEditViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EditEntryScreen(
    dbManager: DbManager,
    viewModel: EntryEditViewModel = viewModel(),
    pendingDate: LocalDate?,
    backPress: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val periodSelectorDate = remember { mutableStateOf<DateState?>(null) }
    val dateTimePicker = remember { DateTimePicker() }

    remember {
        if (pendingDate != null)
            dateTimePicker.timePick(
                {},
                {
                    periodSelectorDate.value = viewModel.addDate(
                        Date(
                            ZonedDateTime.of(it.atDate(pendingDate), ZoneId.systemDefault()),
                            Once(),
                            1,
                            Once()
                        )
                    )
                })
    }
    dateTimePicker.tryCompose()

    Scaffold(topBar = {
        EditEntryTopBar(
            {
                backPress()
                viewModel.commitToDb(dbManager)
            },
            backPress
        )
    }) { paddingValues ->
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
            Spacer(Modifier.height(paddingValues.calculateTopPadding()))

            // name
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                value = state.name, onValueChange = {
                    if (!it.text.contains('\n'))
                        viewModel.setName(it)
                },
                textStyle = MaterialTheme.typography.titleLarge,
                label = { Text("Enter event name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                trailingIcon = {
                    ColorSelectButton(state.color, true) {
                        viewModel.setColor(it)
                    }
                }
            )

            // description
            OutlinedTextField(
                modifier = Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                value = state.contents, onValueChange = {
                    viewModel.setContents(it)
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                label = { Text("Enter description") },
                minLines = 5
            )
//            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
//
//            // hide from calendar view
//            CheckboxOption(hideInCalendarView, "Hide from calendar view")

            // tags
            var tagChoose by remember { mutableStateOf(false) }
            if (tagChoose) TagChooseMenu(dbManager, state.tags, { tagChoose = false }, { viewModel.setTags(it) })

            Row {
                Icon(Icons.Filled.Star, "Tags", Modifier.padding(8.dp))
                FlowRow(
                    Modifier.fillMaxWidth().padding(end = 16.dp)
                ) {
                    for (tag in state.tags)
                        RemovableTagChip(
                            tag.name,
                            tag.colorOrDefault(dbManager),
                            { viewModel.setTags(state.tags - tag) }
                        )
                }
            }

            Box(Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                tagChoose = true
            }) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(horizontal = 44.dp, vertical = 16.dp),
                    text = "Choose tags...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // dates
            for (date in state.dateStates)
                key(date.uiId) {
                    // FIXME:  toDbEntity() is a hack; do the fix in other places
                    Date(date, periodSelectorDate, dateTimePicker, { viewModel.rmDate(date.uiId) }) {
                        viewModel.updateDate(date.uiId, it)
                    }
                }
            Box(
                Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth()/*.padding(top = 8.dp)*/.clickable {
                    dateTimePicker.dateTimePick(
                        {},
                        {
                            periodSelectorDate.value =
                                viewModel.addDate(
                                    Date(
                                        ZonedDateTime.of(it, ZoneId.systemDefault()),
                                        Once(),
                                        1,
                                        Once()
                                    )
                                )
                        })
                }) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(horizontal = 44.dp, vertical = 16.dp),
                    text = "Add date...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // notifications
            var notificationChoose by remember { mutableStateOf(false) }
            if (notificationChoose) NotificationAddMenu({ offset, sound ->
                notificationChoose = false
                for (notification in state.notificationStates) {
                    if (notification.offset.toString() == offset.toString()) {
                        viewModel.setNotificationSound(notification.uiId, sound)
                        return@NotificationAddMenu
                    }
                }
                viewModel.addNotification(offset, sound)
            }, { notificationChoose = false })
            var updated by remember { mutableStateOf(false) }
            updated// fixme this is hack...
            for (notification in state.notificationStates) key(notification.uiId) {
                Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                    viewModel.setNotificationSound(notification.uiId, !notification.sound)
                }) {
                    Icon(
                        if (notification.sound) Icons.Filled.Notifications else Icons.Outlined.Notifications, null,
                        Modifier.align(Alignment.CenterVertically).padding(8.dp)
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                            .weight(1f),
                        text = "${notification.offset.count} ${notification.offset.name} before",// FIXME: text
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            viewModel.rmNotification(notification.uiId)
                        },
                        content = { Icon(Icons.Filled.Clear, contentDescription = null) })
                }
            }
            Box(Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth()/*.padding(top = 8.dp)*/.clickable {
                notificationChoose = true
            }) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(horizontal = 44.dp, vertical = 4.dp),
                    text = "Add notification",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChooseMenu(dbManager: DbManager, tags: List<Tag>, onClose: () -> Unit, onTagsChange: (List<Tag>) -> Unit) {
    val allTags = Tag.all(dbManager).groupingBy { it.name }.reduce { _, accumulator, _ -> accumulator }
    val chosenTags: MutableSet<String> = tags.map { it.name }.toMutableSet()
    var searchTag by remember { mutableStateOf("") }

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
                onTagsChange(chosenTags.map { allTags[it] ?: Tag(it) })
            }) {
                Text("OK")
            }
        },
        text = {
            Column {
                OutlinedTextField(// FIXME: ui paddings
                    modifier = Modifier.padding(horizontal = 8.dp),
                    value = searchTag, onValueChange = {
                        searchTag = it
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    label = { Text("Tag name") }
                )
                FlowRow(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                ) {
                    searchTag // FIXME: yet another filthy hack
                    if (searchTag.isNotEmpty() && !chosenTags.contains(searchTag)) {
                        SelectableTagChip(
                            searchTag,
                            dbManager.defaultTagColor,
                            false
                        ) { if (it) chosenTags += searchTag else chosenTags -= searchTag }
                    }
                    for (tag in chosenTags) {
                        SelectableTagChip(
                            tag,
                            allTags[tag]!!.colorOrDefault(dbManager),
                            true
                        ) { if (it) chosenTags += tag else chosenTags -= tag }
                    }
                    for (tag in allTags.values) {
                        if (!chosenTags.contains(tag.name) && tag.name.contains(
                                searchTag,
                                true
                            ) && tag.name != searchTag
                        ) SelectableTagChip(
                            tag.name,
                            tag.colorOrDefault(dbManager),
                            false
                        ) { if (it) chosenTags += tag.name else chosenTags -= tag.name }
                    }
                }
            }
        }
    )
}

@Composable
fun Date(
    date: DateState,
    periodSelectorDate: MutableState<DateState?>,
    dateTimePicker: DateTimePicker,
    onRmDate: () -> Unit,
    onUpdateDate: (DateState) -> Unit,
) {
    var periodSelectorDate by remember { periodSelectorDate }
    if (periodSelectorDate == date)
        PeriodSelectorDialog(date, { periodSelectorDate = null }, { periodSelectorDate = null }, onUpdateDate)
    val dateStartText = (if (date.isPeriodic) "from " else "") +
            date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy "))
    val timeText = date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("HH:mm"))
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

            // pick date
            val localDateTime = date.getFirstZoneDateTime().toLocalDateTime()

            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                    .weight(.5f).clickable {
                        dateTimePicker.datePick({}, {
                            onUpdateDate(
                                date.setRange(
                                    startOfDayStart = ZonedDateTime.of(
                                        it.atTime(localDateTime.toLocalTime()),
                                        date.timeZoneId
                                    )
                                )
                            )
                        }, localDateTime.toLocalDate())
                    },
                text = dateStartText,
                style = MaterialTheme.typography.bodyLarge
            )

            // pick time
            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                    .weight(.5f).clickable {
                        dateTimePicker.timePick({}, {
                            onUpdateDate(
                                date.setRange(
                                    startOfDayStart = ZonedDateTime.of(
                                        it.atDate(localDateTime.toLocalDate()),
                                        date.timeZoneId
                                    )
                                )
                            )
                        }, localDateTime.toLocalTime())
                    },
                text = timeText,
                style = MaterialTheme.typography.bodyLarge
            )

            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = { onRmDate() },
                content = { Icon(Icons.Filled.Clear, contentDescription = null) })
        }

        // choose period
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
            periodSelectorDate = date
        }.padding(horizontal = 40.dp)) {
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

                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            onUpdateDate(date.removeExceptions(LocalDate.ofEpochDay(epochDay)))
                        },
                        content = { Icon(Icons.Filled.Clear, contentDescription = null) }
                    )
                }
            }

            Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                dateTimePicker.datePick({}, {
                    onUpdateDate(date.addExceptions(it))
                })
            }.padding(horizontal = 40.dp)) {
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                        .weight(1f),
                    text = "Add exception...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun WeekdayButton(active: MutableState<Boolean>, text: String) {
    OutlinedButton(
        onClick = { active.value = !active.value },
        modifier = Modifier.padding(4.dp).size(28.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active.value) MaterialTheme.colorScheme.primaryContainer else
                Color.Transparent
        )
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PeriodSelectorDialog(
    date: DateState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onUpdateDate: (DateState) -> Unit,
) {
    val initialPeriodCount = if (date.period is Once) 1 else date.period.count
    val initialPeriodType = PeriodType(date.period)
    var selectedPeriodCount: Long by remember { mutableStateOf(initialPeriodCount) }
    var selectedPeriodType by remember { mutableStateOf(initialPeriodType) }

    var selectedMillis by remember {
        mutableStateOf(
            (if (date.isEndless) date.getFirstZoneDateTime() else date.getLastZoneDateTime()).toLocalDate()
                .toEpochDay() * 86_400_000
        )
    }
    var occurrencesCount by remember { mutableStateOf(if (date.isEndless) "1" else "${date.getTimesRepeatUI()}") }
    var endVariantSelectedOption by remember {
        mutableStateOf(
            if (date.isEndless) DateSequenceEndVariant.ENDLESS
            else DateSequenceEndVariant.BY_DATE
        )
    }

    val datePeriod = date.period
    val dayOfWeek = date.getFirstZoneDateTime().dayOfWeek.value
    val monSelected =
        remember { mutableStateOf(if (datePeriod is Weekday) datePeriod.testWeekday(Weekday.WD_MON) else dayOfWeek == 1) }
    val tueSelected =
        remember { mutableStateOf(if (datePeriod is Weekday) datePeriod.testWeekday(Weekday.WD_TUE) else dayOfWeek == 2) }
    val wedSelected =
        remember { mutableStateOf(if (datePeriod is Weekday) datePeriod.testWeekday(Weekday.WD_WED) else dayOfWeek == 3) }
    val thuSelected =
        remember { mutableStateOf(if (datePeriod is Weekday) datePeriod.testWeekday(Weekday.WD_THU) else dayOfWeek == 4) }
    val friSelected =
        remember { mutableStateOf(if (datePeriod is Weekday) datePeriod.testWeekday(Weekday.WD_FRI) else dayOfWeek == 5) }
    val satSelected =
        remember { mutableStateOf(if (datePeriod is Weekday) datePeriod.testWeekday(Weekday.WD_SAT) else dayOfWeek == 6) }
    val sunSelected =
        remember { mutableStateOf(if (datePeriod is Weekday) datePeriod.testWeekday(Weekday.WD_SUN) else dayOfWeek == 7) }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val count = if (selectedPeriodCount <= 0)
                    if (selectedPeriodType.period is Once)
                        0
                    else {// TODO:
//                    showErrorToast("Could not parse count: '$periodCountText'")
                        return@TextButton
                    }
                else
                    selectedPeriodCount

                onUpdateDate(
                    date.setPeriod(
                        if (selectedPeriodType.period is Weekday) {
                            val weekdayDays = (if (monSelected.value) Weekday.WD_MON else 0) +
                                    (if (tueSelected.value) Weekday.WD_TUE else 0) +
                                    (if (wedSelected.value) Weekday.WD_WED else 0) +
                                    (if (thuSelected.value) Weekday.WD_THU else 0) +
                                    (if (friSelected.value) Weekday.WD_FRI else 0) +
                                    (if (satSelected.value) Weekday.WD_SAT else 0) +
                                    (if (sunSelected.value) Weekday.WD_SUN else 0)
                            Weekday(
                                count,
                                if (weekdayDays == 0L)
                                    Weekday.dayOfWeekIndexToEnum(date.getFirstZoneDateTime().dayOfWeek.value)
                                else
                                    weekdayDays
                            )
                        } else
                            selectedPeriodType.period.updateCount(count)
                    )
                )

                if (date.isPeriodic) {
                    onUpdateDate(
                        when (endVariantSelectedOption) {
                            DateSequenceEndVariant.ENDLESS -> date.makeEndless()
                            DateSequenceEndVariant.BY_DATE -> date.setRange(
                                startOfDayEnd = ZonedDateTime.of(
                                    LocalDate.ofEpochDay(selectedMillis / 86400000).atStartOfDay(),
                                    date.getFirstZoneDateTime().zone
                                )
                            )

                            DateSequenceEndVariant.OCCURRENCES -> {
                                date.setTimesRepeatUI(if (occurrencesCount.isEmpty()) 1 else occurrencesCount.toLong())
                            }
                        }
                    )
                }

                onConfirm()
            }) {
                Text("OK")
            }
        },
        text = {
            Column {
                PeriodSelector(
                    initialPeriodType,
                    initialPeriodCount,
                    { selectedPeriodType = it },
                    { selectedPeriodCount = it }
                )

                if (selectedPeriodType.period is Weekday) {
                    Row {
                        WeekdayButton(monSelected, "Mon")
                        WeekdayButton(tueSelected, "Tue")
                        WeekdayButton(wedSelected, "Wed")
                        WeekdayButton(thuSelected, "Thu")
                        WeekdayButton(friSelected, "Fri")
                        WeekdayButton(satSelected, "Sat")
                        WeekdayButton(sunSelected, "Sun")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(top = 8.dp)
                )

                //end variant selection
                //endless radiobutton
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (selectedPeriodType.period !is Once) endVariantSelectedOption =
                            DateSequenceEndVariant.ENDLESS
                    }
                ) {
                    RadioButton(
                        selected = (endVariantSelectedOption == DateSequenceEndVariant.ENDLESS),
                        onClick = { endVariantSelectedOption = DateSequenceEndVariant.ENDLESS },
                        Modifier.align(Alignment.CenterVertically),
                        enabled = selectedPeriodType.period !is Once
                    )
                    Text(
                        "Endless",
                        Modifier.padding(vertical = 4.dp, horizontal = 16.dp).align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedPeriodType.period !is Once) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = .4F
                        )
                    )
                }

                //end date radiobutton
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (selectedPeriodType.period !is Once) endVariantSelectedOption =
                            DateSequenceEndVariant.BY_DATE
                    }
                ) {
                    RadioButton(
                        selected = (endVariantSelectedOption == DateSequenceEndVariant.BY_DATE),
                        onClick = { endVariantSelectedOption = DateSequenceEndVariant.BY_DATE },
                        Modifier.align(Alignment.CenterVertically),
                        enabled = selectedPeriodType.period !is Once
                    )
                    DateTimePicker.datePickerField(
                        selectedMillis, "End date",
                        selectedPeriodType.period !is Once
                    ) {
                        endVariantSelectedOption = DateSequenceEndVariant.BY_DATE
                        selectedMillis = it
                    }
                }

                //occurrences radiobutton
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (selectedPeriodType.period !is Once) endVariantSelectedOption =
                            DateSequenceEndVariant.OCCURRENCES
                    }
                ) {
                    RadioButton(
                        selected = (endVariantSelectedOption == DateSequenceEndVariant.OCCURRENCES),
                        onClick = { endVariantSelectedOption = DateSequenceEndVariant.OCCURRENCES },
                        Modifier.align(Alignment.CenterVertically),
                        enabled = selectedPeriodType.period !is Once
                    )

                    OutlinedTextField(
                        enabled = selectedPeriodType.period !is Once,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        value = occurrencesCount,
                        onValueChange = {
                            endVariantSelectedOption = DateSequenceEndVariant.OCCURRENCES
                            try {
                                if (it.toLong() > 0 && it.length <= 5)
                                    occurrencesCount = it
                            } catch (_: NumberFormatException) {
                                if (it.isEmpty())
                                    occurrencesCount = it
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        label = { Text("Occurrences") }
                    )
                }
            }
        }
    )
}

@Composable
fun CheckboxOption(checked: MutableState<Boolean>, label: String) {
    Row(
        Modifier.fillMaxWidth().padding(8.dp).defaultMinSize(minHeight = 52.dp)
            .clickable {
                checked.value = !checked.value
            },
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            checked = checked.value,
            onCheckedChange = { checked.value = it },
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        Text(label, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryTopBar(
    onDone: () -> Unit,
    backPress: () -> Unit
) {// TODO: confirm
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = { BackPressButton(backPress) },
        title = { Text("Edit event", maxLines = 1) },
        actions = { EditDoneButton(onDone) },
    )
}

enum class DateSequenceEndVariant {
    ENDLESS,
    BY_DATE,
    OCCURRENCES
}

data class OffsetType(val period: Period) {
    override fun toString(): String {
        return when (period) {
            is Once -> "at time"
            is Minute -> "minute"
            is Hour -> "hour"
            is Day -> "day"
            is Week -> "week"
            is Month -> "month"
            is Year -> "year"
            is Weekday -> "week"
        }
    }

    companion object {
        fun getAll(): List<OffsetType> {
            val list = mutableListOf<OffsetType>()
            list.add(OffsetType(Once()))
            list.add(OffsetType(Minute(1)))
            list.add(OffsetType(Hour(1)))
            list.add(OffsetType(Day(1)))
            list.add(OffsetType(Week(1)))
            list.add(OffsetType(Month(1)))
            return list
        }
    }
}

data class PeriodType(val period: Period) {
    override fun toString(): String {
        return when (period) {
            is Once -> "ounce"
            is Minute -> "minute"
            is Hour -> "hour"
            is Day -> "day"
            is Week -> "week"
            is Month -> "month"
            is Year -> "year"
            is Weekday -> "week"
        }
    }

    companion object {
        fun getAll(): List<PeriodType> {
            val list = mutableListOf<PeriodType>()
            list.add(PeriodType(Once()))
            list.add(PeriodType(Day(1)))
            list.add(PeriodType(Weekday(1, Weekday.WD_NONE)))
            list.add(PeriodType(Month(1)))
            list.add(PeriodType(Year(1)))
            return list
        }
    }
}
