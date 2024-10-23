package oblitusnumen.calendar.implementation.data;


import android.content.ContentValues;
import android.provider.BaseColumns;

public class Date implements BaseColumns {
    public static final String TABLE_NAME = "dates";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_DESC = "description";
    public static final String COLUMN_NAME_TIME_START = "start";
    public static final String COLUMN_NAME_DURATION = "duration";
    public static final String COLUMN_NAME_TIMES_REPEATS = "recurring";
    public static final String COLUMN_NAME_PERIOD = "period";
    private final DbHelper dbHelper;
    int id;
    int entryId;
    String desc;
    long start;
    long duration;
    int timesRepeat;
    long period;

    Date(DbHelper dbHelper, ContentValues contentValues) {
        this.dbHelper = dbHelper;
        id = (int) contentValues.get(COLUMN_NAME_ID);
        entryId = (int) contentValues.get(COLUMN_NAME_ENTRY_ID);
        desc = (String) contentValues.get(COLUMN_NAME_DESC);
        start = (long) contentValues.get(COLUMN_NAME_TIME_START);
        duration = (long) contentValues.get(COLUMN_NAME_DURATION);
        timesRepeat = (int) contentValues.get(COLUMN_NAME_TIMES_REPEATS);
        period = (long) contentValues.get(COLUMN_NAME_PERIOD);
    }

    public String getDesc() {
        return desc == null ? getEntry().name : desc;
    }

    public Entry getEntry() {
        return Entry.byId(dbHelper, entryId);
    }
}