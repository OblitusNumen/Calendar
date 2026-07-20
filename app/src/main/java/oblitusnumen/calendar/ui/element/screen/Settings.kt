package oblitusnumen.calendar.ui.element.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.app.Activity
import oblitusnumen.calendar.implementation.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.zipDirectoryToStream
import oblitusnumen.calendar.ui.displayOffsetBefore
import oblitusnumen.calendar.ui.element.AddActionRow
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.element.ColorSelectButton
import oblitusnumen.calendar.ui.element.InfoRow
import oblitusnumen.calendar.ui.element.IntTextField
import oblitusnumen.calendar.ui.element.NotificationAddMenu
import oblitusnumen.calendar.ui.element.SectionDivider
import oblitusnumen.calendar.ui.element.SectionHeader
import oblitusnumen.calendar.ui.theme.topBarColors
import java.io.BufferedOutputStream
import java.io.File

@Composable
fun SettingsScreen(dbManager: DbManager, backPress: () -> Unit) {
    val context = LocalContext.current
    Scaffold(topBar = { SettingsTopBar(backPress) }) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            item {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.settings_default_entry_color),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    var color by remember { mutableStateOf(dbManager.defaultEntryColor) }
                    ColorSelectButton(color, false) {
                        color = it
                        dbManager.defaultEntryColor = it
                    }
                }
            }
            item { SectionDivider(tight = true) }
            item {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.settings_default_tag_color),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    var color by remember { mutableStateOf(dbManager.defaultTagColor) }
                    ColorSelectButton(color, false) {
                        color = it
                        dbManager.defaultTagColor = it
                    }
                }
            }
            item { SectionDivider(tight = true) }
            item {
                var notifications by remember { mutableStateOf(dbManager.defaultNotifications) }
                var notificationChoose by remember { mutableStateOf(false) }
                if (notificationChoose)
                    NotificationAddMenu({ offset, sound ->
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
                SectionHeader(stringResource(R.string.settings_default_notifications), emphasised = true)
                for (notification in notifications) {
                    InfoRow(
                        icon = if (notification.second) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        text = notification.first.displayOffsetBefore(context),
                        modifier = Modifier.fillMaxWidth().clickable {
                            notifications =
                                (notifications + (notification.first to !notification.second) - notification)
                                    .sortedBy { it.first.secondsApproximation() }
                            dbManager.defaultNotifications = notifications
                        }
                    ) {
                        IconButton(onClick = {
                            notifications -= notification
                            dbManager.defaultNotifications = notifications
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                }
                AddActionRow(
                    icon = Icons.Filled.Add,
                    label = stringResource(R.string.settings_add_notification),
                    onClick = { notificationChoose = true }
                )
            }
            item { SectionDivider(tight = true) }
            item {
                var notifications by remember { mutableStateOf(dbManager.deadlineNotifications) }
                var notificationChoose by remember { mutableStateOf(false) }
                if (notificationChoose)
                    NotificationAddMenu({ offset, sound ->
                        notificationChoose = false
                        for (notification in notifications) {
                            if (notification.first.toString() == offset.toString()) {
                                notifications -= notification
                                break
                            }
                        }
                        notifications = (notifications + (offset to sound)).sortedBy { it.first.secondsApproximation() }
                        dbManager.deadlineNotifications = notifications
                    }, { notificationChoose = false })
                SectionHeader(stringResource(R.string.settings_deadline_notifications), emphasised = true)
                for (notification in notifications) {
                    InfoRow(
                        icon = if (notification.second) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        text = notification.first.displayOffsetBefore(context),
                        modifier = Modifier.fillMaxWidth().clickable {
                            notifications =
                                (notifications + (notification.first to !notification.second) - notification)
                                    .sortedBy { it.first.secondsApproximation() }
                            dbManager.deadlineNotifications = notifications
                        }
                    ) {
                        IconButton(onClick = {
                            notifications -= notification
                            dbManager.deadlineNotifications = notifications
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                }
                AddActionRow(
                    icon = Icons.Filled.Add,
                    label = stringResource(R.string.settings_add_notification),
                    onClick = { notificationChoose = true }
                )
            }
            item { SectionDivider(tight = true) }
            item {
                LanguageSetting()
            }
            item { SectionDivider(tight = true) }
            item {
                var hour by remember { mutableStateOf(dbManager.morningNotificationHour) }
                var minute by remember { mutableStateOf(dbManager.morningNotificationMinute) }
                Column {
                    SectionHeader(stringResource(R.string.settings_morning_notification_time), emphasised = true)
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IntTextField(
                            value = hour,
                            onValueChange = { v ->
                                if (v != null && v in 0..23) {
                                    hour = v
                                    dbManager.morningNotificationHour = v
                                    dbManager.tryScheduleMorningNotification()
                                }
                            },
                            label = { Text(stringResource(R.string.settings_hour)) },
                            maxDigits = 2
                        )
                        Text(":", Modifier.padding(horizontal = 4.dp))
                        IntTextField(
                            value = minute,
                            onValueChange = { v ->
                                if (v != null && v in 0..59) {
                                    minute = v
                                    dbManager.morningNotificationMinute = v
                                    dbManager.tryScheduleMorningNotification()
                                }
                            },
                            label = { Text(stringResource(R.string.settings_minute)) },
                            maxDigits = 2
                        )
                    }
                }
            }
            item { SectionDivider(tight = true) }
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
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Button(
                        onClick = { saveLauncher.launch("backup.zip") },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(text = stringResource(R.string.settings_export))
                    }
                    Button(
                        onClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(text = stringResource(R.string.settings_restore))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSetting() {
    val context = LocalContext.current
    val options = listOf(
        "" to stringResource(R.string.settings_language_system),
        "en" to stringResource(R.string.settings_language_english),
        "ru" to stringResource(R.string.settings_language_russian),
    )
    val selectedTag = LocaleHelper.getTag(context)
    val selectedLabel = options.firstOrNull { it.first == selectedTag }?.second ?: options[0].second
    var expanded by remember { mutableStateOf(false) }
    Column {
        SectionHeader(stringResource(R.string.settings_language), emphasised = true)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (tag, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            if (tag != selectedTag) {
                                LocaleHelper.setTag(context, tag)
                                (context as? Activity)?.recreate()
                            }
                        }
                    )
                }
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
        title = { Text(stringResource(R.string.settings_title), maxLines = 1) },
    )
}
