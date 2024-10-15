package oblitusnumen.calendar.ui.model;

import oblitusnumen.calendar.implementation.data.CalendarDate;

import java.time.LocalDate;
import java.util.List;

public class DateScreen extends Screen {
    public final LocalDate date;
    public final List<CalendarDate> dates;

    public DateScreen(LocalDate date, List<CalendarDate> dates) {
        this.date = date;
        this.dates = dates;
    }
}
