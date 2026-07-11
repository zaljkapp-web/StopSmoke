with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

start_str = "    val totalSeconds = remember(state.lastCigaretteTime, state.nextAllowedTime, state.isDuringShift, state.dailyLimit) {"
end_str = "    val progress = remember(state.timerSecondsRemaining, totalSeconds) {"

start_idx = content.find(start_str)
end_idx = content.find(end_str)

new_block = "    val totalSeconds = state.totalTimerSeconds\n\n"

content = content[:start_idx] + new_block + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
