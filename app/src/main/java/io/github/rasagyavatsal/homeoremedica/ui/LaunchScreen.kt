package io.github.rasagyavatsal.homeoremedica.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import io.github.rasagyavatsal.homeoremedica.ui.theme.Ease
import io.github.rasagyavatsal.homeoremedica.ui.theme.Theme
import kotlinx.coroutines.delay

// The web app's `--duration-pen`, and the pen's curve.
private const val PEN_MILLIS = 1300
private val PenEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private const val HOLD_MILLIS = 250
private const val FADE_MILLIS = 350

/**
 * The whole name written out left to right, as with a pen, over the app as it opens. It then fades to show the
 * app and calls [onDone].
 */
@Composable
fun LaunchScreen(onDone: () -> Unit) {
    val pen = remember { Animatable(0f) }
    val shown = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        pen.animateTo(1f, tween(PEN_MILLIS, easing = PenEasing))
        delay(HOLD_MILLIS.toLong())
        shown.animateTo(0f, tween(FADE_MILLIS, easing = Ease))
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = shown.value }
            .background(Theme.colors.canvas)
            // The app underneath is not touched through it.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        contentAlignment = Alignment.Center,
    ) {
        Brand(fontSize = 44.sp, pen = { pen.value })
    }
}
