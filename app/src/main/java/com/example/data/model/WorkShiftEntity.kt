package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "work_shifts")
data class WorkShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String, // "YYYY-MM-DD"
    val startHour: Int,     // 0..23
    val startMinute: Int,   // 0..59
    val endHour: Int,       // 0..23
    val endMinute: Int,     // 0..59
    val breakMinutes: Int = 0,
    val hourlyRate: Double = 0.0,
    val note: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Total elapsed minutes from start time to end time,
     * seamlessly handling overnight shifts (e.g., 22:00 to 06:00).
     */
    val grossMinutes: Int
        get() {
            val startTotal = startHour * 60 + startMinute
            val endTotal = endHour * 60 + endMinute
            return if (endTotal < startTotal) {
                (24 * 60 - startTotal) + endTotal
            } else {
                endTotal - startTotal
            }
        }

    /**
     * Net paid working minutes after subtracting unpaid break duration.
     */
    val netMinutes: Int
        get() = maxOf(0, grossMinutes - breakMinutes)

    /**
     * Total paid hours as a decimal (e.g., 7.5 hrs).
     */
    val hoursWorked: Double
        get() = netMinutes / 60.0

    /**
     * Total gross earnings for this shift: hoursWorked * hourlyRate.
     */
    val totalEarnings: Double
        get() = hoursWorked * hourlyRate

    /**
     * Formats duration nicely, e.g. "7h 30m" or "8h".
     */
    fun formattedDuration(): String {
        val h = netMinutes / 60
        val m = netMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    /**
     * Formats time span, e.g. "09:00 AM – 05:30 PM".
     */
    fun formattedTimeRange(is24Hour: Boolean = false): String {
        return "${formatTime(startHour, startMinute, is24Hour)} – ${formatTime(endHour, endMinute, is24Hour)}"
    }

    companion object {
        fun formatTime(hour: Int, minute: Int, is24Hour: Boolean = false): String {
            return if (is24Hour) {
                String.format(Locale.US, "%02d:%02d", hour, minute)
            } else {
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when (hour % 12) {
                    0 -> 12
                    else -> hour % 12
                }
                String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
            }
        }
    }
}
