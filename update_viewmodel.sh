#!/bin/bash
# Remove the setupInitialStateIfNeeded call
sed -i '/com.example.data.AppSetupHelper.setupInitialStateIfNeeded(repository)/d' app/src/main/java/com/example/viewmodel/SmokingViewModel.kt

# Add the completeInitialization function before the last closing brace
sed -i '/^}$/d' app/src/main/java/com/example/viewmodel/SmokingViewModel.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/viewmodel/SmokingViewModel.kt

    fun completeInitialization(bonusCigarettes: Int, smokedToday: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            
            val yesterdayLimit = SmokingScheduleHelper.getStandardLimit(yesterday)
            val smokedYesterdayAmount = yesterdayLimit - bonusCigarettes
            
            // Log for yesterday to set up the exact bonus
            repository.insertLog(
                CigaretteLog(
                    timestamp = LocalDateTime.now().minusDays(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
                    amount = smokedYesterdayAmount,
                    type = "INITIALIZATION",
                    dateString = yesterday.toString()
                )
            )
            
            // Log for today's smoked cigarettes
            if (smokedToday > 0) {
                repository.insertLog(
                    CigaretteLog(
                        timestamp = System.currentTimeMillis() - 10000,
                        amount = smokedToday,
                        type = "INITIALIZATION",
                        dateString = today.toString()
                    )
                )
            } else {
                repository.insertLog(
                    CigaretteLog(
                        timestamp = System.currentTimeMillis() - 10000,
                        amount = 0,
                        type = "INITIALIZATION",
                        dateString = today.toString()
                    )
                )
            }
        }
    }
}
INNER_EOF
