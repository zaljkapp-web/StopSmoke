import re

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

start_idx = content.find("    private fun updateState(")
end_idx = content.find("    private fun scheduleSystemAlarms(")

new_update_state = """    private fun updateState(logs: List<CigaretteLog>, overridesList: List<DayOverride>) {
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

"""

content = content[:start_idx] + new_update_state + content[end_idx:]

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
