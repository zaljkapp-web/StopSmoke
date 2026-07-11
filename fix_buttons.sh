#!/bin/bash
# Remove everything from line 348 to 431
sed -i '348,431d' app/src/main/java/com/example/ui/screens/DashboardScreen.kt

# Insert the correct content
sed -i '347r /dev/stdin' app/src/main/java/com/example/ui/screens/DashboardScreen.kt << 'INNER_EOF'
            // 5. Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Smoking Log Button
                Button(
                    onClick = { viewModel.logCigarette("NORMAL") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceBlue,
                        contentColor = DeepBlue
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(60.dp)
                        .testTag("log_cigarette_button"),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Cigi loggolása",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ELSZÍVVA",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // Coffee Multiplier Button (+2 Cigarettes)
                Button(
                    onClick = { viewModel.logCigarette("COFFEE") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateCard,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .testTag("coffee_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "☕ KÁVÉ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "+2 szál",
                            fontSize = 9.sp,
                            color = TextPrimary.copy(alpha = 0.5f)
                        )
                    }
                }

                // Undo Button
                IconButton(
                    onClick = { viewModel.deleteLastLog() },
                    enabled = state.smokedToday > 0,
                    modifier = Modifier
                        .size(60.dp)
                        .background(if (state.smokedToday > 0) DarkSurface else DarkSurface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Utolsó visszavonása",
                        tint = if (state.smokedToday > 0) AccentRed else AccentRed.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
INNER_EOF
