package oblitusnumen.calendar.implementation.data;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

public class CalendarDate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;
    public final UUID uid = UUID.randomUUID();
    final LocalDateTime date;
    final Entry entry;
    private String desc;

    public CalendarDate(LocalDateTime date, Entry entry) {
        this(date, "", entry);
    }

    public CalendarDate(LocalDateTime date, String desc, Entry entry) {
        this.date = date;
        this.entry = entry;
        this.desc = desc;
    }

    public CalendarDate(CalendarDate calendarDate) {
        this(calendarDate.date, calendarDate.desc, calendarDate.entry);
    }

    public String getDesc() {
        return desc.isEmpty() ? entry.name : desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Entry getEntry() {
        return entry;
    }

    @NonNull
    @Override
    public @NotNull CalendarDate clone() {
        return new CalendarDate(this);
    }

    public static class DateComparator implements Comparator<CalendarDate>, Serializable {
    @Serial
    private static final long serialVersionUID = 1;
        @Override
        public int compare(CalendarDate d1, CalendarDate d2) {
            return d1.date.compareTo(d2.date);
        }
    }
}
