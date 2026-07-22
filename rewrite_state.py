import re

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

new_classes = """data class DayStats(
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
)"""

start_stats = content.find("data class DayStats(")
end_uistate = content.find("class SmokingViewModel(")

content = content[:start_stats] + new_classes + "\n\n" + content[end_uistate:]

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
