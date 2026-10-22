package com.example

import com.example.data.model.WorkShiftEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun standardShift_calculatesCorrectDurationAndEarnings() {
        // 9:00 AM to 5:30 PM (8.5 hrs gross) with 30 min unpaid break = 8.0 hrs net
        val shift = WorkShiftEntity(
            dateString = "2026-10-20",
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 30,
            breakMinutes = 30,
            hourlyRate = 25.0
        )

        assertEquals(510, shift.grossMinutes) // 8h 30m = 510m
        assertEquals(480, shift.netMinutes)   // 510 - 30 = 480m
        assertEquals(8.0, shift.hoursWorked, 0.001)
        assertEquals(200.0, shift.totalEarnings, 0.001) // 8 * 25.0 = 200.0
    }

    @Test
    fun overnightShift_handlesMidnightRolloverAccurately() {
        // 10:00 PM (22:00) to 6:00 AM next morning with 0 min break = 8.0 hrs
        val shift = WorkShiftEntity(
            dateString = "2026-10-20",
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            breakMinutes = 0,
            hourlyRate = 35.0
        )

        assertEquals(480, shift.grossMinutes) // 8h
        assertEquals(480, shift.netMinutes)
        assertEquals(8.0, shift.hoursWorked, 0.001)
        assertEquals(280.0, shift.totalEarnings, 0.001) // 8 * 35.0 = 280.0
    }

    @Test
    fun dynamicHourlyRate_calculatesPreciseEarnings() {
        // 8:00 AM to 4:00 PM with 45 min break @ $32.50/hr
        // 8h = 480m - 45m = 435m = 7.25 hrs * $32.50 = $235.625
        val shift = WorkShiftEntity(
            dateString = "2026-10-21",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            breakMinutes = 45,
            hourlyRate = 32.50
        )

        assertEquals(435, shift.netMinutes)
        assertEquals(7.25, shift.hoursWorked, 0.001)
        assertEquals(235.625, shift.totalEarnings, 0.001)
    }
}
