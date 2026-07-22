import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Fix inner content of timer
old_inner = """                    Text(
                        text = "HÁTRALÉVŐ: ${state.remainingToday} SZÁL",
                        color = WarmGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )"""

new_inner = """                    if (state.isWaitingForBonus) {
                        Text(
                            text = "BÓNUSZ CIGI",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (!state.isDuringShift) {
                        Text(
                            text = "NINCS MŰSZAK",
                            color = WarmGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }"""
content = content.replace(old_inner, new_inner)

# Fix timeStr text logic
old_text_str = """text = if (state.isTimerActive) timeStr else "KÉSZ","""
new_text_str = """text = if (state.isTimerActive) timeStr else if (state.isWaitingForBonus || !state.isDuringShift) "KÉSZ" else "MEHETSZ!","""
content = content.replace(old_text_str, new_text_str)

# Remove quick stats grid
old_stats = """            // 4. Quick Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's ratio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateCard, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "NAPI MENNYISÉG",
                            color = TextPrimary.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.smokedToday}",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ${state.dailyLimit}",
                                color = TextPrimary.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                // Rollover stats
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateCard, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "VISSZAMARADT / BÓNUSZ",
                            color = TextPrimary.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.rolloverCigarettes}",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "szál tegnapról",
                                color = TextPrimary.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }"""

new_stats = """            // 4. Műszakban elszívott
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCard, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MŰSZAKBAN ELSZÍVVA",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${state.smokedToday} SZÁL",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }"""

content = content.replace(old_stats, new_stats)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
