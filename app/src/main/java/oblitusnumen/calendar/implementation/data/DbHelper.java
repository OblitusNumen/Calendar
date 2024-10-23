package oblitusnumen.calendar.implementation.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import oblitusnumen.calendar.implementation.Utils;

public class DbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DB_NAME = "entries.db";
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + Entry.TABLE_NAME + " (\n" +
            "    " + Entry.COLUMN_NAME_ID + " INT PRIMARY KEY AUTOINCREMENT,\n" +
            "    " + Entry.COLUMN_NAME_NAME + " TEXT NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_TAGS = "CREATE TABLE " + Tag.TABLE_NAME + " (\n" +
            "    " + Tag.COLUMN_NAME_ID + " INT PRIMARY KEY AUTOINCREMENT,\n" +
            "    " + Tag.COLUMN_NAME_NAME + " TEXT NOT NULL,\n" +
            "    " + Tag.COLUMN_NAME_COLOR + " INT NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_ENTRY_TAG_LINKS = "CREATE TABLE " + EntryTagLinks.TABLE_NAME + " (\n" +
            "    " + EntryTagLinks.COLUMN_NAME_ENTRY_ID + " INT PRIMARY KEY,\n" +
            "    " + EntryTagLinks.COLUMN_NAME_TAG_ID + " INT NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_DATES = "CREATE TABLE " + Date.TABLE_NAME + " (\n" +
            "    " + Date.COLUMN_NAME_ID + " INT PRIMARY KEY AUTOINCREMENT,\n" +
            "    " + Date.COLUMN_NAME_ENTRY_ID + " INT NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_DESC + " TEXT,\n" +
            "    " + Date.COLUMN_NAME_TIME_START + " BIGINT NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_DURATION + " BIGINT NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_TIMES_REPEATS + " INT NOT NULL,\n" +
            "    " + Date.COLUMN_NAME_PERIOD + " BIGINT NOT NULL\n" +
            ");";
    private static final String SQL_CREATE_NOTIFICATIONS = "CREATE TABLE " + Notification.TABLE_NAME + " (\n" +
            "    " + Notification.COLUMN_NAME_ENTRY_ID + " INT NOT NULL,\n" +
            "    " + Notification.COLUMN_NAME_TIME_OFFSET + " BIGINT NOT NULL\n" +
            ");";

    public DbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        Utils.log("DbHelper");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Utils.log("DbHelper.create");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SQL_CREATE_ENTRIES);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SQL_CREATE_TAGS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SQL_CREATE_ENTRY_TAG_LINKS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SQL_CREATE_DATES);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SQL_CREATE_NOTIFICATIONS);
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
}
