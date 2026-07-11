#!/bin/bash

# Remove the 'if (state.smokedToday > 0)' block and just use 'enabled = state.smokedToday > 0'
sed -i '/if (state.smokedToday > 0) {/d' app/src/main/java/com/example/ui/screens/DashboardScreen.kt
sed -i '/^                }$/d' app/src/main/java/com/example/ui/screens/DashboardScreen.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/screens/DashboardScreen.kt

@Composable
fun InitializationDialog(
    onComplete: (bonusCigarettes: Int, smokedToday: Int) -> Unit
) {
    var bonusText by remember { mutableStateOf("") }
    var smokedText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        title = { Text("Kezdeti beállítás") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Kérlek add meg a jelenlegi állapotot:")
                OutlinedTextField(
                    value = bonusText,
                    onValueChange = { bonusText = it.filter { char -> char.isDigit() } },
                    label = { Text("Bónusz cigik (pl. 2)") }
                )
                OutlinedTextField(
                    value = smokedText,
                    onValueChange = { smokedText = it.filter { char -> char.isDigit() } },
                    label = { Text("Ma elszívott mennyiség (pl. 12)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val bonus = bonusText.toIntOrNull() ?: 0
                val smoked = smokedText.toIntOrNull() ?: 0
                onComplete(bonus, smoked)
            }) {
                Text("Mentés")
            }
        }
    )
}
INNER_EOF
