#!/bin/bash
sed -i '55,69d' app/src/main/java/com/example/ui/screens/DashboardScreen.kt
sed -i '54a\
    val totalSeconds = remember(state.lastCigaretteTime, state.nextAllowedTime, state.isDuringShift, state.dailyLimit) {\
        val last = state.lastCigaretteTime\
        val next = state.nextAllowedTime\
        if (last != null && next != null && next.isAfter(last)) {\
            java.time.temporal.ChronoUnit.SECONDS.between(last, next)\
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
