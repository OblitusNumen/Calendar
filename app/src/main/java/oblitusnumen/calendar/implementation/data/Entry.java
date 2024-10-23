package oblitusnumen.calendar.implementation.data;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

public class Entry implements BaseColumns {
    public static final String TABLE_NAME = "entries";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_NAME = "name";
    private final DbHelper dbHelper;
    int id;
    String name;

    Entry(DbHelper dbHelper, ContentValues contentValues) {
        this.dbHelper = dbHelper;
        this.id = (int) contentValues.get(COLUMN_NAME_ID);
        this.name = (String) contentValues.get(COLUMN_NAME_NAME);
    }

    Entry(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    private Entry(DbHelper dbHelper, int id, String name) {
        this.dbHelper = dbHelper;
        this.id = id;
        this.name = name;
    }

    ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_ID, id);
        contentValues.put(COLUMN_NAME_NAME, name);
        return contentValues;
    }

    @SuppressLint("Range")
    static Entry byId(DbHelper dbHelper, int entryId) {
        try (Cursor cursor = dbHelper.getReadableDatabase().query(Entry.TABLE_NAME, new String[]{Entry.COLUMN_NAME_ID, Entry.COLUMN_NAME_NAME},
                Entry.COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(entryId)}, null, null, Entry.COLUMN_NAME_ID + " DESC")) {
            cursor.moveToFirst();
            return new Entry(dbHelper, cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)), cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)));
        }
    }
}