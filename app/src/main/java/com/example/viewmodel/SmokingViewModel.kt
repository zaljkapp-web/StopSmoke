package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

data class DayStats(
    val date: LocalDate,
    val baseLimit: Int,
    val rollover: Int,
    val totalLimit: Int,
    val smoked: Int,
    val remaining: Int,
    val activeShift: ShiftType
)

data class UiState(
    val currentSmokingDay: LocalDate = LocalDate.now(),
    val activeShift: ShiftType = ShiftType.REST,
    val baseLimit: Int = 25,
    val rolloverCigarettes: Int = 0,
    val dailyLimit: Int = 25,
    val smokedToday: Int = 0,
    val remainingToday: Int = 25,
    val nextAllowedTime: LocalDateTime? = null,
    val timerSecondsRemaining: Long = 0,
    val isTimerActive: Boolean = false,
    val isDuringShift: Boolean = false,
    val shiftStart: LocalDateTime? = null,
    val shiftEnd: LocalDateTime? = null,
    val currentOverride: DayOverride? = null,
    val logsToday: List<CigaretteLog> = emptyList(),
    val allLogs: List<CigaretteLog> = emptyList(),
    val dayStatsList: List<DayStats> = emptyList()
)

class SmokingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SmokingRepository
    private val context: Context get() = getApplication()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        val dbHelper = SmokingDatabaseHelper.getInstance(application)
        repository = SmokingRepository(dbHelper)

        // Combine flows to compute full state reactively
        viewModelScope.launch {
            combine(
                repository.allLogsFlow,
                repository.allOverridesFlow
            ) { logs, overrides ->
                Pair(logs, overrides)
            }.collect { (logs, overrides) ->
                updateState(logs, overrides)
            }
        }

        // Start countdown ticking
        startTimerTicker()
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                val nextAllowed = state.nextAllowedTime
                if (nextAllowed != null) {
                    val now = LocalDateTime.now()
                    val seconds = ChronoUnit.SECONDS.between(now, nextAllowed)
                    if (seconds > 0) {
                        _uiState.update {
                            it.copy(
                                timerSecondsRemaining = seconds,
                                isTimerActive = true
                            )
                        }
                    } else {
                        if (state.isTimerActive) {
                            // Just expired
                            _uiState.update {
                                it.copy(
                                    timerSecondsRemaining = 0,
                                    isTimerActive = false
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            timerSecondsRemaining = 0,
                            isTimerActive = false
                        )
                    }
                }
            }
        }
    }

    private fun updateState(logs: List<CigaretteLog>, overridesList: List<DayOverride>) {
        val now = LocalDateTime.now()
        val overridesMap = overridesList.associateBy { it.dateString }

        // 1. Determine current smoking day based on closing times
        val smokingDay = SmokingScheduleHelper.getSmokingDayForTimestamp(now, overridesMap)

        // 2. Compute historic & daily stats recursively to calculate rollover
        val statsList = mutableListOf<DayStats>()
        var rollover = 0
        
        // Iterate day-by-day from START_DATE to today (or smokingDay)
        var d = SmokingScheduleHelper.START_DATE
        val maxDay = maxOf(smokingDay, LocalDate.now())
        
        while (!d.isAfter(maxDay)) {
            val dStr = d.toString()
            val baseLimit = SmokingScheduleHelper.getStandardLimit(d)
            val dayOverride = overridesMap[dStr]
            val activeShift = SmokingScheduleHelper.getActiveShiftType(d, dayOverride)
            
            val dayLogs = logs.filter { it.dateString == dStr }
            val smoked = dayLogs.sumOf { it.amount }
            
            val totalLimit = baseLimit + rollover
            val remaining = max(0, totalLimit - smoked)
            
            statsList.add(
                DayStats(
                    date = d,
                    baseLimit = baseLimit,
                    rollover = rollover,
                    totalLimit = totalLimit,
                    smoked = smoked,
                    remaining = remaining,
                    activeShift = activeShift
                )
            )

            // If it is before current smoking day, compute rollover for next day
            if (d.isBefore(smokingDay)) {
                rollover = remaining
            }
            
            d = d.plusDays(1)
        }

        // Current day's stats
        val todayStats = statsList.firstOrNull { it.date == smokingDay }
        val currentBase = todayStats?.baseLimit ?: SmokingScheduleHelper.getStandardLimit(smokingDay)
        val currentRollover = rollover
        val currentTotalLimit = currentBase + currentRollover
        val logsToday = logs.filter { it.dateString == smokingDay.toString() }
        val smokedToday = logsToday.sumOf { it.amount }
        val remainingToday = max(0, currentTotalLimit - smokedToday)

        val activeShift = SmokingScheduleHelper.getActiveShiftType(smokingDay, overridesMap[smokingDay.toString()])
        val currentOverride = overridesMap[smokingDay.toString()]

        // Shift boundaries
        val shiftTimes = SmokingScheduleHelper.getShiftTimes(smokingDay, activeShift, currentOverride)
        val isDuringShift = if (shiftTimes != null) {
            now.isAfter(shiftTimes.first) && now.isBefore(shiftTimes.second)
        } else {
            false
        }

        // Calculate timer / next allowed time
        val nextAllowedTime = calculateNextAllowedTime(
            now = now,
            smokingDay = smokingDay,
            activeShift = activeShift,
            shiftTimes = shiftTimes,
            isDuringShift = isDuringShift,
            remainingCount = remainingToday,
            logsToday = logsToday,
            currentOverride = currentOverride
        )

        val secondsRemaining = if (nextAllowedTime != null) {
            max(0, ChronoUnit.SECONDS.between(now, nextAllowedTime))
        } else {
            0
        }

        _uiState.update {
            it.copy(
                currentSmokingDay = smokingDay,
                activeShift = activeShift,
                baseLimit = currentBase,
                rolloverCigarettes = currentRollover,
                dailyLimit = currentTotalLimit,
                smokedToday = smokedToday,
                remainingToday = remainingToday,
                nextAllowedTime = nextAllowedTime,
                timerSecondsRemaining = secondsRemaining,
                isTimerActive = nextAllowedTime != null && secondsRemaining > 0,
                isDuringShift = isDuringShift,
                shiftStart = shiftTimes?.first,
                shiftEnd = shiftTimes?.second,
                currentOverride = currentOverride,
                logsToday = logsToday,
                allLogs = logs,
                dayStatsList = statsList
            )
        }

        // Reschedule android system alarms for precise push notifications
        scheduleSystemAlarms(smokingDay, activeShift, shiftTimes, nextAllowedTime)
    }

    private fun calculateNextAllowedTime(
        now: LocalDateTime,
        smokingDay: LocalDate,
        activeShift: ShiftType,
        shiftTimes: Pair<LocalDateTime, LocalDateTime>?,
        isDuringShift: Boolean,
        remainingCount: Int,
        logsToday: List<CigaretteLog>,
        currentOverride: DayOverride?
    ): LocalDateTime? {
        if (remainingCount <= 0) return null

        val closingTime = SmokingScheduleHelper.getClosingTime(smokingDay, activeShift)
        if (now.isAfter(closingTime)) return null

        // If inside shift, calculate "műszakos számláló" (shift timer)
        if (isDuringShift && shiftTimes != null) {
            val shiftEnd = shiftTimes.second
            val secondsLeftInShift = ChronoUnit.SECONDS.between(now, shiftEnd)
            if (secondsLeftInShift > 0) {
                val intervalSeconds = secondsLeftInShift / remainingCount
                val lastLog = logsToday.maxByOrNull { it.timestamp }
                val baseTime = if (lastLog != null) {
                    val lastLogTime = LocalDateTime.ofEpochSecond(lastLog.timestamp / 1000, 0, java.time.ZoneOffset.UTC) // Approximate or just use last log trigger
                    // To keep things simple and avoid time zone parsing mismatch,
                    // if lastLog is within the current shift, we count from it.
                    val lastLogLDT = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastLog.timestamp), java.time.ZoneId.systemDefault())
                    if (lastLogLDT.isAfter(shiftTimes.first)) lastLogLDT else now
                } else {
                    now
                }
                val candidate = baseTime.plusSeconds(intervalSeconds)
                return if (candidate.isAfter(shiftEnd)) shiftEnd else candidate
            }
        }

        // Standard timing (rest day, or before/after shift)
        val secondsLeftInDay = ChronoUnit.SECONDS.between(now, closingTime)
        if (secondsLeftInDay > 0) {
            val intervalSeconds = secondsLeftInDay / remainingCount
            val lastLog = logsToday.maxByOrNull { it.timestamp }
            val baseTime = if (lastLog != null) {
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastLog.timestamp), java.time.ZoneId.systemDefault())
            } else {
                now
            }
            val candidate = baseTime.plusSeconds(intervalSeconds)
            return if (candidate.isAfter(closingTime)) closingTime else candidate
        }

        return null
    }

    private fun scheduleSystemAlarms(
        smokingDay: LocalDate,
        activeShift: ShiftType,
        shiftTimes: Pair<LocalDateTime, LocalDateTime>?,
        nextAllowedTime: LocalDateTime?
    ) {
        // Cancel previous alarms
        SmokingAlarmScheduler.cancelAlarm(context, SmokingAlarmReceiver.ACTION_TIMER_EXPIRED, SmokingAlarmReceiver.NOTIFICATION_ID_TIMER)
        SmokingAlarmScheduler.cancelAlarm(context, SmokingAlarmReceiver.ACTION_SHIFT_START, SmokingAlarmReceiver.NOTIFICATION_ID_SHIFT_START)
        SmokingAlarmScheduler.cancelAlarm(context, SmokingAlarmReceiver.ACTION_SHIFT_END, SmokingAlarmReceiver.NOTIFICATION_ID_SHIFT_END)

        // Schedule timer alarm
        if (nextAllowedTime != null) {
            SmokingAlarmScheduler.scheduleAlarm(
                context,
                nextAllowedTime,
                SmokingAlarmReceiver.ACTION_TIMER_EXPIRED,
                SmokingAlarmReceiver.NOTIFICATION_ID_TIMER
            )
        }

        // Schedule shift start and end alarms
        if (shiftTimes != null) {
            val (start, end) = shiftTimes
            val now = LocalDateTime.now()
            if (start.isAfter(now)) {
                SmokingAlarmScheduler.scheduleAlarm(
                    context,
                    start,
                    SmokingAlarmReceiver.ACTION_SHIFT_START,
                    SmokingAlarmReceiver.NOTIFICATION_ID_SHIFT_START
                )
            }
            if (end.isAfter(now)) {
                SmokingAlarmScheduler.scheduleAlarm(
                    context,
                    end,
                    SmokingAlarmReceiver.ACTION_SHIFT_END,
                    SmokingAlarmReceiver.NOTIFICATION_ID_SHIFT_END
                )
            }
        }
    }

    // Interactive UI Actions

    /**
     * Log a cigarette
     */
    fun logCigarette(type: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val state = _uiState.value
            val currentDayStr = state.currentSmokingDay.toString()

            val amount = if (type == "COFFEE") 2 else 1
            val newLog = CigaretteLog(
                timestamp = System.currentTimeMillis(),
                amount = amount,
                type = type,
                dateString = currentDayStr
            )
            repository.insertLog(newLog)
        }
    }

    /**
     * Delete last log (undo action)
     */
    fun deleteLastLog() {
        viewModelScope.launch {
            val state = _uiState.value
            val lastLog = state.logsToday.maxByOrNull { it.timestamp }
            if (lastLog != null) {
                repository.deleteLog(lastLog)
            }
        }
    }

    /**
     * Save Vacation Override
     */
    fun updateVacation(isVacation: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentDayStr = state.currentSmokingDay.toString()
            val existing = repository.getOverrideForDay(currentDayStr)
            
            val updated = existing?.copy(isVacation = isVacation) 
                ?: DayOverride(dateString = currentDayStr, isVacation = isVacation)
            
            repository.insertOrUpdateOverride(updated)
        }
    }

    /**
     * Update Overtime hours
     */
    fun updateOvertime(startHours: Int, endHours: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentDayStr = state.currentSmokingDay.toString()
            val existing = repository.getOverrideForDay(currentDayStr)

            val updated = existing?.copy(overtimeStartHours = startHours, overtimeEndHours = endHours)
                ?: DayOverride(dateString = currentDayStr, overtimeStartHours = startHours, overtimeEndHours = endHours)

            repository.insertOrUpdateOverride(updated)
        }
    }

    /**
     * Force reset/zero the timer (for shift end) and reward with shift-end cigarette
     */
    fun triggerShiftEndReward() {
        viewModelScope.launch {
            val state = _uiState.value
            // Reset countdown immediately
            _uiState.update {
                it.copy(
                    nextAllowedTime = LocalDateTime.now(),
                    timerSecondsRemaining = 0,
                    isTimerActive = false
                )
            }
            // Reward with shift-end cig
            logCigarette("SHIFT_END")
        }
    }
}
