package com.college.attendance.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatShortDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatTime(timeStr: String): String {
        return try {
            if (timeStr.contains(":")) {
                val parts = timeStr.split(":")
                val hour = parts[0].toInt()
                val min = parts[1]
                val ampm = if (hour >= 12) "PM" else "AM"
                val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                String.format("%02d:%s %s", h12, min, ampm)
            } else {
                timeStr
            }
        } catch (e: Exception) {
            timeStr
        }
    }
}
