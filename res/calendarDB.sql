CREATE TABLE IF NOT EXISTS "entries"
(
    "id"          INTEGER PRIMARY KEY AUTOINCREMENT,
    "state"       INTEGER NOT NULL,
    "name"        TEXT    NOT NULL,
    "excludeView" INTEGER NOT NULL,
    "color"       INTEGER NOT NULL
);


CREATE TABLE IF NOT EXISTS "tags"
(
    "id"    INTEGER PRIMARY KEY AUTOINCREMENT,
    "name"  TEXT    NOT NULL UNIQUE,
    "color" INTEGER NOT NULL
);


CREATE TABLE IF NOT EXISTS "entryTagLinks"
(
    "entryId" INTEGER NOT NULL,
    "tagId"   INTEGER NOT NULL,
    FOREIGN KEY ("entryId") REFERENCES "entries" ("id"),
    FOREIGN KEY ("tagId") REFERENCES "tags" ("id")
);


CREATE TABLE IF NOT EXISTS "dates"
(
    "id"             INTEGER PRIMARY KEY AUTOINCREMENT,
    "entryId"        INTEGER NOT NULL,
    "description"    TEXT    NOT NULL,
    "timeStart"      BIGINT  NOT NULL,
    "duration"       BIGINT  NOT NULL,
    "timeEnd"        BIGINT  NOT NULL,
    "timesRepeat"    INTEGER NOT NULL,
    "period"         TEXT    NOT NULL,
    "timeZone"       TEXT    NOT NULL,
    "exceptionRules" TEXT    NOT NULL,
    FOREIGN KEY ("entryId") REFERENCES "entries" ("id")
);


CREATE TABLE IF NOT EXISTS "notifications"
(
    "entryId"    INTEGER     NOT NULL,
    "timeOffset" VARCHAR(18) NOT NULL,
    "sound"      INT         NOT NULL,
    FOREIGN KEY ("entryId") REFERENCES "entries" ("id")
);

drop table notifications;
drop table dates;
drop table entryTagLinks;
drop table tags;
drop table entries;