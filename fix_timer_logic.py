import re

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

# Replace updateState block
start_update_idx = content.find("    private fun updateState(")
end_update_idx = content.find("    private fun calculateNextAllowedTime(")

new_update_state = """    private fun updateState(logs: List<CigaretteLog>, overridesList: List<DayOverride>) {
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
            val remaining = maxOf(0, totalLimit - smoked)
            
            statsList.add(
                DayStats(
                    date = d,
                    baseLimit = baseLimit,
                    rollover = rollover,
                    totalLimit = totalLimit,
                    smoked = smoked,
                    remaining = remaining,
                    activeShift = dActiveShift
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
        val remainingToday = maxOf(0, currentTotalLimit - smokedToday)

        // Calculate timer / next allowed time
        val (nextAllowedTime, totalTimerSeconds) = calculateNextAllowedTime(
            now = now,
            smokingDay = smokingDay,
            shiftTimes = shiftTimes,
            remainingCount = remainingToday,
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

"""

content = content[:start_update_idx] + new_update_state + content[end_update_idx:]

end_calculate_idx = content.find("    private fun scheduleSystemAlarms(")

new_calculate = """    private fun calculateNextAllowedTime(
        now: LocalDateTime,
        smokingDay: LocalDate,
        shiftTimes: Pair<LocalDateTime, LocalDateTime>?,
        remainingCount: Int,
        logsToday: List<CigaretteLog>
    ): Pair<LocalDateTime?, Long> {
        if (remainingCount <= 0) return Pair(null, 0L)
        
        val closingTime = LocalDateTime.of(smokingDay.plusDays(1), LocalTime.MIDNIGHT)
        val dayStart = LocalDateTime.of(smokingDay, LocalTime.MIDNIGHT)
        if (now.isAfter(closingTime)) return Pair(null, 0L)

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

        val secondsLeftAtBaseTime = ChronoUnit.SECONDS.between(baseTime, closingTime)
        if (secondsLeftAtBaseTime > 0) {
            val intervalSeconds = secondsLeftAtBaseTime / remainingCount
            val candidate = baseTime.plusSeconds(intervalSeconds)
            return Pair(if (candidate.isAfter(closingTime)) closingTime else candidate, intervalSeconds)
        }

        return Pair(null, 0L)
    }

"""
start_calc_idx = content.find("    private fun calculateNextAllowedTime(")
content = content[:start_calc_idx] + new_calculate + content[end_calculate_idx:]

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
