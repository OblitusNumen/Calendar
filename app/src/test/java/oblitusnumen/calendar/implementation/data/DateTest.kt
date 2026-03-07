package oblitusnumen.calendar.implementation.data

import junit.framework.TestCase
import oblitusnumen.calendar.implementation.data.Period.*
import oblitusnumen.calendar.implementation.data.tables.Date
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTest : TestCase() {
    companion object {
        val UTC_TIME_ZONE: ZoneId = ZoneId.of("UTC")
    }

    fun testNextClosestRawForWeekdays() {
        val date = Date.newInstance(1,
            1,
            1,
            ZonedDateTime.parse("2024-12-09T09:00:00Z").toEpochSecond(),
            Once(),
            ZonedDateTime.parse("2024-12-09T09:00:00Z").toEpochSecond(),
            100L,
            Weekday(3, Weekday.WD_MON + Weekday.WD_WED + Weekday.WD_FRI + Weekday.WD_SUN),
            UTC_TIME_ZONE,
            ""
        )
        date.makeEndless()
        assertEquals(
            ZonedDateTime.parse("2024-12-09T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-09T08:59:59Z").toEpochSecond())!!.toEpochSecond()
        )
        assertEquals(
            ZonedDateTime.parse("2024-12-09T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-09T09:00:00Z").toEpochSecond())!!.toEpochSecond()
        )
        assertEquals(
            ZonedDateTime.parse("2024-12-11T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-09T09:00:01Z").toEpochSecond())!!.toEpochSecond()
        )
        assertEquals(
            ZonedDateTime.parse("2024-12-30T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-15T09:00:01Z").toEpochSecond())!!.toEpochSecond()
        )
        assertEquals(
            ZonedDateTime.parse("2024-12-30T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-20T08:00:01Z").toEpochSecond())!!.toEpochSecond()
        )
        assertEquals(
            ZonedDateTime.parse("2024-12-13T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-11T18:00:00Z").toEpochSecond())!!.toEpochSecond()
        )
        assertEquals(
            ZonedDateTime.parse("2024-12-11T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-10T09:00:00Z").toEpochSecond())!!.toEpochSecond()
        )
        date.setPeriod(Weekday(3, Weekday.WD_MON + Weekday.WD_WED + Weekday.WD_FRI))
        assertEquals(
            ZonedDateTime.parse("2024-12-30T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-14T09:00:01Z").toEpochSecond())!!.toEpochSecond()
        )
        assertEquals(
            ZonedDateTime.parse("2024-12-30T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-20T08:00:01Z").toEpochSecond())!!.toEpochSecond()
        )
        date.setPeriod(Weekday(3, Weekday.WD_MON + Weekday.WD_WED + Weekday.WD_THU + Weekday.WD_FRI))
        date.setTimesRepeatUI(2)
        assertNull(date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-11T18:00:00Z").toEpochSecond()))
        assertEquals(
            ZonedDateTime.parse("2024-12-11T09:00:00Z").toEpochSecond(),
            date.getNextClosestForTesting(ZonedDateTime.parse("2024-12-11T08:00:00Z").toEpochSecond())!!.toEpochSecond()
        )
    }

    fun testDateExceptionRules() {
        var exceptionRules = ExceptionRules()
        assertEquals(0, exceptionRules.exceptionDatesCount())
        assertFalse(exceptionRules.containsDate(0))
        assertEquals(exceptionRules.toString(), "")
        exceptionRules = ExceptionRules("0_2,4,6_10,12,14")
        assertFalse(exceptionRules.containsDate(3))
        assertTrue(exceptionRules.containsDate(4))
        assertTrue(exceptionRules.containsDate(6))
        assertTrue(exceptionRules.containsDate(7))
        assertTrue(exceptionRules.containsDate(10))
        assertEquals(11, exceptionRules.exceptionDatesCount())
        exceptionRules.addDate(11)
        assertEquals(12, exceptionRules.exceptionDatesCount())
        assertEquals("0_2,4,6_12,14", exceptionRules.toString())
        exceptionRules.addDates(3, 13)
        assertEquals("0_14", exceptionRules.toString())
        exceptionRules = ExceptionRules("5_8,12,14")
        exceptionRules.addDates(0, 4)
        assertEquals("0_8,12,14", exceptionRules.toString())
        exceptionRules.addDates(17, 18)
        assertEquals("0_8,12,14,17_18", exceptionRules.toString())
        exceptionRules = ExceptionRules("5_8,12,14")
        exceptionRules.addDates(0, 20)
        assertEquals("0_20", exceptionRules.toString())
        exceptionRules.addDates(0, 20)
        assertEquals("0_20", exceptionRules.toString())
        exceptionRules = ExceptionRules("5_8,12,14")
        exceptionRules.addDates(1, 2)
        assertEquals("1_2,5_8,12,14", exceptionRules.toString())
        exceptionRules.removeDates(8, 12)
        assertEquals("1_2,5_7,14", exceptionRules.toString())
        exceptionRules.removeDates(3, 6)
        assertEquals("1_2,7,14", exceptionRules.toString())
        exceptionRules.removeDates(0, 1)
        assertEquals("2,7,14", exceptionRules.toString())
        exceptionRules.removeDates(0, 13)
        assertEquals("14", exceptionRules.toString())
        exceptionRules = ExceptionRules("5_8,12,14_18")
        exceptionRules.removeDate(6)
        assertEquals("5,7_8,12,14_18", exceptionRules.toString())
        exceptionRules.removeDates(15, 16)
        assertEquals("5,7_8,12,14,17_18", exceptionRules.toString())
        exceptionRules.removeDates(19, 30)
        assertEquals("5,7_8,12,14,17_18", exceptionRules.toString())
        exceptionRules.trimToFitRange(8, 17)
        assertEquals("8,12,14,17", exceptionRules.toString())
        exceptionRules = ExceptionRules()
        exceptionRules.addDates(-15, -5)
        exceptionRules.addDate(-3)
        assertEquals("-15_-5,-3", exceptionRules.toString())
        exceptionRules = ExceptionRules("-10_-9,-8,-3_0,2")
        assertEquals("-10_-9,-8,-3_0,2", exceptionRules.toString())
    }

    fun testSetTimesRepeat() {
        val numberOfDated = 500
        val date = Date.newInstance(
            1, 1, 1, 0, Once(), 0,
            numberOfDated.toLong(), Week(1), UTC_TIME_ZONE, ""
        )
        assertEquals("", date.exceptionRules.toString())
        date.setTimesRepeatUI(50)
        assertEquals(50, date.getTimesRepeatForTesting())
        assertFalse(date.isEmpty)
        assertFalse(date.isEndless)
        date.makeEndless()
        assertFalse(date.isEmpty)
        assertTrue(date.isEndless)
        date.setTimesRepeatUI(50)
        assertEquals(50, date.getTimesRepeatForTesting())
        assertFalse(date.isEmpty)
        assertFalse(date.isEndless)
    }

    fun testForDayBefore() {
        val time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 100, Once(), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1969, 12, 30, 23, 59, 59, 0, ZoneId.of("UTC")))
        assertNull(time)
    }

    fun testForDayAfter() {
        val time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 100, Once(), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC")))
        assertNull(time)
    }

    fun testForDayAt() {
        val time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 100, Once(), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))
        assertNotNull(time)
    }

    fun testForDayAtPeriod() {
        var time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Day(1), UTC_TIME_ZONE, ""
        ).fixEndForTesting().forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")))
        assertNotNull(time)
        time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Day(1), UTC_TIME_ZONE, ""
        ).fixEndForTesting().forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")).plusDays(5))
        assertNotNull(time)
        time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Day(1), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 1, 0, ZoneId.of("UTC")).plusDays(9))
        assertNull(time)
        time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Month(1), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))
        assertNotNull(time)
        time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Month(1), UTC_TIME_ZONE, ""
        ).fixEndForTesting().forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(5))
        assertNotNull(time)
        time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Month(1), UTC_TIME_ZONE, ""
        ).fixEndForTesting().forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(9))
        assertNotNull(time)
    }

    fun testForDayBetweenPeriod() {
        var time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Month(1), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusDays(25))
        assertNull(time)
        time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Day(2), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 11, 0, 0, 0, ZoneId.of("UTC")).plusHours(11))
        assertNull(time)
    }

    fun testForDayAfterPeriod() {
        var time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Month(1), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(10))
        assertNull(time)
        time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 10, Day(1), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")).plusDays(10))
        assertNull(time)
    }

    fun testFixExceptionList() {
        val date = Date.newInstance(1, 1, 1, 0, Once(),
            0, 80, Day(1), UTC_TIME_ZONE, ""
        ).fixEndForTesting()
        date.addExceptions(date.getZDTForTesting(1).toLocalDate(), date.getZDTForTesting(100).toLocalDate())
        assertEquals(80, date.getTimesRepeatForTesting())
        date.fixExceptionList()
        assertEquals("1_79", date.exceptionRules.toString())
        date.setTimesRepeatUI(50)
        assertEquals(50, date.getTimesRepeatForTesting())
        assertFalse(date.isEmpty)
        assertFalse(date.isEndless)
        date.fixExceptionList()
        assertEquals("1_49", date.exceptionRules.toString())
    }

    fun testExceptionsFromGetZonedDTI() {
        val numberOfDated = 50
        var date = Date.newInstance(1, 1, 1, 0, Once(),
            0, numberOfDated.toLong(), Week(1), UTC_TIME_ZONE, ""
        )
        date.addExceptions(date.getZDTForTesting(5).toLocalDate(), date.getZDTForTesting(23).toLocalDate())
        assertEquals(50, date.getTimesRepeatForTesting())
        assertEquals("35_161", date.exceptionRules.toString())
        date.addExceptions(date.getZDTForTesting(20).toLocalDate(), date.getZDTForTesting(23).toLocalDate())
        assertEquals(50, date.getTimesRepeatForTesting())
        assertEquals("35_161", date.exceptionRules.toString())
        date = Date.newInstance(1, 1, 1, 0, Once(),
            0, numberOfDated.toLong(), Month(1), UTC_TIME_ZONE, ""
        )
        date.addExceptions(date.getZDTForTesting(3).toLocalDate(), date.getZDTForTesting(12).toLocalDate())
        assertEquals("90_365", date.exceptionRules.toString())
    }

    fun testFixRanges() {
        val numberOfDated = 50
        var date = Date.newInstance(1, 1, 1, 0, Once(),
            0, numberOfDated.toLong(), Week(1), UTC_TIME_ZONE, ""
        ).fixEndForTesting()
        date.addExceptions(date.getZDTForTesting(0).toLocalDate(), date.getZDTForTesting(50).toLocalDate())
        date.fixDateRange()
        date.fixExceptionList()
        assertEquals(0, date.getTimesRepeatForTesting())
        assertTrue(date.isEmpty)
        assertEquals("", date.exceptionRules.toString())
        date = Date.newInstance(1, 1, 1, 0, Once(),
            0, numberOfDated.toLong(), Week(1), UTC_TIME_ZONE, ""
        ).fixEndForTesting()
        date.addExceptions(date.getZDTForTesting(0).toLocalDate(), date.getZDTForTesting(4).toLocalDate())
        date.addExceptions(date.getZDTForTesting(40).toLocalDate(), date.getZDTForTesting(49).toLocalDate())
        date.addExceptions(date.getZDTForTesting(21).toLocalDate(), date.getZDTForTesting(24).toLocalDate())
        assertEquals(50, date.getTimesRepeatForTesting())
        date.fixDateRange()
        assertEquals(35, date.getTimesRepeatForTesting())
        date.fixDateRange()
        assertEquals(35, date.getTimesRepeatForTesting())
        assertFalse(date.isEmpty)
        date.fixExceptionList()
        assertEquals("147_168", date.exceptionRules.toString())
        date.addExceptions(date.getZDTForTesting(0).toLocalDate(), date.getZDTForTesting(34).toLocalDate())
        date.fixDateRange()
        assertTrue(date.isEmpty)
    }

    fun testForDayIndexAt() {
        val time = Date.newInstance(1, 1, 1, 0, Once(),
            0, 100, Once(), UTC_TIME_ZONE, ""
        ).forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))
        assertNotNull(time)
    }

    fun testTZKievFromMoscowDay() {
        val test = """
                2024-10-19T22:59:59+03:00[Europe/Moscow]
                2024-10-20T22:59:59+03:00[Europe/Moscow]
                2024-10-21T22:59:59+03:00[Europe/Moscow]
                2024-10-22T22:59:59+03:00[Europe/Moscow]
                2024-10-23T22:59:59+03:00[Europe/Moscow]
                2024-10-24T22:59:59+03:00[Europe/Moscow]
                2024-10-25T22:59:59+03:00[Europe/Moscow]
                2024-10-26T22:59:59+03:00[Europe/Moscow]
                2024-10-27T23:59:59+03:00[Europe/Moscow]
                2024-10-28T23:59:59+03:00[Europe/Moscow]
                2024-10-29T23:59:59+03:00[Europe/Moscow]
                2024-10-30T23:59:59+03:00[Europe/Moscow]
                2024-10-31T23:59:59+03:00[Europe/Moscow]
                2024-11-01T23:59:59+03:00[Europe/Moscow]
                2024-11-02T23:59:59+03:00[Europe/Moscow]
                2024-11-03T23:59:59+03:00[Europe/Moscow]
                2024-11-04T23:59:59+03:00[Europe/Moscow]
                2024-11-05T23:59:59+03:00[Europe/Moscow]
                2024-11-06T23:59:59+03:00[Europe/Moscow]
                2024-11-07T23:59:59+03:00[Europe/Moscow]
                2024-11-08T23:59:59+03:00[Europe/Moscow]
                2024-11-09T23:59:59+03:00[Europe/Moscow]
                
                """.trimIndent().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val utc =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond((1729458000 - 86400).toLong()), ZoneId.of("Europe/Moscow"))
        val date = Date.newInstance(1, 1, 1, 972071999, Once(),
            0, 10950, Day(1), ZoneId.of("Europe/Kiev"), ""
        ).fixEndForTesting()
        for (i in -1..20) {
            val time = date.forDay(utc.plusDays(i.toLong()))
            println(utc.plusDays(i.toLong()))
            if (test[i + 1] != "null") {
                assertNotNull(time)
                println(time!!.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
            } else {
                if (time != null) println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
                assertNull(time)
            }
        }
    }

    fun testTZKievFromMoscowDayCount3() {
        val test = """
                null
                2024-10-20T22:59:59+03:00[Europe/Moscow]
                null
                null
                2024-10-23T22:59:59+03:00[Europe/Moscow]
                null
                null
                2024-10-26T22:59:59+03:00[Europe/Moscow]
                null
                null
                2024-10-29T23:59:59+03:00[Europe/Moscow]
                null
                null
                2024-11-01T23:59:59+03:00[Europe/Moscow]
                null
                null
                2024-11-04T23:59:59+03:00[Europe/Moscow]
                null
                null
                2024-11-07T23:59:59+03:00[Europe/Moscow]
                null
                null
                
                """.trimIndent().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val utc =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond((1729458000 - 86400).toLong()), ZoneId.of("Europe/Moscow"))
        val date = Date.newInstance(1, 1, 1, 972071999, Once(),
            0, 10950, Day(3), ZoneId.of("Europe/Kiev"), ""
        ).fixEndForTesting()
        for (i in -1..20) {
            val time = date.forDay(utc.plusDays(i.toLong()))
            println(utc.plusDays(i.toLong()))
            if (test[i + 1] != "null") {
                assertNotNull(time)
                println(time!!.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
            } else {
                if (time != null) println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
                assertNull(time)
            }
        }
    }

    fun testTZMoscowFromKievDay() {
        val test = """
                2024-10-20T23:59:59+03:00[Europe/Kiev]
                2024-10-21T23:59:59+03:00[Europe/Kiev]
                2024-10-22T23:59:59+03:00[Europe/Kiev]
                2024-10-23T23:59:59+03:00[Europe/Kiev]
                2024-10-24T23:59:59+03:00[Europe/Kiev]
                2024-10-25T23:59:59+03:00[Europe/Kiev]
                2024-10-26T23:59:59+03:00[Europe/Kiev]
                2024-10-27T22:59:59+02:00[Europe/Kiev]
                2024-10-28T22:59:59+02:00[Europe/Kiev]
                2024-10-29T22:59:59+02:00[Europe/Kiev]
                2024-10-30T22:59:59+02:00[Europe/Kiev]
                2024-10-31T22:59:59+02:00[Europe/Kiev]
                2024-11-01T22:59:59+02:00[Europe/Kiev]
                2024-11-02T22:59:59+02:00[Europe/Kiev]
                2024-11-03T22:59:59+02:00[Europe/Kiev]
                2024-11-04T22:59:59+02:00[Europe/Kiev]
                2024-11-05T22:59:59+02:00[Europe/Kiev]
                2024-11-06T22:59:59+02:00[Europe/Kiev]
                2024-11-07T22:59:59+02:00[Europe/Kiev]
                2024-11-08T22:59:59+02:00[Europe/Kiev]
                2024-11-09T22:59:59+02:00[Europe/Kiev]
                2024-11-10T22:59:59+02:00[Europe/Kiev]
                
                """.trimIndent().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000), ZoneId.of("Europe/Kiev"))
        val date = Date.newInstance(1, 1, 1, 972071999, Once(),
            0, 10950, Day(1), ZoneId.of("Europe/Moscow"), ""
        ).fixEndForTesting()
        for (i in -1..20) {
            val time = date.forDay(utc.plusDays(i.toLong()))
            println(utc.plusDays(i.toLong()))
            if (test[i + 1] != "null") {
                assertNotNull(time)
                println(time!!.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString())
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString())
            } else {
                if (time != null) println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString())
                assertNull(time)
            }
        }
    }

    fun testTZKievFromMoscowMonth() {
        val test = """
                2024-09-20T22:59:59+03:00[Europe/Moscow]
                2024-10-20T22:59:59+03:00[Europe/Moscow]
                2024-11-20T23:59:59+03:00[Europe/Moscow]
                2024-12-20T23:59:59+03:00[Europe/Moscow]
                2025-01-20T23:59:59+03:00[Europe/Moscow]
                2025-02-20T23:59:59+03:00[Europe/Moscow]
                2025-03-20T23:59:59+03:00[Europe/Moscow]
                2025-04-20T22:59:59+03:00[Europe/Moscow]
                2025-05-20T22:59:59+03:00[Europe/Moscow]
                2025-06-20T22:59:59+03:00[Europe/Moscow]
                2025-07-20T22:59:59+03:00[Europe/Moscow]
                2025-08-20T22:59:59+03:00[Europe/Moscow]
                2025-09-20T22:59:59+03:00[Europe/Moscow]
                2025-10-20T22:59:59+03:00[Europe/Moscow]
                2025-11-20T23:59:59+03:00[Europe/Moscow]
                2025-12-20T23:59:59+03:00[Europe/Moscow]
                2026-01-20T23:59:59+03:00[Europe/Moscow]
                2026-02-20T23:59:59+03:00[Europe/Moscow]
                2026-03-20T23:59:59+03:00[Europe/Moscow]
                2026-04-20T22:59:59+03:00[Europe/Moscow]
                2026-05-20T22:59:59+03:00[Europe/Moscow]
                2026-06-20T22:59:59+03:00[Europe/Moscow]
                
                """.trimIndent().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val test2 = """
                287
                288
                289
                290
                291
                292
                293
                294
                295
                296
                297
                298
                299
                300
                301
                302
                303
                304
                305
                306
                307
                308
                
                """.trimIndent().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val utc =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond((1729458000 - 86400).toLong()), ZoneId.of("Europe/Moscow"))
        val date = Date.newInstance(1, 1, 1, 972071999, Once(),
            0, 10950, Month(1), ZoneId.of("Europe/Kiev"), ""
        ).fixEndForTesting()
        for (i in -1..20) {
            val time = date.forDay(utc.plusMonths(i.toLong()))
            println(utc.plusMonths(i.toLong()))
            if (test[i + 1] != "null") {
                assertNotNull(time)
                println(time!!.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
                assertEquals(
                    date.getZDTForTesting(test2[i + 1].toLong()),
                    date.anyInRange(
                        utc.plusMonths(i.toLong()).toEpochSecond(),
                        utc.plusMonths(i.toLong()).plusDays(1).toEpochSecond()
                    )
                )
            } else {
                if (time != null) println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString())
                assertNull(time)
            }
        }
    }

    fun testTZMoscowFromKievMonth() {
        val test = """
                2024-09-20T23:59:59+03:00[Europe/Kiev]
                2024-10-20T23:59:59+03:00[Europe/Kiev]
                2024-11-20T22:59:59+02:00[Europe/Kiev]
                2024-12-20T22:59:59+02:00[Europe/Kiev]
                2025-01-20T22:59:59+02:00[Europe/Kiev]
                2025-02-20T22:59:59+02:00[Europe/Kiev]
                2025-03-20T22:59:59+02:00[Europe/Kiev]
                2025-04-20T23:59:59+03:00[Europe/Kiev]
                2025-05-20T23:59:59+03:00[Europe/Kiev]
                2025-06-20T23:59:59+03:00[Europe/Kiev]
                2025-07-20T23:59:59+03:00[Europe/Kiev]
                2025-08-20T23:59:59+03:00[Europe/Kiev]
                2025-09-20T23:59:59+03:00[Europe/Kiev]
                2025-10-20T23:59:59+03:00[Europe/Kiev]
                2025-11-20T22:59:59+02:00[Europe/Kiev]
                2025-12-20T22:59:59+02:00[Europe/Kiev]
                2026-01-20T22:59:59+02:00[Europe/Kiev]
                2026-02-20T22:59:59+02:00[Europe/Kiev]
                2026-03-20T22:59:59+02:00[Europe/Kiev]
                2026-04-20T23:59:59+03:00[Europe/Kiev]
                2026-05-20T23:59:59+03:00[Europe/Kiev]
                2026-06-20T23:59:59+03:00[Europe/Kiev]
                
                """.trimIndent().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val test2 = """
                287
                288
                289
                290
                291
                292
                293
                294
                295
                296
                297
                298
                299
                300
                301
                302
                303
                304
                305
                306
                307
                308
                
                """.trimIndent().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val utc =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond((1729458000 - 86400).toLong()), ZoneId.of("Europe/Kiev"))
        val date = Date.newInstance(1, 1, 1, 972071999, Once(),
            0, 10950, Month(1), ZoneId.of("Europe/Moscow"), ""
        ).fixEndForTesting()
        for (i in -1..20) {
            val time = date.forDay(utc.plusMonths(i.toLong()))
            println(utc.plusMonths(i.toLong()))
            if (test[i + 1] != "null") {
                assertNotNull(time)
                println(time!!.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString())
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString())
                assertEquals(
                    date.getZDTForTesting(test2[i + 1].toLong()),
                    date.anyInRange(
                        utc.plusMonths(i.toLong()).toEpochSecond(),
                        utc.plusMonths(i.toLong()).plusDays(1).toEpochSecond()
                    )
                )
            } else {
                if (time != null) println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString())
                assertNull(time)
            }
        }
    }
}