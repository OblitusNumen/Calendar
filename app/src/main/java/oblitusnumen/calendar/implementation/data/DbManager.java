package oblitusnumen.calendar.implementation.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import oblitusnumen.calendar.MainActivity;
import oblitusnumen.calendar.implementation.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DbManager extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DB_NAME = "entries.db";
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE IF NOT EXISTS " + Entry.TABLE_NAME + " (\n" +
            "    " + Entry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    " + Entry.COLUMN_NAME_NAME + " TEXT NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_TAGS = "CREATE TABLE IF NOT EXISTS " + Tag.TABLE_NAME + " (\n" +
            "    " + Tag.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    " + Tag.COLUMN_NAME_NAME + " TEXT NOT NULL,\n" +
            "    " + Tag.COLUMN_NAME_COLOR + " INTEGER NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_ENTRY_TAG_LINKS = "CREATE TABLE IF NOT EXISTS " + EntryTagLinks.TABLE_NAME + " (\n" +
            "    " + EntryTagLinks.COLUMN_NAME_ENTRY_ID + " INTEGER NOT NULL,\n" +
            "    " + EntryTagLinks.COLUMN_NAME_TAG_ID + " INTEGER NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_DATES = "CREATE TABLE IF NOT EXISTS " + Date.TABLE_NAME + " (\n" +
            "    " + Date.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    " + Date.COLUMN_NAME_ENTRY_ID + " INTEGER NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_DESC + " TEXT NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_TIME_START + " BIGINT NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_DURATION + " BIGINT NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_TIMES_REPEATS + " INTEGER NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_PERIOD + " BIGINT NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_NOTIFICATIONS = "CREATE TABLE IF NOT EXISTS " + Notification.TABLE_NAME + " (\n" +
            "    " + Notification.COLUMN_NAME_ENTRY_ID + " INTEGER NOT NULL,\n" +
            "    " + Notification.COLUMN_NAME_TIME_OFFSET + " BIGINT NOT NULL\n" +
            ");";
    final MainActivity activity;

    public DbManager(MainActivity activity) {
        super(activity, DB_NAME, null, DATABASE_VERSION);
        this.activity = activity;
        Utils.log("DbManager created");
    }

    public List<Date> getDates(LocalDateTime start, LocalDateTime end) {
        return getDates(Utils.toEpochSecond(start), Utils.toEpochSecond(end));
    }

    public List<Date> getDates(LocalDate start, LocalDate end) {
        return getDates(start.atStartOfDay(), end.atStartOfDay());
    }

    public Entry createEntry() {
        return new Entry(this);
    }

    public void init() {
        File filesDir = activity.getFilesDir();
        if (filesDir.exists()) return;
        if (!filesDir.mkdirs()) throw new RuntimeException("could not create directory for data: " + filesDir);
        Utils.log("Created files directory");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Utils.log("DbManager.onCreate");
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Entry.TABLE_NAME);
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Tag.TABLE_NAME);
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + EntryTagLinks.TABLE_NAME);
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Date.TABLE_NAME);
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Notification.TABLE_NAME);
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
        sqLiteDatabase.execSQL(SQL_CREATE_TAGS);
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRY_TAG_LINKS);
        sqLiteDatabase.execSQL(SQL_CREATE_DATES);
        sqLiteDatabase.execSQL(SQL_CREATE_NOTIFICATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<Date> getDates(long start, long end) {
        if (end < start)
            throw new IllegalArgumentException("end must be more than start, got start=" + start + ", end=" + end);
        List<Date> dates = new ArrayList<>();
        try (Cursor query = getReadableDatabase().rawQuery("SELECT * FROM " + Date.TABLE_NAME + " WHERE " + Date.COLUMN_NAME_TIME_START + " >= ? AND" +
                " (" + Date.COLUMN_NAME_TIME_START + " + " + Date.COLUMN_NAME_PERIOD + " * (" + Date.COLUMN_NAME_TIMES_REPEATS + " - 1)) < ?", new String[]{String.valueOf(start), String.valueOf(end)})) {
            if (query != null) {
                while (query.moveToNext()) {
                    dates.add(new Date(this, query));
                }
            }
        }
        return dates;
    }

    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<>();
        try (Cursor query = getReadableDatabase().rawQuery("SELECT * FROM " + Tag.TABLE_NAME, new String[]{})) {
            if (query != null) {
                while (query.moveToNext()) {
                    tags.add(new Tag(this, query));
                }
            }
        }
        return tags;
    }

    public Entry entryById(int entryId) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + Entry.TABLE_NAME + " WHERE " + Entry.COLUMN_NAME_ID + " = ?",
                new String[]{String.valueOf(entryId)})) {
            cursor.moveToFirst();
            return new Entry(this, cursor);
        }
    }

    public Tag tagByName(String name) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + Tag.TABLE_NAME + " WHERE " + Entry.COLUMN_NAME_NAME + " = ?",
                new String[]{name})) {
            return cursor != null && cursor.moveToFirst() ? new Tag(this, cursor) : null;
        }
    }

    @NotNull
    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + Entry.TABLE_NAME, new String[]{})) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    entries.add(new Entry(this, cursor));
                }
            }
        }
        return entries;
    }
}
