package oblitusnumen.calendar

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.implementation.data.CalendarDate
import oblitusnumen.calendar.implementation.data.DataManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.ui.model.CalendarTab
import oblitusnumen.calendar.ui.model.DateScreen
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.state.CalendarTab
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.time.LocalDateTime
import java.util.*

class MainActivity : ComponentActivity() {
    private val dataManager = DataManager(this)
    var calendarViewModel: CalendarViewModel? = null

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
        dataManager.initialize()
        setContent {
            CalendarTheme {
                val calendarViewModel = viewModel { CalendarViewModel(dataManager) }
                this.calendarViewModel = calendarViewModel
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(topBar = {
                        Button(onClick = {
                            calendarViewModel.back()
                        }, Modifier.offset(200.dp)) {
                            Text("back")
                        }
                    },
                        bottomBar = {
                            Button(onClick = {
                            }) {
                                Text("bottom")
                            }
                        },
                        floatingActionButton = {
                            if (calendarViewModel.screen is DateScreen) {
                                Button(onClick = {
                                    val now = LocalDateTime.now()
                                    val entry = Entry(dataManager)
                                    entry.set(
                                        "huh", ArrayList(), setOf(
                                            CalendarDate(
                                                (calendarViewModel.screen as DateScreen).date.atStartOfDay()
                                                    .withHour(now.hour).withMinute(now.minute).withSecond(now.second),
                                                entry
                                            )
                                        ), ArrayList()
                                    )
                                }) {
                                    Text("+")
                                }
                            }
                        }) {
                        when (calendarViewModel.screen) {
                            is CalendarTab -> CalendarTab(calendarViewModel)
                            is DateScreen ->
                                Column {
                                    Text("Date ${(calendarViewModel.screen as DateScreen).date}")
//                                    Log.v("calendar", "" + (calendarViewModel.screen as DateScreen).dates.size)
                                    for (date in (calendarViewModel.screen as DateScreen).dates) {
                                        Box(
                                            Modifier.height(200.dp)
                                                .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Column {
                                                Text(date.desc)
                                                Text(date.entry.name)
                                                Text(date.date.toString())
                                            }
                                        }
                                    }
                                }

                        }
                    }
                }
            }
//            NavHostContainer(
//                NavHostController(),
//                padding =
//            )

        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK && event.isTracking
                    && !event.isCanceled)
        ) {//fixme back press from main screen does not close app
            calendarViewModel?.back()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    class CalendarViewModel(val dataManager: DataManager) : ViewModel() {
        var screen: Screen by mutableStateOf(CalendarTab())
        private val stateStack = LinkedList<Screen>()

        fun back() {
            if (!stateStack.isEmpty()) screen = stateStack.pop()
        }

        fun open(screen: Screen) {
            stateStack.push(this.screen)
            this.screen = screen
        }
    }
}
