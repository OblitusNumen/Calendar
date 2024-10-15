package oblitusnumen.calendar.implementation;

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
}
