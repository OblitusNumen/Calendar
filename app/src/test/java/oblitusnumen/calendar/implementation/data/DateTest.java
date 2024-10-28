package oblitusnumen.calendar.implementation.data;

import junit.framework.TestCase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DateTest extends TestCase {
    public void testRemoved() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
        Set<Integer> integers = new HashSet<>();
        Random random = new Random(9182746398213794L);
        for (int i = 0; i < numberOfDated * 3 / 4; i++) {
            integers.add(random.nextInt(numberOfDated));
        }
        for (Integer integer : integers) {
            date.removeEvent(integer);
        }
        System.out.println(date.exceptionRules.toString());
        date.exceptionRules = new Date.ExceptionRules(date, date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
        integers.removeIf(integer -> {
            if (random.nextBoolean()) {
                date.addEvent(integer);
//                System.out.println("integer" + integer);
//                System.out.println("rules" + date.exceptionRules);
                return true;
            }
            return false;
        });
        assertFalse(date.isEmpty());
        date.exceptionRules = new Date.ExceptionRules(date, date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
        boolean isValid = true;
        for (int i = 0; i < numberOfDated; i++) {
            isValid &= date.exceptionRules.isEventPresent(i) != integers.contains(i);
        }
        System.out.println(integers);
        assertTrue(isValid);
        assertFalse(date.isEndless());
        System.out.println(date.exceptionRules);
        for (int i = 0; i < numberOfDated; i++) {
            date.removeEvent(i);
            System.out.println("integer" + i);
            System.out.println("rules" + date.exceptionRules);
        }
        System.out.println(date.timesRepeat);
        assertTrue(date.isEmpty());
        for (int i = 0; i < numberOfDated; i++) {
            date.addEvent(i);
        }
        assertFalse(date.isEmpty());
        date.removeEvents(0, Integer.MAX_VALUE);
        assertEquals(0, date.getTimesRepeat());
        assertFalse(date.isEndless());
        assertTrue(date.isEmpty());
        date.addEvents(0, Integer.MAX_VALUE);
        assertEquals(-1, date.getTimesRepeat());
        assertTrue(date.isEndless());
        for (int i = 0; i < numberOfDated; i++) {
            date.addEvent(i);
        }
        assertFalse(date.isEmpty());
        assertTrue(date.isEndless());
        date.cropToTimesRepeat(numberOfDated);
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
    }

    public void testRemoveIndexes() {
        int numberOfDated = 500;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        assertEquals("", date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        date.removeEvents(300, 600);
        assertEquals(300, date.timesRepeat);
        assertEquals("", date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        System.out.println(date.exceptionRules);
        date.removeEvents(200, 450);
        assertEquals(200, date.timesRepeat);
        assertEquals("", date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        date.cropToTimesRepeat(100);
        assertEquals(100, date.timesRepeat);
        assertEquals("", date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        date.removeEvents(150, 160);
        assertEquals(100, date.timesRepeat);
        assertEquals("", date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        date.cropToTimesRepeat(0);
        assertEquals(0, date.timesRepeat);
        assertEquals("", date.exceptionRules.toString());
        assertTrue(date.isEmpty());
        date.addEvents(50, 100);
        assertEquals(100, date.timesRepeat);
        assertEquals("0-50,", date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        date.addEvent(150);
        assertEquals(151, date.timesRepeat);
        assertEquals("0-50,100-150,", date.exceptionRules.toString());
        assertFalse(date.isEmpty());
        date.addEvent(numberOfDated - 1);
        assertEquals(500, date.timesRepeat);
        assertEquals("0-50,100-150,151-499,", date.exceptionRules.toString());
        date.removeEvents(50, 150);
        assertEquals(500, date.timesRepeat);
        assertEquals("0-150,151-499,", date.exceptionRules.toString());
        date.addEvent(50);
        assertEquals(500, date.timesRepeat);
        assertEquals("0-50,51-150,151-499,", date.exceptionRules.toString());
        date.addEvent(100);
        assertEquals(500, date.timesRepeat);
        assertEquals("0-50,51-100,101-150,151-499,", date.exceptionRules.toString());
        date.removeEvent(150);
        assertEquals(500, date.timesRepeat);
        assertEquals("0-50,51-100,101-499,", date.exceptionRules.toString());
        date.addEvent(150);
        date.addEvent(200);
        assertEquals(500, date.timesRepeat);
        assertEquals("0-50,51-100,101-150,151-200,201-499,", date.exceptionRules.toString());
        date.addEvents(99,202);
        assertEquals(500, date.timesRepeat);
        assertEquals("0-50,51-99,202-499,", date.exceptionRules.toString());
        date.removeEvent(499);
        assertEquals(202, date.timesRepeat);
        assertEquals("0-50,51-99,", date.exceptionRules.toString());
        date.removeEvents(50, 202);
        assertEquals(0, date.timesRepeat);
        assertEquals("", date.exceptionRules.toString());
        assertTrue(date.isEmpty());
    }

    public void testCropToTimesRepeat() {
        int numberOfDated = 500;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        assertEquals("", date.exceptionRules.toString());
        date.cropToTimesRepeat(50);
        assertEquals(50, date.getTimesRepeat());
        assertEquals(50, date.timesRepeat);
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
        assertEquals("", date.exceptionRules.toString());
        date.makeEndless();
        assertEquals(-1, date.getTimesRepeat());
        assertFalse(date.isEmpty());
        assertTrue(date.isEndless());
        assertEquals("", date.exceptionRules.toString());
        date.cropToTimesRepeat(50);
        assertEquals(50, date.timesRepeat);
        assertFalse(date.isEmpty());
        assertFalse(date.isEndless());
        assertEquals("", date.exceptionRules.toString());

    }

    public void testForDayBefore() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, Date.Period.none().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1969, 12, 30, 23, 59, 59, 0, ZoneId.of("UTC")));
        assertNull(time);
    }

    public void testForDayAfter() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, Date.Period.none().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNull(time);
    }

    public void testForDayAt() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, Date.Period.none().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
    }

    public void testForDayAtPeriod() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")).plusDays(5));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 1, 0, ZoneId.of("UTC")).plusDays(9));
        assertNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(5));
        assertNotNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(9));
        assertNotNull(time);
    }

    public void testForDayBetweenPeriod() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusDays(25));
        assertNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.DAY, 2).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 11, 0, 0, 0, ZoneId.of("UTC")).plusHours(11));
        assertNull(time);
    }

    public void testForDayAfterPeriod() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusMonths(10));
        assertNull(time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")).plusDays(10));
        assertNull(time);
    }

    public void testTestCropToTimesRepeat() {
        int numberOfDated = 10;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.cropToTimesRepeat(50);
        assertEquals(50, date.timesRepeat);
        assertEquals("", date.exceptionRules.toString());
    }

    public void testEndEqualsTo() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.removeEvents(5, 23);
        assertEquals(50, date.timesRepeat);
        assertEquals("5-23,", date.exceptionRules.toString());
        date.removeEvents(20, 23);
        assertEquals(50, date.timesRepeat);
        assertEquals("5-23,", date.exceptionRules.toString());
    }

    public void testEndEqualsFrom() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.removeEvents(5, 23);
        assertEquals(50, date.timesRepeat);
        assertEquals("5-23,", date.exceptionRules.toString());
        date.removeEvents(23, 27);
        assertEquals(50, date.timesRepeat);
        assertEquals("5-27,", date.exceptionRules.toString());
    }

    public void testEndMoreThanFrom() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.removeEvents(5, 23);
        assertEquals(50, date.timesRepeat);
        assertEquals("5-23,", date.exceptionRules.toString());
        date.removeEvents(20, 27);
        assertEquals(50, date.timesRepeat);
        assertEquals("5-27,", date.exceptionRules.toString());
    }

    public void testRemoveAll() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.removeEvents(0, 50);
        assertEquals(0, date.timesRepeat);
        assertTrue(date.isEmpty());
        assertEquals("", date.exceptionRules.toString());
        date.addEvents(49, 59);
        date.addEvents(23, 30);
        date.addEvents(69, 90);
        assertEquals(90, date.timesRepeat);
        assertFalse(date.isEmpty());
        assertEquals("0-23,30-49,59-69,", date.exceptionRules.toString());
        date.removeEvents(0, 90);
        assertEquals(0, date.timesRepeat);
        assertTrue(date.isEmpty());
        assertEquals("", date.exceptionRules.toString());
    }

    public void testAddIntersecting() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.removeEvents(20, 30);
        assertEquals(50, date.timesRepeat);
        assertEquals("20-30,", date.exceptionRules.toString());
        date.addEvents(23, 60);
        assertEquals(60, date.timesRepeat);
        assertEquals("20-23,", date.exceptionRules.toString());
    }

    public void testAddNotIntersecting() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.removeEvents(20, 30);
        assertEquals(50, date.timesRepeat);
        assertEquals("20-30,", date.exceptionRules.toString());
        date.addEvents(39, 40);
        assertEquals(50, date.timesRepeat);
        assertEquals("20-30,", date.exceptionRules.toString());
    }

    public void testRmNotIntersecting() {
        int numberOfDated = 50;
        Date date = new Date(null, 0, 0, "", 0, 0, 0, numberOfDated,
                new Date.Period(Date.Period.Modifier.WEEK, 1).toString(), "UTC", "");
        date.removeEvents(5, 10);
        assertEquals(50, date.timesRepeat);
        assertEquals("5-10,", date.exceptionRules.toString());
        date.removeEvents(30, 60);
        assertEquals(30, date.timesRepeat);
        assertEquals("5-10,", date.exceptionRules.toString());
    }

    public void testForDayIndexAt() {
        ZonedDateTime time = new Date(null, 0, 0, "", 0, 0, 0, 100, Date.Period.none().toString(), "UTC", "")
                .forDay(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertNotNull(time);
    }

    public void testForDayIndexAtPeriod() {
        ZonedDateTime utc = ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC"));
        int time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "UTC", "")
                .getZonedDateTimeIndex(utc.toEpochSecond(), utc.toEpochSecond()+86400);
        assertEquals(1, time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "UTC", "")
                .getZonedDateTimeIndex(utc.plusDays(5).toEpochSecond(), utc.plusDays(6).toEpochSecond());
        assertEquals(6, time);
        utc = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .getZonedDateTimeIndex(utc.toEpochSecond(), utc.plusDays(1).toEpochSecond());
        assertEquals(0, time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .getZonedDateTimeIndex(utc.plusMonths(5).toEpochSecond(), utc.plusMonths(5).plusDays(1).toEpochSecond());
        assertEquals(5, time);
        time = new Date(null, 0, 0, "", 0, 0, 0, 10, new Date.Period(Date.Period.Modifier.MONTH, 1).toString(), "UTC", "")
                .getZonedDateTimeIndex(utc.plusMonths(9).toEpochSecond(), utc.plusMonths(9).plusDays(1).toEpochSecond());
        assertEquals(9, time);
    }

    public void testTZKievFromMoscow() {
        String[] test = """
            null
            2024-10-20T23:59:59+03:00[Europe/Moscow]
            2024-10-21T23:59:59+03:00[Europe/Moscow]
            2024-10-22T23:59:59+03:00[Europe/Moscow]
            2024-10-23T23:59:59+03:00[Europe/Moscow]
            2024-10-24T23:59:59+03:00[Europe/Moscow]
            2024-10-25T23:59:59+03:00[Europe/Moscow]
            2024-10-26T23:59:59+03:00[Europe/Moscow]
            null
            2024-10-28T00:59:59+03:00[Europe/Moscow]
            2024-10-29T00:59:59+03:00[Europe/Moscow]
            2024-10-30T00:59:59+03:00[Europe/Moscow]
            2024-10-31T00:59:59+03:00[Europe/Moscow]
            2024-11-01T00:59:59+03:00[Europe/Moscow]
            2024-11-02T00:59:59+03:00[Europe/Moscow]
            2024-11-03T00:59:59+03:00[Europe/Moscow]
            2024-11-04T00:59:59+03:00[Europe/Moscow]
            2024-11-05T00:59:59+03:00[Europe/Moscow]
            2024-11-06T00:59:59+03:00[Europe/Moscow]
            2024-11-07T00:59:59+03:00[Europe/Moscow]
            2024-11-08T00:59:59+03:00[Europe/Moscow]
            2024-11-09T00:59:59+03:00[Europe/Moscow]
            """.split("\n");
        String[] test2 = """
                0
                0
                1
                2
                3
                4
                5
                6
                -1
                7
                8
                9
                10
                11
                12
                13
                14
                15
                16
                17
                18
                19
                """.split("\n");
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000-86400), ZoneId.of("Europe/Moscow"));
        Date date = new Date(null, 0, 0, "", 972071999, 0, 0, 10950,
                new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "Europe/Kiev", "");
        for (int i = -1; i < 21; i++) {
            ZonedDateTime time = date.forDay(utc.plusDays(i));
            System.out.println(utc.plusDays(i));
            if (!test[i+1].equals("null")) {
                System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
                assertNotNull(time);
                assertEquals(test[i+1], time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
//                assertEquals(test2[i+1], date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()) + "");
            } else {
                if (time!= null) System.out.println(time.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toString());
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

    public void testTZMoscowFromKiev() {
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
            null
            null
            """.split("\n");
        String[] test2 = """
                0
                1
                2
                3
                4
                5
                6
                7
                8
                9
                10
                11
                12
                13
                14
                15
                16
                17
                18
                19
                -1
                -1
                """.split("\n");
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1729458000), ZoneId.of("Europe/Kiev"));
        Date date = new Date(null, 0, 0, "", 972071999, 0, 0, 10950,
                new Date.Period(Date.Period.Modifier.DAY, 1).toString(), "Europe/Moscow", "");
        for (int i = -1; i < 21; i++) {
            ZonedDateTime time = date.forDay(utc.plusDays(i));
            if (!test[i+1].equals("null")) {
                assertNotNull(time);
                assertEquals(test[i+1], time.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toString());
//                assertEquals(test2[i+1], date.getZonedDateTimeIndex(utc.plusDays(i).toEpochSecond(), utc.plusDays(i+1).toEpochSecond()) + "");
            } else assertNull(time);
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
}