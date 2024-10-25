package oblitusnumen.calendar.implementation.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

public class Tag implements BaseColumns {
    public static final String TABLE_NAME = "tags";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_COLOR = "color";
    private final DbManager dbManager;
    int id = -1;
    String name;
    int color = -1;
    // TODO: 10/25/24 fix dupe tags

    Tag(DbManager dbManager, ContentValues contentValues) {
        this.dbManager = dbManager;
        this.id = (int) contentValues.get(COLUMN_NAME_ID);
        this.name = (String) contentValues.get(COLUMN_NAME_NAME);
        this.color = (int) contentValues.get(COLUMN_NAME_COLOR);
    }

    public Tag(DbManager dbManager, String name) {
        this.dbManager = dbManager;
        this.name = name;
        Tag tag = dbManager.tagByName(name);
        if (tag != null) {
            id = tag.id;
            this.name = tag.name;
            color = tag.color;
        }
    }

    public Tag(DbManager dbManager, int id, String name, int color) {
        this.dbManager = dbManager;
        this.id = id;
        this.name = name;
        this.color = color;
    }

    Tag(DbManager dbManager, Cursor cursor) {
        this(dbManager, cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_COLOR)));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    void create() {
        ContentValues contentValues = toContentValues();
        contentValues.put(COLUMN_NAME_ID, (Integer) null);
        id = (int) dbManager.getWritableDatabase().insert(TABLE_NAME, null, contentValues);
    }

    ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_ID, id);
        contentValues.put(COLUMN_NAME_NAME, name);
        contentValues.put(COLUMN_NAME_COLOR, color);
        return contentValues;
    }

    void addEntry(int entryId) {
        new EntryTagLinks(dbManager, entryId, id).create();
    }

    void rmEntry(int entryId) {
        new EntryTagLinks(dbManager, entryId, id).delete();
    }
}
