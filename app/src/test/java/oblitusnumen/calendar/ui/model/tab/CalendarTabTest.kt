package oblitusnumen.calendar.ui.model.tab

import junit.framework.TestCase
import oblitusnumen.calendar.ui.element.screen.getNowMonthItemWeekIndex
import oblitusnumen.calendar.ui.element.screen.getNowWeekItemIndexExact
import java.time.LocalDate
import java.time.Month

class CalendarTabTest : TestCase() {
    fun testGetNowItemIndexExact() {
        var now = LocalDate.of(2026, Month.JUNE, 1)
        assertEquals(1, getNowWeekItemIndexExact(now) - getNowMonthItemWeekIndex(now))
        now = LocalDate.of(2026, Month.JUNE, 2)
        assertEquals(1, getNowWeekItemIndexExact(now) - getNowMonthItemWeekIndex(now))
        now = LocalDate.of(2026, Month.JUNE, 3)
        assertEquals(1, getNowWeekItemIndexExact(now) - getNowMonthItemWeekIndex(now))
        now = LocalDate.of(2026, Month.JUNE, 4)
        assertEquals(1, getNowWeekItemIndexExact(now) - getNowMonthItemWeekIndex(now))
        now = LocalDate.of(2026, Month.JUNE, 5)
        assertEquals(1, getNowWeekItemIndexExact(now) - getNowMonthItemWeekIndex(now))
        now = LocalDate.of(2026, Month.JUNE, 6)
        assertEquals(1, getNowWeekItemIndexExact(now) - getNowMonthItemWeekIndex(now))
        now = LocalDate.of(2026, Month.JUNE, 7)
        assertEquals(1, getNowWeekItemIndexExact(now) - getNowMonthItemWeekIndex(now))
    }
}