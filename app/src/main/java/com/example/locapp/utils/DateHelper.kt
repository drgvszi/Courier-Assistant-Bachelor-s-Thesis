package com.example.locapp.utils

import java.util.Calendar
import java.util.Date

class DateHelper {
    fun isDateInRange(date: Date, startDate: Date, endDate: Date): Boolean {

        val calendar = Calendar.getInstance()
        calendar.time = date

        val startCal = Calendar.getInstance()
        startCal.time = startDate
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance()
        endCal.time = endDate
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        return calendar.timeInMillis in startCal.timeInMillis..endCal.timeInMillis
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = android.icu.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = android.icu.util.Calendar.getInstance().apply { time = date2 }
        return cal1.get(android.icu.util.Calendar.YEAR) == cal2.get(android.icu.util.Calendar.YEAR) &&
                cal1.get(android.icu.util.Calendar.MONTH) == cal2.get(android.icu.util.Calendar.MONTH) &&
                cal1.get(android.icu.util.Calendar.DAY_OF_MONTH) == cal2.get(android.icu.util.Calendar.DAY_OF_MONTH)
    }

    fun parseTimeToSeconds(time: String): Int {
        val parts = time.split(":")
        if (parts.size == 3) {
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            val seconds = parts[2].toInt()
            return hours * 3600 + minutes * 60 + seconds
        }
        return 0
    }

    fun formatTime(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


}