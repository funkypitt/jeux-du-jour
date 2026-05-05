package com.freedomfighter.jeuxdujour.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedomfighter.jeuxdujour.ui.theme.AbsentGray
import com.freedomfighter.jeuxdujour.ui.theme.CorrectGreen
import com.freedomfighter.jeuxdujour.ui.theme.KeyBackground
import com.freedomfighter.jeuxdujour.ui.theme.KeyText
import com.freedomfighter.jeuxdujour.ui.theme.PresentYellow

// QWERTZ keyboard layout
private val KEYBOARD_ROWS = listOf(
    listOf('Q', 'W', 'E', 'R', 'T', 'Z', 'U', 'I', 'O', 'P'),
    listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'),
    listOf('Y', 'X', 'C', 'V', 'B', 'N', 'M')
)

@Composable
fun GameKeyboard(
    letterStates: Map<Char, TileState>,
    onKeyPress: (Char) -> Unit,
    onEnter: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        KEYBOARD_ROWS.forEachIndexed { index, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index == 2) {
                    KeyboardKey(
                        text = "ENT",
                        isWide = true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEnter()
                        }
                    )
                }
                row.forEach { letter ->
                    val state = letterStates[letter]
                    KeyboardKey(
                        text = letter.toString(),
                        state = state,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onKeyPress(letter)
                        }
                    )
                }
                if (index == 2) {
                    KeyboardKey(
                        text = "\u232B",
                        isWide = true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackspace()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    text: String,
    state: TileState? = null,
    isWide: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = when (state) {
        TileState.CORRECT -> CorrectGreen
        TileState.PRESENT -> PresentYellow
        TileState.ABSENT -> AbsentGray
        else -> KeyBackground
    }
    val textColor = when (state) {
        TileState.CORRECT, TileState.PRESENT, TileState.ABSENT -> Color.White
        else -> KeyText
    }

    Box(
        modifier = Modifier
            .width(if (isWide) 52.dp else 34.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (isWide) 12.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
