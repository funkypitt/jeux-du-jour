package com.freedomfighter.jeuxdujour.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

val HexagonShapeClip = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2
        val cy = size.height / 2
        val r = minOf(cx, cy)
        for (i in 0..5) {
            val angle = Math.toRadians((60.0 * i - 30.0)).toFloat()
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun HexagonButton(
    letter: Char,
    isCenter: Boolean = false,
    size: Dp = 72.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isCenter) {
        com.freedomfighter.jeuxdujour.ui.theme.HexagonCenterFill
    } else {
        com.freedomfighter.jeuxdujour.ui.theme.HexagonFill
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(HexagonShapeClip)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercase(),
            fontSize = (size.value * 0.35f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
