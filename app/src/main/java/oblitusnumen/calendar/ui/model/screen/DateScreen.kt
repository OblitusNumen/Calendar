package oblitusnumen.calendar.ui.model.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import oblitusnumen.calendar.BackButton
import oblitusnumen.calendar.implementation.bgColorToTextColor
import oblitusnumen.calendar.implementation.data.Date
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.zonedDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateScreen(
    private val day: LocalDate,
    private val dbManager: DbManager,
    private val editEntry: (Int) -> Unit,
    private val backPress: () -> Unit
) : ViewModel() {

    @Composable
    fun compose(modifier: Modifier = Modifier) {
        val dates = remember {
            val begin = zonedDateTime(day)
            dbManager.getDates(
                day,
                day.plusDays(1)
            ).filter { date -> date.forDay(begin) != null }.sortedBy { it.forDay(begin) }
        }
        LazyColumn(modifier) {
            items(dates) {
                drawEntry(it)
            }
        }
    }

    @Composable
    fun functionButton() {
        FloatingActionButton(onClick = { editEntry(-1) }) {
            Icon(Icons.Filled.Add, "add event")
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
    fun drawEntry(date: Date) { //todo maybe show desc too?
        val entry = date.entry
        val tags = entry.getTags()
        Column(
            Modifier.padding(2.dp).fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ).clickable(onClick = { editEntry(entry.id) })
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
                Text(
                    modifier = Modifier.weight(1.0f).padding(end = 8.dp),
                    text = entry.name,
                    style = MaterialTheme.typography.headlineSmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = date.forDay(zonedDateTime(day))
                        !!.format(DateTimeFormatter.ofPattern("HH:mm")), //fixme should show end time
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (tags.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 6.5.dp)
                ) {
                    for (tag in tags) {
                        drawTag(tag.name, Color.Green) //fixme tag.color
                    }
                }
            }
        }
    }

    @Composable
    fun topBar() {
        Row {
            BackButton(backPress)
            Text("Date $day")
        }
    }
}