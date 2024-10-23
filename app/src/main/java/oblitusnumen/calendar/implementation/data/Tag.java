package oblitusnumen.calendar.implementation.data;

import android.content.ContentValues;
import android.provider.BaseColumns;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Tag implements BaseColumns {
    public static final String TABLE_NAME = "tags";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_COLOR = "color";
    private final DbHelper dbHelper;
    int id;
    String name;
    int color;

    Tag(DbHelper dbHelper, ContentValues contentValues) {
        this.dbHelper = dbHelper;
        this.id = (int) contentValues.get(COLUMN_NAME_ID);
        this.name = (String) contentValues.get(COLUMN_NAME_NAME);
        this.color = (int) contentValues.get(COLUMN_NAME_COLOR);
    }

    Tag(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
}
