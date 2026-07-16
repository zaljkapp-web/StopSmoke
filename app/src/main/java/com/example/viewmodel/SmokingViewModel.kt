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
    val totalTimerSeconds: Long = 0,
    val isTimerActive: Boolean = false,
    val isDuringShift: Boolean = false,
    val shiftStart: LocalDateTime? = null,
    val shiftEnd: LocalDateTime? = null,
    val currentOverride: DayOverride? = null,
    val logsToday: List<CigaretteLog> = emptyList(),
    val allLogs: List<CigaretteLog> = emptyList(),
    val dayStatsList: List<DayStats> = emptyList(),
    val lastCigaretteTime: LocalDateTime? = null,
    val needsInitialization: Boolean = false,
    val secondsSinceLastCigarette: Long = 0
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

        viewModelScope.launch {
        }

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
                val now = LocalDateTime.now()
                
                val nextAllowed = state.nextAllowedTime
                val timerSeconds = if (nextAllowed != null) {
                    val seconds = ChronoUnit.SECONDS.between(now, nextAllowed)
                    maxOf(0L, seconds)
                } else {
                    0L
                }
                
                val lastCigTime = state.lastCigaretteTime
                val secondsSinceLast = if (lastCigTime != null) {
                    val seconds = ChronoUnit.SECONDS.between(lastCigTime, now)
                    maxOf(0L, seconds)
                } else {
                    0L
                }

                _uiState.update {
                    it.copy(
                        timerSecondsRemaining = timerSeconds,
                        isTimerActive = nextAllowed != null && timerSeconds > 0,
                        secondsSinceLastCigarette = secondsSinceLast
                    )
                }
            }
        }
    }

    private fun updateState(logs: List<CigaretteLog>, overridesList: List<DayOverride>) {
        val now = LocalDateTime.now()
        val overridesMap = overridesList.associateBy { it.dateString }

        // 1. Determine shift context for UI based on original shift logic
        val shiftDay = SmokingScheduleHelper.getSmokingDayForTimestamp(now, overridesMap)
        val activeShift = SmokingScheduleHelper.getActiveShiftType(shiftDay, overridesMap[shiftDay.toString()])
        val currentOverride = overridesMap[shiftDay.toString()]
        val shiftTimes = SmokingScheduleHelper.getShiftTimes(shiftDay, activeShift, currentOverride)
        val isDuringShift = if (shiftTimes != null) {
            now.isAfter(shiftTimes.first) && now.isBefore(shiftTimes.second)
        } else {
            false
        }

        // 2. The new requirement: 00:00 to 24:00 counting every day.
        val smokingDay = now.toLocalDate() // STRICTLY today's calendar day

        // 3. Compute historic & daily stats recursively to calculate rollover
        val statsList = mutableListOf<DayStats>()
        var rollover = 0
        
        // Iterate day-by-day from the first log date (or today) to today
        val firstLogDateStr = logs.minByOrNull { it.timestamp }?.dateString
        val firstLogDate = firstLogDateStr?.let { 
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        } ?: smokingDay
        
        var d = maxOf(SmokingScheduleHelper.START_DATE, firstLogDate)
        val maxDay = maxOf(smokingDay, LocalDate.now())
        
        while (!d.isAfter(maxDay)) {
            val dStr = d.toString()
            val baseLimit = SmokingScheduleHelper.getStandardLimit(d)
            val dActiveShift = SmokingScheduleHelper.getActiveShiftType(d, overridesMap[dStr])
            
            val dayLogs = logs.filter { it.dateString == dStr }
            val smoked = dayLogs.sumOf { it.amount }
            
            val totalLimit = baseLimit + rollover
            val actualRemaining = totalLimit - smoked
            val displayRemaining = maxOf(0, actualRemaining)
            
            statsList.add(
                DayStats(
                    date = d,
                    baseLimit = baseLimit,
                    rollover = rollover,
                    totalLimit = totalLimit,
                    smoked = smoked,
                    remaining = displayRemaining,
                    activeShift = dActiveShift
                )
            )

            // If it is before current smoking day, compute rollover for next day
            if (d.isBefore(smokingDay)) {
                rollover = actualRemaining
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
        val remainingToday = maxOf(0, currentTotalLimit - smokedToday)

        // Calculate timer / next allowed time
        val (nextAllowedTime, totalTimerSeconds) = calculateNextAllowedTime(
            now = now,
            smokingDay = smokingDay,
            activeShift = activeShift,
            shiftTimes = shiftTimes,
            currentTotalLimit = currentTotalLimit,
            smokedToday = smokedToday,
            logsToday = logsToday
        )

        val secondsRemaining = if (nextAllowedTime != null) {
            maxOf(0, ChronoUnit.SECONDS.between(now, nextAllowedTime))
        } else {
            0
        }

        val lastLog = logs.maxByOrNull { it.timestamp }
        val lastCigaretteTime = if (lastLog != null) {
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastLog.timestamp), java.time.ZoneId.systemDefault())
        } else {
            null
        }

        val secondsSinceLastCigarette = if (lastCigaretteTime != null) {
            maxOf(0, ChronoUnit.SECONDS.between(lastCigaretteTime, now))
        } else {
            0L
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
                totalTimerSeconds = totalTimerSeconds,
                isTimerActive = nextAllowedTime != null && secondsRemaining > 0,
                isDuringShift = isDuringShift,
                shiftStart = shiftTimes?.first,
                shiftEnd = shiftTimes?.second,
                currentOverride = currentOverride,
                logsToday = logsToday,
                allLogs = logs,
                dayStatsList = statsList,
                lastCigaretteTime = lastCigaretteTime,
                secondsSinceLastCigarette = secondsSinceLastCigarette,
                needsInitialization = logs.isEmpty()
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
        currentTotalLimit: Int,
        smokedToday: Int,
        logsToday: List<CigaretteLog>
    ): Pair<LocalDateTime?, Long> {
        val remainingToday = maxOf(0, currentTotalLimit - smokedToday)
        
        // 48-hour rolling window for every day
        val windowEnd = LocalDateTime.of(smokingDay.plusDays(2), java.time.LocalTime.MIDNIGHT)
        val dayStart = LocalDateTime.of(smokingDay, java.time.LocalTime.MIDNIGHT)
        
        if (now.isAfter(windowEnd)) return Pair(null, 0L)

        val lastLog = logsToday.maxByOrNull { it.timestamp }
        val lastLogTime = lastLog?.let {
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), java.time.ZoneId.systemDefault())
        }

        var baseTime = lastLogTime ?: dayStart

        if (shiftTimes != null) {
            val shiftStart = shiftTimes.first
            val shiftEnd = shiftTimes.second

            if (now.isAfter(shiftStart) && baseTime.isBefore(shiftStart)) {
                baseTime = shiftStart
            }
            if (now.isAfter(shiftEnd) && baseTime.isBefore(shiftEnd)) {
                baseTime = shiftEnd
            }
        }
        
        val tomorrowBaseLimit = SmokingScheduleHelper.getStandardLimit(smokingDay.plusDays(1))
        val actualRemainingToday = currentTotalLimit - smokedToday
        val remaining48h = actualRemainingToday + tomorrowBaseLimit
        val remainingCount = maxOf(0, remaining48h)

        if (remainingCount <= 0) return Pair(null, 0L)

        val secondsLeftAtBaseTime = ChronoUnit.SECONDS.between(baseTime, windowEnd)
        if (secondsLeftAtBaseTime > 0) {
            val intervalSeconds = secondsLeftAtBaseTime / remainingCount
            val candidate = baseTime.plusSeconds(intervalSeconds)
            return Pair(if (candidate.isAfter(windowEnd)) windowEnd else candidate, intervalSeconds)
        }

        return Pair(null, 0L)
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

    fun completeInitialization(bonusCigarettes: Int, smokedToday: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            
            val yesterdayLimit = SmokingScheduleHelper.getStandardLimit(yesterday)
            val smokedYesterdayAmount = yesterdayLimit - bonusCigarettes
            
            // Log for yesterday to set up the exact bonus
            repository.insertLog(
                CigaretteLog(
                    timestamp = LocalDateTime.now().minusDays(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
                    amount = smokedYesterdayAmount,
                    type = "INITIALIZATION",
                    dateString = yesterday.toString()
                )
            )
            
            // Log for today's smoked cigarettes
            if (smokedToday > 0) {
                repository.insertLog(
                    CigaretteLog(
                        timestamp = System.currentTimeMillis() - 10000,
                        amount = smokedToday,
                        type = "INITIALIZATION",
                        dateString = today.toString()
                    )
                )
            } else {
                repository.insertLog(
                    CigaretteLog(
                        timestamp = System.currentTimeMillis() - 10000,
                        amount = 0,
                        type = "INITIALIZATION",
                        dateString = today.toString()
                    )
                )
            }
        }
    }
}
