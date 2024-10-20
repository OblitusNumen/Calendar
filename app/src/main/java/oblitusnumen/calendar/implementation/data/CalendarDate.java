package oblitusnumen.calendar.implementation.data;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class CalendarDate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;
    transient DataManager dataManager;
    public final UUID uid = UUID.randomUUID();
    final LocalDateTime date;
    final UUID entry;
    private String desc;

    public CalendarDate(LocalDateTime date, UUID entry) {
        this(date, "", entry);
    }

    public CalendarDate(LocalDateTime date, String desc, UUID entry) {
        this.date = date;
        this.entry = entry;
        this.desc = desc;
    }

    public CalendarDate(CalendarDate calendarDate) {
        this(calendarDate.date, calendarDate.desc, calendarDate.entry);
    }

    public String getDesc() {
        return desc.isEmpty() ? dataManager.getEntryUnsafe(entry).name : desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public UUID getEntry() {
        return entry;
    }

    @NonNull
    @Override
    public @NotNull CalendarDate clone() {
        return new CalendarDate(this);
    }
}
