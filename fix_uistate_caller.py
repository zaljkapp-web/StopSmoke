with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val nextAllowedTime = calculateNextAllowedTime(", "val (nextAllowedTime, totalTimerSeconds) = calculateNextAllowedTime(")

with open('app/src/main/java/com/example/viewmodel/SmokingViewModel.kt', 'w') as f:
    f.write(content)
