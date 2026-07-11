#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/DashboardScreen.kt
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CigaretteLog
import com.example.data.ShiftType
import com.example.ui.theme.*
import com.example.viewmodel.SmokingViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun DashboardScreen(
    viewModel: SmokingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    if (state.needsInitialization) {
        InitializationDialog(onComplete = { bonus, smoked -> viewModel.completeInitialization(bonus, smoked) })
    }

    // Quotes logic: change quote every time we smoke or open
    var currentQuote by remember {
        mutableStateOf(CYNICAL_QUOTES[Random.nextInt(CYNICAL_QUOTES.size)])
    }

    LaunchedEffect(state.smokedToday) {
        currentQuote = CYNICAL_QUOTES[Random.nextInt(CYNICAL_QUOTES.size)]
    }

    val totalSeconds = remember(state.nextAllowedTime) {
        // Approximate total spacing interval for the dial progress
        if (state.isDuringShift) {
            val totalShiftSecs = 8 * 3600L // 8 hours shift as standard
            val limit = state.dailyLimit.coerceAtLeast(1)
            totalShiftSecs / limit
        } else {
            val totalDaySecs = 16 * 3600L // 16 waking hours as standard
            val limit = state.dailyLimit.coerceAtLeast(1)
            totalDaySecs / limit
        }
    }

    val progress = remember(state.timerSecondsRemaining, totalSeconds) {
        if (totalSeconds > 0) {
            (state.timerSecondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // Animated progress to look extremely modern and fluid
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "TimerProgress"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        val availableHeight = maxHeight
        val isTablet = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SmokeShift",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IceBlue,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(WarmGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = DeepBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (state.isDuringShift) AccentGreen else WarmGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.activeShift.displayName} műszak (Vége: ${state.shiftEnd?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "-"})",
                            color = TextPrimary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .background(SlateCard, RoundedCornerShape(12.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = state.currentSmokingDay.format(DateTimeFormatter.ofPattern("MM. dd.")),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // 2. Timer Dial (Hero Element)
            Box(
                modifier = Modifier
                    .size(if (isTablet) 320.dp else 260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = SlateCard,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                // Progress Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(AccentGreen, WarmGold, AccentRed),
                            center = center
                        ),
                        startAngle = 140f,
                        sweepAngle = 260f * (1f - animatedProgress),
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "KÖVETKEZŐ CIGI",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val timeStr = formatRemainingTime(state.timerSecondsRemaining)
                    Text(
                        text = if (state.isTimerActive) timeStr else "KÉSZ",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (state.isTimerActive) IceBlue else AccentGreen
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HÁTRALÉVŐ: ${state.remainingToday} SZÁL",
                        color = WarmGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ELŐZŐ ÓTA: ${formatRemainingTime(state.secondsSinceLastCigarette)}",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }

            // 3. Fun Quote Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(WarmGold, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = currentQuote,
                        color = TextPrimary.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }
            }

            // 4. Quick Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's ratio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateCard, RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "NAPI MENNYISÉG",
                            color = TextPrimary.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.smokedToday}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceBlue
                            )
                            Text(
                                text = " / ${state.dailyLimit}",
                                fontSize = 14.sp,
                                color = TextPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                // Rollover & Overtime indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateCard, RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "VISSZAMARADT / BÓNUSZ",
                            color = TextPrimary.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (state.rolloverCigarettes > 0) "+${state.rolloverCigarettes}" else "0",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.rolloverCigarettes > 0) AccentGreen else TextPrimary
                            )
                            Text(
                                text = " szál tegnapról",
                                fontSize = 11.sp,
                                color = TextPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                            )
                        }
                    }
                }
            }

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

fun formatRemainingTime(seconds: Long): String {
    if (seconds <= 0) return "00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

val CYNICAL_QUOTES = listOf(
    "Még egy szög a koporsódba? Hajrá, úgyis túl sokat költöttél nyugdíj-előtakarékosságra.",
    "A tüdőd már szinte könyörög egy kis koromért. Ne okozz neki csalódást!",
    "Dohányzás: mert a tiszta levegő amúgy is rettenetesen unalmas.",
    "A nikotin az egyetlen barátod, aki veled lesz egészen a... nos, a legvégéig.",
    "Füstöld el a pénzed bátran! A dohányipari vezetők jachtjait is tankolni kell valamiből.",
    "Már tiszta a tüdőd? Gyorsan korrigáld ezt az egészségügyi hibát!",
    "Csak nyugodtan szívd el. Az orvosoknak is kell a praxis-fejlesztési pénz.",
    "Kellemes füstölést! Ne felejtsd el utána alaposan leköhögni a családodat.",
    "Tudtad? Minden szál cigarettával 11 percet spórolsz meg a jövőbeli szenvedéseidből.",
    "Gratulálok, túlélted a legutóbbi szünetet. Íme a jól megérdemelt mérged!",
    "Ne aggódj a légszomj miatt, a lényeg, hogy most megvan a napi megnyugvás."
)

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
