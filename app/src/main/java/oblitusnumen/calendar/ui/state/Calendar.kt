package oblitusnumen.calendar.ui.state

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oblitusnumen.calendar.StringLocale
import java.time.LocalDate
import java.util.Calendar
import java.util.Date

class Calendar

@Composable
fun CalendarTab() {
    val now = LocalDate.now()
    val dayOfMonthNow = now.dayOfMonth
    Column(Modifier.verticalScroll(ScrollState(1))) {
        var then = now.withDayOfMonth(1)
        repeat(4) {
            DisplayMonth(then, now.month.value, dayOfMonthNow)
            then = then.plusMonths(1)
            Log.v("calendar", "month" + then.dayOfMonth)
        }
    }
}

@Composable
fun DisplayMonth(
    then: LocalDate,
    monthNow: Int,
    dayOfMonthNow: Int
) {
    val blockW = LocalConfiguration.current.screenWidthDp.dp.div(7)
    val blockH = blockW.times(1.2f)
    Log.v("calendar", "day" + then.dayOfWeek.value)
    var dayOfWeek = (then.dayOfWeek.value + 6) % 7 + 1
    var dayOfMonth = -dayOfWeek + 2
    Text(StringLocale.monthName[then.month.value - 1] + ", " + then.year)
    while (dayOfMonth <= then.month.maxLength()) {
        Row {
            while (dayOfMonth < 1) {
                Box(
                    Modifier.size(blockW, blockH)
                ) {
                }
                dayOfMonth++
            }
            while (dayOfWeek <= 7) {
                var border = Modifier.size(blockW, blockH)
                    .border(
                        BorderStroke(
                            2.dp, MaterialTheme.colorScheme.primary
                        )
                    )
                var col: Color
                if (monthNow == then.month.value && dayOfMonth == dayOfMonthNow) {
                    border = border.background(MaterialTheme.colorScheme.primary)
                    col = MaterialTheme.colorScheme.background
                } else {
                    col = MaterialTheme.colorScheme.primary
                }
                Box(border) {
                    Text("" + dayOfMonth, Modifier, col)
                }

                dayOfMonth++
                dayOfWeek++
                if (dayOfMonth > then.month.maxLength()) break
            }
            dayOfWeek = 1
        }
    }
}
