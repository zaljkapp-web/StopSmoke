package com.example.data

data class CigaretteLog(
    val id: Int = 0,
    val timestamp: Long,
    val amount: Int,
    val type: String, // "NORMAL", "COFFEE", "SHIFT_END"
    val dateString: String // YYYY-MM-DD
)

data class DayOverride(
    val dateString: String, // YYYY-MM-DD
    val isVacation: Boolean = false,
    val overtimeStartHours: Int = 0, // e.g. 4 if starting 4 hours early
    val overtimeEndHours: Int = 0    // e.g. 4 if ending 4 hours later
)

