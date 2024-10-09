package oblitusnumen.calendar.implementation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CalendarDate {
    List<Date> dates = new ArrayList<>();

    public CalendarDate(Date date) {
        this.dates.add(date);
    }

    public CalendarDate() {
    }

    public boolean hasDates() {
        return !dates.isEmpty();
    }
}
