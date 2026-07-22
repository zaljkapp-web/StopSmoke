import re

with open('app/src/main/java/com/example/ui/screens/ShiftScreen.kt', 'r') as f:
    content = f.read()

# Remove the whole 28 Napos Folyamatos Műszakrend block
block_to_remove = r'''            Box\(
                modifier = Modifier
                    \.fillMaxWidth\(\)
                    \.background\(SlateCard, RoundedCornerShape\(16\.dp\)\)
                    \.border\(1\.dp, SlateBorder, RoundedCornerShape\(16\.dp\)\)
                    \.padding\(16\.dp\)
            \) \{
                Column \{
                    Text\(
                        text = "28 Napos Folyamatos Műszakrend",
                        fontWeight = FontWeight\.Bold,
                        fontSize = 14\.sp,
                        color = IceBlue
                    \)
                    Spacer\(modifier = Modifier\.height\(8\.dp\)\)
                    Text\(
                        text = "A műszakbeosztásod június 22-én kezdődött a következő körforgással:\\n" \+
                                "• 6 nap délelőtt \(Kezdés: 06:00, Zárás: 23:30\)\\n" \+
                                "• 1 nap pihenő \(Zárás: másnap 00:30\)\\n" \+
                                "• 3 nap délután \(Kezdés: 14:00, Zárás: másnap 01:00\)\\n" \+
                                "• 1 nap pihenő \(Zárás: másnap 00:30\)\\n" \+
                                "• 4 nap éjszaka \(Kezdés: 22:00, Zárás: másnap 09:00\)\\n" \+
                                "• 2 nap pihenő\\n" \+
                                "• 4 nap délután\\n" \+
                                "• 1 nap pihenő\\n" \+
                                "• 3 nap éjszaka\\n" \+
                                "• 3 nap pihenő\\n\\n" \+
                                "A heti limit csökkentést \(hetente -1 szál\) is ettől a naptól fogva számoljuk automatikusan\.",
                        fontSize = 11\.sp,
                        color = TextPrimary\.copy\(alpha = 0\.7f\),
                        lineHeight = 16\.sp
                    \)
                \}
            \}'''

content = re.sub(block_to_remove, '', content)

with open('app/src/main/java/com/example/ui/screens/ShiftScreen.kt', 'w') as f:
    f.write(content)
