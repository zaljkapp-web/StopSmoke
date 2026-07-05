package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShiftType
import com.example.ui.theme.*
import com.example.viewmodel.SmokingViewModel
import java.time.format.DateTimeFormatter

@Composable
fun ShiftScreen(
    viewModel: SmokingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Local overtime input states
    var startOvertimeHours by remember(state.currentOverride) {
        mutableStateOf(state.currentOverride?.overtimeStartHours ?: 0)
    }
    var endOvertimeHours by remember(state.currentOverride) {
        mutableStateOf(state.currentOverride?.overtimeEndHours ?: 0)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Header
        item {
            Text(
                text = "Műszak és Túlóra",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = IceBlue
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Igazítsd az időzítőket és az értesítéseket a napi beosztásodhoz és túlóráidhoz.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary.copy(alpha = 0.6f)
                )
            )
        }

        // Current Shift Info Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(SlateCard, DarkSurface)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AKTUÁLIS MŰSZAKADATOK",
                            color = WarmGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(IceBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = state.activeShift.displayName,
                                color = IceBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.activeShift == ShiftType.REST) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Weekend,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ma pihenőnapod van. Élvezd a nyugalmat, a dohányzási időzítőd a mai nap lezárásáig (másnap 00:30) ketyeg.",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    } else {
                        // Display active hours
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val startStr = state.shiftStart?.format(timeFormatter) ?: "--:--"
                        val endStr = state.shiftEnd?.format(timeFormatter) ?: "--:--"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Műszak kezdete",
                                    fontSize = 11.sp,
                                    color = TextPrimary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = startStr,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = IceBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Műszak vége",
                                    fontSize = 11.sp,
                                    color = TextPrimary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = endStr,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                        }

                        if (state.isDuringShift) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "• Műszak folyamatban - A számláló műszakos üzemmódban van",
                                    color = AccentRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shift-end Reward (Műszak vége jutalom)
        if (state.activeShift != ShiftType.REST) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Műszak Végszó",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ha lejárt a műszakod, nullázd le a visszaszámlálót és igényeld meg a jól megérdemelt műszak végi bónusz cigidet!",
                            fontSize = 12.sp,
                            color = TextPrimary.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.triggerShiftEndReward() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmGold,
                                contentColor = DarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("shift_end_reward_button")
                        ) {
                            Text(
                                text = "MŰSZAK VÉGE JUTALOM 🎁",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Overtime Adjustment Section
        if (state.activeShift != ShiftType.REST) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "TÚLÓRA BEÁLLÍTÁSA",
                        color = IceBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )

                    // Start Overtime
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hány órával korábban kezdesz?",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "$startOvertimeHours óra",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmGold
                            )
                        }
                        Slider(
                            value = startOvertimeHours.toFloat(),
                            onValueChange = { startOvertimeHours = it.toInt() },
                            valueRange = 0f..4f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = WarmGold,
                                activeTrackColor = WarmGold,
                                inactiveTrackColor = SlateBorder
                            ),
                            modifier = Modifier.testTag("start_overtime_slider")
                        )
                    }

                    // End Overtime
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hány órával végzel később?",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "$endOvertimeHours óra",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmGold
                            )
                        }
                        Slider(
                            value = endOvertimeHours.toFloat(),
                            onValueChange = { endOvertimeHours = it.toInt() },
                            valueRange = 0f..8f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = WarmGold,
                                activeTrackColor = WarmGold,
                                inactiveTrackColor = SlateBorder
                            ),
                            modifier = Modifier.testTag("end_overtime_slider")
                        )
                    }

                    Button(
                        onClick = { viewModel.updateOvertime(startOvertimeHours, endOvertimeHours) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceBlue,
                            contentColor = DeepBlue
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_overtime_button")
                    ) {
                        Text(
                            text = "Túlóra Mentése",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 28-day schedule explainer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "28 Napos Folyamatos Műszakrend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IceBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A műszakbeosztásod június 22-én kezdődött a következő körforgással:\n" +
                                "• 6 nap délelőtt (Kezdés: 06:00, Zárás: 23:30)\n" +
                                "• 1 nap pihenő (Zárás: másnap 00:30)\n" +
                                "• 3 nap délután (Kezdés: 14:00, Zárás: másnap 01:00)\n" +
                                "• 1 nap pihenő (Zárás: másnap 00:30)\n" +
                                "• 4 nap éjszaka (Kezdés: 22:00, Zárás: másnap 09:00)\n" +
                                "• 2 nap pihenő\n" +
                                "• 4 nap délután\n" +
                                "• 1 nap pihenő\n" +
                                "• 3 nap éjszaka\n" +
                                "• 3 nap pihenő\n\n" +
                                "A heti limit csökkentést (hetente -1 szál) is ettől a naptól fogva számoljuk automatikusan.",
                        fontSize = 11.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
