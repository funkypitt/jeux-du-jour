package com.freedomfighter.jeuxdujour.ui.lemot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedomfighter.jeuxdujour.ui.components.GameHeader
import com.freedomfighter.jeuxdujour.ui.components.GameKeyboard
import com.freedomfighter.jeuxdujour.ui.components.TileCell
import com.freedomfighter.jeuxdujour.ui.components.TileState

@Composable
fun LeMotScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeMotViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                title = "Le Mot",
                onNavigateBack = onNavigateBack,
                trailing = {
                    if (state.isGameOver) {
                        IconButton(onClick = {
                            val text = viewModel.getShareText()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Le Mot", text))
                            Toast.makeText(context, "Copié !", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Partager")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Grid
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until LeMotRepository.MAX_GUESSES) {
                    val isRevealing = state.revealingRow == row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        for (col in 0 until LeMotRepository.WORD_LENGTH) {
                            val letter: Char?
                            val tileState: TileState

                            when {
                                row < state.guesses.size -> {
                                    letter = state.guesses[row][col]
                                    tileState = when (state.evaluations[row][col]) {
                                        LetterEvaluation.CORRECT -> TileState.CORRECT
                                        LetterEvaluation.PRESENT -> TileState.PRESENT
                                        LetterEvaluation.ABSENT -> TileState.ABSENT
                                    }
                                }
                                row == state.currentRow -> {
                                    letter = state.currentInput.getOrNull(col)
                                    tileState = if (letter != null) TileState.FILLED else TileState.EMPTY
                                }
                                else -> {
                                    letter = null
                                    tileState = TileState.EMPTY
                                }
                            }

                            TileCell(
                                letter = letter,
                                state = tileState,
                                animate = isRevealing,
                                animationDelay = col * 150
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keyboard
            val letterStates = viewModel.getLetterStates()
            GameKeyboard(
                letterStates = letterStates,
                onKeyPress = viewModel::onKeyPress,
                onEnter = viewModel::onEnter,
                onBackspace = viewModel::onBackspace,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
