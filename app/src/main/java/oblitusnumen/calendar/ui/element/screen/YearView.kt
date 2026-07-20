package oblitusnumen.calendar.ui.element.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oblitusnumen.calendar.R
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.views.ViewDateWithOptions
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.zonedDateTime
import oblitusnumen.calendar.ui.element.BackPressButton
import oblitusnumen.calendar.ui.theme.topBarColors
import java.time.DayOfWeek
import java.time.LocalDate

private const val PAGER_CENTER = Int.MAX_VALUE / 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearViewScreen(
    dbManager: DbManager,
    openMonthAgenda: (year: Int, monthValue: Int) -> Unit,
    backPress: () -> Unit,
) {
    val today = remember { LocalDate.now(defaultZoneId()) }
    val pagerState = rememberPagerState(initialPage = PAGER_CENTER, pageCount = { Int.MAX_VALUE })
    val coroutineScope = rememberCoroutineScope()
    val currentYear = today.year + pagerState.currentPage - PAGER_CENTER

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = topBarColors(),
                navigationIcon = { BackPressButton(backPress) },
                title = {
                    Text(
                        currentYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.cd_prev_year),
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.cd_next_year),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val year = today.year + page - PAGER_CENTER
            YearGrid(dbManager, year, today, openMonthAgenda)
        }
    }
}

@Composable
private fun YearGrid(
    dbManager: DbManager,
    year: Int,
    today: LocalDate,
    openMonthAgenda: (year: Int, monthValue: Int) -> Unit,
) {
    val occurrences: Set<LocalDate> = remember(year, dbManager) {
        val zStart = zonedDateTime(LocalDate.of(year, 1, 1)).toEpochSecond()
        val zEnd = zonedDateTime(LocalDate.of(year, 1, 1).plusYears(1)).toEpochSecond()
        ViewDateWithOptions.all(dbManager, zStart, zEnd)
            .flatMap { it.getAllInRange(zStart, zEnd) }
            .map { it.toLocalDate() }
            .toSet()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        for (rowIdx in 0..5) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (colIdx in 0..1) {
                    val monthValue = rowIdx * 2 + colIdx + 1
                    MiniMonth(
                        Modifier.weight(1f).fillMaxHeight().padding(4.dp),
                        year,
                        monthValue,
                        today,
                        occurrences,
                        openMonthAgenda,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(
    modifier: Modifier,
    year: Int,
    monthValue: Int,
    today: LocalDate,
    occurrences: Set<LocalDate>,
    openMonthAgenda: (year: Int, monthValue: Int) -> Unit,
) {
    val monthNames = stringArrayResource(R.array.monthNames)
    val monthStart = LocalDate.of(year, monthValue, 1)
    val gridStart = monthStart.with(DayOfWeek.MONDAY).let {
        if (it.isAfter(monthStart)) it.minusWeeks(1) else it
    }

    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { openMonthAgenda(year, monthValue) }
            .padding(4.dp),
    ) {
        Text(
            monthNames[monthValue - 1],
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        for (weekIdx in 0..5) {
            val weekStart = gridStart.plusWeeks(weekIdx.toLong())
            if (weekStart.monthValue != monthValue && weekStart.plusDays(6).monthValue != monthValue) continue
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (dow in 0..6) {
                    val day = weekStart.plusDays(dow.toLong())
                    DayCell(
                        Modifier.weight(1f).fillMaxHeight(),
                        day = day,
                        inCurrentMonth = day.monthValue == monthValue,
                        isToday = day == today,
                        hasEvent = day in occurrences,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    day: LocalDate,
    inCurrentMonth: Boolean,
    isToday: Boolean,
    hasEvent: Boolean,
) {
    if (!inCurrentMonth) {
        Box(modifier)
        return
    }
    val accent = MaterialTheme.colorScheme.primary
    val eventTint = MaterialTheme.colorScheme.primaryContainer
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        hasEvent -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(modifier.padding(1.dp), contentAlignment = Alignment.Center) {
        val cellModifier = Modifier.fillMaxSize().clip(CircleShape).let {
            when {
                isToday -> it.background(accent)
                hasEvent -> it.background(eventTint)
                else -> it
            }
        }.let {
            if (isToday && hasEvent) it.border(1.dp, eventTint, CircleShape) else it
        }
        Box(cellModifier, contentAlignment = Alignment.Center) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}
