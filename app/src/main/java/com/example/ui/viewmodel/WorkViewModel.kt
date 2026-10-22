package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.WorkShiftEntity
import com.example.data.repository.WorkRepository
import com.example.data.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class HistoryFilter {
    ALL,
    ACTIVE,
    ARCHIVED
}

data class DaySummary(
    val dayOfWeek: String, // "Mon", "Tue", etc.
    val dateString: String,
    val dayOfMonth: Int,
    val totalHours: Double,
    val totalEarnings: Double,
    val isToday: Boolean
)

data class WeekSummaryData(
    val weekRangeDisplay: String,
    val totalHours: Double,
    val totalEarnings: Double,
    val effectiveHourlyRate: Double,
    val shiftsCount: Int,
    val days: List<DaySummary>,
    val shifts: List<WorkShiftEntity>
)

data class MonthWeekData(
    val weekLabel: String,
    val dateRange: String,
    val totalHours: Double,
    val totalEarnings: Double
)

data class MonthSummaryData(
    val monthYearDisplay: String,
    val totalHours: Double,
    val totalEarnings: Double,
    val effectiveHourlyRate: Double,
    val shiftsCount: Int,
    val workDaysCount: Int,
    val avgHoursPerWorkDay: Double,
    val weeklyBreakdown: List<MonthWeekData>,
    val shifts: List<WorkShiftEntity>
)

class WorkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WorkRepository(database.workShiftDao(), application)

        viewModelScope.launch {
            val existing = repository.allShifts.first()
            if (existing.isEmpty()) {
                repository.seedSampleShiftsIfEmpty(0)
            }
        }
    }

    val activeShifts: StateFlow<List<WorkShiftEntity>> = repository.activeShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedShifts: StateFlow<List<WorkShiftEntity>> = repository.archivedShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShifts: StateFlow<List<WorkShiftEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currencySymbol = MutableStateFlow(repository.getCurrencySymbol())
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _defaultRate = MutableStateFlow(repository.getDefaultHourlyRate())
    val defaultRate: StateFlow<Double> = _defaultRate.asStateFlow()

    private val _defaultBreakMinutes = MutableStateFlow(repository.getDefaultBreakMinutes())
    val defaultBreakMinutes: StateFlow<Int> = _defaultBreakMinutes.asStateFlow()

    // Weekly Summary state
    private val _selectedWeekStart = MutableStateFlow(DateTimeUtils.getStartOfWeek(Calendar.getInstance()))
    val selectedWeekStart: StateFlow<Calendar> = _selectedWeekStart.asStateFlow()

    // Monthly Summary state
    private val _selectedMonth = MutableStateFlow(DateTimeUtils.getStartOfMonth(Calendar.getInstance()))
    val selectedMonth: StateFlow<Calendar> = _selectedMonth.asStateFlow()

    // History screen filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    // Filtered shifts for history
    val filteredHistoryShifts: StateFlow<List<WorkShiftEntity>> = combine(
        allShifts,
        _searchQuery,
        _historyFilter
    ) { shifts, query, filter ->
        shifts.filter { shift ->
            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.ACTIVE -> !shift.isArchived
                HistoryFilter.ARCHIVED -> shift.isArchived
            }
            val matchesQuery = query.isBlank() ||
                    shift.note.contains(query, ignoreCase = true) ||
                    shift.dateString.contains(query, ignoreCase = true) ||
                    DateTimeUtils.formatDisplayDate(shift.dateString).contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly summary calculation
    val weeklySummary: StateFlow<WeekSummaryData> = combine(
        allShifts,
        _selectedWeekStart
    ) { shifts, weekStart ->
        val weekEnd = (weekStart.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        val startStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(weekStart.time)
        val endStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(weekEnd.time)

        val weekShifts = shifts.filter { it.dateString in startStr..endStr }
        val totalHours = weekShifts.sumOf { it.hoursWorked }
        val totalEarnings = weekShifts.sumOf { it.totalEarnings }
        val effectiveRate = if (totalHours > 0) totalEarnings / totalHours else 0.0

        val daysList = mutableListOf<DaySummary>()
        val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val todayStr = DateTimeUtils.todayDateString()

        val iterCal = weekStart.clone() as Calendar
        for (i in 0..6) {
            val iterStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(iterCal.time)
            val dayShifts = weekShifts.filter { it.dateString == iterStr }
            val dayHours = dayShifts.sumOf { it.hoursWorked }
            val dayPay = dayShifts.sumOf { it.totalEarnings }

            daysList.add(
                DaySummary(
                    dayOfWeek = dayNames[i],
                    dateString = iterStr,
                    dayOfMonth = iterCal.get(Calendar.DAY_OF_MONTH),
                    totalHours = dayHours,
                    totalEarnings = dayPay,
                    isToday = iterStr == todayStr
                )
            )
            iterCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        WeekSummaryData(
            weekRangeDisplay = DateTimeUtils.formatWeekRange(weekStart, weekEnd),
            totalHours = totalHours,
            totalEarnings = totalEarnings,
            effectiveHourlyRate = effectiveRate,
            shiftsCount = weekShifts.size,
            days = daysList,
            shifts = weekShifts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeekSummaryData("", 0.0, 0.0, 0.0, 0, emptyList(), emptyList())
    )

    // Monthly summary calculation
    val monthlySummary: StateFlow<MonthSummaryData> = combine(
        allShifts,
        _selectedMonth
    ) { shifts, monthCal ->
        val monthStart = DateTimeUtils.getStartOfMonth(monthCal)
        val monthEnd = DateTimeUtils.getEndOfMonth(monthCal)

        val startStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(monthStart.time)
        val endStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(monthEnd.time)

        val monthShifts = shifts.filter { it.dateString in startStr..endStr }
        val totalHours = monthShifts.sumOf { it.hoursWorked }
        val totalEarnings = monthShifts.sumOf { it.totalEarnings }
        val effectiveRate = if (totalHours > 0) totalEarnings / totalHours else 0.0

        val distinctDays = monthShifts.map { it.dateString }.distinct().size
        val avgHoursPerDay = if (distinctDays > 0) totalHours / distinctDays else 0.0

        // Weekly breakdown in month
        val weeklyBreakdown = mutableListOf<MonthWeekData>()
        val calIter = monthStart.clone() as Calendar
        var weekIndex = 1

        while (calIter.timeInMillis <= monthEnd.timeInMillis) {
            val weekStartCal = calIter.clone() as Calendar
            val weekEndCal = calIter.clone() as Calendar
            weekEndCal.add(Calendar.DAY_OF_MONTH, 6)
            if (weekEndCal.after(monthEnd)) {
                weekEndCal.time = monthEnd.time
            }

            val wStartStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(weekStartCal.time)
            val wEndStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(weekEndCal.time)

            val wShifts = monthShifts.filter { it.dateString in wStartStr..wEndStr }
            val wHours = wShifts.sumOf { it.hoursWorked }
            val wPay = wShifts.sumOf { it.totalEarnings }

            val labelRange = "${SimpleDateFormat("MMM d", Locale.getDefault()).format(weekStartCal.time)} – ${SimpleDateFormat("MMM d", Locale.getDefault()).format(weekEndCal.time)}"
            weeklyBreakdown.add(
                MonthWeekData(
                    weekLabel = "Week $weekIndex",
                    dateRange = labelRange,
                    totalHours = wHours,
                    totalEarnings = wPay
                )
            )

            weekIndex++
            calIter.add(Calendar.DAY_OF_MONTH, 7)
        }

        MonthSummaryData(
            monthYearDisplay = DateTimeUtils.formatMonthYear(monthCal),
            totalHours = totalHours,
            totalEarnings = totalEarnings,
            effectiveHourlyRate = effectiveRate,
            shiftsCount = monthShifts.size,
            workDaysCount = distinctDays,
            avgHoursPerWorkDay = avgHoursPerDay,
            weeklyBreakdown = weeklyBreakdown,
            shifts = monthShifts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthSummaryData("", 0.0, 0.0, 0.0, 0, 0, 0.0, emptyList(), emptyList())
    )

    // User Actions
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onHistoryFilterChanged(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun navigateWeek(deltaWeeks: Int) {
        val newCal = _selectedWeekStart.value.clone() as Calendar
        newCal.add(Calendar.WEEK_OF_YEAR, deltaWeeks)
        _selectedWeekStart.value = DateTimeUtils.getStartOfWeek(newCal)
    }

    fun resetWeekToCurrent() {
        _selectedWeekStart.value = DateTimeUtils.getStartOfWeek(Calendar.getInstance())
    }

    fun navigateMonth(deltaMonths: Int) {
        val newCal = _selectedMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, deltaMonths)
        _selectedMonth.value = DateTimeUtils.getStartOfMonth(newCal)
    }

    fun resetMonthToCurrent() {
        _selectedMonth.value = DateTimeUtils.getStartOfMonth(Calendar.getInstance())
    }

    fun saveShift(
        id: Long = 0,
        dateString: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        breakMinutes: Int,
        hourlyRate: Double,
        note: String
    ) {
        viewModelScope.launch {
            // Also remember the last used rate for user convenience
            if (hourlyRate > 0) {
                repository.setDefaultHourlyRate(hourlyRate)
                _defaultRate.value = hourlyRate
            }
            if (breakMinutes >= 0) {
                repository.setDefaultBreakMinutes(breakMinutes)
                _defaultBreakMinutes.value = breakMinutes
            }

            val entity = WorkShiftEntity(
                id = id,
                dateString = dateString,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                breakMinutes = breakMinutes,
                hourlyRate = hourlyRate,
                note = note.trim(),
                isArchived = false
            )
            if (id == 0L) {
                repository.insertShift(entity)
            } else {
                repository.updateShift(entity)
            }
        }
    }

    fun toggleArchiveShift(shift: WorkShiftEntity) {
        viewModelScope.launch {
            repository.setArchived(shift.id, !shift.isArchived)
        }
    }

    fun setShiftArchived(shiftId: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(shiftId, archived)
        }
    }

    fun deleteShift(shift: WorkShiftEntity) {
        viewModelScope.launch {
            repository.deleteShift(shift)
        }
    }

    fun archiveOlderThanDays(days: Int, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.archiveOlderThanDays(days)
            onComplete?.invoke(count)
        }
    }

    fun archiveAllActive(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.archiveAllActive()
            onComplete?.invoke(count)
        }
    }

    fun restoreAllArchived(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.restoreAllArchived()
            onComplete?.invoke(count)
        }
    }

    fun deleteAllArchived(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.deleteAllArchived()
            onComplete?.invoke(count)
        }
    }

    fun setCurrency(symbol: String) {
        repository.setCurrencySymbol(symbol)
        _currencySymbol.value = symbol
    }

    fun setDefaultHourlyRate(rate: Double) {
        repository.setDefaultHourlyRate(rate)
        _defaultRate.value = rate
    }

    /**
     * Generates a clean, copyable timesheet text summary
     */
    fun generateTimesheetReport(): String {
        val shifts = allShifts.value
        val symbol = currencySymbol.value
        val sb = StringBuilder()
        sb.appendLine("=== ShiftFlow Work Hours Summary ===")
        sb.appendLine("Generated: ${DateTimeUtils.formatDisplayDate(DateTimeUtils.todayDateString())}")
        val totalHours = shifts.sumOf { it.hoursWorked }
        val totalPay = shifts.sumOf { it.totalEarnings }
        sb.appendLine("Total Shifts: ${shifts.size}")
        sb.appendLine("Total Hours: ${DateTimeUtils.formatHours(totalHours)}")
        sb.appendLine("Total Earnings: ${DateTimeUtils.formatCurrency(totalPay, symbol)}")
        sb.appendLine("------------------------------------")
        shifts.forEach { s ->
            val status = if (s.isArchived) "[Archived] " else ""
            sb.appendLine("${s.dateString} | ${s.formattedTimeRange()} (${s.formattedDuration()}) @ $symbol${s.hourlyRate}/h = $symbol${String.format(Locale.US, "%.2f", s.totalEarnings)} $status${s.note}")
        }
        sb.appendLine("====================================")
        return sb.toString()
    }
}
