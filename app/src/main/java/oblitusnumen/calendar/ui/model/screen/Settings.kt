package oblitusnumen.calendar.ui.model.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.zipDirectoryToStream
import oblitusnumen.calendar.ui.BackPressButton
import oblitusnumen.calendar.ui.ColorSelectButton
import oblitusnumen.calendar.ui.DrawNotificationAddMenu
import oblitusnumen.calendar.ui.theme.topBarColors
import java.io.BufferedOutputStream
import java.io.File

@Composable
fun SettingsScreen(dbManager: DbManager, backPress: () -> Unit) {
    Scaffold(topBar = { SettingsTopBar(backPress) }) { paddingValues ->
        LazyColumn {
            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))
            }
            item {
                Row(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        "Default entry color",
                        Modifier.weight(1f).align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.titleLarge
                    )
                    var color by remember { mutableStateOf(dbManager.defaultEntryColor) }
                    ColorSelectButton(color, false) {
                        color = it
                        dbManager.defaultEntryColor = it
                    }
                }
            }
            item {
                Row(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Default tag color",
                        Modifier.weight(1f).align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.titleLarge
                    )
                    var color by remember { mutableStateOf(dbManager.defaultTagColor) }
                    ColorSelectButton(color, false) {
                        color = it
                        dbManager.defaultTagColor = it
                    }
                }
            }
            item {
                var notifications by remember { mutableStateOf(dbManager.defaultNotifications) }
                var notificationChoose by remember { mutableStateOf(false) }
                if (notificationChoose)
                    DrawNotificationAddMenu({ offset, sound ->
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
                val scope = rememberCoroutineScope()
                val appDataDir = dbManager.filesDir.parentFile!! // contains databases, shared_prefs, files, etc.

                // Launcher for picking backup location
                val saveLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/zip"),
                    onResult = { uri ->
                        uri?.let { pickedUri ->
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // Open OutputStream to the URI provided by the picker
                                    dbManager.contentResolver.openOutputStream(pickedUri)?.use { outStream ->
                                        BufferedOutputStream(outStream).use { buffered ->
                                            // Zip directly to the output stream
                                            zipDirectoryToStream(appDataDir, buffered)
                                        }
                                    }

                                    launch(Dispatchers.Main) {
//                                        TODO:
//                                        Toast.makeText(context, "Backup saved successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    launch(Dispatchers.Main) {
//                                        TODO:
//                                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                )

                // Launcher to pick a ZIP file for restore
                val restoreLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri ->
                        uri?.let {
                            scope.launch(Dispatchers.IO) {
                                // Copy the picked ZIP into app's private dir
                                val stagedZip = File(appDataDir, "restore_staged.zip")
                                dbManager.contentResolver.openInputStream(uri)?.use { input ->
                                    stagedZip.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                        }
                        dbManager.finishApp()
                    }
                )
                Row(Modifier.padding(vertical = 8.dp)) {
                    Button(
                        onClick = { saveLauncher.launch("backup.zip") },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text(text = "Export All Events")
                    }
                    Button(
                        onClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text(text = "Restore Events")
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(backPress: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    CenterAlignedTopAppBar(
        colors = topBarColors(),
        scrollBehavior = scrollBehavior,
        navigationIcon = { BackPressButton(backPress) },
        title = { Text("Settings", maxLines = 1) },
    )
}
