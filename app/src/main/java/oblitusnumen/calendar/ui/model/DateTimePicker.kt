package oblitusnumen.calendar.ui.model

import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun datePickerModal(
        onDateSelected: (LocalDate) -> Unit,
        onDismiss: () -> Unit,
        initialDate: LocalDate = LocalDate.now()
    ) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toEpochDay() * 86400000)

        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val selMillis = datePickerState.selectedDateMillis
                    if (selMillis != null)
                        onDateSelected(LocalDate.ofEpochDay(selMillis / 86400000))
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
                    Text("Dismiss")
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
