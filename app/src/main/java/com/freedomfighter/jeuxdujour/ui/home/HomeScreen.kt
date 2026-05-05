package com.freedomfighter.jeuxdujour.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedomfighter.jeuxdujour.ui.theme.AccentWarm
import com.freedomfighter.jeuxdujour.ui.theme.ConnexionPurple
import com.freedomfighter.jeuxdujour.ui.theme.CorrectGreen
import com.freedomfighter.jeuxdujour.ui.theme.HexagonFill
import com.freedomfighter.jeuxdujour.ui.theme.PresentYellow

@Composable
fun HomeScreen(
    onNavigateToLeMot: () -> Unit,
    onNavigateToHexagone: () -> Unit,
    onNavigateToConnexions: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App title
        Text(
            text = "Jeux du Jour",
            style = MaterialTheme.typography.displayLarge
        )

        // Date + streak on same line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = state.dateDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            if (state.streak > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "\u2022 ${state.streak} jour${if (state.streak > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentWarm
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Game cards — minimal, title-only
        GameCard(
            title = "Le Mot",
            icon = Icons.Default.GridOn,
            accentColor = CorrectGreen,
            status = state.leMotStatus,
            onClick = onNavigateToLeMot
        )

        Spacer(modifier = Modifier.height(12.dp))

        GameCard(
            title = "L'Hexagone",
            icon = Icons.Default.Hexagon,
            accentColor = HexagonFill,
            status = state.hexagoneStatus,
            onClick = onNavigateToHexagone
        )

        Spacer(modifier = Modifier.height(12.dp))

        GameCard(
            title = "Connexions",
            icon = Icons.Default.Extension,
            accentColor = ConnexionPurple,
            status = state.connexionsStatus,
            onClick = onNavigateToConnexions
        )

        Spacer(modifier = Modifier.weight(1f))

        // Mot du jour — subtle, at the bottom
        MotDuJourCard(
            entry = state.motDuJour,
            modifier = Modifier.padding(top = 24.dp)
        )

        // Sound toggle — minimal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = viewModel::toggleSound) {
                Icon(
                    imageVector = if (state.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Son",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    status: GameCardStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title only
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            // Status dot
            StatusIndicator(status = status)
        }
    }
}

@Composable
private fun StatusIndicator(status: GameCardStatus) {
    val color = when (status) {
        GameCardStatus.TODO -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        GameCardStatus.IN_PROGRESS -> PresentYellow
        GameCardStatus.DONE -> CorrectGreen
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}
