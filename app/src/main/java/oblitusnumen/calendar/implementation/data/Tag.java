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
    Set<UUID> entries = new HashSet<>();

    public Tag(String tag) {
        name = tag;
    }

    public Tag(Tag tag) {
        name = tag.name;
        entries.addAll(tag.entries);
    }

    void addEntry(UUID entry) {
        entries.add(entry);
    }

    void rmEntry(UUID entry) {
        entries.remove(entry);
    }

    @Override
    public Tag clone() {
        return new Tag(this);
    }
}
