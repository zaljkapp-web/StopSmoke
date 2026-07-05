package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.SmokingViewModel

@Composable
fun VacationScreen(
    viewModel: SmokingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isVacationActive = state.currentOverride?.isVacation ?: false

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Szabadság Üzemmód",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = IceBlue
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Kapcsold be, ha munkanapon mégsem mész be dolgozni.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary.copy(alpha = 0.6f)
                )
            )
        }

        // Vacation Big Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (isVacationActive) AccentGreen.copy(alpha = 0.1f) else SlateCard,
                            DarkSurface
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    1.dp,
                    if (isVacationActive) AccentGreen else SlateBorder,
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Beach access icon with pulsing scale animation depending on active state
                Icon(
                    imageVector = Icons.Default.BeachAccess,
                    contentDescription = null,
                    tint = if (isVacationActive) AccentGreen else TextPrimary.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = if (isVacationActive) "Szabadságon vagy! 🎉" else "Munkanap Üzemmód",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isVacationActive) AccentGreen else TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isVacationActive) {
                        "A mai napot pihenőnapként kezeljük. Nincsenek műszakos időzítők vagy műszakindító figyelmeztetések. A nap végzési ideje: másnap 00:30."
                    } else {
                        "A beosztásod szerinti normál műszakos rend van érvényben. Ha mára mégis szabadságot kaptál, kapcsold be az alábbi gombot."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Big beautifully stylized Toggle Button
                Button(
                    onClick = { viewModel.updateVacation(!isVacationActive) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVacationActive) AccentRed else AccentGreen,
                        contentColor = DarkBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("toggle_vacation_button")
                ) {
                    Text(
                        text = if (isVacationActive) "Szabadság Kikapcsolása" else "Szabadság Aktiválása 🏖️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Info box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard, RoundedCornerShape(12.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = IceBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "A szabadság mód csak a mai dohányzási napot írja felül pihenőnapra. Holnap újra a normál 28 napos ciklus lép életbe.",
                fontSize = 11.sp,
                color = TextPrimary.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Simple TextAlign import fallback support
typealias TextAlign = androidx.compose.ui.text.style.TextAlign
