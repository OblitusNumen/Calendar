package oblitusnumen.calendar.implementation.data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class CalendarDate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;
    final LocalDateTime date;
    private final Entry entry;
    private String desc;

    public CalendarDate(LocalDateTime date, Entry entry) {
        this(date, "", entry);
    }

    public CalendarDate(LocalDateTime date, String desc, Entry entry) {
        this.date = date;
        this.entry = entry;
        this.desc = desc;
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
}
