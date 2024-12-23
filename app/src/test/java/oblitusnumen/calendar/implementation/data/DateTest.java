package oblitusnumen.calendar.implementation.data;

import junit.framework.TestCase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTest extends TestCase {
    public void testDateExceptionRules() {
        ExceptionRules exceptionRules = new ExceptionRules();
        assertEquals(0, exceptionRules.exceptionDatesCount());
        assertFalse(exceptionRules.containsDate(0));
        assertEquals(exceptionRules.toString(), "");
        exceptionRules = new ExceptionRules("0_2,4,6_10,12,14");
        assertFalse(exceptionRules.containsDate(3));
        assertTrue(exceptionRules.containsDate(4));
        assertTrue(exceptionRules.containsDate(6));
        assertTrue(exceptionRules.containsDate(7));
        assertTrue(exceptionRules.containsDate(10));
        assertEquals(11, exceptionRules.exceptionDatesCount());
        exceptionRules.addDate(11);
        assertEquals(12, exceptionRules.exceptionDatesCount());
        assertEquals("0_2,4,6_12,14", exceptionRules.toString());
        exceptionRules.addDates(3, 13);
        assertEquals("0_14", exceptionRules.toString());
        exceptionRules = new ExceptionRules("5_8,12,14");
        exceptionRules.addDates(0, 4);
        assertEquals("0_8,12,14", exceptionRules.toString());
        exceptionRules.addDates(17, 18);
        assertEquals("0_8,12,14,17_18", exceptionRules.toString());
        exceptionRules = new ExceptionRules("5_8,12,14");
        exceptionRules.addDates(0, 20);
        assertEquals("0_20", exceptionRules.toString());
        exceptionRules.addDates(0, 20);
        assertEquals("0_20", exceptionRules.toString());
        exceptionRules = new ExceptionRules("5_8,12,14");
        exceptionRules.addDates(1, 2);
        assertEquals("1_2,5_8,12,14", exceptionRules.toString());
        exceptionRules.removeDates(8, 12);
        assertEquals("1_2,5_7,14", exceptionRules.toString());
        exceptionRules.removeDates(3, 6);
        assertEquals("1_2,7,14", exceptionRules.toString());
        exceptionRules.removeDates(0, 1);
        assertEquals("2,7,14", exceptionRules.toString());
        exceptionRules.removeDates(0, 13);
        assertEquals("14", exceptionRules.toString());
        exceptionRules = new ExceptionRules("5_8,12,14_18");
        exceptionRules.removeDate(6);
        assertEquals("5,7_8,12,14_18", exceptionRules.toString());
        exceptionRules.removeDates(15, 16);
        assertEquals("5,7_8,12,14,17_18", exceptionRules.toString());
        exceptionRules.removeDates(19, 30);
        assertEquals("5,7_8,12,14,17_18", exceptionRules.toString());
        exceptionRules.trimToFitRange(8, 17);
        assertEquals("8,12,14,17", exceptionRules.toString());
        exceptionRules = new ExceptionRules();
        exceptionRules.addDates(-15, -5);
        exceptionRules.addDate(-3);
        assertEquals("-15_-5,-3", exceptionRules.toString());
        exceptionRules = new ExceptionRules("-10_-9,-8,-3_0,2");
        assertEquals("-10_-9,-8,-3_0,2", exceptionRules.toString());
    }

    public void testSetTimesRepeat() {
        int numberOfDated = 500;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Period(Period.WEEK, 1).toString(), "UTC", "");
        assertEquals("", date.getExceptionRules().toString());
        date.setTimesRepeat(50);
        assertEquals(50, date.getTimesRepeat());
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
        date.makeEndless();
        assertFalse(date.isEmpty());
        assertTrue(date.isEndless());
        date.setTimesRepeat(50);
        assertEquals(50, date.getTimesRepeat());
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
    }

    public void testForDayBefore() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, new Period().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1969, 12, 30, 23, 59, 59, 0, ZoneId.of("UTC")));
        assertNull(time);
    }

    public void testForDayAfter() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, new Period().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNull(time);
    }

    public void testForDayAt() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, new Period().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
    }

    public void testForDayAtPeriod() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.DAY, 1).toString(), "UTC", "")
                .fixEnd().forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.DAY, 1).toString(), "UTC", "")
                .fixEnd().forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")).plusDays(5));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.DAY, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 1, 0, ZoneId.of("UTC")).plusDays(9));
        assertNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .fixEnd().forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(5));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .fixEnd().forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(9));
        assertNotNull(time);
    }

    public void testForDayBetweenPeriod() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusDays(25));
        assertNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.DAY, 2).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 11, 0, 0, 0, ZoneId.of("UTC")).plusHours(11));
        assertNull(time);
    }

    public void testForDayAfterPeriod() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(10));
        assertNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.DAY, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")).plusDays(10));
        assertNull(time);
    }

    public void testFixExceptionList() {
        Date date = new Date(null, 0, 0, "", 0, 0, 0, 80,
                new Period(Period.DAY, 1).toString(), "UTC", "").fixEnd();
        date.addExceptions(date.getZoneDateTime(1).toLocalDate(), date.getZoneDateTime(100).toLocalDate());
        assertEquals(80, date.getTimesRepeat());
        date.fixExceptionList();
        assertEquals("1_79", date.getExceptionRules().toString());
        date.setTimesRepeat(50);
        assertEquals(50, date.getTimesRepeat());
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
        date.fixExceptionList();
        assertEquals("1_49", date.getExceptionRules().toString());
    }

    public void testExceptionsFromGetZonedDTI() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Period(Period.WEEK, 1).toString(), "UTC", "");
        date.addExceptions(date.getZoneDateTime(5).toLocalDate(), date.getZoneDateTime(23).toLocalDate());
        assertEquals(50, date.getTimesRepeat());
        assertEquals("35_161", date.getExceptionRules().toString());
        date.addExceptions(date.getZoneDateTime(20).toLocalDate(), date.getZoneDateTime(23).toLocalDate());
        assertEquals(50, date.getTimesRepeat());
        assertEquals("35_161", date.getExceptionRules().toString());
        date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Period(Period.MONTH, 1).toString(), "UTC", "");
        date.addExceptions(date.getZoneDateTime(3).toLocalDate(), date.getZoneDateTime(12).toLocalDate());
        assertEquals("90_365", date.getExceptionRules().toString());
    }

    public void testFixRanges() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Period(Period.WEEK, 1).toString(), "UTC", "").fixEnd();
        date.addExceptions(date.getZoneDateTime(0).toLocalDate(), date.getZoneDateTime(50).toLocalDate());
        date.fixDateRange();
        date.fixExceptionList();
        assertEquals(0, date.getTimesRepeat());
        assertTrue(date.isEmpty());
        assertEquals("", date.getExceptionRules().toString());
        date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Period(Period.WEEK, 1).toString(), "UTC", "").fixEnd();
        date.addExceptions(date.getZoneDateTime(0).toLocalDate(), date.getZoneDateTime(4).toLocalDate());
        date.addExceptions(date.getZoneDateTime(40).toLocalDate(), date.getZoneDateTime(49).toLocalDate());
        date.addExceptions(date.getZoneDateTime(21).toLocalDate(), date.getZoneDateTime(24).toLocalDate());
        assertEquals(50, date.getTimesRepeat());
        date.fixDateRange();
        assertEquals(35, date.getTimesRepeat());
        date.fixDateRange();
        assertEquals(35, date.getTimesRepeat());
        assertFalse(date.isEmpty());
        date.fixExceptionList();
        assertEquals("147_168", date.getExceptionRules().toString());
        date.addExceptions(date.getZoneDateTime(0).toLocalDate(), date.getZoneDateTime(34).toLocalDate());
        date.fixDateRange();
        assertTrue(date.isEmpty());
    }

    public void testForDayIndexAt() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, new Period().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
    }

    public void testForDayIndexAtPeriod() {
        ZonedDateTime utc = ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC"));
        long time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.DAY, 1).toString(), "UTC", "")
                .fixEnd().getZonedDateTimeIndex(utc.toEpochSecond(), utc.toEpochSecond() + 86400);
        assertEquals(1, time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.DAY, 1).toString(), "UTC", "")
                .fixEnd().getZonedDateTimeIndex(utc.plusDays(5).toEpochSecond(), utc.plusDays(6).toEpochSecond());
        assertEquals(6, time);
        utc = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .getZonedDateTimeIndex(utc.toEpochSecond(), utc.plusDays(1).toEpochSecond());
        assertEquals(0, time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .fixEnd().getZonedDateTimeIndex(utc.plusMonths(5).toEpochSecond(), utc.plusMonths(5).plusDays(1).toEpochSecond());
        assertEquals(5, time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Period(Period.MONTH, 1).toString(), "UTC", "")
                .fixEnd().getZonedDateTimeIndex(utc.plusMonths(9).toEpochSecond(), utc.plusMonths(9).plusDays(1).toEpochSecond());
        assertEquals(9, time);
    }

    public void testTZKievFromMoscowDay() {
        String[] test = """
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
                """.split("\n");
        String[] test2 = """
                8765
                8766
                8767
                8768
                8769
                8770
                8771
                8772
                8773
                8774
                8775
                8776
                8777
                8778
                8779
                8780
                8781
                8782
                8783
                8784
                8785
                8786
                """.split("\n");
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000 - 86400), ZoneId.of("Europe/Moscow"));
        Date date = new Date(null, 0, 0, "", 972071999, 0, 0, 10950,
                new Period(Period.DAY, 1).toString(), "Europe/Kiev", "").fixEnd();
        for (int i = -1; i < 21; i++) {
            ZonedDateTime time = date.forDay(utc.plusDays(i));
            System.out.println(utc.plusDays(i));
            if (!test[i + 1].equals("null")) {
                assertNotNull(time);
                System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
//                assertEquals(test2[i+1], date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()) + "");
            } else {
                if (time != null) System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertNull(time);
            }
        }
//        for (int i = -1; i < 21; i++) {
//            ZonedDateTime time = date.forDay(utc.plusDays(i));
//            if (time == null) {
//                System.out.println("null");
//                continue;
//            }
//            System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
//        }
//        for (int i = -1; i < 21; i++) {
//            System.out.println(date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()));
//        }
    }

    public void testTZKievFromMoscowDayCount3() {
        String[] test = """
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
                """.split("\n");
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000 - 86400), ZoneId.of("Europe/Moscow"));
        Date date = new Date(null, 0, 0, "", 972071999, 0, 0, 10950,
                new Period(Period.DAY, 3).toString(), "Europe/Kiev", "").fixEnd();
        for (int i = -1; i < 21; i++) {
            ZonedDateTime time = date.forDay(utc.plusDays(i));
            System.out.println(utc.plusDays(i));
            if (!test[i + 1].equals("null")) {
                assertNotNull(time);
                System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
//                assertEquals(test2[i+1], date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()) + "");
            } else {
                if (time != null) System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertNull(time);
            }
        }
//        for (int i = -1; i < 21; i++) {
//            ZonedDateTime time = date.forDay(utc.plusDays(i));
//            if (time == null) {
//                System.out.println("null");
//                continue;
//            }
//            System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
//        }
//        for (int i = -1; i < 21; i++) {
//            System.out.println(date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()));
//        }
    }

    public void testTZMoscowFromKievDay() {
        String[] test = """
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
                """.split("\n");
        String[] test2 = """
                8766
                8767
                8768
                8769
                8770
                8771
                8772
                8773
                8774
                8775
                8776
                8777
                8778
                8779
                8780
                8781
                8782
                8783
                8784
                8785
                8786
                8787
                """.split("\n");
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000), ZoneId.of("Europe/Kiev"));
        Date date = new Date(null, 0, 0, "", 972071999, 0, 0, 10950,
                new Period(Period.DAY, 1).toString(), "Europe/Moscow", "").fixEnd();
        for (int i = -1; i < 21; i++) {
            ZonedDateTime time = date.forDay(utc.plusDays(i));
            System.out.println(utc.plusDays(i));
            if (!test[i + 1].equals("null")) {
                assertNotNull(time);
                System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
//                assertEquals(test2[i+1], date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()) + "");
            } else {
                if (time != null) System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
                assertNull(time);
            }
        }
//        for (int i = -1; i < 21; i++) {
//            ZonedDateTime time = date.forDay(utc.plusDays(i));
//            if (time == null) {
//                System.out.println("null");
//                continue;
//            }
//            System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
//        }
//        for (int i = -1; i < 21; i++) {
//            System.out.println(date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()));
//        }
    }

    public void testTZKievFromMoscowMonth() {
        String[] test = """
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
                """.split("\n");
        String[] test2 = """
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
                """.split("\n");
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000 - 86400), ZoneId.of("Europe/Moscow"));
        Date date = new Date(null, 0, 0, "", 972071999, 0, 0, 10950,
                new Period(Period.MONTH, 1).toString(), "Europe/Kiev", "").fixEnd();
        for (int i = -1; i < 21; i++) {
            ZonedDateTime time = date.forDay(utc.plusMonths(i));
            System.out.println(utc.plusMonths(i));
            if (!test[i + 1].equals("null")) {
                assertNotNull(time);
                System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertEquals(test2[i + 1], date.getZonedDateTimeIndex(utc.plusMonths(i).toEpochSecond(), utc.plusMonths(i).plusDays(1).toEpochSecond()) + "");
            } else {
                if (time != null) System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertNull(time);
            }
        }
//        for (int i = -1; i < 21; i++) {
//            ZonedDateTime time = date.forDay(utc.plusMonths(i));
//            if (time == null) {
//                System.out.println("null");
//                continue;
//            }
//            System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
//        }
//        for (int i = -1; i < 21; i++) {
//            System.out.println(date.getZonedDateTimeIndex(utc.plusMonths(i).toEpochSecond(), utc.plusMonths(i).plusDays(1).toEpochSecond()));
//        }
    }

    public void testTZMoscowFromKievMonth() {
        String[] test = """
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
                """.split("\n");
        String[] test2 = """
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
                """.split("\n");
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000 - 86400), ZoneId.of("Europe/Kiev"));
        Date date = new Date(null, 0, 0, "", 972071999, 0, 0, 10950,
                new Period(Period.MONTH, 1).toString(), "Europe/Moscow", "").fixEnd();
        for (int i = -1; i < 21; i++) {
            ZonedDateTime time = date.forDay(utc.plusMonths(i));
            System.out.println(utc.plusMonths(i));
            if (!test[i + 1].equals("null")) {
                assertNotNull(time);
                System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
                assertEquals(test[i + 1], time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
                assertEquals(test2[i + 1], date.getZonedDateTimeIndex(utc.plusMonths(i).toEpochSecond(), utc.plusMonths(i).plusDays(1).toEpochSecond()) + "");
            } else {
                if (time != null) System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
                assertNull(time);
            }
        }
//        for (int i = -1; i < 21; i++) {
//            ZonedDateTime time = date.forDay(utc.plusMonths(i));
//            if (time == null) {
//                System.out.println("null");
//                continue;
//            }
//            System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
//        }
//        for (int i = -1; i < 21; i++) {
//            System.out.println(date.getZonedDateTimeIndex(utc.plusMonths(i).toEpochSecond(), utc.plusMonths(i).plusDays(1).toEpochSecond()));
//        }
    }
}