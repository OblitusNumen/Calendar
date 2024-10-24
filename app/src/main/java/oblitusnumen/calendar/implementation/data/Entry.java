package oblitusnumen.calendar.implementation.data;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import oblitusnumen.calendar.implementation.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Entry implements BaseColumns {
    public static final String TABLE_NAME = "entries";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String CONTENTS_FILENAME = "contents.md";
    private final DbManager dbManager;
    int id = -1;
    String name;

    @SuppressLint("NewApi")
    public String getContents() {
        try (FileInputStream fis = new FileInputStream(getContentsFile())) {
            return new String(fis.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Date> getDates() {
        List<Date> dates = new ArrayList<>();
        try (Cursor query = dbManager.getReadableDatabase().rawQuery("SELECT * FROM " + Date.TABLE_NAME + " WHERE " + Date.COLUMN_NAME_ENTRY_ID + " = ?",
                new String[]{String.valueOf(id)})) {
            if (query != null) {
                while (query.moveToNext()) {
                    dates.add(new Date(dbManager, query));
                }
            }
        }
        return dates;
    }

    Entry(DbManager dbManager, ContentValues contentValues) {
        this.dbManager = dbManager;
        this.id = (int) contentValues.get(COLUMN_NAME_ID);
        this.name = (String) contentValues.get(COLUMN_NAME_NAME);
    }

    @SuppressLint("Range")
    Entry(DbManager dbManager, Cursor cursor) {
        this(dbManager, cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)), cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)));
    }

    /**
     * we initialize <code>Entry</code> here and only here
     * @param dbManager
     */
    Entry(DbManager dbManager) {
        this.dbManager = dbManager;
        ContentValues contentValues = toContentValues();
        contentValues.put(COLUMN_NAME_ID, (Integer) null);
        id = (int) dbManager.getWritableDatabase().insert(TABLE_NAME, null, contentValues);
        if (!getDir().mkdirs()) throw new RuntimeException("could not create directory for entry " + id + ", filename: " + getDir());
        try {
            if (getContentsFile().createNewFile()) throw new IOException();
        } catch (IOException e) {
            throw new RuntimeException("could not create directory for entry " + id + ", filename: " + getDir(), e);
        }
    }

    public File getDir() {
        return new File(dbManager.activity.getFilesDir(), String.valueOf(id));
    }

    public File getContentsFile() {
        return new File(getDir(), CONTENTS_FILENAME);
    }

    public void delete() {
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(id)});
        Utils.rmRecursively(getDir());
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + EntryTagLinks.TABLE_NAME + " WHERE " + EntryTagLinks.COLUMN_NAME_ENTRY_ID + " = ?", new String[]{String.valueOf(id)});
//        dbManager.getWritableDatabase().execSQL("DELETE FROM " + Tag.TABLE_NAME + " WHERE " + Tag.COLUMN_NAME_ENTRY_ID + " = ?", new String[]{String.valueOf(id)});
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + Date.TABLE_NAME + " WHERE " + Date.COLUMN_NAME_ENTRY_ID + " = ?", new String[]{String.valueOf(id)});
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + Notification.TABLE_NAME + " WHERE " + Notification.COLUMN_NAME_ENTRY_ID + " = ?", new String[]{String.valueOf(id)});

        // FIXME: 10/24/24 remove all asociated entries i.e.
    }

    private Entry(DbManager dbManager, int id, String name) {
        this.dbManager = dbManager;
        this.id = id;
        this.name = name;
    }

    ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_ID, id);
        contentValues.put(COLUMN_NAME_NAME, name);
        return contentValues;
    }

    public String getName() {
        return name;
    }

    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<>();
        try (Cursor cursor = dbManager.getReadableDatabase().rawQuery("SELECT " + Tag.TABLE_NAME + ".* " +
                        "FROM " + Tag.TABLE_NAME + " " +
                        "JOIN " + EntryTagLinks.TABLE_NAME + " ON " + Tag.TABLE_NAME + "." + Tag.COLUMN_NAME_ID + " = " + EntryTagLinks.TABLE_NAME + "." + EntryTagLinks.COLUMN_NAME_TAG_ID + " " +
                        "WHERE " + EntryTagLinks.TABLE_NAME + "." + EntryTagLinks.COLUMN_NAME_ENTRY_ID + " = ?",
                new String[]{String.valueOf(id)})) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    tags.add(new Tag(dbManager, cursor));
                }
            }
        }
        return tags;
    }

    public void set(@NotNull String name, @NotNull List<Tag> tags, @NotNull List<Date> dates, @NotNull String contents) {
        try (FileOutputStream fos = new FileOutputStream(getContentsFile())) {
            fos.write(contents.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("could not save contents file for entry " + id, e);
        }

        this.name = name;
        update();

        LinkedList<Tag> tags1 = new LinkedList<>(tags);
        tags1.removeIf(t -> {
            if (t.id == -1) {
                t.create();
                addTag(t.id);
                return true;
            } return false;
        });
//        Map<Integer, Tag> tagsNew = tags1.stream().collect(Collectors.toMap(t -> t.id, t -> t));
        Map<Integer, Tag> tagsOld = getTags().stream().collect(Collectors.toMap(t -> t.id, t -> t));
        for (Tag tag : tags1) {
            if (tagsOld.containsKey(tag.id)) {
                tagsOld.remove(tag.id);
            } else {
                addTag(tag.id);
            }
        }
        for (Integer t : tagsOld.keySet()) {
            rmTag(t);
        }

        LinkedList<Date> dates1 = new LinkedList<>(dates);
        dates1.removeIf(t -> {
            if (t.id == -1) {
                t.createOrUpdate();
                return true;
            } return false;
        });
//        Map<Integer, Date> datesNew = dates1.stream().collect(Collectors.toMap(t -> t.id, t -> t));
        Map<Integer, Date> datesOld = getDates().stream().collect(Collectors.toMap(t -> t.id, t -> t));
        for (Date date : dates1) {
            if (datesOld.containsKey(date.id)) {
                datesOld.remove(date.id);
            } else {
                date.create();
            }
        }
        for (Date d : datesOld.values()) {
            d.delete();
        }
    }

    void update() {
        dbManager.getWritableDatabase().execSQL("UPDATE " + TABLE_NAME + " " +
                "SET " + COLUMN_NAME_NAME + " = ?", new String[]{name});
    }

    void addTag(int tagId) {
        new EntryTagLinks(dbManager, id, tagId).create();
    }

    void rmTag(int tagId) {
        new EntryTagLinks(dbManager, id, tagId).delete();
    }
}