package oblitusnumen.calendar.implementation.data;

import junit.framework.TestCase;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DateTest extends TestCase {
    public void testRemoved() {
        int numberOfDated = 50;
        Date.Removed none = Date.Removed.none(new Date(null, 0, 0, "", 0, 0, 0, numberOfDated, Date.Period.none().toString(), "UTC", ""));
        assertTrue(none.hasAny());
        Set<Integer> integers = new HashSet<>();
        Random random = new Random(9182746398213794L);
        for (int i = 0; i < numberOfDated * 3 / 4; i++) {
            integers.add(random.nextInt(numberOfDated));
        }
        for (Integer integer : integers) {
            System.out.println(integer);
            none.rmIndex(integer);
        }
        System.out.println(none);
        none = new Date.Removed(new Date(null, 0, 0, "", 0, 0, 0, numberOfDated, Date.Period.none().toString(), "UTC", ""), none.toString());
        assertTrue(none.hasAny());
        Date.Removed finalNone = none;
        integers.removeIf(integer -> {
            if (random.nextBoolean()) {
                finalNone.addIndex(integer);
                return true;
            }
            return false;
        });
        assertTrue(none.hasAny());
        none = new Date.Removed(new Date(null, 0, 0, "", 0, 0, 0, numberOfDated, Date.Period.none().toString(), "UTC", ""), none.toString());
        assertTrue(none.hasAny());
        System.out.println(none);
        boolean isValid = true;
        for (int i = 0; i < numberOfDated; i++) {
//            System.out.println(i + ":" + none.isPresent(i));
            isValid &= none.isPresent(i) != integers.contains(i);
        }
        assertTrue(none.hasAny());
        assertTrue(isValid);
        System.out.println(none);
        for (int i = 0; i < numberOfDated; i++) {
            none.rmIndex(i);
        }
        System.out.println(none);
        assertFalse(none.hasAny());
        for (int i = 0; i < numberOfDated; i++) {
            none.addIndexes(0, -1);
        }
        System.out.println(none);
        assertTrue(none.hasAny());
        none.rmIndexes(0, -1);
        assertFalse(none.hasAny());
        for (int i = 0; i < numberOfDated; i++) {
            none.addIndexes(0, -1);
        }
        assertTrue(none.hasAny());
        none.rmIndexes(0, numberOfDated);
        assertFalse(none.hasAny());
    }

    public void testRemoveIndexes() {
        int numberOfDated = 500;
        Date.Removed none = Date.Removed.none(new Date(null, 0, 0, "", 0, 0, 0, numberOfDated, Date.Period.none().toString(), "UTC", ""));
        assertEquals("", none.toString());
        assertTrue(none.hasAny());
        none.rmIndexes(300, 600);
        assertEquals("300-,", none.toString());
        assertTrue(none.hasAny());
        none.rmIndexes(200, 450);
        assertEquals("200-,", none.toString());
        assertTrue(none.hasAny());
        none.rmIndexes(100, -1);
        assertEquals("100-,", none.toString());
        assertTrue(none.hasAny());
        none.rmIndexes(150, -1);
        assertEquals("100-,", none.toString());
        assertTrue(none.hasAny());
        none.rmIndexes(0, -1);
        assertEquals("0-,", none.toString());
        assertFalse(none.hasAny());
        none.addIndexes(50, 100);
        assertEquals("0-49,100-,", none.toString());
        assertTrue(none.hasAny());
        none.addIndex(150);
        assertEquals("0-49,100-149,151-,", none.toString());
        assertTrue(none.hasAny());
        none.addIndex(numberOfDated - 1);
        assertEquals("0-49,100-149,151-498,", none.toString());
        none.rmIndexes(50, 150);
        assertEquals("0-498,", none.toString());
        none.addIndex(50);
        assertEquals("0-49,51-498,", none.toString());
        none.addIndex(100);
        assertEquals("0-49,51-99,101-498,", none.toString());
        none.addIndex(150);
        assertEquals("0-49,51-99,101-149,151-498,", none.toString());
        none.addIndex(200);
        assertEquals("0-49,51-99,101-149,151-199,201-498,", none.toString());
        none.addIndexes(99,202);
        assertEquals("0-49,51-498,", none.toString());
        none.rmIndex(499);
        assertEquals("0-49,51-,", none.toString());
        none.addIndexes(0, -1);
        assertEquals("", none.toString());
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
}