package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.ui.model.colorPicker
import oblitusnumen.calendar.ui.model.screen.EntryEdit.Companion.drawNotificationAddMenu

class SettingsScreen(private val dbManager: DbManager) : ViewModel() {
    @Composable
    fun compose(modifier: Modifier = Modifier, innerPadding: PaddingValues) {
        LazyColumn(modifier) {
            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
            }
            item {
                var color by remember { mutableStateOf(dbManager.defaultEntryColor) }
                var colorPickerShown by remember { mutableStateOf(false) }
                Row(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        "Default entry color",
                        Modifier.weight(1f).align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Box(
                        Modifier.padding(horizontal = 8.dp).size(48.dp).background(color, CircleShape)
                            .border(0.dp, color, CircleShape).align(Alignment.CenterVertically).clickable {
                                colorPickerShown = true
                            }
                    )
                }
                if (colorPickerShown)
                    colorPicker(color, false) {
                        if (it != null) {
                            color = it
                            dbManager.defaultEntryColor = it
                        }
                        colorPickerShown = false
                    }
            }
            item {
                var color by remember { mutableStateOf(dbManager.defaultTagColor) }
                var colorPickerShown by remember { mutableStateOf(false) }
                Row(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Default tag color",
                        Modifier.weight(1f).align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Box(
                        Modifier.padding(horizontal = 8.dp).size(48.dp).background(color, CircleShape)
                            .border(0.dp, color, CircleShape).align(Alignment.CenterVertically).clickable {
                                colorPickerShown = true
                            }
                    )
                }
                if (colorPickerShown)
                    colorPicker(color, false) {
                        if (it != null) {
                            color = it
                            dbManager.defaultTagColor = it
                        }
                        colorPickerShown = false
                    }
            }
            item {
                var notifications by remember { mutableStateOf(dbManager.defaultNotifications) }
                var notificationChoose by remember { mutableStateOf(false) }
                if (notificationChoose)
                    drawNotificationAddMenu({ offset, sound ->
                        notificationChoose = false
                        for (notification in notifications) {
                            if (notification.first.toString() == offset.toString()) {
                                notifications -= notification
                                break
                            }
                        }
                        notifications = (notifications + (offset to sound)).sortedBy { it.first.secondsApproximation() }
                        dbManager.defaultNotifications = notifications
                    }, { notificationChoose = false })
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Default notifications",
                        style = MaterialTheme.typography.titleLarge
                    )
                    for (notification in notifications) {
                        Row(modifier = Modifier.defaultMinSize(minHeight = 52.dp).clickable {
                            notifications =
                                (notifications + (notification.first to !notification.second) - notification)
                                    .sortedBy { it.first.secondsApproximation() }
                            dbManager.defaultNotifications = notifications
                        }) {
                            Icon(
                                if (notification.second) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                null,
                                Modifier.align(Alignment.CenterVertically).padding(8.dp)
                            )
                            Text(
                                modifier = Modifier.align(Alignment.CenterVertically).padding(4.dp)
                                    .weight(1f),
                                text = "${notification.first.count} ${notification.first.javaClass.simpleName} before",// FIXME: text
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                onClick = {
                                    notifications -= notification
                                    dbManager.defaultNotifications = notifications
                                },
                                content = { Icon(Icons.Filled.Clear, contentDescription = null) })
                        }
                    }
                    Box(Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth()/*.padding(top = 8.dp)*/.clickable {
                        notificationChoose = true
                    }) {
                        Text(
                            modifier = Modifier.align(Alignment.CenterStart)
                                .padding(horizontal = 44.dp, vertical = 4.dp),
                            text = "Add notification", // FIXME: start with plus icon
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun topBar(backPress: () -> Unit) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = { Text("Settings", maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = backPress) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Localized description"
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
}