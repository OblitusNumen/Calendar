package oblitusnumen.calendar.implementation.data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Tag implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;
    public String name = "";
    transient DataManager dataManager;
    Set<UUID> entries = new HashSet<>();

    public Tag(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    void addEntry(Entry entry) {
        entries.add(entry);
        dataManager.addTag(this);
    }

    void rmEntry(Entry entry) {
        entries.remove(entry);
        if (entries.isEmpty()) dataManager.rmTag(this);
    }
}
