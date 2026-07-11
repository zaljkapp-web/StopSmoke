with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("timerSecondsRemaining = secondsRemaining,", "timerSecondsRemaining = secondsRemaining,\n                totalTimerSeconds = totalTimerSeconds,")

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
