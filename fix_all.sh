#!/bin/bash
sed -i '316i \                }' app/src/main/java/com/example/ui/screens/DashboardScreen.kt

# The second box ends around 349. Let's find it.
sed -i '350i \                }\
            }' app/src/main/java/com/example/ui/screens/DashboardScreen.kt

