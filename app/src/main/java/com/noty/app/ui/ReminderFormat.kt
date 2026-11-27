package com.noty.app.ui

import android.content.Context
import android.text.format.DateUtils
import com.noty.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A scheduled time as the user should read it: "Today, 4:24 PM", "Tomorrow, 9:00 AM", otherwise
 * "Nov 28, 4:24 PM" (with the year once it isn't this year's). Shared because the note card and
 * the editor both display one, and they must agree: a card reading one thing beside an editor
 * reading another is the kind of mismatch nobody reports and everybody notices.
 */
internal fun formatReminderTime(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String {
    val time = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME)
    return when (daysFromToday(millis, now, ZoneId.systemDefault())) {
        0L -> context.getString(R.string.schedule_today, time)
        1L -> context.getString(R.string.schedule_tomorrow, time)
        else -> DateUtils.formatDateTime(
            context, millis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
        )
    }
}

/** Calendar days from [now]'s date to [millis]'s date in [zone]: 0 today, 1 tomorrow, -1 yesterday. */
internal fun daysFromToday(millis: Long, now: Long, zone: ZoneId): Long =
    ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    )
