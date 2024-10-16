package oblitusnumen.calendar.implementation;

import android.util.Log;

import java.io.File;

public class Utils {
    public static void rmRecursively(File file) {
        if (file.isDirectory()) {
            for (File listFile : file.listFiles()) {
                rmRecursively(listFile);
            }
        }
        if (!file.delete()) throw new RuntimeException("could not delete file " + file);
    }

    public static void log(Object o) {
        log("calendar", o);
    }

    public static void log(String tag, Object o) {
        Log.v(tag, String.valueOf(o));
    }
}
