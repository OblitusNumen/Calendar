package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.convertMillisToDate
import oblitusnumen.calendar.implementation.data.*
import oblitusnumen.calendar.ui.model.DateTimePicker
import oblitusnumen.calendar.ui.model.materialSpinner
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class EntryEdit(
    private val dbManager: DbManager,
    private val entry: Entry,
    private val backPress: () -> Unit
) : ViewModel() {
    private var entryName by mutableStateOf(TextFieldValue(entry.name))
    private var tags: List<Tag> by mutableStateOf(entry.getTags().sortedBy { it.name })
    private var dates: List<Date> by mutableStateOf(entry.getDates().sortedBy { it.start })
    private var notifications: List<Notification> by mutableStateOf(entry.getNotifications().sortedBy { it.offset.secondsApproximation() })
    private var contents by mutableStateOf(TextFieldValue(entry.getContents()))  // FIXME: this should be List<Content>
    private var dateTimePicker = DateTimePicker()

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun compose(modifier: Modifier = Modifier) {
        dateTimePicker.tryCompose()
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).fillMaxHeight()) {
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
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                for (tag in tags)
                    drawTag(tag)
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
                        Period()
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
                    entry.id!!,
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
                        text = "${notification.offset.data} ${notification.offset.modifier} before",// FIXME: text
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
        val bgColor = Color.Transparent //fixme tag color
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
            colors = InputChipDefaults.inputChipColors(containerColor = bgColor),
        )
    }

    @Composable
    fun drawNotificationAddMenu(onConfirm: (Period, Boolean) -> Unit, onDismiss: () -> Unit) {
        var silent by remember { mutableStateOf(false) }
        var offsetCount by remember { mutableStateOf("1") }
        var selectedOffsetType by remember { mutableStateOf(OffsetType(Period.ONCE)) }

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
                        if (selectedOffsetType.id == Period.ONCE) Period() else {
                            val offset = offsetCount.toLong()
                            if (offset > 0) Period(selectedOffsetType.id, offset) else Period()
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
                            enabled = selectedOffsetType.id != Period.ONCE,
                            modifier = Modifier.width(100.dp).padding(horizontal = 8.dp),
                            value = offsetCount, onValueChange = {
                                try {
                                    if (it.toLong() >= 0) offsetCount = it
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
            "every " + date.period.data.toString() + " " + PeriodType(date.period.modifier).toString() +
                    if (date.isEndless) "" else
                        " until " + date.getLastZoneDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        else
            PeriodType(date.period.modifier).toString()
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
        var periodCount by remember { mutableStateOf(if (date.period.modifier == Period.ONCE) "1" else date.period.data.toString()) }
        var selectedPeriod by remember { mutableStateOf(PeriodType(date.period.modifier)) }
        var selectedMillis by
        remember {
            mutableStateOf(
                (if (date.isEndless) date.getFirstZoneDateTime() else date.getLastZoneDateTime()).toLocalDate()
                    .toEpochDay() * 86_400_000
            )
        }
        var occurrencesCount by remember { mutableStateOf(if (date.isEndless) "1" else "${date.timesRepeat}") }
        var endVariantSelectedOption by remember {
            mutableStateOf(
                if (date.isEndless) DateSequenceEndVariant.ENDLESS
                else DateSequenceEndVariant.BY_DATE
            )
        }
        /*val monSelected = remember { mutableStateOf(false) }
        val tueSelected = remember { mutableStateOf(false) }
        val wedSelected = remember { mutableStateOf(false) }
        val thuSelected = remember { mutableStateOf(false) }
        val friSelected = remember { mutableStateOf(false) }
        val satSelected = remember { mutableStateOf(false) }
        val sunSelected = remember { mutableStateOf(false) }*/
        AlertDialog(
            onDismissRequest = onDismiss,
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    date.setPeriod(Period(selectedPeriod.id, periodCount.toLong()))
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
                                date.setTimesRepeat(if (occurrencesCount.isEmpty()) 1 else occurrencesCount.toLong())
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
                            enabled = selectedPeriod.id != Period.ONCE,
                            modifier = Modifier.width(100.dp).padding(horizontal = 8.dp),
                            value = periodCount, onValueChange = {
                                try {
                                    if (it.toLong() >= 0) periodCount = it
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
                    /*Row {
                        drawWeekdayButton(monSelected, "Mon")
                        drawWeekdayButton(tueSelected, "Tue")
                        drawWeekdayButton(wedSelected, "Wed")
                        drawWeekdayButton(thuSelected, "Thu")
                        drawWeekdayButton(friSelected, "Fri")
                        drawWeekdayButton(satSelected, "Sat")
                        drawWeekdayButton(sunSelected, "Sun")
                    }*/
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(top = 8.dp))
                    //end variant selection
                    //endless radiobutton
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (selectedPeriod.id != Period.ONCE) endVariantSelectedOption =
                                DateSequenceEndVariant.ENDLESS
                        }
                    ) {
                        RadioButton(
                            selected = (endVariantSelectedOption == DateSequenceEndVariant.ENDLESS),
                            onClick = { endVariantSelectedOption = DateSequenceEndVariant.ENDLESS },
                            Modifier.align(Alignment.CenterVertically),
                            enabled = selectedPeriod.id != Period.ONCE
                        )
                        Text(
                            "Endless",
                            Modifier.padding(vertical = 4.dp, horizontal = 16.dp).align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedPeriod.id != Period.ONCE) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = .4F
                            )
                        )
                    }
                    //end date radiobutton
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (selectedPeriod.id != Period.ONCE) endVariantSelectedOption =
                                DateSequenceEndVariant.BY_DATE
                        }
                    ) {
                        RadioButton(
                            selected = (endVariantSelectedOption == DateSequenceEndVariant.BY_DATE),
                            onClick = { endVariantSelectedOption = DateSequenceEndVariant.BY_DATE },
                            Modifier.align(Alignment.CenterVertically),
                            enabled = selectedPeriod.id != Period.ONCE
                        )
                        DateTimePicker.datePickerField(
                            selectedMillis, "End date",
                            selectedPeriod.id != Period.ONCE
                        ) {
                            endVariantSelectedOption = DateSequenceEndVariant.BY_DATE
                            selectedMillis = it
                        }
                    }
                    //occurrences radiobutton
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (selectedPeriod.id != Period.ONCE) endVariantSelectedOption =
                                DateSequenceEndVariant.OCCURRENCES
                        }
                    ) {
                        RadioButton(
                            selected = (endVariantSelectedOption == DateSequenceEndVariant.OCCURRENCES),
                            onClick = { endVariantSelectedOption = DateSequenceEndVariant.OCCURRENCES },
                            Modifier.align(Alignment.CenterVertically),
                            enabled = selectedPeriod.id != Period.ONCE
                        )
                        OutlinedTextField(
                            enabled = selectedPeriod.id != Period.ONCE,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            value = occurrencesCount,
                            onValueChange = {
                                endVariantSelectedOption = DateSequenceEndVariant.OCCURRENCES
                                try {
                                    if (it.toLong() > 0)
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
        val bgColor = Color.Transparent //fixme tag color
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

    @Composable
    fun topBar() {// TODO: confirm
        Row {
            BackButton(backPress)
            Button(onClick = {
                entry.set(entryName.text, tags, dates, notifications, contents.text)
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("save")
            }
            Button(onClick = {
                entry.deleteCascade()
                backPress()
            }, modifier = Modifier.align(Alignment.Top)) {
                Text("delete")
            }
        }
    }
}

data class OffsetType(val id: Char) {
    override fun toString(): String {
        return when (id) {
            Period.ONCE -> "at time"
            Period.MINUTE -> "minute"
            Period.HOUR -> "hour"
            Period.DAY -> "day"
            Period.WEEK -> "week"
            Period.MONTH -> "month"
            else -> throw IllegalStateException()
        }
    }

    companion object {
        fun getAll(): List<OffsetType> {
            val list = mutableListOf<OffsetType>()
            list.add(OffsetType(Period.ONCE))
            list.add(OffsetType(Period.MINUTE))
            list.add(OffsetType(Period.HOUR))
            list.add(OffsetType(Period.DAY))
            list.add(OffsetType(Period.WEEK))
            list.add(OffsetType(Period.MONTH))
            return list
        }
    }
}

data class PeriodType(val id: Char) {
    override fun toString(): String {
        return when (id) {
            Period.ONCE -> "ounce"
            Period.DAY -> "day"
            Period.WEEK -> "week"
            Period.WEEKDAY -> "weekday"
            Period.MONTH -> "month"
            Period.YEAR -> "year"
            else -> "once"
        }
    }

    companion object {
        fun getAll(): List<PeriodType> {
            val list = mutableListOf<PeriodType>()
            list.add(PeriodType(Period.ONCE))
            list.add(PeriodType(Period.DAY))
            //list.add(PeriodType(Period.WEEKDAY))
            list.add(PeriodType(Period.WEEK))
            list.add(PeriodType(Period.MONTH))
            list.add(PeriodType(Period.YEAR))
            return list
        }
    }
}
