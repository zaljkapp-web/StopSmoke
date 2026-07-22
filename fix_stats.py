import re

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'r') as f:
    content = f.read()

# Fix successfulDays
content = re.sub(r'val successfulDays = state\.dayStatsList\.count \{ .*? \}', 'val successfulDays = state.dayStatsList.count { it.smoked == 0 }', content)
content = re.sub(r'val streakCount = countStreak\(state.dayStatsList\)', 'val streakCount = 0', content)

# Fix maxVal
content = re.sub(r'val maxVal = last7DaysStats\.maxOfOrNull \{ .*? \}\?\.coerceAtLeast\(1\) \?\: 25', 'val maxVal = last7DaysStats.maxOfOrNull { it.smoked }?.coerceAtLeast(1) ?: 20', content)

# Fix limitHeightPct
content = re.sub(r'val limitHeightPct = \(dayStat\.totalLimit\.toFloat\(\) / maxVal\.toFloat\(\)\)\.coerceIn\(0f, 1f\)', 'val limitHeightPct = (dayStat.smoked.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)', content)

# Fix colors
content = content.replace('if (dayStat.smoked <= dayStat.totalLimit) IceBlue else AccentRed', 'IceBlue')

# Fix isSuccess
content = re.sub(r'val isSuccess = dayStat\.smoked <= dayStat\.totalLimit', 'val isSuccess = true', content)

# Fix rollover text
content = re.sub(r'                    if \(dayStat\.rollover > 0\) \{[\s\S]*?                    \}', '', content)

# Fix text showing limit
content = re.sub(r'text = "\$\{dayStat\.smoked\} / \$\{dayStat\.totalLimit\} szál",', 'text = "${dayStat.smoked} szál",', content)

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'w') as f:
    f.write(content)
