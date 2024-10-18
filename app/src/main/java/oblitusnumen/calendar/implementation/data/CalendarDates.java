package oblitusnumen.calendar.implementation.data;

import java.io.Serial;
import java.io.Serializable;

public class CalendarDates extends SortedList<CalendarDate> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public CalendarDates() {
        super(new CalendarDate.DateComparator());
    }
}
