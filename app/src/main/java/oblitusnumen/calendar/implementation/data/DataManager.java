package oblitusnumen.calendar.implementation.data;

import android.util.Log;
import oblitusnumen.calendar.MainActivity;
import oblitusnumen.calendar.implementation.Utils;

import java.io.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DataManager {
    public static final String DATA_FILE = "data.ces";
    final MainActivity activity;
    private final Set<Tag> tags = new HashSet<>();
    private final Set<Entry> entries = new HashSet<>();
    boolean hasInitialized = false;

    public DataManager(MainActivity activity) {
        this.activity = activity;
    }

    public synchronized void initialize() {
        if (hasInitialized) return;
        hasInitialized = true;
        load();
    }

    private synchronized void load() {
        File filesDir = activity.getFilesDir();
        if (!filesDir.exists()) filesDir.mkdirs();
        File file = new File(filesDir + File.separator + DATA_FILE);
        if (!file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                //noinspection unchecked
                Set<Entry> entries1 = (Set<Entry>) ois.readObject();
                for (Entry entry : entries1) {
                    entry.dataManager = this;
                }
                entries.addAll(entries1);
                //noinspection unchecked
                Set<Tag> tags1 = (Set<Tag>) ois.readObject();
                for (Tag tag : tags1) {
                    tag.dataManager = this;
                }
                tags.addAll(tags1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void addEntry(Entry entry) {
        if (entries.contains(entry)) return;
        if (!entry.getDir().mkdirs()) throw new RuntimeException("could not create dir for entry " + entry.uid);
        entries.add(entry);
        save();
    }

    void rmEntry(Entry entry) {
        if (!entries.contains(entry)) return;
        Utils.rmRecursively(entry.getDir());
        entries.remove(entry);
        save();
    }

    synchronized void save() {
        Log.v("calendar", "saving data...");
        try (FileOutputStream fos = new FileOutputStream(activity.getFilesDir() + File.separator + DATA_FILE)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(entries);
                oos.writeObject(tags);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void rmTag(Tag tag) {
        tags.remove(tag);
    }

    void addTag(Tag tag) {
        tags.add(tag);
    }

    Tag getTag(Tag tag) {
        Tag tagHere = null;
        for (Tag t : tags) {
            if (Objects.equals(t.name, tag.name)) {
                tagHere = t;
                break;
            }
        }

        if (tagHere == null) {
            addTag(tag);
            return tag;
        }
//        if (tag != tagHere) {
        // TODO: 10/16/24 change color here
//        }
        return tagHere;
    }

    public Set<Tag> getTags() {
        return new HashSet<>(tags);
    }

    public Set<CalendarDate> getDates(LocalDateTime start, LocalDateTime end) {
        Set<CalendarDate> result = new HashSet<>();
        for (Entry entry : entries) {
            for (CalendarDate calendarDate : entry.calendarDates) {
                if (calendarDate.date.isAfter(start) && calendarDate.date.isBefore(end)) {
                    result.add(calendarDate);
                }
            }
        }
        return result;
    }
}
