package com.example.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class ShiftType(
    val displayName: String,
    val shortName: String,
    val defaultStartHour: Int,
    val defaultEndHour: Int,
    val closingHour: Int,
    val closingMinute: Int
) {
    MORNING("Délelőtt", "D", 6, 14, 23, 30),
    AFTERNOON("Délután", "Du", 14, 22, 1, 0), // closing next day 01:00
    NIGHT("Éjszaka", "É", 22, 6, 9, 0),       // closing next day 09:00
    REST("Pihenő", "P", 0, 0, 0, 30)          // closing next day 00:30
}

object SmokingScheduleHelper {
    val START_DATE: LocalDate = LocalDate.of(2026, 6, 22)

    /**
     * Get default shift type for a date (without vacation override)
     */
    fun getDefaultShiftType(date: LocalDate): ShiftType {
        val daysBetween = ChronoUnit.DAYS.between(START_DATE, date)
        val cycleIndex = ((daysBetween % 28 + 28) % 28).toInt()
        return when (cycleIndex) {
            in 0..5 -> ShiftType.MORNING
            6 -> ShiftType.REST
            in 7..9 -> ShiftType.AFTERNOON
            10 -> ShiftType.REST
            in 11..14 -> ShiftType.NIGHT
            in 15..16 -> ShiftType.REST
            in 17..20 -> ShiftType.AFTERNOON
            21 -> ShiftType.REST
            in 22..24 -> ShiftType.NIGHT
            else -> ShiftType.REST
        }
    }

    /**
     * Get active shift type taking into account a vacation override
     */
    fun getActiveShiftType(date: LocalDate, override: DayOverride?): ShiftType {
        if (override != null && override.isVacation) {
            return ShiftType.REST
        }
        return getDefaultShiftType(date)
    }

    /**
     * Get standard daily limit for a date (before roll-over additions)
     */
    fun getStandardLimit(date: LocalDate): Int {
        if (date.isBefore(START_DATE)) return 25
        val weeks = ChronoUnit.DAYS.between(START_DATE, date) / 7
        return (25 - weeks.toInt()).coerceAtLeast(1)
    }

    /**
     * Calculates the closing time for a given date
     */
    fun getClosingTime(date: LocalDate, activeShift: ShiftType): LocalDateTime {
        val closingHour = activeShift.closingHour
        val closingMinute = activeShift.closingMinute
        
        return when (activeShift) {
            ShiftType.MORNING -> {
                // Same day 23:30
                LocalDateTime.of(date, LocalTime.of(closingHour, closingMinute))
            }
            ShiftType.AFTERNOON, ShiftType.NIGHT, ShiftType.REST -> {
                // Next day
                LocalDateTime.of(date.plusDays(1), LocalTime.of(closingHour, closingMinute))
            }
        }
    }

    /**
     * Gets start and end times for the shift on a given date (if any)
     */
    fun getShiftTimes(date: LocalDate, activeShift: ShiftType, override: DayOverride?): Pair<LocalDateTime, LocalDateTime>? {
        if (activeShift == ShiftType.REST) return null

        val startHoursOvertime = override?.overtimeStartHours ?: 0
        val endHoursOvertime = override?.overtimeEndHours ?: 0

        val startHour = (activeShift.defaultStartHour - startHoursOvertime + 24) % 24
        // If starting early pushes it to previous calendar day, adjust date
        val startDate = if (activeShift.defaultStartHour - startHoursOvertime < 0) date.minusDays(1) else date
        val shiftStart = LocalDateTime.of(startDate, LocalTime.of(startHour, 0))

        val baseEndHour = activeShift.defaultEndHour
        val baseEndDate = if (activeShift == ShiftType.NIGHT) date.plusDays(1) else date
        val baseEnd = LocalDateTime.of(baseEndDate, LocalTime.of(baseEndHour, 0))
        val shiftEnd = baseEnd.plusHours(endHoursOvertime.toLong())

        return Pair(shiftStart, shiftEnd)
    }

    /**
     * Maps a specific timestamp (LocalDateTime) to its corresponding "smoking day" (LocalDate)
     */
    fun getSmokingDayForTimestamp(timestamp: LocalDateTime, overrides: Map<String, DayOverride>): LocalDate {
        val candidate = timestamp.toLocalDate()
        
        // 1. Check candidate day itself
        val activeShiftCandidate = getActiveShiftType(candidate, overrides[candidate.toString()])
        val closingCandidate = getClosingTime(candidate, activeShiftCandidate)
        
        // 2. Check candidate - 1
        val prevDay = candidate.minusDays(1)
        val activeShiftPrev = getActiveShiftType(prevDay, overrides[prevDay.toString()])
        val closingPrev = getClosingTime(prevDay, activeShiftPrev)

        return when {
            timestamp.isAfter(closingCandidate) -> {
                candidate.plusDays(1)
            }
            timestamp.isBefore(closingPrev) || timestamp.isEqual(closingPrev) -> {
                // Check if it belongs to candidate - 2 (extremely rare edge case, but safe to check)
                val prevPrevDay = prevDay.minusDays(1)
                val activeShiftPrevPrev = getActiveShiftType(prevPrevDay, overrides[prevPrevDay.toString()])
                val closingPrevPrev = getClosingTime(prevPrevDay, activeShiftPrevPrev)
                if (timestamp.isBefore(closingPrevPrev) || timestamp.isEqual(closingPrevPrev)) {
                    prevPrevDay
                } else {
                    prevDay
                }
            }
            else -> {
                candidate
            }
        }
    }
}
