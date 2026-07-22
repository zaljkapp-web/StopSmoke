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
    val smoked: Int,
    val activeShift: ShiftType
)

data class UiState(
    val currentSmokingDay: LocalDate = LocalDate.now(),
    val activeShift: ShiftType = ShiftType.REST,
    val smokedToday: Int = 0,
    val nextAllowedTime: LocalDateTime? = null,
    val timerSecondsRemaining: Long = 0,
    val totalTimerSeconds: Long = 4200,
    val isTimerActive: Boolean = false,
    val isDuringShift: Boolean = false,
    val isWaitingForBonus: Boolean = false,
    val shiftStart: LocalDateTime? = null,
    val shiftEnd: LocalDateTime? = null,
    val currentOverride: DayOverride? = null,
    val logsToday: List<CigaretteLog> = emptyList(),
    val allLogs: List<CigaretteLog> = emptyList(),
    val dayStatsList: List<DayStats> = emptyList(),
    val lastCigaretteTime: LocalDateTime? = null,
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

        // Determine shift context
        val shiftDay = SmokingScheduleHelper.getSmokingDayForTimestamp(now, overridesMap)
        val activeShift = SmokingScheduleHelper.getActiveShiftType(shiftDay, overridesMap[shiftDay.toString()])
        val currentOverride = overridesMap[shiftDay.toString()]
        val shiftTimes = SmokingScheduleHelper.getShiftTimes(shiftDay, activeShift, currentOverride)
        
        val isDuringShift = if (shiftTimes != null) {
            now.isAfter(shiftTimes.first) && now.isBefore(shiftTimes.second)
        } else {
            false
        }

        // Build a simple day stats list for UI 
        val statsList = mutableListOf<DayStats>()
        val firstLogDateStr = logs.minByOrNull { it.timestamp }?.dateString
        val firstLogDate = firstLogDateStr?.let { 
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        } ?: shiftDay
        
        var d = maxOf(SmokingScheduleHelper.START_DATE, firstLogDate)
        val maxDay = maxOf(shiftDay, LocalDate.now())
        
        while (!d.isAfter(maxDay)) {
            val dStr = d.toString()
            val dActiveShift = SmokingScheduleHelper.getActiveShiftType(d, overridesMap[dStr])
            
            val dShiftTimes = SmokingScheduleHelper.getShiftTimes(d, dActiveShift, overridesMap[dStr])
            
            val smoked = if (dShiftTimes != null) {
                logs.filter {
                    val logTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), java.time.ZoneId.systemDefault())
                    !logTime.isBefore(dShiftTimes.first) && !logTime.isAfter(dShiftTimes.second)
                }.sumOf { it.amount }
            } else {
                0
            }

            statsList.add(
                DayStats(
                    date = d,
                    smoked = smoked,
                    activeShift = dActiveShift
                )
            )
            d = d.plusDays(1)
        }

        val logsTodayShift = if (shiftTimes != null) {
            logs.filter {
                val logTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), java.time.ZoneId.systemDefault())
                !logTime.isBefore(shiftTimes.first) && !logTime.isAfter(shiftTimes.second)
            }
        } else {
            emptyList()
        }
        
        val smokedToday = logsTodayShift.sumOf { it.amount }

        var nextAllowedTime: LocalDateTime? = null
        var isWaitingForBonus = false
        val totalTimerSeconds = 4200L

        if (shiftTimes != null) {
            val shiftStart = shiftTimes.first
            val shiftEnd = shiftTimes.second
            
            val lastLog = logsTodayShift.maxByOrNull { it.timestamp }
            val lastLogTime = lastLog?.let {
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), java.time.ZoneId.systemDefault())
            }

            if (isDuringShift) {
                val baseTime = lastLogTime ?: shiftStart
                nextAllowedTime = baseTime.plusMinutes(70)
                if (nextAllowedTime!!.isAfter(shiftEnd)) {
                    nextAllowedTime = shiftEnd
                }
            } else if (now.isAfter(shiftTimes.second)) {
                // Shift has ended. Check if we have taken the bonus cigarette.
                // We check if the last log is on or after the shift end.
                if (lastLogTime != null && !lastLogTime.isBefore(shiftEnd)) {
                    isWaitingForBonus = false
                } else {
                    isWaitingForBonus = true
                    nextAllowedTime = shiftEnd // means 0 seconds remaining
                }
            }
        }
        
        val secondsRemaining = if (nextAllowedTime != null) {
            maxOf(0, ChronoUnit.SECONDS.between(now, nextAllowedTime))
        } else {
            0
        }
        
        val globalLastLog = logs.maxByOrNull { it.timestamp }
        val lastCigaretteTime = if (globalLastLog != null) {
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(globalLastLog.timestamp), java.time.ZoneId.systemDefault())
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
                currentSmokingDay = shiftDay,
                activeShift = activeShift,
                smokedToday = smokedToday,
                nextAllowedTime = nextAllowedTime,
                timerSecondsRemaining = secondsRemaining,
                totalTimerSeconds = totalTimerSeconds,
                isTimerActive = nextAllowedTime != null && secondsRemaining > 0,
                isDuringShift = isDuringShift,
                isWaitingForBonus = isWaitingForBonus,
                shiftStart = shiftTimes?.first,
                shiftEnd = shiftTimes?.second,
                currentOverride = currentOverride,
                logsToday = logsTodayShift,
                allLogs = logs,
                dayStatsList = statsList,
                lastCigaretteTime = lastCigaretteTime,
                secondsSinceLastCigarette = secondsSinceLastCigarette
            )
        }

        scheduleSystemAlarms(shiftDay, activeShift, shiftTimes, nextAllowedTime)
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
