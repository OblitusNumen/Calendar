package oblitusnumen.calendar.implementation.data;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class CalendarDates implements Serializable, Iterable<CalendarDate> {
    @Serial
    private static final long serialVersionUID = 1;
    protected final ArrayList<CalendarDate> list = new ArrayList<>();

    CalendarDates() {
    }

    CalendarDates(Collection<? extends CalendarDate> initial) {
        addAll(initial);
    }

    public CalendarDates(CalendarDates calendarDates) {
        list.addAll(calendarDates.list);
    }

    void addAll(CalendarDates calendarDates) {
        list.addAll(calendarDates.list);
        sort();
    }

    void addAll(Collection<? extends CalendarDate> dates) {
        list.addAll(dates);
        sort();
    }

    private void sort() {
        list.sort(Comparator.comparing(d -> d.date));
    }

    public void add(CalendarDate el) {
        list.add(findAdditionIndex(el), el);
    }

    public CalendarDates withEntry(UUID entry) {
        CalendarDates result = new CalendarDates();
        for (CalendarDate date : list) {
            if (date.entry.equals(entry)) result.list.add(date);
        }
        return result;
    }

    public CalendarDates between(LocalDate start, LocalDate end) {
        return between(start.atStartOfDay(), end.atStartOfDay());
    }

    public CalendarDates between(LocalDateTime start, LocalDateTime end) {
        CalendarDates calendarDates = new CalendarDates();
        calendarDates.list.addAll(list.subList(findIndex(start), findIndex(end)));
        return calendarDates;
    }

    int findAdditionIndex(CalendarDate el) {
        return findIndex(el.date);
    }

    int findIndex(LocalDateTime date) {
        int begin = 0;
        int end = list.size();
        while (true) {
            int center = (begin + end) / 2;
            if (center == end) return center;
            if (list.get(center).date.isAfter(date)) {
                end = center;
            } else {
                if (begin == center) return end;
                begin = center;
            }
        }
    }

    @Override
    public @NotNull String toString() {
        return list.toString();
    }

    @NonNull
    @Override
    public @NotNull CalendarDates clone() {
        return new CalendarDates(this);
    }

    @NonNull
    @Override
    public @NotNull Iterator<CalendarDate> iterator() {
        return list.iterator();
    }
}
