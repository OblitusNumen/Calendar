package oblitusnumen.calendar.implementation.data;


import android.content.ContentValues;
import android.provider.BaseColumns;

public final class Notification implements BaseColumns {
    public static final String TABLE_NAME = "notifications";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_TIME_OFFSET = "offset";
    private final DbHelper dbHelper;
    int entryId;
    long offset;

    Notification(DbHelper dbHelper, ContentValues contentValues) {
        this.dbHelper = dbHelper;
        entryId = (int) contentValues.get(COLUMN_NAME_ENTRY_ID);
        offset = (long) contentValues.get(COLUMN_NAME_TIME_OFFSET);
    }
}
