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

public class Date implements BaseColumns {
    public static final String TABLE_NAME = "dates";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_ENTRY_ID = "entryId";
    public static final String COLUMN_NAME_DESC = "description";
    public static final String COLUMN_NAME_TIME_START = "timeStart";
    public static final String COLUMN_NAME_DURATION = "duration";
    public static final String COLUMN_NAME_TIME_ENDS = "timeEnd";
    public static final String COLUMN_NAME_TIMES_REPEATS = "timesRepeat";
    public static final String COLUMN_NAME_PERIOD = "period";
    public static final String COLUMN_NAME_TIME_ZONE = "timeZone";
    public static final String COLUMN_NAME_REMOVED = "exceptionRules";
    private final DbManager dbManager;
    int id = -1;
    int entryId;
    String desc;
    long start;
    long duration;
    long end;
    int timesRepeat = 1;
    Period period;
    ZoneId zoneId;
    ExceptionRules exceptionRules;

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
        setupTimesRepeat(timesRepeat);
        this.period = new Period(period);
        this.zoneId = ZoneId.of(zoneId);
        this.exceptionRules = new ExceptionRules(this, removed);
    }

    public Date(DbManager dbManager, Entry entry, String desc, ZonedDateTime time, long duration, int timesRepeat, Period period) {
        this.dbManager = dbManager;
        this.entryId = entry.getId();
        this.desc = desc;
        this.start = time.toEpochSecond();
        this.duration = duration;
        this.period = period;
        this.zoneId = time.getZone();
        this.exceptionRules = ExceptionRules.none(this);
        setTimesRepeat(timesRepeat);
    }

    void createOrUpdate() {
        if (!isEmpty()) {
            ContentValues contentValues = toContentValues();
            contentValues.put(COLUMN_NAME_ID, (Integer) null);
            id = (int) dbManager.getWritableDatabase().insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            delete();
        }
    }

    public boolean isEndless() {
        return timesRepeat == Integer.MAX_VALUE;
    }

    public boolean isEmpty() {
        return timesRepeat <= 0;
    }

    public void delete() {
        dbManager.getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(id)});

        // FIXME: 10/24/24 remove all associated entries i.e.
    }

    ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_ID, id);
        contentValues.put(COLUMN_NAME_ENTRY_ID, entryId);
        contentValues.put(COLUMN_NAME_DESC, desc);
        contentValues.put(COLUMN_NAME_TIME_START, start);
        contentValues.put(COLUMN_NAME_DURATION, duration);
        contentValues.put(COLUMN_NAME_TIME_ENDS, end);
        contentValues.put(COLUMN_NAME_TIMES_REPEATS, timesRepeat == Integer.MAX_VALUE ? -1 : timesRepeat);
        contentValues.put(COLUMN_NAME_PERIOD, period.toString());
        contentValues.put(COLUMN_NAME_TIME_ZONE, zoneId.toString());
        contentValues.put(COLUMN_NAME_REMOVED, exceptionRules.toString());
        return contentValues;
    }

    /**
     * @return null if event does not happen during next day from <code>startOfDay</code> or time which event takes place at
     */
    public ZonedDateTime forDay(ZonedDateTime startOfDay) {
        long start = startOfDay.toEpochSecond();
        long end = startOfDay.plusDays(1).toEpochSecond();
        int zonedDateTimeIndex = getZonedDateTimeIndex(start, end);
        if (zonedDateTimeIndex == -1 || !exceptionRules.isEventPresent(zonedDateTimeIndex)) return null;
        return getZoneDateTime(zonedDateTimeIndex);
    }

    public ZonedDateTime getZoneDateTime(ZoneId zoneId, int idx) {
        return getZoneDateTime(idx).withZoneSameInstant(zoneId);
    }

    public ZonedDateTime getZoneDateTime(int idx) {
        return period.getTime(Instant.ofEpochSecond(start).atZone(zoneId), idx);
    }

    public Period getPeriod() {
        return period;
    }

    long getTime(int idx) {
        return getZoneDateTime(idx).toEpochSecond();
    }

    public String getDesc() {
        return desc.isEmpty() ? getEntry().getName() : desc;
    }

    public boolean exists() {
        return id != -1;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Entry getEntry() {
        return Entry.Companion.byId(dbManager, entryId);
    }

    public long getStart() {
        return start;
    }

    private void setupTimesRepeat(int timesRepeat) {
        this.timesRepeat = timesRepeat == -1 ? Integer.MAX_VALUE : timesRepeat;
    }

    /**
     * @return -1 if date is endless otherwise count of date occurring (without respect to deleted occurrences)
     */
    public int getTimesRepeat() {
        return isEndless() ? -1 : timesRepeat;
    }

    private void setTimesRepeat(int timesRepeat) {
        this.timesRepeat = timesRepeat;
        this.end = getTime(timesRepeat - 1);
    }

    int getZonedDateTimeIndex(long start, long finish) {
        if (finish <= this.start) return -1;
        if (this.end == this.start) return this.start >= start ? 0 : -1;
        long period = (this.end - this.start) / timesRepeat;
        int idx = Math.min(timesRepeat - 1, (int) ((finish - this.start) / period));
        long time = getZoneDateTime(idx).toEpochSecond();
        if (time >= start && time < finish)
            return idx;
        if (time >= finish && idx > 1) {
            long timem1 = getZoneDateTime(idx - 1).toEpochSecond();
            if (timem1 >= finish || timem1 < start)
                return -1;
            else
                return idx - 1;
        }
        if (time < start && idx < timesRepeat - 1) {
            long timep1 = getZoneDateTime(idx + 1).toEpochSecond();
            if (timep1 < start || timep1 >= finish)
                return -1;
            else
                return idx + 1;
        }
        return -1;
    }

    void addEvent(int idx) {
        exceptionRules.addEvents(idx, idx + 1);
    }

    void addEvents(int from, int to) {
        exceptionRules.addEvents(from, to);
    }

    void makeEndless() {
        exceptionRules.addEvents(timesRepeat, Integer.MAX_VALUE);
    }

    void cropToTimesRepeat(int timesRepeat) {
        if (timesRepeat > this.timesRepeat) {
            addEvents(this.timesRepeat, timesRepeat);
        } else exceptionRules.removeEvents(timesRepeat, Integer.MAX_VALUE);
    }

    void removeEvent(int idx) {
        exceptionRules.removeEvents(idx, idx + 1);
    }

    void removeEvents(int from, int to) {
        exceptionRules.removeEvents(from, to);
    }

    static class ExceptionRules {
        final ArrayList<Rule> rules = new ArrayList<>();
        private final Date date;

        ExceptionRules(Date date, String removed) {
            this.date = date;
            if (removed.isEmpty()) return;
            for (String s : removed.split(",")) {
                rules.add(new Rule(s));
            }
        }

        static ExceptionRules none(Date date) {
            return new ExceptionRules(date, "");
        }

        private void clearRules(int from, int to) {
            rules.subList(from, to).clear();
        }

        private void addEvents(int from, int to) {//end not including
            if (from > date.timesRepeat) {//this does not create any cases like 5-10,10-20, as last event in date must occur
                rules.add(new Rule(date.timesRepeat, from));
            }
            int idx0 = findIndex(from);//index after rule to be changed before interval
            if (to > date.timesRepeat) {//crop date to size to
                date.setTimesRepeat(to);
                if (idx0 > 0) {
                    Rule rule = rules.get(idx0 - 1);
                    if (rule.end > from) {
                        rule.end = from;
                    }
                }
                clearRules(idx0, rules.size());
                return;
            }
            int idx1 = findIndex(to);//index after rule to be changed after interval
            if (idx0 > 0) {//before interval
                Rule rule0 = rules.get(idx0 - 1);
                if (rule0.start == from) {//rule begins from interval
                    idx0--;
                } else {
                    if (rule0.end > to) {//splitting rule
                        rules.add(idx0, new Rule(to, rule0.end));
                        rule0.end = from;
                        return;
                    }//lowering ceiling of rule
                    if (rule0.end > from) {
                        rule0.end = from;
                    }
                }
            }
            if (idx1 > 0) {//index-1 is in range // after interval
                Rule rule1 = rules.get(idx1 - 1);
                if (rule1.end > to) {//rule is not fully inside interval
                    rule1.start = to;
                    idx1--;
                }
            }
            clearRules(idx0, idx1);
        }

        private void removeEvents(int from, int to) {
            if (from >= date.timesRepeat) return;
            int idx0 = findIndex(from);//index after rule to be changed before interval
            if (to >= date.timesRepeat) {//crop date to size from
                if (idx0 > 0) {
                    Rule rule = rules.get(idx0 - 1);
                    if (rule.end >= from) {
                        from = rule.start;
                        idx0--;
                    }
                }
                clearRules(idx0, rules.size());
                date.setTimesRepeat(from);
                return;
            }
            if (idx0 > 0) {//before interval
                Rule rule0 = rules.get(idx0 - 1);
                if (rule0.end >= from) {//end of rule is in interval
                    from = rule0.start;
                    idx0--;
                }
            }
            int idx1 = findIndex(to);//index after rule to be changed after interval
            if (idx1 > 0) {//index-1 is in range // after interval
                Rule rule1 = rules.get(idx1 - 1);
                if (rule1.end > to) {//rule ends after to
                    to = rule1.end;
                }
            }
            clearRules(idx0, idx1);
            rules.add(idx0, new Rule(from, to));
        }

        private int findIndex(int beginIdx) {
            int begin = 0;
            int end = rules.size();
            while (true) {//search algorithm
                int center = (begin + end) / 2;
                if (center == end) return center;
                if (rules.get(center).start > beginIdx) {
                    end = center;
                } else {
                    if (begin == center) return end;
                    begin = center;
                }
            }
        }

        boolean isEventPresent(int idx) {
            int index = findIndex(idx);//for faster work
            if (index > 0) {//may be in some rules
                return rules.get(index - 1).isPresent(idx);
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

        private class Rule {
            int start;
            int end;

            Rule(String rule) {
                String[] split = rule.split("-");
                switch (split.length) {
                    case 1 -> {
                        start = Integer.parseInt(split[0]);
                        end = Integer.MAX_VALUE;
                    }
                    case 2 -> {
                        start = Integer.parseInt(split[0]);
                        end = Integer.parseInt(split[1]);
                    }
                    default -> throw new RuntimeException("impossible outcome");
                }
            }

            Rule(int start, int end) {
                this.start = start;
                this.end = end;
            }

            boolean isPresent(int idx) {
                return idx < start || end <= idx;
            }

            @Override
            public @NotNull String toString() {
                return start + "-" + (end == date.timesRepeat ? "" : end);
            }
        }
    }
}