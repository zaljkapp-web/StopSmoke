#!/bin/bash
sed -i '45a\
    if (state.needsInitialization) {\
        InitializationDialog(onComplete = { bonus, smoked -> viewModel.completeInitialization(bonus, smoked) })\
    }' app/src/main/java/com/example/ui/screens/DashboardScreen.kt
