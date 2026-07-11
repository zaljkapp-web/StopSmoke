import re

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

start_str = "    private fun calculateNextAllowedTime("
end_str = "    private fun scheduleSystemAlarms("

start_idx = content.find(start_str)
end_idx = content.find(end_str)

new_method = """    private fun calculateNextAllowedTime(
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

        val lastLog = logsToday.maxByOrNull { it.timestamp }
        val lastLogTime = lastLog?.let {
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), java.time.ZoneId.systemDefault())
        }

        val dayStart = closingTime.minusHours(16)
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
            return if (candidate.isAfter(closingTime)) closingTime else candidate
        }

        return null
    }

"""

content = content[:start_idx] + new_method + content[end_idx:]

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
