package oblitusnumen.calendar.implementation.data;

import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import oblitusnumen.calendar.MainActivity;
import oblitusnumen.calendar.implementation.Utils;

import java.io.*;
import java.util.*;

public class DataManager {
    public static final String DATA_FILE = "data.ces";
    final MainActivity activity;
    private final HashMap<String, Tag> tags = new HashMap<>();
    private final HashMap<UUID, Entry> entries = new HashMap<>();
    private final CalendarDates dates = new CalendarDates();
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
        getWritableDatabase();
        File filesDir = activity.getFilesDir();
        if (!filesDir.exists()) filesDir.mkdirs();
        File file = new File(filesDir + File.separator + DATA_FILE);
        if (!file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                //noinspection unchecked
                HashMap<UUID, Entry> entries1 = (HashMap<UUID, Entry>) ois.readObject();
                for (Entry entry : entries1.values()) {
                    entry.dataManager = this;
                    entries.put(entry.uid, entry);
                }
                dates.addAll((CalendarDates) ois.readObject());
                for (CalendarDate date : dates) {
                    date.dataManager = this;
                }
                //noinspection unchecked
                HashMap<String, Tag> tags1 = (HashMap<String, Tag>) ois.readObject();
                tags.putAll(tags1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void save() {
        Log.v("calendar", "saving data...");
        try (FileOutputStream fos = new FileOutputStream(activity.getFilesDir() + File.separator + DATA_FILE)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(entries);
                oos.writeObject(dates);
                oos.writeObject(tags);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Entry createEntry() {
        Entry entry = new Entry();
        if (!entry.getDir().mkdirs()) throw new RuntimeException("could not create dir for entry " + entry.uid);
        entries.put(entry.uid, entry);
        save();
        return entry.clone();
    }

    void updateEntry(Entry entry, CalendarDates entryDates) {
        Entry entry1 = entries.get(entry.uid);
        if (entry1 == null) throw new RuntimeException("should never happen");
        entry1.set(entry, entryDates.withEntry(entry.uid));
        save();
    }

    void rmEntry(UUID entry) {
        Entry entry1 = entries.remove(entry);
        if (entry1 == null) return;
        entry1.set(new Entry(entry1.uid), new CalendarDates());
        Utils.rmRecursively(entry1.getDir());
        save();
    }

//    Tag getTag(Tag tag) {
//        Tag tag1 = tags.get(tag.name);
//        if (tag1 != null) {
//            if (tag1 == tag) return tag1;
//            // FIXME: 10/18/24 change color here
//        }
//        Tag tagHere = null;
//        for (Tag t : tags) {
//            if (Objects.equals(t.name, tag.name)) {
//                tagHere = t;
//                break;
//            }
//        }
//
//        if (tagHere == null) {
//            addTag(tag);
//            return tag;
//        }
//        return tagHere;
//    }

    public Tag getTag(String tag) {
        Tag tag1 = tags.get(tag);
        if (tag1 == null) return new Tag(tag);
        return tag1.clone();
    }

    public Set<Tag> getTags() {
        return new HashSet<>(tags.values());
    }

    public CalendarDates getDates() {
        return dates.clone();
    }

    void rmTagEntryLink(String tag, UUID entry) {
        entries.get(entry).tags.remove(tag);
        Set<UUID> entries1 = tags.get(tag).entries;
        entries1.remove(entry);
        // FIXME: 10/18/24 rm tag if no entries in it
//        if (entries1.isEmpty()) tags.remove(tag);
    }

    void addTagEntryLink(String tag, UUID entry) {
        entries.get(entry).tags.add(tag);
        Tag tag1 = tags.get(tag);
        if (tag1 == null) {
            tag1 = new Tag(tag);
            tags.put(tag, tag1);
        }
        tag1.entries.add(entry);
    }

    public Entry getEntry(UUID entry) {
        return entries.get(entry);
    }

    Entry getEntryUnsafe(UUID entry) {
        return entries.get(entry);
    }
}
