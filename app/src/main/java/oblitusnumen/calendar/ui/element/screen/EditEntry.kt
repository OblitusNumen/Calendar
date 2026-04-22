package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import oblitusnumen.calendar.implementation.MILLIS_PER_DAY
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import oblitusnumen.calendar.R
import oblitusnumen.calendar.ui.displayCount
import oblitusnumen.calendar.ui.displayOffsetBefore
import oblitusnumen.calendar.ui.displayOffsetUnitName
import oblitusnumen.calendar.ui.displayUnitName
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.Period.*
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.ui.element.*
import oblitusnumen.calendar.ui.state.DateState
import oblitusnumen.calendar.ui.viewmodel.EntryEditViewModel
import java.time.LocalDate
import java.time.LocalTime
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
    val durationSelectorDate = remember { mutableStateOf<DateState?>(null) }
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
        null
    }
    dateTimePicker.tryCompose()

    Scaffold(topBar = {
        EditTopBar(
            stringResource(R.string.edit_entry_title),
            {
                backPress()
                viewModel.commitToDb(dbManager)
            },
            backPress
        )
    }) { paddingValues ->
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
            Spacer(Modifier.height(paddingValues.calculateTopPadding()))

            // name and color
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                value = state.name, onValueChange = {
                    if (!it.text.contains('\n'))
                        viewModel.setName(it)
                },
                textStyle = MaterialTheme.typography.titleLarge,
                label = { Text(stringResource(R.string.edit_entry_name_hint)) },
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
                label = { Text(stringResource(R.string.edit_entry_description_hint)) },
                minLines = 5
            )
//            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
//
//            // hide from calendar view
//            CheckboxOption(hideInCalendarView, "Hide from calendar view")

            // tags
            var tagChoose by remember { mutableStateOf(false) }
            if (tagChoose)
                TagChooseMenu(dbManager, state.tags, { tagChoose = false }, { viewModel.setTags(it) })

            // draw tags
            Row {
                Icon(Icons.Filled.Star, stringResource(R.string.cd_tags), Modifier.padding(8.dp))

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

            // choose tags
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).clickable { tagChoose = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Star, null, Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.edit_entry_choose_tags), style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // dates
            for (date in state.dateStates)
                key(date.uiId) {
                    // FIXME:  toDbEntity() is a hack; do the fix in other places
                    Date(
                        date,
                        periodSelectorDate,
                        durationSelectorDate,
                        dateTimePicker,
                        { viewModel.rmDate(date.uiId) }) {
                        viewModel.updateDate(date.uiId, it)
                    }
                }

            // add date
            Row(
                Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().clickable {
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
                }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Schedule, null, Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.edit_entry_add_date), style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // notifications
            var notificationChoose by remember { mutableStateOf(false) }
            if (notificationChoose)
                NotificationAddMenu({ offset, sound ->
                    notificationChoose = false
                    for (notification in state.notificationStates) {
                        if (notification.offset.toString() == offset.toString()) {
                            viewModel.setNotificationSound(notification.uiId, sound)
                            return@NotificationAddMenu
                        }
                    }
                    viewModel.addNotification(offset, sound)
                }, { notificationChoose = false })

            // draw notifications
            for (notification in state.notificationStates) key(notification.uiId) {
                Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                    viewModel.setNotificationSound(notification.uiId, !notification.sound)
                }) {
                    // sound
                    Icon(
                        if (notification.sound) Icons.Filled.Notifications else Icons.Outlined.Notifications, null,
                        Modifier.align(Alignment.CenterVertically).padding(8.dp)
                    )

                    // offset
                    val ctx = LocalContext.current
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                            .weight(1f),
                        text = notification.offset.displayOffsetBefore(ctx),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // remove button
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            viewModel.rmNotification(notification.uiId)
                        },
                        content = { Icon(Icons.Filled.Clear, contentDescription = null) })
                }
            }

            // add notifications
            Row(
                Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().clickable {
                    notificationChoose = true
                }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.edit_entry_add_notification), style = MaterialTheme.typography.bodyLarge)
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
                Text(stringResource(R.string.common_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClose()
                onTagsChange(chosenTags.map { allTags[it] ?: Tag(it) })
            }) {
                Text(stringResource(R.string.common_ok))
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
                    label = { Text(stringResource(R.string.tags_name_label)) }
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
    durationSelectorDate: MutableState<DateState?>,
    dateTimePicker: DateTimePicker,
    onRmDate: () -> Unit,
    onUpdateDate: (DateState) -> Unit,
) {
    val context = LocalContext.current
    var periodSelectorDate by remember { periodSelectorDate }
    if (periodSelectorDate == date)
        PeriodSelectorDialog(date, {
            onUpdateDate(it)
            periodSelectorDate = null
        }, { periodSelectorDate = null })

    var durationSelectorDate by remember { durationSelectorDate }
    if (durationSelectorDate == date)
        DurationSelectorDialog(date, dateTimePicker, {
            durationSelectorDate = null
            onUpdateDate(date.setDuration(it))
        }, { durationSelectorDate = null })

    val dateFormatted = date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy "))
    val dateStartText =
        if (date.isPeriodic) stringResource(R.string.edit_entry_period_from, dateFormatted)
        else dateFormatted
    val timeText = date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val textPeriod: String = if (date.isPeriodic) {
        val every = stringResource(
            R.string.edit_entry_period_every,
            date.period.displayCount(context)
        )
        if (date.isEndless) every
        else every + " " + stringResource(
            R.string.edit_entry_period_until,
            date.getLastZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        )
    } else
        PeriodType(date.period).displayName(context)

    val textDuration: String = when (val dur = date.duration) {
        is Once -> stringResource(R.string.edit_entry_duration_no_end_time)
        is Minute -> {
            val end = date.getFirstZoneDateTime().toLocalTime().plusMinutes(dur.count)
            stringResource(R.string.edit_entry_duration_ends, end.format(DateTimeFormatter.ofPattern("HH:mm")))
        }
        is Hour -> {
            val end = date.getFirstZoneDateTime().toLocalTime().plusHours(dur.count)
            stringResource(R.string.edit_entry_duration_ends, end.format(DateTimeFormatter.ofPattern("HH:mm")))
        }
        else -> stringResource(R.string.edit_entry_duration_for, dur.displayCount(context))
    }

    Column(Modifier.padding(bottom = 6.dp)) {
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp)) {
            Icon(
                Icons.Outlined.Schedule, "",
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

            // remove date
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = { onRmDate() },
                content = { Icon(Icons.Filled.Clear, contentDescription = null) })
        }

        // choose time zine
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).padding(horizontal = 40.dp)) {
            TimeZoneSelector(date.timeZoneId.id, onTimeZoneSelected = {
                val dbEntity = date.toDbEntity()
                onUpdateDate(date.withTimeZone(ZoneId.of(it)))
            })
        }

        // choose duration
        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
            durationSelectorDate = date
        }.padding(horizontal = 40.dp)) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp, vertical = 8.dp)
                    .weight(1f),
                text = textDuration,
                style = MaterialTheme.typography.bodyLarge
            )
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

        // exceptions
        if (date.isPeriodic) {
            for (epochDay in date.exceptionRules.listAll()) {
                val textException = stringResource(R.string.edit_entry_period_except, convertMillisToDate(epochDay * 86400000))

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

            // add exceptions
            Row(
                Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().clickable {
                    dateTimePicker.datePick({}, { onUpdateDate(date.addExceptions(it)) })
                }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(32.dp))
                Icon(Icons.Filled.Add, null, Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.edit_entry_add_exception), style = MaterialTheme.typography.bodyLarge)
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
    onConfirm: (DateState) -> Unit,
    onDismiss: () -> Unit
) {
    val initialPeriodCount = if (date.period is Once) 1 else date.period.count
    val initialPeriodType = PeriodType(date.period)
    var selectedPeriodCount: Long by remember { mutableStateOf(initialPeriodCount) }
    var selectedPeriodType by remember { mutableStateOf(initialPeriodType) }

    var selectedMillis by remember {
        mutableStateOf(
            (if (date.isEndless) date.getFirstZoneDateTime() else date.getLastZoneDateTime()).toLocalDate()
                .toEpochDay() * MILLIS_PER_DAY
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
                Text(stringResource(R.string.common_cancel))
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

                var updatedDateState = date.setPeriod(
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

                if (updatedDateState.isPeriodic) {
                    updatedDateState =
                        when (endVariantSelectedOption) {
                            DateSequenceEndVariant.ENDLESS -> updatedDateState.makeEndless()
                            DateSequenceEndVariant.BY_DATE -> updatedDateState.setRange(
                                startOfDayEnd = ZonedDateTime.of(
                                    LocalDate.ofEpochDay(selectedMillis / MILLIS_PER_DAY).atStartOfDay(),
                                    updatedDateState.getFirstZoneDateTime().zone
                                )
                            )

                            DateSequenceEndVariant.OCCURRENCES -> {
                                updatedDateState.setTimesRepeatUI(if (occurrencesCount.isEmpty()) 1 else occurrencesCount.toLong())
                            }
                        }
                }

                onConfirm(updatedDateState)
            }) {
                Text(stringResource(R.string.common_ok))
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
                        WeekdayButton(monSelected, stringResource(R.string.weekday_mon))
                        WeekdayButton(tueSelected, stringResource(R.string.weekday_tue))
                        WeekdayButton(wedSelected, stringResource(R.string.weekday_wed))
                        WeekdayButton(thuSelected, stringResource(R.string.weekday_thu))
                        WeekdayButton(friSelected, stringResource(R.string.weekday_fri))
                        WeekdayButton(satSelected, stringResource(R.string.weekday_sat))
                        WeekdayButton(sunSelected, stringResource(R.string.weekday_sun))
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
                        stringResource(R.string.edit_entry_endless),
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
                        selectedMillis, stringResource(R.string.edit_entry_end_date_label),
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
                        label = { Text(stringResource(R.string.edit_entry_occurrences_label)) }
                    )
                }
            }
        }
    )
}

@Composable
fun DurationSelectorDialog(
    date: DateState,
    dateTimePicker: DateTimePicker,
    onConfirm: (Period) -> Unit,
    onDismiss: () -> Unit
) {
    val initialOffsetCount = if (date.duration is Once) 1 else date.duration.count
    val initialOffsetType = OffsetType(date.duration)

    var offsetCount: Long by remember { mutableStateOf(initialOffsetCount) }
    var selectedOffsetType by remember { mutableStateOf(initialOffsetType) }

    val startLocalTime = remember { date.getFirstZoneDateTime().toLocalTime() }
    val endLocalTime: LocalTime? = when (val p = selectedOffsetType.period) {
        is Once -> null
        is Minute -> if (offsetCount > 0) startLocalTime.plusMinutes(offsetCount) else null
        is Hour -> if (offsetCount > 0) startLocalTime.plusHours(offsetCount) else null
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        confirmButton = {
            TextButton(onClick = {
                val count = if (offsetCount < 0)
                    if (selectedOffsetType.period is Once) 0
                    else return@TextButton
                else offsetCount

                onConfirm(
                    if (count == 0L || selectedOffsetType.period is Once) Once()
                    else selectedOffsetType.period.updateCount(count)
                )
            }) { Text(stringResource(R.string.common_ok)) }
        },
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val initialEnd = endLocalTime ?: startLocalTime.plusHours(1)
                        dateTimePicker.timePick({}, { picked ->
                            val startMin = startLocalTime.hour * 60 + startLocalTime.minute
                            val endMin = picked.hour * 60 + picked.minute
                            val durationMin = if (endMin > startMin) endMin - startMin
                                             else (24 * 60 - startMin + endMin)
                            when {
                                durationMin == 0 -> {
                                    selectedOffsetType = OffsetType(Once())
                                    offsetCount = 0
                                }
                                durationMin % 60 == 0 -> {
                                    selectedOffsetType = OffsetType(Hour(1))
                                    offsetCount = (durationMin / 60).toLong()
                                }
                                else -> {
                                    selectedOffsetType = OffsetType(Minute(1))
                                    offsetCount = durationMin.toLong()
                                }
                            }
                        }, initialEnd)
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            stringResource(R.string.edit_entry_end_time_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            endLocalTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                                ?: stringResource(R.string.edit_entry_end_time_none),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                OffsetSelector(initialOffsetType, initialOffsetCount, { selectedOffsetType = it }, { offsetCount = it })
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

enum class DateSequenceEndVariant {
    ENDLESS,
    BY_DATE,
    OCCURRENCES
}

data class OffsetType(val period: Period) {
    fun displayName(context: Context): String = period.displayOffsetUnitName(context)

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
    fun displayName(context: Context): String = period.displayUnitName(context)

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
