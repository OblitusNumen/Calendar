package oblitusnumen.calendar.implementation.data;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Date implements BaseColumns { // TODO 10/24/24 8:31 PM sorted
    public static final String TABLE_NAME = "dates";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_DESC = "description";
    public static final String COLUMN_NAME_TIME_START = "start";
    public static final String COLUMN_NAME_DURATION = "duration";
    public static final String COLUMN_NAME_TIME_ENDS = "end";
    public static final String COLUMN_NAME_TIMES_REPEATS = "timesRepeat";
    public static final String COLUMN_NAME_PERIOD = "period";
    public static final String COLUMN_NAME_TIME_ZONE = "timeZone";
    public static final String COLUMN_NAME_REMOVED = "removed";
    private final DbManager dbManager;
    int id = -1;
    int entryId;
    String desc = "";
    long start;
    long duration = 0;
    long end;
    int timesRepeat = 1;
    Period period = Period.none();
    ZoneId zoneId;
    Removed removed;

    Date(DbManager dbManager, Cursor cursor) {
        this(dbManager, cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESC)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIME_START)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DURATION)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIME_ENDS)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_TIMES_REPEATS)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NAME_PERIOD)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NAME_REMOVED)));
    }

    Date(DbManager dbManager, int id, int entryId, String desc, long start, long duration, long end, int timesRepeat, String period, String zoneId, String removed) {
        this.dbManager = dbManager;
        this.id = id;
        this.entryId = entryId;
        this.desc = desc;
        this.start = start;
        this.duration = duration;
        this.end = end;
        this.timesRepeat = timesRepeat;
        this.period = new Period(period);
        this.zoneId = ZoneId.of(zoneId);
        this.removed = new Removed(this, removed);
    }

    public Date(DbManager dbManager, Entry entry, String desc, ZonedDateTime time, long duration, int timesRepeat, Period period) {
        this.dbManager = dbManager;
        this.entryId = entry.id;
        this.desc = desc;
        this.start = time.toEpochSecond();
        this.duration = duration;
        this.timesRepeat = timesRepeat;
        this.period = period;
        this.zoneId = time.getZone();
        this.removed = Removed.none(this);
        this.end = getTime(timesRepeat - 1);
    }// TODO: 10/25/24 remove idx method

    public ZonedDateTime getZoneDateTime(ZoneId zoneId, int idx) {
        return getZoneDateTime(idx).withZoneSameInstant(zoneId);
    }

    ZonedDateTime getZoneDateTime(int idx) {
        return period.getTime(Instant.ofEpochSecond(start).atZone(zoneId), idx);
    }

    long getTime(int idx) {
        return getZoneDateTime(idx).toEpochSecond();
    }

    void create() {
        ContentValues contentValues = toContentValues();
        contentValues.put(COLUMN_NAME_ID, (Integer) null);
        id = (int) dbManager.getWritableDatabase().insert(TABLE_NAME, null, contentValues);
    }

    void createOrUpdate() {
        ContentValues contentValues = toContentValues();
        contentValues.put(COLUMN_NAME_ID, (Integer) null);
        id = (int) dbManager.getWritableDatabase().insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_ID, id);
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId);
        contentValues.put(COLUMN_NAME_DESC, desc);
        contentValues.put(COLUMN_NAME_TIME_START, start);
        contentValues.put(COLUMN_NAME_DURATION, duration);
        contentValues.put(COLUMN_NAME_TIME_ENDS, end);
        contentValues.put(COLUMN_NAME_TIMES_REPEATS, timesRepeat);
        contentValues.put(COLUMN_NAME_PERIOD, period.toString());
        contentValues.put(COLUMN_NAME_TIME_ZONE, zoneId.toString());
        contentValues.put(COLUMN_NAME_REMOVED, removed.toString());
        return contentValues;
    }

    public String getDesc() {
        return desc.isEmpty() ? getEntry().name : desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Entry getEntry() {
        return dbManager.entryById(entryId);
    }

    public long getStart() {
        return start;
    }

    /**
     * @param startOfDay
     * @return null if event does not happen during next day from <code>startOfDay</code> or time which event takes place at
     */
    public ZonedDateTime forDay(ZonedDateTime startOfDay) {// FIXME: 10/25/24
        long start = startOfDay.toEpochSecond();
        long end = startOfDay.plusDays(1).toEpochSecond();
        int zonedDateTimeIndex = getZonedDateTimeIndex(start, end);
        if (!removed.isPresent(zonedDateTimeIndex)) return null;
        ZonedDateTime time = getZoneDateTime(zonedDateTimeIndex);
        return start <= time.toEpochSecond() && time.toEpochSecond() < end ? time : null;
    }

    private int getZonedDateTimeIndex(long start, long finish) {
        int begin = 0;
        int end = timesRepeat;
        while (true) {
            int center = (begin + end) / 2;
            if (center == end) {
                return center;
            }
            long second = getZoneDateTime(center).toEpochSecond();
            if (second >= finish) {//start<finish<=second
                end = center;
            } else if (start <= second) {//second<start<finish
                return center;
            } else {//start<=second<finish
                if (begin == center) return -1;
                begin = center;
            }
        }
    }

    public void delete() {
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(id)});

        // FIXME: 10/24/24 remove all asociated entries i.e.
    }

    public static class Period {
        Modifier modifier;
        int count;

        public Period(Modifier modifier, int count) {
            this.modifier = modifier;
            this.count = count;
        }

        Period(String period) {
            this.modifier = Modifier.modifiers.get(period.charAt(0));
            count = Integer.parseInt(period.substring(1));
        }

        public static Period none() {
            return new Period(Modifier.NONE, 0);
        }

        @Override
        public @NotNull String toString() {
            return "" + modifier.symbol + count;
        }

        public ZonedDateTime getTime(ZonedDateTime time, int idx) {
            return modifier.plus(time, count * idx);
        }

        public enum Modifier {
            NONE('N') {
                @Override
                public ZonedDateTime plus(ZonedDateTime time, int count) {
                    return time;
                }
            },
            DAY('D') {
                @Override
                public ZonedDateTime plus(ZonedDateTime time, int count) {
                    return time.plusDays(count);
                }
            },
            WEEK('W') {
                @Override
                public ZonedDateTime plus(ZonedDateTime time, int count) {
                    return time.plusWeeks(count);
                }
            },
            MONTH('M') {
                @Override
                public ZonedDateTime plus(ZonedDateTime time, int count) {
                    return time.plusMonths(count);
                }
            },
            YEAR('Y') {
                @Override
                public ZonedDateTime plus(ZonedDateTime time, int count) {
                    return time.plusYears(count);
                }
            };

            static final Map<Character, Modifier> modifiers = new HashMap<>();

            static {
                for (Modifier value : Modifier.values()) {
                    modifiers.put(value.symbol, value);
                }
            }

            final char symbol;

            Modifier(char symbol) {
                this.symbol = symbol;
            }

            public abstract ZonedDateTime plus(ZonedDateTime time, int count);
        }
    }

    static class Removed {
        final ArrayList<Rule> rules = new ArrayList<>();
        private final Date date;

        Removed(Date date, String removed) {
            this.date = date;
            if (removed.isEmpty()) return;
            for (String s : removed.split(",")) {
                Rule rule = new Rule(s);
                rules.add(rule);
            }
        }

        static Removed none(Date date) {
            return new Removed(date, "");
        }

        void addIndex(int idx) {
            Rule rule = new Rule(idx);
            int additionIndex = findAdditionIndex(rule);
            if (additionIndex-- > 0) {
                Rule rule1 = rules.get(additionIndex);
                if (rule1.idxTo == idx)
                    if (rule1.idxFrom == idx) {
                        rules.remove(rule1);
                    } else {
                        rule1.idxTo--;
                    }
                else if (rule1.idxFrom == idx) rule1.idxFrom++;
                else {
                    Rule rule2 = new Rule(rule1.idxTo);
                    rule1.idxTo = idx - 1;
                    rule2.idxFrom = idx + 1;
                    rules.add(additionIndex + 1, rule2);
                }
            }
        }

        void rmIndex(int idx) {
            Rule rule = new Rule(idx);
            int additionIndex = findAdditionIndex(rule);
            rules.add(additionIndex, rule);
            if (additionIndex > 0) {
                merge(--additionIndex);
            }
            if (additionIndex + 1 < rules.size()) {
                merge(additionIndex);
            }
        }

        private void merge(int i) {
            Rule r0 = rules.get(i);
            Rule r1 = rules.get(i + 1);
            if (r0.idxTo + 1 == r1.idxFrom) {
                r0.idxTo = r1.idxTo;
                rules.remove(r1);
            }
        }

        private int findAdditionIndex(Rule rule) {
            int begin = 0;
            int end = rules.size();
            while (true) {
                int center = (begin + end) / 2;
                if (center == end) return center;
                if (rules.get(center).idxFrom > rule.idxFrom) {
                    end = center;
                } else {
                    if (begin == center) return end;
                    begin = center;
                }
            }
        }

        boolean isPresent(int idx) {
            if (idx >= date.timesRepeat || idx < 0) return false;
            for (Rule rule : rules) {
                if (!rule.isPresent(idx)) return false;
            }
            return true;
        }

        @Override
        public @NotNull String toString() {
            StringBuilder result = new StringBuilder();
            for (Rule rule : rules) {
                result.append(rule).append(",");
            }
            return result.toString();
        }

        static class Rule {
            int idxFrom;
            int idxTo;

            Rule(String rule) {
                String[] split = rule.split("-");
                switch (split.length) {
                    case 1 -> {
                        idxFrom = Integer.parseInt(split[0]);
                        idxTo = rule.endsWith("-") ? -1 : idxFrom;
                    }
                    case 2 -> {
                        idxFrom = Integer.parseInt(split[0]);
                        idxTo = Integer.parseInt(split[1]);
                    }
                    default -> throw new RuntimeException("impossible outcome");
                }
            }

            Rule(int idx) {
                idxTo = idxFrom = idx;
            }

            boolean isPresent(int idx) {
                return idx < idxFrom || idxTo == -1 || idxTo < idx;
            }

            boolean isEnding() {
                return idxTo == -1;
            }

            @Override
            public @NotNull String toString() {
                return idxFrom + "-" + (idxTo == -1 ? "" : idxTo);
            }
        }
    }
}