package com.freedomfighter.jeuxdujour.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedomfighter.jeuxdujour.ui.theme.TileBorder
import com.freedomfighter.jeuxdujour.ui.theme.TileEmpty

enum class TileState {
    EMPTY, FILLED, CORRECT, PRESENT, ABSENT
}

@Composable
fun TileCell(
    letter: Char?,
    state: TileState,
    size: Dp = 58.dp,
    animate: Boolean = false,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(animate, state) {
        if (animate && state != TileState.EMPTY && state != TileState.FILLED) {
            rotation.snapTo(0f)
            kotlinx.coroutines.delay(animationDelay.toLong())
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 500)
            )
        }
    }

    val bgColor = when {
        rotation.value > 90f -> when (state) {
            TileState.CORRECT -> com.freedomfighter.jeuxdujour.ui.theme.CorrectGreen
            TileState.PRESENT -> com.freedomfighter.jeuxdujour.ui.theme.PresentYellow
            TileState.ABSENT -> com.freedomfighter.jeuxdujour.ui.theme.AbsentGray
            else -> TileEmpty
        }
        state == TileState.CORRECT || state == TileState.PRESENT || state == TileState.ABSENT -> {
            if (!animate) when (state) {
                TileState.CORRECT -> com.freedomfighter.jeuxdujour.ui.theme.CorrectGreen
                TileState.PRESENT -> com.freedomfighter.jeuxdujour.ui.theme.PresentYellow
                TileState.ABSENT -> com.freedomfighter.jeuxdujour.ui.theme.AbsentGray
                else -> TileEmpty
            } else TileEmpty
        }
        else -> TileEmpty
    }

    val textColor = when {
        bgColor == TileEmpty -> Color.Black
        else -> Color.White
    }

    val borderMod = if (bgColor == TileEmpty) {
        Modifier.border(
            width = 2.dp,
            color = if (letter != null) com.freedomfighter.jeuxdujour.ui.theme.TileFilled else TileBorder,
            shape = RoundedCornerShape(4.dp)
        )
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationX = rotation.value
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(borderMod),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                text = letter.uppercase(),
                fontSize = (size.value * 0.5f).sp,
                fontWeight = FontWeight.Bold,
                color = if (rotation.value > 90f) textColor
                       else if (!animate || state == TileState.FILLED || state == TileState.EMPTY) textColor
                       else Color.Black,
                modifier = Modifier.graphicsLayer {
                    if (rotation.value > 90f) rotationX = 180f
                }
            )
        }
    }
}
