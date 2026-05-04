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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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

    // Refresh status when returning to this screen
    LaunchedEffect(Unit) {
        viewModel.loadState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // App title
        Text(
            text = "Jeux du Jour",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Date
        Text(
            text = state.dateDisplay,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Streak
        if (state.streak > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Série : ${state.streak} jour${if (state.streak > 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
                color = AccentWarm
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(24.dp))

        // Game cards
        GameCard(
            title = "Le Mot",
            description = "Trouvez le mot en 6 essais",
            icon = Icons.Default.GridOn,
            accentColor = CorrectGreen,
            status = state.leMotStatus,
            onClick = onNavigateToLeMot
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameCard(
            title = "L'Hexagone",
            description = "Formez des mots avec 7 lettres",
            icon = Icons.Default.Hexagon,
            accentColor = HexagonFill,
            status = state.hexagoneStatus,
            onClick = onNavigateToHexagone
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameCard(
            title = "Connexions",
            description = "Trouvez les 4 groupes de 4 mots",
            icon = Icons.Default.Extension,
            accentColor = ConnexionPurple,
            status = state.connexionsStatus,
            onClick = onNavigateToConnexions
        )

        Spacer(modifier = Modifier.weight(1f))

        // Sound toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::toggleSound) {
                Icon(
                    imageVector = if (state.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Son",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GameCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    status: GameCardStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Status badge
            val badgeColor = when (status) {
                GameCardStatus.TODO -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                GameCardStatus.IN_PROGRESS -> PresentYellow
                GameCardStatus.DONE -> CorrectGreen
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = status.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }
    }
}
