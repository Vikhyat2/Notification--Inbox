package com.bookverse.notificationinbox.ui.util

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/**
 * Short, locale-aware relative time for list items: "Just now", "5 min. ago",
 * "Yesterday", or a date for anything older than a week.
 */
fun formatRelativeTime(timestamp: Long, now: Long, justNow: String): String {
    if (now - timestamp in 0 until DateUtils.MINUTE_IN_MILLIS) return justNow
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

/** Full, locale-aware date and time for the details screen, e.g. "13 Oct 2026, 9:20 pm". */
fun formatFullDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))

/** Current time that ticks every [intervalMillis], so relative times stay fresh on screen. */
@Composable
fun rememberCurrentTimeMillis(intervalMillis: Long = DateUtils.MINUTE_IN_MILLIS): Long {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(intervalMillis)
            value = System.currentTimeMillis()
        }
    }
    return now
}
