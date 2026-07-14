import re

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

# 1. Update rollover calculation to allow negative values in updateState
old_rollover_logic = """            val totalLimit = baseLimit + rollover
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
            }"""

new_rollover_logic = """            val totalLimit = baseLimit + rollover
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
            }"""

content = content.replace(old_rollover_logic, new_rollover_logic)

# 2. Update calculateNextAllowedTime call
old_calc_call = """        // Calculate timer / next allowed time
        val (nextAllowedTime, totalTimerSeconds) = calculateNextAllowedTime(
            now = now,
            smokingDay = smokingDay,
            shiftTimes = shiftTimes,
            remainingCount = remainingToday,
            logsToday = logsToday
        )"""

new_calc_call = """        // Calculate timer / next allowed time
        val (nextAllowedTime, totalTimerSeconds) = calculateNextAllowedTime(
            now = now,
            smokingDay = smokingDay,
            activeShift = activeShift,
            shiftTimes = shiftTimes,
            currentTotalLimit = currentTotalLimit,
            smokedToday = smokedToday,
            logsToday = logsToday
        )"""

content = content.replace(old_calc_call, new_calc_call)


# 3. Update calculateNextAllowedTime signature and body
start_calc_idx = content.find("    private fun calculateNextAllowedTime(")
end_calc_idx = content.find("    private fun scheduleSystemAlarms(")

new_calculate = """    private fun calculateNextAllowedTime(
        now: LocalDateTime,
        smokingDay: LocalDate,
        activeShift: ShiftType,
        shiftTimes: Pair<LocalDateTime, LocalDateTime>?,
        currentTotalLimit: Int,
        smokedToday: Int,
        logsToday: List<CigaretteLog>
    ): Pair<LocalDateTime?, Long> {
        val remainingToday = maxOf(0, currentTotalLimit - smokedToday)
        
        val isNightShift = activeShift == ShiftType.NIGHT
        
        val windowEnd = if (isNightShift) {
            LocalDateTime.of(smokingDay.plusDays(2), java.time.LocalTime.MIDNIGHT)
        } else {
            LocalDateTime.of(smokingDay.plusDays(1), java.time.LocalTime.MIDNIGHT)
        }
        
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
        
        val remainingCount = if (isNightShift) {
            val tomorrowBaseLimit = SmokingScheduleHelper.getStandardLimit(smokingDay.plusDays(1))
            val actualRemainingToday = currentTotalLimit - smokedToday
            val remaining48h = actualRemainingToday + tomorrowBaseLimit
            maxOf(0, remaining48h)
        } else {
            remainingToday
        }

        if (remainingCount <= 0) return Pair(null, 0L)

        val secondsLeftAtBaseTime = ChronoUnit.SECONDS.between(baseTime, windowEnd)
        if (secondsLeftAtBaseTime > 0) {
            val intervalSeconds = secondsLeftAtBaseTime / remainingCount
            val candidate = baseTime.plusSeconds(intervalSeconds)
            return Pair(if (candidate.isAfter(windowEnd)) windowEnd else candidate, intervalSeconds)
        }

        return Pair(null, 0L)
    }

"""

content = content[:start_calc_idx] + new_calculate + content[end_calc_idx:]

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
