package oblitusnumen.calendar

import android.graphics.drawable.shapes.Shape
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.util.Supplier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.implementation.data.CalendarDate
import oblitusnumen.calendar.implementation.data.DataManager
import oblitusnumen.calendar.implementation.data.Entry
import oblitusnumen.calendar.ui.model.CalendarTab
import oblitusnumen.calendar.ui.model.DateScreen
import oblitusnumen.calendar.ui.model.SearchTab
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.Tab
import oblitusnumen.calendar.ui.model.TagsTab
import oblitusnumen.calendar.ui.state.CalendarTab
import oblitusnumen.calendar.ui.state.getWidthPart
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.time.LocalDateTime
import java.util.*

class MainActivity : ComponentActivity() {
    private val dataManager = DataManager(this)
    private var calendarViewModel: CalendarViewModel? = null

    @Composable
    inline fun <reified T: Screen> getTabBoxModifier(classSupplier: Supplier<T>, calendarViewModel: CalendarViewModel): Modifier {
        var modifier = Modifier.width(getWidthPart(3f)).height(getWidthPart(7f))
        modifier = modifier.clickable(
            onClick = {
                calendarViewModel.changeTab(classSupplier.get())
            }
        )
        if (calendarViewModel.screen::class == T::class) modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary)
        return modifier
    }

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
                    Scaffold(
                        topBar = {
                            Box(Modifier.width(getWidthPart(1f)).height(getWidthPart(7f))) {
                                Button(onClick = {
                                    calendarViewModel.back()
                                }) {
                                    Text("back")
                                }
                            }
                        },
                        bottomBar = {
                            Box(Modifier.width(getWidthPart(1f)).height(getWidthPart(7f))) {
                                Row {

                                    Box(getTabBoxModifier({CalendarTab()}, calendarViewModel = calendarViewModel)) {
                                        Text("calendar")
                                    }
                                    Box(getTabBoxModifier({TagsTab()}, calendarViewModel = calendarViewModel)) {
                                        Text("tags")
                                    }
                                    Box(getTabBoxModifier({SearchTab()}, calendarViewModel = calendarViewModel)) {
                                        Text("search")
                                    }
                                }
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
                        Box(Modifier.absolutePadding(
                            PADDING, getWidthPart(7f),
                            PADDING, getWidthPart(7f))) {
                            when (calendarViewModel.screen) {
                                is CalendarTab -> CalendarTab(calendarViewModel)
                                is DateScreen ->
                                    Column(Modifier.verticalScroll(ScrollState(0))) {
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

                                is SearchTab ->
                                    Text("find")

                                is TagsTab ->
                                    Column(Modifier.verticalScroll(ScrollState(0))) {
                                        for (tag in dataManager.tags) {
                                            Box(
                                                Modifier.height(200.dp)
                                                    .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text(tag.name)
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
            if (calendarViewModel != null && calendarViewModel!!.back()) return true
            else return super.onKeyUp(keyCode, event)
        }
        return super.onKeyUp(keyCode, event)
    }

    class CalendarViewModel(val dataManager: DataManager) : ViewModel() {
        // FIXME: add stack for each tab
        var screen: Screen by mutableStateOf(CalendarTab())
        private val stateStack = LinkedList<Screen>()

        fun back(): Boolean {
            if (!stateStack.isEmpty()) {
                screen = stateStack.pop()
                return true
            }
            else return false
        }

        fun open(screen: Screen) {
            var nextScreen = screen
            if (screen !is Tab) {
                stateStack.push(this.screen)
            } else {
                nextScreen = stateStack.firstOrNull { s -> s::class == screen::class } ?: screen
            }
            this.screen = nextScreen
        }

        fun changeTab(searchTab: Screen) {
            open(searchTab)
        }
    }

    companion object {
        val PADDING: Dp = 5.dp
    }
}
