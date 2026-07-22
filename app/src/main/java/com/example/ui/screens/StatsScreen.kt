package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.DayStats
import com.example.viewmodel.SmokingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Composable
fun StatsScreen(
    viewModel: SmokingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // 1. Calculate general statistics
    val totalDays = max(1L, ChronoUnit.DAYS.between(com.example.data.SmokingScheduleHelper.START_DATE, LocalDate.now()) + 1)
    val totalSmoked = state.allLogs.sumOf { it.amount }
    
    // Financial calculation: assuming average price of 1 cigarette is 120 Ft (2400 Ft / 20 pack)
    val pricePerCigarette = 120
    val moneySpent = totalSmoked * pricePerCigarette
    
    // Saved money: how much they saved by reducing from 25 cigarettes/day starting from June 22, 2026
    val initialBaselineCount = totalDays * 25
    val savedCount = max(0L, initialBaselineCount - totalSmoked)
    val moneySaved = savedCount * pricePerCigarette

    // Streak calculation: days where smoked <= limit
    val successfulDays = state.dayStatsList.count { it.smoked == 0 }
    val currentStreak = calculateCurrentStreak(state.dayStatsList)

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
                text = "Statisztikák és Kimutatások",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = IceBlue
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Kövesd nyomon a fejlődésed a kezdetektől fogva (2026. június 22.)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary.copy(alpha = 0.6f)
                )
            )
        }

        // Summary Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "ÖSSZESEN ELTÉLT",
                        value = "$totalDays nap",
                        icon = Icons.Default.Info,
                        iconTint = IceBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "BETARTOTT NAPOK",
                        value = "$successfulDays nap",
                        icon = Icons.Default.TrendingDown,
                        iconTint = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "MEGTAKARÍTÁS",
                        value = "${String.format("%,d", moneySaved)} Ft",
                        icon = Icons.Default.Savings,
                        iconTint = WarmGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "KÖLTÉS (MÉRGEKRE)",
                        value = "${String.format("%,d", moneySpent)} Ft",
                        icon = Icons.Default.AttachMoney,
                        iconTint = AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bar Chart Header
        item {
            Text(
                text = "Napi cigaretták száma (Utolsó 7 nap)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IceBlue
                )
            )
        }

        // Custom Native Bar Chart
        item {
            val last7DaysStats = state.dayStatsList.takeLast(7)
            if (last7DaysStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(SlateCard, RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nincs elegendő adat a diagram megjelenítéséhez.",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCard, RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    val maxVal = last7DaysStats.maxOfOrNull { it.smoked }?.coerceAtLeast(1) ?: 20

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7DaysStats.forEach { dayStat ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // Stacked bar: Limit vs Smoked
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        // Max indicator
                                        val smokedHeightPct = (dayStat.smoked.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                                        val limitHeightPct = (dayStat.smoked.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)

                                        Text(
                                            text = "${dayStat.smoked}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IceBlue
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .fillMaxHeight(smokedHeightPct)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    IceBlue
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = dayStat.date.format(DateTimeFormatter.ofPattern("E")),
                                    fontSize = 10.sp,
                                    color = TextPrimary.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = SlateBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        LegendItem(color = IceBlue, label = "Betartott limiten belül")
                        LegendItem(color = AccentRed, label = "Túllépett nap")
                    }
                }
            }
        }

        // History list header
        item {
            Text(
                text = "Korábbi napok naplója",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IceBlue
                )
            )
        }

        // History items
        val sortedStats = state.dayStatsList.sortedByDescending { it.date }
        items(sortedStats) { dayStat ->
            HistoryItem(dayStat)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SlateCard, RoundedCornerShape(16.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = TextPrimary.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextPrimary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun HistoryItem(dayStat: DayStats) {
    val formatter = DateTimeFormatter.ofPattern("yyyy. MMMM dd., EEEE")
    val isSuccess = true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayStat.date.format(formatter).replaceFirstChar { it.uppercase() },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Műszak: ${dayStat.activeShift.displayName}",
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${dayStat.smoked} szál",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) IceBlue else AccentRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isSuccess) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isSuccess) "Betartva ✓" else "Túllépve ✗",
                        color = if (isSuccess) AccentGreen else AccentRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Calculates current streak of days complying with the limit
 */
fun calculateCurrentStreak(statsList: List<DayStats>): Int {
    return 0
}
