package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry

class EntriesTab(private val dbManager: DbManager,
                 private val editEntry: (Entry) -> Unit) : ViewModel() {
    // TODO:
    @Composable
    fun compose() {
        Column(Modifier.verticalScroll(ScrollState(0)).fillMaxWidth()) {// TODO: state is not saved
            Text("Entries")
            for (entry in dbManager.getEntries()) {
                Box(
                    Modifier.height(50.dp).fillMaxWidth()
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                        .clickable(onClick = {
                            editEntry(entry)
                        })
                ) {
                    Text(entry.name)
                }
            }
        }
    }
}