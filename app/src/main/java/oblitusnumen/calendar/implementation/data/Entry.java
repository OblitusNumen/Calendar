package oblitusnumen.calendar.implementation.data;

import oblitusnumen.calendar.implementation.data.content.Content;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Entry implements Serializable {
    public final UUID uid;
    transient DataManager dataManager;
    String name = "";
    Set<Tag> tags = new HashSet<>();
    Set<CalendarDate> calendarDates = new HashSet<>();
    List<Content> contents = new ArrayList<>();

    public Entry(DataManager dataManager) {
        this(UUID.randomUUID(), dataManager);
    }

    Entry(UUID uid, DataManager dataManager) {
        this.uid = uid;
        this.dataManager = dataManager;
        dataManager.addEntry(this);
    }

    public void addDate(LocalDateTime date) {

    }

    public File getDir() {
        return new File(dataManager.activity.getFilesDir() + File.separator + "entry-" + uid);
    }

    public void set(String name1, Iterable<String> tags2, Set<CalendarDate> calendarDates1, List<Content> contents1) {
        if (!name1.equals(name)) {
            name = name1;
        }
        Set<Tag> tags1 = new HashSet<>();
        for (String t : tags2) {
            tags1.add(dataManager.getTag(t));
        }
        for (Tag tag : tags.toArray(new Tag[0])) {
            rmTag(tag);
        }
        for (Tag tag : tags1) {
            addTag(tag);
        }
        calendarDates.clear();
        calendarDates.addAll(calendarDates1);
        contents.clear();
        contents.addAll(contents1);
        dataManager.save();
    }

    private void addTag(Tag tag) {
        tags.add(tag);
        tag.addEntry(this);
    }

    private void rmTag(Tag tag) {
        tags.remove(tag);
        tag.rmEntry(this);
    }

    public void remove() {
        dataManager.rmEntry(this);
    }

    public String getName() {
        return name;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public Set<CalendarDate> getCalendarDates() {
        return calendarDates;
    }

    public List<Content> getContents() {
        return contents;
    }
}
