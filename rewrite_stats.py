import re

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'r') as f:
    content = f.read()

# Replace everything from line 41 to 57
old_stats_logic = """    // 1. Calculate general statistics
    val totalDays = max(1L, ChronoUnit.DAYS.between(com.example.data.SmokingScheduleHelper.START_DATE, LocalDate.now()) + 1)
    val totalSmoked = state.allLogs.sumOf { it.amount }
    
    // Financial calculation: assuming average price of 1 cigarette is 120 Ft (2400 Ft / 20 pack)
    val pricePerCigarette = 120
    val moneySpent = totalSmoked * pricePerCigarette
    
    // Saved money: how much they saved by reducing from 25 cigarettes/day starting from June 22, 2026
    val initialBaselineCount = totalDays * 25
    val savedCount = max(0L, initialBaselineCount - totalSmoked)
    val moneySaved = savedCount * pricePerCigarette

    // Streak calculation: days where smoked <= limit
    val successfulDays = state.dayStatsList.count { it.smoked == 0 }
    val currentStreak = calculateCurrentStreak(state.dayStatsList)"""

new_stats_logic = """    // 1. Calculate general statistics
    val firstLogDateStr = state.allLogs.minByOrNull { it.timestamp }?.dateString
    val firstLogDate = firstLogDateStr?.let { 
        try { LocalDate.parse(it) } catch (e: Exception) { null }
    } ?: LocalDate.now()
    
    val totalDays = max(1L, ChronoUnit.DAYS.between(firstLogDate, LocalDate.now()) + 1)
    
    // Streak calculation: days where smoked == 0 (no smoking day) or whatever logic we need
    val successfulDays = state.dayStatsList.count { it.smoked == 0 }"""

content = content.replace(old_stats_logic, new_stats_logic)

# Replace the remaining StatCards at line 89
old_statcards = """                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "ÖSSZESEN ELTÉLT",
                        value = "$totalDays nap",
                        icon = Icons.Default.Info,
                        iconTint = IceBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "BETARTOTT NAPOK",
                        value = "$successfulDays nap",
                        icon = Icons.Default.TrendingDown,
                        iconTint = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }"""

new_statcards = """                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "ÖSSZESEN ELTÉLT",
                        value = "$totalDays nap",
                        icon = Icons.Default.Info,
                        iconTint = IceBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "MŰSZAKNÉLKÜLI NAPOK",
                        value = "$successfulDays nap",
                        icon = Icons.Default.TrendingDown,
                        iconTint = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }"""

content = content.replace(old_statcards, new_statcards)

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'w') as f:
    f.write(content)
