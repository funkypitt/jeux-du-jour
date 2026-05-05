package com.freedomfighter.jeuxdujour.ui.connexions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.freedomfighter.jeuxdujour.ui.theme.ConnexionBlue
import com.freedomfighter.jeuxdujour.ui.theme.ConnexionGreen
import com.freedomfighter.jeuxdujour.ui.theme.ConnexionPurple
import com.freedomfighter.jeuxdujour.ui.theme.ConnexionYellow
import com.freedomfighter.jeuxdujour.ui.theme.CorrectGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnexionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConnexionsViewModel = hiltViewModel()
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

    // Haptic on wrong guess
    LaunchedEffect(state.lastWrongGuess) {
        if (state.lastWrongGuess != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameHeader(title = "Connexions", onNavigateBack = onNavigateBack)

                Spacer(modifier = Modifier.height(8.dp))

                // Mistakes indicator
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Erreurs : ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    repeat(state.maxMistakes) { i ->
                        Text(
                            text = if (i < state.mistakes) "\u25CF" else "\u25CB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (i < state.mistakes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                        if (i < state.maxMistakes - 1) Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Solved groups
                state.solvedGroups.sortedBy { it.difficulty }.forEach { group ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        SolvedGroupCard(group = group)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Remaining word grid (4 columns)
                val remaining = state.remainingWords
                if (remaining.isNotEmpty()) {
                    val rows = remaining.chunked(4)
                    rows.forEach { rowWords ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowWords.forEach { word ->
                                val isSelected = word in state.selectedWords
                                WordTile(
                                    word = word,
                                    isSelected = isSelected,
                                    onClick = { viewModel.onWordToggle(word) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if row is incomplete
                            repeat(4 - rowWords.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                if (!state.isGameOver) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = viewModel::onDeselectAll) {
                            Text("Effacer")
                        }
                        Button(
                            onClick = viewModel::onSubmit,
                            enabled = state.selectedWords.size == ConnexionsRepository.WORDS_PER_GROUP,
                            colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                        ) {
                            Text("Valider")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Success flash (group solved)
        SuccessFlash(
            visible = state.showSuccessFlash,
            onDismiss = viewModel::dismissSuccessFlash,
            modifier = Modifier.align(Alignment.Center)
        )

        // Celebration overlay (game won)
        Celebration(
            visible = state.showCelebration,
            onDismiss = viewModel::dismissCelebration
        )
    }
}

@Composable
private fun WordTile(
    word: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = word.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.surface
                   else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun SolvedGroupCard(group: ConnexionGroup) {
    val bgColor = when (group.difficulty) {
        0 -> ConnexionYellow
        1 -> ConnexionGreen
        2 -> ConnexionBlue
        3 -> ConnexionPurple
        else -> ConnexionYellow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = group.category.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = group.words.joinToString(", ") { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
