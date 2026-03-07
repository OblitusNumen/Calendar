package oblitusnumen.calendar.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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
