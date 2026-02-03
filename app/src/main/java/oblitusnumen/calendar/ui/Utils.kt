package oblitusnumen.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

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
fun measureTextLine(style: TextStyle, text: String = "0"): Dp {
    val textMeasurer = rememberTextMeasurer()
    val linePx = remember(textMeasurer, style) {
        textMeasurer.measure(text, style).size.height
    }
    return with(LocalDensity.current) { linePx.toDp() }
}

@Composable
fun PaddingValues.horizontal(): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
        return this@horizontal.calculateLeftPadding(layoutDirection)
    }

    override fun calculateTopPadding(): Dp {
        return 0.dp
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
        return this@horizontal.calculateRightPadding(layoutDirection)
    }

    override fun calculateBottomPadding(): Dp {
        return 0.dp
    }
}

enum class PositionStatus {
    Visible, Above, Below
}
