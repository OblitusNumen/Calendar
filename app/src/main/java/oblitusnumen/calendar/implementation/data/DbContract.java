package oblitusnumen.calendar.implementation.data;

import android.provider.BaseColumns;

public class DbContract {
    private DbContract() {
    }

    public static class Entry implements BaseColumns {
        public static final String TABLE_NAME = "entries";
    }
}
