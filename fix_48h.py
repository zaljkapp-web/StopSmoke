import re

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

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

"""

content = content[:start_calc_idx] + new_calculate + content[end_calc_idx:]

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
