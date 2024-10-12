package oblitusnumen.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.ui.state.CalendarTab
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContent {
//            val count = remember{mutableStateOf(0)}
//
//            Text("Clicks: ${count.value}",
//                fontSize = 28.sp,
//                modifier = Modifier.clickable( onClick = { count.value += 1 })
//            )
//        }
        setContent {
            CalendarTheme {
                val calendarViewModel = viewModel { CalendarViewModel() }
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (calendarViewModel.state) {
                        Screen.CALENDAR ->
                            CalendarTab(calendarViewModel)

                        Screen.DATE -> Text("Date ${calendarViewModel.date}", Modifier.clickable(onClick = {
                            calendarViewModel.setState0(Screen.CALENDAR)
                        }))
                        Screen.ENTRY -> TODO()
                    }
                }
            }
//            NavHostContainer(
//                NavHostController(),
//                padding =
//            )

        }
    }

    class CalendarViewModel : ViewModel() {
        fun setState0(state: Screen) {
            this.state = state
        }

        var listState by mutableStateOf(Int.MAX_VALUE / 2)
        var date: LocalDate? = null
        var calendarLazyListState: LazyListState? = null
        var state by mutableStateOf(Screen.CALENDAR)
    }

    enum class Screen {
        CALENDAR,
        DATE,
        ENTRY
    }
}
