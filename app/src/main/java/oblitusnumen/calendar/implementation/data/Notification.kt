package oblitusnumen.calendar.implementation.data;


import android.content.ContentValues;
import android.provider.BaseColumns;

public final class Notification implements BaseColumns {
    public static final String TABLE_NAME = "notifications";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_TIME_OFFSET = "timeOffset";
    private final DbManager dbManager;
    int entryId;
    long offset;

    Notification(DbManager dbManager, ContentValues contentValues) {
        this.dbManager = dbManager;
        entryId = (int) contentValues.get(COLUMN_NAME_ENTRY_ID);
        offset = (long) contentValues.get(COLUMN_NAME_TIME_OFFSET);
    }
}
