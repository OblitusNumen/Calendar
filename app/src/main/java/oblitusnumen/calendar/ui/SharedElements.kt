package oblitusnumen.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.Entry
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.ui.model.DateTimePicker
import oblitusnumen.calendar.ui.model.colorPicker
import java.time.LocalDate

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