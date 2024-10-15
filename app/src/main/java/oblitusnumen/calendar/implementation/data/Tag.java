package oblitusnumen.calendar.implementation.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Tag implements Serializable {
    public String name = "";
    transient DataManager dataManager;
    Set<Entry> entries = new HashSet<>();

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
