import re

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'r') as f:
    content = f.read()

# Remove the text "Kövesd nyomon a fejlődésed a kezdetektől fogva (2026. június 22.)"
content = re.sub(r'            Text\(\s*text = "Kövesd nyomon a fejlődésed a kezdetektől fogva \(2026\. június 22\.\)",\s*style = MaterialTheme\.typography\.bodySmall\.copy\(\s*color = TextPrimary\.copy\(alpha = 0\.6f\)\s*\)\s*\)\n', '', content)

# Remove the savings row
content = re.sub(r'                Row\(horizontalArrangement = Arrangement\.spacedBy\(12\.dp\), modifier = Modifier\.fillMaxWidth\(\)\) \{\s*StatCard\(\s*title = "MEGTAKARÍTÁS",[\s\S]*?modifier = Modifier\.weight\(1f\)\s*\)\s*\}\n', '', content)

with open('app/src/main/java/com/example/ui/screens/StatsScreen.kt', 'w') as f:
    f.write(content)
