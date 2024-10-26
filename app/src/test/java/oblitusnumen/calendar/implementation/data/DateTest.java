package oblitusnumen.calendar.implementation.data;

import junit.framework.TestCase;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DateTest extends TestCase {
    public void testRemoved() {
        int numberOfDated = 5;
        Date.Removed none = Date.Removed.none(new Date(null, 0, 0, "", 0, 0, 0, numberOfDated, Date.Period.none().toString(), "UTC", ""));
        Set<Integer> integers = new HashSet<>();
        Random random = new Random();
        for (int i = 0; i < numberOfDated * 3 / 4; i++) {
            integers.add(random.nextInt(numberOfDated));
        }
        for (Integer integer : integers) {
            none.rmIndex(integer);
        }
        Date.Removed finalNone = none;
        integers.removeIf(integer -> {
            if (random.nextBoolean()) {
                finalNone.addIndex(integer);
                return true;
            }
            return false;
        });
        none = new Date.Removed(new Date(null, 0, 0, "", 0, 0, 0, numberOfDated, Date.Period.none().toString(), "UTC", ""), none.toString());
        System.out.println(none);
        boolean isValid = true;
        for (int i = 0; i < numberOfDated; i++) {
            System.out.println(i + ":" + none.isPresent(i));
            isValid &= none.isPresent(i) != integers.contains(i);
        }
        assertTrue(isValid);
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