package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SmokingRepository(private val dbHelper: SmokingDatabaseHelper) {
    val allLogsFlow: Flow<List<CigaretteLog>> = dbHelper.allLogsFlow
    val allOverridesFlow: Flow<List<DayOverride>> = dbHelper.allOverridesFlow

    suspend fun getAllLogs(): List<CigaretteLog> = dbHelper.getAllLogs()

    fun getLogsForDayFlow(dateString: String): Flow<List<CigaretteLog>> =
        dbHelper.allLogsFlow.map { logs -> 
            logs.filter { it.dateString == dateString }.sortedBy { it.timestamp } 
        }

    suspend fun getLogsForDay(dateString: String): List<CigaretteLog> =
        dbHelper.getLogsForDay(dateString)

    suspend fun insertLog(log: CigaretteLog) =
        dbHelper.insertLog(log)

    suspend fun deleteLog(log: CigaretteLog) =
        dbHelper.deleteLog(log)

    suspend fun deleteLogById(id: Int) =
        dbHelper.deleteLogById(id)

    suspend fun getOverrideForDay(dateString: String): DayOverride? =
        dbHelper.getOverrideForDay(dateString)

    fun getOverrideForDayFlow(dateString: String): Flow<DayOverride?> =
        dbHelper.allOverridesFlow.map { overrides -> 
            overrides.firstOrNull { it.dateString == dateString } 
        }

    suspend fun insertOrUpdateOverride(override: DayOverride) =
        dbHelper.insertOrUpdateOverride(override)

    suspend fun deleteOverride(override: DayOverride) =
        dbHelper.deleteOverride(override)
}

