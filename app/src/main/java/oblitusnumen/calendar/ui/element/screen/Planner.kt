package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.tables.Tag
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.LocalDate

@Composable
fun PlannerScreen(
    dbManager: DbManager,
    tagsFilter: MutableState<List<Tag>>,
    navBar: @Composable () -> Unit,
    openEditNewTask: () -> Unit,
    openThatDayInfo: (LocalDate) -> Unit,
    openMonthAgenda: (Int, Int) -> Unit,
    openEntriesScreen: () -> Unit,
    openTagsScreen: () -> Unit,
    openSettings: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.close() } } }
    val openDrawer: () -> Unit = remember { { coroutineScope.launch { drawerState.open() } } }

    ModalNavigationDrawer(
        drawerContent = { PlannerDrawer(closeDrawer, openSettings) },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = { PlannerTopBar(openDrawer) },
            bottomBar = navBar,
            floatingActionButton = {
                FloatingActionButton(onClick = openEditNewTask) {
                    Icon(Icons.Filled.Add, "add task")
                }
            }
        ) { paddingValues ->
            LazyColumn(contentPadding = paddingValues) {
                item {
                    // TODO:  
                }
            }
        }
    }
}

@Composable
fun PlannerDrawer(
    closeDrawer: () -> Unit,
    openSettings: () -> Unit
) {
    ModalDrawerSheet {
        Row(Modifier.padding(16.dp)) {
            Text("Planner", Modifier.align(Alignment.CenterVertically).weight(1f))
            IconButton(onClick = closeDrawer) {
                Icon(Icons.Filled.Close, contentDescription = "close drawer")
            }
        }

        NavigationDrawerItem(
            label = { Text(text = "Settings") },
            selected = false,
            onClick = {
                openSettings()
                closeDrawer()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerTopBar(openDrawer: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null
                )
            }
        },
        title = {},
        actions = {
            IconButton(onClick = {
            }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null
                )
            }
        },
    )
}
