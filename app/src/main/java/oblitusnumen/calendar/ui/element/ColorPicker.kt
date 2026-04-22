package oblitusnumen.calendar.ui.element

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.toColor
import oblitusnumen.calendar.implementation.toInt
import kotlin.math.ceil

@Composable
fun ColorPicker(initialColor: Color, allowCustomColor: Boolean, onColorPicked: (Color?) -> Unit) {
    var pickCustom by remember { mutableStateOf(false) }
    if (!pickCustom) {
        Dialog(onDismissRequest = { onColorPicked(null) }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val countInRow = 4
                    val presetColors = DbManager.presetColors
                    repeat(ceil(presetColors.size.toFloat() / countInRow).toInt()) { rowNumber ->
                        Row {
                            repeat(
                                if (presetColors.size - rowNumber * countInRow > countInRow)
                                    countInRow
                                else
                                    presetColors.size % countInRow
                            ) {
                                val presetColor = presetColors[rowNumber * countInRow + it]
                                ColorBox(
                                    presetColor,
                                    initialColor == presetColor,
                                    Modifier.align(Alignment.CenterVertically)
                                ) {
                                    onColorPicked(
                                        presetColor
                                    )
                                }
                            }
                        }
                    }
                    if (allowCustomColor) {
                        Row(modifier = Modifier.padding(16.dp).clickable { pickCustom = true }) {
                            ColorBox(
                                initialColor,
                                !presetColors.contains(initialColor),
                                Modifier.align(Alignment.CenterVertically)
                            ) {
                                pickCustom = true
                            }
                            Text(
                                stringResource(R.string.color_picker_custom_colors),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(8.dp).align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    } else {
        PickCustomColor(onColorPicked, initialColor)
    }
}

@Composable
fun ColorBox(presetColor: Color, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val modifier1 =
        if (selected)
            modifier.border(4.dp, MaterialTheme.colorScheme.primary)
        else
            modifier
    Box(modifier = modifier1.padding(4.dp).background(presetColor).size(48.dp).clickable(onClick = onClick)) {}
}

@Composable
fun PickCustomColor(onColorPicked: (Color?) -> Unit, initialColor: Color) {
    val pickedColor = remember { mutableStateOf(initialColor) }
    val hasError = remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onColorPicked(null) },
        dismissButton = {
            TextButton(onClick = { onColorPicked(null) }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!hasError.value)
                    onColorPicked(pickedColor.value)
            }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        text = {
            Column {
                val focusManager = LocalFocusManager.current
                Row {
                    Box(
                        Modifier.align(Alignment.CenterVertically).background(pickedColor.value, CircleShape)
                            .border(0.dp, pickedColor.value, CircleShape).size(48.dp).padding(horizontal = 8.dp)
                    )
                    ColorTextField(hasError, focusManager, pickedColor.value) { pickedColor.value = it }
                }
                ColorWheel(pickedColor.value) {
                    pickedColor.value = it
                    focusManager.clearFocus()
                }
            }
        }
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun ColorTextField(
    hasError: MutableState<Boolean>,
    focusManager: FocusManager,
    pickedColor: Color,
    onColorPicked: (Color) -> Unit
) {
    var value: String by remember(pickedColor.value) { mutableStateOf(String.format("#%06X", pickedColor.toInt())) }
    OutlinedTextField(// FIXME: ui paddings
        modifier = Modifier.padding(horizontal = 8.dp),
        label = { Text(stringResource(R.string.color_picker_color_label)) },
        value = value,
        onValueChange = {
            if (it.startsWith("#") && it.length <= 7) {
                value = it
            }
        },
        isError = try {
            value.substring(1).hexToInt()
            value.length != 7
            hasError.value = false
            false
        } catch (_: NumberFormatException) {
            hasError.value = true
            true
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = {
            try {
                if (value.length != 7) return@KeyboardActions
                onColorPicked(value.substring(1).hexToInt().toColor()!!)
                focusManager.clearFocus()
            } catch (_: NumberFormatException) {
            }
        }),
        trailingIcon = {
            if (hasError.value)
                Icon(Icons.Filled.Warning, "")
        }
    )
}

@Composable
fun ColorWheel(color: Color, onColorChanged: (Color) -> Unit) {// FIXME: initial color is not being set
    val controller = rememberColorPickerController()
    HsvColorPicker(
        modifier = Modifier.fillMaxWidth().height(450.dp).padding(10.dp),
        controller = controller,
        onColorChanged = { onColorChanged(it.color) }
    )
}