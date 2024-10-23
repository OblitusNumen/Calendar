package oblitusnumen.calendar.implementation.data;

import android.util.Log;
import oblitusnumen.calendar.MainActivity;
import oblitusnumen.calendar.implementation.Utils;

import java.io.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DataManager {
    final MainActivity activity;
    private DbHelper dbHelper;

    public DataManager(MainActivity activity) {
        this.activity = activity;
        dbHelper = new DbHelper(activity);
    }

    public void close() {
        dbHelper.close();
    }

    public List<Entry> getDates

    public Entry createEntry() {
        return new Entry(dbHelper);
    }
}
