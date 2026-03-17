package oblitusnumen.calendar.implementation.data.views

import androidx.compose.ui.graphics.Color
import oblitusnumen.calendar.implementation.data.DbManager
import oblitusnumen.calendar.implementation.data.ExceptionRules
import oblitusnumen.calendar.implementation.data.Period
import oblitusnumen.calendar.implementation.data.tables.Date
import oblitusnumen.calendar.implementation.data.tables.EventOptions
import oblitusnumen.calendar.implementation.data.tables.Notification
import oblitusnumen.calendar.implementation.defaultZoneId
import oblitusnumen.calendar.implementation.log
import oblitusnumen.calendar.implementation.toColor
import java.time.ZoneId

class ViewNotificationDateWithOptions(
    id: Int,
    entryId: Int,
    eventOptionsId: Int,
    epochSecondChainStart: Long,
    duration: Period,// FIXME: fixup in db in date
    epochSecondChainEnd: Long,// FIXME: account for duration in db in date
    timesRepeat: Long,
    period: Period,
    timeZoneId: ZoneId,
    exceptionRules: ExceptionRules,
    val offset: Period,
    val sound: Boolean,
    val name: String,
    val color: Color
) : Date(
    id,
    entryId,
    eventOptionsId,
    epochSecondChainStart,
    duration,
    epochSecondChainEnd,
    timesRepeat,
    period,
    timeZoneId,
    exceptionRules
) {
    fun nextNotificationTime(from: Long) =
        getNext(from)?.let { offset.addTo(it.withZoneSameInstant(defaultZoneId()), -1).toEpochSecond() }

    companion object {
        fun all(dbManager: DbManager): MutableList<ViewNotificationDateWithOptions> {
            dbManager.readableDatabase.rawQuery(
                "select d.*, ${Notification.COLUMN_NAME_TIME_OFFSET} as timeOffset, " +
                        "${Notification.COLUMN_NAME_HAS_SOUND} as sound, " +
                        "${EventOptions.COLUMN_NAME_NAME} as name, ${EventOptions.COLUMN_NAME_COLOR} as color " +
                        "from $TABLE_NAME d " +
                        "join ${Notification.TABLE_NAME} n " +
                        "on d.${COLUMN_NAME_EVENT_OPTIONS_ID} = n.${Notification.COLUMN_NAME_EVENT_OPTIONS_ID} " +
                        "join ${EventOptions.TABLE_NAME} o " +
                        "on d.${COLUMN_NAME_EVENT_OPTIONS_ID} = o.${EventOptions.COLUMN_NAME_ID}",
                arrayOf()
            ).use { cursor ->
                val views: MutableList<ViewNotificationDateWithOptions> = ArrayList()

                val idxId = cursor.getColumnIndex(COLUMN_NAME_ID)
                val idxEntryId = cursor.getColumnIndex(COLUMN_NAME_ENTRY_ID)
                val idxOptionsId = cursor.getColumnIndex(COLUMN_NAME_EVENT_OPTIONS_ID)
                val idxStart = cursor.getColumnIndex(COLUMN_NAME_EPOCH_SECOND_CHAIN_START)
                val idxDuration = cursor.getColumnIndex(COLUMN_NAME_DURATION)
                val idxEnd = cursor.getColumnIndex(COLUMN_NAME_EPOCH_SECOND_CHAIN_END)
                val idxRepeat = cursor.getColumnIndex(COLUMN_NAME_TIMES_REPEATS)
                val idxPeriod = cursor.getColumnIndex(COLUMN_NAME_PERIOD)
                val idxZone = cursor.getColumnIndex(COLUMN_NAME_TIME_ZONE_ID)
                val idxExceptions = cursor.getColumnIndex(COLUMN_NAME_OCCURRENCE_EXCEPTIONS)
                val idxOffset = cursor.getColumnIndex("timeOffset")
                val idxSound = cursor.getColumnIndex("sound")
                val idxName = cursor.getColumnIndex("name")
                val idxColor = cursor.getColumnIndex("color")

                while (cursor.moveToNext())
                    views.add(
                        ViewNotificationDateWithOptions(
                            cursor.getInt(idxId),
                            cursor.getInt(idxEntryId),
                            cursor.getInt(idxOptionsId),
                            cursor.getLong(idxStart),
                            Period.decode(cursor.getString(idxDuration)),
                            cursor.getLong(idxEnd),
                            cursor.getLong(idxRepeat),
                            Period.decode(cursor.getString(idxPeriod)),
                            ZoneId.of(cursor.getString(idxZone)),
                            ExceptionRules(cursor.getString(idxExceptions)),
                            Period.decode(cursor.getString(idxOffset)),
                            cursor.getInt(idxSound) != 0,
                            cursor.getString(idxName),
                            cursor.getInt(idxColor).toColor() ?: dbManager.defaultEntryColor
                        )
                    )
                log(views)
                return views
            }
        }
    }
}