#!/bin/bash

# Fix the totalSeconds calculation
sed -i '56,67d' app/src/main/java/com/example/ui/screens/DashboardScreen.kt
sed -i '55a\
    val totalSeconds = remember(state.lastCigaretteTime, state.nextAllowedTime, state.isDuringShift, state.dailyLimit) {\
        if (state.lastCigaretteTime != null && state.nextAllowedTime != null && state.nextAllowedTime.isAfter(state.lastCigaretteTime)) {\
            java.time.temporal.ChronoUnit.SECONDS.between(state.lastCigaretteTime, state.nextAllowedTime)\
        } else {\
            if (state.isDuringShift) {\
                val totalShiftSecs = 8 * 3600L\
                val limit = state.dailyLimit.coerceAtLeast(1)\
                totalShiftSecs / limit\
            } else {\
                val totalDaySecs = 16 * 3600L\
                val limit = state.dailyLimit.coerceAtLeast(1)\
                totalDaySecs / limit\
            }\
        }\
    }' app/src/main/java/com/example/ui/screens/DashboardScreen.kt

# Fix the sweepAngle formula
sed -i 's/sweepAngle = 260f \* (1f - animatedProgress)/sweepAngle = 260f \* animatedProgress/g' app/src/main/java/com/example/ui/screens/DashboardScreen.kt
