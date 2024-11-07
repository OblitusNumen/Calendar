package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
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

class TagsTab(private val dbManager: DbManager) : ViewModel() {
    // TODO:
    @Composable
    fun compose() {
        Column(Modifier.verticalScroll(ScrollState(0)).fillMaxWidth()) {
            for (tag in dbManager.tags) {
                Box(
                    Modifier.height(50.dp).fillMaxWidth()
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                ) {
                    Text(tag.name)
                }
            }
        }
    }
}