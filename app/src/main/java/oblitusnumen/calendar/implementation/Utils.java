package oblitusnumen.calendar.implementation;

import android.util.Log;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class Utils {
    public static final ZoneOffset ZONE_OFFSET = ZoneId.systemDefault().getRules().getOffset(Instant.now());

    public static void rmRecursively(File file) {
        if (file.isDirectory()) {
            for (File listFile : file.listFiles()) {
                rmRecursively(listFile);
            }
        }
        if (!file.delete()) throw new RuntimeException("could not delete file " + file);
    }

    public static LocalDateTime toLocalDateTime(long epochSec) {
        return LocalDateTime.ofEpochSecond(epochSec, 0, ZONE_OFFSET);
    }

    public static long toEpochSecond(LocalDateTime now) {
        return now.toEpochSecond(ZONE_OFFSET);
    }

    public static void log(Object o) {
        log("calendar", o);
    }

    public static void log(String tag, Object o) {
        Log.v(tag, String.valueOf(o));
    }
}
