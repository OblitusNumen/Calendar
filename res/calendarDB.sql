CREATE TABLE IF NOT EXISTS "EventOptions"
(
    "id"          INTEGER PRIMARY KEY AUTOINCREMENT,
    "state"       INTEGER NOT NULL,
    "name"        TEXT    NOT NULL,
    "color"       INTEGER NOT NULL
);


CREATE TABLE IF NOT EXISTS "Entries"
(
    "id"                INTEGER PRIMARY KEY AUTOINCREMENT,
    "defaultOptionsId"  INTEGER NOT NULL UNIQUE,
    "isTask"            INTEGER NOT NULL,
    FOREIGN KEY ("defaultOptionsId") REFERENCES "EventOptions" ("id")
);


CREATE TABLE IF NOT EXISTS "Tags"
(
    "id"    INTEGER PRIMARY KEY AUTOINCREMENT,
    "name"  TEXT    NOT NULL UNIQUE,
    "color" INTEGER NOT NULL
);


CREATE TABLE IF NOT EXISTS "EntryTagLinks"
(
    "entryId" INTEGER NOT NULL,
    "tagId"   INTEGER NOT NULL,
    PRIMARY KEY ("entryId", "tagId"),
    FOREIGN KEY ("entryId") REFERENCES "Entries" ("id"),
    FOREIGN KEY ("tagId") REFERENCES "Tags" ("id")
);


CREATE TABLE IF NOT EXISTS "Dates"
(
    "id"                    INTEGER PRIMARY KEY AUTOINCREMENT,
    "entryId"               INTEGER NOT NULL,
    "eventOptionsId"        INTEGER NOT NULL,
    "epochSecondChainStart" BIGINT  NOT NULL,
    "duration"              TEXT    NOT NULL,
    "epochSecondChainEnd"   BIGINT  NOT NULL,
    "timesRepeat"           INTEGER NOT NULL,
    "period"                TEXT    NOT NULL,
    "timeZoneId"            TEXT    NOT NULL,
    "occurrenceExceptions"  TEXT    NOT NULL,
    FOREIGN KEY ("entryId") REFERENCES "Entries" ("id"),
    FOREIGN KEY ("eventOptionsId") REFERENCES "EventOptions" ("id")
);


CREATE TABLE IF NOT EXISTS "Notifications"
(
    "eventOptionsId"    INTEGER     NOT NULL,
    "timeOffset"        VARCHAR(18) NOT NULL,
    "hasSound"          INTEGER     NOT NULL,
    PRIMARY KEY ("eventOptionsId", "timeOffset"),
    FOREIGN KEY ("eventOptionsId") REFERENCES "eventOptions" ("id")
);


CREATE TABLE IF NOT EXISTS "Tasks"
(
    "entryId"                   INTEGER PRIMARY KEY NOT NULL UNIQUE,
    "startConstraintTimestamp"  BIGINT,
    "deadlineTimestamp"         BIGINT  NOT NULL,
    "timeZoneId"                TEXT    NOT NULL,
    "timeConsumed"              INTEGER NOT NULL,
    "timeRemaining"             INTEGER NOT NULL,
    FOREIGN KEY ("entryId") REFERENCES "Entries" ("id")
);


CREATE TABLE IF NOT EXISTS "TaskLinks"
(
    "predecessorId"  INTEGER NOT NULL,
    "successorId"   INTEGER NOT NULL,
    PRIMARY KEY ("predecessorId", "successorId"),
    FOREIGN KEY ("predecessorId") REFERENCES "Tasks" ("entryId"),
    FOREIGN KEY ("successorId") REFERENCES "Tasks" ("entryId")
);


CREATE TABLE IF NOT EXISTS "TaskLogs"
(
    "id"                    INTEGER PRIMARY KEY AUTOINCREMENT,
    "taskId"                INTEGER NOT NULL,
    "startOfDayTimestamp"   BIGINT  NOT NULL,
    "timeZoneId"            TEXT    NOT NULL,
    "timeConsumed"          INTEGER NOT NULL,
    FOREIGN KEY ("taskId") REFERENCES "Tasks" ("entryId")
);


DROP TABLE TaskLogs;
DROP TABLE TaskLinks;
DROP TABLE Tasks;
DROP TABLE Notifications;
DROP TABLE Dates;
DROP TABLE EventOptions;
DROP TABLE EntryTagLinks;
DROP TABLE Tags;
DROP TABLE Entries;
