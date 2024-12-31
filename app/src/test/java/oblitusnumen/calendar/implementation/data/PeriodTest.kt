package oblitusnumen.calendar.implementation.data

import junit.framework.TestCase
import oblitusnumen.calendar.implementation.data.Period.Companion.decode
import oblitusnumen.calendar.implementation.data.Period.Weekday
import java.time.LocalDate

class PeriodTest : TestCase() {
    fun testPeriod() {
        val period = Weekday(2, Weekday.WD_MON or Weekday.WD_FRI)
        assertTrue(period.testWeekday(Weekday.WD_MON))
        assertFalse(period.testWeekday(Weekday.WD_TUE))
        assertEquals(Weekday.WD_MON or Weekday.WD_FRI, period.daysMask)
        assertEquals(period.toString(), decode(period.toString()).toString())
        assertTrue(period.testWeekdayIdx(1))
        assertFalse(period.testWeekdayIdx(2))
        val start = LocalDate.of(2024, 12, 29)
        assertFalse(period.verifyWeekday(start, start))
        assertFalse(period.verifyWeekday(start, start.plusDays(1)))
        assertFalse(period.verifyWeekday(start, start.plusDays(2)))
        assertTrue(period.verifyWeekday(start, start.plusDays(8)))
        assertFalse(period.verifyWeekday(start, start.plusDays(10)))
    }
}