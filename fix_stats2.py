import re

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'r') as f:
    content = f.read()

# Replace calculateCurrentStreak entirely
old_func = """fun calculateCurrentStreak(statsList: List<DayStats>): Int {
    var streak = 0
    val sorted = statsList.sortedByDescending { it.date }
    // Skip today if today is not over and limit is not exceeded yet
    for (stat in sorted) {
        if (stat.date == LocalDate.now() && stat.smoked <= stat.totalLimit) {
            continue
        }
        if (stat.smoked <= stat.totalLimit) {
            streak++
        } else {
            break
        }
    }
    return streak
}"""

new_func = """fun calculateCurrentStreak(statsList: List<DayStats>): Int {
    return 0
}"""

content = content.replace(old_func, new_func)

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'w') as f:
    f.write(content)
