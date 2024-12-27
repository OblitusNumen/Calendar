package oblitusnumen.calendar.ui.model.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.Entry
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class EntriesTab(
    private val dbManager: DbManager,
    private val editEntry: (Int) -> Unit
) : ViewModel() {

    @Composable
    fun compose(modifier: Modifier) {
        val entries = remember {// TODO: maybe sort by netx date
            dbManager.getEntries().sortedBy { it.name }
        }
        LazyColumn(modifier) {
            items(entries) {
                drawEntry(it)
            }
        }
    }

    @Composable
    fun drawTag(text: String, bgColor: Color) {
        Text(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.5.dp)
                .background(
                    bgColor,
                    shape = RoundedCornerShape(10.dp)
                ).padding(vertical = 1.dp, horizontal = 6.dp),
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            color = bgColorToTextColor(bgColor)
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun drawEntry(entry: Entry) {
        val tags = entry.getTags()
        Column(
            Modifier.padding(2.dp).fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ).clickable(onClick = { editEntry(entry.id!!) })
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
                Text(
                    modifier = Modifier.weight(1.0f).padding(end = 8.dp),
                    text = entry.name,
                    style = MaterialTheme.typography.headlineSmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                val now = System.currentTimeMillis() / 1000
                var nextDate: ZonedDateTime? = null
                var hasDates = false
                for (date in entry.getDates()) {
                    hasDates = true
                    val next = date.getNext(now)
                    if (nextDate == null || next != null && next < nextDate) nextDate = next
                }
                val nextDateText = if (nextDate != null)
                    nextDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                else if (hasDates) "Ended" else ""
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = nextDateText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }//todo next line "10 events from 2024.01.01 to 2025.01.01"
            if (tags.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 6.5.dp)
                ) {
                    for (tag in tags) {
                        drawTag(tag.name, tag.getColorOrDefault())
                    }
                }
            }
        }
    }

    @Composable
    fun topBar() {
        Text("Entries")
    }
}