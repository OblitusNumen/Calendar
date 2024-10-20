package oblitusnumen.calendar.implementation.data;

import oblitusnumen.calendar.implementation.data.content.Content;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Entry implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;
    public final UUID uid;
    transient DataManager dataManager;
    String name = "";
    Set<String> tags = new HashSet<>();
    Set<UUID> calendarDates = new HashSet<>();
    List<Content> contents = new ArrayList<>();

    Entry() {
        uid = UUID.randomUUID();
    }

    Entry(Entry entry) {
        uid = entry.uid;
        dataManager = entry.dataManager;
        name = entry.name;
        tags.addAll(entry.tags);
        calendarDates.addAll(entry.calendarDates);
        contents.addAll(entry.contents);
    }

    Entry(UUID uid) {
        this.uid = uid;
    }

    File getDir() {
        return new File(dataManager.activity.getFilesDir() + File.separator + "entry-" + uid);
    }

    void set(Entry entry) {
        name = entry.name;
        // TODO: 10/18/24 updateContents
//        getDir();
        contents = new ArrayList<>(entry.contents);
        Set<String> toRemove = new HashSet<>(tags);
        toRemove.removeAll(entry.tags);
        for (String s : toRemove) {
            rmTag(s);
        }
        Set<String> toAdd = new HashSet<>(entry.tags);
        toRemove.removeAll(tags);
        for (String s : toAdd) {
            addTag(s);
        }
        tags.clear();
        tags.addAll(entry.tags);
        CalendarDates managerDates = dataManager.getDates();
        managerDates.list.removeAll(managerDates.withEntry(uid).list);
        managerDates.list.addAll();
        calendarDates.clear();
        for (CalendarDate date : managerDates) {
            calendarDates.add(date.uid);
        }
    }

    private void addTag(String tag) {
        dataManager.addTagEntryLink(tag, uid);
    }

    private void rmTag(String tag) {
        dataManager.rmTagEntryLink(tag, uid);
    }

    void remove() {
        dataManager.rmEntry(this.uid);
    }

    void update(CalendarDates dates) {
        dataManager.updateEntry(this, dates);
    }

    HashSet<Tag> getTags() {
        HashSet<Tag> result = new HashSet<>();
        for (String tag : tags) {
            result.add(dataManager.getTag(tag));
        }
        return result;
    }

    @Override
    protected Entry clone() {
        return new Entry(this);
    }
}
