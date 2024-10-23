package oblitusnumen.calendar.implementation.data;

import android.content.ContentValues;
import android.provider.BaseColumns;

public class EntryTagLinks implements BaseColumns {
    public static final String TABLE_NAME = "entryTagLinks";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_TAG_ID = "tagId";
    private final DbHelper dbHelper;
    int entryId;
    int tagId;

    EntryTagLinks(DbHelper dbHelper, ContentValues contentValues) {
        this.dbHelper = dbHelper;
        entryId = (int) contentValues.get(COLUMN_NAME_ENTRY_ID);
        tagId = (int) contentValues.get(COLUMN_NAME_TAG_ID);
    }

    EntryTagLinks(DbHelper dbHelper, int entryId, int tagId) {
        this.dbHelper = dbHelper;
        this.entryId = entryId;
        this.tagId = tagId;
    }
}