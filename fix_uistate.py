import re

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

# Add totalTimerSeconds to UiState
content = content.replace("val timerSecondsRemaining: Long = 0,", "val timerSecondsRemaining: Long = 0,\n    val totalTimerSeconds: Long = 0,")

# Update ViewModel logic to return Pair<LocalDateTime?, Long>
# Find calculateNextAllowedTime signature and change return type
content = content.replace("): LocalDateTime? {", "): Pair<LocalDateTime?, Long> {")

# Replace return null with return Pair(null, 0L)
content = content.replace("if (remainingCount <= 0) return null", "if (remainingCount <= 0) return Pair(null, 0L)")
content = content.replace("if (now.isAfter(closingTime)) return null", "if (now.isAfter(closingTime)) return Pair(null, 0L)")
# The final return null
content = re.sub(r'return null\s+}', 'return Pair(null, 0L)\n    }', content)

# Replace the candidate return
content = content.replace("return if (candidate.isAfter(closingTime)) closingTime else candidate", "return Pair(if (candidate.isAfter(closingTime)) closingTime else candidate, intervalSeconds)")

# Fix the caller of calculateNextAllowedTime
caller_block = """        val nextAllowedTime = calculateNextAllowedTime(
            now = now,
            smokingDay = smokingDay,
            activeShift = activeShift,
            shiftTimes = shiftTimes,
            isDuringShift = isDuringShift,
            remainingCount = remainingToday,
            logsToday = logsToday,
            currentOverride = currentOverride
        )
        val secondsRemaining = if (nextAllowedTime != null) {
            max(0, ChronoUnit.SECONDS.between(now, nextAllowedTime))
        } else {
            0
        }"""

new_caller_block = """        val (nextAllowedTime, totalTimerSeconds) = calculateNextAllowedTime(
            now = now,
            smokingDay = smokingDay,
            activeShift = activeShift,
            shiftTimes = shiftTimes,
            isDuringShift = isDuringShift,
            remainingCount = remainingToday,
            logsToday = logsToday,
            currentOverride = currentOverride
        )
        val secondsRemaining = if (nextAllowedTime != null) {
            max(0, ChronoUnit.SECONDS.between(now, nextAllowedTime))
        } else {
            0
        }"""

content = content.replace(caller_block, new_caller_block)

# Fix the _uiState.value assignment
assign_block = """            nextAllowedTime = nextAllowedTime,
            timerSecondsRemaining = secondsRemaining,"""
new_assign_block = """            nextAllowedTime = nextAllowedTime,
            timerSecondsRemaining = secondsRemaining,
            totalTimerSeconds = totalTimerSeconds,"""
content = content.replace(assign_block, new_assign_block)


with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
