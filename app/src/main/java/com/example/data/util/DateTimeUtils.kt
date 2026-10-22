package com.example.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private const val DATE_PATTERN = "yyyy-MM-dd"
    private val dateIsoFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)

    fun todayDateString(): String {
        return dateIsoFormat.format(Date())
    }

    fun parseDate(dateString: String): Calendar {
        val cal = Calendar.getInstance()
        try {
            val date = SimpleDateFormat(DATE_PATTERN, Locale.US).parse(dateString)
            if (date != null) {
                cal.time = date
            }
        } catch (_: Exception) { }
        return cal
    }

    fun formatDate(cal: Calendar): String {
        return SimpleDateFormat(DATE_PATTERN, Locale.US).format(cal.time)
    }

    fun formatDisplayDate(dateString: String): String {
        val cal = parseDate(dateString)
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(cal, today) -> "Today, " + SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
            isSameDay(cal, yesterday) -> "Yesterday, " + SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
            else -> SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
        }
    }

    fun formatShortDate(dateString: String): String {
        val cal = parseDate(dateString)
        return SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
    }

    fun formatMonthYear(cal: Calendar): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun formatWeekRange(startCal: Calendar, endCal: Calendar): String {
        val startStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(startCal.time)
        val endStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(endCal.time)
        return "$startStr – $endStr"
    }

    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Returns calendar for the Monday of the week containing the given date.
     */
    fun getStartOfWeek(date: Calendar): Calendar {
        val cal = date.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    /**
     * Returns calendar for the Sunday of the week containing the given date.
     */
    fun getEndOfWeek(date: Calendar): Calendar {
        val cal = getStartOfWeek(date)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal
    }

    fun getStartOfMonth(date: Calendar): Calendar {
        val cal = date.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun getEndOfMonth(date: Calendar): Calendar {
        val cal = getStartOfMonth(date)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal
    }

    fun formatCurrency(amount: Double, symbol: String = "$"): String {
        return String.format(Locale.US, "%s%.2f", symbol, amount)
    }

    fun formatHours(hours: Double): String {
        return if (hours % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f hrs", hours)
        } else {
            String.format(Locale.US, "%.1f hrs", hours)
        }
    }
}
