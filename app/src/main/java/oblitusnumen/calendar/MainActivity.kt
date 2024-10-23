package oblitusnumen.calendar

import android.os.Bundle
import android.view.KeyEvent
import android.widget.CalendarView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.util.Supplier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import oblitusnumen.calendar.MainActivity.CalendarViewModel
import oblitusnumen.calendar.MainActivity.Companion.PADDING
import oblitusnumen.calendar.implementation.data.DataManager
import oblitusnumen.calendar.ui.model.Functional
import oblitusnumen.calendar.ui.model.Screen
import oblitusnumen.calendar.ui.model.Tab
import oblitusnumen.calendar.ui.model.TopBarModifier
import oblitusnumen.calendar.ui.model.tab.CalendarTab
import oblitusnumen.calendar.ui.model.tab.SearchTab
import oblitusnumen.calendar.ui.model.tab.TagsTab
import oblitusnumen.calendar.ui.theme.CalendarTheme
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class MainActivity : ComponentActivity(), TopBarModifier {
    private val dataManager = DataManager(this)
    private var calendarViewModel: CalendarViewModel? = null

    companion object {
        val PADDING: Dp = 5.dp
        val LIST_CENTER = LocalDate.of(1970, 1, 1);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataManager.initialize()
        setContent {
            CalendarTheme {
                val calendarViewModel = viewModel { CalendarViewModel(dataManager) }
                this.calendarViewModel = calendarViewModel
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
                    Scaffold(
                        topBar = {// FIXME:
//                            TopAppBar()
                            Row {
                                var width = getWidthPart(1f)
                                if (calendarViewModel.screen is Tab) {
                                    Button(onClick = {
                                        // FIXME:
//                                openOptionsMenu()
                                    }) {
                                        Text("三")
                                    }
                                    width = width.minus(50.dp)
                                }
                                Box(Modifier.width(width).height(50.dp)) {
                                    (if (calendarViewModel.screen is TopBarModifier) (calendarViewModel.screen as TopBarModifier) else this@MainActivity).topBar(
                                        calendarViewModel
                                    )
                                }
                            }
                        },
                        bottomBar = { BottomBar(calendarViewModel) },
                        floatingActionButton = {
                            if (calendarViewModel.screen is Functional) (calendarViewModel.screen as Functional).functionButton(
                                calendarViewModel
                            )
                        }) {
                        Box(
                            Modifier.absolutePadding(
                                PADDING, 50.dp,
                                PADDING, 50.dp
                            )
                        ) {
                            calendarViewModel.screen.compose(calendarViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        dataManager.close()
        super.onDestroy()
    }

    @Composable
    fun BottomBar(calendarViewModel: CalendarViewModel) {
        Box(Modifier.width(getWidthPart(1f)).height(50.dp)) {
            Row {
                Box(getTabBoxModifier({ CalendarTab() }, calendarViewModel = calendarViewModel)) {
                    Text("calendar")
                }
                Box(getTabBoxModifier({ TagsTab() }, calendarViewModel = calendarViewModel)) {
                    Text("tags")
                }
                Box(getTabBoxModifier({ SearchTab() }, calendarViewModel = calendarViewModel)) {
                    Text("search")
                }
            }
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
            } else return false
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

    @Composable
    override fun topBar(calendarViewModel: CalendarViewModel) {
        // TODO:
    }
}

@Composable
fun getWidthPart(divisor: Float): Dp {
    return LocalConfiguration.current.screenWidthDp.dp.div(divisor)
}

@Composable
fun getWidthPartIncludePadding(divisor: Float): Dp {
    return LocalConfiguration.current.screenWidthDp.dp.minus(PADDING.times(2)).div(divisor)
}

@Composable
fun getHeightPart(divisor: Float): Dp {
    return LocalConfiguration.current.screenHeightDp.dp.div(divisor)
}

@Composable
inline fun <reified T : Screen> getTabBoxModifier(
    classSupplier: Supplier<T>,
    calendarViewModel: CalendarViewModel
): Modifier {
    var modifier = Modifier.width(getWidthPart(3f)).height(50.dp)
    modifier = modifier.clickable(
        onClick = {
            calendarViewModel.changeTab(classSupplier.get())
        }
    )
    if (calendarViewModel.screen::class == T::class) modifier =
        modifier.border(2.dp, MaterialTheme.colorScheme.primary)
    return modifier
}

@Composable
fun BackButton(calendarViewModel: CalendarViewModel) {
    Button(onClick = {
        calendarViewModel.back()
    }) {
        Text("く")
//        Text("←")
    }
}
