package com.kobe.qrbarcode.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormat {

    fun relative(millis: Long, now: Long = System.currentTimeMillis()): String {
        val delta = now - millis
        return when {
            delta < 60_000 -> "Just now"
            delta < 3_600_000 -> "${delta / 60_000} min ago"
            isSameDay(millis, now) -> "Today · " + time(millis)
            isSameDay(millis, now - 86_400_000) -> "Yesterday · " + time(millis)
            delta < 7 * 86_400_000L -> SimpleDateFormat("EEE · HH:mm", Locale.getDefault())
                .format(Date(millis))

            else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    fun full(millis: Long): String =
        SimpleDateFormat("d MMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date(millis))

    fun time(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    fun stamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))

    private fun isSameDay(first: Long, second: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = first }
        val b = Calendar.getInstance().apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
