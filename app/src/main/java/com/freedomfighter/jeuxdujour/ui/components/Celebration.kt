package com.freedomfighter.jeuxdujour.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

@Composable
fun Celebration(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    // Confetti — fullscreen overlay
    val confettiComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/confetti.json")
    )
    val confettiProgress by animateLottieCompositionAsState(
        composition = confettiComposition,
        iterations = 1
    )

    // Cat celebration — centered
    val catComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/cat_celebration.json")
    )
    val catProgress by animateLottieCompositionAsState(
        composition = catComposition,
        iterations = 1
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Confetti background layer
        LottieAnimation(
            composition = confettiComposition,
            progress = { confettiProgress },
            modifier = Modifier.fillMaxSize()
        )

        // Cat centered
        LottieAnimation(
            composition = catComposition,
            progress = { catProgress },
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
        )
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(4000)
            onDismiss()
        }
    }
}

/**
 * Brief success animation (checkmark pop) for finding a word or solving a group.
 */
@Composable
fun SuccessFlash(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset("lottie/check_pop.json")
        )
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = 1
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(80.dp)
            )
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(800)
            onDismiss()
        }
    }
}
