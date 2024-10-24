package oblitusnumen.calendar.implementation.data;

import android.provider.BaseColumns;

public class EntryTagLinks implements BaseColumns {
    public static final String TABLE_NAME = "entryTagLinks";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_TAG_ID = "tagId";
    private final DbManager dbManager;
    int entryId;
    int tagId;

    EntryTagLinks(DbManager dbManager, int entryId, int tagId) {
        this.dbManager = dbManager;
        this.entryId = entryId;
        this.tagId = tagId;
    }

    public void create() {
        dbManager.getWritableDatabase().execSQL("INSERT OR IGNORE INTO " + TABLE_NAME + " (" + COLUMN_NAME_ENTRY_ID + ", " + COLUMN_NAME_TAG_ID + ") " +
                "VALUES (?, ?)", new String[]{String.valueOf(entryId), String.valueOf(tagId)});
    }

    public void delete() {
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME + " " +
                        "WHERE " + COLUMN_NAME_ENTRY_ID + " = ? AND " + COLUMN_NAME_TAG_ID + " = ?",
                new String[]{String.valueOf(entryId), String.valueOf(tagId)});
    }
}