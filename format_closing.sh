#!/bin/bash
# Remove the old Text block
sed -i '139,143d' app/src/main/java/com/example/ui/screens/DashboardScreen.kt

# Insert the new logic
sed -i '138r /dev/stdin' app/src/main/java/com/example/ui/screens/DashboardScreen.kt << 'INNER_EOF'
                        val closingText = if (state.activeShift == ShiftType.MORNING) {
                            String.format("%02d:%02d", state.activeShift.closingHour, state.activeShift.closingMinute)
                        } else {
                            String.format("másnap %02d:%02d", state.activeShift.closingHour, state.activeShift.closingMinute)
                        }
                        Text(
                            text = "${state.activeShift.displayName} műszak (Vége: $closingText)",
                            color = TextPrimary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
INNER_EOF
