package oblitusnumen.calendar.ui.model

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class DateTimePicker {
    private var datePickerShown by mutableStateOf(false)
    private var timePickerShown by mutableStateOf(false)
    private var selectedDate: LocalDate = LocalDate.now()
    private var initialDateTime: LocalDateTime = LocalDateTime.now()
    private var cancelCallback: (() -> Unit)? = null
    private var confirmCallback: ((LocalDateTime) -> Unit)? = null
    private var confirmCallbackDateOnly: ((LocalDate) -> Unit)? = null

    @Composable
    fun tryCompose() {
        if (datePickerShown) {
            datePickerModal({
                if (confirmCallbackDateOnly != null) {
                    confirmCallbackDateOnly?.invoke(it)
                    cleanup()
                    return@datePickerModal
                }
                selectedDate = it
                datePickerShown = false
                timePickerShown = true
            }, {
                cancelCallback?.invoke()
                cleanup()
            }, initialDateTime.toLocalDate())
        }
        if (timePickerShown) {
            timePickerDialog({
                cancelCallback?.invoke()
                cleanup()
            }, { hour, minute ->
                confirmCallback?.invoke(selectedDate.atTime(hour, minute))
                cleanup()
            }, initialDateTime.toLocalTime())
        }
    }

    fun dateTimePick(
        onCancel: () -> Unit,
        onConfirm: (LocalDateTime) -> Unit,
        dateTime: LocalDateTime = LocalDateTime.now()
    ) {
        if (datePickerShown || timePickerShown)
            return
        cancelCallback = onCancel
        confirmCallback = onConfirm
        initialDateTime = dateTime
        datePickerShown = true
    }

    fun datePick(
        onCancel: () -> Unit,
        onConfirm: (LocalDate) -> Unit,
        initialDate: LocalDate = LocalDate.now()
    ) {
        if (datePickerShown || timePickerShown)
            return
        cancelCallback = onCancel
        confirmCallbackDateOnly = onConfirm
        initialDateTime = initialDate.atStartOfDay()
        datePickerShown = true
    }

    private fun cleanup() {
        initialDateTime = LocalDateTime.now()
        cancelCallback = null
        confirmCallback = null
        confirmCallbackDateOnly = null
        datePickerShown = false
        timePickerShown = false
    }

    companion object {
        private fun convertMillisToDate(millis: Long): String {
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            return formatter.format(Date(millis))
        }

        @Composable
        fun datePickerField(selectedMillis: MutableState<Long>, label: String) {
            var showDatePicker by remember { mutableStateOf(false) }
            val selectedDate = convertMillisToDate(selectedMillis.value)

            Box(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            ) {
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { },
                    label = { Text(label) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = !showDatePicker }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )

                if (showDatePicker) {
                    datePickerModal(
                        {
                            selectedMillis.value = it
                            showDatePicker = false
                        },
                        { showDatePicker = false },
                        selectedMillis.value,
                    )
                }
            }
        }

        @Composable
        private fun datePickerModal(
            onDateSelected: (LocalDate) -> Unit,
            onDismiss: () -> Unit,
            initialDate: LocalDate = LocalDate.now()
        ) {
            datePickerModal(
                { onDateSelected(LocalDate.ofEpochDay(it / 86400000)) },
                onDismiss,
                initialDate.toEpochDay() * 86400000
            )
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun datePickerModal(
            onDateSelected: (Long) -> Unit,
            onDismiss: () -> Unit,
            initialDate: Long
        ) {
            val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = initialDate)

            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = {
                        val selMillis = datePickerState.selectedDateMillis
                        if (selMillis != null)
                            onDateSelected(selMillis)
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun timePickerDialog(
            onDismiss: () -> Unit,
            onConfirm: (Int, Int) -> Unit,
            initialTime: LocalTime = LocalTime.now(),
        ) {
            val timePickerState = rememberTimePickerState(
                initialHour = initialTime.hour,
                initialMinute = initialTime.minute,
                is24Hour = true,
            )
            AlertDialog(
                onDismissRequest = onDismiss,
                dismissButton = {
                    TextButton(onClick = { onDismiss() }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onConfirm(timePickerState.hour, timePickerState.minute)
                    }) {
                        Text("OK")
                    }
                },
                text = {
                    TimePicker(
                        state = timePickerState,
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> materialSpinner(
    title: String, options: List<T>,
    onSelect: (option: T) -> Unit, initialOption: T = options[0], modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(initialOption) }

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it },
        modifier = Modifier.then(modifier)
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = selectedOption.toString(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(title, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString(), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        selectedOption = option
                        onSelect(selectedOption)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
