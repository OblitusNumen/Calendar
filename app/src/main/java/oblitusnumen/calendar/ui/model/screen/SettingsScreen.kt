package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.ui.model.colorPicker

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