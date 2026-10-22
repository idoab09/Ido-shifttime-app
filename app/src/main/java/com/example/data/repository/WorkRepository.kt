package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.WorkShiftDao
import com.example.data.model.WorkShiftEntity
import com.example.data.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkRepository(
    private val dao: WorkShiftDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shiftflow_prefs", Context.MODE_PRIVATE)

    val allShifts: Flow<List<WorkShiftEntity>> = dao.getAllShifts()
    val activeShifts: Flow<List<WorkShiftEntity>> = dao.getActiveShifts()
    val archivedShifts: Flow<List<WorkShiftEntity>> = dao.getArchivedShifts()

    fun getDefaultHourlyRate(): Double {
        return prefs.getFloat("default_hourly_rate", 25.0f).toDouble()
    }

    fun setDefaultHourlyRate(rate: Double) {
        prefs.edit().putFloat("default_hourly_rate", rate.toFloat()).apply()
    }

    fun getCurrencySymbol(): String {
        return prefs.getString("currency_symbol", "$") ?: "$"
    }

    fun setCurrencySymbol(symbol: String) {
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    fun getDefaultBreakMinutes(): Int {
        return prefs.getInt("default_break_minutes", 0)
    }

    fun setDefaultBreakMinutes(minutes: Int) {
        prefs.edit().putInt("default_break_minutes", minutes).apply()
    }

    suspend fun insertShift(shift: WorkShiftEntity): Long {
        return dao.insertShift(shift)
    }

    suspend fun updateShift(shift: WorkShiftEntity) {
        dao.updateShift(shift)
    }

    suspend fun deleteShift(shift: WorkShiftEntity) {
        dao.deleteShift(shift)
    }

    suspend fun deleteShiftById(id: Long) {
        dao.deleteShiftById(id)
    }

    suspend fun setArchived(id: Long, isArchived: Boolean) {
        dao.setArchived(id, isArchived)
    }

    suspend fun archiveOlderThanDays(days: Int): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        val cutoffDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        return dao.archiveOlderThan(cutoffDate)
    }

    suspend fun archiveAllActive(): Int {
        return dao.archiveAllActive()
    }

    suspend fun restoreAllArchived(): Int {
        return dao.restoreAllArchived()
    }

    suspend fun deleteAllArchived(): Int {
        return dao.deleteAllArchived()
    }

    /**
     * Seeds initial demo shifts for a warm first launch experience
     * if the database is completely empty.
     */
    suspend fun seedSampleShiftsIfEmpty(existingCount: Int) {
        if (existingCount > 0) return

        val cal = Calendar.getInstance()
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val samples = mutableListOf<WorkShiftEntity>()

        // Today shift
        samples.add(
            WorkShiftEntity(
                dateString = iso.format(cal.time),
                startHour = 9,
                startMinute = 0,
                endHour = 17,
                endMinute = 30,
                breakMinutes = 30,
                hourlyRate = 30.0,
                note = "Design sprint & Client sync",
                isArchived = false
            )
        )

        // Yesterday shift (different rate!)
        val calYesterday = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        samples.add(
            WorkShiftEntity(
                dateString = iso.format(calYesterday.time),
                startHour = 8,
                startMinute = 30,
                endHour = 17,
                endMinute = 0,
                breakMinutes = 30,
                hourlyRate = 32.5,
                note = "Weekend on-call rate",
                isArchived = false
            )
        )

        // 3 days ago shift
        val cal3Days = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -3) }
        samples.add(
            WorkShiftEntity(
                dateString = iso.format(cal3Days.time),
                startHour = 9,
                startMinute = 0,
                endHour = 16,
                endMinute = 0,
                breakMinutes = 0,
                hourlyRate = 28.0,
                note = "Field inspection",
                isArchived = false
            )
        )

        // 5 days ago shift
        val cal5Days = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -5) }
        samples.add(
            WorkShiftEntity(
                dateString = iso.format(cal5Days.time),
                startHour = 10,
                startMinute = 0,
                endHour = 18,
                endMinute = 0,
                breakMinutes = 45,
                hourlyRate = 28.0,
                note = "Office shift",
                isArchived = false
            )
        )

        // 35 days ago archived shift
        val calOld = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -35) }
        samples.add(
            WorkShiftEntity(
                dateString = iso.format(calOld.time),
                startHour = 9,
                startMinute = 0,
                endHour = 17,
                endMinute = 0,
                breakMinutes = 30,
                hourlyRate = 25.0,
                note = "Archived project delivery",
                isArchived = true
            )
        )

        for (sample in samples) {
            dao.insertShift(sample)
        }
    }
}
