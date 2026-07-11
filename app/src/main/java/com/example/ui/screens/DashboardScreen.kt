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
                                color = DarkBg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Active shift indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state.activeShift) {
                                        ShiftType.REST -> AccentGreen
                                        ShiftType.MORNING -> IceBlue
                                        ShiftType.AFTERNOON -> WarmGold
                                        ShiftType.NIGHT -> AccentRed
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (state.activeShift) {
                                ShiftType.REST -> "Pihenő nap (Vége: másnap 00:30)"
                                ShiftType.MORNING -> "Délelőttös műszak (Vége: 23:30)"
                                ShiftType.AFTERNOON -> "Délutános műszak (Vége: másnap 01:00)"
                                ShiftType.NIGHT -> "Éjszakás műszak (Vége: másnap 09:00)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Current day indicator
                Box(
                    modifier = Modifier
                        .background(SlateCard, RoundedCornerShape(12.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = state.currentSmokingDay.format(DateTimeFormatter.ofPattern("MM. dd.")),
                        color = IceBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // 2. Circular Countdown Timer
            Box(
                modifier = Modifier
                    .size(if (isTablet) 220.dp else 240.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SlateCard,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 12.dp.toPx())
                    )
                }

                // Animated Active Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = if (state.timerSecondsRemaining > 0) WarmGold else AccentGreen,
                        startAngle = -90f,
                        sweepAngle = if (state.timerSecondsRemaining > 0) (360f * animatedProgress) else 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Text labels in center
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (state.timerSecondsRemaining > 0) "KÖVETKEZŐ CIGI" else "SZÍVHATOD!",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRemainingTime(state.timerSecondsRemaining),
                        color = if (state.timerSecondsRemaining > 0) TextPrimary else AccentGreen,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "HÁTRALÉVŐ: ${state.remainingToday} SZÁL",
                        color = WarmGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (state.lastCigaretteTime != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ELŐZŐ ÓTA: ${formatRemainingTime(state.secondsSinceLastCigarette)}",
                            color = TextPrimary.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Cynical Quote Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical accent indicator
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .background(WarmGold, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentQuote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.weight(1f)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        .weight(1.8f)
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ELSZÍVVA 🚬",
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
                        .weight(1.2f)
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
                            text = "☕ KÁVÉ +2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "2 szál szívás",
                            fontSize = 9.sp,
                            color = TextPrimary.copy(alpha = 0.5f)
                        )
                    }
                }

                // Undo Button
                if (state.smokedToday > 0) {
                    IconButton(
                        onClick = { viewModel.deleteLastLog() },
                        modifier = Modifier
                            .size(60.dp)
                            .background(DarkSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                            .testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Utolsó visszavonása",
                            tint = AccentRed
                        )
                    }
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
