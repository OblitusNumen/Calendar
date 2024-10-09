package oblitusnumen.calendar.implementation;

import java.util.*;

public class Data {
    final Map<String, Tag> tags = new HashMap<>();
    final List<Entry> entriesByTime = new LinkedList<>();
    final Map<UUID, Entry> entries = new HashMap<>();
}
