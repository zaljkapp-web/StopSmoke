import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

# I will find the block starting with "LaunchedEffect(state.smokedToday)" and going down to "BoxWithConstraints("
start_idx = content.find("LaunchedEffect(state.smokedToday)")
end_idx = content.find("BoxWithConstraints(", start_idx)

original_block = content[start_idx:end_idx]

new_block = """LaunchedEffect(state.smokedToday) {
        currentQuote = CYNICAL_QUOTES[Random.nextInt(CYNICAL_QUOTES.size)]
    }

    val totalSeconds = remember(state.lastCigaretteTime, state.nextAllowedTime, state.isDuringShift, state.dailyLimit) {
        val last = state.lastCigaretteTime
        val next = state.nextAllowedTime
        if (last != null && next != null && next.isAfter(last)) {
            java.time.temporal.ChronoUnit.SECONDS.between(last, next)
        } else {
            if (state.isDuringShift) {
                val totalShiftSecs = 8 * 3600L // 8 hours shift as standard
                val limit = state.dailyLimit.coerceAtLeast(1)
                totalShiftSecs / limit
            } else {
                val totalDaySecs = 16 * 3600L // 16 waking hours as standard
                val limit = state.dailyLimit.coerceAtLeast(1)
                totalDaySecs / limit
            }
        }
    }

    val progress = remember(state.timerSecondsRemaining, totalSeconds) {
        if (totalSeconds > 0) {
            (state.timerSecondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // Animated progress to look extremely modern and fluid
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "TimerProgress"
    )

    """

content = content[:start_idx] + new_block + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
