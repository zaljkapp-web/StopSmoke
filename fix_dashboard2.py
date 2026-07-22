import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    lines = f.readlines()

new_stats = """            // 4. Műszakban elszívott
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCard, RoundedCornerShape(16.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
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
            }
"""

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    for i, line in enumerate(lines):
        if 253 <= i <= 326: # 0-indexed: 253 corresponds to line 254, 326 corresponds to line 327
            continue
        f.write(line)
        if i == 252:
            f.write(new_stats)
