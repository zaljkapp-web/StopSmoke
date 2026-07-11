package com.example.data

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime

object AppSetupHelper {
    suspend fun setupInitialStateIfNeeded(repository: SmokingRepository) {
        val allLogs = repository.getAllLogs()
        if (allLogs.isEmpty()) {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            
            // Limit for yesterday
            val yesterdayLimit = SmokingScheduleHelper.getStandardLimit(yesterday)
            // To get +2 rollover, we need to have remaining = 2 yesterday.
            // So smoked yesterday = limit - 2
            val smokedYesterday = yesterdayLimit - 2
            
            if (smokedYesterday > 0) {
                repository.insertLog(
                    CigaretteLog(
                        timestamp = LocalDateTime.now().minusDays(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
                        amount = smokedYesterday,
                        type = "INITIALIZATION",
                        dateString = yesterday.toString()
                    )
                )
            }
            
            // Today the user smoked 12 cigarettes. We can insert them as one log.
            repository.insertLog(
                CigaretteLog(
                    timestamp = System.currentTimeMillis() - 10000,
                    amount = 12,
                    type = "INITIALIZATION",
                    dateString = today.toString()
                )
            )
            
            // To prevent calculating from June 22nd when there's an initialization, 
            // we should also ensure SmokingViewModel knows not to loop before yesterday.
        }
    }
}
