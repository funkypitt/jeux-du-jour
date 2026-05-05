package com.freedomfighter.jeuxdujour.ui.hexagone

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedomfighter.jeuxdujour.ui.components.Celebration
import com.freedomfighter.jeuxdujour.ui.components.GameHeader
import com.freedomfighter.jeuxdujour.ui.components.SuccessFlash
import com.freedomfighter.jeuxdujour.ui.components.HexagonButton
import com.freedomfighter.jeuxdujour.ui.theme.AccentWarm
import com.freedomfighter.jeuxdujour.ui.theme.CorrectGreen

@Composable
fun HexagoneScreen(
    onNavigateBack: () -> Unit,
    viewModel: HexagoneViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameHeader(title = "L'Hexagone", onNavigateBack = onNavigateBack)

                // Rank and progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = state.rank.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (state.maxScore > 0) state.score.toFloat() / state.maxScore else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = CorrectGreen
                    )
                    Text(
                        text = "${state.score} / ${state.maxScore} pts",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Found words
                if (state.foundWords.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.foundWords.sortedDescending()) { word ->
                            val isPangram = word in state.pangrams
                            Text(
                                text = word.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isPangram) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPangram) AccentWarm else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.3f))

                // Current input
                Text(
                    text = state.currentInput.uppercase().ifEmpty { " " },
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .animateContentSize()
                )

                Spacer(modifier = Modifier.weight(0.3f))

                // Hexagonal layout
                if (state.outerLetters.size >= 6) {
                    HexagonalGrid(
                        centerLetter = state.centerLetter,
                        outerLetters = state.outerLetters,
                        onLetterClick = { letter ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onLetterPress(letter)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = viewModel::onClearInput) {
                        Text("Effacer")
                    }
                    IconButton(onClick = viewModel::onShuffle) {
                        Icon(Icons.Default.Refresh, contentDescription = "Mélanger")
                    }
                    OutlinedButton(onClick = viewModel::onBackspace) {
                        Icon(Icons.Default.Backspace, contentDescription = "Supprimer")
                    }
                    Button(
                        onClick = viewModel::onSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                    ) {
                        Text("Valider")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Success flash (word found)
        SuccessFlash(
            visible = state.showSuccessFlash,
            onDismiss = viewModel::dismissSuccessFlash,
            modifier = Modifier.align(Alignment.Center)
        )

        // Celebration overlay (game complete)
        Celebration(
            visible = state.showCelebration,
            onDismiss = viewModel::dismissCelebration
        )
    }
}

@Composable
private fun HexagonalGrid(
    centerLetter: Char,
    outerLetters: List<Char>,
    onLetterClick: (Char) -> Unit
) {
    val hexSize = 64.dp
    val spacing = 4.dp
    val hOffset = (hexSize.value * 0.87f + spacing.value).dp
    val vOffset = (hexSize.value * 0.75f + spacing.value).dp

    Box(contentAlignment = Alignment.Center) {
        // Center
        HexagonButton(
            letter = centerLetter,
            isCenter = true,
            size = hexSize,
            onClick = { onLetterClick(centerLetter) }
        )

        // Top
        if (outerLetters.size > 0) {
            HexagonButton(
                letter = outerLetters[0],
                size = hexSize,
                onClick = { onLetterClick(outerLetters[0]) },
                modifier = Modifier.offset(y = -vOffset)
            )
        }
        // Top-right
        if (outerLetters.size > 1) {
            HexagonButton(
                letter = outerLetters[1],
                size = hexSize,
                onClick = { onLetterClick(outerLetters[1]) },
                modifier = Modifier.offset(x = hOffset, y = -(vOffset / 2))
            )
        }
        // Bottom-right
        if (outerLetters.size > 2) {
            HexagonButton(
                letter = outerLetters[2],
                size = hexSize,
                onClick = { onLetterClick(outerLetters[2]) },
                modifier = Modifier.offset(x = hOffset, y = (vOffset / 2))
            )
        }
        // Bottom
        if (outerLetters.size > 3) {
            HexagonButton(
                letter = outerLetters[3],
                size = hexSize,
                onClick = { onLetterClick(outerLetters[3]) },
                modifier = Modifier.offset(y = vOffset)
            )
        }
        // Bottom-left
        if (outerLetters.size > 4) {
            HexagonButton(
                letter = outerLetters[4],
                size = hexSize,
                onClick = { onLetterClick(outerLetters[4]) },
                modifier = Modifier.offset(x = -hOffset, y = (vOffset / 2))
            )
        }
        // Top-left
        if (outerLetters.size > 5) {
            HexagonButton(
                letter = outerLetters[5],
                size = hexSize,
                onClick = { onLetterClick(outerLetters[5]) },
                modifier = Modifier.offset(x = -hOffset, y = -(vOffset / 2))
            )
        }
    }
}
