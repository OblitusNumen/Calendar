package oblitusnumen.calendar.implementation.data;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import oblitusnumen.calendar.implementation.Utils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class Date implements BaseColumns { // TODO 10/24/24 8:31 PM sorted
    public static final String TABLE_NAME = "dates";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_DESC = "description";
    public static final String COLUMN_NAME_TIME_START = "start";
    public static final String COLUMN_NAME_DURATION = "duration";
    public static final String COLUMN_NAME_TIMES_REPEATS = "timesRepeat";
    public static final String COLUMN_NAME_PERIOD = "period";
    private final DbManager dbManager;
    int id = -1;
    int entryId;
    String desc = "";
    long start;
    long duration = 0;
    int timesRepeat = 1;
    long period = 86400;

    @SuppressLint("Range")
    Date(DbManager dbManager, Cursor cursor) {
        this(dbManager, cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESC)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIME_START)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DURATION)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_TIMES_REPEATS)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_PERIOD)));
    }

    Date(DbManager dbManager, int id, int entryId, String desc, long start, long duration, int timesRepeat, long period) {
        this.dbManager = dbManager;
        this.id = id;
        this.entryId = entryId;
        this.desc = desc;
        this.start = start;
        this.duration = duration;
        this.timesRepeat = timesRepeat;
        this.period = period;
    }

    Date(DbManager dbManager, ContentValues contentValues) {
        this.dbManager = dbManager;
        id = (int) contentValues.get(COLUMN_NAME_ID);
        entryId = (int) contentValues.get(COLUMN_NAME_ENTRY_ID);
        desc = (String) contentValues.get(COLUMN_NAME_DESC);
        start = (long) contentValues.get(COLUMN_NAME_TIME_START);
        duration = (long) contentValues.get(COLUMN_NAME_DURATION);
        timesRepeat = (int) contentValues.get(COLUMN_NAME_TIMES_REPEATS);
        period = (long) contentValues.get(COLUMN_NAME_PERIOD);
    }

    public Date(@NotNull DbManager dbManager, LocalDateTime now, Entry entry) {
        this.dbManager = dbManager;
        start = Utils.toEpochSecond(now);
        entryId = entry.id;
    }

    void create() {
        ContentValues contentValues = toContentValues();
        contentValues.put(COLUMN_NAME_ID, (Integer) null);
        id = (int) dbManager.getWritableDatabase().insert(TABLE_NAME, null, contentValues);
    }

    void createOrUpdate() {
        ContentValues contentValues = toContentValues();
        contentValues.put(COLUMN_NAME_ID, (Integer) null);
        id = (int) dbManager.getWritableDatabase().insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_ID, id);
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId);
        contentValues.put(COLUMN_NAME_DESC, desc);
        contentValues.put(COLUMN_NAME_TIME_START, start);
        contentValues.put(COLUMN_NAME_DURATION, duration);
        contentValues.put(COLUMN_NAME_TIMES_REPEATS, timesRepeat);
        contentValues.put(COLUMN_NAME_PERIOD, period);
        return contentValues;
    }

    public String getDesc() {
        return desc.isEmpty() ? getEntry().name : desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Entry getEntry() {
        return dbManager.entryById(entryId);
    }

    public long getStart() {
        return start;
    }

    public long getDuration() {
        return duration;
    }

    public int getTimesRepeat() {
        return timesRepeat;
    }

    public long getPeriod() {
        return period;
    }

    public long forDay(LocalDateTime begin) {
        long start = Utils.toEpochSecond(begin);
        long end = start + 86400;
        if (end < this.start) return -1;
        int idx = Math.min(timesRepeat - 1, (int) ((end - this.start) / period));
        long time = this.start + period * idx;
        return time > start ? time : -1;
    }

    public void delete() {
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(id)});

        // FIXME: 10/24/24 remove all asociated entries i.e.
    }
}