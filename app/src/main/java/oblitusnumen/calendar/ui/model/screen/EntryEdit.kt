package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.*
import oblitusnumen.calendar.implementation.data.Period.*
import oblitusnumen.calendar.ui.model.DateTimePicker
import oblitusnumen.calendar.ui.model.materialSpinner
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class EntryEdit(
    private val dbManager: DbManager,
    private val entry: Entry
) : ViewModel() {
    private var entryName by mutableStateOf(TextFieldValue(entry.name))
    private var tags: List<Tag> by mutableStateOf(entry.getTags().sortedBy { it.name })
    private var dates: List<Date> by mutableStateOf(entry.getDates().sortedBy { it.start })
    private var notifications: List<Notification> by mutableStateOf(
        entry.getNotifications().sortedBy { it.offset.secondsApproximation() })
    private var contents by mutableStateOf(TextFieldValue(entry.getContents()))  // FIXME: this should be List<Content>
    private var dateTimePicker = DateTimePicker()

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun compose(modifier: Modifier = Modifier) {
        dateTimePicker.tryCompose()
        val contentOffsetTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(LocalDensity.current).toDp() } + 64.dp
        val contentOffsetBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(LocalDensity.current).toDp() }
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
            Spacer(Modifier.height(contentOffsetTop))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                value = entryName, onValueChange = {
                    if (!it.text.contains('\n'))
                        entryName = it
                },
                textStyle = MaterialTheme.typography.titleLarge,
                label = { Text("Enter event name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            var tagChoose by remember { mutableStateOf(false) }
            if (tagChoose) tagChooseMenu({ tagChoose = false }, { tags = it })
            Row {
                Icon(Icons.Filled.Star, "Tags", Modifier.padding(8.dp))
                FlowRow(
                    Modifier.fillMaxWidth().padding(end = 16.dp)
                ) {
                    for (tag in tags)
                        drawTag(tag)
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
            for (date in dates)
                drawDate(date)
            Box(Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth()/*.padding(top = 8.dp)*/.clickable {
                dateTimePicker.dateTimePick({}, {
                    dates += Date(
                        dbManager,
                        entry,
                        "",
                        ZonedDateTime.of(it, ZoneId.systemDefault()),
                        0,
                        1,
                        Once()
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
            var notificationChoose by remember { mutableStateOf(false) }
            if (notificationChoose) drawNotificationAddMenu({ offset, sound ->
                notificationChoose = false
                for (notification in notifications) {
                    if (notification.offset.toString() == offset.toString()) {
                        notifications -= notification
                        break
                    }
                }
                notifications = (notifications + Notification(
                    dbManager,
                    entry.id,
                    offset,
                    sound
                )).sortedBy { it.offset.secondsApproximation() }
            }, { notificationChoose = false })
            var updated by remember { mutableStateOf(false) }
            updated// fixme this is hack...
            for (notification in notifications) {
                Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                    notification.sound = !notification.sound
                    updated = !updated
                }) {
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
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            notifications -= notification
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
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            OutlinedTextField(
                modifier = Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth().padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                value = contents, onValueChange = {
                    contents = it
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                label = { Text("Enter description") },
                minLines = 5
            )
            Spacer(Modifier.height(contentOffsetBottom))
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun tagChooseMenu(onClose: () -> Unit, tagAcceptor: (List<Tag>) -> Unit) {
        val allTags = dbManager.getAllTags().groupingBy { it.name }.reduce { _, accumulator, _ -> accumulator }
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
                    tagAcceptor(chosenTags.map {
                        allTags.getOrElse(it) { Tag.new(dbManager, it) }
                    })
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
                            drawTag(
                                searchTag,
                                null,
                                false
                            ) { if (it) chosenTags += searchTag else chosenTags -= searchTag }
                        }
                        for (tag in chosenTags) {
                            drawTag(tag, allTags[tag], true) { if (it) chosenTags += tag else chosenTags -= tag }
                        }
                        for (tag in allTags.values) {
                            if (!chosenTags.contains(tag.name) && tag.name.contains(
                                    searchTag,
                                    true
                                ) && tag.name != searchTag
                            ) drawTag(
                                tag.name,
                                tag,
                                false
                            ) { if (it) chosenTags += tag.name else chosenTags -= tag.name }
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun drawTag(name: String, tag: Tag?, chosen: Boolean, onChooseToggle: (Boolean) -> Unit) {
        var selected by remember(name) { mutableStateOf(chosen) }
        val bgColor = tag?.getColorOrDefault() ?: dbManager.defaultTagColor
        InputChip(
            selected,
            {
                selected = !selected
                onChooseToggle(selected)
            },
            {
                Text(
                    name, style = MaterialTheme.typography.bodyLarge,
                    color = bgColorToTextColor(bgColor)
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            trailingIcon = {
                if (selected) Icon(
                    Icons.Filled.Done, null,
                    tint = bgColorToTextColor(bgColor)
                )
            },
            colors = InputChipDefaults.inputChipColors(containerColor = bgColor, selectedContainerColor = bgColor),
        )
    }

    @Composable
    fun drawNotificationAddMenu(onConfirm: (Period, Boolean) -> Unit, onDismiss: () -> Unit) {
        var silent by remember { mutableStateOf(false) }
        var offsetCount by remember { mutableStateOf("1") }
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
                    onConfirm(
                        if (selectedOffsetType.period is Once) Once() else {
                            val offset = offsetCount.toLong()
                            if (offset > 0) selectedOffsetType.period.updateCount(offset) else Once()
                        }, !silent
                    )
                }) {
                    Text("OK")
                }
            },
            text = {
                Column {
                    Row {
                        //offset text field
                        OutlinedTextField(// FIXME: ui paddings
                            enabled = selectedOffsetType.period !is Once,
                            modifier = Modifier.width(100.dp).padding(horizontal = 8.dp),
                            value = offsetCount, onValueChange = {
                                try {
                                    if (it.toLong() >= 0 && it.length <= 3) offsetCount = it
                                } catch (_: NumberFormatException) {
                                    if (it.isEmpty())
                                        offsetCount = it
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
                            { selectedOffsetType = it },
                            selectedOffsetType,
                            Modifier.padding(horizontal = 8.dp).width(150.dp)
                        )
                    }
                    Row {
                        Text("silent")
                        Switch(silent, onCheckedChange = { silent = it })
                    }
                }
            }
        )
    }

    @Composable
    fun drawDate(date: Date) {
        var periodSelectorShown by remember { mutableStateOf(false) }
        if (periodSelectorShown)
            periodSelectorDialog({ periodSelectorShown = false }, { periodSelectorShown = false }, date)
        var updated by remember { mutableStateOf(false) }
        val textStart = (if (date.isPeriodic) "from " else "") +
                date.getFirstZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
        val textPeriod: String = if (date.isPeriodic)
            "every " + date.period.count.toString() + " " + PeriodType(date.period).toString() +
                    if (date.isEndless) "" else
                        " until " + date.getLastZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        else
            PeriodType(date.period).toString()
        Column(Modifier.padding(bottom = 6.dp)) {
            updated// fixme this is hack...
            Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                dateTimePicker.dateTimePick({}, {
                    date.setRange(startOfDayStart = ZonedDateTime.of(it, date.zoneId))
                    updated = !updated
                }, date.getFirstZoneDateTime().toLocalDateTime())
            }) {
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
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = { dates -= date },
                    content = { Icon(Icons.Filled.Clear, contentDescription = null) })
            }
            Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                periodSelectorShown = true
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
                                date.removeExceptions(LocalDate.ofEpochDay(epochDay))
                                updated = !updated
                            },
                            content = { Icon(Icons.Filled.Clear, contentDescription = null) })
                    }
                }
                Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                    dateTimePicker.datePick({}, {
                        date.addExceptions(it)
                        updated = !updated
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
    fun drawWeekdayButton(active: MutableState<Boolean>, text: String) {
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

    enum class DateSequenceEndVariant {
        ENDLESS,
        BY_DATE,
        OCCURRENCES
    }

    @Composable
    fun periodSelectorDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, date: Date) {
        var periodCount by remember {
            mutableStateOf(
                if (date.period is Once)
                    "1"
                else
                    date.period.count.toString()
            )
        }
        var selectedPeriod by remember { mutableStateOf(PeriodType(date.period)) }
        var selectedMillis by
        remember {
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
                    date.setPeriod(
                        if (selectedPeriod.period is Weekday) {
                            val weekdayDays = (if (monSelected.value) Weekday.WD_MON else 0) +
                                    (if (tueSelected.value) Weekday.WD_TUE else 0) +
                                    (if (wedSelected.value) Weekday.WD_WED else 0) +
                                    (if (thuSelected.value) Weekday.WD_THU else 0) +
                                    (if (friSelected.value) Weekday.WD_FRI else 0) +
                                    (if (satSelected.value) Weekday.WD_SAT else 0) +
                                    (if (sunSelected.value) Weekday.WD_SUN else 0)
                            Weekday(
                                periodCount.toLong(),
                                if (weekdayDays == 0L)
                                    Weekday.dayOfWeekIndexToEnum(date.getFirstZoneDateTime().dayOfWeek.value)
                                else
                                    weekdayDays
                            )
                        } else
                            selectedPeriod.period.updateCount(periodCount.toLong())
                    )
                    if (date.isPeriodic) {
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
                    }
                    onConfirm()
                }) {
                    Text("OK")
                }
            },
            text = {
                Column {
                    Row {
                        //period count text field
                        OutlinedTextField(
                            enabled = selectedPeriod.period !is Once,
                            modifier = Modifier.width(100.dp).padding(horizontal = 8.dp),
                            value = periodCount, onValueChange = {
                                try {
                                    if (it.toLong() >= 0 && it.length <= 3) periodCount = it
                                } catch (_: NumberFormatException) {
                                    if (it.isEmpty())
                                        periodCount = it
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
                            { selectedPeriod = it },
                            selectedPeriod,
                            Modifier.padding(horizontal = 8.dp).width(150.dp)
                        )
                    }
                    if (selectedPeriod.period is Weekday) {
                        Row {
                            drawWeekdayButton(monSelected, "Mon")
                            drawWeekdayButton(tueSelected, "Tue")
                            drawWeekdayButton(wedSelected, "Wed")
                            drawWeekdayButton(thuSelected, "Thu")
                            drawWeekdayButton(friSelected, "Fri")
                            drawWeekdayButton(satSelected, "Sat")
                            drawWeekdayButton(sunSelected, "Sun")
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(top = 8.dp)
                    )
                    //end variant selection
                    //endless radiobutton
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (selectedPeriod.period !is Once) endVariantSelectedOption =
                                DateSequenceEndVariant.ENDLESS
                        }
                    ) {
                        RadioButton(
                            selected = (endVariantSelectedOption == DateSequenceEndVariant.ENDLESS),
                            onClick = { endVariantSelectedOption = DateSequenceEndVariant.ENDLESS },
                            Modifier.align(Alignment.CenterVertically),
                            enabled = selectedPeriod.period !is Once
                        )
                        Text(
                            "Endless",
                            Modifier.padding(vertical = 4.dp, horizontal = 16.dp).align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedPeriod.period !is Once) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = .4F
                            )
                        )
                    }
                    //end date radiobutton
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (selectedPeriod.period !is Once) endVariantSelectedOption =
                                DateSequenceEndVariant.BY_DATE
                        }
                    ) {
                        RadioButton(
                            selected = (endVariantSelectedOption == DateSequenceEndVariant.BY_DATE),
                            onClick = { endVariantSelectedOption = DateSequenceEndVariant.BY_DATE },
                            Modifier.align(Alignment.CenterVertically),
                            enabled = selectedPeriod.period !is Once
                        )
                        DateTimePicker.datePickerField(
                            selectedMillis, "End date",
                            selectedPeriod.period !is Once
                        ) {
                            endVariantSelectedOption = DateSequenceEndVariant.BY_DATE
                            selectedMillis = it
                        }
                    }
                    //occurrences radiobutton
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (selectedPeriod.period !is Once) endVariantSelectedOption =
                                DateSequenceEndVariant.OCCURRENCES
                        }
                    ) {
                        RadioButton(
                            selected = (endVariantSelectedOption == DateSequenceEndVariant.OCCURRENCES),
                            onClick = { endVariantSelectedOption = DateSequenceEndVariant.OCCURRENCES },
                            Modifier.align(Alignment.CenterVertically),
                            enabled = selectedPeriod.period !is Once
                        )
                        OutlinedTextField(
                            enabled = selectedPeriod.period !is Once,
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
    fun drawTag(tag: Tag) {
        val bgColor = tag.getColorOrDefault()
        InputChip(
            false,
            { tags = tags - tag },
            {
                Text(
                    tag.name, style = MaterialTheme.typography.bodyLarge,
                    color = bgColorToTextColor(bgColor)
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            trailingIcon = {
                Icon(
                    Icons.Filled.Clear, null,
                    tint = bgColorToTextColor(bgColor)
                )
            },
            colors = InputChipDefaults.inputChipColors(containerColor = bgColor),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun topBar(backPress: () -> Unit) {// TODO: confirm
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = { Text("Edit event", maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = backPress) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    entry.set(entryName.text, tags, dates, notifications, contents.text)// FIXME: catch exception
                }) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
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
