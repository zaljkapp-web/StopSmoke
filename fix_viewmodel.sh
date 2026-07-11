#!/bin/bash
sed -i '/private fun calculateNextAllowedTime(/,/^    }/c\
    private fun calculateNextAllowedTime(\
        now: LocalDateTime,\
        smokingDay: LocalDate,\
        activeShift: ShiftType,\
        shiftTimes: Pair<LocalDateTime, LocalDateTime>?,\
        isDuringShift: Boolean,\
        remainingCount: Int,\
        logsToday: List<CigaretteLog>,\
        currentOverride: DayOverride?\
    ): LocalDateTime? {\
        if (remainingCount <= 0) return null\
        val closingTime = SmokingScheduleHelper.getClosingTime(smokingDay, activeShift)\
        if (now.isAfter(closingTime)) return null\
\
        val lastLog = logsToday.maxByOrNull { it.timestamp }\
        val lastLogTime = lastLog?.let {\
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), java.time.ZoneId.systemDefault())\
        }\
\
        val dayStart = closingTime.minusHours(16)\
        var baseTime = lastLogTime ?: dayStart\
\
        if (shiftTimes != null) {\
            val shiftStart = shiftTimes.first\
            val shiftEnd = shiftTimes.second\
\
            if (now.isAfter(shiftStart) && baseTime.isBefore(shiftStart)) {\
                baseTime = shiftStart\
            }\
            if (now.isAfter(shiftEnd) && baseTime.isBefore(shiftEnd)) {\
                baseTime = shiftEnd\
            }\
        }\
\
        val secondsLeftAtBaseTime = ChronoUnit.SECONDS.between(baseTime, closingTime)\
        if (secondsLeftAtBaseTime > 0) {\
            val intervalSeconds = secondsLeftAtBaseTime / remainingCount\
            val candidate = baseTime.plusSeconds(intervalSeconds)\
            return if (candidate.isAfter(closingTime)) closingTime else candidate\
        }\
\
        return null\
    }' app/src/main/java/com/example/viewmodel/SmokingViewModel.kt
chmod +x fix_viewmodel.sh
./fix_viewmodel.sh
