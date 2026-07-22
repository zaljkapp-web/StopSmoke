import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Remove initialization
content = re.sub(r'    if \(state.needsInitialization\) \{[\s\S]*?    \}\n', '', content)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
